package com.osudroid.game.replay.video

import android.opengl.GLES20
import android.opengl.EGL14
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.opengles.GL10

/**
 * Manages the lifecycle of exporting a replay to MP4 video.
 *
 * Flow:
 *   1. [startExport] saves parameters but does NOT create the encoder yet
 *   2. On the first [onGameFrame], detects the actual framebuffer size and creates the encoder
 *   3. Subsequent [onGameFrame] calls capture, upload, and encode frames
 *   4. [stopExport] signals EOS and finalizes the MP4
 *
 * Thread: All methods must be called on the GL thread.
 *
 * CRITICAL: The encoder's EGL context must NEVER be left current after any method returns.
 * Every method that touches the encoder must save/restore the game's EGL context.
 */
class VideoExportManager {

    companion object {
        private const val TAG = "VideoExportManager"
        const val TARGET_FPS = 60
        const val BITRATE_MBPS = 8

        @Volatile
        private var instance: VideoExportManager? = null

        @JvmStatic
        fun getInstance(): VideoExportManager {
            return instance ?: synchronized(this) {
                instance ?: VideoExportManager().also { instance = it }
            }
        }
    }

    @Volatile var isExporting = false; private set
    @Volatile var exportProgress: Float = 0f; private set
    @Volatile var exportState: ExportState = ExportState.IDLE; private set

    enum class ExportState { IDLE, INITIALIZING, EXPORTING, FINALIZING, COMPLETED, ERROR }

    private var encoder: ReplayVideoEncoder? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var pixelBuffer: ByteBuffer? = null

    private var fixedAccumulator = 0.0
    private var lastFrameTimeNs = 0L
    private val fixedDt = 1.0 / TARGET_FPS

    private var totalReplayDurationSec: Float = 0f
    private var currentReplayTimeSec: Float = 0f
    private var frameCount: Long = 0

    private var actualWidth = 0
    private var actualHeight = 0

    var onProgressChanged: ((progress: Float, frame: Long) -> Unit)? = null
    var onExportComplete: ((file: File) -> Unit)? = null
    var onExportError: ((error: String) -> Unit)? = null

    private var pendingOutputPath: String = ""

    // ── Helpers ────────────────────────────────────────────

    /** Save the current EGL state. */
    private data class EglState(
        val display: android.opengl.EGLDisplay,
        val drawSurface: android.opengl.EGLSurface,
        val readSurface: android.opengl.EGLSurface,
        val context: android.opengl.EGLContext
    ) {
        fun restore() {
            EGL14.eglMakeCurrent(display, drawSurface, readSurface, context)
        }
    }

