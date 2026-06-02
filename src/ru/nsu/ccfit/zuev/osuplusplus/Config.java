package ru.nsu.ccfit.zuev.osuplusplus;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.firebase.messaging.FirebaseMessaging;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.osudroid.multiplayer.Multiplayer;
import com.reco1l.framework.Color4;
import com.reco1l.framework.HexComposition;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import org.anddev.andengine.util.Debug;

import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.scoring.BeatmapLeaderboardScoringMode;

public class Config {
    private static String corePath,
        defaultCorePath,
        beatmapPath,
        cachePath,
        skinPath,
        skinTopPath,
        scorePath,
        onlineUsername,
        onlinePassword,
        onlineDeviceID;

    private static boolean DELETE_OSZ,
        SCAN_DOWNLOAD,
        deleteUnimportedBeatmaps,
        showFirstApproachCircle,
        comboburst,
        enableStoryboard,
        safeBeatmapBg,
        useNightcoreOnMultiplayer,
        videoEnabled,
        deleteUnsupportedVideos,
        submitScoreOnMultiplayer,
        preferModAcronymInMultiplayer,
        keepBackgroundAspectRatio,
        noChangeDimInBreaks,
        dimHitObjects,
        forceMaxRefreshRate,
        shiftPitchInRateChange,
        useCustomSkins,
        useCustomSounds,
        corovans,
        showFPS,
        animateFollowCircle,
        animateComboText,
        snakingInSliders,
        snakingOutSliders,
        playMusicPreview,
        showCursor,
        enableExtension,
        loadAvatar,
        stayOnline,
        burstEffects,
        hitLighting,
        useParticles,
        useCustomComboColors,
        forceRomanized,
        fixFrameOffset,
        removeSliderLock,
        displayScoreStatistics,
        hideReplayMarquee,
        hideInGameUI,
        receiveAnnouncements;

    private static int RES_WIDTH,
        RES_HEIGHT,
        spinnerStyle,
        metronomeSwitch,
        minimumGameplaySynchronizationTime,
        backButtonPressTime;

    private static float soundVolume,
        bgmVolume,
        offset,
        backgroundBrightness,
        playfieldSize,
        playfieldHorizontalPosition,
        playfieldVerticalPosition,
        cursorSize,
        trailLength;

    private static Map<String, String> skins;

    private static Color4[] comboColors;
    private static Context context;

    public static final Set<String> SENSITIVE_KEYS = Set.of(
        "installID",
        "onlineUsername",
        "onlinePassword",
        "starRatingVersion",
        "version"
    );

    /**
     * The shared preferences of the application.
     */
    private static SharedPreferences sharedPreferences;


