package ru.nsu.ccfit.zuev.osu.online;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Response;
import okhttp3.Request;
import okio.BufferedSink;
import okio.Okio;

import org.anddev.andengine.util.Debug;
import org.apache.commons.io.FileUtils;

public class OnlineFileOperator {
    public static boolean downloadFile(String urlstr, String filename) {
        return downloadFile(urlstr, filename, false);
    }

    public static boolean downloadFile(String urlstr, String filename, boolean checkModificationDate) {
        Debug.i("Starting download " + urlstr);
        File file = new File(filename);
        File temp = new File(filename + ".part");
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                Debug.e("Could not create the replay cache directory");
                return false;
            }

            boolean hasCachedFile = isValidReplay(file);
            if (!checkModificationDate && hasCachedFile) {
                Debug.i(file.getName() + " already exists");
                return true;
            }

            if (temp.exists() && !temp.delete()) {
                Debug.e("Could not remove a stale replay part file");
                return false;
            }

            Debug.i("Connected to " + urlstr);

            var builder = new Request.Builder().url(urlstr);

            // Only send If-Modified-Since for a cached archive we can actually use.
            if (checkModificationDate && hasCachedFile) {
                var lastModifiedDate = new Date(file.lastModified());
                var df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
                df.setTimeZone(TimeZone.getTimeZone("GMT"));

                builder.addHeader("If-Modified-Since", df.format(lastModifiedDate) + " GMT");
            }

            Request request = builder.build();
            try (Response response = OnlineManager.client.newCall(request).execute()) {
                if (checkModificationDate && hasCachedFile && response.code() == 304) {
                    return true;
                }
                if (!response.isSuccessful()) {
                    Debug.e("downloadFile HTTP " + response.code() + " for " + urlstr);
                    return false;
                }
                if (response.body() == null) {
                    Debug.e("downloadFile returned an empty body for " + urlstr);
                    return false;
                }

                long expectedLength = response.body().contentLength();
                try (BufferedSink sink = Okio.buffer(Okio.sink(temp))) {
                    sink.writeAll(response.body().source());
                }

                if (expectedLength > 0 && temp.length() != expectedLength) {
                    Debug.e("downloadFile wrote an incomplete file for " + urlstr);
                    temp.delete();
                    return false;
                }
                if (!isValidReplay(temp)) {
                    Debug.e("downloadFile received an invalid replay for " + urlstr);
                    temp.delete();
                    return false;
                }
            }

            if (file.exists() && !file.delete()) {
                FileUtils.copyFile(temp, file);
                temp.delete();
                return true;
            }
            if (!temp.renameTo(file)) {
                FileUtils.copyFile(temp, file);
                temp.delete();
            }
            return true;
        } catch (final IOException e) {
            temp.delete();
            Debug.e("downloadFile IOException " + e.getMessage(), e);
            return false;
        } catch (final Exception e) {
            temp.delete();
            Debug.e("downloadFile Exception " + e.getMessage(), e);
            return false;
        }
    }

    /** Replays are zip archives; this also rejects 404 HTML pages and stale empty files. */
    private static boolean isValidReplay(File file) {
        if (!file.isFile() || file.length() < 4) {
            return false;
        }
        try (FileInputStream input = new FileInputStream(file)) {
            return input.read() == 'P' && input.read() == 'K';
        } catch (IOException e) {
            return false;
        }
    }
}
