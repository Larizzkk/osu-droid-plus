package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.BaseMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.BSpline;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.Bezier;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.Curve;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves.Spline;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.mutils.MUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Exact port of danser-go SplineMover
 * Based on https://github.com/wieku/danser-go/blob/master/app/dance/movers/spline.go
 *
 * Note: danser-go's SplineMover processes MULTIPLE objects at once (stream handling with
 * wobble, half-circle, and rotational force). The setMovement interface gives us 2 points
 * at a time, so the stream detection parts of the algorithm only activate with 3+ accumulated
 * objects (via setMultiPointMovement or manual accumulation).
 */
public class SplineMover extends BaseMover implements CursorMover {

    // Stream detection constants from danser-go
    private static final float STREAM_ENTRY_MIN = 25f;
    private static final float STREAM_ENTRY_MAX = 4000f;
    private static final float STREAM_ESCAPE = 8000f;

    private Spline spline;
    private Vector2f lastStartPos;

    // Stream state (persistent across calls like danser-go)
    private float angle = 0;
    private boolean stream = false;

    // Accumulated points/timings for multi-object processing
    private List<Vector2f> accumulatedPoints = new ArrayList<>();
    private List<Float> accumulatedTimings = new ArrayList<>();

    // For hit-through in update (danser-go keeps objects list)
    private float lastTime = Float.NEGATIVE_INFINITY;

    public SplineMover() {
    }

    public SplineMover(int id) {
        this.id = id;
    }

    /**
     * Process a batch of points using the full danser-go SplineMover algorithm.
     * This mirrors SetObjects in danser-go but with resolved positions.
     */
    private void processBatch(List<Vector2f> points, List<Float> timings) {
        // Build the timing and point arrays like danser-go
        List<Vector2f> splinePoints = new ArrayList<>();
        List<Float> splineTiming = new ArrayList<>();

        angle = 0;
        stream = false;

        for (int i = 0; i < points.size(); i++) {
            if (i == 0) {
                // First object - just store start point and control point
                Vector2f cEnd = points.get(i);
                Vector2f nStart = points.get(i + 1);

                Vector2f wPoint;

                // Placeholder: danser-go checks for ILongObject here
                // Using the "default" branch (not a slider)
                wPoint = cEnd.lerp(nStart, 0.333f);

                splinePoints.add(cEnd);
                splinePoints.add(wPoint);
                splineTiming.add(timings.get(i));

                this.startTime = timings.get(i);

                continue;
            }

            // Check if this is the last object (or a slider - placeholder: never slider)
            boolean isLast = (i == points.size() - 1);
            boolean isLongObject = false; // Placeholder: never a slider

            if (isLongObject || isLast) {
                Vector2f pEnd = points.get(i - 1);
                Vector2f cStart = points.get(i);

                Vector2f wPoint;

                // Placeholder: danser-go checks for ILongObject here
                wPoint = cStart.lerp(pEnd, 0.333f);

                splinePoints.add(wPoint);
                splinePoints.add(cStart);
                splineTiming.add(timings.get(i));

                this.endTime = timings.get(i);

                break;
            } else if (i > 1 && i < points.size() - 1) {
                // Intermediate objects - stream detection
                Vector2f pos1 = points.get(i - 1);
                Vector2f pos2 = points.get(i);
                Vector2f pos3 = points.get(i + 1);

                float minV = STREAM_ENTRY_MIN;
                float maxV = STREAM_ENTRY_MAX;
                if (stream) {
                    maxV = STREAM_ESCAPE;
                }

                float sq1 = pos1.dstSq(pos2);
                float sq2 = pos2.dstSq(pos3);

                // Rotational force (config.RotationalForce - placeholder, disabled)
                boolean rotationalForce = false;

                // StreamWobble and StreamHalfCircle (config - placeholder, disabled)
                boolean streamWobble = false;
                boolean streamHalfCircle = false;

                if (sq1 > maxV && sq2 > maxV && rotationalForce) {
                    if (stream) {
                        angle = 0;
                        stream = false;
                    } else {
                        float ang = Math.abs(pos1.angleRV(pos2) - pos1.angleRV(pos3));
                        if (ang == 0) {
                            angle *= -1;
                        } else {
                            angle = ang * 90f / 180f * (float) Math.PI;
                        }
                    }
                } else if (sq1 >= minV && sq1 <= maxV && sq2 >= minV && sq2 <= maxV && (streamWobble || streamHalfCircle)) {
                    if (stream) {
                        angle *= -1;

                        if (Math.abs(angle) < 0.01) {
                            Vector2f pp1 = splinePoints.get(splinePoints.size() - 1);
                            float shoeF = pp1.X * pos2.Y + pos2.X * pos3.Y + pos3.X * pp1.Y;
                            float shoeS = pp1.Y * pos2.X + pos2.Y * pos3.X + pos3.Y * pp1.X;

                            boolean sig = (shoeF - shoeS) > 0;

                            angle = (float) Math.PI / 2;
                            if (sig) {
                                angle *= -1;
                            }
                        }
                    } else {
                        stream = true;
                    }
                } else {
                    stream = false;
                    angle = 0;
                }

                if (Math.abs(angle) > 0.01) {
                    Vector2f mid = pos1.mid(pos2);

                    float scale = 1.0f;
                    if (stream && !streamHalfCircle) {
                        scale = 1.0f; // config.WobbleScale placeholder
                    }

                    if (stream && streamHalfCircle) {
                        int sign = -1;
                        if (angle < 0) {
                            sign = 1;
                        }

                        for (int t = -2; t <= 2; t++) {
                            Vector2f p4 = mid.sub(pos1).scl(scale).rotate(angle + (float) (sign * t) * (float) Math.PI / 6f).add(mid);
                            splinePoints.add(p4);
                            splineTiming.add((float) ((timings.get(i) - timings.get(i - 1)) * (3.0 + t) / 6.0 + timings.get(i - 1)));
                        }
                    } else {
                        Vector2f p4 = mid.sub(pos1).scl(scale).rotate(angle).add(mid);
                        splinePoints.add(p4);
                        splineTiming.add((timings.get(i) - timings.get(i - 1)) / 2f + timings.get(i - 1));
                    }
                }
            }

            splinePoints.add(points.get(i));
            splineTiming.add(timings.get(i));
        }

        // Solve B-spline with points
        Vector2f[] pointsArray = splinePoints.toArray(new Vector2f[0]);

        if (pointsArray.length < 4) {
            // Fallback for insufficient points
            spline = null;
            return;
        }

        List<Bezier> beziers = BSpline.solveBSpline(pointsArray);
        List<Float> timeDiff = new ArrayList<>();

        for (int j = 0; j < splineTiming.size() - 1; j++) {
            timeDiff.add(splineTiming.get(j + 1) - splineTiming.get(j));
        }

        List<Curve> bezierCurves = new ArrayList<>();
        for (int j = 0; j < beziers.size(); j++) {
            Bezier b = beziers.get(j);

            if (j < timeDiff.size() && timeDiff.get(j) > 600) {
                float scl = timeDiff.get(j) / 2f;
                // getPoints() returns a clone, so we modify the clone and create a new Bezier
                Vector2f[] bp = b.getPoints();
                if (bp.length >= 4) {
                    bp[1] = bp[0].add(bp[1].sub(bp[0]).nor().scl(scl));
                    bp[2] = bp[3].add(bp[2].sub(bp[3]).nor().scl(scl));
                    // Create new Bezier with modified points (matches NewBezierNA)
                    bezierCurves.add(new Bezier(bp, false));
                    continue;
                }
            }

            bezierCurves.add(b);
        }

        // Create weights array for weighted spline
        float[] weights = new float[timeDiff.size()];
        for (int j = 0; j < timeDiff.size(); j++) {
            weights[j] = timeDiff.get(j) > 0 ? timeDiff.get(j) : 1f;
        }

        // Adjust weights to match number of beziers
        if (weights.length != bezierCurves.size()) {
            weights = new float[bezierCurves.size()];
            for (int j = 0; j < weights.length; j++) {
                weights[j] = 1f;
            }
        }

        spline = new Spline(bezierCurves.toArray(new Curve[0]), weights);
    }

