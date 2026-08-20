package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.BaseMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.Bezier;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.mutils.MUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

public class MomentumMover extends BaseMover implements SliderAwareMover {

    private Bezier curve;
    private Vector2f last;
    private boolean first;
    private boolean wasStream;

    // Next-next object position for stream detection (set by AutoCursor)
    private Vector2f nextObjPos = null;
    private boolean nextIsCircle = false;
    private boolean hasFromLong = false;

    private float distanceMultOut = 0.45f;
    private float streamMult = 0.7f;
    private float distanceMult = 0.6f;
    private float restrictArea = 40f;
    private float restrictAngle = 90f;
    private boolean restrictInvert = true;
    private float durationTrigger = 500f;
    private float durationMult = 2.0f;
    private boolean skipStackAngles = false;
    private boolean streamRestrict = true;

    public MomentumMover() {
        last = Vector2f.NewVec2f(0, 0);
        first = true;
        wasStream = false;
    }

    public MomentumMover(int id) {
        this.id = id;
        last = Vector2f.NewVec2f(0, 0);
        first = true;
        wasStream = false;
    }

    private boolean same(Vector2f p1, Vector2f p2) {
        return p1.equals(p2) || (skipStackAngles && Float.compare(p1.X, p2.X) == 0 && Float.compare(p1.Y, p2.Y) == 0);
    }

    private static float anorm(float a) {
        float pi2 = 2f * (float) Math.PI;
        a = a % pi2;
        if (a < 0) a += pi2;
        return a;
    }

    private static float anorm2(float a) {
        a = anorm(a);
        if (a > (float) Math.PI) {
            a = -(2f * (float) Math.PI - a);
        }
        return a;
    }

    @Override
    public void setMovement(PointF startPos, PointF endPos, float startTime, float endTime) {
        setMovement(SliderMovementContext.of(startPos, endPos, startTime, endTime));
    }

    public void setMovementWithNext(SliderMovementContext ctx, PointF nextPosition, boolean nextIsCircle) {
        this.nextObjPos = nextPosition != null ? new Vector2f(nextPosition) : null;
        // In danser-go: fromLong is true when objs[i+2] (the next-next object)
        // is a long object (slider), NOT when the start object is a slider.
        // nextIsCircle from AutoCursor = !nextSeg.endIsSlider
        // So hasFromLong = the next-next object is a slider
        this.hasFromLong = !nextIsCircle && nextPosition != null;
        setMovement(ctx);
    }

    @Override
    public void setMovement(SliderMovementContext ctx) {
        this.startTime = ctx.startTime;
        this.endTime = ctx.endTime;

        Vector2f startV = new Vector2f(ctx.startPos);
        Vector2f endV = new Vector2f(ctx.endPos);

        float dst = startV.dst(endV);

        // danser-go: a2 = last.AngleRV(startPos) when not first
        // then lookahead at objs[i+2] for stream detection and final a2
        float a2 = last.angleRV(startV);
        boolean fromLong = ctx.startIsSlider;

        boolean hasNext = nextObjPos != null && nextIsCircle;
        boolean fromLong2 = hasFromLong;

        // danser-go: sq1 = startPos.DstSq(endPos), sq2 = endPos.DstSq(nextPos)
        boolean stream = false;
        if (hasNext && !fromLong2 && streamRestrict) {
            float min = 25.0f;
            float max = 10000.0f;

            float sq1 = startV.dstSq(endV);
            float sq2 = endV.dstSq(nextObjPos);

            if (sq1 >= min && sq1 <= max && wasStream || (sq2 >= min && sq2 <= max)) {
                stream = true;
            }
        }

        wasStream = stream;

        float a1;
        if (ctx.startIsSlider) {
            a1 = ctx.startAngle;
        } else if (first) {
            a1 = a2 + (float) Math.PI;
        } else {
            a1 = startV.angleRV(last);
        }

        float mult = distanceMultOut;

        float ac = a2 - endV.angleRV(startV);
        float area = restrictArea * (float) Math.PI / 180.0f;

        if (area > 0 && stream && anorm(ac) < anorm((2f * (float) Math.PI) - area)) {
            float a = startV.angleRV(endV);
            float sAngle = 0.5f * (float) Math.PI;
            if (anorm(a1 - a) > (float) Math.PI) {
                a2 = a - sAngle;
            } else {
                a2 = a + sAngle;
            }
            mult = streamMult;
        } else if (!fromLong && area > 0 && Math.abs(anorm2(ac)) < area) {
            float a = endV.angleRV(startV);
            float offset = restrictAngle * (float) Math.PI / 180.0f;
            if ((anorm(a2 - a) < offset) != restrictInvert) {
                a2 = a + offset;
            } else {
                a2 = a - offset;
            }
            mult = distanceMult;
        } else if (hasNext && !fromLong) {
            // danser-go: r = sq1/(sq1+sq2); a = startPos.AngleRV(endPos); a2 = a + r*anorm2(a2-a)
            float sq1 = startV.dstSq(endV);
            float sq2 = endV.dstSq(nextObjPos);
            float r = sq1 / (sq1 + sq2);
            float a = startV.angleRV(endV);
            a2 = a + r * anorm2(a2 - a);
        }

        float duration = ctx.endTime - ctx.startTime;
        if (durationTrigger > 0 && duration >= durationTrigger) {
            mult *= durationMult * (duration / durationTrigger);
        }

        Vector2f p1 = Vector2f.NewVec2fRad(a1, dst * mult).add(startV);
        Vector2f p2 = Vector2f.NewVec2fRad(a2, dst * mult).add(endV);

        if (!same(startV, endV)) {
            last = p2;
            curve = new Bezier(new Vector2f[]{startV, p1, p2, endV}, false);
        } else {
            curve = new Bezier(new Vector2f[]{startV, endV}, false);
        }

        first = false;
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
        last = Vector2f.NewVec2f(0, 0);
        first = true;
        wasStream = false;
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
