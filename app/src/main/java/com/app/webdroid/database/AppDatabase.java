package com.app.webdroid.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.app.webdroid.model.FavoriteItem;
import com.app.webdroid.model.NewsItem;
import com.app.webdroid.model.WebItem;
import com.app.webdroid.model.HistoryItem;
import com.app.webdroid.model.YouTubeItem;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = { NewsItem.class, YouTubeItem.class, WebItem.class,
        FavoriteItem.class, HistoryItem.class }, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract NewsDao newsDao();

    public abstract YouTubeDao youTubeDao();

    public abstract WebDao webDao();

    public abstract FavoriteDao favoriteDao();

    public abstract HistoryDao historyDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "webdroid_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
