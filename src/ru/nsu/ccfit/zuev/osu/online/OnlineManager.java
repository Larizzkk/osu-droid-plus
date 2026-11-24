package ru.nsu.ccfit.zuev.osu.online;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;


import com.osudroid.data.BeatmapInfo;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;

import okhttp3.Request;
import okhttp3.RequestBody;
import org.anddev.andengine.util.Debug;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import ru.nsu.ccfit.zuev.osu.*;
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator;
import ru.nsu.ccfit.zuev.osu.online.PostBuilder.RequestException;

public class OnlineManager {
    public static final String hostname = "osu-droid-plus-server-v2.larizrkpee.workers.dev";
    public static final String serverURL = "https://" + hostname;
    public static final String endpoint = serverURL + "/api/";
    public static final String updateEndpoint = endpoint + "update.php?lang=";
    public static final String updateDownloadURL = endpoint + "update/download";
    public static final String changelogURL = serverURL + "/changelog";
    public static final String defaultAvatarURL = serverURL + "/user/avatar/0.png";
    public static final String profileBannerEndpoint = serverURL + "/user/banner/";
    // Protocol version the osu!droid+ server implements (same as the legacy client).
    private static final String onlineVersion = "60";

    /**
     * Pepper used before the server moved to the current one. Hashes produced with it are still
     * stored for accounts that never logged in after the change.
     */
    private static final String LEGACY_PASSWORD_PEPPER = "239dde358bc836f4ebd09758f492ba14";

    public static final OkHttpClient client = new OkHttpClient();

    private static OnlineManager instance = null;
    private String failMessage = "";

    private boolean stayOnline = true;
    private String ssid = "";
    private long userId = -1L;

    private String username = "";
    private String password = "";
    private String deviceID = "";
    private long rank = 0;
    private long score = 0;
    private float accuracy = 0;
    private float pp = 0;
    private float ppGain = 0;
    private float totalDpp = 0;
    private float totalDppAll = 0;
    private String avatarURL = "";
    private String profileBannerURL = "";
    private int mapRank;

    public static OnlineManager getInstance() {
        if (instance == null) {
            instance = new OnlineManager();
        }
        return instance;
    }

    public static String getReplayURL(int playID) {
        return switch (Config.getBeatmapLeaderboardScoringMode()) {
            case SCORE -> endpoint + "upload/" + playID + ".odr";
            default -> endpoint + "bestpp/" + playID + ".odr";
        };
    }

    public static String getAvatarURL(long userId) {
        return "https://" + hostname + "/user/avatar/" + userId + ".png";
    }

    public static String getProfileBannerURL(long userId) {
        return profileBannerEndpoint + userId + ".png";
    }

    public void init() {
        this.stayOnline = Config.isStayOnline();
        this.username = Config.getOnlineUsername();
        this.password = Config.getOnlinePassword();
        this.deviceID = Config.getOnlineDeviceID();
    }

    private boolean isInvalidCredentials(ArrayList<String> response) {
        return response == null && failMessage != null
                && failMessage.toLowerCase(java.util.Locale.ROOT).contains("invalid credential");
    }

    /** True when the last request was rejected by the server's rate limiter. */
    private boolean isRateLimited() {
        if (failMessage == null) {
            return false;
        }
        final String message = failMessage.toLowerCase(java.util.Locale.ROOT);
        return message.contains("429") || message.contains("too many requests");
    }

    private ArrayList<String> sendRequest(PostBuilder post, String url) throws OnlineManagerException {
        ArrayList<String> response;
        try {
            response = post.requestWithAttempts(url, 3);
        } catch (RequestException e) {
            Debug.e(e.getMessage(), e);
            failMessage = "Cannot connect to server";
            throw new OnlineManagerException("Cannot connect to server", e);
        }
        failMessage = "";

        //TODO debug code
		/*Debug.i("Received " + response.size() + " lines");
		for(String str: response)
		{
			Debug.i(str);
		}*/

        if (response.size() == 0 || response.get(0).length() == 0) {
            failMessage = "Got empty response";
            Debug.i("Received empty response!");
            return null;
        }

        if (!response.get(0).equals("SUCCESS")) {
            Debug.i("sendRequest response code:  " + response.get(0));
            if (response.size() >= 2) {
                failMessage = response.get(1);
            } else
                failMessage = "Unknown server error";
            Debug.i("Received fail: " + failMessage);
            return null;
        }


        return response;
    }

    public boolean logIn() throws OnlineManagerException {
        return logIn(username, password);
    }

    public boolean logIn(String username) throws OnlineManagerException {
        return logIn(username, password);
    }

    public synchronized boolean logIn(String username, String password) throws OnlineManagerException {
        this.username = username;
        this.password = password;

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            failMessage = "Username or password is empty";
            return false;
        }

