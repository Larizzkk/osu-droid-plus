package com.osudroid.plugin;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Async HTTP client for Lua plugins.
 * Requests run on a background thread, callbacks fire on the main thread.
 *
 * SECURITY: Only GET and POST are allowed. No file:// URLs.
 */
public class PluginHttpClient {

    private static final String TAG = "PluginHttp";
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Async HTTP GET request.
     * callback(url, statusCode, responseBody)
     */
    public static void get(String url, Globals sandbox, LuaValue callback) {
        executor.execute(() -> {
            try {
                // Security: block file:// and non-http URLs
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    postCallback(callback, url, -1, "Error: only http/https URLs allowed");
                    return;
                }

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "osu-droid-plugin/1.0");

                int status = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                    status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                conn.disconnect();

                postCallback(callback, url, status, sb.toString().trim());
            } catch (Exception e) {
                Log.e(TAG, "GET failed: " + e.getMessage());
                postCallback(callback, url, -1, "Error: " + e.getMessage());
            }
        });
    }

    /**
     * Async HTTP POST request with body.
     * callback(url, statusCode, responseBody)
     */
    public static void post(String url, String body, Globals sandbox, LuaValue callback) {
        executor.execute(() -> {
            try {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    postCallback(callback, url, -1, "Error: only http/https URLs allowed");
                    return;
                }

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "osu-droid-plugin/1.0");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(body.getBytes("UTF-8"));
                os.close();

                int status = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                    status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                conn.disconnect();

                postCallback(callback, url, status, sb.toString().trim());
            } catch (Exception e) {
                Log.e(TAG, "POST failed: " + e.getMessage());
                postCallback(callback, url, -1, "Error: " + e.getMessage());
            }
        });
    }

    private static void postCallback(LuaValue callback, String url, int status, String body) {
        if (callback == null || !callback.isfunction()) return;
        mainHandler.post(() -> {
            try {
                callback.call(LuaValue.valueOf(url), LuaValue.valueOf(status), LuaValue.valueOf(body));
            } catch (Exception e) {
                Log.e(TAG, "HTTP callback error: " + e.getMessage());
            }
        });
    }
}
