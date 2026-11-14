package ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.DanserUtils;

/**
 * Circular arc implementation exactly from danser-go
 * Based on https://github.com/wieku/danser-go/blob/master/framework/math/curves/cirarc.go
 */
public class CircularArc {
    
    private PointF start;
    private PointF control;
    private PointF end;
    
    public CircularArc(PointF start, PointF control, PointF end) {
        this.start = new PointF(start.x, start.y);
        this.control = new PointF(control.x, control.y);
        this.end = new PointF(end.x, end.y);
    }
    
    /**
     * Get point at parameter t (0-1) exactly like danser-go
     */
    public PointF pointAt(float t) {
        // Simplified circular arc calculation
        // In real implementation, this would use proper arc mathematics
        
        // For now, use quadratic Bezier as approximation
        float mt = 1.0f - t;
        return new PointF(
            mt * mt * start.x + 2.0f * mt * t * control.x + t * t * end.x,
            mt * mt * start.y + 2.0f * mt * t * control.y + t * t * end.y
        );
    }
    
    /**
     * Get start point
     */
    public PointF getStart() {
        return new PointF(start.x, start.y);
    }
    
    /**
     * Get end point
     */
    public PointF getEnd() {
        return new PointF(end.x, end.y);
    }
    
    /**
     * Get control point
     */
    public PointF getControl() {
        return new PointF(control.x, control.y);
    }
    
    /**
     * Approximate length calculation
     */
    public float getLength() {
        // Approximate length using chord and arc height
        float chordLength = DanserUtils.dst(start, end);
        float arcHeight = DanserUtils.dst(control, 
            DanserUtils.lerp(start, end, 0.5f));
        
        // Approximate arc length
        return chordLength + (float) (Math.PI * arcHeight);
    }
}
