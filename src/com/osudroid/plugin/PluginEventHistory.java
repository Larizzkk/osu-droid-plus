package com.osudroid.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores recent gameplay events for plugin analysis.
 * Plugins can access recent hit events for UR calculation, analytics, etc.
 *
 * Events are stored in a circular buffer (last 500 events).
 */
public class PluginEventHistory {

    private static final int MAX_EVENTS = 500;

    public static class HitEvent {
        public final long timeMs;      // gameTime when hit occurred
        public final int objectId;
        public final float accuracy;   // timing accuracy (0=miss, 1=perfect)
        public final float x, y;       // position
        public final int scoreValue;   // 0, 50, 100, 300
        public final boolean endCombo;

        public HitEvent(long timeMs, int objectId, float accuracy, float x, float y,
                        int scoreValue, boolean endCombo) {
            this.timeMs = timeMs;
            this.objectId = objectId;
            this.accuracy = accuracy;
            this.x = x;
            this.y = y;
            this.scoreValue = scoreValue;
            this.endCombo = endCombo;
        }
    }

    public static class CursorEvent {
        public final long timeMs;
        public final float x, y;

        public CursorEvent(long timeMs, float x, float y) {
            this.timeMs = timeMs;
            this.x = x;
            this.y = y;
        }
    }

    private static final List<HitEvent> hitEvents = new ArrayList<>();
    private static final List<CursorEvent> cursorEvents = new ArrayList<>();

    public static void addHitEvent(long timeMs, int objectId, float accuracy,
                                    float x, float y, int scoreValue, boolean endCombo) {
        hitEvents.add(new HitEvent(timeMs, objectId, accuracy, x, y, scoreValue, endCombo));
        if (hitEvents.size() > MAX_EVENTS) {
            hitEvents.remove(0);
        }
    }

    public static void addCursorEvent(long timeMs, float x, float y) {
        // Only store every 5th cursor event to save memory (cursor updates ~120/sec)
        if (cursorEvents.size() % 5 == 0) {
            cursorEvents.add(new CursorEvent(timeMs, x, y));
            if (cursorEvents.size() > MAX_EVENTS / 5) {
                cursorEvents.remove(0);
            }
        }
    }

    /**
     * Get number of recent hit events.
     */
    public static int getHitEventCount() {
        return hitEvents.size();
    }

    /**
     * Get a hit event by index (0=oldest, count-1=newest).
     */
    public static HitEvent getHitEvent(int index) {
        if (index < 0 || index >= hitEvents.size()) return null;
        return hitEvents.get(index);
    }

    /**
     * Get all hit events as a Lua-compatible array.
     * Returns: {time, objectId, accuracy, x, y, score, endCombo} for each event
     */
    public static List<HitEvent> getRecentHits(int count) {
        int start = Math.max(0, hitEvents.size() - count);
        return new ArrayList<>(hitEvents.subList(start, hitEvents.size()));
    }

    /**
     * Get timing errors (accuracy values) for UR calculation.
     * Returns array of timing errors in milliseconds.
     */
    public static float[] getTimingErrors() {
        float[] errors = new float[hitEvents.size()];
        for (int i = 0; i < hitEvents.size(); i++) {
            // accuracy 1.0 = 0ms error, accuracy 0.0 = max error
            errors[i] = (1.0f - hitEvents.get(i).accuracy) * 100f;
        }
        return errors;
    }

    /**
     * Clear all events (called when beatmap ends).
     */
    public static void clear() {
        hitEvents.clear();
        cursorEvents.clear();
    }
}
