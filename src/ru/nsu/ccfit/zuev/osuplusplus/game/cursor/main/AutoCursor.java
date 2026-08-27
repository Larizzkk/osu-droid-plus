package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.main;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.Config;
import com.rian.osu.beatmap.hitobject.HitObject;
import com.rian.osu.beatmap.hitobject.Slider;
import com.rian.osu.beatmap.hitobject.Spinner;
import ru.nsu.ccfit.zuev.osu.game.GameObject;
import ru.nsu.ccfit.zuev.osu.game.GameObjectListener;
import ru.nsu.ccfit.zuev.osu.game.ISliderListener;
import ru.nsu.ccfit.zuev.osu.game.cursor.AutoplayStyle;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.CursorMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.MomentumMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.MoverFactory;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.SliderAwareMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.SliderMovementContext;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.SplineMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.BaseMover;

import java.util.ArrayList;
import java.util.List;

public class AutoCursor extends CursorEntity implements ISliderListener {

    /**
     * Precomputed movement segment matching danser-go's scheduler approach.
     */
    private static class MovementSegment {
        final int objectId;
        final PointF startPos;
        final PointF endPos;
        final float startTimeMs;
        final float endTimeMs;
        final boolean startIsSlider;
        final float startSliderEndAngle;
        final boolean endIsSlider;
        final boolean endIsSpinner;
        final float endSliderStartAngle;
        final float startDistance;
        final float endDistance;

        MovementSegment(int objectId, PointF startPos, PointF endPos,
                        float startTimeMs, float endTimeMs,
                        boolean startIsSlider, float startSliderEndAngle,
                        boolean endIsSlider, boolean endIsSpinner, float endSliderStartAngle,
                        float startDistance, float endDistance) {
            this.objectId = objectId;
            this.startPos = startPos;
            this.endPos = endPos;
            this.startTimeMs = startTimeMs;
            this.endTimeMs = endTimeMs;
            this.startIsSlider = startIsSlider;
            this.startSliderEndAngle = startSliderEndAngle;
            this.endIsSlider = endIsSlider;
            this.endIsSpinner = endIsSpinner;
            this.endSliderStartAngle = endSliderStartAngle;
            this.startDistance = startDistance;
            this.endDistance = endDistance;
        }
    }

    private CursorMover currentMover;
    private AutoplayStyle currentStyle;
    private float gameTimeMs = 0;
    private boolean initialized = false;
    private GameObjectListener cursorListener;

    // Precomputed queue (danser-go scheduler approach)
    private final List<MovementSegment> segmentQueue = new ArrayList<>();
    private int currentSegmentIndex = -1;
    private boolean queueActive = false;

    // Slider tracking state
    private boolean followingSlider = false;
    private boolean justFinishedSlider = false;

    public AutoCursor() {
        super();
        this.setPosition(100f, 100f);
        this.setShowing(true);
        loadAutoplayStyle();
    }

    private void loadAutoplayStyle() {
        String styleValue = Config.getString("autoplayStyle", "linear");
        currentStyle = AutoplayStyle.fromValue(styleValue);
        currentMover = MoverFactory.createMover(currentStyle);
    }

    public void setDifficulty(float preemptMs, float speed, float circleRadius) {
        if (currentMover instanceof BaseMover baseMover) {
            baseMover.setDifficulty(preemptMs, speed, circleRadius);
        }
    }

