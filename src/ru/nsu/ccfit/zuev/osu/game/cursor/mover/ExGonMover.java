package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.BaseMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.animation.easing.Easing;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

import java.util.Random;

/**
 * Exact port of danser-go ExGonMover
 * Based on https://github.com/wieku/danser-go/blob/master/app/dance/movers/exgon.go
 */
public class ExGonMover extends BaseMover implements CursorMover {

    private boolean wasFirst = false;
    private Random rand;
    private Vector2f endPos;
    private Vector2f lastPos;
    private float nextTime;
    private float delay;

    public ExGonMover() {
        endPos = Vector2f.NewVec2f(0, 0);
        lastPos = Vector2f.NewVec2f(0, 0);
        rand = new Random();
    }

    public ExGonMover(int id) {
        this.id = id;
        endPos = Vector2f.NewVec2f(0, 0);
        lastPos = Vector2f.NewVec2f(0, 0);
        rand = new Random();
    }

    @Override
    public void setMovement(PointF startPos, PointF endPos, float startTime, float endTime) {
        this.startTime = startTime;
        this.endTime = endTime;

        // Config.Delay - default 100f
        float configDelay = 100f;
        this.delay = configDelay;

        if (!wasFirst) {
            // Danser-go: rand.New(rand.NewSource((int64(objs[1].GetStartPosition().X)+1000*int64(objs[1].GetStartPosition().Y))*100 + int64(objs[1].GetStartTime())))
            // We don't have raw start position, so we use endPos as approximation
            long seed = ((long) endPos.x + 1000L * (long) endPos.y) * 100L + (long) endTime;
            this.rand = new Random(seed);
            this.wasFirst = true;
        }

        this.nextTime = startTime + delay;

        // Danser-go: mover.lastPos = start.GetStackedEndPositionMod(mover.diff)
        // Danser-go: mover.endPos = end.GetStackedStartPositionMod(mover.diff)
        this.lastPos = new Vector2f(startPos);
        this.endPos = new Vector2f(endPos);
    }

    @Override
    public PointF getPositionAt(float time) {
        // Danser-go: if mover.endTime-time < mover.delay { return mover.endPos }
        if (endTime - time < delay) {
            return endPos.toPointF();
        }

        // Danser-go: if time >= mover.nextTime { generate new random position }
        if (time >= nextTime) {
            nextTime += delay;

            // Danser-go: NewVec2f(568, 426).Mult(NewVec2f(InOutCubic(rand), InOutCubic(rand))).SubS(28, 21)
            float x = 568f * (float) Easing.inOutCubic(rand.nextDouble()) - 28f;
            float y = 426f * (float) Easing.inOutCubic(rand.nextDouble()) - 21f;

            lastPos = Vector2f.NewVec2f(x, y);
        }

        return lastPos.toPointF();
    }

    @Override
    public void reset() {
        wasFirst = false;
        lastPos = Vector2f.NewVec2f(0, 0);
        endPos = Vector2f.NewVec2f(0, 0);
        rand = new Random();
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

        this.startTime = startTime;
        this.endTime = times[times.length - 1];

        // Danser-go uses first and last objects for ExGon
        Vector2f startV = new Vector2f(positions[0]);
        Vector2f endV = new Vector2f(positions[positions.length - 1]);

        this.lastPos = startV;
        this.endPos = endV;
        this.wasFirst = true;
        this.nextTime = startTime;
        this.delay = 100f;
    }
}
