package com.app.webdroid.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.app.webdroid.model.AppConfig;

public class ChannelLogoCache {

    private static final String PREF_NAME = "channel_logo_cache_pref";

    public static String getCachedLogo(Context context, String channelId) {
        if (context == null || channelId == null || channelId.isEmpty()) {
            return null;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(channelId, null);
    }

    public static void saveLogo(Context context, String channelId, String logoUrl) {
        if (context == null || channelId == null || logoUrl == null || logoUrl.isEmpty()) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(channelId, logoUrl).apply();
    }

    public static String extractChannelId(AppConfig.OverviewItem item) {
        if (item == null) return null;
        if ("rss".equalsIgnoreCase(item.provider) || "videos".equalsIgnoreCase(item.provider)
                || "youtube_channel".equalsIgnoreCase(item.provider)) {
            String url = (item.arguments != null && !item.arguments.isEmpty())
                    ? item.arguments.get(0)
                    : "";
            if (url.contains("channel_id=")) {
                String channelId = url.substring(url.indexOf("channel_id=") + 11);
                if (channelId.contains("&")) {
                    channelId = channelId.substring(0, channelId.indexOf("&"));
                }
                return channelId;
            } else if (url.startsWith("UC") && url.length() == 24) {
                return url;
            }
        }
        return null;
    }
}
