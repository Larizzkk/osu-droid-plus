package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.edlplan.framework.math.FMath;
import com.osudroid.beatmaps.HitWindow;
import com.osudroid.beatmaps.hitobjects.HitObject;
import com.osudroid.game.CursorEvent;
import com.osudroid.game.PressResult;
import com.rian.andengine.modifier.UniversalModifier;
import com.rian.andengine.modifier.UniversalModifierSequence;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;

public abstract class GameObject {
    protected boolean endsCombo;
    protected boolean autoPlay = false;
    protected float hitTime = 0;
    protected int id = -1;
    protected Replay.ReplayObjectData replayObjectData = null;
    protected boolean startHit = false;
    protected PointF position = new PointF();
    private float lifetimeEnd;

    /** The {@link PressResult.Hit} returned by {@link #checkPresses}, reused to avoid allocations. */
    private final PressResult.Hit hitResult = new PressResult.Hit();

    public Replay.ReplayObjectData getReplayData() {
        return replayObjectData;
    }

    public void setReplayData(Replay.ReplayObjectData replayObjectData) {
        this.replayObjectData = replayObjectData;
    }

    public void setAutoPlay() {
        autoPlay = true;
    }

    public abstract void update(float dt);

    public float getHitTime() {
        return hitTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public PointF getPosition() {
        return position;
    }

    public boolean isStartHit() {
        return startHit;
    }

    public void playLoopingSamples() {}

    public void stopLoopingSamples() {}

    /**
     * Checks the presses of the current frame against this {@link GameObject}.
     * <p>
     * While this {@link GameObject} is hittable (see {@link GameObjectListener#isObjectHittable}),
     * unprocessed presses are consumed, even those that do not hit, which enforces the start
     * time ordered hit policy (aka "notelock"). Otherwise, nothing is consumed, and only presses
     * that landed on this {@link GameObject} are considered.
     * <p>
     * A press that hits this {@link GameObject} or makes it shake is claimed, so that overlapping
     * {@link GameObject}s locked behind this one, which are updated later, do not react to it.
     *
     * @param listener The {@link GameObjectListener} to get cursors from.
     * @param hitObject The {@link HitObject} associated with this {@link GameObject}.
     * @param objectElapsedTime The elapsed time since the start of this {@link GameObject}, in seconds.
     * @return The {@link PressResult}. A {@link PressResult.Hit} is only valid until the next call.
     */
    @NonNull
    protected final PressResult checkPresses(GameObjectListener listener, HitObject hitObject, double objectElapsedTime) {
        int cursorCount = listener.getCursorsCount();

        // Relax matches osudroid!relax (repos/osudroid-rx): every active object is judged
        // independently. There is no notelock gate, unlike regular gameplay, so a cursor
        // hovering over a later object can hit it even while an earlier one is still alive.
        // This must run before isObjectHittable() or Relax inherits normal notelock.
        if (GameHelper.isRelax()) {
            for (int i = 0; i < cursorCount; i++) {
                var event = listener.getCursor(i).getLatestEvent();

                if (event != null && objectElapsedTime >= 0 && isHit(hitObject, event)) {
                    return hitResult.withEvent(event);
                }
            }

            return PressResult.Ignore.INSTANCE;
        }

        // No Notelock removes the start-time ordered gate: each active object checks the
        // current presses independently, so a later circle can be hit before an earlier one.
        if (GameHelper.isNoNotelock() && !GameHelper.isAutopilot()) {
            return checkNoNotelockPresses(listener, hitObject);
        }

        if (!listener.isObjectHittable(this)) {
            return checkNotelockedPresses(listener, hitObject);
        }

        // For Autopilot, we only need to check if a cursor is pressed.
        if (GameHelper.isAutopilot()) {
            for (int i = 0; i < cursorCount; i++) {
                var cursor = listener.getCursor(i);
                var events = cursor.downEvents;
                int size = events.size();

                while (cursor.latestProcessedDownEventIndex < size) {
                    var event = events.get(cursor.latestProcessedDownEventIndex++);

                    if (canHit(event, hitObject.hitWindow)) {
                        return hitResult.withEvent(event);
                    }
                }
            }

            return PressResult.Ignore.INSTANCE;
        }

        // In regular gameplay, we need to iteratively check whether one or more cursors is on the object while a cursor
        // is being pressed.
        boolean pressedEarly = false;

        for (int i = 0; i < cursorCount; i++) {
            var cursor = listener.getCursor(i);
            var downEvents = cursor.downEvents;
            int downEventsSize = downEvents.size();

            // Consume down events that have not been processed yet, even those that are already past the hit threshold.
            // This ensures that start time ordered hit policy (aka "notelock") is enforced correctly.
            while (cursor.latestProcessedDownEventIndex < downEventsSize) {
                int index = cursor.latestProcessedDownEventIndex++;
                var downEvent = downEvents.get(index);

                if (!canHit(downEvent, hitObject.hitWindow)) {
                    // The press is too early to hit this object, but it still shakes the object if it landed on it.
                    if (!cursor.isDownEventClaimed(index) && isPressOnObject(listener, hitObject, i, downEvent, cursorCount)) {
                        cursor.claimDownEvent(index);
                        pressedEarly = true;
                    }

                    continue;
                }

                if (isPressOnObject(listener, hitObject, i, downEvent, cursorCount)) {
                    cursor.claimDownEvent(index);
                    return hitResult.withEvent(downEvent);
                }
            }
        }

        return pressedEarly ? PressResult.Shake.INSTANCE : PressResult.Ignore.INSTANCE;
    }

    /**
     * Checks whether a press of the current frame landed on this {@link GameObject} while it is
     * locked behind an earlier {@link GameObject}, regardless of whether the press is inside its
     * hit window. This does not consume presses, but it still claims presses that landed here.
     */
    @NonNull
    private PressResult checkNotelockedPresses(GameObjectListener listener, HitObject hitObject) {
        if (autoPlay || GameHelper.isAutopilot() || GameHelper.isRelax() || hitObject.hitWindow == null) {
            return PressResult.Ignore.INSTANCE;
        }

        int cursorCount = listener.getCursorsCount();
        boolean pressed = false;

        for (int i = 0; i < cursorCount; i++) {
            // Cursor.downEvents is cleared every frame, so the whole list is new for this frame.
            var cursor = listener.getCursor(i);
            var downEvents = cursor.downEvents;

            for (int index = 0, size = downEvents.size(); index < size; index++) {
                if (!cursor.isDownEventClaimed(index) &&
                    isPressOnObject(listener, hitObject, i, downEvents.get(index), cursorCount)) {
                    cursor.claimDownEvent(index);
                    pressed = true;
                }
            }
        }

        return pressed ? PressResult.Shake.INSTANCE : PressResult.Ignore.INSTANCE;
    }

    /**
     * Checks presses without the start-time ordered gate. A press is claimed by the first
     * object it physically hits, but a press that misses this object stays available for the
     * other active objects in the same frame.
     */
    @NonNull
    private PressResult checkNoNotelockPresses(GameObjectListener listener, HitObject hitObject) {
        if (autoPlay || hitObject.hitWindow == null) {
            return PressResult.Ignore.INSTANCE;
        }

        int cursorCount = listener.getCursorsCount();

        for (int i = 0; i < cursorCount; i++) {
            var cursor = listener.getCursor(i);
            var downEvents = cursor.downEvents;

            for (int index = 0, size = downEvents.size(); index < size; index++) {
                var event = downEvents.get(index);

                if (!cursor.isDownEventClaimed(index) && canHit(event, hitObject.hitWindow)
                    && isPressOnObject(listener, hitObject, i, event, cursorCount)) {
                    cursor.claimDownEvent(index);
                    return hitResult.withEvent(event);
                }
            }
        }

        return PressResult.Ignore.INSTANCE;
    }

    /**
     * Replay counterpart of the notelock shake: a recorded press that landed on an object which
     * was still locked behind an earlier object must shake it, even though replay hits themselves
     * come from {@link Replay.ReplayObjectData} rather than from live presses.
     */
    public boolean checkReplayNotelockShake(GameObjectListener listener, HitObject hitObject) {
        if (autoPlay || GameHelper.isNoNotelock() || GameHelper.isRelax() || GameHelper.isAutopilot() || hitObject.hitWindow == null) {
            return false;
        }

        if (listener.isObjectHittable(this)) {
            // Live gameplay consumes every unprocessed press in checkPresses(), even presses that
            // did not land on the hittable object. Do the same here without judging the replay hit,
            // otherwise an earlier object would leave presses for locked objects behind it.
            for (int i = 0, cursorCount = listener.getCursorsCount(); i < cursorCount; i++) {
                var cursor = listener.getCursor(i);
                cursor.latestProcessedDownEventIndex = cursor.downEvents.size();
            }
            return false;
        }

        if (!Config.isNotelockShake()) {
            return false;
        }

        return checkNotelockedPresses(listener, hitObject) == PressResult.Shake.INSTANCE;
    }

    /**
     * Determines whether a down {@link CursorEvent} landed on this {@link GameObject}, either by
     * the cursor that generated it or by any other cursor that was held down at the time.
     */
    private boolean isPressOnObject(GameObjectListener listener, HitObject hitObject, int cursorIndex,
                                    CursorEvent downEvent, int cursorCount) {
        for (int j = 0; j < cursorCount; j++) {
            if (cursorIndex == j) {
                if (isHit(hitObject, downEvent)) {
                    return true;
                }
            } else {
                var closestEvent = listener.getCursor(j).getClosestEventBefore(downEvent.systemTime);

                if (closestEvent != null && !closestEvent.isActionUp() && isHit(hitObject, closestEvent)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Determines whether a {@link CursorEvent} can hit this {@link GameObject}.
     *
     * @param cursorEvent The {@link CursorEvent} to check.
     * @param hitWindow The {@link HitWindow} of this {@link GameObject}.
     * @return {@code true} if the {@link CursorEvent} can hit this {@link GameObject}, {@code false} otherwise.
     */
    private boolean canHit(CursorEvent cursorEvent, HitWindow hitWindow) {
        if (cursorEvent.isActionUp()) {
            return false;
        }

        float hittableRange = GameHelper.isAutopilot()
            ? (float) FMath.clamp(hitWindow.getMehWindow() + 100, 200, HitWindow.MISS_WINDOW) / 1000
            : (float) HitWindow.MISS_WINDOW / 1000;

        return cursorEvent.getHitTime() / 1000 >= hitTime - hittableRange;
    }

    /**
     * Determines whether a {@link CursorEvent} hits this {@link GameObject}. The determination is solely based on
     * distance, not considering timing.
     *
     * @param hitObject The {@link HitObject} associated with this {@link GameObject}.
     * @param cursorEvent The {@link CursorEvent} to check.
     * @return {@code true} if the {@link CursorEvent} hits this {@link GameObject}, {@code false} otherwise.
     */
    private boolean isHit(HitObject hitObject, CursorEvent cursorEvent) {
        if (cursorEvent.isActionUp()) {
            return false;
        }

        // Aim Assist is a meaningful hitbox expansion: a tap in the object's
        // wider assist radius still counts, not just a tap directly on the circle.
        float radius = (float) hitObject.getScreenSpaceGameplayRadius()
                * (Config.isCheatAimAssist() ? 2.5f : 1f);

        return Utils.squaredDistance(position, cursorEvent.position) <= Utils.sqr(radius);
    }

    /** Shake circle by 8 units inside the circle's local space, in which the circle is 128 units wide. */
    private static final float SHAKE_MAGNITUDE_RATIO = 8f / 128f;

    /** Obtains the shake magnitude for a {@link HitObject}, in screen pixels. */
    protected static float getShakeMagnitude(HitObject hitObject) {
        return (float) (2 * hitObject.getScreenSpaceGameplayRadius()) * SHAKE_MAGNITUDE_RATIO;
    }

    //region Lifetime management

    /**
     * Whether the underlying {@link HitObject} of this {@link GameObject} has been judged in its entirety, including
     * nested {@link HitObject}s.
     */
    public boolean isJudged() {
        return false;
    }

    /**
     * Called when this {@link GameObject}'s lifetime expires.
     */
    public void onExpire() {}

    /**
     * Obtains the time in seconds since the beatmap started at which this {@link GameObject}'s lifetime ends.
     */
    public float getLifetimeEnd() {
        return lifetimeEnd;
    }

    protected void setLifetimeEnd(float lifetimeEnd) {
        this.lifetimeEnd = lifetimeEnd;
    }

    /**
     * Extends the lifetime of this {@link GameObject} to allow a {@link UniversalModifier} to finish.
     *
     * @param modifier The {@link UniversalModifier} to extend this {@link GameObject}'s lifetime with.
     */
    protected void extendLifetime(UniversalModifier modifier) {
        lifetimeEnd = Math.max(lifetimeEnd, modifier.getEndTime());
    }

    /**
     * Extends the lifetime of this {@link GameObject} to allow a {@link UniversalModifierSequence} to finish.
     *
     * @param sequence The {@link UniversalModifierSequence} to extend this {@link GameObject}'s lifetime with.
     */
    protected void extendLifetime(UniversalModifierSequence sequence) {
        lifetimeEnd = Math.max(lifetimeEnd, sequence.getEndTime());
    }

    //endregion
}
