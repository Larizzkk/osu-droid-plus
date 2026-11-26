package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import com.osudroid.graphics.shader.CursorBlurOverlay;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.Entity;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.GlobalManager;

/**
 * Tracks the cursor's recent path and publishes it to {@link CursorBlurOverlay}.
 *
 * <p>The shutter, blend weights and weight curve are read from the same
 * parameters danser-go exposes: BlendFrames, OversampleMultiplier,
 * BlendFunctionID and GaussWeightsMult. The shader receives 24 positions
 * across that shutter and accumulates the cursor image with the calculated
 * weights. Sampling the real recent path keeps circular/arc movement smooth
 * instead of producing the old straight-chord sawtooth.</p>
 *
 * <p>The tracker is attached after the cursor sprite, so it reads the final
 * cursor position in the same draw pass as the visible cursor.</p>
 */
public final class CursorMotionBlur extends Entity {

    /** Path samples over the exposure. Must match CursorBlurOverlay.BLUR_SAMPLES. */
    private static final int BLUR_SAMPLES = 24;
    private static final int HISTORY_CAPACITY = 512;

    /** Safety cap only for pathological flicks; normal blur is physical. */
    private static final float MAX_BLUR_PX = 512f;
    private final CursorEntity owner;
    private final int slot;

    private MotionBlurSettings settings;

    private boolean hasLastPosition;
    private long lastTimeNanos;

    // Geometry uses this frame's velocity, not a smoothed one: on a fast circle
    // the smoothed direction lags the tangent and bends the forward blur.
    private float pathVelocityX;
    private float pathVelocityY;

    // Ring buffer of recent scene-space positions. head = newest entry.
    private final float[] historyX = new float[HISTORY_CAPACITY];
    private final float[] historyY = new float[HISTORY_CAPACITY];
    private final long[] historyTime = new long[HISTORY_CAPACITY];
    private int historyHead;
    private int historyCount;

    // Reused per-frame snapshot, no allocations in the draw path.
    private final float[] pathOffsetX = new float[BLUR_SAMPLES];
    private final float[] pathOffsetY = new float[BLUR_SAMPLES];
    private float pastSampleX;
    private float pastSampleY;

    public CursorMotionBlur(final CursorEntity owner) {
        this.owner = owner;
        this.slot = CursorBlurOverlay.registerCursor();
        setIgnoreUpdate(true);
        reloadSettings();
    }

    /**
     * Re-reads all danser-go motion blur preferences. Called once per second by
     * the cursor, so settings apply without reloading a beatmap.
     */
    public void reloadSettings() {
        MotionBlurSettings next = MotionBlurSettings.load();
        if (settings != null && next.sameAs(settings)) {
            return;
        }

        settings = next;
        CursorBlurOverlay.setWeights(next.weights);
        reset();
    }

    /** Releases the overlay slot when the cursor entity is discarded. */
    public void release() {
        showSharpCursor();
        CursorBlurOverlay.releaseCursor(slot);
    }

    /** Discards history so the next frame cannot blur across a tap/seek. */
    public void reset() {
        hasLastPosition = false;
        pathVelocityX = 0f;
        pathVelocityY = 0f;
        historyHead = 0;
        historyCount = 0;
        CursorBlurOverlay.clear(slot);
        showSharpCursor();
    }

    private void showSharpCursor() {
        owner.setMotionBlurHidesCursor(false);
    }

    private void hideSharpCursor() {
        owner.setMotionBlurHidesCursor(true);
    }

