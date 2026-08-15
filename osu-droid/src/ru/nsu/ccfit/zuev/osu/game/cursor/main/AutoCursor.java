package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameObject;
import ru.nsu.ccfit.zuev.osu.game.GameObjectListener;
import ru.nsu.ccfit.zuev.osu.game.GameplaySpinner;
import ru.nsu.ccfit.zuev.osu.game.ISliderListener;
import ru.nsu.ccfit.zuev.osu.game.cursor.AutoplayStyle;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.CursorMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.MoverFactory;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.SliderAwareMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.SliderMetadataResolver;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.SliderMovementContext;

public class AutoCursor extends CursorEntity implements ISliderListener {

    private int currentObjectId = -1;
    private CursorMover currentMover;
    private AutoplayStyle currentStyle;
    private float gameTimeMs = 0;
    private boolean initialized = false;

    private GameObject previousObject;
    private GameObjectListener cursorListener;

    // While the cursor is riding a slider's ball (driven by GameplaySlider via
    // followSlider), updateMovement must NOT override the position with the mover
    // output - otherwise the cursor snaps back to the slider's head (teleport bug).
    private boolean followingSlider = false;
    // When the current slider just finished in the previous frame but the next
    // mover segment hasn't been configured yet (slider still in activeObjects),
    // hold the cursor at the last known slider-tail position for one frame.
    private boolean justFinishedSlider = false;

    public AutoCursor() {
        super();
        this.setPosition(
            Config.getRES_WIDTH() / 2f,
            Config.getRES_HEIGHT() / 2f
        );
        this.setShowing(true);
        loadAutoplayStyle();
    }

    private void loadAutoplayStyle() {
        String styleValue = Config.getString("autoplayStyle", "linear");
        currentStyle = AutoplayStyle.fromValue(styleValue);
        currentMover = MoverFactory.createMover(currentStyle);
    }

    public void updateMovement(float deltaTimeSeconds) {
        if (currentMover == null) return;

        gameTimeMs += deltaTimeSeconds * 1000;

        if (followingSlider && !justFinishedSlider) {
            if (cursorListener != null) {
                cursorListener.onUpdatedAutoCursor(getX(), getY());
            }
            return;
        }

        if (justFinishedSlider) {
            if (cursorListener != null) {
                cursorListener.onUpdatedAutoCursor(getX(), getY());
            }
            justFinishedSlider = false;
            return;
        }

        // Follow the mover output exactly, like danser-go. When the current movement
        // is finished but the next one hasn't been queued yet, rest on the target.
        PointF moverPos = currentMover.getPositionAt(gameTimeMs);

        if (moverPos != null) {
            setPosition(moverPos.x, moverPos.y);
        } else {
            PointF target = getCurrentTargetPosition();
            setPosition(target.x, target.y);
        }

        if (cursorListener != null) {
            cursorListener.onUpdatedAutoCursor(getX(), getY());
        }
    }

    private PointF getCurrentTargetPosition() {
        if (previousObject == null) {
            return new PointF(getX(), getY());
        }
        return new PointF(previousObject.getPosition().x, previousObject.getPosition().y);
    }

    public void setPosition(float pX, float pY, GameObjectListener listener) {
        setPosition(pX, pY);
        listener.onUpdatedAutoCursor(pX, pY);
    }

    public void moveToObject(
            GameObject object,
            float secPassed,
            GameObjectListener listener
    ) {
        if (object == null) {
            followingSlider = false;
            return;
        }
        if (currentObjectId == object.getId()) {
            justFinishedSlider = false;
            return;
        }

        cursorListener = listener;
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
            setPosition(targetPos.x, targetPos.y, listener);
            initialized = true;
            previousObject = object;
            return;
        }

        float startTimeMs = gameTimeMs;
        float endTimeMs = hitTimeMs;

        float deltaT = endTimeMs - startTimeMs;
        if (deltaT < 85 && !(object instanceof GameplaySpinner)) {
            deltaT = 85;
            endTimeMs = startTimeMs + deltaT;
        }

        PointF startPos = new PointF(getX(), getY());

        if (currentMover instanceof SliderAwareMover sliderMover) {
            SliderMetadataResolver.SegmentMetadata meta = SliderMetadataResolver.resolveSegment(
                    previousObject, object,
                    startTimeMs, endTimeMs,
                    startPos, targetPos
            );

            SliderMovementContext ctx = SliderMovementContext.builder()
                    .startPos(startPos)
                    .endPos(targetPos)
                    .startTime(startTimeMs)
                    .endTime(endTimeMs)
                    .startIsSlider(meta.start.isSlider)
                    .startAngle(meta.start.endAngle)
                    .endIsSlider(meta.end.isSlider)
                    .endAngle(meta.end.startAngle)
                    .startDistance(meta.start.distanceToReference)
                    .endDistance(meta.end.distanceToReference)
                    .build();

            sliderMover.setMovement(ctx);
        } else {
            currentMover.setMovement(startPos, targetPos, startTimeMs, endTimeMs);
        }

        previousObject = object;
        followingSlider = false;
        listener.onUpdatedAutoCursor(targetPos.x, targetPos.y);
    }

    public void setAutoplayStyle(AutoplayStyle style) {
        this.currentStyle = style;
        this.currentMover = MoverFactory.createMover(style);
        this.initialized = false;
        this.gameTimeMs = 0;
        this.previousObject = null;
    }

    public AutoplayStyle getAutoplayStyle() {
        return currentStyle;
    }

    public void reset() {
        currentObjectId = -1;
        gameTimeMs = 0;
        initialized = false;
        previousObject = null;
        followingSlider = false;
        justFinishedSlider = false;
        cursorListener = null;
        if (currentMover != null) currentMover.reset();
        this.setPosition(
            Config.getRES_WIDTH() / 2f,
            Config.getRES_HEIGHT() / 2f
        );
    }

    /**
     * Called by GameScene while a slider ball is being tracked (autoplay/autopilot).
     * The cursor rides the ball: position is owned here, and updateMovement must not
     * override it (see followingSlider). This keeps slider movement smooth and avoids
     * the teleport back to the slider's head when the slider ends.
     */
    public void followSlider(float x, float y) {
        followingSlider = true;
        setPosition(x, y);
        if (cursorListener != null) {
            cursorListener.onUpdatedAutoCursor(x, y);
        }
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
        justFinishedSlider = true;
        cursorSprite.onSliderEnd();
    }
}
