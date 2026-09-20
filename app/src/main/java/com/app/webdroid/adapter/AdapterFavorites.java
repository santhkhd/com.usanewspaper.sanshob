package com.app.webdroid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.shobmc.san.R;
import com.app.webdroid.model.FavoriteItem;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.util.ArrayList;
import java.util.List;

public class AdapterFavorites extends RecyclerView.Adapter<AdapterFavorites.ViewHolder> {

    private Context context;
    private List<FavoriteItem> items;
    private OnItemClickListener onItemClickListener;
    private OnDeleteClickListener onDeleteClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, FavoriteItem obj, int position);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(View view, FavoriteItem obj, int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener onDeleteClickListener) {
        this.onDeleteClickListener = onDeleteClickListener;
    }

    public AdapterFavorites(Context context, List<FavoriteItem> items) {
        this.context = context;
        this.items = items;
    }

    public void setItems(List<FavoriteItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final FavoriteItem item = items.get(position);
        holder.title.setText(item.title);
        holder.subtitle.setText(item.subtitle);
        String typeBadge = item.type;
        if ("YOUTUBE".equalsIgnoreCase(item.type)) {
            typeBadge = "Video";
        } else if ("RSS".equalsIgnoreCase(item.type)) {
            typeBadge = "Article";
        } else if ("MOVIES".equalsIgnoreCase(item.type)) {
            typeBadge = "Movie";
        } else if ("VIDEOS".equalsIgnoreCase(item.type) || "CHANNEL".equalsIgnoreCase(item.type)) {
            typeBadge = "Channel";
        } else if ("IPTV".equalsIgnoreCase(item.type)) {
            typeBadge = "Live TV";
        } else if ("WEB".equalsIgnoreCase(item.type)) {
            typeBadge = "Web";
        }
        holder.source.setText(typeBadge != null ? typeBadge : "Saved");
        holder.image.setVisibility(View.VISIBLE);

        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(item.imageUrl)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.mipmap.ic_launcher)
                    .into(holder.image);
        } else if (item.targetUrl != null && !item.targetUrl.isEmpty()) {
            // Load Favicon for WEB items
            String favicon = "https://www.google.com/s2/favicons?sz=128&domain=" + getDomainName(item.targetUrl);
            Glide.with(context)
                    .load(favicon)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.mipmap.ic_launcher)
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.mipmap.ic_launcher);
        }

        holder.favorite.setImageResource(R.drawable.ic_favorite);
        holder.favorite.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(v, item, position);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(v, item, position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(v, item, position);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    private String getDomainName(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String domain = uri.getHost();
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (Exception e) {
            return url;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView subtitle; // mapped to date in item_news
        public TextView source;
        public ImageView image;
        public ImageView favorite;

        public ViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.text_title);
            subtitle = v.findViewById(R.id.text_date); // Optional/Hidden
            source = v.findViewById(R.id.text_source);
            image = v.findViewById(R.id.image_news);
            favorite = v.findViewById(R.id.img_favorite);
        }
    }
}
