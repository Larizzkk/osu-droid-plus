package ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.mutils;

/**
 * Exact port of danser-go mutils
 * Based on https://github.com/wieku/danser-go/blob/master/framework/math/mutils/utils.go
 */
public class MUtils {
    
    public static float abs(float a) {
        return a < 0 ? -a : a;
    }
    
    public static int abs(int a) {
        return a < 0 ? -a : a;
    }
    
    public static float clamp(float v, float minV, float maxV) {
        return Math.min(maxV, Math.max(minV, v));
    }
    
    public static int clamp(int v, int minV, int maxV) {
        return Math.min(maxV, Math.max(minV, v));
    }
    
    public static float lerp(float min, float max, float t) {
        return min + (max - min) * t;
    }
    
    public static int lerp(int min, int max, float t) {
        return min + (int) ((max - min) * t);
    }
    
    public static float signum(float a) {
        if (a == 0f) {
            return 0f;
        }
        
        return Math.signum(a);
    }
    
    public static double signum(double a) {
        if (a == 0.0) {
            return 0.0;
        }
        
        return Math.signum(a);
    }
    
    public static float sanitize(float v, float maxV) {
        v = v % maxV;
        if (v < 0) {
            v += maxV;
        }
        return v;
    }
    
    public static double sanitize(double v, double maxV) {
        v = v % maxV;
        if (v < 0) {
            v += maxV;
        }
        return v;
    }
    
    public static float sanitizeAngle(float v) {
        return sanitize(v, (float) (2 * Math.PI));
    }
    
    public static double sanitizeAngle(double v) {
        return sanitize(v, 2 * Math.PI);
    }
    
    public static float sanitizeAngleArc(float a) {
        float sPi = (float) Math.PI;
        
        if (a < -sPi) {
            a += 2 * sPi;
        } else if (a >= sPi) {
            a -= 2 * sPi;
        }
        
        return a;
    }
    
    public static double sanitizeAngleArc(double a) {
        double sPi = Math.PI;
        
        if (a < -sPi) {
            a += 2 * sPi;
        } else if (a >= sPi) {
            a -= 2 * sPi;
        }
        
        return a;
    }
    
    /**
     * Format float with specified precision but removes trailing zeros
     */
    public static String formatWOZeros(float val, int precision) {
        String format = "%." + precision + "f";
        String result = String.format(format, val);
        result = result.replaceAll("0+$", "");
        result = result.replaceAll("\\.$", "");
        return result;
    }
    
    public static String formatWOZeros(double val, int precision) {
        String format = "%." + precision + "f";
        String result = String.format(format, val);
        result = result.replaceAll("0+$", "");
        result = result.replaceAll("\\.$", "");
        return result;
    }
}
