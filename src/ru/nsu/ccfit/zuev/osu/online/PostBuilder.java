package ru.nsu.ccfit.zuev.osu.online;

import ru.nsu.ccfit.zuev.osu.SecurityUtils;

import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Request;

import org.anddev.andengine.util.Debug;

import java.io.BufferedReader;
import java.io.Serial;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.net.URLEncoder;
import java.util.ArrayList;

public abstract class PostBuilder {
    private final StringBuilder values = new StringBuilder();

    public void addParam(final String key, final String value) {
        try {
            if (values.length() > 0) {
                values.append("_");
            }
            addParamInternal(key, value);
            values.append(URLEncoder.encode(value, "UTF-8"));
        } catch (UnsupportedEncodingException ignored) {
        }
    }

    public ArrayList<String> requestWithAttempts(final String scriptUrl, int attempts) throws RequestException {
        ArrayList<String> response = null;
        String signature = SecurityUtils.signRequest(values.toString());

        if (signature != null) {
            addParam("sign", signature);
        }
        for (int i = 0; i < attempts; i++) {
            try {
                response = request(scriptUrl);
            } catch (RequestException e) {
                if (e.getCause() instanceof UnknownHostException) {
                    Debug.e("Cannot resolve server name");
                    break;
                }
                Debug.e("Received error, continuing... ", e);
                response = null;
            }

            // "FAILED" is the fail token the droid+ server uses ("FAIL" is the old one).
            // Treating it as an invalid answer made every rejected login repeat the
            // same request three times, which burns the server's per-minute limit and
            // ends in an empty (rate limited) response.
            if (response == null || response.isEmpty() || response.get(0).isEmpty()
                    || !(response.get(0).equals("FAIL")
                        || response.get(0).equals("FAILED")
                        || response.get(0).equals("SUCCESS"))) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                }
                continue;
            }
            break;
        }

        if (response == null) response = new ArrayList<>();

        if (response.isEmpty()) {
            response.add("");
        }
        return response;
    }

    private ArrayList<String> request(final String scriptUrl) throws RequestException {
        ArrayList<String> response = new ArrayList<>();

        Request request = new Request.Builder()
            .url(scriptUrl)
            .post(getRequestBody())
            .build();

        try (Response resp = OnlineManager.client.newCall(request).execute()) {

            if (resp.isSuccessful() && resp.body() != null) {
                Debug.i("request url=" + scriptUrl);
                Debug.i("request --------Content---------");
                String line;
                BufferedReader reader = new BufferedReader(new StringReader(resp.body().string()));
                while ((line = reader.readLine()) != null) {
                    Debug.i(String.format("request [%d]: %s", response.size(), line));
                    response.add(line);
                }
                Debug.i("request url=" + scriptUrl);
                Debug.i("request -----End of content-----");
            } else if (!resp.isSuccessful()) {
                // Surface the HTTP status instead of a blank answer: an empty body is
                // otherwise reported as "Got empty response" with no way to tell a
                // rate limit or a Cloudflare error apart from a real network failure.
                Debug.e("request " + scriptUrl + " failed with HTTP " + resp.code());
                response.add("FAILED");
                if (resp.code() == 429) {
                    // Rate limited: retrying the login with other hashes only makes it
                    // worse, so surface a message the login screen can show as-is.
                    response.add("Too many requests (HTTP 429). Please wait a minute and try again.");
                } else {
                    response.add("Server error " + resp.code());
                }
            }
        } catch(Exception e) {
            Debug.e(e.getMessage(), e);
        }

        if (response.isEmpty()) {
            response.add("");
        }
        return response;
    }

    protected abstract void addParamInternal(final String key, final String value);

    protected abstract RequestBody getRequestBody();

    public static class RequestException extends Exception {
        @Serial
        private static final long serialVersionUID = 671773899432746143L;

        public RequestException(final Throwable cause) {
            super(cause);
        }
    }
}
