package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.visualization;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.RenderingUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.MathUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * High-quality cursor visualization system based on Java AWT RenderingHints
 * Provides smooth, subpixel-accurate cursor movement visualization
 */
public class CursorVisualizer {
    
    // Visualization settings
    private boolean showMovementTrail = true;
    private boolean showMovementPath = true;
    private boolean showCursorGlow = true;
    private boolean showMovementBounds = false;
    private int renderingQuality = RenderingUtils.RENDERING_QUALITY_BEST;
    
    // Trail system
    private List<PointF> trailPoints = new ArrayList<>();
    private static final int MAX_TRAIL_POINTS = 50;
    private static final float TRAIL_FADE_TIME = 2000f; // milliseconds
    
    // Paint objects for high-quality rendering
    private Paint trailPaint;
    private Paint pathPaint;
    private Paint glowPaint;
    private Paint boundsPaint;
    
    // Movement tracking
    private PointF lastPosition = new PointF(0, 0);
    private float lastUpdateTime = 0;
    private float totalMovementDistance = 0;
    
    public CursorVisualizer() {
        initializePaints();
    }
    
    private void initializePaints() {
        // High-quality trail paint
        trailPaint = RenderingUtils.createCursorTrailPaint(
            0x80FFFFFF, // Semi-transparent white
            4.0f,
            renderingQuality
        );
        
        // Movement path paint
        pathPaint = RenderingUtils.createMovementPathPaint(
            0x40FFFFFF, // More transparent white
            2.0f,
            true, // Dashed path
            renderingQuality
        );
        
        // Cursor glow paint
        glowPaint = RenderingUtils.createCursorPositionPaint(
            0x60FFFFFF, // Very transparent white
            renderingQuality
        );
        
        // Movement bounds paint
        boundsPaint = RenderingUtils.createMovementPathPaint(
            0x20FFFFFF, // Even more transparent
            1.0f,
            false,
            renderingQuality
        );
    }
    
    /**
     * Update cursor position for visualization
     */
    public void updateCursorPosition(float x, float y, float currentTime) {
        PointF currentPos = new PointF(x, y);
        
        // Calculate movement metrics
        if (lastPosition.x != 0 || lastPosition.y != 0) {
            float distance = MathUtils.dst(lastPosition, currentPos);
            totalMovementDistance += distance;
        }
        
        // Update trail points
        if (showMovementTrail) {
            trailPoints.add(new PointF(x, y));
            
            // Remove old trail points
            while (trailPoints.size() > MAX_TRAIL_POINTS) {
                trailPoints.remove(0);
            }
        }
        
        lastPosition = currentPos;
        lastUpdateTime = currentTime;
    }
    
    /**
     * Draw cursor visualization with high-quality rendering
     */
    public void drawVisualization(Canvas canvas, float cursorX, float cursorY, float cursorRadius, int cursorColor) {
        // Apply rendering hints
        canvas.save();
        
        // Draw movement trail
        if (showMovementTrail && trailPoints.size() > 1) {
            drawMovementTrail(canvas);
        }
        
        // Draw movement path
        if (showMovementPath && trailPoints.size() > 1) {
            drawMovementPath(canvas);
        }
        
        // Draw cursor with enhanced effects
        drawCursorEnhanced(canvas, cursorX, cursorY, cursorRadius, cursorColor);
        
        // Draw movement bounds
        if (showMovementBounds) {
            drawMovementBounds(canvas);
        }
        
        canvas.restore();
    }
    
    /**
     * Draw movement trail with fading effect
     */
    private void drawMovementTrail(Canvas canvas) {
        if (trailPoints.size() < 2) return;
        
        Path trailPath = new Path();
        
        // Create smooth trail path
        trailPath.moveTo(trailPoints.get(0).x, trailPoints.get(0).y);
        
        for (int i = 1; i < trailPoints.size(); i++) {
            PointF current = trailPoints.get(i);
            PointF previous = trailPoints.get(i - 1);
            
            // Use quadratic bezier for smooth trail
            PointF control = new PointF(
                (previous.x + current.x) / 2f,
                (previous.y + current.y) / 2f
            );
            
            trailPath.quadTo(control.x, control.y, current.x, current.y);
        }
        
        // Draw trail with fading alpha
        float alpha = calculateTrailAlpha();
        int originalAlpha = trailPaint.getAlpha();
        trailPaint.setAlpha((int) (originalAlpha * alpha));
        canvas.drawPath(trailPath, trailPaint);
        trailPaint.setAlpha(originalAlpha);
    }
    
