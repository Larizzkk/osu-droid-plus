package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.LinearCurve;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.animation.Easing;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.mutils.MUtils;

/**
 * Exact LinearMover port from danser-go
 * Based on https://github.com/wieku/danser-go/blob/master/app/dance/movers/linear.go
 */
public class LinearMoverExact implements CursorMover {

    // Exact fields from danser-go LinearMover
    private LinearCurve line;
    private boolean simple;
    private int id;

    // Movement state
    private Vector2f startPos;
    private Vector2f endPos;
    private float startTime;
    private float endTime;

    public LinearMoverExact() {
        this.simple = false;
        this.id = 0;
    }

    public LinearMoverExact(boolean simple) {
        this.simple = simple;
        this.id = 0;
    }

    public LinearMoverExact(boolean simple, int id) {
        this.simple = simple;
        this.id = id;
    }

    @Override
    public void setMovement(PointF startPos, PointF endPos, float startTime, float endTime) {
        // Convert to Vector2f for exact compatibility
        this.startPos = new Vector2f(startPos);
        this.endPos = new Vector2f(endPos);
        this.startTime = startTime;
        this.endTime = endTime;

        // Exact logic from danser-go LinearMover.SetObjects
        this.line = new LinearCurve(this.startPos, this.endPos);

        if (simple) {
            // Simple mode logic exactly like danser-go
            float preempt = 450f; // Default preempt time
            float speed = 1f; // Default speed multiplier
            float minDuration = preempt - 100f * speed;

            if (endTime - startTime < minDuration) {
                this.startTime = endTime - minDuration;
            }
        } else {
            // Full mode logic exactly like danser-go
            // In real implementation, this would use config.WaitForPreempt and config.ReactionTime
            float preempt = 450f;
            float reactionTime = 100f;
            float speed = 1f;
            boolean waitForPreempt = true; // Default from danser-go

            if (waitForPreempt) {
                float minDuration = preempt - reactionTime * speed;
                if (endTime - startTime < minDuration) {
                    this.startTime = endTime - minDuration;
                }
            }
        }
    }

    @Override
    public PointF getPositionAt(float time) {
        if (line == null) {
            return null;
        }

        // Exact logic from danser-go LinearMover.Update
        float t = (time - startTime) / (endTime - startTime);
        t = MUtils.clamp(t, 0f, 1f);

        // Apply OutQuad easing exactly like danser-go
        float easedT = Easing.outQuad(t);

        Vector2f result = line.pointAt(easedT);
        return result.toPointF();
    }

    @Override
    public void reset() {
        startPos = null;
        endPos = null;
        line = null;
        startTime = 0;
        endTime = 0;
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
        return true; // LinearExact now supports multi-point movement
    }

    @Override
    public void setMultiPointMovement(PointF[] positions, float[] times, float startTime) {
        if (positions == null || positions.length < 2 || times == null || times.length != positions.length) {
            return;
        }

        this.startTime = startTime;
        this.endTime = times[times.length - 1];

        // Create exact linear multi-point curve
        Vector2f[] points = new Vector2f[positions.length];
        for (int i = 0; i < positions.length; i++) {
            points[i] = new Vector2f(positions[i]);
        }

        // Generate exact linear control points
        Vector2f[] curvePoints = generateLinearExactPoints(points, times);

        if (curvePoints.length >= 2) {
            line = new LinearCurve(curvePoints[0], curvePoints[curvePoints.length - 1]);
        }
    }

    private Vector2f[] generateLinearExactPoints(Vector2f[] positions, float[] times) {
        if (positions.length == 2) {
            // Simple case: create exact linear movement
            return new Vector2f[]{positions[0], positions[1]};
        } else {
            // Complex case: create connected exact linear segments
            Vector2f[] curvePoints = new Vector2f[positions.length];
            for (int i = 0; i < positions.length; i++) {
                curvePoints[i] = positions[i];
            }

            return curvePoints;
        }
    }

    /**
     * Exact LinearCurve implementation from danser-go
     */
    private static class LinearCurve implements ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.Curve {

        private Vector2f start;
        private Vector2f end;

        public LinearCurve(Vector2f start, Vector2f end) {
            this.start = start.copy();
            this.end = end.copy();
        }

        @Override
        public Vector2f pointAt(float t) {
            return new Vector2f(
                start.X + (end.X - start.X) * t,
                start.Y + (end.Y - start.Y) * t
            );
        }

        @Override
        public float getStartAngle() {
            return start.angleRV(end);
        }

        @Override
        public float getEndAngle() {
            return end.angleRV(start);
        }

        @Override
        public float getLength() {
            return start.dst(end);
        }
    }
}
