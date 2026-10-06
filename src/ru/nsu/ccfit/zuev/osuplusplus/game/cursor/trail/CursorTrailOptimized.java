package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.trail;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.batch.ColoredSpriteBatch;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

/**
 * Long cursor trail — a faithful port of danser-go's dansercursor.go.
 *
 * Data layout: ring buffer of trail points (x/y), one point every
 * 1/TrailDensity osu!pixels of cursor movement. Points are removed with the
 * danser-go formula (delta in MILLISECONDS):
 *
 *   removeCounter += (count + 3) / (360 / deltaMs) * TrailRemoveSpeed
 *
 * which makes the trail fade out over ~TrailMaxLength/density px of history
 * and disappear right after the cursor stops (points keep aging out every
 * frame regardless of movement).
 *
 * Rendering: every live point is one textured quad inside a single
 * {@link ColoredSpriteBatch} — one draw call for the whole trail. The look
 * matches danser-go's cursortrail shaders:
 *   scale = mix(endScale, 1, smoothstep(inst - points, inst, i))
 *   alpha = smoothstep(inst - points, inst - points * 2/3, i)
 * i.e. the tail (oldest third) fades out, and the head is full cursor size.
 */
public class CursorTrailOptimized extends Entity {

    /** danser-go Cursor.TrailMaxLength default. */
    private static final int TRAIL_MAX_LENGTH = 2000;
    /** danser-go Cursor.TrailDensity default (osu!pixels between points). */
    private static final float TRAIL_DENSITY = 1.0f;
    /** danser-go Cursor.TrailEndScale default. */
    private static final float TRAIL_END_SCALE = 0.4f;
    /** danser-go Cursor.TrailRemoveSpeed default. */
    private static final float TRAIL_REMOVE_SPEED = 1.0f;
    /** Hard cap so insane trail lengths can't allocate forever. */
    private static final int MAX_POINTS = 6000;

    private CursorSprite cursor;
    private TextureRegion trailTexture;

    // Ring buffer of trail points.
    private float[] bufX = new float[0];
    private float[] bufY = new float[0];
    /** Spawn time of each point, in accumulated gameplay seconds (see trailClockSeconds). */
    private float[] bufTime = new float[0];
    private int head = 0;
    private int count = 0;

    private double removeCounter = 0.0;

    /**
     * Gameplay clock of the trail, in seconds. Advanced by the frame delta, which
     * is already speed-multiplied, so DT/HT naturally speed up/slow down aging.
     * Used to stamp point spawn times for the fixed fade-time removal mode.
     */
    private float trailClockSeconds = 0f;

    private float lastX, lastY;
    private boolean hasLastPosition = false;

    /**
     * Teleport threshold in pixels: if a single position update jumps farther than this, the
     * movement was not physical (finger lifted and pressed elsewhere, replay seek, relax pointer
     * repositioning). No trail points are interpolated along the jump; the trail simply continues
     * from the new location. Without this, relax mode drew a straight "ribbon" of trail points
     * between the two touch points.
     */
    private static final float TELEPORT_DISTANCE_SQ = 400f * 400f;

    /** Trail length in osu!pixels (TrailMaxLength * TrailDensity * user scale). */
    private int lengthAdjusted = TRAIL_MAX_LENGTH;

    /**
     * User scale from the Trail Length setting (seconds / 2.0). 1.0 = danser-go
     * defaults (TrailMaxLength 2000, TrailRemoveSpeed 1.0). Scales both the point
     * cap and the aging rate, so the visible trail length grows linearly with the
     * setting while keeping danser's exact look at the default value.
     */
    private float lengthScale = 1f;

    private final TrailBatch batch;

    public CursorTrailOptimized(TextureRegion trailTexture, CursorSprite cursor) {
        this.trailTexture = trailTexture;
        this.cursor = cursor;

        lengthScale = computeLengthScale();
        lengthAdjusted = computeLengthAdjusted();
        ensureCapacity(lengthAdjusted + 64);

        batch = new TrailBatch(trailTexture.getTexture(), MAX_POINTS);
        batch.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

        setIgnoreUpdate(true);
    }

