package ru.nsu.ccfit.zuev.osuplusplus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import java.util.concurrent.atomic.AtomicIntegerArray;
import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.input.touch.controller.ITouchController;
import org.anddev.andengine.opengl.view.RenderSurfaceView;

/**
 * Custom RenderSurfaceView that uses Android's InputEventReceiver at the lowest possible level.
 *
 * This view overrides dispatchTouchEvent() to intercept MotionEvents BEFORE they reach the
 * Engine's OnTouchListener. The raw pointer data is updated immediately on the UI thread,
 * bypassing any queuing or synchronization delays in the Engine.
 *
 * How it works:
 *   1. Touch happens → Android dispatches MotionEvent to dispatchTouchEvent()
 *   2. We IMMEDIATELY update the raw pointer arrays (thread-safe atomic versioning)
 *      and queue every historical + current sample into per-pointer SPSC ring buffers
 *   3. We pass the event to the Engine's normal processing (for game logic events);
 *      the game's update thread drains the sample queues every tick
 *
 * This eliminates the 1-frame queue latency entirely for cursor tracking.
 * On supported devices, InputDevice.getMotionRanges() provides the hardware scan rate,
 * which can be up to 1000Hz on modern touch controllers.
 */
public class DirectInputSurfaceView extends RenderSurfaceView {

    private Engine attachedEngine;

    /**
     * Maximum number of simultaneous touch pointers.
     */
    private static final int MAX_POINTERS = 100;

    /**
     * Atomic version counter for thread-safe raw pointer reads.
     * Even = stable, Odd = being written (on UI thread).
     */
    private final AtomicIntegerArray mPointerVersions = new AtomicIntegerArray(
        MAX_POINTERS
    );

    /**
     * Latest X position for each pointer (surface coordinates).
     */
    private final float[] mPointerX = new float[MAX_POINTERS];

    /**
     * Latest Y position for each pointer (surface coordinates).
     */
    private final float[] mPointerY = new float[MAX_POINTERS];

    /**
     * Whether each pointer is currently down (touching).
     */
    private final boolean[] mPointerDown = new boolean[MAX_POINTERS];

    /**
     * Event time for each pointer (uptime millis).
     */
    private final long[] mPointerEventTime = new long[MAX_POINTERS];

    // ─── Per-pointer sample ring buffers (SPSC: UI thread writes, update thread drains) ───
    // Historical samples from batched MotionEvents are queued here in chronological order so
    // the game's update thread can consume the COMPLETE movement path instead of only the
    // latest position — a latest-sample-wins array loses 5-10 intermediate positions per
    // batch and precision during fast flicks.
    private static final int SAMPLE_POINTER_SLOTS = 10;    // Android pointer IDs are small sequential ints
    private static final int SAMPLE_BUFFER_CAPACITY = 512;

    private final float[] mSampleX = new float[SAMPLE_POINTER_SLOTS * SAMPLE_BUFFER_CAPACITY];
    private final float[] mSampleY = new float[SAMPLE_POINTER_SLOTS * SAMPLE_BUFFER_CAPACITY];
    private final long[] mSampleTime = new long[SAMPLE_POINTER_SLOTS * SAMPLE_BUFFER_CAPACITY];
    /** 1 = finger/pointer down at this sample, 0 = up. Lets the update thread derive
     *  DOWN/MOVE/UP transitions from the stream instead of from the live pointer state
     *  (otherwise a stale UP sample is re-consumed on the next tap as a phantom DOWN). */
    private final boolean[] mSampleDown = new boolean[SAMPLE_POINTER_SLOTS * SAMPLE_BUFFER_CAPACITY];
    /**
     * Encoded Android action (ACTION_DOWN/MOVE/UP) PER SLOT, written by the producer
     * right before the write index advances. The consumer (GameScene drain) classifies
     * each sample from this field instead of inferring the state from previous cursor
     * events.
     */
    private final int[] mSampleAction = new int[SAMPLE_POINTER_SLOTS * SAMPLE_BUFFER_CAPACITY];
    private final AtomicIntegerArray mSampleWriteIndex = new AtomicIntegerArray(SAMPLE_POINTER_SLOTS);
    private final AtomicIntegerArray mSampleReadIndex = new AtomicIntegerArray(SAMPLE_POINTER_SLOTS);

    public DirectInputSurfaceView(final Context context) {
        super(context);
    }

    public DirectInputSurfaceView(
        final Context context,
        final AttributeSet attrs
    ) {
        super(context, attrs);
    }

    @Override
    public void setRenderer(final Engine pEngine) {
        super.setRenderer(pEngine);
        this.attachedEngine = pEngine;

        // requestUnbufferedDispatch is deliberately NOT used: it disables the kernel's
        // batching buffer, delivering every touch sample as its own syscall on the UI
        // thread — under load that adds jitter to both input and rendering.
        // updateRawPointersFromEvent already consumes all historical samples of each
        // batch, which preserves the full movement path without unbuffering.
    }

