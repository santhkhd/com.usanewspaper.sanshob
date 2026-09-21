package com.app.webdroid.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.news.ui.ActivityUsNews;
import com.app.webdroid.util.FollowManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.shobmc.san.R;

import java.util.List;

public class ActivitySections extends AppCompatActivity {

    public static void start(Context context) {
        Intent intent = new Intent(context, ActivitySections.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.app.webdroid.util.Tools.getTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sections);

        boolean isDark = new SharedPref(this).getIsDarkTheme();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int bgCol = ContextCompat.getColor(this, isDark ? R.color.color_dark_toolbar : R.color.color_light_primary);
            getWindow().setStatusBarColor(bgCol);
        }
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false);
        }

        // Setup Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        ImageView btnSearch = findViewById(R.id.btn_section_search);
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                android.widget.EditText etSearch = findViewById(R.id.et_sections_search);
                String q = etSearch != null ? etSearch.getText().toString().trim() : "";
                if (!q.isEmpty()) {
                    ActivityCategoryPage.start(ActivitySections.this, q, q);
                } else {
                    ActivityUsNews.start(ActivitySections.this);
                }
            });
        }

        // Setup Sections RecyclerView & Search Input
        RecyclerView rvSections = findViewById(R.id.rv_sections);
        List<FollowManager.CategoryMeta> allCategories = FollowManager.getAllCategoryMetas();
        SectionsAdapter adapter = new SectionsAdapter(allCategories);

        if (rvSections != null) {
            rvSections.setLayoutManager(new LinearLayoutManager(this));
            rvSections.setAdapter(adapter);
        }

        android.widget.EditText etSectionsSearch = findViewById(R.id.et_sections_search);
        ImageView btnClear = findViewById(R.id.btn_sections_search_clear);

        if (etSectionsSearch != null) {
            etSectionsSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().trim().toLowerCase(java.util.Locale.US);
                    if (btnClear != null) {
                        btnClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    if (query.isEmpty()) {
                        adapter.updateList(allCategories);
                    } else {
                        List<FollowManager.CategoryMeta> filtered = new java.util.ArrayList<>();
                        for (FollowManager.CategoryMeta cat : allCategories) {
                            if ((cat.title != null && cat.title.toLowerCase(java.util.Locale.US).contains(query)) ||
                                (cat.description != null && cat.description.toLowerCase(java.util.Locale.US).contains(query)) ||
                                (cat.key != null && cat.key.toLowerCase(java.util.Locale.US).contains(query))) {
                                filtered.add(cat);
                            }
                        }
                        adapter.updateList(filtered);
                    }
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });

            etSectionsSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                    String q = etSectionsSearch.getText().toString().trim();
                    if (!q.isEmpty()) {
                        ActivityCategoryPage.start(ActivitySections.this, q, q);
                        return true;
                    }
                }
                return false;
            });
        }

        if (btnClear != null && etSectionsSearch != null) {
            btnClear.setOnClickListener(v -> etSectionsSearch.setText(""));
        }
    }

    private class SectionsAdapter extends RecyclerView.Adapter<SectionsAdapter.ViewHolder> {
        private List<FollowManager.CategoryMeta> items;

        public SectionsAdapter(List<FollowManager.CategoryMeta> items) {
            this.items = items != null ? new java.util.ArrayList<>(items) : new java.util.ArrayList<>();
        }

        public void updateList(List<FollowManager.CategoryMeta> newItems) {
            this.items = newItems != null ? new java.util.ArrayList<>(newItems) : new java.util.ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_section_category, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FollowManager.CategoryMeta meta = items.get(position);
            holder.tvTitle.setText(meta.title);
            holder.tvDesc.setText(meta.description);
            holder.ivIcon.setImageResource(meta.iconRes);

            View.OnClickListener clickListener = v -> {
                ActivityCategoryPage.start(ActivitySections.this, meta.key, meta.title);
            };

            holder.itemView.setOnClickListener(clickListener);
            holder.btnMore.setOnClickListener(clickListener);
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivIcon;
            TextView tvTitle;
            TextView tvDesc;
            TextView btnMore;

            public ViewHolder(@NonNull View v) {
                super(v);
                ivIcon = v.findViewById(R.id.iv_section_icon);
                tvTitle = v.findViewById(R.id.tv_section_title);
                tvDesc = v.findViewById(R.id.tv_section_desc);
                btnMore = v.findViewById(R.id.btn_section_more);
            }
        }
    }
}
