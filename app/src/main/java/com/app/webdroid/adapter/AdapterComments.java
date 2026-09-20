package com.app.webdroid.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.app.webdroid.model.YouTubeComment;
import com.app.webdroid.util.YouTubeInnertubeFetcher;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.shobmc.san.R;

import java.util.ArrayList;
import java.util.List;

public class AdapterComments extends RecyclerView.Adapter<AdapterComments.ViewHolder> {

    private final Context context;
    private final List<YouTubeComment> items = new ArrayList<>();

    public AdapterComments(Context context, List<YouTubeComment> initialItems) {
        this.context = context;
        if (initialItems != null) {
            this.items.addAll(initialItems);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCommentAvatar;
        TextView textAvatarFallback;
        TextView textCommentAuthor;
        TextView textCommentTime;
        TextView textCommentBody;
        TextView textLikeCount;
        TextView textReplyCount;
        View layoutFooter;
        View layoutRepliesLoading;
        LinearLayout containerReplies;

        public ViewHolder(View itemView) {
            super(itemView);
            imgCommentAvatar = itemView.findViewById(R.id.img_comment_avatar);
            textAvatarFallback = itemView.findViewById(R.id.text_avatar_fallback);
            textCommentAuthor = itemView.findViewById(R.id.text_comment_author);
            textCommentTime = itemView.findViewById(R.id.text_comment_time);
            textCommentBody = itemView.findViewById(R.id.text_comment_body);
            textLikeCount = itemView.findViewById(R.id.text_like_count);
            textReplyCount = itemView.findViewById(R.id.text_reply_count);
            layoutFooter = itemView.findViewById(R.id.layout_comment_footer);
            layoutRepliesLoading = itemView.findViewById(R.id.layout_replies_loading);
            containerReplies = itemView.findViewById(R.id.container_replies);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_youtube_comment, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YouTubeComment item = items.get(position);

        String author = (item.authorName != null && !item.authorName.isEmpty()) ? item.authorName : "User";
        holder.textCommentAuthor.setText(author);

        if (item.publishedTime != null && !item.publishedTime.isEmpty()) {
            holder.textCommentTime.setText(item.publishedTime);
            holder.textCommentTime.setVisibility(View.VISIBLE);
        } else {
            holder.textCommentTime.setVisibility(View.GONE);
        }

        holder.textCommentBody.setText(item.commentText != null ? item.commentText : "");

        // Likes
        if (item.likeCount != null && !item.likeCount.isEmpty() && !"0".equals(item.likeCount.trim())) {
            holder.textLikeCount.setText(item.likeCount);
        } else {
            holder.textLikeCount.setText("");
        }

        // Replies setup
        if (item.replyCount > 0) {
            holder.textReplyCount.setVisibility(View.VISIBLE);
            if (item.isRepliesExpanded) {
                holder.textReplyCount.setText("▲ Hide replies");
                holder.containerReplies.setVisibility(View.VISIBLE);
                renderReplies(holder, item);
            } else {
                String replyText = (item.replyCount == 1) ? "▼ 1 reply" : "▼ " + item.replyCount + " replies";
                holder.textReplyCount.setText(replyText);
                holder.containerReplies.setVisibility(View.GONE);
            }

            holder.layoutRepliesLoading.setVisibility(item.isLoadingReplies ? View.VISIBLE : View.GONE);

            holder.textReplyCount.setOnClickListener(v -> {
                if (item.isLoadingReplies) return;

                if (item.isRepliesExpanded) {
                    // Collapse
                    item.isRepliesExpanded = false;
                    holder.containerReplies.setVisibility(View.GONE);
                    String replyText = (item.replyCount == 1) ? "▼ 1 reply" : "▼ " + item.replyCount + " replies";
                    holder.textReplyCount.setText(replyText);
                } else {
                    // Expand
                    if (item.replies != null && !item.replies.isEmpty()) {
                        item.isRepliesExpanded = true;
                        renderReplies(holder, item);
                        holder.containerReplies.setVisibility(View.VISIBLE);
                        holder.textReplyCount.setText("▲ Hide replies");
                    } else if (item.replyContinuationToken != null && !item.replyContinuationToken.isEmpty()) {
                        item.isLoadingReplies = true;
                        holder.layoutRepliesLoading.setVisibility(View.VISIBLE);

                        new Thread(() -> {
                            YouTubeInnertubeFetcher.CommentResult repRes =
                                    YouTubeInnertubeFetcher.fetchVideoComments(null, item.replyContinuationToken);

                            holder.itemView.post(() -> {
                                item.isLoadingReplies = false;
                                holder.layoutRepliesLoading.setVisibility(View.GONE);

                                if (repRes.comments != null && !repRes.comments.isEmpty()) {
                                    item.replies.addAll(repRes.comments);
                                    item.replyContinuationToken = repRes.nextContinuationToken;
                                    item.isRepliesExpanded = true;
                                    renderReplies(holder, item);
                                    holder.containerReplies.setVisibility(View.VISIBLE);
                                    holder.textReplyCount.setText("▲ Hide replies");
                                } else {
                                    String replyText = (item.replyCount == 1) ? "▼ 1 reply" : "▼ " + item.replyCount + " replies";
                                    holder.textReplyCount.setText(replyText);
                                }
                            });
                        }).start();
                    }
                }
            });
        } else {
            holder.textReplyCount.setVisibility(View.GONE);
            holder.containerReplies.setVisibility(View.GONE);
            holder.layoutRepliesLoading.setVisibility(View.GONE);
            holder.textReplyCount.setOnClickListener(null);
        }

        // Avatar fallback letter
        String initial = "U";
        if (!author.isEmpty()) {
            String clean = author.startsWith("@") ? author.substring(1) : author;
            if (!clean.isEmpty()) {
                initial = clean.substring(0, 1).toUpperCase();
            }
        }
        holder.textAvatarFallback.setText(initial);
        holder.textAvatarFallback.setVisibility(View.VISIBLE);

        // Load avatar image with Glide
        if (item.authorAvatarUrl != null && !item.authorAvatarUrl.isEmpty()) {
            Glide.with(context)
                    .load(item.authorAvatarUrl)
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            holder.imgCommentAvatar.setVisibility(View.GONE);
                            holder.textAvatarFallback.setVisibility(View.VISIBLE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            holder.imgCommentAvatar.setVisibility(View.VISIBLE);
                            holder.textAvatarFallback.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(holder.imgCommentAvatar);
        } else {
            holder.imgCommentAvatar.setVisibility(View.GONE);
            holder.textAvatarFallback.setVisibility(View.VISIBLE);
        }
    }

    private void renderReplies(ViewHolder holder, YouTubeComment parentComment) {
        holder.containerReplies.removeAllViews();
        if (parentComment.replies == null || parentComment.replies.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(context);

        for (YouTubeComment reply : parentComment.replies) {
            View replyView = inflater.inflate(R.layout.item_youtube_reply, holder.containerReplies, false);

            ImageView imgAvatar = replyView.findViewById(R.id.img_reply_avatar);
            TextView textAvatarFallback = replyView.findViewById(R.id.text_reply_avatar_fallback);
            TextView textAuthor = replyView.findViewById(R.id.text_reply_author);
            TextView textTime = replyView.findViewById(R.id.text_reply_time);
            TextView textBody = replyView.findViewById(R.id.text_reply_body);
            TextView textLikeCount = replyView.findViewById(R.id.text_reply_like_count);

            String repAuthor = (reply.authorName != null && !reply.authorName.isEmpty()) ? reply.authorName : "User";
            textAuthor.setText(repAuthor);

            if (reply.publishedTime != null && !reply.publishedTime.isEmpty()) {
                textTime.setText(reply.publishedTime);
                textTime.setVisibility(View.VISIBLE);
            } else {
                textTime.setVisibility(View.GONE);
            }

            textBody.setText(reply.commentText != null ? reply.commentText : "");

            if (reply.likeCount != null && !reply.likeCount.isEmpty() && !"0".equals(reply.likeCount.trim())) {
                textLikeCount.setText(reply.likeCount);
            } else {
                textLikeCount.setText("");
            }

            // Avatar fallback letter
            String initial = "U";
            if (!repAuthor.isEmpty()) {
                String clean = repAuthor.startsWith("@") ? repAuthor.substring(1) : repAuthor;
                if (!clean.isEmpty()) {
                    initial = clean.substring(0, 1).toUpperCase();
                }
            }
            textAvatarFallback.setText(initial);
            textAvatarFallback.setVisibility(View.VISIBLE);

            if (reply.authorAvatarUrl != null && !reply.authorAvatarUrl.isEmpty()) {
                Glide.with(context)
                        .load(reply.authorAvatarUrl)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                imgAvatar.setVisibility(View.GONE);
                                textAvatarFallback.setVisibility(View.VISIBLE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                imgAvatar.setVisibility(View.VISIBLE);
                                textAvatarFallback.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(imgAvatar);
            } else {
                imgAvatar.setVisibility(View.GONE);
                textAvatarFallback.setVisibility(View.VISIBLE);
            }

            holder.containerReplies.addView(replyView);
        }

        // If more replies are available, show "Show more replies" button
        if (parentComment.replyContinuationToken != null && !parentComment.replyContinuationToken.isEmpty()) {
            TextView btnMoreReplies = new TextView(context);
            btnMoreReplies.setText("Show more replies");
            btnMoreReplies.setTextSize(11);
            btnMoreReplies.setTypeface(null, android.graphics.Typeface.BOLD);
            btnMoreReplies.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.colorAccent));
            btnMoreReplies.setPadding(32, 12, 16, 12);
            btnMoreReplies.setOnClickListener(v -> {
                btnMoreReplies.setText("Loading more replies...");
                btnMoreReplies.setEnabled(false);
                new Thread(() -> {
                    YouTubeInnertubeFetcher.CommentResult moreRepRes =
                            YouTubeInnertubeFetcher.fetchVideoComments(null, parentComment.replyContinuationToken);

                    holder.itemView.post(() -> {
                        if (moreRepRes.comments != null && !moreRepRes.comments.isEmpty()) {
                            parentComment.replies.addAll(moreRepRes.comments);
                            parentComment.replyContinuationToken = moreRepRes.nextContinuationToken;
                            renderReplies(holder, parentComment);
                        } else {
                            btnMoreReplies.setVisibility(View.GONE);
                        }
                    });
                }).start();
            });
            holder.containerReplies.addView(btnMoreReplies);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItems(List<YouTubeComment> newItems) {
        if (newItems == null || newItems.isEmpty()) return;
        int startPos = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(startPos, newItems.size());
    }

    public void setItems(List<YouTubeComment> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }
}
