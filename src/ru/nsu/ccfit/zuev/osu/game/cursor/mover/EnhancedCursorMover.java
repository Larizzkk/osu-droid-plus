package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.MathUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.Easing;
import ru.nsu.ccfit.zuev.osuplusplus.Config;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced cursor mover with slider support and continuous movement
 * Based on danser-go advanced movement patterns
 */
public class EnhancedCursorMover implements CursorMover {
    
    // Movement segments for continuous movement
    private static class MovementSegment {
        PointF startPos;
        PointF endPos;
        float startTime;
        float endTime;
        boolean isSlider;
        PointF[] sliderPoints; // For smooth slider movement
        float sliderDuration;
        
        MovementSegment(PointF startPos, PointF endPos, float startTime, float endTime, boolean isSlider) {
            this.startPos = startPos;
            this.endPos = endPos;
            this.startTime = startTime;
            this.endTime = endTime;
            this.isSlider = isSlider;
            this.sliderPoints = null;
            this.sliderDuration = endTime - startTime;
        }
    }
    
    private List<MovementSegment> movementSegments = new ArrayList<>();
    private int currentSegmentIndex = 0;
    private float totalMovementDistance = 0f;
    private boolean isContinuous = true;
    private float minSegmentDistance = 10f; // Minimum distance to create new segment
    
    // Configuration
    private float sliderFollowRadius = 50f;
    private float sliderFollowSpeed = 1.0f;
    private boolean enableSliderTracking = true;
    private boolean enableContinuousMovement = true;
    private boolean enableProbing = true; // Enable angle offset probing
    private float probingDistanceMultiplier = 1.2f; // Probing enhancement
    
    // Current state
    private PointF currentPosition = new PointF(0, 0);
    private float currentTime = 0f;
    
    public EnhancedCursorMover() {
        // Default constructor
    }
    
    @Override
    public void setMovement(PointF startPos, PointF endPos, float startTime, float endTime) {
        // Check if this is a slider movement (enhanced slider support)
        boolean isSlider = isSliderMovement(startPos, endPos, startTime, endTime);
        if (isSlider && Config.isEnhancedCursorSliderMovementEnabled()) {
            setSliderMovementWithProbing(startPos, endPos, startTime, endTime);
            return;
        }
        
        // Create new movement segment
        MovementSegment segment = new MovementSegment(startPos, endPos, startTime, endTime, false);
        
        // Add probing control points if enabled
        float distance = MathUtils.dst(startPos, endPos);
        if (enableProbing && distance > minSegmentDistance) {
            segment.sliderPoints = generateProbingPoints(startPos, endPos);
        }
        
        movementSegments.add(segment);
        currentSegmentIndex = movementSegments.size() - 1;
        
        // Update current position
        currentPosition = new PointF(startPos.x, startPos.y);
        currentTime = startTime;
        
        // Calculate total movement distance
        totalMovementDistance += distance;
        
        // Remove old segments if too many
        if (movementSegments.size() > 10) {
            movementSegments.remove(0);
            currentSegmentIndex--;
        }
    }
    
    @Override
    public PointF getPositionAt(float time) {
        if (movementSegments.isEmpty()) {
            return currentPosition;
        }
        
        // Find appropriate segment for current time
        while (currentSegmentIndex < movementSegments.size() - 1 && 
               time >= movementSegments.get(currentSegmentIndex + 1).startTime) {
            currentSegmentIndex++;
        }
        
        if (currentSegmentIndex >= movementSegments.size()) {
            currentSegmentIndex = movementSegments.size() - 1;
        }
        
        MovementSegment currentSegment = movementSegments.get(currentSegmentIndex);
        
        // Handle segment boundaries for continuous movement
        if (enableContinuousMovement && currentSegmentIndex < movementSegments.size() - 1) {
            MovementSegment nextSegment = movementSegments.get(currentSegmentIndex + 1);
            
            // Smooth transition between segments
            float transitionTime = currentSegment.endTime;
            float transitionDuration = 50f; // 50ms transition
            
            if (time >= transitionTime && time < transitionTime + transitionDuration) {
                // Interpolate between current and next segment
                float t = (time - transitionTime) / transitionDuration;
                PointF currentEnd = currentSegment.isSlider ? 
                    getSliderPositionAt(currentSegment, currentSegment.endTime) : 
                    currentSegment.endPos;
                PointF nextStart = nextSegment.isSlider ? 
                    getSliderPositionAt(nextSegment, nextSegment.startTime) : 
                    nextSegment.startPos;
                
                return MathUtils.lerp(currentEnd, nextStart, t);
            }
        }
        
        // Get position in current segment
        if (time <= currentSegment.startTime) {
            return currentSegment.startPos;
        }
        
        if (time >= currentSegment.endTime && currentSegmentIndex < movementSegments.size() - 1) {
            // Move to next segment
            currentSegmentIndex++;
            return movementSegments.get(currentSegmentIndex).startPos;
        }
        
        // Calculate position in current segment
        if (currentSegment.isSlider && currentSegment.sliderPoints != null) {
            return getSliderPositionAt(currentSegment, time);
        } else {
            return getLinearPositionAt(currentSegment, time);
        }
    }
    
