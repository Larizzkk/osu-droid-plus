package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.BaseMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.animation.easing.Easing;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.LinearCurve;

/**
 * Linear movement - exact port from danser-go LinearMover
 * Based on https://github.com/wieku/danser-go/blob/master/app/dance/movers/linear.go
 */
public class LinearMover extends BaseMover implements CursorMover {

    private LinearCurve line;

    public LinearMover() {}

    public LinearMover(boolean simple) {}

    public LinearMover(boolean simple, int id) {
        this.id = id;
    }

    @Override
    public void setMovement(PointF startPos, PointF endPos, float startTime, float endTime) {
        this.startTime = startTime;
        this.endTime = endTime;

        Vector2f vStart = new Vector2f(startPos);
        Vector2f vEnd = new Vector2f(endPos);

        this.line = new LinearCurve(vStart, vEnd);

        // Adjust timing for more natural movement
        float preempt = 600f; // Default preempt time
        float speed = 1.0f;
        adjustStartTime(preempt, speed);
    }

    @Override
    public PointF getObjectsPosition(float time, PointF objectPos) {
        return null;
    }

    @Override
    public PointF getPositionAt(float time) {
        if (line == null) return null;

        float t = (time - startTime) / (endTime - startTime);
        // Clamp to [0, 1] to match danser-go behavior
        t = Math.max(0, Math.min(1, t));
        // Apply OutQuad easing like in danser-go
        float easedT = (float) Easing.outQuad(t);
        return line.pointAt(easedT).toPointF();
    }

    @Override
    public void reset() {
        line = null;
        startTime = 0;
        endTime = 0;
    }

    @Override
    public boolean isFinished(float time) {
        return time >= endTime;
    }

    @Override
    public boolean supportsMultiPoint() {
        return false; // Linear movement doesn't support multi-point for continuous movement
    }

    @Override
    public void setMultiPointMovement(PointF[] positions, float[] times, float startTime) {
        // Linear mover doesn't support multi-point movement
        // Fall back to simple two-point movement
        if (positions != null && positions.length >= 2 && times != null && times.length >= 2) {
            setMovement(positions[0], positions[positions.length - 1], startTime, times[times.length - 1]);
        }
    }
}
