package com.osudroid.ui;

import org.anddev.andengine.entity.sprite.batch.ColoredSpriteBatch;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ResourceManager;

import com.osudroid.beatmaps.Beatmap;
import com.osudroid.beatmaps.hitobjects.BankHitSampleInfo;
import com.osudroid.beatmaps.hitobjects.HitObject;
import com.osudroid.beatmaps.hitobjects.HitSampleInfo;
import com.osudroid.beatmaps.hitobjects.Slider;
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderTick;

/**
 * Port of osu!lazer's {@code Seasonal.MainMenuSeasonalLighting}.
 *
 * <p>lazer creates a glowing circle for every hit object that passes while the
 * menu track plays. Whistles are red, claps are pale, finishes are larger and
 * slider ticks are smaller pale lights. Drawing those as individual sprites
 * would create a new scene child for every object and a draw call per light, so
 * this implementation submits all active lights through one
 * {@link ColoredSpriteBatch}. Lights are recycled from a fixed pool and the
 * texture is generated once, which keeps the effect at one draw call and no
 * per-hit allocations.</p>
 */
public final class MenuSeasonalLighting extends ColoredSpriteBatch {

    private static final int MAX_LIGHTS = 192;
    /** Hit objects only drive the rhythm; their glows follow a menu pattern. */
    private static final int PATTERN_GARLAND = 0;
    private static final int PATTERN_LEFT = 1;
    private static final int PATTERN_RIGHT = 2;
    private static final int PATTERN_LOGO = 3;
    private static final int PATTERN_PERIOD = 4;

    private static final float GLOW_DIAMETER = 172f;
    private static final float LIGHT_ALPHA = 0.5f;

    private static TextureRegion glowTexture;

    private SeasonalTheme theme = SeasonalTheme.resolve(true);

    private final Light[] lights = new Light[MAX_LIGHTS];
    private int nextLight;

    private HitObject[] hitObjects = new HitObject[0];
    private int lastObjectIndex = -1;
    private double lastPosition = -1;
    private int spawnCount;
    private float garlandPhase;
    private float leftRisePhase;
    private float rightRisePhase;
    private float logoPhase;
    /**
     * Monotonic fade clock. lazer drives light transforms from the container
     * clock, not from the beatmap track, so a seek must not rewind a glow.
     */
    private volatile double lightTime;
    private String beatmapMd5;

    public MenuSeasonalLighting() {
        super(glowTexture().getTexture(), MAX_LIGHTS + 1);
        setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
        setIgnoreUpdate(true);

        for (int i = 0; i < lights.length; i++) {
            lights[i] = new Light();
        }

    }

    /** Switches the colour palette used for new lights. */
    public void setTheme(SeasonalTheme theme) {
        this.theme = theme != null ? theme : SeasonalTheme.resolve(false);
    }

    /**
     * Replaces the lighting source. Called on the update thread after the
     * selected menu beatmap has been parsed with hit objects.
     */
    public void setBeatmap(Beatmap beatmap, String md5) {
        beatmapMd5 = md5;
        lastObjectIndex = -1;
        lastPosition = -1;

        List<HitObject> objects = new ArrayList<>();
        for (HitObject object : beatmap.getHitObjects().objects) {
            objects.add(object);

            if (object instanceof Slider) {
                objects.addAll(((Slider) object).getNestedHitObjects());
            }
        }

        objects.sort(Comparator.comparingDouble(object -> object.startTime));
        hitObjects = objects.toArray(new HitObject[0]);
    }

    public String getBeatmapMd5() {
        return beatmapMd5;
    }

    public void clear() {
        beatmapMd5 = null;
        hitObjects = new HitObject[0];
        lastObjectIndex = -1;
        lastPosition = -1;
        spawnCount = 0;
        garlandPhase = 0f;
        leftRisePhase = 0f;
        rightRisePhase = 0f;
        logoPhase = 0f;
        clearLights();
    }

