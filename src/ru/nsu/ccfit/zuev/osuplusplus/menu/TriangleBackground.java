package ru.nsu.ccfit.zuev.osuplusplus.menu;

import org.anddev.andengine.entity.Entity;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ResourceManager;

/**
 * Switches between the original osu!droid+ triangle particles and the
 * danser-go visualiser. Both implementations keep their own settings; the
 * style preference only decides which one is attached.
 */
public class TriangleBackground extends Entity {

    public static final String STYLE_LEGACY = "legacy";
    public static final String STYLE_DANSER = "danser";

    private Entity visual;
    private LegacyTriangleBackground legacy;
    private DanserTriangleBackground danser;
    private String style;
    private String backgroundPath;
    private float styleAge;

    public TriangleBackground() {
        this.backgroundPath = ResourceManager.getInstance().getLoadedBackgroundPath();
        rebuild();
    }

    private String currentStyle() {
        String selected = Config.getString("triangleStyle", STYLE_DANSER);
        return STYLE_LEGACY.equals(selected) ? STYLE_LEGACY : STYLE_DANSER;
    }

    private void rebuild() {
        String next = currentStyle();
        if (visual != null) {
            if (legacy != null) legacy.clearAll();
            detachChild(visual);
            visual = null;
            legacy = null;
            danser = null;
        }

        style = next;
        if (STYLE_LEGACY.equals(style)) {
            legacy = new LegacyTriangleBackground();
            visual = legacy;
        } else {
            danser = new DanserTriangleBackground(backgroundPath);
            visual = danser;
        }
        attachChild(visual);
    }

    @Override
    protected void onManagedUpdate(float dt) {
        super.onManagedUpdate(dt);
        styleAge += dt;
        if (styleAge >= 0.5f) {
            styleAge = 0f;
            if (!style.equals(currentStyle())) rebuild();
        }
    }

    public void setKiai(boolean active) {
        if (legacy != null) legacy.setKiai(active);
    }

    public void onBeat() {
        if (legacy != null) legacy.onBeat();
    }

    public void setParallax(float x, float y) {
        if (danser != null) {
            danser.setParallax(x, y);
        } else if (visual != null) {
            visual.setPosition(x * 0.5f, y * 0.5f);
        }
    }

    /** Called whenever the menu/gameplay background changes. */
    public void setBackgroundPath(String path) {
        backgroundPath = path;
        if (danser != null) {
            danser.setBackgroundPath(path);
        }
    }

    public void clearAll() {
        if (legacy != null) legacy.clearAll();
        if (danser != null) danser.clearAll();
    }
}
