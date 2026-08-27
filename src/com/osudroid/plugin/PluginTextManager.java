package com.osudroid.plugin;

import android.graphics.Color;
import android.util.SparseArray;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.opengl.font.Font;
import org.anddev.andengine.opengl.font.FontFactory;

import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager;

/**
 * Manages text elements created by Lua plugins.
 *
 * Renders as AndEngine ChangeableText attached to the game scene.
 * This ensures text is in the same render pipeline as the game
 * and is always visible (unlike WindowManager overlay which renders behind SurfaceView).
 */
public class PluginTextManager {

    private static final String TAG = "PluginText";
    private static final SparseArray<ChangeableText> textViews = new SparseArray<>();
    private static int nextHandle = 1;
    private static Entity pluginLayer;
    private static Font pluginFont;

    public static void init(Entity fgScene) {
        pluginLayer = new Entity();
        fgScene.attachChild(pluginLayer);

        try {
            pluginFont = ResourceManager.getInstance().getFont("smallFont");
        } catch (Exception e) {
            pluginFont = ResourceManager.getInstance().getFont("font");
        }
    }

    public static int createText(String text, float x, float y, float sizeSp,
                                  float r, float g, float b, float a) {
        if (pluginLayer == null || pluginFont == null) return -1;

        int handle = nextHandle++;

        ChangeableText ct = new ChangeableText(x, y, pluginFont, text, 256);
        ct.setColor(r, g, b, a);

        pluginLayer.attachChild(ct);
        textViews.put(handle, ct);

        return handle;
    }

    public static void updateText(int handle, String newText) {
        ChangeableText ct = textViews.get(handle);
        if (ct != null) ct.setText(newText);
    }

    public static void moveText(int handle, float x, float y) {
        ChangeableText ct = textViews.get(handle);
        if (ct != null) ct.setPosition(x, y);
    }

    public static void setTextColor(int handle, float r, float g, float b, float a) {
        ChangeableText ct = textViews.get(handle);
        if (ct != null) ct.setColor(r, g, b, a);
    }

    public static void setTextSize(int handle, float sizeSp) {
    }

    public static void setTextOutline(int handle, float r, float g, float b, float a,
                                       float thicknessPx) {
    }

    public static void setTextAlignment(int handle, String alignment) {
    }

    public static void setTextAlpha(int handle, float alpha) {
        ChangeableText ct = textViews.get(handle);
        if (ct != null) ct.setAlpha(alpha);
    }

    public static void setTextVisible(int handle, boolean visible) {
        ChangeableText ct = textViews.get(handle);
        if (ct != null) ct.setVisible(visible);
    }

    public static void destroyText(int handle) {
        ChangeableText ct = textViews.get(handle);
        if (ct != null && pluginLayer != null) {
            pluginLayer.detachChild(ct);
            textViews.remove(handle);
        }
    }

    public static void destroyAll() {
        if (pluginLayer == null) return;
        for (int i = textViews.size() - 1; i >= 0; i--) {
            pluginLayer.detachChild(textViews.valueAt(i));
        }
        textViews.clear();
    }

    public static void destroy() {
        destroyAll();
        pluginLayer = null;
        pluginFont = null;
    }
}
