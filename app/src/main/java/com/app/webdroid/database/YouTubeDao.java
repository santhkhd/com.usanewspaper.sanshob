package com.app.webdroid.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.app.webdroid.model.YouTubeItem;
import java.util.List;

@Dao
public interface YouTubeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertVideos(List<YouTubeItem> videos);

    @Query("SELECT * FROM youtube_videos ORDER BY pubDateMillis DESC LIMIT :limit OFFSET :offset")
    LiveData<List<YouTubeItem>> getVideosPaged(int limit, int offset);

    @Query("SELECT * FROM youtube_videos ORDER BY pubDateMillis DESC")
    LiveData<List<YouTubeItem>> getAllVideos();

    @Query("SELECT * FROM youtube_videos WHERE channelId = :channelId ORDER BY pubDateMillis DESC")
    LiveData<List<YouTubeItem>> getVideosByChannelId(String channelId);

    @Query("SELECT * FROM youtube_videos WHERE channelName = :channelName ORDER BY pubDateMillis DESC")
    LiveData<List<YouTubeItem>> getVideosByChannel(String channelName);

    @Query("SELECT * FROM youtube_videos WHERE pubDateMillis >= :minTimestamp ORDER BY pubDateMillis DESC")
    LiveData<List<YouTubeItem>> getRecentVideos(long minTimestamp);

    @Query("SELECT * FROM youtube_videos WHERE fetchedAt >= :minTimestamp ORDER BY pubDateMillis DESC")
    LiveData<List<YouTubeItem>> getRecentlyFetchedVideos(long minTimestamp);

    @Query("SELECT COUNT(*) FROM youtube_videos WHERE channelName = :channelName AND fetchedAt > :lastSeen")
    LiveData<Integer> getNewVideoCount(String channelName, long lastSeen);

    @Query("SELECT COUNT(*) FROM youtube_videos WHERE channelName = :channelName AND fetchedAt > :lastSeen")
    int getNewVideoCountSync(String channelName, long lastSeen);

    @Query("DELETE FROM youtube_videos")
    void deleteAll();
}
