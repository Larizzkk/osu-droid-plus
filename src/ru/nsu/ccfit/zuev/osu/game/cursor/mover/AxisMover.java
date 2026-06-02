package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.BaseMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.animation.easing.Easing;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.MultiCurve;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.mutils.MUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

/**
 * Exact port of danser-go AxisAlignedMover
 * Based on https://github.com/wieku/danser-go/blob/master/app/dance/movers/axisaligned.go
 */
public class AxisMover extends BaseMover implements CursorMover {

    private MultiCurve curve;

    public AxisMover() {
    }

    public AxisMover(int id) {
        this.id = id;
    }

    @Override
    public void setMovement(PointF startPos, PointF endPos, float startTime, float endTime) {
        this.startTime = startTime;
        this.endTime = endTime;

        Vector2f startV = new Vector2f(startPos);
        Vector2f endV = new Vector2f(endPos);

        Vector2f midP;

        // EXACT port of the danser-go logic - includes the original bug/behavior:
        // if math32.Abs(endPos.Sub(startPos).X) < math32.Abs(endPos.Sub(endPos).X)
        // endPos.Sub(endPos) is always (0,0), so the condition is always false
        // This means the else branch is ALWAYS taken.
        if (Math.abs(endV.sub(startV).X) < Math.abs(endV.sub(endV).X)) {
            midP = Vector2f.NewVec2f(startV.X, endV.Y);
        } else {
            midP = Vector2f.NewVec2f(endV.X, startV.Y);
        }

        curve = new MultiCurve(new Vector2f[]{startV, midP, endV});
    }

    @Override
    public PointF getPositionAt(float time) {
        if (curve == null) return null;

        float t = (time - startTime) / (endTime - startTime);
        t = MUtils.clamp(t, 0, 1);
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
        return false;
    }

    @Override
    public void setMultiPointMovement(PointF[] positions, float[] times, float startTime) {
        // Axis mover doesn't support multi-point movement
        if (positions != null && positions.length >= 2 && times != null && times.length >= 2) {
            setMovement(positions[0], positions[positions.length - 1], startTime, times[times.length - 1]);
        }
    }
}