    private fun saveGameEgl(): EglState? {
        val d = EGL14.eglGetCurrentDisplay() ?: return null
        val ds = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW) ?: return null
        val rs = EGL14.eglGetCurrentSurface(EGL14.EGL_READ) ?: return null
        val c = EGL14.eglGetCurrentContext() ?: return null
        if (d == EGL14.EGL_NO_DISPLAY || c == EGL14.EGL_NO_CONTEXT) return null
        return EglState(d, ds, rs, c)
    }

    // ── Export lifecycle ───────────────────────────────────

    fun startExport(outputPath: String, replayDurationSec: Float) {
        if (isExporting) {
            Log.w(TAG, "Export already in progress")
            return
        }

        Log.i(TAG, "startExport: output=$outputPath, duration=${replayDurationSec}s")

        pendingOutputPath = outputPath
        totalReplayDurationSec = replayDurationSec
        currentReplayTimeSec = 0f
        fixedAccumulator = 0.0
        lastFrameTimeNs = System.nanoTime()
        frameCount = 0
        actualWidth = 0
        actualHeight = 0

        exportState = ExportState.INITIALIZING
        isExporting = true

        Log.i(TAG, "Export armed — encoder created on first gameplay frame")
    }

    fun onGameFrame(gl: GL10, gameFrameTimeNs: Long): Boolean {
        if (!isExporting) return false

        // Lazy-create encoder on first frame
        if (encoder == null) {
            if (!initializeEncoder()) {
                Log.e(TAG, "Failed to initialize encoder on first frame")
                isExporting = false
                exportState = ExportState.ERROR
                return false
            }
        }

        val now = System.nanoTime()
        val elapsed = (now - lastFrameTimeNs) / 1_000_000_000.0
        lastFrameTimeNs = now

        fixedAccumulator += elapsed

        var frameEncoded = false

        while (fixedAccumulator >= fixedDt) {
            fixedAccumulator -= fixedDt
            currentReplayTimeSec += fixedDt.toFloat()

            try {
                encodeCurrentFrame()
                frameCount++
            } catch (e: Exception) {
                Log.e(TAG, "Error encoding frame, stopping", e)
                stopExport()
                return true
            }

            frameEncoded = true

            if (totalReplayDurationSec > 0 && currentReplayTimeSec >= totalReplayDurationSec) {
                Log.i(TAG, "Duration reached, stopping at frame $frameCount")
                stopExport()
                return true
            }
        }

        return frameEncoded
    }

    /**
     * Create the encoder with actual framebuffer dimensions.
     * CRITICAL: Saves and restores game EGL context around encoder creation.
     */
    private fun initializeEncoder(): Boolean {
        // Save game context BEFORE anything touches the encoder
        val gameEgl = saveGameEgl()
        if (gameEgl == null) {
            Log.e(TAG, "No valid game EGL context")
            return false
        }

        try {
            // Detect actual framebuffer dimensions
            val viewport = IntArray(4)
            GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, viewport, 0)
            actualWidth = viewport[2]
            actualHeight = viewport[3]

            if (actualWidth <= 0 || actualHeight <= 0) {
                Log.e(TAG, "Invalid viewport: ${actualWidth}x${actualHeight}")
                return false
            }

            Log.i(TAG, "Detected viewport: ${actualWidth}x${actualHeight}")

            val bufSize = actualWidth * actualHeight * 4
            pixelBuffer = ByteBuffer.allocateDirect(bufSize).order(ByteOrder.nativeOrder())

            encoder = ReplayVideoEncoder(
                width = actualWidth,
                height = actualHeight,
                fps = TARGET_FPS,
                bitrate = BITRATE_MBPS * 1_000_000,
                outputFile = File(pendingOutputPath)
            ).apply {
                onFrameProgress = { frame ->
                    val progress = if (totalReplayDurationSec > 0) {
                        (currentReplayTimeSec / totalReplayDurationSec).coerceIn(0f, 1f)
                    } else 0f
                    exportProgress = progress
                    onProgressChanged?.invoke(progress, frame)
                }
                onComplete = { file ->
                    exportState = ExportState.COMPLETED
                    isExporting = false
                    Log.i(TAG, "Export completed: ${file.absolutePath}, ${file.length()} bytes, $frameCount frames")
                    mainHandler.post {
                        onExportComplete?.invoke(file) ?: run {
                            try {
                                android.widget.Toast.makeText(
                                    ru.nsu.ccfit.zuev.osuplusplus.GlobalManager.getInstance().mainActivity,
                                    "Video saved: ${file.name}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } catch (_: Exception) {}
                        }
                    }
                }
                onError = { e ->
                    exportState = ExportState.ERROR
                    isExporting = false
                    Log.e(TAG, "Encoder error", e)
                    mainHandler.post {
                        onExportError?.invoke(e.message ?: "Unknown error") ?: run {
                            try {
                                android.widget.Toast.makeText(
                                    ru.nsu.ccfit.zuev.osuplusplus.GlobalManager.getInstance().mainActivity,
                                    "Export failed: ${e.message}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } catch (_: Exception) {}
                        }
                    }
                }
                // start() calls makeCurrent() which steals the game context
                start(gameEgl.display)
            }

            // CRITICAL: Restore game context after encoder creation
            gameEgl.restore()

            exportState = ExportState.EXPORTING
            Log.i(TAG, "Encoder created: ${actualWidth}x${actualHeight}")
            return true

        } catch (e: Exception) {
            // Restore game context even on failure
            try { gameEgl.restore() } catch (_: Exception) {}
            Log.e(TAG, "Failed to create encoder", e)
            exportState = ExportState.ERROR
            isExporting = false
            release()
            mainHandler.post {
                onExportError?.invoke(e.message ?: "Failed to create encoder")
            }
            return false
        }
    }

    fun stopExport() {
        if (!isExporting && exportState != ExportState.EXPORTING) return

        exportState = ExportState.FINALIZING
        isExporting = false

        // Save game context before encoder.stop() steals it
        val gameEgl = saveGameEgl()

        try {
            encoder?.stop()
        } catch (e: Exception) {
            exportState = ExportState.ERROR
            Log.e(TAG, "Error stopping export", e)
            mainHandler.post { onExportError?.invoke(e.message ?: "Failed to finalize") }
        } finally {
            // Restore game context
            gameEgl?.restore()
            encoder = null
            pixelBuffer = null
        }
    }

    /**
     * Force-stop the export immediately without finalizing the MP4.
     * Used when the game scene is being torn down.
     */
    fun forceStop() {
        Log.w(TAG, "Force-stopping export")
        isExporting = false
        exportState = ExportState.ERROR
        release()
    }

    fun release() {
        try { encoder?.release() } catch (e: Exception) { Log.w(TAG, "Error releasing encoder", e) }
        encoder = null
        pixelBuffer = null
    }

    // ── Frame capture ──────────────────────────────────────

    private fun encodeCurrentFrame() {
        val encoder = encoder ?: return
        val buf = pixelBuffer ?: return

        // Game context is current here — read pixels
        buf.rewind()
        GLES20.glReadPixels(
            0, 0, actualWidth, actualHeight,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf
        )
        buf.rewind()

        // Save game EGL state before encoder switches context
        val gameEgl = saveGameEgl() ?: return

        try {
            encoder.encodeFrame(buf, System.nanoTime())
        } finally {
            // ALWAYS restore game's EGL context
            gameEgl.restore()
        }
    }
}
