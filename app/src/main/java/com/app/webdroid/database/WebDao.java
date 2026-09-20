package com.app.webdroid.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.app.webdroid.model.WebItem;
import java.util.List;

@Dao
public interface WebDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWebItems(List<WebItem> items);

    @Query("SELECT * FROM web_items")
    LiveData<List<WebItem>> getAllWebItems();

    @Query("DELETE FROM web_items")
    void deleteAll();
}
