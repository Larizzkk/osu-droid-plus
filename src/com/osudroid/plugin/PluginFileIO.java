package com.osudroid.plugin;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Sandboxed file I/O for Lua plugins.
 * Each plugin can only read/write files in its own subdirectory:
 *   context.filesDir/plugins/<plugin_name>/
 *
 * Access outside this directory is blocked.
 */
public class PluginFileIO {

    private static final String TAG = "PluginFileIO";

    /**
     * Get the plugin's data directory. Creates it if it doesn't exist.
     */
    public static File getPluginDir(Context context, String pluginName) {
        // Sanitize plugin name (remove path separators)
        String safeName = pluginName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        File dir = new File(context.getFilesDir(), "plugins/" + safeName + "/data");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Write content to a file in the plugin's data directory.
     * Returns true on success.
     */
    public static boolean writeFile(Context context, String pluginName, String filename, String content) {
        try {
            File file = new File(getPluginDir(context, pluginName), filename);
            // Security: ensure file is within plugin directory
            if (!file.getCanonicalPath().startsWith(getPluginDir(context, pluginName).getCanonicalPath())) {
                Log.w(TAG, "Security: blocked write to " + filename + " (path traversal)");
                return false;
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "writeFile failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Read content from a file in the plugin's data directory.
     * Returns null if file doesn't exist.
     */
    public static String readFile(Context context, String pluginName, String filename) {
        try {
            File file = new File(getPluginDir(context, pluginName), filename);
            // Security: ensure file is within plugin directory
            if (!file.getCanonicalPath().startsWith(getPluginDir(context, pluginName).getCanonicalPath())) {
                Log.w(TAG, "Security: blocked read from " + filename + " (path traversal)");
                return null;
            }
            if (!file.exists()) return null;

            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            return new String(data, "UTF-8");
        } catch (IOException e) {
            Log.e(TAG, "readFile failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if a file exists in the plugin's data directory.
     */
    public static boolean fileExists(Context context, String pluginName, String filename) {
        File file = new File(getPluginDir(context, pluginName), filename);
        return file.exists() && file.isFile();
    }

    /**
     * Delete a file from the plugin's data directory.
     */
    public static boolean deleteFile(Context context, String pluginName, String filename) {
        try {
            File file = new File(getPluginDir(context, pluginName), filename);
            if (!file.getCanonicalPath().startsWith(getPluginDir(context, pluginName).getCanonicalPath())) {
                return false;
            }
            return file.delete();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * List all files in the plugin's data directory.
     * Returns comma-separated filenames.
     */
    public static String listFiles(Context context, String pluginName) {
        File dir = getPluginDir(context, pluginName);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < files.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(files[i].getName());
        }
        return sb.toString();
    }
}