    /**
     * The batch is a child of this entity, so attaching this entity to the scene (see
     * {@code CursorEntity.attachToScene}) also makes the quads render.
     */
    @Override
    public void onAttached() {
        super.onAttached();

        if (!batch.hasParent()) {
            attachChild(batch);
        }
    }

    /**
     * User scale from the Trail Length setting, which is stored in 0.01s units
     * (default 200 = 2.0s = danser-go defaults).
     */
    private static float computeLengthScale() {
        return Math.max(0.05f, Config.getTrailLength() / 2f);
    }

    /** Point cap: danser's TrailMaxLength * TrailDensity, scaled by the setting. */
    private int computeLengthAdjusted() {
        return Math.max(64, Math.min(MAX_POINTS, (int) (TRAIL_MAX_LENGTH * TRAIL_DENSITY * lengthScale)));
    }

    private void ensureCapacity(int needed) {
        if (bufX.length >= needed) {
            return;
        }
        float[] nx = new float[needed];
        float[] ny = new float[needed];
        float[] nt = new float[needed];
        // Preserve existing points (oldest first).
        for (int i = 0; i < count; i++) {
            int idx = (head + i) % bufX.length;
            nx[i] = bufX[idx];
            ny[i] = bufY[idx];
            nt[i] = bufTime[idx];
        }
        bufX = nx;
        bufY = ny;
        bufTime = nt;
        head = 0;
    }

    /**
     * Update trail with the current cursor position.
     *
     * @param deltaTimeSeconds frame delta in seconds (AndEngine pSecondsElapsed)
     */
    public void updatePosition(float x, float y, float deltaTimeSeconds) {
        trailClockSeconds += deltaTimeSeconds;

        if (!hasLastPosition) {
            lastX = x;
            lastY = y;
            hasLastPosition = true;
            appendPoint(x, y);
        } else {
            float dx = x - lastX;
            float dy = y - lastY;
            float distSq = dx * dx + dy * dy;

            // Teleport guard: finger re-press / relax repositioning / seek. Do NOT interpolate
            // points along the jump; restart the trail from the new position.
            if (distSq >= TELEPORT_DISTANCE_SQ) {
                lastX = x;
                lastY = y;
                appendPoint(x, y);
            } else {
                // Spatial interpolation — matches danser-go: one point every
                // 1/TrailDensity pixels, placed along the exact movement segment.
                // This keeps the trail smooth (no sharp corners) during fast flicks.
                float step = 1.0f / TRAIL_DENSITY;
                float dist = (float) Math.sqrt(distSq);

                if (dist >= step) {
                    float invDist = 1f / dist;
                    for (float d = step; d < dist; d += step) {
                        float t = d * invDist;
                        appendPoint(lastX + dx * t, lastY + dy * t);
                    }
                    lastX = x;
                    lastY = y;
                }
            }
        }

        removeOldPoints(deltaTimeSeconds);
    }

    /**
     * Exact danser-go removal loop. deltaMs must be milliseconds, matching
     * danser (nanoseconds / 1e6).
     *
     * Two modes (see Config.isTrailFastRemoval):
     * - fast removal (danser, default): removal runs every frame from `count`
     *   (not from movement), so the trail visibly disappears right after the
     *   cursor stops — but the more points it holds, the faster each one dies
     *   (~360ms * lengthScale total), which shrinks the trail on medium movements.
     * - fixed fade time: every point lives exactly Trail Fade Time seconds of
     *   gameplay time regardless of the point count — the trail keeps its full
     *   length on medium-speed movements and peels away over the configured time.
     */
    private void removeOldPoints(float deltaTimeSeconds) {
        if (count <= 0) {
            removeCounter = 0;
            return;
        }

        if (count > lengthAdjusted) {
            // Hard cap: truncate oldest points (danser behavior).
            int excess = count - lengthAdjusted;
            head = (head + excess) % bufX.length;
            count = lengthAdjusted;
            removeCounter = 0;
        }

        if (Config.isTrailFastRemoval()) {
            double deltaMs = Math.max(0.5, deltaTimeSeconds * 1000.0);
            double pointsPerFrame = 360.0 / deltaMs;

            // Gameplay feeds rate-multiplied dt and compensates with the same multiplier;
            // menus feed real dt while GameHelper.speedMultiplier is still 0 (no game loaded
            // yet), which would freeze the removal entirely — treat 0 as 1 there.
            double speedMultiplier = GameHelper.getSpeedMultiplier();

            if (speedMultiplier <= 0) {
                speedMultiplier = 1;
            }

            removeCounter += (double) (count + 3) / pointsPerFrame * TRAIL_REMOVE_SPEED * speedMultiplier / lengthScale;

            int times = (int) Math.floor(removeCounter);
            if (times > 0) {
                times = Math.min(times, count);
                head = (head + times) % bufX.length;
                count -= times;
                removeCounter -= times;
            }
        } else {
            // Fixed fade time: drop points older than the configured fade time.
            float fade = Math.max(0.05f, Config.getTrailFadeTime());

            while (count > 0 && trailClockSeconds - bufTime[head] >= fade) {
                head = (head + 1) % bufX.length;
                count--;
            }
        }
    }

