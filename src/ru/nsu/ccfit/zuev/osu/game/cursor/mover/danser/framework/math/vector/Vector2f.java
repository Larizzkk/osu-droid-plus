package ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector;

import android.graphics.PointF;

/**
 * Exact port of danser-go Vector2f
 * Based on https://github.com/wieku/danser-go/blob/master/framework/math/vector/vector2f.go
 */
public class Vector2f {
    
    public static final float EPSILON = 0.00001f;
    public static final float OSU_PI = 3.14159274f;
    
    public float X;
    public float Y;
    
    public Vector2f(float x, float y) {
        this.X = x;
        this.Y = y;
    }
    
    public Vector2f(PointF point) {
        this.X = point.x;
        this.Y = point.y;
    }
    
    public Vector2f() {
        this(0f, 0f);
    }
    
    public static Vector2f NewVec2f(float x, float y) {
        return new Vector2f(x, y);
    }
    
    public static Vector2f NewVec2fRad(float rad, float length) {
        return new Vector2f(
            (float) Math.cos(rad) * length,
            (float) Math.sin(rad) * length
        );
    }
    
    public float X64() {
        return X;
    }
    
    public float Y64() {
        return Y;
    }
    
    public PointF toPointF() {
        return new PointF(X, Y);
    }
    
    public Vector2f copy() {
        return new Vector2f(X, Y);
    }
    
    public Vector2f add(Vector2f v1) {
        return new Vector2f(X + v1.X, Y + v1.Y);
    }
    
    public Vector2f addS(float x, float y) {
        return new Vector2f(X + x, Y + y);
    }
    
    public Vector2f sub(Vector2f v1) {
        return new Vector2f(X - v1.X, Y - v1.Y);
    }
    
    public Vector2f subS(float x, float y) {
        return new Vector2f(X - x, Y - y);
    }
    
    public Vector2f mult(Vector2f v1) {
        return new Vector2f(X * v1.X, Y * v1.Y);
    }
    
    public Vector2f mid(Vector2f v1) {
        return new Vector2f((X + v1.X) / 2f, (Y + v1.Y) / 2f);
    }
    
    public float dot(Vector2f v1) {
        return X * v1.X + Y * v1.Y;
    }
    
    public float dst(Vector2f v1) {
        float x = v1.X - X;
        float y = v1.Y - Y;
        return (float) Math.sqrt(x * x + y * y);
    }
    
    // Dst87 - follows x87 promotion to double
    public float dst87(Vector2f v1) {
        double x = v1.X - X;
        double y = v1.Y - Y;
        return (float) Math.sqrt(x * x + y * y);
    }
    
    public float dstSq(Vector2f v1) {
        float x = v1.X - X;
        float y = v1.Y - Y;
        return x * x + y * y;
    }
    
    // DstSq87 - follows x87 promotion to double
    public float dstSq87(Vector2f v1) {
        double x = v1.X - X;
        double y = v1.Y - Y;
        return (float) (x * x + y * y);
    }
    
    public float angle() {
        return angleR() * 180f / (float) Math.PI;
    }
    
    public float angleR() {
        return (float) Math.atan2(Y, X);
    }
    
    // Nor - exact implementation with floating point errors from osu
    public Vector2f nor() {
        float length = lenSq();
        
        if (length < EPSILON) {
            return copy();
        }
        
        float scale = 1.0f / (float) Math.sqrt(length);
        return new Vector2f(X * scale, Y * scale);
    }
    
    // Nor87 - follows x87 promotion to double
    public Vector2f nor87() {
        float length = lenSq87();
        
        if (length < EPSILON) {
            return copy();
        }
        
        float scale = div87(1.0f, (float) Math.sqrt(length));
        return new Vector2f(mul87(X, scale), mul87(Y, scale));
    }
    
    public float angleRV(Vector2f v1) {
        return (float) Math.atan2(Y - v1.Y, X - v1.X);
    }
    
    public Vector2f lerp(Vector2f v1, float t) {
        return new Vector2f(
            (v1.X - X) * t + X,
            (v1.Y - Y) * t + Y
        );
    }
    
    public Vector2f rotate(float rad) {
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        
        return new Vector2f(
            X * cos - Y * sin,
            X * sin + Y * cos
        );
    }
    
    public float len() {
        return (float) Math.sqrt(X * X + Y * Y);
    }
    
    public float lenSq() {
        return X * X + Y * Y;
    }
    
    // LenSq87 - follows x87 promotion to double
    public float lenSq87() {
        double pX = X;
        double pY = Y;
        return (float) (pX * pX + pY * pY);
    }
    
    public Vector2f scl(float mag) {
        return new Vector2f(X * mag, Y * mag);
    }
    
    // Scl87 - follows x87 promotion to double
    public Vector2f scl87(float mag) {
        return new Vector2f(mul87(X, mag), mul87(Y, mag));
    }
    
    public Vector2f abs() {
        return new Vector2f(Math.abs(X), Math.abs(Y));
    }
    
    // Helper methods for x87 promotion
    private float div87(float a, float b) {
        return (float) ((double) a / (double) b);
    }
    
    private float mul87(float a, float b) {
        return (float) ((double) a * (double) b);
    }
    
    public static boolean isStraightLine32(Vector2f a, Vector2f b, Vector2f c) {
        return Math.abs((b.Y - a.Y) * (c.X - a.X) - (b.X - a.X) * (c.Y - a.Y)) < 0.001f;
    }
    
    public static float angleBetween32(Vector2f centre, Vector2f p1, Vector2f p2) {
        float a = centre.dst(p1);
        float b = centre.dst(p2);
        float c = p1.dst(p2);
        
        return (float) Math.acos((a * a + b * b - c * c) / (2 * a * b));
    }
    
    @Override
    public String toString() {
        return X + "x" + Y;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Vector2f)) return false;
        Vector2f other = (Vector2f) obj;
        return Float.compare(X, other.X) == 0 && Float.compare(Y, other.Y) == 0;
    }
    
    @Override
    public int hashCode() {
        return Float.floatToIntBits(X) * 31 + Float.floatToIntBits(Y);
    }
}
