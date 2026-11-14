package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.BaseMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.Bezier;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.mutils.MUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

public class BezierMover extends BaseMover implements SliderAwareMover {

    private Bezier curve;
    private Vector2f pt;
    private float previousSpeed;
    private float invert;

    private float aggressiveness = 60.0f;
    private float sliderAggressiveness = 3.0f;

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
        setMovement(SliderMovementContext.of(startPos, endPos, startTime, endTime));
    }

    @Override
    public void setMovement(SliderMovementContext ctx) {
        this.startTime = ctx.startTime;
        this.endTime = ctx.endTime;
        // Read aggressiveness from settings on each segment so settings take effect immediately.
        // SliderAggressiveness is the danser-go default (3) as a constant — slider settings removed.
        this.aggressiveness = MoverSettings.getBezierAggressiveness();
        this.sliderAggressiveness = 3.0f;

        Vector2f startV = new Vector2f(ctx.startPos);
        Vector2f endV = new Vector2f(ctx.endPos);

        float duration = Math.max(ctx.endTime - ctx.startTime, 1f);
        float dst = startV.dst(endV);

        if (previousSpeed < 0) {
            previousSpeed = dst / duration;
        }

        Vector2f[] points;

        // danser-go checks ILongObject: sliders AND spinners both provide
        // start/end motion angles, so spinner boundaries take the same branches.
        boolean startLong = ctx.startIsSlider || ctx.startIsSpinner;
        boolean endLong = ctx.endIsSlider || ctx.endIsSpinner;

        if (startV.equals(endV)) {
            points = new Vector2f[]{startV, endV};
        } else if (startLong && endLong) {
            Vector2f pt1 = Vector2f.NewVec2fRad(ctx.startAngle, ctx.startDistance * aggressiveness * sliderAggressiveness / 10f).add(startV);
            Vector2f pt2 = Vector2f.NewVec2fRad(ctx.endAngle, ctx.endDistance * aggressiveness * sliderAggressiveness / 10f).add(endV);
            points = new Vector2f[]{startV, pt1, pt2, endV};
        } else if (startLong) {
            Vector2f pt1 = Vector2f.NewVec2fRad(ctx.startAngle, ctx.startDistance * aggressiveness * sliderAggressiveness / 10f).add(startV);
            float angle = endV.angleRV(this.pt);
            if (Float.isNaN(angle)) {
                angle = 0;
            }
            this.pt = Vector2f.NewVec2fRad(angle, previousSpeed * aggressiveness).add(endV);
            points = new Vector2f[]{startV, pt1, this.pt, endV};
        } else if (endLong) {
            float angle = startV.angleRV(this.pt);
            if (Float.isNaN(angle)) {
                angle = 0;
            }
            this.pt = Vector2f.NewVec2fRad(angle, previousSpeed * aggressiveness).add(startV);
            Vector2f pt2 = Vector2f.NewVec2fRad(ctx.endAngle, ctx.endDistance * aggressiveness * sliderAggressiveness / 10f).add(endV);
            points = new Vector2f[]{startV, this.pt, pt2, endV};
        } else {
            float angle = startV.angleRV(pt);
            if (Float.isNaN(angle)) {
                angle = 0;
            }
            pt = Vector2f.NewVec2fRad(angle, previousSpeed * aggressiveness).add(startV);
            points = new Vector2f[]{startV, pt, endV};
        }

        curve = new Bezier(points, false);
        previousSpeed = (dst + 1.0f) / duration;
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
        // NOTE: do NOT reset pt and previousSpeed here — they must persist across segments
        // like danser-go where mover.pt carries over between SetObjects calls.
        // pt is reset to center only on the VERY first segment (init()), not on each reset.
        // Reset only for game restart.
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
    }
}
