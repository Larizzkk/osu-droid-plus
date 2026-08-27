package com.osudroid.plugin;

import android.util.Log;

import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles onPause/onResume callbacks for Lua plugins.
 */
public class PluginPauseHandler {

    private static final String TAG = "PluginPause";
    private static final Map<LuaPlugin, List<LuaValue>> pauseCallbacks = new HashMap<>();
    private static final Map<LuaPlugin, List<LuaValue>> resumeCallbacks = new HashMap<>();

    public static void registerPauseCallback(LuaPlugin plugin, LuaValue callback) {
        pauseCallbacks.computeIfAbsent(plugin, k -> new ArrayList<>()).add(callback);
    }

    public static void registerResumeCallback(LuaPlugin plugin, LuaValue callback) {
        resumeCallbacks.computeIfAbsent(plugin, k -> new ArrayList<>()).add(callback);
    }

    public static void dispatchPause() {
        for (var entry : pauseCallbacks.entrySet()) {
            for (LuaValue cb : entry.getValue()) {
                try { cb.call(); } catch (Exception e) {
                    Log.w(TAG, "onPause callback error: " + e.getMessage());
                }
            }
        }
    }

    public static void dispatchResume() {
        for (var entry : resumeCallbacks.entrySet()) {
            for (LuaValue cb : entry.getValue()) {
                try { cb.call(); } catch (Exception e) {
                    Log.w(TAG, "onResume callback error: " + e.getMessage());
                }
            }
        }
    }

    public static void clear() {
        pauseCallbacks.clear();
        resumeCallbacks.clear();
    }
}
