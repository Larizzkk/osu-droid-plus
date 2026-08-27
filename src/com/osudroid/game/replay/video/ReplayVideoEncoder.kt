package com.osudroid.game.replay.video

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.GLES20
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Encodes OpenGL-rendered frames into an MP4 video file using MediaCodec + MediaMuxer.
 *
 * This encoder creates its own EGL context (NOT shared with the game) to avoid
 * cross-context texture sharing issues. Textures are created and uploaded entirely
 * within the encoder's context.
 *
 * Flow per frame:
 *   1. The caller provides pixel data via [encodeFrame]
 *   2. [encodeFrame] switches to the encoder's EGL context
 *   3. Uploads pixels to a texture in the encoder's context
 *   4. Renders the texture to MediaCodec's input surface
 *   5. Returns — the caller is responsible for restoring the game's EGL context
 */
class ReplayVideoEncoder(
    private val width: Int,
    private val height: Int,
    private val fps: Int,
    private val bitrate: Int,
    private val outputFile: File
) {

    companion object {
        private const val TAG = "ReplayVideoEncoder"
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val I_FRAME_INTERVAL = 1
        private const val TIMEOUT_US = 10_000L

        private val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        private val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """.trimIndent()

        private val QUAD_VERTICES = floatArrayOf(
            -1f, -1f,
             1f, -1f,
            -1f,  1f,
             1f,  1f
        )

        // Flipped vertically because glReadPixels gives bottom-to-top data
        private val QUAD_TEX_COORDS = floatArrayOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
        )
    }

    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var muxerTrackIndex = -1
    private var isStarted = false
    private var isMuxerStarted = false
    private var frameCount = 0L

    // Encoder's own EGL context (NOT shared with game)
    private var eglDisplay: android.opengl.EGLDisplay? = null
    private var eglContext: android.opengl.EGLContext? = null
    private var eglSurface: android.opengl.EGLSurface? = null

    // Encoder's own texture (created in encoder's context)
    private var encoderTextureId = 0

    private var shaderProgram = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var textureHandle = 0

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer

    private var inputSurface: android.view.Surface? = null

    var onFrameProgress: ((frame: Long) -> Unit)? = null
    var onComplete: ((File) -> Unit)? = null
    var onError: ((Exception) -> Unit)? = null

    // ── Lifecycle ──────────────────────────────────────────

    fun start(gameEglDisplay: android.opengl.EGLDisplay) {
        if (isStarted) {
            Log.w(TAG, "Already started, skipping")
            return
        }

        try {
            Log.i(TAG, "Starting: ${width}x${height} @ ${fps}fps, output=${outputFile.absolutePath}")

            initMediaCodec()
            Log.d(TAG, "MediaCodec configured")

            // Create input surface BEFORE starting codec (required by API)
            inputSurface = mediaCodec!!.createInputSurface()
            Log.d(TAG, "Input surface created")

            // Start codec AFTER creating input surface
            mediaCodec!!.start()
            Log.d(TAG, "MediaCodec started")

            initMuxer()
            Log.d(TAG, "MediaMuxer initialized")

            // Create EGL context — NOT shared with the game
            initEGL(gameEglDisplay)
            Log.d(TAG, "EGL initialized")

            // Make encoder context current to create textures and shaders
            makeCurrent()
            Log.d(TAG, "Encoder EGL context made current")

            initEncoderTexture()
            Log.d(TAG, "Encoder texture created: id=$encoderTextureId")

            initShader()
            Log.d(TAG, "Shader compiled")

            initBuffers()
            Log.d(TAG, "Buffers allocated")

            isStarted = true
            Log.i(TAG, "Encoder started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start", e)
            release()
            throw e
        }
    }

    /**
     * Encode a frame. The pixel data is provided via a ByteBuffer.
     * This method switches to the encoder's EGL context, uploads the pixels to a
     * texture, renders to MediaCodec's input surface, and drains encoded data.
     *
     * IMPORTANT: After this method returns, the game's EGL context has NOT been restored.
     * The caller MUST call [restoreGameContext] afterwards.
     */
    fun encodeFrame(pixelBuffer: ByteBuffer, timestampNs: Long) {
        if (!isStarted) return

        try {
            // Switch to encoder's context
            makeCurrent()

            // Upload pixels to encoder's own texture
            pixelBuffer.rewind()
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, encoderTextureId)
            GLES20.glTexSubImage2D(
                GLES20.GL_TEXTURE_2D, 0,
                0, 0, width, height,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer
            )
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

            // Render to MediaCodec's input surface
            renderToEncoderSurface()

            // Drain encoded data
            drainEncoder(false)

            frameCount++
            onFrameProgress?.invoke(frameCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding frame $frameCount", e)
            throw e
        }
    }

    /**
     * Make the encoder's EGL context current. Must be called before [encodeFrame]
     * if you want to upload/render in the encoder's context.
     */
    fun makeCurrent() {
        val display = eglDisplay ?: return
        val context = eglContext ?: return
        val surface = eglSurface ?: return

        if (!android.opengl.EGL14.eglMakeCurrent(display, surface, surface, context)) {
            val error = android.opengl.EGL14.eglGetError()
            Log.e(TAG, "eglMakeCurrent failed, error=0x${Integer.toHexString(error)}")
        }
    }

    /**
     * Restore the game's EGL context after encoding.
     */
    fun restoreGameContext(
        gameDisplay: android.opengl.EGLDisplay,
        gameDrawSurface: android.opengl.EGLSurface,
        gameReadSurface: android.opengl.EGLSurface,
        gameContext: android.opengl.EGLContext
    ) {
        if (!android.opengl.EGL14.eglMakeCurrent(gameDisplay, gameDrawSurface, gameReadSurface, gameContext)) {
            val error = android.opengl.EGL14.eglGetError()
            Log.e(TAG, "Failed to restore game context, error=0x${Integer.toHexString(error)}")
        }
    }

    fun stop() {
        if (!isStarted) {
            Log.w(TAG, "stop() called but not started")
            return
        }
        isStarted = false
        Log.i(TAG, "Stopping after $frameCount frames")

        var success = false
        try {
            makeCurrent()
            drainEncoder(true)
            Log.d(TAG, "EOS drained")

            if (isMuxerStarted) {
                mediaMuxer?.stop()
                Log.d(TAG, "Muxer stopped")
            }
            success = true
        } catch (e: Exception) {
            Log.e(TAG, "Error during stop", e)
            onError?.invoke(e)
        } finally {
            release()
            if (success) {
                onComplete?.invoke(outputFile)
            }
        }
    }

    fun release() {
        isStarted = false
        isMuxerStarted = false

        try { mediaCodec?.release() } catch (e: Exception) { Log.w(TAG, "Error releasing codec", e) }
        try { mediaMuxer?.release() } catch (e: Exception) { Log.w(TAG, "Error releasing muxer", e) }
        mediaCodec = null
        mediaMuxer = null

        releaseEGL()
        releaseShader()
    }

    // ── MediaCodec ─────────────────────────────────────────

    private fun initMediaCodec() {
        val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
        }

        mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE).also {
            it.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
    }

    private fun initMuxer() {
        mediaMuxer = MediaMuxer(
            outputFile.absolutePath,
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        )
    }

    // ── EGL (NOT shared with game) ─────────────────────────

    private fun initEGL(gameDisplay: android.opengl.EGLDisplay) {
        eglDisplay = gameDisplay

        val configAttribs = intArrayOf(
            android.opengl.EGL14.EGL_RENDERABLE_TYPE, android.opengl.EGL14.EGL_OPENGL_ES2_BIT,
            android.opengl.EGL14.EGL_RED_SIZE, 8,
            android.opengl.EGL14.EGL_GREEN_SIZE, 8,
            android.opengl.EGL14.EGL_BLUE_SIZE, 8,
            android.opengl.EGL14.EGL_ALPHA_SIZE, 8,
            android.opengl.EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
        val numConfigs = IntArray(1)
        android.opengl.EGL14.eglChooseConfig(gameDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)

        if (numConfigs[0] == 0 || configs[0] == null) {
            throw RuntimeException("No suitable EGL config for encoder")
        }
        val eglConfig = configs[0]!!

        // Create a NEW context — NOT shared with the game
        val contextAttribs = intArrayOf(
            android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            android.opengl.EGL14.EGL_NONE
        )
        // Share context = EGL_NO_CONTEXT → no texture sharing
        eglContext = android.opengl.EGL14.eglCreateContext(
            gameDisplay, eglConfig,
            android.opengl.EGL14.EGL_NO_CONTEXT,
            contextAttribs, 0
        )
        if (eglContext == null || eglContext == android.opengl.EGL14.EGL_NO_CONTEXT) {
            val error = android.opengl.EGL14.eglGetError()
            throw RuntimeException("Failed to create encoder EGL context, error=0x${Integer.toHexString(error)}")
        }

        // Create EGL surface from MediaCodec's input Surface
        val surfaceAttribs = intArrayOf(android.opengl.EGL14.EGL_NONE)
        eglSurface = android.opengl.EGL14.eglCreateWindowSurface(
            gameDisplay, eglConfig, inputSurface!!, surfaceAttribs, 0
        )
        if (eglSurface == null || eglSurface == android.opengl.EGL14.EGL_NO_SURFACE) {
            val error = android.opengl.EGL14.eglGetError()
            throw RuntimeException("Failed to create encoder EGL surface, error=0x${Integer.toHexString(error)}")
        }
    }

    private fun releaseEGL() {
        val display = eglDisplay ?: return

        if (eglSurface != null && eglSurface != android.opengl.EGL14.EGL_NO_SURFACE) {
            android.opengl.EGL14.eglDestroySurface(display, eglSurface!!)
            eglSurface = null
        }
        if (eglContext != null && eglContext != android.opengl.EGL14.EGL_NO_CONTEXT) {
            android.opengl.EGL14.eglDestroyContext(display, eglContext!!)
            eglContext = null
        }
        // Do NOT destroy the display — it belongs to the game
        eglDisplay = null
    }

    // ── Encoder's own texture ──────────────────────────────

    private fun initEncoderTexture() {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        encoderTextureId = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, encoderTextureId)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
            width, height, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    // ── Shader ─────────────────────────────────────────────

    private fun initShader() {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        shaderProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(shaderProgram, vertexShader)
        GLES20.glAttachShader(shaderProgram, fragmentShader)
        GLES20.glLinkProgram(shaderProgram)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(shaderProgram)
            GLES20.glDeleteProgram(shaderProgram)
            shaderProgram = 0
            throw RuntimeException("Shader link failed: $log")
        }

        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord")
        textureHandle = GLES20.glGetUniformLocation(shaderProgram, "uTexture")

        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader compile failed: $log")
        }
        return shader
    }

    private fun releaseShader() {
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram)
            shaderProgram = 0
        }
    }

    // ── Buffers ────────────────────────────────────────────

    private fun initBuffers() {
        vertexBuffer = ByteBuffer.allocateDirect(QUAD_VERTICES.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(QUAD_VERTICES); position(0) }

        texCoordBuffer = ByteBuffer.allocateDirect(QUAD_TEX_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(QUAD_TEX_COORDS); position(0) }
    }

    // ── Render ─────────────────────────────────────────────

    private fun renderToEncoderSurface() {
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(shaderProgram)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, encoderTextureId)
        GLES20.glUniform1i(textureHandle, 0)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)

        if (!android.opengl.EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
            val error = android.opengl.EGL14.eglGetError()
            Log.e(TAG, "eglSwapBuffers failed, error=0x${Integer.toHexString(error)}")
        }
    }

    // ── Encoder drain ──────────────────────────────────────

    private fun drainEncoder(endOfStream: Boolean) {
        val codec = mediaCodec ?: return

        if (endOfStream) {
            try {
                codec.signalEndOfInputStream()
            } catch (e: Exception) {
                Log.e(TAG, "signalEndOfInputStream failed", e)
            }
        }

        val bufferInfo = MediaCodec.BufferInfo()

        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)

            when {
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!isMuxerStarted) {
                        val format = codec.outputFormat
                        Log.d(TAG, "Output format: $format")
                        muxerTrackIndex = mediaMuxer!!.addTrack(format)
                        mediaMuxer!!.start()
                        isMuxerStarted = true
                        Log.d(TAG, "Muxer started, track=$muxerTrackIndex")
                    }
                }
                outputIndex >= 0 -> {
                    val outputBuffer = codec.getOutputBuffer(outputIndex) ?: continue

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size > 0 && isMuxerStarted) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        mediaMuxer?.writeSampleData(muxerTrackIndex, outputBuffer, bufferInfo)
                    }

                    codec.releaseOutputBuffer(outputIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        Log.d(TAG, "EOS received")
                        break
                    }
                }
                else -> break
            }
        }
    }

    val isEncoding: Boolean get() = isStarted
    val totalFrames: Long get() = frameCount
}
