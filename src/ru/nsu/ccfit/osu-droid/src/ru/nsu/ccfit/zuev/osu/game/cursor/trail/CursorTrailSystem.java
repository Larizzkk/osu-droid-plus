package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.trail;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Color;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.RenderingUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.MathUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.Easing;
import ru.nsu.ccfit.zuev.osuplusplus.Config;
import ru.nsu.ccfit.zuev.osuplusplus.game.effects.GlowEffectManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Cursor trail system - exact port from danser-go cursor trail implementation
 * Based on https://github.com/wieku/danser-go/blob/master/app/graphics/dansercursor.go
 */
public class CursorTrailSystem {
    
    // Trail configuration (mirroring danser-go settings)
    public enum TrailStyle {
        UNIFIED_COLOR,    // Style 1
        DISTANCE_RAINBOW, // Style 2
        TIME_RAINBOW,     // Style 3
        GRADIENT          // Style 4
    }
    
    // Trail settings from danser-go
    private TrailStyle trailStyle = TrailStyle.UNIFIED_COLOR;
    private float style23Speed = 0.18f; // degrees per second
    private float style4Shift = 0.5f;   // hue shift
    private float trailScale = Config.getTrailSize();
    private float trailEndScale = 0.4f;
    private float trailDensity = Config.getTrailWidth();
    private int trailMaxLength = (int)(Config.getTrailLength() * 1000); // Convert from config scale
    private float trailRemoveSpeed = 1.0f;
    private float glowEndScale = Config.getGlowEndScale();
    private float innerLengthMult = Config.getInnerLengthMult();
    private boolean enableTrailGlow = Config.isTrailGlowEnabled();
    private boolean additiveBlending = Config.isAdditiveBlendingEnabled();
    
    // Trail data
    private List<PointF> trailPoints = new ArrayList<>();
    private List<Float> trailColors = new ArrayList<>();
    private List<Long> trailTimes = new ArrayList<>();
    private PointF lastPosition = new PointF(0, 0);
    private float hueBase = 0f;
    private float removeCounter = 0f;
    
    // Paint objects for trail rendering
    private Paint trailPaint;
    private Paint glowPaint;
    private GlowEffectManager glowManager;
    
    public CursorTrailSystem() {
        initializePaints();
        glowManager = GlowEffectManager.getInstance();
    }
    
    private void initializePaints() {
        // Main trail paint
        trailPaint = RenderingUtils.createCursorTrailPaint(
            0xFFFFFFFF, // White color
            4.0f,
            RenderingUtils.RENDERING_QUALITY_BEST
        );
        
        // Glow paint
        glowPaint = RenderingUtils.createCursorPositionPaint(
            0x80FFFFFF, // Semi-transparent white
            RenderingUtils.RENDERING_QUALITY_BEST
        );
    }
    
    /**
     * Update trail system - exact logic from danser-go
     */
    public void update(float deltaTime, PointF currentPosition) {
        // Update trail length from config
        trailMaxLength = (int)(Config.getTrailLength() * 1000);
        
        // Update time-based rainbow
        if (trailStyle == TrailStyle.TIME_RAINBOW) {
            hueBase += style23Speed / 360f * deltaTime;
            if (hueBase > 1f) {
                hueBase -= 1f;
            }
        }
        
        // Calculate distance and add new trail points
        float distance = MathUtils.dst(currentPosition, lastPosition);
        float pointDistance = 1f / trailDensity;
        
        boolean dirtyLocal = false;
        
        // Add intermediate points if distance is large enough
        while (distance >= pointDistance && trailPoints.size() < trailMaxLength) {
            PointF temp = MathUtils.lerp(lastPosition, currentPosition, pointDistance / distance);
            trailPoints.add(temp);
            trailColors.add(hueBase);
            
            if (trailStyle == TrailStyle.DISTANCE_RAINBOW) {
                hueBase += style23Speed / 360f * pointDistance;
                if (hueBase > 1f) {
                    hueBase -= 1f;
                }
            }
            
            lastPosition = temp;
            distance = MathUtils.dst(currentPosition, lastPosition);
            dirtyLocal = true;
        }
        
        // Remove old trail points
        if (trailPoints.size() > 0) {
            removeCounter += (trailPoints.size() + 3) / (360f / deltaTime) * trailRemoveSpeed;
            int times = (int) Math.floor(removeCounter);
            
            if (times > 0) {
                int removeCount = Math.min(times, trailPoints.size());
                for (int i = 0; i < removeCount; i++) {
                    trailPoints.remove(0);
                    trailColors.remove(0);
                }
                removeCounter -= times;
                dirtyLocal = true;
            }
        }
        
        // Limit trail length
        int lengthAdjusted = (int) (trailMaxLength * trailDensity);
        if (trailPoints.size() > lengthAdjusted) {
            int excess = trailPoints.size() - lengthAdjusted;
            for (int i = 0; i < excess; i++) {
                trailPoints.remove(0);
                trailColors.remove(0);
            }
        }
        
        // Update last position
        if (!currentPosition.equals(lastPosition)) {
            lastPosition = new PointF(currentPosition.x, currentPosition.y);
        }
    }
    
    /**
     * Draw trail - exact rendering logic from danser-go
     */
    public void drawTrail(Canvas canvas, float cursorSize, int baseColor) {
        if (trailPoints.size() == 0) return;
        
        // Apply trail scale
        float scaledSize = cursorSize * trailScale;
        
        // Draw glow if enabled
        if (enableTrailGlow) {
            drawGlowTrail(canvas, scaledSize, baseColor);
        }
        
        // Draw main trail
        drawMainTrail(canvas, scaledSize, baseColor);
    }
    
