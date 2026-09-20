package com.app.webdroid.model;

import java.io.Serializable;
import java.util.List;

public class AiPromptItem implements Serializable {
    public int id;
    public String category;
    public String title;
    public String description;
    public String prompt;
    public List<String> tags;
    public String icon;

    public AiPromptItem() {
    }

    public boolean matches(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String q = query.trim().toLowerCase();
        if (title != null && title.toLowerCase().contains(q)) return true;
        if (description != null && description.toLowerCase().contains(q)) return true;
        if (category != null && category.toLowerCase().contains(q)) return true;
        if (prompt != null && prompt.toLowerCase().contains(q)) return true;
        if (tags != null) {
            for (String tag : tags) {
                if (tag != null && tag.toLowerCase().contains(q)) return true;
            }
        }
        return false;
    }
}
