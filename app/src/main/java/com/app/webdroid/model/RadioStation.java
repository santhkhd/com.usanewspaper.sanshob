package com.app.webdroid.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class RadioStation implements Serializable {
    @SerializedName("name")
    private String name;

    @SerializedName(value = "description", alternate = {"tagline"})
    private String description;

    @SerializedName(value = "stream_url", alternate = {"url"})
    private String streamUrl;

    @SerializedName(value = "image", alternate = {"logo_url", "favicon"})
    private String image;

    private String category;

    public RadioStation() {}

    public RadioStation(String name, String description, String streamUrl, String image, String category) {
        this.name = name;
        this.description = description;
        this.streamUrl = streamUrl;
        this.image = image;
        this.category = category;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