    public static void loadConfig(final Context context) {
        Config.context = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        final SharedPreferences prefs = sharedPreferences;
        // graphics
        useCustomSkins = prefs.getBoolean("skin", false);
        useCustomSounds = prefs.getBoolean("beatmapSounds", true);
        comboburst = prefs.getBoolean("comboburst", false);
        corovans = prefs.getBoolean("images", false);
        showFPS = prefs.getBoolean("fps", true);
        spinnerStyle = Integer.parseInt(prefs.getString("spinnerstyle", "1"));
        showFirstApproachCircle = prefs.getBoolean("showfirstapproachcircle", false);
        metronomeSwitch = Integer.parseInt(prefs.getString("metronomeswitch", "1"));
        enableStoryboard = prefs.getBoolean("enableStoryboard", false);
        videoEnabled = prefs.getBoolean("enableVideo", false);
        keepBackgroundAspectRatio = prefs.getBoolean("keepBackgroundAspectRatio", false);
        noChangeDimInBreaks = prefs.getBoolean("noChangeDimInBreaks", false);
        dimHitObjects = prefs.getBoolean("dimHitObjects", true);
        forceMaxRefreshRate = prefs.getBoolean("forceMaxRefreshRate", false);

        measureDisplaySize();
        setPlayfieldSize(prefs.getInt("playfieldSize", 100) / 100f);
        playfieldHorizontalPosition = prefs.getInt("playfieldHorizontalPosition", 50) / 100f;
        playfieldVerticalPosition = prefs.getInt("playfieldVerticalPosition", 50) / 100f;

        animateFollowCircle = prefs.getBoolean("animateFollowCircle", true);
        animateComboText = prefs.getBoolean("animateComboText", true);
        snakingInSliders = prefs.getBoolean("snakingInSliders", true);
        snakingOutSliders = prefs.getBoolean("snakingOutSliders", true);

        try {
            offset = prefs.getInt("offset", 0);
            backgroundBrightness = prefs.getInt("bgbrightness", 25) / 100f;
            soundVolume = prefs.getInt("soundvolume", 100) / 100f;
            bgmVolume = prefs.getInt("bgmvolume", 100) / 100f;
            cursorSize = prefs.getInt("cursorSize", 50) / 100f;
            trailLength = prefs.getInt("trailLength", 200) / 100f;
        }catch(RuntimeException e) { // use valid integer since this makes the game crash on android m
            prefs.edit()
                .putInt("offset", 0)
                .putInt("bgbrightness", 25)
                .putInt("soundvolume", 100)
                .putInt("bgmvolume", 100)
                .putInt("cursorSize", 50)
                .putInt("trailLength", 200)
                .commit();
            Config.loadConfig(context);
            return;
        }

        //advanced
        defaultCorePath = Environment.getExternalStorageDirectory() + "/osu!droid/";
        corePath = prefs.getString("corePath", defaultCorePath);
        if (corePath.length() == 0) {
            corePath = defaultCorePath;
        }
        if (corePath.charAt(corePath.length() - 1) != '/') {
            corePath += "/";
        }
        scorePath = corePath + "Scores/";

        skinPath = prefs.getString("skinPath", corePath + "Skin/");
        if (skinPath.length() == 0) {
            skinPath = corePath + "Skin/";
        }
        if (skinPath.charAt(skinPath.length() - 1) != '/') {
            skinPath += "/";
        }

        skinTopPath = prefs.getString("skinTopPath", skinPath);
        if (skinTopPath.length() == 0) {
            skinTopPath = skinPath;
        }
        if (skinTopPath.charAt(skinTopPath.length() - 1) != '/') {
            skinTopPath += "/";
        }

        enableExtension = false;// prefs.getBoolean("enableExtension", false);
        cachePath = context.getCacheDir().getPath();
        burstEffects = prefs.getBoolean("bursts", burstEffects);
        hitLighting = prefs.getBoolean("hitlighting", hitLighting);
        useParticles = prefs.getBoolean("particles", true);
        useCustomComboColors = prefs.getBoolean("useCustomColors", useCustomComboColors);
        comboColors = new Color4[4];
        for (int i = 1; i <= 4; i++) {
            comboColors[i - 1] = new Color4(ColorPickerPreference.convertToRGB(prefs.getInt("combo" + i, 0xff000000)), HexComposition.RRGGBB);
        }

        // beatmaps
        DELETE_OSZ = prefs.getBoolean("deleteosz", true);
        SCAN_DOWNLOAD = prefs.getBoolean("scandownload", false);
        deleteUnimportedBeatmaps = prefs.getBoolean("deleteUnimportedBeatmaps", false);
        forceRomanized = prefs.getBoolean("forceromanized", false);
        beatmapPath = prefs.getString("directory", corePath + "Songs/");
        if (beatmapPath.length() == 0) {
            beatmapPath = corePath + "Songs/";
        }
        if (beatmapPath.charAt(beatmapPath.length() - 1) != '/') {
            beatmapPath += "/";
        }
        deleteUnsupportedVideos = prefs.getBoolean("deleteUnsupportedVideos", true);

        // other
        playMusicPreview = prefs.getBoolean("musicpreview", true);
        showCursor = prefs.getBoolean("showcursor", false);
        fixFrameOffset = prefs.getBoolean("fixFrameOffset", true);
        removeSliderLock = prefs.getBoolean("removeSliderLock", false);
        displayScoreStatistics = prefs.getBoolean("displayScoreStatistics", false);
        hideReplayMarquee = prefs.getBoolean("hideReplayMarquee", false);
        hideInGameUI = prefs.getBoolean("hideInGameUI", false);
        receiveAnnouncements = prefs.getBoolean("receiveAnnouncements", true);
        safeBeatmapBg = prefs.getBoolean("safebeatmapbg", false);
        shiftPitchInRateChange = prefs.getBoolean("shiftPitchInRateChange", false);
        minimumGameplaySynchronizationTime = prefs.getInt("gameAudioSynchronizationThreshold", 20);
        backButtonPressTime = Config.getInt("back_button_press_time", 300);

        // Multiplayer
        useNightcoreOnMultiplayer = prefs.getBoolean("player_nightcore", false);
        submitScoreOnMultiplayer = prefs.getBoolean("player_submitScore", true);
        preferModAcronymInMultiplayer = prefs.getBoolean("player_preferModAcronym", false);

        if(receiveAnnouncements) {
            FirebaseMessaging.getInstance().subscribeToTopic("announcements");
        }else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("announcements");
        }

