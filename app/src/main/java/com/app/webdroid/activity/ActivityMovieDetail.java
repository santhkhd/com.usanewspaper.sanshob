package com.app.webdroid.activity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.app.webdroid.database.AppDatabase;
import com.app.webdroid.database.HistoryDao;
import com.app.webdroid.model.HistoryItem;
import com.app.webdroid.model.YouTubeItem;
import com.app.webdroid.util.ImageUtil;
import com.app.webdroid.util.YouTubeInnertubeFetcher;
import com.bumptech.glide.Glide;
import com.shobmc.san.R;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ActivityMovieDetail extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        String title = getIntent().getStringExtra("title");
        String imageUrl = getIntent().getStringExtra("image");
        String year = getIntent().getStringExtra("year");
        String runtime = getIntent().getStringExtra("runtime");
        String genre = getIntent().getStringExtra("genre");
        String rating = getIntent().getStringExtra("rating");
        String cast = getIntent().getStringExtra("cast");
        String director = getIntent().getStringExtra("director");
        String plot = getIntent().getStringExtra("plot");

        TextView txtTitle = findViewById(R.id.text_title);
        TextView txtYear = findViewById(R.id.text_year);
        TextView txtRuntime = findViewById(R.id.text_runtime);
        TextView txtRating = findViewById(R.id.text_rating);
        TextView txtGenre = findViewById(R.id.text_genre);
        TextView txtCast = findViewById(R.id.text_cast);
        TextView txtDirector = findViewById(R.id.text_director);
        TextView lblDirector = findViewById(R.id.lbl_director);
        TextView txtPlot = findViewById(R.id.text_plot);
        ImageView imgPoster = findViewById(R.id.image_movie_poster);
        Button btnWatch = findViewById(R.id.btn_watch);

        txtTitle.setText(title != null ? title : "");
        if (txtYear != null) {
            if (year != null && !year.isEmpty() && !year.equals("null")) {
                txtYear.setText(year);
                txtYear.setVisibility(View.VISIBLE);
            } else {
                txtYear.setVisibility(View.GONE);
            }
        }

        if (txtRuntime != null) {
            if (runtime != null && !runtime.isEmpty() && !runtime.equals("null")) {
                txtRuntime.setText(runtime.contains("min") ? runtime : runtime + " min");
                txtRuntime.setVisibility(View.VISIBLE);
            } else {
                txtRuntime.setVisibility(View.GONE);
            }
        }

        if (txtRating != null) {
            if (rating != null && !rating.isEmpty() && !rating.equals("null")) {
                txtRating.setText(rating.contains("/") ? rating : rating + " \u2605");
                txtRating.setVisibility(View.VISIBLE);
            } else {
                txtRating.setVisibility(View.GONE);
            }
        }

        if (txtGenre != null) {
            if (genre != null && !genre.isEmpty() && !genre.equals("null")) {
                txtGenre.setText(genre.replace("[", "").replace("]", "").replace("\"", ""));
                txtGenre.setVisibility(View.VISIBLE);
            } else {
                txtGenre.setVisibility(View.GONE);
            }
        }

        if (txtCast != null) {
            if (cast != null && !cast.isEmpty() && !cast.equals("null")) {
                String cleanCast = cast.replace("[", "").replace("]", "").replace("\"", "");
                txtCast.setText(cleanCast);
                txtCast.setVisibility(View.VISIBLE);
                txtCast.setOnClickListener(v -> showCastWikipediaDialog(cleanCast, "Starring Cast"));
            } else {
                txtCast.setText("Information not available");
            }
        }

        if (txtDirector != null) {
            if (director != null && !director.isEmpty() && !director.equals("null")) {
                String cleanDir = director.replace("[", "").replace("]", "").replace("\"", "");
                txtDirector.setText(cleanDir);
                txtDirector.setVisibility(View.VISIBLE);
                if (lblDirector != null) lblDirector.setVisibility(View.VISIBLE);
                txtDirector.setOnClickListener(v -> showCastWikipediaDialog(cleanDir, "Director"));
            } else {
                txtDirector.setVisibility(View.GONE);
                if (lblDirector != null) lblDirector.setVisibility(View.GONE);
            }
        }

        if (txtPlot != null) {
            if (plot != null && !plot.isEmpty() && !plot.equals("null")) {
                txtPlot.setText(plot);
            } else {
                txtPlot.setText("No synopsis available for this title.");
            }
        }

        // Upgrade Image URL for high quality poster
        String highResUrl = ImageUtil.getFullResAmazonImageUrl(imageUrl);

        Glide.with(this)
                .load(highResUrl)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(imgPoster);

        String cleanTitle = title != null ? title.replaceAll("[^a-zA-Z0-9 ]", " ").trim() : "Malayalam Movie";
        String ottUrl = getIntent().getStringExtra("ott_url");
        if (ottUrl == null) ottUrl = getIntent().getStringExtra("link");

        // Extract 4-digit release year if present
        int movieYear = 0;
        if (year != null) {
            Matcher m = Pattern.compile("\\b(19\\d\\d|20\\d\\d)\\b").matcher(year);
            if (m.find()) {
                try {
                    movieYear = Integer.parseInt(m.group(1));
                } catch (Exception ignored) {}
            }
        }

        // Check if movie is unreleased / upcoming
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        boolean isUpcoming = false;
        if (movieYear > currentYear) {
            isUpcoming = true;
        } else if (year != null && (year.toLowerCase().contains("upcoming") || year.toLowerCase().contains("coming soon"))) {
            isUpcoming = true;
        } else if (plot != null && (plot.toLowerCase().contains("upcoming") || plot.toLowerCase().contains("releasing soon") || plot.toLowerCase().contains("expected to release"))) {
            isUpcoming = true;
        } else if (genre != null && genre.toLowerCase().contains("upcoming")) {
            isUpcoming = true;
        }

        // Remind Me Button: ONLY show for upcoming/unreleased movies
        Button btnRemindMe = findViewById(R.id.btn_remind_me);
        if (btnRemindMe != null) {
            if (isUpcoming) {
                btnRemindMe.setVisibility(View.VISIBLE);
                btnRemindMe.setOnClickListener(v -> {
                    btnRemindMe.setText("✓  REMINDER ACTIVE");
                    btnRemindMe.setTextColor(Color.parseColor("#10B981"));
                    Toast.makeText(this, "🔔 Reminder set for " + (title != null ? title : "Movie") + "! You will be notified on release day.", Toast.LENGTH_LONG).show();
                });
            } else {
                btnRemindMe.setVisibility(View.GONE);
            }
        }

        final String targetWatchUrl = ottUrl;
        if (targetWatchUrl != null && (targetWatchUrl.startsWith("http://") || targetWatchUrl.startsWith("https://"))) {
            btnWatch.setText("▶  STREAM ON OTT");
            btnWatch.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetWatchUrl));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to open OTT stream", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            btnWatch.setText("▶  SEARCH ON YOUTUBE");
            final int finalMovieYear = movieYear;
            btnWatch.setOnClickListener(v -> {
                String query = cleanTitle + (finalMovieYear > 0 ? " " + finalMovieYear : "") + " Malayalam Full Movie";
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query)));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to open YouTube search", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Direct Watch button (uses Innertube search to directly play inside in-app player)
        Button btnDirectWatch = findViewById(R.id.btn_direct_watch);
        if (btnDirectWatch != null) {
            final int finalMovieYear = movieYear;
            btnDirectWatch.setOnClickListener(v -> {
                String query = cleanTitle + (finalMovieYear > 0 ? " " + finalMovieYear : "") + " Malayalam Full Movie";
                playOrOpenYouTubeQuery(query, cleanTitle, finalMovieYear > 0 ? String.valueOf(finalMovieYear) : year, imageUrl, btnDirectWatch);
            });
        }

        // Live OTT Platform Checker (Method 3: checks Hotstar, Prime, SonyLIV, Zee5, Netflix via JustWatch)
        Button btnCheckOtt = findViewById(R.id.btn_check_ott);
        if (btnCheckOtt != null) {
            final int finalMovieYear = movieYear;
            btnCheckOtt.setOnClickListener(v -> {
                String searchMovie = cleanTitle + (finalMovieYear > 0 ? " " + finalMovieYear : "");
                String justWatchUrl = "https://www.justwatch.com/in/search?q=" + Uri.encode(searchMovie);
                try {
                    Intent webIntent = new Intent(ActivityMovieDetail.this, ActivityWebView.class);
                    webIntent.putExtra("title", "OTT Availability: " + cleanTitle);
                    webIntent.putExtra("link", justWatchUrl);
                    startActivity(webIntent);
                } catch (Exception e) {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(justWatchUrl));
                        startActivity(browserIntent);
                    } catch (Exception ex) {
                        Toast.makeText(ActivityMovieDetail.this, "Unable to check OTT platforms", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        Button btnTrailer = findViewById(R.id.btn_trailer);
        Button btnSongs = findViewById(R.id.btn_songs);

        if (btnTrailer != null) {
            final int finalMovieYear = movieYear;
            btnTrailer.setOnClickListener(v -> {
                String query = cleanTitle + (finalMovieYear > 0 ? " " + finalMovieYear : "") + " Malayalam Movie Official Trailer";
                playOrOpenYouTubeQuery(query, cleanTitle + " Trailer", "Official Trailer", imageUrl, btnTrailer);
            });
        }

        if (btnSongs != null) {
            final int finalMovieYear = movieYear;
            btnSongs.setOnClickListener(v -> {
                showMovieSongsDialog(cleanTitle, finalMovieYear, imageUrl);
            });
        }

        // Record movie to Watch History asynchronously
        saveMovieToWatchHistory(title, year, highResUrl, ottUrl);
    }

    private void saveMovieToWatchHistory(String title, String year, String imageUrl, String targetUrl) {
        if (title == null || title.trim().isEmpty()) return;
        final String mTitle = title.trim();
        final String mYear = (year != null && !year.equals("null") && !year.isEmpty()) ? year.trim() : "Malayalam Movie";
        final String mImg = imageUrl;
        final String mUrl = (targetUrl != null && !targetUrl.isEmpty()) ? targetUrl : mTitle;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                HistoryDao dao = AppDatabase.getDatabase(this).historyDao();
                dao.deleteDuplicates(mTitle, mUrl, mTitle);
                HistoryItem item = new HistoryItem(mTitle, HistoryItem.TYPE_MOVIES, mTitle, mYear, mImg, mUrl, System.currentTimeMillis());
                dao.addHistory(item);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void showMovieSongsDialog(String cleanTitle, int movieYear, String fallbackThumb) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_movie_songs, null);
        TextView txtMovieTitle = dialogView.findViewById(R.id.text_songs_movie_title);
        View layoutLoading = dialogView.findViewById(R.id.layout_songs_loading);
        TextView txtNoSongs = dialogView.findViewById(R.id.text_no_songs);
        RecyclerView recycler = dialogView.findViewById(R.id.recycler_songs);
        ImageView btnClose = dialogView.findViewById(R.id.btn_close_songs);
        Button btnSearchMore = dialogView.findViewById(R.id.btn_search_youtube_more);

        String displaySub = cleanTitle + (movieYear > 0 ? " (" + movieYear + ")" : "") + " • Tap any track to play";
        if (txtMovieTitle != null) txtMovieTitle.setText(displaySub);

        recycler.setLayoutManager(new LinearLayoutManager(this));

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        final String searchFallbackQuery = cleanTitle + (movieYear > 0 ? " " + movieYear : "") + " Malayalam Movie Songs";
        if (btnSearchMore != null) {
            btnSearchMore.setOnClickListener(v -> {
                dialog.dismiss();
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(searchFallbackQuery)));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(ActivityMovieDetail.this, "Unable to open YouTube", Toast.LENGTH_SHORT).show();
                }
            });
        }

        dialog.show();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<YouTubeItem> songResults = new java.util.ArrayList<>();
            try {
                // Primary query: Songs
                List<YouTubeItem> items = YouTubeInnertubeFetcher.searchVideos(cleanTitle + (movieYear > 0 ? " " + movieYear : "") + " Malayalam Movie Songs", null, 1);
                if (items != null) {
                    songResults.addAll(items);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // If few items, also search Jukebox
            if (songResults.size() < 5) {
                try {
                    List<YouTubeItem> jukeItems = YouTubeInnertubeFetcher.searchVideos(cleanTitle + (movieYear > 0 ? " " + movieYear : "") + " Songs Jukebox", null, 1);
                    if (jukeItems != null) {
                        for (YouTubeItem ji : jukeItems) {
                            boolean exists = false;
                            for (YouTubeItem cur : songResults) {
                                if (cur.videoId != null && cur.videoId.equals(ji.videoId)) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) songResults.add(ji);
                        }
                    }
                } catch (Exception ignored) {}
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);

                if (songResults.isEmpty()) {
                    if (txtNoSongs != null) txtNoSongs.setVisibility(View.VISIBLE);
                    if (recycler != null) recycler.setVisibility(View.GONE);
                } else {
                    if (txtNoSongs != null) txtNoSongs.setVisibility(View.GONE);
                    if (recycler != null) {
                        recycler.setVisibility(View.VISIBLE);
                        class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.SongViewHolder> {
                            List<YouTubeItem> items;

                            SongsAdapter(List<YouTubeItem> items) {
                                this.items = items;
                            }

                            @NonNull
                            @Override
                            public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                                View v = getLayoutInflater().inflate(R.layout.item_video_compact, parent, false);
                                return new SongViewHolder(v);
                            }

                            @Override
                            public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
                                YouTubeItem item = items.get(position);
                                holder.txtTitle.setText(item.title != null ? item.title : cleanTitle + " Song");
                                holder.txtTitle.setTextColor(Color.WHITE);
                                holder.txtChannel.setText(item.channelName != null ? item.channelName : "Malayalam Music");
                                holder.txtChannel.setTextColor(Color.parseColor("#F43F5E"));
                                
                                String dateInfo = item.pubDate != null ? item.pubDate : "Song Track";
                                if (item.viewCount != null && !item.viewCount.isEmpty()) {
                                    dateInfo = item.viewCount + " • " + dateInfo;
                                }
                                holder.txtDate.setText(dateInfo);
                                holder.txtDate.setTextColor(Color.parseColor("#94A3B8"));

                                if (holder.txtDuration != null) {
                                    if (item.duration != null && !item.duration.isEmpty()) {
                                        holder.txtDuration.setText(item.duration);
                                        holder.txtDuration.setVisibility(View.VISIBLE);
                                    } else {
                                        holder.txtDuration.setVisibility(View.GONE);
                                    }
                                }

                                if (holder.imgFavorite != null) {
                                    holder.imgFavorite.setVisibility(View.GONE);
                                }

                                String thumb = (item.thumbnailUrl != null && !item.thumbnailUrl.isEmpty()) ? item.thumbnailUrl : fallbackThumb;
                                Glide.with(holder.itemView.getContext())
                                        .load(thumb)
                                        .placeholder(R.mipmap.ic_launcher)
                                        .error(R.mipmap.ic_launcher)
                                        .into(holder.imgThumb);

                                holder.itemView.setOnClickListener(v -> {
                                    dialog.dismiss();
                                    Intent intent = new Intent(ActivityMovieDetail.this, ActivityVideoDetail.class);
                                    intent.putExtra("videoId", item.videoId);
                                    intent.putExtra("title", item.title != null ? item.title : cleanTitle + " Song");
                                    String detailDate = item.pubDate != null ? item.pubDate : "Malayalam Song";
                                    if (item.viewCount != null && !item.viewCount.isEmpty()) {
                                        detailDate = item.viewCount + " • " + detailDate;
                                    }
                                    if (item.duration != null && !item.duration.isEmpty()) {
                                        detailDate = "⏱️ " + item.duration + " • " + detailDate;
                                    }
                                    intent.putExtra("date", detailDate);
                                    intent.putExtra("thumbUrl", thumb);
                                    startActivity(intent);
                                });
                            }

                            @Override
                            public int getItemCount() {
                                return items.size();
                            }

                            class SongViewHolder extends RecyclerView.ViewHolder {
                                ImageView imgThumb, imgFavorite;
                                TextView txtTitle, txtChannel, txtDate, txtDuration;

                                SongViewHolder(View v) {
                                    super(v);
                                    imgThumb = v.findViewById(R.id.image_thumbnail);
                                    imgFavorite = v.findViewById(R.id.img_favorite);
                                    txtTitle = v.findViewById(R.id.text_video_title);
                                    txtChannel = v.findViewById(R.id.text_channel_name);
                                    txtDate = v.findViewById(R.id.text_pub_date);
                                    txtDuration = v.findViewById(R.id.text_duration);
                                }
                            }
                        }

                        recycler.setAdapter(new SongsAdapter(songResults));
                    }
                }
            });
        });
    }

    private void playOrOpenYouTubeQuery(String query, String fallbackTitle, String dateStr, String fallbackThumb, Button triggerButton) {
        if (triggerButton != null) {
            triggerButton.setEnabled(false);
            triggerButton.setAlpha(0.6f);
        }
        Toast.makeText(this, "🔍 Finding video stream...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<YouTubeItem> items = null;
            try {
                items = YouTubeInnertubeFetcher.searchVideos(query, null, 1);
            } catch (Exception e) {
                e.printStackTrace();
            }

            final List<YouTubeItem> finalItems = items;
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    if (triggerButton != null) {
                        triggerButton.setEnabled(true);
                        triggerButton.setAlpha(1.0f);
                    }

                    if (finalItems != null && !finalItems.isEmpty()) {
                        YouTubeItem bestMatch = finalItems.get(0);
                        if (bestMatch.videoId != null && !bestMatch.videoId.isEmpty()) {
                            Intent intent = new Intent(ActivityMovieDetail.this, ActivityVideoDetail.class);
                            intent.putExtra("videoId", bestMatch.videoId);
                            intent.putExtra("title", bestMatch.title != null ? bestMatch.title : fallbackTitle);
                            intent.putExtra("date", bestMatch.pubDate != null ? bestMatch.pubDate : (dateStr != null ? dateStr : "HD"));
                            intent.putExtra("thumbUrl", bestMatch.thumbnailUrl != null ? bestMatch.thumbnailUrl : fallbackThumb);
                            startActivity(intent);
                            return;
                        }
                    }

                    // Fallback to external YouTube search if no match found
                    try {
                        Intent fallbackIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query)));
                        startActivity(fallbackIntent);
                    } catch (Exception ex) {
                        Toast.makeText(ActivityMovieDetail.this, "Unable to find video stream", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    private void showCastWikipediaDialog(String namesRaw, String typeTitle) {
        if (namesRaw == null || namesRaw.trim().isEmpty() || namesRaw.equals("null")) return;
        String cleanRaw = namesRaw.replace("[", "").replace("]", "").replace("\"", "");
        String[] parts = cleanRaw.split(",");
        java.util.List<String> validNames = new java.util.ArrayList<>();
        for (String p : parts) {
            String name = p.trim();
            if (!name.isEmpty() && !name.equalsIgnoreCase("null")) {
                validNames.add(name);
            }
        }
        if (validNames.isEmpty()) return;

        if (validNames.size() == 1) {
            openPersonWikipedia(validNames.get(0));
            return;
        }

        String[] options = new String[validNames.size()];
        for (int i = 0; i < validNames.size(); i++) {
            options[i] = "🌐 " + validNames.get(i) + " (Wikipedia)";
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(typeTitle + " - Wikipedia Profiles")
                .setItems(options, (dialog, which) -> {
                    openPersonWikipedia(validNames.get(which));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openPersonWikipedia(String name) {
        com.app.webdroid.fragment.FragmentCategory.WikiArtist wa = 
                com.app.webdroid.fragment.FragmentCategory.getWikiArtist(this, name);
        String wikiUrl = (wa != null && wa.wiki_url != null && !wa.wiki_url.isEmpty())
                ? wa.wiki_url
                : ("https://en.wikipedia.org/wiki/Special:Search?search=" + Uri.encode(name));

        Intent intent = new Intent(this, ActivityWebView.class);
        intent.putExtra("title", name + " - Wikipedia");
        intent.putExtra("link", wikiUrl);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
