package com.app.webdroid.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.app.webdroid.model.NewsItem;
import java.util.List;

@Dao
public interface NewsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> insertNews(List<NewsItem> newsList);

    @Query("SELECT * FROM rss_news ORDER BY pubDateMillis DESC LIMIT :limit OFFSET :offset")
    LiveData<List<NewsItem>> getNewsPaged(int limit, int offset);

    @Query("SELECT * FROM rss_news ORDER BY pubDateMillis DESC")
    LiveData<List<NewsItem>> getAllNews();

    @Query("SELECT * FROM rss_news ORDER BY pubDateMillis DESC")
    List<NewsItem> getAllNewsSync();

    @Query("SELECT * FROM rss_news WHERE sourceName = :sourceName ORDER BY pubDateMillis DESC")
    LiveData<List<NewsItem>> getNewsBySource(String sourceName);

    @Query("DELETE FROM rss_news WHERE pubDateMillis < :cutoffTime")
    void deleteOldNews(long cutoffTime);

    @Query("DELETE FROM rss_news")
    void deleteAll();
}
