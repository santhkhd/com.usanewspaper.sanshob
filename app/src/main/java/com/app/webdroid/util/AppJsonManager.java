package com.app.webdroid.util;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.app.webdroid.Config;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Loads JSON configuration and content files from online GitHub repository first.
 * If GitHub is down, offline, or network fails, automatically falls back to in-app assets.
 */
public class AppJsonManager {
    private static final String TAG = "AppJsonManager";
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build();

    public interface JsonCallback {
        void onSuccess(String json, boolean fromOnline);
        void onFallback(String json);
        void onError(String error);
    }

    public static String getOnlineUrl(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isEmpty()) return null;
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return pathOrUrl;
        }
        if (Config.GITHUB_ASSETS_BASE_URL != null && !Config.GITHUB_ASSETS_BASE_URL.isEmpty()) {
            return Config.GITHUB_ASSETS_BASE_URL + pathOrUrl;
        }
        return null;
    }

    public static String getAssetName(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isEmpty()) return null;
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            int lastSlash = pathOrUrl.lastIndexOf('/');
            if (lastSlash != -1 && lastSlash < pathOrUrl.length() - 1) {
                String candidate = pathOrUrl.substring(lastSlash + 1);
                int queryIdx = candidate.indexOf('?');
                if (queryIdx != -1) candidate = candidate.substring(0, queryIdx);
                return candidate;
            }
            return null;
        }
        return pathOrUrl;
    }

    public static String loadFromAssets(@NonNull Context context, @NonNull String assetName) {
        try (InputStream is = context.getAssets().open(assetName);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[16384];
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            return baos.toString("UTF-8");
        } catch (Exception e) {
            Log.d(TAG, "Asset not found: " + assetName + " (" + e.getMessage() + ")");
            return null;
        }
    }

    public static String loadFromCache(@NonNull Context context, @NonNull String assetName) {
        try {
            File cacheFile = new File(context.getCacheDir(), "json_cache/" + assetName);
            if (cacheFile.exists() && cacheFile.length() > 0) {
                try (InputStream is = new FileInputStream(cacheFile);
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[16384];
                    int len;
                    while ((len = is.read(buf)) != -1) {
                        baos.write(buf, 0, len);
                    }
                    return baos.toString("UTF-8");
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static void saveToCache(@NonNull Context context, @NonNull String assetName, @NonNull String json) {
        try {
            File dir = new File(context.getCacheDir(), "json_cache");
            if (!dir.exists()) dir.mkdirs();
            File cacheFile = new File(dir, assetName);
            try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                fos.write(json.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
    }

    public static void clearCache(@NonNull Context context) {
        try {
            File dir = new File(context.getCacheDir(), "json_cache");
            if (dir.exists()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) f.delete();
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Loads JSON with online GitHub link first; if GitHub is down, offline, or errors,
     * falls back automatically to the in-app assets folder.
     */
    public static void loadJson(@NonNull Context context, @NonNull String pathOrUrl, @NonNull JsonCallback callback) {
        String assetName = getAssetName(pathOrUrl);
        String onlineUrl = getOnlineUrl(pathOrUrl);

        if (onlineUrl == null) {
            // No remote URL configured: directly load the clean in-app asset
            String assetJson = assetName != null ? loadFromAssets(context, assetName) : null;
            if (assetJson != null) {
                callback.onSuccess(assetJson, false);
            } else {
                callback.onError("Asset not found: " + pathOrUrl);
            }
            return;
        }

        if (!Tools.isOnline(context)) {
            // Offline: check cache first, then fallback to assets
            String cached = assetName != null ? loadFromCache(context, assetName) : null;
            if (cached != null && !cached.isEmpty()) {
                callback.onSuccess(cached, false);
                return;
            }
            String assetJson = assetName != null ? loadFromAssets(context, assetName) : null;
            if (assetJson != null) {
                callback.onFallback(assetJson);
            } else {
                callback.onError("Offline and asset not found: " + pathOrUrl);
            }
            return;
        }

        // Online: fetch from GitHub CDN or Raw
        Request request = new Request.Builder().url(onlineUrl).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull java.io.IOException e) {
                Log.w(TAG, "Request failed (" + onlineUrl + "): " + e.getMessage() + " -> Attempting backup or fallback");
                tryBackupOrFallback();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String json = response.body().string();
                        if (json != null && !json.trim().isEmpty() && (json.trim().startsWith("{") || json.trim().startsWith("["))) {
                            if (assetName != null) {
                                saveToCache(context, assetName, json);
                            }
                            callback.onSuccess(json, true);
                            return;
                        }
                    }
                } catch (Exception ignored) {}
                tryBackupOrFallback();
            }

            private void tryBackupOrFallback() {
                if (assetName != null && onlineUrl.contains("cdn.jsdelivr.net") && Config.GITHUB_RAW_BASE_URL != null) {
                    String rawBackupUrl = Config.GITHUB_RAW_BASE_URL + assetName;
                    Request backupReq = new Request.Builder().url(rawBackupUrl).build();
                    httpClient.newCall(backupReq).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call c, @NonNull java.io.IOException e) {
                            fallback();
                        }

                        @Override
                        public void onResponse(@NonNull Call c, @NonNull Response r) {
                            try {
                                if (r.isSuccessful() && r.body() != null) {
                                    String json = r.body().string();
                                    if (json != null && !json.trim().isEmpty() && (json.trim().startsWith("{") || json.trim().startsWith("["))) {
                                        saveToCache(context, assetName, json);
                                        callback.onSuccess(json, true);
                                        return;
                                    }
                                }
                            } catch (Exception ignored) {}
                            fallback();
                        }
                    });
                } else {
                    fallback();
                }
            }

            private void fallback() {
                String assetJson = assetName != null ? loadFromAssets(context, assetName) : null;
                if (assetJson != null) {
                    callback.onFallback(assetJson);
                    return;
                }
                String cached = assetName != null ? loadFromCache(context, assetName) : null;
                if (cached != null && !cached.isEmpty()) {
                    callback.onSuccess(cached, false);
                    return;
                }
                callback.onError("Asset not found: " + pathOrUrl);
            }
        });
    }

    /**
     * Synchronous version for background threads (e.g., HomeDiscovery background loader).
     */
    @Nullable
    public static String loadJsonSync(@NonNull Context context, @NonNull String pathOrUrl) {
        String assetName = getAssetName(pathOrUrl);
        String onlineUrl = getOnlineUrl(pathOrUrl);

        // 1. If no online URL configured, ALWAYS load directly from APK assets!
        if (onlineUrl == null) {
            return assetName != null ? loadFromAssets(context, assetName) : null;
        }

        if (Tools.isOnline(context)) {
            try {
                Request request = new Request.Builder().url(onlineUrl).build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String json = response.body().string();
                        if (json != null && !json.trim().isEmpty() && (json.trim().startsWith("{") || json.trim().startsWith("["))) {
                            if (assetName != null) {
                                saveToCache(context, assetName, json);
                            }
                            return json;
                        }
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "Sync fetch failed for " + onlineUrl + ": " + e.getMessage());
            }

            // If CDN failed, attempt GitHub raw backup
            if (assetName != null && onlineUrl.contains("cdn.jsdelivr.net") && Config.GITHUB_RAW_BASE_URL != null) {
                try {
                    String rawBackupUrl = Config.GITHUB_RAW_BASE_URL + assetName;
                    Request backupReq = new Request.Builder().url(rawBackupUrl).build();
                    try (Response response = httpClient.newCall(backupReq).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String json = response.body().string();
                            if (json != null && !json.trim().isEmpty() && (json.trim().startsWith("{") || json.trim().startsWith("["))) {
                                saveToCache(context, assetName, json);
                                return json;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        // Fallback: ALWAYS prefer in-app asset first, then cache as last resort
        if (assetName != null) {
            String assetJson = loadFromAssets(context, assetName);
            if (assetJson != null && !assetJson.isEmpty()) return assetJson;
            String cached = loadFromCache(context, assetName);
            if (cached != null && !cached.isEmpty()) return cached;
        }

        return null;
    }
}
