package com.app.webdroid.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AppConfig {
    @SerializedName("rss_news")
    public List<RssSource> rssNews;

    @SerializedName("youtube_channels")
    public List<YouTubeSource> youtubeChannels;

    @SerializedName("webview_sites")
    public List<WebItem> webviewSites;

    @SerializedName("youtube_videos")
    public List<SpecificVideo> youtubeVideos;

    @SerializedName("menus")
    public List<Navigation> menus;

    public static class SpecificVideo {
        public String title;
        public String url;
    }

    @SerializedName("overview")
    public List<OverviewItem> overview;

    public static class OverviewItem {
        public String title;
        public String provider;
        public List<String> arguments;
        public String image;
        // Movie specific fields (optional in JSON)
        public String year;
        public String runtime;
        public String rating;
        public String released;
        public String genre;
        public String plot;
        public List<String> cast;
        public com.google.gson.JsonElement director;
        public boolean isNew = false;
        @SerializedName("ott_url")
        public String ottUrl;
        public String link;

        public String getDirectorString() {
            if (director == null || director.isJsonNull()) return "";
            try {
                if (director.isJsonArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (com.google.gson.JsonElement el : director.getAsJsonArray()) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(el.getAsString());
                    }
                    return sb.toString();
                } else if (director.isJsonPrimitive()) {
                    return director.getAsString();
                }
            } catch (Exception e) {
                // Ignore
            }
            return "";
        }
    }

    public static class RssSource {
        public String title;
        public String url;
        public String category;
    }

    public static class YouTubeSource {
        public String name;
        @SerializedName("channel_id")
        public String channelId;
    }
}
