package com.osudroid.ui;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.text.Text;
import org.anddev.andengine.opengl.font.Font;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ResourceManager;

/**
 * Short beat-synced text flourishes for the seasonal main menu.
 *
 * <p>The glows supply the garland; this layer adds readable nods such as
 * {@code 727}, {@code pp} and {@code Merry christmas}. Downbeats trigger a cue,
 * kiai triggers one on every beat, the beat length sets its lifetime and the
 * audio level sets its size. Cues are pooled, so no entities are created while
 * the menu is running.</p>
 */
public final class MenuSeasonalText extends Entity {

    private static final int MAX_CUES = 8;
    private static final String[] PHRASES = {
        "727", "pp", "Merry christmas", "osu!",
        "droid+", "kiai!", "220", "SS"
    };
    /** Screen fractions; kept clear of the dim bars and the music controls. */
    private static final float[][] POSITIONS = {
        {0.18f, 0.22f}, {0.82f, 0.24f}, {0.50f, 0.17f}, {0.14f, 0.64f},
        {0.86f, 0.62f}, {0.50f, 0.70f}, {0.29f, 0.42f}, {0.71f, 0.44f}
    };

    private final Cue[] cues = new Cue[MAX_CUES];
    private SeasonalTheme theme = SeasonalTheme.resolve(true);
    private double time;
    private int spawnCount;

    public MenuSeasonalText() {
        final Font font = ResourceManager.getInstance().getFont("bigFont");
        for (int i = 0; i < MAX_CUES; i++) {
            final Text text = new Text(0f, 0f, font, PHRASES[i]);
            text.setVisible(false);
            cues[i] = new Cue(text);
            attachChild(text);
        }
    }

    /** Switches the palette used by newly triggered cues. */
    public void setTheme(SeasonalTheme theme) {
        this.theme = theme != null ? theme : SeasonalTheme.resolve(false);
    }

    /** Hides every active cue immediately, e.g. when the effect is disabled. */
    public void clear() {
        for (Cue cue : cues) {
            cue.active = false;
            cue.text.setVisible(false);
        }
    }

    /**
     * Called on a beat. The time signature sets the normal cadence, kiai makes
     * every beat visible, and the beat length plus audio level drive the pop.
     */
    public void onBeat(
        boolean kiai,
        long beatIndex,
        double beatLengthMs,
        int timeSignature,
        float audioLevel
    ) {
        final int cadence = kiai ? 1 : Math.max(2, timeSignature);
        if (beatIndex % cadence != 0) {
            return;
        }

        final int phrase = (int) ((beatIndex / cadence) % PHRASES.length);
        final Cue cue = cues[spawnCount++ % MAX_CUES];
        final Text text = cue.text;

        final float width = Config.getRES_WIDTH();
        final float height = Config.getRES_HEIGHT();
        final float[] anchor = POSITIONS[(spawnCount + phrase) % POSITIONS.length];
        final float jitter = (((phrase * 37 + beatIndex * 13) % 17) - 8) * width * 0.0015f;

        cue.x = width * anchor[0] + jitter - text.getWidth() * 0.5f;
        cue.y = height * anchor[1] - text.getHeight() * 0.5f;

        final float energy = Math.max(0f, Math.min(1f, audioLevel));
        final double beatSeconds = Math.max(0.25, Math.min(1.2, beatLengthMs / 1000.0));
        cue.start = time;
        cue.duration = beatSeconds * (kiai ? 1.35 : 1.8);
        cue.peakAlpha = kiai ? 0.85f : 0.55f;
        cue.rotation = (kiai ? 7f : 3f) * ((phrase & 1) == 0 ? 1f : -1f);
        cue.driftX = (kiai ? 18f : 10f) * ((phrase & 2) == 0 ? 1f : -1f);
        cue.driftY = kiai ? -18f : -10f;

        final float fit = Math.min(1f, width * 0.38f / Math.max(1f, text.getWidth()));
        cue.scale = (kiai ? 1.25f : 0.95f) * (0.9f + 0.35f * energy) * fit;

        final int color = kiai ? theme.randomSideColor() : theme.randomLightColor();
        cue.r = ((color >> 16) & 0xFF) / 255f;
        cue.g = ((color >> 8) & 0xFF) / 255f;
        cue.b = (color & 0xFF) / 255f;

        text.setPosition(cue.x, cue.y);
        text.setScale(cue.scale);
        text.setRotation(0f);
        text.setColor(cue.r, cue.g, cue.b, 0f);
        text.setVisible(true);
        cue.active = true;
    }

    @Override
    public void onUpdate(final float pSecondsElapsed) {
        time += pSecondsElapsed;

        for (Cue cue : cues) {
            if (!cue.active) {
                continue;
            }

            final double t = (time - cue.start) / cue.duration;
            if (t < 0.0 || t >= 1.0) {
                cue.active = false;
                cue.text.setVisible(false);
                continue;
            }

            final float progress = (float) t;
            float alpha = cue.peakAlpha;
            if (progress < 0.18f) {
                alpha *= progress / 0.18f;
            } else if (progress > 0.62f) {
                alpha *= (1f - progress) / 0.38f;
            }

            final float settle = 1f - easeOut(progress);
            cue.text.setScale(cue.scale * (1f + 0.28f * settle));
            cue.text.setRotation(cue.rotation * settle);
            cue.text.setPosition(
                cue.x + cue.driftX * easeOut(progress),
                cue.y + cue.driftY * easeOut(progress)
            );
            cue.text.setColor(cue.r, cue.g, cue.b, alpha);
        }

        super.onUpdate(pSecondsElapsed);
    }

    private static float easeOut(float value) {
        final float inverse = 1f - value;
        return 1f - inverse * inverse;
    }

    private static final class Cue {
        final Text text;
        double start;
        double duration;
        float x;
        float y;
        float r;
        float g;
        float b;
        float scale;
        float rotation;
        float driftX;
        float driftY;
        float peakAlpha;
        boolean active;

        Cue(Text text) {
            this.text = text;
        }
    }
}
