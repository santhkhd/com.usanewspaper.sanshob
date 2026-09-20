package com.app.webdroid.activity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.app.webdroid.adapter.AdapterComments;
import com.app.webdroid.database.AppDatabase;
import com.app.webdroid.database.HistoryDao;
import com.app.webdroid.database.prefs.AdsPref;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.model.FavoriteItem;
import com.app.webdroid.model.HistoryItem;
import com.app.webdroid.model.YouTubeComment;
import com.app.webdroid.util.AdsManager;
import com.app.webdroid.util.ChannelLogoCache;
import com.app.webdroid.util.CustomChannelManager;
import com.app.webdroid.util.LetterAvatarUtil;
import com.app.webdroid.util.YouTubeInnertubeFetcher;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.shobmc.san.R;

import java.util.ArrayList;

public class ActivityVideoDetail extends AppCompatActivity {

    private String videoId, title, date, thumbUrl;
    private String channelName, channelId;
    private ImageView btnFavorite;
    private boolean isFavorite = false;
    private AppDatabase db;
    private android.webkit.WebView webViewPlayer;
    private ImageView imageThumbnail, btnPlay;
    private View overlayView;
    private View customView;
    private android.webkit.WebChromeClient.CustomViewCallback customViewCallback;

    private View layoutCommentsPreview;
    private View layoutCommentsEmptyState;
    private TextView textCommentsEmptyPrompt;
    private View layoutPreviewSnippet;
    private TextView textPreviewCount;
    private TextView textPreviewCommentSnippet;
    private TextView textPreviewAvatarFallback;
    private ImageView imgPreviewAvatar;
    private YouTubeInnertubeFetcher.CommentResult cachedCommentResult;

