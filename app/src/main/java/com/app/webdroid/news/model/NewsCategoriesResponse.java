package com.app.webdroid.news.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class NewsCategoriesResponse {

    @SerializedName("categories")
    private List<CategoryItem> categories;

    public NewsCategoriesResponse() {
        this.categories = new ArrayList<>();
    }

    public List<CategoryItem> getCategories() {
        return categories != null ? categories : new ArrayList<>();
    }

    public void setCategories(List<CategoryItem> categories) {
        this.categories = categories;
    }

    public static class CategoryItem {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("icon")
        private String icon;

        public CategoryItem() {
        }

        public CategoryItem(String id, String name, String icon) {
            this.id = id;
            this.name = name;
            this.icon = icon;
        }

        public String getId() {
            return id != null ? id : "";
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name != null ? name : "";
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIcon() {
            return icon != null ? icon : "newspaper";
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }
    }
}