        // The server looks the account up by username_safe = lowercase(trimmed name), so a
        // stray leading/trailing space in the saved name would otherwise be reported as
        // "Invalid credentials" even with the correct password.
        final String loginName = username.trim();

        // The current server stores sha256(md5(raw password + "taikotaiko")) and expects the
        // client to send that md5, exactly like the original osu!droid client does, so the raw
        // form is tried first.
        //
        // All legacy hash variants are sent in the SAME request (password2..password5):
        // the server accepts any of them, so an old account logs in on the first try
        // instead of burning 5 requests (and the per-IP rate limit) on a retry chain.
        // The RAW password is never sent: the server stores hashes, so it could not
        // validate it anyway, and sending it would expose the plaintext password.
        final String raw = password.trim();
        final String md5Pepper = MD5Calculator.getStringMD5(raw + "taikotaiko");
        final String md5EscapedPepper = MD5Calculator.getStringMD5(
                escapeHTMLSpecialCharacters(addSlashes(raw)) + "taikotaiko");
        final String md5LegacyPepper = MD5Calculator.getStringMD5(raw + LEGACY_PASSWORD_PEPPER);
        final String md5EscapedLegacyPepper = MD5Calculator.getStringMD5(
                escapeHTMLSpecialCharacters(addSlashes(raw)) + LEGACY_PASSWORD_PEPPER);
        final String md5NoPepper = MD5Calculator.getStringMD5(raw);

        PostBuilder post = new URLEncodedPostBuilder();
        post.addParam("username", loginName);
        post.addParam("password", md5Pepper);
        post.addParam("password2", md5EscapedPepper);
        post.addParam("password3", md5LegacyPepper);
        post.addParam("password4", md5EscapedLegacyPepper);
        post.addParam("password5", md5NoPepper);
        post.addParam("version", onlineVersion);

        ArrayList<String> response = sendRequest(post, endpoint + "login.php");

        // A rate-limited or otherwise failed request is not a credential problem; retrying
        // the same login with other hashes would only make it worse.
        if (isRateLimited()) {
            return false;
        }

        // Accounts that were created by the old PHP server have their password hashed over the
        // escaped form (md5(htmlspecialchars(addslashes(password)) + pepper)), and a stored hash
        // may also have been produced with a different pepper. Instead of showing
        // "Invalid credentials" for them, the rejected login is retried with those variants.
        // Servers that predate the multi-hash login ignore password2..password5 and
        // only check the primary hash. Retry the remaining variants one by one so
        // those servers keep working too (a single extra request per variant).
        if (isInvalidCredentials(response)) {
            for (String candidate : new String[]{md5LegacyPepper, md5NoPepper}) {
                if (isInvalidCredentials(response)) {
                    Debug.i("Login rejected, retrying with a legacy password hash");

                    PostBuilder retry = new URLEncodedPostBuilder();
                    retry.addParam("username", loginName);
                    retry.addParam("password", candidate);
                    retry.addParam("version", onlineVersion);

                    response = sendRequest(retry, endpoint + "login.php");

                    // Rate limiting is not a credential problem: stop instead of
                    // burning the remaining attempts and the per-IP budget.
                    if (response == null && isRateLimited()) {
                        break;
                    }
                }
            }
        }

        if (response == null) {
            return false;
        }
        if (response.size() < 2) {
            failMessage = "Invalid server response";
            return false;
        }

        String[] params = response.get(1).split("\\s+");
        if (params.length < 7) {
            failMessage = "Invalid server response";
            return false;
        }
        userId = Long.parseLong(params[0]);
        ssid = params[1];
        rank = Integer.parseInt(params[2]);
        score = Long.parseLong(params[3]);
        pp = Float.parseFloat(params[4]);
        // login.php returns the cumulative overall PP in params[4]. The
        // optional ninth value is the same overall with cheat plays included.
        totalDpp = pp;
        totalDppAll = params.length >= 9 ? Float.parseFloat(params[8]) : pp;
        accuracy = Float.parseFloat(params[5]);
        this.username = params[6];
        if (params.length >= 8) {
            avatarURL = params[7];
        } else {
            avatarURL = "";
        }
        profileBannerURL = getProfileBannerURL(userId);

