package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.state;

import android.graphics.PointF;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * CursorStateTracker - Manages cursor movement history and state
 * Essential for smooth movement transitions and break handling
 */
public class CursorStateTracker {
    
    // State history buffer
    private static final int HISTORY_SIZE = 5;
    private final LinkedList<CursorState> stateHistory;
    
    // Movement prediction buffer
    private static final int PREDICTION_SIZE = 3;
    private final Queue<PointF> predictedPositions;
    
    // Current state
    private CursorState currentState;
    private boolean isPaused;
    private boolean isBreak;
    private long lastUpdateTime;
    private long pauseStartTime;
    
    // Movement smoothing
    private final PointF smoothedPosition;
    private final PointF velocityVector;
    private float smoothingFactor;
    
    // Break handling
    private final PointF preBreakPosition;
    private final PointF postBreakPosition;
    private boolean needsBreakTransition;
    
    public CursorStateTracker() {
        this.stateHistory = new LinkedList<>();
        this.predictedPositions = new LinkedList<>();
        this.currentState = new CursorState();
        this.smoothedPosition = new PointF();
        this.velocityVector = new PointF();
        this.preBreakPosition = new PointF();
        this.postBreakPosition = new PointF();
        this.smoothingFactor = 0.8f;
        this.isPaused = false;
        this.isBreak = false;
        this.needsBreakTransition = false;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * Cursor state snapshot
     */
    public static class CursorState {
        public final PointF position;
        public final PointF velocity;
        public final long timestamp;
        public final float acceleration;
        public final boolean isMoving;
        public final float confidence;
        
        public CursorState() {
            this(new PointF(0, 0), new PointF(0, 0), System.currentTimeMillis(), 0f, false, 1f);
        }
        
        public CursorState(PointF position, PointF velocity, long timestamp, 
                         float acceleration, boolean isMoving, float confidence) {
            this.position = new PointF(position);
            this.velocity = new PointF(velocity);
            this.timestamp = timestamp;
            this.acceleration = acceleration;
            this.isMoving = isMoving;
            this.confidence = confidence;
        }
        
        public CursorState(CursorState other) {
            this.position = new PointF(other.position);
            this.velocity = new PointF(other.velocity);
            this.timestamp = other.timestamp;
            this.acceleration = other.acceleration;
            this.isMoving = other.isMoving;
            this.confidence = other.confidence;
        }
    }
    
    /**
     * Update cursor state with new position
     */
    public void update(PointF newPosition, long currentTime) {
        if (isPaused) {
            // Don't update during pause, but store position for resume
            postBreakPosition.set(newPosition);
            return;
        }
        
        long deltaTime = currentTime - lastUpdateTime;
        if (deltaTime <= 0) {
            deltaTime = 16; // Assume 60 FPS minimum
        }
        
        // Calculate velocity
        PointF newVelocity = new PointF();
        if (!stateHistory.isEmpty()) {
            CursorState lastState = stateHistory.getLast();
            float dt = deltaTime / 1000f; // Convert to seconds
            
            newVelocity.x = (newPosition.x - lastState.position.x) / dt;
            newVelocity.y = (newPosition.y - lastState.position.y) / dt;
        }
        
        // Calculate acceleration
        float acceleration = 0f;
        if (stateHistory.size() >= 2) {
            CursorState[] lastStates = getLastNStates(2);
            float dt = deltaTime / 1000f;
            float dvx = (newVelocity.x - lastStates[1].velocity.x) / dt;
            float dvy = (newVelocity.y - lastStates[1].velocity.y) / dt;
            acceleration = (float) Math.sqrt(dvx * dvx + dvy * dvy);
        }
        
        // Determine if cursor is moving
        boolean isMoving = (Math.abs(newVelocity.x) > 1f || Math.abs(newVelocity.y) > 1f);
        
        // Calculate confidence based on movement consistency
        float confidence = calculateMovementConfidence(newVelocity, acceleration);
        
        // Create new state
        CursorState newState = new CursorState(newPosition, newVelocity, currentTime, 
                                            acceleration, isMoving, confidence);
        
        // Update current state
        currentState = newState;
        
        // Add to history
        stateHistory.addLast(newState);
        while (stateHistory.size() > HISTORY_SIZE) {
            stateHistory.removeFirst();
        }
        
        // Update smoothed position
        updateSmoothedPosition(newPosition);
        
        // Update velocity vector for prediction
        velocityVector.set(newVelocity);
        
        lastUpdateTime = currentTime;
    }
    
    /**
     * Update smoothed position using weighted average
     */
    private void updateSmoothedPosition(PointF newPosition) {
        smoothedPosition.x = smoothedPosition.x * smoothingFactor + newPosition.x * (1f - smoothingFactor);
        smoothedPosition.y = smoothedPosition.y * smoothingFactor + newPosition.y * (1f - smoothingFactor);
    }
    
    /**
     * Calculate movement confidence based on consistency
     */
    private float calculateMovementConfidence(PointF velocity, float acceleration) {
        if (stateHistory.isEmpty()) {
            return 1f;
        }
        
        float confidence = 1f;
        
        // Check velocity consistency
        if (stateHistory.size() >= 2) {
            CursorState[] lastStates = getLastNStates(2);
            float velocityChange = Math.abs(velocity.x - lastStates[1].velocity.x) + 
                                 Math.abs(velocity.y - lastStates[1].velocity.y);
            
            // Lower confidence for sudden velocity changes
            if (velocityChange > 500f) {
                confidence *= 0.8f;
            }
        }
        
        // Check acceleration reasonableness
        if (acceleration > 10000f) {
            confidence *= 0.7f; // Very high acceleration is suspicious
        }
        
        return Math.max(0.1f, confidence);
    }
    
    /**
     * Get last N states from history
     */
    private CursorState[] getLastNStates(int n) {
        CursorState[] result = new CursorState[Math.min(n, stateHistory.size())];
        int index = 0;
        
        for (CursorState state : stateHistory) {
            if (index >= result.length) break;
            result[result.length - 1 - index] = state;
            index++;
        }
        
        return result;
    }
    
    /**
     * Predict next position based on current velocity and acceleration
     */
    public PointF predictNextPosition(float timeAheadMs) {
        if (stateHistory.isEmpty()) {
            return new PointF(currentState.position);
        }
        
        float timeAhead = timeAheadMs / 1000f; // Convert to seconds
        
        PointF predicted = new PointF();
        
        // Use current velocity and acceleration for prediction
        float ax = 0f, ay = 0f;
        
        if (stateHistory.size() >= 2) {
            CursorState[] lastStates = getLastNStates(2);
            float dt = (currentState.timestamp - lastStates[1].timestamp) / 1000f;
            if (dt > 0) {
                ax = (currentState.velocity.x - lastStates[1].velocity.x) / dt;
                ay = (currentState.velocity.y - lastStates[1].velocity.y) / dt;
            }
        }
        
        // kinematic equation: s = ut + 0.5at^2
        predicted.x = currentState.position.x + currentState.velocity.x * timeAhead + 0.5f * ax * timeAhead * timeAhead;
        predicted.y = currentState.position.y + currentState.velocity.y * timeAhead + 0.5f * ay * timeAhead * timeAhead;
        
        return predicted;
    }
    
    /**
     * Handle game pause
     */
    public void onPause() {
        if (!isPaused) {
            isPaused = true;
            pauseStartTime = System.currentTimeMillis();
            preBreakPosition.set(currentState.position);
            
            // Clear prediction buffer during pause
            predictedPositions.clear();
        }
    }
    
    /**
     * Handle game resume
     */
    public void onResume() {
        if (isPaused) {
            isPaused = false;
            long pauseDuration = System.currentTimeMillis() - pauseStartTime;
            
            // If pause was long enough, we need smooth transition
            if (pauseDuration > 1000) {
                needsBreakTransition = true;
            }
            
            // Reset velocity to avoid jumps
            velocityVector.set(0, 0);
        }
    }
    
    /**
     * Handle break period
     */
    public void onBreakStart() {
        isBreak = true;
        preBreakPosition.set(currentState.position);
        needsBreakTransition = false;
    }
    
    /**
     * Handle break end
     */
    public void onBreakEnd() {
        isBreak = false;
        if (isPaused) {
            needsBreakTransition = true;
        }
    }
    
    /**
     * Get smooth transition position for break/pause handling
     */
    public PointF getTransitionPosition(float transitionProgress) {
        if (!needsBreakTransition) {
            return new PointF(currentState.position);
        }
        
        PointF transition = new PointF();
        transition.x = preBreakPosition.x + (postBreakPosition.x - preBreakPosition.x) * transitionProgress;
        transition.y = preBreakPosition.y + (postBreakPosition.y - preBreakPosition.y) * transitionProgress;
        
        if (transitionProgress >= 1f) {
            needsBreakTransition = false;
        }
        
        return transition;
    }
    
    /**
     * Reset state tracker
     */
    public void reset() {
        stateHistory.clear();
        predictedPositions.clear();
        currentState = new CursorState();
        smoothedPosition.set(0, 0);
        velocityVector.set(0, 0);
        isPaused = false;
        isBreak = false;
        needsBreakTransition = false;
        lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * Get smoothed position (for rendering)
     */
    public PointF getSmoothedPosition() {
        return smoothedPosition;
    }
    
    /**
     * Get current velocity
     */
    public PointF getCurrentVelocity() {
        return velocityVector;
    }
    
    /**
     * Get current state
     */
    public CursorState getCurrentState() {
        return currentState;
    }
    
    /**
     * Check if cursor is currently moving
     */
    public boolean isMoving() {
        return currentState.isMoving && !isPaused;
    }
    
    /**
     * Check if cursor is in break
     */
    public boolean isInBreak() {
        return isBreak;
    }
    
    /**
     * Check if cursor is paused
     */
    public boolean isPaused() {
        return isPaused;
    }
    
    /**
     * Set smoothing factor
     */
    public void setSmoothingFactor(float factor) {
        this.smoothingFactor = Math.max(0f, Math.min(1f, factor));
    }
    
    /**
     * Get movement confidence
     */
    public float getMovementConfidence() {
        return currentState.confidence;
    }
    
    /**
     * Get average acceleration over history
     */
    public float getAverageAcceleration() {
        if (stateHistory.isEmpty()) {
            return 0f;
        }
        
        float totalAcceleration = 0f;
        int count = 0;
        
        for (CursorState state : stateHistory) {
            totalAcceleration += state.acceleration;
            count++;
        }
        
        return count > 0 ? totalAcceleration / count : 0f;
    }
    
    /**
     * Check if movement pattern is consistent
     */
    public boolean isMovementConsistent() {
        if (stateHistory.size() < 3) {
            return true;
        }
        
        float avgAcceleration = getAverageAcceleration();
        float variance = 0f;
        int count = 0;
        
        for (CursorState state : stateHistory) {
            float diff = state.acceleration - avgAcceleration;
            variance += diff * diff;
            count++;
        }
        
        variance /= count;
        float stdDeviation = (float) Math.sqrt(variance);
        
        // Movement is consistent if standard deviation is low
        return stdDeviation < avgAcceleration * 0.5f;
    }
    
    /**
     * Get debug information
     */
    public String getDebugInfo() {
        return String.format("Pos: (%.1f, %.1f), Vel: (%.1f, %.1f), Acc: %.1f, Conf: %.2f, Moving: %s, Break: %s, Paused: %s",
                currentState.position.x, currentState.position.y,
                velocityVector.x, velocityVector.y,
                currentState.acceleration, currentState.confidence,
                currentState.isMoving, isBreak, isPaused);
    }
}
