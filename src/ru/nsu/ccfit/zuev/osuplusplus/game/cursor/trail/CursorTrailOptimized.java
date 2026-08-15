package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.trail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.microedition.khronos.opengles.GL10;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

/**
 * Optimized cursor trail implementation inspired by danser-go
 * Features:
 * - Non-breaking continuous trail
 * - Optimized rendering with reusable sprites
 * - Smart interpolation for smooth movement
 * - Memory efficient pooling
 */
public class CursorTrailOptimized extends Entity {

    private final CursorSprite cursor;
    private final TextureRegion trailTexture;

    // Trail points management — CopyOnWriteArrayList for thread safety
    // with the fire-and-forget update/render pipeline.
    private final CopyOnWriteArrayList<TrailPoint> trailPoints;
    private final List<Sprite> trailSprites;
    private int maxTrailPoints = 20;
    private float trailDuration = 0.5f; // seconds
    private float minDistance = 5f; // minimum pixels between trail points
    private float trailScale = 1.0f;
    private float trailWidth = 1.0f;

    private boolean isFirstMove = true;
    private float accumulatedDistance = 0f;
    private float trailLength;
    private long currentTime = 0;
    private float lastX = 0f;
    private float lastY = 0f;

    // Performance optimization
    private static final float MIN_DISTANCE_BETWEEN_POINTS = 2f;
    private static final int DEFAULT_MAX_POINTS = 500; // Increased from 100 to allow longer trails

    /**
     * Represents a single point in the trail with position and time
     */
    private static class TrailPoint {

        float x, y;
        long time;
        float alpha;

        TrailPoint(float x, float y, long time, float alpha) {
            this.x = x;
            this.y = y;
            this.time = time;
            this.alpha = alpha;
        }
    }

    public CursorTrailOptimized(
        TextureRegion trailTexture,
        CursorSprite cursor
    ) {
        this.trailTexture = trailTexture;
        this.cursor = cursor;
        this.trailPoints = new CopyOnWriteArrayList<>();
        this.trailSprites = new ArrayList<>();
        this.trailLength = loadTrailLength();
        this.maxTrailPoints = calculateMaxPoints();

        // Pre-create sprites for performance
        preCreateSprites();
    }

    private float loadTrailLength() {
        try {
            return Config.getTrailLength();
        } catch (Exception e) {
            return 0.5f;
        }
    }

    private int calculateMaxPoints() {
        float fadeTimeMs = trailLength * 1000 * GameHelper.getSpeedMultiplier();
        int fps = 60;
        return Math.min(
            5000,
            (int) ((fadeTimeMs / (1000f / fps)) * 1.5f)
        );
    }

    private void preCreateSprites() {
        for (int i = 0; i < maxTrailPoints; i++) {
            Sprite sprite = new Sprite(0, 0, trailTexture);
            sprite.setVisible(false);
            sprite.setBlendFunction(
                GL10.GL_SRC_ALPHA,
                GL10.GL_ONE_MINUS_SRC_ALPHA
            );
            trailSprites.add(sprite);
            attachChild(sprite);
        }
    }

    /**
     * Update trail with new cursor position
     */
    public void updatePosition(float x, float y, float deltaTimeSeconds) {
        currentTime += deltaTimeSeconds * 1000;

        if (isFirstMove) {
            lastX = x;
            lastY = y;
            isFirstMove = false;
            addTrailPoint(x, y);
            return;
        }

        // Calculate distance and interpolate if needed
        float dx = x - lastX;
        float dy = y - lastY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            accumulatedDistance += distance;

            // Add interpolated points for smooth trail
            float stepSize = MIN_DISTANCE_BETWEEN_POINTS;
            int steps = Math.max(1, (int) (distance / stepSize));

            for (int i = 1; i <= steps; i++) {
                float t = i / (float) steps;
                float interpX = lastX + dx * t;
                float interpY = lastY + dy * t;

                addTrailPoint(interpX, interpY);
            }

            lastX = x;
            lastY = y;
        }

