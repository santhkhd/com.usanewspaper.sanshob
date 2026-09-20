package com.app.webdroid.database.sqlite;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.shobmc.san.BuildConfig;
import com.app.webdroid.model.Navigation;

import java.util.ArrayList;
import java.util.List;

public class DbNavigation extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = BuildConfig.APPLICATION_ID + "_" + "navigation.db";
    public static final String TABLE_MENU = "menu";
    public static final String ID = "id";
    public static final String MENU_NAME = "name";
    public static final String MENU_TYPE = "type";
    public static final String MENU_ICON = "icon";
    public static final String MENU_URL = "url";
    public static final String MENU_URL_DARK = "url_dark";
    private SQLiteDatabase db;

    public DbNavigation(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        try {
            this.db = this.getWritableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SQLiteDatabase getDatabase() {
        if (db == null || !db.isOpen()) {
            try {
                db = getWritableDatabase();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return db;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTableMenu(db, TABLE_MENU);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MENU);
            createTableMenu(db, TABLE_MENU);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void truncateTableMenu(String table) {
        SQLiteDatabase database = getDatabase();
        if (database == null) return;
        try {
            database.execSQL("DROP TABLE IF EXISTS " + table);
            createTableMenu(database, table);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTableMenu(SQLiteDatabase db, String table) {
        if (db == null) return;
        try {
            String CREATE_TABLE = "CREATE TABLE " + table + "("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + MENU_NAME + " TEXT,"
                    + MENU_TYPE + " TEXT,"
                    + MENU_ICON + " TEXT,"
                    + MENU_URL + " TEXT,"
                    + MENU_URL_DARK + " TEXT"
                    + ")";
            db.execSQL(CREATE_TABLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        try {
            db.enableWriteAheadLogging();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addListCategory(List<Navigation> navigations, String table) {
        if (navigations == null || navigations.isEmpty()) return;
        SQLiteDatabase database = getDatabase();
        if (database == null) return;
        try {
            database.beginTransaction();
            for (Navigation navigation : navigations) {
                if (navigation != null) {
                    addOneMenu(database, navigation, table);
                }
            }
            database.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                database.endTransaction();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void addOneMenu(SQLiteDatabase database, Navigation navigation, String table) {
        if (database == null || navigation == null) return;
        try {
            ContentValues values = new ContentValues();
            values.put(MENU_NAME, navigation.name);
            values.put(MENU_TYPE, navigation.type);
            values.put(MENU_ICON, navigation.icon);
            values.put(MENU_URL, navigation.url);
            values.put(MENU_URL_DARK, navigation.url_dark);
            database.insert(table, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Navigation> getAllMenu(String table) {
        return getAllMenus(table);
    }

    private List<Navigation> getAllMenus(String table) {
        List<Navigation> list = new ArrayList<>();
        SQLiteDatabase database = getDatabase();
        if (database == null) return list;
        Cursor cursor = null;
        try {
            cursor = database.rawQuery("SELECT * FROM " + table + " ORDER BY id ASC", null);
            if (cursor != null) {
                list = getAllCategoryFormCursor(cursor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return list;
    }

    @SuppressLint("Range")
    private List<Navigation> getAllCategoryFormCursor(Cursor cursor) {
        List<Navigation> list = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                Navigation navigation = new Navigation();
                navigation.name = cursor.getString(cursor.getColumnIndex(MENU_NAME));
                navigation.type = cursor.getString(cursor.getColumnIndex(MENU_TYPE));
                navigation.icon = cursor.getString(cursor.getColumnIndex(MENU_ICON));
                navigation.url = cursor.getString(cursor.getColumnIndex(MENU_URL));
                navigation.url_dark = cursor.getString(cursor.getColumnIndex(MENU_URL_DARK));
                list.add(navigation);
            } while (cursor.moveToNext());
        }
        return list;
    }

}
