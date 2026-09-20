package com.app.webdroid.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.app.webdroid.model.MalayalamSong;
import com.app.webdroid.model.YouTubeItem;
import com.google.gson.stream.JsonReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class MalayalamSongManager {
    private static final String TAG = "MalayalamSongManager";
    private static volatile MalayalamSongManager instance;

    private final List<MalayalamSong> allSongs = new ArrayList<>(26000);
    private final List<String> distinctYears = new ArrayList<>(100);
    private final Map<String, Integer> yearCounts = new ConcurrentHashMap<>();
    private boolean isLoaded = false;
    private boolean isLoading = false;
    private final List<OnLoadedCallback> pendingCallbacks = new ArrayList<>();

    // In-memory cache for resolved YouTube video IDs & thumbnails
    private final Map<Integer, String> videoIdCache = new ConcurrentHashMap<>();
    private final Map<Integer, String> thumbCache = new ConcurrentHashMap<>();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnLoadedCallback {
        void onLoaded(List<MalayalamSong> songs);
        void onError(String error);
    }

    public interface OnYouTubeResolvedCallback {
        void onResolved(String videoId, String thumbnailUrl, String title);
        void onError(String error);
    }

    public static MalayalamSongManager getInstance() {
        if (instance == null) {
            synchronized (MalayalamSongManager.class) {
                if (instance == null) {
                    instance = new MalayalamSongManager();
                }
            }
        }
        return instance;
    }

    private MalayalamSongManager() {
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public int getTotalCount() {
        return allSongs.size();
    }

    public synchronized void loadSongsAsync(Context context, OnLoadedCallback callback) {
        if (isLoaded) {
            if (callback != null) {
                callback.onLoaded(new ArrayList<>(allSongs));
            }
            return;
        }

        if (callback != null) {
            pendingCallbacks.add(callback);
        }

        if (isLoading) {
            return;
        }

        isLoading = true;
        final Context appContext = context.getApplicationContext();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                InputStream is = appContext.getAssets().open("songs.json");
                JsonReader reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                List<MalayalamSong> parsed = new ArrayList<>(26000);

                reader.beginArray();
                while (reader.hasNext()) {
                    MalayalamSong song = parseSingleSong(reader);
                    if (song != null) {
                        parsed.add(song);
                    }
                }
                reader.endArray();
                reader.close();
                is.close();

                Collections.sort(parsed, (a, b) -> {
                    int yA = parseYear(a.year);
                    int yB = parseYear(b.year);
                    if (yA != yB) return Integer.compare(yB, yA);
                    return a.getCleanSong().compareToIgnoreCase(b.getCleanSong());
                });

                // Compute distinct years and counts
                Map<String, Integer> counts = new ConcurrentHashMap<>();
                List<String> years = new ArrayList<>();
                for (MalayalamSong song : parsed) {
                    String y = song.getCleanYear();
                    if (!y.isEmpty()) {
                        Integer c = counts.get(y);
                        if (c == null) {
                            counts.put(y, 1);
                            years.add(y);
                        } else {
                            counts.put(y, c + 1);
                        }
                    }
                }
                Collections.sort(years, (a, b) -> {
                    int yA = parseYear(a);
                    int yB = parseYear(b);
                    return Integer.compare(yB, yA);
                });

                synchronized (MalayalamSongManager.this) {
                    allSongs.clear();
                    allSongs.addAll(parsed);
                    yearCounts.clear();
                    yearCounts.putAll(counts);
                    distinctYears.clear();
                    distinctYears.addAll(years);
                    isLoaded = true;
                    isLoading = false;
                }

                mainHandler.post(() -> {
                    List<MalayalamSong> snapshot = new ArrayList<>(allSongs);
                    for (OnLoadedCallback cb : pendingCallbacks) {
                        cb.onLoaded(snapshot);
                    }
                    pendingCallbacks.clear();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error parsing songs.json", e);
                synchronized (MalayalamSongManager.this) {
                    isLoading = false;
                }
                mainHandler.post(() -> {
                    for (OnLoadedCallback cb : pendingCallbacks) {
                        cb.onError("Failed to load songs: " + e.getMessage());
                    }
                    pendingCallbacks.clear();
                });
            }
        });
    }

    private MalayalamSong parseSingleSong(JsonReader reader) {
        try {
            MalayalamSong s = new MalayalamSong();
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "sl":
                        s.sl = reader.nextInt();
                        break;
                    case "song":
                        s.song = reader.nextString();
                        break;
                    case "movie":
                        s.movie = reader.nextString();
                        break;
                    case "year":
                        s.year = reader.nextString();
                        break;
                    case "singer":
                        s.singer = reader.nextString();
                        break;
                    case "singers":
                        s.singers = reader.nextString();
                        break;
                    case "lyrics":
                        s.lyrics = reader.nextString();
                        break;
                    case "lyricist":
                        s.lyricist = reader.nextString();
                        break;
                    case "musician":
                        s.musician = reader.nextString();
                        break;
                    case "actors":
                        s.actors = reader.nextString();
                        break;
                    case "raga":
                        s.raga = reader.nextString();
                        break;
                    case "genre":
                        s.genre = reader.nextString();
                        break;
                    case "song_url":
                        s.song_url = reader.nextString();
                        break;
                    case "movie_url":
                        s.movie_url = reader.nextString();
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }
            reader.endObject();
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    public static final String SORT_YEAR_DESC = "newest";
    public static final String SORT_YEAR_ASC = "oldest";
    public static final String SORT_SONG_ASC = "song_asc";
    public static final String SORT_SONG_DESC = "song_desc";
    public static final String SORT_MOVIE_ASC = "movie_asc";
    public static final String SORT_SINGER_ASC = "singer_asc";
    public static final String SORT_MUSICIAN_ASC = "musician_asc";
    public static final String SORT_LYRICIST_ASC = "lyricist_asc";

    public List<String> getDistinctYears() {
        synchronized (this) {
            return new ArrayList<>(distinctYears);
        }
    }

    public int getSongCountForYear(String year) {
        if (year == null) return 0;
        Integer c = yearCounts.get(year.trim());
        return c != null ? c : 0;
    }

    public List<MalayalamSong> filter(String query, String decade, String artist) {
        return filter(query, decade, artist, null, null, SORT_YEAR_DESC);
    }

    public List<MalayalamSong> filter(String query, String decade, String artist, String sortOrder) {
        return filter(query, decade, artist, null, null, sortOrder);
    }

    public List<MalayalamSong> filter(String query, String decade, String artist, String movieFilter, String sortOrder) {
        return filter(query, decade, artist, movieFilter, null, sortOrder);
    }

    public List<MalayalamSong> filter(String query, String decade, String artist, String movieFilter, String selectedYear, String sortOrder) {
        if (!isLoaded) {
            return Collections.emptyList();
        }

        boolean hasQuery = query != null && !query.trim().isEmpty();
        boolean hasDecade = decade != null && !decade.equalsIgnoreCase("all");
        boolean hasArtist = artist != null && !artist.equalsIgnoreCase("all");
        boolean hasMovie = movieFilter != null && !movieFilter.trim().isEmpty();
        boolean hasYear = selectedYear != null && !selectedYear.equalsIgnoreCase("all") && !selectedYear.trim().isEmpty();

        List<MalayalamSong> results;
        if (!hasQuery && !hasDecade && !hasArtist && !hasMovie && !hasYear) {
            results = new ArrayList<>(allSongs);
        } else {
            String[] tokens = hasQuery ? query.trim().toLowerCase().split("\\s+") : null;
            String artistLower = hasArtist ? artist.trim().toLowerCase() : null;
            String movieLower = hasMovie ? movieFilter.trim().toLowerCase() : null;
            String yearTarget = hasYear ? selectedYear.trim() : null;

            results = new ArrayList<>();
            for (MalayalamSong s : allSongs) {
                // Year check
                if (hasYear) {
                    if (s.year == null || !s.year.trim().equals(yearTarget)) {
                        continue;
                    }
                }

                // Movie check
                if (hasMovie) {
                    String cleanMovie = s.getCleanMovie().toLowerCase();
                    if (!cleanMovie.contains(movieLower)) {
                        continue;
                    }
                }

                // Decade check
                if (hasDecade) {
                    if (!matchesDecade(s.year, decade)) {
                        continue;
                    }
                }

                // Artist check
                if (hasArtist) {
                    if (!s.getSearchableText().contains(artistLower)) {
                        continue;
                    }
                }

                // Query check (all tokens must match)
                if (hasQuery && tokens != null) {
                    boolean allTokensMatch = true;
                    String searchTarget = s.getSearchableText();
                    for (String t : tokens) {
                        if (!searchTarget.contains(t)) {
                            allTokensMatch = false;
                            break;
                        }
                    }
                    if (!allTokensMatch) {
                        continue;
                    }
                }

                results.add(s);
            }
        }

        // Apply sorting
        if (sortOrder == null) sortOrder = SORT_YEAR_DESC;

        switch (sortOrder.toLowerCase()) {
            case SORT_YEAR_ASC:
                Collections.sort(results, (a, b) -> {
                    int yA = parseYear(a.year);
                    int yB = parseYear(b.year);
                    if (yA == -1 && yB != -1) return 1;
                    if (yB == -1 && yA != -1) return -1;
                    if (yA != yB) return Integer.compare(yA, yB);
                    return a.getCleanSong().compareToIgnoreCase(b.getCleanSong());
                });
                break;

            case SORT_SONG_ASC:
            case "title":
                Collections.sort(results, (a, b) -> a.getCleanSong().compareToIgnoreCase(b.getCleanSong()));
                break;

            case SORT_SONG_DESC:
                Collections.sort(results, (a, b) -> b.getCleanSong().compareToIgnoreCase(a.getCleanSong()));
                break;

            case SORT_MOVIE_ASC:
                Collections.sort(results, (a, b) -> {
                    int mComp = a.getCleanMovie().compareToIgnoreCase(b.getCleanMovie());
                    if (mComp != 0) return mComp;
                    return a.getCleanSong().compareToIgnoreCase(b.getCleanSong());
                });
                break;

            case SORT_SINGER_ASC:
                Collections.sort(results, (a, b) -> {
                    String sA = a.getDisplayArtists();
                    String sB = b.getDisplayArtists();
                    if (sA.isEmpty() && !sB.isEmpty()) return 1;
                    if (sB.isEmpty() && !sA.isEmpty()) return -1;
                    int sComp = sA.compareToIgnoreCase(sB);
                    if (sComp != 0) return sComp;
                    return a.getCleanSong().compareToIgnoreCase(b.getCleanSong());
                });
                break;

            case SORT_MUSICIAN_ASC:
                Collections.sort(results, (a, b) -> {
                    String mA = a.getDisplayMusician();
                    String mB = b.getDisplayMusician();
                    if (mA.isEmpty() && !mB.isEmpty()) return 1;
                    if (mB.isEmpty() && !mA.isEmpty()) return -1;
                    int mComp = mA.compareToIgnoreCase(mB);
                    if (mComp != 0) return mComp;
                    return a.getCleanSong().compareToIgnoreCase(b.getCleanSong());
                });
                break;

            case SORT_LYRICIST_ASC:
                Collections.sort(results, (a, b) -> {
                    String lA = a.getDisplayLyricist();
                    String lB = b.getDisplayLyricist();
                    if (lA.isEmpty() && !lB.isEmpty()) return 1;
                    if (lB.isEmpty() && !lA.isEmpty()) return -1;
                    int lComp = lA.compareToIgnoreCase(lB);
                    if (lComp != 0) return lComp;
                    return a.getCleanSong().compareToIgnoreCase(b.getCleanSong());
                });
                break;

            case SORT_YEAR_DESC:
            default:
                Collections.sort(results, (a, b) -> {
                    int yA = parseYear(a.year);
                    int yB = parseYear(b.year);
                    if (yA != yB) return Integer.compare(yB, yA);
                    return a.getCleanSong().compareToIgnoreCase(b.getCleanSong());
                });
                break;
        }

        return results;
    }

    public static int parseYear(String year) {
        if (year == null || year.isEmpty() || year.equalsIgnoreCase("NA") || year.equals("0000")) {
            return -1;
        }
        try {
            return Integer.parseInt(year.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean matchesDecade(String year, String decade) {
        if (year == null || year.isEmpty() || year.equals("0000")) return false;
        switch (decade.toLowerCase()) {
            case "2020s":
                return year.startsWith("202");
            case "2010s":
                return year.startsWith("201");
            case "2000s":
                return year.startsWith("200");
            case "90s":
                return year.startsWith("199");
            case "80s":
                return year.startsWith("198");
            case "70s":
                return year.startsWith("197");
            case "oldies":
                return year.startsWith("195") || year.startsWith("196") || year.startsWith("197");
            default:
                return true;
        }
    }

    public void resolveYouTube(MalayalamSong song, OnYouTubeResolvedCallback callback) {
        if (song == null) {
            if (callback != null) callback.onError("Song is null");
            return;
        }

        if (song.resolvedVideoId != null && !song.resolvedVideoId.isEmpty()) {
            if (callback != null) {
                callback.onResolved(song.resolvedVideoId, song.resolvedThumb, song.getCleanSong());
            }
            return;
        }

        String cachedId = videoIdCache.get(song.sl);
        if (cachedId != null && !cachedId.isEmpty()) {
            song.resolvedVideoId = cachedId;
            song.resolvedThumb = thumbCache.get(song.sl);
            if (callback != null) {
                callback.onResolved(cachedId, song.resolvedThumb, song.getCleanSong());
            }
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String query = song.getYouTubeQuery();
                YouTubeInnertubeFetcher.FetchResult res =
                        YouTubeInnertubeFetcher.searchVideosWithContinuation(query, null, null, 1);

                if (res != null && res.items != null && !res.items.isEmpty()) {
                    YouTubeItem top = res.items.get(0);
                    song.resolvedVideoId = top.videoId;
                    song.resolvedThumb = top.thumbnailUrl;
                    videoIdCache.put(song.sl, top.videoId);
                    if (top.thumbnailUrl != null) {
                        thumbCache.put(song.sl, top.thumbnailUrl);
                    }
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onResolved(top.videoId, top.thumbnailUrl, top.title);
                        }
                    });
                    return;
                }

                // Fallback query with only song title + "malayalam song"
                String fallbackQuery = song.getCleanSong() + " malayalam song";
                YouTubeInnertubeFetcher.FetchResult res2 =
                        YouTubeInnertubeFetcher.searchVideosWithContinuation(fallbackQuery, null, null, 1);

                if (res2 != null && res2.items != null && !res2.items.isEmpty()) {
                    YouTubeItem top = res2.items.get(0);
                    song.resolvedVideoId = top.videoId;
                    song.resolvedThumb = top.thumbnailUrl;
                    videoIdCache.put(song.sl, top.videoId);
                    if (top.thumbnailUrl != null) {
                        thumbCache.put(song.sl, top.thumbnailUrl);
                    }
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onResolved(top.videoId, top.thumbnailUrl, top.title);
                        }
                    });
                    return;
                }

                mainHandler.post(() -> {
                    if (callback != null) callback.onError("No YouTube video found for " + song.getCleanSong());
                });

            } catch (Exception e) {
                Log.e(TAG, "Error resolving YouTube video for song", e);
                mainHandler.post(() -> {
                    if (callback != null) callback.onError("Network error: " + e.getMessage());
                });
            }
        });
    }
}
