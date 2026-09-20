package com.app.webdroid.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.webdroid.adapter.AdapterNews;
import com.app.webdroid.database.AppDatabase;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.model.NewsItem;
import com.app.webdroid.news.ui.ActivityUsNews;
import com.app.webdroid.util.FollowManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.shobmc.san.R;

import java.util.ArrayList;
import java.util.List;

public class ActivityCategoryPage extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_KEY = "extra_category_key";

    public static void start(Context context, String categoryKey) {
        Intent intent = new Intent(context, ActivityCategoryPage.class);
        intent.putExtra(EXTRA_CATEGORY_KEY, categoryKey);
        context.startActivity(intent);
    }

    private String categoryKey = "TECH";
    private FollowManager.CategoryMeta categoryMeta;
    private AdapterNews adapter;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.app.webdroid.util.Tools.getTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_page);

        if (getIntent() != null && getIntent().hasExtra(EXTRA_CATEGORY_KEY)) {
            categoryKey = getIntent().getStringExtra(EXTRA_CATEGORY_KEY);
        }
        categoryMeta = FollowManager.getCategoryMeta(categoryKey);

        boolean isDark = new SharedPref(this).getIsDarkTheme();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int bgCol = ContextCompat.getColor(this, isDark ? R.color.color_dark_toolbar : R.color.color_light_primary);
            getWindow().setStatusBarColor(bgCol);
        }
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false);
        }

        // Toolbar Setup
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        TextView tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
        if (tvToolbarTitle != null && categoryMeta != null) {
            tvToolbarTitle.setText(categoryMeta.title);
        }

        ImageView btnSearch = findViewById(R.id.btn_category_search);
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> ActivityUsNews.start(this));
        }

        // Swipe Refresh
        swipeRefresh = findViewById(R.id.swipe_refresh);
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.colorAccent);
            swipeRefresh.setOnRefreshListener(this::loadCategoryNews);
        }

        // News List Setup with Category Header
        RecyclerView rvArticles = findViewById(R.id.rv_category_articles);
        if (rvArticles != null) {
            rvArticles.setLayoutManager(new LinearLayoutManager(this));
            adapter = new AdapterNews(this, new ArrayList<>());
            if (categoryMeta != null) {
                adapter.setCategoryMeta(categoryMeta);
            }
            adapter.setOnItemClickListener((v, obj, position) -> {
                if (obj == null) return;
                Intent intent = new Intent(ActivityCategoryPage.this, ActivityNewsDetail.class);
                intent.putExtra("title", obj.title);
                intent.putExtra("description", obj.description);
                intent.putExtra("link", obj.link);
                intent.putExtra("imageUrl", obj.imageUrl);
                intent.putExtra("pubDate", obj.pubDate);
                intent.putExtra("sourceName", obj.sourceName);
                startActivity(intent);
            });
            rvArticles.setAdapter(adapter);
        }

        loadCategoryNews();
    }

    private void loadCategoryNews() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        AppDatabase.getDatabase(this).newsDao().getAllNews().observe(this, allNews -> {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            if (allNews == null) return;

            List<NewsItem> matched = new ArrayList<>();
            for (NewsItem item : allNews) {
                if (matchesCategory(item, categoryKey)) {
                    matched.add(item);
                }
            }
            if (adapter != null) {
                adapter.setItems(matched);
            }
        });
    }

    private boolean matchesCategory(NewsItem item, String category) {
        if (item == null) return false;
        String itemCat = item.category != null ? item.category.trim().toUpperCase(java.util.Locale.US) : "";
        String source = item.sourceName != null ? item.sourceName.toLowerCase(java.util.Locale.US) : "";
        String title = item.title != null ? item.title.toLowerCase(java.util.Locale.US) : "";
        String desc = item.description != null ? item.description.toLowerCase(java.util.Locale.US) : "";
        String combined = source + " " + title + " " + desc;

        switch (category) {
            case "SPORTS":
                if ("SPORTS".equals(itemCat)) return true;
                return source.contains("sport") || source.contains("espn")
                        || combined.contains("nfl") || combined.contains("nba") || combined.contains("mlb")
                        || combined.contains("football") || combined.contains("basketball") || combined.contains("championship");

            case "WEATHER":
                if (combined.contains("trump") || combined.contains("biden") || combined.contains("election")
                        || combined.contains("congress") || combined.contains("court") || combined.contains("nfl")) {
                    return false;
                }
                boolean isMetSource = source.contains("weather") || source.contains("accuweather")
                        || source.contains("noaa") || source.contains("meteorol") || source.contains("weather service");
                if (isMetSource) return true;
                return combined.contains("temperature") || combined.contains("forecast") || combined.contains("meteorol")
                        || combined.contains("radar") || combined.contains("hurricane") || combined.contains("tornado")
                        || combined.contains("blizzard") || combined.contains("heat wave") || combined.contains("snowfall")
                        || combined.contains("rainfall");

            case "POLITICS":
                if ("POLITICS".equals(itemCat)) return true;
                return source.contains("politico") || source.contains("hill") || source.contains("politic")
                        || combined.contains("biden") || combined.contains("trump") || combined.contains("congress")
                        || combined.contains("senate") || combined.contains("white house") || combined.contains("election");

            case "BUSINESS":
                if ("BUSINESS".equals(itemCat)) return true;
                return source.contains("business") || source.contains("market") || source.contains("wsj")
                        || source.contains("cnbc") || source.contains("bloomberg") || source.contains("finance")
                        || combined.contains("stock") || combined.contains("dow jones") || combined.contains("nasdaq")
                        || combined.contains("fed") || combined.contains("inflation");

            case "TECH":
                if ("TECH".equals(itemCat) || "SCIENCE".equals(itemCat)) return true;
                return source.contains("tech") || source.contains("verge") || source.contains("wired")
                        || combined.contains("ai") || combined.contains("artificial intelligence") || combined.contains("apple")
                        || combined.contains("google") || combined.contains("microsoft") || combined.contains("openai");

            case "HEALTH":
                if ("HEALTH".equals(itemCat)) return true;
                return source.contains("health") || source.contains("medical")
                        || combined.contains("fda") || combined.contains("cdc") || combined.contains("vaccine")
                        || combined.contains("doctor") || combined.contains("hospital") || combined.contains("disease");

            case "ENTERTAINMENT":
                if ("ENTERTAINMENT".equals(itemCat)) return true;
                return source.contains("entertainment") || source.contains("variety") || source.contains("hollywood")
                        || combined.contains("movie") || combined.contains("actor") || combined.contains("film");

            case "WORLD":
                if ("WORLD".equals(itemCat)) return true;
                return source.contains("world") || source.contains("international") || source.contains("foreign")
                        || combined.contains("global") || combined.contains("europe") || combined.contains("asia");

            case "LOCAL":
                if ("LOCAL".equals(itemCat)) return true;
                return source.contains("local") || source.contains("metro") || source.contains("state")
                        || combined.contains("police") || combined.contains("mayor") || combined.contains("county");

            default:
                return true;
        }
    }
}
