package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.main;

import android.graphics.PointF;
import android.util.Log;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameObject;
import ru.nsu.ccfit.zuev.osu.game.GameObjectListener;
import ru.nsu.ccfit.zuev.osu.game.ISliderListener;
import ru.nsu.ccfit.zuev.osu.game.GameplaySpinner;
import ru.nsu.ccfit.zuev.osu.game.cursor.AutoplayStyle;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.CursorMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.MoverFactory;

/**
 * Auto cursor that follows beatmap objects using danser-go movement styles.
 *
 * How danser-go handles hit-through (GenericScheduler):
 * 1. A SINGLE mover instance is kept for the entire beatmap (not recreated per object).
 * 2. The scheduler maintains a queue of objects and feeds them to the mover via SetObjects().
 * 3. The mover creates curves between CONSECUTIVE OBJECT POSITIONS (not cursor-to-object).
 * 4. During an object's time window, the scheduler either:
 *    - Snaps to object position when object starts (lastTime <= gStartTime)
 *    - Calls mover.GetObjectsPosition(time, g) during the object for smooth hit-through
 * 5. Between objects, the scheduler calls mover.Update(time) for the curve transition.
 * 6. Objects are consumed from the queue as they pass.
 *
 * This implementation approximates that by:
 * - Keeping a single mover instance across the whole play session
 * - Calling setMovement with the OBJECT start/end positions (not cursor->object)
 * - Following the mover curve continuously even during objects
 * - Tracking game time so the mover curve is sampled at the right points
 */
public class AutoCursor extends CursorEntity implements ISliderListener {
    private int currentObjectId = -1;
    private CursorMover currentMover;
    private AutoplayStyle currentStyle;

    // Game time tracking in milliseconds (for mover timeline)
    private float gameTimeMs = 0;

    // Previous object end position (for hit-through continuity)
    private PointF lastObjectEndPos;

    // Flag: first object initialization done
    private boolean initialized = false;

    public AutoCursor() {
        super();
        this.setPosition(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f);
        this.setShowing(true);
        loadAutoplayStyle();
    }

    private void loadAutoplayStyle() {
        String styleValue = Config.getString("autoplayStyle", "linear");
        currentStyle = AutoplayStyle.fromValue(styleValue);
        // Create ONE mover instance for the entire game session
        currentMover = MoverFactory.createMover(currentStyle);
        Log.d("AutoCursor", "Loaded autoplay style: " + currentStyle);
    }

    /**
     * Called every frame to advance cursor along the mover curve.
     * Only moves the cursor when the movement is still active.
     * Once finished (e.g. during slider follow or spinner), the game engine
     * positions the cursor via updateAutoBasedPos() - we must NOT override that.
     */
    public void updateMovement(float deltaTimeSeconds) {
        if (currentMover == null) return;

        gameTimeMs += deltaTimeSeconds * 1000;

        // Only update cursor position if the movement is still in progress.
        // After the movement finishes (during slider follow / spinner / gap between objects),
        // the game engine handles cursor positioning via updateAutoBasedPos().
        if (!currentMover.isFinished(gameTimeMs)) {
            PointF pos = currentMover.getPositionAt(gameTimeMs);
            if (pos != null) {
                setPosition(pos.x, pos.y);
            }
        }
    }

    public void setPosition(float pX, float pY, GameObjectListener listener) {
        setPosition(pX, pY);
        listener.onUpdatedAutoCursor(pX, pY);
    }

    /**
     * Move cursor to the specified object using the current mover style.
     *
     * Key hit-through changes:
     * 1. Uses OBJECT end/start positions (not cursor position) as start position
     * 2. Keeps the same mover instance (state persists across calls)
     * 3. Sets the mover's timeline correctly so getPositionAt returns smooth positions
     */
    public void moveToObject(GameObject object, float secPassed, GameObjectListener listener) {
        if (object == null || currentObjectId == object.getId()) {
            return;
        }

        // Sync game time to the game clock
        gameTimeMs = secPassed * 1000;

        float movePositionX = object.getPosition().x;
        float movePositionY = object.getPosition().y;
        float hitTimeMs = object.getHitTime() * 1000;

        if (object instanceof GameplaySpinner) {
            movePositionY += 50;
        }

        currentObjectId = object.getId();

        PointF targetPos = new PointF(movePositionX, movePositionY);

        if (!initialized) {
            // First object: just snap to it
            setPosition(targetPos.x, targetPos.y, listener);
            lastObjectEndPos = new PointF(targetPos.x, targetPos.y);
            initialized = true;
            return;
        }

        // Calculate start and end times for the mover
        float startTimeMs = gameTimeMs;
        float endTimeMs = hitTimeMs;

        // Minimum reaction time (85ms)
        float deltaT = endTimeMs - startTimeMs;
        if (deltaT < 85 && !(object instanceof GameplaySpinner)) {
            deltaT = 85;
            endTimeMs = startTimeMs + deltaT;
        }

        // HIT-THROUGH: Use the ACTUAL cursor position as curve start point.
        // For sliders, the cursor follows the slider ball to its end position,
        // so getX()/getY() reflects the slider's end, not its hit position.
        // This prevents the "cursor jumps back to slider start" bug.
        PointF startPos = new PointF(getX(), getY());

        // Reuse the SAME mover instance - state continuity is essential
        // HalfCircleMover needs invert to persist, BezierMover needs previousSpeed, etc.
        currentMover.setMovement(startPos, targetPos, startTimeMs, endTimeMs);

        // Store end position for next object's hit-through
        lastObjectEndPos = new PointF(targetPos.x, targetPos.y);

        listener.onUpdatedAutoCursor(targetPos.x, targetPos.y);
    }

    public void setAutoplayStyle(AutoplayStyle style) {
        this.currentStyle = style;
        // Reset game time and mover state when switching styles
        this.currentMover = MoverFactory.createMover(style);
        this.initialized = false;
        this.gameTimeMs = 0;
        Log.d("AutoCursor", "Set autoplay style: " + style);
    }

    public AutoplayStyle getAutoplayStyle() {
        return currentStyle;
    }

    /**
     * Reset the autoplay state for a new play session.
     */
    public void reset() {
        currentObjectId = -1;
        gameTimeMs = 0;
        initialized = false;
        lastObjectEndPos = null;

        if (currentMover != null) {
            currentMover.reset();
        }

        this.setPosition(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f);
    }

    @Override
    public void onSliderStart() {
        cursorSprite.onSliderStart();
    }

    @Override
    public void onSliderTracking() {
        cursorSprite.onSliderTracking();
    }

    @Override
    public void onSliderEnd() {
        cursorSprite.onSliderEnd();
    }
}
