package com.osudroid.plugin;

import android.util.SparseArray;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.primitive.Rectangle;

/**
 * Rectangles created by Lua plugins. Useful for progress bars, panels, hit
 * error gauges and similar overlays without shipping textures.
 *
 * <p>All geometry is clamped to sane bounds so a broken plugin cannot create a
 * NaN/Infinity transform or a gigantic quad that stalls the renderer.</p>
 */
public final class PluginRectManager {

    private static final SparseArray<Rectangle> rects = new SparseArray<>();
    private static int nextHandle = 1;
    private static Entity pluginLayer;

    private PluginRectManager() {}

    public static void init(Entity fgScene) {
        pluginLayer = new Entity();
        fgScene.attachChild(pluginLayer);
    }

    public static int addRect(float x, float y, float width, float height,
                              float r, float g, float b, float a) {
        if (pluginLayer == null) {
            return -1;
        }
        int handle = nextHandle++;
        Rectangle rect = new Rectangle(
            clamp(x, -10_000f, 10_000f),
            clamp(y, -10_000f, 10_000f),
            clamp(width, 0f, 4096f),
            clamp(height, 0f, 4096f)
        );
        rect.setColor(clamp(r, 0f, 1f), clamp(g, 0f, 1f), clamp(b, 0f, 1f),
            clamp(a, 0f, 1f));
        pluginLayer.attachChild(rect);
        rects.put(handle, rect);
        return handle;
    }

    public static void moveRect(int handle, float x, float y) {
        Rectangle rect = rects.get(handle);
        if (rect != null) {
            rect.setPosition(clamp(x, -10_000f, 10_000f), clamp(y, -10_000f, 10_000f));
        }
    }

    public static void resizeRect(int handle, float width, float height) {
        Rectangle rect = rects.get(handle);
        if (rect != null) {
            rect.setSize(clamp(width, 0f, 4096f), clamp(height, 0f, 4096f));
        }
    }

    public static void setRectColor(int handle, float r, float g, float b, float a) {
        Rectangle rect = rects.get(handle);
        if (rect != null) {
            rect.setColor(clamp(r, 0f, 1f), clamp(g, 0f, 1f), clamp(b, 0f, 1f),
                clamp(a, 0f, 1f));
        }
    }

    public static void setRectVisible(int handle, boolean visible) {
        Rectangle rect = rects.get(handle);
        if (rect != null) {
            rect.setVisible(visible);
        }
    }

    public static void setRectZIndex(int handle, int index) {
        Rectangle rect = rects.get(handle);
        if (rect != null && pluginLayer != null) {
            if (rect.hasParent()) {
                rect.detachSelf();
            }
            // attachChild(index) silently refuses an out-of-range index, which
            // would leave the rectangle detached forever.
            int target = Math.max(0, Math.min(index, pluginLayer.getChildCount()));
            pluginLayer.attachChild(rect, target);
        }
    }

    public static void destroyRect(int handle) {
        Rectangle rect = rects.get(handle);
        if (rect != null) {
            if (rect.hasParent()) {
                rect.detachSelf();
            }
            rects.remove(handle);
        }
    }

    public static void destroyAll() {
        if (pluginLayer != null) {
            for (int i = rects.size() - 1; i >= 0; i--) {
                Rectangle rect = rects.valueAt(i);
                if (rect.hasParent()) {
                    rect.detachSelf();
                }
            }
        }
        rects.clear();
    }

    public static void destroy() {
        destroyAll();
        pluginLayer = null;
    }

    private static float clamp(float value, float min, float max) {
        if (!Float.isFinite(value)) {
            return 0f;
        }
        return Math.max(min, Math.min(max, value));
    }
}