    /**
     * Returns the Engine attached to this view.
     */
    public Engine getAttachedEngine() {
        return attachedEngine;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event == null || attachedEngine == null) {
            return super.dispatchTouchEvent(event);
        }

        // Step 1: IMMEDIATELY update raw pointer data (before Engine processing)
        // This runs on the UI thread and provides the absolute latest touch position
        // to the game engine, bypassing the 1-frame queue latency.
        updateRawPointersFromEvent(event);

        // Step 2: Signal the engine's UpdateThread to wake up for tap-critical events.
        // ONLY DOWN/UP/CANCEL signal here. Signaling on every MOVE (120+ Hz) triggered
        // up to 4 extra full update cycles per frame in coupled mode (each recomputing
        // dt) — that made frame times jitter and input WORSE. MOVE positions reach the
        // game through the SPSC sample queue drained every update tick anyway.
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN
            || action == MotionEvent.ACTION_POINTER_DOWN
            || action == MotionEvent.ACTION_UP
            || action == MotionEvent.ACTION_POINTER_UP
            || action == MotionEvent.ACTION_CANCEL) {
            attachedEngine.signalTouchInterrupt();
        }

        // Step 3: Let the Engine process the event normally through the queue
        // This handles the normal touch event flow for game logic
        return super.dispatchTouchEvent(event);
    }

    /**
     * Immediately extracts ALL touch data from a MotionEvent (including historical
     * samples) and writes it to the thread-safe raw pointer arrays.
     *
     * Android batches MOVE events — a single MotionEvent can contain 5-10
     * historical positions between the last and current event. By processing
     * ALL historical samples in chronological order, the game engine sees
     * the COMPLETE finger movement path for smooth slider tracking.
     */
    private void updateRawPointersFromEvent(MotionEvent event) {
        try {
            int action = event.getActionMasked();
            int pointerCount = Math.min(event.getPointerCount(), MAX_POINTERS);
            int historySize = event.getHistorySize();

            for (int h = 0; h < historySize; h++) {
                for (int i = 0; i < pointerCount; i++) {
                    int pointerId = event.getPointerId(i);
                    if (pointerId < 0 || pointerId >= MAX_POINTERS) continue;

                    long histTime = event.getHistoricalEventTime(h);
                    float x = event.getHistoricalX(i, h);
                    float y = event.getHistoricalY(i, h);

                    boolean isDown =
                        action == MotionEvent.ACTION_CANCEL
                            ? false
                            : mPointerDown[pointerId] ||
                              isDownAction(action, i, event.getActionIndex());

                    // Atomic write
                    mPointerVersions.incrementAndGet(pointerId);
                    mPointerX[pointerId] = x;
                    mPointerY[pointerId] = y;
                    mPointerDown[pointerId] = isDown;
                    mPointerEventTime[pointerId] = histTime;
                    mPointerVersions.incrementAndGet(pointerId);

                    // Queue the sample for ordered consumption by the update thread.
                    // Encode the per-sample action: historical entries of a MOVE batch
                    // are MOVEs; DOWN/UP actions only land on the affected pointer via
                    // the current-sample pass below. A pointer that already lifted must
                    // NOT be resurrected by historical MOVE entries — classify strictly.
                    int histAction;
                    if (action == MotionEvent.ACTION_MOVE) {
                        histAction = mPointerDown[pointerId] || isDownAction(action, i, event.getActionIndex())
                            ? MotionEvent.ACTION_MOVE
                            : MotionEvent.ACTION_UP;
                    } else {
                        histAction = isDown ? MotionEvent.ACTION_MOVE : MotionEvent.ACTION_UP;
                    }
                    pushPointerSample(pointerId, x, y, histTime, isDown, histAction);
                }
            }

            // Current (latest) sample — always written last so the UpdateThread
            // reads the most recent finger position.
            long eventTime = event.getEventTime();
            for (int i = 0; i < pointerCount; i++) {
                int pointerId = event.getPointerId(i);
                if (pointerId < 0 || pointerId >= MAX_POINTERS) continue;

                float x = event.getX(i);
                float y = event.getY(i);
                boolean isDown =
                    action == MotionEvent.ACTION_CANCEL
                        ? false
                        : isDownAction(action, i, event.getActionIndex()) ||
                          (action != MotionEvent.ACTION_UP &&
                              action != MotionEvent.ACTION_POINTER_UP &&
                              mPointerDown[pointerId]);

                // Atomic write with version guard
                mPointerVersions.incrementAndGet(pointerId);
                mPointerX[pointerId] = x;
                mPointerY[pointerId] = y;
                mPointerDown[pointerId] = isDown;
                mPointerEventTime[pointerId] = eventTime;
                mPointerVersions.incrementAndGet(pointerId);

                // Queue the sample for ordered consumption by the update thread.
                // encodedAction carries the exact Android action (DOWN/MOVE/UP) so the
                // consumer classifies each sample without guessing from the previous
                // cursor state — a MOVE batch must never re-classify a lifted pointer
                // as pressed (that synthesized phantom DOWN events mid-stream: streams
                // hit "easier" than upstream).
                int curAction = isDown
                    ? (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN)
                        ? MotionEvent.ACTION_DOWN
                        : MotionEvent.ACTION_MOVE
                    : MotionEvent.ACTION_UP;
                pushPointerSample(pointerId, x, y, eventTime, isDown, curAction);
            }
        } catch (Exception ignored) {
            // Never crash in the input path
        }
    }

    /**
     * Determines if the given pointer index is in a "down" state based on the action.
     */
    private static boolean isDownAction(
        int action,
        int pointerIndex,
        int actionIndex
    ) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                return pointerIndex == actionIndex;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                return pointerIndex != actionIndex;
            case MotionEvent.ACTION_MOVE:
                return true; // still touching
            default:
                return false;
        }
    }

    private void pushPointerSample(int pointerId, float x, float y, long eventTime, boolean down, int encodedAction) {
        if (pointerId < 0 || pointerId >= SAMPLE_POINTER_SLOTS) return;

        int slot = pointerId * SAMPLE_BUFFER_CAPACITY;
        int writeIndex = mSampleWriteIndex.get(pointerId);
        int nextWrite = (writeIndex + 1) % SAMPLE_BUFFER_CAPACITY;

        // Full buffer: drop the OLDEST sample by advancing the read cursor.
        if (nextWrite == mSampleReadIndex.get(pointerId)) {
            mSampleReadIndex.incrementAndGet(pointerId);
        }

        mSampleX[slot + writeIndex] = x;
        mSampleY[slot + writeIndex] = y;
        mSampleTime[slot + writeIndex] = eventTime;
        mSampleDown[slot + writeIndex] = down;
        mSampleAction[slot + writeIndex] = encodedAction;
        mSampleWriteIndex.set(pointerId, nextWrite);
    }

    /**
     * Pops the oldest queued touch sample of the given pointer.
     * Runs on the game's update thread.
     *
     * NOTE: the timestamp MUST be delivered as a long — SystemClock.uptimeMillis
     * values (> 2^24 ms) lose all sub-~32ms precision through a float, which breaks
     * the consumer's timestamp dedup and the sub-frame offset computation.
     *
     * @param pointerId The pointer ID to read from.
     * @param outCoords Array of length 4+ receiving [x, y, unused, down(1/0)].
     * @param outTime   Array of length 1+ receiving the sample time (uptime millis).
     * @return true if a sample was available, false if the queue is empty.
     */
    public boolean popPointerSample(int pointerId, float[] outCoords, long[] outTime) {
        return popPointerSample(pointerId, outCoords, outTime, null);
    }

    /**
     * Pops the oldest queued touch sample, additionally reporting the encoded Android
     * action (ACTION_DOWN/MOVE/UP) captured at queue time through {@code outAction[0]}
     * when the array is non-null. Consumers classify from the recorded action instead
     * of guessing from previous cursor state.
     */
    public boolean popPointerSample(int pointerId, float[] outCoords, long[] outTime, int[] outAction) {
        if (pointerId < 0 || pointerId >= SAMPLE_POINTER_SLOTS || outTime == null) return false;

        int readIndex = mSampleReadIndex.get(pointerId);
        if (readIndex == mSampleWriteIndex.get(pointerId)) {
            return false; // Empty.
        }

        int slot = pointerId * SAMPLE_BUFFER_CAPACITY;
        outCoords[0] = mSampleX[slot + readIndex];
        outCoords[1] = mSampleY[slot + readIndex];
        outTime[0] = mSampleTime[slot + readIndex];
        outCoords[3] = mSampleDown[slot + readIndex] ? 1f : 0f;
        if (outAction != null) {
            outAction[0] = mSampleAction[slot + readIndex];
        }
        mSampleReadIndex.set(pointerId, (readIndex + 1) % SAMPLE_BUFFER_CAPACITY);
        return true;
    }

    /**
     * Number of sample queue slots (mirrors the raw pointer capacity used by callers).
     */
    public int getMaxSampleSlots() {
        return SAMPLE_POINTER_SLOTS;
    }

    /**
     * Number of queued samples for diagnostics.
     */
    public int getQueuedSampleCount(int pointerId) {
        if (pointerId < 0 || pointerId >= SAMPLE_POINTER_SLOTS) return 0;
        int writeIndex = mSampleWriteIndex.get(pointerId);
        int readIndex = mSampleReadIndex.get(pointerId);
        return (writeIndex - readIndex + SAMPLE_BUFFER_CAPACITY) % SAMPLE_BUFFER_CAPACITY;
    }

    /**
     * Clears the sample queues (call when raw pointers reset).
     * Drains EVERY slot: skipping pointers that look "not down" leaves their last
     * UP sample queued, and re-consuming it on the next tap synthesizes a phantom
     * DOWN at a stale position (ghost press far from the finger).
     */
    public void clearPointerSamples() {
        for (int i = 0; i < SAMPLE_POINTER_SLOTS; i++) {
            mSampleReadIndex.set(i, mSampleWriteIndex.get(i));
        }
    }

}
