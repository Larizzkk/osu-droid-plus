package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;

/**
 * Base interface for cursor movement algorithms.
 * Based on danser-go movers.
 */
public interface CursorMover {
    
    /**
     * Initialize the mover with start and end positions.
     * @param startPos Starting position
     * @param endPos Ending position  
     * @param startTime Time when movement should start (in milliseconds)
     * @param endTime Time when movement should end (in milliseconds)
     */
    void setMovement(PointF startPos, PointF endPos, float startTime, float endTime);
    
    /**
     * Get cursor position at specific time.
     * @param time Current time (in milliseconds)
     * @return Position at given time
     */
    PointF getPositionAt(float time);
    
    /**
     * Get cursor position for hit-through behavior.
     * @param time Current time (in milliseconds)
     * @param objectPos Current object position
     * @return Position with hit-through effect, or null if not applicable
     */
    PointF getObjectsPosition(float time, PointF objectPos);
    
    /**
     * Reset the mover state.
     */
    void reset();
    
    /**
     * Check if movement is finished.
     * @param time Current time (in milliseconds)
     * @return true if movement is complete
     */
    boolean isFinished(float time);
    
    /**
     * Check if mover supports multi-point movement for continuous autoplay.
     * @return true if multi-point movement is supported
     */
    boolean supportsMultiPoint();
    
    /**
     * Set multi-point movement for continuous autoplay.
     * @param positions Array of positions to move through
     * @param times Array of times for each position
     * @param startTime Time when movement should start (in milliseconds)
     */
    void setMultiPointMovement(PointF[] positions, float[] times, float startTime);
}