    private void appendPoint(float x, float y) {
        if (count >= bufX.length) {
            ensureCapacity(bufX.length * 2);
        }
        int tail = (head + count) % bufX.length;
        bufX[tail] = x;
        bufY[tail] = y;
        bufTime[tail] = trailClockSeconds;
        count++;
    }

    public void reset() {
        head = 0;
        count = 0;
        removeCounter = 0;
        hasLastPosition = false;
        batch.setIndex(0);
    }

    /**
     * Marks an input discontinuity (finger lifted, re-pressed elsewhere, replay seek).
     * The next position update starts a fresh ribbon from that position instead of
     * interpolating from the last fed point — regardless of the distance between them.
     * Existing points are kept so they keep aging out naturally. Unlike the 400px
     * teleport guard this is event-based and covers close re-presses.
     */
    public void markDiscontinuity() {
        hasLastPosition = false;
    }

    /**
     * Seeds the ribbon at an absolute position WITHOUT drawing a segment to it (used
     * after a trail recreation on skin hot-swap: the new trail entity must resume from
     * the cursor's CURRENT position, not interpolate from its uninitialized (0,0)
     * state — that interpolation stretched a ribbon across the whole screen).
     */
    public void syncToPosition(float x, float y) {
        lastX = x;
        lastY = y;
        hasLastPosition = true;
        org.anddev.andengine.util.Debug.i("CursorTrailOptimized: syncToPosition ("
            + (int) x + "," + (int) y + ") ring=" + count + "/" + bufX.length);
    }

    /**
     * Runs ONLY the removal pass for the current points, without appending anything.
     * Used while the cursor is hidden (finger lifted during replay playback): danser's
     * removal runs every frame from the point count regardless of movement, which is
     * exactly what makes the trail peel away and vanish after the cursor stops —
     * skipping it while hidden froze the ribbon on screen forever.
     */
    public void updateRemovalOnly(float deltaTimeSeconds) {
        trailClockSeconds += deltaTimeSeconds;
        removeOldPoints(deltaTimeSeconds);
    }

    public void updateTrailLength() {
        lengthScale = computeLengthScale();
        int old = lengthAdjusted;
        lengthAdjusted = computeLengthAdjusted();
        if (lengthAdjusted < old) {
            ensureCapacity(lengthAdjusted + 64);
            if (count > lengthAdjusted) {
                head = (head + (count - lengthAdjusted)) % bufX.length;
                count = lengthAdjusted;
            }
        } else {
            ensureCapacity(lengthAdjusted + 64);
        }
    }

    public boolean hasPoints() {
        return count > 0;
    }

    public int getActivePointCount() {
        return count;
    }

    /** Effective trail length in seconds (the Trail Length setting). */
    public float getTrailLength() {
        return lengthScale * 2f;
    }

    public void cleanup() {
        batch.detachSelf();
        detachSelf();
    }