        return true;
    }

    boolean tryToLogIn() throws OnlineManagerException {
        if (logIn(username, password) == false) {
            stayOnline = false;
            return false;
        }
        return true;
    }

    public boolean sendRecord(BeatmapInfo beatmap, String scoreData, String replayPath, double localPP) throws OnlineManagerException {
        Debug.i("Sending record...");

        File replayFile = new File(replayPath);
        if (!replayFile.exists()) {
            failMessage = "Replay file not found";
            Debug.e("Replay file not found");
            return false;
        }

        var post = new FormDataPostBuilder();
        post.addParam("userID", String.valueOf(userId));
        post.addParam("ssid", ssid);
        post.addParam("filename", beatmap.getFullBeatmapName().trim());
        post.addParam("hash", beatmap.getMD5());
        post.addParam("data", scoreData);
        post.addParam("version", onlineVersion);
        // Tells the server that this build already uses the osu!standard score
        // formula; older builds omit it and their raw score is converted.
        post.addParam("scoreVersion", "2");
        // Used only when the server cannot download the beatmap and therefore
        // has nothing to score; without it those plays were stored as 0 PP.
        post.addParam("localPP", String.valueOf(localPP));
        // Cheat plays still submit and count in their mode pool; the flag keeps
        // the separate cheat board and ranked-score ladder accurate.
        post.addParam("cheat", Config.hasCheatsEnabled() ? "1" : "0");

        post.addParam("cs", String.valueOf(beatmap.getCircleSize()));
        post.addParam("ar", String.valueOf(beatmap.getApproachRate()));
        post.addParam("od", String.valueOf(beatmap.getOverallDifficulty()));
        post.addParam("hp", String.valueOf(beatmap.getHpDrainRate()));

        MediaType replayMime = MediaType.parse("application/octet-stream");
        RequestBody replayFileBody = RequestBody.create(replayFile, replayMime);

        post.addParam("replayFile", replayFile.getName(), replayFileBody);
        post.addParam("replayFileChecksum", FileUtils.getSHA256Checksum(replayFile));

        ArrayList<String> response = sendRequest(post, endpoint + "submit.php");

        if (response == null) {
            return false;
        }

        if (failMessage.equals("Invalid record data"))
            return false;

        if (response.size() < 2) {
            failMessage = "Invalid server response";
            return false;
        }

        String[] resp = response.get(1).split("\\s+");
        if (resp.length < 4) {
            failMessage = "Invalid server response";
            return false;
        }

        rank = Integer.parseInt(resp[0]);
        score = Long.parseLong(resp[1]);
        accuracy = Float.parseFloat(resp[2]);
        mapRank = Integer.parseInt(resp[3]);
        pp = Float.parseFloat(resp[4]);
        // submit.php sends the cumulative overall PP as the optional sixth value.
        totalDpp = resp.length >= 6 ? Float.parseFloat(resp[5]) : pp;
        // The seventh value is how much the profile actually gained. The fifth
        // value is only what the play is worth and can be non-zero even when the
        // weighted top-100 does not change.
        ppGain = resp.length >= 7 ? Float.parseFloat(resp[6]) : pp;
        // Eighth value: the overall with cheat plays included.
        totalDppAll = resp.length >= 8 ? Float.parseFloat(resp[7]) : totalDpp;

        return true;
    }

    public ArrayList<String> getTop(final String hash) throws OnlineManagerException {
        PostBuilder post = new URLEncodedPostBuilder();
        post.addParam("hash", hash);
        post.addParam("uid", String.valueOf(userId));

        post.addParam("type", Config.getBeatmapLeaderboardScoringMode().getApiType());
        post.addParam(
            "limit",
            String.valueOf(Config.isBeatmapLeaderboardLimitEnabled()
                ? Config.getBeatmapLeaderboardMaxPlayers()
                : 100)
        );

        ArrayList<String> response = sendRequest(post, endpoint + "getrank.php");

        if (response == null) {
            return new ArrayList<>();
        }

        response.remove(0);

        return response;
    }

    public RankedStatus getBeatmapStatus(String md5) throws OnlineManagerException {
        // The map's ranked status is owned by our server, not osu.direct. This
        // keeps every map on the server ranked even when the upstream mirror
        // reports it as graveyard/pending or does not know it at all.
        var builder = new Request.Builder().url(endpoint + "v2/md5/" + md5);
        var request = builder.build();

        try (var response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                var json = new JSONObject(response.body().string());
                return RankedStatus.valueOf(json.optInt("ranked", 1));
            }
        } catch (final IOException e) {
            Debug.e("getBeatmapStatus IOException " + e.getMessage(), e);
        } catch (final JSONException e) {
            Debug.e("getBeatmapStatus JSONException " + e.getMessage(), e);
        } catch (final IllegalArgumentException e) {
            Debug.e("getBeatmapStatus IllegalArgumentException " + e.getMessage(), e);
        }

        return null;
    }

    public boolean loadAvatarToTextureManager() {
        return loadAvatarToTextureManager(avatarURL);
    }

    public boolean loadProfileBannerToTextureManager() {
        return loadProfileBannerToTextureManager(profileBannerURL);
    }

    public boolean loadAvatarToTextureManager(String avatarURL) {
        if (avatarURL == null || avatarURL.isEmpty()) return false;

        String filename = MD5Calculator.getStringMD5(avatarURL);
        Debug.i("Loading avatar from " + avatarURL);
        Debug.i("filename = " + filename);
        File picfile = new File(Config.getCachePath(), filename);
        OnlineFileOperator.downloadFile(avatarURL, picfile.getAbsolutePath(), true);

        var bitmap = loadFileToBitmap(picfile);
        int imageWidth = 0, imageHeight = 0;

        if (bitmap != null) {
            imageWidth = bitmap.getWidth();
            imageHeight = bitmap.getHeight();
        }

        if (imageWidth * imageHeight > 0) {
            // Avatar has been cached locally
            ResourceManager.getInstance().loadHighQualityFile(filename, picfile);
            if (ResourceManager.getInstance().getAvatarTextureIfLoaded(avatarURL) != null) {
                return true;
            }
        } else {
            // Avatar not found, download the default avatar
            String defaultAvatarFilename = MD5Calculator.getStringMD5(defaultAvatarURL);
            File avatarFile = new File(Config.getCachePath(), defaultAvatarFilename);
            OnlineFileOperator.downloadFile(defaultAvatarURL, avatarFile.getAbsolutePath());

            bitmap = loadFileToBitmap(avatarFile);
            if (bitmap != null) {
                imageWidth = bitmap.getWidth();
                imageHeight = bitmap.getHeight();
            }

            if (imageWidth * imageHeight > 0) {
                //Avatar has been cached locally
                ResourceManager.getInstance().loadHighQualityFile(defaultAvatarFilename, avatarFile);
                if (ResourceManager.getInstance().getAvatarTextureIfLoaded(defaultAvatarURL) != null) {
                    return true;
                }
            }
        }

        Debug.i("Success!");
        return false;
    }

    public boolean loadProfileBannerToTextureManager(String bannerURL) {
        if (bannerURL == null || bannerURL.isEmpty()) return false;

        if (ResourceManager.getInstance().getProfileBannerTextureIfLoaded(bannerURL) != null) {
            return true;
        }

        String filename = MD5Calculator.getStringMD5(bannerURL);
        Debug.i("Loading profile banner from " + bannerURL);
        File bannerFile = new File(Config.getCachePath(), filename);
        OnlineFileOperator.downloadFile(bannerURL, bannerFile.getAbsolutePath(), true);

        var bitmap = loadFileToBitmap(bannerFile);
        int imageWidth = 0, imageHeight = 0;

        if (bitmap != null) {
            imageWidth = bitmap.getWidth();
            imageHeight = bitmap.getHeight();
        }

        if (imageWidth * imageHeight <= 0) {
            return false;
        }

        ResourceManager.getInstance().loadHighQualityFile(filename, bannerFile);
        return ResourceManager.getInstance().getProfileBannerTextureIfLoaded(bannerURL) != null;
    }

    private Bitmap loadFileToBitmap(File file) {
        if (!file.exists()) {
            return null;
        }

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            return BitmapFactory.decodeFile(file.getPath());
        } catch (NullPointerException e) {
            return null;
        }
    }

    public String getScorePack(int playid) throws OnlineManagerException {
        PostBuilder post = new URLEncodedPostBuilder();
        post.addParam("playID", String.valueOf(playid));

        post.addParam("type", Config.getBeatmapLeaderboardScoringMode().getApiType());

        ArrayList<String> response = sendRequest(post, endpoint + "gettop.php");

        if (response == null || response.size() < 2) {
            return "";
        }

        return response.get(1);
    }

    public String getFailMessage() {
        return failMessage;
    }

    public long getRank() {
        return rank;
    }

    public long getScore() {
        return score;
    }

    public float getPP() {
        return pp;
    }

    public float getPPGain() {
        return ppGain;
    }

    public float getTotalDpp() {
        return totalDpp;
    }

    public float getTotalDppAll() {
        return totalDppAll;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public String getAvatarURL() {
        return avatarURL;
    }

    public String getProfileBannerURL() {
        return profileBannerURL;
    }

    public String getUsername() {
        return username;
    }

    public long getUserId() {
        return userId;
    }

    public String getSessionId() {
        return ssid;
    }

    public String getPassword() {
        return password;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public boolean isStayOnline() {
        return stayOnline;
    }

    public void setStayOnline(boolean stayOnline) {
        this.stayOnline = stayOnline;
    }

    public int getMapRank() {
        return mapRank;
    }

    /**
     * Equivalent to PHP's htmlspecialchars() with the default flags, which is what
     * the server applies to the password before hashing.
     */
    private static String escapeHTMLSpecialCharacters(String str) {
        return str
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /** Equivalent to PHP's addslashes(). */
    private static String addSlashes(String str) {
        return str
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\\", "\\\\");
    }

    public static class OnlineManagerException extends Exception {
        private static final long serialVersionUID = -5703212596292949401L;

        public OnlineManagerException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public OnlineManagerException(final String message) {
            super(message);
        }
    }

}