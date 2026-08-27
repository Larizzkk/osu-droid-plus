package com.osudroid.plugin;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.anddev.andengine.entity.scene.Scene;
import org.luaj.vm2.Globals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Main plugin manager. Loads .lua files from plugins/ folder,
 * creates sandboxed environments, and dispatches game events to plugins.
 * 
 * Thread safety: all methods are called from the main (game) thread.
 * No synchronization needed.
 */
public class PluginManager {

    private static final String TAG = "PluginManager";
    private static final String PLUGIN_DIR_NAME = "plugins";
    private static java.io.File logFile;

    private static void logToFile(String msg) {
        try {
            if (logFile == null) {
                logFile = new java.io.File("/sdcard/plugin_log.txt");
            }
            java.io.FileWriter w = new java.io.FileWriter(logFile, true);
            w.write(android.text.format.DateFormat.format("HH:mm:ss.SSS", System.currentTimeMillis()).toString());
            w.write(" ");
            w.write(msg);
            w.write("\n");
            w.flush();
            w.close();
        } catch (Exception ignored) {}
    }

    private static PluginManager instance;
    private final List<LuaPlugin> plugins = new ArrayList<>();
    private boolean loaded = false;
    private boolean suppressDialog = false;

    public static PluginManager getInstance() {
        if (instance == null) instance = new PluginManager();
        return instance;
    }

    /**
     * Load all .lua plugins from the plugins/ directory.
     * Call once at app startup, BEFORE any GameScene is created.
     */
    public void loadPlugins(Context context) {
        if (loaded) return;
        loaded = true;

        logToFile("=== Plugin system starting ===");
        logToFile("Internal files dir: " + context.getFilesDir().getAbsolutePath());

        int totalFound = 0;

        // === 1. Internal plugins directory (app storage) ===
        File internalDir = new File(context.getFilesDir(), PLUGIN_DIR_NAME);
        logToFile("Internal plugin dir: " + internalDir.getAbsolutePath() + " (exists=" + internalDir.exists() + ")");
        if (!internalDir.exists()) {
            boolean created = internalDir.mkdirs();
            logToFile("Created internal dir: " + created);
        }
        File[] internalFiles = internalDir.listFiles((d, name) -> name.endsWith(".lua"));
        logToFile("Internal .lua files: " + (internalFiles != null ? internalFiles.length : 0));
        totalFound += loadPluginsFromDir(context, internalDir);

        // === 2. External Downloads/osudroidplus/plugins directory ===
        File externalDir = getExternalPluginDir(context);
        if (externalDir != null) {
            logToFile("External plugin dir: " + externalDir.getAbsolutePath() + " (exists=" + externalDir.exists() + ")");
            if (!externalDir.exists()) {
                boolean created = externalDir.mkdirs();
                logToFile("Created external dir: " + created);
            }
            File[] externalFiles = externalDir.listFiles((d, name) -> name.endsWith(".lua"));
            logToFile("External .lua files: " + (externalFiles != null ? externalFiles.length : 0));
            totalFound += loadPluginsFromDir(context, externalDir);
        } else {
            logToFile("WARNING: External storage not available");
        }

        logToFile("=== Plugin scan complete: " + totalFound + " files found, " + plugins.size() + " loaded ===");

        if (totalFound == 0) {
            logToFile("No plugins found. Place .lua files in:");
            logToFile("  Internal: " + internalDir.getAbsolutePath());
            if (externalDir != null) {
                logToFile("  External: " + externalDir.getAbsolutePath());
            }
        }

        if (!suppressDialog && !plugins.isEmpty() && hasNewPlugins(context)) {
            showPluginDialog();
        }
        suppressDialog = false;
    }

