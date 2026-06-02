package ru.nsu.ccfit.zuev.osuplusplus.game.autoplay;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameObject;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.*;

/**
 * Manages autoplay cursor movement patterns and behaviors
 * Delegating to CursorMover implementations for 1:1 danser-go parity.
 */
public class AutoplayMovementManager {
    
    private CursorMover mover;
    private PointF currentPosition;
    private String moverType;
    
    public AutoplayMovementManager() {
        // Initialize at screen center
        float centerX = Config.getRES_WIDTH() / 2f;
        float centerY = Config.getRES_HEIGHT() / 2f;
        currentPosition = new PointF(centerX, centerY);
        loadSettings();
    }
    
    public void loadSettings() {
        setMovementType(Config.getString("autoplayMovementType", "linear"));
    }
    
    public void setMovementType(String type) {
        this.moverType = type.toLowerCase();
        switch (moverType) {
            case "bezier":
                mover = new BezierMover(0);
                break;
            case "aggressive":
                mover = new AggressiveMover(0);
                break;
            case "pippi":
                mover = new PippiMover(0);
                break;
            case "circular":
                mover = new HalfCircleMover(0);
                break;
            case "axis":
                mover = new AxisMover(0);
                break;
            case "exgon":
                mover = new ExGonMover(0);
                break;
            case "momentum":
                mover = new MomentumMover(0);
                break;
            case "spline":
                mover = new SplineMover(0);
                break;
            case "flower":
                mover = new AngleOffsetMover(0);
                break;
            case "linear":
            default:
                mover = new LinearMover(false, 0);
                break;
        }
    }
    
    public void startMovement(PointF from, PointF to, float startTime) {
        // Find the hit time of the object or calculate it. 
        // For simplicity in Tag mode, we assume movement ends at the next object's hit time.
    }
    
    // This is called by TagManager
    public void startMovement(PointF from, PointF to, float startTime, float endTime) {
        if (mover != null) {
            mover.setMovement(from, to, startTime, endTime);
        }
    }
    
    public PointF updatePosition(float currentTime, GameObject targetObject) {
        if (mover == null) {
            return currentPosition;
        }
        
        // If we don't have an active movement but have a target, start one if possible.
        // In TagManager, startMovement is called explicitly.
        
        PointF pos = mover.getPositionAt(currentTime);
        if (pos != null) {
            currentPosition.set(pos.x, pos.y);
        }
        
        return currentPosition;
    }
    
    public PointF getCurrentPosition() {
        return currentPosition;
    }
    
    public void reset() {
        if (mover != null) {
            mover.reset();
        }
        float centerX = Config.getRES_WIDTH() / 2f;
        float centerY = Config.getRES_HEIGHT() / 2f;
        currentPosition.set(centerX, centerY);
    }
}
