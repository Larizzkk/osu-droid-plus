package org.anddev.andengine.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * High-precision frame limiter matching osu!stable's four modes.
 *
 * Two-phase wait: coarse sleep (Thread.sleep) + fine parkNanos.
 * touchInterrupted is an AtomicBoolean to avoid race conditions
 * when signal arrives between checks.
 */
public final class FrameLimiter {

    public static final int MODE_UNLIMITED = 0;
    public static final int MODE_POWER_SAVE = 1;
    public static final int MODE_VSYNC = 2;
    public static final int MODE_OPTIMAL = 3;

    private static final long NS_PER_S = 1_000_000_000L;

    private volatile int mode = MODE_UNLIMITED;
    private volatile int customFps = 0;
    private volatile float displayRefreshRate = 60f;

    private volatile long targetFrameNs = 0;

    /**
     * Atomic flag to abort limitFrame() early on touch input.
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
     * Uses getAndSet to ensure no signal is lost even if limitFrame()
     * is between check-and-reset.
     */
    public void signalTouchInterrupt() {
        this.touchInterrupted.set(true);
    }

    /**
     * Returns true if the swap interval needs re-application.
     * Caller should call markSwapIntervalApplied() after applying.
     */
    public boolean isSwapIntervalDirty() {
        boolean dirty = this.swapIntervalDirty;
        this.swapIntervalDirty = false;
        return dirty;
    }

    public void markSwapIntervalApplied() {
        this.swapIntervalDirty = false;
    }

    private void recomputeTarget() {
        int fps;
        switch (mode) {
            case MODE_POWER_SAVE:
                fps = 30;
                break;
            case MODE_VSYNC:
                fps = (int) displayRefreshRate;
                break;
            case MODE_OPTIMAL:
                fps = Math.min((int) (displayRefreshRate * 4), 480);
                break;
            case MODE_UNLIMITED:
            default:
                fps = customFps > 0 ? customFps : 0;
                break;
        }
        this.targetFrameNs = fps > 0 ? NS_PER_S / fps : 0;
    }

    /**
     * Blocks the calling thread until the next frame is due.
     * Two phases: coarse Thread.sleep + fine parkNanos.
     * Aborted early if touchInterrupted is set.
     *
     * @param startNs System.nanoTime() at frame start.
     * @return Actual elapsed nanoseconds.
     */
    public long limitFrame(long startNs) {
        final long targetNs = this.targetFrameNs;
        if (targetNs <= 0) {
            return System.nanoTime() - startNs;
        }

        // Clear any leftover interrupt from previous frame.
        touchInterrupted.set(false);

        // Phase 1: coarse sleep in chunks (checkable for touch interrupts).
        long remaining = targetNs - (System.nanoTime() - startNs);
        if (remaining > 5_000_000L) {
            try {
                long sleepNs = remaining - 4_000_000L;
                while (sleepNs > 1_000_000L && !touchInterrupted.get()) {
                    long chunk = Math.min(sleepNs, 2_000_000L);
                    Thread.sleep(chunk / 1_000_000L);
                    sleepNs -= chunk;
                    remaining = targetNs - (System.nanoTime() - startNs);
                    if (remaining <= 5_000_000L) break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return System.nanoTime() - startNs;
            }
        }

        // Abort on touch interrupt.
        if (touchInterrupted.getAndSet(false)) {
            return System.nanoTime() - startNs;
        }

        // Phase 2: parkNanos for the fine tail.
        remaining = targetNs - (System.nanoTime() - startNs);
        if (remaining > 100_000L) {
            // Park in small windows so touch interrupts can still abort.
            while (remaining > 100_000L) {
                if (touchInterrupted.getAndSet(false)) {
                    return System.nanoTime() - startNs;
                }
                LockSupport.parkNanos(Math.min(remaining, 500_000L));
                remaining = targetNs - (System.nanoTime() - startNs);
            }
        }

        return System.nanoTime() - startNs;
    }

    /** Returns the target FPS for the current mode. */
    public int getTargetFps() {
        return targetFrameNs > 0 ? (int) (NS_PER_S / targetFrameNs) : 0;
    }

    /** Returns the target frame time in nanoseconds. */
    public long getTargetFrameNs() {
        return targetFrameNs;
    }

    public int getMode() {
        return mode;
    }

    public float getDisplayRefreshRate() {
        return displayRefreshRate;
    }
}
