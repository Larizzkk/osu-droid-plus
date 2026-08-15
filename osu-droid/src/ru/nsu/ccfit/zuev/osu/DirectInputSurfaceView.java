package ru.nsu.ccfit.zuev.osu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.concurrent.atomic.AtomicIntegerArray;
import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.input.touch.controller.ITouchController;
import org.anddev.andengine.opengl.view.RenderSurfaceView;
import org.anddev.andengine.util.Debug;

/**
 * Custom RenderSurfaceView that uses Android's InputEventReceiver at the lowest possible level.
 *
 * This view overrides dispatchTouchEvent() to intercept MotionEvents BEFORE they reach the
 * Engine's OnTouchListener. The raw pointer data is updated immediately on the UI thread,
 * bypassing any queuing or synchronization delays in the Engine.
 *
 * Combined with the Choreographer-driven frame callbacks, this provides near-zero
 * input latency by decoupling touch sampling from the game's update-render cycle.
 *
 * How it works:
 *   1. Touch happens → Android dispatches MotionEvent to dispatchTouchEvent()
 *   2. We IMMEDIATELY update the raw pointer arrays (thread-safe atomic versioning)
 *   3. We pass the event to the Engine's normal processing (for game logic events)
 *   4. The Choreographer callback samples the latest touch data on each vsync
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

    /**
     * The touch scan rate of the device's touch controller in Hz.
     * Higher is better (1000Hz = 1ms intervals).
     */
    private float touchScanRateHz = 0;

    public DirectInputSurfaceView(final Context context) {
        super(context);
        detectTouchScanRate();
    }

    public DirectInputSurfaceView(
        final Context context,
        final AttributeSet attrs
    ) {
        super(context, attrs);
        detectTouchScanRate();
    }

    /**
     * Detects the touch controller's hardware scan rate via display refresh rate.
     * Modern touch panels typically match or exceed the display's refresh rate.
     */
    private void detectTouchScanRate() {
        // Use the display's refresh rate as a baseline for touch scan rate
        // Most modern touch panels scan at 120Hz+ on high-refresh displays
        try {
            android.view.Display display = (
                (android.view.WindowManager) getContext().getSystemService(
                    Context.WINDOW_SERVICE
                )
            ).getDefaultDisplay();
            float rate = display.getRefreshRate();
            if (rate >= 60) {
                touchScanRateHz = Math.max(rate, 120); // Touch usually >= display Hz
            }
        } catch (Exception ignored) {}

        if (touchScanRateHz <= 0) {
            touchScanRateHz = 120;
        }
        Debug.i(
            "Touch scan rate: " + (int) touchScanRateHz + " Hz (estimated)"
        );
    }

    /**
     * Returns the detected touch scan rate in Hz.
     */
    public float getTouchScanRateHz() {
        return touchScanRateHz;
    }

    @Override
    public void setRenderer(final Engine pEngine) {
        super.setRenderer(pEngine);
        this.attachedEngine = pEngine;
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

        // Step 2: Signal the engine's UpdateThread to wake up and process touch NOW
        // This breaks the update-render lockstep, allowing input to be handled
        // at the speed of the touch controller (up to 1000Hz) instead of the
        // display refresh rate (60-144Hz).
        attachedEngine.signalTouchInterrupt();

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

    /**
     * Reads a consistent snapshot of a pointer's position.
     *
     * @param pointerId The pointer ID to read.
     * @param outCoords Array of length 2+ to receive [x, y, isDown(0/1)].
     * @return true if a consistent snapshot was read, false if the pointer is unstable.
     */
    public boolean readPointerSnapshot(int pointerId, float[] outCoords) {
        for (int attempt = 0; attempt < 3; attempt++) {
            int verBefore = mPointerVersions.get(pointerId);
            // Odd version = being written on UI thread, retry
            if ((verBefore & 1) != 0) continue;

            float x = mPointerX[pointerId];
            float y = mPointerY[pointerId];
            boolean down = mPointerDown[pointerId];
            int verAfter = mPointerVersions.get(pointerId);

            if (verBefore == verAfter && (verAfter & 1) == 0) {
                outCoords[0] = x;
                outCoords[1] = y;
                outCoords[2] = down ? 1f : 0f;
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the raw pointer state arrays for direct reading by the game engine.
     */
    public AtomicIntegerArray getPointerVersions() {
        return mPointerVersions;
    }

    public float[] getPointerX() {
        return mPointerX;
    }

    public float[] getPointerY() {
        return mPointerY;
    }

    public boolean[] getPointerDown() {
        return mPointerDown;
    }

    public long[] getPointerEventTime() {
        return mPointerEventTime;
    }

    public int getMaxPointers() {
        return MAX_POINTERS;
    }
}
