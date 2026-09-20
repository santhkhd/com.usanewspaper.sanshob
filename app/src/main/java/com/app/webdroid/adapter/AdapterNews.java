package com.app.webdroid.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.app.webdroid.model.NewsItem;
import com.app.webdroid.util.FollowManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.shobmc.san.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdapterNews extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private List<NewsItem> items;
    private List<String> favoriteIds = new ArrayList<>();
    private OnItemClickListener onItemClickListener;
    private OnFavoriteClickListener onFavoriteClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, NewsItem obj, int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(View view, NewsItem obj, int position);
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener onFavoriteClickListener) {
        this.onFavoriteClickListener = onFavoriteClickListener;
    }

    public void setFavoriteIds(List<String> favoriteIds) {
        this.favoriteIds = favoriteIds != null ? favoriteIds : new ArrayList<>();
        notifyDataSetChanged();
    }

    public AdapterNews(Context context, List<NewsItem> items) {
        this.context = context;
        this.items = items != null ? items : new ArrayList<>();
    }

    public void setItems(List<NewsItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<NewsItem> getItems() {
        return items != null ? items : new ArrayList<>();
    }

    public static final int VIEW_TYPE_CARD_IMAGE = 0;
    public static final int VIEW_TYPE_COMPACT_LIST = 1;
    public static final int VIEW_TYPE_AD = 2;
    public static final int VIEW_TYPE_CATEGORY_HEADER = 3;

    private FollowManager.CategoryMeta categoryMeta = null;
    private OnFollowStateChangeListener onFollowStateChangeListener;

    public interface OnFollowStateChangeListener {
        void onFollowStateChanged(String categoryKey, boolean isFollowed);
    }

    public void setCategoryMeta(FollowManager.CategoryMeta meta) {
        this.categoryMeta = meta;
        notifyDataSetChanged();
    }

    public void setOnFollowStateChangeListener(OnFollowStateChangeListener listener) {
        this.onFollowStateChangeListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (categoryMeta != null && position == 0) {
            return VIEW_TYPE_CATEGORY_HEADER;
        }
        int actualIndex = (categoryMeta != null) ? position - 1 : position;
        if (items.get(actualIndex).isNativeAd) {
            return VIEW_TYPE_AD;
        }
        if (items.get(actualIndex).hasRealImage()) {
            return VIEW_TYPE_CARD_IMAGE;
        }
        return VIEW_TYPE_COMPACT_LIST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_CATEGORY_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_page_header, parent, false);
            return new CategoryHeaderViewHolder(view);
        } else if (viewType == VIEW_TYPE_AD) {
            com.app.webdroid.database.prefs.AdsPref adsPref = new com.app.webdroid.database.prefs.AdsPref(context);
            return new com.app.webdroid.util.AdsManager((android.app.Activity) context)
                    .createNativeAdViewHolder(context, parent, adsPref.getNativeAdStyleProductList());
        } else if (viewType == VIEW_TYPE_COMPACT_LIST) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news_compact, parent, false);
            return new ViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_CATEGORY_HEADER) {
            if (categoryMeta == null) return;
            CategoryHeaderViewHolder h = (CategoryHeaderViewHolder) holder;

            // Load Cover Banner
            if (h.ivBanner != null) {
                Glide.with(context)
                        .load(categoryMeta.coverUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_placeholder_media)
                        .error(R.drawable.ic_placeholder_media)
                        .into(h.ivBanner);
            }

            // Load Category Avatar
            if (h.ivAvatar != null) {
                Glide.with(context)
                        .load(categoryMeta.avatarUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .circleCrop()
                        .placeholder(categoryMeta.iconRes)
                        .error(categoryMeta.iconRes)
                        .into(h.ivAvatar);
            }

            // Set Title & Description
            if (h.tvTitle != null) {
                h.tvTitle.setText(categoryMeta.title);
            }
            if (h.tvDesc != null) {
                if (categoryMeta.description != null && !categoryMeta.description.isEmpty()) {
                    h.tvDesc.setText(categoryMeta.description);
                    h.tvDesc.setVisibility(View.VISIBLE);
                } else {
                    h.tvDesc.setVisibility(View.GONE);
                }
            }

            // Set Metrics (Followers, Posts, Views)
            if (h.tvFollowers != null) {
                h.tvFollowers.setText(FollowManager.getFormattedFollowers(context, categoryMeta.key));
            }
            if (h.tvPosts != null) {
                h.tvPosts.setText(categoryMeta.postsCount);
            }
            if (h.tvViews != null) {
                h.tvViews.setText(categoryMeta.viewsCount);
            }

            // Follow Button State
            boolean isFollowed = FollowManager.isFollowed(context, categoryMeta.key);
            updateFollowButtonUi(h.btnFollow, isFollowed);

            h.btnFollow.setOnClickListener(v -> {
                h.btnFollow.animate().scaleX(1.15f).scaleY(1.15f).setDuration(120)
                        .withEndAction(() -> h.btnFollow.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start())
                        .start();

                boolean newFollowed = FollowManager.toggleFollow(context, categoryMeta.key);
                updateFollowButtonUi(h.btnFollow, newFollowed);
                if (h.tvFollowers != null) {
                    h.tvFollowers.setText(FollowManager.getFormattedFollowers(context, categoryMeta.key));
                }
                Toast.makeText(context, (newFollowed ? "Followed " : "Unfollowed ") + categoryMeta.title, Toast.LENGTH_SHORT).show();
                if (onFollowStateChangeListener != null) {
                    onFollowStateChangeListener.onFollowStateChanged(categoryMeta.key, newFollowed);
                }
            });

            return;
        }

        if (getItemViewType(position) == VIEW_TYPE_AD) {
            com.solodroidx.ads.nativead.NativeAdViewHolder nativeHolder = (com.solodroidx.ads.nativead.NativeAdViewHolder) holder;
            new com.app.webdroid.util.AdsManager((android.app.Activity) context).bindNativeAdViewHolder(context, nativeHolder);
            return;
        }

        ViewHolder myHolder = (ViewHolder) holder;
        final int itemPos = (categoryMeta != null) ? position - 1 : position;
        final NewsItem item = items.get(itemPos);

        // 1. Malayalam-friendly Title
        myHolder.title.setText(item.title != null ? item.title.trim() : "");

        // 2. 🔴 BREAKING Badge
        boolean isBreaking = isBreakingNews(item);
        if (myHolder.badgeBreaking != null) {
            myHolder.badgeBreaking.setVisibility(isBreaking ? View.VISIBLE : View.GONE);
        }

        // 3. Compact Relative Time & Source
        String compactTime = formatCompactDate(item.pubDateMillis, item.pubDate);
        myHolder.date.setText(compactTime);

        if (item.sourceName != null && !item.sourceName.trim().isEmpty()) {
            myHolder.source.setText(item.sourceName.trim());
            myHolder.source.setVisibility(View.VISIBLE);
            if (myHolder.metaDot != null) myHolder.metaDot.setVisibility(View.VISIBLE);
        } else {
            myHolder.source.setVisibility(View.GONE);
            if (myHolder.metaDot != null) myHolder.metaDot.setVisibility(View.GONE);
        }

        // 3.5 Source Favicon / Icon
        String sourceIcon = item.getSourceIconUrl();
        if (myHolder.imgSourceIcon != null) {
            if (sourceIcon != null && !sourceIcon.isEmpty()) {
                myHolder.imgSourceIcon.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(sourceIcon)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_newspaper)
                        .error(R.drawable.ic_newspaper)
                        .into(myHolder.imgSourceIcon);
            } else {
                myHolder.imgSourceIcon.setVisibility(View.GONE);
            }
        }

        // 3.6 Clean Description for Compact List
        if (myHolder.description != null) {
            if (item.description != null && !item.description.trim().isEmpty()) {
                String cleanDesc = android.text.Html.fromHtml(item.description).toString().trim();
                cleanDesc = cleanDesc.replaceAll("\\s+", " ");
                if (!cleanDesc.isEmpty()) {
                    myHolder.description.setText(cleanDesc);
                    myHolder.description.setVisibility(View.VISIBLE);
                } else {
                    myHolder.description.setVisibility(View.GONE);
                }
            } else {
                myHolder.description.setVisibility(View.GONE);
            }
        }

        // 4. 16:9 Image with Smooth CrossFade (Only for cards with real photo)
        if (myHolder.image != null) {
            if (item.hasRealImage()) {
                myHolder.image.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(item.imageUrl.trim())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_placeholder_media)
                        .error(R.drawable.ic_placeholder_media)
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .into(myHolder.image);
            } else {
                myHolder.image.setVisibility(View.GONE);
            }
        }

        // 5. Favorite State
        boolean isFavorite = item.link != null && favoriteIds.contains(item.link);
        int accentColor = ContextCompat.getColor(context, R.color.colorAccent);
        boolean isDark = new com.app.webdroid.database.prefs.SharedPref(context).getIsDarkTheme();
        int mutedColor = isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B");

        if (isFavorite) {
            myHolder.favorite.setImageResource(R.drawable.ic_favorite);
            myHolder.favorite.setColorFilter(accentColor);
        } else {
            myHolder.favorite.setImageResource(R.drawable.ic_favorite_border);
            myHolder.favorite.setColorFilter(mutedColor);
        }

        // 6. Reactions & Engagement Row
        String newsId = String.valueOf(Math.abs((item.link != null ? item.link : item.title).hashCode()));
        SharedPreferences sp = context.getSharedPreferences("news_reactions", Context.MODE_PRIVATE);
        String likeKey = "liked_" + newsId;
        int baseLikes = (Math.abs(newsId.hashCode()) % 70) + 15;
        boolean isLiked = sp.getBoolean(likeKey, false);

        if (myHolder.textLikes != null) {
            myHolder.textLikes.setText(String.valueOf(baseLikes + (isLiked ? 1 : 0)));
        }
        if (myHolder.imgLike != null) {
            myHolder.imgLike.setColorFilter(isLiked ? accentColor : mutedColor);
        }

        // Calculate compact comments count
        int commentCount = (Math.abs(newsId.hashCode()) % 8);
        if (myHolder.textComments != null) {
            myHolder.textComments.setText(String.valueOf(commentCount));
        }

        // Calculate compact share count
        int shareCount = (Math.abs(newsId.hashCode()) % 12) + 1;
        if (myHolder.textShareCount != null) {
            myHolder.textShareCount.setText(String.valueOf(shareCount));
        }

        // Like Click
        if (myHolder.layoutLike != null) {
            myHolder.layoutLike.setOnClickListener(v -> {
                boolean nowLiked = !sp.getBoolean(likeKey, false);
                sp.edit().putBoolean(likeKey, nowLiked).apply();
                if (myHolder.textLikes != null) {
                    myHolder.textLikes.setText(String.valueOf(baseLikes + (nowLiked ? 1 : 0)));
                }
                if (myHolder.imgLike != null) {
                    myHolder.imgLike.setColorFilter(nowLiked ? accentColor : mutedColor);
                }
                Toast.makeText(context, nowLiked ? "Liked!" : "Unliked", Toast.LENGTH_SHORT).show();
            });
        }

        // Comment Click
        if (myHolder.layoutComment != null) {
            myHolder.layoutComment.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(v, item, itemPos);
                }
            });
        }

        // Share Click
        if (myHolder.layoutShare != null) {
            myHolder.layoutShare.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_SUBJECT, item.title);
                    intent.putExtra(Intent.EXTRA_TEXT, item.title + "\n\n" + (item.link != null ? item.link : ""));
                    context.startActivity(Intent.createChooser(intent, "Share Article"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        // Card Click
        myHolder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(v, item, itemPos);
            }
        });

        // Favorite Heart Click with Tactile Bounce Animation
        myHolder.favorite.setOnClickListener(v -> {
            myHolder.favorite.animate().scaleX(1.25f).scaleY(1.25f).setDuration(120)
                    .withEndAction(() -> myHolder.favorite.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start())
                    .start();
            if (onFavoriteClickListener != null) {
                onFavoriteClickListener.onFavoriteClick(v, item, itemPos);
            }
        });
    }

    public static boolean isBreakingNews(NewsItem item) {
        if (item == null) return false;
        String t = item.title != null ? item.title.toLowerCase() : "";
        String s = item.sourceName != null ? item.sourceName.toLowerCase() : "";
        return t.contains("breaking") || t.contains("ബ്രേക്കിംഗ്") || t.contains("flash news")
                || t.contains("ഫ്‌ളാഷ്") || s.contains("breaking");
    }

    public static String formatCompactDate(long millis, String rawDate) {
        if (millis > 0) {
            long diff = System.currentTimeMillis() - millis;
            if (diff >= 0 && diff < 7 * DateUtils.DAY_IN_MILLIS) {
                CharSequence relative = DateUtils.getRelativeTimeSpanString(
                        millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
                if (relative != null && relative.length() > 0 && !relative.toString().contains("1970")) {
                    return relative.toString();
                }
            }
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
                return sdf.format(new Date(millis));
            } catch (Exception ignored) {}
        }

        if (rawDate != null && !rawDate.trim().isEmpty()) {
            String trimmed = rawDate.trim();
            // Try RFC822 parse: "EEE, dd MMM yyyy HH:mm:ss Z"
            try {
                SimpleDateFormat rfc = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
                Date d = rfc.parse(trimmed);
                if (d != null) {
                    return formatCompactDate(d.getTime(), null);
                }
            } catch (Exception ignored) {}

            // Try ISO8601 parse: "yyyy-MM-dd'T'HH:mm:ss"
            try {
                SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date d = iso.parse(trimmed);
                if (d != null) {
                    return formatCompactDate(d.getTime(), null);
                }
            } catch (Exception ignored) {}

            // Clean fallback
            if (trimmed.length() > 16 && trimmed.contains(",")) {
                try {
                    String[] parts = trimmed.split(",");
                    if (parts.length > 1) {
                        String rest = parts[1].trim();
                        if (rest.length() >= 11) {
                            return rest.substring(0, 11).trim();
                        }
                    }
                } catch (Exception ignored) {}
            }
            return trimmed;
        }
        return "Recently";
    }

    @Override
    public int getItemCount() {
        int count = items != null ? items.size() : 0;
        if (categoryMeta != null) {
            count += 1;
        }
        return count;
    }

    private void updateFollowButtonUi(TextView btn, boolean isFollowed) {
        if (btn == null) return;
        if (isFollowed) {
            btn.setText("✕ Unfollow");
            btn.setBackgroundResource(R.drawable.bg_btn_unfollow);
            btn.setTextColor(0xFF0F172A);
        } else {
            btn.setText("+ Follow");
            btn.setBackgroundResource(R.drawable.bg_btn_follow);
            btn.setTextColor(0xFFFFFFFF);
        }
    }

    public static class CategoryHeaderViewHolder extends RecyclerView.ViewHolder {
        public ImageView ivBanner;
        public ImageView ivAvatar;
        public TextView tvTitle;
        public TextView tvDesc;
        public TextView btnFollow;
        public TextView tvFollowers;
        public TextView tvPosts;
        public TextView tvViews;

        public CategoryHeaderViewHolder(View v) {
            super(v);
            ivBanner = v.findViewById(R.id.iv_category_banner);
            ivAvatar = v.findViewById(R.id.iv_category_avatar);
            tvTitle = v.findViewById(R.id.tv_category_title);
            tvDesc = v.findViewById(R.id.tv_category_description);
            btnFollow = v.findViewById(R.id.btn_category_follow);
            tvFollowers = v.findViewById(R.id.tv_followers_count);
            tvPosts = v.findViewById(R.id.tv_posts_count);
            tvViews = v.findViewById(R.id.tv_views_count);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView badgeBreaking;
        public TextView date;
        public TextView metaDot;
        public TextView source;
        public ImageView image;
        public ImageView favorite;
        public View layoutLike;
        public View layoutComment;
        public View layoutShare;
        public ImageView imgLike;
        public TextView textLikes;
        public TextView textComments;
        public TextView textShareCount;
        public ImageView imgSourceIcon;
        public TextView description;

        public ViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.text_title);
            badgeBreaking = v.findViewById(R.id.badge_breaking);
            date = v.findViewById(R.id.text_date);
            metaDot = v.findViewById(R.id.text_meta_dot);
            source = v.findViewById(R.id.text_source);
            image = v.findViewById(R.id.image_news);
            favorite = v.findViewById(R.id.img_favorite);
            layoutLike = v.findViewById(R.id.layout_like_news);
            layoutComment = v.findViewById(R.id.layout_comment_news);
            layoutShare = v.findViewById(R.id.layout_share_news);
            imgLike = v.findViewById(R.id.img_like_news);
            textLikes = v.findViewById(R.id.text_likes_count);
            textComments = v.findViewById(R.id.text_comments_count);
            textShareCount = v.findViewById(R.id.text_share_count);
            imgSourceIcon = v.findViewById(R.id.img_source_icon);
            description = v.findViewById(R.id.text_description);
        }
    }
}

