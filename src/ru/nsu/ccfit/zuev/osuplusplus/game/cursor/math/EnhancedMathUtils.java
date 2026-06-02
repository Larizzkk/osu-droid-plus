package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.math;

import android.graphics.PointF;

/**
 * Enhanced MathUtils - GC-friendly vector mathematics for cursor movement
 * Optimized for Android to minimize allocations and ensure smooth 60+ FPS
 */
public class EnhancedMathUtils {
    
    // Pre-allocated temporary objects to avoid GC
    private static final PointF tempPoint1 = new PointF();
    private static final PointF tempPoint2 = new PointF();
    private static final PointF tempPoint3 = new PointF();
    
    // Pre-allocated commonly used values
    private static final float PI = (float) Math.PI;
    private static final float TWO_PI = 2f * PI;
    private static final float HALF_PI = PI / 2f;
    
    /**
     * GC-friendly linear interpolation
     * @param a Start value
     * @param b End value
     * @param t Interpolation factor [0,1]
     * @return a + (b - a) * t
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
    
    /**
     * GC-friendly vector linear interpolation
     * @param from Start vector
     * @param to End vector
     * @param t Interpolation factor [0,1]
     * @param result Output vector to avoid allocation
     */
    public static void lerp(PointF from, PointF to, float t, PointF result) {
        result.x = from.x + (to.x - from.x) * t;
        result.y = from.y + (to.y - from.y) * t;
    }
    
