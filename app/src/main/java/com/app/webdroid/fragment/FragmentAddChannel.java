package com.app.webdroid.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.webdroid.activity.ActivityVideoDetail;
import com.app.webdroid.activity.MainActivity;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.model.AppConfig;
import com.app.webdroid.util.CustomChannelManager;
import com.bumptech.glide.Glide;
import android.app.Dialog;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.shobmc.san.R;

import java.util.ArrayList;
import java.util.List;

public class FragmentAddChannel extends Fragment {

    private static final String ARG_CATEGORY_SLUG = "categorySlug";

    private SharedPref sharedPref;
    private String initialCategorySlug;
    private CustomChannelManager.CategoryOption initialCategory = null;

    // Header views
    private TextView tvToolbarTitle;
    private TextView tvToolbarSubtitle;
    private TextView tvContextCategory;

    // Search Input & Action
    private EditText etSearchQuery;
    private ImageView btnSearchClear;
    private View btnSearchAction;
    private View btnPasteChannelLink;

    // Filter Chips
    private TextView chipAll;
    private TextView chipChannels;
    private TextView chipVideos;
    private String activeFilter = "all";

    // Results, Banner & Loading
    private ProgressBar progressSearch;
    private View layoutSearchEmpty;
    private RecyclerView rvSearchResults;
    private SearchResultAdapter searchAdapter;

    // Top Channel Banner
    private View layoutChannelBanner;
    private ImageView imgChannelBanner;
    private TextView tvChannelBannerName;
    private TextView btnAddChannelBanner;
    private CustomChannelManager.SearchResultItem topMatchedChannel = null;

    // Pagination & State
    private String currentSearchQuery = "";
    private String currentContinuationToken = null;
    private boolean isLoadingMore = false;
    private final List<CustomChannelManager.SearchResultItem> rawSearchResults = new ArrayList<>();

