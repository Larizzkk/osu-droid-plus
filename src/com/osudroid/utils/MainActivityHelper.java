package com.osudroid.utils;

import ru.nsu.ccfit.zuev.osuplusplus.GlobalManager;

/**
 * Bridge class to break the circular dependency between Java (MainActivity) and Kotlin code.
 * <p>
 * MainActivity.java imports Kotlin classes (OverlayFPSCounter), which creates a dependency cycle:
 * Java MainActivity -> Kotlin OverlayFPSCounter -> Kotlin FPSCounter -> Java GlobalManager -> Java MainActivity.
 * The Kotlin compiler can't fully resolve MainActivity's type in this cycle, so methods called
 * on it from Kotlin files appear as "Unresolved reference".
 * <p>
 * This helper provides static access to those methods without creating a circular dependency.
 */
public final class MainActivityHelper {

    private MainActivityHelper() {
        // Utility class
    }

    /**
     * Delegates to {@link ru.nsu.ccfit.zuev.osuplusplus.MainActivity#loadBeatmapLibrary()}.
     */
    public static void loadBeatmapLibrary() {
        GlobalManager.getInstance().getMainActivity().loadBeatmapLibrary();
    }

    /**
     * Delegates to {@link ru.nsu.ccfit.zuev.osuplusplus.MainActivity#checkNewSkins()}.
     */
    public static void checkNewSkins() {
        GlobalManager.getInstance().getMainActivity().checkNewSkins();
    }

    /**
     * Delegates to {@link ru.nsu.ccfit.zuev.osuplusplus.MainActivity#forcedExit()}.
     */
    public static void forcedExit() {
        GlobalManager.getInstance().getMainActivity().forcedExit();
    }

    /**
     * Delegates to {@link ru.nsu.ccfit.zuev.osuplusplus.MainActivity#getVersionCode()}.
     */
    public static long getVersionCode() {
        return GlobalManager.getInstance().getMainActivity().getVersionCode();
    }

    /**
     * Delegates to {@link ru.nsu.ccfit.zuev.osuplusplus.MainActivity#getRefreshRate()}.
     */
    public static float getRefreshRate() {
        return GlobalManager.getInstance().getMainActivity().getRefreshRate();
    }
}