    /**
     * Advances the lighting clock. The track position only decides which hit
     * objects have passed; fades use a separate monotonic clock, like lazer's
     * container clock, so seeks and pauses cannot rewind or freeze a glow.
     */
    public void update(double positionMs, float frameElapsedMs) {
        lightTime += frameElapsedMs;

        if (hitObjects.length == 0) {
            return;
        }

        // lazer looks 50 ms ahead because the light fades in before the hit.
        double time = positionMs + 50;

        boolean resetIndex = lastObjectIndex < 0
                || lastObjectIndex >= hitObjects.length
                || hitObjects[lastObjectIndex].startTime > time
                || Math.abs(positionMs - lastPosition) > 500;
        if (resetIndex) {
            lastObjectIndex = findLastObjectBefore(time);
        }

        while (lastObjectIndex < hitObjects.length - 1) {
            HitObject object = hitObjects[lastObjectIndex + 1];
            if (object.startTime > time) {
                break;
            }

            // Match lazer: skip the burst on a frame that is already slow.
            if (frameElapsedMs < 20f) {
                addLight(object);
            }

            lastObjectIndex++;
        }

        lastPosition = positionMs;
    }

    private int findLastObjectBefore(double time) {
        int low = 0;
        int high = hitObjects.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (hitObjects[middle].startTime < time) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low - 1;
    }

    private void addLight(HitObject object) {
        Light light = lights[nextLight];
        nextLight = (nextLight + 1) % lights.length;

        light.startTime = object.startTime;
        light.spawnTime = lightTime;
        light.active = true;
        light.tick = object instanceof SliderTick;
        light.scale = light.tick ? 0.5f : 1f;

        if (!light.tick && hasSample(object, BankHitSampleInfo.HIT_FINISH)) {
            light.scale = 3f;
        }

        int color = theme.randomLightColor();
        light.r = ((color >> 16) & 0xFF) / 255f;
        light.g = ((color >> 8) & 0xFF) / 255f;
        light.b = (color & 0xFF) / 255f;

        if (light.tick) {
            light.offsetX = (float) ((Math.random() * 2 - 1) * 20);
            light.offsetY = (float) ((Math.random() * 2 - 1) * 20);
        } else {
            light.offsetX = 0;
            light.offsetY = 0;
        }

        // Hit objects keep the rhythm, but the glows are laid out as menu
        // decorations: a garland across the top, a smooth rise along each
        // side, and a cluster of lights around the logo. Each light keeps
        // moving along its part of the pattern for its whole lifetime. The
        // texture, size, colour and fade timing are unchanged.
        int slot = spawnCount++ % PATTERN_PERIOD;
        light.pattern = slot;
        if (slot == PATTERN_GARLAND) {
            light.phase = garlandPhase;
            garlandPhase += 0.13f;
            if (garlandPhase >= 1f) {
                garlandPhase -= 1f;
            }
        } else if (slot == PATTERN_LEFT || slot == PATTERN_RIGHT) {
            boolean left = slot == PATTERN_LEFT;
            light.phase = left ? leftRisePhase : rightRisePhase;
            if (left) {
                leftRisePhase += 0.11f;
                if (leftRisePhase > 0.7f) {
                    leftRisePhase = 0f;
                }
            } else {
                rightRisePhase += 0.11f;
                if (rightRisePhase > 0.7f) {
                    rightRisePhase = 0f;
                }
            }
        } else {
            light.phase = logoPhase;
            logoPhase += 0.7f;
        }
    }

    /** Moves one glow along its part of the pattern at its current age. */
    private void moveLight(Light light, double age, float width, float height) {
        float margin = GLOW_DIAMETER * 0.5f * light.scale + 24f;
        float usableWidth = Math.max(1f, width - margin * 2f);
        float x;
        float y;

        if (light.pattern == PATTERN_GARLAND) {
            float u = Math.min(1f, light.phase + (float) (age * 0.00028));
            float eased = u * u * (3f - 2f * u);
            float top = Math.max(margin, height * 0.08f);
            x = margin + eased * usableWidth;
            y = top + height * 0.14f * (float) Math.sin(Math.PI * u)
                    + height * 0.02f * (float) Math.sin(u * Math.PI * 4f + age * 0.004f);
        } else if (light.pattern == PATTERN_LEFT || light.pattern == PATTERN_RIGHT) {
            float u = Math.min(1f, light.phase + (float) (age * 0.00034));
            float eased = u * u * (3f - 2f * u);
            float bottom = Math.max(margin, height - margin);
            float top = margin + Math.max(0f, height - margin * 2f) * 0.18f;
            float sway = width * 0.035f
                    * (float) Math.sin(u * Math.PI * 2f + age * 0.003f);
            x = light.pattern == PATTERN_LEFT ? margin + sway : width - margin - sway;
            y = bottom - (bottom - top) * eased;
        } else {
            float angle = light.phase + (float) (age * 0.0016);
            float radius = Math.min(width, height) * 0.20f
                    * (1f + 0.08f * (float) Math.sin(age * 0.006f + light.phase));
            x = width / 2f + (float) Math.cos(angle) * radius;
            y = height / 2f + (float) Math.sin(angle) * radius * 0.55f;
        }

        light.x = x;
        light.y = y;
    }

