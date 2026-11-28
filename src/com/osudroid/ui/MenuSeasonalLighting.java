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

    private static final float PLAYFIELD_WIDTH = 512f;
    private static final float PLAYFIELD_HEIGHT = 384f;
    private static final float GLOW_DIAMETER = 172f;
    private static final float LIGHT_ALPHA = 0.5f;

    private static TextureRegion glowTexture;

    private SeasonalTheme theme = SeasonalTheme.resolve();

    private final Light[] lights = new Light[MAX_LIGHTS];
    private int nextLight;

    private HitObject[] hitObjects = new HitObject[0];
    private int lastObjectIndex = -1;
    private double lastPosition = -1;
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
        this.theme = theme != null ? theme : SeasonalTheme.resolve();
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

        // Keep the whole glow on screen. lazer stretches the 512x384 playfield
        // to a 16:10 box, which puts lights far outside the visible area on
        // tall phones; using the real screen height and clamping the centre
        // gives the same motion without glows disappearing off-screen.
        float width = Config.getRES_WIDTH();
        float height = Config.getRES_HEIGHT();
        float margin = GLOW_DIAMETER * 0.5f * light.scale + 24f;

        light.x = clampVisible(
                object.getPosition().x / PLAYFIELD_WIDTH * width,
                margin, width);
        light.y = clampVisible(
                object.getPosition().y / PLAYFIELD_HEIGHT * height,
                margin, height);
    }

    private static float clampVisible(float value, float margin, float size) {
        if (size <= margin * 2f) {
            return size / 2f;
        }
        return Math.max(margin, Math.min(size - margin, value));
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

            float move = light.tick ? easeOut(Math.min(1f, (float) ((lightTime - light.spawnTime) / 1400.0))) : 0f;
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
