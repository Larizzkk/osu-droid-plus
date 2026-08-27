package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import org.anddev.andengine.entity.particle.ParticleSystem;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.particle.initializer.ScaleInitializer;
import org.anddev.andengine.entity.particle.modifier.AlphaModifier;
import org.anddev.andengine.entity.particle.modifier.ExpireModifier;
import org.anddev.andengine.entity.particle.modifier.ScaleModifier;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

/**
 * Cursor trail rendered as a series of fading, shrinking particles.
 * 
 * Trail lifecycle:
 *   1. Particle spawns at emitter position (cursor pos) at full size + full alpha.
 *   2. Over its lifetime the particle fades to transparent AND shrinks to zero,
 *      creating a smooth triangle / teardrop taper like danser-go.
 *   3. The trail length is driven by Config.getTrailLength() (default 2.0 seconds).
 *   4. Spawn rate is calculated from the device refresh rate so that the trail
 *      is dense enough to never show gaps.
 */
public class CursorTrail extends ParticleSystem {
    private final CursorSprite cursor;

    /** Base trail lifetime in seconds. */
    private static final float BASE_LIFETIME = 0.25f;

    public CursorTrail(
            PointParticleEmitter emitter,
            int spawnRate,
            TextureRegion pTextureRegion,
            CursorSprite cursor
    ) {
        super(emitter, spawnRate, spawnRate, spawnRate, pTextureRegion);

        this.cursor = cursor;

        // Trail lifetime scales with the user-configured trailLength setting.
        // trailLength defaults to 2.0 (200 / 100).
        float trailLengthMultiplier = Math.max(0.2f, Config.getTrailLength());
        float lifetime = BASE_LIFETIME * trailLengthMultiplier;

        // Particle expiration — how long each trail dot lives.
        addParticleModifier(new ExpireModifier(lifetime));

        // Alpha fade: full opaque → fully transparent over the particle lifetime.
        // This creates the smooth tail fade.
        addParticleModifier(new AlphaModifier(1.0f, 0.0f, 0.0f, lifetime));

        // Scale fade: full size → zero size over the particle lifetime.
        // Combined with the alpha fade this creates the triangle / teardrop taper.
        addParticleModifier(new ScaleModifier(
                cursor.baseSize, 0.0f, 0.0f, lifetime));

        setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        addParticleInitializer(new ScaleInitializer(cursor.baseSize));
        setParticlesSpawnEnabled(false);
        updateRotation();
    }

    public void update() {
        updateRotation();
    }

    private void updateRotation() {
        if (OsuSkin.get().isRotateCursorTrail()) {
            setRotation(cursor.getRotation());
        }
    }
}
