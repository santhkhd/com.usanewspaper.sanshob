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
                getSupportActionBar().setDisplayShowTitleEnabled(true);
                getSupportActionBar().setTitle("SECTIONS");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        ImageView btnSearch = findViewById(R.id.btn_section_search);
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> ActivityUsNews.start(this));
        }

        // Setup Sections RecyclerView
        RecyclerView rvSections = findViewById(R.id.rv_sections);
        if (rvSections != null) {
            rvSections.setLayoutManager(new LinearLayoutManager(this));
            List<FollowManager.CategoryMeta> categories = FollowManager.getAllCategoryMetas();
            rvSections.setAdapter(new SectionsAdapter(categories));
        }
    }

    private class SectionsAdapter extends RecyclerView.Adapter<SectionsAdapter.ViewHolder> {
        private final List<FollowManager.CategoryMeta> items;

        public SectionsAdapter(List<FollowManager.CategoryMeta> items) {
            this.items = items;
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
                ActivityCategoryPage.start(ActivitySections.this, meta.key);
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
