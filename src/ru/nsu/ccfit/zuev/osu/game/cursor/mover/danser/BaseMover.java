package ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

/**
 * Port of danser-go basicMover
 * Based on https://github.com/wieku/danser-go/blob/master/app/dance/movers/mover.go
 */
public abstract class BaseMover {
    protected float startTime;
    protected float endTime;
    protected int id;
    
    // Settings for hit-through behavior
    protected boolean waitForPreempt = true;
    protected float reactionTime = 100f; // Default reaction time in ms
    protected boolean choppyLongObjects = true; // Always enable hit-through

    public void reset(int id) {
        this.id = id;
    }

    public float getStartTime() {
        return startTime;
    }

    public float getEndTime() {
        return endTime;
    }
    
    /**
     * Get position for specific object - handles hit-through behavior
     */
    public PointF getObjectsPosition(float time, PointF objectPos) {
        // Default implementation - just return the mover position
        // Override in specific movers for hit-through behavior
        return null;
    }
    
    /**
     * Adjust start time based on preempt/reaction settings
     */
    protected void adjustStartTime(float preempt, float speed) {
        if (waitForPreempt) {
            float adjustedTime = endTime - (preempt - reactionTime * speed);
            startTime = Math.max(startTime, adjustedTime);
        }
    }
    
    /**
     * Check if mover supports multi-point movement for continuous autoplay.
     * Default implementation returns false, override in specific movers.
     */
    public boolean supportsMultiPoint() {
        return false; // Default: don't support multi-point
    }
    
    /**
     * Set basic movement between two points.
     * Abstract method to be implemented by specific movers.
     */
    public abstract void setMovement(PointF startPos, PointF endPos, float startTime, float endTime);
    
    /**
     * Set multi-point movement for continuous autoplay.
     * Default implementation falls back to two-point movement.
     */
    public void setMultiPointMovement(PointF[] positions, float[] times, float startTime) {
        // Default implementation: fall back to simple two-point movement
        if (positions != null && positions.length >= 2 && times != null && times.length >= 2) {
            // Use first and last positions/times
            setMovement(positions[0], positions[positions.length - 1], startTime, times[times.length - 1]);
        }
    }
}
