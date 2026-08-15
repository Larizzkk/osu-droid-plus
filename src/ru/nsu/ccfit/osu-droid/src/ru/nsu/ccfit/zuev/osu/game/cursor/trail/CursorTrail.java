package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.trail;

import org.anddev.andengine.entity.particle.ParticleSystem;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.particle.initializer.ScaleInitializer;
import org.anddev.andengine.entity.particle.modifier.AlphaModifier;
import org.anddev.andengine.entity.particle.modifier.ExpireModifier;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CursorTrail extends ParticleSystem {
    private final CursorSprite cursor;
    private float trailLength;

    public CursorTrail(
            PointParticleEmitter emitter,
            int spawnRate,
            TextureRegion pTextureRegion,
            CursorSprite cursor
    ) {
        super(emitter, spawnRate, spawnRate, spawnRate, pTextureRegion);

        this.cursor = cursor;

        // Load trail length from preferences
        this.trailLength = loadTrailLength();

        // Apply trail length settings
        addParticleModifier(new ExpireModifier(trailLength * GameHelper.getSpeedMultiplier()));
        addParticleModifier(new AlphaModifier(GameHelper.getSpeedMultiplier(), 0.0f, 0f, trailLength));

        setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        addParticleInitializer(new ScaleInitializer(cursor.baseSize));
        setParticlesSpawnEnabled(false);
        updateRotation();
    }

    private float loadTrailLength() {
        try {
            return Config.getTrailLength();
        } catch (Exception e) {
            return 0.5f;
        }
    }

    public void updateTrailLength() {
        // Update trail length from preferences
        this.trailLength = loadTrailLength();

        // Note: For now, the trail length will be updated when trails are recreated
        // This is simpler and avoids issues with modifier clearing
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
