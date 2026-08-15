package ru.nsu.ccfit.zuev.osu.menu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ResourceManager;

public class TriangleBackground extends Entity {

    private final Random random = new Random();
    private final float screenWidth;
    private final float screenHeight;
    private float spawnTimer = 0;

    private static final float INTERVAL = 0.35f;
    private static final float EDGE_FADE_DIST = 80f;
    private static final float REMOVE_DIST = 80f;
    private static final int POOL_SIZE = 300;

    private final ArrayList<Particle> active = new ArrayList<>(128);
    private final ArrayList<Particle> pool = new ArrayList<>(POOL_SIZE);
    private float kiaiBoostTimer = 0f;

    private TextureRegion texTriangle, texCircle, texSquare, currentTex;

    private static class Particle {

        Sprite s;
        float sx, sy;
        float rot;
        float life;
        float max;
        float a0;

        static Particle obtain(
            ArrayList<Particle> pool,
            TextureRegion tex,
            float sz
        ) {
            int size = pool.size();
            if (size > 0) {
                Particle p = pool.remove(size - 1);
                p.s.setSize(sz, sz);
                return p;
            }
            Particle p = new Particle();
            p.s = new Sprite(0, 0, sz, sz, tex);
            return p;
        }

        void free(ArrayList<Particle> pool) {
            if (pool.size() < POOL_SIZE) {
                if (s != null && s.hasParent()) s.detachSelf();
                pool.add(this);
            }
        }
    }

    public TriangleBackground() {
        this.screenWidth = Config.getRES_WIDTH();
        this.screenHeight = Config.getRES_HEIGHT();
        preloadTextures();
    }

    private void preloadTextures() {
        texTriangle = getOrLoad("triangle", "gfx/triangle.png");
        if (texTriangle == null) texTriangle =
            ResourceManager.getInstance().getTexture("star");
        texCircle = getOrLoad("circle", "gfx/circle.png");
        texSquare = getOrLoad("square", "gfx/square.png");
        currentTex = texTriangle;
    }

    private TextureRegion getOrLoad(String name, String path) {
        TextureRegion t = ResourceManager.getInstance().getTexture(name);
        if (t == null) t = ResourceManager.getInstance().loadTexture(
            name,
            path,
            false
        );
        return t;
    }

    private TextureRegion getShapeTexture() {
        String shape = ru.nsu.ccfit.zuev.osuplusplus.Config.getString(
            "triangleShape",
            "triangle"
        );
        TextureRegion tex;
        switch (shape) {
            case "circle":
                tex = texCircle;
                break;
            case "square":
                tex = texSquare;
                break;
            default:
                tex = texTriangle;
                break;
        }
        return tex != null ? tex : texTriangle;
    }