    @Override
    protected void doDraw(final GL10 pGL, final Camera pCamera) {
        final CursorSprite cursor = owner.getCursorSprite();
        if (
            cursor == null ||
            !owner.isShowing() ||
            settings == null ||
            !settings.enabled ||
            slot < 0
        ) {
            CursorBlurOverlay.clear(slot);
            showSharpCursor();
            hasLastPosition = false;
            historyCount = 0;
            return;
        }

        final long now = System.nanoTime();
        final float x = owner.getX();
        final float y = owner.getY();

        if (!hasLastPosition || lastTimeNanos == 0L) {
            lastTimeNanos = now;
            hasLastPosition = true;
            historyHead = 0;
            historyCount = 0;
            appendHistory(x, y, now);
            CursorBlurOverlay.clear(slot);
            showSharpCursor();
            return;
        }

        final float dt = (now - lastTimeNanos) * 1.0e-9f;

        // Frame hitch: restart sampling instead of a huge smear. A large cursor
        // jump is not a reason to reset: at 30 FPS a fast flick can move more
        // than 320 px per frame, and clearing the blur there made the effect
        // depend on the frame rate. Presses and seeks reset the tracker
        // explicitly through reset().
        if (dt > 0.15f) {
            lastTimeNanos = now;
            pathVelocityX = 0f;
            pathVelocityY = 0f;
            historyHead = 0;
            historyCount = 0;
            appendHistory(x, y, now);
            CursorBlurOverlay.clear(slot);
            showSharpCursor();
            return;
        }

        if (dt < 0.001f) {
            return;
        }

        lastTimeNanos = now;
        appendHistory(x, y, now);
        trimHistory(now);

        // Use a fixed time window instead of this frame's displacement. At high
        // refresh rates a cursor often repeats a position for a frame; dividing
        // that zero movement by a short dt made the forward half of the blur
        // flicker. A fixed window gives the same average velocity at 30, 60 and
        // 120 FPS, matching the shutter better.
        updatePathVelocity(x, y, now);

        final float cursorWidth = cursor.getWidth() * Math.abs(cursor.getScaleX());
        final float cursorHeight = cursor.getHeight() * Math.abs(cursor.getScaleY());
        if (cursorWidth <= 0f || cursorHeight <= 0f) {
            CursorBlurOverlay.clear(slot);
            showSharpCursor();
            return;
        }

        // Sample the actual path across the shutter. Past positions come from
        // real history; the future half uses the fixed-window velocity.
        final long halfExposure = settings.exposureNanos / 2L;
        for (int i = 0; i < BLUR_SAMPLES; i++) {
            final long targetTime = now - halfExposure
                + (settings.exposureNanos * i) / (BLUR_SAMPLES - 1);
            samplePath(targetTime, x, y, i);
        }

        final Camera camera = GlobalManager.getInstance().getCamera();
        final var engine = GlobalManager.getInstance().getEngine();
        if (camera == null || engine == null) {
            CursorBlurOverlay.clear(slot);
            showSharpCursor();
            return;
        }

        final int surfaceWidth = engine.getSurfaceWidth();
        final int surfaceHeight = engine.getSurfaceHeight();
        final float cameraWidth = camera.getWidth();
        final float cameraHeight = camera.getHeight();
        if (surfaceWidth <= 0 || surfaceHeight <= 0 || cameraWidth <= 0f || cameraHeight <= 0f) {
            CursorBlurOverlay.clear(slot);
            showSharpCursor();
            return;
        }

        final float scaleX = surfaceWidth / cameraWidth;
        final float scaleY = surfaceHeight / cameraHeight;

        float nx = (x - camera.getMinX()) / cameraWidth;
        float ny = (y - camera.getMinY()) / cameraHeight;
        float screenRotation = cursor.getRotation();

        final boolean flipped = Math.abs(camera.getRotation() % 360f) > 1f;
        if (flipped) {
            nx = 1f - nx;
            ny = 1f - ny;
            screenRotation += 180f;
        }

        float radius = 0f;
        final float maxRadius = Math.min(
            MAX_BLUR_PX,
            Math.min(surfaceWidth, surfaceHeight) * 0.5f
        );
        for (int i = 0; i < BLUR_SAMPLES; i++) {
            float ox = pathOffsetX[i] * scaleX;
            float oy = pathOffsetY[i] * scaleY;
            if (flipped) {
                ox = -ox;
                oy = -oy;
            }

            final float length = (float) Math.sqrt(ox * ox + oy * oy);
            if (length > maxRadius) {
                final float factor = maxRadius / length;
                ox *= factor;
                oy *= factor;
            }

            pathOffsetX[i] = ox;
            pathOffsetY[i] = oy;
            radius = Math.max(radius, (float) Math.sqrt(ox * ox + oy * oy));
        }

        if (radius < 0.35f || !CursorBlurOverlay.isOperational()) {
            CursorBlurOverlay.clear(slot);
            showSharpCursor();
            return;
        }

        CursorBlurOverlay.submitPath(
            slot,
            nx * surfaceWidth,
            ny * surfaceHeight,
            cursorWidth * scaleX,
            cursorHeight * scaleY,
            screenRotation,
            pathOffsetX,
            pathOffsetY,
            radius,
            1f
        );
        hideSharpCursor();
    }

    /**
     * Average velocity over the exposure window. The position at the start of
     * the window is interpolated from the recorded path, so a frame that did
     * not move still contributes the recent movement instead of zero. This is
     * the live equivalent of danser sampling the cursor at fixed times inside
     * its oversampled shutter.
     */
    private void updatePathVelocity(float x, float y, long now) {
        final long window = settings.exposureNanos;
        if (historyCount < 2 || window <= 0L) {
            pathVelocityX = 0f;
            pathVelocityY = 0f;
            return;
        }

        samplePast(now - window, x, y);
        final float seconds = window * 1.0e-9f;
        pathVelocityX = (x - pastSampleX) / seconds;
        pathVelocityY = (y - pastSampleY) / seconds;
    }

