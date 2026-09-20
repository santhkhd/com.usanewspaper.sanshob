package com.app.webdroid.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.shobmc.san.R;
import com.app.webdroid.adapter.AdapterVideos;
import com.app.webdroid.database.AppDatabase;
import com.app.webdroid.model.YouTubeItem;
import com.app.webdroid.worker.SyncWorker;
import java.util.ArrayList;
import java.util.List;

public class FragmentVideos extends Fragment {

    private RecyclerView recyclerView;
    private AdapterVideos adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private android.widget.ProgressBar progressBar;
    private boolean isFetched = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videos, container, false);

        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar_videos);
        if (toolbar != null) {
            com.app.webdroid.database.prefs.SharedPref sharedPref = new com.app.webdroid.database.prefs.SharedPref(requireContext());
            if (sharedPref.getIsDarkTheme()) {
                toolbar.setBackgroundColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.color_dark_toolbar));
                toolbar.setTitleTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.white));
            } else {
                toolbar.setBackgroundColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.color_light_primary));
                toolbar.setTitleTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.color_light_title_toolbar));
            }
        }

        recyclerView = view.findViewById(R.id.recycler_view_videos);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_videos);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdapterVideos(getContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener((v, obj, position) -> {
            if (getActivity() instanceof com.app.webdroid.activity.MainActivity) {
                ((com.app.webdroid.activity.MainActivity) getActivity()).showInterstitialAdForListItem(() -> {
                    Intent intent = new Intent(getContext(), com.app.webdroid.activity.ActivityVideoDetail.class);
                    intent.putExtra("videoId", obj.videoId);
                    intent.putExtra("title", obj.title);
                    String timeText = "";
                    CharSequence timeAgo = com.app.webdroid.util.Tools.getTimeAgo(obj.pubDateMillis);
                    if (timeAgo != null && timeAgo.length() > 0) {
                        timeText = timeAgo.toString();
                    } else if (obj.pubDate != null && !obj.pubDate.trim().isEmpty()) {
                        timeText = obj.pubDate.trim();
                    }
                    String viewsText = "";
                    if (obj.viewCount != null && !obj.viewCount.trim().isEmpty()) {
                        viewsText = obj.viewCount.trim();
                        if (!viewsText.toLowerCase().contains("view")) {
                            viewsText = viewsText + " views";
                        }
                    }
                    String displayDate = (!viewsText.isEmpty() && !timeText.isEmpty()) ? (viewsText + " • " + timeText) : (!viewsText.isEmpty() ? viewsText : timeText);
                    intent.putExtra("date", displayDate);
                    intent.putExtra("thumbUrl", obj.thumbnailUrl);
                    intent.putExtra("channelName", obj.channelName);
                    intent.putExtra("channelId", obj.getAuthorChannelId());
                    startActivity(intent);
                });
            }
        });

        progressBar = view.findViewById(R.id.progress_videos);
        TextView textEmpty = view.findViewById(R.id.text_empty);
        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton btnFullChannel = view.findViewById(R.id.btn_full_channel);

        AppDatabase db = AppDatabase.getDatabase(getContext());

        final androidx.lifecycle.MutableLiveData<Long> filterDuration = new androidx.lifecycle.MutableLiveData<>(0L); // 0 = All

        String cidArg = null;
        if (getArguments() != null) {
            String url = getArguments().getString("url");
            if (url != null && !url.equals("ALL_VIDEOS") && !url.isEmpty()) {
                cidArg = url;
            }
        }

        final String activeChannelId;
        final String activeChannelName = (getArguments() != null) ? getArguments().getString("name") : null;

        if (cidArg != null) {
            // Specific Channel
            if (activeChannelName != null && toolbar != null) {
                toolbar.setTitle(activeChannelName);
            }

            // Extract Channel ID robustly
            String channelId = "";
            if (cidArg.contains("channel_id=")) {
                android.net.Uri uri = android.net.Uri.parse(cidArg);
                channelId = uri.getQueryParameter("channel_id");
                if (channelId == null) {
                    int index = cidArg.indexOf("channel_id=");
                    channelId = cidArg.substring(index + 11);
                    if (channelId.contains("&")) {
                        channelId = channelId.substring(0, channelId.indexOf("&"));
                    }
                }
            } else if (cidArg.startsWith("http")) {
                channelId = android.net.Uri.parse(cidArg).getLastPathSegment();
            } else {
                channelId = cidArg;
            }

            activeChannelId = channelId;

            if (toolbar != null) {
                toolbar.inflateMenu(R.menu.menu_channel_videos);
                toolbar.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.menu_open_youtube) {
                        openFullChannelOnYouTube(activeChannelId);
                        return true;
                    }
                    return false;
                });
            }

            if (btnFullChannel != null) {
                btnFullChannel.setVisibility(View.VISIBLE);
                btnFullChannel.setOnClickListener(v -> openFullChannelOnYouTube(activeChannelId));
            }

            db.youTubeDao().getVideosByChannelId(activeChannelId).observe(getViewLifecycleOwner(), items -> {
                adapter.setItems(injectNativeAds(items));
                swipeRefreshLayout.setRefreshing(false);

                if (items != null && !items.isEmpty()) {
                    textEmpty.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                }

                if (!isFetched) {
                    isFetched = true;
                    if (items == null || items.isEmpty()) {
                        textEmpty.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.VISIBLE);
                    }
                    syncSpecificChannel(activeChannelId, activeChannelName);
                }
            });
        } else {
            activeChannelId = null;
            // All Videos / Latest Videos
            if (btnFullChannel != null) {
                btnFullChannel.setVisibility(View.GONE);
            }
            if (toolbar != null) {
                toolbar.inflateMenu(R.menu.menu_fragment_videos);
                toolbar.setOnMenuItemClickListener(item -> {
                long duration = 0;
                int id = item.getItemId();
                if (id == R.id.menu_filter_5m)
                    duration = 5 * 60 * 1000L;
                else if (id == R.id.menu_filter_10m)
                    duration = 10 * 60 * 1000L;
                else if (id == R.id.menu_filter_15m)
                    duration = 15 * 60 * 1000L;
                else if (id == R.id.menu_filter_30m)
                    duration = 30 * 60 * 1000L;
                else if (id == R.id.menu_filter_1h)
                    duration = 60 * 60 * 1000L;
                else if (id == R.id.menu_filter_3h)
                    duration = 3 * 60 * 60 * 1000L;
                else if (id == R.id.menu_filter_5h)
                    duration = 5 * 60 * 60 * 1000L;

                item.setChecked(true);
                filterDuration.setValue(duration);
                return true;
            });
            }

            androidx.lifecycle.LiveData<List<YouTubeItem>> videosLiveData = androidx.lifecycle.Transformations
                    .switchMap(filterDuration, duration -> {
                        if (duration == 0) {
                            return db.youTubeDao().getAllVideos();
                        } else {
                            long minTimestamp = System.currentTimeMillis() - duration;
                            return db.youTubeDao().getRecentlyFetchedVideos(minTimestamp);
                        }
                    });

            videosLiveData.observe(getViewLifecycleOwner(), items -> {
                adapter.setItems(injectNativeAds(items));
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                if (items.isEmpty()) {
                    textEmpty.setVisibility(View.VISIBLE);
                    if (filterDuration.getValue() == 0) {
                        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
                        WorkManager.getInstance(getContext()).enqueue(request);
                    }
                } else {
                    textEmpty.setVisibility(View.GONE);
                }
            });
        }

        db.favoriteDao().getAllFavorites().observe(getViewLifecycleOwner(),
                new Observer<List<com.app.webdroid.model.FavoriteItem>>() {
                    @Override
                    public void onChanged(List<com.app.webdroid.model.FavoriteItem> favoriteItems) {
                        List<String> ids = new ArrayList<>();
                        for (com.app.webdroid.model.FavoriteItem item : favoriteItems) {
                            if (com.app.webdroid.model.FavoriteItem.TYPE_YOUTUBE.equals(item.type)) {
                                ids.add(item.itemId);
                            }
                        }
                        adapter.setFavoriteIds(ids);
                    }
                });

        adapter.setOnFavoriteClickListener((v, obj, position) -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                if (db.favoriteDao().isFavorite(obj.videoId, com.app.webdroid.model.FavoriteItem.TYPE_YOUTUBE) > 0) {
                    db.favoriteDao().removeFavorite(obj.videoId, com.app.webdroid.model.FavoriteItem.TYPE_YOUTUBE);
                } else {
                    com.app.webdroid.model.FavoriteItem fav = new com.app.webdroid.model.FavoriteItem();
                    fav.itemId = obj.videoId;
                    fav.type = com.app.webdroid.model.FavoriteItem.TYPE_YOUTUBE;
                    fav.title = obj.title;
                    fav.subtitle = obj.pubDate;
                    fav.imageUrl = obj.thumbnailUrl;
                    fav.targetUrl = "https://www.youtube.com/watch?v=" + obj.videoId;
                    db.favoriteDao().addFavorite(fav);
                }
            });
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (activeChannelId != null && !activeChannelId.isEmpty()) {
                syncSpecificChannel(activeChannelId, activeChannelName);
            } else {
                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
                WorkManager.getInstance(getContext()).enqueue(request);
            }
        });

        return view;
    }

    private java.util.List<YouTubeItem> injectNativeAds(java.util.List<YouTubeItem> items) {
        if (getContext() == null || items == null)
            return items;

        com.app.webdroid.database.prefs.AdsPref adsPref = new com.app.webdroid.database.prefs.AdsPref(getContext());
        if (!adsPref.getAdStatus())
            return items;

        // Interval: every 10th item (safe density)
        int interval = Math.max(10, adsPref.getNativeAdIndex());
        java.util.List<YouTubeItem> newItems = new ArrayList<>();
        int count = 0;
        for (YouTubeItem item : items) {
            if (item.isNativeAd)
                continue;
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

    private void syncSpecificChannel(String channelId, String name) {
        if (getContext() == null || channelId == null)
            return;
        // Deep fetch channel videos using Innertube multi-page fetcher
        new Thread(() -> {
            try {
                List<YouTubeItem> items = com.app.webdroid.util.YouTubeInnertubeFetcher.fetchChannelVideos(channelId, name, 3);
                if (items != null && !items.isEmpty()) {
                    AppDatabase.getDatabase(getContext()).youTubeDao().insertVideos(items);
                }
            } catch (Exception e) {
                android.util.Log.e("FragmentVideos", "Failed to sync channel: " + channelId, e);
            }
        }).start();
    }

    private void openFullChannelOnYouTube(String channelId) {
        if (channelId == null || channelId.isEmpty()) return;
        String url = "https://www.youtube.com/channel/" + channelId + "/videos";
        try {
            Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://www.youtube.com/channel/" + channelId + "/videos"));
            startActivity(appIntent);
        } catch (Exception ex) {
            try {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(webIntent);
            } catch (Exception e) {
                if (getContext() != null) {
                    android.widget.Toast.makeText(getContext(), "Could not open YouTube", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void watchYoutubeVideo(String id) {
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + id));
        try {
            startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            startActivity(webIntent);
        }
    }
}
