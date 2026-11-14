package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.MathUtils;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;

/**
 * Bezier curve - exact port from danser-go framework/math/curves/bezier.go
 * Based on https://en.wikipedia.org/wiki/B%C3%A9zier_curve#Terminology
 */
public class BezierCurve {
    private PointF[] points;
    private float controlLength;
    private float approxLength;
    
    // Creates a bezier curve with approximated length
    public BezierCurve(PointF[] points) {
        this.points = points;
        calculateControlLength();
        this.approxLength = controlLength;
    }
    
    // Creates a bezier curve with non-approximated length
    public static BezierCurve newBezierNA(PointF[] points) {
        BezierCurve bz = new BezierCurve(points);
        bz.calculateControlLength();
        bz.approxLength = bz.controlLength;
        return bz;
    }
    
    private void calculateControlLength() {
        controlLength = 0;
        for (int i = 1; i < points.length; i++) {
            controlLength += MathUtils.dst(points[i], points[i-1]);
        }
        approxLength = controlLength;
    }
    
    // Calculates the approximate length of the curve to 2 decimal points of accuracy in most cases
    public void calculateLength() {
        float length = 0;
        
        int sections = (int) Math.ceil(controlLength);
        
        PointF previous = points[0];
        for (int i = 1; i <= sections; i++) {
            PointF current = pointAt((float) i / sections);
            
            length += MathUtils.dst(current, previous);
            
            previous = current;
        }
        
        approxLength = length;
    }
    
    // https://en.wikipedia.org/wiki/B%C3%A9zier_curve#Terminology
    public PointF pointAt(float t) {
        PointF p = new PointF(0, 0);
        int n = points.length - 1;
        
        for (int i = 0; i <= n; i++) {
            float b = bernstein(i, n, t);
            p.x += points[i].x * b;
            p.y += points[i].y * b;
        }
        
        return p;
    }
    
    public float getLength() {
        return approxLength;
    }
    
    public float getStartAngle() {
        return MathUtils.angleRV(points[0], pointAt(1.0f / controlLength));
    }
    
    public float getEndAngle() {
        return MathUtils.angleRV(points[points.length - 1], pointAt(1.0f - 1.0f / controlLength));
    }
    
    // https://en.wikipedia.org/wiki/Bernstein_polynomial
    private static float bernstein(int i, int n, float t) {
        return (float) (binomialCoefficient(n, i) * pow(t, i) * pow(1.0 - t, n - i));
    }
    
    // https://en.wikipedia.org/wiki/Binomial_coefficient#Multiplicative_formula
    private static long binomialCoefficient(int n, int k) {
        if (k < 0 || k > n) {
            return 0;
        }
        
        if (k == 0 || k == n) {
            return 1;
        }
        
        k = Math.min(k, n - k);
        
        long c = 1;
        for (int i = 1; i <= k; i++) {
            c = c * (n + 1 - i) / i;
        }
        
        return c;
    }
}
