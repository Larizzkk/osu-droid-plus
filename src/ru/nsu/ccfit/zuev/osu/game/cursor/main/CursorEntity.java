package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager;
import ru.nsu.ccfit.zuev.osu.game.cursor.trail.CursorTrail;

public class CursorEntity extends Entity {
    protected final CursorSprite cursorSprite;
    private CursorTrail trail = null;
    private PointParticleEmitter emitter = null;
    private boolean isShowing = false;
    private float particleOffsetX, particleOffsetY;

    public CursorEntity() {
        TextureRegion cursorTex = ResourceManager.getInstance().getTexture("cursor");
        cursorSprite = new CursorSprite(-cursorTex.getWidth() / 2f, -cursorTex.getWidth() / 2f, cursorTex);

        if (Config.isUseParticles()) {
            TextureRegion trailTex = ResourceManager.getInstance().getTexture("cursortrail");

            particleOffsetX = -trailTex.getWidth() / 2f;
            particleOffsetY = -trailTex.getHeight() / 2f;

            // Spawn rate must be high enough to create a dense, gap-free trail.
            // At 60fps × 4 = 240 particles/sec, with ~0.25s lifetime = ~60 visible dots.
            var spawnRate = (int) (GlobalManager.getInstance().getMainActivity().getRefreshRate() * 4);

            emitter = new PointParticleEmitter(particleOffsetX, particleOffsetY);
            trail = new CursorTrail(emitter, spawnRate, trailTex, cursorSprite);
            trail.setParticlesSpawnEnabled(false);
        }

        attachChild(cursorSprite);
        setVisible(false);

        // Not necessary to update by itself since it's done by GameScene.
        setIgnoreUpdate(true);
    }

    public void setShowing(boolean showing) {
        isShowing = showing;
        setVisible(showing);
        cursorSprite.setVisible(showing);
        if (trail != null) {
            trail.setParticlesSpawnEnabled(showing);
            if (!showing) {
                trail.reset();
            }
        }
    }

    public void click() {
        if (isShowing) {
            cursorSprite.handleClick();
        }
    }

    public void update(float pSecondsElapsed) {
        if (isShowing) {
            cursorSprite.update(pSecondsElapsed);

            if (trail != null) {
                trail.update();
            }
        }

        super.onManagedUpdate(pSecondsElapsed);
    }

    public void attachToScene(Scene fgScene) {
        if (trail != null) {
            fgScene.attachChild(trail);
        }
        fgScene.attachChild(this);
    }

    private float lastX = -1000, lastY = -1000;

    @Override
    public void setPosition(float pX, float pY) {
        if (emitter != null) {
            float dx = pX - lastX;
            float dy = pY - lastY;
            // Always update emitter position to prevent trail gaps.
            // The old threshold of 0.5 caused visible trail lag at 60-90 fps.
            emitter.setCenter(pX + particleOffsetX, pY + particleOffsetY);
            lastX = pX;
            lastY = pY;
        }

        super.setPosition(pX, pY);
    }
}
