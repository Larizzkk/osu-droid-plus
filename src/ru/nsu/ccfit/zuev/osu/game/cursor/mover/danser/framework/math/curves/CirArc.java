package ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves;

import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

/**
 * Exact port of danser-go CirArc (Circular Arc)
 * Based on https://github.com/wieku/danser-go/blob/master/framework/math/curves/cirarc.go
 */
public class CirArc implements Curve {
    
    public static final float OSU_PI = 3.14159274f;
    
    private Vector2f pt1, pt2, pt3;
    private Vector2f centre;
    private double startAngle;
    private double totalAngle;
    private double dir;
    private float r;
    
    // Stable version with x87 promotion
    private Vector2f centreS;
    private double tInitialS;
    private double tFinalS;
    private float rS;
    
    private boolean unstable;
    
    public CirArc(Vector2f a, Vector2f b, Vector2f c) {
        this.pt1 = a.copy();
        this.pt2 = b.copy();
        this.pt3 = c.copy();
        this.dir = 1.0;
        
        if (Vector2f.isStraightLine32(a, b, c)) {
            unstable = true;
        }
        
        double d = 2 * (a.X * (b.Y - c.Y) + b.X * (c.Y - a.Y) + c.X * (a.Y - b.Y));
        double aSq = a.lenSq();
        double bSq = b.lenSq();
        double cSq = c.lenSq();
        
        centre = new Vector2f(
            (float) ((aSq * (b.Y - c.Y) + bSq * (c.Y - a.Y) + cSq * (a.Y - b.Y)) / d),
            (float) ((aSq * (c.X - b.X) + bSq * (a.X - c.X) + cSq * (b.X - a.X)) / d)
        );
        
        r = a.dst(centre);
        startAngle = Math.atan2(a.Y - centre.Y, a.X - centre.X);
        
        double endAngle = Math.atan2(c.Y - centre.Y, c.X - centre.X);
        
        while (endAngle < startAngle) {
            endAngle += 2 * Math.PI;
        }
        
        totalAngle = endAngle - startAngle;
        
        Vector2f aToC = c.sub(a);
        aToC = new Vector2f(aToC.Y, -aToC.X);
        
        if (aToC.dot(b.sub(a)) < 0) {
            dir = -dir;
            totalAngle = 2 * Math.PI - totalAngle;
        }
        
        arcStable(a, b, c);
    }
    
    private void arcStable(Vector2f a, Vector2f b, Vector2f c) {
        double d = 2 * (a.X * (b.Y - c.Y) + b.X * (c.Y - a.Y) + c.X * (a.Y - b.Y));
        
        double aSq = a.lenSq87();
        double bSq = b.lenSq87();
        double cSq = c.lenSq87();
        
        centreS = new Vector2f(
            (float) ((aSq * (b.Y - c.Y) + bSq * (c.Y - a.Y) + cSq * (a.Y - b.Y)) / d),
            (float) ((aSq * (c.X - b.X) + bSq * (a.X - c.X) + cSq * (b.X - a.X)) / d)
        );
        
        rS = a.dst87(centreS);
        
        tInitialS = ctAt(a, centreS);
        double tMid = ctAt(b, centreS);
        tFinalS = ctAt(c, centreS);
        
        while (tMid < tInitialS) {
            tMid += 2 * OSU_PI;
        }
        
        while (tFinalS < tInitialS) {
            tFinalS += 2 * OSU_PI;
        }
        
        if (tMid > tFinalS) {
            tFinalS -= 2 * OSU_PI;
        }
    }
    
    private double ctAt(Vector2f pt, Vector2f centre) {
        return Math.atan2(pt.Y - centre.Y, pt.X - centre.X);
    }
    
    @Override
    public Vector2f pointAt(float t) {
        double theta = startAngle + dir * t * totalAngle;
        return new Vector2f(
            (float) (Math.cos(theta) * r) + centre.X,
            (float) (Math.sin(theta) * r) + centre.Y
        );
    }
    
    public Vector2f pointAtL(double t) {
        double theta = startAngle + dir * t * totalAngle;
        return new Vector2f(
            (float) (Math.cos(theta) * r) + centre.X,
            (float) (Math.sin(theta) * r) + centre.Y
        );
    }
    
    public Vector2f pointAtS(double t) {
        double theta = tFinalS * t + tInitialS * (1 - t);
        return new Vector2f(
            add87((float) (Math.cos(theta) * rS), centreS.X),
            add87((float) (Math.sin(theta) * rS), centreS.Y)
        );
    }
    
    // Helper for x87 promotion
    private float add87(float a, float b) {
        return (float) ((double) a + (double) b);
    }
    
    @Override
    public float getLength() {
        return (float) (r * totalAngle);
    }
    
    @Override
    public float getStartAngle() {
        return pt1.angleRV(pointAt(1.0f / getLength()));
    }
    
    @Override
    public float getEndAngle() {
        return pt3.angleRV(pointAt((getLength() - 1.0f) / getLength()));
    }
    
    public boolean isUnstable() {
        return unstable;
    }
    
    public Vector2f getCentre() {
        return centre.copy();
    }
    
    public Vector2f getCentreS() {
        return centreS.copy();
    }
    
    public float getRadius() {
        return r;
    }
    
    public float getRadiusS() {
        return rS;
    }
}
