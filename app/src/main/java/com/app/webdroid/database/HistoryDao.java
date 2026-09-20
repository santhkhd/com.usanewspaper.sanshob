package com.app.webdroid.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.app.webdroid.model.HistoryItem;
import java.util.List;

@Dao
public interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addHistory(HistoryItem item);

    @Delete
    void delete(HistoryItem item);

    @Query("DELETE FROM history WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM history WHERE itemId = :itemId AND type = :type")
    void removeHistory(String itemId, String type);

    @Query("DELETE FROM history WHERE (:itemId IS NOT NULL AND :itemId != '' AND itemId = :itemId) OR (:targetUrl IS NOT NULL AND :targetUrl != '' AND targetUrl = :targetUrl) OR (:title IS NOT NULL AND :title != '' AND title = :title)")
    void deleteDuplicates(String itemId, String targetUrl, String title);

    @Query("DELETE FROM history")
    void clearAllHistory();

    @Query("SELECT * FROM history ORDER BY watchedAt DESC")
    LiveData<List<HistoryItem>> getAllHistory();

    @Query("SELECT * FROM history ORDER BY watchedAt DESC LIMIT :limit")
    LiveData<List<HistoryItem>> getRecentHistory(int limit);

    @Query("SELECT * FROM history ORDER BY watchedAt DESC")
    List<HistoryItem> getAllHistoryDirect();

    @Query("SELECT COUNT(*) FROM history")
    int getHistoryCount();
}
