package com.app.webdroid.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import android.app.Activity;
import android.app.ProgressDialog;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.shobmc.san.R;
import com.app.webdroid.activity.ActivityMovieDetail;
import com.app.webdroid.activity.ActivityRadioPlayer;
import com.app.webdroid.activity.ActivityVideoDetail;
import com.app.webdroid.activity.ActivityWebView;
import com.app.webdroid.model.AppConfig;
import com.app.webdroid.model.NewsItem;
import com.app.webdroid.model.RadioStation;
import com.app.webdroid.model.YouTubeItem;
import com.app.webdroid.util.CustomChannelManager;
import com.app.webdroid.util.YouTubeInnertubeFetcher;

import java.util.ArrayList;
import java.util.List;

public class AdapterHomeDiscovery {

    public static String getShortTitle(String rawTitle) {
        if (rawTitle == null) return "";
        String s = rawTitle.trim();
        s = s.replaceAll("^[\\p{So}\\p{Sk}\\p{Sm}\\p{Sc}\\p{Cs}\\s]+", "");
        s = s.replaceAll("(?i)\\s*\\|.*$", "");
        s = s.replaceAll("(?i)\\s*-\\s*Live.*$", "");
        s = s.replaceAll("(?i)\\s*Live\\s*(24x7|24/7|Stream|News|HD|Now)?", "");
        s = s.replaceAll("(?i)\\s*(24x7|24/7)\\s*", " ");
        s = s.replaceAll("(?i)\\s*Official\\s*(Channel|Page)?", "");
        s = s.replaceAll("(?i)\\s*\\(.*?\\)", "");
        s = s.replaceAll("(?i)\\s*\\[.*?\\]", "");
        s = s.replaceAll("(?i)\\s*Super\\s*Hits?", " Hits");
        s = s.replaceAll("(?i)\\s*Mega\\s*Hits?", " Hits");
        s = s.replaceAll("(?i)\\s*Iconic\\s*Hits?", " Hits");
        s = s.replaceAll("\\s+", " ").trim();
        return s.isEmpty() ? rawTitle.trim() : s;
    }

    // ------------------------------------------------------------------------
    // 1. Trending Adapter (Horizontal video cards)
    // ------------------------------------------------------------------------
    public static class TrendingAdapter extends RecyclerView.Adapter<TrendingAdapter.ViewHolder> {
        private final Context context;
        private final List<YouTubeItem> items = new ArrayList<>();
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(YouTubeItem item);
        }

        public TrendingAdapter(Context context) {
            this.context = context;
        }

