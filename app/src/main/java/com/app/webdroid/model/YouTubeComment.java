package com.app.webdroid.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class YouTubeComment implements Serializable {
    public String commentId;
    public String authorName;
    public String authorAvatarUrl;
    public String commentText;
    public String publishedTime;
    public String likeCount;
    public int replyCount;
    public String replyContinuationToken;
    public List<YouTubeComment> replies = new ArrayList<>();
    public boolean isRepliesExpanded = false;
    public boolean isLoadingReplies = false;
    public boolean isReply = false;

    public YouTubeComment() {
    }

    public YouTubeComment(String commentId, String authorName, String authorAvatarUrl,
                          String commentText, String publishedTime, String likeCount, int replyCount) {
        this.commentId = commentId;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.commentText = commentText;
        this.publishedTime = publishedTime;
        this.likeCount = likeCount;
        this.replyCount = replyCount;
    }
}
