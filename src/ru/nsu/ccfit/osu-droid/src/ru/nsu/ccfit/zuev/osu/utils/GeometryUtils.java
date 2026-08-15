package ru.nsu.ccfit.zuev.osuplusplus.utils;

import android.graphics.PointF;
import android.graphics.RectF;
import java.util.List;
import java.util.ArrayList;

/**
 * Geometry utilities for game calculations
 * Based on osu!lazer's GeometryUtils.cs
 */
public class GeometryUtils {
    
    /**
     * Rotate a point around an arbitrary origin
     * @param point The point to rotate
     * @param origin The centre origin to rotate around
     * @param angle The angle to rotate (in degrees)
     * @return Rotated point
     */
    public static PointF rotatePointAroundOrigin(PointF point, PointF origin, float angle) {
        float radians = (float) Math.toRadians(-angle);
        
        float translatedX = point.x - origin.x;
        float translatedY = point.y - origin.y;
        
        float rotatedX = (float) (translatedX * Math.cos(radians) + translatedY * Math.sin(radians));
        float rotatedY = (float) (-translatedX * Math.sin(radians) + translatedY * Math.cos(radians));
        
        return new PointF(rotatedX + origin.x, rotatedY + origin.y);
    }
    
    /**
     * Rotate a vector around the origin
     * @param vector The vector to rotate
     * @param angle The angle to rotate (in degrees)
     * @return Rotated vector
     */
    public static PointF rotateVector(PointF vector, float angle) {
        float radians = (float) Math.toRadians(angle);
        
        return new PointF(
            (float) (vector.x * Math.cos(radians) + vector.y * Math.sin(radians)),
            (float) (-vector.x * Math.sin(radians) + vector.y * Math.cos(radians))
        );
    }
    
    /**
     * Calculate distance between two points
     * @param p1 First point
     * @param p2 Second point
     * @return Distance between points
     */
    public static float distance(PointF p1, PointF p2) {
        float dx = p1.x - p2.x;
        float dy = p1.y - p2.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Calculate squared distance between two points (faster than distance)
     * @param p1 First point
     * @param p2 Second point
     * @return Squared distance between points
     */
    public static float distanceSquared(PointF p1, PointF p2) {
        float dx = p1.x - p2.x;
        float dy = p1.y - p2.y;
        return dx * dx + dy * dy;
    }
    
    /**
     * Linear interpolation between two points
     * @param p1 Start point
     * @param p2 End point
     * @param t Interpolation factor (0.0 to 1.0)
     * @return Interpolated point
     */
    public static PointF lerp(PointF p1, PointF p2, float t) {
        return new PointF(
            p1.x + (p2.x - p1.x) * t,
            p1.y + (p2.y - p1.y) * t
        );
    }
    
    /**
     * Clamp value between min and max
     * @param value Value to clamp
     * @param min Minimum value
     * @param max Maximum value
     * @return Clamped value
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Clamp point within rectangle bounds
     * @param point Point to clamp
     * @param bounds Rectangle bounds
     * @return Clamped point
     */
    public static PointF clampPoint(PointF point, RectF bounds) {
        return new PointF(
            clamp(point.x, bounds.left, bounds.right),
            clamp(point.y, bounds.top, bounds.bottom)
        );
    }
    
    /**
     * Check if point is within rectangle
     * @param point Point to check
     * @param bounds Rectangle bounds
     * @return True if point is within bounds
     */
    public static boolean pointInRect(PointF point, RectF bounds) {
        return point.x >= bounds.left && point.x <= bounds.right &&
               point.y >= bounds.top && point.y <= bounds.bottom;
    }
    
    /**
     * Check if point is within circle
     * @param point Point to check
     * @param center Circle center
     * @param radius Circle radius
     * @return True if point is within circle
     */
    public static boolean pointInCircle(PointF point, PointF center, float radius) {
        return distanceSquared(point, center) <= radius * radius;
    }
    
    /**
     * Calculate angle between two points in degrees
     * @param from Start point
     * @param to End point
     * @return Angle in degrees
     */
    public static float angleBetweenPoints(PointF from, PointF to) {
        return (float) Math.toDegrees(Math.atan2(to.y - from.y, to.x - from.x));
    }
    
    /**
     * Normalize angle to 0-360 range
     * @param angle Angle in degrees
     * @return Normalized angle
     */
    public static float normalizeAngle(float angle) {
        angle = angle % 360;
        if (angle < 0) {
            angle += 360;
        }
        return angle;
    }
    
    /**
     * Calculate shortest angle difference in degrees
     * @param fromAngle Starting angle
     * @param toAngle Target angle
     * @return Shortest angle difference
     */
    public static float angleDifference(float fromAngle, float toAngle) {
        float diff = toAngle - fromAngle;
        diff = normalizeAngle(diff + 180) - 180;
        return diff;
    }
    
    /**
     * Get point on circle at given angle
     * @param center Circle center
     * @param radius Circle radius
     * @param angle Angle in degrees
     * @return Point on circle
     */
    public static PointF pointOnCircle(PointF center, float radius, float angle) {
        float radians = (float) Math.toRadians(angle);
        return new PointF(
            center.x + radius * (float) Math.cos(radians),
            center.y + radius * (float) Math.sin(radians)
        );
    }
    
    /**
     * Calculate bounding box for list of points
     * @param points List of points
     * @return Bounding rectangle
     */
    public static RectF calculateBounds(List<PointF> points) {
        if (points == null || points.isEmpty()) {
            return new RectF();
        }
        
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        
        for (PointF point : points) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
        }
        
        return new RectF(minX, minY, maxX, maxY);
    }
    
