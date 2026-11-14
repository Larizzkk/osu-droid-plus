package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

/**
 * Easing functions ported exactly from danser-go easing system.
 * Based on https://github.com/wieku/danser-go/blob/master/framework/math/animation/easing/
 */
public class DanserEasing {
    
    /**
     * Easing types matching danser-go
     */
    public enum Type {
        LINEAR,
        IN_QUAD,
        OUT_QUAD,
        IN_OUT_QUAD,
        IN_CUBIC,
        OUT_CUBIC,
        IN_OUT_CUBIC,
        IN_QUART,
        OUT_QUART,
        IN_OUT_QUART,
        IN_QUINT,
        OUT_QUINT,
        IN_OUT_QUINT,
        IN_SINE,
        OUT_SINE,
        IN_OUT_SINE,
        IN_EXPO,
        OUT_EXPO,
        IN_OUT_EXPO,
        IN_CIRC,
        OUT_CIRC,
        IN_OUT_CIRC,
        IN_ELASTIC,
        OUT_ELASTIC,
        IN_OUT_ELASTIC,
        IN_BACK,
        OUT_BACK,
        IN_OUT_BACK,
        IN_BOUNCE,
        OUT_BOUNCE,
        IN_OUT_BOUNCE
    }
    
    /**
     * Apply easing function exactly like danser-go
     */
    public static float apply(Type type, float t) {
        t = DanserUtils.clamp(t, 0f, 1f);
        
        switch (type) {
            case LINEAR:
                return linear(t);
            case IN_QUAD:
                return inQuad(t);
            case OUT_QUAD:
                return outQuad(t);
            case IN_OUT_QUAD:
                return inOutQuad(t);
            case IN_CUBIC:
                return inCubic(t);
            case OUT_CUBIC:
                return outCubic(t);
            case IN_OUT_CUBIC:
                return inOutCubic(t);
            case IN_QUART:
                return inQuart(t);
            case OUT_QUART:
                return outQuart(t);
            case IN_OUT_QUART:
                return inOutQuart(t);
            case IN_QUINT:
                return inQuint(t);
            case OUT_QUINT:
                return outQuint(t);
            case IN_OUT_QUINT:
                return inOutQuint(t);
            case IN_SINE:
                return inSine(t);
            case OUT_SINE:
                return outSine(t);
            case IN_OUT_SINE:
                return inOutSine(t);
            case IN_EXPO:
                return inExpo(t);
            case OUT_EXPO:
                return outExpo(t);
            case IN_OUT_EXPO:
                return inOutExpo(t);
            case IN_CIRC:
                return inCirc(t);
            case OUT_CIRC:
                return outCirc(t);
            case IN_OUT_CIRC:
                return inOutCirc(t);
            case IN_ELASTIC:
                return inElastic(t);
            case OUT_ELASTIC:
                return outElastic(t);
            case IN_OUT_ELASTIC:
                return inOutElastic(t);
            case IN_BACK:
                return inBack(t);
            case OUT_BACK:
                return outBack(t);
            case IN_OUT_BACK:
                return inOutBack(t);
            case IN_BOUNCE:
                return inBounce(t);
            case OUT_BOUNCE:
                return outBounce(t);
            case IN_OUT_BOUNCE:
                return inOutBounce(t);
            default:
                return linear(t);
        }
    }
    
    // Linear easing
    public static float linear(float t) {
        return t;
    }
    
    // Quadratic easing
    public static float inQuad(float t) {
        return t * t;
    }
    
    public static float outQuad(float t) {
        return t * (2.0f - t);
    }
    
    public static float inOutQuad(float t) {
        if (t < 0.5f) {
            return 2.0f * t * t;
        } else {
            return -1.0f + (4.0f - 2.0f * t) * t;
        }
    }
    
    // Cubic easing
    public static float inCubic(float t) {
        return t * t * t;
    }
    
    public static float outCubic(float t) {
        float p = t - 1.0f;
        return p * p * p + 1.0f;
    }
    
    public static float inOutCubic(float t) {
        if (t < 0.5f) {
            return 4.0f * t * t * t;
        } else {
            float p = 2.0f * t - 2.0f;
            return 1.0f + p * p * p / 2.0f;
        }
    }
    
    // Quartic easing
    public static float inQuart(float t) {
        return t * t * t * t;
    }
    
    public static float outQuart(float t) {
        float p = t - 1.0f;
        return 1.0f - p * p * p * p;
    }
    
    public static float inOutQuart(float t) {
        if (t < 0.5f) {
            return 8.0f * t * t * t * t;
        } else {
            float p = t - 1.0f;
            return 1.0f - 8.0f * p * p * p * p;
        }
    }
    
    // Quintic easing
    public static float inQuint(float t) {
        return t * t * t * t * t;
    }
    
    public static float outQuint(float t) {
        float p = t - 1.0f;
        return p * p * p * p * p + 1.0f;
    }
    
    public static float inOutQuint(float t) {
        if (t < 0.5f) {
            return 16.0f * t * t * t * t * t;
        } else {
            float p = t - 1.0f;
            return 1.0f + 16.0f * p * p * p * p * p;
        }
    }
    
    // Sine easing
    public static float inSine(float t) {
        return (float) (1.0f - Math.cos((t * DanserUtils.PI) / 2.0f));
    }
    
