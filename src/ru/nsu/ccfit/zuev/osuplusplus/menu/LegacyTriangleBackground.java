package ru.nsu.ccfit.zuev.osuplusplus.menu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.entity.sprite.batch.ColoredSpriteBatch;
import org.anddev.andengine.opengl.texture.ITexture;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ResourceManager;

/**
 * Original osu!droid+ particle visualiser.
 *
 * <p>The old implementation created one AndEngine {@code Sprite} per particle,
 * which meant hundreds of scene children, matrix pushes and draw calls. This
 * port keeps the same particle simulation but renders every visible particle
 * through a single {@link ColoredSpriteBatch}, so the whole background costs
 * one draw call and no per-frame object allocations.</p>
 */
public class LegacyTriangleBackground extends ColoredSpriteBatch {

    private static final float INTERVAL = 0.35f;
    private static final float EDGE_FADE_DIST = 80f;
    private static final float REMOVE_DIST = 80f;
    private static final int POOL_SIZE = 300;
    private static final float SETTINGS_REFRESH = 0.5f;

    private final Random random = new Random();
    private final float screenWidth;
    private final float screenHeight;
    private float spawnTimer;

    private final ArrayList<Particle> active = new ArrayList<>(128);
    private final ArrayList<Particle> pool = new ArrayList<>(POOL_SIZE);

    private final TextureRegion triangleTexture;
    private final TextureRegion circleTexture;
    private final TextureRegion squareTexture;
    private TextureRegion activeTexture;

    private boolean enabled = true;
    private int maxTriangles = 25;
    private boolean rotateEnabled = true;
    private char direction = 'd';
    private float brightness = 1f;
    private float kiaiBoostTimer;
    private float settingsAge;

    private static final class Particle {

        float x;
        float y;
        float size;
        float rotation;
        float rotationSpeed;
        float speedX;
        float speedY;
        float life;
        float maxLife;
        float baseAlpha;
        float alpha;

        static Particle obtain(ArrayList<Particle> pool) {
            return pool.isEmpty() ? new Particle() : pool.remove(pool.size() - 1);
        }

        void free(ArrayList<Particle> pool) {
            if (pool.size() < POOL_SIZE) {
                pool.add(this);
            }
        }
    }

    public LegacyTriangleBackground() {
        super(requireInitialTexture().getTexture(), POOL_SIZE);

        triangleTexture = getOrLoad("triangle", "gfx/triangle.png");
        circleTexture = getOrLoad("circle", "gfx/circle.png");
        squareTexture = getOrLoad("square", "gfx/square.png");
        activeTexture = valid(triangleTexture) ? triangleTexture : valid(circleTexture)
            ? circleTexture
            : requireInitialTexture();

        screenWidth = Config.getRES_WIDTH();
        screenHeight = Config.getRES_HEIGHT();

        this.mTexture = activeTexture.getTexture();
        setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        refreshSettings();
    }

    private static TextureRegion requireInitialTexture() {
        TextureRegion texture = getOrLoad("triangle", "gfx/triangle.png");
        if (valid(texture)) {
            return texture;
        }
        texture = getOrLoad("cursor", "gfx/cursor.png");
        if (valid(texture)) {
            return texture;
        }
        texture = getOrLoad("hitcircle", "gfx/hitcircle.png");
        if (valid(texture)) {
            return texture;
        }
        throw new IllegalStateException("No texture available for legacy triangles");
    }

    private static boolean valid(TextureRegion region) {
        return region != null && region.getTexture() != null;
    }

    private static TextureRegion getOrLoad(String name, String path) {
        ResourceManager resources = ResourceManager.getInstance();
        TextureRegion region = resources.getTexture(name);
        if (!valid(region)) {
            region = resources.loadTexture(name, path, false);
        }
        return valid(region) ? region : null;
    }

    private void refreshSettings() {
        enabled = Config.getBoolean("trianglesEnabled", true);
        maxTriangles = Math.max(1, Math.min(POOL_SIZE, Config.getInt("triangleCount", 25)));
        rotateEnabled = Config.getBoolean("triangleRotate", true);

        String selected = Config.getString("triangleDirection", "down");
        direction = selected == null || selected.isEmpty() ? 'd' : selected.charAt(0);

        String shape = Config.getString("triangleShape", "triangle");
        TextureRegion next;
        switch (shape == null ? "" : shape) {
            case "circle":
                next = valid(circleTexture) ? circleTexture : triangleTexture;
                break;
            case "square":
                next = valid(squareTexture) ? squareTexture : triangleTexture;
                break;
            default:
                next = triangleTexture;
                break;
        }

        if (!valid(next)) {
            next = valid(circleTexture) ? circleTexture : requireInitialTexture();
        }

        if (next != activeTexture) {
            activeTexture = next;
            this.mTexture = next.getTexture();
        }
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
            if (!active.isEmpty()) {
                clearAll();
            }
            return;
        }

