package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

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
 * The original osu!droid cursor trail (repos/osu-droid).
 *
 * A point-particle stream emitted at the cursor position, with a fixed expiry
 * and fade. Kept as a separate class so the stable-style and ribbon trails can
 * remain available as other options.
 */
public class CursorTrailClassic extends ParticleSystem {
    private final CursorSprite cursor;

    public CursorTrailClassic(
            PointParticleEmitter emitter,
            int spawnRate,
            TextureRegion pTextureRegion,
            CursorSprite cursor
    ) {
        super(emitter, spawnRate, spawnRate, spawnRate, pTextureRegion);
        this.cursor = cursor;

        // Cancelling the speed multiplier for the trail. Before a map starts,
        // GameHelper.speedMultiplier is still 0 (only GameScene sets it), which
        // made the expire time and starting alpha zero: the menu cursor trail
        // was created but every particle died instantly after an app restart.
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

    private void prewarm() {
        setParticlesSpawnEnabled(true);
        onManagedUpdate(10.0f);
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
}
