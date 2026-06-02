package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.math;

import android.graphics.PointF;
import java.util.ArrayList;

/**
 * CurveUtils - Advanced curve utilities with arc-length parametrization
 * Ensures constant speed movement along curves for smooth cursor motion
 */
public class CurveUtils {
    
    // Pre-allocated temporary objects
    private static final PointF tempPoint1 = new PointF();
    private static final PointF tempPoint2 = new PointF();
    private static final PointF tempPoint3 = new PointF();
    
    /**
     * Bezier curve with arc-length caching for constant speed
     */
    public static class BezierCurve {
        private final PointF p0, p1, p2, p3;
        private final boolean isCubic;
        private final ArrayList<Float> arcLengthTable;
        private final int samples;
        private float totalLength;
        
        public BezierCurve(PointF p0, PointF p1, PointF p2, PointF p3) {
            this.p0 = new PointF(p0);
            this.p1 = new PointF(p1);
            this.p2 = new PointF(p2);
            this.p3 = new PointF(p3);
            this.isCubic = true;
            this.samples = 100; // Higher precision for smooth movement
            this.arcLengthTable = new ArrayList<>(samples + 1);
            precomputeArcLength();
        }
        
        public BezierCurve(PointF p0, PointF p1, PointF p2) {
            this.p0 = new PointF(p0);
            this.p1 = new PointF(p1);
            this.p2 = new PointF(p2);
            this.p3 = null;
            this.isCubic = false;
            this.samples = 100;
            this.arcLengthTable = new ArrayList<>(samples + 1);
            precomputeArcLength();
        }
        
        /**
         * Precompute arc length table for constant speed movement
         */
        private void precomputeArcLength() {
            arcLengthTable.clear();
            arcLengthTable.add(0f);
            
            float previousX = p0.x;
            float previousY = p0.y;
            totalLength = 0f;
            
            for (int i = 1; i <= samples; i++) {
                float t = (float) i / samples;
                PointF current = tempPoint1;
                
                if (isCubic) {
                    EnhancedMathUtils.bezierCubic(p0, p1, p2, p3, t, current);
                } else {
                    EnhancedMathUtils.bezierQuadratic(p0, p1, p2, t, current);
                }
                
                float dx = current.x - previousX;
                float dy = current.y - previousY;
                float segmentLength = (float) Math.sqrt(dx * dx + dy * dy);
                totalLength += segmentLength;
                
                arcLengthTable.add(totalLength);
                
                previousX = current.x;
                previousY = current.y;
            }
        }
        
        /**
         * Get point at normalized distance [0,1] along curve (constant speed)
         */
        public void getPointAt(float normalizedDistance, PointF result) {
            if (totalLength <= 0.0001f) {
                result.set(p0);
                return;
            }
            
            float targetLength = normalizedDistance * totalLength;
            targetLength = EnhancedMathUtils.clamp(targetLength, 0, totalLength);
            
            // Binary search in arc length table
            int low = 0;
            int high = arcLengthTable.size() - 1;
            
            while (low < high) {
                int mid = (low + high) / 2;
                if (arcLengthTable.get(mid) < targetLength) {
                    low = mid + 1;
                } else {
                    high = mid;
                }
            }
            
            // Linear interpolation between samples
            int sampleIndex = Math.max(0, low - 1);
            float prevLength = arcLengthTable.get(sampleIndex);
            float nextLength = arcLengthTable.get(Math.min(sampleIndex + 1, arcLengthTable.size() - 1));
            
            float segmentLength = nextLength - prevLength;
            float t = segmentLength > 0.0001f ? (targetLength - prevLength) / segmentLength : 0f;
            
            float baseT = (float) sampleIndex / samples;
            float actualT = baseT + t / samples;
            
            if (isCubic) {
                EnhancedMathUtils.bezierCubic(p0, p1, p2, p3, actualT, result);
            } else {
                EnhancedMathUtils.bezierQuadratic(p0, p1, p2, actualT, result);
            }
        }
        
        public float getTotalLength() {
            return totalLength;
        }
    }
    
    /**
     * Catmull-Rom Spline for natural transitions between points
     */
    public static class CatmullRomSpline {
        private final ArrayList<PointF> controlPoints;
        private final ArrayList<BezierCurve> segments;
        private final ArrayList<Float> segmentLengths;
        private float totalLength;
        
        public CatmullRomSpline(ArrayList<PointF> points) {
            this.controlPoints = new ArrayList<>(points);
            this.segments = new ArrayList<>();
            this.segmentLengths = new ArrayList<>();
            generateSpline();
        }
        
