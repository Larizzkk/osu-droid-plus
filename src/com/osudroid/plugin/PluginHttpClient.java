package com.osudroid.plugin;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Async HTTP client for Lua plugins.
 * Requests run on a background thread, callbacks fire on the main thread.
 *
 * <p>Security: only public HTTPS endpoints are allowed (see
 * {@link PluginUrlPolicy}), redirects are not followed, POST bodies are capped
 * and responses larger than 1 MB are rejected. This still gives plugins a
 * normal web API while preventing localhost/LAN probing and unbounded memory
 * use.</p>
 */
public class PluginHttpClient {

    private static final String TAG = "PluginHttp";
    private static final int MAX_RESPONSE_BYTES = 1_000_000;
    private static final int MAX_POST_BYTES = 64_000;

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** Async HTTPS GET. callback(url, statusCode, responseBody). */
    public static void get(String url, Globals sandbox, LuaValue callback) {
        executor.execute(() -> execute("GET", url, null, callback));
    }

    /** Async HTTPS POST with a UTF-8 body. callback(url, statusCode, responseBody). */
    public static void post(String url, String body, Globals sandbox, LuaValue callback) {
        executor.execute(() -> execute("POST", url, body == null ? "" : body, callback));
    }

    private static void execute(String method, String url, String body, LuaValue callback) {
        String reason = PluginUrlPolicy.validate(url);
        if (reason != null) {
            postCallback(callback, url, -1, "Error: " + reason);
            return;
        }

        if (body != null && body.getBytes(StandardCharsets.UTF_8).length > MAX_POST_BYTES) {
            postCallback(callback, url, -1, "Error: POST body is too large");
            return;
        }

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "osu-droid-plugin/1.2");
            conn.setRequestProperty("Accept", "application/json, text/plain, */*");

            if (body != null) {
                byte[] data = body.getBytes(StandardCharsets.UTF_8);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(data.length);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(data);
                }
            }

            int status = conn.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                ? conn.getInputStream()
                : conn.getErrorStream();
            String response = stream == null ? "" : readLimited(stream);
            postCallback(callback, url, status, response);
        } catch (ResponseTooLargeException e) {
            postCallback(callback, url, -1, "Error: response is larger than 1 MB");
        } catch (Exception e) {
            Log.e(TAG, method + " failed: " + e.getMessage());
            postCallback(callback, url, -1, "Error: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readLimited(InputStream stream) throws IOException {
        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_RESPONSE_BYTES) {
                    throw new ResponseTooLargeException();
                }
                out.write(buffer, 0, read);
            }
            return out.toString("UTF-8");
        }
    }

    private static void postCallback(LuaValue callback, String url, int status, String body) {
        if (callback == null || !callback.isfunction()) {
            return;
        }
        mainHandler.post(() -> {
            try {
                callback.call(LuaValue.valueOf(url), LuaValue.valueOf(status), LuaValue.valueOf(body));
            } catch (Exception e) {
                Log.e(TAG, "HTTP callback error: " + e.getMessage());
            }
        });
    }

    private static final class ResponseTooLargeException extends IOException {}
}
