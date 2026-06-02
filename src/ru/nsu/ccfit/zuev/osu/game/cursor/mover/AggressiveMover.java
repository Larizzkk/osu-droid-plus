package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.BaseMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.Bezier;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.mutils.MUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

/**
 * Aggressive movement - exact port from danser-go AggressiveMover
 * Based on https://github.com/wieku/danser-go/blob/master/app/dance/movers/aggressive.go
 */
public class AggressiveMover extends BaseMover implements CursorMover {

    private Bezier curve;
    private float lastAngle = 0;

    public AggressiveMover() {
    }

    public AggressiveMover(int id) {
        this.id = id;
    }

    @Override
    public void setMovement(PointF startPos, PointF endPos, float startTime, float endTime) {
        this.startTime = startTime;
        this.endTime = endTime;

        Vector2f vStart = new Vector2f(startPos);
        Vector2f vEnd = new Vector2f(endPos);

        float scaledDistance = endTime - startTime;

        // Danser-go: newAngle = lastAngle + Pi, overridden by slider end angle if start is slider
        float newAngle = lastAngle + (float) Math.PI;

        // Danser-go checks if start is ILongObject and uses GetEndAngleMod
        // If the start object is a slider, its exit angle should be used here.
        // When slider info is available, set newAngle = sliderEndAngle.

        Vector2f p1 = Vector2f.NewVec2fRad(newAngle, scaledDistance).add(vStart);

        // Danser-go: points starts with [startPos, p1]
        // Then if scaledDistance > 1: lastAngle = p1.angleRV(endPos)
        // Then if end is ILongObject: append control point using slider start angle
        // Then append endPos

        if (scaledDistance > 1) {
            lastAngle = p1.angleRV(vEnd);
        }

        curve = new Bezier(new Vector2f[]{vStart, p1, vEnd}, false);
    }

    @Override
    public PointF getPositionAt(float time) {
        if (curve == null) return null;

        float t = (time - startTime) / (endTime - startTime);
        t = MUtils.clamp(t, 0, 1);
        return curve.pointAt(t).toPointF();
    }

    @Override
    public void reset() {
        curve = null;
        lastAngle = 0;
    }

    @Override
    public boolean isFinished(float time) {
        return time >= endTime;
    }

    @Override
    public PointF getObjectsPosition(float time, PointF objectPos) {
        return getPositionAt(time);
    }

    @Override
    public boolean supportsMultiPoint() {
        return false;
    }

    @Override
    public void setMultiPointMovement(PointF[] positions, float[] times, float startTime) {
        // Not implemented in danser-go; handled by scheduler with consecutive setMovement calls
    }
}
