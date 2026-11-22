package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import java.util.Locale;

import ru.nsu.ccfit.zuev.osu.Config;

/**
 * danser-go motion blur parameters and their calculated blend weights.
 *
 * <p>danser blends {@code BlendFrames} rendered frames that are
 * {@code OversampleMultiplier} times denser than the output. At 60 output FPS
 * the shutter is therefore {@code (BlendFrames / OversampleMultiplier) / 60}
 * seconds. The shader integrates nine path positions across that shutter using
 * the same {@code 1 + 100 * easing(t)} weights as danser-go.</p>
 */
final class MotionBlurSettings {

    static final int SAMPLES = 9;
    static final int FUNCTION_GAUSS_SYMMETRIC = 27;

    static final int DEFAULT_OVERSAMPLE = 16;
    static final int DEFAULT_BLEND_FRAMES = 24;
    static final int DEFAULT_FUNCTION = FUNCTION_GAUSS_SYMMETRIC;
    static final int DEFAULT_GAUSS_MULTIPLIER_HUNDREDTHS = 150;

    final boolean enabled;
    final int oversampleMultiplier;
    final int blendFrames;
    final int blendFunction;
    final float gaussWeightsMultiplier;
    final String manualWeights;
    final float exposureSeconds;
    final long exposureNanos;
    final long historyWindowNanos;
    final float[] weights = new float[SAMPLES];

    private MotionBlurSettings(
        boolean enabled,
        int oversampleMultiplier,
        int blendFrames,
        int blendFunction,
        float gaussWeightsMultiplier,
        String manualWeights
    ) {
        this.enabled = enabled;
        this.oversampleMultiplier = oversampleMultiplier;
        this.blendFrames = blendFrames;
        this.blendFunction = blendFunction;
        this.gaussWeightsMultiplier = gaussWeightsMultiplier;
        this.manualWeights = manualWeights;

        double outputFrames = (double) blendFrames / oversampleMultiplier;
        this.exposureSeconds = (float) (outputFrames / 60.0);
        this.exposureNanos = Math.max(1L, (long) (exposureSeconds * 1.0e9));
        // Past sampling needs at least the exposure, plus one frame to interpolate.
        this.historyWindowNanos = Math.max(120_000_000L, exposureNanos * 2L);

        buildWeights();
    }

    static MotionBlurSettings load() {
        boolean enabled = Config.getBoolean("cursorDirectionalBlur", true);
        // Density and Gauss calibration stay fixed to danser-go defaults;
        // only the blending curve is user-selectable.
        int oversample = DEFAULT_OVERSAMPLE;
        int frames = DEFAULT_BLEND_FRAMES;
        int function = Math.max(
            0,
            Math.min(Easing.COUNT - 1, Config.getInt("motionBlurBlendFunction", DEFAULT_FUNCTION))
        );
        float gauss = DEFAULT_GAUSS_MULTIPLIER_HUNDREDTHS / 100f;
        String manual = "";
        return new MotionBlurSettings(enabled, oversample, frames, function, gauss, manual);
    }

    boolean sameAs(MotionBlurSettings other) {
        return other != null &&
            enabled == other.enabled &&
            oversampleMultiplier == other.oversampleMultiplier &&
            blendFrames == other.blendFrames &&
            blendFunction == other.blendFunction &&
            Float.compare(gaussWeightsMultiplier, other.gaussWeightsMultiplier) == 0 &&
            manualWeights.equals(other.manualWeights);
    }

    boolean hasCustomWeights() {
        return !manualWeights.isEmpty();
    }

    private void buildWeights() {
        if (parseManualWeights(manualWeights)) {
            return;
        }

        for (int i = 0; i < SAMPLES; i++) {
            double t = SAMPLES <= 1 ? 0.0 : (double) i / (SAMPLES - 1);
            double value = 1.0 + Easing.evaluate(blendFunction, t, gaussWeightsMultiplier) * 100.0;
            // Back easings can overshoot; never feed a negative weight to the shader.
            weights[i] = (float) Math.max(0.0, value);
        }
    }

