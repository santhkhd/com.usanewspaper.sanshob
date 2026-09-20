package com.app.webdroid.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shobmc.san.R;
import com.app.webdroid.activity.ActivityNewsDetail;
import com.app.webdroid.activity.ActivityRadioPlayer;
import com.app.webdroid.activity.MainActivity;
import com.app.webdroid.adapter.AdapterNews;
import com.app.webdroid.database.AppDatabase;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.model.AppConfig;
import com.app.webdroid.model.NewsItem;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FragmentHomeDiscovery extends Fragment {

    private MainActivity activity;
    private SharedPref sharedPref;

    private SwipeRefreshLayout swipeRefresh;
    private Toolbar toolbar;
    private TextView toolbarTitle;
    private ImageView btnSearch;

    // Multi-Category News Tabs
    private AdapterNews categoryNewsAdapter;
    private RecyclerView rvCategoryNews;
    private View layoutHomeLoading;
    private String currentSelectedCategory = "FOR_YOU";
    private final List<TextView> allCategoryTabs = new ArrayList<>();

    // In-memory static cache for instant launch rendering
    private static List<NewsItem> sCachedNews = null;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            activity = (MainActivity) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home_discovery, container, false);
        sharedPref = new SharedPref(requireContext());

        layoutHomeLoading = root.findViewById(R.id.layout_home_loading);
        if (sCachedNews == null || sCachedNews.isEmpty()) {
            if (layoutHomeLoading != null) layoutHomeLoading.setVisibility(View.VISIBLE);
        } else {
            if (layoutHomeLoading != null) layoutHomeLoading.setVisibility(View.GONE);
        }

        setupToolbar(root);
        initViews(root);
        loadAllSections();

        return root;
    }

    private void setupToolbar(View root) {
        toolbar = root.findViewById(R.id.toolbar);
        toolbarTitle = root.findViewById(R.id.toolbar_title);
        btnSearch = root.findViewById(R.id.btn_home_search);

        if (toolbar != null) {
            if (sharedPref.getNavigationDrawer()) {
                toolbar.setNavigationIcon(R.drawable.ic_menu);
                toolbar.setNavigationOnClickListener(v -> {
                    if (activity != null) {
                        activity.openDrawer();
                    }
                });
            } else {
                toolbar.setNavigationIcon(null);
            }
        }

        if (toolbarTitle != null) {
            toolbarTitle.setText(R.string.app_name);
        }

        View btnUsNewsHub = root.findViewById(R.id.btn_home_us_news_hub);
        if (btnUsNewsHub != null) {
            btnUsNewsHub.setOnClickListener(v -> com.app.webdroid.news.ui.ActivityUsNews.start(requireContext()));
        }

        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> com.app.webdroid.news.ui.ActivityUsNews.start(requireContext()));
        }

        ImageView btnOverflow = root.findViewById(R.id.btn_home_overflow);
        if (btnOverflow != null) {
            btnOverflow.setOnClickListener(v -> {
                if (activity != null) {
                    androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(v.getContext(), v);
                    popup.getMenuInflater().inflate(R.menu.main, popup.getMenu());
                    popup.getMenu().removeItem(R.id.action_search);
                    popup.setOnMenuItemClickListener(item -> activity.onOptionsItemSelected(item));
                    popup.show();
                }
            });
        }

        if (sharedPref.getIsDarkTheme()) {
            if (toolbar != null) {
                toolbar.setBackgroundColor(ContextCompat.getColor(root.getContext(), R.color.color_dark_toolbar));
            }
        } else {
            if (toolbar != null) {
                toolbar.setBackgroundColor(ContextCompat.getColor(root.getContext(), R.color.color_light_primary));
            }
        }
    }

    private void initViews(View root) {
        swipeRefresh = root.findViewById(R.id.swipe_refresh);
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.colorAccent);
            swipeRefresh.setOnRefreshListener(this::loadAllSections);
        }

        // Unlimited RSS Feed RecyclerView
        rvCategoryNews = root.findViewById(R.id.rv_home_category_news);
        if (rvCategoryNews != null) {
            rvCategoryNews.setLayoutManager(new LinearLayoutManager(getContext()));
            categoryNewsAdapter = new AdapterNews(getContext(), new ArrayList<>());
            categoryNewsAdapter.setOnItemClickListener((v, obj, position) -> {
                if (obj == null || getContext() == null) return;
                Intent intent = new Intent(getContext(), ActivityNewsDetail.class);
                intent.putExtra("title", obj.title);
                intent.putExtra("description", obj.description);
                intent.putExtra("link", obj.link);
                intent.putExtra("imageUrl", obj.imageUrl);
                intent.putExtra("pubDate", obj.pubDate);
                intent.putExtra("sourceName", obj.sourceName);
                startActivity(intent);
            });
            rvCategoryNews.setAdapter(categoryNewsAdapter);

            rvCategoryNews.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (activity != null) {
                        activity.onScroll(dy);
                    }
                }
            });
        }

        // Setup Modern Category Tabs
        setupCategoryTabs(root);

        // Instant bind from memory cache or database
        if (sCachedNews != null && !sCachedNews.isEmpty()) {
            filterAndDisplayCategoryNews(currentSelectedCategory);
        } else {
            observeNewsLiveData();
            triggerInitialNewsSync();
        }
    }

    private void loadAllSections() {
        Context ctx = getContext();
        if (ctx == null) return;
        if (swipeRefresh != null) {
            swipeRefresh.post(() -> {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
            });
        }

        new Thread(() -> {
            List<NewsItem> cachedNews = new ArrayList<>();
            try {
                List<NewsItem> fromDb = AppDatabase.getDatabase(ctx).newsDao().getAllNewsSync();
                if (fromDb != null && !fromDb.isEmpty()) {
                    cachedNews.addAll(fromDb);
                }
            } catch (Exception ignored) {}

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded() || getView() == null) return;
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    if (!cachedNews.isEmpty()) {
                        sCachedNews = cachedNews;
                        filterAndDisplayCategoryNews(currentSelectedCategory);
                    } else {
                        observeNewsLiveData();
                    }
                    if (layoutHomeLoading != null) {
                        layoutHomeLoading.setVisibility(View.GONE);
                    }
                });
            }

            triggerInitialNewsSync();
        }).start();
    }

    private void observeNewsLiveData() {
        if (getContext() == null || !isAdded() || getView() == null) return;
        try {
            AppDatabase.getDatabase(getContext()).newsDao().getAllNews().observe(getViewLifecycleOwner(), newsItems -> {
                if (newsItems != null && !newsItems.isEmpty()) {
                    sCachedNews = newsItems;
                    filterAndDisplayCategoryNews(currentSelectedCategory);
                    if (layoutHomeLoading != null) {
                        layoutHomeLoading.setVisibility(View.GONE);
                    }
                }
            });
        } catch (Exception ignored) {}
    }

    private void setupCategoryTabs(View root) {
        allCategoryTabs.clear();
        int[] tabIds = {
                R.id.tab_home_for_you, R.id.tab_home_trending, R.id.tab_home_channels, R.id.tab_home_radio,
                R.id.tab_home_local, R.id.tab_home_weather, R.id.tab_home_politics, R.id.tab_home_business,
                R.id.tab_home_tech, R.id.tab_home_sports, R.id.tab_home_health,
                R.id.tab_home_entertainment, R.id.tab_home_world
        };
        String[] catKeys = {
                "FOR_YOU", "TRENDING", "CHANNELS", "RADIO",
                "LOCAL", "WEATHER", "POLITICS", "BUSINESS",
                "TECH", "SPORTS", "HEALTH", "ENTERTAINMENT", "WORLD"
        };

        for (int i = 0; i < tabIds.length; i++) {
            TextView tab = root.findViewById(tabIds[i]);
            if (tab != null) {
                allCategoryTabs.add(tab);
                final String catKey = catKeys[i];
                tab.setOnClickListener(v -> handleTabClick(tab, catKey));
            }
        }
    }

    private void handleTabClick(TextView selectedTab, String categoryKey) {
        if ("CHANNELS".equals(categoryKey)) {
            if (activity != null) {
                activity.loadWebPage("News Channels", "CATEGORY", "news_channels.json", "news_channels.json");
            } else if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadWebPage("News Channels", "CATEGORY", "news_channels.json", "news_channels.json");
            }
            return;
        }

        if ("RADIO".equals(categoryKey)) {
            Context ctx = getContext() != null ? getContext() : requireContext();
            startActivity(new Intent(ctx, ActivityRadioPlayer.class));
            return;
        }

        selectCategoryTab(selectedTab, categoryKey);
    }

    private void selectCategoryTab(TextView selectedTab, String categoryKey) {
        currentSelectedCategory = categoryKey;
        if (selectedTab != null) {
            selectedTab.setBackgroundResource(R.drawable.bg_home_tab_selected);
            selectedTab.setTextColor(0xFF0A2540);
        }
        for (TextView tab : allCategoryTabs) {
            if (tab != selectedTab && tab != null) {
                tab.setBackgroundResource(R.drawable.bg_home_tab_unselected);
                tab.setTextColor(0xFFFFFFFF);
            }
        }

        filterAndDisplayCategoryNews(categoryKey);
        if (rvCategoryNews != null) {
            rvCategoryNews.scrollToPosition(0);
        }
    }

    private void filterAndDisplayCategoryNews(String categoryKey) {
        List<NewsItem> all = sCachedNews;
        if (all == null || all.isEmpty()) {
            new Thread(() -> {
                if (getContext() != null) {
                    try {
                        List<NewsItem> dbItems = AppDatabase.getDatabase(getContext()).newsDao().getAllNewsSync();
                        if (dbItems != null && !dbItems.isEmpty()) {
                            sCachedNews = dbItems;
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> filterAndDisplayCategoryNews(categoryKey));
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }).start();
            return;
        }

        List<NewsItem> matched = new ArrayList<>();
        for (NewsItem item : all) {
            if (matchesCategory(item, categoryKey)) {
                matched.add(item);
            }
        }

        // STRICT CATEGORY ISOLATION: Show ONLY articles matching the respective category
        // Never pollute a specific category with unrelated stories
        if (categoryNewsAdapter != null) {
            categoryNewsAdapter.setItems(matched);
        }
    }

    private void triggerInitialNewsSync() {
        Context ctx = getContext();
        if (ctx == null) return;
        new Thread(() -> {
            try {
                InputStream is = ctx.getAssets().open("news_rss.json");
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                Type listType = new TypeToken<List<AppConfig.RssSource>>(){}.getType();
                List<AppConfig.RssSource> sources = new Gson().fromJson(reader, listType);
                if (sources != null && !sources.isEmpty()) {
                    // Group sources by category so EVERY category is represented equally
                    java.util.Map<String, List<AppConfig.RssSource>> byCat = new java.util.LinkedHashMap<>();
                    for (AppConfig.RssSource s : sources) {
                        String cat = (s.category != null && !s.category.isEmpty()) ? s.category.toUpperCase(java.util.Locale.US) : "TOP";
                        List<AppConfig.RssSource> catList = byCat.get(cat);
                        if (catList == null) {
                            catList = new ArrayList<>();
                            byCat.put(cat, catList);
                        }
                        catList.add(s);
                    }

                    // Select up to 3 sources for each category
                    List<AppConfig.RssSource> selectedSources = new ArrayList<>();
                    for (java.util.Map.Entry<String, List<AppConfig.RssSource>> entry : byCat.entrySet()) {
                        List<AppConfig.RssSource> list = entry.getValue();
                        int take = Math.min(list.size(), 3);
                        for (int i = 0; i < take; i++) {
                            selectedSources.add(list.get(i));
                        }
                    }

                    com.app.webdroid.util.RssParser parser = new com.app.webdroid.util.RssParser();
                    List<NewsItem> allFetched = Collections.synchronizedList(new ArrayList<>());
                    ExecutorService pool = Executors.newFixedThreadPool(8);
                    CountDownLatch latch = new CountDownLatch(selectedSources.size());

                    for (AppConfig.RssSource src : selectedSources) {
                        pool.execute(() -> {
                            try {
                                java.net.URL u = new java.net.URL(src.url);
                                java.net.HttpURLConnection c = (java.net.HttpURLConnection) u.openConnection();
                                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                                c.setConnectTimeout(4000);
                                c.setReadTimeout(4000);
                                if (c.getResponseCode() == 200) {
                                    List<NewsItem> parsed = parser.parseNews(c.getInputStream(), src.title, src.category);
                                    if (parsed != null && !parsed.isEmpty()) {
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
                        latch.await(6, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {}
                    pool.shutdown();

                    if (!allFetched.isEmpty() && ctx != null) {
                        AppDatabase.getDatabase(ctx).newsDao().insertNews(new ArrayList<>(allFetched));
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private boolean matchesCategory(NewsItem item, String category) {
        if (item == null) return false;
        String itemCat = item.category != null ? item.category.trim().toUpperCase(java.util.Locale.US) : "";
        String source = item.sourceName != null ? item.sourceName.toLowerCase(java.util.Locale.US) : "";
        String title = item.title != null ? item.title.toLowerCase(java.util.Locale.US) : "";
        String desc = item.description != null ? item.description.toLowerCase(java.util.Locale.US) : "";
        String combined = source + " " + title + " " + desc;

        switch (category) {
            case "FOR_YOU":
                return true;

            case "TRENDING":
                if ("TOP".equals(itemCat)) return true;
                return combined.contains("breaking") || combined.contains("alert") || combined.contains("live")
                        || combined.contains("top") || combined.contains("massive") || combined.contains("exclusive")
                        || combined.contains("urgent");

            case "SPORTS":
                if ("SPORTS".equals(itemCat)) return true;
                return source.contains("sport") || source.contains("espn")
                        || combined.contains("nfl") || combined.contains("nba") || combined.contains("mlb")
                        || combined.contains("nhl") || combined.contains("football") || combined.contains("basketball")
                        || combined.contains("baseball") || combined.contains("super bowl") || combined.contains("touchdown")
                        || combined.contains("quarterback") || combined.contains("soccer") || combined.contains("tennis")
                        || combined.contains("golf") || combined.contains("athlete") || combined.contains("olympics")
                        || combined.contains("championship");

            case "WEATHER":
                if ("WEATHER".equals(itemCat)) return true;
                return source.contains("weather") || source.contains("accuweather") || source.contains("noaa")
                        || combined.contains("weather") || combined.contains("storm") || combined.contains("hurricane")
                        || combined.contains("tornado") || combined.contains("rain") || combined.contains("flood")
                        || combined.contains("snow") || combined.contains("temperature") || combined.contains("forecast")
                        || combined.contains("radar") || combined.contains("blizzard") || combined.contains("heat wave");

            case "POLITICS":
                if ("POLITICS".equals(itemCat)) return true;
                return source.contains("politico") || source.contains("hill") || source.contains("politic")
                        || combined.contains("biden") || combined.contains("trump") || combined.contains("congress")
                        || combined.contains("senate") || combined.contains("white house") || combined.contains("house of representatives")
                        || combined.contains("democrat") || combined.contains("republican") || combined.contains("supreme court")
                        || combined.contains("election") || combined.contains("capitol") || combined.contains("governor");

            case "BUSINESS":
                if ("BUSINESS".equals(itemCat)) return true;
                return source.contains("business") || source.contains("market") || source.contains("wsj")
                        || source.contains("cnbc") || source.contains("bloomberg") || source.contains("finance")
                        || combined.contains("stock") || combined.contains("dow jones") || combined.contains("nasdaq")
                        || combined.contains("s&p 500") || combined.contains("fed") || combined.contains("inflation")
                        || combined.contains("economy") || combined.contains("revenue") || combined.contains("earnings")
                        || combined.contains("wall street") || combined.contains("interest rate");

            case "TECH":
                if ("TECH".equals(itemCat) || "SCIENCE".equals(itemCat)) return true;
                return source.contains("tech") || source.contains("verge") || source.contains("wired")
                        || source.contains("space.com")
                        || combined.contains("ai") || combined.contains("artificial intelligence") || combined.contains("apple")
                        || combined.contains("google") || combined.contains("microsoft") || combined.contains("meta")
                        || combined.contains("nvidia") || combined.contains("openai") || combined.contains("chatgpt")
                        || combined.contains("software") || combined.contains("cyber");

            case "HEALTH":
                if ("HEALTH".equals(itemCat)) return true;
                return source.contains("health") || source.contains("medical")
                        || combined.contains("fda") || combined.contains("cdc") || combined.contains("vaccine")
                        || combined.contains("doctor") || combined.contains("hospital") || combined.contains("diet")
                        || combined.contains("treatment") || combined.contains("cancer") || combined.contains("disease");

            case "ENTERTAINMENT":
                if ("ENTERTAINMENT".equals(itemCat)) return true;
                return source.contains("entertainment") || source.contains("variety") || source.contains("hollywood")
                        || combined.contains("movie") || combined.contains("actor") || combined.contains("actress")
                        || combined.contains("box office") || combined.contains("film") || combined.contains("oscar")
                        || combined.contains("celebrity") || combined.contains("music") || combined.contains("series");

            case "WORLD":
                if ("WORLD".equals(itemCat)) return true;
                return source.contains("world") || source.contains("international") || source.contains("foreign")
                        || combined.contains("ukraine") || combined.contains("russia") || combined.contains("china")
                        || combined.contains("middle east") || combined.contains("europe") || combined.contains("israel")
                        || combined.contains("nato") || combined.contains("un ") || combined.contains("global");

            case "LOCAL":
                if ("LOCAL".equals(itemCat)) return true;
                return source.contains("local") || source.contains("metro") || source.contains("state")
                        || combined.contains("police") || combined.contains("mayor") || combined.contains("county")
                        || combined.contains("sheriff") || combined.contains("city council") || combined.contains("highway");

            default:
                return true;
        }
    }
}

