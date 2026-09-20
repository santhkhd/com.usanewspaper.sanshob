package com.app.webdroid.news.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.shobmc.san.R;
import com.app.webdroid.news.model.NewsCategoriesResponse.CategoryItem;
import com.app.webdroid.news.viewmodel.NewsViewModel;
import com.app.webdroid.news.viewmodel.NewsViewModel.SortOption;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;

public class ActivityUsNews extends AppCompatActivity {

    public static void start(Context context) {
        Intent intent = new Intent(context, ActivityUsNews.class);
        context.startActivity(intent);
    }

    private NewsViewModel viewModel;
    private NewsAdapter adapter;

    private MaterialToolbar toolbar;
    private MaterialCardView cardSearch;
    private EditText edtSearch;
    private ImageButton btnClearSearch;
    private MaterialCardView cardOfflineBanner;
    private TextView txtOfflineMsg;
    private ChipGroup chipGroupCategories;
    private TextView txtSectionTitle;
    private TextView txtLastUpdatedTime;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerNews;
    private LinearLayout layoutEmptyState;
    private TextView txtEmptyTitle;
    private TextView txtEmptySubtitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_us_news);

        initViews();
        setupRecyclerView();
        setupViewModel();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_us_news);
        cardSearch = findViewById(R.id.card_search_container);
        edtSearch = findViewById(R.id.edt_search_query);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        cardOfflineBanner = findViewById(R.id.card_offline_banner);
        txtOfflineMsg = findViewById(R.id.txt_offline_status_msg);
        chipGroupCategories = findViewById(R.id.chip_group_categories);
        txtSectionTitle = findViewById(R.id.txt_section_title);
        txtLastUpdatedTime = findViewById(R.id.txt_last_updated_time);
        swipeRefresh = findViewById(R.id.swipe_refresh_us_news);
        recyclerNews = findViewById(R.id.recycler_us_news);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        txtEmptyTitle = findViewById(R.id.txt_empty_state_title);
        txtEmptySubtitle = findViewById(R.id.txt_empty_state_subtitle);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new NewsAdapter(this);
        recyclerNews.setLayoutManager(new LinearLayoutManager(this));
        recyclerNews.setHasFixedSize(true);
        recyclerNews.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(NewsViewModel.class);

        // Observe Stories
        viewModel.stories.observe(this, stories -> {
            adapter.submitList(stories);
            boolean isEmpty = stories == null || stories.isEmpty();
            layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            if (isEmpty) {
                if (Boolean.TRUE.equals(viewModel.isOffline.getValue())) {
                    txtEmptyTitle.setText("You're offline.");
                    txtEmptySubtitle.setText("Showing previously cached news once synced. Pull down to retry.");
                } else {
                    txtEmptyTitle.setText("No latest news available.");
                    txtEmptySubtitle.setText("Pull down to refresh.");
                }
            }
        });

        // Observe Loading
        viewModel.isLoading.observe(this, isLoading -> swipeRefresh.setRefreshing(isLoading));

        // Observe Offline State
        viewModel.isOffline.observe(this, isOffline -> {
            cardOfflineBanner.setVisibility(isOffline ? View.VISIBLE : View.GONE);
        });

        // Observe Last Updated Text
        viewModel.lastUpdatedText.observe(this, text -> {
            txtLastUpdatedTime.setText(text);
            if (Boolean.TRUE.equals(viewModel.isOffline.getValue())) {
                txtOfflineMsg.setText("You're offline. " + text);
            }
        });

        // Observe Categories
        viewModel.categories.observe(this, this::populateCategoryChips);
    }

    private void setupListeners() {
        // Pull to refresh
        swipeRefresh.setOnRefreshListener(() -> viewModel.loadNews());

        // Search Action
        ImageButton btnSearch = findViewById(R.id.btn_action_search);
        btnSearch.setOnClickListener(v -> {
            if (cardSearch.getVisibility() == View.VISIBLE) {
                cardSearch.setVisibility(View.GONE);
                edtSearch.setText("");
                hideKeyboard(edtSearch);
            } else {
                cardSearch.setVisibility(View.VISIBLE);
                edtSearch.requestFocus();
                showKeyboard(edtSearch);
            }
        });

        // Search text watcher
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(TextUtils.isEmpty(s) ? View.GONE : View.VISIBLE);
                viewModel.setSearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> edtSearch.setText(""));

        // Sort Action
        ImageButton btnSort = findViewById(R.id.btn_action_sort);
        btnSort.setOnClickListener(v -> showSortDialog());
    }

    private void populateCategoryChips(List<CategoryItem> categories) {
        chipGroupCategories.removeAllViews();
        if (categories == null) return;

        String selectedCatId = viewModel.selectedCategoryId.getValue();
        if (selectedCatId == null) selectedCatId = "all";

        for (CategoryItem cat : categories) {
            Chip chip = new Chip(this);
            chip.setText(cat.getName());
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChecked(cat.getId().equalsIgnoreCase(selectedCatId));
            chip.setTag(cat.getId());

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    String catId = (String) buttonView.getTag();
                    viewModel.selectCategory(catId);
                    txtSectionTitle.setText(cat.getName().equalsIgnoreCase("Top Stories") ? "Latest Stories" : cat.getName());
                }
            });

            chipGroupCategories.addView(chip);
        }
    }

    private void showSortDialog() {
        String[] options = {"Latest (Default)", "Most Relevant (Multi-Source)", "By Category"};
        int checkedItem = 0;
        SortOption current = viewModel.currentSort.getValue();
        if (current == SortOption.MOST_RELEVANT) checkedItem = 1;
        else if (current == SortOption.CATEGORY) checkedItem = 2;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Sort Stories")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    if (which == 0) {
                        viewModel.setSortOption(SortOption.LATEST);
                    } else if (which == 1) {
                        viewModel.setSortOption(SortOption.MOST_RELEVANT);
                    } else {
                        viewModel.setSortOption(SortOption.CATEGORY);
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
