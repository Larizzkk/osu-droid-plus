package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.scheduler;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.CursorMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.PlayfieldBounds;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic scheduler - exact port from danser-go GenericScheduler
 * Based on https://github.com/wieku/danser-go/blob/master/app/dance/schedulers/generic.go
 */
public class GenericScheduler {
    
    private CursorMover mover;
    private List<MovementSegment> movementSegments = new ArrayList<>();
    private float lastTime = 0f;
    private int index;
    private int id;
    
    // Movement segment for tracking
    private static class MovementSegment {
        PointF startPos;
        PointF endPos;
        float startTime;
        float endTime;
        
        MovementSegment(PointF startPos, PointF endPos, float startTime, float endTime) {
            this.startPos = startPos;
            this.endPos = endPos;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
    
    public GenericScheduler(CursorMover mover, int index) {
        this.mover = mover;
        this.index = index;
        this.id = index;
    }
    
    /**
     * Initialize scheduler with movement segments
     */
    public void init(List<MovementSegment> segments) {
        this.movementSegments = new ArrayList<>(segments);
        this.lastTime = -1f;
        
        // Initialize mover with first segment
        if (!movementSegments.isEmpty()) {
            MovementSegment firstSegment = movementSegments.get(0);
            mover.setMovement(firstSegment.startPos, firstSegment.endPos, 
                firstSegment.startTime, firstSegment.endTime);
        }
    }
    
    /**
     * Update scheduler
     */
    public void update(float time, float deltaTime) {
        if (movementSegments.isEmpty()) {
            return;
        }
        
        // Check if we need to move to next segment
        if (mover.isFinished(time) && movementSegments.size() > 1) {
            int currentSegmentIndex = getCurrentSegmentIndex(time);
            if (currentSegmentIndex < movementSegments.size() - 1) {
                MovementSegment nextSegment = movementSegments.get(currentSegmentIndex + 1);
                mover.setMovement(nextSegment.startPos, nextSegment.endPos, 
                    nextSegment.startTime, nextSegment.endTime);
            }
        }
        
        lastTime = time;
    }
    
    /**
     * Get position at specific time
     */
    public PointF getPositionAt(float time) {
        if (movementSegments.isEmpty()) {
            return PlayfieldBounds.getPlayfieldCenter();
        }
        
        // Find current segment
        MovementSegment currentSegment = getCurrentSegment(time);
        if (currentSegment == null) {
            return movementSegments.get(0).startPos;
        }
        
        return mover.getPositionAt(time);
    }
    
    /**
     * Get current movement segment
     */
    private MovementSegment getCurrentSegment(float time) {
        for (MovementSegment segment : movementSegments) {
            if (time >= segment.startTime && time <= segment.endTime) {
                return segment;
            }
        }
        
        // If no segment found, return the last one
        return movementSegments.get(movementSegments.size() - 1);
    }
    
    /**
     * Get current segment index
     */
    private int getCurrentSegmentIndex(float time) {
        for (int i = 0; i < movementSegments.size(); i++) {
            MovementSegment segment = movementSegments.get(i);
            if (time >= segment.startTime && time <= segment.endTime) {
                return i;
            }
        }
        
        return movementSegments.size() - 1;
    }
    
    /**
     * Add movement segment
     */
    public void addMovementSegment(PointF startPos, PointF endPos, float startTime, float endTime) {
        MovementSegment segment = new MovementSegment(
            PlayfieldBounds.clampToPlayfield(startPos),
            PlayfieldBounds.clampToPlayfield(endPos),
            startTime,
            endTime
        );
        
        movementSegments.add(segment);
    }
    
    /**
     * Clear all movement segments
     */
    public void clearSegments() {
        movementSegments.clear();
    }
    
    /**
     * Set mover
     */
    public void setMover(CursorMover mover) {
        this.mover = mover;
        this.mover.reset();
        
        // Reinitialize with current segment if available
        if (!movementSegments.isEmpty()) {
            MovementSegment currentSegment = getCurrentSegment(lastTime);
            if (currentSegment != null) {
                this.mover.setMovement(currentSegment.startPos, currentSegment.endPos, 
                    currentSegment.startTime, currentSegment.endTime);
            }
        }
    }
    
    /**
     * Reset scheduler
     */
    public void reset() {
        movementSegments.clear();
        lastTime = -1f;
        if (mover != null) {
            mover.reset();
        }
    }
    
    /**
     * Get mover
     */
    public CursorMover getMover() {
        return mover;
    }
    
    /**
     * Get movement segments count
     */
    public int getSegmentCount() {
        return movementSegments.size();
    }
}
