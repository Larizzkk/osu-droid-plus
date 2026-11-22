package ru.nsu.ccfit.zuev.osuplusplus.menu;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;

/**
 * Port of danser-go's triangle background visualiser
 * (app/graphics/gui/drawables/triangles.go).
 *
 * Large triangles rise from below the screen, speed up with the low FFT bins
 * of the playing track and take their colour from the current background.
 * The constants below intentionally match danser-go so the effect behaves the
 * same way it does there.
 */
public class DanserTriangleBackground extends Entity {

    private static final float SEPARATION = 1.4f;
    private static final float MIN_SIZE = 60f;
    private static final float MAX_SIZE = 200f;
    private static final int FFT_BARS = 10;
    private static final int MAX_TRIANGLES = 40;
    private static final float SETTINGS_REFRESH = 0.5f;

    private final Random random = new Random();
    private final float screenWidth = Config.getRES_WIDTH();
    private final float screenHeight = Config.getRES_HEIGHT();
    private final ArrayList<Triangle> triangles = new ArrayList<>(MAX_TRIANGLES);

    private float velocity;
    private boolean firstUpdate = true;
    private int[] colorPalette;

    private TextureRegion triangleTexture;
    private TextureRegion shadowTexture;
    private TextureRegion activeTexture;
    private int textureWidth;
    private int textureHeight;

    private boolean enabled = true;
    private boolean shadowed = true;
    private float density = 1f;
    private float scale = 1f;
    private float speed = 1f;
    private float settingsAge;
    private final float[] fftBars = new float[FFT_BARS];

    private static class Triangle {

        final Sprite sprite;
        final float size;
        final float shade;
        final int colorIndex;

        Triangle(Sprite sprite, float size, float shade, int colorIndex) {
            this.sprite = sprite;
            this.size = size;
            this.shade = shade;
            this.colorIndex = colorIndex;
        }
    }

    public DanserTriangleBackground(String backgroundPath) {
        triangleTexture = getOrLoad("triangle", "gfx/triangle.png");
        shadowTexture = getOrLoad(
            "triangle-shadow",
            "gfx/triangle-shadow.png"
        );
        activeTexture =
            triangleTexture != null ? triangleTexture : shadowTexture;
        if (activeTexture != null) {
            textureWidth = activeTexture.getWidth();
            textureHeight = activeTexture.getHeight();
        }
        refreshSettings();
        setBackgroundPath(backgroundPath);
    }

    /** SharedPreferences reads are batched; a changed setting applies within 0.5 s. */
    private void refreshSettings() {
        enabled = Config.getBoolean("trianglesEnabled", true);
        shadowed = Config.getBoolean("triangleShadowed", true);
        density = Math.max(0f, Config.getInt("triangleDensity", 100) / 100f);
        scale = Math.max(0.1f, Config.getInt("triangleScale", 100) / 100f);
        speed = Math.max(0.1f, Config.getInt("triangleSpeed", 100) / 100f);

        TextureRegion next = shadowed && shadowTexture != null
            ? shadowTexture
            : triangleTexture;
        if (next != null && next != activeTexture) {
            activeTexture = next;
            textureWidth = next.getWidth();
            textureHeight = next.getHeight();
        }
    }

    private TextureRegion getOrLoad(String name, String path) {
        TextureRegion region = ResourceManager.getInstance().getTexture(name);
        if (region == null) {
            region = ResourceManager.getInstance().loadTexture(
                name,
                path,
                false
            );
        }
        return region;
    }

    @Override
    protected void onManagedUpdate(float dt) {
        super.onManagedUpdate(dt);

        settingsAge += dt;
        if (settingsAge >= SETTINGS_REFRESH) {
            settingsAge = 0f;
            refreshSettings();
        }

        if (!enabled || activeTexture == null) {
            if (!triangles.isEmpty()) clearAll();
            return;
        }

        float deltaMs = Math.max(0f, dt * 1000f);
        velocity = Math.max(velocity, fftBoost());
        velocity *= 1f - (0.05f * deltaMs) / 16f;
        if (velocity < 0f) velocity = 0f;

        float currentSpeed = (velocity + 0.5f) * speed;

        for (int i = triangles.size() - 1; i >= 0; i--) {
            Triangle triangle = triangles.get(i);
            Sprite sprite = triangle.sprite;

            float normalisedScale =
                (triangle.size / textureHeight) *
                textureWidth /
                MAX_SIZE /
                (screenHeight / 768f) /
                scale;
            float factor =
                0.2f + (1f - normalisedScale * 0.8f) * SEPARATION;
            sprite.setPosition(
                sprite.getX(),
                sprite.getY() -
                    (deltaMs / 16f) *
                    currentSpeed *
                    factor *
                    (screenHeight / 768f)
            );

            // danser removes a triangle when its centre is fully above the
            // top edge plus half its height, not as soon as its bottom touches 0.
            if (
                sprite.getY() + sprite.getHeight() / 2f <
                -screenHeight / 2f - sprite.getHeight() / 2f
            ) {
                detachChild(sprite);
                triangles.remove(i);
            }
        }

        int target = (int) (MAX_TRIANGLES * density);
        int toAdd = target - triangles.size();
        for (int i = 0; i < toAdd; i++) {
            spawn(firstUpdate);
        }
        if (toAdd > 0) firstUpdate = false;
    }