    /**
     * Re-pulls the trail texture after a mid-game skin switch. The batch GL texture is
     * rebound via SpriteBatch.setTexture() when the atlas changed, and the dead
     * CursorSprite reference is replaced (TrailBatch reads cursor.baseSize and
     * cursor.getRotation() every frame).
     * Must run on the update/GL thread.
     */
    public void refreshTexture(TextureRegion newTrailTexture, CursorSprite newCursor) {
        if (newTrailTexture == null || newCursor == null) {
            org.anddev.andengine.util.Debug.e("CursorTrailOptimized.refreshTexture: null region/cursor — trail keeps OLD texture");
            return;
        }
        org.anddev.andengine.opengl.texture.ITexture newGl = newTrailTexture.getTexture();
        trailTexture = newTrailTexture;
        cursor = newCursor;
        // Quad sizes must track the new region: skins ship cursortrail textures of
        // different pixel sizes, and the batch bakes them at construction.
        batch.quadW = newTrailTexture.getWidth();
        batch.quadH = newTrailTexture.getHeight();
        batch.setTexture(newGl);
        org.anddev.andengine.util.Debug.i("CursorTrailOptimized.refreshTexture: tex="
            + newTrailTexture.getWidth() + "x" + newTrailTexture.getHeight()
            + " gl=" + newGl.getClass().getSimpleName() + " points=" + count);
    }

    /**
     * The batch renders up to MAX_POINTS quads every frame; quads beyond
     * `count` are collapsed to the newest point with alpha 0 so they cost
     * only vertex writes, never pixels.
     */
    private class TrailBatch extends ColoredSpriteBatch {

        /** Texture-space quad size (px) at cursor base scale. Updated on skin switch. */
        private float quadW;
        private float quadH;

        TrailBatch(org.anddev.andengine.opengl.texture.ITexture texture, int capacity) {
            super(texture, capacity);
            quadW = trailTexture.getWidth();
            quadH = trailTexture.getHeight();
            setIgnoreUpdate(true);
        }

        @Override
        protected boolean onUpdateSpriteBatch() {
            resetColors();

            if (count <= 0) {
                // Nothing to draw; submit an empty batch (index 0).
                setIndex(0);
                return true;
            }

            float baseSize = cursor.baseSize * Config.getTrailSize();
            float halfW = quadW * 0.5f;
            float halfH = quadH * 0.5f;
            boolean rotate = OsuSkin.get().isRotateCursorTrail();
            float cursorRotation = cursor.getRotation();

            // norm: 0 = oldest point, 1 = newest point (cursor).
            int renderCount = Math.min(count, lengthAdjusted);
            int startIdx = (head + count - renderCount) % bufX.length;

            int rect = 0;
            for (int i = 0; i < renderCount && rect < MAX_POINTS; i++, rect++) {
                int idx = (startIdx + i) % bufX.length;
                float px = bufX[idx];
                float py = bufY[idx];

                // i=0 -> oldest, i=renderCount-1 -> newest
                float norm = renderCount > 1 ? (float) i / (renderCount - 1) : 1f;

                // danser-go vertex shader:
                //   mix(endScale, 1, smoothstep(inst - points, inst, i))
                float scaleNorm = smoothstep(norm);
                float scale = baseSize * (TRAIL_END_SCALE + (1f - TRAIL_END_SCALE) * scaleNorm);

                // danser-go fragment shader:
                //   color.a *= smoothstep(inst - points, inst - points * 2/3, i)
                float alpha = smoothstepFaded(norm);

                float w = quadW * scale;
                float h = quadH * scale;

                if (rotate) {
                    // Rotation must pivot around the point's center. The (pX, pY, w, h,
                    // rotation, scale) overload rotates around (pX + w/2, pY + h/2), so the
                    // unrotated quad's top-left has to be the point minus half the scaled
                    // size — otherwise every segment is offset by half a quad diagonally.
                    drawWithoutChecks(trailTexture,
                        px - w * 0.5f, py - h * 0.5f, w, h,
                        cursorRotation,
                        1f, 1f);
                } else {
                    drawWithoutChecks(trailTexture,
                        px - halfW * scale, py - halfH * scale,
                        w, h);
                }
                putColor(rect, 1f, 1f, 1f, alpha);
            }

            setIndex(rect);
            return true;
        }

        /** GL smoothstep(0, 1, x) — scale curve. */
        private float smoothstep(float x) {
            x = x < 0f ? 0f : (x > 1f ? 1f : x);
            return x * x * (3f - 2f * x);
        }

        /**
         * danser fragment alpha: smoothstep(inst - points, inst - points*2/3, i)
         * With inst = points (we only render live points), that's
         * smoothstep(0, 1/3, norm) — the oldest third fades in.
         */
        private float smoothstepFaded(float norm) {
            return smoothstep(norm * 3f);
        }
    }
}
