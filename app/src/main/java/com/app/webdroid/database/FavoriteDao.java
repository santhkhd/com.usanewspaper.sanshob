package com.app.webdroid.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.app.webdroid.model.FavoriteItem;
import java.util.List;

@Dao
public interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addFavorite(FavoriteItem item);

    @Delete
    void delete(FavoriteItem item);

    @Query("DELETE FROM favorites WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM favorites WHERE itemId = :itemId AND type = :type")
    void removeFavorite(String itemId, String type);

    @Query("DELETE FROM favorites WHERE (:itemId IS NOT NULL AND :itemId != '' AND itemId = :itemId) OR (:targetUrl IS NOT NULL AND :targetUrl != '' AND targetUrl = :targetUrl) OR (:title IS NOT NULL AND :title != '' AND title = :title)")
    void removeFavoriteComprehensive(String itemId, String targetUrl, String title);

    @Query("DELETE FROM favorites WHERE (:id > 0 AND id = :id) OR (:itemId IS NOT NULL AND :itemId != '' AND itemId = :itemId) OR (:targetUrl IS NOT NULL AND :targetUrl != '' AND targetUrl = :targetUrl) OR (:title IS NOT NULL AND :title != '' AND title = :title)")
    void removeFavoriteByIdOrDetails(long id, String itemId, String targetUrl, String title);

    @Query("SELECT * FROM favorites ORDER BY id DESC")
    LiveData<List<FavoriteItem>> getAllFavorites();

    @Query("SELECT COUNT(*) FROM favorites WHERE itemId = :itemId AND type = :type")
    int isFavorite(String itemId, String type);

    @Query("SELECT COUNT(*) FROM favorites WHERE (:targetUrl IS NOT NULL AND :targetUrl != '' AND targetUrl = :targetUrl) OR (:title IS NOT NULL AND :title != '' AND title = :title)")
    int isFavoriteByTargetOrTitle(String targetUrl, String title);
}
