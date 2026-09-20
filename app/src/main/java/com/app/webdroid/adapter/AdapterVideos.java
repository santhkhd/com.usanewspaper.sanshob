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
import com.app.webdroid.model.YouTubeItem;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.util.ArrayList;
import java.util.List;

public class AdapterVideos extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<YouTubeItem> items;
    private List<String> favoriteIds = new ArrayList<>();
    private OnItemClickListener onItemClickListener;
    private OnFavoriteClickListener onFavoriteClickListener;

    public static final int VIEW_TYPE_LIST = 0;
    public static final int VIEW_TYPE_COMPACT = 1;
    public static final int VIEW_TYPE_GRID = 2;
    public static final int VIEW_TYPE_AD = 3;
    private int startViewType = VIEW_TYPE_COMPACT; // Default as requested

    public interface OnItemClickListener {
        void onItemClick(View view, YouTubeItem obj, int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(View view, YouTubeItem obj, int position);
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener onFavoriteClickListener) {
        this.onFavoriteClickListener = onFavoriteClickListener;
    }

    public interface OnChannelAddClickListener {
        void onChannelAddClick(View view, YouTubeItem obj, int position);
    }

    private OnChannelAddClickListener onChannelAddClickListener;

    public void setOnChannelAddClickListener(OnChannelAddClickListener onChannelAddClickListener) {
        this.onChannelAddClickListener = onChannelAddClickListener;
    }

    public void setFavoriteIds(List<String> favoriteIds) {
        this.favoriteIds = favoriteIds;
        notifyDataSetChanged();
    }

    public AdapterVideos(Context context, List<YouTubeItem> items) {
        this.context = context;
        this.items = items;
    }

    public void setViewType(int viewType) {
        this.startViewType = viewType;
        notifyDataSetChanged();
    }

    public void setItems(List<YouTubeItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void insertItems(List<YouTubeItem> newItems) {
        int startPos = this.items.size();
        this.items.addAll(newItems);
        notifyItemRangeInserted(startPos, newItems.size());
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position).isNativeAd) {
            return VIEW_TYPE_AD;
        }
        return startViewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_AD) {
            com.app.webdroid.database.prefs.AdsPref adsPref = new com.app.webdroid.database.prefs.AdsPref(context);
            return new com.app.webdroid.util.AdsManager((android.app.Activity) context)
                    .createNativeAdViewHolder(context, parent, adsPref.getNativeAdStyleProductList());
        }
        int layoutId;
        if (viewType == VIEW_TYPE_COMPACT) {
            layoutId = R.layout.item_video_compact;
        } else if (viewType == VIEW_TYPE_GRID) {
            layoutId = R.layout.item_video_grid;
        } else {
            layoutId = R.layout.item_video;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_AD) {
            com.solodroidx.ads.nativead.NativeAdViewHolder nativeHolder = (com.solodroidx.ads.nativead.NativeAdViewHolder) holder;
            new com.app.webdroid.util.AdsManager((android.app.Activity) context).bindNativeAdViewHolder(context,
                    nativeHolder);
            return;
        }

        ViewHolder myHolder = (ViewHolder) holder;
        final YouTubeItem item = items.get(position);
        myHolder.title.setText(item.title);
        if (myHolder.channel != null) {
            myHolder.channel.setText(item.channelName);
            myHolder.channel.setOnClickListener(v -> {
                if (onChannelAddClickListener != null) {
                    onChannelAddClickListener.onChannelAddClick(v, item, position);
                } else {
                    com.app.webdroid.util.CustomChannelManager.showAddChannelDialog(context, item.channelName, item.getAuthorChannelId(), item.thumbnailUrl, null, () -> notifyItemChanged(position));
                }
            });
        }

        if (myHolder.btnAddChannel != null) {
            boolean isAdded = com.app.webdroid.util.CustomChannelManager.isChannelAddedAnywhere(context, item.channelName);
            if (isAdded) {
                myHolder.btnAddChannel.setText("✓ Saved");
                myHolder.btnAddChannel.setAlpha(0.6f);
            } else {
                myHolder.btnAddChannel.setText("+ Save");
                myHolder.btnAddChannel.setAlpha(1.0f);
            }

            myHolder.btnAddChannel.setOnClickListener(v -> {
                if (onChannelAddClickListener != null) {
                    onChannelAddClickListener.onChannelAddClick(v, item, position);
                } else {
                    com.app.webdroid.util.CustomChannelManager.showAddChannelDialog(context, item.channelName, item.getAuthorChannelId(), item.thumbnailUrl, null, () -> notifyItemChanged(position));
                }
            });
        }

        if (myHolder.duration != null) {
            if (item.duration != null && !item.duration.trim().isEmpty()) {
                myHolder.duration.setText(item.duration.trim());
                myHolder.duration.setVisibility(View.VISIBLE);
                myHolder.duration.bringToFront();
            } else {
                myHolder.duration.setVisibility(View.GONE);
            }
        }

        if (myHolder.date != null) {
            String timeText = "";
            CharSequence timeAgo = com.app.webdroid.util.Tools.getTimeAgo(item.pubDateMillis);
            if (timeAgo != null && timeAgo.length() > 0) {
                timeText = timeAgo.toString();
            } else if (item.pubDate != null && !item.pubDate.trim().isEmpty()) {
                timeText = item.pubDate.trim();
            }

            String viewsText = "";
            if (item.viewCount != null && !item.viewCount.trim().isEmpty()) {
                viewsText = item.viewCount.trim();
                if (!viewsText.toLowerCase().contains("view")) {
                    viewsText = viewsText + " views";
                }
            }

            if (!viewsText.isEmpty() && !timeText.isEmpty()) {
                myHolder.date.setText(viewsText + " • " + timeText);
            } else if (!viewsText.isEmpty()) {
                myHolder.date.setText(viewsText);
            } else if (!timeText.isEmpty()) {
                myHolder.date.setText(timeText);
            } else {
                myHolder.date.setText("");
            }
        }

        if (item.thumbnailUrl != null) {
            Glide.with(context)
                    .load(item.thumbnailUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.mipmap.ic_launcher)
                    .into(myHolder.image);
        }

        boolean isFavorite = favoriteIds.contains(item.videoId);
        myHolder.favorite.setImageResource(isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

        myHolder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(v, item, position);
            }
        });

        myHolder.favorite.setOnClickListener(v -> {
            if (onFavoriteClickListener != null) {
                onFavoriteClickListener.onFavoriteClick(v, item, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView channel;
        public TextView btnAddChannel;
        public TextView date;
        public TextView duration;
        public ImageView image;
        public ImageView favorite;

        public ViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.text_video_title);
            channel = v.findViewById(R.id.text_channel_name);
            btnAddChannel = v.findViewById(R.id.btn_add_channel);
            date = v.findViewById(R.id.text_pub_date); // Optional in compact
            duration = v.findViewById(R.id.text_duration);
            image = v.findViewById(R.id.image_thumbnail);
            favorite = v.findViewById(R.id.img_favorite);

            // Ensure tint is applied programmatically if XML fails on some versions
            favorite.setColorFilter(android.graphics.Color.parseColor("#CC0000")); // Dark Red
        }
    }
}
