package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.BaseMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.animation.easing.Easing;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.Curve;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.MultiCurve;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.mutils.MUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Exact port of danser-go PippiMover
 * Based on https://github.com/wieku/danser-go/blob/master/app/dance/movers/pippi.go
 */
public class PippiMover extends BaseMover implements CursorMover {

    private static final float SIXTY_TIME = 1000f / 60f;
    private Curve curve;

    // Wobble configuration (defaults matching danser-go config)
    private float rotationSpeed = 1.0f;
    private float radiusMultiplier = 1.0f;
    private float spinnerRadius = 50f;
    private float circleRadius = 64f; // Default circle radius

    // Preempt from difficulty (defaults)
    private float preempt = 1200f;
    private float speed = 1.0f;

    public PippiMover() {
    }

    public PippiMover(int id) {
        this.id = id;
    }

    private Vector2f modifyPos(float time, boolean spinner, Vector2f pos) {
        // rad = math.Mod(time/1000 * config.RotationSpeed, 1) * 2 * math.Pi
        double rad = ((time / 1000.0 * rotationSpeed) % 1.0) * 2.0 * Math.PI;

        float radius;
        if (spinner) {
            radius = spinnerRadius;
        } else {
            radius = circleRadius * MUtils.clamp(radiusMultiplier, 0, 1);
        }

        Vector2f mVec = Vector2f.NewVec2fRad((float) rad, radius);
        return pos.add(mVec);
    }

    @Override
    public void setMovement(PointF startPos, PointF endPos, float startTime, float endTime) {
        // Danser-go: mover.startTime = max(start.GetEndTime(), end.GetStartTime()-(preempt-100*speed))
        // This makes the cursor start BEFORE the current object ends, for smooth transition
        float adjustedStartTime = Math.max(startTime, endTime - (preempt - 100f * speed));

        this.startTime = adjustedStartTime;
        this.endTime = endTime;

        Vector2f startV = new Vector2f(startPos);
        Vector2f endV = new Vector2f(endPos);

        float timeDifference = endTime - adjustedStartTime;

        // Danser-go pre-allocates: points = make([]vector.Vector2f, 0, int(math.Ceil(timeDifference/sixtyTime)))
        List<Vector2f> points = new ArrayList<>((int) Math.ceil(timeDifference / SIXTY_TIME) + 2);

        // First point
        // In danser-go: if cOk && startC.DoubleClick { points add startPos } else { points add modifyPos }
        // Placeholder: DoubleClick = false for both
        boolean startDoubleClick = false;
        boolean endDoubleClick = false;
        boolean startIsSpinner = false;
        boolean endIsSpinner = false;

        if (startDoubleClick) {
            points.add(startV);
        } else {
            points.add(modifyPos(startTime, startIsSpinner, startV));
        }

        // Intermediate points every 60fps frame
        for (float t = SIXTY_TIME; t < timeDifference; t += SIXTY_TIME) {
            float f = t / timeDifference;

            Vector2f basePos = startV.lerp(endV, f);

            if (startDoubleClick) {
                basePos = startV.lerp(basePos, f);
            }

            if (endDoubleClick) {
                basePos = basePos.lerp(endV, f);
            }

            basePos = modifyPos(adjustedStartTime + t, false, basePos);

            points.add(basePos);
        }

        // Last point
        if (endDoubleClick) {
            points.add(endV);
        } else {
            points.add(modifyPos(endTime, endIsSpinner, endV));
        }

        // Danser-go creates MultiCurve with a single CLine segment containing all points
        Vector2f[] pointsArray = points.toArray(new Vector2f[0]);
        curve = new MultiCurve(pointsArray);
    }

    @Override
    public PointF getPositionAt(float time) {
        if (curve == null) return null;

        // Danser-go uses OutQuad easing on t
        float t = (time - startTime) / (endTime - startTime);
        t = MUtils.clamp(t, 0, 1);
        return curve.pointAt((float) Easing.outQuad(t)).toPointF();
    }

    @Override
    public PointF getObjectsPosition(float time, PointF objectPos) {
        // Danser-go PippiMover overrides GetObjectsPosition to return wobble positions
        // during object hit windows instead of snapping to the object
        Vector2f pos = new Vector2f(objectPos);
        return modifyPos(time, false, pos).toPointF();
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
    public boolean supportsMultiPoint() {
        return false;
    }

    @Override
    public void setMultiPointMovement(PointF[] positions, float[] times, float startTime) {
        // Handled by scheduler with consecutive setMovement calls
    }
}