    private PointF getSliderPositionAt(MovementSegment segment, float time) {
        if (segment.sliderPoints == null || segment.sliderPoints.length < 2) {
            return getLinearPositionAt(segment, time);
        }
        
        float t = (time - segment.startTime) / (segment.endTime - segment.startTime);
        t = MathUtils.clamp(t, 0f, 1f);
        
        // Interpolate along slider path
        int pointIndex = (int) (t * (segment.sliderPoints.length - 1));
        float localT = (t * (segment.sliderPoints.length - 1)) - pointIndex;
        
        if (pointIndex >= segment.sliderPoints.length - 1) {
            return segment.sliderPoints[segment.sliderPoints.length - 1];
        }
        
        PointF p1 = segment.sliderPoints[pointIndex];
        PointF p2 = segment.sliderPoints[pointIndex + 1];
        
        return MathUtils.lerp(p1, p2, localT);
    }
    
    private PointF getLinearPositionAt(MovementSegment segment, float time) {
        float t = (time - segment.startTime) / (segment.endTime - segment.startTime);
        t = MathUtils.clamp(t, 0f, 1f);
        
        // Use OutQuad easing for smooth movement
        float easedT = Easing.outQuad(t);
        
        return MathUtils.lerp(segment.startPos, segment.endPos, easedT);
    }
    
    private PointF[] generateSliderPoints(PointF start, PointF end, float duration) {
        int numPoints = Math.max(3, (int) (duration / 16.67f)); // 60 FPS
        
        PointF[] points = new PointF[numPoints];
        
        for (int i = 0; i < numPoints; i++) {
            float t = (float) i / (numPoints - 1);
            
            // Create smooth curve path
            float midT = 0.5f - 0.5f * (float) Math.cos(t * Math.PI);
            PointF midPoint = new PointF(
                start.x + (end.x - start.x) * 0.5f + (float) Math.sin(t * Math.PI * 2) * (end.y - start.y) * 0.2f,
                start.y + (end.y - start.y) * 0.5f - (float) Math.cos(t * Math.PI * 2) * (end.x - start.x) * 0.2f
            );
            
            points[i] = midPoint;
        }
        
        points[0] = start;
        points[numPoints - 1] = end;
        
        return points;
    }
    
    @Override
    public void reset() {
        movementSegments.clear();
        currentSegmentIndex = 0;
        totalMovementDistance = 0f;
        currentPosition = new PointF(0, 0);
        currentTime = 0f;
    }
    
    @Override
    public boolean isFinished(float time) {
        if (movementSegments.isEmpty()) {
            return true;
        }
        
        MovementSegment lastSegment = movementSegments.get(movementSegments.size() - 1);
        return time >= lastSegment.endTime;
    }
    
    @Override
    public PointF getObjectsPosition(float time, PointF objectPos) {
        return null; // Simple implementation for enhanced cursor
    }
    
    @Override
    public boolean supportsMultiPoint() {
        return true; // Enhanced cursor supports multi-point movement
    }
    
    @Override
    public void setMultiPointMovement(PointF[] positions, float[] times, float startTime) {
        if (positions == null || positions.length < 2 || times == null || times.length != positions.length) {
            return;
        }
        
        // Clear existing segments
        movementSegments.clear();
        currentSegmentIndex = 0;
        
        // Create movement segments for all positions
        for (int i = 0; i < positions.length - 1; i++) {
            PointF start = positions[i];
            PointF end = positions[i + 1];
            float segmentStartTime = (i == 0) ? startTime : times[i];
            float segmentEndTime = times[i + 1];
            
            MovementSegment segment = new MovementSegment(start, end, segmentStartTime, segmentEndTime, false);
            
            // Add probing control points if enabled
            float distance = MathUtils.dst(start, end);
            if (enableProbing && distance > minSegmentDistance) {
                segment.sliderPoints = generateProbingPoints(start, end);
            }
            
            movementSegments.add(segment);
        }
        
        // Set initial position
        if (positions.length > 0) {
            currentPosition = new PointF(positions[0].x, positions[0].y);
            currentTime = startTime;
        }
    }
    
    /**
     * Check if objects are close enough for slider-like movement
     */
    private boolean isCloseEnough(PointF pos1, PointF pos2) {
        return MathUtils.dst(pos1, pos2) < sliderFollowRadius;
    }
    
    /**
     * Get current movement statistics
     */
    public MovementStats getStats() {
        return new MovementStats(
            movementSegments.size(),
            currentSegmentIndex,
            totalMovementDistance,
            isContinuous,
            enableSliderTracking
        );
    }
    
    /**
     * Configuration methods
     */
    public void setSliderFollowRadius(float radius) { this.sliderFollowRadius = radius; }
    public void setSliderFollowSpeed(float speed) { this.sliderFollowSpeed = speed; }
    public void setEnableSliderTracking(boolean enable) { this.enableSliderTracking = enable; }
    public void setEnableContinuousMovement(boolean enable) { this.isContinuous = enable; }
    public void setMinSegmentDistance(float distance) { this.minSegmentDistance = distance; }
    
