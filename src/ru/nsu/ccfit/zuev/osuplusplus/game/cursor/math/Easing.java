package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.math;

/**
 * Easing functions for smooth cursor movement
 * Mimics human hand inertia and natural acceleration/deceleration
 */
public class Easing {
    
    // Pre-allocated constants for performance
    private static final float PI = (float) Math.PI;
    private static final float HALF_PI = PI / 2f;
    
    /**
     * Linear easing (no acceleration)
     */
    public static float linear(float t) {
        return t;
    }
    
    /**
     * Quadratic easing in
     */
    public static float quadIn(float t) {
        return t * t;
    }
    
    /**
     * Quadratic easing out
     */
    public static float quadOut(float t) {
        return t * (2f - t);
    }
    
    /**
     * Quadratic easing in-out
     */
    public static float quadInOut(float t) {
        return t < 0.5f ? 2f * t * t : -1f + (4f - 2f * t) * t;
    }
    
    /**
     * Cubic easing in
     */
    public static float cubicIn(float t) {
        return t * t * t;
    }
    
    /**
     * Cubic easing out
     */
    public static float cubicOut(float t) {
        float tMinus1 = t - 1f;
        return tMinus1 * tMinus1 * tMinus1 + 1f;
    }
    
    /**
     * Cubic easing in-out
     */
    public static float cubicInOut(float t) {
        return t < 0.5f ? 4f * t * t * t : (t - 1f) * (2f * t - 2f) * (2f * t - 2f) + 1f;
    }
    
    /**
     * Quartic easing in
     */
    public static float quartIn(float t) {
        return t * t * t * t;
    }
    
    /**
     * Quartic easing out
     */
    public static float quartOut(float t) {
        float tMinus1 = t - 1f;
        return 1f - tMinus1 * tMinus1 * tMinus1 * tMinus1;
    }
    
    /**
     * Quartic easing in-out
     */
    public static float quartInOut(float t) {
        if (t < 0.5f) {
            return 8f * t * t * t * t;
        } else {
            float tMinus1 = 2f * t - 2f;
            return 1f - 8f * tMinus1 * tMinus1 * tMinus1 * tMinus1;
        }
    }
    
    /**
     * Quintic easing in
     */
    public static float quintIn(float t) {
        return t * t * t * t * t;
    }
    
    /**
     * Quintic easing out
     */
    public static float quintOut(float t) {
        float tMinus1 = t - 1f;
        return 1f + tMinus1 * tMinus1 * tMinus1 * tMinus1 * tMinus1;
    }
    
    /**
     * Quintic easing in-out
     */
    public static float quintInOut(float t) {
        if (t < 0.5f) {
            return 16f * t * t * t * t * t;
        } else {
            float tMinus1 = 2f * t - 2f;
            return 1f + 16f * tMinus1 * tMinus1 * tMinus1 * tMinus1 * tMinus1;
        }
    }
    
    /**
     * Sine easing in
     */
    public static float sineIn(float t) {
        return (float) (1f - Math.cos(t * HALF_PI));
    }
    
    /**
     * Sine easing out
     */
    public static float sineOut(float t) {
        return (float) Math.sin(t * HALF_PI);
    }
    
    /**
     * Sine easing in-out
     */
    public static float sineInOut(float t) {
        return (float) (-0.5f * (Math.cos(PI * t) - 1f));
    }
    
    /**
     * Exponential easing in
     */
    public static float expoIn(float t) {
        return t == 0f ? 0f : (float) Math.pow(2f, 10f * (t - 1f));
    }
    
    /**
     * Exponential easing out
     */
    public static float expoOut(float t) {
        return t == 1f ? 1f : 1f - (float) Math.pow(2f, -10f * t);
    }
    
    /**
     * Exponential easing in-out
     */
    public static float expoInOut(float t) {
        if (t == 0f) return 0f;
        if (t == 1f) return 1f;
        
        if (t < 0.5f) {
            return (float) (0.5f * Math.pow(2f, 20f * t - 10f));
        } else {
            return (float) (0.5f * (2f - Math.pow(2f, -20f * t + 10f)));
        }
    }
    
