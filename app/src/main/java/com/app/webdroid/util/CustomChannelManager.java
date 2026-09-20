package com.app.webdroid.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.app.webdroid.model.AppConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.shobmc.san.R;
import android.app.Dialog;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Typeface;
import android.util.TypedValue;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.graphics.Insets;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.activity.MainActivity;
import org.json.JSONArray;
import org.json.JSONObject;

public class CustomChannelManager {

    private static final String PREF_NAME = "custom_user_channels_pref";
    private static final String KEY_PREFIX = "custom_channels_";
    private static final String PREF_CUSTOM_CATEGORIES = "custom_user_categories_pref";
    private static final String KEY_USER_CATEGORIES = "user_created_categories_list";

    private static final java.util.Map<String, String> CHANNEL_AVATAR_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public static String findCachedChannelAvatar(Context context, String channelId) {
        if (channelId == null || channelId.isEmpty() || context == null) return null;
        if (CHANNEL_AVATAR_CACHE.containsKey(channelId)) {
            return CHANNEL_AVATAR_CACHE.get(channelId);
        }
        for (AppConfig.OverviewItem it : getAllCustomChannels(context)) {
            if (it.arguments != null && !it.arguments.isEmpty()) {
                String arg = it.arguments.get(0);
                if ((arg.contains(channelId) || arg.equals(channelId)) && it.image != null && !it.image.isEmpty()) {
                    CHANNEL_AVATAR_CACHE.put(channelId, it.image);
                    return it.image;
                }
            }
        }
        try (java.io.InputStream is = context.getAssets().open("channels.json");
             java.io.InputStreamReader reader = new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8)) {
            Type type = new TypeToken<List<AppConfig.OverviewItem>>() {}.getType();
            List<AppConfig.OverviewItem> list = new Gson().fromJson(reader, type);
            if (list != null) {
                for (AppConfig.OverviewItem it : list) {
                    if (it.arguments != null && !it.arguments.isEmpty()) {
                        String arg = it.arguments.get(0);
                        if (arg.contains(channelId) && it.image != null && !it.image.isEmpty()) {
                            CHANNEL_AVATAR_CACHE.put(channelId, it.image);
                            return it.image;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static class CategoryOption {
        public String title;
        public String jsonUrl;
        public boolean isCustom;

        public CategoryOption(String title, String jsonUrl) {
            this(title, jsonUrl, false);
        }

        public CategoryOption(String title, String jsonUrl, boolean isCustom) {
            this.title = title;
            this.jsonUrl = jsonUrl;
            this.isCustom = isCustom;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public static List<CategoryOption> getAvailableCategories() {
        List<CategoryOption> list = new ArrayList<>();
        list.add(new CategoryOption("📺 All US News Channels", "channels.json"));
        list.add(new CategoryOption("🏛️ National Newspapers", "usa_national_newspapers.json"));
        list.add(new CategoryOption("🗺️ Newspapers by State", "usa_states_newspapers.json"));
        list.add(new CategoryOption("📰 Live News Feeds", "news_rss.json"));
        list.add(new CategoryOption("📻 US News & Talk Radio", "news_radio.json"));
        return list;
    }

    public static List<CategoryOption> getUserCategories(Context context) {
        List<CategoryOption> list = new ArrayList<>();
        if (context == null) return list;
        SharedPreferences prefs = context.getSharedPreferences(PREF_CUSTOM_CATEGORIES, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_USER_CATEGORIES, null);
        if (json != null && !json.isEmpty()) {
            Type type = new TypeToken<List<CategoryOption>>() {}.getType();
            try {
                List<CategoryOption> parsed = new Gson().fromJson(json, type);
                if (parsed != null) {
                    for (CategoryOption cat : parsed) {
                        if (cat.title != null && cat.title.contains("📁")) {
                            cat.title = cat.title.replace("📁", "").trim();
                        }
                        list.add(cat);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    private static void saveUserCategories(Context context, List<CategoryOption> list) {
        if (context == null || list == null) return;
        for (CategoryOption cat : list) {
            if (cat.title != null && cat.title.contains("📁")) {
                cat.title = cat.title.replace("📁", "").trim();
            }
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_CUSTOM_CATEGORIES, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_USER_CATEGORIES, new Gson().toJson(list)).apply();
    }

    public static CategoryOption createNewCategory(Context context, String rawName) {
        if (context == null || rawName == null || rawName.trim().isEmpty()) return null;
        String cleanName = rawName.replace("📁", "").trim();
        String slug = cleanName.toLowerCase().replaceAll("[^a-z0-9]", "_").replaceAll("_+", "_");
        if (slug.isEmpty()) {
            slug = "cat_" + System.currentTimeMillis();
        }
        String jsonSlug = "custom_cat_" + slug + ".json";
        String displayTitle = cleanName;

        CategoryOption newCat = new CategoryOption(displayTitle, jsonSlug, true);

        // Save in user categories
        List<CategoryOption> existing = getUserCategories(context);
        boolean exists = false;
        for (CategoryOption cat : existing) {
            if (cat.jsonUrl.equalsIgnoreCase(jsonSlug) || cat.title.equalsIgnoreCase(displayTitle)) {
                exists = true;
                newCat = cat;
                break;
            }
        }
        if (!exists) {
            existing.add(newCat);
            saveUserCategories(context, existing);
        }

        // Also save to youtube_categories.json overview list so it appears in the Categories browser tab!
        AppConfig.OverviewItem catItem = new AppConfig.OverviewItem();
        catItem.title = displayTitle;
        catItem.provider = "overview";
        catItem.arguments = new ArrayList<>();
        catItem.arguments.add(jsonSlug);
        catItem.image = "letter_avatar";
        saveCustomChannel(context, "youtube_categories.json", catItem);

        return newCat;
    }

    public static void deleteUserCategoryBySlug(Context context, String jsonSlug) {
        if (context == null || jsonSlug == null || jsonSlug.isEmpty()) return;
        List<CategoryOption> existing = getUserCategories(context);
        boolean removed = false;
        for (int i = 0; i < existing.size(); i++) {
            if (existing.get(i).jsonUrl.equalsIgnoreCase(jsonSlug)) {
                existing.remove(i);
                removed = true;
                break;
            }
        }
        if (removed) {
            saveUserCategories(context, existing);
        }
    }

    public static boolean isCustomCategorySlug(String jsonSlug) {
        if (jsonSlug == null) return false;
        return jsonSlug.toLowerCase().startsWith("custom_cat_");
    }

    public static List<CategoryOption> getAvailableCategories(Context context) {
        List<CategoryOption> list = new ArrayList<>(getAvailableCategories());
        if (context != null) {
            List<CategoryOption> userCats = getUserCategories(context);
            if (userCats != null && !userCats.isEmpty()) {
                list.addAll(userCats);
            }
        }
        return list;
    }

    public interface OnCategoryCreatedListener {
        void onCategoryCreated(CategoryOption category);
    }

    public static void showCreateCategoryDialog(Context context, OnCategoryCreatedListener listener) {
        if (context == null) return;
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_category, null);
        android.widget.EditText etCategoryName = dialogView.findViewById(R.id.et_category_name);
        ImageView btnClose = dialogView.findViewById(R.id.btn_dialog_close);
        View btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        View btnCreate = dialogView.findViewById(R.id.btn_dialog_create);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> {
                String name = etCategoryName != null && etCategoryName.getText() != null ? etCategoryName.getText().toString().trim() : "";
                if (name.isEmpty()) {
                    if (etCategoryName != null) etCategoryName.setError("Please enter a category name");
                    return;
                }
                CategoryOption newCat = createNewCategory(context, name);
                Toast.makeText(context, "Category \"" + newCat.title + "\" created!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                if (listener != null) {
                    listener.onCategoryCreated(newCat);
                }
            });
        }

        dialog.show();
        if (etCategoryName != null) {
            etCategoryName.requestFocus();
        }
    }

    // =========================================================================
    // Custom Newspaper Addition & Auto Category Opening
    // =========================================================================
    public interface OnNewspaperAddedListener {
        void onNewspaperAdded(AppConfig.OverviewItem newspaper, CategoryOption category);
    }

    public static List<CategoryOption> getNewspaperCategories(Context context) {
        List<CategoryOption> list = new ArrayList<>();
        list.add(new CategoryOption("🏛️ National Newspapers", "usa_national_newspapers.json"));

        // Add all User Created Categories
        if (context != null) {
            List<CategoryOption> userCats = getUserCategories(context);
            if (userCats != null) {
                for (CategoryOption uc : userCats) {
                    list.add(new CategoryOption("📁 " + uc.title, uc.jsonUrl, true));
                }
            }
        }

        // Add 50 States from usa_states_newspapers.json
        if (context != null) {
            try (java.io.InputStream is = context.getAssets().open("usa_states_newspapers.json");
                 java.io.InputStreamReader reader = new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8)) {
                Type type = new TypeToken<List<AppConfig.OverviewItem>>() {}.getType();
                List<AppConfig.OverviewItem> stateItems = new Gson().fromJson(reader, type);
                if (stateItems != null) {
                    for (AppConfig.OverviewItem s : stateItems) {
                        if (s.arguments != null && !s.arguments.isEmpty()) {
                            String sUrl = s.arguments.get(0);
                            if (sUrl.startsWith("states/")) {
                                list.add(new CategoryOption(s.title, sUrl, false));
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return list;
    }

    public static AppConfig.OverviewItem addNewspaper(Context context, String name, String url, CategoryOption category) {
        if (context == null || name == null || url == null || category == null) return null;

        String cleanUrl = url.trim();
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://" + cleanUrl;
        }

        String domain = extractDomain(cleanUrl);
        String faviconUrl = "https://www.google.com/s2/favicons?sz=128&domain=" + domain;

        AppConfig.OverviewItem item = new AppConfig.OverviewItem();
        item.title = name.trim();
        item.provider = "web";
        item.arguments = new ArrayList<>();
        item.arguments.add(cleanUrl);
        item.image = faviconUrl;
        item.year = "Custom Added";
        item.genre = category.title != null ? category.title.replace("🏛️", "").replace("📁", "").trim() : "Newspaper";
        item.rating = "5.0";

        saveCustomChannel(context, category.jsonUrl, item);

        if (category.isCustom) {
            createNewCategory(context, category.title);
        }

        return item;
    }

    public static String extractDomain(String url) {
        if (url == null || url.isEmpty()) return "news.google.com";
        try {
            java.net.URI uri = new java.net.URI(url);
            String domain = uri.getHost();
            if (domain != null) {
                return domain.startsWith("www.") ? domain.substring(4) : domain;
            }
        } catch (Exception ignored) {}
        String cleaned = url.replace("http://", "").replace("https://", "");
        int slashIdx = cleaned.indexOf('/');
        if (slashIdx != -1) {
            cleaned = cleaned.substring(0, slashIdx);
        }
        return cleaned.startsWith("www.") ? cleaned.substring(4) : cleaned;
    }

    public static void showAddNewspaperDialog(android.app.Activity activity, String defaultCategorySlug, OnNewspaperAddedListener listener) {
        if (activity == null || activity.isFinishing()) return;

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_add_newspaper, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etName = dialogView.findViewById(R.id.et_newspaper_name);
        EditText etUrl = dialogView.findViewById(R.id.et_newspaper_url);
        ImageView imgFavicon = dialogView.findViewById(R.id.img_favicon_preview);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_newspaper_category);
        View btnCreateCategory = dialogView.findViewById(R.id.btn_create_new_category_inline);
        ImageView btnClose = dialogView.findViewById(R.id.btn_dialog_close);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel_newspaper);
        View btnAdd = dialogView.findViewById(R.id.btn_add_and_open_newspaper);

        // Populate Categories
        List<CategoryOption> categories = getNewspaperCategories(activity);
        ArrayAdapter<CategoryOption> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);

        // Select default category if passed
        if (defaultCategorySlug != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).jsonUrl.equalsIgnoreCase(defaultCategorySlug)) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }

        // Live favicon preview on URL change
        etUrl.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s != null ? s.toString().trim() : "";
                if (!input.isEmpty() && (input.contains(".") || input.length() > 3)) {
                    String domain = extractDomain(input.startsWith("http") ? input : ("https://" + input));
                    String favUrl = "https://www.google.com/s2/favicons?sz=128&domain=" + domain;
                    Glide.with(activity)
                            .load(favUrl)
                            .placeholder(R.drawable.ic_newspaper)
                            .error(R.drawable.ic_newspaper)
                            .into(imgFavicon);
                } else {
                    imgFavicon.setImageResource(R.drawable.ic_newspaper);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Inline Create New Category button
        btnCreateCategory.setOnClickListener(v -> {
            showCreateCategoryDialog(activity, newCat -> {
                categories.add(1, newCat);
                adapter.notifyDataSetChanged();
                spinnerCategory.setSelection(1);
            });
        });

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                String name = etName != null && etName.getText() != null ? etName.getText().toString().trim() : "";
                String url = etUrl != null && etUrl.getText() != null ? etUrl.getText().toString().trim() : "";

                if (name.isEmpty()) {
                    if (etName != null) {
                        etName.setError("Please enter a newspaper name");
                        etName.requestFocus();
                    }
                    return;
                }
                if (url.isEmpty()) {
                    if (etUrl != null) {
                        etUrl.setError("Please enter a website URL");
                        etUrl.requestFocus();
                    }
                    return;
                }

                CategoryOption selectedCategory = (CategoryOption) spinnerCategory.getSelectedItem();
                if (selectedCategory == null && !categories.isEmpty()) {
                    selectedCategory = categories.get(0);
                }

                if (selectedCategory != null) {
                    AppConfig.OverviewItem addedItem = addNewspaper(activity, name, url, selectedCategory);
                    dialog.dismiss();

                    Toast.makeText(activity, "✓ " + name + " added! Opening " + selectedCategory.title + "...", Toast.LENGTH_SHORT).show();

                    // Automatically open user created category or existing category!
                    if (activity instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) activity;
                        mainActivity.loadWebPage(selectedCategory.title, "CATEGORY", selectedCategory.jsonUrl, selectedCategory.jsonUrl);
                    }

                    if (listener != null) {
                        listener.onNewspaperAdded(addedItem, selectedCategory);
                    }
                }
            });
        }

        dialog.show();
    }

    public static List<AppConfig.OverviewItem> getCustomChannels(Context context, String jsonUrl) {
        List<AppConfig.OverviewItem> list = new ArrayList<>();
        if (context == null || jsonUrl == null) return list;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String key = KEY_PREFIX + jsonUrl.toLowerCase().trim();
        String json = prefs.getString(key, null);

        if (json != null && !json.isEmpty()) {
            Type type = new TypeToken<List<AppConfig.OverviewItem>>() {}.getType();
            try {
                List<AppConfig.OverviewItem> parsed = new Gson().fromJson(json, type);
                if (parsed != null) {
                    for (AppConfig.OverviewItem it : parsed) {
                        if (it.title != null && it.title.contains("📁")) {
                            it.title = it.title.replace("📁", "").trim();
                        }
                    }
                    list.addAll(parsed);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public static List<AppConfig.OverviewItem> getAllCustomChannels(Context context) {
        List<AppConfig.OverviewItem> all = new ArrayList<>();
        if (context == null) return all;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        java.util.Map<String, ?> entries = prefs.getAll();
        if (entries != null) {
            Type type = new TypeToken<List<AppConfig.OverviewItem>>() {}.getType();
            Gson gson = new Gson();
            for (Object val : entries.values()) {
                if (val instanceof String) {
                    try {
                        List<AppConfig.OverviewItem> parsed = gson.fromJson((String) val, type);
                        if (parsed != null) {
                            for (AppConfig.OverviewItem item : parsed) {
                                if (item.title != null && item.title.contains("📁")) {
                                    item.title = item.title.replace("📁", "").trim();
                                }
                                boolean exists = false;
                                for (AppConfig.OverviewItem existing : all) {
                                    if (existing.title != null && existing.title.equalsIgnoreCase(item.title)) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    all.add(item);
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return all;
    }

    public static void saveCustomChannel(Context context, String jsonUrl, AppConfig.OverviewItem item) {
        if (context == null || jsonUrl == null || item == null) return;
        if (item.title != null && item.title.contains("📁")) {
            item.title = item.title.replace("📁", "").trim();
        }

        List<AppConfig.OverviewItem> list = getCustomChannels(context, jsonUrl);
        // Avoid duplicates by title or url
        String targetUrl = (item.arguments != null && !item.arguments.isEmpty()) ? item.arguments.get(0) : "";
        for (int i = 0; i < list.size(); i++) {
            AppConfig.OverviewItem ex = list.get(i);
            String exUrl = (ex.arguments != null && !ex.arguments.isEmpty()) ? ex.arguments.get(0) : "";
            if (ex.title.equalsIgnoreCase(item.title) || (!targetUrl.isEmpty() && exUrl.equalsIgnoreCase(targetUrl))) {
                list.remove(i);
                break;
            }
        }

        list.add(item);
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String key = KEY_PREFIX + jsonUrl.toLowerCase().trim();
        prefs.edit().putString(key, new Gson().toJson(list)).apply();

    }

    public static void deleteCustomChannel(Context context, String jsonUrl, AppConfig.OverviewItem item) {
        if (context == null || jsonUrl == null || item == null) return;

        List<AppConfig.OverviewItem> list = getCustomChannels(context, jsonUrl);
        String targetUrl = (item.arguments != null && !item.arguments.isEmpty()) ? item.arguments.get(0) : "";
        boolean removed = false;
        for (int i = 0; i < list.size(); i++) {
            AppConfig.OverviewItem ex = list.get(i);
            String exUrl = (ex.arguments != null && !ex.arguments.isEmpty()) ? ex.arguments.get(0) : "";
            if (ex.title.equalsIgnoreCase(item.title) || (!targetUrl.isEmpty() && exUrl.equalsIgnoreCase(targetUrl))) {
                list.remove(i);
                removed = true;
                break;
            }
        }

        if (removed) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String key = KEY_PREFIX + jsonUrl.toLowerCase().trim();
            prefs.edit().putString(key, new Gson().toJson(list)).apply();

        }
    }

    public static void deleteCustomChannelFromAllCategories(Context context, String title, String targetUrl) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        java.util.Map<String, ?> entries = prefs.getAll();
        if (entries == null || entries.isEmpty()) return;

        Gson gson = new Gson();
        Type type = new TypeToken<List<AppConfig.OverviewItem>>() {}.getType();
        SharedPreferences.Editor editor = prefs.edit();
        boolean anyModified = false;

        for (java.util.Map.Entry<String, ?> entry : entries.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.startsWith(KEY_PREFIX) && entry.getValue() instanceof String) {
                try {
                    List<AppConfig.OverviewItem> list = gson.fromJson((String) entry.getValue(), type);
                    if (list != null) {
                        boolean modified = false;
                        for (int i = list.size() - 1; i >= 0; i--) {
                            AppConfig.OverviewItem ex = list.get(i);
                            String exUrl = (ex.arguments != null && !ex.arguments.isEmpty()) ? ex.arguments.get(0) : "";
                            boolean matchesTitle = false;
                            if (title != null && !title.isEmpty() && ex.title != null) {
                                String t1 = title.trim().toLowerCase();
                                String t2 = ex.title.trim().toLowerCase();
                                matchesTitle = t1.equals(t2) || t1.contains(t2) || t2.contains(t1);
                            }
                            boolean matchesUrl = false;
                            if (targetUrl != null && !targetUrl.isEmpty() && !exUrl.isEmpty()) {
                                String u1 = targetUrl.trim().toLowerCase();
                                String u2 = exUrl.trim().toLowerCase();
                                matchesUrl = u1.equals(u2) || u1.contains(u2) || u2.contains(u1);
                            }
                            if (matchesTitle || matchesUrl) {
                                list.remove(i);
                                modified = true;
                            }
                        }
                        if (modified) {
                            editor.putString(key, gson.toJson(list));
                            anyModified = true;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        if (anyModified) {
            editor.apply();
        }

    }

    private static final String KEY_HIDDEN_PREFIX = "hidden_ch_";

    public static java.util.Set<String> getHiddenChannelIdentifiers(Context context, String jsonUrl) {
        if (context == null || jsonUrl == null) return new java.util.HashSet<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String key = KEY_HIDDEN_PREFIX + jsonUrl.toLowerCase().trim();
        return prefs.getStringSet(key, new java.util.HashSet<>());
    }

    public static boolean isChannelHidden(Context context, String jsonUrl, AppConfig.OverviewItem item) {
        if (context == null || jsonUrl == null || item == null) return false;
        java.util.Set<String> hidden = getHiddenChannelIdentifiers(context, jsonUrl);
        if (hidden == null || hidden.isEmpty()) return false;
        String targetUrl = (item.arguments != null && !item.arguments.isEmpty()) ? item.arguments.get(0).toLowerCase().trim() : "";
        String title = item.title != null ? item.title.toLowerCase().trim() : "";
        return (!targetUrl.isEmpty() && hidden.contains(targetUrl)) || (!title.isEmpty() && hidden.contains(title));
    }

    public static void hideChannel(Context context, String jsonUrl, AppConfig.OverviewItem item) {
        if (context == null || jsonUrl == null || item == null) return;
        if (isCustomChannel(context, jsonUrl, item)) {
            deleteCustomChannel(context, jsonUrl, item);
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String key = KEY_HIDDEN_PREFIX + jsonUrl.toLowerCase().trim();
        java.util.Set<String> existing = prefs.getStringSet(key, new java.util.HashSet<>());
        java.util.Set<String> set = new java.util.HashSet<>(existing);
        String targetUrl = (item.arguments != null && !item.arguments.isEmpty()) ? item.arguments.get(0).toLowerCase().trim() : "";
        if (!targetUrl.isEmpty()) set.add(targetUrl);
        if (item.title != null && !item.title.trim().isEmpty()) set.add(item.title.toLowerCase().trim());
        prefs.edit().putStringSet(key, set).apply();
    }

    public static void restoreHiddenChannels(Context context, String jsonUrl) {
        if (context == null || jsonUrl == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String key = KEY_HIDDEN_PREFIX + jsonUrl.toLowerCase().trim();
        prefs.edit().remove(key).apply();
    }

    public static boolean hasHiddenChannels(Context context, String jsonUrl) {
        if (context == null || jsonUrl == null) return false;
        java.util.Set<String> hidden = getHiddenChannelIdentifiers(context, jsonUrl);
        return hidden != null && !hidden.isEmpty();
    }

    public static boolean isCustomChannel(Context context, String jsonUrl, AppConfig.OverviewItem item) {
        if (context == null || jsonUrl == null || item == null) return false;
        List<AppConfig.OverviewItem> list = getCustomChannels(context, jsonUrl);
        String targetUrl = (item.arguments != null && !item.arguments.isEmpty()) ? item.arguments.get(0) : "";
        for (AppConfig.OverviewItem ex : list) {
            String exUrl = (ex.arguments != null && !ex.arguments.isEmpty()) ? ex.arguments.get(0) : "";
            if (ex.title.equalsIgnoreCase(item.title) || (!targetUrl.isEmpty() && exUrl.equalsIgnoreCase(targetUrl))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isChannelAdded(Context context, String jsonUrl, String channelNameOrId) {
        if (context == null || jsonUrl == null || channelNameOrId == null || channelNameOrId.trim().isEmpty()) return false;
        List<AppConfig.OverviewItem> list = getCustomChannels(context, jsonUrl);
        String cleanTarget = channelNameOrId.trim().toLowerCase();
        for (AppConfig.OverviewItem ex : list) {
            if (ex.title != null && ex.title.trim().equalsIgnoreCase(cleanTarget)) return true;
            String exUrl = (ex.arguments != null && !ex.arguments.isEmpty()) ? ex.arguments.get(0).toLowerCase() : "";
            if (!exUrl.isEmpty() && exUrl.contains(cleanTarget)) return true;
        }
        return false;
    }

    public static boolean isChannelAddedAnywhere(Context context, String channelNameOrId) {
        if (context == null || channelNameOrId == null || channelNameOrId.trim().isEmpty()) return false;
        for (CategoryOption cat : getAvailableCategories(context)) {
            if (isChannelAdded(context, cat.jsonUrl, channelNameOrId)) {
                return true;
            }
        }
        return false;
    }

    public static void addDirectChannel(Context context, String channelName, String channelId, String thumbUrl, CategoryOption category) {
        if (context == null || category == null) return;
        String feedUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId;
        AppConfig.OverviewItem item = new AppConfig.OverviewItem();
        item.title = channelName != null && !channelName.isEmpty() ? channelName.trim() : "Custom Channel";
        item.provider = "videos";
        item.arguments = new ArrayList<>();
        item.arguments.add(feedUrl);
        item.image = (thumbUrl != null && !thumbUrl.isEmpty()) ? thumbUrl : "https://img.icons8.com/color/96/youtube-play.png";
        saveCustomChannel(context, category.jsonUrl, item);
        if (thumbUrl != null && !thumbUrl.isEmpty() && channelId != null && !channelId.isEmpty()) {
            ChannelLogoCache.saveLogo(context, channelId, thumbUrl);
        }
    }

    public static void addDirectPlaylist(Context context, String playlistTitle, String playlistId, String thumbUrl, CategoryOption category) {
        if (context == null || category == null) return;
        String cleanId = playlistId != null ? playlistId.trim() : "";
        if (cleanId.startsWith("VL")) cleanId = cleanId.substring(2);
        String feedUrl = "https://www.youtube.com/feeds/videos.xml?playlist_id=" + cleanId;
        AppConfig.OverviewItem item = new AppConfig.OverviewItem();
        item.title = playlistTitle != null && !playlistTitle.isEmpty() ? playlistTitle.trim() : "Custom Playlist";
        item.provider = "videos";
        item.arguments = new ArrayList<>();
        item.arguments.add(feedUrl);
        item.image = (thumbUrl != null && !thumbUrl.isEmpty()) ? thumbUrl : "https://img.icons8.com/color/96/playlist.png";
        saveCustomChannel(context, category.jsonUrl, item);
    }

    public static void addDirectVideo(Context context, String videoTitle, String videoId, String thumbUrl, CategoryOption category) {
        if (context == null || category == null || videoId == null || videoId.isEmpty()) return;
        AppConfig.OverviewItem item = new AppConfig.OverviewItem();
        item.title = (videoTitle != null && !videoTitle.isEmpty()) ? videoTitle.trim() : "YouTube Video";
        item.provider = "videos";
        item.arguments = new ArrayList<>();
        item.arguments.add("https://www.youtube.com/watch?v=" + videoId);
        item.image = (thumbUrl != null && !thumbUrl.isEmpty()) ? thumbUrl : "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
        saveCustomChannel(context, category.jsonUrl, item);
    }

    // =========================================================================
    // Full-Window Category Selection Classes & Methods (Uniform Style)
    // =========================================================================
    public static class FullWindowCategoryItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_CATEGORY = 1;

        public int viewType;
        public String headerTitle;
        public CategoryOption category;

        public static FullWindowCategoryItem makeHeader(String title) {
            FullWindowCategoryItem item = new FullWindowCategoryItem();
            item.viewType = TYPE_HEADER;
            item.headerTitle = title;
            return item;
        }

        public static FullWindowCategoryItem makeCategory(CategoryOption category) {
            FullWindowCategoryItem item = new FullWindowCategoryItem();
            item.viewType = TYPE_CATEGORY;
            item.category = category;
            return item;
        }
    }

    public static class FullWindowCategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        public interface OnCategorySelectedListener {
            void onCategorySelected(CategoryOption category);
        }

        private final Context context;
        private final List<FullWindowCategoryItem> items;
        private final OnCategorySelectedListener listener;
        private CategoryOption selectedCategory;

        public FullWindowCategoryAdapter(Context context,
                                  List<FullWindowCategoryItem> items,
                                  CategoryOption initialSelected,
                                  OnCategorySelectedListener listener) {
            this.context = context;
            this.items = items;
            this.selectedCategory = initialSelected;
            this.listener = listener;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).viewType;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == FullWindowCategoryItem.TYPE_HEADER) {
                View v = LayoutInflater.from(context).inflate(R.layout.item_full_window_category_header, parent, false);
                return new HeaderViewHolder(v);
            } else {
                View v = LayoutInflater.from(context).inflate(R.layout.item_full_window_category_row, parent, false);
                return new CategoryRowViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            FullWindowCategoryItem item = items.get(position);
            if (holder instanceof HeaderViewHolder) {
                HeaderViewHolder hh = (HeaderViewHolder) holder;
                hh.tvHeader.setText(item.headerTitle);
            } else if (holder instanceof CategoryRowViewHolder) {
                CategoryRowViewHolder ch = (CategoryRowViewHolder) holder;
                CategoryOption cat = item.category;
                ch.tvName.setText(cat.title);

                boolean isSelected = selectedCategory != null &&
                        (selectedCategory == cat || (selectedCategory.jsonUrl != null && selectedCategory.jsonUrl.equalsIgnoreCase(cat.jsonUrl)));

                ch.imgCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);

                int accentColor = ContextCompat.getColor(context, R.color.colorAccent);
                TypedValue tv = new TypedValue();
                context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true);
                int primaryColor = tv.resourceId != 0 ? ContextCompat.getColor(context, tv.resourceId) : tv.data;

                ch.tvName.setTextColor(isSelected ? accentColor : primaryColor);
                ch.tvName.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);

                if (isSelected) {
                    ch.layoutRow.setBackgroundResource(R.drawable.bg_category_selected);
                } else {
                    TypedValue rippleTv = new TypedValue();
                    context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, rippleTv, true);
                    ch.layoutRow.setBackgroundResource(rippleTv.resourceId);
                }

                ch.itemView.setOnClickListener(v -> {
                    selectedCategory = cat;
                    notifyDataSetChanged();
                    if (listener != null) {
                        listener.onCategorySelected(cat);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView tvHeader;

            HeaderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvHeader = itemView.findViewById(R.id.tv_category_section_header);
            }
        }

        static class CategoryRowViewHolder extends RecyclerView.ViewHolder {
            View layoutRow;
            TextView tvName;
            ImageView imgCheck;

            CategoryRowViewHolder(@NonNull View itemView) {
                super(itemView);
                layoutRow = itemView.findViewById(R.id.layout_category_row);
                tvName = itemView.findViewById(R.id.tv_category_name);
                imgCheck = itemView.findViewById(R.id.img_category_selected);
            }
        }
    }

    public static void showAddChannelDialog(Context context, String channelName, String channelId, String thumbUrl,
                                            String defaultCategoryHint, Runnable onAddedCallback) {
        showFullWindowCategorySelector(context, channelName, channelId, thumbUrl, defaultCategoryHint, onAddedCallback);
    }

    public static void showFullWindowCategorySelector(Context context,
                                                      String channelName,
                                                      String channelId,
                                                      String thumbUrl,
                                                      String defaultCategoryHint,
                                                      Runnable onAddedCallback) {
        if (context == null) return;
        final String cleanName = (channelName != null && !channelName.isEmpty()) ? channelName.trim() : "YouTube Channel";
        final String cleanId = (channelId != null && !channelId.startsWith("search:")) ? channelId.trim() : "";

        SharedPref sharedPref = new SharedPref(context);
        Dialog fullWindowDialog = new Dialog(context, R.style.FullScreenCategoryDialogTheme);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_full_window_select_category, null);
        fullWindowDialog.setContentView(dialogView);

        Window window = fullWindowDialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            int surfaceColor = ContextCompat.getColor(context,
                    sharedPref.getIsDarkTheme() ? R.color.color_dark_background : R.color.color_light_background);
            window.setBackgroundDrawable(new ColorDrawable(surfaceColor));
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(surfaceColor);
            window.setNavigationBarColor(surfaceColor);

            WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, window.getDecorView());
            if (insetsController != null) {
                insetsController.setAppearanceLightStatusBars(!sharedPref.getIsDarkTheme());
                insetsController.setAppearanceLightNavigationBars(!sharedPref.getIsDarkTheme());
            }
        }

        View layoutRoot = dialogView.findViewById(R.id.layout_full_screen_root);
        View header = dialogView.findViewById(R.id.layout_modal_header);
        View bottomBar = dialogView.findViewById(R.id.layout_modal_bottom_bar);

        ViewCompat.setOnApplyWindowInsetsListener(layoutRoot, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (header != null) {
                header.setPadding(
                        header.getPaddingLeft(),
                        systemBars.top + (int) (6 * context.getResources().getDisplayMetrics().density),
                        header.getPaddingRight(),
                        header.getPaddingBottom()
                );
            }
            if (bottomBar != null) {
                bottomBar.setPadding(
                        bottomBar.getPaddingLeft(),
                        bottomBar.getPaddingTop(),
                        bottomBar.getPaddingRight(),
                        systemBars.bottom + (int) (12 * context.getResources().getDisplayMetrics().density)
                );
            }
            return insets;
        });

        ImageView btnBack = dialogView.findViewById(R.id.btn_modal_back);
        ImageView imgAvatar = dialogView.findViewById(R.id.img_modal_channel_avatar);
        TextView tvChannelPrompt = dialogView.findViewById(R.id.tv_modal_channel_prompt);
        RecyclerView rvCategories = dialogView.findViewById(R.id.rv_modal_categories);
        View btnCreateNewCategory = dialogView.findViewById(R.id.btn_modal_create_category);
        MaterialButton btnConfirmAdd = dialogView.findViewById(R.id.btn_modal_add_confirm);

        btnBack.setOnClickListener(v -> fullWindowDialog.dismiss());

        tvChannelPrompt.setText("Add \"" + cleanName + "\" to");

        if (imgAvatar != null && thumbUrl != null && !thumbUrl.isEmpty()) {
            imgAvatar.setVisibility(View.VISIBLE);
            try {
                Glide.with(context)
                        .load(thumbUrl)
                        .placeholder(R.drawable.ic_live_tv)
                        .circleCrop()
                        .into(imgAvatar);
            } catch (Exception ignored) {}
        }

        List<CategoryOption> allCategories = getAvailableCategories(context);

        // Smart Category Selection
        CategoryOption initialCategory = null;
        if (defaultCategoryHint != null && !defaultCategoryHint.isEmpty()) {
            String hintLower = defaultCategoryHint.toLowerCase();
            for (CategoryOption cat : allCategories) {
                String catTitle = cat.title.toLowerCase();
                String catJson = cat.jsonUrl.toLowerCase();
                if (catTitle.contains(hintLower) || catJson.contains(hintLower) || hintLower.contains(catJson)) {
                    initialCategory = cat;
                    break;
                }
            }
        }
        if (initialCategory == null) {
            String lowerName = cleanName.toLowerCase();
            for (CategoryOption cat : allCategories) {
                if (lowerName.contains("news") && cat.jsonUrl.contains("news")) { initialCategory = cat; break; }
                if ((lowerName.contains("comedy") || lowerName.contains("karikku") || lowerName.contains("funny")) && cat.jsonUrl.contains("comedy")) { initialCategory = cat; break; }
                if ((lowerName.contains("cook") || lowerName.contains("kitchen") || lowerName.contains("food") || lowerName.contains("recipe")) && cat.jsonUrl.contains("cookery")) { initialCategory = cat; break; }
                if ((lowerName.contains("travel") || lowerName.contains("tech travel eat")) && cat.jsonUrl.contains("travel")) { initialCategory = cat; break; }
                if ((lowerName.contains("music") || lowerName.contains("audio") || lowerName.contains("songs")) && cat.jsonUrl.contains("music")) { initialCategory = cat; break; }
                if ((lowerName.contains("tech") || lowerName.contains("gadgets") || lowerName.contains("auto")) && cat.jsonUrl.contains("tech")) { initialCategory = cat; break; }
                if ((lowerName.contains("movie") || lowerName.contains("film") || lowerName.contains("cinema")) && cat.jsonUrl.contains("movies")) { initialCategory = cat; break; }
                if ((lowerName.contains("trailer") || lowerName.contains("teaser")) && cat.jsonUrl.contains("trailers")) { initialCategory = cat; break; }
            }
        }
        if (initialCategory == null && !allCategories.isEmpty()) {
            initialCategory = allCategories.get(0);
        }

        final CategoryOption[] selectedCategoryHolder = new CategoryOption[]{initialCategory};

        List<FullWindowCategoryItem> listItems = new ArrayList<>();
        if (initialCategory != null) {
            listItems.add(FullWindowCategoryItem.makeHeader("Suggested category"));
            listItems.add(FullWindowCategoryItem.makeCategory(initialCategory));
            listItems.add(FullWindowCategoryItem.makeHeader("Other categories"));
            for (CategoryOption cat : allCategories) {
                if (!cat.jsonUrl.equalsIgnoreCase(initialCategory.jsonUrl)) {
                    listItems.add(FullWindowCategoryItem.makeCategory(cat));
                }
            }
        } else {
            listItems.add(FullWindowCategoryItem.makeHeader("All categories"));
            for (CategoryOption cat : allCategories) {
                listItems.add(FullWindowCategoryItem.makeCategory(cat));
            }
        }

        rvCategories.setLayoutManager(new LinearLayoutManager(context));
        FullWindowCategoryAdapter adapter = new FullWindowCategoryAdapter(
                context,
                listItems,
                selectedCategoryHolder[0],
                selectedCat -> {
                    selectedCategoryHolder[0] = selectedCat;
                    btnConfirmAdd.setText("Add to " + selectedCat.title);
                }
        );
        rvCategories.setAdapter(adapter);

        if (selectedCategoryHolder[0] != null) {
            btnConfirmAdd.setText("Add to " + selectedCategoryHolder[0].title);
        }

        // "＋ Create New Category"
        btnCreateNewCategory.setOnClickListener(v -> {
            showCreateCategoryDialog(context, newCat -> {
                if (newCat != null) {
                    if (!cleanId.isEmpty() && (cleanId.startsWith("PL") || cleanId.startsWith("VLPL") || cleanId.contains("list="))) {
                        String pid = cleanId;
                        if (pid.contains("list=")) {
                            int idx = pid.indexOf("list=");
                            pid = pid.substring(idx + 5);
                            if (pid.contains("&")) pid = pid.substring(0, pid.indexOf("&"));
                        }
                        if (pid.startsWith("VL")) pid = pid.substring(2);
                        final String finalPid = pid;
                        Executors.newSingleThreadExecutor().execute(() -> {
                            String titleToUse = cleanName;
                            String thumbToUse = (thumbUrl != null && !thumbUrl.isEmpty()) ? thumbUrl : "";
                            if (titleToUse.equals("YouTube Channel") || titleToUse.equals("Custom Playlist") || titleToUse.startsWith("http") || titleToUse.startsWith("PL") || titleToUse.startsWith("VL") || thumbToUse.isEmpty()) {
                                try {
                                    String feedUrl = "https://www.youtube.com/feeds/videos.xml?playlist_id=" + finalPid;
                                    Request req = new Request.Builder().url(feedUrl).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)").build();
                                    Response resp = new OkHttpClient().newCall(req).execute();
                                    if (resp.isSuccessful() && resp.body() != null) {
                                        String xml = resp.body().string();
                                        if (titleToUse.equals("YouTube Channel") || titleToUse.equals("Custom Playlist") || titleToUse.startsWith("http") || titleToUse.startsWith("PL") || titleToUse.startsWith("VL")) {
                                            Pattern tp = Pattern.compile("<title>(.*?)</title>");
                                            Matcher tm = tp.matcher(xml);
                                            if (tm.find()) titleToUse = tm.group(1).trim();
                                        }
                                        if (thumbToUse.isEmpty()) {
                                            Pattern thp = Pattern.compile("<media:thumbnail[^>]+url=['\"]([^'\"]+)['\"]");
                                            Matcher thm = thp.matcher(xml);
                                            if (thm.find()) thumbToUse = thm.group(1).trim();
                                        }
                                    }
                                } catch (Exception ignored) {}
                            }
                            final String finalTitle = (titleToUse != null && !titleToUse.isEmpty() && !titleToUse.equals("YouTube Channel")) ? titleToUse : "Playlist " + finalPid;
                            final String finalThumb = thumbToUse;
                            addDirectPlaylist(context, finalTitle, finalPid, finalThumb, newCat);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Toast.makeText(context, "Added playlist \"" + finalTitle + "\" to " + newCat.title + "!", Toast.LENGTH_LONG).show();
                                fullWindowDialog.dismiss();
                                if (onAddedCallback != null) onAddedCallback.run();
                            });
                        });
                    } else if (!cleanId.isEmpty() && cleanId.startsWith("UC") && cleanId.length() == 24) {
                        addDirectChannel(context, cleanName, cleanId, thumbUrl, newCat);
                        Toast.makeText(context, "Added \"" + cleanName + "\" to " + newCat.title + "!", Toast.LENGTH_LONG).show();
                        fullWindowDialog.dismiss();
                        if (onAddedCallback != null) onAddedCallback.run();
                    } else {
                        String queryOrHandle = !cleanId.isEmpty() ? cleanId : cleanName;
                        resolveAndAddChannel(context, cleanName, queryOrHandle, newCat, new AddChannelCallback() {
                            @Override
                            public void onSuccess(AppConfig.OverviewItem item, String categoryTitle) {
                                Toast.makeText(context, "Added \"" + (item != null ? item.title : cleanName) + "\" to " + categoryTitle + "!", Toast.LENGTH_LONG).show();
                                fullWindowDialog.dismiss();
                                if (onAddedCallback != null) onAddedCallback.run();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            });
        });

        // "Add to [Category]" Button
        btnConfirmAdd.setOnClickListener(v -> {
            CategoryOption targetCat = selectedCategoryHolder[0];
            if (targetCat == null) {
                Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!cleanId.isEmpty() && (cleanId.startsWith("PL") || cleanId.startsWith("VLPL") || cleanId.contains("list="))) {
                String pid = cleanId;
                if (pid.contains("list=")) {
                    int idx = pid.indexOf("list=");
                    pid = pid.substring(idx + 5);
                    if (pid.contains("&")) pid = pid.substring(0, pid.indexOf("&"));
                }
                if (pid.startsWith("VL")) pid = pid.substring(2);
                btnConfirmAdd.setEnabled(false);
                btnConfirmAdd.setText("Adding Playlist...");
                final String finalPid = pid;
                Executors.newSingleThreadExecutor().execute(() -> {
                    String titleToUse = cleanName;
                    String thumbToUse = (thumbUrl != null && !thumbUrl.isEmpty()) ? thumbUrl : "";
                    if (titleToUse.equals("YouTube Channel") || titleToUse.equals("Custom Playlist") || titleToUse.startsWith("http") || titleToUse.startsWith("PL") || titleToUse.startsWith("VL") || thumbToUse.isEmpty()) {
                        try {
                            String feedUrl = "https://www.youtube.com/feeds/videos.xml?playlist_id=" + finalPid;
                            Request req = new Request.Builder().url(feedUrl).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)").build();
                            Response resp = new OkHttpClient().newCall(req).execute();
                            if (resp.isSuccessful() && resp.body() != null) {
                                String xml = resp.body().string();
                                if (titleToUse.equals("YouTube Channel") || titleToUse.equals("Custom Playlist") || titleToUse.startsWith("http") || titleToUse.startsWith("PL") || titleToUse.startsWith("VL")) {
                                    Pattern tp = Pattern.compile("<title>(.*?)</title>");
                                    Matcher tm = tp.matcher(xml);
                                    if (tm.find()) titleToUse = tm.group(1).trim();
                                }
                                if (thumbToUse.isEmpty()) {
                                    Pattern thp = Pattern.compile("<media:thumbnail[^>]+url=['\"]([^'\"]+)['\"]");
                                    Matcher thm = thp.matcher(xml);
                                    if (thm.find()) thumbToUse = thm.group(1).trim();
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    final String finalTitle = (titleToUse != null && !titleToUse.isEmpty() && !titleToUse.equals("YouTube Channel")) ? titleToUse : "Playlist " + finalPid;
                    final String finalThumb = thumbToUse;
                    addDirectPlaylist(context, finalTitle, finalPid, finalThumb, targetCat);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(context, "Added playlist \"" + finalTitle + "\" to " + targetCat.title + "!", Toast.LENGTH_LONG).show();
                        fullWindowDialog.dismiss();
                        if (onAddedCallback != null) {
                            onAddedCallback.run();
                        }
                    });
                });
            } else if (!cleanId.isEmpty() && cleanId.startsWith("UC") && cleanId.length() == 24) {
                addDirectChannel(context, cleanName, cleanId, thumbUrl, targetCat);
                Toast.makeText(context, "Added \"" + cleanName + "\" to " + targetCat.title + "!", Toast.LENGTH_LONG).show();
                fullWindowDialog.dismiss();
                if (onAddedCallback != null) {
                    onAddedCallback.run();
                }
            } else {
                btnConfirmAdd.setEnabled(false);
                btnConfirmAdd.setText("Adding...");
                String queryOrHandle = !cleanId.isEmpty() ? cleanId : cleanName;
                resolveAndAddChannel(context, cleanName, queryOrHandle, targetCat, new AddChannelCallback() {
                    @Override
                    public void onSuccess(AppConfig.OverviewItem item, String categoryTitle) {
                        Toast.makeText(context, "Added \"" + (item != null ? item.title : cleanName) + "\" to " + categoryTitle + "!", Toast.LENGTH_LONG).show();
                        fullWindowDialog.dismiss();
                        if (onAddedCallback != null) {
                            onAddedCallback.run();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        btnConfirmAdd.setEnabled(true);
                        btnConfirmAdd.setText("Add to " + targetCat.title);
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        fullWindowDialog.show();
    }

    public interface AddChannelCallback {
        void onSuccess(AppConfig.OverviewItem item, String categoryTitle);
        void onError(String errorMessage);
    }

    public static void resolveAndAddChannel(Context context, String channelName, String inputUrlOrHandle,
                                            CategoryOption category, AddChannelCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        if (channelName == null || channelName.trim().isEmpty()) {
            callback.onError("Please enter a channel name");
            return;
        }

        if (inputUrlOrHandle == null || inputUrlOrHandle.trim().isEmpty()) {
            callback.onError("Please enter a YouTube link or handle");
            return;
        }

        final String cleanInput = inputUrlOrHandle.trim();
        final String cleanName = channelName.trim();

        Executors.newSingleThreadExecutor().execute(() -> {
            // Check if user entered a playlist link or PL... ID
            if (cleanInput.startsWith("PL") || cleanInput.startsWith("VLPL") || cleanInput.contains("list=")) {
                String pid = cleanInput;
                if (pid.contains("list=")) {
                    int idx = pid.indexOf("list=");
                    pid = pid.substring(idx + 5);
                    if (pid.contains("&")) pid = pid.substring(0, pid.indexOf("&"));
                }
                if (pid.startsWith("VL")) pid = pid.substring(2);
                if (!pid.isEmpty()) {
                    final String finalPid = pid;
                    String titleToUse = cleanName;
                    String thumbToUse = "";
                    if (titleToUse.equals("YouTube Channel") || titleToUse.equals("Custom Playlist") || titleToUse.startsWith("http") || titleToUse.startsWith("PL") || titleToUse.startsWith("VL")) {
                        try {
                            String feedUrl = "https://www.youtube.com/feeds/videos.xml?playlist_id=" + finalPid;
                            Request req = new Request.Builder().url(feedUrl).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)").build();
                            Response resp = new OkHttpClient().newCall(req).execute();
                            if (resp.isSuccessful() && resp.body() != null) {
                                String xml = resp.body().string();
                                Pattern tp = Pattern.compile("<title>(.*?)</title>");
                                Matcher tm = tp.matcher(xml);
                                if (tm.find()) titleToUse = tm.group(1).trim();
                                Pattern thp = Pattern.compile("<media:thumbnail[^>]+url=['\"]([^'\"]+)['\"]");
                                Matcher thm = thp.matcher(xml);
                                if (thm.find()) thumbToUse = thm.group(1).trim();
                            }
                        } catch (Exception ignored) {}
                    }
                    final String finalTitle = (titleToUse != null && !titleToUse.isEmpty() && !titleToUse.equals("YouTube Channel")) ? titleToUse : "Playlist " + finalPid;
                    addDirectPlaylist(context, finalTitle, finalPid, thumbToUse, category);
                    AppConfig.OverviewItem plItem = new AppConfig.OverviewItem();
                    plItem.title = finalTitle;
                    plItem.provider = "videos";
                    plItem.arguments = new ArrayList<>();
                    plItem.arguments.add("https://www.youtube.com/feeds/videos.xml?playlist_id=" + finalPid);
                    plItem.image = (!thumbToUse.isEmpty()) ? thumbToUse : "https://img.icons8.com/color/96/playlist.png";
                    mainHandler.post(() -> callback.onSuccess(plItem, category.title));
                    return;
                }
            }

            String channelId = null;

            // Check if user entered direct UC channel ID
            if (cleanInput.startsWith("UC") && cleanInput.length() == 24 && !cleanInput.contains(" ") && !cleanInput.contains("/")) {
                channelId = cleanInput;
            } else if (cleanInput.contains("channel_id=")) {
                int idx = cleanInput.indexOf("channel_id=");
                channelId = cleanInput.substring(idx + 11);
                if (channelId.contains("&")) {
                    channelId = channelId.substring(0, channelId.indexOf("&"));
                }
            } else if (cleanInput.contains("/channel/UC")) {
                int idx = cleanInput.indexOf("/channel/");
                channelId = cleanInput.substring(idx + 9);
                if (channelId.contains("/")) {
                    channelId = channelId.substring(0, channelId.indexOf("/"));
                }
                if (channelId.contains("?")) {
                    channelId = channelId.substring(0, channelId.indexOf("?"));
                }
            } else {
                // User entered handle (e.g. @MalluTraveler or https://youtube.com/@handle)
                String handleUrl = cleanInput;
                if (!handleUrl.startsWith("http")) {
                    if (!handleUrl.startsWith("@")) {
                        handleUrl = "@" + handleUrl;
                    }
                    handleUrl = "https://www.youtube.com/" + handleUrl;
                }

                try {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(handleUrl)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                            .build();

                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        String html = response.body().string();
                        Pattern p = Pattern.compile("\"channelId\":\\s*\"(UC[a-zA-Z0-9_\\-]{22})\"");
                        Matcher m = p.matcher(html);
                        if (m.find()) {
                            channelId = m.group(1);
                        } else {
                            Pattern p2 = Pattern.compile("itemprop=\"identifier\"\\s+content=\"(UC[a-zA-Z0-9_\\-]{22})\"");
                            Matcher m2 = p2.matcher(html);
                            if (m2.find()) {
                                channelId = m2.group(1);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Fallback: Query Innertube search to resolve channel name or handle to browseId
                if (channelId == null || channelId.isEmpty()) {
                    try {
                        JSONObject payload = new JSONObject();
                        JSONObject ctx = new JSONObject();
                        JSONObject clientObj = new JSONObject();
                        clientObj.put("clientName", "WEB");
                        clientObj.put("clientVersion", "2.20240101.00.00");
                        ctx.put("client", clientObj);
                        payload.put("context", ctx);
                        payload.put("query", cleanInput);

                        okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json; charset=utf-8");
                        okhttp3.RequestBody b = okhttp3.RequestBody.create(payload.toString(), JSON);
                        Request r = new Request.Builder()
                                .url("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
                                .post(b)
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                                .build();
                        Response res = new OkHttpClient().newCall(r).execute();
                        if (res.isSuccessful() && res.body() != null) {
                            String resStr = res.body().string();
                            Pattern bp = Pattern.compile("\"browseId\":\\s*\"(UC[a-zA-Z0-9_\\-]{22})\"");
                            Matcher bm = bp.matcher(resStr);
                            if (bm.find()) {
                                channelId = bm.group(1);
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            if (channelId != null && !channelId.isEmpty()) {
                String feedUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId;
                AppConfig.OverviewItem item = new AppConfig.OverviewItem();
                String resolvedTitle = cleanName;
                if (resolvedTitle.equals("YouTube Channel") || resolvedTitle.startsWith("UC") || resolvedTitle.startsWith("http")) {
                    try {
                        String rUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId;
                        Request req = new Request.Builder().url(rUrl).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)").build();
                        Response resp = new OkHttpClient().newCall(req).execute();
                        if (resp.isSuccessful() && resp.body() != null) {
                            String xml = resp.body().string();
                            Pattern tp = Pattern.compile("<title>(.*?)</title>");
                            Matcher tm = tp.matcher(xml);
                            if (tm.find()) resolvedTitle = tm.group(1).trim();
                        }
                    } catch (Exception ignored) {}
                }

                String logo = ChannelLogoCache.getCachedLogo(context, channelId);
                if (logo == null || logo.isEmpty()) {
                    try {
                        JSONObject payload = new JSONObject();
                        JSONObject ctx = new JSONObject();
                        JSONObject clientObj = new JSONObject();
                        clientObj.put("clientName", "WEB");
                        clientObj.put("clientVersion", "2.20240101.00.00");
                        ctx.put("client", clientObj);
                        payload.put("context", ctx);
                        payload.put("browseId", channelId);

                        okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json; charset=utf-8");
                        okhttp3.RequestBody b = okhttp3.RequestBody.create(payload.toString(), JSON);
                        Request r = new Request.Builder()
                                .url("https://www.youtube.com/youtubei/v1/browse?prettyPrint=false")
                                .post(b)
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                                .build();
                        Response res = new OkHttpClient().newCall(r).execute();
                        if (res.isSuccessful() && res.body() != null) {
                            String resStr = res.body().string();
                            JSONObject data = new JSONObject(resStr);
                            if (data.has("metadata") && data.getJSONObject("metadata").has("channelMetadataRenderer")) {
                                JSONObject meta = data.getJSONObject("metadata").getJSONObject("channelMetadataRenderer");
                                if (meta.has("avatar")) {
                                    JSONArray thumbs = meta.getJSONObject("avatar").getJSONArray("thumbnails");
                                    if (thumbs.length() > 0) {
                                        logo = thumbs.getJSONObject(thumbs.length() - 1).getString("url");
                                        if (logo != null && logo.startsWith("//")) {
                                            logo = "https:" + logo;
                                        }
                                    }
                                }
                                if (meta.has("title")) {
                                    String fetchedTitle = meta.optString("title", "");
                                    if (!fetchedTitle.isEmpty() && (resolvedTitle.equals("YouTube Channel") || resolvedTitle.startsWith("UC") || resolvedTitle.startsWith("http"))) {
                                        resolvedTitle = fetchedTitle;
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }

                item.title = resolvedTitle;
                item.provider = "videos";
                item.arguments = new ArrayList<>();
                item.arguments.add(feedUrl);

                if (logo != null && !logo.isEmpty()) {
                    item.image = logo;
                    ChannelLogoCache.saveLogo(context, channelId, logo);
                } else {
                    item.image = "https://img.icons8.com/color/96/youtube-play.png";
                }

                saveCustomChannel(context, category.jsonUrl, item);

                mainHandler.post(() -> callback.onSuccess(item, category.title));
            } else {
                // Fallback: If handle resolution didn't get UC id, use direct link as web or search
                mainHandler.post(() -> callback.onError("Could not verify YouTube channel or playlist ID. Please check the link or @handle and try again."));
            }
        });
    }

    public static class SearchResultItem {
        public String title;
        public String id;
        public String type; // "CHANNEL", "PLAYLIST", or "VIDEO"
        public String thumbnailUrl;
        public String subtitle;
        public String channelTitle;
        public String channelId;
        public String duration = "";
        public String viewCount = "";

        public SearchResultItem(String title, String id, String type, String thumbnailUrl, String subtitle) {
            this(title, id, type, thumbnailUrl, subtitle, "", "");
        }

        public SearchResultItem(String title, String id, String type, String thumbnailUrl, String subtitle, String channelTitle, String channelId) {
            this(title, id, type, thumbnailUrl, subtitle, channelTitle, channelId, "", "");
        }

        public SearchResultItem(String title, String id, String type, String thumbnailUrl, String subtitle, String channelTitle, String channelId, String duration, String viewCount) {
            this.title = title;
            this.id = id;
            this.type = type;
            this.thumbnailUrl = thumbnailUrl;
            this.subtitle = subtitle;
            this.channelTitle = channelTitle;
            this.channelId = channelId;
            this.duration = duration != null ? duration : "";
            this.viewCount = viewCount != null ? viewCount : "";
        }
    }

    public static class SearchPageResult {
        public List<SearchResultItem> items;
        public String nextContinuationToken;

        public SearchPageResult(List<SearchResultItem> items, String nextContinuationToken) {
            this.items = items != null ? items : new ArrayList<>();
            this.nextContinuationToken = nextContinuationToken;
        }
    }

    public interface SearchCallback {
        void onSuccess(List<SearchResultItem> results);
        void onError(String message);
    }

    public interface SearchPageCallback {
        void onSuccess(SearchPageResult result);
        void onError(String message);
    }

    public static void searchYouTubePage(String query, String filterType, String continuationToken, SearchPageCallback callback) {
        if ((query == null || query.trim().isEmpty()) && (continuationToken == null || continuationToken.isEmpty())) {
            if (callback != null) callback.onError("Empty search query");
            return;
        }
        Handler mainHandler = new Handler(Looper.getMainLooper());

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .build();

                List<SearchResultItem> results = new ArrayList<>();
                java.util.HashSet<String> seenIds = new java.util.HashSet<>();
                String nextContinuationToken = null;
                okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json; charset=utf-8");

                JSONObject payload = new JSONObject();
                JSONObject ctx = new JSONObject();
                JSONObject clientObj = new JSONObject();
                clientObj.put("clientName", "WEB");
                clientObj.put("clientVersion", "2.20240501.01.00");
                clientObj.put("hl", "en");
                clientObj.put("gl", "IN");
                ctx.put("client", clientObj);
                payload.put("context", ctx);

                if (continuationToken == null || continuationToken.isEmpty()) {
                    payload.put("query", query != null ? query.trim() : "");
                    if ("channel".equalsIgnoreCase(filterType)) {
                        payload.put("params", "EgIQAg%3D%3D");
                    }
                } else {
                    payload.put("continuation", continuationToken);
                }

                okhttp3.RequestBody body = okhttp3.RequestBody.create(payload.toString(), JSON);
                Request req = new Request.Builder()
                        .url("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
                        .post(body)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .build();

                Response resp = client.newCall(req).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    String respStr = resp.body().string();
                    JSONObject json = new JSONObject(respStr);
                    List<JSONObject> rawItems = new ArrayList<>();

                    if (continuationToken == null || continuationToken.isEmpty()) {
                        JSONObject contents = json.optJSONObject("contents");
                        if (contents != null) {
                            JSONObject twoCol = contents.optJSONObject("twoColumnSearchResultsRenderer");
                            if (twoCol != null) {
                                JSONObject primary = twoCol.optJSONObject("primaryContents");
                                if (primary != null) {
                                    JSONObject sectionList = primary.optJSONObject("sectionListRenderer");
                                    if (sectionList != null) {
                                        JSONArray secContents = sectionList.optJSONArray("contents");
                                        if (secContents != null) {
                                            for (int s = 0; s < secContents.length(); s++) {
                                                JSONObject sec = secContents.getJSONObject(s);
                                                if (sec.has("continuationItemRenderer")) {
                                                    JSONObject cont = sec.optJSONObject("continuationItemRenderer");
                                                    if (cont != null && cont.has("continuationEndpoint")) {
                                                        JSONObject ep = cont.optJSONObject("continuationEndpoint");
                                                        if (ep != null && ep.has("continuationCommand")) {
                                                            nextContinuationToken = ep.getJSONObject("continuationCommand").optString("token", null);
                                                        }
                                                    }
                                                }
                                                JSONObject itemSec = sec.optJSONObject("itemSectionRenderer");
                                                if (itemSec != null) {
                                                    JSONArray itemsArr = itemSec.optJSONArray("contents");
                                                    if (itemsArr != null) {
                                                        for (int j = 0; j < itemsArr.length(); j++) {
                                                            rawItems.add(itemsArr.getJSONObject(j));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        JSONArray commands = json.optJSONArray("onResponseReceivedCommands");
                        if (commands != null) {
                            for (int c = 0; c < commands.length(); c++) {
                                JSONObject act = commands.getJSONObject(c).optJSONObject("appendContinuationItemsAction");
                                if (act != null) {
                                    JSONArray contItems = act.optJSONArray("continuationItems");
                                    if (contItems != null) {
                                        for (int ci = 0; ci < contItems.length(); ci++) {
                                            JSONObject itemObj = contItems.getJSONObject(ci);
                                            if (itemObj.has("continuationItemRenderer")) {
                                                JSONObject cont = itemObj.optJSONObject("continuationItemRenderer");
                                                if (cont != null && cont.has("continuationEndpoint")) {
                                                    JSONObject ep = cont.optJSONObject("continuationEndpoint");
                                                    if (ep != null && ep.has("continuationCommand")) {
                                                        nextContinuationToken = ep.getJSONObject("continuationCommand").optString("token", null);
                                                    }
                                                }
                                            }
                                            JSONObject itemSection = itemObj.optJSONObject("itemSectionRenderer");
                                            if (itemSection != null) {
                                                JSONArray cList = itemSection.optJSONArray("contents");
                                                if (cList != null) {
                                                    for (int k = 0; k < cList.length(); k++) {
                                                        rawItems.add(cList.getJSONObject(k));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    for (JSONObject it : rawItems) {
                        if (it.has("channelRenderer")) {
                            JSONObject cr = it.getJSONObject("channelRenderer");
                            String cid = cr.optString("channelId", "");
                            String cTitle = "";
                            if (cr.has("title")) {
                                cTitle = cr.optJSONObject("title") != null ? cr.optJSONObject("title").optString("simpleText", "") : cr.optString("title", "");
                            }
                            String thumb = "";
                            JSONObject tObj = cr.optJSONObject("thumbnail");
                            if (tObj != null) {
                                JSONArray tArr = tObj.optJSONArray("thumbnails");
                                if (tArr != null && tArr.length() > 0) {
                                    thumb = tArr.getJSONObject(tArr.length() - 1).optString("url", "");
                                    if (thumb.startsWith("//")) thumb = "https:" + thumb;
                                }
                            }
                            String subText = "";
                            JSONObject sObj = cr.optJSONObject("subscriberCountText");
                            if (sObj != null) subText = sObj.optString("simpleText", "");
                            if (subText.isEmpty()) {
                                JSONObject vObj = cr.optJSONObject("videoCountText");
                                if (vObj != null) subText = vObj.optString("simpleText", "");
                            }
                            if (!cid.isEmpty() && !cTitle.isEmpty() && !seenIds.contains(cid)) {
                                seenIds.add(cid);
                                results.add(new SearchResultItem(cTitle, cid, "CHANNEL", thumb, subText.isEmpty() ? "YouTube Channel" : subText, cTitle, cid));
                            }
                        } else if (it.has("videoRenderer")) {
                            JSONObject vr = it.getJSONObject("videoRenderer");
                            String vid = vr.optString("videoId", "");
                            String vTitle = "";
                            JSONObject titleObj = vr.optJSONObject("title");
                            if (titleObj != null) {
                                JSONArray runs = titleObj.optJSONArray("runs");
                                if (runs != null && runs.length() > 0) {
                                    StringBuilder sb = new StringBuilder();
                                    for (int r = 0; r < runs.length(); r++) {
                                        sb.append(runs.getJSONObject(r).optString("text", ""));
                                    }
                                    vTitle = sb.toString();
                                } else {
                                    vTitle = titleObj.optString("simpleText", "");
                                }
                            }
                            String thumb = "";
                            JSONObject tObj = vr.optJSONObject("thumbnail");
                            if (tObj != null) {
                                JSONArray tArr = tObj.optJSONArray("thumbnails");
                                if (tArr != null && tArr.length() > 0) {
                                    thumb = tArr.getJSONObject(tArr.length() - 1).optString("url", "");
                                    if (thumb.startsWith("//")) thumb = "https:" + thumb;
                                }
                            }
                            String chName = "";
                            String chId = "";
                            JSONObject ownerObj = vr.optJSONObject("ownerText");
                            if (ownerObj == null) ownerObj = vr.optJSONObject("shortBylineText");
                            if (ownerObj != null) {
                                JSONArray runs = ownerObj.optJSONArray("runs");
                                if (runs != null && runs.length() > 0) {
                                    JSONObject firstRun = runs.getJSONObject(0);
                                    chName = firstRun.optString("text", "");
                                    JSONObject nav = firstRun.optJSONObject("navigationEndpoint");
                                    if (nav != null) {
                                        JSONObject bEndpoint = nav.optJSONObject("browseEndpoint");
                                        if (bEndpoint != null) {
                                            chId = bEndpoint.optString("browseId", "");
                                        }
                                    }
                                }
                            }
                            String viewCount = "";
                            JSONObject vcObj = vr.optJSONObject("viewCountText");
                            if (vcObj != null) {
                                viewCount = vcObj.optString("simpleText", "");
                            } else {
                                JSONObject svcObj = vr.optJSONObject("shortViewCountText");
                                if (svcObj != null) viewCount = svcObj.optString("simpleText", "");
                            }
                            String timeText = "";
                            JSONObject timeObj = vr.optJSONObject("publishedTimeText");
                            if (timeObj != null) timeText = timeObj.optString("simpleText", "");

                            String duration = YouTubeInnertubeFetcher.extractDurationFromVideoRenderer(vr);

                            String sub = (!chName.isEmpty() ? chName : "YouTube Video");
                            if (!viewCount.isEmpty()) sub += " • " + viewCount;
                            if (!timeText.isEmpty()) sub += " • " + timeText;

                            if (!vid.isEmpty() && !vTitle.isEmpty() && !seenIds.contains(vid)) {
                                seenIds.add(vid);
                                SearchResultItem resItem = new SearchResultItem(vTitle, vid, "VIDEO", thumb, sub, chName, chId, duration, viewCount);
                                results.add(resItem);
                            }
                        } else if (it.has("lockupViewModel")) {
                            JSONObject lvm = it.getJSONObject("lockupViewModel");
                            String vid = lvm.optString("contentId", "");
                            String vTitle = "";
                            String chName = "";
                            String chId = "";
                            String viewCount = "";
                            String timeText = "";
                            JSONObject meta = lvm.optJSONObject("metadata");
                            if (meta != null) {
                                JSONObject lockupMeta = meta.optJSONObject("lockupMetadataViewModel");
                                if (lockupMeta != null) {
                                    JSONObject titleObj = lockupMeta.optJSONObject("title");
                                    if (titleObj != null) {
                                        vTitle = titleObj.optString("content", "");
                                    }
                                    JSONObject metaInner = lockupMeta.optJSONObject("metadata");
                                    if (metaInner != null) {
                                        JSONObject contentMeta = metaInner.optJSONObject("contentMetadataViewModel");
                                        if (contentMeta != null) {
                                            JSONArray rows = contentMeta.optJSONArray("metadataRows");
                                            if (rows != null) {
                                                for (int r = 0; r < rows.length(); r++) {
                                                    JSONArray parts = rows.getJSONObject(r).optJSONArray("metadataParts");
                                                    if (parts != null) {
                                                        for (int p = 0; p < parts.length(); p++) {
                                                            JSONObject partText = parts.getJSONObject(p).optJSONObject("text");
                                                            if (partText != null) {
                                                                String c = partText.optString("content", "");
                                                                if (c.contains("views") || c.contains("view")) {
                                                                    viewCount = c;
                                                                } else if (c.contains("ago") || c.contains("Streamed")) {
                                                                    timeText = c;
                                                                } else if (chName.isEmpty() && !c.isEmpty()) {
                                                                    chName = c;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            String duration = YouTubeInnertubeFetcher.extractDurationFromLockup(lvm);
                            String thumb = "";
                            JSONObject contentImage = lvm.optJSONObject("contentImage");
                            if (contentImage != null) {
                                JSONObject thumbVM = contentImage.optJSONObject("thumbnailViewModel");
                                if (thumbVM != null) {
                                    JSONObject imageObj = thumbVM.optJSONObject("image");
                                    if (imageObj != null) {
                                        JSONArray sources = imageObj.optJSONArray("sources");
                                        if (sources != null && sources.length() > 0) {
                                            thumb = sources.getJSONObject(sources.length() - 1).optString("url");
                                        }
                                    }
                                }
                            }
                            String sub = (!chName.isEmpty() ? chName : "YouTube Video");
                            if (!viewCount.isEmpty()) sub += " • " + viewCount;
                            if (!timeText.isEmpty()) sub += " • " + timeText;

                            if (!vid.isEmpty() && !vTitle.isEmpty() && !seenIds.contains(vid)) {
                                seenIds.add(vid);
                                SearchResultItem resItem = new SearchResultItem(vTitle, vid, "VIDEO", thumb, sub, chName, chId, duration, viewCount);
                                results.add(resItem);
                            }
                        }
                    }

                    final String finalNextToken = nextContinuationToken;
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onSuccess(new SearchPageResult(results, finalNextToken));
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        if (callback != null) callback.onError("Failed to fetch results from YouTube");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    if (callback != null) callback.onError("Network error: " + e.getMessage());
                });
            }
        });
    }

    public static void searchYouTube(String query, String filterType, SearchCallback callback) {
        searchYouTubePage(query, filterType, null, new SearchPageCallback() {
            @Override
            public void onSuccess(SearchPageResult result) {
                if (callback != null) {
                    callback.onSuccess(result.items);
                }
            }

            @Override
            public void onError(String message) {
                if (callback != null) {
                    callback.onError(message);
                }
            }
        });
    }

    public static void showUniversalAddChannelDialog(Context context, String defaultCategorySlug, Runnable onAddedCallback) {
        if (context == null) return;
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_channel, null);

        View btnClose = dialogView.findViewById(R.id.btn_dialog_close);
        TextView tabSearch = dialogView.findViewById(R.id.tab_search_mode);
        TextView tabDirect = dialogView.findViewById(R.id.tab_direct_mode);
        View containerSearch = dialogView.findViewById(R.id.container_search_mode);
        View containerDirect = dialogView.findViewById(R.id.container_direct_mode);

        EditText etSearchQuery = dialogView.findViewById(R.id.et_dialog_search_query);
        View btnSearchAction = dialogView.findViewById(R.id.btn_dialog_search_action);
        TextView chipAll = dialogView.findViewById(R.id.chip_filter_all);
        TextView chipChannels = dialogView.findViewById(R.id.chip_filter_channels);
        TextView chipPlaylists = dialogView.findViewById(R.id.chip_filter_playlists);
        ProgressBar progressSearch = dialogView.findViewById(R.id.progress_search);
        RecyclerView rvSearchResults = dialogView.findViewById(R.id.rv_search_results);

        View cardSelected = dialogView.findViewById(R.id.card_selected_item);
        ImageView imgSelectedThumb = dialogView.findViewById(R.id.img_selected_thumb);
        TextView tvSelectedTitle = dialogView.findViewById(R.id.tv_selected_title);
        TextView tvSelectedType = dialogView.findViewById(R.id.tv_selected_type);
        View btnClearSelection = dialogView.findViewById(R.id.btn_clear_selection);

        EditText etDirectName = dialogView.findViewById(R.id.et_channel_name);
        EditText etDirectUrl = dialogView.findViewById(R.id.et_channel_url);

        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_category);
        View btnNewCategory = dialogView.findViewById(R.id.btn_dialog_new_category);
        ProgressBar progressLoading = dialogView.findViewById(R.id.progress_loading);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnAddConfirm = dialogView.findViewById(R.id.btn_add_confirm);

        final SearchResultItem[] selectedItemHolder = new SearchResultItem[1];
        final String[] activeFilterHolder = new String[]{"all"};

        // Populate Category Spinner
        List<CategoryOption> categories = getAvailableCategories(context);
        ArrayAdapter<CategoryOption> spinnerAdapter = new ArrayAdapter<>(context, R.layout.item_spinner_category, categories);
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerCategory.setAdapter(spinnerAdapter);

        if (defaultCategorySlug != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).jsonUrl.equalsIgnoreCase(defaultCategorySlug)) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }

        if (btnNewCategory != null) {
            btnNewCategory.setOnClickListener(v -> {
                showCreateCategoryDialog(context, newCat -> {
                    List<CategoryOption> updatedList = getAvailableCategories(context);
                    ArrayAdapter<CategoryOption> updatedAdapter = new ArrayAdapter<>(context, R.layout.item_spinner_category, updatedList);
                    updatedAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
                    spinnerCategory.setAdapter(updatedAdapter);
                    for (int i = 0; i < updatedList.size(); i++) {
                        if (updatedList.get(i).jsonUrl.equalsIgnoreCase(newCat.jsonUrl)) {
                            spinnerCategory.setSelection(i);
                            break;
                        }
                    }
                });
            });
        }

        // Setup Search Results List
        rvSearchResults.setLayoutManager(new LinearLayoutManager(context));
        SearchResultAdapter searchAdapter = new SearchResultAdapter(context, item -> {
            selectedItemHolder[0] = item;
            if (cardSelected != null) {
                cardSelected.setVisibility(View.VISIBLE);
                if (tvSelectedTitle != null) tvSelectedTitle.setText(item.title);
                if (tvSelectedType != null) {
                    tvSelectedType.setText(("PLAYLIST".equals(item.type) ? "📋 Playlist • " : "📺 Channel • ") + item.subtitle);
                }
                if (imgSelectedThumb != null && item.thumbnailUrl != null && !item.thumbnailUrl.isEmpty()) {
                    Glide.with(context).load(item.thumbnailUrl).placeholder(R.drawable.ic_placeholder_media).into(imgSelectedThumb);
                }
            }
        });
        rvSearchResults.setAdapter(searchAdapter);

        // Clear Selection
        if (btnClearSelection != null) {
            btnClearSelection.setOnClickListener(v -> {
                selectedItemHolder[0] = null;
                if (cardSelected != null) cardSelected.setVisibility(View.GONE);
                searchAdapter.clearSelection();
            });
        }

        // Mode Switching: Search vs Direct
        tabSearch.setOnClickListener(v -> {
            tabSearch.setTextColor(android.graphics.Color.WHITE);
            tabSearch.setBackgroundResource(R.drawable.bg_btn_accent);
            tabDirect.setTextColor(android.graphics.Color.parseColor("#94A3B8"));
            tabDirect.setBackground(null);
            containerSearch.setVisibility(View.VISIBLE);
            containerDirect.setVisibility(View.GONE);
        });

        tabDirect.setOnClickListener(v -> {
            tabDirect.setTextColor(android.graphics.Color.WHITE);
            tabDirect.setBackgroundResource(R.drawable.bg_btn_accent);
            tabSearch.setTextColor(android.graphics.Color.parseColor("#94A3B8"));
            tabSearch.setBackground(null);
            containerDirect.setVisibility(View.VISIBLE);
            containerSearch.setVisibility(View.GONE);
        });

        // Filter chips logic
        Runnable updateChips = () -> {
            String f = activeFilterHolder[0];
            chipAll.setBackgroundResource("all".equals(f) ? R.drawable.bg_badge : R.drawable.bg_badge_outline);
            chipAll.setTextColor("all".equals(f) ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#94A3B8"));

            chipChannels.setBackgroundResource("channel".equals(f) ? R.drawable.bg_badge : R.drawable.bg_badge_outline);
            chipChannels.setTextColor("channel".equals(f) ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#94A3B8"));

            chipPlaylists.setBackgroundResource("playlist".equals(f) ? R.drawable.bg_badge : R.drawable.bg_badge_outline);
            chipPlaylists.setTextColor("playlist".equals(f) ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#94A3B8"));
        };

        chipAll.setOnClickListener(v -> { activeFilterHolder[0] = "all"; updateChips.run(); });
        chipChannels.setOnClickListener(v -> { activeFilterHolder[0] = "channel"; updateChips.run(); });
        chipPlaylists.setOnClickListener(v -> { activeFilterHolder[0] = "playlist"; updateChips.run(); });

        // Search Action
        Runnable doSearch = () -> {
            String q = etSearchQuery != null && etSearchQuery.getText() != null ? etSearchQuery.getText().toString().trim() : "";
            if (q.isEmpty()) {
                Toast.makeText(context, "Please enter a name or topic to search", Toast.LENGTH_SHORT).show();
                return;
            }
            if (progressSearch != null) progressSearch.setVisibility(View.VISIBLE);
            if (rvSearchResults != null) rvSearchResults.setVisibility(View.GONE);

            searchYouTube(q, activeFilterHolder[0], new SearchCallback() {
                @Override
                public void onSuccess(List<SearchResultItem> results) {
                    if (progressSearch != null) progressSearch.setVisibility(View.GONE);
                    if (rvSearchResults != null) {
                        rvSearchResults.setVisibility(View.VISIBLE);
                        searchAdapter.setItems(results);
                        if (results.isEmpty()) {
                            Toast.makeText(context, "No results found. Try a different search term.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onError(String message) {
                    if (progressSearch != null) progressSearch.setVisibility(View.GONE);
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
            });
        };

        if (btnSearchAction != null) {
            btnSearchAction.setOnClickListener(v -> doSearch.run());
        }
        if (etSearchQuery != null) {
            etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
                doSearch.run();
                return true;
            });
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (btnAddConfirm != null) {
            btnAddConfirm.setOnClickListener(v -> {
                CategoryOption selectedCat = (CategoryOption) spinnerCategory.getSelectedItem();
                if (selectedCat == null) {
                    Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (containerSearch.getVisibility() == View.VISIBLE && selectedItemHolder[0] != null) {
                    // Selected from YouTube Search
                    SearchResultItem res = selectedItemHolder[0];
                    if ("PLAYLIST".equalsIgnoreCase(res.type)) {
                        addDirectPlaylist(context, res.title, res.id, res.thumbnailUrl, selectedCat);
                        Toast.makeText(context, "Added playlist \"" + res.title + "\" to " + selectedCat.title + "!", Toast.LENGTH_LONG).show();
                    } else {
                        addDirectChannel(context, res.title, res.id, res.thumbnailUrl, selectedCat);
                        Toast.makeText(context, "Added channel \"" + res.title + "\" to " + selectedCat.title + "!", Toast.LENGTH_LONG).show();
                    }
                    dialog.dismiss();
                    if (onAddedCallback != null) onAddedCallback.run();
                } else {
                    // Direct Link Mode
                    String name = "";
                    String url = "";

                    if (containerSearch.getVisibility() == View.VISIBLE) {
                        name = etSearchQuery != null && etSearchQuery.getText() != null ? etSearchQuery.getText().toString().trim() : "";
                        url = name;
                    } else {
                        name = etDirectName != null && etDirectName.getText() != null ? etDirectName.getText().toString().trim() : "";
                        url = etDirectUrl != null && etDirectUrl.getText() != null ? etDirectUrl.getText().toString().trim() : "";
                    }

                    if (name.isEmpty() || url.isEmpty()) {
                        Toast.makeText(context, "Please select a search result or enter link/name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final String finalName = name;
                    if (progressLoading != null) progressLoading.setVisibility(View.VISIBLE);
                    btnAddConfirm.setEnabled(false);

                    resolveAndAddChannel(context, finalName, url, selectedCat, new AddChannelCallback() {
                        @Override
                        public void onSuccess(AppConfig.OverviewItem item, String categoryTitle) {
                            Toast.makeText(context, "Added \"" + finalName + "\" to " + categoryTitle + "!", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                            if (onAddedCallback != null) onAddedCallback.run();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            if (progressLoading != null) progressLoading.setVisibility(View.GONE);
                            btnAddConfirm.setEnabled(true);
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }

        dialog.show();
    }

    private static class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.VH> {
        private final Context context;
        private final List<SearchResultItem> items = new ArrayList<>();
        private final OnResultSelectedListener listener;
        private int selectedIndex = -1;

        public interface OnResultSelectedListener {
            void onSelected(SearchResultItem item);
        }

        public SearchResultAdapter(Context context, OnResultSelectedListener listener) {
            this.context = context;
            this.listener = listener;
        }

        public void setItems(List<SearchResultItem> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            selectedIndex = -1;
            notifyDataSetChanged();
        }

        public void clearSelection() {
            int prev = selectedIndex;
            selectedIndex = -1;
            if (prev >= 0) notifyItemChanged(prev);
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_youtube_search_result, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            SearchResultItem item = items.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvSubtitle.setText(item.subtitle);
            holder.badgeType.setText(item.type);

            if ("PLAYLIST".equalsIgnoreCase(item.type)) {
                holder.badgeType.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#0284C7")));
            } else {
                holder.badgeType.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E11D48")));
            }

            if (item.thumbnailUrl != null && !item.thumbnailUrl.isEmpty()) {
                Glide.with(context)
                        .load(item.thumbnailUrl)
                        .placeholder(R.drawable.ic_placeholder_media)
                        .into(holder.imgThumb);
            } else {
                holder.imgThumb.setImageResource("PLAYLIST".equalsIgnoreCase(item.type) ? R.drawable.ic_play_circle : R.drawable.ic_youtube_brand);
            }

            boolean isSelected = (position == selectedIndex);
            holder.card.setStrokeColor(isSelected ? android.graphics.Color.parseColor("#10B981") : android.graphics.Color.parseColor("#334155"));
            holder.card.setCardBackgroundColor(isSelected ? android.graphics.Color.parseColor("#064E3B") : android.graphics.Color.parseColor("#1E293B"));
            if (holder.btnAdd != null) {
                holder.btnAdd.setText(isSelected ? "✓ Selected" : "+ Add");
            }

            holder.itemView.setOnClickListener(v -> {
                int prev = selectedIndex;
                selectedIndex = holder.getAdapterPosition();
                if (prev >= 0) notifyItemChanged(prev);
                notifyItemChanged(selectedIndex);
                if (listener != null) listener.onSelected(item);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            MaterialCardView card;
            ImageView imgThumb;
            TextView tvTitle;
            TextView tvSubtitle;
            TextView badgeType;
            TextView btnAdd;

            VH(View v) {
                super(v);
                card = v.findViewById(R.id.card_search_result);
                imgThumb = v.findViewById(R.id.img_result_thumb);
                tvTitle = v.findViewById(R.id.tv_result_title);
                tvSubtitle = v.findViewById(R.id.tv_result_subtitle);
                badgeType = v.findViewById(R.id.badge_result_type);
                btnAdd = v.findViewById(R.id.btn_result_add);
            }
        }
    }

    public static void saveChannelOverride(Context context, String channelKey, String newId) {
        if (context == null || channelKey == null || newId == null) return;
        context.getSharedPreferences("channel_id_overrides", Context.MODE_PRIVATE)
                .edit()
                .putString(channelKey.trim().toLowerCase(java.util.Locale.US), newId.trim())
                .apply();
    }

    public static String getChannelOverride(Context context, String channelKey, String defaultId) {
        if (context == null || channelKey == null) return defaultId;
        return context.getSharedPreferences("channel_id_overrides", Context.MODE_PRIVATE)
                .getString(channelKey.trim().toLowerCase(java.util.Locale.US), defaultId);
    }

    public static void showEditChannelDialog(Context context, String channelName, String currentId, Runnable onSaved) {
        if (context == null) return;

        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (18 * context.getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, pad / 2);

        TextView tvHint = new TextView(context);
        tvHint.setText("If the live stream is not working or offline, enter a new YouTube Channel ID (e.g. UC...), Video ID, or .m3u8 stream URL:");
        tvHint.setTextSize(13);
        tvHint.setTextColor(0xFF64748B);
        tvHint.setPadding(0, 0, 0, (int) (10 * context.getResources().getDisplayMetrics().density));
        layout.addView(tvHint);

        com.google.android.material.textfield.TextInputLayout inputLayout =
                new com.google.android.material.textfield.TextInputLayout(context);
        inputLayout.setHint("Channel ID / Video ID / Stream URL");
        inputLayout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);

        com.google.android.material.textfield.TextInputEditText etId =
                new com.google.android.material.textfield.TextInputEditText(context);
        etId.setText(currentId != null ? currentId : "");
        etId.setSingleLine(true);
        inputLayout.addView(etId);
        layout.addView(inputLayout);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle("✏️ Edit Stream: " + (channelName != null ? channelName : "Channel"))
                .setView(layout)
                .setPositiveButton("Save & Reload", (d, w) -> {
                    String newId = etId.getText() != null ? etId.getText().toString().trim() : "";
                    if (!newId.isEmpty()) {
                        saveChannelOverride(context, channelName, newId);
                        if (currentId != null && !currentId.isEmpty()) {
                            saveChannelOverride(context, currentId, newId);
                        }
                        android.widget.Toast.makeText(context, "✓ Channel ID updated!", android.widget.Toast.LENGTH_SHORT).show();
                        if (onSaved != null) onSaved.run();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
