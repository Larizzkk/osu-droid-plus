package com.osudroid.plugin;

import android.util.SparseArray;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager;

/**
 * Manages sprites created by Lua plugins.
 * Sprites are added to a dedicated Entity layer on top of the game scene.
 * Using Entity (not Scene) to avoid breaking AndEngine rendering.
 */
public class PluginSpriteManager {

    private static final SparseArray<Sprite> sprites = new SparseArray<>();
    private static int nextHandle = 1;
    private static Entity pluginLayer;

    /**
     * Initialize with the foreground scene. Creates an Entity layer on top.
     */
    public static void init(Entity fgScene) {
        pluginLayer = new Entity();
        fgScene.attachChild(pluginLayer);
    }

    public static int addSprite(String textureName, float x, float y) {
        if (pluginLayer == null) return -1;

        TextureRegion tex = ResourceManager.getInstance().getTexture(textureName);
        if (tex == null) return -1;

        int handle = nextHandle++;
        Sprite sprite = new Sprite(x - tex.getWidth() / 2f, y - tex.getHeight() / 2f, tex);
        pluginLayer.attachChild(sprite);
        sprites.put(handle, sprite);

        return handle;
    }

    public static void moveSprite(int handle, float x, float y) {
        Sprite s = sprites.get(handle);
        if (s != null) {
            TextureRegion tex = s.getTextureRegion();
            float hw = tex != null ? tex.getWidth() / 2f : 0;
            float hh = tex != null ? tex.getHeight() / 2f : 0;
            s.setPosition(x - hw, y - hh);
        }
    }

    public static void scaleSprite(int handle, float scale) {
        Sprite s = sprites.get(handle);
        if (s != null) s.setScale(scale);
    }

    public static void setSpriteAlpha(int handle, float alpha) {
        Sprite s = sprites.get(handle);
        if (s != null) s.setAlpha(alpha);
    }

    public static void setSpriteColor(int handle, float r, float g, float b, float a) {
        Sprite s = sprites.get(handle);
        if (s != null) s.setColor(r, g, b, a);
    }

    public static void setSpriteRotation(int handle, float degrees) {
        Sprite s = sprites.get(handle);
        if (s != null) s.setRotation(degrees);
    }

    public static void setSpriteVisible(int handle, boolean visible) {
        Sprite s = sprites.get(handle);
        if (s != null) s.setVisible(visible);
    }

    public static void setSpriteZIndex(int handle, int index) {
        Sprite s = sprites.get(handle);
        if (s != null && pluginLayer != null) {
            pluginLayer.attachChild(s, index);
        }
    }

    public static void destroySprite(int handle) {
        Sprite s = sprites.get(handle);
        if (s != null && pluginLayer != null) {
            pluginLayer.detachChild(s);
            sprites.remove(handle);
        }
    }

    /**
     * Remove all plugin sprites.
     */
    public static void destroyAll() {
        if (pluginLayer == null) return;
        for (int i = sprites.size() - 1; i >= 0; i--) {
            pluginLayer.detachChild(sprites.valueAt(i));
        }
        sprites.clear();
    }

    /**
     * Remove the entire plugin layer from the scene.
     */
    public static void destroy() {
        destroyAll();
        pluginLayer = null;
    }
}