        // Update trail points and sprites
        updateTrailPoints();
        updateTrailSprites();
    }

    private void addTrailPoint(float x, float y) {
        TrailPoint point = new TrailPoint(x, y, currentTime, 1.0f);
        trailPoints.add(point);

        // Remove old points if we exceed maximum
        while (trailPoints.size() > maxTrailPoints) {
            trailPoints.remove(0);
        }
    }

    private void updateTrailPoints() {
        long fadeTimeMs = (long) (trailLength *
            1000 *
            GameHelper.getSpeedMultiplier());

        // Snapshot to avoid CME with CopyOnWriteArrayList
        Object[] snapshot = trailPoints.toArray();
        for (int i = snapshot.length - 1; i >= 0; i--) {
            TrailPoint point = (TrailPoint) snapshot[i];
            if (point == null) continue;
            long age = currentTime - point.time;

            if (age > fadeTimeMs) {
                trailPoints.remove(point);
            } else {
                float alpha = 1.0f - age / (float) fadeTimeMs;
                point.alpha = Math.max(0, Math.min(1, alpha));
            }
        }
    }

    private void updateTrailSprites() {
        // Snapshot trail points for thread safety
        TrailPoint[] points = trailPoints.toArray(new TrailPoint[0]);
        int pointCount = points.length;

        // Ensure we have enough sprites
        while (trailSprites.size() < pointCount) {
            Sprite sprite = new Sprite(0, 0, trailTexture);
            sprite.setVisible(false);
            sprite.setBlendFunction(
                GL10.GL_SRC_ALPHA,
                GL10.GL_ONE_MINUS_SRC_ALPHA
            );
            trailSprites.add(sprite);
            attachChild(sprite);
        }

        // Update sprites to match trail points
        for (int i = 0; i < pointCount; i++) {
            if (i < trailSprites.size()) {
                TrailPoint point = points[i];
                if (point == null) continue;
                Sprite sprite = trailSprites.get(i);

                float offsetX = -trailTexture.getWidth() / 2f;
                float offsetY = -trailTexture.getHeight() / 2f;
                sprite.setPosition(point.x + offsetX, point.y + offsetY);

                sprite.setAlpha(point.alpha);
                float scale = cursor.baseSize * point.alpha;
                sprite.setScale(scale);

                if (OsuSkin.get().isRotateCursorTrail()) {
                    sprite.setRotation(cursor.getRotation());
                }

                sprite.setVisible(true);
            }
        }

        // Hide unused sprites
        for (int i = pointCount; i < trailSprites.size(); i++) {
            trailSprites.get(i).setVisible(false);
        }
    }

    /**
     * Update trail length from configuration
     */
    public void updateTrailLength() {
        this.trailLength = loadTrailLength();
        this.maxTrailPoints = calculateMaxPoints();
        // Trail points will automatically adjust on next update
    }

    /**
     * Reset trail state
     */
    public void reset() {
        trailPoints.clear();
        isFirstMove = true;
        accumulatedDistance = 0f;
        currentTime = 0;

        // Hide all sprites
        for (Sprite sprite : trailSprites) {
            sprite.setVisible(false);
        }
    }

    /**
     * Thread-safe access to check if trail has points.
     */
    public boolean hasPoints() {
        return !trailPoints.isEmpty();
    }

    /**
     * Update trail scale
     */
    public void updateTrailScale(float scale) {
        this.trailScale = scale;
        // Update existing sprites
        for (int i = 0; i < trailSprites.size(); i++) {
            Sprite sprite = trailSprites.get(i);
            float progress = (float) i / trailSprites.size();
            float currentScale = trailScale * (1.0f - progress * 0.5f);
            sprite.setScale(currentScale);
        }
    }

    /**
     * Update trail width
     */
    public void updateTrailWidth(float width) {
        this.trailWidth = width;
        // Update existing sprites
        for (Sprite sprite : trailSprites) {
            sprite.setWidth(sprite.getWidth() * width);
        }
    }

    /**
     * Clean up trail resources
     */
    public void cleanup() {
        for (Sprite sprite : trailSprites) {
            detachChild(sprite);
        }
        trailSprites.clear();
    }

    /**
     * Force update of all trail elements
     */
    public void update() {
        updateTrailPoints();
        updateTrailSprites();
    }

    /**
     * Get current trail length in seconds
     */
    public float getTrailLength() {
        return trailLength;
    }

    /**
     * Get number of active trail points
     */
    public int getActivePointCount() {
        return trailPoints.size();
    }
}
