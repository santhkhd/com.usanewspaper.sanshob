package com.app.webdroid.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "history", indices = { @Index(value = { "itemId", "type" }, unique = true) })
public class HistoryItem implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String itemId; // videoId, URL, or movie title
    public String type;   // "YOUTUBE", "MOVIES", "IPTV", "SONG", "WEB"
    public String title;
    public String subtitle; // Channel name, year, or artist
    public String imageUrl;
    public String targetUrl; // Playable link or content URL
    public long watchedAt;   // System.currentTimeMillis()

    public static final String TYPE_YOUTUBE = "YOUTUBE";
    public static final String TYPE_MOVIES = "MOVIES";
    public static final String TYPE_IPTV = "IPTV";
    public static final String TYPE_SONG = "SONG";
    public static final String TYPE_WEB = "WEB";

    public HistoryItem() {
    }

    @Ignore
    public HistoryItem(String itemId, String type, String title, String subtitle, String imageUrl, String targetUrl, long watchedAt) {
        this.itemId = itemId;
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.targetUrl = targetUrl;
        this.watchedAt = watchedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistoryItem that = (HistoryItem) o;
        if (id > 0 && that.id > 0 && id == that.id) return true;
        if (itemId != null && !itemId.isEmpty() && that.itemId != null && itemId.equalsIgnoreCase(that.itemId)) {
            if (type != null && that.type != null) {
                return type.equalsIgnoreCase(that.type);
            }
            return true;
        }
        if (targetUrl != null && !targetUrl.isEmpty() && that.targetUrl != null && targetUrl.equalsIgnoreCase(that.targetUrl)) return true;
        if (title != null && !title.isEmpty() && that.title != null && title.equalsIgnoreCase(that.title)) return true;
        return false;
    }

    @Override
    public int hashCode() {
        if (id > 0) return Long.hashCode(id);
        if (itemId != null && !itemId.isEmpty()) return itemId.hashCode();
        if (targetUrl != null && !targetUrl.isEmpty()) return targetUrl.hashCode();
        if (title != null && !title.isEmpty()) return title.hashCode();
        return super.hashCode();
    }
}