    /**
     * Scale point from origin
     * @param point Point to scale
     * @param origin Scale origin
     * @param scale Scale factor
     * @return Scaled point
     */
    public static PointF scalePoint(PointF point, PointF origin, float scale) {
        return new PointF(
            origin.x + (point.x - origin.x) * scale,
            origin.y + (point.y - origin.y) * scale
        );
    }
    
    /**
     * Reflect point across line defined by two points
     * @param point Point to reflect
     * @param linePoint1 First point on line
     * @param linePoint2 Second point on line
     * @return Reflected point
     */
    public static PointF reflectPointAcrossLine(PointF point, PointF linePoint1, PointF linePoint2) {
        // Calculate line direction vector
        PointF lineDir = new PointF(linePoint2.x - linePoint1.x, linePoint2.y - linePoint1.y);
        float lineLength = (float) Math.sqrt(lineDir.x * lineDir.x + lineDir.y * lineDir.y);
        
        if (lineLength == 0) {
            return point; // Line is a point
        }
        
        // Normalize line direction
        lineDir.x /= lineLength;
        lineDir.y /= lineLength;
        
        // Vector from line point to point to reflect
        PointF toPoint = new PointF(point.x - linePoint1.x, point.y - linePoint1.y);
        
        // Project onto line
        float projection = toPoint.x * lineDir.x + toPoint.y * lineDir.y;
        PointF projectedPoint = new PointF(
            linePoint1.x + projection * lineDir.x,
            linePoint1.y + projection * lineDir.y
        );
        
        // Calculate reflection
        return new PointF(
            2 * projectedPoint.x - point.x,
            2 * projectedPoint.y - point.y
        );
    }
    
    /**
     * Check if three points are collinear
     * @param p1 First point
     * @param p2 Second point
     * @param p3 Third point
     * @param tolerance Tolerance for collinearity check
     * @return True if points are collinear
     */
    public static boolean arePointsCollinear(PointF p1, PointF p2, PointF p3, float tolerance) {
        // Calculate area of triangle formed by three points
        float area = Math.abs(
            (p2.x - p1.x) * (p3.y - p1.y) - (p3.x - p1.x) * (p2.y - p1.y)
        ) / 2.0f;
        
        return area <= tolerance;
    }
    
    /**
     * Calculate center of circle passing through three points
     * @param p1 First point on circle
     * @param p2 Second point on circle
     * @param p3 Third point on circle
     * @return Center of circle, or null if points are collinear
     */
    public static PointF calculateCircleCenter(PointF p1, PointF p2, PointF p3) {
        // Check if points are collinear
        if (arePointsCollinear(p1, p2, p3, 0.001f)) {
            return null;
        }
        
        // Calculate perpendicular bisectors
        float midX1 = (p1.x + p2.x) / 2;
        float midY1 = (p1.y + p2.y) / 2;
        float midX2 = (p2.x + p3.x) / 2;
        float midY2 = (p2.y + p3.y) / 2;
        
        // Calculate slopes
        float slope1 = -(p2.x - p1.x) / (p2.y - p1.y);
        float slope2 = -(p3.x - p2.x) / (p3.y - p2.y);
        
        // Calculate intersection point
        float centerX = (midY2 - midY1 + slope1 * midX1 - slope2 * midX2) / (slope1 - slope2);
        float centerY = midY1 + slope1 * (centerX - midX1);
        
        return new PointF(centerX, centerY);
    }
}
