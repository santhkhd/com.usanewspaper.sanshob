package com.app.webdroid.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "youtube_videos", indices = { @Index(value = "videoId", unique = true) })
public class YouTubeItem implements Serializable {
    @PrimaryKey(autoGenerate = true)
    @com.google.gson.annotations.SerializedName(value = "_db_id")
    public long id;

    @com.google.gson.annotations.SerializedName(value = "video_id", alternate = {"videoId"})
    public String videoId;
    public String title;
    @com.google.gson.annotations.SerializedName(value = "thumb_url", alternate = {"thumbnailUrl", "image", "thumbnail"})
    public String thumbnailUrl;
    @com.google.gson.annotations.SerializedName(value = "pubDate", alternate = {"pub_date", "date"})
    public String pubDate;
    public long pubDateMillis;
    @com.google.gson.annotations.SerializedName(value = "channelName", alternate = {"channel_name", "channel"})
    public String channelName;
    public String channelId;
    public String description;
    public String link;
    public long fetchedAt;
    @com.google.gson.annotations.SerializedName(value = "duration", alternate = {"length", "lengthText"})
    public String duration;
    @com.google.gson.annotations.SerializedName(value = "viewCount", alternate = {"view_count", "views"})
    public String viewCount;

    public YouTubeItem() {
    }

    @androidx.room.Ignore
    public boolean isNativeAd = false;

    @androidx.room.Ignore
    public String authorChannelId;

    public String getAuthorChannelId() {
        if (authorChannelId != null && !authorChannelId.isEmpty() && authorChannelId.startsWith("UC")) {
            return authorChannelId;
        }
        if (channelId != null && channelId.startsWith("UC")) {
            return channelId;
        }
        if (description != null && description.startsWith("UC")) {
            return description;
        }
        return authorChannelId;
    }
}
