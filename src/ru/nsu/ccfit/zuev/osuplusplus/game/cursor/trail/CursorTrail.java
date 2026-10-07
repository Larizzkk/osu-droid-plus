package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.trail;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.batch.ColoredSpriteBatch;
import org.anddev.andengine.opengl.texture.ITexture;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

/**
 * Legacy cursor trail — the STANDARD osu! stable behavior (decompiled point
 * generator {@code #=zQYIyZ$H9RyvP8jWtkg==}, branch selected by
 * {@code trailSprite.zHDm4ShXLTfVQ() != null}).
 *
 * Stable has two trail branches:
 * <ul>
 *   <li>ribbon branch (skin defines {@code cursortrail2}): a point every 0.25
 *       cursor-quad WITH interpolation along the movement segment — a smooth,
 *       danser-like strip. Deliberately NOT used here;</li>
 *   <li><b>standard branch (this one — no {@code cursortrail2}): a point is
 *       spawned at most every {@code 16.666666666666668 ms}, directly AT the
 *       current cursor position, with NO interpolation along the segment.</b>
 *       At speed the dots stay visibly separated — the characteristic "torn"
 *       stable trail — while a stationary cursor stacks fading dots in place.
 *       Every spawned point fades with {@code FadeOut(150, 0, 0)}: a linear
 *       fade to 0 over 150ms.</li>
 * </ul>
 * Also from stable: spawning stops while the cursor is hidden but existing
 * points are NOT wiped (they age out on their own); centered quads of
 * {@code cursortrail * cursor.baseSize}; rotation by the cursor when the skin's
 * CursorTrailRotate is on; standard alpha blending.
 *
 * Rendering: ring buffer of up to {@link #MAX_POINTS} points drawn in a single
 * {@link ColoredSpriteBatch} (one draw call), the same approach as
 * {@link CursorTrailOptimized}; the entity updates itself as a child of the
 * scene, so points age out even while the cursor is hidden.
 */
public class CursorTrail extends Entity {

    /** Original osu! trail renderer ring buffer: 2048 quads. */
    private static final int MAX_POINTS = 2048;

    /** Stable's spawn gate: one point per 16.666666666666668 ms of trail clock. */
    private static final float SPAWN_GATE_SECONDS = 0.016666667f;

    /**
     * Point lifetime in real seconds — stable's FadeOut(150, 0, 0). The speed
     * multiplier is cancelled from the lifetime (see {@link #lifetimeSeconds()}),
     * exactly like the old {@code ExpireModifier(0.1f * speedMultiplier)} did.
     */
    private static final float BASE_LIFETIME_SECONDS = 0.15f;

    private CursorSprite cursor;
    private TextureRegion trailTexture;

    // Ring buffer of trail points (oldest at head).
    private final float[] bufX = new float[MAX_POINTS];
    private final float[] bufY = new float[MAX_POINTS];
    private final float[] bufBirth = new float[MAX_POINTS];
    private int head = 0;
    private int count = 0;

    /** Trail clock in seconds, advanced by the scene's frame delta. */
    private float clock = 0f;

    /** Trail clock of the last spawn (stable's last trail spawn timestamp). */
    private float lastSpawnTime = -1f;

    private boolean spawnEnabled = false;

    private final TrailBatch batch;

    public CursorTrail(TextureRegion pTextureRegion, CursorSprite cursor) {
        this.cursor = cursor;
        this.trailTexture = pTextureRegion;

        batch = new TrailBatch(pTextureRegion.getTexture(), MAX_POINTS);
    }

    /**
     * The batch is a child of this entity, so attaching this entity to the scene
     * (see {@code CursorEntity.attachToScene}) also makes the quads render.
     */
    @Override
    public void onAttached() {
        super.onAttached();

        if (!batch.hasParent()) {
            attachChild(batch);
        }
    }

    @Override
    protected void onManagedUpdate(float pSecondsElapsed) {
        clock += pSecondsElapsed;

        // Drop points that finished their fade. Runs every frame regardless of
        // spawn state, so the remaining points age out while the cursor is hidden.
        float life = lifetimeSeconds();
        while (count > 0 && clock - bufBirth[head] >= life) {
            head = (head + 1) % MAX_POINTS;
            count--;
        }

        super.onManagedUpdate(pSecondsElapsed);
    }