    private static boolean hasSample(HitObject object, String name) {
        for (HitSampleInfo sample : object.getSamples()) {
            if (sample instanceof BankHitSampleInfo
                    && name.equals(((BankHitSampleInfo) sample).name)) {
                return true;
            }
        }
        return false;
    }

    private void clearLights() {
        for (Light light : lights) {
            light.active = false;
        }
    }

    @Override
    protected boolean onUpdateSpriteBatch() {
        setIndex(0);
        resetColors();

        final float width = Config.getRES_WIDTH();
        final float height = Config.getRES_HEIGHT();

        int rect = 0;
        for (Light light : lights) {
            if (!light.active) {
                continue;
            }

            float alpha = light.alpha(lightTime);
            if (alpha <= 0f) {
                // The fade-in starts at spawn; only retire once the fade-out ends.
                if (lightTime >= light.spawnTime + light.lifetime()) {
                    light.active = false;
                }
                continue;
            }

            double age = Math.max(0.0, lightTime - light.spawnTime);
            moveLight(light, age, width, height);
            float move = light.tick ? easeOut(Math.min(1f, (float) (age / 1400.0))) : 0f;
            float diameter = GLOW_DIAMETER * light.scale;
            float x = light.x + light.offsetX * move - diameter / 2f;
            float y = light.y + light.offsetY * move - diameter / 2f;

            drawWithoutChecks(glowTexture, x, y, diameter, diameter);
            putColor(rect++, light.r, light.g, light.b, alpha);
        }

        setIndex(rect);
        return true;
    }

    private static float easeIn(float value) {
        return value * value;
    }

    private static float easeOut(float value) {
        float inverse = 1f - value;
        return 1f - inverse * inverse;
    }

    /** Shared lazer-style radial glow, also used by the side flashes. */
    public static synchronized TextureRegion getGlowTexture() {
        return glowTexture();
    }

    private static synchronized TextureRegion glowTexture() {
        if (glowTexture == null) {
            // The client already ships the radial white glow used by hit lighting.
            glowTexture = ResourceManager.getInstance().loadHighQualityAsset(
                    "seasonal-glow",
                    "gfx/seasonal-glow.png"
            );
            if (glowTexture == null) {
                glowTexture = ResourceManager.getInstance().getTexture("star");
            }
        }
        return glowTexture;
    }

    private static final class Light {
        float x;
        float y;
        float r;
        float g;
        float b;
        float scale;
        float offsetX;
        float offsetY;
        int pattern;
        float phase;
        double startTime;
        double spawnTime;
        boolean tick;
        boolean active;

        double lifetime() {
            return tick ? 1250.0 : 1150.0;
        }

        float alpha(double time) {
            double age = time - spawnTime;
            if (age < 0) {
                return 0f;
            }

            float alpha;
            if (tick) {
                if (age >= 1250) {
                    return 0f;
                }
                alpha = age < 250
                        ? (float) (age / 250.0)
                        : 1f - easeOut((float) ((age - 250) / 1000.0));
            } else {
                if (age >= 1150) {
                    return 0f;
                }
                alpha = age < 150
                        ? (float) (age / 150.0)
                        : 1f - easeIn((float) ((age - 150) / 1000.0));
            }

            return Math.max(0f, Math.min(1f, alpha)) * LIGHT_ALPHA;
        }
    }

}
