package com.app.webdroid.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import com.app.webdroid.database.sqlite.DbNavigation;
import com.app.webdroid.model.AppConfig;
import com.app.webdroid.model.Navigation;
import com.app.webdroid.model.WebItem;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ConfigManager {
    private static ConfigManager instance;
    private AppConfig currentConfig;
    private static final String PREF_NAME = "app_config";
    private static final String KEY_CONFIG = "json_config";

    // Replace with actual URL or load from string resource
    private static final String CONFIG_URL = com.app.webdroid.Config.JSON_CONFIG_URL;

    private ConfigManager() {
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public void loadConfig(Context context, LoadCallback callback) {
        // First load from cache
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CONFIG, null);
        if (json != null) {
            try {
                currentConfig = new Gson().fromJson(json, AppConfig.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Fetch remote safely or load from in-app assets
        if (CONFIG_URL == null || CONFIG_URL.isEmpty() || !CONFIG_URL.startsWith("http")) {
            try {
                java.io.InputStream is = context.getAssets().open("config.json");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                String responseBody = new String(buffer, java.nio.charset.StandardCharsets.UTF_8);
                currentConfig = new Gson().fromJson(responseBody, AppConfig.class);
                updateNavigationFromConfig(context, currentConfig);
                prefs.edit().putString(KEY_CONFIG, responseBody).apply();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (currentConfig != null && callback != null) {
                callback.onConfigLoaded(currentConfig);
            }
            return;
        }

        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(CONFIG_URL).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (currentConfig != null) {
                        if (callback != null)
                            callback.onConfigLoaded(currentConfig);
                    } else {
                        if (callback != null)
                            callback.onError(e);
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        prefs.edit().putString(KEY_CONFIG, responseBody).apply();
                        currentConfig = new Gson().fromJson(responseBody, AppConfig.class);
                        updateNavigationFromConfig(context, currentConfig);
                        if (callback != null)
                            callback.onConfigLoaded(currentConfig);
                    } else {
                        if (currentConfig != null) {
                            if (callback != null)
                                callback.onConfigLoaded(currentConfig);
                        } else {
                            if (callback != null)
                                callback.onError(new IOException("Config Fetch Failed"));
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            if (currentConfig != null && callback != null) {
                callback.onConfigLoaded(currentConfig);
            }
        }
    }

    public AppConfig getConfig() {
        return currentConfig;
    }

    public void updateNavigationFromConfig(Context context, AppConfig config) {
        if (config == null)
            return;

        DbNavigation db = new DbNavigation(context);
        SQLiteDatabase database = db.getWritableDatabase();
        if (database == null) return;

        try {
            database.beginTransaction();
            db.truncateTableMenu(DbNavigation.TABLE_MENU);

            // Add Menus from new structure
            if (config.menus != null) {
                for (Navigation nav : config.menus) {
                    if ("url".equalsIgnoreCase(nav.type))
                        nav.type = "WEB";
                    db.addOneMenu(database, nav, DbNavigation.TABLE_MENU);
                }
            }

            // Add Specific YouTube Videos
            if (config.youtubeVideos != null && !config.youtubeVideos.isEmpty()) {
                String manualChannelId = "manual_config_videos";
                List<com.app.webdroid.model.YouTubeItem> items = new java.util.ArrayList<>();
                for (AppConfig.SpecificVideo video : config.youtubeVideos) {
                    String videoId = extractVideoId(video.url);
                    if (videoId != null) {
                        com.app.webdroid.model.YouTubeItem item = new com.app.webdroid.model.YouTubeItem();
                        item.videoId = videoId;
                        item.title = video.title;
                        item.channelId = manualChannelId;
                        item.channelName = "Featured Videos";
                        item.thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/0.jpg";
                        item.pubDate = "Featured";
                        item.description = "";
                        item.link = "https://www.youtube.com/watch?v=" + videoId;
                        items.add(item);
                    }
                }

                com.app.webdroid.database.AppDatabase appDb = com.app.webdroid.database.AppDatabase.getDatabase(context);
                com.app.webdroid.database.AppDatabase.databaseWriteExecutor.execute(() -> {
                    appDb.youTubeDao().insertVideos(items);
                });

                Navigation nav = new Navigation();
                nav.name = "Featured Videos";
                nav.type = "YOUTUBE";
                nav.url = manualChannelId;
                nav.icon = "https://cdn-icons-png.flaticon.com/512/3670/3670147.png";
                db.addOneMenu(database, nav, DbNavigation.TABLE_MENU);
            }

            // Add Overview Items (New Structure)
            if (config.overview != null) {
                for (AppConfig.OverviewItem item : config.overview) {
                    Navigation nav = new Navigation();
                    nav.name = item.title;
                    nav.icon = item.image;
                    if (nav.icon == null || nav.icon.isEmpty()) {
                        if (item.arguments != null && !item.arguments.isEmpty()) {
                            nav.icon = getFaviconUrl(item.arguments.get(0));
                        }
                    }

                    if ("rss".equalsIgnoreCase(item.provider)) {
                        nav.type = "RSS";
                        if (item.arguments != null && !item.arguments.isEmpty())
                            nav.url = item.arguments.get(0);
                    } else if ("movies".equalsIgnoreCase(item.provider)) {
                        nav.type = "MOVIES";
                        if (item.arguments != null && !item.arguments.isEmpty())
                            nav.url = item.arguments.get(0);
                    } else if ("overview".equalsIgnoreCase(item.provider)) {
                        nav.type = "CATEGORY";
                        if (item.arguments != null && !item.arguments.isEmpty())
                            nav.url = item.arguments.get(0);
                    } else if ("latest_videos".equalsIgnoreCase(item.provider)) {
                        nav.type = "YOUTUBE";
                        nav.url = "ALL_VIDEOS";
                    } else if ("favorites".equalsIgnoreCase(item.provider)) {
                        nav.type = "FAVORITES";
                    } else {
                        nav.type = "WEB";
                        if (item.arguments != null && !item.arguments.isEmpty())
                            nav.url = item.arguments.get(0);
                    }

                    db.addOneMenu(database, nav, DbNavigation.TABLE_MENU);
                }
            }

            // Add Web Items
            if (config.webviewSites != null) {
                for (WebItem site : config.webviewSites) {
                    Navigation nav = new Navigation();
                    nav.name = site.title;
                    nav.type = site.openType != null ? site.openType : "WEB";
                    nav.url = site.url;
                    nav.icon = site.iconUrl != null ? site.iconUrl
                            : "https://cdn-icons-png.flaticon.com/512/1006/1006771.png";
                    db.addOneMenu(database, nav, DbNavigation.TABLE_MENU);
                }
            }

            // Add Favorites
            Navigation navFav = new Navigation();
            navFav.name = "Favorites";
            navFav.type = "FAVORITES";
            navFav.icon = "https://cdn-icons-png.flaticon.com/512/2589/2589175.png";
            db.addOneMenu(database, navFav, DbNavigation.TABLE_MENU);

            database.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                database.endTransaction();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String extractVideoId(String url) {
        String videoId = null;
        if (url != null && url.trim().length() > 0) {
            String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";
            java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = compiledPattern.matcher(url);
            if (matcher.find()) {
                videoId = matcher.group();
            }
        }
        return videoId;
    }

    private String getFaviconUrl(String url) {
        if (url == null)
            return null;
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            return "https://www.google.com/s2/favicons?sz=128&domain=" + parsedUrl.getProtocol() + "://"
                    + parsedUrl.getHost();
        } catch (java.net.MalformedURLException e) {
            return "https://www.google.com/s2/favicons?sz=128&domain=" + url;
        }
    }

    public interface LoadCallback {
        void onConfigLoaded(AppConfig config);

        void onError(Exception e);
    }
}
