package com.app.webdroid.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "web_items", indices = { @Index(value = "url", unique = true) })
public class WebItem implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;
    public String url;
    public String openType; // "IN_APP" or "EXTERNAL_BROWSER"
    public String iconUrl; // Optional if we want icons for the grid

    public static final String OPEN_IN_APP = "IN_APP";
    public static final String OPEN_EXTERNAL = "EXTERNAL_BROWSER";

    public WebItem() {
    }
}