        /**
         * Generate Catmull-Rom spline segments
         */
        private void generateSpline() {
            segments.clear();
            segmentLengths.clear();
            totalLength = 0f;
            
            if (controlPoints.size() < 2) return;
            
            for (int i = 0; i < controlPoints.size() - 1; i++) {
                PointF p0 = controlPoints.get(Math.max(0, i - 1));
                PointF p1 = controlPoints.get(i);
                PointF p2 = controlPoints.get(Math.min(controlPoints.size() - 1, i + 1));
                PointF p3 = controlPoints.get(Math.min(controlPoints.size() - 1, i + 2));
                
                // Convert Catmull-Rom to Bezier
                PointF cp1 = tempPoint1;
                PointF cp2 = tempPoint2;
                
                cp1.set(
                    p1.x + (p2.x - p0.x) / 6f,
                    p1.y + (p2.y - p0.y) / 6f
                );
                
                cp2.set(
                    p2.x - (p3.x - p1.x) / 6f,
                    p2.y - (p3.y - p1.y) / 6f
                );
                
                BezierCurve segment = new BezierCurve(p1, cp1, cp2, p2);
                segments.add(segment);
                segmentLengths.add(segment.getTotalLength());
                totalLength += segment.getTotalLength();
            }
        }
        
        /**
         * Get point at normalized distance along spline
         */
        public void getPointAt(float normalizedDistance, PointF result) {
            if (segments.isEmpty()) {
                if (!controlPoints.isEmpty()) {
                    result.set(controlPoints.get(0));
                } else {
                    result.set(0, 0);
                }
                return;
            }
            
            if (totalLength <= 0.0001f) {
                result.set(controlPoints.get(0));
                return;
            }
            
            float targetLength = normalizedDistance * totalLength;
            targetLength = EnhancedMathUtils.clamp(targetLength, 0, totalLength);
            
            // Find segment
            float accumulatedLength = 0f;
            for (int i = 0; i < segments.size(); i++) {
                float segmentLength = segmentLengths.get(i);
                if (accumulatedLength + segmentLength >= targetLength) {
                    float segmentT = (targetLength - accumulatedLength) / segmentLength;
                    segments.get(i).getPointAt(segmentT, result);
                    return;
                }
                accumulatedLength += segmentLength;
            }
            
            // Return last point if we didn't find segment
            result.set(controlPoints.get(controlPoints.size() - 1));
        }
        
        public float getTotalLength() {
            return totalLength;
        }
    }
    
    /**
     * Generate control points for smooth curve through hit objects
     */
    public static void generateSmoothCurve(ArrayList<PointF> points, ArrayList<PointF> controlPoints) {
        controlPoints.clear();
        
        if (points.size() < 2) {
            controlPoints.addAll(points);
            return;
        }
        
        // Add first point
        controlPoints.add(new PointF(points.get(0)));
        
        // Generate intermediate control points
        for (int i = 1; i < points.size() - 1; i++) {
            PointF prev = points.get(i - 1);
            PointF current = points.get(i);
            PointF next = points.get(i + 1);
            
            // Calculate control point for smooth transition
            PointF cp = tempPoint1;
            cp.set(
                current.x + (next.x - prev.x) * 0.25f,
                current.y + (next.y - prev.y) * 0.25f
            );
            
            controlPoints.add(new PointF(cp));
        }
        
        // Add last point
        controlPoints.add(new PointF(points.get(points.size() - 1)));
    }
    
    /**
     * Calculate curve tightness based on distance between points
     */
    public static float calculateTightness(PointF p1, PointF p2) {
        float distance = EnhancedMathUtils.distance(p1, p2);
        // Tighter curves for closer points, looser for distant points
        return EnhancedMathUtils.clamp(distance / 200f, 0.1f, 1f);
    }
    
    /**
     * Adaptive sampling - more samples for complex curves
     */
    public static int calculateSampleCount(PointF p1, PointF p2, PointF p3, PointF p4) {
        // Calculate curvature complexity
        float angle1 = EnhancedMathUtils.angleBetween(p1, p2);
        float angle2 = EnhancedMathUtils.angleBetween(p2, p3);
        float angle3 = EnhancedMathUtils.angleBetween(p3, p4);
        
        float angleChange = Math.abs(angle2 - angle1) + Math.abs(angle3 - angle2);
        
        // More samples for sharper curves
        int baseSamples = 50;
        int additionalSamples = (int) (angleChange * 20f);
        
        return Math.min(baseSamples + additionalSamples, 200);
    }
    
    /**
     * Check if curve is self-intersecting (for debugging)
     */
    public static boolean isSelfIntersecting(PointF p0, PointF p1, PointF p2, PointF p3) {
        // Simplified check - can be improved with actual intersection testing
        float d1 = EnhancedMathUtils.distance(p0, p1);
        float d2 = EnhancedMathUtils.distance(p1, p2);
        float d3 = EnhancedMathUtils.distance(p2, p3);
        
        // Very sharp turns might indicate self-intersection
        float angle1 = EnhancedMathUtils.angleBetween(p0, p1);
        float angle2 = EnhancedMathUtils.angleBetween(p1, p2);
        float angle3 = EnhancedMathUtils.angleBetween(p2, p3);
        
        float angleChange1 = Math.abs(EnhancedMathUtils.normalizeAngleSigned(angle2 - angle1));
        float angleChange2 = Math.abs(EnhancedMathUtils.normalizeAngleSigned(angle3 - angle2));
        
        return angleChange1 > Math.PI * 0.8f || angleChange2 > Math.PI * 0.8f;
    }
}
