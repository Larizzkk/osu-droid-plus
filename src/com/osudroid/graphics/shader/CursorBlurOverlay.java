package com.osudroid.graphics.shader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.MotionEvent;

import java.io.File;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.Config;

/**
 * Transparent GLES2 layer above the game surface.
 *
 * <p>The main AndEngine renderer is GLES1/fixed-function and cannot execute
 * fragment shaders in its own context. This overlay hosts a real GLES2 context
 * and draws only the cursor blur; everything else stays in the game surface
 * below it. The overlay never consumes touch input, so gameplay input still
 * reaches the engine unchanged.</p>
 *
 * <p>Cursor entities publish an immutable-ish snapshot through static arrays.
 * The GL thread copies those values once per frame and issues one draw call per
 * visible cursor. The fragment shader accumulates the cursor image at thirteen
 * positions across the danser-go shutter, using weights calculated from the
 * selected blending function (or manual weights). Those offsets follow the
 * cursor's recent path, not a single straight velocity vector, so arcs and fast
 * circular movement stay curved instead of forming a sawtooth. It is still a
 * shutter blur, not a trail: the window is only the exposure.</p>
 */
public final class CursorBlurOverlay extends GLSurfaceView {

    private static final String TAG = "CursorBlurOverlay";

    /** Enough slots for every simultaneous game cursor plus the menu cursor. */
    private static final int MAX_CURSORS = 16;
    /** Path samples over the exposure interval. More taps remove the visible
     *  gaps in fast flicks without changing the shutter length. Fallback taps
     *  are used when a GLES2 driver cannot fit this many fragment uniforms. */
    private static final int BLUR_SAMPLES = 13;
    private static final int FALLBACK_SAMPLES = 9;

    private static final Object STATE_LOCK = new Object();
    private static final boolean[] USED = new boolean[MAX_CURSORS];
    private static final boolean[] VISIBLE = new boolean[MAX_CURSORS];
    private static final float[] CENTER_X = new float[MAX_CURSORS];
    private static final float[] CENTER_Y = new float[MAX_CURSORS];
    private static final float[] WIDTH = new float[MAX_CURSORS];
    private static final float[] HEIGHT = new float[MAX_CURSORS];
    private static final float[] ROTATION = new float[MAX_CURSORS];
    private static final float[][] OFFSET_X = new float[MAX_CURSORS][BLUR_SAMPLES];
    private static final float[][] OFFSET_Y = new float[MAX_CURSORS][BLUR_SAMPLES];
    private static final float[] RADIUS = new float[MAX_CURSORS];
    private static final float[] ALPHA = new float[MAX_CURSORS];
    private static final float[] WEIGHTS = new float[BLUR_SAMPLES];

    static {
        for (int i = 0; i < BLUR_SAMPLES; i++) {
            WEIGHTS[i] = 1f;
        }
    }

    private static volatile CursorBlurOverlay instance;
    private static volatile int textureGeneration;
    private static volatile String skinPath;

    private final Renderer renderer;

    public CursorBlurOverlay(Context context) {
        super(context);

        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        setPreserveEGLContextOnPause(true);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        // Above other SurfaceViews, but below regular window UI. This keeps the
        // overlay under menus/dialogs while still being above the game surface.
        setZOrderMediaOverlay(true);
        setFocusable(false);
        setClickable(false);

        skinPath = Config.getSkinPath();
        renderer = new Renderer(context);
        setRenderer(renderer);
        // The blur is published from the game draw pass; render on demand so the
        // overlay never intentionally displays a snapshot older than the current
        // game frame while idle.
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        instance = this;
    }

    private static void requestOverlayRender() {
        CursorBlurOverlay overlay = instance;
        if (overlay != null) {
            overlay.requestRender();
        }
    }

    /**
     * Reserves a slot for one cursor entity. Slots are reused after a cursor
     * releases them: releaseCursor() clears the stored path first, so a
     * recycled slot cannot draw at the previous cursor's position.
     */
    public static int registerCursor() {
        synchronized (STATE_LOCK) {
            for (int i = 0; i < MAX_CURSORS; i++) {
                if (!USED[i]) {
                    USED[i] = true;
                    clearLocked(i);
                    return i;
                }
            }
        }
        return -1;
    }

