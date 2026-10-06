package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.trail;

import org.anddev.andengine.entity.particle.ParticleSystem;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.particle.initializer.ScaleInitializer;
import org.anddev.andengine.entity.particle.modifier.AlphaModifier;
import org.anddev.andengine.entity.particle.modifier.ExpireModifier;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

/**
 * Legacy particle cursor trail — a 1:1 port of upstream osu-droid:
 * fixed 0.1s particle lifetime (cancelled by the speed multiplier), particle
 * capacity bound to the spawn rate, particle size bound to the cursor size.
 * The Trail Length setting applies to the Long trail only.
 */
public class CursorTrail extends ParticleSystem {
    private final CursorSprite cursor;

    public CursorTrail(
            PointParticleEmitter emitter,
            int spawnRate,
            TextureRegion pTextureRegion,
            CursorSprite cursor
    ) {
        super(emitter, spawnRate, spawnRate, spawnRate, pTextureRegion);

        this.cursor = cursor;

        // Cancelling the speed multiplier for the trail.
        // GameHelper.speedMultiplier stays 0 until the first gameplay load, while the menu
        // cursor is constructed before any game — a 0 multiplier would give the particles a
        // zero lifetime and a zero alpha, making the menu trail invisible. Gameplay always
        // runs with a positive multiplier, so the clamp only affects the menu path.
        float speedMultiplier = GameHelper.getSpeedMultiplier();

        if (speedMultiplier <= 0f) {
            speedMultiplier = 1f;
        }

        addParticleModifier(new ExpireModifier(0.1f * speedMultiplier));
        addParticleModifier(new AlphaModifier(speedMultiplier, 0.0f, 0f, 0.10f));

        setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        addParticleInitializer(new ScaleInitializer(cursor.baseSize));
        setParticlesSpawnEnabled(false);
        prewarm();
        updateRotation();
    }

    public void update() {
        updateRotation();
    }

    /**
     * Rebinds the particle texture after a skin hot-swap: particles capture the
     * TextureRegion at spawn time and would otherwise keep the previous skin's
     * (unloaded) GL texture.
     */
    public void refreshTexture(TextureRegion newTrailTexture) {
        if (newTrailTexture == null) {
            return;
        }
        setTextureRegion(newTrailTexture);
    }

    private void prewarm() {
        setParticlesSpawnEnabled(true);
        // Fill every particle slot with one large-delta update.
        onManagedUpdate(10.0f);
        // Drain the spawn accumulator (rate * 10 - max) back to ~0.
        // Each 0.3 s pass spawns a batch and lets it fully expire, consuming
        // the surplus without leaving a burst on the first real gameplay frame.
        // 20 passes covers all realistic spawn rates and speed multipliers.
        for (int i = 0; i < 20; i++) {
            onManagedUpdate(0.3f);
        }
        setParticlesSpawnEnabled(false);
    }

    private void updateRotation() {
        if (OsuSkin.get().isRotateCursorTrail()) {
            setRotation(cursor.getRotation());
        }
    }

    /**
     * Reset the trail - disable spawning and clear existing particles.
     * Called when the cursor is hidden or the trail is switched.
     */
    public void reset() {
        setParticlesSpawnEnabled(false);
        // Force all particles to expire by updating with a large time step
        // This is the standard way to clear ParticleSystem particles
        onManagedUpdate(1000.0f);
        // Expired particles are recycled internally by ParticleSystem; force one more update so
        // every dead particle's position/alpha is re-applied (no stale sprites left on screen).
        onManagedUpdate(0.016f);
    }
}
