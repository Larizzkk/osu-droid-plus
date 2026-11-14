package ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.math32;

/**
 * Exact port of danser-go math32
 * Based on https://github.com/wieku/danser-go/blob/master/framework/math/math32/math.go
 */
public class Math32 {
    
    public static final float PI = (float) Math.PI;
    
    public static float abs(float v) {
        return (float) Math.abs(v);
    }
    
    public static float acos(float v) {
        return (float) Math.acos(v);
    }
    
    public static float asin(float v) {
        return (float) Math.asin(v);
    }
    
    public static float atan(float v) {
        return (float) Math.atan(v);
    }
    
    public static float atan2(float y, float x) {
        return (float) Math.atan2(y, x);
    }
    
    public static float ceil(float v) {
        return (float) Math.ceil(v);
    }
    
    public static float cos(float v) {
        return (float) Math.cos(v);
    }
    
    public static float floor(float v) {
        return (float) Math.floor(v);
    }
    
    public static float inf(int sign) {
        return (float) (sign > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
    }
    
    public static float round(float v) {
        return floor(v + 0.5f);
    }
    
    public static boolean isNaN(float v) {
        return Float.isNaN(v);
    }
    
    public static float sin(float v) {
        return (float) Math.sin(v);
    }
    
    public static float sqrt(float v) {
        return (float) Math.sqrt(v);
    }
    
    public static float max(float a, float b) {
        return Math.max(a, b);
    }
    
    public static float min(float a, float b) {
        return Math.min(a, b);
    }
    
    public static float mod(float a, float b) {
        return (float) (a % b);
    }
    
    public static float nan() {
        return Float.NaN;
    }
    
    public static float pow(float a, float b) {
        return (float) Math.pow(a, b);
    }
    
    public static float tan(float v) {
        return (float) Math.tan(v);
    }
}
