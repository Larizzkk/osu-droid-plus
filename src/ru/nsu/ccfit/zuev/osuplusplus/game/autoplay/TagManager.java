package ru.nsu.ccfit.zuev.osuplusplus.game.autoplay;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages Tag mod - multi-cursor autoplay where each cursor hits its own objects sequentially
 * Based on danser-go implementation
 */
public class TagManager {
    
    public static class TagCursor {
        public int id;
        public AutoplayMovementManager movementManager;
        public PointF position;
        public PointF lastPosition;
        public List<GameObject> assignedObjects;
        public int currentObjectIndex;
        public boolean isActive;
        public String moverType;
        
        public TagCursor(int id, String moverType) {
            this.id = id;
            this.moverType = moverType;
            this.movementManager = new AutoplayMovementManager();
            this.assignedObjects = new ArrayList<>();
            this.currentObjectIndex = 0;
            this.isActive = true;
            
            // Initialize position at screen center
            float centerX = Config.getRES_WIDTH() / 2f;
            float centerY = Config.getRES_HEIGHT() / 2f;
            this.position = new PointF(centerX, centerY);
            this.lastPosition = new PointF(centerX, centerY);
            
            // Set movement type for this cursor
            this.movementManager.loadSettings(); // Load default settings
            // Override movement type if needed
            this.movementManager.setMovementType(moverType);
        }
        
        public void assignObject(GameObject object) {
            assignedObjects.add(object);
        }
        
        public GameObject getCurrentObject() {
            if (currentObjectIndex < assignedObjects.size()) {
                return assignedObjects.get(currentObjectIndex);
            }
            return null;
        }
        
        public void moveToNextObject() {
            currentObjectIndex++;
        }
        
        public boolean hasMoreObjects() {
            return currentObjectIndex < assignedObjects.size();
        }
    }
    
    private List<TagCursor> cursors;
    private int cursorCount;
    private boolean isInitialized;
    
    public TagManager() {
        this.cursorCount = 4; // Default 4 cursors like in danser-go
        this.cursors = new ArrayList<>();
        this.isInitialized = false;
    }
    
    public void initialize(int cursorCount) {
        this.cursorCount = cursorCount;
        this.cursors.clear();
        
        // Get available mover types from settings
        String[] moverTypes = getMoverTypes();
        
        // Create cursors with different movement styles
        for (int i = 0; i < cursorCount; i++) {
            String moverType = moverTypes[i % moverTypes.length];
            TagCursor cursor = new TagCursor(i, moverType);
            cursors.add(cursor);
        }
        
        this.isInitialized = true;
    }

    private String[] getMoverTypes() {
        // Get mover types from settings, similar to danser-go
        String[] defaultMovers = {"linear", "bezier", "aggressive", "pippi", "circular", "axis", "exgon", "momentum", "spline", "flower"};
        String[] configuredMovers = Config.getString("tagMovers", "linear,bezier,aggressive,pippi,circular,axis,exgon,momentum,spline,flower").split(",");
        
        if (configuredMovers.length == 0) {
            return defaultMovers;
        }
        
        // Clean up mover names
        for (int i = 0; i < configuredMovers.length; i++) {
            configuredMovers[i] = configuredMovers[i].trim().toLowerCase();
        }
        
        return configuredMovers;
    }
    
    public void assignObjectsToCursors(List<GameObject> allObjects) {
        if (!isInitialized) {
            initialize(cursorCount);
        }
        
        // Clear previous assignments
        for (TagCursor cursor : cursors) {
            cursor.assignedObjects.clear();
            cursor.currentObjectIndex = 0;
        }
        
        // Assign objects to cursors (following danser-go logic)
        for (int i = 0; i < allObjects.size(); i++) {
            GameObject object = allObjects.get(i);
            int cursorIndex = i % cursorCount;
            cursors.get(cursorIndex).assignObject(object);
        }
    }
    