        //Init
        onlineDeviceID = prefs.getString("installID", null);
        if (onlineDeviceID == null) {
            onlineDeviceID = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
            Editor editor = prefs.edit();
            editor.putString("installID", onlineDeviceID);
            editor.putString("corePath", corePath);
            editor.putString("skinTopPath", skinTopPath);
            editor.putString("skinPath", skinPath);
            editor.commit();
        }

        loadOnlineConfig(context);
    }

    public static void loadOnlineConfig(final Context context) {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        onlineUsername = prefs.getString("onlineUsername", "");
        onlinePassword = prefs.getString("onlinePassword", null);
        stayOnline = prefs.getBoolean("stayOnline", false);
        loadAvatar = prefs.getBoolean("loadAvatar",false);
    }

    public static void measureDisplaySize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Activity activity = (Activity) context;
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

        int width = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);
        int height = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);

        // Tries to emulate the original behavior, the game was designed for 1280x720
        // resolution, so we try to approximate the scale factor.
        float ratio = 1280f / width;

        RES_WIDTH = (int) (width * ratio);
        RES_HEIGHT = (int) (height * ratio);

        Log.v("Config", "Display size: " + width + "x" + height + "\nViewport size: " + RES_WIDTH + "x" + RES_HEIGHT);
    }

    public static boolean isEnableStoryboard() {
        return backgroundBrightness > 0.02 && enableStoryboard;
    }

    public static void setEnableStoryboard(boolean enableStoryboard) {
        Config.enableStoryboard = enableStoryboard;
    }

    public static boolean isFixFrameOffset() {
        return fixFrameOffset;
    }

    public static boolean isRemoveSliderLock() {
        //noinspection DataFlowIssue
        return Multiplayer.isConnected() ? Multiplayer.room.getGameplaySettings().isRemoveSliderLock() : removeSliderLock;
    }

    public static boolean isDisplayScoreStatistics() {
        return displayScoreStatistics;
    }

    @NonNull
    public static DifficultyAlgorithm getDifficultyAlgorithm() {
        return Config.getString("difficultyAlgorithm", "0").equals("1")
                ? DifficultyAlgorithm.standard
                : DifficultyAlgorithm.droid;
    }

    public static boolean isEnableExtension() {
        return enableExtension;
    }

    public static void setEnableExtension(boolean enableExtension) {
        Config.enableExtension = enableExtension;
    }

    public static boolean isShowFPS() {
        return showFPS;
    }

    public static void setShowFPS(final boolean showFPS) {
        Config.showFPS = showFPS;
    }

    public static boolean isShowScoreboard() {
        return getBoolean("showscoreboard", true);
    }

    public static void setShowScoreboard(final boolean showScoreboard) {
        setBoolean("showscoreboard", showScoreboard);
    }

    public static boolean isCorovans() {
        return corovans;
    }

    public static void setCorovans(final boolean corovans) {
        Config.corovans = corovans;
    }

    public static float getSoundVolume() {
        return soundVolume;
    }

    public static void setSoundVolume(final float volume) {
        Config.soundVolume = volume;
    }

    public static float getBgmVolume() {
        return bgmVolume;
    }

    public static void setBgmVolume(float bgmVolume) {
        Config.bgmVolume = bgmVolume;
    }

    public static float getOffset() {
        return offset;
    }

    public static void setOffset(final float offset) {
        Config.offset = offset;
    }

    public static String getCorePath() {
        return corePath;
    }

    public static void setCorePath(final String corePath) {
        Config.corePath = corePath;
    }

    public static String getBeatmapPath() {
        return beatmapPath;
    }

    public static void setBeatmapPath(final String path) {
        beatmapPath = path;
    }

    public static int getRES_WIDTH() {
        return RES_WIDTH;
    }

    public static void setRES_WIDTH(final int rES_WIDTH) {
        RES_WIDTH = rES_WIDTH;
    }

    public static int getRES_HEIGHT() {
        return RES_HEIGHT;
    }

    public static void setRES_HEIGHT(final int rES_HEIGHT) {
        RES_HEIGHT = rES_HEIGHT;
    }

    public static boolean isDELETE_OSZ() {
        return DELETE_OSZ;
    }

    public static void setDELETE_OSZ(final boolean dELETE_OSZ) {
        DELETE_OSZ = dELETE_OSZ;
    }

    public static boolean isSCAN_DOWNLOAD() {
        return SCAN_DOWNLOAD;
    }

    public static void setSCAN_DOWNLOAD(final boolean sCAN_DOWNLOAD) {
        SCAN_DOWNLOAD = sCAN_DOWNLOAD;
    }

    public static boolean isDeleteUnimportedBeatmaps() {
        return deleteUnimportedBeatmaps;
    }

    public static void setDeleteUnimportedBeatmaps(boolean deleteUnimportedBeatmaps) {
        Config.deleteUnimportedBeatmaps = deleteUnimportedBeatmaps;
    }

    public static boolean isUseCustomSkins() {
        return useCustomSkins;
    }

    public static void setUseCustomSkins(final boolean useCustomSkins) {
        Config.useCustomSkins = useCustomSkins;
    }

    public static boolean isUseCustomSounds() {
        return useCustomSounds;
    }

    public static void setUseCustomSounds(boolean useCustomSounds) {
        Config.useCustomSounds = useCustomSounds;
    }

    public static int getTextureQuality() {
        return 1;
    }

    public static float getBackgroundBrightness() {
        return backgroundBrightness;
    }

    public static void setBackgroundBrightness(final float backgroundBrightness) {
        Config.backgroundBrightness = backgroundBrightness;
    }

    public static boolean isAnimateFollowCircle() {
        return animateFollowCircle;
    }

    public static boolean isAnimateComboText() {
        return animateComboText;
    }


    public static boolean isSnakingInSliders()
    {
        return snakingInSliders;
    }

    public static boolean isSnakingOutSliders() {
        return snakingOutSliders;
    }

    public static boolean isPlayMusicPreview() {
        return playMusicPreview;
    }

    public static void setPlayMusicPreview(final boolean playMusicPreview) {
        Config.playMusicPreview = playMusicPreview;
    }

    public static boolean isShowCursor() {
        return showCursor;
    }

    public static void setShowCursor(final boolean showCursor) {
        Config.showCursor = showCursor;
    }

    public static String getOnlineUsername() {
        return !onlineUsername.isEmpty() ? onlineUsername : "Guest";
    }

    public static void setOnlineUsername(String onlineUsername) {
        Config.onlineUsername = onlineUsername;
    }

    public static String getOnlinePassword() {
        return onlinePassword;
    }

    public static void setOnlinePassword(String onlinePassword) {
        Config.onlinePassword = onlinePassword;
    }

    public static boolean isStayOnline() {
        return stayOnline && BuildType.hasOnlineAccess();
    }

    public static void setStayOnline(boolean stayOnline) {
        Config.stayOnline = stayOnline;
    }

    public static BeatmapLeaderboardScoringMode getBeatmapLeaderboardScoringMode() {
        return BeatmapLeaderboardScoringMode.parse(Integer.parseInt(getString("beatmapLeaderboardScoringMode", "0")));
    }

    public static void setBeatmapLeaderboardScoringMode(BeatmapLeaderboardScoringMode beatmapLeaderboardScoringMode) {
        setString("beatmapLeaderboardScoringMode", String.valueOf(beatmapLeaderboardScoringMode.ordinal()));
    }

    public static boolean getLoadAvatar() {
        return loadAvatar;
    }

    public static void setLoadAvatar(boolean loadAvatar) {
        Config.loadAvatar = loadAvatar;
    }

    public static String getOnlineDeviceID() {
        return onlineDeviceID;
    }

    public static String getCachePath() {
        return cachePath;
    }

    public static void setCachePath(String cachePath) {
        Config.cachePath = cachePath;
    }

    public static boolean isBurstEffects() {
        return burstEffects;
    }

    public static void setBurstEffects(boolean burstEffects) {
        Config.burstEffects = burstEffects;
    }

    public static boolean isHitLighting() {
        return hitLighting;
    }

    public static void setHitLighting(boolean hitLighting) {
        Config.hitLighting = hitLighting;
    }

    public static boolean isUseParticles() {
        return useParticles;
    }

    public static void setUseParticles(boolean useParticles) {
        Config.useParticles = useParticles;
    }

    public static String getSkinPath() {
        return skinPath;
    }

    public static void setSkinPath(String skinPath) {
        Config.skinPath = skinPath;
    }

    public static String getSkinTopPath() {
        return skinTopPath;
    }

    public static void setSkinTopPath(String skinTopPath) {
        Config.skinTopPath = skinTopPath;
    }

    public static String getScorePath() {
        return scorePath;
    }

    public static void setScorePath(String scorePath) {
        Config.scorePath = scorePath;
    }

    public static boolean isUseCustomComboColors() {
        return useCustomComboColors;
    }

    public static void setUseCustomComboColors(boolean useCustomComboColors) {
        Config.useCustomComboColors = useCustomComboColors;
    }

    public static Color4[] getComboColors() {
        return comboColors;
    }

    public static int getSpinnerStyle() {
        return spinnerStyle;
    }

    public static void setSpinnerStyle(int spinnerStyle) {
        Config.spinnerStyle = spinnerStyle;
    }

    public static boolean isShowFirstApproachCircle() {
        return showFirstApproachCircle;
    }

    public static void setShowFirstApproachCircle(boolean showFirstApproachCircle) {
        Config.showFirstApproachCircle = showFirstApproachCircle;
    }

    public static int getMetronomeSwitch() {
        return metronomeSwitch;
    }

    public static void setMetronomeSwitch(int metronomeSwitch) {
        Config.metronomeSwitch = metronomeSwitch;
    }

    public static boolean isComboburst() {
        return comboburst;
    }

    public static void setComboburst(boolean comboburst) {
        Config.comboburst = comboburst;
    }

    public static boolean isForceRomanized() {
        return forceRomanized;
    }

    public static void setForceRomanized(boolean forceRomanized) {
        Config.forceRomanized = forceRomanized;
    }

    public static float getCursorSize() {
        return cursorSize;
    }

    public static void setCursorSize(final float cursorSize) {
        Config.cursorSize = cursorSize;
    }

    public static float getPlayfieldSize() {
        return playfieldSize;
    }

    public static void setPlayfieldSize(final float playfieldSize) {
        Config.playfieldSize = playfieldSize;
    }

    public static float getPlayfieldHorizontalPosition() {
        return playfieldHorizontalPosition;
    }

    public static void setPlayfieldHorizontalPosition(float playfieldHorizontalPosition) {
        Config.playfieldHorizontalPosition = playfieldHorizontalPosition;
    }

    public static float getPlayfieldVerticalPosition() {
        return playfieldVerticalPosition;
    }

    public static void setPlayfieldVerticalPosition(float playfieldVerticalPosition) {
        Config.playfieldVerticalPosition = playfieldVerticalPosition;
    }

    public static boolean isHideReplayMarquee() {
        return hideReplayMarquee;
    }

    public static void setHideReplayMarquee(boolean hideReplayMarquee) {
        Config.hideReplayMarquee = hideReplayMarquee;
    }

    public static boolean isHideInGameUI() {
        return hideInGameUI;
    }

    public static void setHideInGameUI(boolean hideInGameUI) {
        Config.hideInGameUI = hideInGameUI;
    }

    public static boolean isReceiveAnnouncements() {
        return receiveAnnouncements;
    }

    public static void setReceiveAnnouncements(boolean receiveAnnouncements) {
        Config.receiveAnnouncements = receiveAnnouncements;
    }

    public static boolean isSafeBeatmapBg() {
        return safeBeatmapBg;
    }

    public static void setSafeBeatmapBg(boolean safeBeatmapBg) {
        Config.safeBeatmapBg = safeBeatmapBg;
    }

    public static boolean isTrianglesAnimation() {
        return false;
    }

    public static String getDefaultCorePath() {
        return defaultCorePath;
    }

    public static void loadSkins() {
        File[] folders = FileUtils.listFiles(new File(skinTopPath), file -> file.isDirectory() && !file.getName().startsWith("."));
        skins = new HashMap<>();
        for(File folder : folders) {
            skins.put(folder.getName(), folder.getPath());
            Debug.i("skins: " + folder.getName() + " - " + folder.getPath());
        }
    }

    public static Map<String, String> getSkins(){
        return skins;
    }

    public static void addSkin(String name, String path) {
        if(skins == null) skins = new HashMap<>();
        skins.put(name, path);
    }

    public static boolean isUseNightcoreOnMultiplayer() {
        return useNightcoreOnMultiplayer;
    }

    public static void setUseNightcoreOnMultiplayer(boolean value) {
        useNightcoreOnMultiplayer = value;
    }

    public static boolean isVideoEnabled() {
        return videoEnabled;
    }

    public static void setVideoEnabled(boolean value) {
        videoEnabled = value;
    }

    public static boolean isDeleteUnsupportedVideos() {
        return deleteUnsupportedVideos;
    }

    public static boolean isSubmitScoreOnMultiplayer() {
        return submitScoreOnMultiplayer;
    }

    public static void setSubmitScoreOnMultiplayer(boolean submitScoreOnMultiplayer) {
        Config.submitScoreOnMultiplayer = submitScoreOnMultiplayer;
    }

    public static boolean isPreferModAcronymInMultiplayer() {
        return preferModAcronymInMultiplayer;
    }

    public static void setPreferModAcronymInMultiplayer(boolean preferModAcronymInMultiplayer) {
        Config.preferModAcronymInMultiplayer = preferModAcronymInMultiplayer;
    }

    public static boolean isKeepBackgroundAspectRatio() {
        return keepBackgroundAspectRatio;
    }

    public static boolean isNoChangeDimInBreaks() {
        return noChangeDimInBreaks;
    }

    public static boolean isDimHitObjects() {
        return dimHitObjects;
    }

    public static boolean isForceMaxRefreshRate() {
        return forceMaxRefreshRate;
    }

    public static boolean isShiftPitchInRateChange() {
        return shiftPitchInRateChange;
    }

    public static boolean isDisplayPlayfieldBorder() {
        return getBoolean("displayPlayfieldBorder", false);
    }

    public static int getMinimumGameplaySynchronizationTime() {
        return minimumGameplaySynchronizationTime;
    }

    public static int getBackButtonPressTime() {
        return backButtonPressTime;
    }

    public static boolean isPreferNoVideoDownloads() {
        return getBoolean("preferNoVideoDownloads", false);
    }

    public static boolean isHighPrecisionInput() {
        return getBoolean("highPrecisionInput", false);
    }

    // Shared Preferences
    // It's preferred to use these methods to access shared preferences instead of adding new fields to this class.
    // If the option is expected to be accessed frequently consider storing it locally as a field where it's needed.

    public static boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public static void setBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).commit();
    }

    public static int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    public static void setInt(String key, int value) {
        sharedPreferences.edit().putInt(key, value).commit();
    }

    public static long getLong(String key, long defaultValue) {
        return sharedPreferences.getLong(key, defaultValue);
    }

    public static void setLong(String key, long value) {
        sharedPreferences.edit().putLong(key, value).commit();
    }

    public static String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public static void setString(String key, String value) {
        sharedPreferences.edit().putString(key, value).commit();
    }

    public static float getFloat(String key, float defaultValue) {
        return sharedPreferences.getFloat(key, defaultValue);
    }

    public static void setFloat(String key, float value) {
        sharedPreferences.edit().putFloat(key, value).commit();
    }



    // Slider Movement Settings
    public static boolean isBezierSliderMovementEnabled() {
        return getBoolean("bezier_enable_slider_movement", true);
    }

    public static float getBezierSliderAngleOffset() {
        return getInt("bezier_slider_angle_offset", 22) * (float) Math.PI / 180f;
    }

    public static float getBezierSliderDistanceMultiplier() {
        return getInt("bezier_slider_distance_mult", 80) / 100f;
    }

    public static boolean isFlowerSliderMovementEnabled() {
        return getBoolean("flower_enable_slider_movement", true);
    }

    public static float getFlowerSliderAngleOffset() {
        return getInt("flower_slider_angle_offset", 45) * (float) Math.PI / 180f;
    }

    public static float getFlowerSliderDistanceMultiplier() {
        return getInt("flower_slider_distance_mult", 140) / 100f;
    }

    public static boolean isPippiSliderMovementEnabled() {
        return getBoolean("pippi_enable_slider_movement", true);
    }

    public static float getPippiSliderAngleOffset() {
        return getInt("pippi_slider_angle_offset", 15) * (float) Math.PI / 180f;
    }

    public static float getPippiSliderDistanceMultiplier() {
        return getInt("pippi_slider_distance_mult", 60) / 100f;
    }

    public static boolean isSplineSliderMovementEnabled() {
        return getBoolean("spline_enable_slider_movement", true);
    }

    public static float getSplineSliderAngleOffset() {
        return getInt("spline_slider_angle_offset", 36) * (float) Math.PI / 180f;
    }

    public static float getSplineSliderDistanceMultiplier() {
        return getInt("spline_slider_distance_mult", 80) / 100f;
    }

    public static boolean isMomentumSliderMovementEnabled() {
        return getBoolean("momentum_enable_slider_movement", true);
    }

    public static float getMomentumSliderAngleOffset() {
        return getInt("momentum_slider_angle_offset", 30) * (float) Math.PI / 180f;
    }

    public static float getMomentumSliderDistanceMultiplier() {
        return getInt("momentum_slider_distance_mult", 110) / 100f;
    }

    public static float getMomentumSliderMaxAngleChange() {
        return getInt("momentum_slider_max_angle_change", 60) * (float) Math.PI / 180f;
    }

    public static boolean isExGonSliderMovementEnabled() {
        return getBoolean("exgon_enable_slider_movement", true);
    }

    public static float getExGonSliderAngleOffsetRange() {
        return getInt("exgon_slider_angle_offset_range", 45) * (float) Math.PI / 180f;
    }

    public static float getExGonSliderDistanceMultiplier() {
        return getInt("exgon_slider_distance_mult", 70) / 100f;
    }

    public static boolean isEnhancedCursorSliderMovementEnabled() {
        return getBoolean("enhanced_cursor_enable_slider_movement", true);
    }

    public static float getEnhancedCursorSliderAngleOffset() {
        return getInt("enhanced_cursor_slider_angle_offset", 45) * (float) Math.PI / 180f;
    }

    public static float getEnhancedCursorSliderDistanceMultiplier() {
        return getInt("enhanced_cursor_slider_distance_mult", 120) / 100f;
    }

    public static int getEnhancedCursorSliderSegments() {
        return getInt("enhanced_cursor_slider_segments", 8);
    }

    public static boolean isHalfCircleSliderMovementEnabled() {
        return getBoolean("half_circle_enable_slider_movement", true);
    }

    public static float getHalfCircleSliderAngleOffset() {
        return getInt("half_circle_slider_angle_offset", 60) * (float) Math.PI / 180f;
    }

    public static float getHalfCircleSliderDistanceMultiplier() {
        return getInt("half_circle_slider_distance_mult", 140) / 100f;
    }

    public static boolean isAxisSliderMovementEnabled() {
        return getBoolean("axis_enable_slider_movement", true);
    }

    public static float getAxisSliderProbeOffset() {
        return getInt("axis_slider_probe_offset", 40) / 100f;
    }

    public static boolean isAggressiveSliderMovementEnabled() {
        return getBoolean("aggressive_enable_slider_movement", true);
    }

    public static float getAggressiveSliderAngleOffset() {
        return getInt("aggressive_slider_angle_offset", 60) * (float) Math.PI / 180f;
    }

    public static float getAggressiveSliderDistanceMultiplier() {
        return getInt("aggressive_slider_distance_mult", 120) / 100f;
    }

    public static float getTrailLength() {
        return getInt("trailLength", 100) / 100f;
    }

    public static void setTrailLength(float trailLength) {
        setInt("trailLength", (int) (trailLength * 100));
    }

    // Glow Settings
    public static boolean isCursorGlowEnabled() {
        return false; // Disabled
    }

    public static void setCursorGlowEnabled(boolean enabled) {
        setBoolean("cursorGlowEnabled", false); // Always false
    }

    public static boolean isTrailGlowEnabled() {
        return false; // Disabled
    }

    public static void setTrailGlowEnabled(boolean enabled) {
        setBoolean("trailGlowEnabled", false); // Always false
    }

    public static boolean isObjectGlowEnabled() {
        return false; // Disabled
    }

    public static void setObjectGlowEnabled(boolean enabled) {
        setBoolean("objectGlowEnabled", false); // Always false
    }

    public static float getGlowEndScale() {
        return getInt("glowEndScale", 40) / 100f;
    }

    public static void setGlowEndScale(float scale) {
        setInt("glowEndScale", (int) (scale * 100));
    }

    public static float getInnerLengthMult() {
        return getInt("innerLengthMult", 90) / 100f;
    }

    public static void setInnerLengthMult(float mult) {
        setInt("innerLengthMult", (int) (mult * 100));
    }

    public static boolean isAdditiveBlendingEnabled() {
        return getBoolean("additiveBlendingEnabled", true);
    }

    public static void setAdditiveBlendingEnabled(boolean enabled) {
        setBoolean("additiveBlendingEnabled", enabled);
    }

    public static float getGlowIntensity() {
        return getInt("glowIntensity", 80) / 100f;
    }

    public static void setGlowIntensity(float intensity) {
        setInt("glowIntensity", (int) (intensity * 100));
    }

    // Trail Size and Width Settings
    public static float getTrailSize() {
        return getInt("trailSize", 100) / 100f;
    }

    public static void setTrailSize(float size) {
        setInt("trailSize", (int) (size * 100));
    }

    public static float getTrailWidth() {
        return getInt("trailWidth", 100) / 100f;
    }

    public static void setTrailWidth(float width) {
        setInt("trailWidth", (int) (width * 100));
    }
}
