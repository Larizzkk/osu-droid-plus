package ru.nsu.ccfit.zuev.osuplusplus.menu;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager;

public class TriangleBackground extends Entity {

    private final Random random = new Random();
    private final float screenWidth;
    private final float screenHeight;
    private float spawnTimer = 0;
    private TextureRegion tex;

    private static final float INTERVAL = 0.35f;

    private final ArrayList<Tri> active = new ArrayList<>();
    private float kiaiBoostTimer = 0f;

    private static class Tri {
        Sprite s; float sy; float rot; float life; float max; float a0;
    }

    public TriangleBackground() {
        this.screenWidth = Config.getRES_WIDTH();
        this.screenHeight = Config.getRES_HEIGHT();
        this.tex = ResourceManager.getInstance().getTexture("triangle");
        if (this.tex == null) {
            this.tex = ResourceManager.getInstance().loadTexture(
                "triangle", "gfx/triangle.png", false);
        }
        if (this.tex == null) {
            this.tex = ResourceManager.getInstance().getTexture("star");
        }
    }

    @Override
    protected void onManagedUpdate(float dt) {
        super.onManagedUpdate(dt);
        if (tex == null) return;

        if (!ru.nsu.ccfit.zuev.osuplusplus.Config.getBoolean("trianglesEnabled", true)) {
            if (!active.isEmpty()) clearAll();
            return;
        }

        // Kiai boost countdown
        if (kiaiBoostTimer > 0) kiaiBoostTimer -= dt;
        float boost = kiaiBoostTimer > 0 ? 1f + kiaiBoostTimer * 3f : 1f; // speed boost up to 4x
        float alphaBoost = kiaiBoostTimer > 0 ? 1f + kiaiBoostTimer * 2f : 1f; // alpha boost up to 3x

        int maxTri = ru.nsu.ccfit.zuev.osuplusplus.Config.getInt("triangleCount", 25);
        spawnTimer += dt * boost; // spawn faster during boost
        if (spawnTimer >= INTERVAL && active.size() < maxTri) {
            spawnTimer = 0;
            spawn();
        }

        Iterator<Tri> it = active.iterator();
        while (it.hasNext()) {
            Tri t = it.next();
            t.s.setPosition(t.s.getX(), t.s.getY() - t.sy * dt * boost);
            t.s.setRotation(t.s.getRotation() + t.rot * boost * dt);
            t.life += dt;
            if (t.life > t.max) {
                float f = (t.life - t.max) / 1.2f;
                t.s.setAlpha(Math.max(0, t.a0 * (1 - Math.min(1, f)) * alphaBoost));
            } else if (boost > 1) {
                t.s.setAlpha(Math.min(1f, t.a0 * alphaBoost));
            }
            if (t.s.getY() < -32 || t.s.getAlpha() <= 0.001f) {
                detachChild(t.s); it.remove();
            }
        }
    }

    /**
     * Call during kiai moments. Triangles speed up and glow brighter for 1 second.
     */
    public void setKiai(boolean active) {
        if (active) kiaiBoostTimer = 1f;
    }

    private void spawn() {
        float baseSz = ru.nsu.ccfit.zuev.osuplusplus.Config.getInt("triangleSize", 15);
        float sz = Math.max(4, baseSz * (0.3f + random.nextFloat() * 0.7f));
        float x = random.nextFloat() * screenWidth;
        float y = screenHeight + sz;

        Tri t = new Tri();
        t.s = new Sprite(x, y, sz, sz, tex);
        t.s.setAlpha(0.04f + random.nextFloat() * 0.08f);
        t.s.setRotation(random.nextFloat() * 360);
        t.sy = 15 + random.nextFloat() * 100;
        t.rot = (random.nextFloat() - 0.5f) * 50;
        t.life = 0; t.max = 2 + random.nextFloat() * 4; t.a0 = t.s.getAlpha();
        attachChild(t.s);
        active.add(t);
    }

    public void onBeat() {
        int maxTri = ru.nsu.ccfit.zuev.osuplusplus.Config.getInt("triangleCount", 25);
        for (int i = 0; i < 3 && active.size() < maxTri; i++) spawnTimer = INTERVAL;
    }

    public void clearAll() {
        for (Tri t : active) detachChild(t.s);
        active.clear();
    }
}
