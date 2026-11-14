package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import java.util.List;
import java.util.ArrayList;

/**
 * Advanced curve utilities ported from danser-go curve system.
 * Based on https://github.com/wieku/danser-go/blob/master/framework/math/curves/
 */
public class DanserCurveUtils {
    
    /**
     * Multi-segment curve implementation like danser-go
     */
    public static class MultiCurve {
        private List<CurveSegment> segments;
        private float totalLength;
        
        public MultiCurve() {
            segments = new ArrayList<>();
            totalLength = 0;
        }
        
        public void addSegment(CurveSegment segment) {
            segments.add(segment);
            totalLength += segment.getLength();
        }
        
        public PointF pointAt(float t) {
            if (segments.isEmpty()) {
                return new PointF(0, 0);
            }
            
            t = DanserUtils.clamp(t, 0f, 1f);
            float targetLength = t * totalLength;
            float currentLength = 0;
            
            for (CurveSegment segment : segments) {
                if (currentLength + segment.getLength() >= targetLength) {
                    float segmentT = (targetLength - currentLength) / segment.getLength();
                    return segment.pointAt(segmentT);
                }
                currentLength += segment.getLength();
            }
            
            return segments.get(segments.size() - 1).pointAt(1);
        }
        
        public float getLength() {
            return totalLength;
        }
    }
    
    /**
     * Base curve segment interface
     */
    public interface CurveSegment {
        PointF pointAt(float t);
        float getLength();
    }
    
    /**
     * Linear curve segment (straight line)
     */
    public static class LinearSegment implements CurveSegment {
        private PointF start;
        private PointF end;
        private float length;
        
        public LinearSegment(PointF start, PointF end) {
            this.start = start;
            this.end = end;
            this.length = DanserUtils.dst(start, end);
        }
        
        @Override
        public PointF pointAt(float t) {
            return DanserUtils.lerp(start, end, t);
        }
        
        @Override
        public float getLength() {
            return length;
        }
    }
    
    /**
     * Bezier curve segment exactly like danser-go
     */
    public static class BezierSegment implements CurveSegment {
        private PointF[] points;
        private float length;
        private boolean lengthCalculated;
        
        public BezierSegment(PointF... points) {
            this.points = points;
            this.lengthCalculated = false;
        }
        
        @Override
        public PointF pointAt(float t) {
            if (points.length == 2) {
                return DanserUtils.lerp(points[0], points[1], t);
            } else if (points.length == 3) {
                return DanserUtils.calculateBezierPoint(points[0], points[1], points[2], t);
            } else if (points.length == 4) {
                return DanserUtils.calculateCubicBezierPoint(points[0], points[1], points[2], points[3], t);
            }
            return points[0];
        }
        
        @Override
        public float getLength() {
            if (!lengthCalculated) {
                length = calculateLength();
                lengthCalculated = true;
            }
            return length;
        }
        
        private float calculateLength() {
            // Approximate length using numerical integration
            float length = 0;
            PointF prevPoint = pointAt(0);
            
            for (int i = 1; i <= 20; i++) {
                float t = (float) i / 20;
                PointF currentPoint = pointAt(t);
                length += DanserUtils.dst(prevPoint, currentPoint);
                prevPoint = currentPoint;
            }
            
            return length;
        }
    }
    
    /**
     * Circular arc segment
     */
    public static class ArcSegment implements CurveSegment {
        private PointF center;
        private float radius;
        private float startAngle;
        private float endAngle;
        private float length;
        
        public ArcSegment(PointF center, float radius, float startAngle, float endAngle) {
            this.center = center;
            this.radius = radius;
            this.startAngle = startAngle;
            this.endAngle = endAngle;
            this.length = Math.abs(endAngle - startAngle) * radius;
        }
        
        @Override
        public PointF pointAt(float t) {
            float angle = startAngle + (endAngle - startAngle) * t;
            return DanserUtils.add(center, DanserUtils.newVec2fRad(angle, radius));
        }
        
        @Override
        public float getLength() {
            return length;
        }
    }
    