    private float fftBoost() {
        try {
            var songService = GlobalManager.getInstance().getSongService();
            if (songService == null) return 0f;
            if (!songService.getSpectrum(fftBars, FFT_BARS)) return 0f;

            double boost = 0;
            for (int i = 0; i < FFT_BARS; i++) {
                boost +=
                    (2 * (double) fftBars[i] * fftBars[i] *
                        (FFT_BARS - i)) /
                    FFT_BARS;
            }
            return (float) Math.min(boost * 12, 12);
        } catch (Throwable ignored) {
            return 0f;
        }
    }

    private void spawn(boolean onscreen) {
        float size =
            (MIN_SIZE + random.nextFloat() * (MAX_SIZE - MIN_SIZE)) *
            (screenHeight / 768f) *
            scale;
        float width = size * textureWidth / textureHeight;

        TextureRegion region = activeTexture.deepCopy();
        region.setFlippedVertical(random.nextFloat() >= 0.5f);

        Sprite sprite = new Sprite(0, 0, width, size, region);
        float centerX = (random.nextFloat() - 0.5f) * screenWidth;
        float centerY = screenHeight + size;
        if (onscreen) {
            centerY =
                screenHeight / 2f -
                (random.nextFloat() - 0.5f) * (screenHeight + size);
        }
        sprite.setPosition(
            screenWidth / 2f + centerX - width / 2f,
            centerY - size / 2f
        );

        float shade = random.nextFloat() * 0.2f;
        int colorIndex = random.nextInt(Integer.MAX_VALUE);
        if (colorPalette != null && colorPalette.length > 0) {
            int color = colorPalette[colorIndex % colorPalette.length];
            sprite.setColor(
                ((color >> 16) & 0xff) / 255f,
                ((color >> 8) & 0xff) / 255f,
                (color & 0xff) / 255f,
                1f
            );
        } else {
            sprite.setColor(shade, shade, shade, 1f);
        }

        attachChild(sprite);
        triangles.add(new Triangle(sprite, size, shade, colorIndex));
    }

    /** Offsets the whole field for danser's 0.5x background parallax. */
    public void setParallax(float x, float y) {
        setPosition(x * 0.5f, y * 0.5f);
    }

    /** Rebuilds the palette when the menu/gameplay background changes. */
    public void setBackgroundPath(String path) {
        colorPalette = extractPalette(path);
        applyPalette();
    }

    private void applyPalette() {
        for (Triangle triangle : triangles) {
            if (colorPalette != null && colorPalette.length > 0) {
                int color = colorPalette[triangle.colorIndex % colorPalette.length];
                triangle.sprite.setColor(
                    ((color >> 16) & 0xff) / 255f,
                    ((color >> 8) & 0xff) / 255f,
                    (color & 0xff) / 255f,
                    1f
                );
            } else {
                triangle.sprite.setColor(
                    triangle.shade,
                    triangle.shade,
                    triangle.shade,
                    1f
                );
            }
        }
    }

    public void clearAll() {
        for (Triangle triangle : triangles) {
            detachChild(triangle.sprite);
        }
        triangles.clear();
        velocity = 0f;
        firstUpdate = true;
    }

    /**
     * Extracts a small prominent-colour palette from the background image.
     * danser-go uses prominentcolor's k-means; this keeps the same idea
     * (quantise, skip black, return the most frequent distinct colours)
     * without pulling an image-processing dependency into the APK.
     */
    private static int[] extractPalette(String path) {
        if (path == null || path.isEmpty()) return null;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

            BitmapFactory.Options options = new BitmapFactory.Options();
            int step = Math.max(
                1,
                Math.min(bounds.outWidth, bounds.outHeight) / 96
            );
            options.inSampleSize = step;
            Bitmap bitmap = BitmapFactory.decodeFile(path, options);
            if (bitmap == null) return null;

            Map<Integer, int[]> buckets = new HashMap<>();
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int pixelStep = Math.max(1, Math.min(width, height) / 64);

            for (int y = 0; y < height; y += pixelStep) {
                for (int x = 0; x < width; x += pixelStep) {
                    int pixel = bitmap.getPixel(x, y);
                    int alpha = (pixel >>> 24) & 0xff;
                    if (alpha < 128) continue;
                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >> 8) & 0xff;
                    int b = pixel & 0xff;
                    if (r + g + b == 0) continue;

                    int key = ((r >> 3) << 10) | ((g >> 3) << 5) | (b >> 3);
                    int[] bucket = buckets.get(key);
                    if (bucket == null) {
                        bucket = new int[] { 0, 0, 0, 0 };
                        buckets.put(key, bucket);
                    }
                    bucket[0] += r;
                    bucket[1] += g;
                    bucket[2] += b;
                    bucket[3]++;
                }
            }
            bitmap.recycle();

            List<int[]> sorted = new ArrayList<>(buckets.values());
            sorted.sort((a, b) -> Integer.compare(b[3], a[3]));

            int[] result = new int[10];
            int count = 0;
            for (int[] bucket : sorted) {
                int n = bucket[3];
                int r = bucket[0] / n;
                int g = bucket[1] / n;
                int b = bucket[2] / n;

                boolean tooClose = false;
                for (int i = 0; i < count; i++) {
                    int other = result[i];
                    int dr = r - ((other >> 16) & 0xff);
                    int dg = g - ((other >> 8) & 0xff);
                    int db = b - (other & 0xff);
                    if (dr * dr + dg * dg + db * db < 900) {
                        tooClose = true;
                        break;
                    }
                }
                if (tooClose) continue;

                result[count++] = (0xff << 24) | (r << 16) | (g << 8) | b;
                if (count == result.length) break;
            }

            if (count == 0) return null;
            int[] trimmed = new int[count];
            System.arraycopy(result, 0, trimmed, 0, count);
            return trimmed;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
