package com.app.webdroid.util;

import android.util.Xml;
import com.app.webdroid.model.NewsItem;
import com.app.webdroid.model.YouTubeItem;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RssParser {
    private static final String NS = null;

    public List<NewsItem> parseNews(InputStream in, String sourceName) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readRss(parser, sourceName);
        } finally {
            in.close();
        }
    }

    public List<YouTubeItem> parseYouTube(InputStream in, String channelName)
            throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readAtom(parser, channelName);
        } finally {
            in.close();
        }
    }

    private List<NewsItem> readRss(XmlPullParser parser, String sourceName) throws XmlPullParserException, IOException {
        List<NewsItem> items = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, NS, "rss");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("channel")) {
                items.addAll(readChannel(parser, sourceName));
            } else {
                skip(parser);
            }
        }
        return items;
    }

    private List<NewsItem> readChannel(XmlPullParser parser, String sourceName)
            throws XmlPullParserException, IOException {
        List<NewsItem> items = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("item")) {
                items.add(readItem(parser, sourceName));
            } else {
                skip(parser);
            }
        }
        return items;
    }

    private NewsItem readItem(XmlPullParser parser, String sourceName) throws XmlPullParserException, IOException {
        String title = null;
        String description = null;
        String link = null;
        String pubDate = null;
        String imageUrl = null;
        String itemSourceName = sourceName;
        String sourceUrl = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("title")) {
                title = readText(parser);
            } else if (name.equals("description")) {
                description = readText(parser);
            } else if (name.equals("content:encoded") || name.equals("encoded")) {
                String contentEncoded = readText(parser);
                if ((imageUrl == null || imageUrl.isEmpty()) && contentEncoded != null && contentEncoded.contains("<img")) {
                    try {
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<img[^>]+src=['\"]([^'\"]+)['\"]", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(contentEncoded);
                        if (m.find()) {
                            imageUrl = m.group(1);
                        }
                    } catch (Exception ignored) {}
                }
            } else if (name.equals("link")) {
                link = readText(parser);
            } else if (name.equals("pubDate") || name.equals("dc:date") || name.equals("date")) {
                String d = readText(parser);
                if (pubDate == null)
                    pubDate = d; // Prefer pubDate if already found, or take first found
            } else if (name.equals("image")) {
                String img = readText(parser);
                if (img != null && !img.trim().isEmpty() && img.startsWith("http")) {
                    imageUrl = img.trim();
                }
            } else if (name.equals("media:content") || name.equals("media:thumbnail") || name.equals("enclosure")) {
                String url = parser.getAttributeValue(null, "url");
                if (url != null && !url.trim().isEmpty() && url.startsWith("http")) {
                    imageUrl = url.trim();
                }
                skip(parser); // skip content
            } else if (name.equals("source")) {
                String sUrl = parser.getAttributeValue(null, "url");
                if (sUrl != null && !sUrl.trim().isEmpty()) {
                    sourceUrl = sUrl.trim();
                }
                String sName = readText(parser);
                if (sName != null && !sName.trim().isEmpty()) {
                    itemSourceName = sName.trim();
                }
            } else {
                skip(parser);
            }
        }

        // Extract image from description HTML if still null
        if ((imageUrl == null || imageUrl.isEmpty()) && description != null && description.contains("<img")) {
            try {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("<img[^>]+src=['\"]([^'\"]+)['\"]", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(description);
                if (m.find()) {
                    imageUrl = m.group(1);
                }
            } catch (Exception ignored) {}
        }

        long pubInMillis = parseRssDateToMillis(pubDate);
        NewsItem item = new NewsItem(title, description, imageUrl, pubDate, pubInMillis, itemSourceName, link);
        item.sourceUrl = sourceUrl;
        return item;
    }

    public static long parseRssDateToMillis(String pubDate) {
        if (pubDate == null || pubDate.trim().isEmpty()) {
            return System.currentTimeMillis();
        }
        String cleanDate = pubDate.trim();

        // 1. Try modern java.time API (API 26+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                return java.time.OffsetDateTime.parse(cleanDate).toInstant().toEpochMilli();
            } catch (Exception ignored) {}
            try {
                return java.time.ZonedDateTime.parse(cleanDate, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli();
            } catch (Exception ignored) {}
            try {
                return java.time.Instant.parse(cleanDate).toEpochMilli();
            } catch (Exception ignored) {}
        }

        // 2. Comprehensive SimpleDateFormat fallbacks
        String[] formats = {
                "EEE, dd MMM yyyy HH:mm:ss Z",
                "EEE, dd MMM yyyy HH:mm:ss z",
                "EEE, dd MMM yyyy HH:mm:ss zzz",
                "EEE, d MMM yyyy HH:mm:ss Z",
                "EEE, d MMM yyyy HH:mm:ss z",
                "EEE, dd MMM yyyy HH:mm Z",
                "EEE, dd MMM yyyy HH:mm z",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "dd MMM yyyy HH:mm:ss Z",
                "dd MMM yyyy HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss",
                "EEE, dd MMM yyyy"
        };

        // Normalize RFC 822 timezone colons (e.g. +05:30 -> +0530)
        String fixedDate = cleanDate;
        if (fixedDate.length() > 6 && fixedDate.charAt(fixedDate.length() - 3) == ':') {
            char sign = fixedDate.charAt(fixedDate.length() - 6);
            if (sign == '+' || sign == '-') {
                fixedDate = fixedDate.substring(0, fixedDate.length() - 3) + fixedDate.substring(fixedDate.length() - 2);
            }
        }

        for (String format : formats) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(format, java.util.Locale.US);
                if (format.contains("'Z'")) {
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                }
                java.util.Date d = sdf.parse(cleanDate);
                if (d != null) return d.getTime();
            } catch (Exception ignored) {}
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(format, java.util.Locale.US);
                java.util.Date d = sdf.parse(fixedDate);
                if (d != null) return d.getTime();
            } catch (Exception ignored) {}
        }

        return System.currentTimeMillis();
    }

    private List<YouTubeItem> readAtom(XmlPullParser parser, String channelName)
            throws XmlPullParserException, IOException {
        List<YouTubeItem> items = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, NS, "feed");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;
            String name = parser.getName();
            if (name.equals("entry")) {
                items.add(readEntry(parser, channelName));
            } else {
                skip(parser);
            }
        }
        return items;
    }

    private YouTubeItem readEntry(XmlPullParser parser, String channelName) throws XmlPullParserException, IOException {
        YouTubeItem item = new YouTubeItem();
        item.channelName = channelName;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;
            String name = parser.getName();
            if (name.equals("title")) {
                item.title = readText(parser);
            } else if (name.equals("yt:videoId")) {
                item.videoId = readText(parser);
            } else if (name.equals("published")) {
                item.pubDate = readText(parser);
            } else if (name.equals("media:group")) {
                readMediaGroup(parser, item);
            } else if (name.equals("yt:duration")) {
                String secStr = parser.getAttributeValue(null, "seconds");
                if (secStr != null) {
                    try {
                        int secs = Integer.parseInt(secStr);
                        if (secs > 0) {
                            int m = secs / 60;
                            int s = secs % 60;
                            int h = m / 60;
                            m = m % 60;
                            item.duration = h > 0 ? String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s) : String.format(java.util.Locale.US, "%d:%02d", m, s);
                        }
                    } catch (Exception ignored) {}
                }
                skip(parser);
            } else {
                skip(parser);
            }
        }

        // Parse date for youtube (Atom ISO 8601)
        if (item.pubDate != null) {
            try {
                java.text.SimpleDateFormat sdf;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US);
                } else {
                    // Legacy fix for +00:00 colon
                    String s = item.pubDate;
                    if (s.lastIndexOf(":") > s.lastIndexOf("+") && s.lastIndexOf("+") > 0) {
                        int lastColon = s.lastIndexOf(":");
                        s = s.substring(0, lastColon) + s.substring(lastColon + 1);
                    } else if (s.lastIndexOf(":") > s.lastIndexOf("-") && s.lastIndexOf("-") > 10) {
                        int lastColon = s.lastIndexOf(":");
                        s = s.substring(0, lastColon) + s.substring(lastColon + 1);
                    }
                    // item.pubDate is already set, effectively we parse 's' or update item.pubDate
                    sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.US);
                    // Use 's' for parsing if we modified it
                    item.pubDate = s;
                }

                java.util.Date d = sdf.parse(item.pubDate);
                if (d != null)
                    item.pubDateMillis = d.getTime();
                else
                    item.pubDateMillis = System.currentTimeMillis();
            } catch (Exception e) {
                // Try simpler format if timezone fails
                item.pubDateMillis = System.currentTimeMillis();
            }
        } else {
            item.pubDateMillis = System.currentTimeMillis();
        }
        item.fetchedAt = System.currentTimeMillis();

        return item;
    }

    private void readMediaGroup(XmlPullParser parser, YouTubeItem item) throws XmlPullParserException, IOException {
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;
            String name = parser.getName();
            if (name.equals("media:thumbnail")) {
                String url = parser.getAttributeValue(null, "url");
                if (url != null)
                    item.thumbnailUrl = url;
                skip(parser);
            } else if (name.equals("yt:duration")) {
                String secStr = parser.getAttributeValue(null, "seconds");
                if (secStr != null) {
                    try {
                        int secs = Integer.parseInt(secStr);
                        if (secs > 0) {
                            int m = secs / 60;
                            int s = secs % 60;
                            int h = m / 60;
                            m = m % 60;
                            item.duration = h > 0 ? String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s) : String.format(java.util.Locale.US, "%d:%02d", m, s);
                        }
                    } catch (Exception ignored) {}
                }
                skip(parser);
            } else if (name.equals("media:community")) {
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) continue;
                    String cName = parser.getName();
                    if (cName.equals("media:statistics")) {
                        String vStr = parser.getAttributeValue(null, "views");
                        if (vStr != null) {
                            try {
                                long v = Long.parseLong(vStr);
                                if (v >= 1_000_000) {
                                    item.viewCount = String.format(java.util.Locale.US, "%.1fM views", v / 1_000_000.0).replace(".0M", "M");
                                } else if (v >= 1_000) {
                                    item.viewCount = String.format(java.util.Locale.US, "%.1fK views", v / 1_000.0).replace(".0K", "K");
                                } else {
                                    item.viewCount = v + " views";
                                }
                            } catch (Exception ignored) {}
                        }
                        skip(parser);
                    } else {
                        skip(parser);
                    }
                }
            } else {
                skip(parser);
            }
        }
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        StringBuilder result = new StringBuilder();
        int eventType = parser.next();
        while (eventType != XmlPullParser.END_TAG) {
            if (eventType == XmlPullParser.TEXT || eventType == XmlPullParser.CDSECT || eventType == XmlPullParser.ENTITY_REF) {
                result.append(parser.getText());
            } else if (eventType == XmlPullParser.START_TAG) {
                skip(parser);
            }
            eventType = parser.next();
        }
        return result.toString().trim();
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
