package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.utils;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.CursorMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.AngleOffsetMover;
import ru.nsu.ccfit.zuev.osu.game.GameObject;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Manager for multiple cursor movers with coordination and synchronization
 * Supports Tag Mod and multi-cursor autoplay scenarios
 */
public class MultiCursorManager {
    
    private List<CursorMover> movers;
    private Map<Integer, PointF> lastPositions;
    private List<String> movementStyles;
    private boolean tagModEnabled;
    private int cursorCount;
    
    public MultiCursorManager() {
        this.movers = new ArrayList<>();
        this.lastPositions = new HashMap<>();
        this.movementStyles = new ArrayList<>();
        this.tagModEnabled = false;
        this.cursorCount = 1;
        
        // Default movement styles
        movementStyles.add("flower");
        movementStyles.add("linear");
        movementStyles.add("bezier");
        movementStyles.add("aggressive");
    }
    
    /**
     * Initialize cursors with specified movement styles
     */
    public void initializeCursors(int cursorCount, List<String> styles) {
        this.cursorCount = cursorCount;
        this.movementStyles = styles != null ? styles : getDefaultStyles();
        
        movers.clear();
        lastPositions.clear();
        
        for (int i = 0; i < cursorCount; i++) {
            String style = getMovementStyle(i);
            CursorMover mover = createMover(style, i);
            movers.add(mover);
            lastPositions.put(i, new PointF(0, 0));
        }
    }
    
    /**
     * Get movement style for cursor index
     */
    private String getMovementStyle(int index) {
        if (movementStyles.isEmpty()) {
            return "flower";
        }
        return movementStyles.get(index % movementStyles.size());
    }
    
    /**
     * Create mover based on style name
     */
    private CursorMover createMover(String style, int id) {
        switch (style.toLowerCase()) {
            case "flower":
            case "angleoffset":
                return MoverUtils.createFlowerMover(id);
            case "linear":
                return new LinearMover(id);
            case "bezier":
                return new BezierMover(id);
            case "aggressive":
                return new AggressiveMover(id);
            case "pippi":
                return new PippiMover(id);
            case "exgon":
                return new ExGonMover(id);
            case "momentum":
                return new MomentumMover(id);
            case "spline":
                return new SplineMover(id);
            case "axis":
                return new AxisMover(id);
            default:
                return MoverUtils.createFlowerMover(id);
        }
    }
    
    /**
     * Get default movement styles
     */
    private List<String> getDefaultStyles() {
        List<String> styles = new ArrayList<>();
        styles.add("flower");
        styles.add("linear");
        styles.add("bezier");
        styles.add("aggressive");
        return styles;
    }
    
    /**
     * Set movement for all cursors
     */
    public void setMovementForAll(PointF startPos, PointF endPos, float startTime, float endTime) {
        for (int i = 0; i < movers.size(); i++) {
            CursorMover mover = movers.get(i);
            if (mover != null) {
                // Add slight variation for each cursor
                PointF variedStart = addVariation(startPos, i);
                PointF variedEnd = addVariation(endPos, i);
                mover.setMovement(variedStart, variedEnd, startTime, endTime);
            }
        }
    }
    
    /**
     * Add slight variation to position for visual diversity
     */
    private PointF addVariation(PointF pos, int cursorIndex) {
        float variation = 5.0f; // 5 pixels variation
        float angle = (cursorIndex * 2 * (float) Math.PI) / cursorCount;
        
        return new PointF(
            pos.x + variation * (float) Math.cos(angle),
            pos.y + variation * (float) Math.sin(angle)
        );
    }
    
    /**
     * Get position for specific cursor at time
     */
    public PointF getPositionAt(int cursorIndex, float time) {
        if (cursorIndex < 0 || cursorIndex >= movers.size()) {
            return new PointF(0, 0);
        }
        
        CursorMover mover = movers.get(cursorIndex);
        if (mover != null) {
            PointF pos = mover.getPositionAt(time);
            if (pos != null) {
                lastPositions.put(cursorIndex, pos);
                return pos;
            }
        }
        
        return lastPositions.getOrDefault(cursorIndex, new PointF(0, 0));
    }
    
    /**
     * Get all cursor positions at time
     */
    public List<PointF> getAllPositionsAt(float time) {
        List<PointF> positions = new ArrayList<>();
        for (int i = 0; i < movers.size(); i++) {
            positions.add(getPositionAt(i, time));
        }
        return positions;
    }
    