    /**
     * Initialize the precomputed movement queue from HitObject data.
     * Mirrors danser-go's GenericScheduler.Init().
     *
     * KEY RULE from danser-go linear.go SetObjects():
     *   startTime = previousObject.GetEndTime()
     *   endTime   = nextObject.GetStartTime()
     *
     * For sliders, GetEndTime() = hitTime + duration.
     * For circles, GetEndTime() = hitTime.
     */
    public void initQueue(HitObject[] hitObjects, GameObjectListener listener) {
        this.cursorListener = listener;
        segmentQueue.clear();
        currentSegmentIndex = -1;
        queueActive = false;

        if (hitObjects == null || hitObjects.length == 0) return;

        // Extract positions and timing for each object
        int count = hitObjects.length;
        float[] posX = new float[count];
        float[] posY = new float[count];
        float[] endPosX = new float[count]; // slider tail / circle head (same for circles)
        float[] endPosY = new float[count];
        float[] hitTimes = new float[count];  // GetStartTime()
        float[] endTimes = new float[count];  // GetEndTime()
        boolean[] isSlider = new boolean[count];
        boolean[] isSpinner = new boolean[count];
        float[] startAngles = new float[count];
        float[] endAngles = new float[count];

        for (int i = 0; i < count; i++) {
            HitObject ho = hitObjects[i];
            var pos = ho.getScreenSpaceGameplayStackedPosition();
            posX[i] = pos.x;
            posY[i] = pos.y;
            // endPosition = slider tail (where ball ends) or same as head for circles
            var endPos = ho.getScreenSpaceGameplayStackedEndPosition();
            endPosX[i] = endPos.x;
            endPosY[i] = endPos.y;
            hitTimes[i] = (float) ho.startTime;
            endTimes[i] = (float) ho.getEndTime();
            isSlider[i] = ho instanceof Slider;
            isSpinner[i] = ho instanceof Spinner;

            if (isSlider[i]) {
                Slider s = (Slider) ho;
                var path = s.getPath().getCalculatedPath();
                if (path.size() >= 2) {
                    float dx0 = (float)(path.get(1).x - path.get(0).x);
                    float dy0 = (float)(path.get(1).y - path.get(0).y);
                    startAngles[i] = (float) Math.atan2(dy0, dx0);
                    int last = path.size() - 1;
                    float dx1 = (float)(path.get(last).x - path.get(last - 1).x);
                    float dy1 = (float)(path.get(last).y - path.get(last - 1).y);
                    endAngles[i] = (float) Math.atan2(dy1, dx1);
                }
            }
        }

        // === PREPROCESSING (matching danser-go GenericScheduler.Init) ===

        // 1. Double-click merging: merge overlapping circles within 1.995r + 3ms
        //    into a single DummyCircle (danser generic.go lines 56-80)
        float circleRadius = hitObjects.length > 0 ?
            (float) hitObjects[0].getScreenSpaceGameplayRadius() : 64f;
        float mergeThreshold = circleRadius * 1.995f;
        float mergeTimeThreshold = 3f; // ms

        boolean[] merged = new boolean[count];
        List<Float> mPosX = new ArrayList<>();
        List<Float> mPosY = new ArrayList<>();
        List<Float> mEndPosX = new ArrayList<>();
        List<Float> mEndPosY = new ArrayList<>();
        List<Float> mHitTimes = new ArrayList<>();
        List<Float> mEndTimes = new ArrayList<>();
        List<Boolean> mIsSlider = new ArrayList<>();
        List<Boolean> mIsSpinner = new ArrayList<>();
        List<Float> mStartAngles = new ArrayList<>();
        List<Float> mEndAngles = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            if (merged[i]) continue;
            if (isSpinner[i]) {
                // Spinners can't be merged
                mPosX.add(posX[i]); mPosY.add(posY[i]);
                mEndPosX.add(endPosX[i]); mEndPosY.add(endPosY[i]);
                mHitTimes.add(hitTimes[i]); mEndTimes.add(endTimes[i]);
                mIsSlider.add(false); mIsSpinner.add(true);
                mStartAngles.add(0f); mEndAngles.add(0f);
                continue;
            }

            // Check if next circle is a double-click candidate
            if (i + 1 < count && !isSlider[i] && !isSlider[i + 1]
                    && !isSpinner[i + 1] && !merged[i + 1]) {
                float dx = posX[i + 1] - posX[i];
                float dy = posY[i + 1] - posY[i];
                float dst = (float) Math.sqrt(dx * dx + dy * dy);
                float timeDiff = hitTimes[i + 1] - endTimes[i];

                if (dst <= mergeThreshold && timeDiff <= mergeTimeThreshold) {
                    // Merge: midpoint position, averaged time
                    float midX = (posX[i] + posX[i + 1]) / 2f;
                    float midY = (posY[i] + posY[i + 1]) / 2f;
                    float avgTime = (hitTimes[i] + hitTimes[i + 1]) / 2f;

                    mPosX.add(midX); mPosY.add(midY);
                    mEndPosX.add(midX); mEndPosY.add(midY);
                    mHitTimes.add(avgTime); mEndTimes.add(avgTime);
                    mIsSlider.add(false); mIsSpinner.add(false);
                    mStartAngles.add(0f); mEndAngles.add(0f);
                    merged[i] = true;
                    merged[i + 1] = true;
                    continue;
                }
            }

            // Normal circle
            mPosX.add(posX[i]); mPosY.add(posY[i]);
            mEndPosX.add(endPosX[i]); mEndPosY.add(endPosY[i]);
            mHitTimes.add(hitTimes[i]); mEndTimes.add(endTimes[i]);
            mIsSlider.add(isSlider[i]); mIsSpinner.add(isSpinner[i]);
            mStartAngles.add(startAngles[i]); mEndAngles.add(endAngles[i]);
        }

