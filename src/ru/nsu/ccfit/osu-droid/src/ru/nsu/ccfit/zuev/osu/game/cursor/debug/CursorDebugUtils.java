package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.debug;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import ru.nsu.ccfit.zuev.osuplusplus.game.cursor.math.CurveUtils;
import ru.nsu.ccfit.zuev.osuplusplus.game.cursor.state.CursorStateTracker;
import java.util.ArrayList;

/**
 * CursorDebugUtils - Debug utilities for cursor movement visualization
 * Helps identify movement issues and optimize performance
 */
public class CursorDebugUtils {
    
    // Debug flags
    private static boolean DEBUG_ENABLED = false;
    private static boolean DRAW_CURVES = true;
    private static boolean DRAW_CONTROL_POINTS = true;
    private static boolean DRAW_VELOCITY = true;
    private static boolean DRAW_PREDICTION = true;
    private static boolean DRAW_PERFORMANCE = true;
    
    // Paint objects (reused to avoid GC)
    private static final Paint curvePaint = new Paint();
    private static final Paint controlPointPaint = new Paint();
    private static final Paint velocityPaint = new Paint();
    private static final Paint predictionPaint = new Paint();
    private static final Paint textPaint = new Paint();
    private static final Paint performancePaint = new Paint();
    
    // Performance tracking
    private static long lastFrameTime = 0;
    private static int frameCount = 0;
    private static float averageFPS = 60f;
    private static long gcCount = 0;
    private static ArrayList<Long> frameTimes = new ArrayList<>(60);
    
    // Debug data
    private static ArrayList<PointF> debugPoints = new ArrayList<>();
    private static ArrayList<PointF> controlPoints = new ArrayList<>();
    private static PointF lastDebugPosition = new PointF();
    
    static {
        // Initialize paints
        curvePaint.setColor(Color.argb(128, 255, 255, 0)); // Yellow curve
        curvePaint.setStyle(Paint.Style.STROKE);
        curvePaint.setStrokeWidth(2f);
        curvePaint.setAntiAlias(true);
        
        controlPointPaint.setColor(Color.argb(200, 255, 0, 0)); // Red control points
        controlPointPaint.setStyle(Paint.Style.FILL);
        controlPointPaint.setAntiAlias(true);
        
        velocityPaint.setColor(Color.argb(128, 0, 255, 0)); // Green velocity
        velocityPaint.setStyle(Paint.Style.STROKE);
        velocityPaint.setStrokeWidth(3f);
        velocityPaint.setAntiAlias(true);
        
        predictionPaint.setColor(Color.argb(128, 0, 0, 255)); // Blue prediction
        predictionPaint.setStyle(Paint.Style.STROKE);
        predictionPaint.setStrokeWidth(1f);
        predictionPaint.setAntiAlias(true);
        
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24f);
        textPaint.setAntiAlias(true);
        
