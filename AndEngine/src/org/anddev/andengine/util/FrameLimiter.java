package org.anddev.andengine.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * High-precision frame limiter matching osu!stable's frame limiter modes.
 *
 * Three-phase wait on an absolute deadline:
 *   1. coarse Thread.sleep in ~1ms chunks (abortable by touch input),
 *   2. fine LockSupport.parkNanos in ~200us windows,
 *   3. busy-spin for the final ~150us.
 *
 * Thread.sleep and parkNanos both oversleep by ~50-200us on Android; the spin
 * tail removes that jitter, keeping frame times tight at high refresh rates
 * (osu!stable/lazer use the same sleep+spin approach).
 *
 * touchInterrupted is an AtomicBoolean to avoid race conditions
 * when signal arrives between checks.
 */
public final class FrameLimiter {

    public static final int MODE_UNLIMITED = 0;
    public static final int MODE_VSYNC = 2;
    public static final int MODE_OPTIMAL = 3;

    private static final long NS_PER_S = 1_000_000_000L;

    /**
     * The last wait phase busy-spins once the remaining time is below this
     * threshold; below ~150us a parkNanos round-trip costs more than spinning.
     */
    private static final long SPIN_THRESHOLD_NS = 150_000L;

    private volatile int mode = MODE_UNLIMITED;
    private volatile int customFps = 0;
    private volatile float displayRefreshRate = 60f;

    private volatile long targetFrameNs = 0;

    /**
     * Atomic flag to abort the current wait early on touch input.
     * Uses getAndSet(false) to avoid losing signals between reads.
     */
    private final AtomicBoolean touchInterrupted = new AtomicBoolean(false);

    /** Tracks whether the current mode requires eglSwapInterval to be re-applied. */
    private volatile boolean swapIntervalDirty = true;

    private static final FrameLimiter INSTANCE = new FrameLimiter();

    public static FrameLimiter getInstance() {
        return INSTANCE;
    }

    private FrameLimiter() {}

    public void configure(int mode, int customFps, float displayRefreshRate) {
        int oldMode = this.mode;
        this.mode = mode;
        this.customFps = customFps;
        this.displayRefreshRate = displayRefreshRate;
        recomputeTarget();
        // Signal swap interval needs update when mode changes.
        if (mode != oldMode) {
            this.swapIntervalDirty = true;
        }
    }

    public void setMode(int mode) {
        int oldMode = this.mode;
        this.mode = mode;
        recomputeTarget();
        if (mode != oldMode) {
            this.swapIntervalDirty = true;
        }
    }

    public void setDisplayRefreshRate(float hz) {
        this.displayRefreshRate = hz;
        recomputeTarget();
    }

    /**
     * Called from the UI thread on touch input.
     * Uses getAndSet to ensure no signal is lost even if a wait loop
     * is between check-and-reset.
     */
    public void signalTouchInterrupt() {
        this.touchInterrupted.set(true);
    }

    /**
     * Returns true if the swap interval needs re-application.
     * Caller should re-apply eglSwapInterval afterwards.
     */
    public boolean isSwapIntervalDirty() {
        boolean dirty = this.swapIntervalDirty;
        this.swapIntervalDirty = false;
        return dirty;
    }

    private void recomputeTarget() {
        int fps;
        switch (mode) {
            case MODE_VSYNC:
                // Pure display vsync (upstream osu!droid behavior): NO software
                // limiter on top of eglSwapInterval(1). A software sleep racing the
                // vsync deadline only adds jitter — a frame that misses the swap
                // deadline blocks for a FULL extra display period. The GL swap
                // paces both render and (via yieldDraw) the update thread.
                // targetFps=0 also keeps UIEngine's fallback limiter disabled.
                fps = 0;
                break;
            case MODE_OPTIMAL:
                // Decoupled updates at 4x display refresh, no hard cap (a 480 cap
                // throttled 144/165/240Hz devices). Every update tick drains the
                // SPSC input queue, so higher update rate = lower input latency.
                fps = (int) (displayRefreshRate * 4);
                break;
            case MODE_UNLIMITED:
            default:
                fps = customFps > 0 ? Math.max(10, customFps) : 0;
                break;
        }
        this.targetFrameNs = fps > 0 ? NS_PER_S / fps : 0;
    }

    /**
     * Blocks the calling thread until the next frame is due.
     *
     * @param startNs System.nanoTime() at frame start.
     * @return Actual elapsed nanoseconds.
     */
    public long limitFrame(long startNs) {
        final long targetNs = this.targetFrameNs;
        if (targetNs <= 0) {
            return System.nanoTime() - startNs;
        }

        waitUntil(startNs + targetNs);
        return System.nanoTime() - startNs;
    }

    /**
     * Blocks the calling thread until the given absolute deadline
     * (System.nanoTime() basis). Returns early if touch input arrives.
     *
     * @return the remaining time at return (usually ~0, negative if aborted).
     */
    public long waitUntil(final long deadlineNs) {
        long remaining = deadlineNs - System.nanoTime();
        if (remaining <= 0) {
            return remaining;
        }

        // Abort immediately if a touch arrived between frames.
        if (touchInterrupted.getAndSet(false)) {
            return deadlineNs - System.nanoTime();
        }

        // Phase 1: coarse sleep in ~1ms chunks (abortable by touch interrupts).
        // Leaves a 2ms margin for the precise phases: Thread.sleep on Android
        // commonly oversleeps by 1-2ms, so sleeping right up to the deadline
        // would jitter. The nanos argument avoids sleep(0) for sub-ms chunks.
        while (remaining > 3_000_000L) {
            try {
                // 1ms chunks: with the 2ms margin above, every chunk is a full
                // millisecond, so sleep(0) truncation cannot occur here.
                Thread.sleep(1);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return deadlineNs - System.nanoTime();
            }
            if (touchInterrupted.getAndSet(false)) {
                return deadlineNs - System.nanoTime();
            }
            remaining = deadlineNs - System.nanoTime();
        }

        // Phase 2: parkNanos in ~200us windows down to the spin threshold.
        while (remaining > SPIN_THRESHOLD_NS) {
            LockSupport.parkNanos(Math.min(remaining - SPIN_THRESHOLD_NS, 200_000L));
            if (touchInterrupted.getAndSet(false)) {
                return deadlineNs - System.nanoTime();
            }
            remaining = deadlineNs - System.nanoTime();
        }

        // Phase 3: busy-spin the final <=150us. A parkNanos wake-up is typically
        // 50-150us late, which would be visible as frame-time noise at high
        // refresh rates; spinning costs negligible CPU for that brief window.
        while ((remaining = deadlineNs - System.nanoTime()) > 0) {
            if (touchInterrupted.get()) {
                touchInterrupted.set(false);
                return remaining;
            }
        }

        return 0;
    }

    /** Returns the target FPS for the current mode. */
    public int getTargetFps() {
        return targetFrameNs > 0 ? (int) (NS_PER_S / targetFrameNs) : 0;
    }

    public int getMode() {
        return mode;
    }

    public float getDisplayRefreshRate() {
        return displayRefreshRate;
    }
}