        // 2. Timing spread: push overlapping circles 1ms apart
        //    (danser generic.go lines 83-92)
        for (int i = 0; i < mHitTimes.size() - 1; i++) {
            float curEnd = mEndTimes.get(i);
            for (int j = i + 1; j < mHitTimes.size(); j++) {
                if (curEnd < mHitTimes.get(j)) break;
                // Overlapping: push start time 1ms past current end
                if (!mIsSlider.get(j)) {
                    mHitTimes.set(j, curEnd + 1f);
                }
            }
        }

        // Use preprocessed arrays for segment building
        int mCount = mPosX.size();
        if (mCount == 0) {
            queueActive = false;
            return;
        }

        // Dummy initial segment: (100,100) at t=-500 → first object
        segmentQueue.add(new MovementSegment(
            -1,
            new PointF(100f, 100f),
            new PointF(mPosX.get(0), mPosY.get(0)),
            -500f, mHitTimes.get(0),
            false, 0f,
            mIsSlider.get(0), mIsSpinner.get(0), mStartAngles.get(0),
            0f, 0f
        ));

        // Build segments for each consecutive pair from preprocessed data
        for (int i = 0; i < mCount - 1; i++) {
            PointF startPos = new PointF(mEndPosX.get(i), mEndPosY.get(i));
            PointF endPos = new PointF(mPosX.get(i + 1), mPosY.get(i + 1));

            float dx = endPos.x - startPos.x;
            float dy = endPos.y - startPos.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            float segStart = mEndTimes.get(i);
            float segEnd = mHitTimes.get(i + 1);

            // Ensure minimum segment duration for overlapping objects
            if (segStart >= segEnd) {
                segEnd = segStart + 85f;
            }

            segmentQueue.add(new MovementSegment(
                i + 1,
                startPos, endPos,
                segStart, segEnd,
                mIsSlider.get(i), mEndAngles.get(i),
                mIsSlider.get(i + 1), mIsSpinner.get(i + 1), mStartAngles.get(i + 1),
                dist, dist
            ));
        }

