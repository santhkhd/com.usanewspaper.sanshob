package com.app.webdroid.news.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.shobmc.san.R;
import com.app.webdroid.news.model.NewsStory;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    public interface OnStoryClickListener {
        void onStoryClick(NewsStory story);
    }

    private final Context context;
    private final List<NewsStory> stories = new ArrayList<>();
    private OnStoryClickListener listener;

    public NewsAdapter(Context context) {
        this.context = context;
    }

    public void setOnStoryClickListener(OnStoryClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<NewsStory> newStories) {
        stories.clear();
        if (newStories != null) {
            stories.addAll(newStories);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_us_news_card, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsStory story = stories.get(position);
        holder.bind(story);
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }

    class NewsViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtCategoryChip;
        private final TextView txtMultiSourceBadge;
        private final TextView txtTime;
        private final TextView txtHeadline;
        private final TextView txtSource;
        private final TextView txtSummary;
        private final MaterialButton btnReadFull;
        private final ImageButton btnShare;

        NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCategoryChip = itemView.findViewById(R.id.txt_news_category_chip);
            txtMultiSourceBadge = itemView.findViewById(R.id.txt_multi_source_badge);
            txtTime = itemView.findViewById(R.id.txt_news_time);
            txtHeadline = itemView.findViewById(R.id.txt_news_headline);
            txtSource = itemView.findViewById(R.id.txt_news_source);
            txtSummary = itemView.findViewById(R.id.txt_news_summary);
            btnReadFull = itemView.findViewById(R.id.btn_read_full_story);
            btnShare = itemView.findViewById(R.id.btn_share_story);
        }

        void bind(NewsStory story) {
            txtHeadline.setText(story.getTitle());
            txtSource.setText(story.getSource());
            txtTime.setText(story.getFormattedAge());
            txtCategoryChip.setText(story.getCategory());
            txtSummary.setText(story.getSummary());

            // Multi-source coverage badge
            if (story.getSourceCount() > 1) {
                txtMultiSourceBadge.setVisibility(View.VISIBLE);
                txtMultiSourceBadge.setText(story.getSourceCount() + " sources");
            } else {
                txtMultiSourceBadge.setVisibility(View.GONE);
            }

            // Click on card
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStoryClick(story);
                } else {
                    openOriginalArticle(story.getUrl());
                }
            });

            // Read Full Story button opens original article link directly via ACTION_VIEW
            btnReadFull.setOnClickListener(v -> openOriginalArticle(story.getUrl()));

            // Share button
            btnShare.setOnClickListener(v -> {
                try {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, story.getTitle());
                    String shareBody = story.getTitle() + "\n\n" + story.getSummary() + "\n\nRead more: " + story.getUrl();
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                    context.startActivity(Intent.createChooser(shareIntent, "Share News Story"));
                } catch (Exception ignored) {
                }
            });
        }

        private void openOriginalArticle(String url) {
            if (TextUtils.isEmpty(url)) {
                Toast.makeText(context, "Story URL unavailable", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(browserIntent);
            } catch (Exception e) {
                Toast.makeText(context, "Unable to open browser", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
