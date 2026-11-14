package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.DashPathEffect;
import android.graphics.PointF;

/**
 * Rendering utilities for high-quality cursor movement visualization
 * Based on Java AWT RenderingHints: https://docs.oracle.com/javase/6/docs/api/java/awt/RenderingHints.html
 */
public class RenderingUtils {
    
    // Rendering quality constants (mirroring Java AWT RenderingHints)
    public static final int RENDERING_QUALITY_SPEED = 0;
    public static final int RENDERING_QUALITY_DEFAULT = 1;
    public static final int RENDERING_QUALITY_FASTER = 2;
    public static final int RENDERING_QUALITY_BETTER = 3;
    public static final int RENDERING_QUALITY_BEST = 4;
    
    // Anti-aliasing constants
    public static final boolean ANTIALIASING_ON = true;
    public static final boolean ANTIALIASING_OFF = false;
    public static final boolean ANTIALIASING_DEFAULT = true;
    
    // Text antialiasing constants
    public static final boolean TEXT_ANTIALIASING_ON = true;
    public static final boolean TEXT_ANTIALIASING_OFF = false;
    public static final boolean TEXT_ANTIALIASING_DEFAULT = true;
    
    // Fractional metrics constants
    public static final boolean FRACTIONALMETRICS_ON = true;
    public static final boolean FRACTIONALMETRICS_OFF = false;
    public static final boolean FRACTIONALMETRICS_DEFAULT = false;
    
    // Interpolation constants
    public static final boolean INTERPOLATION_BILINEAR = true;
    public static final boolean INTERPOLATION_NEAREST_NEIGHBOR = false;
    
    // Alpha compositing constants
    public static final int ALPHA_COMPOSITE_CLEAR = 0;
    public static final int ALPHA_COMPOSITE_SRC = 1;
    public static final int ALPHA_COMPOSITE_SRC_OVER = 2;
    public static final int ALPHA_COMPOSITE_DST_OVER = 3;
    public static final int ALPHA_COMPOSITE_SRC_IN = 4;
    public static final int ALPHA_COMPOSITE_DST_IN = 5;
    public static final int ALPHA_COMPOSITE_SRC_OUT = 6;
    public static final int ALPHA_COMPOSITE_DST_OUT = 7;
    public static final int ALPHA_COMPOSITE_SRC_ATOP = 8;
    public static final int ALPHA_COMPOSITE_DST_ATOP = 9;
    public static final int ALPHA_COMPOSITE_XOR = 10;
    
    /**
     * Apply rendering hints to Paint object based on Java AWT RenderingHints
     */
    public static void applyRenderingHints(Paint paint, int renderingQuality) {
        switch (renderingQuality) {
            case RENDERING_QUALITY_SPEED:
                paint.setAntiAlias(false);
                paint.setFilterBitmap(false);
                paint.setDither(false);
                break;
                
            case RENDERING_QUALITY_DEFAULT:
                paint.setAntiAlias(ANTIALIASING_DEFAULT);
                paint.setFilterBitmap(true);
                paint.setDither(true);
                break;
                
            case RENDERING_QUALITY_FASTER:
                paint.setAntiAlias(true);
                paint.setFilterBitmap(false);
                paint.setDither(true);
                break;
                
            case RENDERING_QUALITY_BETTER:
                paint.setAntiAlias(true);
                paint.setFilterBitmap(true);
                paint.setDither(true);
                break;
                
            case RENDERING_QUALITY_BEST:
                paint.setAntiAlias(true);
                paint.setFilterBitmap(true);
                paint.setDither(true);
                // Additional high-quality settings
                paint.setSubpixelText(true);
                // Android doesn't have hinting like Java AWT
                break;
                
            default:
                paint.setAntiAlias(true);
                paint.setFilterBitmap(true);
                paint.setDither(true);
                break;
        }
    }
    
    /**
     * Apply text rendering hints to Paint object
     */
    public static void applyTextRenderingHints(Paint paint, boolean antialiasing, boolean fractionalMetrics) {
        paint.setAntiAlias(antialiasing);
        paint.setSubpixelText(antialiasing);
        // Android doesn't have hinting like Java AWT
        
        // Fractional metrics affect text positioning accuracy
        if (fractionalMetrics) {
            paint.setTextAlign(Paint.Align.LEFT);
            // Android doesn't have fractional metrics like Java AWT
            paint.setLetterSpacing(0);
        }
    }
    
    /**
     * Create high-quality cursor trail Paint
     */
    public static Paint createCursorTrailPaint(int color, float strokeWidth, int quality) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        
        applyRenderingHints(paint, quality);
        
        // Add subtle glow effect for better visibility
        paint.setShadowLayer(2f, 0f, 0f, color & 0x80FFFFFF);
        
