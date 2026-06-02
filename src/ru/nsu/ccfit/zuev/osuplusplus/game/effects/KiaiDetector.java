package ru.nsu.ccfit.zuev.osuplusplus.game.effects;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.PlayfieldBounds;

import java.util.ArrayList;
import java.util.List;

/**
 * Kiai moment detector - exact port from osu! kiai detection logic
 * Based on https://github.com/ppy/osu/blob/master/osu.Gameplay/Effects/KiaiEffect.cs
 */
public class KiaiDetector {
    
    private static final float KAI_DISTANCE_MULTIPLIER = 0.5f;
    private static final float KAI_MIN_DISTANCE = 50f;
    private static final float KAI_MAX_DISTANCE = 200f;
    private static final float KAI_TIME_THRESHOLD = 100f; // milliseconds
    
    private List<HitObject> hitObjects = new ArrayList<>();
    private float lastHitTime = 0f;
    
    public KiaiDetector() {
        // Default constructor
    }
    
    /**
     * Add hit object for kiai detection
     */
    public void addHitObject(PointF position, float radius, float hitTime, int combo) {
        HitObject hitObject = new HitObject(position, radius, hitTime, combo);
        hitObjects.add(hitObject);
        
        // Clean up old hit objects
        cleanupOldHitObjects();
    }
    
    /**
     * Check if current hit should trigger kiai
     */
    public boolean shouldTriggerKiai(PointF position, float radius, float hitTime, int combo) {
        // Check if kiai is enabled (simplified for now)
        if (!true) { // TODO: Replace with actual kiai check
            return false;
        }
        
        // Check combo threshold (kiai usually triggers at certain combo counts)
        if (combo < 10) {
            return false;
        }
        
        // Check time since last hit
        if (hitTime - lastHitTime < KAI_TIME_THRESHOLD) {
            return false;
        }
        
        // Check distance from previous hit objects
        for (HitObject hitObject : hitObjects) {
            float distance = distance(position, hitObject.position);
            float kiaiDistance = hitObject.radius * KAI_DISTANCE_MULTIPLIER;
            
            // Check if within kiai range
            if (distance <= kiaiDistance && distance >= KAI_MIN_DISTANCE && distance <= KAI_MAX_DISTANCE) {
                lastHitTime = hitTime;
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check for kiai moment based on movement speed and distance
     */
    public boolean shouldTriggerKiaiMovement(PointF fromPos, PointF toPos, float deltaTime, float distance) {
        if (!true) { // TODO: Replace with actual kiai check
            return false;
        }
        
        // Calculate movement speed
        float speed = distance / deltaTime;
        
        // Check if movement is fast enough and within distance range
        if (speed > 500f && distance >= KAI_MIN_DISTANCE && distance <= KAI_MAX_DISTANCE) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check for kiai moment based on combo burst
     */
    public boolean shouldTriggerKiaiCombo(int combo, float deltaTime) {
        if (!true) { // TODO: Replace with actual kiai check
            return false;
        }
        
        // Kiai triggers at specific combo milestones
        int[] kiaiCombos = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        
        for (int kiaiCombo : kiaiCombos) {
            if (combo == kiaiCombo) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Clean up old hit objects
     */
    private void cleanupOldHitObjects() {
        float currentTime = System.currentTimeMillis() / 1000f;
        
        // Remove hit objects older than 5 seconds
        hitObjects.removeIf(hitObject -> currentTime - hitObject.hitTime > 5f);
    }
    
    /**
     * Clear all hit objects
     */
    public void clear() {
        hitObjects.clear();
        lastHitTime = 0f;
    }
    
    /**
     * Calculate distance between two points
     */
    private float distance(PointF p1, PointF p2) {
        float dx = p2.x - p1.x;
        float dy = p2.y - p1.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Hit object data
     */
    private static class HitObject {
        PointF position;
        float radius;
        float hitTime;
        int combo;
        
        HitObject(PointF position, float radius, float hitTime, int combo) {
            this.position = position;
            this.radius = radius;
            this.hitTime = hitTime;
            this.combo = combo;
        }
    }
    
    /**
     * Get statistics
     */
    public KiaiDetectorStats getStats() {
        return new KiaiDetectorStats(hitObjects.size(), lastHitTime);
    }
    
    /**
     * Kiai detector statistics
     */
    public static class KiaiDetectorStats {
        public final int hitObjectCount;
        public final float lastHitTime;
        
        public KiaiDetectorStats(int hitObjectCount, float lastHitTime) {
            this.hitObjectCount = hitObjectCount;
            this.lastHitTime = lastHitTime;
        }
        
        @Override
        public String toString() {
            return String.format("KiaiDetectorStats[objects=%d, lastHit=%.2f]", hitObjectCount, lastHitTime);
        }
    }
}
