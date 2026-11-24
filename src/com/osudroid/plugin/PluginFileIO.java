package com.osudroid.plugin;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Sandboxed file I/O for Lua plugins.
 * Each plugin can only read/write files in its own subdirectory:
 *   context.filesDir/plugins/<plugin_name>/data/
 *
 * <p>Names are sanitized, canonical paths must stay inside the plugin data
 * directory (this also rejects symlinks escaping it), and reads/writes are
 * capped at 1 MB.</p>
 */
public final class PluginFileIO {

    private static final String TAG = "PluginFileIO";
    private static final int MAX_FILE_BYTES = 1_000_000;

    private PluginFileIO() {}

    public static String safePluginName(String pluginName) {
        return (pluginName == null ? "plugin" : pluginName)
            .replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    public static File getPluginDir(Context context, String pluginName) {
        File dir = new File(context.getFilesDir(), "plugins/" + safePluginName(pluginName) + "/data");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private static File resolve(Context context, String pluginName, String filename) throws IOException {
        if (filename == null || filename.isEmpty() || filename.length() > 200) {
            return null;
        }
        if (filename.indexOf('\0') >= 0) {
            return null;
        }

        File dir = getPluginDir(context, pluginName);
        File file = new File(dir, filename);
        String root = dir.getCanonicalPath() + File.separator;
        String path = file.getCanonicalPath();
        if (!path.startsWith(root)) {
            Log.w(TAG, "Security: blocked access to " + filename + " (path traversal)");
            return null;
        }
        return file;
    }

    public static boolean writeFile(Context context, String pluginName, String filename, String content) {
        File file = null;
        try {
            file = resolve(context, pluginName, filename);
            if (file == null) {
                return false;
            }
            byte[] data = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
            if (data.length > MAX_FILE_BYTES) {
                Log.w(TAG, "Blocked oversized plugin file: " + filename);
                return false;
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data);
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "writeFile failed: " + e.getMessage());
            return false;
        }
    }

    public static String readFile(Context context, String pluginName, String filename) {
        File file;
        try {
            file = resolve(context, pluginName, filename);
        } catch (IOException e) {
            return null;
        }
        if (file == null || !file.isFile()) {
            return null;
        }
        if (file.length() > MAX_FILE_BYTES) {
            Log.w(TAG, "Blocked oversized plugin file read: " + filename);
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = fis.read(buffer)) != -1) {
                total += read;
                if (total > MAX_FILE_BYTES) {
                    return null;
                }
                out.write(buffer, 0, read);
            }
            return out.toString("UTF-8");
        } catch (IOException e) {
            Log.e(TAG, "readFile failed: " + e.getMessage());
            return null;
        }
    }

    public static boolean fileExists(Context context, String pluginName, String filename) {
        try {
            File file = resolve(context, pluginName, filename);
            return file != null && file.isFile();
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean deleteFile(Context context, String pluginName, String filename) {
        try {
            File file = resolve(context, pluginName, filename);
            return file != null && file.isFile() && file.delete();
        } catch (IOException e) {
            return false;
        }
    }

    /** Comma-separated file names in the plugin's data directory. */
    public static String listFiles(Context context, String pluginName) {
        File[] files = getPluginDir(context, pluginName).listFiles(File::isFile);
        if (files == null || files.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < files.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(files[i].getName());
        }
        return sb.toString();
    }
}