    /** Interpolates the recorded path at an arbitrary past time. */
    private void samplePast(long targetTime, float currentX, float currentY) {
        final int newest = historyHead;
        final long newestTime = historyTime[newest];

        if (targetTime >= newestTime) {
            pastSampleX = currentX;
            pastSampleY = currentY;
            return;
        }

        final int oldest = (historyHead - historyCount + 1 + HISTORY_CAPACITY)
            % HISTORY_CAPACITY;
        if (targetTime <= historyTime[oldest]) {
            final float dt = (targetTime - historyTime[oldest]) * 1.0e-9f;
            pastSampleX = historyX[oldest] + pathVelocityX * dt;
            pastSampleY = historyY[oldest] + pathVelocityY * dt;
            return;
        }

        pastSampleX = currentX;
        pastSampleY = currentY;
        int newer = newest;
        for (int k = 1; k < historyCount; k++) {
            final int older = (historyHead - k + HISTORY_CAPACITY) % HISTORY_CAPACITY;
            if (historyTime[older] <= targetTime) {
                final long span = historyTime[newer] - historyTime[older];
                final float t = span > 0L
                    ? (targetTime - historyTime[older]) / (float) span
                    : 1f;
                pastSampleX = historyX[older]
                    + (historyX[newer] - historyX[older]) * t;
                pastSampleY = historyY[older]
                    + (historyY[newer] - historyY[older]) * t;
                return;
            }
            newer = older;
        }
    }

    private void appendHistory(float x, float y, long time) {
        if (historyCount == 0) {
            historyHead = 0;
        } else {
            historyHead = (historyHead + 1) % HISTORY_CAPACITY;
        }

        historyX[historyHead] = x;
        historyY[historyHead] = y;
        historyTime[historyHead] = time;

        if (historyCount < HISTORY_CAPACITY) {
            historyCount++;
        }
    }

    private void trimHistory(long now) {
        while (historyCount > 1) {
            final int oldest = (historyHead - historyCount + 1 + HISTORY_CAPACITY)
                % HISTORY_CAPACITY;
            if (historyTime[oldest] >= now - settings.historyWindowNanos) {
                break;
            }
            historyCount--;
        }
    }

    /**
     * Writes the scene-space offset from the current cursor to the sampled
     * position at {@code targetTime}. Between frames the path is interpolated;
     * outside the available history it is extrapolated with the current frame's
     * velocity.
     */
    private void samplePath(long targetTime, float currentX, float currentY, int index) {
        float sampleX;
        float sampleY;

        if (historyCount == 0) {
            sampleX = currentX;
            sampleY = currentY;
        } else {
            final int newest = historyHead;
            final long newestTime = historyTime[newest];

            if (targetTime >= newestTime) {
                final float dt = (targetTime - newestTime) * 1.0e-9f;
                sampleX = historyX[newest] + pathVelocityX * dt;
                sampleY = historyY[newest] + pathVelocityY * dt;
            } else {
                final int oldest = (historyHead - historyCount + 1 + HISTORY_CAPACITY)
                    % HISTORY_CAPACITY;
                if (targetTime <= historyTime[oldest]) {
                    final float dt = (targetTime - historyTime[oldest]) * 1.0e-9f;
                    sampleX = historyX[oldest] + pathVelocityX * dt;
                    sampleY = historyY[oldest] + pathVelocityY * dt;
                } else {
                    sampleX = currentX;
                    sampleY = currentY;
                    int newer = newest;
                    for (int k = 1; k < historyCount; k++) {
                        final int older = (historyHead - k + HISTORY_CAPACITY)
                            % HISTORY_CAPACITY;
                        if (historyTime[older] <= targetTime) {
                            final long span = historyTime[newer] - historyTime[older];
                            final float t = span > 0L
                                ? (targetTime - historyTime[older]) / (float) span
                                : 1f;
                            sampleX = historyX[older]
                                + (historyX[newer] - historyX[older]) * t;
                            sampleY = historyY[older]
                                + (historyY[newer] - historyY[older]) * t;
                            break;
                        }
                        newer = older;
                    }
                }
            }
        }

        pathOffsetX[index] = sampleX - currentX;
        pathOffsetY[index] = sampleY - currentY;
    }
}