        performancePaint.setColor(Color.CYAN);
        performancePaint.setTextSize(20f);
        performancePaint.setAntiAlias(true);
    }
    
    /**
     * Enable/disable debug mode
     */
    public static void setDebugEnabled(boolean enabled) {
        DEBUG_ENABLED = enabled;
    }
    
    /**
     * Set debug flags
     */
    public static void setDebugFlags(boolean drawCurves, boolean drawControlPoints, 
                                   boolean drawVelocity, boolean drawPrediction, 
                                   boolean drawPerformance) {
        DRAW_CURVES = drawCurves;
        DRAW_CONTROL_POINTS = drawControlPoints;
        DRAW_VELOCITY = drawVelocity;
        DRAW_PREDICTION = drawPrediction;
        DRAW_PERFORMANCE = drawPerformance;
    }
    
    /**
     * Draw debug information on canvas
     */
    public static void drawDebugInfo(Canvas canvas, PointF cursorPosition, 
                                   CursorStateTracker stateTracker, 
                                   CurveUtils.BezierCurve currentCurve) {
        if (!DEBUG_ENABLED) {
            return;
        }
        
        // Update performance metrics
        updatePerformanceMetrics();
        
        // Draw current curve
        if (DRAW_CURVES && currentCurve != null) {
            drawBezierCurve(canvas, currentCurve);
        }
        
        // Draw control points
        if (DRAW_CONTROL_POINTS && !controlPoints.isEmpty()) {
            drawControlPoints(canvas);
        }
        
        // Draw velocity vector
        if (DRAW_VELOCITY && stateTracker != null) {
            drawVelocityVector(canvas, cursorPosition, stateTracker.getCurrentVelocity());
        }
        
        // Draw prediction
        if (DRAW_PREDICTION && stateTracker != null) {
            drawPredictionPath(canvas, stateTracker);
        }
        
        // Draw performance info
        if (DRAW_PERFORMANCE) {
            drawPerformanceInfo(canvas);
        }
        
        // Draw debug text
        drawDebugText(canvas, cursorPosition, stateTracker);
    }
    
    /**
     * Draw Bezier curve with sampling
     */
    private static void drawBezierCurve(Canvas canvas, CurveUtils.BezierCurve curve) {
        PointF prevPoint = new PointF();
        curve.getPointAt(0f, prevPoint);
        
        // Sample curve for smooth drawing
        int samples = 50;
        for (int i = 1; i <= samples; i++) {
            float t = (float) i / samples;
            PointF currentPoint = new PointF();
            curve.getPointAt(t, currentPoint);
            
            canvas.drawLine(prevPoint.x, prevPoint.y, currentPoint.x, currentPoint.y, curvePaint);
            prevPoint.set(currentPoint);
        }
    }
    
    /**
     * Draw control points
     */
    private static void drawControlPoints(Canvas canvas) {
        for (PointF point : controlPoints) {
            canvas.drawCircle(point.x, point.y, 8f, controlPointPaint);
        }
    }
    
    /**
     * Draw velocity vector
     */
    private static void drawVelocityVector(Canvas canvas, PointF position, PointF velocity) {
        if (velocity == null) return;
        
        // Scale velocity for visualization
        float scale = 0.1f;
        PointF end = new PointF(
            position.x + velocity.x * scale,
            position.y + velocity.y * scale
        );
        
        canvas.drawLine(position.x, position.y, end.x, end.y, velocityPaint);
        
        // Draw arrowhead
        float angle = (float) Math.atan2(velocity.y, velocity.x);
        float arrowLength = 10f;
        float arrowAngle = 0.5f; // 30 degrees
        
        PointF arrow1 = new PointF(
            end.x - arrowLength * (float) Math.cos(angle - arrowAngle),
            end.y - arrowLength * (float) Math.sin(angle - arrowAngle)
        );
        
        PointF arrow2 = new PointF(
            end.x - arrowLength * (float) Math.cos(angle + arrowAngle),
            end.y - arrowLength * (float) Math.sin(angle + arrowAngle)
        );
        
        canvas.drawLine(end.x, end.y, arrow1.x, arrow1.y, velocityPaint);
        canvas.drawLine(end.x, end.y, arrow2.x, arrow2.y, velocityPaint);
    }
    
    /**
     * Draw prediction path
     */
    private static void drawPredictionPath(Canvas canvas, CursorStateTracker stateTracker) {
        if (stateTracker == null) return;
        
        PointF currentPos = stateTracker.getSmoothedPosition();
        PointF prevPoint = new PointF(currentPos);
        
        // Draw predicted positions
        for (int i = 1; i <= 10; i++) {
            PointF predicted = stateTracker.predictNextPosition(i * 50f); // 50ms intervals
            
            canvas.drawLine(prevPoint.x, prevPoint.y, predicted.x, predicted.y, predictionPaint);
            prevPoint.set(predicted);
        }
    }
    
    /**
     * Draw performance information
     */
    private static void drawPerformanceInfo(Canvas canvas) {
        String perfText = String.format("FPS: %.1f | GC: %d | Frames: %d", 
                                      averageFPS, gcCount, frameCount);
        
        // Draw background for better readability
        RectF background = new RectF(10, 10, 300, 40);
        canvas.drawRect(background, performancePaint);
        
        canvas.drawText(perfText, 20, 35, performancePaint);
    }
    
    /**
     * Draw debug text information
     */
    private static void drawDebugText(Canvas canvas, PointF cursorPosition, CursorStateTracker stateTracker) {
        int yOffset = 60;
        
        // Cursor position
        String posText = String.format("Pos: (%.1f, %.1f)", cursorPosition.x, cursorPosition.y);
        canvas.drawText(posText, 20, yOffset, textPaint);
        yOffset += 25;
        
        // State tracker info
        if (stateTracker != null) {
            String stateText = stateTracker.getDebugInfo();
            String[] lines = stateText.split(", ");
            
            for (String line : lines) {
                canvas.drawText(line, 20, yOffset, textPaint);
                yOffset += 25;
            }
        }
        
        // Memory info
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        float memoryUsage = (float) usedMemory / maxMemory * 100f;
        
        String memText = String.format("Memory: %.1f%% (%.1fMB / %.1fMB)", 
                                     memoryUsage, usedMemory / 1024f / 1024f, maxMemory / 1024f / 1024f);
        canvas.drawText(memText, 20, yOffset, textPaint);
    }
    
    /**
     * Update performance metrics
     */
    private static void updatePerformanceMetrics() {
        long currentTime = System.currentTimeMillis();
        
        if (lastFrameTime > 0) {
            long frameTime = currentTime - lastFrameTime;
            frameTimes.add(frameTime);
            
            if (frameTimes.size() > 60) {
                frameTimes.remove(0);
            }
            
            // Calculate average FPS
            long totalFrameTime = 0;
            for (Long time : frameTimes) {
                totalFrameTime += time;
            }
            
            if (totalFrameTime > 0) {
                averageFPS = 1000f * frameTimes.size() / totalFrameTime;
            }
        }
        
        lastFrameTime = currentTime;
        frameCount++;
        
        // Check for GC
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long prevUsedMemory = usedMemory;
        
        // Simple GC detection (memory usage drops significantly)
        if (frameCount % 60 == 0) { // Check every 60 frames
            if (prevUsedMemory > usedMemory * 1.1f) {
                gcCount++;
            }
        }
    }
    
    /**
     * Add debug point for path visualization
     */
    public static void addDebugPoint(PointF point) {
        if (DEBUG_ENABLED) {
            debugPoints.add(new PointF(point));
            
            // Limit debug points to prevent memory issues
            if (debugPoints.size() > 1000) {
                debugPoints.remove(0);
            }
        }
    }
    
    /**
     * Add control point for curve visualization
     */
    public static void addControlPoint(PointF point) {
        if (DEBUG_ENABLED) {
            controlPoints.add(new PointF(point));
            
            if (controlPoints.size() > 50) {
                controlPoints.remove(0);
            }
        }
    }
    
    /**
     * Clear debug data
     */
    public static void clearDebugData() {
        debugPoints.clear();
        controlPoints.clear();
        frameTimes.clear();
        frameCount = 0;
        gcCount = 0;
        averageFPS = 60f;
    }
    
    /**
     * Get performance report
     */
    public static String getPerformanceReport() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        float memoryUsage = (float) usedMemory / maxMemory * 100f;
        
        return String.format("Performance Report:\n" +
                           "Average FPS: %.1f\n" +
                           "Frame Count: %d\n" +
                           "GC Count: %d\n" +
                           "Memory Usage: %.1f%% (%.1fMB / %.1fMB)\n" +
                           "Debug Points: %d\n" +
                           "Control Points: %d",
                           averageFPS, frameCount, gcCount, memoryUsage,
                           usedMemory / 1024f / 1024f, maxMemory / 1024f / 1024f,
                           debugPoints.size(), controlPoints.size());
    }
    
    /**
     * Check if performance is acceptable
     */
    public static boolean isPerformanceAcceptable() {
        return averageFPS >= 55f && gcCount < frameCount * 0.01f; // Less than 1% GC
    }
    
    /**
     * Get performance warnings
     */
    public static ArrayList<String> getPerformanceWarnings() {
        ArrayList<String> warnings = new ArrayList<>();
        
        if (averageFPS < 55f) {
            warnings.add(String.format("Low FPS: %.1f (should be >= 55)", averageFPS));
        }
        
        if (gcCount > frameCount * 0.01f) {
            warnings.add(String.format("High GC rate: %d GCs in %d frames (%.2f%%)", 
                                      gcCount, frameCount, (float) gcCount / frameCount * 100f));
        }
        
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        float memoryUsage = (float) usedMemory / maxMemory * 100f;
        
        if (memoryUsage > 80f) {
            warnings.add(String.format("High memory usage: %.1f%%", memoryUsage));
        }
        
        return warnings;
    }
    
    /**
     * Log performance metrics
     */
    public static void logPerformanceMetrics() {
        if (!DEBUG_ENABLED) return;
        
        System.out.println("=== Cursor Performance Metrics ===");
        System.out.println(getPerformanceReport());
        
        ArrayList<String> warnings = getPerformanceWarnings();
        if (!warnings.isEmpty()) {
            System.out.println("Performance Warnings:");
            for (String warning : warnings) {
                System.out.println("  - " + warning);
            }
        }
        System.out.println("================================");
    }
}
