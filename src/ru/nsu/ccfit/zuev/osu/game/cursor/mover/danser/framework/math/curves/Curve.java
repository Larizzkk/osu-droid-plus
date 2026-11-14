package ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves;

import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

/**
 * Exact port of danser-go Curve interface
 * Based on https://github.com/wieku/danser-go/blob/master/framework/math/curves/curve.go
 */
public interface Curve {
    
    /**
     * Get point at parameter t (0-1)
     */
    Vector2f pointAt(float t);
    
    /**
     * Get start angle of the curve
     */
    float getStartAngle();
    
    /**
     * Get end angle of the curve
     */
    float getEndAngle();
    
    /**
     * Get length of the curve
     */
    float getLength();
}
