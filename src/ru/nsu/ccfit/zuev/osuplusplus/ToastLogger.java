package ru.nsu.ccfit.zuev.osuplusplus;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.widget.Toast;

import androidx.annotation.StringRes;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.helper.StringTable;

public class ToastLogger {
    private static ToastLogger instance = null;
    Activity activity;
    ArrayList<String> debugLog = new ArrayList<>();
    float percentage;

    private ToastLogger(final Activity activity) {
        this.activity = activity;
    }

    public static void init(final Activity activity) {
        instance = new ToastLogger(activity);
    }

    public static void showText(final String message, final boolean showlong) {
        if (instance == null) {
            return;
        }

        instance.debugLog.add(message);

        instance.activity.runOnUiThread(() -> 
            Toast.makeText(instance.activity, message,
                showlong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show());
    }

    public static void showText(@StringRes final int resID, final boolean showlong) {
        showText(StringTable.get(resID), showlong);
    }

    public static ArrayList<String> getLog() {
        if (instance == null) {
            return null;
        }
        return instance.debugLog;
    }

    public static String getLogText() {
        if (instance == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String line : instance.debugLog) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    public static void copyLogToClipboard() {
        if (instance == null || instance.activity == null) {
            return;
        }

        try {
            ClipboardManager clipboard = (ClipboardManager) instance.activity.getSystemService(Activity.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("osu!droid+ Log", getLogText());
            clipboard.setPrimaryClip(clip);
        } catch (Exception e) {
            // Ignore clipboard errors
        }
    }

    public static float getPercentage() {
        if (instance == null) {
            return -1;
        }
        return instance.percentage;
    }

    public static void setPercentage(final float perc) {
        if (instance == null) {
            return;
        }
        instance.percentage = perc;
    }

}