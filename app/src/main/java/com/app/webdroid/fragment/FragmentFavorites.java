package com.app.webdroid.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.shobmc.san.R;
import com.app.webdroid.activity.ActivityMovieDetail;
import com.app.webdroid.activity.ActivityNewsDetail;
import com.app.webdroid.activity.ActivityVideoDetail;
import com.app.webdroid.activity.ActivityWebView;
import com.app.webdroid.activity.MainActivity;
import com.app.webdroid.adapter.AdapterFavorites;
import com.app.webdroid.adapter.AdapterHistoryHorizontal;
import com.app.webdroid.database.AppDatabase;
import com.app.webdroid.database.FavoriteDao;
import com.app.webdroid.database.HistoryDao;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.model.FavoriteItem;
import com.app.webdroid.model.HistoryItem;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class FragmentFavorites extends Fragment {

    public static final String FILTER_ALL = "ALL";
    public static final String FILTER_HISTORY = "HISTORY";
    public static final String FILTER_CHANNELS = "CHANNELS";
    public static final String FILTER_VIDEOS = "VIDEOS";
    public static final String FILTER_ARTICLES = "ARTICLES";
    public static final String FILTER_MOVIES = "MOVIES";

    private NestedScrollView nestedScrollView;
    private RecyclerView recyclerView;
    private HorizontalScrollView scrollTabsContainer;
    private ChipGroup chipGroup;
    private LinearLayout lytEmpty;
    private ImageView imgEmptyIcon;
    private TextView textEmptyTitle;
    private TextView textEmptySubtitle;

    private LinearLayout layoutHistorySection;
    private TextView textHistoryTitle;
    private TextView btnClearHistory;
    private RecyclerView recyclerViewHistory;
    private AdapterHistoryHorizontal adapterHistory;
    private TextView textSectionSavedHeader;

    private AdapterFavorites adapter;
    private FavoriteDao favoriteDao;
    private HistoryDao historyDao;

    private MainActivity activity;
    private SharedPref sharedPref;
    private Toolbar toolbar;

    private List<FavoriteItem> allFavorites = new ArrayList<>();
    private List<HistoryItem> allHistory = new ArrayList<>();
    private String currentFilter = FILTER_ALL;

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
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        sharedPref = new SharedPref(requireContext());
        AppDatabase db = AppDatabase.getDatabase(getContext());
        favoriteDao = db.favoriteDao();
        historyDao = db.historyDao();

        nestedScrollView = view.findViewById(R.id.nested_scroll_view);
        recyclerView = view.findViewById(R.id.recycler_view_favorites);
        scrollTabsContainer = view.findViewById(R.id.scroll_tabs_container);
        chipGroup = view.findViewById(R.id.chip_group);
        lytEmpty = view.findViewById(R.id.lyt_empty);
        imgEmptyIcon = view.findViewById(R.id.img_empty_icon);
        textEmptyTitle = view.findViewById(R.id.text_empty_title);
        textEmptySubtitle = view.findViewById(R.id.text_empty_subtitle);

        layoutHistorySection = view.findViewById(R.id.layout_history_section);
        textHistoryTitle = view.findViewById(R.id.text_history_title);
        btnClearHistory = view.findViewById(R.id.btn_clear_history);
        recyclerViewHistory = view.findViewById(R.id.recycler_view_history);
        textSectionSavedHeader = view.findViewById(R.id.text_section_saved_header);

        Button btnExplore = view.findViewById(R.id.btn_explore);
        btnExplore.setOnClickListener(v -> {
            if (activity != null) {
                activity.loadHomeDiscovery();
            }
        });

        // Setup Main Grid RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new AdapterFavorites(getContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Setup Horizontal Watch History Carousel
        if (recyclerViewHistory != null) {
            LinearLayoutManager hLm = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
            recyclerViewHistory.setLayoutManager(hLm);
            adapterHistory = new AdapterHistoryHorizontal(getContext(), new ArrayList<>());
            recyclerViewHistory.setAdapter(adapterHistory);

            adapterHistory.setOnItemClickListener((v, obj, position) -> launchContent(obj.type, obj.itemId, obj.targetUrl, obj.title, obj.subtitle, obj.imageUrl));

            adapterHistory.setOnDeleteClickListener((v, obj, position) -> {
                if (getContext() == null || obj == null) return;
                new AlertDialog.Builder(getContext())
                        .setTitle("Remove from History?")
                        .setMessage("Remove \"" + (obj.title != null ? obj.title : "this item") + "\" from your watch history?")
                        .setPositiveButton("Remove", (dialog, which) -> deleteHistoryItem(obj))
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // Clear All History action
        if (btnClearHistory != null) {
            btnClearHistory.setOnClickListener(v -> {
                if (getContext() == null) return;
                new AlertDialog.Builder(getContext())
                        .setTitle("Clear Watch History?")
                        .setMessage("This will clear all recently watched videos, movies, and live streams from your device history.")
                        .setPositiveButton("Clear All", (dialog, which) -> clearAllHistory())
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // Scroll listener to hide/show appbar and bottom nav
        if (nestedScrollView != null) {
            nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                int dy = scrollY - oldScrollY;
                if (activity != null) {
                    activity.onScroll(dy);
                }
            });
        }

        // Main Adapter Click Listener
        adapter.setOnItemClickListener((v, obj, position) -> launchContent(obj.type, obj.itemId, obj.targetUrl, obj.title, obj.subtitle, obj.imageUrl));

        // Main Adapter Delete / Remove Listener
        adapter.setOnDeleteClickListener((v, obj, position) -> {
            if (getContext() == null || obj == null) return;
            if (FILTER_HISTORY.equalsIgnoreCase(currentFilter)) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Remove from History?")
                        .setMessage("Remove \"" + (obj.title != null ? obj.title : "this item") + "\" from your watch history?")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            HistoryItem h = new HistoryItem();
                            h.id = obj.id;
                            h.itemId = obj.itemId;
                            h.title = obj.title;
                            deleteHistoryItem(h);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                new AlertDialog.Builder(getContext())
                        .setTitle("Remove from Saved Content?")
                        .setMessage("Are you sure you want to remove \"" + (obj.title != null ? obj.title : "this item") + "\" from your saved items?")
                        .setPositiveButton("Remove", (dialog, which) -> removeFavoriteItem(obj))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        // Observe Favorites
        favoriteDao.getAllFavorites().observe(getViewLifecycleOwner(), items -> {
            allFavorites = items != null ? new ArrayList<>(items) : new ArrayList<>();
            refreshView();
        });

        // Observe Watch History
        historyDao.getAllHistory().observe(getViewLifecycleOwner(), items -> {
            allHistory = items != null ? new ArrayList<>(items) : new ArrayList<>();
            if (adapterHistory != null) {
                adapterHistory.setItems(allHistory);
            }
            refreshView();
        });

        setupToolbar(view);

        return view;
    }

    private void launchContent(String type, String itemId, String targetUrl, String title, String subtitle, String imageUrl) {
        if ("IPTV".equalsIgnoreCase(type) || (targetUrl != null && targetUrl.contains(".m3u8"))) {
            Intent intent = new Intent(getContext(), ActivityVideoDetail.class);
            intent.putExtra("videoId", targetUrl != null && !targetUrl.isEmpty() ? targetUrl : itemId);
            intent.putExtra("title", title);
            intent.putExtra("date", subtitle != null ? subtitle : "Kerala Local IPTV • Live 24/7");
            intent.putExtra("thumbUrl", imageUrl);
            startActivity(intent);
        } else if (FavoriteItem.TYPE_YOUTUBE.equalsIgnoreCase(type) || "VIDEO".equalsIgnoreCase(type) || HistoryItem.TYPE_SONG.equalsIgnoreCase(type)) {
            Intent intent = new Intent(getContext(), ActivityVideoDetail.class);
            intent.putExtra("videoId", itemId);
            intent.putExtra("title", title);
            intent.putExtra("date", subtitle);
            intent.putExtra("thumbUrl", imageUrl);
            intent.putExtra("channelName", subtitle);
            startActivity(intent);
        } else if (FavoriteItem.TYPE_MOVIES.equalsIgnoreCase(type) || "MOVIE".equalsIgnoreCase(type)) {
            Intent intent = new Intent(getContext(), ActivityMovieDetail.class);
            intent.putExtra("title", title);
            intent.putExtra("image", imageUrl);
            intent.putExtra("year", subtitle);
            startActivity(intent);
        } else if (FavoriteItem.TYPE_RSS.equalsIgnoreCase(type) || "NEWS".equalsIgnoreCase(type) || "rss_item".equalsIgnoreCase(type)) {
            Intent intent = new Intent(getContext(), ActivityNewsDetail.class);
            intent.putExtra("title", title);
            intent.putExtra("description", subtitle != null ? subtitle : title);
            intent.putExtra("link", targetUrl != null && !targetUrl.isEmpty() ? targetUrl : itemId);
            intent.putExtra("imageUrl", imageUrl);
            intent.putExtra("pubDate", subtitle != null ? subtitle : "Saved");
            intent.putExtra("sourceName", subtitle != null ? subtitle : "Saved Story");
            startActivity(intent);
        } else if ("VIDEOS".equalsIgnoreCase(type) || "CHANNEL".equalsIgnoreCase(type)
                || (targetUrl != null && targetUrl.contains("youtube.com/feeds"))) {
            if (activity != null) {
                activity.loadWebPage(title, "VIDEOS", targetUrl != null ? targetUrl : itemId, null);
            }
        } else {
            if (getContext() != null && targetUrl != null) {
                com.app.webdroid.util.Tools.showOpenLinkDialog(getContext(), title, targetUrl,
                        new com.app.webdroid.util.Tools.OnOpenLinkChoiceListener() {
                            @Override
                            public void onOpenInApp() {
                                Intent intent = new Intent(getContext(), ActivityWebView.class);
                                intent.putExtra("title", title);
                                intent.putExtra("link", targetUrl);
                                startActivity(intent);
                            }

                            @Override
                            public void onOpenOutside() {
                                try {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(targetUrl));
                                    startActivity(intent);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
            }
        }
    }

    private void setupToolbar(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        if (toolbar == null) return;

        if (activity != null) {
            activity.setSupportActionBar(toolbar);
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                activity.getSupportActionBar().setTitle("Saved & History");
            }
            toolbar.setNavigationOnClickListener(v -> {
                if (activity != null) {
                    activity.loadHomeDiscovery();
                }
            });
        }

        if (sharedPref.getToolbar()) {
            toolbar.setVisibility(View.VISIBLE);
        } else {
            toolbar.setVisibility(View.GONE);
        }
    }

    /**
     * Updates filter chips adhering to user rule:
     * - "All" and "History" are shown by default.
     * - Rest ("My Channels", "Saved Videos", "Saved Articles", "Movies") only shown if user has saved items in that category!
     */
    private void updateChips() {
        chipGroup.removeAllViews();

        boolean hasChannels = false;
        boolean hasVideos = false;
        boolean hasArticles = false;
        boolean hasMovies = false;

        if (allFavorites != null) {
            for (FavoriteItem it : allFavorites) {
                String t = it.type != null ? it.type.toUpperCase() : "";
                if (t.equals("CHANNEL") || t.equals("VIDEOS") || t.equals("WEB") || t.equals("IPTV")
                        || (it.targetUrl != null && it.targetUrl.contains("youtube.com/feeds"))) {
                    hasChannels = true;
                } else if (t.equals("YOUTUBE") || t.equals("VIDEO")) {
                    hasVideos = true;
                } else if (t.equals("RSS") || t.equals("ARTICLE")) {
                    hasArticles = true;
                } else if (t.equals("MOVIES") || t.equals("MOVIE")) {
                    hasMovies = true;
                }
            }
        }

        // Validate currentFilter in case category was emptied
        if (FILTER_CHANNELS.equals(currentFilter) && !hasChannels) currentFilter = FILTER_ALL;
        if (FILTER_VIDEOS.equals(currentFilter) && !hasVideos) currentFilter = FILTER_ALL;
        if (FILTER_ARTICLES.equals(currentFilter) && !hasArticles) currentFilter = FILTER_ALL;
        if (FILTER_MOVIES.equals(currentFilter) && !hasMovies) currentFilter = FILTER_ALL;

        // "All" and "History" ALWAYS shown by default
        addChip("All", FILTER_ALL, currentFilter.equals(FILTER_ALL));
        addChip("Watch History", FILTER_HISTORY, currentFilter.equals(FILTER_HISTORY));

        // Rest only shown if user has saved/favorited items in that category
        if (hasChannels) {
            addChip("My Channels", FILTER_CHANNELS, currentFilter.equals(FILTER_CHANNELS));
        }
        if (hasVideos) {
            addChip("Saved Videos", FILTER_VIDEOS, currentFilter.equals(FILTER_VIDEOS));
        }
        if (hasArticles) {
            addChip("Saved Articles", FILTER_ARTICLES, currentFilter.equals(FILTER_ARTICLES));
        }
        if (hasMovies) {
            addChip("Movies", FILTER_MOVIES, currentFilter.equals(FILTER_MOVIES));
        }

        filter(currentFilter);
    }

    private void addChip(String label, String type, boolean checked) {
        Chip chip = new Chip(getContext());
        chip.setText(label);
        chip.setTag(type);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setChipBackgroundColorResource(R.color.color_chip_bg);
        chip.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.color_chip_text));
        chip.setChipStrokeColorResource(R.color.color_chip_stroke);
        chip.setChipStrokeWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        chip.setChipCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, getResources().getDisplayMetrics()));
        chip.setChecked(checked);

        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                filter(type);
            }
        });
        chipGroup.addView(chip);
    }

    private void refreshView() {
        boolean noFavorites = (allFavorites == null || allFavorites.isEmpty());
        boolean noHistory = (allHistory == null || allHistory.isEmpty());

        if (noFavorites && noHistory) {
            if (layoutHistorySection != null) layoutHistorySection.setVisibility(View.GONE);
            if (scrollTabsContainer != null) scrollTabsContainer.setVisibility(View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
            if (textSectionSavedHeader != null) textSectionSavedHeader.setVisibility(View.GONE);
            if (lytEmpty != null) {
                lytEmpty.setVisibility(View.VISIBLE);
                if (imgEmptyIcon != null) imgEmptyIcon.setImageResource(R.drawable.ic_bookmark);
                if (textEmptyTitle != null) textEmptyTitle.setText("No watch history or saved items yet");
                if (textEmptySubtitle != null) textEmptySubtitle.setText("Watch videos, movies, or save channels to access them anytime");
            }
        } else {
            if (scrollTabsContainer != null) scrollTabsContainer.setVisibility(View.VISIBLE);
            updateChips();
        }
    }

    private void filter(String type) {
        currentFilter = type;

        if (FILTER_HISTORY.equalsIgnoreCase(type)) {
            // Show full watch history in main recycler view
            if (layoutHistorySection != null) layoutHistorySection.setVisibility(View.GONE);
            if (textSectionSavedHeader != null) textSectionSavedHeader.setVisibility(View.GONE);

            if (allHistory == null || allHistory.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                lytEmpty.setVisibility(View.VISIBLE);
                if (imgEmptyIcon != null) imgEmptyIcon.setImageResource(R.drawable.ic_history);
                if (textEmptyTitle != null) textEmptyTitle.setText("Your watch history is empty");
                if (textEmptySubtitle != null) textEmptySubtitle.setText("Videos, songs, live TV, and movies you watch will appear here");
            } else {
                lytEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

                List<FavoriteItem> converted = new ArrayList<>();
                for (HistoryItem h : allHistory) {
                    FavoriteItem fav = new FavoriteItem();
                    fav.id = h.id;
                    fav.itemId = h.itemId;
                    fav.type = h.type;
                    fav.title = h.title;
                    String timeAgo = AdapterHistoryHorizontal.formatRelativeTime(h.watchedAt);
                    fav.subtitle = (h.subtitle != null && !h.subtitle.isEmpty() ? h.subtitle + " • " : "") + timeAgo;
                    fav.imageUrl = h.imageUrl;
                    fav.targetUrl = h.targetUrl;
                    converted.add(fav);
                }
                adapter.setItems(converted);
            }
            return;
        }

        // If FILTER_ALL: show horizontal History carousel at the top if history is not empty
        if (FILTER_ALL.equalsIgnoreCase(type)) {
            if (allHistory != null && !allHistory.isEmpty()) {
                if (layoutHistorySection != null) {
                    layoutHistorySection.setVisibility(View.VISIBLE);
                    if (textHistoryTitle != null) {
                        textHistoryTitle.setText("Watch History (" + allHistory.size() + ")");
                    }
                }
                if (textSectionSavedHeader != null && allFavorites != null && !allFavorites.isEmpty()) {
                    textSectionSavedHeader.setVisibility(View.VISIBLE);
                } else if (textSectionSavedHeader != null) {
                    textSectionSavedHeader.setVisibility(View.GONE);
                }
            } else {
                if (layoutHistorySection != null) layoutHistorySection.setVisibility(View.GONE);
                if (textSectionSavedHeader != null) textSectionSavedHeader.setVisibility(View.GONE);
            }

            if (allFavorites == null || allFavorites.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                lytEmpty.setVisibility(View.VISIBLE);
                if (imgEmptyIcon != null) imgEmptyIcon.setImageResource(R.drawable.ic_bookmark);
                if (textEmptyTitle != null) textEmptyTitle.setText("No saved items yet");
                if (textEmptySubtitle != null) textEmptySubtitle.setText("Save channels, videos, articles, and movies to keep them here");
            } else {
                lytEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.setItems(allFavorites);
            }
            return;
        }

        // Specific category filter (CHANNELS, VIDEOS, ARTICLES, MOVIES)
        if (layoutHistorySection != null) layoutHistorySection.setVisibility(View.GONE);
        if (textSectionSavedHeader != null) textSectionSavedHeader.setVisibility(View.GONE);

        List<FavoriteItem> filtered = new ArrayList<>();
        if (allFavorites != null) {
            for (FavoriteItem item : allFavorites) {
                if (matchesFilter(item, type)) {
                    filtered.add(item);
                }
            }
        }

        if (filtered.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            lytEmpty.setVisibility(View.VISIBLE);
            if (imgEmptyIcon != null) imgEmptyIcon.setImageResource(R.drawable.ic_bookmark);
            if (textEmptyTitle != null) textEmptyTitle.setText("No " + getFilterLabel(type) + " saved yet");
            if (textEmptySubtitle != null) textEmptySubtitle.setText("Browse content to add to your saved collection");
        } else {
            lytEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.setItems(filtered);
        }
    }

    private boolean matchesFilter(FavoriteItem item, String filter) {
        if (filter == null || FILTER_ALL.equalsIgnoreCase(filter)) return true;
        String type = item.type != null ? item.type.toUpperCase() : "";

        if (FILTER_CHANNELS.equalsIgnoreCase(filter)) {
            return type.equals("CHANNEL") || type.equals("VIDEOS") || type.equals("WEB") || type.equals("IPTV")
                    || (item.targetUrl != null && item.targetUrl.contains("youtube.com/feeds"));
        } else if (FILTER_VIDEOS.equalsIgnoreCase(filter)) {
            return type.equals("YOUTUBE") || type.equals("VIDEO");
        } else if (FILTER_ARTICLES.equalsIgnoreCase(filter)) {
            return type.equals("RSS") || type.equals("ARTICLE");
        } else if (FILTER_MOVIES.equalsIgnoreCase(filter)) {
            return type.equals("MOVIES") || type.equals("MOVIE");
        }
        return true;
    }

    private String getFilterLabel(String filter) {
        if (FILTER_CHANNELS.equalsIgnoreCase(filter)) return "channels";
        if (FILTER_VIDEOS.equalsIgnoreCase(filter)) return "videos";
        if (FILTER_ARTICLES.equalsIgnoreCase(filter)) return "articles";
        if (FILTER_MOVIES.equalsIgnoreCase(filter)) return "movies";
        return "items";
    }

    private void deleteHistoryItem(HistoryItem obj) {
        if (obj == null) return;

        if (allHistory != null) {
            for (int i = allHistory.size() - 1; i >= 0; i--) {
                HistoryItem it = allHistory.get(i);
                if (it == obj || (obj.id > 0 && it.id == obj.id)
                        || (obj.itemId != null && obj.itemId.equalsIgnoreCase(it.itemId))
                        || (obj.title != null && obj.title.equalsIgnoreCase(it.title))) {
                    allHistory.remove(i);
                }
            }
        }
        if (adapterHistory != null) {
            adapterHistory.setItems(allHistory);
        }
        refreshView();

        if (getContext() != null) {
            Toast.makeText(getContext(), "\"" + (obj.title != null ? obj.title : "Item") + "\" removed from history", Toast.LENGTH_SHORT).show();
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (historyDao != null) {
                    if (obj.id > 0) {
                        historyDao.deleteById(obj.id);
                    }
                    if (obj.itemId != null && obj.type != null) {
                        historyDao.removeHistory(obj.itemId, obj.type);
                    }
                    historyDao.deleteDuplicates(obj.itemId, obj.targetUrl, obj.title);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void clearAllHistory() {
        if (allHistory != null) {
            allHistory.clear();
        }
        if (adapterHistory != null) {
            adapterHistory.setItems(new ArrayList<>());
        }
        refreshView();

        if (getContext() != null) {
            Toast.makeText(getContext(), "Watch history cleared", Toast.LENGTH_SHORT).show();
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (historyDao != null) {
                    historyDao.clearAllHistory();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void removeFavoriteItem(FavoriteItem obj) {
        if (obj == null) return;

        if (allFavorites != null) {
            for (int i = allFavorites.size() - 1; i >= 0; i--) {
                FavoriteItem it = allFavorites.get(i);
                if (it == obj || (obj.id > 0 && it.id == obj.id)
                        || (obj.itemId != null && obj.itemId.equalsIgnoreCase(it.itemId))
                        || (obj.targetUrl != null && obj.targetUrl.equalsIgnoreCase(it.targetUrl))
                        || (obj.title != null && obj.title.equalsIgnoreCase(it.title))) {
                    allFavorites.remove(i);
                }
            }
        }
        refreshView();

        if (getContext() != null) {
            Toast.makeText(getContext(), "\"" + (obj.title != null ? obj.title : "Item") + "\" removed from saved", Toast.LENGTH_SHORT).show();
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (favoriteDao != null) {
                    if (obj.id > 0) {
                        favoriteDao.deleteById(obj.id);
                    }
                    favoriteDao.delete(obj);
                    favoriteDao.removeFavoriteByIdOrDetails(obj.id, obj.itemId, obj.targetUrl, obj.title);
                    favoriteDao.removeFavoriteComprehensive(obj.itemId, obj.targetUrl, obj.title);
                    if (obj.type != null) {
                        favoriteDao.removeFavorite(obj.itemId, obj.type);
                        if (obj.targetUrl != null) {
                            favoriteDao.removeFavorite(obj.targetUrl, obj.type);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