    /**
     * Feeds the current cursor position. Called once per frame from
     * {@code CursorEntity.update()} while the cursor is showing.
     *
     * Stable standard branch: a point is spawned at most every
     * {@link #SPAWN_GATE_SECONDS}, at the position the cursor has RIGHT NOW —
     * no distance threshold, no interpolation along the movement segment. Fast
     * movement leaves the dots visibly separated (torn trail), a stationary
     * cursor stacks fading dots in place. The gate timestamp persists across
     * hide/show, exactly like stable's last-spawn field.
     */
    public void feedPosition(float x, float y) {
        if (!spawnEnabled) {
            return;
        }
        if (clock - lastSpawnTime < SPAWN_GATE_SECONDS) {
            return;
        }
        lastSpawnTime = clock;
        appendPoint(x, y);
    }

    /**
     * Point lifetime in clock seconds. The scene delta is speed-multiplied during
     * gameplay (DT/HT), so the multiplier is folded in to keep the fade at
     * BASE_LIFETIME_SECONDS of REAL time. The menu path keeps a 0 multiplier until
     * the first gameplay load; treat it as 1 (see §30.2).
     */
    private float lifetimeSeconds() {
        float speedMultiplier = GameHelper.getSpeedMultiplier();

        if (speedMultiplier <= 0f) {
            speedMultiplier = 1f;
        }

        return BASE_LIFETIME_SECONDS * speedMultiplier;
    }

    private void appendPoint(float x, float y) {
        int tail = (head + count) % MAX_POINTS;
        bufX[tail] = x;
        bufY[tail] = y;
        bufBirth[tail] = clock;

        if (count < MAX_POINTS) {
            count++;
        } else {
            // Ring full: overwrite the oldest point.
            head = (head + 1) % MAX_POINTS;
        }
    }

    /**
     * Enables/disables spawning (cursor shown/hidden). Existing points are left
     * alone either way — they age out on their own (stable's cursor-hidden path
     * only returns early, it never erases already-spawned points).
     */
    public void setParticlesSpawnEnabled(boolean enabled) {
        spawnEnabled = enabled;
    }

    /**
     * Rebinds the trail texture after a skin hot-swap: live points keep fading but
     * with the new GL texture, quad sizes track the new region, and the dead
     * CursorSprite reference is replaced (the batch reads cursor.baseSize and
     * cursor.getRotation() every draw — CursorEntity rebuilds the sprite in place).
     * Must run on the update/GL thread.
     */
    public void refreshTexture(TextureRegion newTrailTexture, CursorSprite newCursor) {
        if (newTrailTexture == null || newCursor == null) {
            return;
        }
        trailTexture = newTrailTexture;
        cursor = newCursor;
        batch.setTexture(newTrailTexture.getTexture());
    }

    /**
     * Clears all points. Used on trail switches / replay resets — NOT on cursor
     * hide (the original lets hidden points age out on their own).
     */
    public void reset() {
        head = 0;
        count = 0;
    }

    /**
     * Renders every live point as one quad; a single draw call for the whole
     * trail. Newest points are drawn last so they sit on top of older ones.
     */
    private class TrailBatch extends ColoredSpriteBatch {

        TrailBatch(ITexture texture, int capacity) {
            super(texture, capacity);
            setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
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

            float scale = cursor.baseSize;
            float w = trailTexture.getWidth() * scale;
            float h = trailTexture.getHeight() * scale;
            boolean rotate = OsuSkin.get().isRotateCursorTrail();
            float rotation = rotate ? cursor.getRotation() : 0f;
            float life = lifetimeSeconds();

            int rect = 0;
            for (int i = 0; i < count && rect < MAX_POINTS; i++, rect++) {
                int idx = (head + i) % MAX_POINTS;

                // Linear fade — stable's FadeOut(150): 1 at spawn -> 0 at lifetime end.
                float alpha = 1f - (clock - bufBirth[idx]) / life;
                if (alpha < 0f) {
                    alpha = 0f;
                } else if (alpha > 1f) {
                    alpha = 1f;
                }

                float px = bufX[idx];
                float py = bufY[idx];

                if (rotate) {
                    // Rotation must pivot around the point's center, otherwise every
                    // quad is offset half a size diagonally.
                    drawWithoutChecks(trailTexture,
                        px - w * 0.5f, py - h * 0.5f, w, h,
                        rotation,
                        1f, 1f);
                } else {
                    drawWithoutChecks(trailTexture,
                        px - w * 0.5f, py - h * 0.5f, w, h);
                }
                putColor(rect, 1f, 1f, 1f, alpha);
            }

            setIndex(rect);
            return true;
        }
    }
}