    private TextView textTitle;
    private TextView textSecondaryTitle;
    private TextView textDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.app.webdroid.util.Tools.getTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_detail);

        boolean isDark = new SharedPref(this).getIsDarkTheme();

        // 1. Clean status bar matching neutral background
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int bgCol = ContextCompat.getColor(this, isDark ? R.color.color_dark_background : R.color.color_light_background);
            getWindow().setStatusBarColor(bgCol);
        }
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(!isDark);
        }

        if (getIntent() != null) {
            videoId = getIntent().getStringExtra("videoId");
            title = getIntent().getStringExtra("title");
            date = getIntent().getStringExtra("date");
            thumbUrl = getIntent().getStringExtra("thumbUrl");
            channelName = getIntent().getStringExtra("channelName");
            channelId = getIntent().getStringExtra("channelId");
        }

        // Setup 56dp Clean Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(isDark ? 0xFFFFFFFF : 0xFF0F172A);
        }

        // 2. Setup 16:9 Aspect Ratio Hero Video Player
        View layoutHeader = findViewById(R.id.layout_header);
        if (layoutHeader != null) {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int target169Height = (int) (screenWidth * 9f / 16f);
            layoutHeader.getLayoutParams().height = target169Height;
        }

        imageThumbnail = findViewById(R.id.image_thumbnail);
        btnPlay = findViewById(R.id.btn_play);
        overlayView = findViewById(R.id.overlay_view);
        webViewPlayer = findViewById(R.id.web_view_player);
        MaterialButton btnOpenYoutube = findViewById(R.id.btn_open_youtube);

        // 3. Multi-tier Title Hierarchy
        textTitle = findViewById(R.id.text_title);
        textSecondaryTitle = findViewById(R.id.text_secondary_title);
        textDate = findViewById(R.id.text_date);
        btnFavorite = findViewById(R.id.btn_favorite);

        setupTitleHierarchy(title, date);

        // 4. Compact Channel Section
        View layoutChannelContainer = findViewById(R.id.layout_channel_container);
        TextView textChannelName = findViewById(R.id.text_channel_name);
        TextView textChannelSub = findViewById(R.id.text_channel_sub);
        MaterialButton btnAddChannel = findViewById(R.id.btn_add_channel);
        ImageView imgChannelBadge = findViewById(R.id.img_channel_badge);

        if (channelName != null && !channelName.isEmpty() && !"USA News".equals(channelName)) {
            if (layoutChannelContainer != null) {
                layoutChannelContainer.setVisibility(View.VISIBLE);
                int cardBgRes = isDark ? R.drawable.bg_channel_detail_card_dark : R.drawable.bg_channel_detail_card_light;
                layoutChannelContainer.setBackgroundResource(cardBgRes);

                if (textChannelName != null) {
                    textChannelName.setText(channelName);
                }
                if (textChannelSub != null) {
                    textChannelSub.setText("YouTube Channel • Official");
                }

                // Load Channel Avatar
                if (imgChannelBadge != null) {
                    loadChannelAvatar(imgChannelBadge, channelId, channelName);
                }

                if (btnAddChannel != null) {
                    updateAddChannelButtonState(btnAddChannel, channelName, isDark);

                    btnAddChannel.setOnClickListener(v -> {
                        CustomChannelManager.showAddChannelDialog(
                                this,
                                channelName,
                                channelId,
                                thumbUrl,
                                null,
                                () -> updateAddChannelButtonState(btnAddChannel, channelName, isDark)
                        );
                    });
                }
            }
        }

        // Tonal styling for "Watch on YouTube" button
        boolean isIptv = videoId != null && (videoId.startsWith("http") || videoId.contains(".m3u8"));
        if (btnOpenYoutube != null) {
            if (isIptv) {
                btnOpenYoutube.setText("Open in External Video Player");
                btnOpenYoutube.setIconResource(R.drawable.ic_external_browser);
            } else {
                btnOpenYoutube.setText("Watch on YouTube App");
                btnOpenYoutube.setIconResource(R.drawable.ic_play_circle);
            }
            if (isDark) {
                btnOpenYoutube.setBackgroundTintList(ColorStateList.valueOf(0xFF131B2B));
                btnOpenYoutube.setStrokeColor(ColorStateList.valueOf(0xFF1E293B));
                btnOpenYoutube.setTextColor(0xFFFFFFFF);
            } else {
                btnOpenYoutube.setBackgroundTintList(ColorStateList.valueOf(0xFFF1F5F9));
                btnOpenYoutube.setStrokeColor(ColorStateList.valueOf(0xFFE2E8F0));
                btnOpenYoutube.setTextColor(0xFF0F172A);
            }
            btnOpenYoutube.setOnClickListener(v -> openExternalYouTubeApp());
        }

        // 5. Comments Preview Setup
        layoutCommentsPreview = findViewById(R.id.layout_comments_preview);
        layoutCommentsEmptyState = findViewById(R.id.layout_comments_empty_state);
        textCommentsEmptyPrompt = findViewById(R.id.text_comments_empty_prompt);
        layoutPreviewSnippet = findViewById(R.id.layout_preview_snippet);
        textPreviewCount = findViewById(R.id.text_preview_count);
        textPreviewCommentSnippet = findViewById(R.id.text_preview_comment_snippet);
        textPreviewAvatarFallback = findViewById(R.id.text_preview_avatar_fallback);
        imgPreviewAvatar = findViewById(R.id.img_preview_avatar);

        if (isIptv) {
            if (layoutCommentsPreview != null) layoutCommentsPreview.setVisibility(View.GONE);
        } else {
            if (layoutCommentsPreview != null) {
                layoutCommentsPreview.setVisibility(View.VISIBLE);
                layoutCommentsPreview.setOnClickListener(v -> showCommentsBottomSheet());
            }
            loadCommentsPreview();
        }

        // Thumbnail loading
        if (thumbUrl != null) {
            Glide.with(this)
                    .load(thumbUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.mipmap.ic_launcher)
                    .into(imageThumbnail);
        }

        btnPlay.setOnClickListener(v -> playEmbeddedVideo());
        imageThumbnail.setOnClickListener(v -> playEmbeddedVideo());

        // Favorite initialization
        db = AppDatabase.getDatabase(this);
        checkFavorite();

        btnFavorite.setOnClickListener(v -> {
            btnFavorite.animate().scaleX(1.25f).scaleY(1.25f).setDuration(120)
                    .withEndAction(() -> btnFavorite.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start())
                    .start();
            toggleFavorite();
        });

        loadNativeAd();

        // Autoplay HTML5 embedded player
        playEmbeddedVideo();

        // Record to Watch History asynchronously
        recordWatchHistory();
    }

    private void recordWatchHistory() {
        if (videoId == null || videoId.trim().isEmpty()) return;
        final String vId = videoId.trim();
        final String vTitle = (title != null && !title.trim().isEmpty()) ? title.trim() : "USA News Video";
        final String vThumb = thumbUrl;
        final String vChannel = (channelName != null && !channelName.trim().isEmpty()) ? channelName.trim() : (date != null ? date : "");
        final boolean isIptv = vId.startsWith("http") || vId.contains(".m3u8");
        String vType = isIptv ? HistoryItem.TYPE_IPTV : HistoryItem.TYPE_YOUTUBE;
        if (vChannel.toLowerCase().contains("song") || vTitle.toLowerCase().contains("song") || vTitle.toLowerCase().contains("ganam")) {
            vType = HistoryItem.TYPE_SONG;
        }

        final String finalType = vType;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                if (db != null) {
                    HistoryDao dao = db.historyDao();
                    dao.deleteDuplicates(vId, vId, vTitle);
                    HistoryItem item = new HistoryItem(vId, finalType, vTitle, vChannel, vThumb, vId, System.currentTimeMillis());
                    dao.addHistory(item);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Splits long composite video titles (pipe, dash, em-dash) into main title, secondary title, and metadata.
     */
    private void setupTitleHierarchy(String rawTitle, String rawDate) {
        if (rawTitle == null || rawTitle.trim().isEmpty()) {
            return;
        }

        String mainTitle = rawTitle.trim();
        String secondaryTitle = "";

        if (mainTitle.contains("|")) {
            String[] parts = mainTitle.split("\\|");
            mainTitle = parts[0].trim();
            if (parts.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < parts.length; i++) {
                    String p = parts[i].trim();
                    if (!p.isEmpty()) {
                        if (sb.length() > 0) sb.append(" • ");
                        sb.append(p);
                    }
                }
                secondaryTitle = sb.toString();
            }
        } else if (mainTitle.contains(" — ")) {
            String[] parts = mainTitle.split(" — ");
            mainTitle = parts[0].trim();
            if (parts.length > 1) {
                secondaryTitle = parts[1].trim();
            }
        } else if (mainTitle.contains(" - ") && mainTitle.length() > 35) {
            String[] parts = mainTitle.split(" - ");
            mainTitle = parts[0].trim();
            if (parts.length > 1) {
                secondaryTitle = parts[1].trim();
            }
        }

        if (textTitle != null) {
            textTitle.setText(mainTitle);
        }

        if (textSecondaryTitle != null) {
            if (!secondaryTitle.isEmpty()) {
                textSecondaryTitle.setText(secondaryTitle);
                textSecondaryTitle.setVisibility(View.VISIBLE);
            } else {
                textSecondaryTitle.setVisibility(View.GONE);
            }
        }

        if (textDate != null) {
            String meta = (rawDate != null && !rawDate.isEmpty()) ? rawDate : "Live Stream • HD 24/7";
            meta = meta.replace("⏱️", "").trim();
            if (meta.startsWith("•")) meta = meta.substring(1).trim();
            textDate.setText(meta);
        }
    }

    private void updateAddChannelButtonState(MaterialButton btnAddChannel, String channel, boolean isDark) {
        boolean isAdded = CustomChannelManager.isChannelAddedAnywhere(this, channel);
        int accent = ContextCompat.getColor(this, R.color.colorAccent);
        if (isAdded) {
            btnAddChannel.setText("✓ Added");
            btnAddChannel.setEnabled(false);
            btnAddChannel.setTextColor(isDark ? 0xFF94A3B8 : 0xFF64748B);
            btnAddChannel.setStrokeColor(ColorStateList.valueOf(isDark ? 0xFF334155 : 0xFFCBD5E1));
            btnAddChannel.setAlpha(0.85f);
        } else {
            btnAddChannel.setText("+ Add");
            btnAddChannel.setEnabled(true);
            btnAddChannel.setTextColor(accent);
            btnAddChannel.setStrokeColor(ColorStateList.valueOf(accent));
            btnAddChannel.setAlpha(1.0f);
        }
    }

    private void loadChannelAvatar(ImageView imgChannelBadge, String cid, String cname) {
        String cached = ChannelLogoCache.getCachedLogo(this, cid);
        if (cached != null && !cached.isEmpty()) {
            Glide.with(this)
                    .load(cached)
                    .circleCrop()
                    .placeholder(R.drawable.ic_live_tv)
                    .into(imgChannelBadge);
        } else {
            int avatarSize = (int) (38 * getResources().getDisplayMetrics().density);
            android.graphics.drawable.Drawable letterAvatar =
                    LetterAvatarUtil.createLetterAvatar(this, (cname != null && !cname.isEmpty()) ? cname : "C", avatarSize);
            imgChannelBadge.setImageDrawable(letterAvatar);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_video_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.menu_share) {
            shareVideo();
            return true;
        } else if (id == R.id.menu_copy_link) {
            copyVideoLink();
            return true;
        } else if (id == R.id.menu_open_youtube) {
            openExternalYouTubeApp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareVideo() {
        String shareUrl = (videoId != null && (videoId.startsWith("http") || videoId.contains(".m3u8")))
                ? videoId : ("https://youtu.be/" + videoId);
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, (title != null ? title + "\n" : "") + shareUrl);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share Video"));
    }

    private void copyVideoLink() {
        String shareUrl = (videoId != null && (videoId.startsWith("http") || videoId.contains(".m3u8")))
                ? videoId : ("https://youtu.be/" + videoId);
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Video Link", shareUrl);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Link copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadNativeAd() {
        // AdMob Policy Compliance (Valuable inventory: Replicated content)
        // and YouTube API Terms of Service (III.D.4):
        // Ads must not be displayed alongside third-party/unowned video audiovisual content.
        com.google.android.material.card.MaterialCardView nativeAdContainer = findViewById(
                R.id.native_ad_view_container);
        if (nativeAdContainer != null) {
            nativeAdContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && webViewPlayer != null && webViewPlayer.getVisibility() == View.VISIBLE) {
            try {
                android.app.PictureInPictureParams params = new android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(new android.util.Rational(16, 9))
                        .build();
                enterPictureInPictureMode(params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, android.content.res.Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        View titleSection = findViewById(R.id.layout_title_section);
        View channelContainer = findViewById(R.id.layout_channel_container);
        View btnYoutube = findViewById(R.id.btn_open_youtube);

        if (isInPictureInPictureMode) {
            if (getSupportActionBar() != null) getSupportActionBar().hide();
            if (titleSection != null) titleSection.setVisibility(View.GONE);
            if (channelContainer != null) channelContainer.setVisibility(View.GONE);
            if (btnYoutube != null) btnYoutube.setVisibility(View.GONE);
            if (layoutCommentsPreview != null) layoutCommentsPreview.setVisibility(View.GONE);
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().show();
            if (titleSection != null) titleSection.setVisibility(View.VISIBLE);
            if (channelContainer != null && channelName != null && !channelName.isEmpty() && !"USA News".equals(channelName)) {
                channelContainer.setVisibility(View.VISIBLE);
            }
            if (btnYoutube != null) btnYoutube.setVisibility(View.VISIBLE);
            boolean isIptvVideo = videoId != null && (videoId.startsWith("http") || videoId.contains(".m3u8"));
            if (layoutCommentsPreview != null && !isIptvVideo) layoutCommentsPreview.setVisibility(View.VISIBLE);
        }
    }

    private void playEmbeddedVideo() {
        if (videoId == null || videoId.isEmpty()) return;

        // If videoId is an IPTV HTTP/m3u8 stream, load player directly
        if (videoId.startsWith("http") || videoId.contains(".m3u8")) {
            loadWebViewPlayer();
            return;
        }

        // If videoId is a YouTube Live Channel ID (starts with UC and 24 chars), resolve active live stream videoId
        if (videoId.startsWith("UC") && videoId.length() == 24) {
            Toast.makeText(this, "Loading live stream...", Toast.LENGTH_SHORT).show();
            if (btnPlay != null) btnPlay.setVisibility(View.GONE);
            new Thread(() -> {
                String resolvedId = YouTubeInnertubeFetcher.resolveLiveVideoId(videoId, title);
                final String finalId = resolvedId;
                runOnUiThread(() -> {
                    if (finalId != null && !finalId.isEmpty()) {
                        videoId = finalId;
                        loadCommentsPreview();
                    }
                    loadWebViewPlayer();
                });
            }).start();
        } else {
            loadWebViewPlayer();
        }
    }

    private void loadWebViewPlayer() {
        if (webViewPlayer == null) return;
        webViewPlayer.getSettings().setJavaScriptEnabled(true);
        webViewPlayer.getSettings().setDomStorageEnabled(true);
        webViewPlayer.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webViewPlayer.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webViewPlayer.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webViewPlayer.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(android.webkit.WebView view, android.webkit.WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    if (url.contains("youtube.com/watch") || url.contains("youtu.be/")) {
                        openExternalYouTubeApp();
                        return true;
                    }
                }
                return false;
            }
        });

        webViewPlayer.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    onHideCustomView();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                if (getSupportActionBar() != null) getSupportActionBar().hide();

                setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

                WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
                controller.hide(WindowInsetsCompat.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

                android.view.ViewGroup decor = (android.view.ViewGroup) getWindow().getDecorView();
                decor.addView(customView, new android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT));
                webViewPlayer.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;

                setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

                WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
                controller.show(WindowInsetsCompat.Type.systemBars());
                if (getSupportActionBar() != null) getSupportActionBar().show();

                android.view.ViewGroup decor = (android.view.ViewGroup) getWindow().getDecorView();
                decor.removeView(customView);
                customView = null;
                if (customViewCallback != null) customViewCallback.onCustomViewHidden();
                webViewPlayer.setVisibility(View.VISIBLE);
            }
        });

        boolean isIptv = videoId.startsWith("http") || videoId.contains(".m3u8");

        String html;
        if (isIptv) {
            html = "<!DOCTYPE html><html><head>" +
                    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">" +
                    "<script src=\"https://cdn.jsdelivr.net/npm/hls.js@latest\"></script>" +
                    "<style>" +
                    "html, body { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000000; overflow: hidden; display: flex; align-items: center; justify-content: center; }" +
                    "video { width: 100%; height: 100%; object-fit: contain; outline: none; }" +
                    "</style></head><body>" +
                    "<video id=\"video\" src=\"" + videoId + "\" controls autoplay playsinline webkit-playsinline></video>" +
                    "<script>" +
                    "function initPlayer() {" +
                    "  var video = document.getElementById('video');" +
                    "  var videoSrc = '" + videoId + "';" +
                    "  if (typeof Hls !== 'undefined' && Hls.isSupported()) {" +
                    "    var hls = new Hls({ enableWorker: true, lowLatencyMode: true });" +
                    "    hls.loadSource(videoSrc);" +
                    "    hls.attachMedia(video);" +
                    "    hls.on(Hls.Events.MANIFEST_PARSED, function() { video.play().catch(function(e){}); });" +
                    "    hls.on(Hls.Events.ERROR, function(event, data) {" +
                    "      if (data.fatal) {" +
                    "        hls.destroy();" +
                    "        video.src = videoSrc;" +
                    "        video.play().catch(function(e){});" +
                    "      }" +
                    "    });" +
                    "  } else {" +
                    "    video.src = videoSrc;" +
                    "    video.play().catch(function(e){});" +
                    "  }" +
                    "}" +
                    "if (document.readyState === 'loading') {" +
                    "  document.addEventListener('DOMContentLoaded', initPlayer);" +
                    "} else {" +
                    "  initPlayer();" +
                    "}" +
                    "</script></body></html>";
        } else {
            String iframeSrc;
            if (videoId.startsWith("PL") || videoId.startsWith("VL")) {
                String cleanPid = videoId.startsWith("VL") ? videoId.substring(2) : videoId;
                iframeSrc = "https://www.youtube-nocookie.com/embed/videoseries?list=" + cleanPid + "&autoplay=1&playsinline=1&enablejsapi=1&origin=https://m.youtube.com&widget_referrer=https://m.youtube.com&modestbranding=1&rel=0&showinfo=0&fs=1";
            } else {
                iframeSrc = "https://www.youtube-nocookie.com/embed/" + videoId + "?autoplay=1&playsinline=1&enablejsapi=1&origin=https://m.youtube.com&widget_referrer=https://m.youtube.com&modestbranding=1&rel=0&showinfo=0&fs=1";
            }
            html = "<!DOCTYPE html><html><head>" +
                    "<base href=\"https://m.youtube.com\">" +
                    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">" +
                    "<style>" +
                    "html, body { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000000; overflow: hidden; }" +
                    "iframe { border: 0; width: 100%; height: 100%; }" +
                    "</style></head><body>" +
                    "<iframe src=\"" + iframeSrc + "\" allowfullscreen allow=\"autoplay; encrypted-media; picture-in-picture\"></iframe>" +
                    "</body></html>";
        }

        if (imageThumbnail != null) imageThumbnail.setVisibility(View.GONE);
        if (btnPlay != null) btnPlay.setVisibility(View.GONE);
        if (overlayView != null) overlayView.setVisibility(View.GONE);
        webViewPlayer.setVisibility(View.VISIBLE);
        webViewPlayer.loadDataWithBaseURL(isIptv ? "https://localhost" : "https://m.youtube.com", html, "text/html", "UTF-8", null);
    }

    private void openExternalYouTubeApp() {
        if (videoId == null || videoId.isEmpty()) return;
        if (videoId.startsWith("http") || videoId.contains(".m3u8")) {
            try {
                Intent extIntent = new Intent(Intent.ACTION_VIEW);
                extIntent.setDataAndType(Uri.parse(videoId), "video/*");
                startActivity(extIntent);
            } catch (Exception e) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(videoId)));
                } catch (Exception ignored) {}
            }
            return;
        }

        String targetUrl;
        if (videoId.startsWith("UC") && videoId.length() == 24) {
            targetUrl = "https://www.youtube.com/channel/" + videoId + "/live";
        } else if (videoId.startsWith("PL") || videoId.startsWith("VL")) {
            String cleanPid = videoId.startsWith("VL") ? videoId.substring(2) : videoId;
            targetUrl = "https://www.youtube.com/playlist?list=" + cleanPid;
        } else {
            targetUrl = "https://www.youtube.com/watch?v=" + videoId;
        }
        boolean isSpecial = videoId.startsWith("UC") || videoId.startsWith("PL") || videoId.startsWith("VL");
        Intent appIntent = new Intent(Intent.ACTION_VIEW, isSpecial ? Uri.parse(targetUrl) : Uri.parse("vnd.youtube:" + videoId));
        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
        try {
            startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            startActivity(webIntent);
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null && webViewPlayer != null && webViewPlayer.getWebChromeClient() != null) {
            webViewPlayer.getWebChromeClient().onHideCustomView();
        } else {
            super.onBackPressed();
        }
    }

    private void checkFavorite() {
        boolean isIptv = videoId != null && (videoId.startsWith("http") || videoId.contains(".m3u8"));
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = db.favoriteDao().isFavorite(videoId, isIptv ? "IPTV" : FavoriteItem.TYPE_YOUTUBE);
            if (count == 0) {
                String targetUrl = isIptv ? videoId : ("https://www.youtube.com/watch?v=" + videoId);
                count = db.favoriteDao().isFavoriteByTargetOrTitle(targetUrl, title);
            }
            isFavorite = count > 0;
            updateFavoriteIcon();
        });
    }

    private void toggleFavorite() {
        boolean isIptv = videoId != null && (videoId.startsWith("http") || videoId.contains(".m3u8"));
        String target = isIptv ? videoId : ("https://www.youtube.com/watch?v=" + videoId);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (isFavorite) {
                db.favoriteDao().removeFavoriteComprehensive(videoId, target, title);
                isFavorite = false;
                runOnUiThread(() -> Toast.makeText(ActivityVideoDetail.this, "Removed from Favorites", Toast.LENGTH_SHORT).show());
            } else {
                FavoriteItem item = new FavoriteItem();
                item.itemId = videoId;
                item.type = isIptv ? "IPTV" : FavoriteItem.TYPE_YOUTUBE;
                item.title = title;
                item.subtitle = (date != null && !date.isEmpty()) ? date : (isIptv ? "Kerala Local IPTV • Live 24/7" : "Live Stream • 24/7");
                item.imageUrl = thumbUrl;
                item.targetUrl = target;
                db.favoriteDao().addFavorite(item);
                isFavorite = true;
                runOnUiThread(() -> Toast.makeText(ActivityVideoDetail.this, "Saved to Favorites", Toast.LENGTH_SHORT).show());
            }
            updateFavoriteIcon();
        });
    }

    private void updateFavoriteIcon() {
        runOnUiThread(() -> {
            if (btnFavorite == null) return;
            boolean isDark = new SharedPref(this).getIsDarkTheme();
            if (isFavorite) {
                btnFavorite.setImageResource(R.drawable.ic_favorite);
                btnFavorite.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent));
            } else {
                btnFavorite.setImageResource(R.drawable.ic_favorite_border);
                btnFavorite.setColorFilter(isDark ? 0xFF94A3B8 : 0xFF64748B);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webViewPlayer != null) {
            webViewPlayer.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webViewPlayer != null) {
            webViewPlayer.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webViewPlayer != null) {
            webViewPlayer.destroy();
        }
    }

    private void loadCommentsPreview() {
        if (videoId == null || videoId.isEmpty() || videoId.startsWith("http") || videoId.contains(".m3u8") || (videoId.startsWith("UC") && videoId.length() == 24)) {
            return;
        }
        final String vid = videoId;
        new Thread(() -> {
            YouTubeInnertubeFetcher.CommentResult res = YouTubeInnertubeFetcher.fetchVideoComments(vid, null);
            cachedCommentResult = res;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (res == null) return;
                if (res.commentsDisabled) {
                    if (layoutCommentsEmptyState != null) layoutCommentsEmptyState.setVisibility(View.VISIBLE);
                    if (layoutPreviewSnippet != null) layoutPreviewSnippet.setVisibility(View.GONE);
                    if (textCommentsEmptyPrompt != null) {
                        textCommentsEmptyPrompt.setText("Comments are turned off");
                    }
                    if (textPreviewCount != null) {
                        textPreviewCount.setText("Off");
                    }
                    return;
                }

                if (textPreviewCount != null && res.totalCommentsCount != null && !res.totalCommentsCount.isEmpty()) {
                    textPreviewCount.setText(res.totalCommentsCount);
                }

                if (!res.comments.isEmpty()) {
                    if (layoutCommentsEmptyState != null) layoutCommentsEmptyState.setVisibility(View.GONE);
                    if (layoutPreviewSnippet != null) layoutPreviewSnippet.setVisibility(View.VISIBLE);
                    YouTubeComment topComment = res.comments.get(0);
                    if (textPreviewCommentSnippet != null && topComment.commentText != null) {
                        textPreviewCommentSnippet.setText(topComment.commentText);
                    }
                    if (topComment.authorAvatarUrl != null && !topComment.authorAvatarUrl.isEmpty() && imgPreviewAvatar != null) {
                        Glide.with(ActivityVideoDetail.this)
                                .load(topComment.authorAvatarUrl)
                                .circleCrop()
                                .into(imgPreviewAvatar);
                        if (textPreviewAvatarFallback != null) textPreviewAvatarFallback.setVisibility(View.GONE);
                    } else if (topComment.authorName != null && !topComment.authorName.isEmpty() && textPreviewAvatarFallback != null) {
                        String clean = topComment.authorName.startsWith("@") ? topComment.authorName.substring(1) : topComment.authorName;
                        textPreviewAvatarFallback.setText(clean.isEmpty() ? "U" : clean.substring(0, 1).toUpperCase());
                    }
                } else {
                    if (layoutCommentsEmptyState != null) layoutCommentsEmptyState.setVisibility(View.VISIBLE);
                    if (layoutPreviewSnippet != null) layoutPreviewSnippet.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void showCommentsBottomSheet() {
        if (isFinishing() || isDestroyed()) return;
        final String currentVideoId = videoId;
        if (currentVideoId == null || currentVideoId.isEmpty() || currentVideoId.startsWith("http") || currentVideoId.contains(".m3u8")) {
            return;
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_comments_bottom_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView textDialogCommentCount = sheetView.findViewById(R.id.text_dialog_comment_count);
        ImageView btnClose = sheetView.findViewById(R.id.btn_close_comments);
        ProgressBar progressComments = sheetView.findViewById(R.id.progress_dialog_comments);
        ProgressBar progressMore = sheetView.findViewById(R.id.progress_dialog_more);
        TextView textEmpty = sheetView.findViewById(R.id.text_comments_empty);
        androidx.recyclerview.widget.RecyclerView recyclerView = sheetView.findViewById(R.id.recycler_dialog_comments);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }

        androidx.recyclerview.widget.LinearLayoutManager layoutManager =
                new androidx.recyclerview.widget.LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        final AdapterComments adapter = new AdapterComments(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int targetHeight = (int) (screenHeight * 0.75);
        sheetView.setMinimumHeight(targetHeight);

        // Configure expanded bottom sheet height
        bottomSheetDialog.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            android.widget.FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                android.view.ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                if (params != null) {
                    params.height = targetHeight;
                    bottomSheet.setLayoutParams(params);
                }
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setPeekHeight(targetHeight);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });

        final String[] activeContinuation = new String[]{
                cachedCommentResult != null ? cachedCommentResult.nextContinuationToken : null
        };
        final boolean[] isLoadingMore = new boolean[]{false};

        if (cachedCommentResult != null && !cachedCommentResult.comments.isEmpty()) {
            if (progressComments != null) progressComments.setVisibility(View.GONE);
            if (cachedCommentResult.totalCommentsCount != null && !cachedCommentResult.totalCommentsCount.isEmpty()) {
                if (textDialogCommentCount != null) {
                    textDialogCommentCount.setText(cachedCommentResult.totalCommentsCount);
                }
            }
            adapter.setItems(cachedCommentResult.comments);
        } else if (cachedCommentResult != null && cachedCommentResult.commentsDisabled) {
            if (progressComments != null) progressComments.setVisibility(View.GONE);
            if (textEmpty != null) {
                textEmpty.setVisibility(View.VISIBLE);
                textEmpty.setText("Comments are turned off for this video");
            }
        } else {
            if (progressComments != null) progressComments.setVisibility(View.VISIBLE);
            new Thread(() -> {
                YouTubeInnertubeFetcher.CommentResult initialRes =
                        YouTubeInnertubeFetcher.fetchVideoComments(currentVideoId, null);
                cachedCommentResult = initialRes;
                activeContinuation[0] = initialRes.nextContinuationToken;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (progressComments != null) progressComments.setVisibility(View.GONE);
                    if (initialRes.commentsDisabled) {
                        if (textEmpty != null) {
                            textEmpty.setVisibility(View.VISIBLE);
                            textEmpty.setText("Comments are turned off for this video");
                        }
                        return;
                    }
                    if (initialRes.totalCommentsCount != null && !initialRes.totalCommentsCount.isEmpty()) {
                        if (textDialogCommentCount != null) {
                            textDialogCommentCount.setText(initialRes.totalCommentsCount);
                        }
                        if (textPreviewCount != null) {
                            textPreviewCount.setText(initialRes.totalCommentsCount);
                        }
                    }
                    if (initialRes.comments.isEmpty()) {
                        if (textEmpty != null) {
                            textEmpty.setVisibility(View.VISIBLE);
                            textEmpty.setText("No comments yet");
                        }
                    } else {
                        adapter.setItems(initialRes.comments);
                    }
                });
            }).start();
        }

        // Endless scroll pagination
        recyclerView.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy <= 0) return;
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoadingMore[0] && activeContinuation[0] != null && !activeContinuation[0].isEmpty()) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 4 && firstVisibleItemPosition >= 0) {
                        isLoadingMore[0] = true;
                        if (progressMore != null) progressMore.setVisibility(View.VISIBLE);
                        new Thread(() -> {
                            YouTubeInnertubeFetcher.CommentResult moreRes =
                                    YouTubeInnertubeFetcher.fetchVideoComments(currentVideoId, activeContinuation[0]);
                            activeContinuation[0] = moreRes.nextContinuationToken;
                            runOnUiThread(() -> {
                                if (progressMore != null) progressMore.setVisibility(View.GONE);
                                isLoadingMore[0] = false;
                                if (moreRes.comments != null && !moreRes.comments.isEmpty()) {
                                    adapter.addItems(moreRes.comments);
                                }
                            });
                        }).start();
                    }
                }
            }
        });

        bottomSheetDialog.show();
    }
}
