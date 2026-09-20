package com.app.webdroid.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "rss_news", indices = { @Index(value = "link", unique = true) })
public class NewsItem implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;
    public String description;
    public String imageUrl;
    public String pubDate; // stored as string or long timestamp
    public long pubDateMillis;
    public String sourceName;
    public String link;

    // Constructor
    public NewsItem() {
    }

    @androidx.room.Ignore
    public NewsItem(String title, String description, String imageUrl, String pubDate, long pubDateMillis,
            String sourceName, String link) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.pubDate = pubDate;
        this.pubDateMillis = pubDateMillis;
        this.sourceName = sourceName;
        this.link = link;
    }

    @androidx.room.Ignore
    public boolean isNativeAd = false;

    @androidx.room.Ignore
    public String sourceUrl;

    public boolean hasRealImage() {
        return imageUrl != null
                && !imageUrl.trim().isEmpty()
                && !imageUrl.contains("google.com/s2/favicons")
                && !imageUrl.contains("icons8.com")
                && !imageUrl.endsWith(".ico")
                && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"));
    }

    public String getSourceIconUrl() {
        if (sourceUrl != null && !sourceUrl.isEmpty()) {
            try {
                String host = new java.net.URL(sourceUrl).getHost();
                if (host != null && !host.isEmpty()) {
                    return "https://www.google.com/s2/favicons?sz=128&domain=" + host;
                }
            } catch (Exception ignored) {
            }
        }
        if (link != null && !link.isEmpty()) {
            try {
                String host = new java.net.URL(link).getHost();
                if (host != null && !host.isEmpty() && !host.contains("google.com")) {
                    return "https://www.google.com/s2/favicons?sz=128&domain=" + host;
                }
            } catch (Exception ignored) {
            }
        }
        if (sourceName != null && !sourceName.trim().isEmpty()) {
            String clean = sourceName.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
            return "https://www.google.com/s2/favicons?sz=128&domain=" + clean + ".com";
        }
        return null;
    }
}
