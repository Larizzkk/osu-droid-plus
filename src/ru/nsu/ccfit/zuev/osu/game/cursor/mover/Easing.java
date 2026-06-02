package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

/**
 * Easing functions ported from danser-go
 * Based on https://github.com/wieku/danser-go/tree/master/framework/math/animation/easing
 */
public class Easing {
    
    // Linear easing
    public static float linear(float t) {
        return t;
    }
    
    // Quad easing functions - exactly from danser-go
    public static float inQuad(float t) {
        return t * t;
    }
    
    public static float outQuad(float t) {
        return -t * (t - 2);
    }
    
    public static float inOutQuad(float t) {
        if (t < 0.5f) {
            return 2f * t * t;
        }
        return -2f * t * t + 4f * t - 1f;
    }
    
    // Cubic easing functions
    public static float inCubic(float t) {
        return t * t * t;
    }
    
    public static float outCubic(float t) {
        float p = t - 1f;
        return p * p * p + 1f;
    }
    
    public static float inOutCubic(float t) {
        if (t < 0.5f) {
            return 4f * t * t * t;
        }
        float p = 2f * t - 2f;
        return p * p * p / 2f + 1f;
    }
    
    // Quart easing functions
    public static float inQuart(float t) {
        return t * t * t * t;
    }
    
    public static float outQuart(float t) {
        float p = t - 1f;
        return 1f - p * p * p * p;
    }
    
    public static float inOutQuart(float t) {
        if (t < 0.5f) {
            return 8f * t * t * t * t;
        }
        float p = t - 1f;
        return 1f - 8f * p * p * p * p;
    }
    
    // Quint easing functions
    public static float inQuint(float t) {
        return t * t * t * t * t;
    }
    
    public static float outQuint(float t) {
        float p = t - 1f;
        return p * p * p * p * p + 1f;
    }
    
    public static float inOutQuint(float t) {
        if (t < 0.5f) {
            return 16f * t * t * t * t * t;
        }
        float p = 2f * t - 2f;
        return 1f + 16f * p * p * p * p * p;
    }
    
    // Sine easing functions
    public static float inSine(float t) {
        return 1f - (float) Math.cos(t * Math.PI / 2f);
    }
    
    public static float outSine(float t) {
        return (float) Math.sin(t * Math.PI / 2f);
    }
    
    public static float inOutSine(float t) {
        return -0.5f * ((float) Math.cos(Math.PI * t) - 1f);
    }
    
    // Exponential easing functions
    public static float inExpo(float t) {
        return t == 0f ? 0f : (float) Math.pow(2f, 10f * (t - 1f));
    }
    
    public static float outExpo(float t) {
        return t == 1f ? 1f : 1f - (float) Math.pow(2f, -10f * t);
    }
    
    public static float inOutExpo(float t) {
        if (t == 0f) return 0f;
        if (t == 1f) return 1f;
        if (t < 0.5f) {
            return 0.5f * (float) Math.pow(2f, 20f * t - 10f);
        }
        return 0.5f * (2f - (float) Math.pow(2f, -20f * t + 10f));
    }
    
    // Circular easing functions
    public static float inCirc(float t) {
        return 1f - (float) Math.sqrt(1f - t * t);
    }
    
    public static float outCirc(float t) {
        float p = t - 1f;
        return (float) Math.sqrt(1f - p * p);
    }
    
    public static float inOutCirc(float t) {
        if (t < 0.5f) {
            return 0.5f * (1f - (float) Math.sqrt(1f - 4f * t * t));
        }
        float p = 2f * t - 2f;
        return 0.5f * ((float) Math.sqrt(1f - p * p) + 1f);
    }
    
    // Back easing functions
    public static float inBack(float t) {
        float s = 1.70158f;
        return t * t * ((s + 1f) * t - s);
    }
    
    public static float outBack(float t) {
        float s = 1.70158f;
        float p = t - 1f;
        return p * p * ((s + 1f) * p + s) + 1f;
    }
    
    public static float inOutBack(float t) {
        float s = 1.70158f * 1.525f;
        if (t < 0.5f) {
            return 0.5f * (4f * t * t * ((s + 1f) * t - s));
        }
        float p = 2f * t - 2f;
        return 0.5f * (p * p * ((s + 1f) * p + s) + 2f);
    }
    
    // Elastic easing functions
    public static float inElastic(float t) {
        if (t == 0f || t == 1f) return t;
        return -0.5f * (float) Math.pow(2f, 10f * (t - 1f)) * (float) Math.sin((t - 1.1f) * 5f * Math.PI);
    }
    
    public static float outElastic(float t) {
        if (t == 0f || t == 1f) return t;
        return 0.5f * (float) Math.pow(2f, -10f * t) * (float) Math.sin((t - 0.1f) * 5f * Math.PI) + 1f;
    }
    
    public static float inOutElastic(float t) {
        if (t == 0f) return 0f;
        if (t == 1f) return 1f;
        if (t < 0.5f) {
            return -0.5f * (float) Math.pow(2f, 20f * t - 10f) * (float) Math.sin((t - 0.1f) * 5f * Math.PI);
        }
        return 0.5f * (float) Math.pow(2f, -20f * t + 10f) * (float) Math.sin((t - 0.1f) * 5f * Math.PI) + 1f;
    }
    
    // Bounce easing functions
    public static float outBounce(float t) {
        if (t < 0.36363636f) {
            return 7.5625f * t * t;
        } else if (t < 0.72727273f) {
            float p = t - 0.54545456f;
            return 7.5625f * p * p + 0.75f;
        } else if (t < 0.90909091f) {
            float p = t - 0.8181818f;
            return 7.5625f * p * p + 0.9375f;
        } else {
            float p = t - 0.95454544f;
            return 7.5625f * p * p + 0.984375f;
        }
    }
    
    public static float inBounce(float t) {
        return 1f - outBounce(1f - t);
    }
    
    public static float inOutBounce(float t) {
        if (t < 0.5f) {
            return inBounce(2f * t) * 0.5f;
        }
        return outBounce(2f * t - 1f) * 0.5f + 0.5f;
    }
}
