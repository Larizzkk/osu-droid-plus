package ru.nsu.ccfit.zuev.osuplusplus.menu;

import org.anddev.andengine.entity.Entity;

/**
 * Keeps the triangle effect behind the playfield and the menu. The original
 * particle code moved to {@link LegacyTriangleBackground}; picking another
 * style arrives with the danser-go visualiser.
 */
public class TriangleBackground extends Entity {

    private final LegacyTriangleBackground legacy;

    public TriangleBackground() {
        legacy = new LegacyTriangleBackground();
        attachChild(legacy);
    }

    public void setKiai(boolean active) {
        legacy.setKiai(active);
    }

    public void onBeat() {
        legacy.onBeat();
    }

    public void clearAll() {
        legacy.clearAll();
    }
}