    /**
     * Check if all cursors are finished
     */
    public boolean areAllFinished(float time) {
        for (CursorMover mover : movers) {
            if (mover != null && !mover.isFinished(time)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Reset all movers
     */
    public void resetAll() {
        for (CursorMover mover : movers) {
            if (mover != null) {
                mover.reset();
            }
        }
        lastPositions.clear();
    }
    
    /**
     * Set Tag Mod enabled/disabled
     */
    public void setTagModEnabled(boolean enabled) {
        this.tagModEnabled = enabled;
        if (enabled) {
            // Reinitialize with Tag Mod settings
            initializeCursors(cursorCount, movementStyles);
        }
    }
    
    /**
     * Assign objects to cursors for Tag Mod
     */
    public void assignObjectsToCursors(GameObject[] objects) {
        if (!tagModEnabled || objects == null) {
            return;
        }
        
        // Simple round-robin assignment
        for (int i = 0; i < objects.length; i++) {
            int cursorIndex = i % cursorCount;
            assignObjectToCursor(cursorIndex, objects[i]);
        }
    }
    
    /**
     * Assign specific object to cursor
     */
    private void assignObjectToCursor(int cursorIndex, GameObject object) {
        if (cursorIndex >= movers.size()) {
            return;
        }
        
        CursorMover mover = movers.get(cursorIndex);
        // AngleOffsetMover now uses standard setMovement like other movers
    }
    
    /**
     * Update cursor count
     */
    public void updateCursorCount(int newCount) {
        if (newCount != cursorCount) {
            this.cursorCount = newCount;
            initializeCursors(newCount, movementStyles);
        }
    }
    
    /**
     * Update movement styles
     */
    public void updateMovementStyles(List<String> newStyles) {
        if (newStyles != null && !newStyles.equals(movementStyles)) {
            this.movementStyles = newStyles;
            initializeCursors(cursorCount, movementStyles);
        }
    }
    
    /**
     * Get cursor count
     */
    public int getCursorCount() {
        return cursorCount;
    }
    
    /**
     * Get movement styles
     */
    public List<String> getMovementStyles() {
        return new ArrayList<>(movementStyles);
    }
    
    /**
     * Check if Tag Mod is enabled
     */
    public boolean isTagModEnabled() {
        return tagModEnabled;
    }
    
    /**
     * Get mover by index
     */
    public CursorMover getMover(int index) {
        if (index >= 0 && index < movers.size()) {
            return movers.get(index);
        }
        return null;
    }
    
    /**
     * Calculate average distance between cursors
     */
    public float calculateAverageCursorDistance() {
        if (movers.size() < 2) return 0;
        
        float totalDistance = 0;
        int count = 0;
        
        for (int i = 0; i < movers.size(); i++) {
            for (int j = i + 1; j < movers.size(); j++) {
                PointF pos1 = lastPositions.get(i);
                PointF pos2 = lastPositions.get(j);
                if (pos1 != null && pos2 != null) {
                    totalDistance += MoverUtils.calculateDistance(pos1, pos2);
                    count++;
                }
            }
        }
        
        return count > 0 ? totalDistance / count : 0;
    }
    
    /**
     * Check if cursors are too close (collision detection)
     */
    public boolean hasCursorCollisions(float threshold) {
        for (int i = 0; i < movers.size(); i++) {
            for (int j = i + 1; j < movers.size(); j++) {
                PointF pos1 = lastPositions.get(i);
                PointF pos2 = lastPositions.get(j);
                if (pos1 != null && pos2 != null) {
                    float distance = MoverUtils.calculateDistance(pos1, pos2);
                    if (distance < threshold) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Get performance metrics
     */
    public PerformanceMetrics getPerformanceMetrics() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.cursorCount = cursorCount;
        metrics.activeMovers = movers.size();
        metrics.tagModEnabled = tagModEnabled;
        metrics.averageCursorDistance = calculateAverageCursorDistance();
        metrics.hasCollisions = hasCursorCollisions(20.0f); // 20 pixel threshold
        
        return metrics;
    }
    
    /**
     * Performance metrics for multi-cursor system
     */
    public static class PerformanceMetrics {
        public int cursorCount;
        public int activeMovers;
        public boolean tagModEnabled;
        public float averageCursorDistance;
        public boolean hasCollisions;
        
        @Override
        public String toString() {
            return String.format("Cursors: %d, Active: %d, TagMod: %s, AvgDist: %.2f, Collisions: %s",
                cursorCount, activeMovers, tagModEnabled, averageCursorDistance, hasCollisions);
        }
    }
    
    // Placeholder mover classes (these would be implemented separately)
    private static class LinearMover implements CursorMover {
        private final int id;
        public LinearMover(int id) { this.id = id; }
        @Override public void setMovement(PointF s, PointF e, float st, float et) {}
        @Override public PointF getPositionAt(float t) { return new PointF(0, 0); }
        @Override public void reset() {}
        @Override public boolean isFinished(float t) { return true; }
        @Override public PointF getObjectsPosition(float t, PointF p) { return p; }
        @Override public boolean supportsMultiPoint() { return false; }
        @Override public void setMultiPointMovement(PointF[] p, float[] t, float st) {}
    }
    
    private static class BezierMover extends LinearMover {
        public BezierMover(int id) { super(id); }
    }
    
    private static class AggressiveMover extends LinearMover {
        public AggressiveMover(int id) { super(id); }
    }
    
    private static class PippiMover extends LinearMover {
        public PippiMover(int id) { super(id); }
    }
    
    private static class ExGonMover extends LinearMover {
        public ExGonMover(int id) { super(id); }
    }
    
    private static class MomentumMover extends LinearMover {
        public MomentumMover(int id) { super(id); }
    }
    
    private static class SplineMover extends LinearMover {
        public SplineMover(int id) { super(id); }
    }
    
    private static class AxisMover extends LinearMover {
        public AxisMover(int id) { super(id); }
    }
}
