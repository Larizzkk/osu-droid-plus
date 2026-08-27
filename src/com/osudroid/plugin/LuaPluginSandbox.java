package com.osudroid.plugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

/**
 * Creates a sandboxed LuaJ 3.0.1 environment for each plugin.
 * 
 * Security:
 * - Removes io, os, debug, package, loadlib libraries
 * - Blocks rawget, rawset, setmetatable, getmetatable
 * - Time-based timeout: if a single plugin function call exceeds 15ms, it is logged
 * - Injects safe game API functions
 */
public class LuaPluginSandbox {

    private static final String TAG = "LuaSandbox";
    static final long MAX_CALL_TIME_NS = 15_000_000L; // 15ms in nanoseconds

    /**
     * Creates a sandboxed Lua environment for a single plugin.
     * @param pluginName used to isolate file I/O per plugin
     */
    public static Globals createSandbox(Context gameContext, String pluginName) {
        Log.d(TAG, "Creating sandbox for plugin: " + pluginName);
        Globals globals = JsePlatform.standardGlobals();
        Log.d(TAG, "Sandbox created with standard libraries");

        // === REMOVE DANGEROUS LIBRARIES ===
        globals.set("io", LuaValue.NIL);
        globals.set("os", LuaValue.NIL);
        globals.set("debug", LuaValue.NIL);
        globals.set("package", LuaValue.NIL);
        globals.set("loadlib", LuaValue.NIL);
        globals.set("collectgarbage", LuaValue.NIL);
        globals.set("rawget", LuaValue.NIL);
        globals.set("rawset", LuaValue.NIL);
        globals.set("rawequal", LuaValue.NIL);
        globals.set("rawlen", LuaValue.NIL);
        globals.set("setmetatable", LuaValue.NIL);
        globals.set("getmetatable", LuaValue.NIL);
        globals.set("newproxy", LuaValue.NIL);
        globals.set("module", LuaValue.NIL);
        globals.set("require", LuaValue.NIL);

        // === INJECT SAFE GAME API ===
        injectGameAPI(globals, gameContext, pluginName);

        return globals;
    }

    /**
     * Backward-compatible overload.
     */
    public static Globals createSandbox(Context gameContext) {
        return createSandbox(gameContext, "plugin");
    }