    public static void releaseCursor(int slot) {
        if (slot < 0 || slot >= MAX_CURSORS) {
            return;
        }
        synchronized (STATE_LOCK) {
            clearLocked(slot);
            USED[slot] = false;
        }
    }

    public static void clear(int slot) {
        if (slot < 0 || slot >= MAX_CURSORS) {
            return;
        }
        synchronized (STATE_LOCK) {
            clearLocked(slot);
        }
        requestOverlayRender();
    }

    /**
     * Publishes the cursor image and a short screen-space path for the current
     * exposure. Offsets are relative to the current cursor centre and follow
     * the actual recent path, so circular/arc movement is blurred as an arc
     * instead of a series of straight chords (the sawtooth artefact).
     */
    public static void submitPath(
        int slot,
        float centerX,
        float centerY,
        float width,
        float height,
        float rotationDegrees,
        float[] offsetX,
        float[] offsetY,
        float radiusPx,
        float alpha
    ) {
        if (slot < 0 || slot >= MAX_CURSORS) {
            return;
        }

        synchronized (STATE_LOCK) {
            CENTER_X[slot] = centerX;
            CENTER_Y[slot] = centerY;
            WIDTH[slot] = width;
            HEIGHT[slot] = height;
            ROTATION[slot] = rotationDegrees;
            for (int i = 0; i < BLUR_SAMPLES; i++) {
                OFFSET_X[slot][i] = offsetX[i];
                OFFSET_Y[slot][i] = offsetY[i];
            }
            RADIUS[slot] = radiusPx;
            ALPHA[slot] = alpha;
            VISIBLE[slot] = width > 0f && height > 0f && radiusPx > 0.01f && alpha > 0.001f;
        }
        requestOverlayRender();
    }

    /**
     * Sets the danser-go blend weights for all cursors. They are global because
     * every cursor reads the same motion-blur preferences.
     */
    public static void setWeights(float[] weights) {
        if (weights == null || weights.length != BLUR_SAMPLES) {
            return;
        }
        synchronized (STATE_LOCK) {
            System.arraycopy(weights, 0, WEIGHTS, 0, BLUR_SAMPLES);
        }
        requestOverlayRender();
    }

    /** Called after a skin change so the GL thread reloads cursor.png. */
    public static void setSkinPath(String path) {
        if (path == null) {
            return;
        }
        if (!path.endsWith("/") && !path.endsWith("\\")) {
            path += "/";
        }
        if (!path.equals(skinPath)) {
            skinPath = path;
            textureGeneration++;
            requestOverlayRender();
        }
    }