    @Override
    protected void onManagedUpdate(float dt) {
        super.onManagedUpdate(dt);

        currentTex = getShapeTexture();
        if (currentTex == null) return;

        if (
            !ru.nsu.ccfit.zuev.osuplusplus.Config.getBoolean(
                "trianglesEnabled",
                true
            )
        ) {
            if (!active.isEmpty()) clearAll();
            return;
        }

        if (kiaiBoostTimer > 0) kiaiBoostTimer -= dt;
        float boost = kiaiBoostTimer > 0 ? 1f + kiaiBoostTimer * 3f : 1f;
        float alphaBoost = kiaiBoostTimer > 0 ? 1f + kiaiBoostTimer * 2f : 1f;

        int maxTri = ru.nsu.ccfit.zuev.osuplusplus.Config.getInt(
            "triangleCount",
            25
        );
        spawnTimer += dt * boost;
        if (spawnTimer >= INTERVAL && active.size() < maxTri) {
            spawnTimer = 0;
            spawn();
        }

        boolean rotateEnabled = ru.nsu.ccfit.zuev.osuplusplus.Config.getBoolean(
            "triangleRotate",
            true
        );
        String direction = ru.nsu.ccfit.zuev.osuplusplus.Config.getString(
            "triangleDirection",
            "down"
        );

        Iterator<Particle> it = active.iterator();
        while (it.hasNext()) {
            Particle p = it.next();

            float dx = 0,
                dy = 0;
            switch (direction.charAt(0)) {
                case 'l':
                    dx = -p.sx * dt * boost;
                    break; // "left"
                case 'r':
                    dx = p.sx * dt * boost;
                    break; // "right"
                default:
                    dy = -p.sy * dt * boost;
                    break; // "down"
            }
            p.s.setPosition(p.s.getX() + dx, p.s.getY() + dy);

            if (rotateEnabled) p.s.setRotation(
                p.s.getRotation() + p.rot * boost * dt
            );

            p.life += dt;
            if (p.life > p.max) {
                float f = (p.life - p.max) / 1.2f;
                p.s.setAlpha(
                    Math.max(0, p.a0 * (1 - Math.min(1, f)) * alphaBoost)
                );
            } else if (boost > 1) {
                p.s.setAlpha(Math.min(1f, p.a0 * alphaBoost));
            }

            float edgeFade = 1f;
            float px = p.s.getX(),
                py = p.s.getY();
            switch (direction.charAt(0)) {
                case 'l':
                    edgeFade = Math.min(
                        1f,
                        Math.max(0f, (px + REMOVE_DIST) / EDGE_FADE_DIST)
                    );
                    break;
                case 'r':
                    edgeFade = Math.min(
                        1f,
                        Math.max(
                            0f,
                            (screenWidth + REMOVE_DIST - px) / EDGE_FADE_DIST
                        )
                    );
                    break;
                default:
                    edgeFade = Math.min(
                        1f,
                        Math.max(0f, (py + REMOVE_DIST) / EDGE_FADE_DIST)
                    );
                    break;
            }
            p.s.setAlpha(Math.min(p.s.getAlpha(), edgeFade));

            boolean remove;
            switch (direction.charAt(0)) {
                case 'l':
                    remove = px < -REMOVE_DIST;
                    break;
                case 'r':
                    remove = px > screenWidth + REMOVE_DIST;
                    break;
                default:
                    remove = py < -REMOVE_DIST;
                    break;
            }
            if (remove || p.s.getAlpha() <= 0.001f) {
                detachChild(p.s);
                p.free(pool);
                it.remove();
            }
        }
    }

    public void setKiai(boolean active) {
        if (active) kiaiBoostTimer = 1f;
    }

    private void spawn() {
        float baseSz = ru.nsu.ccfit.zuev.osuplusplus.Config.getInt(
            "triangleSize",
            15
        );
        float sz = Math.max(4, baseSz * (0.3f + random.nextFloat() * 0.7f));
        String direction = ru.nsu.ccfit.zuev.osuplusplus.Config.getString(
            "triangleDirection",
            "down"
        );

        float x, y;
        switch (direction.charAt(0)) {
            case 'l':
                x = screenWidth + sz;
                y = random.nextFloat() * screenHeight;
                break;
            case 'r':
                x = -sz;
                y = random.nextFloat() * screenHeight;
                break;
            default:
                x = random.nextFloat() * screenWidth;
                y = screenHeight + sz;
                break;
        }

        Particle p = Particle.obtain(pool, currentTex, sz);
        p.s.setPosition(x, y);
        p.s.setAlpha(0.04f + random.nextFloat() * 0.08f);
        p.s.setRotation(random.nextFloat() * 360);
        p.s.clearEntityModifiers();
        p.s.setVisible(true);
        p.sx = 15 + random.nextFloat() * 100;
        p.sy = 15 + random.nextFloat() * 100;
        p.rot = (random.nextFloat() - 0.5f) * 50;
        p.life = 0;
        p.max = 2 + random.nextFloat() * 4;
        p.a0 = p.s.getAlpha();
        attachChild(p.s);
        active.add(p);
    }

    public void onBeat() {
        int maxTri = ru.nsu.ccfit.zuev.osuplusplus.Config.getInt(
            "triangleCount",
            25
        );
        for (int i = 0; i < 3 && active.size() < maxTri; i++) {
            spawn();
        }
    }

    public void clearAll() {
        for (Particle p : active) {
            detachChild(p.s);
            p.free(pool);
        }
        active.clear();
    }
}
