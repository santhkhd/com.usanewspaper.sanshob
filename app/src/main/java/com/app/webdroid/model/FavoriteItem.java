package com.app.webdroid.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "favorites", indices = { @Index(value = { "itemId", "type" }, unique = true) })
public class FavoriteItem implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String itemId; // Link for News/Web, VideoID for YouTube
    public String type; // "RSS", "YOUTUBE", "WEB"
    public String title;
    public String subtitle; // Date or Channel name
    public String imageUrl;
    public String targetUrl; // Content URL

    public static final String TYPE_RSS = "RSS";
    public static final String TYPE_YOUTUBE = "YOUTUBE";
    public static final String TYPE_WEB = "WEB";
    public static final String TYPE_MOVIES = "MOVIES";

    public FavoriteItem() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FavoriteItem that = (FavoriteItem) o;
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