    /**
     * Movement statistics
     */
    public static class MovementStats {
        public final int segmentCount;
        public final int currentSegmentIndex;
        public final float totalDistance;
        public final boolean isContinuous;
        public final boolean sliderTrackingEnabled;
        
        public MovementStats(int segmentCount, int currentSegmentIndex, float totalDistance, 
                              boolean isContinuous, boolean sliderTrackingEnabled) {
            this.segmentCount = segmentCount;
            this.currentSegmentIndex = currentSegmentIndex;
            this.totalDistance = totalDistance;
            this.isContinuous = isContinuous;
            this.sliderTrackingEnabled = sliderTrackingEnabled;
        }
        
        @Override
        public String toString() {
            return String.format(
                "MovementStats[segments=%d, current=%d, distance=%.1f, continuous=%s, slider=%s]",
                segmentCount, currentSegmentIndex, totalDistance, isContinuous, sliderTrackingEnabled
            );
        }
    }
    
    /**
     * Generate probing points with angle offset for "probing" effect
     */
    private PointF[] generateProbingPoints(PointF start, PointF end) {
        float distance = MathUtils.dst(start, end);
        float scaledDistance = distance * probingDistanceMultiplier;
        
        // Calculate angle offsets for enhanced probing
        float baseAngle = (float) Math.atan2(end.y - start.y, end.x - start.x);
        float angleOffset = (float) Math.PI / 5f; // 36 degrees offset
        
        // Create control points for enhanced probing
        PointF control1 = new PointF(
            start.x + (float) Math.cos(baseAngle + angleOffset) * scaledDistance * 0.4f,
            start.y + (float) Math.sin(baseAngle + angleOffset) * scaledDistance * 0.4f
        );
        
        PointF control2 = new PointF(
            end.x + (float) Math.cos(baseAngle - Math.PI + angleOffset) * scaledDistance * 0.4f,
            end.y + (float) Math.sin(baseAngle - Math.PI + angleOffset) * scaledDistance * 0.4f
        );
        
        return new PointF[]{control1, control2};
    }
    
    /**
     * Check if movement represents a slider (long duration, short distance)
     */
    private boolean isSliderMovement(PointF start, PointF end, float startTime, float endTime) {
        float distance = MathUtils.dst(start, end);
        float duration = endTime - startTime;
        // Slider detection: long duration relative to distance
        return duration > 300 && distance < 100 && duration / distance > 3.0f;
    }
    
    /**
     * Set Enhanced slider movement with enhanced probing
     */
    private void setSliderMovementWithProbing(PointF start, PointF end, float startTime, float endTime) {
        // Calculate slider angle for enhanced probing
        float sliderAngle = (float) Math.atan2(end.y - start.y, end.x - start.x);
        float sliderDistance = MathUtils.dst(start, end);
        
        // Enhanced cursor slider probing with expanded segments using Config
        float probeDistance = sliderDistance * Config.getEnhancedCursorSliderDistanceMultiplier();
        float angleOffset = Config.getEnhancedCursorSliderAngleOffset();
        int segments = Config.getEnhancedCursorSliderSegments();
        
        // Create enhanced control points for slider
        PointF control1 = new PointF(
            start.x + (float) Math.cos(sliderAngle + angleOffset) * probeDistance,
            start.y + (float) Math.sin(sliderAngle + angleOffset) * probeDistance
        );
        
        PointF control2 = new PointF(
            end.x + (float) Math.cos(sliderAngle - Math.PI + angleOffset) * probeDistance,
            end.y + (float) Math.sin(sliderAngle - Math.PI + angleOffset) * probeDistance
        );
        
        // Additional slider-specific enhanced control points
        PointF midPoint = new PointF(
            (start.x + end.x) * 0.5f,
            (start.y + end.y) * 0.5f
        );
        
        PointF enhancedOffset = new PointF(
            (float) Math.cos(sliderAngle + Math.PI / 2f) * probeDistance * 0.4f,
            (float) Math.sin(sliderAngle + Math.PI / 2f) * probeDistance * 0.4f
        );
        
        // Create new movement segment for slider
        MovementSegment segment = new MovementSegment(start, end, startTime, endTime, true);
        segment.sliderPoints = new PointF[]{
            start,
            control1,
            new PointF(midPoint.x + enhancedOffset.x, midPoint.y + enhancedOffset.y),
            control2,
            end
        };
        
        movementSegments.add(segment);
        currentSegmentIndex = movementSegments.size() - 1;
        
        // Update current position
        currentPosition = new PointF(start.x, start.y);
        currentTime = startTime;
        
        // Calculate total movement distance
        totalMovementDistance += sliderDistance;
        
        // Remove old segments if too many
        if (movementSegments.size() > 10) {
            movementSegments.remove(0);
            currentSegmentIndex--;
        }
    }
}
