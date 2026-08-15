package org.anddev.andengine.util;

import java.util.concurrent.locks.LockSupport;

/**
 * High-precision frame limiter matching osu!stable's four modes.
 *
 * Timing uses a hybrid approach:
 *   1. coarse sleep (Thread.sleep) for the bulk of the wait
 *   2. LockSupport.parkNanos for the fine-grained tail
 *   3. Thread.yield() for the final sub-ms precision
 *
 * This avoids Thread.sleep()'s ~15ms Android quantization error
 * and keeps frame times stable even at 240–480+ FPS targets.
 * LockSupport.parkNanos avoids busy-spinning the CPU.
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
     * Set by the update thread when a touch interrupt arrives.
     * limitFrame() checks this to abort sleeping early.
     */
    private volatile boolean touchInterrupted = false;

    private static final FrameLimiter INSTANCE = new FrameLimiter();

    public static FrameLimiter getInstance() {
        return INSTANCE;
    }

    private FrameLimiter() {}

    public void configure(int mode, int customFps, float displayRefreshRate) {
        this.mode = mode;
        this.customFps = customFps;
        this.displayRefreshRate = displayRefreshRate;
        recomputeTarget();
    }

    public void setMode(int mode) {
        this.mode = mode;
        recomputeTarget();
    }

    public void setDisplayRefreshRate(float hz) {
        this.displayRefreshRate = hz;
        recomputeTarget();
    }

    /**
     * Called from the UI thread when a touch event arrives.
     * Causes limitFrame() to abort sleeping early so the update thread
     * processes input with minimal latency.
     */
    public void signalTouchInterrupt() {
        this.touchInterrupted = true;
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
     * Uses a hybrid sleep+parkNanos+yield approach:
     *   1. Thread.sleep for the coarse chunk (~remaining - 5ms)
     *   2. LockSupport.parkNanos for the fine chunk (~remaining - 100us)
     *   3. Thread.yield for the final sub-ms precision
     *
     * If a touch interrupt arrives during sleep, the remaining wait is
     * aborted so the update thread processes input immediately.
     *
     * @param startNs The System.nanoTime() at the start of this frame.
     * @return The actual elapsed time in nanoseconds for this frame.
     */
    public long limitFrame(long startNs) {
        final long targetNs = this.targetFrameNs;
        if (targetNs <= 0) {
            return System.nanoTime() - startNs;
        }

        this.touchInterrupted = false;

        // Phase 1: coarse sleep — sleep in short chunks so we can
        // abort early when touchInterrupted is set.
        long remaining = targetNs - (System.nanoTime() - startNs);
        if (remaining > 5_000_000L) {
            try {
                long sleepNs = remaining - 4_000_000L;
                while (sleepNs > 1_000_000L && !this.touchInterrupted) {
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

        // Early exit on touch interrupt — skip remaining precision phases.
        if (this.touchInterrupted) {
            this.touchInterrupted = false;
            return System.nanoTime() - startNs;
        }

        // Phase 2: parkNanos — precise nanosecond sleep without busy-spinning.
        remaining = targetNs - (System.nanoTime() - startNs);
        if (remaining > 200_000L) {
            LockSupport.parkNanos(remaining - 100_000L);
        }

        // Phase 3: yield — final sub-millisecond precision without burning CPU.
        while (System.nanoTime() - startNs < targetNs) {
            if (this.touchInterrupted) {
                this.touchInterrupted = false;
                break;
            }
            Thread.yield();
        }

        return System.nanoTime() - startNs;
    }

    /**
     * Returns the target FPS for the current mode.
     */
    public int getTargetFps() {
        return targetFrameNs > 0 ? (int) (NS_PER_S / targetFrameNs) : 0;
    }

    /**
     * Returns the target frame time in nanoseconds.
     */
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
