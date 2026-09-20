package com.app.webdroid.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.webdroid.activity.ActivityVideoDetail;
import com.app.webdroid.model.MalayalamSong;
import com.app.webdroid.util.MalayalamSongManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.shobmc.san.R;

import java.util.ArrayList;
import java.util.List;

public class AdapterSongs extends RecyclerView.Adapter<AdapterSongs.SongViewHolder> {

    private final Context context;
    private final List<MalayalamSong> songs = new ArrayList<>();

    public interface OnSongInteractionListener {
        void onMovieClick(String movieName);
        void onShareSong(MalayalamSong song);
    }

    private OnSongInteractionListener interactionListener;

    public AdapterSongs(Context context) {
        this.context = context;
    }

    public void setInteractionListener(OnSongInteractionListener listener) {
        this.interactionListener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSongs(List<MalayalamSong> newSongs) {
        songs.clear();
        if (newSongs != null) {
            songs.addAll(newSongs);
        }
        notifyDataSetChanged();
    }

    public void addMoreSongs(List<MalayalamSong> moreSongs) {
        if (moreSongs != null && !moreSongs.isEmpty()) {
            int start = songs.size();
            songs.addAll(moreSongs);
            notifyItemRangeInserted(start, moreSongs.size());
        }
    }

    public void stopCurrentPlayer() {
        // No inline player to stop
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_malayalam_song, parent, false);
        return new SongViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        MalayalamSong song = songs.get(position);
        holder.bind(song);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public class SongViewHolder extends RecyclerView.ViewHolder {
        final TextView tvSongTitle;
        final TextView tvMovieTitle;
        final TextView tvSongYearBadge;
        final TextView tvSongArtists;
        final TextView tvSongCredits;
        final TextView tvSongRaga;
        final MaterialButton btnPlayYouTube;
        final MaterialButton btnSearchYouTube;
        final MaterialButton btnViewLyrics;
        final ProgressBar progressSongAction;
        final ImageView btnShareSong;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSongTitle = itemView.findViewById(R.id.tv_song_title);
            tvMovieTitle = itemView.findViewById(R.id.tv_movie_title);
            tvSongYearBadge = itemView.findViewById(R.id.tv_song_year_badge);
            tvSongArtists = itemView.findViewById(R.id.tv_song_artists);
            tvSongCredits = itemView.findViewById(R.id.tv_song_credits);
            tvSongRaga = itemView.findViewById(R.id.tv_song_raga);
            btnPlayYouTube = itemView.findViewById(R.id.btn_play_youtube);
            btnSearchYouTube = itemView.findViewById(R.id.btn_search_youtube);
            btnViewLyrics = itemView.findViewById(R.id.btn_view_lyrics);
            progressSongAction = itemView.findViewById(R.id.progress_song_action);
            btnShareSong = itemView.findViewById(R.id.btn_share_song);
        }

        public void bind(MalayalamSong song) {
            tvSongTitle.setText(song.getCleanSong());

            // Movie Name & Click
            String movie = song.getCleanMovie();
            if (!movie.isEmpty()) {
                tvMovieTitle.setVisibility(View.VISIBLE);
                tvMovieTitle.setText(movie);
                tvMovieTitle.setOnClickListener(v -> {
                    if (interactionListener != null) {
                        interactionListener.onMovieClick(movie);
                    }
                });
            } else {
                tvMovieTitle.setVisibility(View.GONE);
            }

            // Year Badge
            String year = song.getCleanYear();
            if (!year.isEmpty()) {
                tvSongYearBadge.setVisibility(View.VISIBLE);
                tvSongYearBadge.setText(year);
            } else {
                tvSongYearBadge.setVisibility(View.GONE);
            }

            // Artists / Singers
            String artists = song.getDisplayArtists();
            if (!artists.isEmpty()) {
                tvSongArtists.setVisibility(View.VISIBLE);
                tvSongArtists.setText("🎙️ " + artists);
            } else {
                tvSongArtists.setVisibility(View.GONE);
            }

            // Musician & Lyricist
            String musician = song.getDisplayMusician();
            String lyricist = song.getDisplayLyricist();
            StringBuilder creditsSb = new StringBuilder();
            if (!musician.isEmpty()) {
                creditsSb.append("🎼 ").append(musician);
            }
            if (!lyricist.isEmpty()) {
                if (creditsSb.length() > 0) creditsSb.append("  |  ");
                creditsSb.append("✍️ ").append(lyricist);
            }
            if (creditsSb.length() > 0) {
                tvSongCredits.setVisibility(View.VISIBLE);
                tvSongCredits.setText(creditsSb.toString());
            } else {
                tvSongCredits.setVisibility(View.GONE);
            }

            // Raga
            String raga = song.getDisplayRaga();
            if (!raga.isEmpty()) {
                tvSongRaga.setVisibility(View.VISIBLE);
                tvSongRaga.setText("🎵 രാഗം: " + raga);
            } else {
                tvSongRaga.setVisibility(View.GONE);
            }

            // Play on YouTube
            btnPlayYouTube.setOnClickListener(v -> {
                openSongInYouTube(song, progressSongAction);
            });

            // Direct Search on YouTube (Movie name + Song line)
            if (btnSearchYouTube != null) {
                btnSearchYouTube.setOnClickListener(v -> {
                    openDirectYouTubeSearch(context, song);
                });
            }

            // Lyrics & Info button
            btnViewLyrics.setOnClickListener(v -> {
                showLyricsAndDetailsDialog(song);
            });

            // Whole card click opens rich Lyrics & Details sheet
            itemView.setOnClickListener(v -> {
                showLyricsAndDetailsDialog(song);
            });

            // Share button
            btnShareSong.setOnClickListener(v -> {
                if (interactionListener != null) {
                    interactionListener.onShareSong(song);
                } else {
                    shareSong(song);
                }
            });
        }
    }

