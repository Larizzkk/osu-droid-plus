package ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.animation.easing;

/**
 * Complete port of danser-go easing functions.
 * Based on https://github.com/wieku/danser-go/blob/master/framework/math/animation/easing/equations.go
 */
public class Easing {

    // Linear
    public static double linear(double t) { return t; }

    // Quad
    public static double inQuad(double t) { return t * t; }
    public static double outQuad(double t) { return -t * (t - 2); }
    public static double inOutQuad(double t) {
        if (t < 0.5) return 2 * t * t;
        return -2 * t * t + 4 * t - 1;
    }

    // Cubic
    public static double inCubic(double t) { return t * t * t; }
    public static double outCubic(double t) { double p = t - 1; return p * p * p + 1; }
    public static double inOutCubic(double t) {
        if (t < 0.5) return 4 * t * t * t;
        double p = 2 * t - 2;
        return 0.5 * p * p * p + 1;
    }

    // Quart
    public static double inQuart(double t) { return t * t * t * t; }
    public static double outQuart(double t) { double p = t - 1; return -(p * p * p * p - 1); }
    public static double inOutQuart(double t) {
        if (t < 0.5) return 8 * t * t * t * t;
        double p = t - 1;
        return -8 * p * p * p * p + 1;
    }

    // Quint
    public static double inQuint(double t) { return t * t * t * t * t; }
    public static double outQuint(double t) { double p = t - 1; return p * p * p * p * p + 1; }
    public static double inOutQuint(double t) {
        if (t < 0.5) return 16 * t * t * t * t * t;
        double p = 2 * t - 2;
        return 0.5 * p * p * p * p * p + 1;
    }

    // Sine
    public static double inSine(double t) { return 1 - Math.cos(t * Math.PI / 2); }
    public static double outSine(double t) { return Math.sin(t * Math.PI / 2); }
    public static double inOutSine(double t) { return -0.5 * (Math.cos(Math.PI * t) - 1); }

    // Exponential
    public static double inExpo(double t) { return t == 0 ? 0 : Math.pow(2, 10 * (t - 1)); }
    public static double outExpo(double t) { return t == 1 ? 1 : 1 - Math.pow(2, -10 * t); }
    public static double inOutExpo(double t) {
        if (t == 0 || t == 1) return t;
        if (t < 0.5) return 0.5 * Math.pow(2, 20 * t - 10);
        return -0.5 * Math.pow(2, -20 * t + 10) + 1;
    }

    // Circular
    public static double inCirc(double t) { return 1 - Math.sqrt(1 - t * t); }
    public static double outCirc(double t) { double p = t - 1; return Math.sqrt(1 - p * p); }
    public static double inOutCirc(double t) {
        if (t < 0.5) return 0.5 * (1 - Math.sqrt(1 - 4 * t * t));
        return 0.5 * (Math.sqrt(-(2 * t - 3) * (2 * t - 1)) + 1);
    }

    // Back
    public static double inBack(double t) { double s = 1.70158; return t * t * ((s + 1) * t - s); }
    public static double outBack(double t) { double s = 1.70158; double p = t - 1; return p * p * ((s + 1) * p + s) + 1; }
    public static double inOutBack(double t) {
        double s = 1.70158 * 1.525;
        if (t < 0.5) return 0.5 * (2 * t * 2 * t * ((s + 1) * 2 * t - s));
        double p = 2 * t - 2;
        return 0.5 * (p * p * ((s + 1) * p + s) + 2);
    }

    // Elastic
    public static double inElastic(double t) {
        if (t == 0 || t == 1) return t;
        return -Math.pow(2, 10 * (t - 1)) * Math.sin((t - 1.1) * 5 * Math.PI);
    }
    public static double outElastic(double t) {
        if (t == 0 || t == 1) return t;
        return Math.pow(2, -10 * t) * Math.sin((t - 0.1) * 5 * Math.PI) + 1;
    }
    public static double inOutElastic(double t) {
        if (t == 0 || t == 1) return t;
        if (t < 0.5) return -0.5 * Math.pow(2, 20 * t - 10) * Math.sin((20 * t - 11.125) * Math.PI / 4.5);
        return Math.pow(2, -20 * t + 10) * Math.sin((20 * t - 11.125) * Math.PI / 4.5) * 0.5 + 1;
    }

    // Bounce
    public static double outBounce(double t) {
        if (t < 0.36363636) return 7.5625 * t * t;
        if (t < 0.72727272) { t -= 0.54545454; return 7.5625 * t * t + 0.75; }
        if (t < 0.90909090) { t -= 0.81818181; return 7.5625 * t * t + 0.9375; }
        t -= 0.95454545; return 7.5625 * t * t + 0.984375;
    }
    public static double inBounce(double t) { return 1 - outBounce(1 - t); }
    public static double inOutBounce(double t) {
        if (t < 0.5) return inBounce(2 * t) * 0.5;
        return outBounce(2 * t - 1) * 0.5 + 0.5;
    }

