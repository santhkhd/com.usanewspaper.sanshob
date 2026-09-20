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
import com.app.webdroid.model.AppConfig;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class AdapterCategory extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private List<AppConfig.OverviewItem> items;
    private List<String> favoriteIds = new ArrayList<>();
    private OnItemClickListener onItemClickListener;
    private OnFavoriteClickListener onFavoriteClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, AppConfig.OverviewItem obj, int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(View view, AppConfig.OverviewItem obj, int position);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(View view, AppConfig.OverviewItem obj, int position);
    }

    public interface OnMoreClickListener {
        void onMoreClick(View view, AppConfig.OverviewItem obj, int position);
    }

    private OnItemLongClickListener onItemLongClickListener;
    private OnMoreClickListener onMoreClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener onFavoriteClickListener) {
        this.onFavoriteClickListener = onFavoriteClickListener;
    }

    public void setOnMoreClickListener(OnMoreClickListener onMoreClickListener) {
        this.onMoreClickListener = onMoreClickListener;
    }

    public void setFavoriteIds(List<String> favoriteIds) {
        this.favoriteIds = favoriteIds;
        notifyDataSetChanged();
    }

    private java.util.Map<String, Integer> badges = new java.util.HashMap<>();

    public void setBadges(java.util.Map<String, Integer> badges) {
        this.badges = badges;
        notifyDataSetChanged();
    }

    public AdapterCategory(Context context, List<AppConfig.OverviewItem> items) {
        this.context = context;
        this.items = items;
    }

    private boolean isFavoritesEnabled = true;

    public void setFavoritesEnabled(boolean enabled) {
        this.isFavoritesEnabled = enabled;
        notifyDataSetChanged();
    }

    private int currentViewMode = VIEW_TYPE_DEFAULT;

    public void setViewMode(int mode) {
        this.currentViewMode = mode;
        notifyDataSetChanged();
    }

    public static final int VIEW_TYPE_DEFAULT = 0;
    public static final int VIEW_TYPE_MOVIE = 1;
    public static final int VIEW_TYPE_AD = 2;

    @Override
    public int getItemViewType(int position) {
        AppConfig.OverviewItem item = items.get(position);
        if ("native_ad".equalsIgnoreCase(item.provider)) {
            return VIEW_TYPE_AD;
        } else if (currentViewMode == VIEW_TYPE_MOVIE) {
            return VIEW_TYPE_MOVIE;
        }
        return VIEW_TYPE_DEFAULT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_AD) {
            com.app.webdroid.database.prefs.AdsPref adsPref = new com.app.webdroid.database.prefs.AdsPref(context);
            return new com.app.webdroid.util.AdsManager((android.app.Activity) context)
                    .createNativeAdViewHolder(context, parent, adsPref.getNativeAdStyleProductList());
        } else if (viewType == VIEW_TYPE_MOVIE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie, parent, false);
            return new ViewHolder(view);
        }

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_AD) {
            com.solodroidx.ads.nativead.NativeAdViewHolder nativeHolder = (com.solodroidx.ads.nativead.NativeAdViewHolder) holder;
            new com.app.webdroid.util.AdsManager((android.app.Activity) context).bindNativeAdViewHolder(context,
                    nativeHolder);
            // Ensure full span for grid
            ViewGroup.LayoutParams layoutParams = nativeHolder.itemView.getLayoutParams();
            if (layoutParams instanceof androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams) {
                ((androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams) layoutParams).setFullSpan(true);
            }
            return;
        }

        ViewHolder myHolder = (ViewHolder) holder;
        final AppConfig.OverviewItem item = items.get(position);

        String cleanTitle = cleanEmoji(item.title);
        String shortTitle = AdapterHomeDiscovery.getShortTitle(cleanTitle.isEmpty() ? item.title : cleanTitle);
        myHolder.title.setText(shortTitle.isEmpty() ? item.title : shortTitle);

        if (myHolder.year != null && item.year != null) {
            myHolder.year.setText(item.year);
            myHolder.year.setVisibility(View.VISIBLE);
        } else if (myHolder.year != null) {
            myHolder.year.setVisibility(View.GONE);
        }

        if (myHolder.runtime != null && item.runtime != null && !item.runtime.isEmpty()) {
            myHolder.runtime.setText("• " + item.runtime);
            myHolder.runtime.setVisibility(View.VISIBLE);
        } else if (myHolder.runtime != null) {
            myHolder.runtime.setVisibility(View.GONE);
        }

        String imageUrl = com.app.webdroid.util.ImageUtil.upgradeAmazonImageUrl(item.image);
        boolean isFolderOrAvatar = (imageUrl != null && (imageUrl.contains("folder") || imageUrl.contains("letter_avatar")));
        boolean isMissingImage = (imageUrl == null || imageUrl.trim().isEmpty());
        int vectorIcon = getVectorIconForCategory(cleanTitle.isEmpty() ? item.title : cleanTitle);

        if (!isMissingImage && !isFolderOrAvatar) {
            myHolder.image.clearColorFilter();
            String cid = com.app.webdroid.util.ChannelLogoCache.extractChannelId(item);
            String titleLower = (item.title != null) ? item.title.toLowerCase() : "";
            boolean isPersonOrChannel = cid != null
                    || imageUrl.contains("googleusercontent.com")
                    || imageUrl.contains("ytimg.com")
                    || imageUrl.contains("wikimedia.org")
                    || imageUrl.contains("wikipedia")
                    || titleLower.contains("mohanlal")
                    || titleLower.contains("mammootty")
                    || titleLower.contains("fahadh")
                    || titleLower.contains("prithviraj")
                    || titleLower.contains("tovino")
                    || titleLower.contains("dulquer")
                    || titleLower.contains("suresh gopi")
                    || titleLower.contains("basil")
                    || titleLower.contains("nivin")
                    || titleLower.contains("asif ali")
                    || titleLower.contains("manju")
                    || titleLower.contains("urvashi")
                    || titleLower.contains("jagathy")
                    || titleLower.contains("innocent")
                    || titleLower.contains("salim kumar")
                    || titleLower.contains("suraj")
                    || titleLower.contains("dileep")
                    || titleLower.contains("ashokan")
                    || titleLower.contains("mamukkoya")
                    || titleLower.contains("haneefa")
                    || titleLower.contains("pappu")
                    || titleLower.contains("mukesh")
                    || titleLower.contains("jagadish")
                    || titleLower.contains("sreenivasan")
                    || titleLower.contains("yesudas")
                    || titleLower.contains("nazir");

            if (currentViewMode == VIEW_TYPE_MOVIE) {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_placeholder_media)
                        .error(vectorIcon != 0 ? vectorIcon : R.drawable.ic_placeholder_media)
                        .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(200))
                        .centerCrop()
                        .into(myHolder.image);
            } else if (isPersonOrChannel) {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_placeholder_media)
                        .error(vectorIcon != 0 ? vectorIcon : R.drawable.ic_placeholder_media)
                        .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(200))
                        .circleCrop()
                        .into(myHolder.image);
            } else {
                int cornerPx = (int) (10 * context.getResources().getDisplayMetrics().density);
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_placeholder_media)
                        .error(vectorIcon != 0 ? vectorIcon : R.drawable.ic_placeholder_media)
                        .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(200))
                        .transform(new com.bumptech.glide.load.resource.bitmap.CenterCrop(), new com.bumptech.glide.load.resource.bitmap.RoundedCorners(cornerPx))
                        .into(myHolder.image);
            }
        } else {
            // Missing image or letter avatar requested
            String url = (item.arguments != null && !item.arguments.isEmpty()) ? item.arguments.get(0) : null;
            if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                myHolder.image.clearColorFilter();
                String favicon = "https://www.google.com/s2/favicons?sz=128&domain=" + getDomainName(url);
                Glide.with(context)
                        .load(favicon)
                        .placeholder(vectorIcon != 0 ? vectorIcon : R.drawable.ic_placeholder_media)
                        .error(vectorIcon != 0 ? vectorIcon : R.drawable.ic_placeholder_media)
                        .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(200))
                        .into(myHolder.image);
            } else if (vectorIcon != 0) {
                myHolder.image.setImageResource(vectorIcon);
                myHolder.image.setColorFilter(androidx.core.content.ContextCompat.getColor(context, R.color.color_light_primary));
            } else {
                myHolder.image.clearColorFilter();
                int avatarSize = (int) (68 * context.getResources().getDisplayMetrics().density);
                android.graphics.drawable.Drawable avatar = com.app.webdroid.util.LetterAvatarUtil.createLetterAvatar(context, cleanTitle.isEmpty() ? item.title : cleanTitle, avatarSize);
                myHolder.image.setImageDrawable(avatar);
            }
        }

        // Badge Logic
        if (myHolder.badge != null) {
            if (badges.containsKey(item.title)) {
                Integer count = badges.get(item.title);
                if (count != null && count > 0) {
                    myHolder.badge.setText(String.valueOf(count));
                    myHolder.badge.setVisibility(View.VISIBLE);
                } else {
                    myHolder.badge.setVisibility(View.GONE);
                }
            } else {
                myHolder.badge.setVisibility(View.GONE);
            }
        }

        // Favorite Logic
        if (!isFavoritesEnabled) {
            myHolder.favorite.setVisibility(View.GONE);
        } else {
            String id;
            if (item.arguments != null && !item.arguments.isEmpty()) {
                id = item.arguments.get(0);
            } else {
                id = item.title;
            }

            boolean isFav = favoriteIds.contains(id);
            myHolder.favorite.setImageResource(isFav ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

            if ("overview".equalsIgnoreCase(item.provider) ||
                    "favorites".equalsIgnoreCase(item.provider) ||
                    "latest_videos".equalsIgnoreCase(item.provider)) {
                myHolder.favorite.setVisibility(View.GONE);
            } else {
                myHolder.favorite.setVisibility(View.VISIBLE);
            }
        }

        myHolder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(v, item, position);
            }
        });

        myHolder.itemView.setOnLongClickListener(v -> {
            if (onItemLongClickListener != null) {
                return onItemLongClickListener.onItemLongClick(v, item, position);
            }
            return false;
        });

        myHolder.favorite.setOnClickListener(v -> {
            if (onFavoriteClickListener != null) {
                onFavoriteClickListener.onFavoriteClick(v, item, position);
            }
        });

        if (myHolder.btnMore != null) {
            myHolder.btnMore.setOnClickListener(v -> {
                if (onMoreClickListener != null) {
                    onMoreClickListener.onMoreClick(v, item, position);
                }
            });
        }
    }

    public void setItems(List<AppConfig.OverviewItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public ImageView image;
        public ImageView favorite;
        public ImageView btnMore;
        public TextView badge;
        // Movie fields
        public TextView year;
        public TextView runtime;

        public ViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.text_title);
            image = v.findViewById(R.id.image_icon); // In item_movie this ID is image_movie_poster
            if (image == null)
                image = v.findViewById(R.id.image_movie_poster);

            favorite = v.findViewById(R.id.img_favorite);
            btnMore = v.findViewById(R.id.btn_card_more);
            badge = v.findViewById(R.id.text_badge);
            year = v.findViewById(R.id.text_year);
            runtime = v.findViewById(R.id.text_runtime);
        }
    }

    public static String cleanEmoji(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\p{So}\\p{Cn}\\p{Cs}\\p{Sk}]", "")
                   .replaceAll("^[\\s\\-•★⭐🔥]+", "")
                   .trim();
    }

    public static int getVectorIconForCategory(String title) {
        if (title == null) return 0;
        String lower = title.toLowerCase();
        if (lower.contains("live tv") || lower.contains("iptv") || lower.contains("television") || lower.contains("tv serial") || lower.contains("top shows")) {
            return R.drawable.ic_live_tv;
        }
        if (lower.contains("movie") || lower.contains("cinema") || lower.contains("film") || lower.contains("trailer") || lower.contains("hit") || lower.contains("blockbuster") || lower.contains("ott")) {
            return R.drawable.ic_movie;
        }
        if (lower.contains("radio") || lower.contains("fm") || lower.contains("broadcast")) {
            return R.drawable.ic_radio;
        }
        if (lower.contains("newspaper") || lower.contains("paper") || lower.contains("epaper") || lower.contains("വാർത്ത")) {
            return R.drawable.ic_newspaper;
        }
        if (lower.contains("rss") || lower.contains("article") || lower.contains("post")) {
            return R.drawable.ic_rss_feed;
        }
        if (lower.contains("trending") || lower.contains("popular") || lower.contains("viral") || lower.contains("status")) {
            return R.drawable.ic_trending_up;
        }
        if (lower.contains("music") || lower.contains("song") || lower.contains("audio") || lower.contains("jukebox")) {
            return R.drawable.ic_music_note;
        }
        if (lower.contains("kid") || lower.contains("cartoon") || lower.contains("children") || lower.contains("animation")) {
            return R.drawable.ic_child_care;
        }
        if (lower.contains("tech") || lower.contains("gadget") || lower.contains("science") || lower.contains("it")) {
            return R.drawable.ic_memory;
        }
        if (lower.contains("travel") || lower.contains("flight") || lower.contains("tourism") || lower.contains("journey")) {
            return R.drawable.ic_flight;
        }
        if (lower.contains("channel") || lower.contains("creator") || lower.contains("youtube")) {
            return R.drawable.ic_live_tv;
        }
        return 0;
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
}
