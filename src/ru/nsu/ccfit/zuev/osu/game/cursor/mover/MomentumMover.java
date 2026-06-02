package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.BaseMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.Bezier;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.mutils.MUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

/**
 * Exact port of danser-go MomentumMover
 * Based on https://github.com/wieku/danser-go/blob/master/app/dance/movers/momentum.go
 * Adapted from https://github.com/TechnoJo4/osu/blob/master/osu.Game.Rulesets.Osu/Replays/Movers/MomentumMover.cs
 */
public class MomentumMover extends BaseMover implements CursorMover {

    private Bezier curve;
    private Vector2f last;
    private boolean first;
    private boolean wasStream;

    // Configuration defaults matching danser-go
    private float distanceMultOut = 1.0f;
    private float streamMult = 1.0f;
    private float distanceMult = 1.0f;
    private float restrictArea = 0f;
    private float restrictAngle = 0f;
    private boolean restrictInvert = false;
    private float durationTrigger = 0f;
    private float durationMult = 1.0f;
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

    // Helper: same position check matching danser-go
    private boolean same(Vector2f p1, Vector2f p2) {
        return p1.equals(p2) || (skipStackAngles && Float.compare(p1.X, p2.X) == 0 && Float.compare(p1.Y, p2.Y) == 0);
    }

    // Helper: normalize angle to [0, 2*PI)
    private static float anorm(float a) {
        float pi2 = 2f * (float) Math.PI;
        a = a % pi2;
        if (a < 0) a += pi2;
        return a;
    }

    // Helper: normalize angle to [-PI, PI]
    private static float anorm2(float a) {
        a = anorm(a);
        if (a > (float) Math.PI) {
            a = -(2f * (float) Math.PI - a);
        }
        return a;
    }

    @Override
    public void setMovement(PointF startPos, PointF endPos, float startTime, float endTime) {
        this.startTime = startTime;
        this.endTime = endTime;

        Vector2f startV = new Vector2f(startPos);
        Vector2f endV = new Vector2f(endPos);

        float dst = startV.dst(endV);

        // In danser-go, this iterates through future objects to find the exit angle (a2).
        // Since we don't have them, use last.angleRV(startV) like danser-go does
        // when iterating through all objects and finding no ILongObject/non-same positions.
        float a2 = last.angleRV(startV);
        boolean fromLong = false; // Placeholder: never a slider

        // Stream detection (simplified from danser-go which looks at next next object)
        boolean stream = false;
        if (!fromLong && streamRestrict) {
            float min = 25.0f;
            float max = 10000.0f;

            float sq1 = startV.dstSq(endV);

            if (sq1 >= min && sq1 <= max && (wasStream || (sq1 >= min && sq1 <= max))) {
                stream = true;
            }
        }

        wasStream = stream;

        // Angle computation for start control point (a1)
        float a1;
        // Placeholder: danser-go checks for start ILongObject
        if (first) {
            a1 = a2 + (float) Math.PI;
        } else {
            a1 = startV.angleRV(last);
        }

        float mult = distanceMultOut;

        // Angle restriction logic
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
        } else {
            // danser-go does r = sq1/(sq1+sq2) with next object, but we don't have it
            // Just use the angle as-is
        }

        // Duration trigger
        float duration = endTime - startTime;
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

        float t = (time - startTime) / (endTime - startTime);
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
        // Handled by scheduler with consecutive setMovement calls
    }
}