    /**
     * GC-friendly vector rotation
     * @param v Vector to rotate
     * @param angleRad Angle in radians
     * @param result Output vector to avoid allocation
     */
    public static void rotate(PointF v, float angleRad, PointF result) {
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);
        float rx = v.x * cos - v.y * sin;
        float ry = v.x * sin + v.y * cos;
        result.set(rx, ry);
    }
    
    /**
     * GC-friendly vector rotation with offset
     * @param v Vector to rotate
     * @param angleRad Angle in radians
     * @param centerX Center X coordinate
     * @param centerY Center Y coordinate
     * @param result Output vector to avoid allocation
     */
    public static void rotateAround(PointF v, float angleRad, float centerX, float centerY, PointF result) {
        // Translate to origin
        float tx = v.x - centerX;
        float ty = v.y - centerY;
        
        // Rotate
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);
        float rx = tx * cos - ty * sin;
        float ry = tx * sin + ty * cos;
        
        // Translate back
        result.set(rx + centerX, ry + centerY);
    }
    
    /**
     * Normalize angle to [0, 2PI]
     */
    public static float normalizeAngle(float angle) {
        angle = angle % TWO_PI;
        if (angle < 0) {
            angle += TWO_PI;
        }
        return angle;
    }
    
    /**
     * Normalize angle to [-PI, PI]
     */
    public static float normalizeAngleSigned(float angle) {
        angle = normalizeAngle(angle);
        if (angle > PI) {
            angle -= TWO_PI;
        }
        return angle;
    }
    
    /**
     * Calculate angle between two vectors
     * @param from Start vector
     * @param to End vector
     * @return Angle in radians
     */
    public static float angleBetween(PointF from, PointF to) {
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        return (float) Math.atan2(dy, dx);
    }
    
    /**
     * Calculate distance between two points
     */
    public static float distance(PointF a, PointF b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Calculate squared distance (faster, no sqrt)
     */
    public static float distanceSquared(PointF a, PointF b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return dx * dx + dy * dy;
    }
    
    /**
     * GC-friendly vector addition
     */
    public static void add(PointF a, PointF b, PointF result) {
        result.set(a.x + b.x, a.y + b.y);
    }
    
    /**
     * GC-friendly vector subtraction
     */
    public static void subtract(PointF a, PointF b, PointF result) {
        result.set(a.x - b.x, a.y - b.y);
    }
    
    /**
     * GC-friendly vector multiplication by scalar
     */
    public static void multiply(PointF v, float scalar, PointF result) {
        result.set(v.x * scalar, v.y * scalar);
    }
    
    /**
     * GC-friendly vector normalization
     * @param v Vector to normalize
     * @param result Output vector
     * @return Original length (0 if vector was zero)
     */
    public static float normalize(PointF v, PointF result) {
        float length = (float) Math.sqrt(v.x * v.x + v.y * v.y);
        if (length > 0.0001f) { // Avoid division by zero
            result.set(v.x / length, v.y / length);
        } else {
            result.set(0, 0);
        }
        return length;
    }
    
    /**
     * Calculate dot product
     */
    public static float dot(PointF a, PointF b) {
        return a.x * b.x + a.y * b.y;
    }
    
    /**
     * Calculate cross product (2D)
     */
    public static float cross(PointF a, PointF b) {
        return a.x * b.y - a.y * b.x;
    }
    
    /**
     * Check if point is within rectangle
     */
    public static boolean pointInRect(PointF point, float rectX, float rectY, float rectWidth, float rectHeight) {
        return point.x >= rectX && point.x <= rectX + rectWidth &&
               point.y >= rectY && point.y <= rectY + rectHeight;
    }
    
    /**
     * Clamp value to range
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Clamp point to rectangle
     */
    public static void clamp(PointF point, float minX, float minY, float maxX, float maxY, PointF result) {
        result.set(clamp(point.x, minX, maxX), clamp(point.y, minY, maxY));
    }
    
    /**
     * Get temporary point for calculations (thread-local)
     * WARNING: Use with caution, only for temporary calculations
     */
    public static PointF getTempPoint1() {
        return tempPoint1;
    }
    
    public static PointF getTempPoint2() {
        return tempPoint2;
    }
    
    public static PointF getTempPoint3() {
        return tempPoint3;
    }
    
    /**
     * Smooth step function for easing
     */
    public static float smoothStep(float edge0, float edge1, float t) {
        t = clamp((t - edge0) / (edge1 - edge0), 0, 1);
        return t * t * (3f - 2f * t);
    }
    
    /**
     * Check if two points are approximately equal
     */
    public static boolean approximatelyEqual(PointF a, PointF b, float epsilon) {
        return Math.abs(a.x - b.x) < epsilon && Math.abs(a.y - b.y) < epsilon;
    }
    
    /**
     * Reflect vector across normal
     */
    public static void reflect(PointF vector, PointF normal, PointF result) {
        float dot = dot(vector, normal);
        result.set(vector.x - 2f * dot * normal.x, vector.y - 2f * dot * normal.y);
    }
    
    /**
     * Project vector onto another vector
     */
    public static void project(PointF vector, PointF onto, PointF result) {
        float dot = dot(vector, onto);
        float ontoLengthSquared = onto.x * onto.x + onto.y * onto.y;
        if (ontoLengthSquared > 0.0001f) {
            float scale = dot / ontoLengthSquared;
            result.set(onto.x * scale, onto.y * scale);
        } else {
            result.set(0, 0);
        }
    }
    
    /**
     * Get perpendicular vector (rotated 90 degrees counter-clockwise)
     */
    public static void perpendicular(PointF v, PointF result) {
        result.set(-v.y, v.x);
    }
    
    /**
     * Calculate bezier curve point (quadratic)
     */
    public static void bezierQuadratic(PointF p0, PointF p1, PointF p2, float t, PointF result) {
        float oneMinusT = 1f - t;
        float oneMinusTSquared = oneMinusT * oneMinusT;
        float tSquared = t * t;
        
        result.x = oneMinusTSquared * p0.x + 2f * oneMinusT * t * p1.x + tSquared * p2.x;
        result.y = oneMinusTSquared * p0.y + 2f * oneMinusT * t * p1.y + tSquared * p2.y;
    }
    
    /**
     * Calculate bezier curve point (cubic)
     */
    public static void bezierCubic(PointF p0, PointF p1, PointF p2, PointF p3, float t, PointF result) {
        float oneMinusT = 1f - t;
        float oneMinusTCubed = oneMinusT * oneMinusT * oneMinusT;
        float tCubed = t * t * t;
        float threeOneMinusTSquaredT = 3f * oneMinusT * oneMinusT * t;
        float threeOneMinusTTSquared = 3f * oneMinusT * t * t;
        
        result.x = oneMinusTCubed * p0.x + threeOneMinusTSquaredT * p1.x + threeOneMinusTTSquared * p2.x + tCubed * p3.x;
        result.y = oneMinusTCubed * p0.y + threeOneMinusTSquaredT * p1.y + threeOneMinusTTSquared * p2.y + tCubed * p3.y;
    }
}