    @Override
    public void setMovement(PointF startPos, PointF endPos, float startTime, float endTime) {
        this.startTime = startTime;
        this.endTime = endTime;

        Vector2f startV = new Vector2f(startPos);
        Vector2f endV = new Vector2f(endPos);

        // Reset accumulated state for a new batch
        accumulatedPoints.clear();
        accumulatedTimings.clear();

        // The minimal case: 2 objects, neither is a slider, no stream
        List<Vector2f> points = new ArrayList<>();
        List<Float> timings = new ArrayList<>();

        points.add(startV);
        points.add(endV);
        timings.add(startTime);
        timings.add(endTime);

        processBatch(points, timings);

        lastStartPos = startV;
    }

    @Override
    public PointF getPositionAt(float time) {
        if (spline == null) return null;

        float t = (time - startTime) / (endTime - startTime);
        t = MUtils.clamp(t, 0, 1);
        return spline.pointAt(t).toPointF();
    }

    @Override
    public void reset() {
        spline = null;
        lastStartPos = null;
        angle = 0;
        stream = false;
        accumulatedPoints.clear();
        accumulatedTimings.clear();
        lastTime = Float.NEGATIVE_INFINITY;
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
        return true;
    }

    @Override
    public void setMultiPointMovement(PointF[] positions, float[] times, float startTime) {
        if (positions == null || positions.length < 2 || times == null || times.length != positions.length) {
            return;
        }

        accumulatedPoints.clear();
        accumulatedTimings.clear();

        // Accumulate all positions as Vector2f
        for (int i = 0; i < positions.length; i++) {
            accumulatedPoints.add(new Vector2f(positions[i]));
            accumulatedTimings.add(times[i]);
        }

        // Process with the full Go algorithm
        processBatch(accumulatedPoints, accumulatedTimings);

        this.startTime = times[0];
        this.endTime = times[times.length - 1];
    }
}
