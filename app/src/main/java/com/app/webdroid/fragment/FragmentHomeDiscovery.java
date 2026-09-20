package com.app.webdroid.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shobmc.san.R;
import com.app.webdroid.activity.ActivityMovieDetail;
import com.app.webdroid.activity.ActivityNewsDetail;
import com.app.webdroid.activity.ActivityRadioPlayer;
import com.app.webdroid.activity.ActivityVideoDetail;
import com.app.webdroid.activity.MainActivity;
import com.app.webdroid.adapter.AdapterHomeDiscovery;
import com.app.webdroid.adapter.AdapterNews;
import com.app.webdroid.database.AppDatabase;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.model.AppConfig;
import com.app.webdroid.model.NewsItem;
import com.app.webdroid.model.RadioStation;
import com.app.webdroid.model.YouTubeItem;
import com.app.webdroid.util.CustomChannelManager;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FragmentHomeDiscovery extends Fragment {

    private MainActivity activity;
    private SharedPref sharedPref;

    private SwipeRefreshLayout swipeRefresh;
    private Toolbar toolbar;
    private TextView toolbarTitle;
    private ImageView btnSearch;

    // Multi-Category News Tabs & Reference UI Views
    private AdapterNews categoryNewsAdapter;
    private RecyclerView rvCategoryNews;
    private View cardHeroBreakingNews;
    private ImageView imgHeroNews;
    private TextView badgeHeroTag;
    private TextView tvHeroTitle;
    private TextView tvHeroSource;
    private ImageView btnHeroShare;
    private TextView tvLocationCity;
    private TextView tvLocationDate;
    private TextView badgeActiveTab;
    private String currentSelectedCategory = "FOR_YOU";
    private final List<TextView> allCategoryTabs = new ArrayList<>();

    // Featured Adapters
    private AdapterHomeDiscovery.TvChannelsAdapter tvAdapter;
    private AdapterHomeDiscovery.NewsDiscoveryAdapter newsAdapter;
    private AdapterHomeDiscovery.MoviesDiscoveryAdapter moviesAdapter;
    private AdapterHomeDiscovery.RadioDiscoveryAdapter radioAdapter;

    // Dynamic Category Sections Container
    private LinearLayout containerCategorySections;
    private View layoutHomeLoading;
    private final RecyclerView.RecycledViewPool categoryRecycledPool = new RecyclerView.RecycledViewPool();
    private int lastUserCategoryCount = -1;
    private int lastCustomChannelsCount = -1;

    // Model for category section data
    private static class CategoryDef {
        final String title;
        final String jsonUrl;
        final int iconRes;

        CategoryDef(String title, String jsonUrl, int iconRes) {
            this.title = title;
            this.jsonUrl = jsonUrl;
            this.iconRes = iconRes;
        }
    }

    public static class CategorySectionData {
        public String title;
        public String jsonUrl;
        public int iconRes;
        public boolean isUserCategory;
        public List<AppConfig.OverviewItem> items = new ArrayList<>();
    }

    // In-memory static cache for instant launch rendering
    private static List<AppConfig.OverviewItem> sCachedTv = null;
    private static List<AppConfig.OverviewItem> sCachedMovies = null;
    private static List<RadioStation> sCachedRadio = null;
    private static List<NewsItem> sCachedNews = null;
    private static List<CategorySectionData> sCachedCategories = null;

    // Built-in categories definitions with short titles
    private static final List<CategoryDef> BUILT_IN_CATEGORIES = new ArrayList<>();
    static {
        BUILT_IN_CATEGORIES.add(new CategoryDef("Newspapers by State", "usa_states_newspapers.json", R.drawable.ic_newspaper));
        BUILT_IN_CATEGORIES.add(new CategoryDef("News by Category", "categories.json", R.drawable.ic_grid_view));
    }

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

        View containerHome = root.findViewById(R.id.container_home_content);
        layoutHomeLoading = root.findViewById(R.id.layout_home_loading);
        if (sCachedTv == null) {
            if (containerHome != null) containerHome.setAlpha(0f);
            if (layoutHomeLoading != null) layoutHomeLoading.setVisibility(View.VISIBLE);
        } else {
            if (containerHome != null) containerHome.setAlpha(1f);
            if (layoutHomeLoading != null) layoutHomeLoading.setVisibility(View.GONE);
        }

        setupToolbar(root);
        initViews(root);
        loadAllSections();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAndRefreshUserCategories();
    }

    private void checkAndRefreshUserCategories() {
        if (getContext() == null || getActivity() == null) return;
        Context ctx = getContext();
        List<CustomChannelManager.CategoryOption> userCats = CustomChannelManager.getUserCategories(ctx);
        int currentCatCount = userCats != null ? userCats.size() : 0;
        int currentCustomChCount = CustomChannelManager.getAllCustomChannels(ctx).size();

        if (lastUserCategoryCount != -1 && (currentCatCount != lastUserCategoryCount || currentCustomChCount != lastCustomChannelsCount)) {
            // User category or custom channel lineup changed, reload sections smoothly
            loadAllSections();
        }
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

        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                if (activity != null) {
                    activity.loadWebPage("Search News", "VIDEOS", "search:USA News Breaking|CAM%3D", "search:USA News Breaking|CAM%3D");
                }
            });
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
        swipeRefresh.setColorSchemeResources(R.color.colorAccent);
        swipeRefresh.setOnRefreshListener(this::loadAllSections);

        // 0. Bind Live Weather & Financial Market Ticker
        com.app.webdroid.util.WeatherTickerManager.bind(root, requireContext());

        // US News Hub & Search Toolbar Buttons
        View btnUsNewsHub = root.findViewById(R.id.btn_home_us_news_hub);
        if (btnUsNewsHub != null) {
            btnUsNewsHub.setOnClickListener(v -> com.app.webdroid.news.ui.ActivityUsNews.start(requireContext()));
        }
        View btnSearch = root.findViewById(R.id.btn_home_search);
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> com.app.webdroid.news.ui.ActivityUsNews.start(requireContext()));
        }

        // Category Tabs & Location Header
        setupCategoryTabs(root);
        tvLocationCity = root.findViewById(R.id.tv_home_location_city);
        tvLocationDate = root.findViewById(R.id.tv_home_location_date);
        badgeActiveTab = root.findViewById(R.id.badge_home_active_tab);
        updateLocationAndDate();

        // Hero Breaking News Card
        cardHeroBreakingNews = root.findViewById(R.id.card_hero_breaking_news);
        imgHeroNews = root.findViewById(R.id.img_hero_news);
        badgeHeroTag = root.findViewById(R.id.badge_hero_tag);
        tvHeroTitle = root.findViewById(R.id.tv_hero_title);
        tvHeroSource = root.findViewById(R.id.tv_hero_source);
        btnHeroShare = root.findViewById(R.id.btn_hero_share);

        // Category Feed RecyclerView
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
        }

        // AI News Research & Intelligence Hub
        View cardAiPrompts = root.findViewById(R.id.card_home_ai_prompts);
        if (cardAiPrompts != null) {
            cardAiPrompts.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.app.webdroid.activity.ActivityAiPrompts.class);
                startActivity(intent);
            });
        }

        androidx.core.widget.NestedScrollView nestedScrollView = root.findViewById(R.id.nested_scroll_view);
        if (nestedScrollView != null) {
            nestedScrollView.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (activity != null) {
                    activity.onScroll(scrollY - oldScrollY);
                }
            });
        }

        containerCategorySections = root.findViewById(R.id.container_category_sections);

        // 1. Live US News Channels
        setupSectionHeader(root.findViewById(R.id.header_tv), R.drawable.ic_live_tv,
                "Live News Channels", () -> {
                    if (activity != null) {
                        activity.loadWebPage("News Channels", "CATEGORY", "news_channels.json", "news_channels.json");
                    }
                });
        RecyclerView rvTv = root.findViewById(R.id.rv_tv);
        rvTv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        tvAdapter = new AdapterHomeDiscovery.TvChannelsAdapter(getContext());
        tvAdapter.setOnItemClickListener(this::handleCategoryItemClick);
        rvTv.setAdapter(tvAdapter);

        // 2. Featured USA Newspapers
        setupSectionHeader(root.findViewById(R.id.header_movies), R.drawable.ic_newspaper,
                "Featured USA Newspapers", () -> {
                    if (activity != null) {
                        activity.loadWebPage("National Newspapers", "CATEGORY", "usa_national_newspapers.json", "usa_national_newspapers.json");
                    }
                });
        RecyclerView rvMovies = root.findViewById(R.id.rv_movies);
        rvMovies.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        moviesAdapter = new AdapterHomeDiscovery.MoviesDiscoveryAdapter(getContext());
        moviesAdapter.setOnItemClickListener(item -> {
            if (activity != null && item != null) {
                if (item.arguments != null && !item.arguments.isEmpty()) {
                    String targetArg = item.arguments.get(0);
                    if ("web".equalsIgnoreCase(item.provider) || targetArg.startsWith("http")) {
                        activity.loadWebPage(item.title != null ? item.title : "Newspaper", "WEB", targetArg, targetArg);
                    } else if ("assets".equalsIgnoreCase(item.provider) && targetArg.endsWith(".html")) {
                        activity.loadWebPage(item.title != null ? item.title : "Newspaper", "WEBVIEW", targetArg, targetArg);
                    } else {
                        activity.loadWebPage(item.title != null ? item.title : "Newspapers", "CATEGORY", targetArg, targetArg);
                    }
                }
            }
        });
        rvMovies.setAdapter(moviesAdapter);

        // 3. Latest News
        setupSectionHeader(root.findViewById(R.id.header_news), R.drawable.ic_newspaper,
                "Breaking News (RSS)", () -> {
                    if (activity != null) {
                        activity.loadWebPage("News Feed", "RSS", "ALL_NEWS", "ALL_NEWS");
                    }
                });
        RecyclerView rvNews = root.findViewById(R.id.rv_news);
        rvNews.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        newsAdapter = new AdapterHomeDiscovery.NewsDiscoveryAdapter(getContext());
        rvNews.setAdapter(newsAdapter);

        // 4. Radio
        setupSectionHeader(root.findViewById(R.id.header_radio), R.drawable.ic_radio,
                "US News & Talk Radio", () -> {
                    if (getContext() != null) {
                        startActivity(new Intent(getContext(), ActivityRadioPlayer.class));
                    }
                });
        RecyclerView rvRadio = root.findViewById(R.id.rv_radio);
        rvRadio.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        radioAdapter = new AdapterHomeDiscovery.RadioDiscoveryAdapter(getContext());
        rvRadio.setAdapter(radioAdapter);

        // Instant bind from in-memory cache
        if (sCachedTv != null && !sCachedTv.isEmpty()) {
            tvAdapter.setItems(sCachedTv);
        }
        if (sCachedMovies != null && !sCachedMovies.isEmpty()) {
            moviesAdapter.setItems(sCachedMovies);
        }
        if (sCachedNews != null && !sCachedNews.isEmpty()) {
            newsAdapter.setItems(sCachedNews);
            updateLocationAndDate();
            filterAndDisplayCategoryNews(currentSelectedCategory, getBadgeLabelForCategory(currentSelectedCategory));
        } else {
            observeNewsLiveData();
            triggerInitialNewsSync();
        }
        if (sCachedRadio != null && !sCachedRadio.isEmpty()) {
            radioAdapter.setItems(sCachedRadio);
        }
        if (sCachedCategories != null && !sCachedCategories.isEmpty()) {
            renderCategorySections(sCachedCategories);
        }
    }

    private void setupSectionHeader(View headerView, int iconRes, String title, Runnable onSeeAll) {
        if (headerView == null) return;
        ImageView icon = headerView.findViewById(R.id.img_section_icon);
        TextView tvTitle = headerView.findViewById(R.id.tv_section_title);
        TextView btnSeeAll = headerView.findViewById(R.id.btn_see_all);

        if (icon != null) icon.setImageResource(iconRes);
        if (tvTitle != null) tvTitle.setText(title);
        if (btnSeeAll != null) {
            btnSeeAll.setOnClickListener(v -> {
                if (onSeeAll != null) onSeeAll.run();
            });
        }
    }

    private void loadAllSections() {
        if (getContext() == null) return;
        sCachedTv = null;
        sCachedMovies = null;
        sCachedRadio = null;
        sCachedNews = null;
        sCachedCategories = null;
        if (swipeRefresh != null) {
            swipeRefresh.post(() -> {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
            });
        }

        if (getView() != null && getContext() != null) {
            com.app.webdroid.util.WeatherTickerManager.refreshLiveMetrics(getContext(), getView());
        }

        new Thread(() -> {
            // 1. Load US News Channels
            List<AppConfig.OverviewItem> tvList = loadOverviewAsset("news_channels.json");

            // 2. Load Featured USA Newspapers
            List<AppConfig.OverviewItem> moviesList = loadOverviewAsset("usa_national_newspapers.json");

            // Preload top images asynchronously without stalling section generation
            Context ctx = getContext();
            if (ctx != null) {
                final Context appCtx = ctx.getApplicationContext();
                new Thread(() -> {
                    try {
                        for (AppConfig.OverviewItem it : tvList) {
                            if (it.image != null && !it.image.isEmpty()) {
                                com.bumptech.glide.Glide.with(appCtx)
                                        .load(it.image)
                                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                                        .preload();
                            }
                        }
                        for (AppConfig.OverviewItem it : moviesList) {
                            if (it.image != null && !it.image.isEmpty()) {
                                com.bumptech.glide.Glide.with(appCtx)
                                        .load(it.image)
                                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                                        .preload();
                            }
                        }
                    } catch (Exception ignored) {}
                }).start();
            }

            // 3. Load Radio
            List<RadioStation> radioList = loadRadioAsset();

            // 6. News from Database
            List<NewsItem> cachedNews = new ArrayList<>();
            if (ctx != null) {
                try {
                    List<NewsItem> fromDb = AppDatabase.getDatabase(ctx).newsDao().getAllNewsSync();
                    if (fromDb != null && !fromDb.isEmpty()) {
                        cachedNews.addAll(fromDb);
                    }
                } catch (Exception ignored) {
                }
            }

            // 7. Load Dynamic Category Sections (Built-in Categories + User-Created Categories)
            List<CategorySectionData> categorySections = new ArrayList<>();

            // A. Built-in categories
            for (CategoryDef cat : BUILT_IN_CATEGORIES) {
                CategorySectionData sec = new CategorySectionData();
                sec.title = cat.title;
                sec.jsonUrl = cat.jsonUrl;
                sec.iconRes = cat.iconRes;
                sec.isUserCategory = false;

                List<AppConfig.OverviewItem> list = new ArrayList<>();

                // Add any user-added custom channels to this category first
                if (ctx != null) {
                    List<AppConfig.OverviewItem> customList = CustomChannelManager.getCustomChannels(ctx, cat.jsonUrl);
                    if (customList != null) {
                        for (AppConfig.OverviewItem it : customList) {
                            if (!CustomChannelManager.isChannelHidden(ctx, cat.jsonUrl, it)) {
                                list.add(it);
                            }
                        }
                    }
                }

                // Add built-in channels
                List<AppConfig.OverviewItem> assetItems = loadOverviewAsset(cat.jsonUrl);
                if (assetItems != null) {
                    for (AppConfig.OverviewItem it : assetItems) {
                        if (ctx != null && CustomChannelManager.isChannelHidden(ctx, cat.jsonUrl, it)) {
                            continue;
                        }
                        boolean exists = false;
                        for (AppConfig.OverviewItem ex : list) {
                            if (ex.title != null && ex.title.equalsIgnoreCase(it.title)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            list.add(it);
                        }
                    }
                }

                if (!list.isEmpty()) {
                    sec.items = new ArrayList<>(list.subList(0, Math.min(list.size(), 52)));
                    categorySections.add(sec);
                }
            }

            // B. User-created categories (ADDED AT THE BOTTOM OF THE HOME PAGE)
            if (ctx != null) {
                List<CustomChannelManager.CategoryOption> userCats = CustomChannelManager.getUserCategories(ctx);
                lastUserCategoryCount = userCats != null ? userCats.size() : 0;
                lastCustomChannelsCount = CustomChannelManager.getAllCustomChannels(ctx).size();

                if (userCats != null) {
                    for (CustomChannelManager.CategoryOption uCat : userCats) {
                        CategorySectionData sec = new CategorySectionData();
                        sec.title = uCat.title;
                        sec.jsonUrl = uCat.jsonUrl;
                        sec.iconRes = R.drawable.ic_bookmark;
                        sec.isUserCategory = true;

                        List<AppConfig.OverviewItem> uItems = new ArrayList<>();
                        List<AppConfig.OverviewItem> customList = CustomChannelManager.getCustomChannels(ctx, uCat.jsonUrl);
                        if (customList != null) {
                            for (AppConfig.OverviewItem it : customList) {
                                if (!CustomChannelManager.isChannelHidden(ctx, uCat.jsonUrl, it)) {
                                    uItems.add(it);
                                }
                            }
                        }

                        if (uItems.isEmpty()) {
                            // Provide "+ Add Channels" action card so user can populate the new category directly!
                            AppConfig.OverviewItem addPlaceholder = new AppConfig.OverviewItem();
                            addPlaceholder.title = "+ Add Channels";
                            addPlaceholder.provider = "add_channel_action";
                            uItems.add(addPlaceholder);
                        } else {
                            // Append add more channels card at the end of the horizontal row
                            AppConfig.OverviewItem addMore = new AppConfig.OverviewItem();
                            addMore.title = "+ Add More";
                            addMore.provider = "add_channel_action";
                            uItems.add(addMore);
                        }

                        sec.items = uItems;
                        categorySections.add(sec);
                    }
                }
            }

            final List<AppConfig.OverviewItem> finalTv = tvList;
            final List<AppConfig.OverviewItem> finalMovies = moviesList;
            final List<RadioStation> finalRadio = radioList;
            final List<NewsItem> finalNews = cachedNews;
            final List<CategorySectionData> finalCategories = categorySections;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded() || getView() == null) return;
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                    sCachedTv = finalTv;
                    sCachedMovies = finalMovies;
                    sCachedRadio = finalRadio;
                    sCachedNews = finalNews;
                    sCachedCategories = finalCategories;

                    if (tvAdapter != null && !finalTv.isEmpty()) {
                        tvAdapter.setItems(finalTv);
                    }

                    if (moviesAdapter != null && !finalMovies.isEmpty()) {
                        moviesAdapter.setItems(finalMovies);
                    }

                    if (radioAdapter != null && !finalRadio.isEmpty()) {
                        radioAdapter.setItems(finalRadio.subList(0, Math.min(finalRadio.size(), 15)));
                    }

                    if (newsAdapter != null && !finalNews.isEmpty()) {
                        newsAdapter.setItems(finalNews.subList(0, Math.min(finalNews.size(), 10)));
                    } else {
                        observeNewsLiveData();
                    }

                    updateLocationAndDate();
                    filterAndDisplayCategoryNews(currentSelectedCategory, getBadgeLabelForCategory(currentSelectedCategory));

                    // Render dynamic categories with horizontal scrolling cards
                    renderCategorySections(finalCategories);

                    if (layoutHomeLoading != null) {
                        layoutHomeLoading.setVisibility(View.GONE);
                    }

                    View containerHome = getView() != null ? getView().findViewById(R.id.container_home_content) : null;
                    if (containerHome != null && containerHome.getAlpha() < 1f) {
                        containerHome.animate().alpha(1f).setDuration(250).start();
                    }
                });
            }
        }).start();
    }

    private void renderCategorySections(List<CategorySectionData> sections) {
        if (containerCategorySections == null || getContext() == null) return;
        containerCategorySections.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (CategorySectionData sec : sections) {
            View sectionView = inflater.inflate(R.layout.item_home_category_section, containerCategorySections, false);

            // 1. Setup Section Header (Icon, Title, "See all →")
            View headerView = sectionView.findViewById(R.id.section_header);
            setupSectionHeader(headerView, sec.iconRes, sec.title, () -> {
                if (activity != null) {
                    activity.loadWebPage(sec.title, "CATEGORY", sec.jsonUrl, sec.jsonUrl);
                }
            });

            // 2. Setup Horizontal RecyclerView for category cards
            RecyclerView rv = sectionView.findViewById(R.id.rv_category_items);
            rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            rv.setRecycledViewPool(categoryRecycledPool);
            rv.setHasFixedSize(true);
            rv.setNestedScrollingEnabled(false);

            AdapterHomeDiscovery.CategoryChannelsAdapter adapter = new AdapterHomeDiscovery.CategoryChannelsAdapter(
                    getContext(),
                    sec.jsonUrl,
                    sec.title,
                    new AdapterHomeDiscovery.CategoryChannelsAdapter.OnItemClickListener() {
                        @Override
                        public void onItemClick(AppConfig.OverviewItem item) {
                            handleCategoryItemClick(item);
                        }

                        @Override
                        public void onAddChannelClick(String categoryJsonUrl, String categoryTitle) {
                            if (activity != null) {
                                activity.loadWebPage("Add Channel / Playlist", "ADD_CHANNEL", categoryJsonUrl, categoryJsonUrl);
                            }
                        }
                    }
            );
            adapter.setItems(sec.items);
            rv.setAdapter(adapter);

            containerCategorySections.addView(sectionView);
        }
    }

    private void handleCategoryItemClick(AppConfig.OverviewItem obj) {
        if (obj == null || getContext() == null) return;

        // Check if it's a Movie item
        boolean isMovieItem = ("movies".equalsIgnoreCase(obj.provider)
                || ((obj.year != null && !obj.year.isEmpty()) && (obj.plot != null && !obj.plot.isEmpty())))
                && !"iptv".equalsIgnoreCase(obj.provider)
                && !"live".equalsIgnoreCase(obj.provider);

        if (isMovieItem) {
            Intent intent = new Intent(getContext(), ActivityMovieDetail.class);
            intent.putExtra("title", obj.title != null ? obj.title : "");
            intent.putExtra("image", obj.image != null ? obj.image : "");
            intent.putExtra("year", obj.year != null ? obj.year : "");
            intent.putExtra("runtime", obj.runtime != null ? obj.runtime : "");
            intent.putExtra("rating", obj.rating != null ? obj.rating : "");
            intent.putExtra("director", obj.getDirectorString());
            intent.putExtra("genre", obj.genre != null ? obj.genre : "");
            intent.putExtra("plot", obj.plot != null ? obj.plot : "");
            if (obj.cast != null) {
                intent.putExtra("cast", android.text.TextUtils.join(", ", obj.cast));
            }
            String targetOtt = (obj.ottUrl != null && !obj.ottUrl.isEmpty()) ? obj.ottUrl : obj.link;
            if (targetOtt != null && !targetOtt.isEmpty()) {
                intent.putExtra("ott_url", targetOtt);
            }
            startActivity(intent);
            return;
        }

        String targetUrl = (obj.arguments != null && !obj.arguments.isEmpty()) ? obj.arguments.get(0) : "";

        // Check if it's an IPTV stream
        if (targetUrl.contains(".m3u8") || "iptv".equalsIgnoreCase(obj.provider)) {
            Intent intent = new Intent(getContext(), ActivityVideoDetail.class);
            intent.putExtra("videoId", targetUrl);
            intent.putExtra("title", obj.title);
            intent.putExtra("thumbUrl", obj.image);
            intent.putExtra("date", "Kerala Local IPTV • Live 24/7");
            startActivity(intent);
            return;
        }

        // Check if it's a direct YouTube video URL
        String extractedVideoId = null;
        if (targetUrl.contains("watch?v=")) {
            extractedVideoId = targetUrl.substring(targetUrl.indexOf("watch?v=") + 8);
            if (extractedVideoId.contains("&")) {
                extractedVideoId = extractedVideoId.substring(0, extractedVideoId.indexOf("&"));
            }
        } else if (targetUrl.contains("youtu.be/")) {
            extractedVideoId = targetUrl.substring(targetUrl.indexOf("youtu.be/") + 9);
            if (extractedVideoId.contains("?")) {
                extractedVideoId = extractedVideoId.substring(0, extractedVideoId.indexOf("?"));
            }
        }

        if (extractedVideoId != null && !extractedVideoId.isEmpty() && !"live".equalsIgnoreCase(obj.provider)) {
            Intent intent = new Intent(getContext(), ActivityVideoDetail.class);
            intent.putExtra("videoId", extractedVideoId);
            intent.putExtra("title", obj.title);
            intent.putExtra("thumbUrl", obj.image);
            startActivity(intent);
            return;
        }

        // Check live stream
        if (targetUrl.contains("/live") || "live".equalsIgnoreCase(obj.provider)) {
            final String liveTarget = targetUrl;
            final String title = obj.title;
            final String thumb = obj.image;
            android.app.ProgressDialog pd = new android.app.ProgressDialog(getContext());
            pd.setMessage("Connecting to " + title + "...");
            pd.setCancelable(true);
            try { pd.show(); } catch (Exception ignored) {}

            new Thread(() -> {
                String liveVid = com.app.webdroid.util.YouTubeInnertubeFetcher.resolveLiveVideoId(liveTarget, title);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try { pd.dismiss(); } catch (Exception ignored) {}
                        Intent intent = new Intent(getContext(), ActivityVideoDetail.class);
                        intent.putExtra("videoId", (liveVid != null && !liveVid.isEmpty()) ? liveVid : liveTarget);
                        intent.putExtra("title", title);
                        intent.putExtra("thumbUrl", thumb);
                        intent.putExtra("date", "Live Stream • HD 24/7");
                        startActivity(intent);
                    });
                }
            }).start();
            return;
        }

        // Provider routing
        String type = "VIDEOS";
        if ("overview".equalsIgnoreCase(obj.provider) || "category".equalsIgnoreCase(obj.provider)) {
            type = "CATEGORY";
        } else if ("rss".equalsIgnoreCase(obj.provider)) {
            type = targetUrl.contains("youtube.com/feeds") ? "VIDEOS" : "RSS";
        } else if ("movies".equalsIgnoreCase(obj.provider)) {
            type = "MOVIES";
        } else if ("youtube_channel".equalsIgnoreCase(obj.provider)) {
            if (targetUrl.startsWith("UC")) {
                targetUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=" + targetUrl;
                type = "VIDEOS";
            } else if (targetUrl.contains("channel_id=")) {
                type = "VIDEOS";
            } else {
                type = "YOUTUBE";
            }
        } else if ("youtube_playlist".equalsIgnoreCase(obj.provider)) {
            if (!targetUrl.startsWith("http")) {
                targetUrl = "https://www.youtube.com/feeds/videos.xml?playlist_id=" + targetUrl;
            }
            type = "VIDEOS";
        }

        if (targetUrl.contains("youtube.com/feeds") || targetUrl.contains("channel_id=") || targetUrl.contains("playlist_id=") || targetUrl.startsWith("playlists/")) {
            type = "VIDEOS";
        }

        if (activity != null) {
            activity.loadWebPage(obj.title, type, targetUrl, targetUrl);
        }
    }

    private void observeNewsLiveData() {
        if (getContext() == null || !isAdded() || getView() == null) return;
        try {
            AppDatabase.getDatabase(getContext()).newsDao().getAllNews().observe(getViewLifecycleOwner(), newsItems -> {
                if (newsItems != null && !newsItems.isEmpty()) {
                    sCachedNews = newsItems;
                    if (newsAdapter != null) {
                        newsAdapter.setItems(newsItems.subList(0, Math.min(newsItems.size(), 10)));
                    }
                    updateLocationAndDate();
                    filterAndDisplayCategoryNews(currentSelectedCategory, getBadgeLabelForCategory(currentSelectedCategory));
                }
            });
        } catch (Exception ignored) {}
    }

    private void setupCategoryTabs(View root) {
        allCategoryTabs.clear();
        int[] tabIds = {
                R.id.tab_home_for_you, R.id.tab_home_trending, R.id.tab_home_local,
                R.id.tab_home_weather, R.id.tab_home_politics, R.id.tab_home_business,
                R.id.tab_home_tech, R.id.tab_home_sports, R.id.tab_home_health,
                R.id.tab_home_entertainment, R.id.tab_home_world, R.id.tab_home_channels
        };
        String[] catKeys = {
                "FOR_YOU", "TRENDING", "LOCAL", "WEATHER", "POLITICS", "BUSINESS",
                "TECH", "SPORTS", "HEALTH", "ENTERTAINMENT", "WORLD", "CHANNELS"
        };
        String[] badgeLabels = {
                "FOR YOU", "TRENDING", "LOCAL NEWS", "WEATHER ALERTS", "POLITICS", "WALL STREET",
                "TECH & AI", "SPORTS", "HEALTH & SCIENCE", "ENTERTAINMENT", "WORLD NEWS", "LIVE TV"
        };

        for (int i = 0; i < tabIds.length; i++) {
            TextView tab = root.findViewById(tabIds[i]);
            if (tab != null) {
                allCategoryTabs.add(tab);
                final String catKey = catKeys[i];
                final String label = badgeLabels[i];
                tab.setOnClickListener(v -> selectCategoryTab(tab, catKey, label));
            }
        }
    }

    private void selectCategoryTab(TextView selectedTab, String categoryKey, String badgeLabel) {
        currentSelectedCategory = categoryKey;
        if (selectedTab != null) {
            selectedTab.setBackgroundResource(R.drawable.bg_chip_selected);
            selectedTab.setTextColor(android.graphics.Color.WHITE);
        }
        for (TextView tab : allCategoryTabs) {
            if (tab != selectedTab && tab != null) {
                tab.setBackgroundResource(R.drawable.bg_chip_unselected);
                tab.setTextColor(android.graphics.Color.WHITE);
            }
        }

        if (badgeActiveTab != null) {
            badgeActiveTab.setText(badgeLabel);
        }

        if ("CHANNELS".equals(categoryKey)) {
            if (cardHeroBreakingNews != null) cardHeroBreakingNews.setVisibility(View.GONE);
            if (rvCategoryNews != null) rvCategoryNews.setVisibility(View.GONE);
            View headerTv = getView() != null ? getView().findViewById(R.id.header_tv) : null;
            if (headerTv != null) {
                headerTv.getParent().requestChildFocus(headerTv, headerTv);
            }
            return;
        }

        if (rvCategoryNews != null) rvCategoryNews.setVisibility(View.VISIBLE);
        filterAndDisplayCategoryNews(categoryKey, badgeLabel);
    }

    private String getBadgeLabelForCategory(String key) {
        if ("FOR_YOU".equals(key)) return "FOR YOU";
        if ("TRENDING".equals(key)) return "TRENDING";
        if ("LOCAL".equals(key)) return "LOCAL NEWS";
        if ("WEATHER".equals(key)) return "WEATHER ALERTS";
        if ("POLITICS".equals(key)) return "POLITICS";
        if ("BUSINESS".equals(key)) return "WALL STREET";
        if ("TECH".equals(key)) return "TECH & AI";
        if ("SPORTS".equals(key)) return "SPORTS";
        if ("HEALTH".equals(key)) return "HEALTH";
        if ("ENTERTAINMENT".equals(key)) return "ENTERTAINMENT";
        if ("WORLD".equals(key)) return "WORLD";
        if ("CHANNELS".equals(key)) return "LIVE TV";
        return "BREAKING";
    }

    private void filterAndDisplayCategoryNews(String categoryKey, String badgeLabel) {
        List<NewsItem> all = sCachedNews;
        if (all == null || all.isEmpty()) {
            new Thread(() -> {
                if (getContext() != null) {
                    try {
                        List<NewsItem> dbItems = AppDatabase.getDatabase(getContext()).newsDao().getAllNewsSync();
                        if (dbItems != null && !dbItems.isEmpty()) {
                            sCachedNews = dbItems;
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> filterAndDisplayCategoryNews(categoryKey, badgeLabel));
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

        if (matched.size() < 3 && !"WEATHER".equals(categoryKey)) {
            for (NewsItem item : all) {
                if (!matched.contains(item)) {
                    matched.add(item);
                    if (matched.size() >= 8) break;
                }
            }
        }

        if (matched.isEmpty()) {
            if (cardHeroBreakingNews != null) cardHeroBreakingNews.setVisibility(View.GONE);
            if (categoryNewsAdapter != null) categoryNewsAdapter.setItems(new ArrayList<>());
            return;
        }

        NewsItem hero = null;
        int heroIdx = -1;
        for (int i = 0; i < matched.size(); i++) {
            NewsItem it = matched.get(i);
            if (it.hasRealImage()) {
                hero = it;
                heroIdx = i;
                break;
            }
        }
        if (hero == null) {
            hero = matched.get(0);
            heroIdx = 0;
        }

        if (cardHeroBreakingNews != null) {
            cardHeroBreakingNews.setVisibility(View.VISIBLE);
            final NewsItem finalHero = hero;
            cardHeroBreakingNews.setOnClickListener(v -> {
                if (getContext() == null) return;
                Intent intent = new Intent(getContext(), ActivityNewsDetail.class);
                intent.putExtra("title", finalHero.title);
                intent.putExtra("description", finalHero.description);
                intent.putExtra("link", finalHero.link);
                intent.putExtra("imageUrl", finalHero.imageUrl);
                intent.putExtra("pubDate", finalHero.pubDate);
                intent.putExtra("sourceName", finalHero.sourceName);
                startActivity(intent);
            });

            if (tvHeroTitle != null) tvHeroTitle.setText(hero.title != null ? hero.title : "");
            if (tvHeroSource != null) {
                String src = hero.sourceName != null ? hero.sourceName : "Top News";
                String dt = hero.pubDate != null ? hero.pubDate : "Live";
                tvHeroSource.setText(src.toUpperCase(Locale.US) + " • " + dt);
            }
            if (badgeHeroTag != null) {
                if ("WEATHER".equals(categoryKey)) {
                    badgeHeroTag.setText("🌤️ WEATHER ALERT");
                } else if ("LOCAL".equals(categoryKey)) {
                    badgeHeroTag.setText("📍 LOCAL PRESS");
                } else if ("POLITICS".equals(categoryKey)) {
                    badgeHeroTag.setText("🏛️ CAPITOL & POLITICS");
                } else if ("BUSINESS".equals(categoryKey)) {
                    badgeHeroTag.setText("📈 WALL STREET");
                } else {
                    badgeHeroTag.setText("⚡ " + badgeLabel);
                }
            }

            if (imgHeroNews != null && getContext() != null) {
                if (hero.imageUrl != null && !hero.imageUrl.isEmpty()) {
                    Glide.with(getContext())
                            .load(hero.imageUrl)
                            .placeholder(R.drawable.ic_placeholder_media)
                            .error(R.drawable.ic_placeholder_media)
                            .into(imgHeroNews);
                } else {
                    imgHeroNews.setImageResource(R.drawable.ic_placeholder_media);
                }
            }

            if (btnHeroShare != null) {
                final NewsItem finalHeroShare = hero;
                btnHeroShare.setOnClickListener(v -> {
                    if (getContext() == null) return;
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, finalHeroShare.title + "\n" + finalHeroShare.link);
                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, "Share Article"));
                });
            }
        }

        List<NewsItem> feed = new ArrayList<>();
        for (int i = 0; i < matched.size(); i++) {
            if (i != heroIdx) {
                feed.add(matched.get(i));
                if (feed.size() >= 8) break;
            }
        }
        if (categoryNewsAdapter != null) {
            categoryNewsAdapter.setItems(feed);
        }
    }

    private void updateLocationAndDate() {
        if (getContext() == null) return;
        android.content.SharedPreferences sp = getContext().getSharedPreferences("weather_ticker_prefs", Context.MODE_PRIVATE);
        String city = sp.getString("weather_city", "United States");
        if (tvLocationCity != null) {
            tvLocationCity.setText(city);
        }
        if (tvLocationDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy, hh:mm a", Locale.US);
            tvLocationDate.setText(sdf.format(new Date()));
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
                    com.app.webdroid.util.RssParser parser = new com.app.webdroid.util.RssParser();
                    List<NewsItem> allFetched = new ArrayList<>();
                    for (int i = 0; i < Math.min(sources.size(), 6); i++) {
                        AppConfig.RssSource src = sources.get(i);
                        try {
                            java.net.URL u = new java.net.URL(src.url);
                            java.net.HttpURLConnection c = (java.net.HttpURLConnection) u.openConnection();
                            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                            c.setConnectTimeout(5000);
                            c.setReadTimeout(5000);
                            if (c.getResponseCode() == 200) {
                                List<NewsItem> parsed = parser.parseNews(c.getInputStream(), src.title);
                                if (parsed != null && !parsed.isEmpty()) {
                                    allFetched.addAll(parsed);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    if (!allFetched.isEmpty()) {
                        AppDatabase.getDatabase(ctx).newsDao().insertNews(allFetched);
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private boolean matchesCategory(NewsItem item, String category) {
        if (item == null) return false;
        String source = item.sourceName != null ? item.sourceName.toLowerCase() : "";
        String title = item.title != null ? item.title.toLowerCase() : "";
        String desc = item.description != null ? item.description.toLowerCase() : "";
        String combined = source + " " + title + " " + desc;

        switch (category) {
            case "FOR_YOU":
                return true;
            case "TRENDING":
                return combined.contains("breaking") || combined.contains("alert") || combined.contains("live")
                        || combined.contains("top") || combined.contains("massive") || combined.contains("attack")
                        || combined.contains("exclusive") || combined.contains("urgent");
            case "LOCAL":
                return source.contains("local") || source.contains("state") || source.contains("city")
                        || combined.contains("police") || combined.contains("mayor") || combined.contains("county")
                        || combined.contains("local") || combined.contains("sheriff") || combined.contains("highway");
            case "WEATHER":
                return source.contains("weather") || source.contains("accuweather") || source.contains("noaa")
                        || combined.contains("weather") || combined.contains("storm") || combined.contains("hurricane")
                        || combined.contains("tornado") || combined.contains("rain") || combined.contains("flood")
                        || combined.contains("snow") || combined.contains("temperature") || combined.contains("forecast")
                        || combined.contains("radar") || combined.contains("blizzard") || combined.contains("heat wave");
            case "POLITICS":
                return source.contains("politico") || source.contains("hill") || source.contains("politic")
                        || combined.contains("biden") || combined.contains("trump") || combined.contains("congress")
                        || combined.contains("senate") || combined.contains("house") || combined.contains("white house")
                        || combined.contains("democrat") || combined.contains("republican") || combined.contains("supreme court")
                        || combined.contains("election") || combined.contains("capitol") || combined.contains("governor");
            case "BUSINESS":
                return source.contains("business") || source.contains("market") || source.contains("wsj")
                        || source.contains("cnbc") || source.contains("bloomberg") || source.contains("finance")
                        || combined.contains("stock") || combined.contains("dow") || combined.contains("nasdaq")
                        || combined.contains("fed") || combined.contains("inflation") || combined.contains("economy")
                        || combined.contains("market") || combined.contains("revenue") || combined.contains("earnings");
            case "TECH":
                return source.contains("tech") || source.contains("verge") || source.contains("wired")
                        || combined.contains("ai") || combined.contains("apple") || combined.contains("google")
                        || combined.contains("microsoft") || combined.contains("meta") || combined.contains("nvidia")
                        || combined.contains("openai") || combined.contains("software") || combined.contains("cyber");
            case "WORLD":
                return source.contains("world") || source.contains("international") || source.contains("foreign")
                        || combined.contains("ukraine") || combined.contains("russia") || combined.contains("china")
                        || combined.contains("middle east") || combined.contains("europe") || combined.contains("israel")
                        || combined.contains("nato") || combined.contains("un ") || combined.contains("global");
            case "SPORTS":
                return source.contains("sport") || source.contains("espn") || combined.contains("nfl")
                        || combined.contains("nba") || combined.contains("mlb") || combined.contains("nhl")
                        || combined.contains("football") || combined.contains("basketball") || combined.contains("baseball")
                        || combined.contains("super bowl") || combined.contains("championship");
            case "ENTERTAINMENT":
                return source.contains("entertainment") || source.contains("variety") || source.contains("hollywood")
                        || source.contains("movie") || combined.contains("actor") || combined.contains("box office")
                        || combined.contains("film") || combined.contains("oscar") || combined.contains("celebrity")
                        || combined.contains("music") || combined.contains("series");
            case "HEALTH":
                return source.contains("health") || source.contains("medical") || combined.contains("fda")
                        || combined.contains("cdc") || combined.contains("vaccine") || combined.contains("doctor")
                        || combined.contains("hospital") || combined.contains("diet") || combined.contains("treatment");
            default:
                return true;
        }
    }

    private List<YouTubeItem> loadPlaylistAsset(String path) {
        List<YouTubeItem> list = new ArrayList<>();
        if (getContext() == null) return list;
        try {
            String json = com.app.webdroid.util.AppJsonManager.loadJsonSync(getContext(), path);
            if (json != null) {
                Type type = new TypeToken<List<YouTubeItem>>() {}.getType();
                List<YouTubeItem> parsed = new Gson().fromJson(json, type);
                if (parsed != null) list.addAll(parsed);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<AppConfig.OverviewItem> loadOverviewAsset(String path) {
        List<AppConfig.OverviewItem> list = new ArrayList<>();
        if (getContext() == null) return list;
        try {
            String json = com.app.webdroid.util.AppJsonManager.loadJsonSync(getContext(), path);
            if (json != null) {
                Type type = new TypeToken<List<AppConfig.OverviewItem>>() {}.getType();
                List<AppConfig.OverviewItem> parsed = new Gson().fromJson(json, type);
                if (parsed != null) list.addAll(parsed);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<RadioStation> loadRadioAsset() {
        List<RadioStation> list = new ArrayList<>();
        if (getContext() == null) return list;
        try {
            String json = com.app.webdroid.util.AppJsonManager.loadJsonSync(getContext(), "news_radio.json");
            if (json != null) {
                JSONArray arr = new JSONArray(json);
                Gson gson = new Gson();
                Type stationType = new TypeToken<List<RadioStation>>() {}.getType();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject catObj = arr.getJSONObject(i);
                    JSONArray stArray = catObj.getJSONArray("stations");
                    List<RadioStation> stations = gson.fromJson(stArray.toString(), stationType);
                    if (stations != null) list.addAll(stations);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
