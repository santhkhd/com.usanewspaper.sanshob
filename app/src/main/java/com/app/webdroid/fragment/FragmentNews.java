package com.app.webdroid.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.webdroid.activity.MainActivity;
import com.app.webdroid.adapter.AdapterNews;
import com.app.webdroid.database.AppDatabase;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.model.AppConfig;
import com.app.webdroid.model.FavoriteItem;
import com.app.webdroid.model.NewsItem;
import com.app.webdroid.util.RssParser;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shobmc.san.R;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FragmentNews extends Fragment {

    private RecyclerView recyclerView;
    private AdapterNews adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private boolean isFetched = false;

    private ChipGroup chipGroup;
    private HorizontalScrollView scrollCategoryChips;
    private List<NewsItem> allCachedNews = new ArrayList<>();
    private String selectedCategory = "ALL";
    private String initialSourceName = null;
    private List<String> passedUrls = new ArrayList<>();
    private String passedUrl = null;
    private String searchQuery = "";

    private LinearLayout layoutSearchContainer;
    private EditText editSearchNews;
    private ImageView btnClearSearch;

    private LinearLayout layoutNewsSkeleton;
    private LinearLayout layoutEmptyNews;
    private LinearLayout layoutErrorNews;

    private MainActivity activity;
    private SharedPref sharedPref;
    private Toolbar toolbar;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            activity = (MainActivity) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news, container, false);

        sharedPref = new SharedPref(requireContext());

        recyclerView = view.findViewById(R.id.recycler_view_news);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_news);
        progressBar = view.findViewById(R.id.progress_news);
        chipGroup = view.findViewById(R.id.chip_group_news_categories);
        scrollCategoryChips = view.findViewById(R.id.scroll_category_chips);

        layoutSearchContainer = view.findViewById(R.id.layout_search_container);
        editSearchNews = view.findViewById(R.id.edit_search_news);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);

        layoutNewsSkeleton = view.findViewById(R.id.layout_news_skeleton);
        layoutEmptyNews = view.findViewById(R.id.layout_empty_news);
        layoutErrorNews = view.findViewById(R.id.layout_error_news);
        MaterialButton btnEmptyRefresh = view.findViewById(R.id.btn_empty_refresh);
        MaterialButton btnErrorRetry = view.findViewById(R.id.btn_error_retry);

        if (btnEmptyRefresh != null) {
            btnEmptyRefresh.setOnClickListener(v -> syncAllUsaRss());
        }
        if (btnErrorRetry != null) {
            btnErrorRetry.setOnClickListener(v -> {
                if (layoutErrorNews != null) layoutErrorNews.setVisibility(View.GONE);
                syncAllUsaRss();
            });
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdapterNews(getContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).onScroll(dy);
                }
            }
        });

        // Click listener to open News Detail
        adapter.setOnItemClickListener((v, obj, position) -> {
            if (obj == null || getContext() == null) return;
            Intent intent = new Intent(getContext(), com.app.webdroid.activity.ActivityNewsDetail.class);
            intent.putExtra("title", obj.title);
            intent.putExtra("description", obj.description);
            intent.putExtra("link", obj.link);
            intent.putExtra("imageUrl", obj.imageUrl);
            intent.putExtra("pubDate", obj.pubDate);
            intent.putExtra("sourceName", obj.sourceName);
            startActivity(intent);
        });

        // Determine initial category and URLs from arguments if provided
        String sourceTitle = null;
        if (getArguments() != null) {
            sourceTitle = getArguments().getString("name");
            passedUrl = getArguments().getString("url");
            List<String> pUrls = getArguments().getStringArrayList("urls");
            if (pUrls != null) {
                passedUrls.addAll(pUrls);
            }
            if (passedUrl != null && !passedUrls.contains(passedUrl)) {
                passedUrls.add(0, passedUrl);
            }
        }
        initialSourceName = sourceTitle;

        if (sourceTitle != null) {
            String lower = sourceTitle.toLowerCase();
            if (lower.contains("science") || lower.contains("space") || lower.contains("nasa")) {
                selectedCategory = "SCIENCE";
            } else if (lower.contains("politic") || lower.contains("white house") || lower.contains("congress")) {
                selectedCategory = "POLITICS";
            } else if (lower.contains("top") || lower.contains("breaking") || lower.contains("headline")) {
                selectedCategory = "TOP";
            } else if (lower.contains("health") || lower.contains("medical")) {
                selectedCategory = "HEALTH";
            } else if (lower.contains("cinema") || lower.contains("entertainment") || lower.contains("hollywood")) {
                selectedCategory = "CINEMA";
            } else if (lower.contains("sports") || lower.contains("nfl") || lower.contains("nba")) {
                selectedCategory = "SPORTS";
            } else if (lower.contains("business") || lower.contains("market") || lower.contains("finance") || lower.contains("wall street")) {
                selectedCategory = "BUSINESS";
            } else if (lower.contains("world") || lower.contains("global") || lower.contains("international")) {
                selectedCategory = "WORLD";
            } else if (lower.contains("tech") || lower.contains("ai") || lower.contains("technology")) {
                selectedCategory = "TECH";
            } else {
                selectedCategory = "SPECIFIC_SOURCE";
            }
        }

        setupCategoryChips();
        setupSearchControls();

        // Observe Room DB for cached news
        AppDatabase db = AppDatabase.getDatabase(getContext());
        db.newsDao().getAllNews().observe(getViewLifecycleOwner(), newsItems -> {
            if (newsItems != null) {
                allCachedNews = new ArrayList<>(newsItems);
            }
            applyCategoryAndSearchFilter();

            if (!isFetched) {
                isFetched = true;
                if (allCachedNews.isEmpty()) {
                    showSkeletonLoading(true);
                }
                syncAllUsaRss();
            }
            swipeRefreshLayout.setRefreshing(false);
        });

        // Observe favorites for heart icon synchronization
        db.favoriteDao().getAllFavorites().observe(getViewLifecycleOwner(), favoriteItems -> {
            List<String> ids = new ArrayList<>();
            for (FavoriteItem item : favoriteItems) {
                if (FavoriteItem.TYPE_RSS.equals(item.type)) {
                    ids.add(item.itemId);
                }
            }
            adapter.setFavoriteIds(ids);
        });

        adapter.setOnFavoriteClickListener((v, obj, position) -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                if (db.favoriteDao().isFavorite(obj.link, FavoriteItem.TYPE_RSS) > 0) {
                    db.favoriteDao().removeFavorite(obj.link, FavoriteItem.TYPE_RSS);
                } else {
                    FavoriteItem fav = new FavoriteItem();
                    fav.itemId = obj.link;
                    fav.type = FavoriteItem.TYPE_RSS;
                    fav.title = obj.title;
                    fav.subtitle = obj.pubDate;
                    fav.imageUrl = obj.imageUrl;
                    fav.targetUrl = obj.link;
                    db.favoriteDao().addFavorite(fav);
                }
            });
        });

        swipeRefreshLayout.setOnRefreshListener(this::syncAllUsaRss);

        setupToolbar(view);

        return view;
    }

    private void setupSearchControls() {
        if (editSearchNews != null) {
            editSearchNews.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s != null ? s.toString().trim() : "";
                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    applyCategoryAndSearchFilter();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (editSearchNews != null) {
                    editSearchNews.setText("");
                }
            });
        }
    }

    private void toggleSearchBar() {
        if (layoutSearchContainer == null) return;
        if (layoutSearchContainer.getVisibility() == View.VISIBLE) {
            layoutSearchContainer.setVisibility(View.GONE);
            if (editSearchNews != null) {
                editSearchNews.setText("");
            }
            hideKeyboard();
        } else {
            layoutSearchContainer.setVisibility(View.VISIBLE);
            if (editSearchNews != null) {
                editSearchNews.requestFocus();
                showKeyboard(editSearchNews);
            }
        }
    }

    private void showKeyboard(View view) {
        if (getContext() == null || view == null) return;
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        if (getContext() == null || getView() == null) return;
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    private void setupCategoryChips() {
        if (chipGroup == null) return;

        int targetChipId = R.id.chip_all;
        if ("TOP".equals(selectedCategory)) targetChipId = R.id.chip_top;
        else if ("POLITICS".equals(selectedCategory)) targetChipId = R.id.chip_politics;
        else if ("BUSINESS".equals(selectedCategory)) targetChipId = R.id.chip_business;
        else if ("TECH".equals(selectedCategory)) targetChipId = R.id.chip_tech;
        else if ("WORLD".equals(selectedCategory)) targetChipId = R.id.chip_world;
        else if ("SPORTS".equals(selectedCategory)) targetChipId = R.id.chip_sports;
        else if ("CINEMA".equals(selectedCategory) || "ENTERTAINMENT".equals(selectedCategory)) targetChipId = R.id.chip_cinema;
        else if ("SCIENCE".equals(selectedCategory)) targetChipId = R.id.chip_science;
        else if ("HEALTH".equals(selectedCategory)) targetChipId = R.id.chip_health;

        chipGroup.check(targetChipId);

        final int scrollTargetId = targetChipId;
        if (scrollCategoryChips != null && targetChipId != R.id.chip_all) {
            scrollCategoryChips.post(() -> {
                View chip = scrollCategoryChips.findViewById(scrollTargetId);
                if (chip != null) {
                    scrollCategoryChips.smoothScrollTo(chip.getLeft() - 40, 0);
                }
            });
        }

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_top) {
                selectedCategory = "TOP";
            } else if (checkedId == R.id.chip_politics) {
                selectedCategory = "POLITICS";
            } else if (checkedId == R.id.chip_business) {
                selectedCategory = "BUSINESS";
            } else if (checkedId == R.id.chip_tech) {
                selectedCategory = "TECH";
            } else if (checkedId == R.id.chip_world) {
                selectedCategory = "WORLD";
            } else if (checkedId == R.id.chip_sports) {
                selectedCategory = "SPORTS";
            } else if (checkedId == R.id.chip_cinema) {
                selectedCategory = "CINEMA";
            } else if (checkedId == R.id.chip_science) {
                selectedCategory = "SCIENCE";
            } else if (checkedId == R.id.chip_health) {
                selectedCategory = "HEALTH";
            } else {
                selectedCategory = "ALL";
            }

            if (scrollCategoryChips != null && checkedId != View.NO_ID) {
                View chip = scrollCategoryChips.findViewById(checkedId);
                if (chip != null) {
                    scrollCategoryChips.smoothScrollTo(chip.getLeft() - 40, 0);
                }
            }

            applyCategoryAndSearchFilter();
        });
    }

    private void applyCategoryAndSearchFilter() {
        if (allCachedNews == null || adapter == null) return;

        List<NewsItem> categoryFiltered = new ArrayList<>();

        if (!passedUrls.isEmpty()) {
            for (NewsItem item : allCachedNews) {
                boolean matchSource = initialSourceName != null && (initialSourceName.equalsIgnoreCase(item.sourceName)
                        || (item.sourceName != null && item.sourceName.toLowerCase().contains(initialSourceName.toLowerCase())));
                boolean matchUrl = false;
                if (item.sourceUrl != null) {
                    for (String u : passedUrls) {
                        if (u.equalsIgnoreCase(item.sourceUrl)) {
                            matchUrl = true;
                            break;
                        }
                    }
                }
                if (matchSource || matchUrl) {
                    categoryFiltered.add(item);
                }
            }
            if (categoryFiltered.isEmpty()) {
                for (NewsItem item : allCachedNews) {
                    if (matchesCategory(item, selectedCategory)) {
                        categoryFiltered.add(item);
                    }
                }
            }
        } else if ("ALL".equalsIgnoreCase(selectedCategory)) {
            categoryFiltered.addAll(allCachedNews);
        } else if ("SPECIFIC_SOURCE".equalsIgnoreCase(selectedCategory) && initialSourceName != null) {
            for (NewsItem item : allCachedNews) {
                if (initialSourceName.equalsIgnoreCase(item.sourceName)
                        || (item.sourceName != null && item.sourceName.toLowerCase().contains(initialSourceName.toLowerCase()))) {
                    categoryFiltered.add(item);
                }
            }
        } else {
            for (NewsItem item : allCachedNews) {
                if (matchesCategory(item, selectedCategory)) {
                    categoryFiltered.add(item);
                }
            }
        }

        List<NewsItem> finalFiltered = new ArrayList<>();
        if (searchQuery != null && !searchQuery.isEmpty()) {
            String q = searchQuery.toLowerCase();
            for (NewsItem item : categoryFiltered) {
                boolean matchTitle = item.title != null && item.title.toLowerCase().contains(q);
                boolean matchDesc = item.description != null && item.description.toLowerCase().contains(q);
                boolean matchSource = item.sourceName != null && item.sourceName.toLowerCase().contains(q);
                if (matchTitle || matchDesc || matchSource) {
                    finalFiltered.add(item);
                }
            }
        } else {
            finalFiltered.addAll(categoryFiltered);
        }

        showSkeletonLoading(false);

        if (finalFiltered.isEmpty()) {
            if (layoutEmptyNews != null) layoutEmptyNews.setVisibility(View.VISIBLE);
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        } else {
            if (layoutEmptyNews != null) layoutEmptyNews.setVisibility(View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        }

        List<NewsItem> withAds = injectNativeAds(finalFiltered);
        adapter.setItems(withAds);
        if (recyclerView != null) {
            recyclerView.scrollToPosition(0);
        }
    }

    private void showSkeletonLoading(boolean show) {
        if (layoutNewsSkeleton != null) {
            layoutNewsSkeleton.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                layoutNewsSkeleton.animate().alpha(0.6f).setDuration(600).withEndAction(
                        () -> layoutNewsSkeleton.animate().alpha(1.0f).setDuration(600).start()
                ).start();
            }
        }
        if (recyclerView != null && show) {
            recyclerView.setVisibility(View.GONE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private boolean matchesCategory(NewsItem item, String category) {
        if (item == null) return false;
        String itemCat = item.category != null ? item.category.trim().toUpperCase(java.util.Locale.US) : "";
        if (!itemCat.isEmpty() && itemCat.equalsIgnoreCase(category)) {
            return true;
        }
        String source = item.sourceName != null ? item.sourceName.toLowerCase(java.util.Locale.US) : "";
        String title = item.title != null ? item.title.toLowerCase(java.util.Locale.US) : "";
        String desc = item.description != null ? item.description.toLowerCase(java.util.Locale.US) : "";
        String combined = source + " " + title + " " + desc;

        switch (category) {
            case "TOP":
                return "TOP".equals(itemCat) || source.contains("top") || source.contains("latest") || source.contains("breaking") || source.contains("headline")
                        || title.contains("breaking") || title.contains("live") || title.contains("alert");
            case "POLITICS":
                return "POLITICS".equals(itemCat) || source.contains("politico") || source.contains("hill") || source.contains("politic")
                        || combined.contains("biden") || combined.contains("trump") || combined.contains("congress")
                        || combined.contains("senate") || combined.contains("house") || combined.contains("white house")
                        || combined.contains("democrat") || combined.contains("republican") || combined.contains("supreme court")
                        || combined.contains("election") || combined.contains("capitol") || combined.contains("governor");
            case "BUSINESS":
                return "BUSINESS".equals(itemCat) || source.contains("business") || source.contains("market") || source.contains("wsj")
                        || source.contains("cnbc") || source.contains("bloomberg") || source.contains("finance")
                        || combined.contains("stock") || combined.contains("dow") || combined.contains("nasdaq")
                        || combined.contains("fed") || combined.contains("inflation") || combined.contains("economy")
                        || combined.contains("market") || combined.contains("revenue") || combined.contains("earnings");
            case "TECH":
                return "TECH".equals(itemCat) || source.contains("tech") || source.contains("verge") || source.contains("wired")
                        || combined.contains("ai") || combined.contains("apple") || combined.contains("google")
                        || combined.contains("microsoft") || combined.contains("meta") || combined.contains("nvidia")
                        || combined.contains("openai") || combined.contains("software") || combined.contains("cyber");
            case "WORLD":
                return "WORLD".equals(itemCat) || source.contains("world") || source.contains("international") || source.contains("foreign")
                        || combined.contains("ukraine") || combined.contains("russia") || combined.contains("china")
                        || combined.contains("middle east") || combined.contains("europe") || combined.contains("israel")
                        || combined.contains("nato") || combined.contains("un ") || combined.contains("global");
            case "SPORTS":
                return "SPORTS".equals(itemCat) || source.contains("sport") || source.contains("espn") || combined.contains("nfl")
                        || combined.contains("nba") || combined.contains("mlb") || combined.contains("nhl")
                        || combined.contains("football") || combined.contains("basketball") || combined.contains("baseball")
                        || combined.contains("super bowl") || combined.contains("championship");
            case "CINEMA":
            case "ENTERTAINMENT":
                return "ENTERTAINMENT".equals(itemCat) || "CINEMA".equals(itemCat) || source.contains("entertainment") || source.contains("variety") || source.contains("hollywood")
                        || source.contains("movie") || combined.contains("actor") || combined.contains("box office")
                        || combined.contains("film") || combined.contains("oscar") || combined.contains("celebrity")
                        || combined.contains("music") || combined.contains("series");
            case "SCIENCE":
                return "SCIENCE".equals(itemCat) || source.contains("science") || source.contains("space") || source.contains("nasa")
                        || combined.contains("astronomy") || combined.contains("planet") || combined.contains("telescope")
                        || combined.contains("climate") || combined.contains("spacex") || combined.contains("mars");
            case "HEALTH":
                return "HEALTH".equals(itemCat) || source.contains("health") || source.contains("medical") || combined.contains("fda")
                        || combined.contains("cdc") || combined.contains("vaccine") || combined.contains("doctor")
                        || combined.contains("hospital") || combined.contains("diet") || combined.contains("treatment");
            default:
                return false;
        }
    }

    private void setupToolbar(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        if (toolbar == null) return;

        toolbar.setTitle("News");
        boolean isDark = sharedPref.getIsDarkTheme();
        toolbar.setTitleTextColor(isDark ? 0xFFFFFFFF : 0xFF0F172A);

        if (activity != null) {
            activity.setSupportActionBar(toolbar);
            if (sharedPref.getNavigationDrawer()) {
                toolbar.setNavigationIcon(R.drawable.ic_menu);
                if (toolbar.getNavigationIcon() != null) {
                    toolbar.getNavigationIcon().setTint(isDark ? 0xFFFFFFFF : 0xFF0F172A);
                }
                toolbar.setNavigationOnClickListener(v -> activity.openDrawer());
            } else {
                if (activity.getSupportActionBar() != null) {
                    activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            }
        }

        toolbar.setVisibility(sharedPref.getToolbar() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_news, menu);
        boolean isDark = sharedPref != null && sharedPref.getIsDarkTheme();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getIcon() != null) {
                item.getIcon().setTint(isDark ? 0xFFFFFFFF : 0xFF0F172A);
            }
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_news_search) {
            toggleSearchBar();
            return true;
        } else if (id == R.id.action_news_refresh) {
            syncAllUsaRss();
            return true;
        } else if (id == R.id.action_news_saved) {
            if (activity != null) {
                activity.loadWebPage("Saved", "FAVORITES", null, null);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void syncAllUsaRss() {
        if (getContext() == null) return;
        Context appContext = getContext().getApplicationContext();
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        new Thread(() -> {
            List<AppConfig.RssSource> sources = new ArrayList<>();
            if (!passedUrls.isEmpty()) {
                for (String u : passedUrls) {
                    AppConfig.RssSource src = new AppConfig.RssSource();
                    src.title = initialSourceName != null ? initialSourceName : "News Feed";
                    src.url = u;
                    src.category = selectedCategory;
                    sources.add(src);
                }
            } else {
                sources = getUsaRssSources(appContext);
            }

            if (sources == null || sources.isEmpty()) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        showSkeletonLoading(false);
                    });
                }
                return;
            }

            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<Future<?>> futures = new ArrayList<>();
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(6, TimeUnit.SECONDS)
                    .readTimeout(6, TimeUnit.SECONDS)
                    .connectionPool(new ConnectionPool(15, 5, TimeUnit.MINUTES))
                    .build();
            RssParser parser = new RssParser();

            for (AppConfig.RssSource source : sources) {
                futures.add(executor.submit(() -> {
                    try {
                        Request request = new Request.Builder()
                                .url(source.url)
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                                .build();
                        Response response = client.newCall(request).execute();
                        if (response.isSuccessful() && response.body() != null) {
                            InputStream stream = response.body().byteStream();
                            List<NewsItem> items = parser.parseNews(stream, source.title, source.category);
                            if (items != null && !items.isEmpty()) {
                                AppDatabase.getDatabase(appContext).newsDao().insertNews(items);
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("FragmentNews", "Failed to sync RSS: " + source.url, e);
                    }
                }));
            }

            for (Future<?> f : futures) {
                try {
                    f.get(8, TimeUnit.SECONDS);
                } catch (Exception ignored) {}
            }
            executor.shutdown();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    showSkeletonLoading(false);
                });
            }
        }).start();
    }

    private List<AppConfig.RssSource> getUsaRssSources(Context appContext) {
        List<AppConfig.RssSource> sources = new ArrayList<>();
        try {
            InputStream is = appContext.getAssets().open("news_rss.json");
            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<AppConfig.RssSource>>(){}.getType();
            List<AppConfig.RssSource> loaded = new Gson().fromJson(reader, listType);
            if (loaded != null && !loaded.isEmpty()) {
                sources.addAll(loaded);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (sources.isEmpty()) {
            String[][] fallback = {
                    {"The New York Times", "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml"},
                    {"The Washington Post", "https://feeds.washingtonpost.com/rss/national"},
                    {"The Wall Street Journal", "https://feeds.a.dj.com/rss/WSJcomUSBusiness.xml"},
                    {"USA TODAY", "http://rssfeeds.usatoday.com/usatoday-NewsTopStories"},
                    {"Fox News", "https://moxie.foxnews.com/google-publisher/latest.xml"},
                    {"CNN Top Stories", "http://rss.cnn.com/rss/cnn_topstories.rss"},
                    {"Politico", "https://rss.politico.com/politics-news.xml"},
                    {"The Hill", "https://thehill.com/feed/"},
                    {"NPR News", "https://feeds.npr.org/1001/rss.xml"},
                    {"ABC News", "https://abcnews.go.com/abcnews/topstories"},
                    {"CBS News", "https://www.cbsnews.com/latest/rss/main"},
                    {"NBC News", "https://feeds.nbcnews.com/nbcnews/public/news"}
            };
            for (String[] pair : fallback) {
                AppConfig.RssSource s = new AppConfig.RssSource();
                s.title = pair[0];
                s.url = pair[1];
                sources.add(s);
            }
        }
        return sources;
    }

    private List<NewsItem> injectNativeAds(List<NewsItem> items) {
        if (getContext() == null || items == null)
            return items;

        com.app.webdroid.database.prefs.AdsPref adsPref = new com.app.webdroid.database.prefs.AdsPref(getContext());
        if (adsPref.getNativeAdIndex() <= 0)
            return items;
        if (!adsPref.getAdStatus())
            return items;

        int interval = adsPref.getNativeAdIndex();
        List<NewsItem> newItems = new ArrayList<>();
        int count = 0;
        for (NewsItem item : items) {
            if (item.isNativeAd)
                continue;
            newItems.add(item);
            count++;
            if (count % interval == 0) {
                NewsItem adItem = new NewsItem();
                adItem.isNativeAd = true;
                newItems.add(adItem);
            }
        }
        return newItems;
    }
}
