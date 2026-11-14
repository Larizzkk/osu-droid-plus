package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import ru.nsu.ccfit.zuev.osu.Config;

/**
 * Centralized mover settings — exact port of danser-go app/settings/dance.go.
 * All defaults match danser-go DefaultsFactory.
 *
 * Reads preferences safely: SeekBarPreference persists Int values, but some
 * keys may have been stored as Float by older builds — handle both to avoid
 * ClassCastException in SharedPreferencesImpl.getInt.
 */
public final class MoverSettings {

    private MoverSettings() {}

    /**
     * Hot-path cache for {@link #getNumber}. Called every frame while a spinner
     * is being spun (SpinnerMoverFactory.radius()) and on every segment
     * configuration; the raw fallback read allocates a full map snapshot, so
     * values are cached per gameplay session instead.
     */
    private static final android.util.SparseArray<float[]> numberCache =
        new android.util.SparseArray<>();

    /** Clears the per-session settings cache (called on each gameplay load/reset). */
    public static void clearCache() {
        synchronized (numberCache) {
            numberCache.clear();
        }
    }

    /** Safe read: value may be stored as Int (SeekBarPreference) or Float (legacy). */
    public static float getNumber(String key, float def) {
        int keyHash = key.hashCode();
        synchronized (numberCache) {
            int idx = numberCache.indexOfKey(keyHash);
            if (idx >= 0) {
                return numberCache.valueAt(idx)[0];
            }
        }

        float result = def;
        try {
            Object v = Config.getRaw(key);
            if (v instanceof Number) {
                result = ((Number) v).floatValue();
            }
        } catch (Exception ignored) {}

        synchronized (numberCache) {
            numberCache.put(keyHash, new float[]{result});
        }
        return result;
    }

    public static boolean getBool(String key, boolean def) {
        try {
            return Config.getBoolean(key, def);
        } catch (Exception e) {
            // May be stored as String "1"/"0"
            try {
                Object v = Config.getRaw(key);
                if (v instanceof String) {
                    return "1".equals(v) || "true".equals(v);
                }
            } catch (Exception ignored) {}
        }
        return def;
    }

    // ── Bezier (dance.go: bezier) ──
    public static float getBezierAggressiveness() {
        return getNumber("bezierAggressiveness", 60f);
    }
    // bezierSliderAggressiveness removed: slider settings were removed per request;
    // the port uses the danser-go default (3) as a constant.

    // ── Linear (dance.go: linear) ──
    public static boolean getLinearWaitForPreempt() {
        return getBool("linearWaitForPreempt", true);
    }
    public static float getLinearReactionTime() {
        return getNumber("linearReactionTime", 100f);
    }
    public static boolean getLinearChoppyLongObjects() {
        return getBool("linearChoppyLongObjects", false);
    }

    // ── Flower / AngleOffset (dance.go: flower) ──
    public static float getFlowerAngleOffset() {
        return getNumber("flowerAngleOffset", 90f);
    }
    public static float getFlowerDistanceMult() {
        // XML stores as percentage: 67 = 0.67x. danser default: 0.666
        return getNumber("flowerDistanceMult", 67f) / 100f;
    }
    public static float getFlowerStreamAngleOffset() {
        return getNumber("flowerStreamAngleOffset", 90f);
    }
    public static float getFlowerLongJump() {
        return getNumber("flowerLongJump", -1f);
    }
    public static float getFlowerLongJumpMult() {
        // XML stores as percentage: 70 = 0.70x. danser default: 0.7
        return getNumber("flowerLongJumpMult", 70f) / 100f;
    }
    public static boolean getFlowerLongJumpOnEqualPos() {
        return getBool("flowerLongJumpOnEqualPos", false);
    }

    // ── HalfCircle / Circular (dance.go: circular) ──
    public static float getCircularRadiusMultiplier() {
        // XML stores as percentage: 100 = 1.00x. danser default: 1.0
        return getNumber("circularRadiusMultiplier", 100f) / 100f;
    }
    public static float getCircularStreamTrigger() {
        return getNumber("circularStreamTrigger", 130f);
    }

    // ── Spline (dance.go: spline) ──
    public static boolean getSplineRotationalForce() {
        return getBool("splineRotationalForce", false);
    }
    public static boolean getSplineStreamHalfCircle() {
        return getBool("splineStreamHalfCircle", true);
    }
    public static boolean getSplineStreamWobble() {
        return getBool("splineStreamWobble", true);
    }
    public static float getSplineWobbleScale() {
        // XML stores as percentage: 67 = 0.67x. danser default: 0.67
        return getNumber("splineWobbleScale", 67f) / 100f;
    }

    // ── Momentum (dance.go: momentum) ──
    public static boolean getMomentumSkipStackAngles() {
        return getBool("momentumSkipStackAngles", false);
    }
    public static boolean getMomentumStreamRestrict() {
        return getBool("momentumStreamRestrict", true);
    }
    public static float getMomentumStreamMult() {
        // XML stores as percentage: 70 = 0.70x. danser default: 0.7
        return getNumber("momentumStreamMult", 70f) / 100f;
    }
    public static float getMomentumDurationMult() {
        // XML stores as percentage: 200 = 2.00x. danser default: 2.0
        return getNumber("momentumDurationMult", 200f) / 100f;
    }
    public static float getMomentumDurationTrigger() {
        return getNumber("momentumDurationTrigger", 500f);
    }
    public static float getMomentumRestrictAngle() {
        return getNumber("momentumRestrictAngle", 90f);
    }
    public static float getMomentumRestrictArea() {
        return getNumber("momentumRestrictArea", 40f);
    }
    public static boolean getMomentumRestrictInvert() {
        return getBool("momentumRestrictInvert", true);
    }
    public static float getMomentumDistanceMult() {
        // XML stores as percentage: 60 = 0.60x. danser default: 0.6
        return getNumber("momentumDistanceMult", 60f) / 100f;
    }
    public static float getMomentumDistanceMultOut() {
        // XML stores as percentage: 45 = 0.45x. danser default: 0.45
        return getNumber("momentumDistanceMultOut", 45f) / 100f;
    }

    // ── ExGon (dance.go: exgon) ──
    public static float getExGonDelay() {
        return getNumber("exgonDelay", 50f);
    }

    // ── Pippi (dance.go: pippi) ──
    public static float getPippiRotationSpeed() {
        // XML stores as percentage: 160 = 1.60x. danser default: 1.6
        return getNumber("pippiRotationSpeed", 160f) / 100f;
    }
    public static float getPippiRadiusMultiplier() {
        // XML stores as percentage: 98 = 0.98x. danser default: 0.98
        return getNumber("pippiRadiusMultiplier", 98f) / 100f;
    }
    public static float getPippiSpinnerRadius() {
        return getNumber("pippiSpinnerRadius", 100f);
    }

    // Aggressive: danser-go has NO settings for this mover (aggressive.go is
    // parameter-free); the slider-geometry knobs that were here were removed
    // per request. The port matches danser-go exactly.

    // ── Spinners (dancenew.go: spinner) ──
    public static String getSpinnerMover() {
        return Config.getString("spinnerMover", "circle");
    }
    public static float getSpinnerRadius() {
        return getNumber("spinnerRadius", 100f);
    }
    // CenterOffsetX/Y removed: danser-go has the settings, but the spin center is
    // the playfield center (256,192 osu!px) — same point the spinner graphics use —
    // and per request no center offsets are kept at all.
}
