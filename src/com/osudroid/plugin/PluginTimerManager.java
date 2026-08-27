package com.osudroid.plugin;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages timers (setTimeout/setInterval) for Lua plugins.
 * All callbacks run on the main thread to avoid threading issues.
 */
public class PluginTimerManager {

    private static final String TAG = "PluginTimer";
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final AtomicInteger nextId = new AtomicInteger(1);
    private static final Map<Integer, Runnable> timers = new HashMap<>();

    /**
     * Schedule a one-shot timer. Returns a handle for cancellation.
     */
    public static int setTimeout(Globals sandbox, LuaValue func, long delayMs) {
        int id = nextId.getAndIncrement();
        Runnable runnable = () -> {
            try {
                func.call();
            } catch (Exception e) {
                Log.e(TAG, "setTimeout callback error: " + e.getMessage());
            }
            timers.remove(id);
        };
        timers.put(id, runnable);
        mainHandler.postDelayed(runnable, delayMs);
        return id;
    }

    /**
     * Schedule a repeating timer. Returns a handle for cancellation.
     */
    public static int setInterval(Globals sandbox, LuaValue func, long intervalMs) {
        int id = nextId.getAndIncrement();
        Runnable[] runnableRef = new Runnable[1];
        runnableRef[0] = new Runnable() {
            @Override
            public void run() {
                try {
                    func.call();
                } catch (Exception e) {
                    Log.e(TAG, "setInterval callback error: " + e.getMessage());
                }
                // Re-schedule if still in timers map
                if (timers.containsKey(id)) {
                    mainHandler.postDelayed(this, intervalMs);
                }
            }
        };
        timers.put(id, runnableRef[0]);
        mainHandler.postDelayed(runnableRef[0], intervalMs);
        return id;
    }

    /**
     * Cancel a timer by handle.
     */
    public static void clearInterval(int id) {
        Runnable runnable = timers.remove(id);
        if (runnable != null) {
            mainHandler.removeCallbacks(runnable);
        }
    }

    /**
     * Cancel all timers for a plugin (called on unload).
     */
    public static void cancelAll() {
        for (Runnable runnable : timers.values()) {
            mainHandler.removeCallbacks(runnable);
        }
        timers.clear();
    }
}
