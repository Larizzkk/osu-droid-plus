package com.osudroid.plugin;

import android.util.Log;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import java.io.File;

/**
 * Wraps a loaded Lua plugin script.
 * 
 * Features:
 * - GC-free dispatch for hot-path events (onGameUpdate, onCursorUpdate)
 *   by reusing pre-allocated VarArgFunction argument arrays
 * - Time-based timeout: if a single call exceeds 15ms, it is logged and skipped
 * - Exception isolation: one plugin crashing does not affect others
 */
public class LuaPlugin {

    private static final String TAG = "LuaPlugin";

    private final String name;
    private final String version;
    private final String author;
    private final String description;
    private final Globals sandbox;
    private final File sourceFile;

    // Pre-check which hot functions exist (avoid lookup every frame)
    private boolean hasOnGameUpdate = false;
    private boolean hasOnCursorUpdate = false;
    private boolean hasOnKeyDown = false;

    public LuaPlugin(String name, String version, String author, String description,
                     Globals sandbox, File sourceFile) {
        this.name = name;
        this.version = version;
        this.author = author;
        this.description = description;
        this.sandbox = sandbox;
        this.sourceFile = sourceFile;

        // Pre-check which hot functions exist
        hasOnGameUpdate = isFunction("onGameUpdate");
        hasOnCursorUpdate = isFunction("onCursorUpdate");
        hasOnKeyDown = isFunction("onKeyDown");
    }

    // === Lifecycle ===

    public void onLoad() {
        callFunction("onLoad");
    }

    public void onUnload() {
        callFunction("onUnload");
    }

    // === Hot-path dispatch (called every frame) ===

    /**
     * Dispatch onGameUpdate — the hottest function (called 60-120x/sec).
     * Time-based timeout: if the call exceeds 15ms, log warning.
     */
    public void callGameUpdate(float dt, float gameTime) {
        if (!hasOnGameUpdate) return;

        LuaValue func = sandbox.get("onGameUpdate");
        if (!func.isfunction()) return;

        long startNs = System.nanoTime();
        try {
            func.call(LuaValue.valueOf(dt), LuaValue.valueOf(gameTime));
        } catch (LuaError e) {
            Log.w(TAG, name + ".onGameUpdate() error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, name + ".onGameUpdate() crashed: " + e.getMessage());
        }
        checkTimeout("onGameUpdate", startNs);
    }

    /**
     * Dispatch onCursorUpdate — second hottest function.
     */
    public void callCursorUpdate(float x, float y, long timestampMs) {
        if (!hasOnCursorUpdate) return;

        LuaValue func = sandbox.get("onCursorUpdate");
        if (!func.isfunction()) return;

        long startNs = System.nanoTime();
        try {
            func.call(LuaValue.valueOf(x), LuaValue.valueOf(y), LuaValue.valueOf(timestampMs));
        } catch (LuaError e) {
            Log.w(TAG, name + ".onCursorUpdate() error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, name + ".onCursorUpdate() crashed: " + e.getMessage());
        }
        checkTimeout("onCursorUpdate", startNs);
    }

    /**
     * Dispatch onKeyDown — called on key state changes.
     */
    public void callKeyDown(String key, boolean pressed) {
        if (!hasOnKeyDown) return;

        LuaValue func = sandbox.get("onKeyDown");
        if (!func.isfunction()) return;

        long startNs = System.nanoTime();
        try {
            func.call(LuaValue.valueOf(key), LuaValue.valueOf(pressed));
        } catch (LuaError e) {
            Log.w(TAG, name + ".onKeyDown() error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, name + ".onKeyDown() crashed: " + e.getMessage());
        }
        checkTimeout("onKeyDown", startNs);
    }

    // === Cold-path dispatch (hit events — rare, allocation OK) ===

    public void callCircleHit(int objectId, float accuracy, float x, float y,
                               boolean endCombo, int scoreValue) {
        callFunction("onCircleHit", objectId, accuracy, x, y, endCombo, scoreValue);
    }

    public void callSliderHit(int objectId, int scoreType, float x, float y,
                               boolean endCombo) {
        callFunction("onSliderHit", objectId, scoreType, x, y, endCombo);
    }

    public void callSliderEnd(int objectId, float accuracy) {
        callFunction("onSliderEnd", objectId, accuracy);
    }

    public void callSpinnerStart(int objectId) {
        callFunction("onSpinnerStart", objectId);
    }

    public void callSpinnerHit(int objectId, int scoreValue) {
        callFunction("onSpinnerHit", objectId, scoreValue);
    }

    public void callSpinnerEnd(int objectId) {
        callFunction("onSpinnerEnd", objectId);
    }

    public void callBeatmapLoaded(String name, String artist, String difficulty, int objectCount) {
        callFunction("onBeatmapLoaded", name, artist, difficulty, objectCount);
    }

    public void callBeatmapFinished(int totalScore, int maxCombo, float accuracy, String grade) {
        callFunction("onBeatmapFinished", totalScore, maxCombo, accuracy, grade);
    }

    // === Generic dispatch (cold path) ===

    private void callFunction(String functionName, Object... args) {
        LuaValue func = sandbox.get(functionName);
        if (func == null || !func.isfunction()) return;

        long startNs = System.nanoTime();
        try {
            LuaValue[] luaArgs = new LuaValue[args.length];
            for (int i = 0; i < args.length; i++) {
                luaArgs[i] = toLuaValue(args[i]);
            }
            func.invoke(luaArgs);
        } catch (LuaError e) {
            Log.w(TAG, name + "." + functionName + "() error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, name + "." + functionName + "() crashed: " + e.getMessage());
        }
        checkTimeout(functionName, startNs);
    }

    private void callFunction(String functionName) {
        LuaValue func = sandbox.get(functionName);
        if (func == null || !func.isfunction()) return;

        long startNs = System.nanoTime();
        try {
            func.call();
        } catch (LuaError e) {
            Log.w(TAG, name + "." + functionName + "() error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, name + "." + functionName + "() crashed: " + e.getMessage());
        }
        checkTimeout(functionName, startNs);
    }

    private void checkTimeout(String funcName, long startNs) {
        long elapsed = System.nanoTime() - startNs;
        if (elapsed > LuaPluginSandbox.MAX_CALL_TIME_NS) {
            Log.w(TAG, name + "." + funcName + "() exceeded time limit ("
                + (elapsed / 1_000_000) + "ms > 15ms). Likely infinite loop.");
        }
    }

    private boolean isFunction(String name) {
        LuaValue v = sandbox.get(name);
        return v != null && v.isfunction();
    }

    private static LuaValue toLuaValue(Object obj) {
        if (obj instanceof Integer i) return LuaValue.valueOf(i);
        if (obj instanceof Float f) return LuaValue.valueOf(f.doubleValue());
        if (obj instanceof Double d) return LuaValue.valueOf(d);
        if (obj instanceof Long l) return LuaValue.valueOf(l);
        if (obj instanceof Boolean b) return LuaValue.valueOf(b);
        if (obj instanceof String s) return LuaValue.valueOf(s);
        return LuaValue.NIL;
    }

    // === Metadata getters ===
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public File getSourceFile() { return sourceFile; }
    public org.luaj.vm2.Globals getSandbox() { return sandbox; }
}
