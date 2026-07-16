package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.BaseMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.Bezier;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.mutils.MUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

public class AngleOffsetMover extends BaseMover implements SliderAwareMover {

    private Bezier curve;
    private float lastAngle = 0;
    private Vector2f lastPoint = new Vector2f(0, 0);
    private float invert = 1;

    private float angleOffset = 90f;
    private float distanceMult = 0.666f;
    private float streamAngleOffset = 90f;
    private float longJump = -1f;
    private float longJumpMult = 0.7f;
    private boolean longJumpOnEqualPos = false;

    public AngleOffsetMover() {
    }

    public AngleOffsetMover(int id) {
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

        float distance = vStart.dst(vEnd);
        float timeDelta = ctx.endTime - ctx.startTime;

        float scaledDistance = distance * distanceMult;
        float newAngle = angleOffset * (float) Math.PI / 180.0f;

        if (ctx.startTime > 0 && longJump >= 0 && timeDelta > longJump) {
            scaledDistance = timeDelta * longJumpMult;
        }

        Vector2f[] points;

        if (vStart.equals(vEnd)) {
            if (longJumpOnEqualPos) {
                scaledDistance = timeDelta * longJumpMult;
                lastAngle += (float) Math.PI;

                Vector2f pt1;
                if (ctx.startIsSlider) {
                    pt1 = Vector2f.NewVec2fRad(ctx.startAngle, scaledDistance).add(vStart);
                } else {
                    pt1 = Vector2f.NewVec2fRad(lastAngle, scaledDistance).add(vStart);
                }

                if (!ctx.endIsSlider) {
                    float angle = lastAngle - newAngle * invert;
                    Vector2f pt2 = Vector2f.NewVec2fRad(angle, scaledDistance).add(vEnd);
                    lastAngle = angle;
                    points = new Vector2f[]{vStart, pt1, pt2, vEnd};
                } else {
                    Vector2f pt2 = Vector2f.NewVec2fRad(ctx.endAngle, scaledDistance).add(vEnd);
                    points = new Vector2f[]{vStart, pt1, pt2, vEnd};
                }
            } else {
                points = new Vector2f[]{vStart, vEnd};
            }
        } else if (ctx.startIsSlider && ctx.endIsSlider) {
            invert *= -1;

            Vector2f pt1 = Vector2f.NewVec2fRad(ctx.startAngle, scaledDistance).add(vStart);
            Vector2f pt2 = Vector2f.NewVec2fRad(ctx.endAngle, scaledDistance).add(vEnd);

            points = new Vector2f[]{vStart, pt1, pt2, vEnd};
        } else if (ctx.startIsSlider) {
            invert *= -1;
            lastAngle = vStart.angleRV(vEnd) - newAngle * invert;

            Vector2f pt1 = Vector2f.NewVec2fRad(ctx.startAngle, scaledDistance).add(vStart);
            Vector2f pt2 = Vector2f.NewVec2fRad(lastAngle, scaledDistance).add(vEnd);

            points = new Vector2f[]{vStart, pt1, pt2, vEnd};
        } else if (ctx.endIsSlider) {
            lastAngle += (float) Math.PI;

            Vector2f pt1 = Vector2f.NewVec2fRad(lastAngle, scaledDistance).add(vStart);
            Vector2f pt2 = Vector2f.NewVec2fRad(ctx.endAngle, scaledDistance).add(vEnd);

            points = new Vector2f[]{vStart, pt1, pt2, vEnd};
        } else {
            if (Vector2f.angleBetween32(vStart, lastPoint, vEnd) >= angleOffset * (float) Math.PI / 180.0f) {
                invert *= -1;
                newAngle = streamAngleOffset * (float) Math.PI / 180.0f;
            }

            float angle = vStart.angleRV(vEnd) - newAngle * invert;
            Vector2f pt1 = Vector2f.NewVec2fRad(lastAngle + (float) Math.PI, scaledDistance).add(vStart);
            Vector2f pt2 = Vector2f.NewVec2fRad(angle, scaledDistance).add(vEnd);
            lastAngle = angle;
            points = new Vector2f[]{vStart, pt1, pt2, vEnd};
        }

        curve = new Bezier(points, false);
        lastPoint = vStart;
    }

    @Override
    public PointF getPositionAt(float time) {
        if (curve == null) return null;

        float denom = Math.max(endTime - startTime, 1f);
        float t = MUtils.clamp((time - startTime) / denom, 0, 1);
        return curve.pointAt(t).toPointF();
    }

    @Override
    public void reset() {
        curve = null;
        lastAngle = 0;
        invert = 1;
        lastPoint = new Vector2f(0, 0);
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