    /**
     * danser's deprecated manual-weights field accepted a list of weights. It is
     * still useful as an override, so it is resampled linearly to our 9 taps.
     */
    private boolean parseManualWeights(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        String[] parts = text.split("[,;\\s]+");
        if (parts.length == 0) {
            return false;
        }

        float[] parsed = new float[parts.length];
        int count = 0;
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            try {
                float value = Float.parseFloat(part);
                if (!Float.isFinite(value) || value < 0f) {
                    return false;
                }
                parsed[count++] = value;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        if (count == 0) {
            return false;
        }

        boolean nonZero = false;
        for (int i = 0; i < count; i++) {
            nonZero |= parsed[i] > 0f;
        }
        if (!nonZero) {
            return false;
        }

        if (count == 1) {
            for (int i = 0; i < SAMPLES; i++) {
                weights[i] = parsed[0];
            }
            return true;
        }

        for (int i = 0; i < SAMPLES; i++) {
            float position = (SAMPLES <= 1 ? 0f : (float) i / (SAMPLES - 1)) * (count - 1);
            int low = (int) Math.floor(position);
            int high = Math.min(count - 1, low + 1);
            float frac = position - low;
            weights[i] = parsed[low] + (parsed[high] - parsed[low]) * frac;
        }
        return true;
    }

    String describe() {
        return String.format(
            Locale.US,
            "oversample=%d, frames=%d, function=%d, gauss=%.2f, exposure=%.2fms",
            oversampleMultiplier,
            blendFrames,
            blendFunction,
            gaussWeightsMultiplier,
            exposureSeconds * 1000f
        );
    }

    /** Exact ports of danser-go's motion-blur easing functions. */
    private static final class Easing {

        static final int COUNT = 30;

        static double evaluate(int id, double t, double gaussMultiplier) {
            t = Math.max(0.0, Math.min(1.0, t));
            switch (id) {
                case 0: return 1.0; // Flat
                case 1: return t; // Linear
                case 2: return inQuad(t);
                case 3: return outQuad(t);
                case 4: return inOutQuad(t);
                case 5: return inCubic(t);
                case 6: return outCubic(t);
                case 7: return inOutCubic(t);
                case 8: return inQuart(t);
                case 9: return outQuart(t);
                case 10: return inOutQuart(t);
                case 11: return inQuint(t);
                case 12: return outQuint(t);
                case 13: return inOutQuint(t);
                case 14: return inSine(t);
                case 15: return outSine(t);
                case 16: return inOutSine(t);
                case 17: return inExpo(t);
                case 18: return outExpo(t);
                case 19: return inOutExpo(t);
                case 20: return inCirc(t);
                case 21: return outCirc(t);
                case 22: return inOutCirc(t);
                case 23: return inBack(t) + 0.100004; // danser inBack
                case 24: return outBack(t);
                case 25: return inOutBack(t) + 0.100004; // danser inOutBack
                case 26: return gauss(t, gaussMultiplier);
                case 27: return gaussSymmetric(t, gaussMultiplier);
                case 28: return 1.0 - Math.abs(t * 2.0 - 1.0);
                case 29: return Math.sqrt(Math.max(0.0, 1.0 - Math.pow(0.5 - t, 2.0)));
                default: return 1.0;
            }
        }

        private static double inQuad(double t) { return t * t; }
        private static double outQuad(double t) { return -t * (t - 2.0); }

        private static double inOutQuad(double t) {
            if (t < 0.5) return 2.0 * t * t;
            t = 2.0 * t - 1.0;
            return -0.5 * (t * (t - 2.0) - 1.0);
        }

        private static double inCubic(double t) { return t * t * t; }

        private static double outCubic(double t) {
            t -= 1.0;
            return t * t * t + 1.0;
        }

        private static double inOutCubic(double t) {
            t *= 2.0;
            if (t < 1.0) return 0.5 * t * t * t;
            t -= 2.0;
            return 0.5 * (t * t * t + 2.0);
        }

        private static double inQuart(double t) { return t * t * t * t; }

        private static double outQuart(double t) {
            t -= 1.0;
            return -(t * t * t * t - 1.0);
        }

        private static double inOutQuart(double t) {
            t *= 2.0;
            if (t < 1.0) return 0.5 * t * t * t * t;
            t -= 2.0;
            return -0.5 * (t * t * t * t - 2.0);
        }

        private static double inQuint(double t) { return t * t * t * t * t; }

        private static double outQuint(double t) {
            t -= 1.0;
            return t * t * t * t * t + 1.0;
        }

        private static double inOutQuint(double t) {
            t *= 2.0;
            if (t < 1.0) return 0.5 * t * t * t * t * t;
            t -= 2.0;
            return 0.5 * (t * t * t * t * t + 2.0);
        }

        private static double inSine(double t) { return -Math.cos(t * Math.PI / 2.0) + 1.0; }
        private static double outSine(double t) { return Math.sin(t * Math.PI / 2.0); }
        private static double inOutSine(double t) { return -0.5 * (Math.cos(Math.PI * t) - 1.0); }

        private static double inExpo(double t) {
            return t == 0.0 ? 0.0 : Math.pow(2.0, 10.0 * (t - 1.0));
        }

        private static double outExpo(double t) {
            return t == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * t);
        }

        private static double inOutExpo(double t) {
            if (t == 0.0) return 0.0;
            if (t == 1.0) return 1.0;
            if (t < 0.5) return 0.5 * Math.pow(2.0, 20.0 * t - 10.0);
            return 1.0 - 0.5 * Math.pow(2.0, -20.0 * t + 10.0);
        }

        private static double inCirc(double t) { return -(Math.sqrt(1.0 - t * t) - 1.0); }

        private static double outCirc(double t) {
            t -= 1.0;
            return Math.sqrt(1.0 - t * t);
        }

        private static double inOutCirc(double t) {
            t *= 2.0;
            if (t < 1.0) return -0.5 * (Math.sqrt(1.0 - t * t) - 1.0);
            t -= 2.0;
            return 0.5 * (Math.sqrt(1.0 - t * t) + 1.0);
        }

        private static double inBack(double t) {
            final double s = 1.70158;
            return t * t * ((s + 1.0) * t - s);
        }

        private static double outBack(double t) {
            final double s = 1.70158;
            t -= 1.0;
            return t * t * ((s + 1.0) * t + s) + 1.0;
        }

        private static double inOutBack(double t) {
            double s = 1.70158;
            t *= 2.0;
            if (t < 1.0) {
                s *= 1.525;
                return 0.5 * (t * t * ((s + 1.0) * t - s));
            }
            t -= 2.0;
            s *= 1.525;
            return 0.5 * (t * t * ((s + 1.0) * t + s) + 2.0);
        }

        private static double gauss(double t, double multiplier) {
            return Math.exp(-Math.pow(multiplier * (t - 1.0), 2.0));
        }

        private static double gaussSymmetric(double t, double multiplier) {
            return Math.exp(-Math.pow(multiplier * (t * 2.0 - 1.0), 2.0));
        }
    }
}