    public void openSongInYouTube(MalayalamSong song, ProgressBar spinner) {
        if (song.resolvedVideoId != null && !song.resolvedVideoId.isEmpty()) {
            launchVideoDetail(song, song.resolvedVideoId, song.resolvedThumb);
        } else {
            if (spinner != null) spinner.setVisibility(View.VISIBLE);
            MalayalamSongManager.getInstance().resolveYouTube(song, new MalayalamSongManager.OnYouTubeResolvedCallback() {
                @Override
                public void onResolved(String videoId, String thumbnailUrl, String title) {
                    if (spinner != null) spinner.setVisibility(View.GONE);
                    launchVideoDetail(song, videoId, thumbnailUrl);
                }

                @Override
                public void onError(String error) {
                    if (spinner != null) spinner.setVisibility(View.GONE);
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void launchVideoDetail(MalayalamSong song, String videoId, String thumbUrl) {
        Intent intent = new Intent(context, ActivityVideoDetail.class);
        intent.putExtra("videoId", videoId);
        intent.putExtra("title", song.getCleanSong() + (!song.getCleanMovie().isEmpty() ? " (" + song.getCleanMovie() + ")" : ""));
        intent.putExtra("date", song.getCleanYear());
        intent.putExtra("thumbUrl", thumbUrl);
        intent.putExtra("channelName", song.getDisplayArtists());
        context.startActivity(intent);
    }

    public void shareSong(MalayalamSong song) {
        String shareText;
        if (song.resolvedVideoId != null && !song.resolvedVideoId.isEmpty()) {
            shareText = "🎵 " + song.getCleanSong() + (!song.getCleanMovie().isEmpty() ? " - " + song.getCleanMovie() : "") +
                    (!song.getCleanYear().isEmpty() ? " (" + song.getCleanYear() + ")" : "") +
                    "\nWatch on YouTube: https://youtu.be/" + song.resolvedVideoId;
        } else {
            shareText = "🎵 " + song.getCleanSong() + (!song.getCleanMovie().isEmpty() ? " (" + song.getCleanMovie() : "") +
                    (!song.getCleanYear().isEmpty() ? " • " + song.getCleanYear() + ")" : ")") +
                    (!song.getDisplayArtists().isEmpty() ? "\n🎙️ Singers: " + song.getDisplayArtists() : "") +
                    (!song.getDisplayMusician().isEmpty() ? "\n🎼 Music: " + song.getDisplayMusician() : "") +
                    (!song.getDisplayLyricist().isEmpty() ? "\n✍️ Lyrics: " + song.getDisplayLyricist() : "");
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        context.startActivity(Intent.createChooser(intent, "Share Song"));
    }

    public void showLyricsAndDetailsDialog(MalayalamSong song) {
        try {
            BottomSheetDialog dialog = new BottomSheetDialog(context);
            View view = LayoutInflater.from(context).inflate(R.layout.dialog_song_lyrics, null);
            dialog.setContentView(view);

            TextView tvTitle = view.findViewById(R.id.tv_lyrics_song_title);
            TextView tvMovieYear = view.findViewById(R.id.tv_lyrics_movie_year);
            MaterialButton btnPlayYoutube = view.findViewById(R.id.btn_lyrics_play_youtube);
            MaterialButton btnMovieSongs = view.findViewById(R.id.btn_lyrics_movie_songs);
            ImageView btnCopy = view.findViewById(R.id.btn_copy_lyrics);
            ImageView btnShare = view.findViewById(R.id.btn_share_lyrics);
            ImageView btnClose = view.findViewById(R.id.btn_close_lyrics);

            LinearLayout rowSingers = view.findViewById(R.id.row_lyrics_singers);
            TextView tvArtists = view.findViewById(R.id.tv_lyrics_artists);
            LinearLayout rowMusician = view.findViewById(R.id.row_lyrics_musician);
            TextView tvMusician = view.findViewById(R.id.tv_lyrics_musician);
            LinearLayout rowLyricist = view.findViewById(R.id.row_lyrics_lyricist);
            TextView tvLyricist = view.findViewById(R.id.tv_lyrics_lyricist);
            LinearLayout rowRaga = view.findViewById(R.id.row_lyrics_raga);
            TextView tvRaga = view.findViewById(R.id.tv_lyrics_raga);
            LinearLayout rowActors = view.findViewById(R.id.row_lyrics_actors);
            TextView tvActors = view.findViewById(R.id.tv_lyrics_actors);

            TextView tvLyrics = view.findViewById(R.id.tv_lyrics_text);
            View layoutNoLyrics = view.findViewById(R.id.layout_no_lyrics);

            // Title & Movie Year
            if (tvTitle != null) tvTitle.setText(song.getCleanSong());
            if (tvMovieYear != null) {
                String myText = song.getCleanMovie() + (!song.getCleanYear().isEmpty() ? " • " + song.getCleanYear() : "");
                tvMovieYear.setText(myText);
            }

            // Quick Play Button
            if (btnPlayYoutube != null) {
                btnPlayYoutube.setOnClickListener(v -> {
                    dialog.dismiss();
                    openSongInYouTube(song, null);
                });
            }

            // Direct YouTube Search (Movie name + Song line)
            MaterialButton btnSearchYoutube = view.findViewById(R.id.btn_lyrics_search_youtube);
            if (btnSearchYoutube != null) {
                btnSearchYoutube.setOnClickListener(v -> {
                    dialog.dismiss();
                    openDirectYouTubeSearch(context, song);
                });
            }

            // All Movie Songs Button
            if (btnMovieSongs != null) {
                String movieName = song.getCleanMovie();
                if (!movieName.isEmpty()) {
                    btnMovieSongs.setVisibility(View.VISIBLE);
                    btnMovieSongs.setText("All in " + movieName);
                    btnMovieSongs.setOnClickListener(v -> {
                        dialog.dismiss();
                        if (interactionListener != null) {
                            interactionListener.onMovieClick(movieName);
                        }
                    });
                } else {
                    btnMovieSongs.setVisibility(View.GONE);
                }
            }

            // Metadata: Singers
            String singers = song.getDisplayArtists();
            if (!singers.isEmpty()) {
                if (rowSingers != null) rowSingers.setVisibility(View.VISIBLE);
                if (tvArtists != null) tvArtists.setText(singers);
            } else {
                if (rowSingers != null) rowSingers.setVisibility(View.GONE);
            }

            // Musician
            String musician = song.getDisplayMusician();
            if (!musician.isEmpty()) {
                if (rowMusician != null) rowMusician.setVisibility(View.VISIBLE);
                if (tvMusician != null) tvMusician.setText(musician);
            } else {
                if (rowMusician != null) rowMusician.setVisibility(View.GONE);
            }

            // Lyricist
            String lyricist = song.getDisplayLyricist();
            if (!lyricist.isEmpty()) {
                if (rowLyricist != null) rowLyricist.setVisibility(View.VISIBLE);
                if (tvLyricist != null) tvLyricist.setText(lyricist);
            } else {
                if (rowLyricist != null) rowLyricist.setVisibility(View.GONE);
            }

            // Raga
            String raga = song.getDisplayRaga();
            if (!raga.isEmpty()) {
                if (rowRaga != null) rowRaga.setVisibility(View.VISIBLE);
                if (tvRaga != null) tvRaga.setText(raga);
            } else {
                if (rowRaga != null) rowRaga.setVisibility(View.GONE);
            }

            // Actors
            String actors = song.getDisplayActors();
            if (!actors.isEmpty()) {
                if (rowActors != null) rowActors.setVisibility(View.VISIBLE);
                if (tvActors != null) tvActors.setText(actors);
            } else {
                if (rowActors != null) rowActors.setVisibility(View.GONE);
            }

            // Lyrics
            if (song.hasLyrics()) {
                if (tvLyrics != null) {
                    tvLyrics.setVisibility(View.VISIBLE);
                    tvLyrics.setText(song.getCleanLyrics());
                }
                if (layoutNoLyrics != null) layoutNoLyrics.setVisibility(View.GONE);
            } else {
                if (tvLyrics != null) tvLyrics.setVisibility(View.GONE);
                if (layoutNoLyrics != null) layoutNoLyrics.setVisibility(View.VISIBLE);
            }

            // Copy button
            if (btnCopy != null) {
                btnCopy.setOnClickListener(v -> {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    String copyData = song.getCleanSong() + (!song.getCleanMovie().isEmpty() ? " (" + song.getCleanMovie() + ")" : "") +
                            (song.hasLyrics() ? "\n\n" + song.getCleanLyrics() : "\nSingers: " + song.getDisplayArtists());
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Song Details", copyData);
                    if (cm != null) {
                        cm.setPrimaryClip(clip);
                        Toast.makeText(context, "✓ Copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Share button
            if (btnShare != null) {
                btnShare.setOnClickListener(v -> {
                    shareSong(song);
                });
            }

            // Close button
            if (btnClose != null) {
                btnClose.setOnClickListener(v -> dialog.dismiss());
            }

            dialog.show();
        } catch (Exception e) {
            Toast.makeText(context, "Could not open details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void openDirectYouTubeSearch(Context context, MalayalamSong song) {
        if (song == null || context == null) return;
        String query = song.getYouTubeSearchQuery();
        try {
            android.content.Intent appIntent = new android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("vnd.youtube.search:" + android.net.Uri.encode(query)));
            appIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(appIntent);
        } catch (Exception e) {
            try {
                android.content.Intent webIntent = new android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(song.getYouTubeSearchUrl()));
                webIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(webIntent);
            } catch (Exception ex) {
                Toast.makeText(context, "Could not open YouTube search", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