        if (kiaiBoostTimer > 0f) {
            kiaiBoostTimer -= dt;
        }
        float boost = kiaiBoostTimer > 0f ? 1f + kiaiBoostTimer * 3f : 1f;
        float alphaBoost = kiaiBoostTimer > 0f ? 1f + kiaiBoostTimer * 2f : 1f;

        spawnTimer += dt * boost;
        if (spawnTimer >= INTERVAL && active.size() < maxTriangles) {
            spawnTimer = 0f;
            spawn();
        }

        Iterator<Particle> iterator = active.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();

            switch (direction) {
                case 'l':
                    particle.x -= particle.speedX * dt * boost;
                    break;
                case 'r':
                    particle.x += particle.speedX * dt * boost;
                    break;
                default:
                    particle.y -= particle.speedY * dt * boost;
                    break;
            }

            if (rotateEnabled) {
                particle.rotation += particle.rotationSpeed * boost * dt;
            }

            particle.life += dt;
            if (particle.life > particle.maxLife) {
                float fade = (particle.life - particle.maxLife) / 1.2f;
                particle.alpha = Math.max(
                    0f,
                    particle.baseAlpha * (1f - Math.min(1f, fade)) * alphaBoost
                );
            } else if (boost > 1f) {
                particle.alpha = Math.min(1f, particle.baseAlpha * alphaBoost);
            }

            float edgeFade = 1f;
            switch (direction) {
                case 'l':
                    edgeFade = Math.min(
                        1f,
                        Math.max(0f, (particle.x + REMOVE_DIST) / EDGE_FADE_DIST)
                    );
                    break;
                case 'r':
                    edgeFade = Math.min(
                        1f,
                        Math.max(
                            0f,
                            (screenWidth + REMOVE_DIST - particle.x) / EDGE_FADE_DIST
                        )
                    );
                    break;
                default:
                    edgeFade = Math.min(
                        1f,
                        Math.max(0f, (particle.y + REMOVE_DIST) / EDGE_FADE_DIST)
                    );
                    break;
            }
            particle.alpha = Math.min(particle.alpha, edgeFade);

            boolean remove;
            switch (direction) {
                case 'l':
                    remove = particle.x < -REMOVE_DIST;
                    break;
                case 'r':
                    remove = particle.x > screenWidth + REMOVE_DIST;
                    break;
                default:
                    remove = particle.y < -REMOVE_DIST;
                    break;
            }

            if (remove || particle.alpha <= 0.001f) {
                particle.free(pool);
                iterator.remove();
            }
        }
    }

    @Override
    protected boolean onUpdateSpriteBatch() {
        setIndex(0);
        resetColors();

        if (!enabled || activeTexture == null || active.isEmpty()) {
            return true;
        }

        final float tint = brightness;
        for (int i = 0; i < active.size(); i++) {
            Particle particle = active.get(i);
            drawWithoutChecks(
                activeTexture,
                particle.x - particle.size * 0.5f,
                particle.y - particle.size * 0.5f,
                particle.size,
                particle.size,
                particle.rotation
            );
            putColor(i, tint, tint, tint, particle.alpha);
        }
        return true;
    }

    public void setKiai(boolean active) {
        if (active) {
            kiaiBoostTimer = 1f;
        }
    }

    private void spawn() {
        float baseSize = Config.getInt("triangleSize", 15);
        float size = Math.max(4f, baseSize * (0.3f + random.nextFloat() * 0.7f));

        float x;
        float y;
        switch (direction) {
            case 'l':
                x = screenWidth + size;
                y = random.nextFloat() * screenHeight;
                break;
            case 'r':
                x = -size;
                y = random.nextFloat() * screenHeight;
                break;
            default:
                x = random.nextFloat() * screenWidth;
                y = screenHeight + size;
                break;
        }

        Particle particle = Particle.obtain(pool);
        particle.x = x;
        particle.y = y;
        particle.size = size;
        particle.rotation = random.nextFloat() * 360f;
        particle.rotationSpeed = (random.nextFloat() - 0.5f) * 50f;
        particle.speedX = 15f + random.nextFloat() * 100f;
        particle.speedY = 15f + random.nextFloat() * 100f;
        particle.life = 0f;
        particle.maxLife = 2f + random.nextFloat() * 4f;
        particle.baseAlpha = 0.04f + random.nextFloat() * 0.08f;
        particle.alpha = particle.baseAlpha;
        active.add(particle);
    }

    public void onBeat() {
        for (int i = 0; i < 3 && active.size() < maxTriangles; i++) {
            spawn();
        }
    }

    /** Applies the same background dim as the danser triangles. */
    public void setBrightness(float brightness) {
        this.brightness = Math.max(0f, Math.min(1f, brightness));
    }

    public void clearAll() {
        for (Particle particle : active) {
            particle.free(pool);
        }
        active.clear();
        spawnTimer = 0f;
    }
}
