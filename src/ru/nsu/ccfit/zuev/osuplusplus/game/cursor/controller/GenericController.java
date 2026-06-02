package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.controller;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.CursorMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.MoverFactory;
import ru.nsu.ccfit.zuev.osu.game.cursor.AutoplayStyle;
import ru.nsu.ccfit.zuev.osuplusplus.game.cursor.scheduler.GenericScheduler;
import ru.nsu.ccfit.zuev.osuplusplus.game.cursor.trail.CursorTrailSystem;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.PlayfieldBounds;
import ru.nsu.ccfit.zuev.osu.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic cursor controller - exact port from danser-go GenericController
 * Based on https://github.com/wieku/danser-go/blob/master/app/dance/controller.go
 */
public class GenericController {
    
    private List<CursorData> cursors = new ArrayList<>();
    private List<GenericScheduler> schedulers = new ArrayList<>();
    private int cursorCount = 1;
    
    public GenericController() {
        // Default constructor
    }
    
    public GenericController(int cursorCount) {
        this.cursorCount = cursorCount;
    }
    
    /**
     * Initialize cursors and schedulers
     */
    public void initCursors() {
        cursors.clear();
        schedulers.clear();
        
        // Create cursors and schedulers
        for (int i = 0; i < cursorCount; i++) {
            CursorData cursorData = new CursorData();
            cursorData.cursor = new CursorData.Cursor();
            cursorData.trailSystem = new CursorTrailSystem();
            cursorData.currentMover = MoverFactory.createMover(AutoplayStyle.LINEAR);
            
            // Configure trail based on mover style
            configureTrailSystem(cursorData.trailSystem, cursorData.currentMover);
            
            cursors.add(cursorData);
            schedulers.add(new GenericScheduler(cursorData.currentMover, i));
        }
    }
    
    /**
     * Update all cursors
     */
    public void update(float time, float deltaTime) {
        for (int i = 0; i < cursors.size(); i++) {
            CursorData cursorData = cursors.get(i);
            GenericScheduler scheduler = schedulers.get(i);
            
            // Update scheduler
            scheduler.update(time, deltaTime);
            
            // Update cursor position
            PointF newPos = scheduler.getPositionAt(time);
            if (newPos != null) {
                cursorData.cursor.setPosition(newPos.x, newPos.y);
                
                // Update trail system
                cursorData.trailSystem.update(deltaTime, newPos);
            }
            
            // Update cursor visual effects
            cursorData.cursor.update(deltaTime);
        }
    }
    
    /**
     * Get all cursors
     */
    public List<CursorData.Cursor> getCursors() {
        List<CursorData.Cursor> result = new ArrayList<>();
        for (CursorData cursorData : cursors) {
            result.add(cursorData.cursor);
        }
        return result;
    }
    
    /**
     * Get all cursor data
     */
    public List<CursorData> getAllCursorData() {
        return cursors;
    }
    
    /**
     * Set mover for specific cursor
     */
    public void setMover(int cursorIndex, AutoplayStyle style) {
        if (cursorIndex >= 0 && cursorIndex < cursors.size()) {
            CursorData cursorData = cursors.get(cursorIndex);
            CursorMover newMover = MoverFactory.createMover(style);
            
            cursorData.currentMover = newMover;
            schedulers.get(cursorIndex).setMover(newMover);
            
            // Reconfigure trail system
            configureTrailSystem(cursorData.trailSystem, newMover);
        }
    }
    