    // Float versions
    public static float linear(float t) { return t; }
    public static float inQuad(float t) { return t * t; }
    public static float outQuad(float t) { return -t * (t - 2); }
    public static float inOutQuad(float t) {
        if (t < 0.5f) return 2 * t * t;
        return -2 * t * t + 4 * t - 1;
    }
    public static float inCubic(float t) { return t * t * t; }
    public static float outCubic(float t) { float p = t - 1; return p * p * p + 1; }
    public static float inOutCubic(float t) {
        if (t < 0.5f) return 4 * t * t * t;
        float p = 2 * t - 2;
        return 0.5f * p * p * p + 1;
    }
    public static float inQuart(float t) { return t * t * t * t; }
    public static float outQuart(float t) { float p = t - 1; return -(p * p * p * p - 1); }
    public static float inOutQuart(float t) {
        if (t < 0.5f) return 8 * t * t * t * t;
        float p = t - 1;
        return -8 * p * p * p * p + 1;
    }
    public static float inQuint(float t) { return t * t * t * t * t; }
    public static float outQuint(float t) { float p = t - 1; return p * p * p * p * p + 1; }
    public static float inOutQuint(float t) {
        if (t < 0.5f) return 16 * t * t * t * t * t;
        float p = 2 * t - 2;
        return 0.5f * p * p * p * p * p + 1;
    }
    public static float inSine(float t) { return (float) (1 - Math.cos(t * Math.PI / 2)); }
    public static float outSine(float t) { return (float) Math.sin(t * Math.PI / 2); }
    public static float inOutSine(float t) { return (float) (-0.5 * (Math.cos(Math.PI * t) - 1)); }
    public static float inExpo(float t) { return t == 0 ? 0 : (float) Math.pow(2, 10 * (t - 1)); }
    public static float outExpo(float t) { return t == 1 ? 1 : (float) (1 - Math.pow(2, -10 * t)); }
    public static float inOutExpo(float t) {
        if (t == 0 || t == 1) return t;
        if (t < 0.5f) return (float) (0.5 * Math.pow(2, 20 * t - 10));
        return (float) (-0.5 * Math.pow(2, -20 * t + 10) + 1);
    }
    public static float inCirc(float t) { return (float) (1 - Math.sqrt(1 - t * t)); }
    public static float outCirc(float t) { float p = t - 1; return (float) Math.sqrt(1 - p * p); }
    public static float inOutCirc(float t) {
        if (t < 0.5f) return (float) (0.5 * (1 - Math.sqrt(1 - 4 * t * t)));
        return (float) (0.5 * (Math.sqrt(-(2 * t - 3) * (2 * t - 1)) + 1));
    }
    public static float inBack(float t) { float s = 1.70158f; return t * t * ((s + 1) * t - s); }
    public static float outBack(float t) { float s = 1.70158f; float p = t - 1; return p * p * ((s + 1) * p + s) + 1; }
    public static float inOutBack(float t) {
        float s = 1.70158f * 1.525f;
        if (t < 0.5f) return 0.5f * (2 * t * 2 * t * ((s + 1) * 2 * t - s));
        float p = 2 * t - 2;
        return 0.5f * (p * p * ((s + 1) * p + s) + 2);
    }
    public static float inElastic(float t) {
        if (t == 0 || t == 1) return t;
        return (float) (-Math.pow(2, 10 * (t - 1)) * Math.sin((t - 1.1) * 5 * Math.PI));
    }
    public static float outElastic(float t) {
        if (t == 0 || t == 1) return t;
        return (float) (Math.pow(2, -10 * t) * Math.sin((t - 0.1) * 5 * Math.PI) + 1);
    }
    public static float inOutElastic(float t) {
        if (t == 0 || t == 1) return t;
        if (t < 0.5f) return (float) (-0.5 * Math.pow(2, 20 * t - 10) * Math.sin((20 * t - 11.125f) * Math.PI / 4.5));
        return (float) (Math.pow(2, -20 * t + 10) * Math.sin((20 * t - 11.125f) * Math.PI / 4.5) * 0.5 + 1);
    }
    public static float outBounce(float t) {
        if (t < 0.36363636f) return 7.5625f * t * t;
        if (t < 0.72727272f) { t -= 0.54545454f; return 7.5625f * t * t + 0.75f; }
        if (t < 0.90909090f) { t -= 0.81818181f; return 7.5625f * t * t + 0.9375f; }
        t -= 0.95454545f; return 7.5625f * t * t + 0.984375f;
    }
    public static float inBounce(float t) { return 1 - outBounce(1 - t); }
    public static float inOutBounce(float t) {
        if (t < 0.5f) return inBounce(2 * t) * 0.5f;
        return outBounce(2 * t - 1) * 0.5f + 0.5f;
    }
}