    private void drawMainTrail(Canvas canvas, float cursorSize, int baseColor) {
        float innerLength = cursorSize * (16f / 18f);
        
        for (int i = 0; i < trailPoints.size(); i++) {
            PointF point = trailPoints.get(i);
            float progress = (float) i / trailPoints.size();
            
            // Calculate size with end scale
            float size = innerLength * MathUtils.lerp(trailScale, trailEndScale, progress);
            
            // Calculate color based on trail style
            int color = calculateTrailColor(baseColor, i, progress);
            
            // Draw trail point
            Paint paint = new Paint(trailPaint);
            paint.setColor(color);
            paint.setAlpha((int) (255 * progress)); // Fade out older points
            
            canvas.drawCircle(point.x, point.y, size, paint);
        }
    }
    
    private void drawGlowTrail(Canvas canvas, float cursorSize, int baseColor) {
        if (!enableTrailGlow) return;
        
        float intensity = Config.getGlowIntensity();
        
        for (int i = 0; i < trailPoints.size(); i++) {
            PointF point = trailPoints.get(i);
            float progress = (float) i / trailPoints.size();
            
            // Apply inner length multiplier
            if (progress < innerLengthMult) {
                continue;
            }
            
            // Calculate size with glow end scale
            float size = cursorSize * MathUtils.lerp(trailScale, glowEndScale, progress);
            
            // Calculate glow color
            int glowColor = calculateGlowColor(baseColor, i, progress);
            
            // Use improved glow effect
            glowManager.drawTrailGlow(canvas, point, size, glowColor, progress, intensity);
        }
    }
    
    private int calculateTrailColor(int baseColor, int index, float progress) {
        switch (trailStyle) {
            case UNIFIED_COLOR:
                return baseColor;
                
            case DISTANCE_RAINBOW:
            case TIME_RAINBOW:
                // Apply hue shift
                float hue = trailColors.get(index) + hueBase;
                return shiftHue(baseColor, hue * 360f);
                
            case GRADIENT:
                // Apply gradient shift
                float shift = style4Shift * (1f - progress);
                return shiftHue(baseColor, shift * 360f);
                
            default:
                return baseColor;
        }
    }
    
    private int calculateGlowColor(int baseColor, int index, float progress) {
        int color = calculateTrailColor(baseColor, index, progress);
        
        // Make glow more transparent
        int alpha = (int) (Color.alpha(color) * 0.5f);
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
    
    private int shiftHue(int color, float hueShift) {
        // Simple hue shift implementation
        // In a real implementation, this would use HSV color space
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[0] = (hsv[0] + hueShift) % 360f;
        return Color.HSVToColor(hsv);
    }
    
    /**
     * Clear all trail data
     */
    public void clear() {
        trailPoints.clear();
        trailColors.clear();
        trailTimes.clear();
        lastPosition = new PointF(0, 0);
        hueBase = 0f;
        removeCounter = 0f;
    }
    
    /**
     * Attach trail system to scene (for rendering)
     */
    public void attachToScene(org.anddev.andengine.entity.scene.Scene scene) {
        // The trail system doesn't need to attach to the scene directly
        // It will be drawn by the AutoCursor's drawTrail method
        // This method is kept for compatibility
    }
    
    /**
     * Update glow settings from config
     */
    public void updateGlowSettings() {
        this.glowEndScale = Config.getGlowEndScale();
        this.innerLengthMult = Config.getInnerLengthMult();
        this.enableTrailGlow = Config.isTrailGlowEnabled();
        this.additiveBlending = Config.isAdditiveBlendingEnabled();
    }
    
    /**
     * Get trail statistics
     */
    public TrailStats getStats() {
        return new TrailStats(
            trailPoints.size(),
            trailStyle,
            hueBase,
            trailMaxLength
        );
    }
    
    // Configuration methods
    public void setTrailStyle(TrailStyle style) { this.trailStyle = style; }
    public void setStyle23Speed(float speed) { this.style23Speed = speed; }
    public void setStyle4Shift(float shift) { this.style4Shift = shift; }
    public void setTrailScale(float scale) { this.trailScale = scale; }
    public void setTrailEndScale(float scale) { this.trailEndScale = scale; }
    public void setTrailDensity(float density) { this.trailDensity = density; }
    public void setTrailMaxLength(int length) { this.trailMaxLength = length; }
    public void setTrailRemoveSpeed(float speed) { this.trailRemoveSpeed = speed; }
    public void setEnableTrailGlow(boolean enable) { this.enableTrailGlow = enable; }
    public void setAdditiveBlending(boolean enable) { this.additiveBlending = enable; }
    
    /**
     * Trail statistics
     */
    public static class TrailStats {
        public final int pointCount;
        public final TrailStyle style;
        public final float hueBase;
        public final int maxLength;
        
        public TrailStats(int pointCount, TrailStyle style, float hueBase, int maxLength) {
            this.pointCount = pointCount;
            this.style = style;
            this.hueBase = hueBase;
            this.maxLength = maxLength;
        }
        
        @Override
        public String toString() {
            return String.format(
                "TrailStats[points=%d, style=%s, hue=%.2f, maxLength=%d]",
                pointCount, style, hueBase, maxLength
            );
        }
    }
}
