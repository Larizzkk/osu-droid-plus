package ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves;

import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

/**
 * Exact port of danser-go Bezier curve
 * Based on https://github.com/wieku/danser-go/blob/master/framework/math/curves/bezier.go
 */
public class Bezier implements Curve {
    
    private Vector2f[] points;
    private float controlLength;
    private float approxLength;
    
    /**
     * Creates a bezier curve with approximated length
     */
    public Bezier(Vector2f[] points) {
        this.points = points.clone();
        this.controlLength = calculateControlLength();
        this.approxLength = this.controlLength;
        calculateLength();
    }
    
    /**
     * Creates a bezier curve with non-approximated length
     */
    public Bezier(Vector2f[] points, boolean noApprox) {
        this.points = points.clone();
        this.controlLength = calculateControlLength();
        this.approxLength = this.controlLength;
    }
    
    private float calculateControlLength() {
        float length = 0f;
        for (int i = 1; i < points.length; i++) {
            length += points[i].dst(points[i - 1]);
        }
        return length;
    }
    
    /**
     * Calculates the approximate length of the curve to 2 decimal points of accuracy
     */
    public void calculateLength() {
        float length = 0f;
        
        int sections = (int) Math.ceil(controlLength);
        
        Vector2f previous = points[0];
        for (int i = 1; i <= sections; i++) {
            Vector2f current = pointAt((float) i / sections);
            length += current.dst(previous);
            previous = current;
        }
        
        approxLength = length;
    }
    
    /**
     * Point at parameter t using Bernstein polynomials
     * https://en.wikipedia.org/wiki/B%C3%A9zier_curve#Terminology
     */
    @Override
    public Vector2f pointAt(float t) {
        Vector2f p = new Vector2f(0f, 0f);
        int n = points.length - 1;
        
        for (int i = 0; i <= n; i++) {
            float b = bernstein(i, n, t);
            p.X += points[i].X * b;
            p.Y += points[i].Y * b;
        }
        
        return p;
    }
    
    @Override
    public float getLength() {
        return approxLength;
    }
    
    @Override
    public float getStartAngle() {
        return points[0].angleRV(pointAt(1.0f / controlLength));
    }
    
    @Override
    public float getEndAngle() {
        return points[points.length - 1].angleRV(pointAt(1.0f - 1.0f / controlLength));
    }
    
    /**
     * Binomial coefficient
     * https://en.wikipedia.org/wiki/Binomial_coefficient#Multiplicative_formula
     */
    private static long binomialCoefficient(long n, long k) {
        if (k < 0 || k > n) {
            return 0;
        }
        
        if (k == 0 || k == n) {
            return 1;
        }
        
        k = Math.min(k, n - k);
        
        long c = 1;
        for (long i = 1; i <= k; i++) {
            c *= (n + 1 - i) / i;
        }
        
        return c;
    }
    
    /**
     * Bernstein polynomial
     * https://en.wikipedia.org/wiki/Bernstein_polynomial
     */
    private static float bernstein(int i, int n, float t) {
        return (float) binomialCoefficient(n, i) * 
               (float) Math.pow(t, i) * 
               (float) Math.pow(1.0 - t, n - i);
    }
    
    public Vector2f[] getPoints() {
        return points.clone();
    }
    
    public float getControlLength() {
        return controlLength;
    }
}