        return paint;
    }
    
    /**
     * Create smooth cursor position Paint
     */
    public static Paint createCursorPositionPaint(int color, int quality) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        
        applyRenderingHints(paint, quality);
        applyTextRenderingHints(paint, true, false);
        
        return paint;
    }
    
    /**
     * Create movement path visualization Paint
     */
    public static Paint createMovementPathPaint(int color, float strokeWidth, boolean dashed, int quality) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        
        if (dashed) {
            float[] dashPattern = {10f, 5f};
            paint.setPathEffect(new DashPathEffect(dashPattern, 0f));
        }
        
        applyRenderingHints(paint, quality);
        
        return paint;
    }
    
    /**
     * Draw smooth bezier curve with high quality rendering
     */
    public static void drawSmoothBezierCurve(Canvas canvas, Path path, Paint paint, PointF[] points) {
        if (points.length < 2) return;
        
        path.reset();
        path.moveTo(points[0].x, points[0].y);
        
        if (points.length == 2) {
            // Simple line
            path.lineTo(points[1].x, points[1].y);
        } else if (points.length == 3) {
            // Quadratic bezier
            path.quadTo(points[1].x, points[1].y, points[2].x, points[2].y);
        } else if (points.length == 4) {
            // Cubic bezier
            path.cubicTo(points[1].x, points[1].y, points[2].x, points[2].y, points[3].x, points[3].y);
        } else {
            // Complex bezier - use smooth curve through all points
            for (int i = 1; i < points.length - 1; i++) {
                PointF control = points[i];
                PointF next = points[i + 1];
                PointF mid = new PointF((control.x + next.x) / 2f, (control.y + next.y) / 2f);
                path.quadTo(control.x, control.y, mid.x, mid.y);
            }
            path.lineTo(points[points.length - 1].x, points[points.length - 1].y);
        }
        
        canvas.drawPath(path, paint);
    }
    
    /**
     * Draw cursor with subpixel precision
     */
    public static void drawCursorWithSubpixelPrecision(Canvas canvas, float x, float y, float radius, Paint paint) {
        // Use subpixel positioning for smooth movement
        canvas.drawCircle(x, y, radius, paint);
        
        // Add inner glow for better visibility
        Paint glowPaint = new Paint(paint);
        glowPaint.setAlpha((int) (paint.getAlpha() * 0.3f));
        glowPaint.setStrokeWidth(paint.getStrokeWidth() * 0.5f);
        canvas.drawCircle(x, y, radius * 1.2f, glowPaint);
    }
    
    /**
     * Apply alpha compositing mode (mirroring Java AWT AlphaComposite)
     */
    public static void applyAlphaCompositing(Paint paint, int compositeMode) {
        switch (compositeMode) {
            case ALPHA_COMPOSITE_CLEAR:
                paint.setXfermode(null); // Clear not directly supported
                break;
            case ALPHA_COMPOSITE_SRC:
                paint.setXfermode(null); // Source only
                break;
            case ALPHA_COMPOSITE_SRC_OVER:
                paint.setXfermode(null); // Default
                break;
            case ALPHA_COMPOSITE_DST_OVER:
                // Destination over - would need custom implementation
                break;
            case ALPHA_COMPOSITE_SRC_IN:
                // Source in - would need custom implementation
                break;
            case ALPHA_COMPOSITE_DST_IN:
                // Destination in - would need custom implementation
                break;
            case ALPHA_COMPOSITE_SRC_OUT:
                // Source out - would need custom implementation
                break;
            case ALPHA_COMPOSITE_DST_OUT:
                // Destination out - would need custom implementation
                break;
            case ALPHA_COMPOSITE_SRC_ATOP:
                // Source atop - would need custom implementation
                break;
            case ALPHA_COMPOSITE_DST_ATOP:
                // Destination atop - would need custom implementation
                break;
            case ALPHA_COMPOSITE_XOR:
                paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.XOR));
                break;
            default:
                paint.setXfermode(null);
                break;
        }
    }
    
    /**
     * Get optimal rendering quality based on device capabilities
     */
    public static int getOptimalRenderingQuality() {
        // In a real implementation, this would check device capabilities
        // For now, return BEST for high-end rendering
        return RENDERING_QUALITY_BEST;
    }
    
    /**
     * Create bounds for cursor movement area
     */
    public static RectF createMovementBounds(PointF[] points, float padding) {
        if (points.length == 0) {
            return new RectF();
        }
        
        float minX = points[0].x;
        float maxX = points[0].x;
        float minY = points[0].y;
        float maxY = points[0].y;
        
        for (PointF point : points) {
            minX = Math.min(minX, point.x);
            maxX = Math.max(maxX, point.x);
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
        }
        
        return new RectF(
            minX - padding,
            minY - padding,
            maxX + padding,
            maxY + padding
        );
    }
}