        public void setItems(List<YouTubeItem> newItems) {
            items.clear();
            if (newItems != null) {
                items.addAll(newItems);
            }
            notifyDataSetChanged();
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_discovery_video, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            YouTubeItem item = items.get(position);
            holder.tvTitle.setText(item.title != null ? item.title : "");

            String subtitle = "";
            if (item.channelName != null && !item.channelName.isEmpty()) {
                subtitle = item.channelName;
            }
            if (item.pubDate != null && !item.pubDate.isEmpty()) {
                subtitle = subtitle.isEmpty() ? item.pubDate : (subtitle + " • " + item.pubDate);
            }
            holder.tvSubtitle.setText(subtitle);

            if (item.duration != null && !item.duration.isEmpty()) {
                holder.tvDuration.setText(item.duration);
                holder.tvDuration.setVisibility(View.VISIBLE);
            } else {
                holder.tvDuration.setVisibility(View.GONE);
            }

            Glide.with(context)
                    .load(item.thumbnailUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_placeholder_media)
                    .error(R.drawable.ic_placeholder_media)
                    .centerCrop()
                    .into(holder.imgThumb);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                } else {
                    Intent intent = new Intent(context, ActivityVideoDetail.class);
                    intent.putExtra("videoId", item.videoId);
                    intent.putExtra("title", item.title);
                    intent.putExtra("thumbUrl", item.thumbnailUrl);
                    intent.putExtra("date", item.pubDate != null ? item.pubDate : "");
                    context.startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgThumb;
            TextView tvDuration, tvTitle, tvSubtitle;

            ViewHolder(View itemView) {
                super(itemView);
                imgThumb = itemView.findViewById(R.id.img_discovery_thumb);
                tvDuration = itemView.findViewById(R.id.tv_discovery_duration);
                tvTitle = itemView.findViewById(R.id.tv_discovery_title);
                tvSubtitle = itemView.findViewById(R.id.tv_discovery_channel);
            }
        }
    }

    // ------------------------------------------------------------------------
    // 2. TV Channels Adapter (Horizontal circular/square avatar cards)
    // ------------------------------------------------------------------------
    public static class TvChannelsAdapter extends RecyclerView.Adapter<TvChannelsAdapter.ViewHolder> {
        private final Context context;
        private final List<AppConfig.OverviewItem> items = new ArrayList<>();
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(AppConfig.OverviewItem item);
        }

        public TvChannelsAdapter(Context context) {
            this.context = context;
        }

        public void setItems(List<AppConfig.OverviewItem> newItems) {
            items.clear();
            if (newItems != null) {
                items.addAll(newItems);
            }
            notifyDataSetChanged();
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_discovery_channel, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppConfig.OverviewItem item = items.get(position);
            holder.tvTitle.setText(getShortTitle(item.title));

            String imgUrl = item.image;
            if (imgUrl != null && (imgUrl.contains("icons8.com") || imgUrl.contains("google.com/s2/favicons"))) {
                imgUrl = null;
            }
            if ((imgUrl == null || imgUrl.isEmpty()) && item.arguments != null && !item.arguments.isEmpty()) {
                String arg = item.arguments.get(0);
                String fallbackAvatar = CustomChannelManager.findCachedChannelAvatar(context, arg);
                if (fallbackAvatar != null && !fallbackAvatar.isEmpty()) {
                    imgUrl = fallbackAvatar;
                }
            }
            if (imgUrl == null || imgUrl.isEmpty()) {
                imgUrl = item.image;
            }

            if (holder.tvBadge != null) {
                holder.tvBadge.setVisibility(View.GONE);
            }

            if (imgUrl != null && !imgUrl.isEmpty()) {
                holder.imgAvatar.setVisibility(View.VISIBLE);
                holder.tvFallback.setVisibility(View.GONE);
                Glide.with(context)
                        .load(imgUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .fitCenter()
                        .into(holder.imgAvatar);
            } else {
                holder.imgAvatar.setVisibility(View.GONE);
                holder.tvFallback.setVisibility(View.VISIBLE);
                String initial = (item.title != null && !item.title.isEmpty())
                        ? item.title.substring(0, 1).toUpperCase() : "TV";
                holder.tvFallback.setText(initial);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                } else {
                    String targetUrl = (item.arguments != null && !item.arguments.isEmpty())
                            ? item.arguments.get(0) : "";
                    if (targetUrl.isEmpty()) return;

                    if (targetUrl.contains(".m3u8") || "iptv".equalsIgnoreCase(item.provider)) {
                        Intent intent = new Intent(context, ActivityVideoDetail.class);
                        intent.putExtra("videoId", targetUrl);
                        intent.putExtra("title", item.title);
                        intent.putExtra("thumbUrl", item.image);
                        intent.putExtra("date", "Kerala Local IPTV • Live 24/7");
                        context.startActivity(intent);
                        return;
                    }

                    if (targetUrl.contains("/live") || "live".equalsIgnoreCase(item.provider)) {
                        ProgressDialog pd = new ProgressDialog(context);
                        pd.setMessage("Connecting to " + (item.title != null ? item.title : "Live TV") + "...");
                        pd.setCancelable(true);
                        try { pd.show(); } catch (Exception ignored) {}

                        new Thread(() -> {
                            String liveVid = YouTubeInnertubeFetcher.resolveLiveVideoId(targetUrl, item.title);
                            if (context instanceof Activity) {
                                ((Activity) context).runOnUiThread(() -> {
                                    try { pd.dismiss(); } catch (Exception ignored) {}
                                    Intent intent = new Intent(context, ActivityVideoDetail.class);
                                    intent.putExtra("videoId", (liveVid != null && !liveVid.isEmpty()) ? liveVid : targetUrl);
                                    intent.putExtra("title", item.title);
                                    intent.putExtra("thumbUrl", item.image);
                                    intent.putExtra("date", "Live Stream • HD 24/7");
                                    context.startActivity(intent);
                                });
                            }
                        }).start();
                        return;
                    }

                    Intent intent = new Intent(context, ActivityVideoDetail.class);
                    intent.putExtra("videoId", targetUrl);
                    intent.putExtra("title", item.title);
                    intent.putExtra("thumbUrl", item.image);
                    intent.putExtra("date", "Live 24/7 • HD");
                    context.startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgAvatar;
            TextView tvFallback, tvTitle, tvBadge;

            ViewHolder(View itemView) {
                super(itemView);
                imgAvatar = itemView.findViewById(R.id.img_channel_avatar);
                tvFallback = itemView.findViewById(R.id.tv_channel_avatar_fallback);
                tvTitle = itemView.findViewById(R.id.tv_channel_title);
                tvBadge = itemView.findViewById(R.id.tv_channel_badge);
            }
        }
    }

    // ------------------------------------------------------------------------
    // 3. News Discovery Adapter (Horizontal article cards)
    // ------------------------------------------------------------------------
    public static class NewsDiscoveryAdapter extends RecyclerView.Adapter<NewsDiscoveryAdapter.ViewHolder> {
        private final Context context;
        private final List<NewsItem> items = new ArrayList<>();
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(NewsItem item);
        }

        public NewsDiscoveryAdapter(Context context) {
            this.context = context;
        }

        public void setItems(List<NewsItem> newItems) {
            items.clear();
            if (newItems != null) {
                items.addAll(newItems);
            }
            notifyDataSetChanged();
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_discovery_news, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NewsItem item = items.get(position);
            holder.tvTitle.setText(getShortTitle(item.title));

            String meta = "";
            if (item.sourceName != null && !item.sourceName.isEmpty()) {
                meta = item.sourceName;
            }
            if (item.pubDate != null && !item.pubDate.isEmpty()) {
                meta = meta.isEmpty() ? item.pubDate : (meta + " • " + item.pubDate);
            }
            holder.tvMeta.setText(meta);

            Glide.with(context)
                    .load(item.imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .placeholder(R.drawable.ic_placeholder_media)
                    .error(R.drawable.ic_newspaper)
                    .centerCrop()
                    .into(holder.imgThumb);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                } else if (item.link != null && !item.link.isEmpty()) {
                    Intent intent = new Intent(context, ActivityWebView.class);
                    intent.putExtra("title", item.sourceName != null ? item.sourceName : "News");
                    intent.putExtra("link", item.link);
                    context.startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgThumb;
            TextView tvTitle, tvMeta;

            ViewHolder(View itemView) {
                super(itemView);
                imgThumb = itemView.findViewById(R.id.img_news_thumb);
                tvTitle = itemView.findViewById(R.id.tv_news_title);
                tvMeta = itemView.findViewById(R.id.tv_news_meta);
            }
        }
    }

    // ------------------------------------------------------------------------
    // 4. Movies Discovery Adapter (Horizontal movie poster cards)
    // ------------------------------------------------------------------------
    public static class MoviesDiscoveryAdapter extends RecyclerView.Adapter<MoviesDiscoveryAdapter.ViewHolder> {
        private final Context context;
        private final List<AppConfig.OverviewItem> items = new ArrayList<>();
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(AppConfig.OverviewItem item);
        }

        public MoviesDiscoveryAdapter(Context context) {
            this.context = context;
        }

        public void setItems(List<AppConfig.OverviewItem> newItems) {
            items.clear();
            if (newItems != null) {
                items.addAll(newItems);
            }
            notifyDataSetChanged();
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_discovery_movie, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppConfig.OverviewItem item = items.get(position);
            holder.tvTitle.setText(getShortTitle(item.title));

            String sub = "";
            if (item.year != null && !item.year.isEmpty()) {
                sub = item.year;
            }
            if (item.genre != null && !item.genre.isEmpty()) {
                sub = sub.isEmpty() ? item.genre : (sub + " • " + item.genre);
            }
            holder.tvSubtitle.setText(sub);

            if (item.rating != null && !item.rating.isEmpty()) {
                holder.tvBadge.setText("★ " + item.rating);
                holder.tvBadge.setVisibility(View.VISIBLE);
            } else {
                holder.tvBadge.setVisibility(View.GONE);
            }

            Glide.with(context)
                    .load(item.image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .placeholder(R.drawable.ic_placeholder_media)
                    .error(R.drawable.ic_movie)
                    .centerCrop()
                    .into(holder.imgPoster);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                } else {
                    Intent intent = new Intent(context, ActivityMovieDetail.class);
                    intent.putExtra("title", item.title != null ? item.title : "");
                    intent.putExtra("image", item.image != null ? item.image : "");
                    intent.putExtra("year", item.year != null ? item.year : "");
                    intent.putExtra("runtime", item.runtime != null ? item.runtime : "");
                    intent.putExtra("rating", item.rating != null ? item.rating : "");
                    intent.putExtra("director", item.getDirectorString());
                    intent.putExtra("genre", item.genre != null ? item.genre : "");
                    intent.putExtra("plot", item.plot != null ? item.plot : "");
                    if (item.cast != null) {
                        intent.putExtra("cast", android.text.TextUtils.join(", ", item.cast));
                    }
                    String targetOtt = (item.ottUrl != null && !item.ottUrl.isEmpty()) ? item.ottUrl : item.link;
                    if (targetOtt != null && !targetOtt.isEmpty()) {
                        intent.putExtra("ott_url", targetOtt);
                    }
                    context.startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgPoster;
            TextView tvBadge, tvTitle, tvSubtitle;

            ViewHolder(View itemView) {
                super(itemView);
                imgPoster = itemView.findViewById(R.id.img_movie_poster);
                tvBadge = itemView.findViewById(R.id.tv_movie_badge);
                tvTitle = itemView.findViewById(R.id.tv_movie_title);
                tvSubtitle = itemView.findViewById(R.id.tv_movie_year);
            }
        }
    }

    // ------------------------------------------------------------------------
    // 5. Radio Discovery Adapter (Horizontal radio cards)
    // ------------------------------------------------------------------------
    public static class RadioDiscoveryAdapter extends RecyclerView.Adapter<RadioDiscoveryAdapter.ViewHolder> {
        private final Context context;
        private final List<RadioStation> items = new ArrayList<>();
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(RadioStation item);
        }

        public RadioDiscoveryAdapter(Context context) {
            this.context = context;
        }

        public void setItems(List<RadioStation> newItems) {
            items.clear();
            if (newItems != null) {
                items.addAll(newItems);
            }
            notifyDataSetChanged();
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_discovery_radio, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RadioStation item = items.get(position);
            holder.tvTitle.setText(item.getName());

            if (item.getImage() != null && !item.getImage().isEmpty()) {
                holder.imgLogo.setVisibility(View.VISIBLE);
                holder.tvFallback.setVisibility(View.GONE);
                Glide.with(context)
                        .load(item.getImage())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .placeholder(R.drawable.ic_radio)
                        .error(R.drawable.ic_radio)
                        .fitCenter()
                        .into(holder.imgLogo);
            } else {
                holder.imgLogo.setVisibility(View.GONE);
                holder.tvFallback.setVisibility(View.VISIBLE);
                String initial = (item.getName() != null && !item.getName().isEmpty())
                        ? item.getName().substring(0, 1).toUpperCase() : "FM";
                holder.tvFallback.setText(initial);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                } else {
                    Intent intent = new Intent(context, ActivityRadioPlayer.class);
                    context.startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgLogo;
            TextView tvFallback, tvTitle;

            ViewHolder(View itemView) {
                super(itemView);
                imgLogo = itemView.findViewById(R.id.img_radio_logo);
                tvFallback = itemView.findViewById(R.id.tv_radio_fallback);
                tvTitle = itemView.findViewById(R.id.tv_radio_title);
            }
        }
    }

    // ------------------------------------------------------------------------
    // 7. Category Channels Adapter (Horizontal scrolling cards for each category)
    // ------------------------------------------------------------------------
    public static class CategoryChannelsAdapter extends RecyclerView.Adapter<CategoryChannelsAdapter.ViewHolder> {
        private final Context context;
        private final List<AppConfig.OverviewItem> items = new ArrayList<>();
        private final String categoryJsonUrl;
        private final String categoryTitle;
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(AppConfig.OverviewItem item);
            void onAddChannelClick(String categoryJsonUrl, String categoryTitle);
        }

        public CategoryChannelsAdapter(Context context, String categoryJsonUrl, String categoryTitle, OnItemClickListener listener) {
            this.context = context;
            this.categoryJsonUrl = categoryJsonUrl;
            this.categoryTitle = categoryTitle;
            this.listener = listener;
        }

        public void setItems(List<AppConfig.OverviewItem> newItems) {
            items.clear();
            if (newItems != null) {
                items.addAll(newItems);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_discovery_channel, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppConfig.OverviewItem item = items.get(position);

            if ("add_channel_action".equalsIgnoreCase(item.provider)) {
                holder.tvTitle.setText("+ Add");
                holder.imgAvatar.setVisibility(View.VISIBLE);
                holder.tvFallback.setVisibility(View.GONE);
                holder.imgAvatar.setImageResource(R.drawable.ic_bookmark);
                holder.imgAvatar.setColorFilter(androidx.core.content.ContextCompat.getColor(context, R.color.colorAccent));
                if (holder.tvBadge != null) {
                    holder.tvBadge.setVisibility(View.GONE);
                }

                holder.itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAddChannelClick(categoryJsonUrl, categoryTitle);
                    }
                });
                return;
            }

            holder.imgAvatar.clearColorFilter();
            String shortTitle = getShortTitle(item.title);
            holder.tvTitle.setText(shortTitle);

            if (holder.tvBadge != null) {
                holder.tvBadge.setVisibility(View.GONE);
            }

            String imageUrl = com.app.webdroid.util.ImageUtil.upgradeAmazonImageUrl(item.image);
            if (imageUrl != null && imageUrl.contains("icons8.com")) {
                imageUrl = null; // Discard generic icons8 cliparts
            }
            if (imageUrl == null || imageUrl.isEmpty()) {
                if (item.arguments != null && !item.arguments.isEmpty()) {
                    String arg = item.arguments.get(0);
                    String cid = null;
                    if (arg.contains("channel_id=")) {
                        cid = arg.split("channel_id=")[1].split("&")[0];
                    } else if (arg.startsWith("UC") && arg.length() == 24) {
                        cid = arg;
                    }
                    if (cid != null) {
                        imageUrl = com.app.webdroid.util.CustomChannelManager.findCachedChannelAvatar(context, cid);
                    }
                }
            }

            if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.contains("folder") && !imageUrl.contains("letter_avatar")) {
                holder.imgAvatar.setVisibility(View.VISIBLE);
                holder.tvFallback.setVisibility(View.GONE);
                Glide.with(context)
                        .load(imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .placeholder(R.drawable.ic_live_tv)
                        .error(R.drawable.ic_live_tv)
                        .fitCenter()
                        .into(holder.imgAvatar);
            } else {
                holder.imgAvatar.setVisibility(View.GONE);
                holder.tvFallback.setVisibility(View.VISIBLE);
                String initial = (!shortTitle.isEmpty())
                        ? shortTitle.substring(0, 1).toUpperCase() : "TV";
                holder.tvFallback.setText(initial);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private static String cleanEmoji(String text) {
            if (text == null) return "";
            return text.replaceAll("[\\p{So}\\p{Cn}\\p{Cs}\\p{Sk}]", "")
                    .replaceAll("^[\\s\\-•★⭐🔥📁]+", "")
                    .trim();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgAvatar;
            TextView tvFallback, tvTitle, tvBadge;

            ViewHolder(View itemView) {
                super(itemView);
                imgAvatar = itemView.findViewById(R.id.img_channel_avatar);
                tvFallback = itemView.findViewById(R.id.tv_channel_avatar_fallback);
                tvTitle = itemView.findViewById(R.id.tv_channel_title);
                tvBadge = itemView.findViewById(R.id.tv_channel_badge);
            }
        }
    }
}