    /**
     * Circular easing in
     */
    public static float circIn(float t) {
        return 1f - (float) Math.sqrt(1f - t * t);
    }
    
    /**
     * Circular easing out
     */
    public static float circOut(float t) {
        float tMinus1 = t - 1f;
        return (float) Math.sqrt(1f - tMinus1 * tMinus1);
    }
    
    /**
     * Circular easing in-out
     */
    public static float circInOut(float t) {
        return t < 0.5f ? 
            0.5f * (1f - (float) Math.sqrt(1f - 4f * t * t)) :
            0.5f * ((float) Math.sqrt(1f - (2f * t - 2f) * (2f * t - 2f)) + 1f);
    }
    
    /**
     * Back easing in
     */
    public static float backIn(float t) {
        float s = 1.70158f;
        return t * t * ((s + 1f) * t - s);
    }
    
    /**
     * Back easing out
     */
    public static float backOut(float t) {
        float s = 1.70158f;
        float tMinus1 = t - 1f;
        return tMinus1 * tMinus1 * ((s + 1f) * tMinus1 + s) + 1f;
    }
    
    /**
     * Back easing in-out
     */
    public static float backInOut(float t) {
        float s = 1.70158f * 1.525f;
        if (t < 0.5f) {
            return 0.5f * (4f * t * t * ((s + 1f) * t - s));
        } else {
            float tMinus2 = 2f * t - 2f;
            return 0.5f * (tMinus2 * tMinus2 * ((s + 1f) * tMinus2 + s) + 2f);
        }
    }
    
    /**
     * Elastic easing in
     */
    public static float elasticIn(float t) {
        if (t == 0f || t == 1f) return t;
        
        float p = 0.3f;
        float s = p / 4f;
        float tMinus1 = t - 1f;
        
        return -(float) (Math.pow(2f, 10f * tMinus1) * Math.sin((tMinus1 - s) * 2f * PI / p));
    }
    
    /**
     * Elastic easing out
     */
    public static float elasticOut(float t) {
        if (t == 0f || t == 1f) return t;
        
        float p = 0.3f;
        float s = p / 4f;
        
        return (float) (Math.pow(2f, -10f * t) * Math.sin((t - s) * 2f * PI / p) + 1f);
    }
    
    /**
     * Elastic easing in-out
     */
    public static float elasticInOut(float t) {
        if (t == 0f || t == 1f) return t;
        
        float p = 0.3f * 1.5f;
        float s = p / 4f;
        float tMinus2 = 2f * t - 2f;
        
        if (t < 0.5f) {
            return -0.5f * (float) (Math.pow(2f, 10f * tMinus2) * Math.sin((tMinus2 - s) * 2f * PI / p));
        } else {
            return 0.5f * (float) (Math.pow(2f, -10f * tMinus2) * Math.sin((tMinus2 - s) * 2f * PI / p) + 1f);
        }
    }
    
    /**
     * Bounce easing in
     */
    public static float bounceIn(float t) {
        return 1f - bounceOut(1f - t);
    }
    
    /**
     * Bounce easing out
     */
    public static float bounceOut(float t) {
        if (t < 1f / 2.75f) {
            return 7.5625f * t * t;
        } else if (t < 2f / 2.75f) {
            float tMinus1_5 = t - 1.5f / 2.75f;
            return 7.5625f * tMinus1_5 * tMinus1_5 + 0.75f;
        } else if (t < 2.5f / 2.75f) {
            float tMinus2_25 = t - 2.25f / 2.75f;
            return 7.5625f * tMinus2_25 * tMinus2_25 + 0.9375f;
        } else {
            float tMinus2_625 = t - 2.625f / 2.75f;
            return 7.5625f * tMinus2_625 * tMinus2_625 + 0.984375f;
        }
    }
    
