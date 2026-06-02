package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.BaseMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.CirArc;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.Curve;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.LinearCurve;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.mutils.MUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

/**
 * Exact port of danser-go HalfCircleMover
 * Based on https://github.com/wieku/danser-go/blob/master/app/dance/movers/halfcircle.go
 */
public class HalfCircleMover extends BaseMover implements CursorMover {

    private Curve curve;
    private float invert;

    public HalfCircleMover() {
        invert = -1;
    }

    public HalfCircleMover(int id) {
        this.id = id;
        invert = -1;
    }

    @Override
    public void setMovement(PointF startPos, PointF endPos, float startTime, float endTime) {
        this.startTime = startTime;
        this.endTime = endTime;

        Vector2f startV = new Vector2f(startPos);
        Vector2f endV = new Vector2f(endPos);

        // StreamTrigger from config - default 100f means stream trigger is active
        float streamTrigger = 100f;
        // RadiusMultiplier from config - default 1.0f
        float radiusMultiplier = 1.0f;

        if (streamTrigger < 0 || (endTime - startTime) < streamTrigger) {
            invert = -1f * invert;
        }

        if (startV.equals(endV)) {
            curve = new LinearCurve(startV, endV);
        } else {
            Vector2f point = startV.mid(endV);
            Vector2f p = point.sub(startV).rotate(invert * (float) Math.PI / 2f).scl(radiusMultiplier).add(point);
            curve = new CirArc(startV, p, endV);
        }
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
        invert = -1;
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
        return false;
    }

    @Override
    public void setMultiPointMovement(PointF[] positions, float[] times, float startTime) {
        // Handled by scheduler with consecutive setMovement calls
    }
}
