package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.trail;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

import java.util.ArrayList;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

/**
 * Advanced cursor trail implementation with vertex-based rendering
 * Inspired by danser-go's approach for maximum performance and visual quality
 *
 * Features:
 * - Continuous smooth trail without gaps
 * - Vertex-based rendering for optimal performance
 * - Smart interpolation and distance-based point generation
 * - Configurable trail styles and effects
 * - Memory-efficient with object pooling
 */
public class CursorTrailAdvanced extends Entity {
    private final CursorSprite cursor;
    private final TextureRegion trailTexture;

    // Trail data structures
    private final List<TrailVertex> trailVertices;
    private final List<Sprite> trailSprites;
    private final int maxVertices;

    // Movement tracking
    private float lastX, lastY;
    private boolean isFirstMove = true;
    private float accumulatedDistance = 0f;
    private long currentTime = 0;

    // Trail configuration
    private int maxTrailPoints = 30;
    private float trailLength = 0.8f; // seconds
    private float minDistance = 3f; // minimum pixels between trail points
    private float trailScale = 1.0f;
    private float trailWidth = 1.0f;
    private boolean enableGlow = false;
    private int trailStyle = 0; // 0=normal, 1=fade
    private float pointSpacing = 2.0f;

    // Performance optimization
    private static final int DEFAULT_MAX_VERTICES = 200;
    private static final float MIN_SPACING = 1f;
    private static final float MAX_SPACING = 5f;

    /**
     * Represents a vertex in the trail mesh
     */
    private static class TrailVertex {
        float x, y;
        long time;
        float alpha;
        float hue; // For rainbow effects
        float width; // Variable width support

        TrailVertex(float x, float y, long time, float alpha) {
            this.x = x;
            this.y = y;
            this.time = time;
            this.alpha = alpha;
            this.hue = 0f;
            this.width = 1f;
        }
    }

    public CursorTrailAdvanced(TextureRegion trailTexture, CursorSprite cursor) {
        this.trailTexture = trailTexture;
        this.cursor = cursor;
        this.trailVertices = new ArrayList<>();
        this.trailSprites = new ArrayList<>();
        this.trailLength = loadTrailLength();
        this.maxVertices = calculateMaxVertices();

        // Load configuration
        loadConfiguration();

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

    private void loadConfiguration() {
        // Load trail configuration from preferences
        trailStyle = Config.getInt("trailStyle", 0);
        enableGlow = Config.getBoolean("trailGlow", false);
        int spacingValue = Config.getInt("trailSpacing", 20); // Default 20 = 2.0
        int widthValue = Config.getInt("trailWidth", 10); // Default 10 = 1.0

        pointSpacing = spacingValue / 10.0f; // Convert to float
        trailWidth = widthValue / 10.0f; // Convert to float

        // Clamp values to reasonable ranges
        pointSpacing = Math.max(MIN_SPACING, Math.min(MAX_SPACING, pointSpacing));
        trailWidth = Math.max(0.5f, Math.min(3f, trailWidth));
    }

    private int calculateMaxVertices() {
        float fadeTimeMs = trailLength * 1000 * GameHelper.getSpeedMultiplier();
        int fps = 60;
        return Math.min(DEFAULT_MAX_VERTICES, (int) (fadeTimeMs / (1000f / fps) * 2f));
    }

    private void preCreateSprites() {
        for (int i = 0; i < maxVertices; i++) {
            Sprite sprite = new Sprite(0, 0, trailTexture);
            sprite.setVisible(false);
            sprite.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
            trailSprites.add(sprite);
            attachChild(sprite);
        }
    }

    /**
     * Update trail with new cursor position
     */
    public void updatePosition(float x, float y) {
        currentTime += 16; // Assume ~60fps

        if (isFirstMove) {
            lastX = x;
            lastY = y;
            isFirstMove = false;
            addVertex(x, y);
            return;
        }

        // Calculate distance and interpolate
        float dx = x - lastX;
        float dy = y - lastY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            accumulatedDistance += distance;

            // Add interpolated vertices for smooth trail
            int steps = Math.max(1, (int) (distance / pointSpacing));

            for (int i = 1; i <= steps; i++) {
                float t = i / (float) steps;
                float interpX = lastX + dx * t;
                float interpY = lastY + dy * t;

                addVertex(interpX, interpY);
            }

            lastX = x;
            lastY = y;
        }

        // Update trail vertices
        updateTrailVertices();
    }

    private void addVertex(float x, float y) {
        TrailVertex vertex = new TrailVertex(x, y, currentTime, 1.0f);

        // Set hue for rainbow effect (removed)

        // Variable width based on speed
        if (trailVertices.size() > 0) {
            TrailVertex last = trailVertices.get(trailVertices.size() - 1);
            float speed = (float) Math.sqrt((x - last.x) * (x - last.x) + (y - last.y) * (y - last.y));
            vertex.width = Math.max(0.5f, Math.min(1.5f, 1f + speed * 0.1f));
        }

        trailVertices.add(vertex);

        // Remove old vertices if we exceed maximum
        while (trailVertices.size() > maxVertices) {
            trailVertices.remove(0);
        }
    }

