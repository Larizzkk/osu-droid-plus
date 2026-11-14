package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.BaseMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.animation.easing.Easing;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.CirArc;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.Curve;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.LinearCurve;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.MultiCurve;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.mutils.MUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

import android.graphics.PointF;

/**
 * HalfCircle movement - 1:1 exact port from danser-go HalfCircleMover
 * Based on https://github.com/wieku/danser-go/blob/master/app/dance/movers/halfcircle.go
 */
public class HalfCircleMoverExact implements CursorMover {

    private Curve curve;
    private float invert = -1f;
    private int id;

    private float startTime;
    private float endTime;

    // Config defaults from danser-go
    private float streamTrigger = -1f;
    private float radiusMultiplier = 1.0f;

    public HalfCircleMoverExact() {
        this.invert = -1f;
    }

    public HalfCircleMoverExact(int id) {
        this();
        this.id = id;
    }

    @Override
    public void setMovement(PointF startPos, PointF endPos, float startTime, float endTime) {
        this.startTime = startTime;
        this.endTime = endTime;

        Vector2f startVec = new Vector2f(startPos);
        Vector2f endVec = new Vector2f(endPos);

        if (streamTrigger < 0 || (endTime - startTime) < streamTrigger) {
            invert *= -1;
        }

        if (startVec.equals(endVec)) {
            curve = new LinearCurve(startVec, endVec);
        } else {
            Vector2f point = startVec.mid(endVec);
            // p := point.Sub(startPos).Rotate(mover.invert * math.Pi / 2).Scl(float32(config.RadiusMultiplier)).Add(point)
            Vector2f p = point.sub(startVec).rotate(invert * (float) Math.PI / 2f).scl(radiusMultiplier).add(point);
            curve = new CirArc(startVec, p, endVec);
        }
    }

    @Override
    public PointF getPositionAt(float time) {
        if (curve == null) return null;

        float t = (time - startTime) / (endTime - startTime);
        t = MUtils.clamp(t, 0f, 1f);

        return curve.pointAt(t).toPointF();
    }

    @Override
    public void reset() {
        curve = null;
        invert = -1f;
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
        return true; // HalfCircleExact now supports multi-point movement
    }

    @Override
    public void setMultiPointMovement(PointF[] positions, float[] times, float startTime) {
        if (positions == null || positions.length < 2 || times == null || times.length != positions.length) {
            return;
        }

        this.startTime = startTime;
        this.endTime = times[times.length - 1];

        // Create half-circle multi-point curve
        Vector2f[] points = new Vector2f[positions.length];
        for (int i = 0; i < positions.length; i++) {
            points[i] = new Vector2f(positions[i]);
        }

        // Generate exact half-circle control points
        Vector2f[] curvePoints = generateHalfCircleExactPoints(points, times);

        if (curvePoints.length == 2) {
            curve = new LinearCurve(curvePoints[0], curvePoints[1]);
        } else if (curvePoints.length >= 3) {
            curve = new MultiCurve(curvePoints);
        }
    }

    private Vector2f[] generateHalfCircleExactPoints(Vector2f[] positions, float[] times) {
        if (positions.length == 2) {
            // Simple case: create exact half-circle
            Vector2f start = positions[0];
            Vector2f end = positions[1];

            if (start.equals(end)) {
                return new Vector2f[]{start, end};
            }

            float streamTrigger = 100f;
            if (streamTrigger < 0 || (times[1] - times[0]) < streamTrigger) {
                invert = -1 * invert;
            }

            Vector2f point = start.mid(end);
            float radiusMultiplier = 1.0f;
            Vector2f p = point.sub(start).rotate(invert * (float) Math.PI / 2f).scl(radiusMultiplier).add(point);

            return new Vector2f[]{start, p, end};
        } else {
            // Complex case: create connected exact half-circles
            Vector2f[] curvePoints = new Vector2f[positions.length * 2 - 1];
            curvePoints[0] = positions[0];

            for (int i = 1; i < positions.length; i++) {
                Vector2f prev = positions[i-1];
                Vector2f curr = positions[i];

                if (prev.equals(curr)) {
                    curvePoints[i*2-1] = prev;
                    curvePoints[i*2] = curr;
                    continue;
                }

                float streamTrigger = 100f;
                if (streamTrigger < 0 || (times[i] - times[i-1]) < streamTrigger) {
                    invert = -1 * invert;
                }

                Vector2f point = prev.mid(curr);
                float radiusMultiplier = 1.0f;
                Vector2f p = point.sub(prev).rotate(invert * (float) Math.PI / 2f).scl(radiusMultiplier).add(point);

                curvePoints[i*2-1] = p;
                curvePoints[i*2] = curr;
            }

            return curvePoints;
        }
    }
}