    private static void injectGameAPI(Globals globals, Context ctx, String pluginName) {
        SharedPreferences prefs = ctx.getSharedPreferences("game", 0);

        // === UTILITY ===
        globals.set("log", new OneArgFunction() {
            public LuaValue call(LuaValue msg) {
                Log.d("Plugin", msg.tojstring());
                return LuaValue.NIL;
            }
        });

        // === SETTINGS (read-only) ===
        globals.set("getSetting", new OneArgFunction() {
            public LuaValue call(LuaValue key) {
                String k = key.tojstring();
                if (prefs.contains(k)) {
                    String val = prefs.getString(k, "");
                    try { return LuaValue.valueOf(Double.parseDouble(val)); }
                    catch (NumberFormatException e) { return LuaValue.valueOf(val); }
                }
                return LuaValue.NIL;
            }
        });

        // === GAME STATE ===
        globals.set("getGameTime", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getElapsedTime()); }
        });
        globals.set("isGamePlaying", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.isPlaying()); }
        });
        globals.set("isPaused", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.isPaused()); }
        });

        // === NOTIFICATIONS ===
        globals.set("showNotification", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                String text = args.narg() > 0 ? args.arg(1).tojstring() : "";
                double duration = args.narg() > 1 ? args.arg(2).todouble() : 2.0;
                GameState.showNotification(text, duration);
                return LuaValue.NIL;
            }
        });

        // === SPRITES ===
        globals.set("addSprite", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                String tex = args.narg() > 0 ? args.arg(1).tojstring() : "cursor";
                float x = args.narg() > 1 ? (float) args.arg(2).todouble() : 0;
                float y = args.narg() > 2 ? (float) args.arg(3).todouble() : 0;
                return LuaValue.valueOf(PluginSpriteManager.addSprite(tex, x, y));
            }
        });
        globals.set("moveSprite", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                PluginSpriteManager.moveSprite(args.arg(1).toint(),
                    (float) args.arg(2).todouble(), (float) args.arg(3).todouble());
                return LuaValue.NIL;
            }
        });
        globals.set("scaleSprite", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                PluginSpriteManager.scaleSprite(args.arg(1).toint(), (float) args.arg(2).todouble());
                return LuaValue.NIL;
            }
        });
        globals.set("setSpriteAlpha", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                PluginSpriteManager.setSpriteAlpha(args.arg(1).toint(), (float) args.arg(2).todouble());
                return LuaValue.NIL;
            }
        });
        globals.set("destroySprite", new OneArgFunction() {
            public LuaValue call(LuaValue handle) {
                PluginSpriteManager.destroySprite(handle.toint());
                return LuaValue.NIL;
            }
        });

        // === TEXT RENDERING ===
        globals.set("createText", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                String text = args.narg() > 0 ? args.arg(1).tojstring() : "";
                float x = args.narg() > 1 ? (float) args.arg(2).todouble() : 0;
                float y = args.narg() > 2 ? (float) args.arg(3).todouble() : 0;
                float size = args.narg() > 3 ? (float) args.arg(4).todouble() : 24;
                float r = 1, g = 1, b = 1, a = 1;
                if (args.narg() > 4 && args.arg(5).istable()) {
                    LuaTable color = args.arg(5).checktable();
                    r = (float) color.get(1).todouble();
                    g = (float) color.get(2).todouble();
                    b = (float) color.get(3).todouble();
                    a = (float) color.get(4).todouble();
                }
                return LuaValue.valueOf(PluginTextManager.createText(text, x, y, size, r, g, b, a));
            }
        });
        globals.set("updateText", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                PluginTextManager.updateText(args.arg(1).toint(), args.arg(2).tojstring());
                return LuaValue.NIL;
            }
        });
        globals.set("moveText", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                PluginTextManager.moveText(args.arg(1).toint(),
                    (float) args.arg(2).todouble(), (float) args.arg(3).todouble());
                return LuaValue.NIL;
            }
        });
        globals.set("setTextColor", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                LuaTable t = args.arg(2).checktable();
                PluginTextManager.setTextColor(args.arg(1).toint(),
                    (float) t.get(1).todouble(), (float) t.get(2).todouble(),
                    (float) t.get(3).todouble(), (float) t.get(4).todouble());
                return LuaValue.NIL;
            }
        });
        globals.set("setTextSize", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                PluginTextManager.setTextSize(args.arg(1).toint(), (float) args.arg(2).todouble());
                return LuaValue.NIL;
            }
        });
        globals.set("setTextAlignment", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                PluginTextManager.setTextAlignment(args.arg(1).toint(), args.arg(2).tojstring());
                return LuaValue.NIL;
            }
        });
        globals.set("setTextOutline", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                LuaTable t = args.arg(2).checktable();
                PluginTextManager.setTextOutline(args.arg(1).toint(),
                    (float) t.get(1).todouble(), (float) t.get(2).todouble(),
                    (float) t.get(3).todouble(), (float) t.get(4).todouble(),
                    (float) args.arg(3).todouble());
                return LuaValue.NIL;
            }
        });
        globals.set("setTextAlpha", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                PluginTextManager.setTextAlpha(args.arg(1).toint(), (float) args.arg(2).todouble());
                return LuaValue.NIL;
            }
        });
        globals.set("destroyText", new OneArgFunction() {
            public LuaValue call(LuaValue handle) {
                PluginTextManager.destroyText(handle.toint());
                return LuaValue.NIL;
            }
        });

        // === KEY INPUT ===
        globals.set("isKeyDown", new OneArgFunction() {
            public LuaValue call(LuaValue key) {
                return LuaValue.valueOf(GameState.isKeyDown(key.tojstring()));
            }
        });

        // === ACCURACY / SCORING ===
        globals.set("getAccuracy", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getAccuracy()); }
        });
        globals.set("getScore", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getScore()); }
        });
        globals.set("getCombo", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getCombo()); }
        });
        globals.set("getMaxCombo", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getMaxCombo()); }
        });
        globals.set("getObjectCount", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getObjectCount()); }
        });
        globals.set("getObjectsHit", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getObjectsHit()); }
        });
        globals.set("getMisses", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getMisses()); }
        });
        globals.set("getUnstableRate", new ZeroArgFunction() {
            public LuaValue call() {
                float ur = GameState.getUnstableRate();
                return ur >= 0 ? LuaValue.valueOf(ur) : LuaValue.NIL;
            }
        });
        globals.set("getPP", new ZeroArgFunction() {
            public LuaValue call() {
                float pp = GameState.getPP();
                return pp >= 0 ? LuaValue.valueOf(pp) : LuaValue.NIL;
            }
        });
        globals.set("getAR", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getAR()); }
        });
        globals.set("getCS", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getCS()); }
        });
        globals.set("getOD", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getOD()); }
        });
        globals.set("getHP", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getHP()); }
        });
        globals.set("getSpeedMultiplier", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getSpeedMultiplier()); }
        });

        // === MATH HELPERS ===
        globals.set("random", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                int a = args.narg() > 0 ? args.arg(1).toint() : 0;
                int b = args.narg() > 1 ? args.arg(2).toint() : 1;
                return LuaValue.valueOf(a + (int)(Math.random() * (b - a + 1)));
            }
        });

        // === TIMERS ===

        // setTimeout(func, delayMs) -> handle
        globals.set("setTimeout", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                LuaValue func = args.arg(1);
                long delay = args.narg() > 1 ? (long) args.arg(2).todouble() : 0;
                return LuaValue.valueOf(PluginTimerManager.setTimeout(globals, func, delay));
            }
        });

        // setInterval(func, intervalMs) -> handle
        globals.set("setInterval", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                LuaValue func = args.arg(1);
                long interval = args.narg() > 1 ? (long) args.arg(2).todouble() : 1000;
                return LuaValue.valueOf(PluginTimerManager.setInterval(globals, func, interval));
            }
        });

        // clearInterval(handle)
        globals.set("clearInterval", new OneArgFunction() {
            public LuaValue call(LuaValue handle) {
                PluginTimerManager.clearInterval(handle.toint());
                return LuaValue.NIL;
            }
        });

        // clearTimeout(handle) — alias for clearInterval
        globals.set("clearTimeout", new OneArgFunction() {
            public LuaValue call(LuaValue handle) {
                PluginTimerManager.clearInterval(handle.toint());
                return LuaValue.NIL;
            }
        });

        // === FILE I/O (sandboxed to plugin's own directory) ===

        globals.set("fileWrite", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                String filename = args.arg(1).tojstring();
                String content = args.arg(2).tojstring();
                boolean ok = PluginFileIO.writeFile(ctx, pluginName, filename, content);
                return LuaValue.valueOf(ok);
            }
        });

        globals.set("fileRead", new OneArgFunction() {
            public LuaValue call(LuaValue filename) {
                String content = PluginFileIO.readFile(ctx, pluginName, filename.tojstring());
                return content != null ? LuaValue.valueOf(content) : LuaValue.NIL;
            }
        });

        globals.set("fileExists", new OneArgFunction() {
            public LuaValue call(LuaValue filename) {
                return LuaValue.valueOf(PluginFileIO.fileExists(ctx, pluginName, filename.tojstring()));
            }
        });

        globals.set("fileDelete", new OneArgFunction() {
            public LuaValue call(LuaValue filename) {
                return LuaValue.valueOf(PluginFileIO.deleteFile(ctx, pluginName, filename.tojstring()));
            }
        });

        globals.set("fileList", new ZeroArgFunction() {
            public LuaValue call() {
                String list = PluginFileIO.listFiles(ctx, pluginName);
                return LuaValue.valueOf(list);
            }
        });

        // === HTTP (async) ===

        // httpGet(url, callback) — callback(url, status, body)
        globals.set("httpGet", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                String url = args.arg(1).tojstring();
                LuaValue callback = args.narg() > 1 ? args.arg(2) : LuaValue.NIL;
                PluginHttpClient.get(url, globals, callback);
                return LuaValue.NIL;
            }
        });

        // httpPost(url, body, callback) — callback(url, status, body)
        globals.set("httpPost", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                String url = args.arg(1).tojstring();
                String body = args.narg() > 1 ? args.arg(2).tojstring() : "";
                LuaValue callback = args.narg() > 2 ? args.arg(3) : LuaValue.NIL;
                PluginHttpClient.post(url, body, globals, callback);
                return LuaValue.NIL;
            }
        });

        // === AUDIO ===

        globals.set("playSound", new OneArgFunction() {
            public LuaValue call(LuaValue soundName) {
                try {
                    ru.nsu.ccfit.zuev.osuplusplus.ResourceManager.getInstance()
                        .getSound(soundName.tojstring()).play();
                } catch (Exception e) {
                    // Sound not found, ignore
                }
                return LuaValue.NIL;
            }
        });

        globals.set("playCustomSound", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                try {
                    ru.nsu.ccfit.zuev.osuplusplus.ResourceManager.getInstance()
                        .getCustomSound(args.arg(1).tojstring(), args.narg() > 1 ? args.arg(2).toint() : 1)
                        .play();
                } catch (Exception e) {
                    // Sound not found, ignore
                }
                return LuaValue.NIL;
            }
        });        // === MORE GAME STATE ===

        globals.set("getCurrentHp", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getCurrentHp()); }
        });

        globals.set("getCursorX", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getCursorX()); }
        });
        globals.set("getCursorY", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getCursorY()); }
        });

        globals.set("getBeatmapName", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getBeatmapName()); }
        });
        globals.set("getBeatmapArtist", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getBeatmapArtist()); }
        });
        globals.set("getBeatmapDifficulty", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getBeatmapDifficulty()); }
        });
        globals.set("getActiveMods", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getActiveMods()); }
        });

        globals.set("getHitCount", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(PluginEventHistory.getHitEventCount());
            }
        });

        globals.set("getTimingErrors", new ZeroArgFunction() {
            public LuaValue call() {
                float[] errors = PluginEventHistory.getTimingErrors();
                LuaTable t = new LuaTable();
                for (int i = 0; i < errors.length; i++) {
                    t.set(i + 1, LuaValue.valueOf(errors[i]));
                }
                return t;
            }
        });

        globals.set("getRecentHits", new OneArgFunction() {
            public LuaValue call(LuaValue count) {
                var hits = PluginEventHistory.getRecentHits(count.toint());
                LuaTable t = new LuaTable();
                for (int i = 0; i < hits.size(); i++) {
                    var h = hits.get(i);
                    LuaTable entry = new LuaTable();
                    entry.set("time", LuaValue.valueOf(h.timeMs));
                    entry.set("objectId", LuaValue.valueOf(h.objectId));
                    entry.set("accuracy", LuaValue.valueOf(h.accuracy));
                    entry.set("x", LuaValue.valueOf(h.x));
                    entry.set("y", LuaValue.valueOf(h.y));
                    entry.set("score", LuaValue.valueOf(h.scoreValue));
                    entry.set("endCombo", LuaValue.valueOf(h.endCombo));
                    t.set(i + 1, entry);
                }
                return t;
            }
        });

        // === SCREEN / PLAYFIELD SIZE ===
        globals.set("getScreenSize", new ZeroArgFunction() {
            public LuaValue call() {
                LuaTable t = new LuaTable();
                t.set("width", LuaValue.valueOf(ru.nsu.ccfit.zuev.osuplusplus.Config.getRES_WIDTH()));
                t.set("height", LuaValue.valueOf(ru.nsu.ccfit.zuev.osuplusplus.Config.getRES_HEIGHT()));
                return t;
            }
        });

        globals.set("getPlayfieldSize", new ZeroArgFunction() {
            public LuaValue call() {
                float pw = GameState.getPlayfieldWidth();
                float ph = GameState.getPlayfieldHeight();
                LuaTable t = new LuaTable();
                t.set("width", LuaValue.valueOf(pw));
                t.set("height", LuaValue.valueOf(ph));
                return t;
            }
        });

        // === MISSING SPRITE APIs ===
        globals.set("setSpriteColor", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                int handle = args.arg(1).toint();
                float r = (float) args.arg(2).todouble();
                float g = (float) args.arg(3).todouble();
                float b = (float) args.arg(4).todouble();
                float a = args.narg() > 4 ? (float) args.arg(5).todouble() : 1.0f;
                PluginSpriteManager.setSpriteColor(handle, r, g, b, a);
                return LuaValue.NIL;
            }
        });
        globals.set("setSpriteRotation", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                PluginSpriteManager.setSpriteRotation(args.arg(1).toint(),
                    (float) args.arg(2).todouble());
                return LuaValue.NIL;
            }
        });
        globals.set("setSpriteVisible", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                PluginSpriteManager.setSpriteVisible(args.arg(1).toint(),
                    args.arg(2).toboolean());
                return LuaValue.NIL;
            }
        });
        globals.set("setSpriteZIndex", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                PluginSpriteManager.setSpriteZIndex(args.arg(1).toint(),
                    args.arg(2).toint());
                return LuaValue.NIL;
            }
        });

        // === MISSING TEXT APIs ===
        globals.set("setTextVisible", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                PluginTextManager.setTextVisible(args.arg(1).toint(),
                    args.arg(2).toboolean());
                return LuaValue.NIL;
            }
        });

        // === INTER-PLUGIN EVENT BUS ===
        globals.set("emit", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                String eventName = args.narg() > 0 ? args.arg(1).tojstring() : "";
                LuaValue[] extra = new LuaValue[Math.max(0, args.narg() - 1)];
                for (int i = 0; i < extra.length; i++) {
                    extra[i] = args.arg(i + 2);
                }
                PluginEventBus.emit(eventName, extra);
                return LuaValue.NIL;
            }
        });
        globals.set("on", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                String eventName = args.narg() > 0 ? args.arg(1).tojstring() : "";
                LuaValue callback = args.narg() > 1 ? args.arg(2) : LuaValue.NIL;
                // Find the plugin that owns this sandbox
                LuaPlugin owner = PluginManager.getInstance().findPluginBySandbox(globals);
                if (owner != null) {
                    PluginEventBus.on(owner, globals, eventName, callback);
                }
                return LuaValue.NIL;
            }
        });
        globals.set("off", new OneArgFunction() {
            public LuaValue call(LuaValue eventName) {
                LuaPlugin owner = PluginManager.getInstance().findPluginBySandbox(globals);
                if (owner != null) {
                    PluginEventBus.off(owner, eventName.tojstring());
                }
                return LuaValue.NIL;
            }
        });

        // === PAUSE/RESUME ===
        globals.set("onPause", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                LuaValue func = args.narg() > 0 ? args.arg(1) : LuaValue.NIL;
                LuaPlugin owner = PluginManager.getInstance().findPluginBySandbox(globals);
                if (owner != null && func.isfunction()) {
                    PluginPauseHandler.registerPauseCallback(owner, func);
                }
                return LuaValue.NIL;
            }
        });
        globals.set("onResume", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                LuaValue func = args.narg() > 0 ? args.arg(1) : LuaValue.NIL;
                LuaPlugin owner = PluginManager.getInstance().findPluginBySandbox(globals);
                if (owner != null && func.isfunction()) {
                    PluginPauseHandler.registerResumeCallback(owner, func);
                }
                return LuaValue.NIL;
            }
        });

        // === MODS INFO ===
        globals.set("getMods", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(GameState.getActiveMods());
            }
        });

        // === ENGINE STATE ===
        globals.set("isKiai", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getIsKiai()); }
        });
        globals.set("isSliderTracking", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getIsSliderTracking()); }
        });
        globals.set("isFlashlight", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getIsFlashlight()); }
        });
        globals.set("isRelax", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getIsRelax()); }
        });
        globals.set("isAutoplay", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getIsAutoplay()); }
        });
        globals.set("getActiveObjectCount", new ZeroArgFunction() {
            public LuaValue call() { return LuaValue.valueOf(GameState.getActiveObjectCount()); }
        });

        // === MATH UTILITIES ===
        globals.set("lerp", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                double a = args.narg() > 0 ? args.arg(1).todouble() : 0;
                double b = args.narg() > 1 ? args.arg(2).todouble() : 1;
                double t = args.narg() > 2 ? args.arg(3).todouble() : 0.5;
                return LuaValue.valueOf(a + (b - a) * t);
            }
        });
        globals.set("clamp", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                double v = args.narg() > 0 ? args.arg(1).todouble() : 0;
                double lo = args.narg() > 1 ? args.arg(2).todouble() : 0;
                double hi = args.narg() > 2 ? args.arg(3).todouble() : 1;
                return LuaValue.valueOf(Math.max(lo, Math.min(hi, v)));
            }
        });
        globals.set("distance", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                double x1 = args.narg() > 0 ? args.arg(1).todouble() : 0;
                double y1 = args.narg() > 1 ? args.arg(2).todouble() : 0;
                double x2 = args.narg() > 2 ? args.arg(3).todouble() : 0;
                double y2 = args.narg() > 3 ? args.arg(4).todouble() : 0;
                return LuaValue.valueOf(Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1)));
            }
        });
        globals.set("angle", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                double x1 = args.narg() > 0 ? args.arg(1).todouble() : 0;
                double y1 = args.narg() > 1 ? args.arg(2).todouble() : 0;
                double x2 = args.narg() > 2 ? args.arg(3).todouble() : 0;
                double y2 = args.narg() > 3 ? args.arg(4).todouble() : 0;
                return LuaValue.valueOf(Math.toDegrees(Math.atan2(y2-y1, x2-x1)));
            }
        });
        globals.set("smoothstep", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                double edge0 = args.narg() > 0 ? args.arg(1).todouble() : 0;
                double edge1 = args.narg() > 1 ? args.arg(2).todouble() : 1;
                double x = args.narg() > 2 ? args.arg(3).todouble() : 0.5;
                double t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
                return LuaValue.valueOf(t * t * (3 - 2 * t));
            }
        });
        globals.set("map", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                double v = args.narg() > 0 ? args.arg(1).todouble() : 0;
                double inMin = args.narg() > 1 ? args.arg(2).todouble() : 0;
                double inMax = args.narg() > 2 ? args.arg(3).todouble() : 1;
                double outMin = args.narg() > 3 ? args.arg(4).todouble() : 0;
                double outMax = args.narg() > 4 ? args.arg(5).todouble() : 1;
                double t = (v - inMin) / (inMax - inMin);
                return LuaValue.valueOf(outMin + (outMax - outMin) * t);
            }
        });

        // === TABLE UTILITIES ===
        globals.set("tableSize", new OneArgFunction() {
            public LuaValue call(LuaValue t) {
                if (!t.istable()) return LuaValue.valueOf(0);
                return LuaValue.valueOf(t.checktable().length());
            }
        });
        globals.set("tableKeys", new OneArgFunction() {
            public LuaValue call(LuaValue t) {
                if (!t.istable()) return new LuaTable();
                LuaTable src = t.checktable();
                LuaTable result = new LuaTable();
                int idx = 1;
                int max = src.length();
                for (int i = 1; i <= max; i++) {
                    if (!src.get(i).isnil()) {
                        result.set(idx++, src.get(i));
                    }
                }
                return result;
            }
        });

        // === STRING HELPERS ===
        globals.set("formatNumber", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
                double num = args.narg() > 0 ? args.arg(1).todouble() : 0;
                int decimals = args.narg() > 1 ? args.arg(2).toint() : 0;
                String fmt = "%." + decimals + "f";
                return LuaValue.valueOf(String.format(java.util.Locale.US, fmt, num));
            }
        });
    }
}
