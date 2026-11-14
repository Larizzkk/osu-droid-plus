package ru.nsu.ccfit.zuev.osuplusplus;

import org.anddev.andengine.engine.camera.Camera;

import java.util.Random;

/**
 * Simple screen shake effect using AndEngine Camera offset.
 */
public class ScreenShake {

    private static final Random random = new Random();
    private float intensity = 0;
    private float duration = 0;
    private float elapsed = 0;
    private float offsetX = 0;
    private float offsetY = 0;
    private Camera camera;
    private float origCenterX;
    private float origCenterY;

    public ScreenShake(Camera camera) {
        this.camera = camera;
        this.origCenterX = camera.getCenterX();
        this.origCenterY = camera.getCenterY();
    }

    /**
     * Trigger a screen shake.
     * @param intensity Max pixel offset
     * @param duration Duration in seconds
     */
    public void shake(float intensity, float duration) {
        this.intensity = Math.max(this.intensity, intensity);
        this.duration = Math.max(this.duration, duration);
        this.elapsed = 0;
    }

    /**
     * Update the shake effect. Call every frame.
     */
    public void update(float dt) {
        if (duration <= 0 || camera == null) return;

        elapsed += dt;
        if (elapsed >= duration) {
            // Shake over - reset camera
            offsetX = 0;
            offsetY = 0;
            duration = 0;
            intensity = 0;
            camera.setCenter(origCenterX, origCenterY);
            return;
        }

        // Decay
        float decay = 1 - (elapsed / duration);
        float currentIntensity = intensity * decay * decay;

        // Random offset
        offsetX = (random.nextFloat() - 0.5f) * 2 * currentIntensity;
        offsetY = (random.nextFloat() - 0.5f) * 2 * currentIntensity;

        camera.setCenter(origCenterX + offsetX, origCenterY + offsetY);
    }

    /**
     * Reset the camera position and stop shaking.
     */
    public void reset() {
        if (camera != null) {
            camera.setCenter(origCenterX, origCenterY);
        }
        offsetX = 0;
        offsetY = 0;
        duration = 0;
        intensity = 0;
        elapsed = 0;
    }
}
