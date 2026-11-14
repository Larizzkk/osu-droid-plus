package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;

/**
 * Mathematical utilities ported from danser-go
 * Based on https://github.com/wieku/danser-go/tree/master/framework/math
 */
public class MathUtils {
    
    public static final float EPSILON = 0.00001f;
    public static final float PI = (float) Math.PI;
    public static final float TWO_PI = 2f * PI;
    public static final float HALF_PI = PI / 2f;
    
    // Vector operations exactly from danser-go
    public static PointF newVec2f(float x, float y) {
        return new PointF(x, y);
    }
    
    public static PointF newVec2fRad(float rad, float length) {
        return new PointF(
            (float) Math.cos(rad) * length,
            (float) Math.sin(rad) * length
        );
    }
    
    public static PointF add(PointF a, PointF b) {
        return new PointF(a.x + b.x, a.y + b.y);
    }
    
    public static PointF add(PointF v, float x, float y) {
        return new PointF(v.x + x, v.y + y);
    }
    
    public static PointF sub(PointF a, PointF b) {
        return new PointF(a.x - b.x, a.y - b.y);
    }
    
    public static PointF sub(PointF v, float x, float y) {
        return new PointF(v.x - x, v.y - y);
    }
    
    public static PointF mult(PointF a, PointF b) {
        return new PointF(a.x * b.x, a.y * b.y);
    }
    
    public static PointF mid(PointF a, PointF b) {
        return new PointF((a.x + b.x) / 2f, (a.y + b.y) / 2f);
    }
    
    public static float dot(PointF a, PointF b) {
        return a.x * b.x + a.y * b.y;
    }
    
    public static float dst(PointF a, PointF b) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    public static float dstSq(PointF a, PointF b) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        return dx * dx + dy * dy;
    }
    
    public static float angle(PointF v) {
        return angleR(v) * 180f / PI;
    }
    
    public static float angleR(PointF v) {
        return (float) Math.atan2(v.y, v.x);
    }
    
    public static PointF nor(PointF v) {
        float length = lenSq(v);
        if (length < EPSILON) {
            return v;
        }
        float scale = 1f / (float) Math.sqrt(length);
        return new PointF(v.x * scale, v.y * scale);
    }
    
    public static float angleRV(PointF from, PointF to) {
        return (float) Math.atan2(to.y - from.y, to.x - from.x);
    }
    
    public static PointF lerp(PointF a, PointF b, float t) {
        return new PointF(
            (b.x - a.x) * t + a.x,
            (b.y - a.y) * t + a.y
        );
    }
    
    public static PointF rotate(PointF v, float rad) {
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        return new PointF(
            v.x * cos - v.y * sin,
            v.x * sin + v.y * cos
        );
    }
    
    public static PointF scale(PointF v, float scale) {
        return new PointF(v.x * scale, v.y * scale);
    }
    
    public static float lenSq(PointF v) {
        return v.x * v.x + v.y * v.y;
    }
    
    public static float len(PointF v) {
        return (float) Math.sqrt(lenSq(v));
    }
    
    // Utility functions from danser-go mutils
    public static float clamp(float v, float minV, float maxV) {
        return Math.max(minV, Math.min(maxV, v));
    }
    
    public static float lerp(float min, float max, float t) {
        return min + (max - min) * t;
    }
    
    public static float abs(float a) {
        return a < 0 ? -a : a;
    }
    
    public static int signum(float a) {
        if (a == 0) return 0;
        return a < 0 ? -1 : 1;
    }
    
    public static float sanitize(float v, float maxV) {
        v = v % maxV;
        if (v < 0) {
            v += maxV;
        }
        return v;
    }
    
    public static float sanitizeAngle(float v) {
        return sanitize(v, TWO_PI);
    }
    
    public static float sanitizeAngleArc(float a) {
        float sPi = PI;
        if (a < -sPi) {
            a += 2 * sPi;
        } else if (a >= sPi) {
            a -= 2 * sPi;
        }
        return a;
    }
    
    // Check if value is NaN
    public static boolean isNaN(float value) {
        return Float.isNaN(value);
    }
}