    public static float outSine(float t) {
        return (float) Math.sin((t * DanserUtils.PI) / 2.0f);
    }
    
    public static float inOutSine(float t) {
        return (float) (-(Math.cos(DanserUtils.PI * t) - 1.0f) / 2.0f);
    }
    
    // Exponential easing
    public static float inExpo(float t) {
        return t == 0.0f ? 0.0f : (float) Math.pow(2.0f, 10.0f * (t - 1.0f));
    }
    
    public static float outExpo(float t) {
        return t == 1.0f ? 1.0f : (float) (1.0f - Math.pow(2.0f, -10.0f * t));
    }
    
    public static float inOutExpo(float t) {
        if (t == 0.0f) return 0.0f;
        if (t == 1.0f) return 1.0f;
        
        if (t < 0.5f) {
            return (float) (Math.pow(2.0f, 20.0f * t - 10.0f) / 2.0f);
        } else {
            return (float) ((2.0f - Math.pow(2.0f, -20.0f * t + 10.0f)) / 2.0f);
        }
    }
    
    // Circular easing
    public static float inCirc(float t) {
        return 1.0f - (float) Math.sqrt(1.0f - t * t);
    }
    
    public static float outCirc(float t) {
        float p = t - 1.0f;
        return (float) Math.sqrt(1.0f - p * p);
    }
    
    public static float inOutCirc(float t) {
        if (t < 0.5f) {
            return (float) ((1.0f - Math.sqrt(1.0f - 4.0f * t * t)) / 2.0f);
        } else {
            float p = 2.0f * t - 2.0f;
            return (float) ((Math.sqrt(1.0f - p * p) + 1.0f) / 2.0f);
        }
    }
    
    // Elastic easing
    public static float inElastic(float t) {
        if (t == 0.0f || t == 1.0f) {
            return t;
        }
        
        float p = 0.3f;
        float s = p / 4.0f;
        t -= 1.0f;
        
        return (float) (-(Math.pow(2.0f, 10.0f * t) * Math.sin((t - s) * 2.0f * DanserUtils.PI / p)));
    }
    
    public static float outElastic(float t) {
        if (t == 0.0f || t == 1.0f) {
            return t;
        }
        
        float p = 0.3f;
        float s = p / 4.0f;
        
        return (float) (Math.pow(2.0f, -10.0f * t) * Math.sin((t - s) * 2.0f * DanserUtils.PI / p) + 1.0f);
    }
    
    public static float inOutElastic(float t) {
        if (t == 0.0f || t == 1.0f) {
            return t;
        }
        
        float p = 0.3f * 1.5f;
        float s = p / 4.0f;
        t = t * 2.0f - 1.0f;
        
        if (t < 0.0f) {
            return (float) (-0.5f * Math.pow(2.0f, 10.0f * t) * Math.sin((t - s) * 2.0f * DanserUtils.PI / p));
        } else {
            return (float) (Math.pow(2.0f, -10.0f * t) * Math.sin((t - s) * 2.0f * DanserUtils.PI / p) * 0.5f + 1.0f);
        }
    }
    
    // Back easing
    public static float inBack(float t) {
        float s = 1.70158f;
        return t * t * ((s + 1.0f) * t - s);
    }
    
    public static float outBack(float t) {
        float s = 1.70158f;
        float p = t - 1.0f;
        return p * p * ((s + 1.0f) * p + s) + 1.0f;
    }
    
    public static float inOutBack(float t) {
        float s = 1.70158f * 1.525f;
        
        if (t < 0.5f) {
            return (float) (t * t * ((s + 1.0f) * t - s) * 2.0f);
        } else {
            float p = t * 2.0f - 2.0f;
            return (float) (p * p * ((s + 1.0f) * p + s) / 2.0f + 1.0f);
        }
    }
    
    // Bounce easing
    public static float outBounce(float t) {
        if (t < 1.0f / 2.75f) {
            return 7.5625f * t * t;
        } else if (t < 2.0f / 2.75f) {
            t -= 1.5f / 2.75f;
            return 7.5625f * t * t + 0.75f;
        } else if (t < 2.5f / 2.75f) {
            t -= 2.25f / 2.75f;
            return 7.5625f * t * t + 0.9375f;
        } else {
            t -= 2.625f / 2.75f;
            return 7.5625f * t * t + 0.984375f;
        }
    }
    
    public static float inBounce(float t) {
        return 1.0f - outBounce(1.0f - t);
    }
    
    public static float inOutBounce(float t) {
        if (t < 0.5f) {
            return inBounce(t * 2.0f) * 0.5f;
        } else {
            return outBounce(t * 2.0f - 1.0f) * 0.5f + 0.5f;
        }
    }
    
    /**
     * Get easing type by string name (for compatibility with danser-go)
     */
    public static Type getTypeByName(String name) {
        switch (name.toLowerCase()) {
            case "linear": return Type.LINEAR;
            case "inquad": return Type.IN_QUAD;
            case "outquad": return Type.OUT_QUAD;
            case "inoutquad": return Type.IN_OUT_QUAD;
            case "incubic": return Type.IN_CUBIC;
            case "outcubic": return Type.OUT_CUBIC;
            case "inoutcubic": return Type.IN_OUT_CUBIC;
            case "out": return Type.OUT_QUAD; // Default for danser-go
            default: return Type.LINEAR;
        }
    }
}
