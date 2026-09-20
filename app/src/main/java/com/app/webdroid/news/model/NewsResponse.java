package com.app.webdroid.news.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class NewsResponse {

    @SerializedName("version")
    private String version;

    @SerializedName("generated_at")
    private String generatedAt;

    @SerializedName("country")
    private String country;

    @SerializedName("language")
    private String language;

    @SerializedName("stories")
    private List<NewsStory> stories;

    public NewsResponse() {
        this.stories = new ArrayList<>();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<NewsStory> getStories() {
        return stories != null ? stories : new ArrayList<>();
    }

    public void setStories(List<NewsStory> stories) {
        this.stories = stories;
    }
}
