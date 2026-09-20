package com.app.webdroid.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.app.webdroid.Config;
import com.shobmc.san.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FollowManager {

    private static final String PREF_NAME = "category_follow_prefs";
    private static final String KEY_FOLLOWED_SET = "followed_categories_set";
    private static final String KEY_FOLLOWER_DELTA = "follower_delta_";
    private static final String KEY_REMOTE_COUNT = "remote_count_";

    public static class CategoryMeta {
        public final String key;
        public final String title;
        public final String description;
        public final int iconRes;
        public final String coverUrl;
        public final String avatarUrl;
        public final long baseFollowers;
        public final String postsCount;
        public final String viewsCount;

        public CategoryMeta(String key, String title, String description, int iconRes,
                            String coverUrl, String avatarUrl, long baseFollowers,
                            String postsCount, String viewsCount) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.iconRes = iconRes;
            this.coverUrl = coverUrl;
            this.avatarUrl = avatarUrl;
            this.baseFollowers = baseFollowers;
            this.postsCount = postsCount;
            this.viewsCount = viewsCount;
        }
    }

    private static final Map<String, CategoryMeta> CATEGORY_REGISTRY = new HashMap<>();

    static {
        // 1. Technology (Matching Screenshot 1)
        CATEGORY_REGISTRY.put("TECH", new CategoryMeta(
                "TECH",
                "Technology",
                "Click here to show all news about Technology Topic",
                R.drawable.ic_memory,
                "https://images.unsplash.com/photo-1518770660439-4636190af475?w=1200&q=80",
                "https://images.unsplash.com/photo-1518770660439-4636190af475?w=200&q=80",
                218400L,
                "2.7M",
                "23.2M"
        ));

        // 2. Health (Matching Screenshot 2)
        CATEGORY_REGISTRY.put("HEALTH", new CategoryMeta(
                "HEALTH",
                "Health",
                "Click here to show all news about Health Topic",
                R.drawable.ic_favorite,
                "https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=1200&q=80",
                "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=200&q=80",
                290500L,
                "884.5k",
                "85.2M"
        ));

        // 3. Sports
        CATEGORY_REGISTRY.put("SPORTS", new CategoryMeta(
                "SPORTS",
                "Sports",
                "Click here to show all news about Sports Topic",
                R.drawable.ic_trending_up,
                "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=1200&q=80",
                "https://images.unsplash.com/photo-1517649763962-0c623266ddc0?w=200&q=80",
                412800L,
                "3.1M",
                "92.4M"
        ));

        // 4. Politics
        CATEGORY_REGISTRY.put("POLITICS", new CategoryMeta(
                "POLITICS",
                "Politics",
                "Click here to show all news about Politics Topic",
                R.drawable.ic_newspaper,
                "https://images.unsplash.com/photo-1541872703-74c5e44368f9?w=1200&q=80",
                "https://images.unsplash.com/photo-1540910419892-4a36d2c3266c?w=200&q=80",
                345100L,
                "1.9M",
                "64.8M"
        ));

        // 5. Business
        CATEGORY_REGISTRY.put("BUSINESS", new CategoryMeta(
                "BUSINESS",
                "Business",
                "Click here to show all news about Business Topic",
                R.drawable.ic_trending_up,
                "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?w=1200&q=80",
                "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?w=200&q=80",
                180200L,
                "1.2M",
                "45.6M"
        ));

        // 6. Entertainment
        CATEGORY_REGISTRY.put("ENTERTAINMENT", new CategoryMeta(
                "ENTERTAINMENT",
                "Entertainment",
                "Click here to show all news about Entertainment Topic",
                R.drawable.ic_movie,
                "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=1200&q=80",
                "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=200&q=80",
                275600L,
                "2.4M",
                "78.1M"
        ));

        // 7. World
        CATEGORY_REGISTRY.put("WORLD", new CategoryMeta(
                "WORLD",
                "World",
                "Click here to show all news about World Topic",
                R.drawable.ic_public,
                "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1200&q=80",
                "https://images.unsplash.com/photo-1526778548025-fa2f459cd5c1?w=200&q=80",
                192400L,
                "1.6M",
                "52.0M"
        ));

        // 8. Local News
        CATEGORY_REGISTRY.put("LOCAL", new CategoryMeta(
                "LOCAL",
                "Local news",
                "Click here to show all news about Local news Topic",
                R.drawable.ic_home,
                "https://images.unsplash.com/photo-1477959858617-67f30bc75b82?w=1200&q=80",
                "https://images.unsplash.com/photo-1480714378408-67cf0d13bc1b?w=200&q=80",
                98300L,
                "450.0k",
                "21.5M"
        ));

        // 9. Weather
        CATEGORY_REGISTRY.put("WEATHER", new CategoryMeta(
                "WEATHER",
                "Weather",
                "Click here to show all news about Weather Topic",
                R.drawable.ic_wb_sunny,
                "https://images.unsplash.com/photo-1504608524841-42fe6f032b4b?w=1200&q=80",
                "https://images.unsplash.com/photo-1592210454359-9043f067919b?w=200&q=80",
                156700L,
                "620.0k",
                "38.9M"
        ));
    }

    public static CategoryMeta getCategoryMeta(String key) {
        if (key == null) return null;
        String upper = key.toUpperCase(Locale.US);
        CategoryMeta meta = CATEGORY_REGISTRY.get(upper);
        if (meta != null) return meta;

        // Dynamic fallback for any category from categories.json
        String title = key.replace("_", " ");
        String[] words = title.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase(Locale.US)).append(" ");
            }
        }
        String cleanTitle = sb.toString().trim();
        return new CategoryMeta(
                upper,
                cleanTitle,
                "Click here to show all news about " + cleanTitle + " Topic",
                R.drawable.ic_newspaper,
                "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=1200&q=80",
                "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=200&q=80",
                125000L,
                "1.2M",
                "35.0M"
        );
    }

    public static List<CategoryMeta> getAllCategoryMetas() {
        return new ArrayList<>(CATEGORY_REGISTRY.values());
    }

    public static boolean isFollowed(Context context, String categoryKey) {
        if (context == null || categoryKey == null) return false;
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> set = sp.getStringSet(KEY_FOLLOWED_SET, Collections.emptySet());
        return set.contains(categoryKey.toUpperCase(Locale.US));
    }

    public static boolean toggleFollow(Context context, String categoryKey) {
        boolean currently = isFollowed(context, categoryKey);
        setFollowed(context, categoryKey, !currently);
        return !currently;
    }

    public static void setFollowed(Context context, String categoryKey, boolean followed) {
        if (context == null || categoryKey == null) return;
        String normalizedKey = categoryKey.toUpperCase(Locale.US);
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> existing = new HashSet<>(sp.getStringSet(KEY_FOLLOWED_SET, Collections.emptySet()));

        int delta = sp.getInt(KEY_FOLLOWER_DELTA + normalizedKey, 0);

        if (followed) {
            existing.add(normalizedKey);
            delta += 1;
        } else {
            existing.remove(normalizedKey);
            delta -= 1;
        }

        sp.edit()
                .putStringSet(KEY_FOLLOWED_SET, existing)
                .putInt(KEY_FOLLOWER_DELTA + normalizedKey, delta)
                .apply();

        // Asynchronous sync to Google Sheets backend
        syncWithGoogleSheet(context, normalizedKey, followed);
    }

    public static Set<String> getFollowedCategories(Context context) {
        if (context == null) return Collections.emptySet();
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return new HashSet<>(sp.getStringSet(KEY_FOLLOWED_SET, Collections.emptySet()));
    }

    public static String getFormattedFollowers(Context context, String categoryKey) {
        if (categoryKey == null) return "0";
        CategoryMeta meta = getCategoryMeta(categoryKey);
        long base = meta != null ? meta.baseFollowers : 100000L;

        if (context != null) {
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            long remote = sp.getLong(KEY_REMOTE_COUNT + categoryKey.toUpperCase(Locale.US), -1);
            if (remote > 0) {
                base = remote;
            }
            int delta = sp.getInt(KEY_FOLLOWER_DELTA + categoryKey.toUpperCase(Locale.US), 0);
            base += delta;
        }

        return formatMetric(Math.max(base, 0));
    }

    public static String formatMetric(long number) {
        if (number >= 1_000_000) {
            return new DecimalFormat("#.#M").format(number / 1_000_000.0);
        } else if (number >= 1_000) {
            return new DecimalFormat("#.#k").format(number / 1_000.0);
        } else {
            return String.valueOf(number);
        }
    }

    /**
     * Send follow / unfollow signal to Google Sheets Web App backend
     */
    private static void syncWithGoogleSheet(Context context, String categoryKey, boolean isFollow) {
        String endpoint = Config.GOOGLE_SHEET_WEBAPP_URL;
        if (endpoint == null || endpoint.trim().isEmpty() || endpoint.startsWith("0000")) return;

        final String deviceId = android.provider.Settings.Secure.getString(
                context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        new Thread(() -> {
            try {
                String action = isFollow ? "follow" : "unfollow";
                String urlStr = endpoint + (endpoint.contains("?") ? "&" : "?")
                        + "action=" + action
                        + "&category=" + URLEncoder.encode(categoryKey, "UTF-8")
                        + "&deviceId=" + URLEncoder.encode(deviceId != null ? deviceId : "android_user", "UTF-8");

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                int status = conn.getResponseCode();
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == 307 || status == 303) {
                    String redirectUrl = conn.getHeaderField("Location");
                    conn.disconnect();
                    conn = (HttpURLConnection) new URL(redirectUrl).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);
                    status = conn.getResponseCode();
                }

                if (status == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    if ("success".equalsIgnoreCase(json.optString("status"))) {
                        long totalFollowers = json.optLong("followers", -1);
                        if (totalFollowers > 0 && context != null) {
                            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                            sp.edit()
                                    .putLong(KEY_REMOTE_COUNT + categoryKey, totalFollowers)
                                    .putInt(KEY_FOLLOWER_DELTA + categoryKey, 0)
                                    .apply();
                        }
                    }
                }
                conn.disconnect();
            } catch (Exception ignored) {}
        }).start();
    }

    /**
     * Fetch all remote category follower counts from Google Sheets
     */
    public static void fetchAllRemoteFollowers(Context context) {
        String endpoint = Config.GOOGLE_SHEET_WEBAPP_URL;
        if (endpoint == null || endpoint.trim().isEmpty() || endpoint.startsWith("0000")) return;

        new Thread(() -> {
            try {
                String urlStr = endpoint + (endpoint.contains("?") ? "&" : "?") + "action=getfollowers";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                int status = conn.getResponseCode();
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == 307 || status == 303) {
                    String redirectUrl = conn.getHeaderField("Location");
                    conn.disconnect();
                    conn = (HttpURLConnection) new URL(redirectUrl).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);
                    status = conn.getResponseCode();
                }

                if (status == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONObject followersMap = json.optJSONObject("followers");
                    if (followersMap != null && context != null) {
                        SharedPreferences.Editor ed = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
                        for (String key : CATEGORY_REGISTRY.keySet()) {
                            long count = followersMap.optLong(key, -1);
                            if (count > 0) {
                                ed.putLong(KEY_REMOTE_COUNT + key, count);
                            }
                        }
                        ed.apply();
                    }
                }
                conn.disconnect();
            } catch (Exception ignored) {}
        }).start();
    }
}
