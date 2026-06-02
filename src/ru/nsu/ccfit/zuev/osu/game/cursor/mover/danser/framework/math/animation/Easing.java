package ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.animation;

/**
 * Exact port of danser-go easing functions
 * Based on https://github.com/wieku/danser-go/blob/master/framework/math/animation/easing/
 */
public class Easing {
    
    // OutQuad easing exactly like danser-go
    public static float outQuad(float t) {
        return t * (2f - t);
    }
    
    // InOutCubic easing for smooth movement
    public static float inOutCubic(float t) {
        if (t < 0.5f) {
            return 4f * t * t * t;
        } else {
            float p = 2f * t - 2f;
            return 1f + p * p * p / 2f;
        }
    }
    
    // InOutSine easing for smooth movement
    public static float inOutSine(float t) {
        return -(float) Math.cos(Math.PI * t) + 1f;
    }
    
    // Linear easing
    public static float linear(float t) {
        return t;
    }
    
    // InQuad easing
    public static float inQuad(float t) {
        return t * t;
    }
    
    // OutSine easing
    public static float outSine(float t) {
        return (float) Math.sin(t * Math.PI / 2f);
    }
}
