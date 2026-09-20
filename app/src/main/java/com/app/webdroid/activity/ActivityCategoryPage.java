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
import com.app.webdroid.model.AppConfig;
import com.app.webdroid.model.NewsItem;
import com.app.webdroid.news.ui.ActivityUsNews;
import com.app.webdroid.util.FollowManager;
import com.app.webdroid.util.RssParser;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shobmc.san.R;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ActivityCategoryPage extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_KEY = "extra_category_key";
    public static final String EXTRA_CATEGORY_TITLE = "extra_category_title";

    public static void start(Context context, String categoryKey, String categoryTitle) {
        Intent intent = new Intent(context, ActivityCategoryPage.class);
        intent.putExtra(EXTRA_CATEGORY_KEY, categoryKey);
        if (categoryTitle != null) {
            intent.putExtra(EXTRA_CATEGORY_TITLE, categoryTitle);
        }
        context.startActivity(intent);
    }

    public static void start(Context context, String categoryKey) {
        start(context, categoryKey, null);
    }

    private String categoryKey = "TECH";
    private String categoryTitle = null;
    private FollowManager.CategoryMeta categoryMeta;
    private AdapterNews adapter;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.app.webdroid.util.Tools.getTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_page);

        if (getIntent() != null) {
            if (getIntent().hasExtra(EXTRA_CATEGORY_KEY)) {
                categoryKey = getIntent().getStringExtra(EXTRA_CATEGORY_KEY);
            }
            if (getIntent().hasExtra(EXTRA_CATEGORY_TITLE)) {
                categoryTitle = getIntent().getStringExtra(EXTRA_CATEGORY_TITLE);
            }
        }
        categoryMeta = FollowManager.getCategoryMeta(categoryKey);
        if (categoryTitle != null && !categoryTitle.isEmpty() && categoryMeta != null && !categoryTitle.equalsIgnoreCase(categoryMeta.title)) {
            categoryMeta = new FollowManager.CategoryMeta(
                    categoryMeta.key,
                    categoryTitle,
                    "All latest news and updates on " + categoryTitle,
                    categoryMeta.iconRes,
                    categoryMeta.coverUrl,
                    categoryMeta.avatarUrl,
                    categoryMeta.baseFollowers,
                    categoryMeta.postsCount,
                    categoryMeta.viewsCount
            );
        }

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

        // 1. Observe cached news from Room DB for instant display
        AppDatabase.getDatabase(this).newsDao().getAllNews().observe(this, allNews -> {
            if (allNews == null) return;

            List<NewsItem> matched = new ArrayList<>();
            for (NewsItem item : allNews) {
                if (matchesCategory(item, categoryKey)) {
                    matched.add(item);
                }
            }
            if (adapter != null && !matched.isEmpty()) {
                adapter.setItems(matched);
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
            }
        });

        // 2. Fetch live dedicated RSS feeds specifically for this category
        fetchCategoryFeedsLive();
    }

    private void fetchCategoryFeedsLive() {
        new Thread(() -> {
            try {
                List<AppConfig.RssSource> sources = getSourcesForCategory(this, categoryKey);
                if (sources.isEmpty()) {
                    runOnUiThread(() -> {
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    });
                    return;
                }

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(6, TimeUnit.SECONDS)
                        .readTimeout(6, TimeUnit.SECONDS)
                        .build();
                RssParser parser = new RssParser();
                List<NewsItem> allFetched = Collections.synchronizedList(new ArrayList<>());
                ExecutorService pool = Executors.newFixedThreadPool(Math.min(sources.size(), 4));
                CountDownLatch latch = new CountDownLatch(sources.size());

                for (AppConfig.RssSource src : sources) {
                    pool.execute(() -> {
                        try {
                            Request req = new Request.Builder()
                                    .url(src.url)
                                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                    .build();
                            Response resp = client.newCall(req).execute();
                            if (resp.isSuccessful() && resp.body() != null) {
                                List<NewsItem> parsed = parser.parseNews(resp.body().byteStream(), src.title, categoryKey);
                                if (parsed != null && !parsed.isEmpty()) {
                                    for (NewsItem it : parsed) {
                                        if (it.category == null || it.category.isEmpty()) {
                                            it.category = categoryKey;
                                        }
                                    }
                                    allFetched.addAll(parsed);
                                }
                            }
                        } catch (Exception ignored) {
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                try {
                    latch.await(7, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}
                pool.shutdown();

                if (!allFetched.isEmpty()) {
                    List<NewsItem> copy = new ArrayList<>(allFetched);
                    AppDatabase.getDatabase(ActivityCategoryPage.this).newsDao().insertNews(copy);
                    runOnUiThread(() -> {
                        if (adapter != null) {
                            List<NewsItem> cur = adapter.getItems();
                            if (cur == null || cur.isEmpty() || cur.size() < copy.size()) {
                                adapter.setItems(copy);
                            }
                        }
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    });
                } else {
                    runOnUiThread(() -> {
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                });
            }
        }).start();
    }

    private List<AppConfig.RssSource> getSourcesForCategory(Context context, String key) {
        List<AppConfig.RssSource> result = new ArrayList<>();
        String upperKey = key != null ? key.toUpperCase(Locale.US) : "";

        // 1. Check if news_rss.json has predefined sources matching this category
        try (InputStream is = context.getAssets().open("news_rss.json");
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<AppConfig.RssSource>>(){}.getType();
            List<AppConfig.RssSource> allRss = new Gson().fromJson(reader, listType);
            if (allRss != null) {
                for (AppConfig.RssSource src : allRss) {
                    if (src.category != null && src.category.equalsIgnoreCase(upperKey)) {
                        result.add(src);
                    }
                }
            }
        } catch (Exception ignored) {}

        // 2. Generate Google News dedicated RSS feed for the specific topic/category
        String queryTitle = categoryMeta != null ? categoryMeta.title : upperKey.replace("_", " ");
        try {
            String encoded = URLEncoder.encode(queryTitle + " US news", "UTF-8");
            AppConfig.RssSource googleTopic = new AppConfig.RssSource();
            googleTopic.title = queryTitle + " (Google News)";
            googleTopic.url = "https://news.google.com/rss/search?q=" + encoded + "&hl=en-US&gl=US&ceid=US:en";
            googleTopic.category = upperKey;
            result.add(googleTopic);
        } catch (Exception ignored) {}

        return result;
    }

    private boolean matchesCategory(NewsItem item, String category) {
        if (item == null) return false;
        String itemCat = item.category != null ? item.category.trim().toUpperCase(Locale.US) : "";
        String normCat = category != null ? category.trim().toUpperCase(Locale.US) : "";

        // 1. Direct Category Tag Match
        if (!normCat.isEmpty() && normCat.equalsIgnoreCase(itemCat)) {
            return true;
        }

        String source = item.sourceName != null ? item.sourceName.toLowerCase(Locale.US) : "";
        String title = item.title != null ? item.title.toLowerCase(Locale.US) : "";
        String desc = item.description != null ? item.description.toLowerCase(Locale.US) : "";
        String combined = source + " " + title + " " + desc;

        // 2. Fine-grained Specific Category Keywords
        if (normCat.contains("WHITE_HOUSE") || normCat.contains("PRESIDENT")) {
            return combined.contains("white house") || combined.contains("president") || combined.contains("biden") || combined.contains("trump") || combined.contains("oval office");
        }
        if (normCat.contains("CONGRESS") || normCat.contains("SENATE") || normCat.contains("HOUSE")) {
            return combined.contains("congress") || combined.contains("senate") || combined.contains("capitol") || combined.contains("lawmaker") || combined.contains("speaker") || combined.contains("house of rep");
        }
        if (normCat.contains("COURT") || normCat.contains("JUDGE") || normCat.contains("JUSTICE")) {
            return combined.contains("supreme court") || combined.contains("court") || combined.contains("judge") || combined.contains("justice") || combined.contains("ruling");
        }
        if (normCat.contains("INFLAT") || normCat.contains("RATE") || normCat.contains("FED")) {
            return combined.contains("inflation") || combined.contains("federal reserve") || combined.contains("fed ") || combined.contains("interest rate") || combined.contains("cpi");
        }
        if (normCat.contains("JOB") || normCat.contains("UNEMPLOY")) {
            return combined.contains("job") || combined.contains("unemployment") || combined.contains("hiring") || combined.contains("labor") || combined.contains("wage");
        }
        if (normCat.contains("STOCK") || normCat.contains("DOW") || normCat.contains("NASDAQ") || normCat.contains("SP500") || normCat.contains("WALL_STREET")) {
            return combined.contains("stock") || combined.contains("wall street") || combined.contains("dow") || combined.contains("nasdaq") || combined.contains("s&p") || combined.contains("market");
        }
        if (normCat.contains("ECONOM")) {
            return combined.contains("economy") || combined.contains("economic") || combined.contains("gdp") || combined.contains("recession") || combined.contains("inflation");
        }
        if (normCat.contains("BREAK")) {
            return combined.contains("breaking") || combined.contains("alert") || combined.contains("urgent");
        }
        if (normCat.contains("TOP") || normCat.contains("HEADLINE")) {
            return "TOP".equals(itemCat) || source.contains("top") || source.contains("breaking") || title.contains("breaking");
        }

        // 3. Standard Category Enums
        switch (normCat) {
            case "SPORTS":
                if ("SPORTS".equals(itemCat)) return true;
                return source.contains("sport") || source.contains("espn")
                        || combined.contains("nfl") || combined.contains("nba") || combined.contains("mlb")
                        || combined.contains("football") || combined.contains("basketball") || combined.contains("championship");

            case "WEATHER":
                if ("WEATHER".equals(itemCat)) return true;
                boolean isMetSource = source.contains("weather") || source.contains("accuweather")
                        || source.contains("noaa") || source.contains("meteorol") || source.contains("weather service");
                if (isMetSource) return true;
                return combined.contains("temperature") || combined.contains("forecast") || combined.contains("meteorol")
                        || combined.contains("radar") || combined.contains("hurricane") || combined.contains("tornado")
                        || combined.contains("blizzard") || combined.contains("heat wave");

            case "POLITICS":
                if ("POLITICS".equals(itemCat)) return true;
                return source.contains("politico") || source.contains("hill") || source.contains("politic")
                        || combined.contains("biden") || combined.contains("trump") || combined.contains("congress")
                        || combined.contains("senate") || combined.contains("white house") || combined.contains("election");

            case "BUSINESS":
                if ("BUSINESS".equals(itemCat)) return true;
                return source.contains("business") || source.contains("market") || source.contains("wsj")
                        || source.contains("cnbc") || source.contains("bloomberg") || source.contains("finance")
                        || combined.contains("stock") || combined.contains("dow jones") || combined.contains("nasdaq");

            case "TECH":
            case "TECHNOLOGY":
                if ("TECH".equals(itemCat) || "TECHNOLOGY".equals(itemCat) || "SCIENCE".equals(itemCat)) return true;
                return source.contains("tech") || source.contains("verge") || source.contains("wired")
                        || combined.contains("ai") || combined.contains("artificial intelligence") || combined.contains("apple")
                        || combined.contains("google") || combined.contains("microsoft") || combined.contains("openai");

            case "HEALTH":
                if ("HEALTH".equals(itemCat)) return true;
                return source.contains("health") || source.contains("medical")
                        || combined.contains("fda") || combined.contains("cdc") || combined.contains("vaccine")
                        || combined.contains("doctor") || combined.contains("hospital") || combined.contains("disease");

            case "ENTERTAINMENT":
            case "CINEMA":
                if ("ENTERTAINMENT".equals(itemCat) || "CINEMA".equals(itemCat)) return true;
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
                // Category Title Keyword Fallback
                if (categoryMeta != null && categoryMeta.title != null) {
                    String[] words = categoryMeta.title.toLowerCase(Locale.US).split("\\s+");
                    for (String w : words) {
                        if (w.length() > 3 && !"news".equals(w) && !"with".equals(w) && !"from".equals(w) && !"feed".equals(w)) {
                            if (combined.contains(w)) return true;
                        }
                    }
                }
                return false;
        }
    }
}