    private static void clearLocked(int slot) {
        VISIBLE[slot] = false;
        ALPHA[slot] = 0f;
        RADIUS[slot] = 0f;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Let the sibling game surface handle the event.
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    private static final class Renderer implements GLSurfaceView.Renderer {

        private static final String VERTEX_SHADER =
            "attribute vec2 aPosition;\n" +
            "uniform vec2 uCenter;\n" +
            "uniform vec2 uHalfSize;\n" +
            "uniform vec2 uImageSize;\n" +
            "uniform float uRotation;\n" +
            "uniform vec2 uScreenSize;\n" +
            "varying vec2 vUv;\n" +
            "void main() {\n" +
            "    vec2 local = aPosition * uHalfSize;\n" +
            "    float c = cos(uRotation);\n" +
            "    float s = sin(uRotation);\n" +
            "    vec2 rotated = vec2(local.x * c - local.y * s,\n" +
            "                       local.x * s + local.y * c);\n" +
            "    vec2 screen = uCenter + rotated;\n" +
            "    gl_Position = vec4(screen.x / uScreenSize.x * 2.0 - 1.0,\n" +
            "                       1.0 - screen.y / uScreenSize.y * 2.0,\n" +
            "                       0.0, 1.0);\n" +
            "    vUv = vec2(local.x / uImageSize.x + 0.5,\n" +
            "               local.y / uImageSize.y + 0.5);\n" +
            "}\n";

        /**
         * Gaussian convolution implemented in the fragment shader. The weight
         * is normalised by the summed alpha so the cursor colour does not fade
         * towards black at transparent texture edges.
         */
        private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform vec2 uOffsets[13];\n" +
            "uniform float uWeights[13];\n" +
            "uniform float uAlpha;\n" +
            "uniform float uRotation;\n" +
            "uniform vec2 uImageSize;\n" +
            "varying vec2 vUv;\n" +
            "void main() {\n" +
            "    vec3 colorSum = vec3(0.0);\n" +
            "    float alphaSum = 0.0;\n" +
            "    float weightSum = 0.0;\n" +
            "    float c = cos(uRotation);\n" +
            "    float s = sin(uRotation);\n" +
            "    for (int i = 0; i < 13; i++) {\n" +
            // danser-go calculates 1 + 100 * easing(t) on the CPU; the exact
            // curve (and optional manual weights) arrives in this array.
            "        float w = uWeights[i];\n" +
            "        vec2 offset = uOffsets[i];\n" +
            // The quad is rotated, so a screen-space offset has to be moved back
            // into the cursor's local UV axes before sampling.
            "        vec2 localOffset = vec2(offset.x * c + offset.y * s,\n" +
            "                                -offset.x * s + offset.y * c);\n" +
            "        vec2 uv = vUv + vec2(localOffset.x / uImageSize.x,\n" +
            "                             localOffset.y / uImageSize.y);\n" +
            "        float inside = step(0.0, uv.x) * step(uv.x, 1.0) *\n" +
            "                       step(0.0, uv.y) * step(uv.y, 1.0);\n" +
            "        vec4 sampleColor = texture2D(uTexture, uv) * inside;\n" +
            "        colorSum += sampleColor.rgb * sampleColor.a * w;\n" +
            "        alphaSum += sampleColor.a * w;\n" +
            "        weightSum += w;\n" +
            "    }\n" +
            "    float alpha = (alphaSum / weightSum) * uAlpha;\n" +
            "    if (alpha <= 0.001) discard;\n" +
            "    gl_FragColor = vec4(colorSum / weightSum, alpha);\n" +
            "}\n";

        /** Kept for GLES2 devices whose fragment uniform budget is too small. */
        private static final String FRAGMENT_SHADER_FALLBACK = FRAGMENT_SHADER
            .replace("[13]", "[" + FALLBACK_SAMPLES + "]")
            .replace("i < 13", "i < " + FALLBACK_SAMPLES);

        private final Context context;

        private ShaderProgram program;
        private int activeSamples = BLUR_SAMPLES;
        private ShaderQuad quad;
        private int textureId;
        private int loadedGeneration = -1;
        private int surfaceWidth;
        private int surfaceHeight;

        // Snapshot copied from the synchronized static state.
        private final boolean[] visible = new boolean[MAX_CURSORS];
        private final float[] centerX = new float[MAX_CURSORS];
        private final float[] centerY = new float[MAX_CURSORS];
        private final float[] width = new float[MAX_CURSORS];
        private final float[] height = new float[MAX_CURSORS];
        private final float[] rotation = new float[MAX_CURSORS];
        private final float[][] offsetX = new float[MAX_CURSORS][BLUR_SAMPLES];
        private final float[][] offsetY = new float[MAX_CURSORS][BLUR_SAMPLES];
        private final float[] radius = new float[MAX_CURSORS];
        private final float[] alpha = new float[MAX_CURSORS];
        private final float[] weights = new float[BLUR_SAMPLES];
        private final java.nio.FloatBuffer offsetBuffer = java.nio.ByteBuffer
            .allocateDirect(BLUR_SAMPLES * 2 * Float.BYTES)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer();
        private final java.nio.FloatBuffer weightBuffer = java.nio.ByteBuffer
            .allocateDirect(BLUR_SAMPLES * Float.BYTES)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer();

        Renderer(Context context) {
            this.context = context;
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            GLES20.glDisable(GLES20.GL_CULL_FACE);
            GLES20.glEnable(GLES20.GL_BLEND);
            // The surface is composited by SurfaceFlinger as premultiplied alpha.
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glClearColor(0f, 0f, 0f, 0f);

            program = ShaderProgram.create(VERTEX_SHADER, FRAGMENT_SHADER);
            activeSamples = BLUR_SAMPLES;
            if (program == null) {
                Log.w(TAG, "13-tap blur shader unavailable; falling back to 9 taps");
                program = ShaderProgram.create(VERTEX_SHADER, FRAGMENT_SHADER_FALLBACK);
                activeSamples = FALLBACK_SAMPLES;
            }
            quad = new ShaderQuad();
            textureId = 0;
            loadedGeneration = -1;

            if (program == null) {
                Log.e(TAG, "Shader program could not be created; cursor blur disabled");
            }
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            surfaceWidth = width;
            surfaceHeight = height;
            GLES20.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            if (program == null || surfaceWidth <= 0 || surfaceHeight <= 0) {
                return;
            }

            if (loadedGeneration != textureGeneration) {
                loadTexture();
                loadedGeneration = textureGeneration;
            }
            if (textureId == 0) {
                return;
            }

            copyState();

            program.use();
            program.setUniform1i("uTexture", 0);
            program.setUniform2f("uScreenSize", surfaceWidth, surfaceHeight);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

            for (int i = 0; i < MAX_CURSORS; i++) {
                if (!visible[i] || alpha[i] <= 0.002f) {
                    continue;
                }

                float halfWidth = width[i] * 0.5f + radius[i] + 2f;
                float halfHeight = height[i] * 0.5f + radius[i] + 2f;

                program.setUniform2f("uCenter", centerX[i], centerY[i]);
                program.setUniform2f("uHalfSize", halfWidth, halfHeight);
                program.setUniform2f("uImageSize", width[i], height[i]);
                program.setUniform1f("uRotation", (float) Math.toRadians(rotation[i]));
                program.setUniform1f("uAlpha", alpha[i]);

                offsetBuffer.clear();
                for (int j = 0; j < BLUR_SAMPLES; j++) {
                    offsetBuffer.put(offsetX[i][j]);
                    offsetBuffer.put(offsetY[i][j]);
                }
                offsetBuffer.position(0);
                program.setUniform2fv("uOffsets[0]", activeSamples, offsetBuffer);

                weightBuffer.clear();
                weightBuffer.put(weights, 0, BLUR_SAMPLES);
                weightBuffer.position(0);
                program.setUniform1fv("uWeights[0]", activeSamples, weightBuffer);

                quad.draw(program.getPositionAttribute());
            }

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }

        private void copyState() {
            synchronized (STATE_LOCK) {
                System.arraycopy(WEIGHTS, 0, weights, 0, BLUR_SAMPLES);
                for (int i = 0; i < MAX_CURSORS; i++) {
                    visible[i] = VISIBLE[i];
                    centerX[i] = CENTER_X[i];
                    centerY[i] = CENTER_Y[i];
                    width[i] = WIDTH[i];
                    height[i] = HEIGHT[i];
                    rotation[i] = ROTATION[i];
                    for (int j = 0; j < BLUR_SAMPLES; j++) {
                        offsetX[i][j] = OFFSET_X[i][j];
                        offsetY[i][j] = OFFSET_Y[i][j];
                    }
                    radius[i] = RADIUS[i];
                    alpha[i] = ALPHA[i];
                }
            }
        }

        private void loadTexture() {
            Bitmap bitmap = loadCursorBitmap();
            if (bitmap == null) {
                Log.e(TAG, "Cursor bitmap not found");
                return;
            }

            int[] ids = new int[1];
            GLES20.glGenTextures(1, ids, 0);
            if (ids[0] == 0) {
                bitmap.recycle();
                return;
            }

            if (textureId != 0) {
                GLES20.glDeleteTextures(1, new int[] { textureId }, 0);
            }
            textureId = ids[0];

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            );
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            );
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE
            );
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE
            );
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();

            int error = GLES20.glGetError();
            if (error != GLES20.GL_NO_ERROR) {
                Log.e(TAG, "Cursor texture upload failed: 0x" + Integer.toHexString(error));
                GLES20.glDeleteTextures(1, new int[] { textureId }, 0);
                textureId = 0;
            }
        }

        private Bitmap loadCursorBitmap() {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            // Keep straight alpha so the shader can weight rgb by alpha without
            // double-premultiplying Android's default Bitmap representation.
            options.inPremultiplied = false;

            String path = skinPath;
            if (path != null) {
                File file = new File(path, "cursor.png");
                if (file.isFile()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), options);
                    if (bitmap != null) {
                        return bitmap;
                    }
                }

                File hd = new File(path, "cursor@2x.png");
                if (hd.isFile()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(hd.getPath(), options);
                    if (bitmap != null) {
                        return bitmap;
                    }
                }
            }

            try {
                return BitmapFactory.decodeStream(
                    context.getAssets().open("gfx/cursor.png"),
                    null,
                    options
                );
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}