        queueActive = true;
        advanceToSegment(0);
    }

    /**
     * Configure the mover for a specific segment, like danser-go's SetObjects.
     */
    private void advanceToSegment(int index) {
        if (index < 0 || index >= segmentQueue.size()) return;

        MovementSegment seg = segmentQueue.get(index);
        currentSegmentIndex = index;

        PointF startPos = seg.startPos;
        PointF endPos = seg.endPos;
        float startTimeMs = seg.startTimeMs;
        float endTimeMs = seg.endTimeMs;

        if (currentMover instanceof SliderAwareMover sliderMover) {
            SliderMovementContext ctx = SliderMovementContext.builder()
                    .startPos(startPos)
                    .endPos(endPos)
                    .startTime(startTimeMs)
                    .endTime(endTimeMs)
                    .startIsSlider(seg.startIsSlider)
                    .startAngle(seg.startSliderEndAngle)
                    .endIsSlider(seg.endIsSlider)
                    .endAngle(seg.endSliderStartAngle)
                    .startDistance(seg.startDistance)
                    .endDistance(seg.endDistance)
                    .build();

            if (currentMover instanceof MomentumMover momentumMover) {
                PointF nextObjPos = null;
                boolean nextIsCircle = false;
                if (index + 1 < segmentQueue.size()) {
                    MovementSegment nextSeg = segmentQueue.get(index + 1);
                    nextObjPos = nextSeg.endPos;
                    // In danser-go: hasNext requires objs[i+2].(*objects.Circle)
                    // So nextIsCircle = NOT slider AND NOT spinner
                    nextIsCircle = !nextSeg.endIsSlider && !nextSeg.endIsSpinner;
                }
                // Build upcoming segments list for the lookahead traversal loop.
                // danser traverses objs[i+2], objs[i+3], ... looking for ILongObject or non-stacked pair.
                // We pass segments from index+1 onwards (end positions + isLong flags).
                java.util.List<PointF> upcomingPositions = new java.util.ArrayList<>();
                boolean[] upcomingIsLong = null;
                if (index + 1 < segmentQueue.size()) {
                    int upcomingCount = segmentQueue.size() - (index + 1);
                    upcomingIsLong = new boolean[upcomingCount];
                    for (int u = index + 1; u < segmentQueue.size(); u++) {
                        MovementSegment uSeg = segmentQueue.get(u);
                        upcomingPositions.add(uSeg.endPos);
                        upcomingIsLong[u - (index + 1)] = uSeg.endIsSlider || uSeg.endIsSpinner;
                    }
                }
                momentumMover.setMovementWithNext(ctx, nextObjPos, nextIsCircle, upcomingPositions, upcomingIsLong);
            } else {
                sliderMover.setMovement(ctx);
            }
        } else {
            currentMover.setMovement(startPos, endPos, startTimeMs, endTimeMs);
        }

        // Multi-point support for SplineMover
        if (currentMover.supportsMultiPoint() && index + 1 < segmentQueue.size()) {
            int count = Math.min(segmentQueue.size() - index, 20);
            PointF[] positions = new PointF[count];
            float[] times = new float[count];
            for (int i = 0; i < count; i++) {
                MovementSegment s = segmentQueue.get(index + i);
                positions[i] = new PointF(s.endPos.x, s.endPos.y);
                times[i] = s.endTimeMs;
            }
            if (currentMover instanceof SplineMover splineMover) {
                boolean firstIsSlider = seg.startIsSlider;
                float firstAngle = firstIsSlider ? seg.startSliderEndAngle : 0f;
                MovementSegment lastSeg = segmentQueue.get(index + count - 1);
                boolean lastIsSlider = lastSeg.endIsSlider;
                float lastAngle = lastIsSlider ? lastSeg.endSliderStartAngle : 0f;
                splineMover.setMultiPointWithMetadata(positions, times,
                        firstIsSlider, firstAngle,
                        lastIsSlider, lastAngle);
            } else {
                currentMover.setMultiPointMovement(positions, times, startTimeMs);
            }
        }
    }

    /**
     * Update cursor position.
     *
     * Frame order in GameScene:
     *   1. updateMovement()  ← cursor is positioned here
     *   2. updatePassiveObjects()  ← GameplaySlider.update() → followSlider()
     *   3. updateActiveObjects()
     *
     * During slider tracking:
     *   - followSlider() is called by GameplaySlider, setting followingSlider=true
     *   - updateMovement() must still ADVANCE segments (danser-go does this),
     *     but NOT move the cursor (followSlider owns the position)
     *   - This ensures that when the slider ends, the mover is already set up
     *     for the slider→circle transition
     *
     * After slider ends:
     *   - onSliderEnd() sets followingSlider=false, justFinishedSlider=true
     *   - Next frame: justFinishedSlider processed
     *   - Frame after: mover takes over seamlessly (segment already set up)
     */
    public void updateMovement(float deltaTimeSeconds, float gameTimeSeconds) {
        if (currentMover == null) return;

        gameTimeMs = gameTimeSeconds * 1000f;

        // === SEGMENT ADVANCEMENT (always runs, even during slider tracking) ===
        // This matches danser-go: the scheduler always processes the queue,
        // regardless of slider tracking state.
        if (queueActive && !segmentQueue.isEmpty()) {
            // While loop to catch up multiple segments (e.g. after long slider)
            while (currentSegmentIndex >= 0 && currentSegmentIndex < segmentQueue.size()) {
                MovementSegment current = segmentQueue.get(currentSegmentIndex);
                if (gameTimeMs > current.endTimeMs && currentSegmentIndex + 1 < segmentQueue.size()) {
                    advanceToSegment(currentSegmentIndex + 1);
                } else {
                    break;
                }
            }
        }

        // === SLIDER TRACKING: hold position while riding the ball ===
        if (followingSlider && !justFinishedSlider) {
            // Cursor position is owned by followSlider(). Don't move it.
            // But segment advancement above has already been done.
            if (cursorListener != null) {
                cursorListener.onUpdatedAutoCursor(getX(), getY());
            }
            return;
        }

        // === JUST FINISHED SLIDER: one frame grace period ===
        if (justFinishedSlider) {
            justFinishedSlider = false;
            // Fall through to mover sampling below — the segment is already
            // set up for slider→circle from the advancement above.
        }

        // === NORMAL CURSOR POSITIONING via mover ===
        if (queueActive && !segmentQueue.isEmpty()) {
            PointF moverPos = currentMover.getPositionAt(gameTimeMs);
            if (moverPos != null) {
                setPosition(moverPos.x, moverPos.y);
            } else if (currentSegmentIndex >= 0 && currentSegmentIndex < segmentQueue.size()) {
                MovementSegment seg = segmentQueue.get(currentSegmentIndex);
                setPosition(seg.endPos.x, seg.endPos.y);
            }
        } else {
            PointF moverPos = currentMover.getPositionAt(gameTimeMs);
            if (moverPos != null) {
                setPosition(moverPos.x, moverPos.y);
            }
        }

        if (cursorListener != null) {
            cursorListener.onUpdatedAutoCursor(getX(), getY());
        }
    }

    public void setPosition(float pX, float pY, GameObjectListener listener) {
        setPosition(pX, pY);
        listener.onUpdatedAutoCursor(pX, pY);
    }

    /**
     * Legacy reactive path — ignored when precomputed queue is active.
     */
    public void moveToObject(GameObject object, float secPassed, GameObjectListener listener) {
        moveToObject(object, secPassed, listener, null);
    }

    /**
     * Legacy reactive path — ignored when precomputed queue is active.
     * Falls back to reactive mode when queue is not initialized (e.g. during seek).
     */
    public void moveToObject(GameObject object, float secPassed, GameObjectListener listener,
                             List<GameObject> activeObjects) {
        if (object == null) {
            followingSlider = false;
            return;
        }
        if (queueActive) return;

        cursorListener = listener;
        gameTimeMs = secPassed * 1000;
        initialized = false;
        currentMover.reset();
    }

    public void setAutoplayStyle(AutoplayStyle style) {
        this.currentStyle = style;
        this.currentMover = MoverFactory.createMover(currentStyle);
        this.initialized = false;
        this.gameTimeMs = 0;
        this.queueActive = false;
        this.segmentQueue.clear();
        this.currentSegmentIndex = -1;
    }

    public AutoplayStyle getAutoplayStyle() {
        return currentStyle;
    }

    public void reset() {
        gameTimeMs = 0;
        initialized = false;
        followingSlider = false;
        justFinishedSlider = false;
        cursorListener = null;
        queueActive = false;
        segmentQueue.clear();
        currentSegmentIndex = -1;
        if (currentMover != null) currentMover.reset();
        this.setPosition(100f, 100f);
    }

    /**
     * Called by GameplaySlider while a slider ball is being tracked.
     * The cursor rides the ball: position is owned here.
     */
    public void followSlider(float x, float y) {
        followingSlider = true;
        setPosition(x, y);
        if (cursorListener != null) {
            cursorListener.onUpdatedAutoCursor(x, y);
        }
    }

    @Override
    public void onSliderStart() {
        cursorSprite.onSliderStart();
    }

    @Override
    public void onSliderTracking() {
        cursorSprite.onSliderTracking();
    }

    @Override
    public void onSliderEnd() {
        justFinishedSlider = true;
        followingSlider = false;
        cursorSprite.onSliderEnd();
    }
}
