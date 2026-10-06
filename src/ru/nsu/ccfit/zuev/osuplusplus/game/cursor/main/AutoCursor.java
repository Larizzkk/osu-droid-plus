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
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.MoverSettings;
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
        final boolean startIsSpinner;
        final float startSliderEndAngle;
        final boolean endIsSlider;
        final boolean endIsSpinner;
        final float endSliderStartAngle;
        final float startDistance;
        final float endDistance;
        /**
         * Gameplay time (ms) at which the spinner that ENDS this segment stops owning
         * the cursor (= the spinner's end time). danser-go semantics: a DanceSpinner
         * owns the cursor only inside its [start, end] window; afterwards the regular
         * mover takes over, and when no segment follows (final spinner of the map)
         * the queue is empty and the cursor simply STOPS at the spin exit point.
         * 0 for segments that do not end at a spinner.
         */
        final float endSpinnerEndTimeMs;

        MovementSegment(int objectId, PointF startPos, PointF endPos,
                        float startTimeMs, float endTimeMs,
                        boolean startIsSlider, boolean startIsSpinner, float startSliderEndAngle,
                        boolean endIsSlider, boolean endIsSpinner, float endSliderStartAngle,
                        float startDistance, float endDistance, float endSpinnerEndTimeMs) {
            this.objectId = objectId;
            this.startPos = startPos;
            this.endPos = endPos;
            this.startTimeMs = startTimeMs;
            this.endTimeMs = endTimeMs;
            this.startIsSlider = startIsSlider;
            this.startIsSpinner = startIsSpinner;
            this.startSliderEndAngle = startSliderEndAngle;
            this.endIsSlider = endIsSlider;
            this.endIsSpinner = endIsSpinner;
            this.endSliderStartAngle = endSliderStartAngle;
            this.startDistance = startDistance;
            this.endDistance = endDistance;
            this.endSpinnerEndTimeMs = endSpinnerEndTimeMs;
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

    // Spinner state
    private boolean spinning = false;
    private float spinnerStartTimeMs = 0f;
    /**
     * Gameplay time (ms) at which the spinner being spun ends. danser-go semantics:
     * a DanceSpinner owns the cursor only inside [start, end]; outside that window
     * the regular mover owns the cursor (break movement). The end of the spinner
     * that terminates the current segment equals the start time of the following
     * segment (initQueue: segment.start = prevObject.GetEndTime()); with no
     * following segment (final spinner) the spin lasts until the scene ends.
     */
    private float spinEndTimeMs = Float.MAX_VALUE;
    private float circleRadius = 64f;

    /**
     * danser-go (app/dance/spinners/mover.go): rpms = 0.00795. The spin position is
     * evaluated as a pure function of gameplay time via the selected SpinnerMover
     * (circle/heart/triangle/square/cube — see SpinnerMoverFactory), never integrated
     * from frame deltas, so rate mods and seeks stay consistent with the baked
     * segment endpoints.
     */
    private ru.nsu.ccfit.zuev.osu.game.cursor.mover.SpinnerMoverFactory.SpinnerMover currentSpinnerMover =
        ru.nsu.ccfit.zuev.osu.game.cursor.mover.SpinnerMoverFactory.getMoverByName(
            ru.nsu.ccfit.zuev.osu.game.cursor.mover.MoverSettings.getSpinnerMover());

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
        this.circleRadius = circleRadius;
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
        resetTransientState();
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
        float[] sliderVelAtEnd = new float[count];   // velocity at slider end (for BezierMover control points)
        float[] sliderVelAtStart = new float[count];  // velocity at slider start

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
                    sliderVelAtStart[i] = (float) Math.sqrt(dx0 * dx0 + dy0 * dy0);
                    int last = path.size() - 1;
                    float dx1 = (float)(path.get(last).x - path.get(last - 1).x);
                    float dy1 = (float)(path.get(last).y - path.get(last - 1).y);
                    endAngles[i] = (float) Math.atan2(dy1, dx1);
                    sliderVelAtEnd[i] = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);
                }
                // danser-go: s1.GetStackedPositionAtMod(startTime-10).Dst(startPos)
                // This is the slider's velocity * 10ms — approximate with path segment length
                // For proper velocity, compute total path length / duration * 10
                float duration = endTimes[i] - hitTimes[i];
                if (duration > 0) {
                    float totalLen = 0;
                    for (int j = 1; j < path.size(); j++) {
                        float ddx = (float)(path.get(j).x - path.get(j-1).x);
                        float ddy = (float)(path.get(j).y - path.get(j-1).y);
                        totalLen += (float) Math.sqrt(ddx * ddx + ddy * ddy);
                    }
                    // velocity * 10ms = (totalLen / duration) * 10
                    sliderVelAtEnd[i] = totalLen / duration * 10f;
                    sliderVelAtStart[i] = totalLen / duration * 10f;
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
        List<Float> mVelAtEnd = new ArrayList<>();
        List<Float> mVelAtStart = new ArrayList<>();
        // End time of the spinner each entry refers to (0 for non-spinners).
        List<Float> mSpinnerEndTimes = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            if (merged[i]) continue;
            if (isSpinner[i]) {
                // Spinners can't be merged. Bake the spin-shape entry/exit points as the
                // segment endpoints (danser-go: DanceSpinner.StartPosRaw/EndPosRaw =
                // mover.GetPositionAt(Start/EndTime)), evaluated with the SAME SpinnerMover
                // the spin will use, so the approach mover glides exactly onto the shape
                // and the cursor never teleports when the spin starts or ends.
                ru.nsu.ccfit.zuev.osu.game.cursor.mover.SpinnerMoverFactory.SpinnerMover smover =
                    ru.nsu.ccfit.zuev.osu.game.cursor.mover.SpinnerMoverFactory.getMoverByName(
                        ru.nsu.ccfit.zuev.osu.game.cursor.mover.MoverSettings.getSpinnerMover());

                PointF entry = smover.getPositionAt(hitTimes[i], hitTimes[i]);
                PointF exit = smover.getPositionAt(endTimes[i], hitTimes[i]);
                mPosX.add(entry.x); mPosY.add(entry.y);
                mEndPosX.add(exit.x); mEndPosY.add(exit.y);

                // danser-go: DanceSpinner is an ILongObject — movers receive the motion
                // direction at its start/end and the 10ms travel distance, so the leaving
                // curve continues along the spin.
                float spinDurationMs = Math.max(1f, endTimes[i] - hitTimes[i]);
                float probe = Math.min(10f, spinDurationMs);
                PointF startAhead = smover.getPositionAt(hitTimes[i] + probe, hitTimes[i]);
                PointF endBefore = smover.getPositionAt(endTimes[i] - probe, hitTimes[i]);

                // GetStartAngleMod: angle of (pos(start+probe) - pos(start)) — forward.
                mStartAngles.add((float) Math.atan2(
                    startAhead.y - entry.y, startAhead.x - entry.x));
                // GetEndAngleMod: angle of (pos(end-probe) - pos(end)) — backward
                // (danser semantics: the leaving control point sits behind the exit).
                mEndAngles.add((float) Math.atan2(
                    endBefore.y - exit.y, endBefore.x - exit.x));
                // 10ms travel distances — used as segment start/end control lengths.
                float dStart = (float) Math.hypot(startAhead.x - entry.x, startAhead.y - entry.y);
                float dEnd = (float) Math.hypot(exit.x - endBefore.x, exit.y - endBefore.y);
                mVelAtStart.add(dStart); mVelAtEnd.add(dEnd);

                mHitTimes.add(hitTimes[i]); mEndTimes.add(endTimes[i]);
                mIsSlider.add(false);                mIsSpinner.add(true);
                mSpinnerEndTimes.add(endTimes[i]);
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
                    mIsSlider.add(false);                    mIsSpinner.add(false);
                    mStartAngles.add(0f); mEndAngles.add(0f);
                    mVelAtEnd.add(0f); mVelAtStart.add(0f);
                    mSpinnerEndTimes.add(0f);
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
            mVelAtEnd.add(sliderVelAtEnd[i]); mVelAtStart.add(sliderVelAtStart[i]);
            mSpinnerEndTimes.add(0f);
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
            false, false, 0f,
            mIsSlider.get(0), mIsSpinner.get(0), mStartAngles.get(0),
            0f, 0f,
            // When the FIRST object is a spinner the dummy segment must end at its
            // start time so the spin window is not collapsed to a single instant.
            // Mid-map spinners get their real end times in the pair loop below.
            mIsSpinner.get(0) ? mSpinnerEndTimes.get(0) : 0f
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

            // danser-go: startDistance = s1.GetStackedPositionAtMod(startTime-10).Dst(startPos)
            // = slider velocity at end * 10ms (how far ball moves in last 10ms)
            // endDistance = s2.GetStackedPositionAtMod(endTime+10).Dst(endPos)
            // = slider velocity at start * 10ms (how far ball moves in first 10ms)
            float sd = mIsSlider.get(i) || mIsSpinner.get(i)
                ? mVelAtEnd.get(i) : dist;
            float ed = mIsSlider.get(i + 1) || mIsSpinner.get(i + 1)
                ? mVelAtStart.get(i + 1) : dist;
            segmentQueue.add(new MovementSegment(
                i + 1,
                startPos, endPos,
                segStart, segEnd,
                mIsSlider.get(i), mIsSpinner.get(i), mEndAngles.get(i),
                mIsSlider.get(i + 1), mIsSpinner.get(i + 1), mStartAngles.get(i + 1),
                sd, ed,
                // When this segment ends at a spinner, remember when that spin stops.
                mIsSpinner.get(i + 1) ? mSpinnerEndTimes.get(i + 1) : 0f
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
                    .startIsSpinner(seg.startIsSpinner)
                    .startAngle(seg.startSliderEndAngle)
                    .endIsSlider(seg.endIsSlider)
                    .endIsSpinner(seg.endIsSpinner)
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
            // danser-go spline.go SetObjects(): the window starts at the current
            // (just-expired) object's end position at its end time, then each upcoming
            // object's start position at its start time. Without the leading point the
            // spline's time base begins at the next object's start and the cursor
            // teleports there for the whole break.
            PointF[] positions = new PointF[count + 1];
            float[] times = new float[count + 1];
            positions[0] = new PointF(seg.startPos.x, seg.startPos.y);
            times[0] = seg.startTimeMs;
            for (int i = 0; i < count; i++) {
                MovementSegment s = segmentQueue.get(index + i);
                positions[i + 1] = new PointF(s.endPos.x, s.endPos.y);
                times[i + 1] = s.endTimeMs;
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
        // Matches danser-go: only advance to next object when its start time is reached.
        // During breaks, the cursor stays at the end position of the previous object.
        if (queueActive && !segmentQueue.isEmpty()) {
            while (currentSegmentIndex >= 0 && currentSegmentIndex + 1 < segmentQueue.size()) {
                MovementSegment next = segmentQueue.get(currentSegmentIndex + 1);
                if (gameTimeMs >= next.startTimeMs) {
                    advanceToSegment(currentSegmentIndex + 1);
                } else {
                    break;
                }
            }
            // Spin termination is owned by the spin-window rule below (danser-go:
            // the spin ends at the spinner's end time — even when the next segment
            // also ends at a spinner, the gap between them is a mover BREAK, not a
            // continuation of the previous shape).
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

        // === SPINNER MOVEMENT ===
        // danser-go (app/dance/spinners): position is a PURE function of gameplay
        // time via the selected SpinnerMover (circle/heart/triangle/square/cube).
        // Time-based evaluation (instead of integrating frame deltas) keeps the
        // shape/speed correct under rate mods and identical to the entry/exit
        // points baked in initQueue().
        if (spinning) {
            PointF spinPos = currentSpinnerMover.getPositionAt(gameTimeMs, spinnerStartTimeMs);
            setPosition(spinPos.x, spinPos.y);
        } else if (queueActive && !segmentQueue.isEmpty()) {
            // Check if current segment ends at a spinner
            if (currentSegmentIndex >= 0 && currentSegmentIndex < segmentQueue.size()) {
                MovementSegment seg = segmentQueue.get(currentSegmentIndex);
                if (seg.endIsSpinner && gameTimeMs >= seg.endTimeMs) {
                    // Start spinning. seg.endPos is the spin-shape ENTRY point baked by
                    // initQueue() via the SAME spinner mover (position at the spinner's
                    // start time), so the approach mover glides exactly here and the
                    // spin starts without a teleport.
                    spinning = true;
                    spinnerStartTimeMs = seg.endTimeMs;
                    spinEndTimeMs = seg.endSpinnerEndTimeMs > 0f
                        ? seg.endSpinnerEndTimeMs
                        : seg.endTimeMs;
                    PointF spinPos = currentSpinnerMover.getPositionAt(spinnerStartTimeMs, spinnerStartTimeMs);
                    setPosition(spinPos.x, spinPos.y);
                } else {
                    // Normal mover positioning
                    PointF moverPos = currentMover.getPositionAt(gameTimeMs);
                    if (moverPos != null) {
                        setPosition(moverPos.x, moverPos.y);
                    } else {
                        setPosition(seg.endPos.x, seg.endPos.y);
                    }
                }
            }
        } else {
            // No precomputed queue (e.g. before initQueue ran): keep sampling the mover
            // only while it holds a movement window; otherwise hold the current position.
            PointF moverPos = currentMover.getPositionAt(gameTimeMs);
            if (moverPos != null) {
                setPosition(moverPos.x, moverPos.y);
            }
        }

        // === SPIN WINDOW (danser-go semantics) ===
        // The spinner owns the cursor only inside its [start, end] window, even for the
        // final spinner of the map. Afterwards the queue has no next segment and the
        // cursor stops at the last point of the spin, like danser-go with an empty queue.
        if (spinning && gameTimeMs >= spinEndTimeMs) {
            spinning = false;
            boolean advanced = false;
            while (currentSegmentIndex + 1 < segmentQueue.size()
                    && segmentQueue.get(currentSegmentIndex + 1).startTimeMs <= gameTimeMs) {
                advanceToSegment(currentSegmentIndex + 1);
                advanced = true;
            }
            if (advanced) {
                // A following segment exists: the regular mover owns the cursor again.
                PointF moverPos = currentMover.getPositionAt(gameTimeMs);
                if (moverPos != null) {
                    setPosition(moverPos.x, moverPos.y);
                }
            }
            // else: no next segment — the cursor stays at the spin exit point.
        }

        if (cursorListener != null) {
            cursorListener.onUpdatedAutoCursor(getX(), getY());
        }
        // Update trail AFTER position is set so it never lags behind cursor
        updateTrailFromMovement(deltaTimeSeconds);
    }

    public void setPosition(float pX, float pY, GameObjectListener listener) {
        setPosition(pX, pY);
        listener.onUpdatedAutoCursor(pX, pY);
    }

    /**
     * Resets transient movement-ownership state (slider following / spinning) without
     * touching the precomputed queue. Called on scene load and replay seek, where the
     * cursor can jump out of a spinner/slider mid-follow; a stale followingSlider flag
     * would freeze updateMovement() for the rest of the map.
     */
    public void resetTransientState() {
        followingSlider = false;
        justFinishedSlider = false;
        spinning = false;
        spinEndTimeMs = Float.MAX_VALUE;
    }

    /**
     * Rewinds the precomputed queue to the given gameplay time after a replay seek.
     * The advancement loop in updateMovement() only ever moves forward, so a backward
     * seek has to find the segment containing the target time and reconfigure the mover
     * for it, resuming the spin at the correct angle when the target is inside a spinner.
     */
    public void seekTo(float gameTimeSeconds) {
        resetTransientState();

        if (!queueActive || segmentQueue.isEmpty()) {
            return;
        }

        float ms = gameTimeSeconds * 1000f;

        // Same indexing rule as the advancement loop in updateMovement(): the current
        // segment is the last one whose start time has been reached.
        int idx = 0;
        while (idx + 1 < segmentQueue.size() && segmentQueue.get(idx + 1).startTimeMs <= ms) {
            ++idx;
        }

        advanceToSegment(idx);

        MovementSegment seg = segmentQueue.get(idx);

        if (seg.endIsSpinner && ms >= seg.endTimeMs) {
            // The target lies inside or after the spinner that ends this segment.
            float spinEnd = seg.endSpinnerEndTimeMs > 0f
                ? seg.endSpinnerEndTimeMs
                : seg.endTimeMs;
            boolean hasFollow = idx + 1 < segmentQueue.size();

            if (ms < spinEnd && (!hasFollow || segmentQueue.get(idx + 1).startTimeMs > ms)) {
                // Inside the spin window: resume the spin at the exact position at the
                // target time (the spinner mover is a pure function of gameplay time —
                // see updateMovement). The window ends at the spinner's end even with
                // no following segment.
                spinning = true;
                spinnerStartTimeMs = seg.endTimeMs;
                spinEndTimeMs = spinEnd;
                PointF spinPos = currentSpinnerMover.getPositionAt(ms, spinnerStartTimeMs);
                setPosition(spinPos.x, spinPos.y);
            } else if (!hasFollow) {
                // After the FINAL spinner: no following segment exists (empty queue in
                // danser-go) — the cursor freezes at the spin exit point.
                PointF exitPos = currentSpinnerMover.getPositionAt(spinEnd, seg.endTimeMs);
                setPosition(exitPos.x, exitPos.y);
            } else {
                // After a mid-map spinner: the regular mover owns the cursor again.
                PointF pos = currentMover.getPositionAt(ms);

                if (pos != null) {
                    setPosition(pos.x, pos.y);
                } else {
                    setPosition(seg.endPos.x, seg.endPos.y);
                }
            }
        } else {
            PointF pos = currentMover.getPositionAt(ms);

            if (pos != null) {
                setPosition(pos.x, pos.y);
            } else {
                setPosition(seg.endPos.x, seg.endPos.y);
            }
        }

        // The seek jump must never be interpolated by the trail (same rule as any
        // other cursor teleport).
        onCursorPress();
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

    /**
     * Hot style/sub-settings change requested from the replay/autoplay visual
     * settings panel while gameplay is running.
     *
     * setAutoplayStyle() alone only invalidates the queue and the cursor would freeze
     * until the next segment, so the queue is re-baked from the same objects with the
     * new mover (including spinner entry/exit points) and rewound to the current
     * gameplay time via seekTo.
     *
     * Must be called on the update thread (GameScene.onReplayMovementStyleChanged
     * routes through Execution.updateThread already).
     *
     * @param style    the new movement style (null = keep current, only sub-settings changed).
     * @param objects  the same HitObject[] passed to initQueue at map load.
     * @param listener the GameObjectListener used at initQueue time.
     * @param currentGameTimeSeconds current gameplay time, used to resume from.
     */
    public void applyStyleLive(AutoplayStyle style, HitObject[] objects, GameObjectListener listener, float currentGameTimeSeconds) {
        if (style != null) {
            // Persist so the next map load uses the new style.
            ru.nsu.ccfit.zuev.osu.Config.setString("autoplayStyle", style.getValue());
            currentStyle = style;
            currentMover = MoverFactory.createMover(currentStyle);
        } else if (currentMover == null) {
            return;
        }

        // Invalidate the per-session settings cache so sub-settings edits reach the new mover.
        MoverSettings.clearCache();
        currentSpinnerMover = ru.nsu.ccfit.zuev.osu.game.cursor.mover.SpinnerMoverFactory.getMoverByName(
            MoverSettings.getSpinnerMover());

        // Re-bake the queue with the new mover's geometry.
        initialized = false;
        queueActive = false;
        segmentQueue.clear();
        currentSegmentIndex = -1;
        initQueue(objects, listener);

        // Jump the (new) mover to NOW and continue; also cuts the trail.
        seekTo(currentGameTimeSeconds);
    }

    public AutoplayStyle getAutoplayStyle() {
        return currentStyle;
    }

    public void reset() {
        gameTimeMs = 0;
        initialized = false;
        followingSlider = false;
        justFinishedSlider = false;
        spinning = false;
        spinEndTimeMs = Float.MAX_VALUE;
        cursorListener = null;
        queueActive = false;
        segmentQueue.clear();
        currentSegmentIndex = -1;
        if (currentMover != null) currentMover.reset();
        // Refresh the cached mover settings (numbers are cached per session for the
        // hot path) and re-read the spinner mover so a settings change applies to the
        // next play.
        ru.nsu.ccfit.zuev.osu.game.cursor.mover.MoverSettings.clearCache();
        currentSpinnerMover = ru.nsu.ccfit.zuev.osu.game.cursor.mover.SpinnerMoverFactory.getMoverByName(
            ru.nsu.ccfit.zuev.osu.game.cursor.mover.MoverSettings.getSpinnerMover());
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
