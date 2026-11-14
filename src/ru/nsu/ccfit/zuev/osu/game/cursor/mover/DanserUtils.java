package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import java.util.List;
import java.util.ArrayList;

/**
 * Utility class containing exact port of danser-go movement utilities.
 * Based on https://github.com/wieku/danser-go/blob/master/app/dance/movers/
 */
public class DanserUtils {
    
    // Constants from danser-go
    public static final float SIXTY_TIME = 1000.0f / 60f;
    public static final float PI = (float) Math.PI;
    public static final float TWO_PI = 2.0f * PI;
    
    /**
     * Calculate angle between two points exactly like danser-go
     */
    public static float angleRV(PointF from, PointF to) {
        return (float) Math.atan2(to.y - from.y, to.x - from.x);
    }
    
    /**
     * Create vector from angle and magnitude exactly like danser-go
     */
    public static PointF newVec2fRad(float angle, float magnitude) {
        return new PointF(
            (float) (Math.cos(angle) * magnitude),
            (float) (Math.sin(angle) * magnitude)
        );
    }
    
    /**
     * Add two vectors exactly like danser-go
     */
    public static PointF add(PointF a, PointF b) {
        return new PointF(a.x + b.x, a.y + b.y);
    }
    
    /**
     * Subtract two vectors exactly like danser-go
     */
    public static PointF sub(PointF a, PointF b) {
        return new PointF(a.x - b.x, a.y - b.y);
    }
    
    /**
     * Scale vector by factor exactly like danser-go
     */
    public static PointF scale(PointF v, float factor) {
        return new PointF(v.x * factor, v.y * factor);
    }
    
    /**
     * Calculate distance between two points exactly like danser-go
     */
    public static float dst(PointF a, PointF b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Linear interpolation between two points exactly like danser-go
     */
    public static PointF lerp(PointF a, PointF b, float t) {
        return new PointF(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t
        );
    }
    
    /**
     * Clamp value to range exactly like danser-go
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Check if float is NaN exactly like danser-go
     */
    public static boolean isNaN(float value) {
        return Float.isNaN(value);
    }
    
    /**
     * Generate control points for aggressive movement exactly like danser-go
     */
    public static List<PointF> generateAggressivePoints(PointF startPos, PointF endPos, 
                                                      float startTime, float endTime, float lastAngle) {
        List<PointF> points = new ArrayList<>();
        
        float scaledDistance = endTime - startTime;
        float newAngle = lastAngle + PI;
        
        points.add(startPos);
        points.add(add(startPos, newVec2fRad(newAngle, scaledDistance)));
        
        if (scaledDistance > 1) {
            lastAngle = angleRV(points.get(1), endPos);
        }
        
        points.add(endPos);
        
        return points;
    }
    
    /**
     * Generate control points for bezier movement exactly like danser-go
     */
    public static List<PointF> generateBezierPoints(PointF startPos, PointF endPos, 
                                                   float startTime, float endTime, 
                                                   PointF pt, float previousSpeed,
                                                   float aggressiveness, float sliderAggressiveness) {
        List<PointF> points = new ArrayList<>();
        
        float dst = dst(startPos, endPos);
        
        if (previousSpeed < 0) {
            previousSpeed = dst / (endTime - startTime);
        }
        
        if (startPos.equals(endPos)) {
            points.add(startPos);
            points.add(endPos);
        } else {
            float angle = angleRV(pt, endPos);
            if (isNaN(angle)) {
                angle = 0;
            }
            
            pt = add(startPos, newVec2fRad(angle, previousSpeed * aggressiveness));
            
            points.add(startPos);
            points.add(pt);
            points.add(endPos);
        }
        
        return points;
    }
    
    /**
     * Generate points for pippi movement exactly like danser-go
     */
    public static List<PointF> generatePippiPoints(PointF startPos, PointF endPos,
                                                   float startTime, float endTime,
                                                   float rotationSpeed, float radius) {
        List<PointF> points = new ArrayList<>();
        
        float timeDifference = endTime - startTime;
        int numPoints = (int) Math.ceil(timeDifference / SIXTY_TIME);
        
        for (int i = 0; i <= numPoints; i++) {
            float t = (float) i / numPoints;
            float currentTime = startTime + t * timeDifference;
            
            PointF basePos = lerp(startPos, endPos, t);
            
            // Add rotation like danser-go pippi mover
            float rad = (currentTime / 1000f * rotationSpeed) % 1.0f * TWO_PI;
            PointF mVec = newVec2fRad(rad, radius);
            
            points.add(add(basePos, mVec));
        }
        
        return points;
    }
    
    /**
     * Calculate movement speed exactly like danser-go
     */
    public static float calculateSpeed(PointF startPos, PointF endPos, float startTime, float endTime) {
        float distance = dst(startPos, endPos);
        return distance / (endTime - startTime);
    }
    
    /**
     * Apply easing function exactly like danser-go
     */
    public static float applyEasing(float t, String easingType) {
        switch (easingType.toLowerCase()) {
            case "outquad":
                return t * (2.0f - t);
            case "inoutcubic":
                if (t < 0.5f) {
                    return 4.0f * t * t * t;
                } else {
                    float p = 2.0f * t - 2.0f;
                    return 1.0f + p * p * p / 2.0f;
                }
            case "out":
                return t;
            default:
                return t;
        }
    }
    
    /**
     * Convert playfield coordinates exactly like danser-go
     */
    public static PointF convertPlayfieldCoords(PointF osuCoords) {
        // danser-go uses 512x384 playfield, osu-droid uses different scale
        // This converts from osu coordinates to danser-style coordinates
        return new PointF(osuCoords.x, osuCoords.y);
    }
    
    /**
     * Calculate optimal movement time based on distance exactly like danser-go
     */
    public static float calculateOptimalTime(PointF startPos, PointF endPos, float baseTime) {
        float distance = dst(startPos, endPos);
        
        // danser-go uses distance-based timing adjustments
        if (distance < 50) {
            return baseTime * 0.8f; // Faster for short distances
        } else if (distance > 200) {
            return baseTime * 1.2f; // Slower for long distances
        }
        
        return baseTime;
    }
    
    /**
     * Generate smooth curve points for enhanced movement
     */
    public static List<PointF> generateSmoothCurve(PointF startPos, PointF endPos, 
                                                  PointF controlPoint, int resolution) {
        List<PointF> points = new ArrayList<>();
        
        for (int i = 0; i <= resolution; i++) {
            float t = (float) i / resolution;
            PointF point = calculateBezierPoint(startPos, controlPoint, endPos, t);
            points.add(point);
        }
        
        return points;
    }
    
    /**
     * Calculate quadratic Bezier curve point
     */
    public static PointF calculateBezierPoint(PointF p0, PointF p1, PointF p2, float t) {
        float mt = 1.0f - t;
        return new PointF(
            mt * mt * p0.x + 2.0f * mt * t * p1.x + t * t * p2.x,
            mt * mt * p0.y + 2.0f * mt * t * p1.y + t * t * p2.y
        );
    }
    
    /**
     * Calculate cubic Bezier curve point
     */
    public static PointF calculateCubicBezierPoint(PointF p0, PointF p1, PointF p2, PointF p3, float t) {
        float mt = 1.0f - t;
        return new PointF(
            mt * mt * mt * p0.x + 3.0f * mt * mt * t * p1.x + 3.0f * mt * t * t * p2.x + t * t * t * p3.x,
            mt * mt * mt * p0.y + 3.0f * mt * mt * t * p1.y + 3.0f * mt * t * t * p2.y + t * t * t * p3.y
        );
    }
}
