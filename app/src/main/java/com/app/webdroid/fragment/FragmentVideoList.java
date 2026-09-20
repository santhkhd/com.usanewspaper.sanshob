package com.app.webdroid.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.shobmc.san.R;
import com.app.webdroid.adapter.AdapterVideos;
import com.app.webdroid.model.YouTubeItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.app.webdroid.database.AppDatabase;
import com.app.webdroid.database.FavoriteDao;
import com.app.webdroid.model.FavoriteItem;
import java.util.concurrent.Executors;

import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.List;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.app.webdroid.activity.MainActivity;
import com.app.webdroid.database.prefs.SharedPref;

public class FragmentVideoList extends Fragment {

    private RecyclerView recyclerView;
    private AdapterVideos adapter;
    private ProgressBar progressBar;
    private TextView textError;
    private String jsonFilename;
    private String categoryTitle; // Added

    private String nextPageToken = null;
    private String channelContinuationToken = null;
    private boolean isFetched = false;
    private boolean isLoading = false;
    private FavoriteDao favoriteDao;
    private MainActivity activity; // Added
    private SharedPref sharedPref; // Added
    private Toolbar toolbar; // Added
    private TextView toolbarTitle; // Added
    private View layoutPlaylistBanner;
    private TextView tvPlaylistBannerTitle;
    private View layoutChannelBanner;
    private TextView tvChannelBannerName;
    private ImageView imgChannelBanner;
    private com.google.android.material.button.MaterialButton btnAddChannelBanner;

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            activity = (MainActivity) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_list, container, false);

        if (getArguments() != null) {
            jsonFilename = getArguments().getString("url");
            categoryTitle = getArguments().getString("name");
        }

        sharedPref = new SharedPref(requireContext()); // Initialize pref

        // Setup Menu
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                // Clear menu to ensure no duplicates if fragment reused? No, append.
                menuInflater.inflate(R.menu.menu_video_list, menu);
                MenuItem searchItem = menu.findItem(R.id.action_search);
                if (searchItem != null) {
                    androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
                    if (searchView != null) {
                        searchView.setQueryHint("Search US news, videos, channels...");
                        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                            @Override
                            public boolean onQueryTextSubmit(String query) {
                                if (query != null && !query.trim().isEmpty()) {
                                    searchView.clearFocus();
                                    searchItem.collapseActionView();
                                    performUserSearch(query.trim());
                                }
                                return true;
                            }

                            @Override
                            public boolean onQueryTextChange(String newText) {
                                return false;
                            }
                        });
                    }
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_view_compact) {
                    if (adapter != null)
                        setViewMode(AdapterVideos.VIEW_TYPE_COMPACT);
                    menuItem.setChecked(true);
                    return true;
                } else if (menuItem.getItemId() == R.id.action_view_normal) {
                    if (adapter != null)
                        setViewMode(AdapterVideos.VIEW_TYPE_LIST);
                    menuItem.setChecked(true);
                    return true;
                } else if (menuItem.getItemId() == R.id.action_view_grid) {
                    if (adapter != null)
                        setViewMode(AdapterVideos.VIEW_TYPE_GRID);
                    menuItem.setChecked(true);
                    return true;
                } else if (menuItem.getItemId() == R.id.action_favorites) {
                    if (getActivity() instanceof com.app.webdroid.activity.MainActivity) {
                        ((com.app.webdroid.activity.MainActivity) getActivity()).loadWebPage("Favorites", "FAVORITES",
                                "", "");
                    }
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.STARTED);

        recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        textError = view.findViewById(R.id.text_error);
        layoutPlaylistBanner = view.findViewById(R.id.layout_playlist_banner);
        tvPlaylistBannerTitle = view.findViewById(R.id.tv_playlist_banner_title);
        layoutChannelBanner = view.findViewById(R.id.layout_channel_banner);
        tvChannelBannerName = view.findViewById(R.id.tv_channel_banner_name);
        imgChannelBanner = view.findViewById(R.id.img_channel_banner);
        btnAddChannelBanner = view.findViewById(R.id.btn_add_channel_banner);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdapterVideos(getContext(), new ArrayList<>());
        favoriteDao = AppDatabase.getDatabase(getContext()).favoriteDao();

        // Observe favorites
        favoriteDao.getAllFavorites().observe(getViewLifecycleOwner(), favorites -> {
            List<String> ids = new ArrayList<>();
            if (favorites != null) {
                for (FavoriteItem f : favorites) {
                    if (FavoriteItem.TYPE_YOUTUBE.equals(f.type)) {
                        ids.add(f.itemId);
                    }
                }
            }
            adapter.setFavoriteIds(ids);
        });

        adapter.setOnItemClickListener((v, item, position) -> {
            Intent intent = new Intent(getContext(), com.app.webdroid.activity.ActivityVideoDetail.class);
            intent.putExtra("videoId", item.videoId);
            intent.putExtra("title", item.title);
            String displayDate = item.pubDate != null ? item.pubDate : "";
            if (item.viewCount != null && !item.viewCount.isEmpty()) {
                displayDate = item.viewCount + (!displayDate.isEmpty() ? " • " + displayDate : "");
            }
            if (item.duration != null && !item.duration.isEmpty()) {
                displayDate = "⏱️ " + item.duration + (!displayDate.isEmpty() ? " • " + displayDate : "");
            }
            intent.putExtra("date", displayDate);
            intent.putExtra("thumbUrl", item.thumbnailUrl);
            intent.putExtra("channelName", item.channelName);
            intent.putExtra("channelId", item.getAuthorChannelId());
            startActivity(intent);
        });

        adapter.setOnChannelAddClickListener((v, item, position) -> {
            com.app.webdroid.util.CustomChannelManager.showAddChannelDialog(
                    requireContext(),
                    item.channelName,
                    item.getAuthorChannelId(),
                    item.thumbnailUrl,
                    categoryTitle,
                    () -> adapter.notifyItemChanged(position)
            );
        });

        adapter.setOnFavoriteClickListener((v, item, position) -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                boolean isFav = favoriteDao.isFavorite(item.videoId, FavoriteItem.TYPE_YOUTUBE) > 0;
                if (isFav) {
                    favoriteDao.removeFavorite(item.videoId, FavoriteItem.TYPE_YOUTUBE);
                } else {
                    FavoriteItem fav = new FavoriteItem();
                    fav.itemId = item.videoId;
                    fav.type = FavoriteItem.TYPE_YOUTUBE;
                    fav.title = item.title;
                    fav.subtitle = item.channelName;
                    fav.imageUrl = item.thumbnailUrl;
                    fav.targetUrl = "https://www.youtube.com/watch?v=" + item.videoId;
                    favoriteDao.addFavorite(fav);
                }
            });
        });

        setupToolbar(view); // Setup toolbar

        recyclerView.setAdapter(adapter);

        // Apply saved view mode (default compact, or list, or grid)
        int savedMode = sharedPref.getVideoViewMode();
        setViewMode(savedMode);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (activity != null) {
                    activity.onScroll(dy);
                }
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (!isLoading && (nextPageToken != null || channelContinuationToken != null)) {
                    if (layoutManager != null
                            && layoutManager.findLastCompletelyVisibleItemPosition() == adapter.getItemCount() - 1) {
                        loadNextPage();
                    }
                }
            }
        });

        loadData();

        return view;
    }

    private void setViewMode(int mode) {
        if (adapter == null)
            return;

        if (sharedPref != null) {
            sharedPref.setVideoViewMode(mode);
        }

        adapter.setViewType(mode);
        if (mode == AdapterVideos.VIEW_TYPE_GRID) {
            androidx.recyclerview.widget.GridLayoutManager glm = new androidx.recyclerview.widget.GridLayoutManager(
                    getContext(), 2);
            glm.setSpanSizeLookup(new androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (adapter.getItemViewType(position) == AdapterVideos.VIEW_TYPE_AD) {
                        return 2;
                    }
                    return 1;
                }
            });
            recyclerView.setLayoutManager(glm);
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        // Re-assign adapter to refresh views completely
        recyclerView.setAdapter(adapter);
    }

    private void loadData() {
        if (jsonFilename == null || jsonFilename.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            textError.setVisibility(View.VISIBLE);
            textError.setText("No content provided");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        textError.setVisibility(View.GONE);

        setupBanner(jsonFilename);

        if (jsonFilename.startsWith("http") || jsonFilename.startsWith("UC") || jsonFilename.startsWith("PL") || jsonFilename.startsWith("VL") || jsonFilename.startsWith("search:")) {
            if (jsonFilename.contains("youtube.com/feeds") || jsonFilename.contains("channel_id=") || jsonFilename.contains("playlist_id=") || jsonFilename.startsWith("UC") || jsonFilename.startsWith("PL") || jsonFilename.startsWith("VL") || jsonFilename.startsWith("search:")) {
                // Use local database for YouTube feeds with unique channel or playlist ID or search query
                final String finalCid = extractFeedIdentifier(jsonFilename);
                AppDatabase db = AppDatabase.getDatabase(getContext());
                db.youTubeDao().getVideosByChannelId(finalCid).observe(getViewLifecycleOwner(), items -> {
                    if (items != null && !items.isEmpty()) {
                        adapter.setItems(injectNativeAds(items));
                        progressBar.setVisibility(View.GONE);
                    }

                    if (!isFetched) {
                        isFetched = true;
                        if (items == null || items.isEmpty()) {
                            progressBar.setVisibility(View.VISIBLE);
                        }
                        if (jsonFilename.startsWith("search:")) {
                            fetchSearchVideosInnertube(finalCid, categoryTitle);
                        } else if (jsonFilename.contains("channel_id=") || jsonFilename.contains("playlist_id=") || finalCid.startsWith("UC") || finalCid.startsWith("PL") || finalCid.startsWith("VL")) {
                            fetchChannelVideosInnertube(finalCid, categoryTitle);
                        } else {
                            fetchVideoFeed(jsonFilename);
                        }
                    }
                });
            } else {
                fetchVideoFeed(jsonFilename);
            }
        } else {
            loadFromAssets(jsonFilename);
        }
    }

    public void performUserSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        categoryTitle = "Search: " + query.trim();
        if (toolbarTitle != null) {
            toolbarTitle.setText(categoryTitle);
        }
        jsonFilename = "search:" + query.trim() + "|CAI%3D";
        channelContinuationToken = null;
        isFetched = false;
        if (adapter != null) {
            adapter.setItems(new ArrayList<>());
        }
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (textError != null) textError.setVisibility(View.GONE);
        if (layoutChannelBanner != null) layoutChannelBanner.setVisibility(View.GONE);
        setupBanner(jsonFilename);
        fetchSearchVideosInnertube(jsonFilename, categoryTitle);
    }

    private void fetchSearchVideosInnertube(String searchSpec, String name) {
        isLoading = true;
        new Thread(() -> {
            try {
                String spec = searchSpec.startsWith("search:") ? searchSpec.substring(7) : searchSpec;
                String query = spec;
                String params = null;
                if (spec.contains("|")) {
                    String[] parts = spec.split("\\|", 2);
                    query = parts[0];
                    params = parts[1];
                }

                com.app.webdroid.util.YouTubeInnertubeFetcher.FetchResult result =
                        com.app.webdroid.util.YouTubeInnertubeFetcher.searchVideosWithContinuation(query, params, null, 3);
                if (result != null && result.items != null && !result.items.isEmpty()) {
                    channelContinuationToken = result.nextContinuationToken;
                    for (YouTubeItem it : result.items) {
                        it.channelId = searchSpec;
                    }
                    AppDatabase db = AppDatabase.getDatabase(getContext());
                    if (db != null) {
                        db.youTubeDao().insertVideos(result.items);
                    }
                    updateUi(result.items);

                    // Show Top Channel Banner ONLY if an actual YouTube channel was matched
                    String chName = result.matchedChannelTitle;
                    String chId = result.matchedChannelId;
                    String chThumb = result.matchedChannelThumb;

                    if (chName != null && !chName.isEmpty() && getActivity() != null) {
                        final String finalChName = chName;
                        final String finalChId = chId;
                        final String finalChThumb = chThumb;
                        getActivity().runOnUiThread(() -> {
                            if (layoutChannelBanner != null) {
                                layoutChannelBanner.setVisibility(View.VISIBLE);
                                if (tvChannelBannerName != null) {
                                    tvChannelBannerName.setText(finalChName);
                                }
                                if (imgChannelBanner != null) {
                                    imgChannelBanner.setColorFilter(null);
                                    imgChannelBanner.setPadding(0, 0, 0, 0);
                                    if (finalChThumb != null && !finalChThumb.isEmpty()) {
                                        try {
                                            com.bumptech.glide.Glide.with(FragmentVideoList.this)
                                                    .load(finalChThumb)
                                                    .placeholder(R.drawable.ic_live_tv)
                                                    .circleCrop()
                                                    .into(imgChannelBanner);
                                        } catch (Exception ignored) {}
                                    } else {
                                        imgChannelBanner.setImageResource(R.drawable.ic_live_tv);
                                    }
                                }
                                if (btnAddChannelBanner != null) {
                                    boolean isAdded = com.app.webdroid.util.CustomChannelManager.isChannelAddedAnywhere(getContext(), finalChName);
                                    if (isAdded) {
                                        btnAddChannelBanner.setText("✓ Added");
                                        btnAddChannelBanner.setEnabled(false);
                                        btnAddChannelBanner.setAlpha(0.6f);
                                    } else {
                                        btnAddChannelBanner.setText("+ Add Channel");
                                        btnAddChannelBanner.setEnabled(true);
                                        btnAddChannelBanner.setAlpha(1.0f);
                                    }
                                    btnAddChannelBanner.setOnClickListener(bv -> {
                                        com.app.webdroid.util.CustomChannelManager.showAddChannelDialog(
                                                requireContext(),
                                                finalChName,
                                                finalChId,
                                                finalChThumb,
                                                categoryTitle,
                                                () -> {
                                                    btnAddChannelBanner.setText("✓ Added");
                                                    btnAddChannelBanner.setEnabled(false);
                                                    btnAddChannelBanner.setAlpha(0.6f);
                                                    adapter.notifyDataSetChanged();
                                                }
                                        );
                                    });
                                }
                            }
                        });
                    } else if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (layoutChannelBanner != null) {
                                layoutChannelBanner.setVisibility(View.GONE);
                            }
                        });
                    }
                } else {
                    showError("No videos found");
                }
            } catch (Exception e) {
                Log.e("FragmentVideoList", "Innertube search error for " + searchSpec, e);
                showError("Error searching videos");
            } finally {
                isLoading = false;
            }
        }).start();
    }

    private void fetchChannelVideosInnertube(String channelId, String name) {
        isLoading = true;
        new Thread(() -> {
            try {
                com.app.webdroid.util.YouTubeInnertubeFetcher.FetchResult result =
                        com.app.webdroid.util.YouTubeInnertubeFetcher.fetchChannelVideosWithContinuation(channelId, name, null, 5);
                if (result != null && result.items != null && !result.items.isEmpty()) {
                    channelContinuationToken = result.nextContinuationToken;
                    AppDatabase db = AppDatabase.getDatabase(getContext());
                    if (db != null) {
                        db.youTubeDao().insertVideos(result.items);
                    }
                    updateUi(result.items);
                } else {
                    fetchVideoFeed(jsonFilename);
                }
            } catch (Exception e) {
                Log.e("FragmentVideoList", "Innertube fetch error for " + channelId, e);
                fetchVideoFeed(jsonFilename);
            } finally {
                isLoading = false;
            }
        }).start();
    }

    private void setupToolbar(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        toolbarTitle = view.findViewById(R.id.toolbar_title);

        if (toolbar == null)
            return;

        toolbar.setTitle("");
        toolbarTitle.setText(categoryTitle != null ? categoryTitle : "");

        if (activity != null) {
            activity.setSupportActionBar(toolbar);
            if (sharedPref.getNavigationDrawer()) {
                activity.setupNavigationDrawer(toolbar);
            } else {
                // Back button if no drawer? or default
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        if (sharedPref.getToolbar()) {
            toolbar.setVisibility(View.VISIBLE);
        } else {
            toolbar.setVisibility(View.GONE);
        }

        if (sharedPref.getIsDarkTheme()) {
            toolbar.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_dark_toolbar));
            toolbar.getContext().setTheme(androidx.appcompat.R.style.ThemeOverlay_AppCompat_Dark);
            toolbarTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.color_dark_title_toolbar));
        } else {
            toolbar.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_light_primary));
            toolbar.setPopupTheme(androidx.appcompat.R.style.ThemeOverlay_AppCompat_Light);
            toolbarTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.color_light_title_toolbar));
        }
    }

    private void loadFromAssets(String filename) {
        if (getContext() == null) return;
        final android.content.res.AssetManager assetManager = requireContext().getApplicationContext().getAssets();
        new Thread(() -> {
            try {
                InputStream is = assetManager.open(filename);
                java.io.Reader reader = new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8);
                List<YouTubeItem> items = new Gson().fromJson(reader, new TypeToken<List<YouTubeItem>>() {
                }.getType());
                reader.close();

                updateUi(items);

                // Live Update: Query live RSS for latest 15 videos and merge newly uploaded episodes to the top!
                if (filename.startsWith("playlists/") && filename.endsWith(".json")) {
                    String pid = filename.substring(filename.lastIndexOf("/") + 1).replace(".json", "");
                    if (pid.startsWith("PL")) {
                        fetchLiveRssUpdates("https://www.youtube.com/feeds/videos.xml?playlist_id=" + pid, items);
                    }
                }

            } catch (Exception e) {
                android.util.Log.e("FragmentVideoList", "loadFromAssets error: " + filename, e);
                showError(e.getMessage());
            }
        }).start();
    }

    private void fetchLiveRssUpdates(String url, List<YouTubeItem> existingItems) {
        new Thread(() -> {
            List<YouTubeItem> liveItems = new ArrayList<>();
            String pid = extractPlaylistId(url);
            if (pid != null && !pid.isEmpty()) {
                try {
                    com.app.webdroid.util.YouTubeInnertubeFetcher.FetchResult res =
                            com.app.webdroid.util.YouTubeInnertubeFetcher.fetchChannelVideosWithContinuation(pid, categoryTitle, null, 1);
                    if (res != null && res.items != null && !res.items.isEmpty()) {
                        liveItems.addAll(res.items);
                    }
                } catch (Exception ignored) {}
            }

            if (liveItems.isEmpty()) {
                InputStream inputStream = null;
                try {
                    java.net.URL feedUrl = new java.net.URL(url);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) feedUrl.openConnection();
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36");
                    if (conn.getResponseCode() == 200) {
                        inputStream = conn.getInputStream();
                    }
                } catch (Exception ignored) {}

                // Invidious fallback if YouTube blocks
                if (inputStream == null && url != null && url.contains("playlist_id=")) {
                    try {
                        if (pid != null && !pid.isEmpty()) {
                            String mirrorUrl = "https://invidious.nerdvpn.de/feed/playlist/" + pid;
                            java.net.URL mUrl = new java.net.URL(mirrorUrl);
                            java.net.HttpURLConnection mConn = (java.net.HttpURLConnection) mUrl.openConnection();
                            mConn.setConnectTimeout(6000);
                            mConn.setReadTimeout(6000);
                            mConn.setInstanceFollowRedirects(true);
                            mConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                            if (mConn.getResponseCode() == 200) {
                                inputStream = mConn.getInputStream();
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (inputStream != null) {
                    try {
                        org.xmlpull.v1.XmlPullParser parser = android.util.Xml.newPullParser();
                        parser.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                        parser.setInput(inputStream, null);
                        parser.nextTag();

                        parser.require(org.xmlpull.v1.XmlPullParser.START_TAG, null, "feed");
                        while (parser.next() != org.xmlpull.v1.XmlPullParser.END_TAG) {
                            if (parser.getEventType() != org.xmlpull.v1.XmlPullParser.START_TAG) continue;
                            String name = parser.getName();
                            if (name.equals("entry")) {
                                try {
                                    YouTubeItem item = readEntry(parser);
                                    if (item != null && item.videoId != null) {
                                        liveItems.add(item);
                                    }
                                } catch (Exception ignored) {}
                            } else {
                                skip(parser);
                            }
                        }
                        inputStream.close();
                    } catch (Exception ignored) {}
                }
            }

            if (!liveItems.isEmpty()) {
                java.util.Set<String> existingIds = new java.util.HashSet<>();
                if (existingItems != null) {
                    for (YouTubeItem it : existingItems) {
                        if (it.videoId != null) existingIds.add(it.videoId);
                    }
                }

                List<YouTubeItem> newlyUploaded = new ArrayList<>();
                for (YouTubeItem live : liveItems) {
                    if (live.videoId != null && !existingIds.contains(live.videoId)) {
                        newlyUploaded.add(live);
                    }
                }

                // If newly uploaded episodes exist, prepend them right at the top!
                if (!newlyUploaded.isEmpty()) {
                    List<YouTubeItem> mergedList = new ArrayList<>(newlyUploaded);
                    if (existingItems != null) {
                        mergedList.addAll(existingItems);
                    }
                    updateUi(mergedList);
                }
            }
        }).start();
    }

    private void loadNextPage() {
        if (channelContinuationToken != null && !channelContinuationToken.isEmpty()) {
            final String finalCid = extractFeedIdentifier(jsonFilename);
            isLoading = true;
            new Thread(() -> {
                try {
                    com.app.webdroid.util.YouTubeInnertubeFetcher.FetchResult result;
                    if (finalCid != null && finalCid.startsWith("search:")) {
                        String spec = finalCid.substring(7);
                        String query = spec;
                        String params = null;
                        if (spec.contains("|")) {
                            String[] parts = spec.split("\\|", 2);
                            query = parts[0];
                            params = parts[1];
                        }
                        result = com.app.webdroid.util.YouTubeInnertubeFetcher.searchVideosWithContinuation(query, params, channelContinuationToken, 2);
                        if (result != null && result.items != null) {
                            for (YouTubeItem it : result.items) {
                                it.channelId = finalCid;
                            }
                        }
                    } else {
                        result = com.app.webdroid.util.YouTubeInnertubeFetcher.fetchChannelVideosWithContinuation(finalCid, categoryTitle, channelContinuationToken, 3);
                    }
                    if (result != null && result.items != null && !result.items.isEmpty()) {
                        channelContinuationToken = result.nextContinuationToken;
                        AppDatabase db = AppDatabase.getDatabase(getContext());
                        if (db != null) {
                            db.youTubeDao().insertVideos(result.items);
                        }
                    } else {
                        channelContinuationToken = null;
                    }
                } catch (Exception e) {
                    Log.e("FragmentVideoList", "Error loading next page", e);
                } finally {
                    isLoading = false;
                }
            }).start();
            return;
        }

        if (jsonFilename != null && nextPageToken != null) {
            String url = jsonFilename;
            if (url.contains("pageToken=")) {
                url = url.replaceAll("pageToken=[^&]*", "pageToken=" + nextPageToken);
            } else {
                url += "&pageToken=" + nextPageToken;
            }
            fetchVideoFeed(url);
        }
    }

    private void fetchVideoFeed(String url) {
        isLoading = true;
        new Thread(() -> {
            InputStream inputStream = null;

            // 1. Try Invidious mirror directly for YouTube channel feeds (since Google edge CDN returns 404 in India)
            if (url != null && url.contains("channel_id=")) {
                String cid = extractFeedIdentifier(url);
                if (cid != null && !cid.isEmpty()) {
                    try {
                        String invidiousUrl = "https://inv.nadeko.net/feed/channel/" + cid;
                        java.net.URL feedUrl = new java.net.URL(invidiousUrl);
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) feedUrl.openConnection();
                        conn.setConnectTimeout(8000);
                        conn.setReadTimeout(8000);
                        conn.setInstanceFollowRedirects(true);
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                        if (conn.getResponseCode() == 200) {
                            inputStream = conn.getInputStream();
                        }
                    } catch (Exception ignored) {}
                }
            }

            // 2. Fallback to original URL
            if (inputStream == null) {
                try {
                    Log.d("FragmentVideoList", "Fetching URL: " + url);
                    java.net.URL feedUrl = new java.net.URL(url);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) feedUrl.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent",
                            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36");
                    if (conn.getResponseCode() == 200) {
                        inputStream = conn.getInputStream();
                    }
                } catch (Exception ignored) {}
            }

            // 3. Fallback mirrors if still null
            if (inputStream == null && url != null) {
                try {
                    String mirrorUrl = null;
                    if (url.contains("channel_id=")) {
                        String cid = extractFeedIdentifier(url);
                        if (cid != null && !cid.isEmpty()) {
                            mirrorUrl = "https://invidious.nerdvpn.de/feed/channel/" + cid;
                        }
                    } else if (url.contains("playlist_id=")) {
                        String pid = extractPlaylistId(url);
                        if (pid != null && !pid.isEmpty()) {
                            mirrorUrl = "https://invidious.nerdvpn.de/feed/playlist/" + pid;
                        }
                    }
                    if (mirrorUrl != null) {
                        java.net.URL mUrl = new java.net.URL(mirrorUrl);
                        java.net.HttpURLConnection mConn = (java.net.HttpURLConnection) mUrl.openConnection();
                        mConn.setConnectTimeout(8000);
                        mConn.setReadTimeout(8000);
                        mConn.setInstanceFollowRedirects(true);
                        mConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                        if (mConn.getResponseCode() == 200) {
                            inputStream = mConn.getInputStream();
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (inputStream == null) {
                showError("Feed is temporarily updating.");
                isLoading = false;
                return;
            }

            try {
                List<YouTubeItem> items = new ArrayList<>();
                boolean isJson = url.contains("googleapis.com") || url.contains("v3") || url.contains("alt=json");

                if (isJson) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JsonObject jsonObject = new Gson().fromJson(sb.toString(), JsonObject.class);
                    if (jsonObject.has("nextPageToken")) {
                        nextPageToken = jsonObject.get("nextPageToken").getAsString();
                    } else {
                        nextPageToken = null;
                    }

                    if (jsonObject.has("items")) {
                        JsonArray jsonArray = jsonObject.getAsJsonArray("items");
                        for (JsonElement element : jsonArray) {
                            JsonObject obj = element.getAsJsonObject();
                            YouTubeItem item = new YouTubeItem();
                            JsonObject snippet = obj.getAsJsonObject("snippet");
                            item.title = snippet.get("title").getAsString();
                            item.description = snippet.get("description").getAsString();
                            item.pubDate = snippet.get("publishedAt").getAsString();
                            item.thumbnailUrl = snippet.getAsJsonObject("thumbnails").getAsJsonObject("medium")
                                    .get("url").getAsString();
                            item.channelName = snippet.get("channelTitle").getAsString();

                            if (snippet.has("resourceId")) {
                                item.videoId = snippet.getAsJsonObject("resourceId").get("videoId").getAsString();
                            } else if (obj.has("id") && obj.get("id").isJsonObject()
                                    && obj.getAsJsonObject("id").has("videoId")) {
                                item.videoId = obj.getAsJsonObject("id").get("videoId").getAsString();
                            }

                            items.add(item);
                        }
                    }
                } else {
                    android.util.Xml.Encoding encoding = android.util.Xml.Encoding.UTF_8;
                    org.xmlpull.v1.XmlPullParser parser = android.util.Xml.newPullParser();
                    parser.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                    parser.setInput(inputStream, null);
                    parser.nextTag();

                    String feedTitle = "";

                    parser.require(org.xmlpull.v1.XmlPullParser.START_TAG, null, "feed");
                    while (parser.next() != org.xmlpull.v1.XmlPullParser.END_TAG) {
                        if (parser.getEventType() != org.xmlpull.v1.XmlPullParser.START_TAG) {
                            continue;
                        }
                        String name = parser.getName();
                        if (name.equals("title")) { // Capture feed title
                            try {
                                feedTitle = readText(parser);
                            } catch (Exception e) {
                                Log.e("FragmentVideoList", "Error reading title", e);
                            }
                        } else if (name.equals("entry")) {
                            try {
                                YouTubeItem item = readEntry(parser);
                                if (item != null && item.videoId != null) {
                                    items.add(item);
                                }
                            } catch (Exception e) {
                                Log.e("FragmentVideoList", "Error parsing entry", e);
                            }
                        } else {
                            skip(parser);
                        }
                    }

                    // Set channel name
                    if (feedTitle != null && !feedTitle.isEmpty()) {
                        for (YouTubeItem item : items) {
                            item.channelName = feedTitle;
                        }
                    }
                    nextPageToken = null; // XML feeds don't support pagination usually
                }

                if (!isJson)
                    inputStream.close();

                Log.d("FragmentVideoList", "Parsed items count: " + items.size());
                updateUi(items);

            } catch (Exception e) {
                Log.e("FragmentVideoList", "Fetch Error", e);
                showError("Feed is temporarily updating.");
                e.printStackTrace();
            } finally {
                isLoading = false;
            }
        }).start();
    }

    private YouTubeItem readEntry(org.xmlpull.v1.XmlPullParser parser)
            throws org.xmlpull.v1.XmlPullParserException, IOException {
        parser.require(org.xmlpull.v1.XmlPullParser.START_TAG, null, "entry");
        String title = null;
        String videoId = null;
        String desc = "";
        String thumb = "";
        String date = "";

        String duration = "";
        String viewCount = "";

        while (parser.next() != org.xmlpull.v1.XmlPullParser.END_TAG) {
            if (parser.getEventType() != org.xmlpull.v1.XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("title")) {
                title = readText(parser);
            } else if (name.equals("yt:videoId") || name.equals("videoId")) {
                videoId = readText(parser);
                thumb = "https://i.ytimg.com/vi/" + videoId + "/mqdefault.jpg";
            } else if (name.equals("published")) {
                date = readText(parser);
            } else if (name.equals("yt:duration")) {
                String secStr = parser.getAttributeValue(null, "seconds");
                if (secStr != null) {
                    try {
                        int secs = Integer.parseInt(secStr);
                        if (secs > 0) {
                            int m = secs / 60;
                            int s = secs % 60;
                            int h = m / 60;
                            m = m % 60;
                            duration = h > 0 ? String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s) : String.format(java.util.Locale.US, "%d:%02d", m, s);
                        }
                    } catch (Exception ignored) {}
                }
                skip(parser);
            } else if (name.equals("media:group")) {
                while (parser.next() != org.xmlpull.v1.XmlPullParser.END_TAG) {
                    if (parser.getEventType() != org.xmlpull.v1.XmlPullParser.START_TAG)
                        continue;
                    String gName = parser.getName();
                    if (gName.equals("media:title")) {
                        title = readText(parser);
                    } else if (gName.equals("media:description")) {
                        desc = readText(parser);
                    } else if (gName.equals("media:thumbnail")) {
                        String url = parser.getAttributeValue(null, "url");
                        if (url != null)
                            thumb = url;
                        skip(parser);
                    } else if (gName.equals("yt:duration")) {
                        String secStr = parser.getAttributeValue(null, "seconds");
                        if (secStr != null) {
                            try {
                                int secs = Integer.parseInt(secStr);
                                if (secs > 0) {
                                    int m = secs / 60;
                                    int s = secs % 60;
                                    int h = m / 60;
                                    m = m % 60;
                                    duration = h > 0 ? String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s) : String.format(java.util.Locale.US, "%d:%02d", m, s);
                                }
                            } catch (Exception ignored) {}
                        }
                        skip(parser);
                    } else if (gName.equals("media:community")) {
                        while (parser.next() != org.xmlpull.v1.XmlPullParser.END_TAG) {
                            if (parser.getEventType() != org.xmlpull.v1.XmlPullParser.START_TAG) continue;
                            String cName = parser.getName();
                            if (cName.equals("media:statistics")) {
                                String vStr = parser.getAttributeValue(null, "views");
                                if (vStr != null) {
                                    try {
                                        long v = Long.parseLong(vStr);
                                        if (v >= 1_000_000) {
                                            viewCount = String.format(java.util.Locale.US, "%.1fM views", v / 1_000_000.0).replace(".0M", "M");
                                        } else if (v >= 1_000) {
                                            viewCount = String.format(java.util.Locale.US, "%.1fK views", v / 1_000.0).replace(".0K", "K");
                                        } else {
                                            viewCount = v + " views";
                                        }
                                    } catch (Exception ignored) {}
                                }
                                skip(parser);
                            } else {
                                skip(parser);
                            }
                        }
                    } else {
                        skip(parser);
                    }
                }
            } else {
                skip(parser);
            }
        }

        YouTubeItem item = new YouTubeItem();
        item.title = title;
        item.videoId = videoId;
        item.description = desc;
        item.thumbnailUrl = thumb;
        item.pubDate = date;
        item.duration = duration != null ? duration : "";
        item.viewCount = viewCount != null ? viewCount : "";
        return item;
    }

    private String readText(org.xmlpull.v1.XmlPullParser parser)
            throws IOException, org.xmlpull.v1.XmlPullParserException {
        String result = "";
        if (parser.next() == org.xmlpull.v1.XmlPullParser.TEXT) {
            result = parser.getText().trim();
            parser.nextTag();
        }
        return result;
    }

    private void skip(org.xmlpull.v1.XmlPullParser parser) throws org.xmlpull.v1.XmlPullParserException, IOException {
        if (parser.getEventType() != org.xmlpull.v1.XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case org.xmlpull.v1.XmlPullParser.END_TAG:
                    depth--;
                    break;
                case org.xmlpull.v1.XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private void updateUi(List<YouTubeItem> items) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (items != null && !items.isEmpty()) {
                    if (jsonFilename != null && jsonFilename.contains("youtube.com/feeds")) {
                        // Extract unique identifier (playlist ID or channel ID)
                        String channelId = extractFeedIdentifier(jsonFilename);

                        // Set missing fields
                        for (YouTubeItem it : items) {
                            if (it.channelId == null)
                                it.channelId = channelId;
                            if (it.fetchedAt == 0)
                                it.fetchedAt = System.currentTimeMillis();
                            if (it.pubDateMillis == 0 && it.pubDate != null) {
                                // Try parsing YouTube RSS format: 2026-01-20T12:00:00+00:00
                                try {
                                    // ISO 8601
                                    java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(it.pubDate);
                                    it.pubDateMillis = odt.toInstant().toEpochMilli();
                                } catch (Exception e) {
                                    try {
                                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                                                "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
                                        java.util.Date d = sdf.parse(it.pubDate);
                                        if (d != null)
                                            it.pubDateMillis = d.getTime();
                                    } catch (Exception e2) {
                                        try {
                                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                                                    "yyyy-MM-dd", java.util.Locale.US);
                                            java.util.Date d = sdf.parse(it.pubDate);
                                            if (d != null)
                                                it.pubDateMillis = d.getTime();
                                        } catch (Exception e3) {
                                        }
                                    }
                                }
                            }
                            if (it.pubDateMillis == 0)
                                it.pubDateMillis = System.currentTimeMillis();
                        }

                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            AppDatabase.getDatabase(getContext()).youTubeDao().insertVideos(items);
                        });
                    }

                    adapter.setItems(injectNativeAds(items));
                } else {
                    if (adapter.getItemCount() == 0) {
                        textError.setVisibility(View.VISIBLE);
                        textError.setText("No videos found");
                    }
                }
            });
        }
    }

    private List<YouTubeItem> injectNativeAds(List<YouTubeItem> items) {
        if (getContext() == null || items == null)
            return items;

        com.app.webdroid.database.prefs.AdsPref adsPref = new com.app.webdroid.database.prefs.AdsPref(getContext());
        if (!adsPref.getAdStatus())
            return items;

        // Interval: every 10th item (safe density)
        int interval = Math.max(10, adsPref.getNativeAdIndex());
        List<YouTubeItem> newItems = new ArrayList<>();
        int count = 0;
        for (YouTubeItem item : items) {
            if (item.isNativeAd)
                continue; // Avoid double injection
            newItems.add(item);
            count++;
            if (count % interval == 0) {
                YouTubeItem adItem = new YouTubeItem();
                adItem.isNativeAd = true;
                newItems.add(adItem);
            }
        }
        return newItems;
    }

    private void showError(String msg) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (adapter != null && adapter.getItemCount() > 0) {
                    return; // Preserve existing cached videos
                }
                textError.setVisibility(View.VISIBLE);
                textError.setText("Videos are temporarily updating.\nTap the banner above to watch all videos on YouTube.");
            });
        }
    }

    private String extractFeedIdentifier(String url) {
        if (url == null) return "unknown";
        if (url.startsWith("search:")) {
            return url;
        }
        if ((url.startsWith("UC") || url.startsWith("PL") || url.startsWith("VL")) && !url.contains("/") && !url.contains("?")) {
            return url;
        }
        if (url.contains("playlist_id=")) {
            String pid = extractPlaylistId(url);
            if (pid != null && !pid.isEmpty()) return pid;
        } else if (url.contains("channel_id=")) {
            try {
                android.net.Uri uri = android.net.Uri.parse(url);
                String cid = uri.getQueryParameter("channel_id");
                if (cid != null && !cid.isEmpty()) return cid;
            } catch (Exception ignored) {}
            int idx = url.indexOf("channel_id=");
            String sub = url.substring(idx + 11);
            if (sub.contains("&")) sub = sub.substring(0, sub.indexOf("&"));
            return sub;
        }
        try {
            return android.net.Uri.parse(url).getLastPathSegment();
        } catch (Exception e) {
            return "feed_" + Math.abs(url.hashCode());
        }
    }

    private String extractPlaylistId(String url) {
        if (url == null || !url.contains("playlist_id=")) return null;
        try {
            android.net.Uri uri = android.net.Uri.parse(url);
            String pid = uri.getQueryParameter("playlist_id");
            if (pid != null && !pid.isEmpty()) return pid;
        } catch (Exception ignored) {}
        int idx = url.indexOf("playlist_id=");
        String sub = url.substring(idx + 12);
        if (sub.contains("&")) sub = sub.substring(0, sub.indexOf("&"));
        return sub;
    }

    private void openYouTubeUrl(String url) {
        if (url == null || url.isEmpty() || getContext() == null) return;
        try {
            android.content.Intent appIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));
            appIntent.setPackage("com.google.android.youtube");
            startActivity(appIntent);
        } catch (Exception ex) {
            try {
                android.content.Intent webIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));
                startActivity(webIntent);
            } catch (Exception ignored) {}
        }
    }

    private void setupBanner(String source) {
        if (layoutPlaylistBanner == null) return;
        String bannerUrl = getPlaylistOrChannelUrl(source, categoryTitle);
        if (bannerUrl != null && !bannerUrl.isEmpty()) {
            layoutPlaylistBanner.setVisibility(View.VISIBLE);
            if (bannerUrl.contains("results?search_query=")) {
                tvPlaylistBannerTitle.setText("Watch More on YouTube");
            } else if (bannerUrl.contains("playlist?list=")) {
                tvPlaylistBannerTitle.setText("Watch All 1000+ Episodes on YouTube");
            } else {
                tvPlaylistBannerTitle.setText("Watch " + (categoryTitle != null && !categoryTitle.isEmpty() ? categoryTitle : "More") + " on YouTube");
            }
            layoutPlaylistBanner.setOnClickListener(v -> openYouTubeUrl(bannerUrl));
        } else {
            layoutPlaylistBanner.setVisibility(View.GONE);
        }
    }

    private String getPlaylistOrChannelUrl(String source, String title) {
        if (source == null) source = "";
        if (title == null) title = "";
        String s = source.toLowerCase();
        String t = title.toLowerCase();

        if (source.startsWith("search:")) {
            String q = source.substring(7);
            if (q.contains("|")) q = q.split("\\|")[0];
            try {
                return "https://www.youtube.com/results?search_query=" + java.net.URLEncoder.encode(q, "UTF-8");
            } catch (Exception ignored) {
                return "https://www.youtube.com/results?search_query=" + q;
            }
        }

        if (source.contains("playlist_id=")) {
            String pid = extractPlaylistId(source);
            if (pid != null && !pid.isEmpty()) return "https://www.youtube.com/playlist?list=" + pid;
        }
        if (source.contains("channel_id=")) {
            String cid = extractFeedIdentifier(source);
            if (cid != null && !cid.isEmpty() && !cid.equals("unknown")) return "https://www.youtube.com/channel/" + cid + "/videos";
        }
        if (source.startsWith("PL") || source.startsWith("VL")) {
            String cleanPid = source.startsWith("VL") ? source.substring(2) : source;
            return "https://www.youtube.com/playlist?list=" + cleanPid;
        }
        if (source.startsWith("UC") && !source.contains("/")) {
            return "https://www.youtube.com/channel/" + source + "/videos";
        }

        if (s.contains("marimayam") || t.contains("marimayam")) {
            return "https://www.youtube.com/playlist?list=PLQNCaDxtvXKRFGSoP9eQIgThMBnOadHyO";
        } else if (s.contains("uppum") || t.contains("uppum")) {
            return "https://www.youtube.com/playlist?list=PLlCMxD9I3oIlbCMt5bw3tz16LG_3lGh3Y";
        } else if (s.contains("thatteem") || t.contains("thatteem")) {
            return "https://www.youtube.com/playlist?list=PLQNCaDxtvXKSfKc8-v-WA-qZQ3EM155F4";
        } else if (s.contains("chakkappazham") || t.contains("chakkappazham")) {
            return "https://www.youtube.com/playlist?list=PLo6nvUgrCYly1-xEGBNu4E2cqnFLIoEXW";
        } else if (s.contains("comedy_stars") || t.contains("comedy stars")) {
            return "https://www.youtube.com/playlist?list=PL06gwaCm769I83Aw9T7WkfZyioIpdz1oz";
        } else if (s.contains("top_singer") || t.contains("top singer")) {
            return "https://www.youtube.com/playlist?list=PLo6nvUgrCYly_IxKn0Xh32zdQn6nWped4";
        } else if (s.contains("star_singer") || t.contains("star singer")) {
            return "https://www.youtube.com/playlist?list=PLS6GOsr8ulfVuKwcoE83OqzmhsVXHtxMd";
        } else if (s.contains("badai") || t.contains("badai")) {
            return "https://www.youtube.com/playlist?list=PLOUsdOhe9vrH_IhgXHV6QAEZTzIMpCR7C";
        } else if (s.contains("karikku") || t.contains("karikku")) {
            return "https://www.youtube.com/playlist?list=PLtV_yvBUCcokb_tGMWTMLjJrHFNInB8WM";
        } else if (s.contains("akkarakazhchakal") || t.contains("akkarakazhchakal")) {
            return "https://www.youtube.com/playlist?list=PLyr-328vWiQ3hZK1q-Hq0P5_XWnocDCoy";
        } else if (s.contains("veena") || t.contains("veena")) {
            return "https://www.youtube.com/channel/UCugwC12zllpi9zJoUP5ZVyw";
        } else if (s.contains("shaan") || t.contains("shaan")) {
            return "https://www.youtube.com/channel/UC75_5G5PZgO9L5XpQ5tD8oA";
        } else if (s.contains("travel") || t.contains("sujith") || t.contains("tech travel eat")) {
            return "https://www.youtube.com/channel/UCeoRAN5sr02w8_9aFWxIM4g";
        } else if (s.contains("asianet") || t.contains("asianet")) {
            return "https://www.youtube.com/channel/UCf8w5m0YsRa8MHQ5bwSGmbw";
        } else if (s.contains("manorama") || t.contains("manorama")) {
            return "https://www.youtube.com/channel/UCP0uG-mcMImgKnJz-VjJZmQ";
        }
        return null;
    }
}
