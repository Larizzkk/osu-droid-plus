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

public class SplineMover extends BaseMover implements SliderAwareMover {

    private static final float STREAM_ENTRY_MIN = 25f;
    private static final float STREAM_ENTRY_MAX = 4000f;
    private static final float STREAM_ESCAPE = 8000f;

    private Spline spline;
    private Vector2f lastStartPos;

    private float angle = 0;
    private boolean stream = false;

    private boolean rotationalForce = false;
    private boolean streamWobble = false;
    private boolean streamHalfCircle = false;
    private float wobbleScale = 1.0f;

    private List<Vector2f> accumulatedPoints = new ArrayList<>();
    private List<Float> accumulatedTimings = new ArrayList<>();

    private float lastTime = Float.NEGATIVE_INFINITY;

    public SplineMover() {
    }

    public SplineMover(int id) {
        this.id = id;
    }

    private void processBatch(List<Vector2f> points, List<Float> timings,
                              boolean firstIsSlider, float firstAngle,
                              boolean lastIsSlider, float lastAngle) {
        List<Vector2f> splinePoints = new ArrayList<>();
        List<Float> splineTiming = new ArrayList<>();

        angle = 0;
        stream = false;

        for (int i = 0; i < points.size(); i++) {
            if (i == 0) {
                Vector2f cEnd = points.get(i);
                Vector2f nStart = points.get(i + 1);

                Vector2f wPoint;
                if (firstIsSlider) {
                    wPoint = cEnd.add(Vector2f.NewVec2fRad(firstAngle, cEnd.dst(nStart) * 0.7f));
                } else {
                    wPoint = cEnd.lerp(nStart, 0.333f);
                }

                splinePoints.add(cEnd);
                splinePoints.add(wPoint);
                splineTiming.add(timings.get(i));

                this.startTime = timings.get(i);

                continue;
            }

            boolean isLast = (i == points.size() - 1);
            boolean isLongObject = lastIsSlider && isLast;

            if (isLongObject || isLast) {
                Vector2f pEnd = points.get(i - 1);
                Vector2f cStart = points.get(i);

                Vector2f wPoint;
                if (isLongObject) {
                    wPoint = cStart.add(Vector2f.NewVec2fRad(lastAngle, cStart.dst(pEnd) * 0.7f));
                } else {
                    wPoint = cStart.lerp(pEnd, 0.333f);
                }

                splinePoints.add(wPoint);
                splinePoints.add(cStart);
                splineTiming.add(timings.get(i));

                this.endTime = timings.get(i);

                break;
            } else if (i > 1 && i < points.size() - 1) {
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
                        scale = wobbleScale;
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

        Vector2f[] pointsArray = splinePoints.toArray(new Vector2f[0]);

        if (pointsArray.length < 4) {
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
                Vector2f[] bp = b.getPoints();
                if (bp.length >= 4) {
                    bp[1] = bp[0].add(bp[1].sub(bp[0]).nor().scl(scl));
                    bp[2] = bp[3].add(bp[2].sub(bp[3]).nor().scl(scl));
                    bezierCurves.add(new Bezier(bp, false));
                    continue;
                }
            }

            bezierCurves.add(b);
        }

        float[] weights = new float[timeDiff.size()];
        for (int j = 0; j < timeDiff.size(); j++) {
            weights[j] = timeDiff.get(j) > 0 ? timeDiff.get(j) : 1f;
        }

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
        setMovement(SliderMovementContext.of(startPos, endPos, startTime, endTime));
    }

    @Override
    public void setMovement(SliderMovementContext ctx) {
        this.startTime = ctx.startTime;
        this.endTime = ctx.endTime;

        Vector2f startV = new Vector2f(ctx.startPos);
        Vector2f endV = new Vector2f(ctx.endPos);

        accumulatedPoints.clear();
        accumulatedTimings.clear();

        List<Vector2f> points = new ArrayList<>();
        List<Float> timings = new ArrayList<>();

        points.add(startV);
        points.add(endV);
        timings.add(ctx.startTime);
        timings.add(ctx.endTime);

        processBatch(points, timings,
                ctx.startIsSlider, ctx.startAngle,
                ctx.endIsSlider, ctx.endAngle);

        lastStartPos = startV;
    }

    @Override
    public PointF getPositionAt(float time) {
        if (spline == null) return null;

        float denom = Math.max(endTime - startTime, 1f);
        float t = (time - startTime) / denom;
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

        for (int i = 0; i < positions.length; i++) {
            accumulatedPoints.add(new Vector2f(positions[i]));
            accumulatedTimings.add(times[i]);
        }

        processBatch(accumulatedPoints, accumulatedTimings,
                false, 0f, false, 0f);

        this.startTime = times[0];
        this.endTime = times[times.length - 1];
    }
}