    /**
     * Draw movement path visualization
     */
    private void drawMovementPath(Canvas canvas) {
        if (trailPoints.size() < 2) return;
        
        PointF[] points = trailPoints.toArray(new PointF[0]);
        RenderingUtils.drawSmoothBezierCurve(canvas, new Path(), pathPaint, points);
    }
    
    /**
     * Draw cursor with enhanced visual effects
     */
    private void drawCursorEnhanced(Canvas canvas, float x, float y, float radius, int color) {
        // Draw cursor glow
        if (showCursorGlow) {
            RenderingUtils.drawCursorWithSubpixelPrecision(canvas, x, y, radius * 1.3f, glowPaint);
        }
        
        // Draw main cursor
        android.graphics.Paint cursorPaint = RenderingUtils.createCursorPositionPaint(
            color,
            renderingQuality
        );
        
        RenderingUtils.drawCursorWithSubpixelPrecision(canvas, x, y, radius, cursorPaint);
        
        // Draw movement indicator
        if (totalMovementDistance > 100) {
            drawMovementIndicator(canvas, x, y, radius);
        }
    }
    
    /**
     * Draw movement indicator around cursor
     */
    private void drawMovementIndicator(Canvas canvas, float x, float y, float radius) {
        float speed = calculateMovementSpeed();
        float indicatorRadius = radius * (1.2f + speed * 0.3f);
        
        Paint indicatorPaint = new Paint(pathPaint);
        indicatorPaint.setStyle(Paint.Style.STROKE);
        indicatorPaint.setStrokeWidth(2f);
        indicatorPaint.setAlpha((int) (128 * (1f - speed))); // Fade based on speed
        
        canvas.drawCircle(x, y, indicatorRadius, indicatorPaint);
    }
    
    /**
     * Draw movement bounds visualization
     */
    private void drawMovementBounds(Canvas canvas) {
        if (trailPoints.size() < 2) return;
        
        RectF bounds = RenderingUtils.createMovementBounds(
            trailPoints.toArray(new PointF[0]), 
            20f
        );
        
        boundsPaint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(bounds, boundsPaint);
    }
    
    /**
     * Calculate trail alpha based on time
     */
    private float calculateTrailAlpha() {
        float timeSinceLastUpdate = System.currentTimeMillis() - lastUpdateTime;
        return Math.max(0f, 1f - (timeSinceLastUpdate / TRAIL_FADE_TIME));
    }
    
    /**
     * Calculate current movement speed
     */
    private float calculateMovementSpeed() {
        if (trailPoints.size() < 2) return 0f;
        
        PointF recent = trailPoints.get(trailPoints.size() - 1);
        PointF previous = trailPoints.get(Math.max(0, trailPoints.size() - 5));
        
        float distance = MathUtils.dst(previous, recent);
        float timeDiff = 5f * 16.67f; // Assuming 60 FPS
        
        return Math.min(1f, distance / (timeDiff * 100f)); // Normalize speed
    }
    
    /**
     * Get visualization statistics
     */
    public CursorVisualizationStats getStats() {
        return new CursorVisualizationStats(
            trailPoints.size(),
            totalMovementDistance,
            calculateMovementSpeed(),
            renderingQuality
        );
    }
    
    /**
     * Clear all visualization data
     */
    public void clear() {
        trailPoints.clear();
        lastPosition = new PointF(0, 0);
        totalMovementDistance = 0;
        lastUpdateTime = 0;
    }
    
    // Configuration methods
    public void setShowMovementTrail(boolean show) { this.showMovementTrail = show; }
    public void setShowMovementPath(boolean show) { this.showMovementPath = show; }
    public void setShowCursorGlow(boolean show) { this.showCursorGlow = show; }
    public void setShowMovementBounds(boolean show) { this.showMovementBounds = show; }
    public void setRenderingQuality(int quality) { 
        this.renderingQuality = quality;
        initializePaints(); // Recreate paints with new quality
    }
    
    /**
     * Statistics for cursor visualization
     */
    public static class CursorVisualizationStats {
        public final int trailPointCount;
        public final float totalDistance;
        public final float currentSpeed;
        public final int renderingQuality;
        
        public CursorVisualizationStats(int trailPointCount, float totalDistance, float currentSpeed, int renderingQuality) {
            this.trailPointCount = trailPointCount;
            this.totalDistance = totalDistance;
            this.currentSpeed = currentSpeed;
            this.renderingQuality = renderingQuality;
        }
        
        @Override
        public String toString() {
            return String.format(
                "CursorStats[trail=%d, distance=%.1f, speed=%.2f, quality=%d]",
                trailPointCount, totalDistance, currentSpeed, renderingQuality
            );
        }
    }
}
