package com.app.webdroid.news.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class NewsStory implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("category")
    private String category;

    @SerializedName("category_id")
    private String categoryId;

    @SerializedName("title")
    private String title;

    @SerializedName("source")
    private String source;

    @SerializedName("source_domain")
    private String sourceDomain;

    @SerializedName("published_at")
    private String publishedAt;

    @SerializedName("summary")
    private String summary;

    @SerializedName("summary_type")
    private String summaryType;

    @SerializedName("url")
    private String url;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("age_minutes")
    private long ageMinutes;

    @SerializedName("story_group_id")
    private String storyGroupId;

    @SerializedName("source_count")
    private int sourceCount = 1;

    public NewsStory() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category != null ? category : "Top US News";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSource() {
        return source != null ? source : "US News";
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceDomain() {
        return sourceDomain;
    }

    public void setSourceDomain(String sourceDomain) {
        this.sourceDomain = sourceDomain;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getSummary() {
        return summary != null ? summary : "";
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSummaryType() {
        return summaryType != null ? summaryType : "extractive";
    }

    public void setSummaryType(String summaryType) {
        this.summaryType = summaryType;
    }

    public String getUrl() {
        return url != null ? url : "";
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getAgeMinutes() {
        return ageMinutes;
    }

    public void setAgeMinutes(long ageMinutes) {
        this.ageMinutes = ageMinutes;
    }

    public String getStoryGroupId() {
        return storyGroupId;
    }

    public void setStoryGroupId(String storyGroupId) {
        this.storyGroupId = storyGroupId;
    }

    public int getSourceCount() {
        return sourceCount;
    }

    public void setSourceCount(int sourceCount) {
        this.sourceCount = sourceCount;
    }

    /**
     * Formats relative time elapsed in a clean, human-readable string.
     */
    public String getFormattedAge() {
        if (ageMinutes <= 0) {
            return "Just now";
        } else if (ageMinutes < 60) {
            return ageMinutes + " min ago";
        } else if (ageMinutes < 1440) {
            long hours = ageMinutes / 60;
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else {
            long days = ageMinutes / 1440;
            return days + (days == 1 ? " day ago" : " days ago");
        }
    }
}