    /**
     * Bounce easing in-out
     */
    public static float bounceInOut(float t) {
        return t < 0.5f ? bounceIn(t * 2f) * 0.5f : bounceOut(t * 2f - 1f) * 0.5f + 0.5f;
    }
    
    /**
     * Custom easing for cursor movement - smooth acceleration
     */
    public static float cursorSmooth(float t) {
        // Combination of sine and cubic for natural hand movement
        float sineComponent = sineInOut(t);
        float cubicComponent = cubicInOut(t);
        return sineComponent * 0.7f + cubicComponent * 0.3f;
    }
    
    /**
     * Custom easing for fast cursor movement
     */
    public static float cursorFast(float t) {
        // More aggressive acceleration for fast movements
        return t < 0.3f ? cubicIn(t * 3.333f) * 0.3f : 
               0.3f + cubicOut((t - 0.3f) * 1.428f) * 0.7f;
    }
    
    /**
     * Custom easing for precise cursor movement
     */
    public static float cursorPrecise(float t) {
        // Gentle easing for precise movements
        return sineInOut(t);
    }
    
    /**
     * Ease function enum for easy selection
     */
    public enum Type {
        LINEAR, QUAD_IN, QUAD_OUT, QUAD_INOUT,
        CUBIC_IN, CUBIC_OUT, CUBIC_INOUT,
        QUART_IN, QUART_OUT, QUART_INOUT,
        QUINT_IN, QUINT_OUT, QUINT_INOUT,
        SINE_IN, SINE_OUT, SINE_INOUT,
        EXPO_IN, EXPO_OUT, EXPO_INOUT,
        CIRC_IN, CIRC_OUT, CIRC_INOUT,
        BACK_IN, BACK_OUT, BACK_INOUT,
        ELASTIC_IN, ELASTIC_OUT, ELASTIC_INOUT,
        BOUNCE_IN, BOUNCE_OUT, BOUNCE_INOUT,
        CURSOR_SMOOTH, CURSOR_FAST, CURSOR_PRECISE
    }
    
    /**
     * Apply easing function by type
     */
    public static float apply(Type type, float t) {
        switch (type) {
            case LINEAR: return linear(t);
            case QUAD_IN: return quadIn(t);
            case QUAD_OUT: return quadOut(t);
            case QUAD_INOUT: return quadInOut(t);
            case CUBIC_IN: return cubicIn(t);
            case CUBIC_OUT: return cubicOut(t);
            case CUBIC_INOUT: return cubicInOut(t);
            case QUART_IN: return quartIn(t);
            case QUART_OUT: return quartOut(t);
            case QUART_INOUT: return quartInOut(t);
            case QUINT_IN: return quintIn(t);
            case QUINT_OUT: return quintOut(t);
            case QUINT_INOUT: return quintInOut(t);
            case SINE_IN: return sineIn(t);
            case SINE_OUT: return sineOut(t);
            case SINE_INOUT: return sineInOut(t);
            case EXPO_IN: return expoIn(t);
            case EXPO_OUT: return expoOut(t);
            case EXPO_INOUT: return expoInOut(t);
            case CIRC_IN: return circIn(t);
            case CIRC_OUT: return circOut(t);
            case CIRC_INOUT: return circInOut(t);
            case BACK_IN: return backIn(t);
            case BACK_OUT: return backOut(t);
            case BACK_INOUT: return backInOut(t);
            case ELASTIC_IN: return elasticIn(t);
            case ELASTIC_OUT: return elasticOut(t);
            case ELASTIC_INOUT: return elasticInOut(t);
            case BOUNCE_IN: return bounceIn(t);
            case BOUNCE_OUT: return bounceOut(t);
            case BOUNCE_INOUT: return bounceInOut(t);
            case CURSOR_SMOOTH: return cursorSmooth(t);
            case CURSOR_FAST: return cursorFast(t);
            case CURSOR_PRECISE: return cursorPrecise(t);
            default: return linear(t);
        }
    }
    
    /**
     * Get easing function by string name
     */
    public static Type getTypeFromString(String name) {
        try {
            return Type.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Type.LINEAR;
        }
    }
}
