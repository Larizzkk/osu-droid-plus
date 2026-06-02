package ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves;

import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

/**
 * Linear curve implementation exactly from danser-go
 * Based on https://github.com/wieku/danser-go/blob/master/framework/math/curves/linear.go
 */
public class LinearCurve implements Curve {
    
    private Vector2f start;
    private Vector2f end;
    
    public LinearCurve(Vector2f start, Vector2f end) {
        this.start = start.copy();
        this.end = end.copy();
    }
    
    /**
     * Get point at parameter t (0-1) exactly like danser-go
     */
    @Override
    public Vector2f pointAt(float t) {
        return new Vector2f(
            start.X + (end.X - start.X) * t,
            start.Y + (end.Y - start.Y) * t
        );
    }
    
    /**
     * Get start angle exactly like danser-go
     */
    @Override
    public float getStartAngle() {
        return start.angleRV(end);
    }
    
    /**
     * Get end angle exactly like danser-go
     */
    @Override
    public float getEndAngle() {
        return end.angleRV(start);
    }
    
    /**
     * Get length of the curve exactly like danser-go
     */
    @Override
    public float getLength() {
        return start.dst(end);
    }
    
    /**
     * Get start point
     */
    public Vector2f getStart() {
        return start.copy();
    }
    
    /**
     * Get end point
     */
    public Vector2f getEnd() {
        return end.copy();
    }
}
