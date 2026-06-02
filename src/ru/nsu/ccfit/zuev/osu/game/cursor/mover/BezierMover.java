package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.BaseMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.Bezier;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.mutils.MUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

/**
 * Exact port of danser-go BezierMover
 * Based on https://github.com/wieku/danser-go/blob/master/app/dance/movers/bezier.go
 */
public class BezierMover extends BaseMover implements CursorMover {

    private Bezier curve;
    private Vector2f pt;
    private float previousSpeed;
    private float invert;

    public BezierMover() {
        init();
    }

    public BezierMover(int id) {
        this.id = id;
        init();
    }

    private void init() {
        pt = Vector2f.NewVec2f(512f / 2f, 384f / 2f);
        invert = 1f;
        previousSpeed = -1f;
    }

    @Override
    public void setMovement(PointF startPos, PointF endPos, float startTime, float endTime) {
        this.startTime = startTime;
        this.endTime = endTime;

        Vector2f startV = new Vector2f(startPos);
        Vector2f endV = new Vector2f(endPos);

        float dst = startV.dst(endV);

        if (previousSpeed < 0) {
            previousSpeed = dst / (endTime - startTime);
        }

        // In danser-go, these check start/end for ILongObject (slider) interface.
        // We don't have access to objects, so always false (placeholders).
        boolean ok1 = false;
        boolean ok2 = false;

        float genScale = previousSpeed;
        float aggressiveness = 1.0f;
        float sliderAggressiveness = 1.0f;

        Vector2f[] points;

        if (startV.equals(endV)) {
            points = new Vector2f[]{startV, endV};
        } else if (ok1 && ok2) {
            // Both are sliders (placeholder - never taken)
            float endAngle = 0;
            float startAngle = 0;
            pt = Vector2f.NewVec2fRad(endAngle, dst * aggressiveness * sliderAggressiveness / 10f).add(startV);
            Vector2f pt2 = Vector2f.NewVec2fRad(startAngle, dst * aggressiveness * sliderAggressiveness / 10f).add(endV);
            points = new Vector2f[]{startV, pt, pt2, endV};
        } else if (ok1) {
            // Start is slider (placeholder - never taken)
            float endAngle = 0;
            Vector2f pt1 = Vector2f.NewVec2fRad(endAngle, dst * aggressiveness * sliderAggressiveness / 10f).add(startV);
            pt = Vector2f.NewVec2fRad(endV.angleRV(pt), genScale * aggressiveness).add(endV);
            points = new Vector2f[]{startV, pt1, pt, endV};
        } else if (ok2) {
            // End is slider (placeholder - never taken)
            float startAngle = 0;
            pt = Vector2f.NewVec2fRad(startV.angleRV(pt), genScale * aggressiveness).add(startV);
            Vector2f pt1 = Vector2f.NewVec2fRad(startAngle, dst * aggressiveness * sliderAggressiveness / 10f).add(endV);
            points = new Vector2f[]{startV, pt, pt1, endV};
        } else {
            // Neither is slider
            float angle = startV.angleRV(pt);
            if (Float.isNaN(angle)) {
                angle = 0;
            }
            pt = Vector2f.NewVec2fRad(angle, previousSpeed * aggressiveness).add(startV);
            points = new Vector2f[]{startV, pt, endV};
        }

        curve = new Bezier(points, false);
        previousSpeed = (dst + 1.0f) / (endTime - startTime);
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
        pt = Vector2f.NewVec2f(512f / 2f, 384f / 2f);
        previousSpeed = -1f;
        invert = 1f;
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
