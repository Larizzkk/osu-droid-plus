package com.osudroid.plugin;

import android.util.SparseArray;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.opengl.font.Font;

import ru.nsu.ccfit.zuev.osu.ResourceManager;

/**
 * Manages text elements created by Lua plugins.
 *
 * <p>AndEngine fonts are immutable, so a requested size is mapped to the
 * closest bundled font; changing the size rebuilds the entity in place while
 * preserving position, colour, alpha and visibility.</p>
 */
public final class PluginTextManager {

    private static final int MAX_TEXT_LENGTH = 256;
    private static final SparseArray<ChangeableText> textViews = new SparseArray<>();
    private static final SparseArray<String> textValues = new SparseArray<>();
    private static final SparseArray<Font> textFonts = new SparseArray<>();
    private static int nextHandle = 1;
    private static Entity pluginLayer;
    private static Font defaultFont;

    private PluginTextManager() {}

    public static void init(Entity fgScene) {
        pluginLayer = new Entity();
        fgScene.attachChild(pluginLayer);
        defaultFont = ResourceManager.getInstance().getFont("smallFont");
    }

    private static Font fontForSize(float sizeSp) {
        ResourceManager resources = ResourceManager.getInstance();
        if (sizeSp > 26f) {
            Font font = resources.getFont("font");
            return font != null ? font : defaultFont;
        }
        if (sizeSp > 22f) {
            Font font = resources.getFont("middleFont");
            return font != null ? font : defaultFont;
        }
        Font font = resources.getFont("smallFont");
        return font != null ? font : defaultFont;
    }

    public static int createText(String text, float x, float y, float sizeSp,
                                  float r, float g, float b, float a) {
        Font font = fontForSize(sizeSp);
        if (pluginLayer == null || font == null) {
            return -1;
        }

        int handle = nextHandle++;
        String value = clampText(text);

        ChangeableText ct = new ChangeableText(x, y, font, value, MAX_TEXT_LENGTH);
        ct.setColor(r, g, b, a);

        pluginLayer.attachChild(ct);
        textViews.put(handle, ct);
        textValues.put(handle, value);
        textFonts.put(handle, font);
        return handle;
    }

    public static void updateText(int handle, String newText) {
        ChangeableText ct = textViews.get(handle);
        if (ct != null) {
            String value = clampText(newText);
            ct.setText(value);
            textValues.put(handle, value);
        }
    }

    public static void moveText(int handle, float x, float y) {
        ChangeableText ct = textViews.get(handle);
        if (ct != null) {
            ct.setPosition(x, y);
        }
    }

    public static void setTextColor(int handle, float r, float g, float b, float a) {
        ChangeableText ct = textViews.get(handle);
        if (ct != null) {
            ct.setColor(r, g, b, a);
        }
    }

    public static void setTextSize(int handle, float sizeSp) {
        ChangeableText old = textViews.get(handle);
        String value = textValues.get(handle);
        if (old == null || value == null || pluginLayer == null) {
            return;
        }

        Font font = fontForSize(sizeSp);
        if (font == null || textFonts.get(handle) == font) {
            return;
        }

        ChangeableText replacement = new ChangeableText(
            old.getX(), old.getY(), font, value, MAX_TEXT_LENGTH
        );
        replacement.setColor(old.getRed(), old.getGreen(), old.getBlue(), old.getAlpha());
        replacement.setVisible(old.isVisible());
        replacement.setRotation(old.getRotation());
        replacement.setScale(old.getScaleX(), old.getScaleY());

        if (old.hasParent()) {
            old.detachSelf();
        }
        pluginLayer.attachChild(replacement);
        textViews.put(handle, replacement);
        textFonts.put(handle, font);
    }

    public static void setTextOutline(int handle, float r, float g, float b, float a,
                                       float thicknessPx) {
        // Bundled fonts are not stroke fonts; accepted for API compatibility.
    }

    public static void setTextAlignment(int handle, String alignment) {
        // ChangeableText has no alignment; the x coordinate is the anchor.
    }

    public static void setTextAlpha(int handle, float alpha) {
        ChangeableText ct = textViews.get(handle);
        if (ct != null) {
            ct.setAlpha(alpha);
        }
    }

    public static void setTextVisible(int handle, boolean visible) {
        ChangeableText ct = textViews.get(handle);
        if (ct != null) {
            ct.setVisible(visible);
        }
    }

    public static void destroyText(int handle) {
        ChangeableText ct = textViews.get(handle);
        if (ct != null) {
            if (ct.hasParent()) {
                ct.detachSelf();
            }
            textViews.remove(handle);
            textValues.remove(handle);
            textFonts.remove(handle);
        }
    }

    public static void destroyAll() {
        for (int i = textViews.size() - 1; i >= 0; i--) {
            ChangeableText text = textViews.valueAt(i);
            if (text.hasParent()) {
                text.detachSelf();
            }
        }
        textViews.clear();
        textValues.clear();
        textFonts.clear();
    }

    public static void destroy() {
        destroyAll();
        pluginLayer = null;
        defaultFont = null;
    }

    private static String clampText(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) : text;
    }
}