    /**
     * Show a dialog listing all loaded plugins.
     * Uses the game's MessageDialog system (same as beatmap loading dialog).
     */
    private void showPluginDialog() {
        try {
            com.reco1l.osu.ui.MessageDialog dialog = new com.reco1l.osu.ui.MessageDialog();
            dialog.setTitle("Plugins Loaded");

            StringBuilder sb = new StringBuilder();
            sb.append(plugins.size()).append(" plugin(s) loaded:\n\n");
            for (LuaPlugin plugin : plugins) {
                sb.append("• ").append(plugin.getName())
                  .append(" v").append(plugin.getVersion())
                  .append("\n  by ").append(plugin.getAuthor())
                  .append("\n  ").append(plugin.getDescription())
                  .append("\n\n");
            }
            sb.append("Place .lua files in Downloads/osudroidplus/plugins/");

            dialog.setMessage(sb.toString());
            dialog.addButton("OK", android.graphics.Color.WHITE, new kotlin.jvm.functions.Function1<com.reco1l.osu.ui.MessageDialog, kotlin.Unit>() {
                @Override public kotlin.Unit invoke(com.reco1l.osu.ui.MessageDialog d) {
                    d.dismiss();
                    return kotlin.Unit.INSTANCE;
                }
            });
            dialog.setAllowDismiss(true);

            // Show on UI thread (dialog requires fragment transaction)
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> dialog.show());
        } catch (Exception e) {
            Log.e(TAG, "Failed to show plugin dialog", e);
        }
    }

    /**
     * Check if the set of loaded plugins differs from what was previously shown.
     * Compares a hash of all plugin names+versions stored in SharedPreferences.
     */
    private boolean hasNewPlugins(Context context) {
        try {
            StringBuilder sb = new StringBuilder();
            for (LuaPlugin p : plugins) {
                sb.append(p.getName()).append("@").append(p.getVersion()).append(";");
            }
            String currentFingerprint = sb.toString();

            android.content.SharedPreferences prefs = context.getSharedPreferences("plugins", 0);
            String previousFingerprint = prefs.getString("last_fingerprint", "");

            if (!currentFingerprint.equals(previousFingerprint)) {
                prefs.edit().putString("last_fingerprint", currentFingerprint).apply();
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Get the external plugin directory: Downloads/osudroidplus/plugins/
     * Returns null if external storage is not available.
     */
    private File getExternalPluginDir(Context context) {
        try {
            // Try Downloads/osudroidplus/plugins first
            File downloads = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS);
            if (downloads != null) {
                return new File(downloads, "osudroidplus/plugins");
            }

            // Fallback: use context.getExternalFilesDir
            File extDir = context.getExternalFilesDir(null);
            if (extDir != null) {
                return new File(extDir, "osudroidplus/plugins");
            }
        } catch (Exception e) {
            Log.w(TAG, "Cannot access external storage: " + e.getMessage());
        }
        return null;
    }

    /**
     * Load all .lua files from a directory.
     */
    private int loadPluginsFromDir(Context context, File dir) {
        File[] luaFiles = dir.listFiles((d, name) -> name.endsWith(".lua"));
        if (luaFiles == null || luaFiles.length == 0) {
            return 0;
        }

        logToFile("Found " + luaFiles.length + " plugin(s) in " + dir.getName());

        for (File file : luaFiles) {
            try {
                loadSinglePlugin(context, file);
            } catch (Exception e) {
                logToFile("FAILED to load plugin: " + file.getName() + " - " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        return luaFiles.length;
    }

    private void loadSinglePlugin(Context context, File file) {
        logToFile("--- Loading plugin: " + file.getName() + " ---");
        logToFile("  Path: " + file.getAbsolutePath());
        logToFile("  Size: " + file.length() + " bytes");

        try {
            // 1. Pre-read file to extract plugin name for sandbox isolation
            String fileContent = new String(java.nio.file.Files.readAllBytes(file.toPath()), "UTF-8");
            String preName = extractMetaString(fileContent, "name");
            if (preName.isEmpty()) preName = file.getName().replace(".lua", "");
            logToFile("  Pre-read name: " + preName);

            // 2. Create sandboxed Lua environment (with plugin name for file I/O isolation)
            logToFile("  Creating sandbox...");
            Globals sandbox = LuaPluginSandbox.createSandbox(context, preName);
            logToFile("  Sandbox created OK");

            // 3. Execute the .lua file (defines functions + metadata)
            logToFile("  Executing Lua file...");
            sandbox.loadfile(file.getAbsolutePath()).call();
            logToFile("  Lua file executed OK");

            // 4. Read metadata from sandbox globals
            String name = getMetadataString(sandbox, "name", preName);
            String version = getMetadataString(sandbox, "version", "0.0.0");
            String author = getMetadataString(sandbox, "author", "Unknown");
            String description = getMetadataString(sandbox, "description", "");
            logToFile("  Metadata: name=" + name + ", version=" + version + ", author=" + author);

            if (name.isEmpty()) {
                logToFile("  SKIPPED: no 'name' field");
                return;
            }

            // 5. Create plugin wrapper
            LuaPlugin plugin = new LuaPlugin(name, version, author, description, sandbox, file);
            logToFile("  Plugin wrapper created");

            // 6. Check if plugin is enabled (default: true)
            android.content.SharedPreferences prefs = context.getSharedPreferences("plugins", 0);
            boolean enabled = prefs.getBoolean("enabled_" + name, true);
            if (!enabled) {
                logToFile("  ⏸ DISABLED by user — skipping onLoad()");
                return;
            }

            // 7. Call onLoad()
            logToFile("  Calling onLoad()...");
            plugin.onLoad();
            logToFile("  onLoad() completed OK");

            // 8. Register
            plugins.add(plugin);
            PluginRegistry.getInstance().register(plugin);

            logToFile("  ✓ SUCCESS: " + name + " v" + version + " by " + author);
        } catch (Exception e) {
            logToFile("  ✗ FAILED to load " + file.getName() + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            logToFile("  Stack trace: " + sw.toString());
        }
    }

    private static String extractMetaString(String source, String key) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern
                .compile(key + "\\s*=\\s*[\"']([^\"']*)[\"']")
                .matcher(source);
            if (m.find()) return m.group(1).trim();
        } catch (Exception ignored) {}
        return "";
    }

    private String getMetadataString(Globals sandbox, String key, String defaultValue) {
        try {
            org.luaj.vm2.LuaValue val = sandbox.get(key);
            if (val != null && val.isstring()) {
                return val.tojstring();
            }
        } catch (Exception e) {
            // Ignore
        }
        return defaultValue;
    }

    // === EVENT DISPATCH (called by GameScene) ===

    /**
     * Initialize the plugin system for a gameplay session.
     * Creates sprite/text layers and sets game state bridge.
     */
    public void initGameplay(Activity activity, Scene fgScene) {
        GameState.setActivity(activity);
        PluginSpriteManager.init(fgScene);
        PluginTextManager.init(fgScene);
        GameState.setPlaying(true);
    }

    /**
     * Clean up after gameplay ends.
     */
    public void destroyGameplay() {
        PluginTextManager.destroyAll();
        PluginSpriteManager.destroy();
        PluginTimerManager.cancelAll();
        PluginEventHistory.clear();
        PluginEventBus.clear();
        PluginPauseHandler.clear();
        GameState.reset();
    }

    // --- Hit events (cold path — allocation OK) ---

    public void dispatchCircleHit(int objectId, float accuracy, float x, float y,
                                   boolean endCombo, int scoreValue) {
        // Record to event history
        PluginEventHistory.addHitEvent(
            (long) GameState.getElapsedTime() * 1000, objectId, accuracy, x, y, scoreValue, endCombo);
        for (LuaPlugin plugin : plugins) {
            plugin.callCircleHit(objectId, accuracy, x, y, endCombo, scoreValue);
        }
    }

    public void dispatchSliderHit(int objectId, int scoreType, float x, float y,
                                   boolean endCombo) {
        for (LuaPlugin plugin : plugins) {
            plugin.callSliderHit(objectId, scoreType, x, y, endCombo);
        }
    }

    public void dispatchSliderEnd(int objectId, float accuracy) {
        for (LuaPlugin plugin : plugins) {
            plugin.callSliderEnd(objectId, accuracy);
        }
    }

    public void dispatchSpinnerStart(int objectId) {
        for (LuaPlugin plugin : plugins) {
            plugin.callSpinnerStart(objectId);
        }
    }

    public void dispatchSpinnerHit(int objectId, int scoreValue) {
        for (LuaPlugin plugin : plugins) {
            plugin.callSpinnerHit(objectId, scoreValue);
        }
    }

    public void dispatchSpinnerEnd(int objectId) {
        for (LuaPlugin plugin : plugins) {
            plugin.callSpinnerEnd(objectId);
        }
    }

    public void dispatchBeatmapLoaded(String name, String artist, String difficulty, int objectCount) {
        for (LuaPlugin plugin : plugins) {
            plugin.callBeatmapLoaded(name, artist, difficulty, objectCount);
        }
    }

    public void dispatchBeatmapFinished(int totalScore, int maxCombo, float accuracy, String grade) {
        for (LuaPlugin plugin : plugins) {
            plugin.callBeatmapFinished(totalScore, maxCombo, accuracy, grade);
        }
    }

    // --- Hot-path dispatch (GC-free, called every frame) ---

    public void dispatchGameUpdate(float dt, float gameTime) {
        for (LuaPlugin plugin : plugins) {
            plugin.callGameUpdate(dt, gameTime);
        }
    }

    public void dispatchCursorUpdate(float x, float y, long timestampMs) {
        for (LuaPlugin plugin : plugins) {
            plugin.callCursorUpdate(x, y, timestampMs);
        }
    }

    public void dispatchKeyDown(String key, boolean pressed) {
        for (LuaPlugin plugin : plugins) {
            plugin.callKeyDown(key, pressed);
        }
    }

    // === Notification bridge ===

    public void showNotification(String text, double duration) {
        try {
            ru.nsu.ccfit.zuev.osu.ToastLogger.showText("[Plugin] " + text, false);
        } catch (Exception e) {
            Log.i("Plugin", "Notification: " + text);
        }
    }

    // === Accessors ===

    public List<LuaPlugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    public boolean hasPlugins() {
        return !plugins.isEmpty();
    }

    public LuaPlugin findPluginBySandbox(org.luaj.vm2.Globals sandbox) {
        for (LuaPlugin p : plugins) {
            if (p.getSandbox() == sandbox) return p;
        }
        return null;
    }

    /**
     * Unload all plugins. Called when app is closing.
     */
    public void unloadAll() {
        for (LuaPlugin plugin : plugins) {
            try {
                plugin.onUnload();
            } catch (Exception e) {
                Log.e(TAG, "Error unloading plugin: " + plugin.getName(), e);
            }
        }
        plugins.clear();
        PluginRegistry.getInstance().clear();
        PluginEventBus.clear();
        PluginPauseHandler.clear();
        loaded = false;
    }

    /**
     * Force-reload all plugins. Called after enable/disable toggle.
     */
    public void reloadPlugins(Context context) {
        unloadAll();
        suppressDialog = true;
        loadPlugins(context);
    }
}
