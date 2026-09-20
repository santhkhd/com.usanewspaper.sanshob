package com.app.webdroid.model;

import java.io.Serializable;

public class MalayalamSong implements Serializable {
    public int sl;
    public String song;
    public String movie;
    public String year;
    public String singer;      // legacy field
    public String lyrics;      // holds full lyrics text in MSIDB, or singer in legacy json
    public String musician;
    public String lyricist;    // from MSIDB
    public String singers;     // from MSIDB
    public String actors;      // from MSIDB
    public String raga;        // from MSIDB
    public String genre;       // from MSIDB
    public String song_url;
    public String movie_url;

    // Transient runtime fields
    public transient String resolvedVideoId;
    public transient String resolvedThumb;
    public transient String searchableText;

    public MalayalamSong() {
    }

    public String getCleanSong() {
        if (song == null) return "";
        String s = song.trim();
        if (s.endsWith("...")) {
            s = s.substring(0, s.length() - 3).trim();
        }
        return s;
    }

    public String getCleanMovie() {
        return movie != null ? movie.trim() : "";
    }

    public String getCleanYear() {
        if (year == null || year.trim().equals("0000") || year.trim().isEmpty()) {
            return "";
        }
        return year.trim();
    }

    public String getDisplayArtists() {
        // Prefer explicit singers field from MSIDB
        if (singers != null && !singers.trim().isEmpty()) {
            return singers.trim();
        }

        // Fallback for legacy format
        StringBuilder sb = new StringBuilder();
        if (lyrics != null && !lyrics.trim().isEmpty() && !lyrics.trim().equals("0000")
                && !lyrics.trim().matches("^\\d+$") && lyrics.length() < 120) {
            sb.append(lyrics.trim());
        }
        if (singer != null && !singer.trim().isEmpty() && !singer.trim().equals("0000")
                && !singer.trim().matches("^\\d+$")) {
            if (sb.length() > 0 && !sb.toString().contains(singer.trim())) {
                sb.append(", ").append(singer.trim());
            } else if (sb.length() == 0) {
                sb.append(singer.trim());
            }
        }
        return sb.toString();
    }

    public String getDisplayLyricist() {
        if (lyricist != null && !lyricist.trim().isEmpty()) {
            return lyricist.trim();
        }
        return "";
    }

    public String getDisplayMusician() {
        if (musician == null || musician.trim().isEmpty() || musician.trim().equals("0000")) {
            return "";
        }
        return musician.trim();
    }

    public String getDisplayRaga() {
        if (raga != null && !raga.trim().isEmpty()) {
            return raga.trim();
        }
        return "";
    }

    public String getDisplayActors() {
        if (actors != null && !actors.trim().isEmpty()) {
            return actors.trim();
        }
        return "";
    }

    public String getDisplayGenre() {
        if (genre != null && !genre.trim().isEmpty()) {
            return genre.trim();
        }
        return "";
    }

    public boolean hasLyrics() {
        return lyrics != null && lyrics.trim().length() > 20
                && (lyrics.contains("\n") || lyrics.length() > 60);
    }

    public String getCleanLyrics() {
        if (lyrics == null) return "";
        return lyrics.trim();
    }

    public String getSearchableText() {
        if (searchableText == null) {
            StringBuilder sb = new StringBuilder();
            if (song != null) sb.append(song).append(" ");
            if (movie != null) sb.append(movie).append(" ");
            if (year != null && !year.equals("0000")) sb.append(year).append(" ");
            if (singers != null) sb.append(singers).append(" ");
            if (singer != null && !singer.matches("^\\d+$")) sb.append(singer).append(" ");
            if (lyricist != null) sb.append(lyricist).append(" ");
            if (musician != null) sb.append(musician).append(" ");
            if (raga != null) sb.append(raga).append(" ");
            if (actors != null) sb.append(actors).append(" ");
            if (genre != null) sb.append(genre).append(" ");
            searchableText = sb.toString().toLowerCase();
        }
        return searchableText;
    }

    public String getYouTubeQuery() {
        return getYouTubeSearchQuery();
    }

    public String getYouTubeSearchQuery() {
        String sName = getCleanSong();
        String mName = getCleanMovie();
        StringBuilder q = new StringBuilder();
        if (!sName.isEmpty()) {
            q.append(sName);
        }
        if (!mName.isEmpty()) {
            if (q.length() > 0) q.append(" ");
            q.append(mName);
        }
        q.append(" malayalam song");
        return q.toString().trim();
    }

    public String getYouTubeSearchUrl() {
        try {
            return "https://www.youtube.com/results?search_query=" + java.net.URLEncoder.encode(getYouTubeSearchQuery(), "UTF-8");
        } catch (Exception e) {
            return "https://www.youtube.com";
        }
    }
}