    public void assignObjectsToCursorsComboTag(List<GameObject> allObjects) {
        if (!isInitialized) {
            initialize(cursorCount);
        }
        
        // Clear previous assignments
        for (TagCursor cursor : cursors) {
            cursor.assignedObjects.clear();
            cursor.currentObjectIndex = 0;
        }
        
        // Assign objects to cursors by combo (following danser-go ComboTag logic)
        // Using object ID as combo proxy since getComboIndex() is not available
        for (int i = 0; i < allObjects.size(); i++) {
            GameObject object = allObjects.get(i);
            int comboSet = object.getId(); // Use ID as combo proxy
            int cursorIndex = comboSet % cursorCount;
            cursors.get(cursorIndex).assignObject(object);
        }
    }
    
    public void updateCursors(float currentTime) {
        if (!isInitialized) return;
        
        for (TagCursor cursor : cursors) {
            updateCursor(cursor, currentTime);
        }
    }
    
    private void updateCursor(TagCursor cursor, float currentTime) {
        if (!cursor.isActive || !cursor.hasMoreObjects()) {
            return;
        }
        
        GameObject currentObject = cursor.getCurrentObject();
        if (currentObject == null) {
            return;
        }
        
        float objectTime = currentObject.getHitTime();
        PointF objectPos = new PointF(currentObject.getPosition().x, currentObject.getPosition().y);
        
        // Check if it's time to hit the object (following danser-go timing)
        if (currentTime >= objectTime) {
            // Hit current object - cursor should be at object position
            cursor.position.set(objectPos.x, objectPos.y);
            cursor.lastPosition = new PointF(objectPos.x, objectPos.y);
            
            // Move to next object for future
            cursor.moveToNextObject();
            GameObject nextObject = cursor.getCurrentObject();
            
            if (nextObject != null) {
                PointF nextPos = new PointF(nextObject.getPosition().x, nextObject.getPosition().y);
                // Start movement to next object
                cursor.movementManager.startMovement(objectPos, nextPos, currentTime, nextObject.getHitTime());
            }
            
        } else if (currentTime >= objectTime - 100) { // 100ms preempt like danser-go
            // In preempt window - move towards object
            PointF newPos = cursor.movementManager.updatePosition(currentTime, currentObject);
            if (newPos != null) {
                cursor.position.set(newPos.x, newPos.y);
            } else {
                // If no movement active, interpolate to object
                float t = (currentTime - (objectTime - 100)) / 100f; // 0 to 1 over 100ms
                t = Math.max(0, Math.min(1, t));
                
                if (cursor.lastPosition != null) {
                    cursor.position.set(
                        cursor.lastPosition.x + t * (objectPos.x - cursor.lastPosition.x),
                        cursor.lastPosition.y + t * (objectPos.y - cursor.lastPosition.y)
                    );
                } else {
                    cursor.position.set(objectPos.x, objectPos.y);
                }
            }
        } else {
            // Before preempt - stay at last position or move to next object if available
            GameObject nextObject = cursor.getCurrentObject();
            if (nextObject != null && nextObject != currentObject) {
                // Move towards next object early
                PointF nextPos = new PointF(nextObject.getPosition().x, nextObject.getPosition().y);
                float t = Math.max(0, Math.min(1, (currentTime - (objectTime - 500)) / 400f)); // Start moving 500ms early
                
                if (cursor.lastPosition != null) {
                    cursor.position.set(
                        cursor.lastPosition.x + t * (nextPos.x - cursor.lastPosition.x),
                        cursor.lastPosition.y + t * (nextPos.y - cursor.lastPosition.y)
                    );
                } else {
                    cursor.position.set(nextPos.x, nextPos.y);
                }
            }
        }
    }
    
    public List<TagCursor> getCursors() {
        return new ArrayList<>(cursors);
    }
    
    public int getCursorCount() {
        return cursorCount;
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }
    
    public void reset() {
        if (cursors != null) {
            cursors.clear();
        }
        isInitialized = false;
    }
}