    public static FragmentAddChannel newInstance(String categorySlug) {
        FragmentAddChannel fragment = new FragmentAddChannel();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY_SLUG, categorySlug);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            initialCategorySlug = getArguments().getString(ARG_CATEGORY_SLUG);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_add_channel, container, false);
        sharedPref = new SharedPref(requireContext());

        resolveInitialCategory();
        initViews(root);
        applyTheme(root);
        setupSearch();

        return root;
    }

    private void resolveInitialCategory() {
        if (getContext() == null) return;
        List<CustomChannelManager.CategoryOption> allCats = CustomChannelManager.getAvailableCategories(getContext());
        if (initialCategorySlug != null) {
            for (CustomChannelManager.CategoryOption cat : allCats) {
                if (cat.jsonUrl.equalsIgnoreCase(initialCategorySlug)) {
                    initialCategory = cat;
                    break;
                }
            }
        }
        if (initialCategory == null && !allCats.isEmpty()) {
            initialCategory = allCats.get(0);
        }
    }

    private void initViews(View root) {
        View btnBack = root.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        tvToolbarTitle = root.findViewById(R.id.tv_toolbar_title);
        tvToolbarSubtitle = root.findViewById(R.id.tv_toolbar_subtitle);
        tvContextCategory = root.findViewById(R.id.tv_context_category);

        if (initialCategory != null && initialCategorySlug != null && !initialCategorySlug.isEmpty()) {
            if (tvContextCategory != null) {
                tvContextCategory.setVisibility(View.VISIBLE);
                tvContextCategory.setText(initialCategory.title);
            }
        }

        etSearchQuery = root.findViewById(R.id.et_search_query);
        btnSearchClear = root.findViewById(R.id.btn_search_clear);
        btnSearchAction = root.findViewById(R.id.btn_search_action);
        btnPasteChannelLink = root.findViewById(R.id.btn_paste_channel_link);

        chipAll = root.findViewById(R.id.chip_filter_all);
        chipChannels = root.findViewById(R.id.chip_filter_channels);
        chipVideos = root.findViewById(R.id.chip_filter_videos);

        layoutChannelBanner = root.findViewById(R.id.layout_channel_banner);
        imgChannelBanner = root.findViewById(R.id.img_channel_banner);
        tvChannelBannerName = root.findViewById(R.id.tv_channel_banner_name);
        btnAddChannelBanner = root.findViewById(R.id.btn_add_channel_banner);

        progressSearch = root.findViewById(R.id.progress_search);
        layoutSearchEmpty = root.findViewById(R.id.layout_search_empty);
        rvSearchResults = root.findViewById(R.id.rv_search_results);

        if (btnPasteChannelLink != null) {
            btnPasteChannelLink.setOnClickListener(v -> showPasteChannelLinkDialog());
        }
    }

    private void applyTheme(View root) {
        if (getContext() == null || sharedPref == null) return;
        boolean isDark = sharedPref.getIsDarkTheme();

        View rootLayout = root.findViewById(R.id.layout_add_channel_root);
        if (rootLayout != null) {
            rootLayout.setBackgroundColor(Color.parseColor(isDark ? "#070A10" : "#F8FAFC"));
        }

        View toolbar = root.findViewById(R.id.layout_toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(Color.parseColor(isDark ? "#0D131F" : "#FFFFFF"));
        }

        ImageView btnBack = root.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setImageTintList(ColorStateList.valueOf(Color.parseColor(isDark ? "#FFFFFF" : "#0F172A")));
        }

        if (tvToolbarTitle != null) {
            tvToolbarTitle.setTextColor(Color.parseColor(isDark ? "#FFFFFF" : "#0F172A"));
        }
        if (tvToolbarSubtitle != null) {
            tvToolbarSubtitle.setTextColor(Color.parseColor(isDark ? "#94A3B8" : "#64748B"));
        }

        View searchInput = root.findViewById(R.id.layout_search_input);
        if (searchInput != null) {
            searchInput.setBackgroundResource(isDark ? R.drawable.bg_search_input : R.drawable.bg_search_input_light);
        }

        if (etSearchQuery != null) {
            etSearchQuery.setTextColor(Color.parseColor(isDark ? "#FFFFFF" : "#0F172A"));
            etSearchQuery.setHintTextColor(Color.parseColor(isDark ? "#64748B" : "#94A3B8"));
        }

        ImageView searchIcon = root.findViewById(R.id.img_search_icon);
        if (searchIcon != null) {
            searchIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor(isDark ? "#94A3B8" : "#64748B")));
        }

        TextView tvEmptyTitle = root.findViewById(R.id.tv_search_empty_title);
        if (tvEmptyTitle != null) {
            tvEmptyTitle.setTextColor(Color.parseColor(isDark ? "#E2E8F0" : "#0F172A"));
        }
        TextView tvEmptySubtitle = root.findViewById(R.id.tv_search_empty_subtitle);
        if (tvEmptySubtitle != null) {
            tvEmptySubtitle.setTextColor(Color.parseColor(isDark ? "#94A3B8" : "#64748B"));
        }
        ImageView emptyIcon = root.findViewById(R.id.img_search_empty_icon);
        if (emptyIcon != null) {
            emptyIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor(isDark ? "#475569" : "#94A3B8")));
        }

        if (layoutChannelBanner != null) {
            layoutChannelBanner.setBackgroundResource(isDark ? R.drawable.bg_channel_banner : R.drawable.bg_channel_banner_light);
        }
        if (tvChannelBannerName != null) {
            tvChannelBannerName.setTextColor(Color.parseColor(isDark ? "#FFFFFF" : "#0F172A"));
        }
        TextView tvBannerSub = root.findViewById(R.id.tv_channel_banner_sub);
        if (tvBannerSub != null) {
            tvBannerSub.setTextColor(Color.parseColor(isDark ? "#94A3B8" : "#64748B"));
        }

        selectFilter("all");
    }

    private void setupSearch() {
        if (getContext() == null) return;

        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        searchAdapter = new SearchResultAdapter(getContext(), new SearchResultAdapter.OnItemActionListener() {
            @Override
            public void onVideoClick(CustomChannelManager.SearchResultItem item) {
                Intent intent = new Intent(getContext(), ActivityVideoDetail.class);
                intent.putExtra("videoId", item.id);
                intent.putExtra("title", item.title);
                String dateSubtitle = item.subtitle != null ? item.subtitle : "";
                if (item.duration != null && !item.duration.isEmpty()) {
                    dateSubtitle = "⏱️ " + item.duration + (dateSubtitle.isEmpty() ? "" : " • " + dateSubtitle);
                }
                intent.putExtra("date", dateSubtitle);
                intent.putExtra("thumbUrl", item.thumbnailUrl);
                intent.putExtra("channelName", item.channelTitle);
                intent.putExtra("channelId", item.channelId);
                startActivity(intent);
            }

            @Override
            public void onAddChannelClick(CustomChannelManager.SearchResultItem item, TextView btnAdd) {
                showFullWindowCategorySelector(item, btnAdd);
            }
        });
        rvSearchResults.setAdapter(searchAdapter);

        // Infinite scroll pagination via Innertube continuation token
        rvSearchResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).onScroll(dy);
                }
                if (dy > 0) {
                    RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
                    if (lm instanceof LinearLayoutManager) {
                        LinearLayoutManager llm = (LinearLayoutManager) lm;
                        int visibleItemCount = llm.getChildCount();
                        int totalItemCount = llm.getItemCount();
                        int firstVisibleItemPosition = llm.findFirstVisibleItemPosition();

                        if (!isLoadingMore && currentContinuationToken != null && !currentContinuationToken.isEmpty()) {
                            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 4
                                    && firstVisibleItemPosition >= 0) {
                                loadMoreSearchResults();
                            }
                        }
                    }
                }
            }
        });

        // Filter chips click listeners
        chipAll.setOnClickListener(v -> selectFilter("all"));
        chipChannels.setOnClickListener(v -> selectFilter("channel"));
        chipVideos.setOnClickListener(v -> selectFilter("video"));

        // Text watcher for clear icon
        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSearchClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSearchClear.setOnClickListener(v -> {
            etSearchQuery.setText("");
            etSearchQuery.requestFocus();
        });

        btnSearchAction.setOnClickListener(v -> executeSearch());
        etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                executeSearch();
                return true;
            }
            return false;
        });
    }

    private void selectFilter(String filter) {
        activeFilter = filter;
        boolean isDark = sharedPref != null && sharedPref.getIsDarkTheme();
        int inactiveBg = isDark ? R.drawable.bg_search_chip_inactive_dark : R.drawable.bg_search_chip_inactive_light;
        int inactiveTextColor = isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#475569");

        chipAll.setBackgroundResource("all".equals(filter) ? R.drawable.bg_search_chip_active : inactiveBg);
        chipAll.setTextColor("all".equals(filter) ? Color.WHITE : inactiveTextColor);

        chipChannels.setBackgroundResource("channel".equals(filter) ? R.drawable.bg_search_chip_active : inactiveBg);
        chipChannels.setTextColor("channel".equals(filter) ? Color.WHITE : inactiveTextColor);

        chipVideos.setBackgroundResource("video".equals(filter) ? R.drawable.bg_search_chip_active : inactiveBg);
        chipVideos.setTextColor("video".equals(filter) ? Color.WHITE : inactiveTextColor);

        if (!rawSearchResults.isEmpty()) {
            reorganizeAndDisplayResults();
        } else {
            String q = etSearchQuery.getText() != null ? etSearchQuery.getText().toString().trim() : "";
            if (!q.isEmpty()) {
                executeSearch();
            }
        }
    }

    private void executeSearch() {
        String query = etSearchQuery.getText() != null ? etSearchQuery.getText().toString().trim() : "";
        if (query.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a search query", Toast.LENGTH_SHORT).show();
            return;
        }

        hideKeyboard();
        currentSearchQuery = query;
        currentContinuationToken = null;
        isLoadingMore = false;

        progressSearch.setVisibility(View.VISIBLE);
        layoutSearchEmpty.setVisibility(View.GONE);
        layoutChannelBanner.setVisibility(View.GONE);
        rvSearchResults.setVisibility(View.GONE);

        // Pass filter to search (innertube handles channel filter, or fetches all)
        String apiFilter = "channel".equals(activeFilter) ? "channel" : "all";
        CustomChannelManager.searchYouTubePage(query, apiFilter, null, new CustomChannelManager.SearchPageCallback() {
            @Override
            public void onSuccess(CustomChannelManager.SearchPageResult result) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    progressSearch.setVisibility(View.GONE);

                    rawSearchResults.clear();
                    if (result != null && result.items != null) {
                        rawSearchResults.addAll(result.items);
                    }
                    currentContinuationToken = result != null ? result.nextContinuationToken : null;

                    if (rawSearchResults.isEmpty()) {
                        layoutSearchEmpty.setVisibility(View.VISIBLE);
                        rvSearchResults.setVisibility(View.GONE);
                        layoutChannelBanner.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "No results found for \"" + query + "\"", Toast.LENGTH_SHORT).show();
                    } else {
                        layoutSearchEmpty.setVisibility(View.GONE);
                        rvSearchResults.setVisibility(View.VISIBLE);
                        reorganizeAndDisplayResults();
                        setupTopMatchedBanner();
                        rvSearchResults.scrollToPosition(0);
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    progressSearch.setVisibility(View.GONE);
                    layoutSearchEmpty.setVisibility(View.VISIBLE);
                    layoutChannelBanner.setVisibility(View.GONE);
                    rvSearchResults.setVisibility(View.GONE);
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupTopMatchedBanner() {
        topMatchedChannel = null;
        for (CustomChannelManager.SearchResultItem it : rawSearchResults) {
            if ("CHANNEL".equalsIgnoreCase(it.type)) {
                topMatchedChannel = it;
                break;
            }
        }

        if (topMatchedChannel != null && !"video".equals(activeFilter)) {
            layoutChannelBanner.setVisibility(View.VISIBLE);
            tvChannelBannerName.setText(topMatchedChannel.title);
            if (topMatchedChannel.thumbnailUrl != null && !topMatchedChannel.thumbnailUrl.isEmpty()) {
                Glide.with(FragmentAddChannel.this)
                        .load(topMatchedChannel.thumbnailUrl)
                        .placeholder(R.drawable.ic_live_tv)
                        .circleCrop()
                        .into(imgChannelBanner);
            }

            boolean isAdded = CustomChannelManager.isChannelAddedAnywhere(getContext(), topMatchedChannel.title);
            btnAddChannelBanner.setText(isAdded ? "✓ Added" : "+ Add");
            btnAddChannelBanner.setEnabled(!isAdded);
            btnAddChannelBanner.setAlpha(isAdded ? 0.6f : 1.0f);

            btnAddChannelBanner.setOnClickListener(bv -> {
                showFullWindowCategorySelector(topMatchedChannel, btnAddChannelBanner);
            });
        } else {
            layoutChannelBanner.setVisibility(View.GONE);
        }
    }

    private void reorganizeAndDisplayResults() {
        List<DisplayItem> displayItems = new ArrayList<>();

        List<CustomChannelManager.SearchResultItem> channels = new ArrayList<>();
        List<CustomChannelManager.SearchResultItem> videos = new ArrayList<>();

        for (CustomChannelManager.SearchResultItem item : rawSearchResults) {
            if ("CHANNEL".equalsIgnoreCase(item.type)) {
                channels.add(item);
            } else if ("VIDEO".equalsIgnoreCase(item.type)) {
                videos.add(item);
            } else {
                channels.add(item);
            }
        }

        if ("channel".equals(activeFilter)) {
            for (CustomChannelManager.SearchResultItem ch : channels) {
                displayItems.add(DisplayItem.channel(ch));
            }
        } else if ("video".equals(activeFilter)) {
            for (CustomChannelManager.SearchResultItem vid : videos) {
                displayItems.add(DisplayItem.video(vid));
            }
        } else {
            // "all" mode: sectioned CHANNELS then VIDEOS
            if (!channels.isEmpty()) {
                displayItems.add(DisplayItem.header("CHANNELS (" + channels.size() + ")"));
                for (CustomChannelManager.SearchResultItem ch : channels) {
                    displayItems.add(DisplayItem.channel(ch));
                }
            }
            if (!videos.isEmpty()) {
                displayItems.add(DisplayItem.header("VIDEOS (" + videos.size() + ")"));
                for (CustomChannelManager.SearchResultItem vid : videos) {
                    displayItems.add(DisplayItem.video(vid));
                }
            }
        }

        searchAdapter.setDisplayItems(displayItems);
    }

    private void loadMoreSearchResults() {
        if (isLoadingMore || currentContinuationToken == null || currentContinuationToken.isEmpty()) {
            return;
        }

        isLoadingMore = true;
        CustomChannelManager.searchYouTubePage(currentSearchQuery, "all", currentContinuationToken, new CustomChannelManager.SearchPageCallback() {
            @Override
            public void onSuccess(CustomChannelManager.SearchPageResult result) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    isLoadingMore = false;
                    if (result != null) {
                        currentContinuationToken = result.nextContinuationToken;
                        if (result.items != null && !result.items.isEmpty()) {
                            rawSearchResults.addAll(result.items);
                            reorganizeAndDisplayResults();
                        }
                    } else {
                        currentContinuationToken = null;
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    isLoadingMore = false;
                });
            }
        });
    }

    // =========================================================================
    // Full-Window Category Selection Modal
    // =========================================================================
    private void showFullWindowCategorySelector(CustomChannelManager.SearchResultItem item, TextView btnTrigger) {
        if (getContext() == null || item == null) return;
        String chTitle = (item.channelTitle != null && !item.channelTitle.isEmpty()) ? item.channelTitle : item.title;
        String idToUse = (item.channelId != null && !item.channelId.isEmpty()) ? item.channelId : item.id;
        CustomChannelManager.showFullWindowCategorySelector(
                requireContext(),
                chTitle,
                idToUse,
                item.thumbnailUrl,
                initialCategory != null ? initialCategory.title : null,
                () -> {
                    showAddSuccess(chTitle, "", btnTrigger);
                }
        );
    }

    private void performAddChannel(CustomChannelManager.SearchResultItem item,
                                   CustomChannelManager.CategoryOption category,
                                   TextView btnTrigger) {
        if (getContext() == null || item == null || category == null) return;

        String chTitle = (item.channelTitle != null && !item.channelTitle.isEmpty()) ? item.channelTitle : item.title;
        String idToUse = (item.channelId != null && !item.channelId.isEmpty()) ? item.channelId : item.id;

        if (idToUse != null && !idToUse.isEmpty()) {
            if (idToUse.startsWith("PL") || idToUse.startsWith("VLPL") || idToUse.contains("list=")) {
                String pid = idToUse;
                if (pid.contains("list=")) {
                    int idx = pid.indexOf("list=");
                    pid = pid.substring(idx + 5);
                    if (pid.contains("&")) pid = pid.substring(0, pid.indexOf("&"));
                }
                if (pid.startsWith("VL")) pid = pid.substring(2);
                CustomChannelManager.addDirectPlaylist(getContext(), chTitle, pid, item.thumbnailUrl, category);
                showAddSuccess(chTitle, category.title, btnTrigger);
                return;
            } else if (idToUse.startsWith("UC") && idToUse.length() == 24) {
                CustomChannelManager.addDirectChannel(getContext(), chTitle, idToUse, item.thumbnailUrl, category);
                showAddSuccess(chTitle, category.title, btnTrigger);
                return;
            }
        }

        CustomChannelManager.resolveAndAddChannel(getContext(), chTitle, (idToUse != null && !idToUse.isEmpty()) ? idToUse : chTitle, category, new CustomChannelManager.AddChannelCallback() {
            @Override
            public void onSuccess(AppConfig.OverviewItem res, String categoryTitle) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> showAddSuccess(chTitle, categoryTitle, btnTrigger));
            }

            @Override
            public void onError(String errorMessage) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showAddSuccess(String channelTitle, String categoryTitle, TextView btnTrigger) {
        if (categoryTitle != null && !categoryTitle.isEmpty()) {
            if (getView() != null) {
                Snackbar.make(getView(), channelTitle + " added to " + categoryTitle, Snackbar.LENGTH_LONG)
                        .setAction("OK", v -> {})
                        .show();
            } else if (getContext() != null) {
                Toast.makeText(getContext(), "✓ " + channelTitle + " added to " + categoryTitle + "!", Toast.LENGTH_SHORT).show();
            }
        }

        if (btnTrigger != null) {
            btnTrigger.setText("✓ Added");
            btnTrigger.setEnabled(false);
            btnTrigger.setAlpha(0.6f);
        }

        if (searchAdapter != null) {
            searchAdapter.notifyDataSetChanged();
        }
    }

    // =========================================================================
    // Create New Category Dialog (with Create & Add Flow)
    // =========================================================================
    private void showCreateCategoryDialog(CustomChannelManager.SearchResultItem itemToAutoAdd, TextView btnTrigger) {
        showCreateCategoryDialog(itemToAutoAdd, btnTrigger, null);
    }

    private void showCreateCategoryDialog(CustomChannelManager.SearchResultItem itemToAutoAdd, TextView btnTrigger, Dialog parentDialogToDismiss) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_category, null);
        EditText etCategoryName = dialogView.findViewById(R.id.et_category_name);
        ImageView btnClose = dialogView.findViewById(R.id.btn_dialog_close);
        View btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnCreate = dialogView.findViewById(R.id.btn_dialog_create);

        if (btnCreate instanceof MaterialButton && itemToAutoAdd != null) {
            ((MaterialButton) btnCreate).setText("Create & Add");
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> {
                String name = etCategoryName != null && etCategoryName.getText() != null
                        ? etCategoryName.getText().toString().trim() : "";
                if (name.isEmpty()) {
                    if (etCategoryName != null) etCategoryName.setError("Please enter a category name");
                    return;
                }

                // Duplicate prevention
                for (CustomChannelManager.CategoryOption existing : CustomChannelManager.getAvailableCategories(getContext())) {
                    if (existing.title.equalsIgnoreCase(name)) {
                        if (etCategoryName != null) etCategoryName.setError("Category with this name already exists");
                        return;
                    }
                }

                CustomChannelManager.CategoryOption newCat = CustomChannelManager.createNewCategory(getContext(), name);
                dialog.dismiss();

                if (parentDialogToDismiss != null && parentDialogToDismiss.isShowing()) {
                    parentDialogToDismiss.dismiss();
                }

                if (newCat != null) {
                    if (itemToAutoAdd != null) {
                        performAddChannel(itemToAutoAdd, newCat, btnTrigger);
                    } else {
                        Toast.makeText(getContext(), "Created category \"" + newCat.title + "\"!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        dialog.show();
        if (etCategoryName != null) {
            etCategoryName.requestFocus();
        }
    }

    // =========================================================================
    // Direct Link Paste Dialog
    // =========================================================================
    private void showPasteChannelLinkDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_paste_channel_link, null);
        EditText etName = dialogView.findViewById(R.id.et_dialog_channel_name);
        EditText etUrl = dialogView.findViewById(R.id.et_dialog_channel_url);
        ImageView btnClose = dialogView.findViewById(R.id.btn_link_dialog_close);
        View btnCancel = dialogView.findViewById(R.id.btn_dialog_link_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_link_confirm);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String name = etName != null && etName.getText() != null ? etName.getText().toString().trim() : "";
                String url = etUrl != null && etUrl.getText() != null ? etUrl.getText().toString().trim() : "";

                if (url.isEmpty()) {
                    if (etUrl != null) etUrl.setError("Enter channel link, handle, or ID");
                    return;
                }

                if (name.isEmpty()) {
                    name = url;
                }

                boolean isPlaylist = url.startsWith("PL") || url.startsWith("VLPL") || url.contains("list=");
                CustomChannelManager.SearchResultItem tempItem =
                        new CustomChannelManager.SearchResultItem(name, url, isPlaylist ? "PLAYLIST" : "CHANNEL", null, "Direct Link", name, url);

                dialog.dismiss();
                showFullWindowCategorySelector(tempItem, null);
            });
        }

        dialog.show();
        if (etUrl != null) {
            etUrl.requestFocus();
        }
    }

    private void hideKeyboard() {
        if (getContext() != null && etSearchQuery != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etSearchQuery.getWindowToken(), 0);
            }
        }
    }

    // =========================================================================
    // DisplayItem Model for Sectioned Results
    // =========================================================================
    private static class DisplayItem {
        static final int TYPE_HEADER = 0;
        static final int TYPE_CHANNEL = 1;
        static final int TYPE_VIDEO = 2;

        int viewType;
        String headerTitle;
        CustomChannelManager.SearchResultItem item;

        static DisplayItem header(String title) {
            DisplayItem d = new DisplayItem();
            d.viewType = TYPE_HEADER;
            d.headerTitle = title;
            return d;
        }

        static DisplayItem channel(CustomChannelManager.SearchResultItem item) {
            DisplayItem d = new DisplayItem();
            d.viewType = TYPE_CHANNEL;
            d.item = item;
            return d;
        }

        static DisplayItem video(CustomChannelManager.SearchResultItem item) {
            DisplayItem d = new DisplayItem();
            d.viewType = TYPE_VIDEO;
            d.item = item;
            return d;
        }
    }

    // =========================================================================
    // Recycler Adapter for Sectioned Results
    // =========================================================================
    private static class SearchResultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final Context context;
        private final SharedPref sharedPref;
        private final List<DisplayItem> items = new ArrayList<>();
        private final OnItemActionListener listener;

        interface OnItemActionListener {
            void onVideoClick(CustomChannelManager.SearchResultItem item);
            void onAddChannelClick(CustomChannelManager.SearchResultItem item, TextView btnAdd);
        }

        SearchResultAdapter(Context context, OnItemActionListener listener) {
            this.context = context;
            this.sharedPref = new SharedPref(context);
            this.listener = listener;
        }

        void setDisplayItems(List<DisplayItem> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).viewType;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == DisplayItem.TYPE_HEADER) {
                View v = LayoutInflater.from(context).inflate(R.layout.item_search_section_header, parent, false);
                return new HeaderViewHolder(v);
            } else if (viewType == DisplayItem.TYPE_CHANNEL) {
                View v = LayoutInflater.from(context).inflate(R.layout.item_channel_search_result, parent, false);
                return new ChannelViewHolder(v);
            } else {
                View v = LayoutInflater.from(context).inflate(R.layout.item_video_compact, parent, false);
                return new VideoViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            DisplayItem displayItem = items.get(position);
            boolean isDark = sharedPref != null && sharedPref.getIsDarkTheme();

            if (holder instanceof HeaderViewHolder) {
                HeaderViewHolder hh = (HeaderViewHolder) holder;
                hh.tvTitle.setText(displayItem.headerTitle);
            } else if (holder instanceof ChannelViewHolder) {
                ChannelViewHolder ch = (ChannelViewHolder) holder;
                CustomChannelManager.SearchResultItem item = displayItem.item;

                ch.tvTitle.setText(item.title);
                ch.tvTitle.setTextColor(Color.parseColor(isDark ? "#FFFFFF" : "#0F172A"));

                ch.tvSubtitle.setText(item.subtitle != null && !item.subtitle.isEmpty() ? item.subtitle : "YouTube Channel");
                ch.tvSubtitle.setTextColor(Color.parseColor(isDark ? "#94A3B8" : "#64748B"));

                if (item.thumbnailUrl != null && !item.thumbnailUrl.isEmpty()) {
                    Glide.with(context)
                            .load(item.thumbnailUrl)
                            .placeholder(R.drawable.ic_live_tv)
                            .circleCrop()
                            .into(ch.imgAvatar);
                } else {
                    ch.imgAvatar.setImageResource(R.drawable.ic_live_tv);
                }

                boolean isAdded = CustomChannelManager.isChannelAddedAnywhere(context, item.title);
                ch.btnAdd.setText(isAdded ? "✓ Added" : "+ Add");
                ch.btnAdd.setEnabled(!isAdded);
                ch.btnAdd.setAlpha(isAdded ? 0.6f : 1.0f);

                ch.btnAdd.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAddChannelClick(item, ch.btnAdd);
                    }
                });

                ch.itemView.setOnClickListener(v -> {
                    if (listener != null && !isAdded) {
                        listener.onAddChannelClick(item, ch.btnAdd);
                    }
                });

            } else if (holder instanceof VideoViewHolder) {
                VideoViewHolder vh = (VideoViewHolder) holder;
                CustomChannelManager.SearchResultItem item = displayItem.item;

                vh.title.setText(item.title);
                vh.title.setTextColor(Color.parseColor(isDark ? "#FFFFFF" : "#0F172A"));

                String chName = (item.channelTitle != null && !item.channelTitle.isEmpty()) ? item.channelTitle : "YouTube";
                vh.channel.setText(chName);

                if (vh.date != null) {
                    vh.date.setText(item.subtitle);
                    vh.date.setTextColor(Color.parseColor(isDark ? "#94A3B8" : "#64748B"));
                }

                if (vh.duration != null) {
                    if (item.duration != null && !item.duration.isEmpty()) {
                        vh.duration.setText(item.duration);
                        vh.duration.setVisibility(View.VISIBLE);
                    } else {
                        vh.duration.setVisibility(View.GONE);
                    }
                }

                if (item.thumbnailUrl != null && !item.thumbnailUrl.isEmpty()) {
                    Glide.with(context)
                            .load(item.thumbnailUrl)
                            .placeholder(R.drawable.ic_placeholder_media)
                            .into(vh.image);
                }

                boolean isAdded = CustomChannelManager.isChannelAddedAnywhere(context, chName);
                if (vh.btnAddChannel != null) {
                    vh.btnAddChannel.setText(isAdded ? "✓ Added" : "+ Add");
                    vh.btnAddChannel.setEnabled(!isAdded);
                    vh.btnAddChannel.setAlpha(isAdded ? 0.6f : 1.0f);
                    vh.btnAddChannel.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onAddChannelClick(item, vh.btnAddChannel);
                        }
                    });
                }

                vh.itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onVideoClick(item);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;

            HeaderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_section_title);
            }
        }

        static class ChannelViewHolder extends RecyclerView.ViewHolder {
            ImageView imgAvatar;
            TextView tvTitle;
            TextView tvSubtitle;
            TextView btnAdd;

            ChannelViewHolder(@NonNull View itemView) {
                super(itemView);
                imgAvatar = itemView.findViewById(R.id.img_channel_avatar);
                tvTitle = itemView.findViewById(R.id.tv_channel_title);
                tvSubtitle = itemView.findViewById(R.id.tv_channel_subtitle);
                btnAdd = itemView.findViewById(R.id.btn_channel_add);
            }
        }

        static class VideoViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView channel;
            TextView btnAddChannel;
            TextView date;
            TextView duration;
            ImageView image;

            VideoViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.text_video_title);
                channel = itemView.findViewById(R.id.text_channel_name);
                btnAddChannel = itemView.findViewById(R.id.btn_add_channel);
                date = itemView.findViewById(R.id.text_pub_date);
                duration = itemView.findViewById(R.id.text_duration);
                image = itemView.findViewById(R.id.image_thumbnail);
            }
        }
    }
}