    private void updateTrailVertices() {
        long fadeTimeMs = (long) (trailLength * 1000 * GameHelper.getSpeedMultiplier());

        // Update alpha values and remove expired vertices
        for (int i = trailVertices.size() - 1; i >= 0; i--) {
            TrailVertex vertex = trailVertices.get(i);
            long age = currentTime - vertex.time;

            if (age > fadeTimeMs) {
                trailVertices.remove(i);
            } else {
                // Update alpha based on age
                float alpha = 1.0f - (age / (float) fadeTimeMs);
                vertex.alpha = Math.max(0, Math.min(1, alpha));

                // Apply style-specific effects
                if (trailStyle == 1) { // Fade style
                    vertex.alpha *= (1f - (i / (float) trailVertices.size()) * 0.5f);
                }
            }
        }

        // Update sprites
        updateTrailSprites();
    }

    private void updateTrailSprites() {
        // Ensure we have enough sprites
        while (trailSprites.size() < trailVertices.size()) {
            Sprite sprite = new Sprite(0, 0, trailTexture);
            sprite.setVisible(false);
            sprite.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
            trailSprites.add(sprite);
            attachChild(sprite);
        }

        // Update sprites to match trail vertices
        for (int i = 0; i < trailVertices.size(); i++) {
            if (i < trailSprites.size()) {
                TrailVertex vertex = trailVertices.get(i);
                Sprite sprite = trailSprites.get(i);

                // Position sprite at vertex
                float offsetX = -trailTexture.getWidth() / 2f;
                float offsetY = -trailTexture.getHeight() / 2f;
                sprite.setPosition(vertex.x + offsetX, vertex.y + offsetY);

                // Set alpha and scale
                sprite.setAlpha(vertex.alpha);
                float scale = cursor.baseSize * vertex.alpha * vertex.width * trailWidth;
                sprite.setScale(scale);

                // Apply color effects (rainbow removed)
                // Reset to white for all styles
                sprite.setColor(1, 1, 1);

                // Apply rotation if enabled
                if (OsuSkin.get().isRotateCursorTrail()) {
                    sprite.setRotation(cursor.getRotation());
                }

                sprite.setVisible(true);
            }
        }

        // Hide unused sprites
        for (int i = trailVertices.size(); i < trailSprites.size(); i++) {
            trailSprites.get(i).setVisible(false);
        }
    }

    /**
     * Update trail configuration
     */
    public void updateTrailLength() {
        this.trailLength = loadTrailLength();
        loadConfiguration();
    }

    /**
     * Update trail scale
     */
    public void updateTrailScale(float scale) {
        this.trailScale = scale;
        // Update existing sprites with gradient scaling
        for (int i = 0; i < trailSprites.size(); i++) {
            Sprite sprite = trailSprites.get(i);
            float progress = (float) i / trailSprites.size();
            float currentScale = trailScale * (1.0f - progress * 0.7f);
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
     * Reset trail state
     */
    public void reset() {
        trailVertices.clear();
        isFirstMove = true;
        accumulatedDistance = 0f;
        currentTime = 0;

        // Hide all sprites
        for (Sprite sprite : trailSprites) {
            sprite.setVisible(false);
        }
    }

    /**
     * Force update of all trail elements
     */
    public void update() {
        updateTrailVertices();
    }

    /**
     * Set trail style
     */
    public void setTrailStyle(int style) {
        this.trailStyle = Math.max(0, Math.min(1, style));
        Config.setString("trailStyle", String.valueOf(this.trailStyle));
    }

    /**
     * Set trail width multiplier
     */
    public void setTrailWidth(float width) {
        this.trailWidth = Math.max(0.5f, Math.min(3f, width));
        Config.setInt("trailWidth", (int)(this.trailWidth * 10)); // Convert to integer
    }

    /**
     * Set point spacing
     */
    public void setPointSpacing(float spacing) {
        this.pointSpacing = Math.max(MIN_SPACING, Math.min(MAX_SPACING, spacing));
        Config.setInt("trailSpacing", (int)(this.pointSpacing * 10)); // Convert to integer
    }

    /**
     * Get current trail statistics
     */
    public TrailStats getStats() {
        return new TrailStats(trailVertices.size(), maxVertices, trailLength, trailStyle);
    }

    /**
     * Trail statistics for debugging
     */
    public static class TrailStats {
        public final int activeVertices;
        public final int maxVertices;
        public final float trailLength;
        public final int trailStyle;

        TrailStats(int activeVertices, int maxVertices, float trailLength, int trailStyle) {
            this.activeVertices = activeVertices;
            this.maxVertices = maxVertices;
            this.trailLength = trailLength;
            this.trailStyle = trailStyle;
        }

        @Override
        public String toString() {
            return String.format("Trail: %d/%d vertices, length=%.2fs, style=%d",
                activeVertices, maxVertices, trailLength, trailStyle);
        }
    }
}