    /**
     * Create smooth curve through multiple points like danser-go
     */
    public static MultiCurve createSmoothCurve(List<PointF> points) {
        MultiCurve curve = new MultiCurve();
        
        if (points.size() < 2) {
            return curve;
        }
        
        for (int i = 0; i < points.size() - 1; i++) {
            PointF start = points.get(i);
            PointF end = points.get(i + 1);
            
            if (i == 0) {
                // First segment - simple line
                curve.addSegment(new LinearSegment(start, end));
            } else {
                // Middle segments - create smooth bezier
                PointF prev = points.get(i - 1);
                PointF next = i + 2 < points.size() ? points.get(i + 2) : end;
                
                PointF control1 = calculateControlPoint(prev, start, end, 0.25f);
                PointF control2 = calculateControlPoint(start, end, next, 0.75f);
                
                curve.addSegment(new BezierSegment(start, control1, control2, end));
            }
        }
        
        return curve;
    }
    
    /**
     * Calculate control point for smooth bezier curve
     */
    private static PointF calculateControlPoint(PointF prev, PointF current, PointF next, float t) {
        PointF tangent = DanserUtils.sub(next, prev);
        tangent = DanserUtils.scale(tangent, 0.5f);
        return DanserUtils.add(current, DanserUtils.scale(tangent, t - 0.5f));
    }
    
    /**
     * Create aggressive curve like danser-go AggressiveMover
     */
    public static MultiCurve createAggressiveCurve(PointF start, PointF end, float lastAngle) {
        MultiCurve curve = new MultiCurve();
        
        List<PointF> points = DanserUtils.generateAggressivePoints(start, end, 0, 1000, lastAngle);
        
        if (points.size() >= 3) {
            curve.addSegment(new BezierSegment(points.toArray(new PointF[0])));
        } else {
            curve.addSegment(new LinearSegment(start, end));
        }
        
        return curve;
    }
    
    /**
     * Create bezier curve like danser-go BezierMover
     */
    public static MultiCurve createBezierCurve(PointF start, PointF end, PointF pt, float previousSpeed, float aggressiveness) {
        MultiCurve curve = new MultiCurve();
        
        List<PointF> points = DanserUtils.generateBezierPoints(start, end, 0, 1000, pt, previousSpeed, aggressiveness, 1.0f);
        
        if (points.size() >= 3) {
            curve.addSegment(new BezierSegment(points.toArray(new PointF[0])));
        } else {
            curve.addSegment(new LinearSegment(start, end));
        }
        
        return curve;
    }
    
    /**
     * Create pippi curve like danser-go PippiMover
     */
    public static MultiCurve createPippiCurve(PointF start, PointF end, float startTime, float endTime, float rotationSpeed, float radius) {
        MultiCurve curve = new MultiCurve();
        
        List<PointF> points = DanserUtils.generatePippiPoints(start, end, startTime, endTime, rotationSpeed, radius);
        
        // Connect points with linear segments
        for (int i = 0; i < points.size() - 1; i++) {
            curve.addSegment(new LinearSegment(points.get(i), points.get(i + 1)));
        }
        
        return curve;
    }
    
    /**
     * Optimize curve by removing redundant points
     */
    public static List<PointF> optimizeCurve(List<PointF> points, float tolerance) {
        if (points.size() <= 2) {
            return new ArrayList<>(points);
        }
        
        List<PointF> optimized = new ArrayList<>();
        optimized.add(points.get(0));
        
        for (int i = 1; i < points.size() - 1; i++) {
            PointF prev = points.get(i - 1);
            PointF current = points.get(i);
            PointF next = points.get(i + 1);
            
            // Check if current point is redundant
            float distToLine = pointToLineDistance(current, prev, next);
            
            if (distToLine > tolerance) {
                optimized.add(current);
            }
        }
        
        optimized.add(points.get(points.size() - 1));
        return optimized;
    }
    
    /**
     * Calculate distance from point to line segment
     */
    private static float pointToLineDistance(PointF point, PointF lineStart, PointF lineEnd) {
        PointF line = DanserUtils.sub(lineEnd, lineStart);
        PointF pointToStart = DanserUtils.sub(point, lineStart);
        
        float lineLength = DanserUtils.dst(lineStart, lineEnd);
        if (lineLength == 0) {
            return DanserUtils.dst(point, lineStart);
        }
        
        float t = DanserUtils.clamp(
            (pointToStart.x * line.x + pointToStart.y * line.y) / (lineLength * lineLength),
            0, 1
        );
        
        PointF projection = DanserUtils.add(lineStart, DanserUtils.scale(line, t));
        return DanserUtils.dst(point, projection);
    }
}
