package com.app.webdroid.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.text.HtmlCompat;

import com.app.webdroid.Config;
import com.shobmc.san.R;
import com.app.webdroid.database.AppDatabase;
import com.app.webdroid.model.FavoriteItem;
import com.app.webdroid.model.NewsItem;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class ActivityNewsDetail extends AppCompatActivity {

    private String title = "";
    private String description = "";
    private String link = "";
    private String imageUrl = "";
    private String pubDate = "";
    private String sourceName = "";
    private String newsId = "";

    private ImageView imgLike;
    private ImageView imgDislike;
    private TextView txtLikeCount;
    private TextView txtDislikeCount;
    private TextView txtBarCommentCount;
    private LinearLayout containerComments;

    private int baseLikes = 24;
    private int baseDislikes = 2;
    private AppDatabase db;
    private boolean isFavorite = false;

    // Text-to-Speech (Audio News Reader)
    private TextToSpeech tts;
    private boolean isTtsPlaying = false;
    private float ttsSpeed = 1.0f;
    private TextView tvTtsStatus;
    private ImageView btnTtsPlay;
    private ImageView btnTtsStop;
    private TextView btnTtsSpeed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        db = AppDatabase.getDatabase(this);

        if (getIntent() != null) {
            title = getIntent().getStringExtra("title");
            description = getIntent().getStringExtra("description");
            link = getIntent().getStringExtra("link");
            imageUrl = getIntent().getStringExtra("imageUrl");
            pubDate = getIntent().getStringExtra("pubDate");
            sourceName = getIntent().getStringExtra("sourceName");
        }

        if (title == null) title = "";
        if (description == null) description = "";
        if (link == null) link = "";
        if (sourceName == null || sourceName.isEmpty()) sourceName = "USA News";

        newsId = String.valueOf(Math.abs((link + title).hashCode()));

        Toolbar toolbar = findViewById(R.id.toolbar_news);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(sourceName);
        }

        initViews();
        setupTextToSpeech();
        setupReactions();
        setupComments();
        fetchArticleDataFromGoogleSheet();
    }

    private void initViews() {
        TextView txtTitle = findViewById(R.id.text_news_detail_title);
        TextView txtCategory = findViewById(R.id.text_news_detail_category);
        TextView txtDate = findViewById(R.id.text_news_detail_date);
        TextView txtDesc = findViewById(R.id.text_news_detail_description);
        ImageView imgNews = findViewById(R.id.image_news_detail);
        View btnContinueReading = findViewById(R.id.btn_continue_reading);

        txtTitle.setText(title);
        txtCategory.setText(sourceName);
        txtDate.setText(pubDate != null && !pubDate.isEmpty() ? pubDate : "Today");

        // Format description
        String cleanDesc = description != null ? android.text.Html.fromHtml(description).toString().trim() : "";
        if (cleanDesc.isEmpty()) {
            cleanDesc = title;
        }
        txtDesc.setText(cleanDesc);

        View cardImage = findViewById(R.id.card_image_news_detail);
        boolean hasValidImage = imageUrl != null && !imageUrl.trim().isEmpty()
                && !imageUrl.contains("google.com/s2/favicons")
                && !imageUrl.contains("icons8.com")
                && !imageUrl.endsWith(".ico")
                && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"));

        if (cardImage != null) {
            if (hasValidImage) {
                cardImage.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_placeholder_media)
                        .error(R.drawable.ic_placeholder_media)
                        .into(imgNews);
            } else {
                cardImage.setVisibility(View.GONE);
            }
        }

        // CONTINUE READING opens full article in WebView
        btnContinueReading.setOnClickListener(v -> openFullArticleInWebView());

        // Floating bottom bar triggers
        View barWriteComment = findViewById(R.id.bar_write_comment);
        if (barWriteComment != null) {
            barWriteComment.setOnClickListener(v -> showCommentLoginDialog());
        }

        View btnOpenAddComment = findViewById(R.id.btn_open_add_comment);
        if (btnOpenAddComment != null) {
            btnOpenAddComment.setOnClickListener(v -> showCommentLoginDialog());
        }

        View barShareButton = findViewById(R.id.bar_share_button);
        if (barShareButton != null) {
            barShareButton.setOnClickListener(v -> shareNewsArticle());
        }
    }

    private void openFullArticleInWebView() {
        if (link == null || link.isEmpty()) return;
        Intent intent = new Intent(this, ActivityWebView.class);
        intent.putExtra("title", title);
        intent.putExtra("link", link);
        startActivity(intent);
    }

    private void shareNewsArticle() {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, title);
            intent.putExtra(Intent.EXTRA_TEXT, title + "\n\nRead more at: " + link);
            startActivity(Intent.createChooser(intent, "Share News"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupReactions() {
        View btnLike = findViewById(R.id.btn_news_like);
        View btnDislike = findViewById(R.id.btn_news_dislike);
        imgLike = findViewById(R.id.img_news_like);
        imgDislike = findViewById(R.id.img_news_dislike);
        txtLikeCount = findViewById(R.id.text_news_like_count);
        txtDislikeCount = findViewById(R.id.text_news_dislike_count);

        int hash = Math.abs(newsId.hashCode());
        baseLikes = (hash % 120) + 18;
        baseDislikes = (hash % 8) + 1;

        SharedPreferences sp = getSharedPreferences("news_reactions", MODE_PRIVATE);
        String reactionKey = "reaction_state_" + newsId;
        String legacyLikeKey = "liked_" + newsId;
        String legacyDislikeKey = "disliked_" + newsId;

        String currentReaction = sp.getString(reactionKey, null);
        if (currentReaction == null) {
            if (sp.getBoolean(legacyLikeKey, false)) {
                currentReaction = "like";
            } else if (sp.getBoolean(legacyDislikeKey, false)) {
                currentReaction = "dislike";
            } else {
                currentReaction = "none";
            }
        }

        updateReactionUI(currentReaction);

        btnLike.setOnClickListener(v -> {
            String curr = sp.getString(reactionKey, "none");
            String newReaction;
            if ("like".equals(curr)) {
                // Tapping like again cancels/un-likes
                newReaction = "none";
                if (baseLikes > 0) baseLikes--;
                Toast.makeText(this, "Unliked", Toast.LENGTH_SHORT).show();
            } else if ("dislike".equals(curr)) {
                // Switching from dislike to like
                newReaction = "like";
                if (baseDislikes > 0) baseDislikes--;
                baseLikes++;
                Toast.makeText(this, "Liked!", Toast.LENGTH_SHORT).show();
            } else {
                // Neutral to like
                newReaction = "like";
                baseLikes++;
                Toast.makeText(this, "Liked!", Toast.LENGTH_SHORT).show();
            }

            sp.edit()
                    .putString(reactionKey, newReaction)
                    .putBoolean(legacyLikeKey, "like".equals(newReaction))
                    .putBoolean(legacyDislikeKey, "dislike".equals(newReaction))
                    .apply();

            updateReactionUI(newReaction);
            postReactionToGoogleSheet(newReaction);
        });

        btnDislike.setOnClickListener(v -> {
            String curr = sp.getString(reactionKey, "none");
            String newReaction;
            if ("dislike".equals(curr)) {
                // Tapping dislike again cancels/un-dislikes
                newReaction = "none";
                if (baseDislikes > 0) baseDislikes--;
                Toast.makeText(this, "Removed dislike", Toast.LENGTH_SHORT).show();
            } else if ("like".equals(curr)) {
                // Switching from like to dislike
                newReaction = "dislike";
                if (baseLikes > 0) baseLikes--;
                baseDislikes++;
                Toast.makeText(this, "Disliked", Toast.LENGTH_SHORT).show();
            } else {
                // Neutral to dislike
                newReaction = "dislike";
                baseDislikes++;
                Toast.makeText(this, "Disliked", Toast.LENGTH_SHORT).show();
            }

            sp.edit()
                    .putString(reactionKey, newReaction)
                    .putBoolean(legacyLikeKey, "like".equals(newReaction))
                    .putBoolean(legacyDislikeKey, "dislike".equals(newReaction))
                    .apply();

            updateReactionUI(newReaction);
            postReactionToGoogleSheet(newReaction);
        });
    }

    private void updateReactionUI(String reaction) {
        if (txtLikeCount != null) {
            txtLikeCount.setText(String.valueOf(baseLikes));
        }
        if (txtDislikeCount != null) {
            txtDislikeCount.setText(String.valueOf(baseDislikes));
        }
        if (imgLike != null) {
            imgLike.setColorFilter("like".equals(reaction)
                    ? Color.parseColor("#10B981")
                    : Color.parseColor("#94A3B8"));
        }
        if (imgDislike != null) {
            imgDislike.setColorFilter("dislike".equals(reaction)
                    ? Color.parseColor("#EF4444")
                    : Color.parseColor("#94A3B8"));
        }
    }

    private void setupComments() {
        containerComments = findViewById(R.id.container_news_comments);
        txtBarCommentCount = findViewById(R.id.bar_comment_count);
        renderComments();
    }

    private static final int[] AVATAR_COLORS = new int[]{
            Color.parseColor("#E11D48"), Color.parseColor("#2563EB"),
            Color.parseColor("#059669"), Color.parseColor("#7C3AED"),
            Color.parseColor("#D97706"), Color.parseColor("#0891B2")
    };

    private void renderComments() {
        if (containerComments == null) return;
        containerComments.removeAllViews();

        SharedPreferences sp = getSharedPreferences("news_comments", MODE_PRIVATE);
        String savedJson = sp.getString("comments_" + newsId, null);

        List<CommentItem> list = new ArrayList<>();
        if (savedJson != null && !savedJson.isEmpty()) {
            try {
                Type listType = new TypeToken<List<CommentItem>>() {}.getType();
                list = new Gson().fromJson(savedJson, listType);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Filter out any default mock comments so they are permanently removed
        List<CommentItem> filteredList = new ArrayList<>();
        if (list != null) {
            for (CommentItem c : list) {
                if (c != null && c.name != null) {
                    if ("Arun Kumar".equals(c.name) || "Sangeetha Pillai".equals(c.name) || "Muhammed Shafi".equals(c.name)) {
                        continue;
                    }
                    filteredList.add(c);
                }
            }
        }

        if (txtBarCommentCount != null) {
            txtBarCommentCount.setText(String.valueOf(filteredList.size()));
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        if (filteredList.isEmpty()) {
            View emptyView = inflater.inflate(R.layout.item_comment_empty, containerComments, false);
            emptyView.setOnClickListener(v -> showCommentLoginDialog());
            containerComments.addView(emptyView);
            return;
        }

        for (CommentItem c : filteredList) {
            View itemView = inflater.inflate(R.layout.item_comment, containerComments, false);
            TextView txtAvatar = itemView.findViewById(R.id.text_avatar);
            TextView txtName = itemView.findViewById(R.id.text_commenter_name);
            TextView txtTime = itemView.findViewById(R.id.text_comment_time);
            TextView txtBody = itemView.findViewById(R.id.text_comment_body);

            String displayName = (c.name != null && !c.name.trim().isEmpty()) ? c.name.trim() : "Reader";
            txtName.setText(displayName);
            txtAvatar.setText(displayName.substring(0, 1).toUpperCase());

            // Set distinct colored avatar background
            int colorIdx = Math.abs(displayName.hashCode()) % AVATAR_COLORS.length;
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(AVATAR_COLORS[colorIdx]);
            txtAvatar.setBackground(gd);

            txtTime.setText(c.time != null ? c.time : "Just now");
            txtBody.setText(c.body != null ? c.body : "");

            containerComments.addView(itemView);
        }
    }

    private void showCommentLoginDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_news_comment_login, null);
        dialog.setContentView(dialogView);

        SharedPreferences userSp = getSharedPreferences("user_profile", MODE_PRIVATE);
        TextInputEditText editName = dialogView.findViewById(R.id.input_user_name);
        TextInputEditText editEmail = dialogView.findViewById(R.id.input_user_email);
        TextInputEditText editComment = dialogView.findViewById(R.id.input_user_comment);
        View btnContinue = dialogView.findViewById(R.id.btn_comment_continue);
        View btnSkip = dialogView.findViewById(R.id.btn_comment_skip);

        // Pre-fill remembered name and email
        if (editName != null) editName.setText(userSp.getString("name", ""));
        if (editEmail != null) editEmail.setText(userSp.getString("email", ""));

        if (btnSkip != null) {
            btnSkip.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> {
                String commentText = (editComment != null && editComment.getText() != null)
                        ? editComment.getText().toString().trim() : "";
                if (commentText.isEmpty()) {
                    Toast.makeText(this, "Please enter your comment", Toast.LENGTH_SHORT).show();
                    return;
                }

                String name = (editName != null && editName.getText() != null && !editName.getText().toString().trim().isEmpty())
                        ? editName.getText().toString().trim() : "Kerala News Reader";
                String email = (editEmail != null && editEmail.getText() != null)
                        ? editEmail.getText().toString().trim() : "";

                // Save user profile for next time
                userSp.edit()
                        .putString("name", name)
                        .putString("email", email)
                        .apply();

                // Save comment locally
                SharedPreferences commentSp = getSharedPreferences("news_comments", MODE_PRIVATE);
                String key = "comments_" + newsId;
                String savedJson = commentSp.getString(key, null);
                List<CommentItem> list = new ArrayList<>();
                if (savedJson != null && !savedJson.isEmpty()) {
                    try {
                        Type listType = new TypeToken<List<CommentItem>>() {}.getType();
                        list = new Gson().fromJson(savedJson, listType);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (list == null) list = new ArrayList<>();
                list.add(0, new CommentItem(name, commentText, "Just now"));
                commentSp.edit().putString(key, new Gson().toJson(list)).apply();

                // Submit to Google Sheet / Webhook asynchronously if configured
                submitCommentToGoogleSheet(name, email, commentText, title);

                renderComments();
                Toast.makeText(this, "Comment posted successfully!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void submitCommentToGoogleSheet(String name, String email, String comment, String articleTitle) {
        new Thread(() -> {
            try {
                String sheetEndpoint = getGoogleSheetEndpoint();
                if (sheetEndpoint != null && sheetEndpoint.startsWith("http")) {
                    URL url = new URL(sheetEndpoint);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);

                    String payload = "action=" + URLEncoder.encode("addComment", "UTF-8")
                            + "&articleId=" + URLEncoder.encode(newsId, "UTF-8")
                            + "&name=" + URLEncoder.encode(name, "UTF-8")
                            + "&email=" + URLEncoder.encode(email, "UTF-8")
                            + "&comment=" + URLEncoder.encode(comment, "UTF-8")
                            + "&title=" + URLEncoder.encode(articleTitle, "UTF-8")
                            + "&timestamp=" + URLEncoder.encode(String.valueOf(System.currentTimeMillis()), "UTF-8");

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(payload.getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    }
                    conn.getResponseCode();
                    conn.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String getAnonymousDeviceId() {
        try {
            String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (id != null && !id.trim().isEmpty()) {
                return id;
            }
        } catch (Exception ignored) {}
        SharedPreferences sp = getSharedPreferences("app_device_meta", MODE_PRIVATE);
        String uuid = sp.getString("device_uuid", null);
        if (uuid == null) {
            uuid = java.util.UUID.randomUUID().toString();
            sp.edit().putString("device_uuid", uuid).apply();
        }
        return uuid;
    }

    private void postReactionToGoogleSheet(String reactionVote) {
        new Thread(() -> {
            try {
                String sheetEndpoint = getGoogleSheetEndpoint();
                if (sheetEndpoint == null || !sheetEndpoint.startsWith("http")) return;

                String deviceId = getAnonymousDeviceId();

                URL url = new URL(sheetEndpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                String payload = "action=" + URLEncoder.encode("reaction", "UTF-8")
                        + "&vote=" + URLEncoder.encode(reactionVote, "UTF-8")
                        + "&articleId=" + URLEncoder.encode(newsId, "UTF-8")
                        + "&deviceId=" + URLEncoder.encode(deviceId, "UTF-8")
                        + "&title=" + URLEncoder.encode(title, "UTF-8")
                        + "&timestamp=" + URLEncoder.encode(String.valueOf(System.currentTimeMillis()), "UTF-8");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception ignored) {}
        }).start();
    }

    private void fetchArticleDataFromGoogleSheet() {
        new Thread(() -> {
            try {
                String sheetEndpoint = getGoogleSheetEndpoint();
                if (sheetEndpoint == null || !sheetEndpoint.startsWith("http")) return;

                String deviceId = getAnonymousDeviceId();
                String queryUrl = sheetEndpoint + (sheetEndpoint.contains("?") ? "&" : "?")
                        + "action=getArticleData&articleId=" + URLEncoder.encode(newsId, "UTF-8")
                        + "&deviceId=" + URLEncoder.encode(deviceId, "UTF-8")
                        + "&title=" + URLEncoder.encode(title, "UTF-8");

                String response = makeGetRequestWithRedirects(queryUrl);
                if (response != null && !response.isEmpty()) {
                    JSONObject json = new JSONObject(response);
                    if ("success".equalsIgnoreCase(json.optString("status"))) {
                        int remoteLikes = json.optInt("likes", -1);
                        int remoteDislikes = json.optInt("dislikes", -1);
                        String remoteUserVote = json.optString("userVote", null);
                        JSONArray remoteComments = json.optJSONArray("comments");

                        runOnUiThread(() -> {
                            if (remoteLikes >= 0) {
                                baseLikes = remoteLikes;
                            }
                            if (remoteDislikes >= 0) {
                                baseDislikes = remoteDislikes;
                            }
                            if (remoteUserVote != null && !remoteUserVote.isEmpty()) {
                                SharedPreferences sp = getSharedPreferences("news_reactions", MODE_PRIVATE);
                                sp.edit()
                                        .putString("reaction_state_" + newsId, remoteUserVote)
                                        .putBoolean("liked_" + newsId, "like".equals(remoteUserVote))
                                        .putBoolean("disliked_" + newsId, "dislike".equals(remoteUserVote))
                                        .apply();
                                updateReactionUI(remoteUserVote);
                            } else {
                                SharedPreferences sp = getSharedPreferences("news_reactions", MODE_PRIVATE);
                                String curr = sp.getString("reaction_state_" + newsId, "none");
                                updateReactionUI(curr);
                            }

                            if (remoteComments != null && remoteComments.length() > 0) {
                                List<CommentItem> list = new ArrayList<>();
                                for (int i = 0; i < remoteComments.length(); i++) {
                                    JSONObject cObj = remoteComments.optJSONObject(i);
                                    if (cObj != null) {
                                        list.add(new CommentItem(
                                                cObj.optString("name", "Reader"),
                                                cObj.optString("comment", ""),
                                                cObj.optString("date", "Recently")
                                        ));
                                    }
                                }
                                if (!list.isEmpty()) {
                                    saveAndRenderRemoteComments(list);
                                }
                            }
                        });
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private String makeGetRequestWithRedirects(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 303) {
                String newUrl = conn.getHeaderField("Location");
                conn.disconnect();
                conn = (HttpURLConnection) new URL(newUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                status = conn.getResponseCode();
            }

            if (status == 200) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                conn.disconnect();
                return sb.toString();
            }
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void saveAndRenderRemoteComments(List<CommentItem> list) {
        if (list == null || list.isEmpty()) return;
        SharedPreferences commentSp = getSharedPreferences("news_comments", MODE_PRIVATE);
        String key = "comments_" + newsId;
        commentSp.edit().putString(key, new Gson().toJson(list)).apply();
        renderComments();
    }

    private String getGoogleSheetEndpoint() {
        String sheetEndpoint = Config.GOOGLE_SHEET_WEBAPP_URL;
        if (sheetEndpoint == null || sheetEndpoint.trim().isEmpty()) {
            sheetEndpoint = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .getString("google_sheet_url", "");
        }
        return sheetEndpoint != null ? sheetEndpoint.trim() : "";
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupTextToSpeech() {
        tvTtsStatus = findViewById(R.id.tv_tts_status);
        btnTtsPlay = findViewById(R.id.btn_tts_play);
        btnTtsStop = findViewById(R.id.btn_tts_stop);
        btnTtsSpeed = findViewById(R.id.btn_tts_speed);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(java.util.Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(java.util.Locale.getDefault());
                }
                tts.setSpeechRate(ttsSpeed);
            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                runOnUiThread(() -> {
                    isTtsPlaying = true;
                    if (btnTtsPlay != null) btnTtsPlay.setImageResource(R.drawable.ic_pause);
                    if (btnTtsStop != null) btnTtsStop.setVisibility(View.VISIBLE);
                    if (tvTtsStatus != null) tvTtsStatus.setText("Playing audio briefing...");
                });
            }

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    isTtsPlaying = false;
                    if (btnTtsPlay != null) btnTtsPlay.setImageResource(R.drawable.ic_play_arrow);
                    if (btnTtsStop != null) btnTtsStop.setVisibility(View.GONE);
                    if (tvTtsStatus != null) tvTtsStatus.setText("Audio briefing finished");
                });
            }

            @Override
            public void onError(String utteranceId) {
                runOnUiThread(() -> {
                    isTtsPlaying = false;
                    if (btnTtsPlay != null) btnTtsPlay.setImageResource(R.drawable.ic_play_arrow);
                    if (btnTtsStop != null) btnTtsStop.setVisibility(View.GONE);
                    if (tvTtsStatus != null) tvTtsStatus.setText("Audio playback error");
                });
            }
        });

        if (btnTtsPlay != null) {
            btnTtsPlay.setOnClickListener(v -> {
                if (isTtsPlaying) {
                    pauseOrStopTts();
                } else {
                    speakArticle();
                }
            });
        }

        if (btnTtsStop != null) {
            btnTtsStop.setOnClickListener(v -> pauseOrStopTts());
        }

        if (btnTtsSpeed != null) {
            btnTtsSpeed.setOnClickListener(v -> {
                if (ttsSpeed == 1.0f) {
                    ttsSpeed = 1.25f;
                } else if (ttsSpeed == 1.25f) {
                    ttsSpeed = 1.5f;
                } else if (ttsSpeed == 1.5f) {
                    ttsSpeed = 2.0f;
                } else {
                    ttsSpeed = 1.0f;
                }
                btnTtsSpeed.setText(String.format(java.util.Locale.US, "%.1fx", ttsSpeed).replace(".0x", "x").replace("1x", "1.0x"));
                if (tts != null) {
                    tts.setSpeechRate(ttsSpeed);
                }
            });
        }
    }

    private void speakArticle() {
        if (tts == null) return;
        String cleanDesc = HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim();
        String textToRead = title + ". " + cleanDesc;
        if (textToRead.trim().isEmpty()) {
            Toast.makeText(this, "No article text to read", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "news_article_tts");
        tts.speak(textToRead, TextToSpeech.QUEUE_FLUSH, params, "news_article_tts");
    }

    private void pauseOrStopTts() {
        if (tts != null) {
            tts.stop();
        }
        isTtsPlaying = false;
        if (btnTtsPlay != null) btnTtsPlay.setImageResource(R.drawable.ic_play_arrow);
        if (btnTtsStop != null) btnTtsStop.setVisibility(View.GONE);
        if (tvTtsStatus != null) tvTtsStatus.setText("Audio briefing paused");
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseOrStopTts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    public static class CommentItem {
        public String name;
        public String body;
        public String time;

        public CommentItem() {}
        public CommentItem(String name, String body, String time) {
            this.name = name;
            this.body = body;
            this.time = time;
        }
    }
}
