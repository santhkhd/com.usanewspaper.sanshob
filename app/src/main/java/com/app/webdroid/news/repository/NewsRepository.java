package com.app.webdroid.news.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.app.webdroid.Config;
import com.app.webdroid.news.model.NewsCategoriesResponse;
import com.app.webdroid.news.model.NewsResponse;
import com.app.webdroid.news.model.NewsStory;
import com.app.webdroid.news.network.NewsApiClient;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsRepository {

    private static final String PREFS_NAME = "us_news_repo_prefs";
    private static final String KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp";
    private static final String CACHE_FILE_NAME = "us_news_cache.json";
    private static final String CATEGORIES_CACHE_FILE = "us_news_categories_cache.json";

    private static volatile NewsRepository instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final Handler mainHandler;

    public interface NewsCallback {
        void onSuccess(List<NewsStory> stories, boolean isFromCache, long lastUpdatedMinutesAgo);
        void onError(String message);
    }

    public interface CategoriesCallback {
        void onSuccess(List<NewsCategoriesResponse.CategoryItem> categories);
        void onError(String message);
    }

    private NewsRepository(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static NewsRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (NewsRepository.class) {
                if (instance == null) {
                    instance = new NewsRepository(context);
                }
            }
        }
        return instance;
    }

    /**
     * Checks whether the device currently has active network connectivity.
     */
    public boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Primary method to fetch news stories.
     * Tries live network fetch first. If network is offline or fails, falls back gracefully to offline cache.
     */
    public void getNews(@NonNull NewsCallback callback) {
        String remoteUrl = Config.US_NEWS_GITHUB_RAW_URL;

        // If URL is empty or unconfigured, load from local cache or assets
        if (TextUtils.isEmpty(remoteUrl) || !isNetworkAvailable()) {
            loadFromOfflineCacheOrAsset(callback, "You're offline. Showing previously cached news.");
            return;
        }

        NewsApiClient.getApiService().getNewsFeed(remoteUrl).enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsResponse> call, @NonNull Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStories() != null) {
                    List<NewsStory> stories = response.body().getStories();
                    // Save to local cache in background
                    saveNewsToCache(response.body());
                    long now = System.currentTimeMillis();
                    prefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, now).apply();
                    callback.onSuccess(stories, false, 0);
                } else {
                    // Fallback to cache on non-200 HTTP code
                    loadFromOfflineCacheOrAsset(callback, "Unable to reach server. Showing cached stories.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<NewsResponse> call, @NonNull Throwable t) {
                // Fallback to cache on timeout or network error
                loadFromOfflineCacheOrAsset(callback, "Network error. Showing cached stories.");
            }
        });
    }

    /**
     * Fetches categories list.
     */
    public void getCategories(@NonNull CategoriesCallback callback) {
        String remoteUrl = Config.US_NEWS_CATEGORIES_RAW_URL;

        if (TextUtils.isEmpty(remoteUrl) || !isNetworkAvailable()) {
            loadCategoriesFromCacheOrAsset(callback);
            return;
        }

        NewsApiClient.getApiService().getCategories(remoteUrl).enqueue(new Callback<NewsCategoriesResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsCategoriesResponse> call, @NonNull Response<NewsCategoriesResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCategories() != null) {
                    saveCategoriesToCache(response.body());
                    callback.onSuccess(response.body().getCategories());
                } else {
                    loadCategoriesFromCacheOrAsset(callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<NewsCategoriesResponse> call, @NonNull Throwable t) {
                loadCategoriesFromCacheOrAsset(callback);
            }
        });
    }

    /**
     * Loads news from internal storage cache or bundled assets.
     */
    private void loadFromOfflineCacheOrAsset(@NonNull NewsCallback callback, String fallbackMsg) {
        long lastSync = prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0);
        long ageMinutes = lastSync > 0 ? Math.max(0, (System.currentTimeMillis() - lastSync) / 60000) : 0;

        // 1. Try internal storage cache
        File cacheFile = new File(context.getFilesDir(), CACHE_FILE_NAME);
        if (cacheFile.exists()) {
            try (FileInputStream fis = new FileInputStream(cacheFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
                NewsResponse cached = gson.fromJson(reader, NewsResponse.class);
                if (cached != null && cached.getStories() != null && !cached.getStories().isEmpty()) {
                    callback.onSuccess(cached.getStories(), true, ageMinutes);
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        // 2. Try bundled asset fallback
        try (InputStream is = context.getAssets().open("us_news.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            NewsResponse assetNews = gson.fromJson(reader, NewsResponse.class);
            if (assetNews != null && assetNews.getStories() != null && !assetNews.getStories().isEmpty()) {
                callback.onSuccess(assetNews.getStories(), true, ageMinutes);
                return;
            }
        } catch (Exception ignored) {
        }

        // Empty state
        callback.onSuccess(new ArrayList<>(), true, 0);
    }

    private void loadCategoriesFromCacheOrAsset(@NonNull CategoriesCallback callback) {
        File cacheFile = new File(context.getFilesDir(), CATEGORIES_CACHE_FILE);
        if (cacheFile.exists()) {
            try (FileInputStream fis = new FileInputStream(cacheFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
                NewsCategoriesResponse cached = gson.fromJson(reader, NewsCategoriesResponse.class);
                if (cached != null && cached.getCategories() != null && !cached.getCategories().isEmpty()) {
                    callback.onSuccess(cached.getCategories());
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        try (InputStream is = context.getAssets().open("categories.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            NewsCategoriesResponse assetCats = gson.fromJson(reader, NewsCategoriesResponse.class);
            if (assetCats != null && assetCats.getCategories() != null && !assetCats.getCategories().isEmpty()) {
                callback.onSuccess(assetCats.getCategories());
                return;
            }
        } catch (Exception ignored) {
        }

        callback.onSuccess(new ArrayList<>());
    }

    private void saveNewsToCache(NewsResponse response) {
        new Thread(() -> {
            try {
                File file = new File(context.getFilesDir(), CACHE_FILE_NAME);
                String json = gson.toJson(response);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(json.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void saveCategoriesToCache(NewsCategoriesResponse response) {
        new Thread(() -> {
            try {
                File file = new File(context.getFilesDir(), CATEGORIES_CACHE_FILE);
                String json = gson.toJson(response);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(json.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {
            }
        }).start();
    }
}
