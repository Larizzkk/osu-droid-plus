package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.BaseMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.Bezier;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.mutils.MUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

public class AggressiveMover extends BaseMover implements SliderAwareMover {

    private Bezier curve;
    private float lastAngle = 0;

    public AggressiveMover() {
    }

    public AggressiveMover(int id) {
        this.id = id;
    }

    @Override
    public void setMovement(PointF startPos, PointF endPos, float startTime, float endTime) {
        setMovement(SliderMovementContext.of(startPos, endPos, startTime, endTime));
    }

    @Override
    public void setMovement(SliderMovementContext ctx) {
        this.startTime = ctx.startTime;
        this.endTime = ctx.endTime;

        Vector2f vStart = new Vector2f(ctx.startPos);
        Vector2f vEnd = new Vector2f(ctx.endPos);

        float scaledDistance = ctx.endTime - ctx.startTime;

        float newAngle;
        if (ctx.startIsSlider) {
            newAngle = ctx.startAngle;
        } else {
            newAngle = lastAngle + (float) Math.PI;
        }

        Vector2f p1 = Vector2f.NewVec2fRad(newAngle, scaledDistance).add(vStart);

        if (scaledDistance > 1) {
            lastAngle = p1.angleRV(vEnd);
        }

        if (ctx.startIsSlider && ctx.endIsSlider) {
            Vector2f p2 = Vector2f.NewVec2fRad(ctx.endAngle, scaledDistance).add(vEnd);
            curve = new Bezier(new Vector2f[]{vStart, p1, p2, vEnd}, false);
        } else if (ctx.endIsSlider) {
            Vector2f p2 = Vector2f.NewVec2fRad(ctx.endAngle, scaledDistance).add(vEnd);
            curve = new Bezier(new Vector2f[]{vStart, p1, p2, vEnd}, false);
        } else {
            curve = new Bezier(new Vector2f[]{vStart, p1, vEnd}, false);
        }
    }

    @Override
    public PointF getPositionAt(float time) {
        if (curve == null) return null;

        float denom = Math.max(endTime - startTime, 1f);
        float t = (time - startTime) / denom;
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
    }
}