    /**
     * Configure trail system based on mover style
     */
    private void configureTrailSystem(CursorTrailSystem trailSystem, CursorMover mover) {
        // Determine mover style
        AutoplayStyle style = AutoplayStyle.LINEAR; // Default
        
        if (mover instanceof ru.nsu.ccfit.zuev.osu.game.cursor.mover.AggressiveMover) {
            style = AutoplayStyle.AGGRESSIVE;
        } else if (mover instanceof ru.nsu.ccfit.zuev.osu.game.cursor.mover.BezierMover) {
            style = AutoplayStyle.BEZIER;
        } else if (mover instanceof ru.nsu.ccfit.zuev.osu.game.cursor.mover.SplineMover) {
            style = AutoplayStyle.SPLINE;
        } else if (mover instanceof ru.nsu.ccfit.zuev.osu.game.cursor.mover.PippiMover) {
            style = AutoplayStyle.PIPPI;
        } else if (mover instanceof ru.nsu.ccfit.zuev.osu.game.cursor.mover.MomentumMover) {
            style = AutoplayStyle.MOMENTUM;
        } else if (mover instanceof ru.nsu.ccfit.zuev.osu.game.cursor.mover.ExGonMover) {
            style = AutoplayStyle.EXGON;
        } else if (mover instanceof ru.nsu.ccfit.zuev.osu.game.cursor.mover.AxisMover) {
            style = AutoplayStyle.AXIS;
        } else if (mover instanceof ru.nsu.ccfit.zuev.osu.game.cursor.mover.HalfCircleMover) {
            style = AutoplayStyle.CIRCULAR;
        } else if (mover instanceof ru.nsu.ccfit.zuev.osu.game.cursor.mover.AngleOffsetMover) {
            style = AutoplayStyle.FLOWER;
        }
        
        // Configure trail based on style
        switch (style) {
            case AGGRESSIVE:
                trailSystem.setTrailStyle(CursorTrailSystem.TrailStyle.DISTANCE_RAINBOW);
                trailSystem.setStyle23Speed(0.5f);
                trailSystem.setTrailScale(1.2f);
                break;
            case BEZIER:
                trailSystem.setTrailStyle(CursorTrailSystem.TrailStyle.TIME_RAINBOW);
                trailSystem.setStyle23Speed(0.3f);
                trailSystem.setTrailScale(1.0f);
                break;
            case PIPPI:
                trailSystem.setTrailStyle(CursorTrailSystem.TrailStyle.GRADIENT);
                trailSystem.setStyle4Shift(0.8f);
                trailSystem.setTrailScale(1.5f);
                break;
            case EXGON:
                trailSystem.setTrailStyle(CursorTrailSystem.TrailStyle.UNIFIED_COLOR);
                trailSystem.setTrailScale(0.8f);
                break;
            default:
                trailSystem.setTrailStyle(CursorTrailSystem.TrailStyle.UNIFIED_COLOR);
                trailSystem.setTrailScale(1.0f);
                break;
        }
        
        // Common settings
        trailSystem.setTrailDensity(1.0f);
        trailSystem.setTrailMaxLength(1000);
        trailSystem.setEnableTrailGlow(true);
    }
    
    /**
     * Reset all cursors
     */
    public void reset() {
        for (CursorData cursorData : cursors) {
            cursorData.currentMover.reset();
            cursorData.trailSystem.clear();
            cursorData.cursor.reset();
        }
        
        for (GenericScheduler scheduler : schedulers) {
            scheduler.reset();
        }
    }
    
    /**
     * Cursor data container
     */
    public static class CursorData {
        public Cursor cursor;
        public CursorMover currentMover;
        public CursorTrailSystem trailSystem;
        
        public CursorData() {
            cursor = new Cursor();
            currentMover = null;
            trailSystem = null;
        }
        
        /**
         * Simple cursor class for position tracking
         */
        public static class Cursor {
            private PointF position = new PointF(0, 0);
            private float scale = 1.0f;
            private float alpha = 1.0f;
            private boolean visible = true;
            private boolean leftButton = false;
            private boolean rightButton = false;
            
            public void setPosition(float x, float y) {
                position.x = x;
                position.y = y;
            }
            
            public PointF getPosition() {
                return position;
            }
            
            public void setScale(float scale) {
                this.scale = scale;
            }
            
            public float getScale() {
                return scale;
            }
            
            public void setAlpha(float alpha) {
                this.alpha = alpha;
            }
            
            public float getAlpha() {
                return alpha;
            }
            
            public void setVisible(boolean visible) {
                this.visible = visible;
            }
            
            public boolean isVisible() {
                return visible;
            }
            
            public void setLeftButton(boolean pressed) {
                this.leftButton = pressed;
            }
            
            public boolean isLeftButtonPressed() {
                return leftButton;
            }
            
            public void setRightButton(boolean pressed) {
                this.rightButton = pressed;
            }
            
            public boolean isRightButtonPressed() {
                return rightButton;
            }
            
            public void update(float deltaTime) {
                // Update animations and effects
            }
            
            public void reset() {
                position = new PointF(0, 0);
                scale = 1.0f;
                alpha = 1.0f;
                visible = true;
                leftButton = false;
                rightButton = false;
            }
        }
    }
}
