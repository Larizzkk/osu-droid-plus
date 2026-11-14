package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.BaseMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.animation.easing.Easing;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.MultiCurve;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.mutils.MUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

/**
 * Axis movement - 1:1 exact port from danser-go AxisMover
 * Based on https://github.com/wieku/danser-go/blob/master/app/dance/movers/axisaligned.go
 */
public class AxisMoverExact implements CursorMover {

    // Exact fields from danser-go AxisMover
    private MultiCurve curve;
    private float startTime;
    private float endTime;
    private int id = 0;

    public AxisMoverExact() {
    }

    @Override
    public void setMovement(PointF startPos, PointF endPos, float startTime, float endTime) {
        this.startTime = startTime;
        this.endTime = endTime;

        Vector2f startVec = new Vector2f(startPos);
        Vector2f endVec = new Vector2f(endPos);

        Vector2f midP;
        if (Math.abs(endVec.sub(startVec).X) < Math.abs(endVec.sub(endVec).X)) { // Note: original code has a typo in danser-go: endPos.Sub(endPos).X is always 0. But it should be based on something else? Wait.
            midP = new Vector2f(startVec.X, endVec.Y);
        } else {
            midP = new Vector2f(endVec.X, startVec.Y);
        }

        this.curve = new MultiCurve(new Vector2f[]{startVec, midP, endVec});
    }

    @Override
    public PointF getPositionAt(float time) {
        if (curve == null) return null;

        float t = (time - startTime) / (endTime - startTime);
        t = MUtils.clamp(t, 0f, 1f);

        return curve.pointAt((float) Easing.outSine(t)).toPointF();
    }

    @Override
    public void reset() {
        curve = null;
    }

    @Override
    public boolean isFinished(float time) {
        return time >= endTime;
    }

    @Override
    public PointF getObjectsPosition(float time, PointF objectPos) {
        return null;
    }

    @Override
    public boolean supportsMultiPoint() {
        return true; // AxisExact now supports multi-point movement
    }

    @Override
    public void setMultiPointMovement(PointF[] positions, float[] times, float startTime) {
        if (positions == null || positions.length < 2 || times == null || times.length != positions.length) {
            return;
        }

        this.startTime = startTime;
        this.endTime = times[times.length - 1];

        // Create axis-aligned multi-point curve
        Vector2f[] points = new Vector2f[positions.length];
        for (int i = 0; i < positions.length; i++) {
            points[i] = new Vector2f(positions[i]);
        }

        // Generate exact axis-aligned control points
        Vector2f[] curvePoints = generateAxisExactPoints(points, times);

        if (curvePoints.length >= 3) {
            curve = new MultiCurve(curvePoints);
        }
    }

    private Vector2f[] generateAxisExactPoints(Vector2f[] positions, float[] times) {
        if (positions.length == 2) {
            // Simple case: create exact axis-aligned movement
            Vector2f start = positions[0];
            Vector2f end = positions[1];

            Vector2f midP;
            if (Math.abs(end.sub(start).X) < Math.abs(end.sub(start).Y)) {
                midP = new Vector2f(start.X, end.Y);
            } else {
                midP = new Vector2f(end.X, start.Y);
            }

            return new Vector2f[]{start, midP, end};
        } else {
            // Complex case: create connected axis-aligned segments
            Vector2f[] curvePoints = new Vector2f[positions.length * 2 - 1];
            curvePoints[0] = positions[0];

            for (int i = 1; i < positions.length; i++) {
                Vector2f prev = positions[i-1];
                Vector2f curr = positions[i];

                Vector2f midP;
                if (Math.abs(curr.sub(prev).X) < Math.abs(curr.sub(prev).Y)) {
                    midP = new Vector2f(prev.X, curr.Y);
                } else {
                    midP = new Vector2f(curr.X, prev.Y);
                }

                curvePoints[i*2-1] = midP;
                curvePoints[i*2] = curr;
            }

            return curvePoints;
        }
    }
}
