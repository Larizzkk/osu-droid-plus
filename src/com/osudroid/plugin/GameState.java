package com.osudroid.plugin;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Bridge between Lua plugins and the game's read-only state.
 * All methods are static and read-only. No methods modify game state.
 * 
 * PluginManager sets the gameScene reference when gameplay starts,
 * and clears it when gameplay ends.
 */
public class GameState {

    private static Object gameScene; // GameScene instance (avoid import to prevent circular deps)
    private static Activity activity;
    private static float elapsedTime = 0;
    private static boolean playing = false;
    private static boolean paused = false;

    // Set by GameScene during gameplay
    public static void setGameScene(Object scene) { gameScene = scene; }
    public static void setActivity(Activity act) { activity = act; }
    public static void setElapsedTime(float t) { elapsedTime = t; }
    public static void setPlaying(boolean p) { playing = p; }
    public static void setPaused(boolean p) { paused = p; }

    public static float getElapsedTime() { return elapsedTime; }
    public static boolean isPlaying() { return playing; }
    public static boolean isPaused() { return paused; }

    // === Key state (filled by GameScene each frame) ===
    private static boolean k1 = false, k2 = false, m1 = false, m2 = false;

    public static void setKeyState(String key, boolean pressed) {
        switch (key) {
            case "k1": k1 = pressed; break;
            case "k2": k2 = pressed; break;
            case "m1": m1 = pressed; break;
            case "m2": m2 = pressed; break;
        }
    }

    public static boolean isKeyDown(String key) {
        switch (key) {
            case "k1": return k1;
            case "k2": return k2;
            case "m1": return m1;
            case "m2": return m2;
            default: return false;
        }
    }

    // === Scoring (read-only, set by GameScene) ===
    private static float accuracy = 0;
    private static int score = 0;
    private static int combo = 0;
    private static int maxCombo = 0;
    private static int objectCount = 0;
    private static int objectsHit = 0;
    private static int misses = 0;
    private static float unstableRate = -1;
    private static float pp = -1;
    private static float ar = 9, cs = 4, od = 8, hp = 6;
    private static float speedMultiplier = 1.0f;
    private static float currentHp = 100f;
    private static float playfieldWidth = 512f, playfieldHeight = 384f;

    // === Cursor position (updated each frame) ===
    private static float cursorX = 0, cursorY = 0;

    // === Beatmap info ===
    private static String beatmapName = "";
    private static String beatmapArtist = "";
    private static String beatmapDifficulty = "";
    private static String beatmapMD5 = "";

    // === Mod info ===
    private static String activeMods = "";

    // === Engine state (read-only, set by GameScene) ===
    private static boolean isKiai = false;
    private static boolean isSliderTracking = false;
    private static boolean isFlashlight = false;
    private static boolean isRelax = false;
    private static boolean isAutoplay = false;
    private static int activeObjectCount = 0;
    private static float breakTimeRemaining = 0;

    public static void setAccuracy(float v) { accuracy = v; }
    public static void setScore(int v) { score = v; }
    public static void setCombo(int v) { combo = v; }
    public static void setMaxCombo(int v) { maxCombo = v; }
    public static void setObjectCount(int v) { objectCount = v; }
    public static void setObjectsHit(int v) { objectsHit = v; }
    public static void setMisses(int v) { misses = v; }
    public static void setUnstableRate(float v) { unstableRate = v; }
    public static void setPP(float v) { pp = v; }
    public static void setAR(float v) { ar = v; }
    public static void setCS(float v) { cs = v; }
    public static void setOD(float v) { od = v; }
    public static void setHP(float v) { hp = v; }
    public static void setSpeedMultiplier(float v) { speedMultiplier = v; }

    public static float getAccuracy() { return accuracy; }
    public static int getScore() { return score; }
    public static int getCombo() { return combo; }
    public static int getMaxCombo() { return maxCombo; }
    public static int getObjectCount() { return objectCount; }
    public static int getObjectsHit() { return objectsHit; }
    public static int getMisses() { return misses; }
    public static float getUnstableRate() { return unstableRate; }
    public static float getPP() { return pp; }
    public static float getAR() { return ar; }
    public static float getCS() { return cs; }
    public static float getOD() { return od; }
    public static float getHP() { return hp; }
    public static float getSpeedMultiplier() { return speedMultiplier; }
    public static float getCurrentHp() { return currentHp; }
    public static void setCurrentHp(float v) { currentHp = v; }
    public static float getPlayfieldWidth() { return playfieldWidth; }
    public static float getPlayfieldHeight() { return playfieldHeight; }
    public static void setPlayfieldSize(float w, float h) { playfieldWidth = w; playfieldHeight = h; }

    // === Cursor position ===
    public static float getCursorX() { return cursorX; }
    public static float getCursorY() { return cursorY; }
    public static void setCursorPosition(float x, float y) { cursorX = x; cursorY = y; }

    // === Beatmap info ===
    public static String getBeatmapName() { return beatmapName; }
    public static String getBeatmapArtist() { return beatmapArtist; }
    public static String getBeatmapDifficulty() { return beatmapDifficulty; }
    public static String getBeatmapMD5() { return beatmapMD5; }
    public static String getActiveMods() { return activeMods; }
    public static void setBeatmapInfo(String name, String artist, String difficulty, String md5) {
        beatmapName = name;
        beatmapArtist = artist;
        beatmapDifficulty = difficulty;
        beatmapMD5 = md5;
    }
    public static void setActiveMods(String mods) { activeMods = mods; }
    public static void setIsKiai(boolean v) { isKiai = v; }
    public static boolean getIsKiai() { return isKiai; }
    public static void setIsSliderTracking(boolean v) { isSliderTracking = v; }
    public static boolean getIsSliderTracking() { return isSliderTracking; }
    public static void setIsFlashlight(boolean v) { isFlashlight = v; }
    public static boolean getIsFlashlight() { return isFlashlight; }
    public static void setIsRelax(boolean v) { isRelax = v; }
    public static boolean getIsRelax() { return isRelax; }
    public static void setIsAutoplay(boolean v) { isAutoplay = v; }
    public static boolean getIsAutoplay() { return isAutoplay; }
    public static void setActiveObjectCount(int v) { activeObjectCount = v; }
    public static int getActiveObjectCount() { return activeObjectCount; }
    public static void setBreakTimeRemaining(float v) { breakTimeRemaining = v; }
    public static float getBreakTimeRemaining() { return breakTimeRemaining; }

    // === Notification ===
    public static void showNotification(String text, double duration) {
        // Delegate to PluginManager which will post to the game's notification system
        PluginManager.getInstance().showNotification(text, duration);
    }

    // === Activity access (for text overlay) ===
    public static Activity getActivity() { return activity; }

    public static void reset() {
        gameScene = null;
        elapsedTime = 0;
        playing = false;
        paused = false;
        accuracy = 0;
        score = 0;
        combo = 0;
        maxCombo = 0;
        objectCount = 0;
        objectsHit = 0;
        misses = 0;
        unstableRate = -1;
        pp = -1;
        k1 = false; k2 = false; m1 = false; m2 = false;
        currentHp = 100f;
        playfieldWidth = 512f; playfieldHeight = 384f;
        cursorX = 0; cursorY = 0;
        beatmapName = ""; beatmapArtist = ""; beatmapDifficulty = ""; beatmapMD5 = "";
        activeMods = "";
        isKiai = false; isSliderTracking = false; isFlashlight = false;
        isRelax = false; isAutoplay = false; activeObjectCount = 0; breakTimeRemaining = 0;
    }
}
