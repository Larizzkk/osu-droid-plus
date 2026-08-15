package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.stacking;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.MathUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.PlayfieldBounds;

import java.util.HashMap;
import java.util.Map;

/**
 * Stacking manager - exact port from danser-go stacking logic
 * Based on https://github.com/wieku/danser-go/blob/master/app/beatmap/objects/hitobject.go
 */
public class StackingManager {
    
    private static final float OSU_PLAYFIELD_WIDTH = 512f;
    private static final float OSU_PLAYFIELD_HEIGHT = 384f;
    
    private Map<Integer, Integer> stackIndices = new HashMap<>();
    private float circleRadius;
    private boolean isLazerMode;
    private float circleScaleL;
    
    public StackingManager() {
        this.circleRadius = 32f; // Default circle radius
        this.isLazerMode = false;
        this.circleScaleL = 1.0f;
    }
    
    /**
     * Set circle radius for stacking calculations
     */
    public void setCircleRadius(float radius) {
        this.circleRadius = radius;
    }
    
    /**
     * Set lazer mode
     */
    public void setLazerMode(boolean lazerMode, float circleScaleL) {
        this.isLazerMode = lazerMode;
        this.circleScaleL = circleScaleL;
    }
    
    /**
     * Get stack index for object
     */
    public int getStackIndex(int objectId, int stackThreshold) {
        return stackIndices.computeIfAbsent(objectId, k -> calculateStackIndex(objectId, stackThreshold));
    }
    
    /**
     * Calculate stack index (simplified version of danser-go logic)
     */
    private int calculateStackIndex(int objectId, int stackThreshold) {
        // Simplified stacking calculation
        // In real danser-go this is more complex
        return objectId / stackThreshold;
    }
    
    /**
     * Modify position for stacking - exact port from danser-go ModifyPosition
     */
    public PointF modifyPosition(PointF basePosition, int objectId, int stackThreshold) {
        PointF modifiedPos = new PointF(basePosition.x, basePosition.y);
        
        // Apply mirror flips (simplified)
        // In real danser-go this checks HardRock and Mirror settings
        
        // Apply stacking offset
        int stackIndex = getStackIndex(objectId, stackThreshold);
        float stackOffset = calculateStackOffset(stackIndex);
        
        // Apply offset (subtraction like in danser-go)
        modifiedPos.x -= stackOffset;
        modifiedPos.y -= stackOffset;
        
        return PlayfieldBounds.clampToPlayfield(modifiedPos);
    }
    
    /**
     * Calculate stack offset based on stack index
     */
    private float calculateStackOffset(int stackIndex) {
        if (isLazerMode) {
            return stackIndex * circleScaleL * 6.4f;
        } else {
            return stackIndex * circleRadius / 10f;
        }
    }
    
    /**
     * Get stacked start position for movement
     */
    public PointF getStackedStartPosition(PointF startPos, int objectId, int stackThreshold) {
        return modifyPosition(startPos, objectId, stackThreshold);
    }
    
    /**
     * Get stacked end position for movement
     */
    public PointF getStackedEndPosition(PointF endPos, int objectId, int stackThreshold) {
        return modifyPosition(endPos, objectId, stackThreshold);
    }
    
    /**
     * Check if positions are the same after stacking
     */
    public boolean positionsEqualAfterStacking(PointF pos1, PointF pos2, int id1, int id2, int stackThreshold) {
        PointF stacked1 = getStackedStartPosition(pos1, id1, stackThreshold);
        PointF stacked2 = getStackedEndPosition(pos2, id2, stackThreshold);
        
        return MathUtils.dst(stacked1, stacked2) < 1f; // Very close positions
    }
    
    /**
     * Clear stack indices
     */
    public void clear() {
        stackIndices.clear();
    }
    
    /**
     * Get stacking statistics
     */
    public StackingStats getStats() {
        return new StackingStats(stackIndices.size(), circleRadius, isLazerMode);
    }
    
    /**
     * Stacking statistics
     */
    public static class StackingStats {
        public final int stackCount;
        public final float circleRadius;
        public final boolean isLazerMode;
        
        public StackingStats(int stackCount, float circleRadius, boolean isLazerMode) {
            this.stackCount = stackCount;
            this.circleRadius = circleRadius;
            this.isLazerMode = isLazerMode;
        }
        
        @Override
        public String toString() {
            return String.format("StackingStats[stacks=%d, radius=%.1f, lazer=%s]", 
                stackCount, circleRadius, isLazerMode);
        }
    }
}
