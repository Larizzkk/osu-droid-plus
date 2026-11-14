package ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

public abstract class BaseMover {
    protected float startTime;
    protected float endTime;
    protected int id;

    protected float preempt = 450f;
    protected float speed = 1.0f;
    protected float circleRadius = 64f;

    protected boolean waitForPreempt = true;
    protected float reactionTime = 100f;
    protected boolean choppyLongObjects = false;

    public void reset(int id) {
        this.id = id;
    }

    public void setDifficulty(float preempt, float speed, float circleRadius) {
        this.preempt = preempt;
        this.speed = speed;
        this.circleRadius = circleRadius;
    }

    public float getPreempt() {
        return preempt;
    }

    public float getSpeed() {
        return speed;
    }

    public float getCircleRadius() {
        return circleRadius;
    }

    public float getStartTime() {
        return startTime;
    }

    public float getEndTime() {
        return endTime;
    }

    public PointF getObjectsPosition(float time, PointF objectPos) {
        return null;
    }

    protected void adjustStartTime(float preempt, float speed) {
        if (waitForPreempt) {
            float adjustedTime = endTime - (preempt - reactionTime * speed);
            startTime = Math.max(startTime, adjustedTime);
        }
    }

    public boolean supportsMultiPoint() {
        return false;
    }

    public abstract void setMovement(PointF startPos, PointF endPos, float startTime, float endTime);

    public void setMultiPointMovement(PointF[] positions, float[] times, float startTime) {
        if (positions != null && positions.length >= 2 && times != null && times.length >= 2) {
            setMovement(positions[0], positions[positions.length - 1], startTime, times[times.length - 1]);
        }
    }
}
