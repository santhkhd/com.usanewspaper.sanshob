package com.app.webdroid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.app.webdroid.model.HistoryItem;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.shobmc.san.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdapterHistoryHorizontal extends RecyclerView.Adapter<AdapterHistoryHorizontal.ViewHolder> {

    private final Context context;
    private List<HistoryItem> items = new ArrayList<>();
    private OnItemClickListener onItemClickListener;
    private OnDeleteClickListener onDeleteClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, HistoryItem obj, int position);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(View view, HistoryItem obj, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    public AdapterHistoryHorizontal(Context context, List<HistoryItem> items) {
        this.context = context;
        if (items != null) {
            this.items = items;
        }
    }

    public void setItems(List<HistoryItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<HistoryItem> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final HistoryItem item = items.get(position);
        holder.title.setText(item.title != null ? item.title : "Untitled");

        // Relative time formatting
        String timeAgo = formatRelativeTime(item.watchedAt);
        if (item.subtitle != null && !item.subtitle.trim().isEmpty()) {
            holder.subtitle.setText(item.subtitle + (!timeAgo.isEmpty() ? " • " + timeAgo : ""));
        } else {
            holder.subtitle.setText(!timeAgo.isEmpty() ? timeAgo : "Recently watched");
        }

        // Type badge
        String badge = "Video";
        if (HistoryItem.TYPE_IPTV.equalsIgnoreCase(item.type)) {
            badge = "Live TV";
        } else if (HistoryItem.TYPE_MOVIES.equalsIgnoreCase(item.type)) {
            badge = "Movie";
        } else if (HistoryItem.TYPE_SONG.equalsIgnoreCase(item.type)) {
            badge = "Song";
        } else if (HistoryItem.TYPE_WEB.equalsIgnoreCase(item.type)) {
            badge = "Web";
        }
        holder.badge.setText(badge);

        // Thumbnail
        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(item.imageUrl)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.mipmap.ic_launcher);
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(v, item, holder.getAdapterPosition());
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(v, item, holder.getAdapterPosition());
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(v, item, holder.getAdapterPosition());
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static String formatRelativeTime(long timestamp) {
        if (timestamp <= 0) return "";
        long diff = System.currentTimeMillis() - timestamp;
        if (diff < 0) return "Just now";
        if (diff < 60 * 1000) {
            return "Just now";
        } else if (diff < 60 * 60 * 1000) {
            long mins = diff / (60 * 1000);
            return mins + (mins == 1 ? " min ago" : " mins ago");
        } else if (diff < 24 * 60 * 60 * 1000) {
            long hours = diff / (60 * 60 * 1000);
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (diff < 48 * 60 * 60 * 1000) {
            return "Yesterday";
        } else if (diff < 7 * 24 * 60 * 60 * 1000) {
            long days = diff / (24 * 60 * 60 * 1000);
            return days + (days == 1 ? " day ago" : " days ago");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView image;
        public TextView badge;
        public TextView title;
        public TextView subtitle;
        public ImageView btnDelete;

        public ViewHolder(View v) {
            super(v);
            image = v.findViewById(R.id.image_history_thumb);
            badge = v.findViewById(R.id.text_history_badge);
            title = v.findViewById(R.id.text_history_title);
            subtitle = v.findViewById(R.id.text_history_subtitle);
            btnDelete = v.findViewById(R.id.btn_delete_history);
        }
    }
}
