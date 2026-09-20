package com.app.webdroid.util;

import android.util.Log;
import com.app.webdroid.model.YouTubeComment;
import com.app.webdroid.model.YouTubeItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class YouTubeInnertubeFetcher {

    private static final String TAG = "InnertubeFetcher";
    private static final String BROWSE_URL = "https://www.youtube.com/youtubei/v1/browse?prettyPrint=false";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final String NEXT_URL = "https://www.youtube.com/youtubei/v1/next?prettyPrint=false";

    public static class CommentResult {
        public List<YouTubeComment> comments = new ArrayList<>();
        public String totalCommentsCount = "";
        public String nextContinuationToken = null;
        public boolean commentsDisabled = false;
    }

    public static class FetchResult {
        public List<YouTubeItem> items = new ArrayList<>();
        public String nextContinuationToken = null;
        public String matchedChannelTitle = null;
        public String matchedChannelId = null;
        public String matchedChannelThumb = null;
    }

    public static String parseDurationFromAccessibilityLabel(String label) {
        if (label == null || label.isEmpty()) return "";
        try {
            Matcher mHour = Pattern.compile("(\\d+)\\s+hour").matcher(label);
            Matcher mMin = Pattern.compile("(\\d+)\\s+minute").matcher(label);
            Matcher mSec = Pattern.compile("(\\d+)\\s+second").matcher(label);
            int h = mHour.find() ? Integer.parseInt(mHour.group(1)) : 0;
            int m = mMin.find() ? Integer.parseInt(mMin.group(1)) : 0;
            int s = mSec.find() ? Integer.parseInt(mSec.group(1)) : 0;
            if (h > 0 || m > 0 || s > 0) {
                if (h > 0) {
                    return String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s);
                } else {
                    return String.format(java.util.Locale.US, "%d:%02d", m, s);
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    public static String extractDurationFromLockup(JSONObject lvm) {
        if (lvm == null) return "";
        try {
            JSONObject contentImage = lvm.optJSONObject("contentImage");
            if (contentImage != null) {
                JSONObject thumbVM = contentImage.optJSONObject("thumbnailViewModel");
                if (thumbVM != null) {
                    JSONArray overlays = thumbVM.optJSONArray("overlays");
                    if (overlays != null) {
                        for (int o = 0; o < overlays.length(); o++) {
                            JSONObject ov = overlays.getJSONObject(o);

                            // 1. thumbnailBottomOverlayViewModel (Standard Modern YouTube)
                            JSONObject bottomVM = ov.optJSONObject("thumbnailBottomOverlayViewModel");
                            if (bottomVM != null) {
                                JSONArray badges = bottomVM.optJSONArray("badges");
                                if (badges != null) {
                                    for (int b = 0; b < badges.length(); b++) {
                                        JSONObject badgeObj = badges.getJSONObject(b).optJSONObject("thumbnailBadgeViewModel");
                                        if (badgeObj != null) {
                                            String txt = badgeObj.optString("text", "");
                                            if (!txt.isEmpty() && (txt.contains(":") || Character.isDigit(txt.charAt(0)))) {
                                                return txt;
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. thumbnailOverlayBadgeViewModel
                            JSONObject badgeVM = ov.optJSONObject("thumbnailOverlayBadgeViewModel");
                            if (badgeVM != null) {
                                JSONArray badges = badgeVM.optJSONArray("thumbnailBadges");
                                if (badges == null) badges = badgeVM.optJSONArray("badges");
                                if (badges != null) {
                                    for (int b = 0; b < badges.length(); b++) {
                                        JSONObject badgeObj = badges.getJSONObject(b).optJSONObject("thumbnailBadgeViewModel");
                                        if (badgeObj != null) {
                                            String txt = badgeObj.optString("text", "");
                                            if (!txt.isEmpty() && (txt.contains(":") || Character.isDigit(txt.charAt(0)))) {
                                                return txt;
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. thumbnailOverlayTimeStatusRenderer
                            JSONObject timeStatus = ov.optJSONObject("thumbnailOverlayTimeStatusRenderer");
                            if (timeStatus != null) {
                                JSONObject txtObj = timeStatus.optJSONObject("text");
                                if (txtObj != null) {
                                    String s = txtObj.optString("simpleText", "");
                                    if (!s.isEmpty()) return s;
                                }
                            }
                        }
                    }
                }
            }

            // Fallback: AccessibilityContext label (e.g. "... 15 minutes, 41 seconds")
            JSONObject rendererContext = lvm.optJSONObject("rendererContext");
            if (rendererContext != null) {
                JSONObject a11y = rendererContext.optJSONObject("accessibilityContext");
                if (a11y != null) {
                    String label = a11y.optString("label", "");
                    String dur = parseDurationFromAccessibilityLabel(label);
                    if (!dur.isEmpty()) return dur;
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    public static String extractDurationFromVideoRenderer(JSONObject vr) {
        if (vr == null) return "";
        try {
            JSONObject ltObj = vr.optJSONObject("lengthText");
            if (ltObj != null) {
                String s = ltObj.optString("simpleText", "");
                if (!s.isEmpty()) return s;
            }
            if (vr.has("thumbnailOverlays")) {
                JSONArray overlays = vr.optJSONArray("thumbnailOverlays");
                if (overlays != null) {
                    for (int o = 0; o < overlays.length(); o++) {
                        JSONObject to = overlays.getJSONObject(o).optJSONObject("thumbnailOverlayTimeStatusRenderer");
                        if (to != null && to.has("text")) {
                            String s = to.getJSONObject("text").optString("simpleText", "");
                            if (!s.isEmpty()) return s;
                        }
                    }
                }
            }
            // Accessibility fallback on title
            JSONObject titleObj = vr.optJSONObject("title");
            if (titleObj != null && titleObj.has("accessibility")) {
                JSONObject a11y = titleObj.optJSONObject("accessibility");
                if (a11y != null && a11y.has("accessibilityData")) {
                    String label = a11y.getJSONObject("accessibilityData").optString("label", "");
                    String dur = parseDurationFromAccessibilityLabel(label);
                    if (!dur.isEmpty()) return dur;
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    /**
     * Fetches multiple pages of videos for a given YouTube channel ID using Innertube.
     */
    public static List<YouTubeItem> fetchChannelVideos(String channelId, String channelName, int maxPages) {
        FetchResult result = fetchChannelVideosWithContinuation(channelId, channelName, null, maxPages);
        return result.items;
    }

    /**
     * Fetches multiple pages of videos, optionally starting from an existing continuation token.
     */
    public static FetchResult fetchChannelVideosWithContinuation(String channelId, String channelName, String startContinuationToken, int maxPages) {
        FetchResult fetchResult = new FetchResult();
        if (channelId == null || channelId.isEmpty()) {
            return fetchResult;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        String continuationToken = startContinuationToken;
        boolean isPlaylist = channelId.startsWith("PL") || channelId.startsWith("VL");
        String browseId = isPlaylist ? (channelId.startsWith("VL") ? channelId : "VL" + channelId) : channelId;

        for (int page = 0; page < maxPages; page++) {
            try {
                JSONObject payload = new JSONObject();
                JSONObject context = new JSONObject();
                JSONObject clientObj = new JSONObject();
                clientObj.put("clientName", "WEB");
                clientObj.put("clientVersion", "2.20230622.06.00");
                clientObj.put("hl", "en");
                clientObj.put("gl", "IN");
                context.put("client", clientObj);
                payload.put("context", context);

                if (page == 0 && (continuationToken == null || continuationToken.isEmpty())) {
                    payload.put("browseId", browseId);
                    if (!isPlaylist) {
                        payload.put("params", "EgZ2aWRlb3PyBgQKAjoA"); // Videos tab for channels
                    }
                } else {
                    if (continuationToken == null || continuationToken.isEmpty()) {
                        break;
                    }
                    payload.put("continuation", continuationToken);
                }

                RequestBody body = RequestBody.create(payload.toString(), JSON_MEDIA_TYPE);
                Request request = new Request.Builder()
                        .url(BROWSE_URL)
                        .post(body)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    break;
                }

                String responseStr = response.body().string();
                JSONObject jsonResp = new JSONObject(responseStr);

                continuationToken = null;
                JSONArray itemsArray = null;

                if (page == 0 && (startContinuationToken == null || startContinuationToken.isEmpty())) {
                    if (jsonResp.has("contents")) {
                        JSONObject contents = jsonResp.getJSONObject("contents");
                        JSONObject browseResults = contents.optJSONObject("twoColumnBrowseResultsRenderer");
                        if (browseResults == null) {
                            browseResults = contents.optJSONObject("singleColumnBrowseResultsRenderer");
                        }
                        if (browseResults != null) {
                            JSONArray tabs = browseResults.optJSONArray("tabs");
                            if (tabs != null) {
                                for (int t = 0; t < tabs.length(); t++) {
                                    JSONObject tab = tabs.getJSONObject(t).optJSONObject("tabRenderer");
                                    if (tab != null) {
                                        String title = tab.optString("title");
                                        boolean isSelected = tab.optBoolean("selected", false);
                                        JSONObject content = tab.optJSONObject("content");
                                        if (content != null) {
                                            if (content.has("richGridRenderer")) {
                                                itemsArray = content.getJSONObject("richGridRenderer").optJSONArray("contents");
                                                break;
                                            } else if (content.has("playlistVideoListRenderer")) {
                                                itemsArray = content.getJSONObject("playlistVideoListRenderer").optJSONArray("contents");
                                                break;
                                            } else if (content.has("sectionListRenderer")) {
                                                JSONArray secContents = content.getJSONObject("sectionListRenderer").optJSONArray("contents");
                                                if (secContents != null) {
                                                    itemsArray = new JSONArray();
                                                    for (int s = 0; s < secContents.length(); s++) {
                                                        JSONObject sObj = secContents.getJSONObject(s);
                                                        if (sObj.has("itemSectionRenderer")) {
                                                            JSONArray isrContents = sObj.getJSONObject("itemSectionRenderer").optJSONArray("contents");
                                                            if (isrContents != null) {
                                                                for (int ic = 0; ic < isrContents.length(); ic++) {
                                                                    JSONObject icObj = isrContents.getJSONObject(ic);
                                                                    if (icObj.has("playlistVideoListRenderer")) {
                                                                        JSONArray pvlContents = icObj.getJSONObject("playlistVideoListRenderer").optJSONArray("contents");
                                                                        if (pvlContents != null) {
                                                                            for (int p = 0; p < pvlContents.length(); p++) {
                                                                                itemsArray.put(pvlContents.getJSONObject(p));
                                                                            }
                                                                        }
                                                                    } else {
                                                                        itemsArray.put(icObj);
                                                                    }
                                                                }
                                                            }
                                                        } else if (sObj.has("playlistVideoListRenderer")) {
                                                            JSONArray pvlContents = sObj.getJSONObject("playlistVideoListRenderer").optJSONArray("contents");
                                                            if (pvlContents != null) {
                                                                for (int p = 0; p < pvlContents.length(); p++) {
                                                                    itemsArray.put(pvlContents.getJSONObject(p));
                                                                }
                                                            }
                                                        } else if (sObj.has("continuationItemRenderer")) {
                                                            itemsArray.put(sObj);
                                                        }
                                                    }
                                                    if (itemsArray.length() > 0) break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    JSONArray actions = jsonResp.optJSONArray("onResponseReceivedActions");
                    if (actions != null && actions.length() > 0) {
                        JSONObject action0 = actions.getJSONObject(0);
                        JSONObject appendAction = action0.optJSONObject("appendContinuationItemsAction");
                        if (appendAction != null) {
                            itemsArray = appendAction.optJSONArray("continuationItems");
                        }
                    }
                }

                if (itemsArray == null || itemsArray.length() == 0) {
                    break;
                }

                long baseTime = System.currentTimeMillis();

                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject rawItem = itemsArray.getJSONObject(i);

                    // Check for continuation token
                    if (rawItem.has("continuationItemRenderer")) {
                        JSONObject contRenderer = rawItem.getJSONObject("continuationItemRenderer");
                        JSONObject endpoint = contRenderer.optJSONObject("continuationEndpoint");
                        if (endpoint != null) {
                            JSONObject command = endpoint.optJSONObject("continuationCommand");
                            if (command != null) {
                                continuationToken = command.optString("token", null);
                            }
                        }
                        continue;
                    }

                    JSONObject richItem = rawItem.optJSONObject("richItemRenderer");
                    JSONObject contentObj = richItem != null ? richItem.optJSONObject("content") : rawItem;
                    if (contentObj == null) continue;

                    String videoId = null;
                    String title = null;
                    String pubDate = "";
                    String thumbUrl = null;
                    String duration = "";
                    String viewCount = "";

                    // Modern Format: lockupViewModel
                    if (contentObj.has("lockupViewModel")) {
                        JSONObject lvm = contentObj.getJSONObject("lockupViewModel");
                        String cType = lvm.optString("contentType", "");
                        if ("LOCKUP_CONTENT_TYPE_PLAYLIST".equalsIgnoreCase(cType)
                                || "LOCKUP_CONTENT_TYPE_CHANNEL".equalsIgnoreCase(cType)
                                || "LOCKUP_CONTENT_TYPE_RADIO".equalsIgnoreCase(cType)) {
                            continue;
                        }
                        String candidateId = lvm.optString("contentId", null);
                        if (candidateId == null || !candidateId.matches("^[a-zA-Z0-9_-]{11}$")) {
                            continue;
                        }
                        videoId = candidateId;
                        JSONObject meta = lvm.optJSONObject("metadata");
                        if (meta != null) {
                            JSONObject lockupMeta = meta.optJSONObject("lockupMetadataViewModel");
                            if (lockupMeta != null) {
                                JSONObject titleObj = lockupMeta.optJSONObject("title");
                                if (titleObj != null) {
                                    title = titleObj.optString("content", "");
                                }
                                JSONObject metaInner = lockupMeta.optJSONObject("metadata");
                                if (metaInner != null) {
                                    JSONObject contentMeta = metaInner.optJSONObject("contentMetadataViewModel");
                                    if (contentMeta != null) {
                                        JSONArray rows = contentMeta.optJSONArray("metadataRows");
                                        if (rows != null) {
                                            for (int r = 0; r < rows.length(); r++) {
                                                JSONArray parts = rows.getJSONObject(r).optJSONArray("metadataParts");
                                                if (parts != null) {
                                                    for (int p = 0; p < parts.length(); p++) {
                                                        JSONObject partText = parts.getJSONObject(p).optJSONObject("text");
                                                        if (partText != null) {
                                                            String c = partText.optString("content", "");
                                                            if (c.contains("views") || c.contains("view")) {
                                                                viewCount = c;
                                                            } else if (c.contains("ago") || c.contains("Streamed")) {
                                                                pubDate = c;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        duration = extractDurationFromLockup(lvm);
                        JSONObject contentImage = lvm.optJSONObject("contentImage");
                        if (contentImage != null) {
                            JSONObject thumbVM = contentImage.optJSONObject("thumbnailViewModel");
                            if (thumbVM != null) {
                                JSONObject imageObj = thumbVM.optJSONObject("image");
                                if (imageObj != null) {
                                    JSONArray sources = imageObj.optJSONArray("sources");
                                    if (sources != null && sources.length() > 0) {
                                        thumbUrl = sources.getJSONObject(sources.length() - 1).optString("url");
                                    }
                                }
                            }
                        }
                    }
                    // Classic Format: videoRenderer
                    else if (contentObj.has("videoRenderer")) {
                        JSONObject vr = contentObj.getJSONObject("videoRenderer");
                        videoId = vr.optString("videoId", null);
                        JSONObject titleObj = vr.optJSONObject("title");
                        if (titleObj != null) {
                            JSONArray runs = titleObj.optJSONArray("runs");
                            if (runs != null && runs.length() > 0) {
                                title = runs.getJSONObject(0).optString("text", "");
                            } else {
                                title = titleObj.optString("simpleText", "");
                            }
                        }
                        JSONObject publishedObj = vr.optJSONObject("publishedTimeText");
                        if (publishedObj != null) {
                            pubDate = publishedObj.optString("simpleText", "");
                        }
                        JSONObject thumbObj = vr.optJSONObject("thumbnail");
                        if (thumbObj != null) {
                            JSONArray thumbs = thumbObj.optJSONArray("thumbnails");
                            if (thumbs != null && thumbs.length() > 0) {
                                thumbUrl = thumbs.getJSONObject(thumbs.length() - 1).optString("url");
                            }
                        }
                        duration = extractDurationFromVideoRenderer(vr);
                        JSONObject vcObj = vr.optJSONObject("shortViewCountText");
                        if (vcObj == null) vcObj = vr.optJSONObject("viewCountText");
                        if (vcObj != null) {
                            viewCount = vcObj.optString("simpleText", "");
                        }
                    }
                    // Playlist Format: playlistVideoRenderer
                    else if (contentObj.has("playlistVideoRenderer")) {
                        JSONObject pvr = contentObj.getJSONObject("playlistVideoRenderer");
                        videoId = pvr.optString("videoId", null);
                        JSONObject titleObj = pvr.optJSONObject("title");
                        if (titleObj != null) {
                            JSONArray runs = titleObj.optJSONArray("runs");
                            if (runs != null && runs.length() > 0) {
                                title = runs.getJSONObject(0).optString("text", "");
                            } else {
                                title = titleObj.optString("simpleText", "");
                            }
                        }
                        JSONObject thumbObj = pvr.optJSONObject("thumbnail");
                        if (thumbObj != null) {
                            JSONArray thumbs = thumbObj.optJSONArray("thumbnails");
                            if (thumbs != null && thumbs.length() > 0) {
                                thumbUrl = thumbs.getJSONObject(thumbs.length() - 1).optString("url");
                            }
                        }
                        JSONObject ltObj = pvr.optJSONObject("lengthText");
                        if (ltObj != null) {
                            duration = ltObj.optString("simpleText", "");
                        }
                        if (duration.isEmpty() && pvr.has("lengthSeconds")) {
                            int secs = pvr.optInt("lengthSeconds", 0);
                            if (secs > 0) {
                                int m = secs / 60;
                                int s = secs % 60;
                                int h = m / 60;
                                m = m % 60;
                                duration = h > 0 ? String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s) : String.format(java.util.Locale.US, "%d:%02d", m, s);
                            }
                        }
                    }

                    if (videoId != null && videoId.matches("^[a-zA-Z0-9_-]{11}$") && title != null && !title.isEmpty()) {
                        if (thumbUrl == null || thumbUrl.isEmpty()) {
                            thumbUrl = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
                        }

                        YouTubeItem item = new YouTubeItem();
                        item.videoId = videoId;
                        item.title = title;
                        item.thumbnailUrl = thumbUrl;
                        item.pubDate = pubDate.isEmpty() ? "Recent" : pubDate;
                        item.channelId = channelId;
                        item.channelName = channelName;
                        item.authorChannelId = channelId;
                        item.description = channelId;
                        item.link = "https://www.youtube.com/watch?v=" + videoId;
                        item.duration = duration != null ? duration : "";
                        item.viewCount = viewCount != null ? viewCount : "";
                        item.fetchedAt = baseTime;
                        item.pubDateMillis = parseRelativeDateToMillis(pubDate, baseTime, fetchResult.items.size());

                        fetchResult.items.add(item);
                    }
                }

                Log.d(TAG, "Page " + (page + 1) + " fetched " + fetchResult.items.size() + " videos for " + channelName);

            } catch (Exception e) {
                Log.e(TAG, "Error fetching page " + page + " for channel/playlist " + channelId, e);
                break;
            }
        }

        fetchResult.nextContinuationToken = continuationToken;

        // Fallback to RSS if Innertube yielded no results on initial page
        if (fetchResult.items.isEmpty() && (startContinuationToken == null || startContinuationToken.isEmpty())) {
            Log.d(TAG, "Innertube empty, falling back to RSS for " + channelName);
            try {
                String feedUrl = isPlaylist
                        ? "https://www.youtube.com/feeds/videos.xml?playlist_id=" + (channelId.startsWith("VL") ? channelId.substring(2) : channelId)
                        : "https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId;
                Request req = new Request.Builder().url(feedUrl).build();
                Response resp = client.newCall(req).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    RssParser parser = new RssParser();
                    List<YouTubeItem> rssItems = parser.parseYouTube(resp.body().byteStream(), channelName);
                    for (YouTubeItem it : rssItems) {
                        if (it.channelId == null || it.channelId.isEmpty()) it.channelId = channelId;
                        if (it.link == null || it.link.isEmpty()) it.link = "https://www.youtube.com/watch?v=" + it.videoId;
                        it.fetchedAt = System.currentTimeMillis();
                    }
                    fetchResult.items.addAll(rssItems);
                }
            } catch (Exception ex) {
                Log.e(TAG, "RSS fallback also failed for " + channelId, ex);
            }
        }

        return fetchResult;
    }

    private static long parseRelativeDateToMillis(String pubDate, long baseTime, int itemIndex) {
        if (pubDate == null || pubDate.isEmpty()) {
            return baseTime - ((long) itemIndex * 60000L);
        }
        try {
            Pattern p = Pattern.compile("(\\d+)\\s*(second|minute|hour|day|week|month|year)", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(pubDate);
            if (m.find()) {
                long num = Long.parseLong(m.group(1));
                String unit = m.group(2).toLowerCase();
                long multiplier = 60000L;
                if (unit.startsWith("second")) multiplier = 1000L;
                else if (unit.startsWith("minute")) multiplier = 60000L;
                else if (unit.startsWith("hour")) multiplier = 3600000L;
                else if (unit.startsWith("day")) multiplier = 86400000L;
                else if (unit.startsWith("week")) multiplier = 7L * 86400000L;
                else if (unit.startsWith("month")) multiplier = 30L * 86400000L;
                else if (unit.startsWith("year")) multiplier = 365L * 86400000L;

                return baseTime - (num * multiplier);
            }
        } catch (Exception ignored) {}

        return baseTime - ((long) itemIndex * 60000L);
    }

    /**
     * Searches YouTube dynamically using Innertube, supporting sorting/filtering (e.g. CAI%3D for upload date) and pagination.
     */
    public static List<YouTubeItem> searchVideos(String query, String params, int maxPages) {
        FetchResult result = searchVideosWithContinuation(query, params, null, maxPages);
        return result.items;
    }

    public static FetchResult searchVideosWithContinuation(String query, String params, String startContinuationToken, int maxPages) {
        FetchResult fetchResult = new FetchResult();
        if ((query == null || query.isEmpty()) && (startContinuationToken == null || startContinuationToken.isEmpty())) {
            return fetchResult;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        String continuationToken = startContinuationToken;

        for (int page = 0; page < maxPages; page++) {
            try {
                JSONObject payload = new JSONObject();
                JSONObject context = new JSONObject();
                JSONObject clientObj = new JSONObject();
                clientObj.put("clientName", "WEB");
                clientObj.put("clientVersion", "2.20240501.01.00");
                clientObj.put("hl", "en");
                clientObj.put("gl", "IN");
                context.put("client", clientObj);
                payload.put("context", context);

                if (page == 0 && (continuationToken == null || continuationToken.isEmpty())) {
                    payload.put("query", query);
                    String effectiveParams = params;
                    if (effectiveParams == null || effectiveParams.isEmpty()) {
                        effectiveParams = "EgIQAQ%3D%3D"; // Filter: Type = Video
                    } else if ("CAM%3D".equals(effectiveParams)) {
                        effectiveParams = "EgIQAUBaA+gBAQ%3D%3D"; // Filter: Type = Video, Sort = View count
                    } else if ("CAI%3D".equals(effectiveParams)) {
                        effectiveParams = "EgIQAUgBogEA"; // Filter: Type = Video, Sort = Upload date
                    } else if ("CAE%3D".equals(effectiveParams)) {
                        effectiveParams = "EgIQAUBKAaIBAA%3D%3D"; // Filter: Type = Video, Sort = Rating
                    }
                    payload.put("params", effectiveParams);
                } else {
                    if (continuationToken == null || continuationToken.isEmpty()) {
                        break;
                    }
                    payload.put("continuation", continuationToken);
                }

                RequestBody body = RequestBody.create(payload.toString(), JSON_MEDIA_TYPE);
                Request request = new Request.Builder()
                        .url("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
                        .post(body)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    break;
                }

                String responseData = response.body().string();
                JSONObject json = new JSONObject(responseData);

                continuationToken = null;
                List<JSONObject> rawItems = new ArrayList<>();

                if (page == 0 && (startContinuationToken == null || startContinuationToken.isEmpty())) {
                    JSONObject contents = json.optJSONObject("contents");
                    if (contents != null) {
                        JSONObject twoCol = contents.optJSONObject("twoColumnSearchResultsRenderer");
                        if (twoCol != null) {
                            JSONObject primary = twoCol.optJSONObject("primaryContents");
                            if (primary != null) {
                                JSONObject sectionList = primary.optJSONObject("sectionListRenderer");
                                if (sectionList != null) {
                                    JSONArray secContents = sectionList.optJSONArray("contents");
                                    if (secContents != null) {
                                        for (int s = 0; s < secContents.length(); s++) {
                                            JSONObject sec = secContents.getJSONObject(s);
                                            if (sec.has("continuationItemRenderer")) {
                                                JSONObject cont = sec.optJSONObject("continuationItemRenderer");
                                                if (cont != null && cont.has("continuationEndpoint")) {
                                                    JSONObject ep = cont.optJSONObject("continuationEndpoint");
                                                    if (ep != null && ep.has("continuationCommand")) {
                                                        continuationToken = ep.getJSONObject("continuationCommand").optString("token", null);
                                                    }
                                                }
                                            }
                                            JSONObject itemSection = sec.optJSONObject("itemSectionRenderer");
                                            if (itemSection != null) {
                                                JSONArray cList = itemSection.optJSONArray("contents");
                                                if (cList != null) {
                                                    for (int c = 0; c < cList.length(); c++) {
                                                        rawItems.add(cList.getJSONObject(c));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    JSONArray commands = json.optJSONArray("onResponseReceivedCommands");
                    if (commands != null) {
                        for (int c = 0; c < commands.length(); c++) {
                            JSONObject act = commands.getJSONObject(c).optJSONObject("appendContinuationItemsAction");
                            if (act != null) {
                                JSONArray contItems = act.optJSONArray("continuationItems");
                                if (contItems != null) {
                                    for (int ci = 0; ci < contItems.length(); ci++) {
                                        JSONObject itemObj = contItems.getJSONObject(ci);
                                        if (itemObj.has("continuationItemRenderer")) {
                                            JSONObject cont = itemObj.optJSONObject("continuationItemRenderer");
                                            if (cont != null && cont.has("continuationEndpoint")) {
                                                JSONObject ep = cont.optJSONObject("continuationEndpoint");
                                                if (ep != null && ep.has("continuationCommand")) {
                                                    continuationToken = ep.getJSONObject("continuationCommand").optString("token", null);
                                                }
                                            }
                                        }
                                        JSONObject itemSection = itemObj.optJSONObject("itemSectionRenderer");
                                        if (itemSection != null) {
                                            JSONArray cList = itemSection.optJSONArray("contents");
                                            if (cList != null) {
                                                for (int k = 0; k < cList.length(); k++) {
                                                    rawItems.add(cList.getJSONObject(k));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                long baseTime = System.currentTimeMillis();
                for (int i = 0; i < rawItems.size(); i++) {
                    JSONObject contentObj = rawItems.get(i);
                    String videoId = null;
                    String title = null;
                    String pubDate = "";
                    String thumbUrl = null;
                    String channelName = "";

                    String channelBrowseId = null;
                    String duration = "";
                    String viewCount = "";

                    if (contentObj.has("channelRenderer")) {
                        JSONObject cr = contentObj.getJSONObject("channelRenderer");
                        String chId = cr.optString("channelId", "");
                        String chTitle = "";
                        JSONObject titleObj = cr.optJSONObject("title");
                        if (titleObj != null) {
                            chTitle = titleObj.optString("simpleText", "");
                        }
                        String chThumb = "";
                        JSONObject thumbObj = cr.optJSONObject("thumbnail");
                        if (thumbObj != null) {
                            JSONArray arr = thumbObj.optJSONArray("thumbnails");
                            if (arr != null && arr.length() > 0) {
                                chThumb = arr.getJSONObject(arr.length() - 1).optString("url", "");
                            }
                        }
                        if (!chId.isEmpty()) {
                            fetchResult.matchedChannelId = chId;
                            fetchResult.matchedChannelTitle = chTitle;
                            fetchResult.matchedChannelThumb = chThumb;
                        }
                    }

                    if (contentObj.has("lockupViewModel")) {
                        JSONObject lvm = contentObj.getJSONObject("lockupViewModel");
                        String cType = lvm.optString("contentType", "");
                        if ("LOCKUP_CONTENT_TYPE_PLAYLIST".equalsIgnoreCase(cType)
                                || "LOCKUP_CONTENT_TYPE_CHANNEL".equalsIgnoreCase(cType)
                                || "LOCKUP_CONTENT_TYPE_RADIO".equalsIgnoreCase(cType)) {
                            continue;
                        }
                        String candidateId = lvm.optString("contentId", null);
                        if (candidateId == null || !candidateId.matches("^[a-zA-Z0-9_-]{11}$")) {
                            continue;
                        }
                        videoId = candidateId;
                        JSONObject meta = lvm.optJSONObject("metadata");
                        if (meta != null) {
                            JSONObject lockupMeta = meta.optJSONObject("lockupMetadataViewModel");
                            if (lockupMeta != null) {
                                JSONObject titleObj = lockupMeta.optJSONObject("title");
                                if (titleObj != null) {
                                    title = titleObj.optString("content", "");
                                }
                                JSONObject metaInner = lockupMeta.optJSONObject("metadata");
                                if (metaInner != null) {
                                    JSONObject contentMeta = metaInner.optJSONObject("contentMetadataViewModel");
                                    if (contentMeta != null) {
                                        JSONArray rows = contentMeta.optJSONArray("metadataRows");
                                        if (rows != null) {
                                            for (int r = 0; r < rows.length(); r++) {
                                                JSONArray parts = rows.getJSONObject(r).optJSONArray("metadataParts");
                                                if (parts != null) {
                                                    for (int p = 0; p < parts.length(); p++) {
                                                        JSONObject partText = parts.getJSONObject(p).optJSONObject("text");
                                                        if (partText != null) {
                                                            String c = partText.optString("content", "");
                                                            if (c.contains("views") || c.contains("view")) {
                                                                viewCount = c;
                                                            } else if (c.contains("ago") || c.contains("Streamed")) {
                                                                pubDate = c;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        duration = extractDurationFromLockup(lvm);
                        JSONObject contentImage = lvm.optJSONObject("contentImage");
                        if (contentImage != null) {
                            JSONObject thumbVM = contentImage.optJSONObject("thumbnailViewModel");
                            if (thumbVM != null) {
                                JSONObject imageObj = thumbVM.optJSONObject("image");
                                if (imageObj != null) {
                                    JSONArray sources = imageObj.optJSONArray("sources");
                                    if (sources != null && sources.length() > 0) {
                                        thumbUrl = sources.getJSONObject(sources.length() - 1).optString("url");
                                    }
                                }
                            }
                        }
                    } else if (contentObj.has("videoRenderer")) {
                        JSONObject vr = contentObj.getJSONObject("videoRenderer");
                        videoId = vr.optString("videoId", null);
                        JSONObject titleObj = vr.optJSONObject("title");
                        if (titleObj != null) {
                            JSONArray runs = titleObj.optJSONArray("runs");
                            if (runs != null && runs.length() > 0) {
                                StringBuilder sb = new StringBuilder();
                                for (int r = 0; r < runs.length(); r++) {
                                    sb.append(runs.getJSONObject(r).optString("text", ""));
                                }
                                title = sb.toString();
                            } else {
                                title = titleObj.optString("simpleText", "");
                            }
                        }
                        JSONObject publishedObj = vr.optJSONObject("publishedTimeText");
                        if (publishedObj != null) {
                            pubDate = publishedObj.optString("simpleText", "");
                        }
                        JSONObject thumbObj = vr.optJSONObject("thumbnail");
                        if (thumbObj != null) {
                            JSONArray thumbs = thumbObj.optJSONArray("thumbnails");
                            if (thumbs != null && thumbs.length() > 0) {
                                thumbUrl = thumbs.getJSONObject(thumbs.length() - 1).optString("url");
                            }
                        }
                        duration = extractDurationFromVideoRenderer(vr);
                        JSONObject vcObj = vr.optJSONObject("shortViewCountText");
                        if (vcObj == null) vcObj = vr.optJSONObject("viewCountText");
                        if (vcObj != null) {
                            viewCount = vcObj.optString("simpleText", "");
                        }
                        JSONObject ownerObj = vr.optJSONObject("ownerText");
                        if (ownerObj == null) {
                            ownerObj = vr.optJSONObject("shortBylineText");
                        }
                        if (ownerObj != null) {
                            JSONArray runs = ownerObj.optJSONArray("runs");
                            if (runs != null && runs.length() > 0) {
                                JSONObject r0 = runs.getJSONObject(0);
                                channelName = r0.optString("text", "");
                                JSONObject nav = r0.optJSONObject("navigationEndpoint");
                                if (nav != null) {
                                    JSONObject be = nav.optJSONObject("browseEndpoint");
                                    if (be != null) {
                                        channelBrowseId = be.optString("browseId", "");
                                        if (channelBrowseId.isEmpty()) {
                                            channelBrowseId = be.optString("canonicalBaseUrl", "");
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (videoId != null && videoId.matches("^[a-zA-Z0-9_-]{11}$") && title != null && !title.isEmpty()) {
                        if (thumbUrl == null || thumbUrl.isEmpty()) {
                            thumbUrl = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
                        }
                        YouTubeItem item = new YouTubeItem();
                        item.videoId = videoId;
                        item.title = title;
                        item.thumbnailUrl = thumbUrl;
                        item.pubDate = pubDate.isEmpty() ? "Recent" : pubDate;
                        item.channelName = (channelName != null && !channelName.isEmpty()) ? channelName : "US News";
                        item.channelId = query;
                        item.authorChannelId = channelBrowseId;
                        item.description = (channelBrowseId != null && !channelBrowseId.isEmpty()) ? channelBrowseId : "";
                        item.link = "https://www.youtube.com/watch?v=" + videoId;
                        item.duration = duration != null ? duration : "";
                        item.viewCount = viewCount != null ? viewCount : "";
                        item.fetchedAt = baseTime;
                        item.pubDateMillis = parseRelativeDateToMillis(pubDate, baseTime, fetchResult.items.size());
                        fetchResult.items.add(item);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Search error", e);
                break;
            }
        }

        fetchResult.nextContinuationToken = continuationToken;
        return fetchResult;
    }

    /**
     * Resolves the current active 24x7 live stream video ID for a channel (e.g. UC... or channel URL).
     */
    public static String resolveLiveVideoId(String channelUrlOrId) {
        return resolveLiveVideoId(channelUrlOrId, null);
    }

    public static String resolveLiveVideoId(String channelUrlOrId, String channelTitle) {
        if (channelUrlOrId == null || channelUrlOrId.isEmpty()) return null;
        String liveUrl = channelUrlOrId;
        if (channelUrlOrId.startsWith("UC")) {
            liveUrl = "https://www.youtube.com/channel/" + channelUrlOrId + "/live";
        } else if (!channelUrlOrId.endsWith("/live")) {
            if (channelUrlOrId.contains("channel_id=")) {
                String cid = channelUrlOrId.substring(channelUrlOrId.indexOf("channel_id=") + 11);
                if (cid.contains("&")) cid = cid.substring(0, cid.indexOf("&"));
                liveUrl = "https://www.youtube.com/channel/" + cid + "/live";
            }
        }
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
                    .followRedirects(true)
                    .build();
            Request req = new Request.Builder()
                    .url(liveUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();
            Response resp = client.newCall(req).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                String html = resp.body().string();
                Pattern p = Pattern.compile("<link rel=\"canonical\" href=\"https://www.youtube.com/watch\\?v=([a-zA-Z0-9_-]{11})\">");
                Matcher m = p.matcher(html);
                if (m.find()) {
                    return m.group(1);
                }
                // Only extract videoId if the page actually indicates an active live broadcast
                if (html.contains("\"isLive\":true") || html.contains("\"label\":\"LIVE\"")) {
                    Pattern p2 = Pattern.compile("\"videoId\":\"([a-zA-Z0-9_-]{11})\"");
                    Matcher m2 = p2.matcher(html);
                    if (m2.find()) {
                        return m2.group(1);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking canonical live stream for " + channelUrlOrId, e);
        }

        // Secondary fallback: Innertube Live Search
        if (channelTitle != null && !channelTitle.trim().isEmpty()) {
            try {
                String cleanTitle = channelTitle.replace("24x7", "").replace("Live", "").trim();
                String query = cleanTitle + " Live";
                JSONObject context = new JSONObject();
                JSONObject clientJson = new JSONObject();
                clientJson.put("clientName", "WEB");
                clientJson.put("clientVersion", "2.20240101.00.00");
                clientJson.put("hl", "en");
                clientJson.put("gl", "IN");
                context.put("client", clientJson);

                JSONObject payload = new JSONObject();
                payload.put("context", context);
                payload.put("query", query);
                payload.put("params", "EgJAAQ%3D%3D");

                okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json; charset=utf-8");
                okhttp3.RequestBody body = okhttp3.RequestBody.create(payload.toString(), JSON);

                Request searchReq = new Request.Builder()
                        .url("https://www.youtube.com/youtubei/v1/search")
                        .post(body)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build();

                OkHttpClient searchClient = new OkHttpClient.Builder()
                        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                        .build();

                Response sResp = searchClient.newCall(searchReq).execute();
                if (sResp.isSuccessful() && sResp.body() != null) {
                    JSONObject sData = new JSONObject(sResp.body().string());
                    JSONObject contents = sData.optJSONObject("contents");
                    if (contents != null) {
                        JSONObject twoCol = contents.optJSONObject("twoColumnSearchResultsRenderer");
                        if (twoCol != null) {
                            JSONObject primary = twoCol.optJSONObject("primaryContents");
                            if (primary != null) {
                                JSONObject sectionList = primary.optJSONObject("sectionListRenderer");
                                if (sectionList != null) {
                                    JSONArray secContents = sectionList.optJSONArray("contents");
                                    if (secContents != null) {
                                        for (int i = 0; i < secContents.length(); i++) {
                                            JSONObject itSec = secContents.getJSONObject(i).optJSONObject("itemSectionRenderer");
                                            if (itSec != null) {
                                                JSONArray items = itSec.optJSONArray("contents");
                                                if (items != null) {
                                                    for (int j = 0; j < items.length(); j++) {
                                                        JSONObject vr = items.getJSONObject(j).optJSONObject("videoRenderer");
                                                        if (vr != null && vr.has("videoId")) {
                                                            return vr.getString("videoId");
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "Innertube search live fallback failed for " + channelTitle, ex);
            }
        }
        return null;
    }

    /**
     * Fetches comments for a YouTube video using the Innertube API.
     * If continuationToken is null, it resolves the initial comment section token first.
     * If continuationToken is provided, it fetches the next page of comments.
     */
    public static CommentResult fetchVideoComments(String videoId, String continuationToken) {
        CommentResult result = new CommentResult();
        if ((videoId == null || videoId.isEmpty()) && (continuationToken == null || continuationToken.isEmpty())) {
            return result;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        try {
            String activeContinuation = continuationToken;

            // Step 1: If no continuation token was provided, fetch the initial comment token for this video
            if (activeContinuation == null || activeContinuation.isEmpty()) {
                JSONObject payload1 = new JSONObject();
                JSONObject context1 = new JSONObject();
                JSONObject clientObj1 = new JSONObject();
                clientObj1.put("clientName", "WEB");
                clientObj1.put("clientVersion", "2.20230622.06.00");
                clientObj1.put("hl", "en");
                clientObj1.put("gl", "IN");
                context1.put("client", clientObj1);
                payload1.put("context", context1);
                payload1.put("videoId", videoId);

                Request req1 = new Request.Builder()
                        .url(NEXT_URL)
                        .post(RequestBody.create(JSON_MEDIA_TYPE, payload1.toString()))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .build();

                try (Response resp1 = client.newCall(req1).execute()) {
                    if (!resp1.isSuccessful() || resp1.body() == null) {
                        return result;
                    }
                    String body1 = resp1.body().string();
                    JSONObject json1 = new JSONObject(body1);

                    JSONObject contents = json1.optJSONObject("contents");
                    if (contents != null) {
                        JSONObject twoCol = contents.optJSONObject("twoColumnWatchNextResults");
                        if (twoCol != null) {
                            JSONObject res = twoCol.optJSONObject("results");
                            if (res != null) {
                                JSONObject innerRes = res.optJSONObject("results");
                                if (innerRes != null) {
                                    JSONArray conts = innerRes.optJSONArray("contents");
                                    if (conts != null) {
                                        for (int i = 0; i < conts.length(); i++) {
                                            JSONObject isr = conts.getJSONObject(i).optJSONObject("itemSectionRenderer");
                                            if (isr != null && "comment-item-section".equals(isr.optString("sectionIdentifier"))) {
                                                JSONArray isrContents = isr.optJSONArray("contents");
                                                if (isrContents != null) {
                                                    for (int j = 0; j < isrContents.length(); j++) {
                                                        JSONObject cir = isrContents.getJSONObject(j).optJSONObject("continuationItemRenderer");
                                                        if (cir != null) {
                                                            JSONObject ep = cir.optJSONObject("continuationEndpoint");
                                                            if (ep != null) {
                                                                JSONObject cmd = ep.optJSONObject("continuationCommand");
                                                                if (cmd != null && cmd.has("token")) {
                                                                    activeContinuation = cmd.getString("token");
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeContinuation == null || activeContinuation.isEmpty()) {
                result.commentsDisabled = true;
                return result;
            }

            // Step 2: Fetch comments using the continuation token
            JSONObject payload2 = new JSONObject();
            JSONObject context2 = new JSONObject();
            JSONObject clientObj2 = new JSONObject();
            clientObj2.put("clientName", "WEB");
            clientObj2.put("clientVersion", "2.20230622.06.00");
            clientObj2.put("hl", "en");
            clientObj2.put("gl", "IN");
            context2.put("client", clientObj2);
            payload2.put("context", context2);
            payload2.put("continuation", activeContinuation);

            Request req2 = new Request.Builder()
                    .url(NEXT_URL)
                    .post(RequestBody.create(JSON_MEDIA_TYPE, payload2.toString()))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build();

            try (Response resp2 = client.newCall(req2).execute()) {
                if (!resp2.isSuccessful() || resp2.body() == null) {
                    return result;
                }
                String body2 = resp2.body().string();
                JSONObject json2 = new JSONObject(body2);

                // 1. Check onResponseReceivedEndpoints for comment count & next page token & reply tokens
                JSONArray endpoints = json2.optJSONArray("onResponseReceivedEndpoints");
                Map<String, String> replyTokenMap = new HashMap<>();

                if (endpoints != null) {
                    for (int e = 0; e < endpoints.length(); e++) {
                        JSONObject ep = endpoints.getJSONObject(e);
                        JSONObject cmd = ep.optJSONObject("appendContinuationItemsAction");
                        if (cmd == null) cmd = ep.optJSONObject("appendContinuationItemsCommand");
                        if (cmd == null) cmd = ep.optJSONObject("reloadContinuationItemsCommand");
                        if (cmd == null && ep.has("continuationItems")) cmd = ep;

                        if (cmd != null) {
                            JSONArray items = cmd.optJSONArray("continuationItems");
                            if (items != null) {
                                for (int it = 0; it < items.length(); it++) {
                                    JSONObject item = items.getJSONObject(it);
                                    // Total comments count text
                                    JSONObject chr = item.optJSONObject("commentsHeaderRenderer");
                                    if (chr != null) {
                                        JSONObject countText = chr.optJSONObject("countText");
                                        if (countText != null) {
                                            if (countText.has("simpleText")) {
                                                result.totalCommentsCount = countText.getString("simpleText");
                                            } else if (countText.has("runs")) {
                                                JSONArray runs = countText.getJSONArray("runs");
                                                StringBuilder sb = new StringBuilder();
                                                for (int r = 0; r < runs.length(); r++) {
                                                    sb.append(runs.getJSONObject(r).optString("text", ""));
                                                }
                                                result.totalCommentsCount = sb.toString().trim();
                                            }
                                        }
                                    }

                                    // Next continuation token (for comments pagination or replies pagination)
                                    JSONObject cir = item.optJSONObject("continuationItemRenderer");
                                    if (cir != null) {
                                        JSONObject contEp = cir.optJSONObject("continuationEndpoint");
                                        if (contEp == null) {
                                            JSONObject btn = cir.optJSONObject("button");
                                            if (btn != null) {
                                                JSONObject btnRend = btn.optJSONObject("buttonRenderer");
                                                if (btnRend != null) {
                                                    contEp = btnRend.optJSONObject("command");
                                                }
                                            }
                                        }
                                        if (contEp != null) {
                                            JSONObject contCmd = contEp.optJSONObject("continuationCommand");
                                            if (contCmd != null && contCmd.has("token")) {
                                                result.nextContinuationToken = contCmd.getString("token");
                                            }
                                        }
                                    }

                                    // Comment thread renderer: check for replies and reply continuation token
                                    JSONObject ctr = item.optJSONObject("commentThreadRenderer");
                                    if (ctr != null) {
                                        String cid = "";
                                        JSONObject cvm = ctr.optJSONObject("commentViewModel");
                                        if (cvm != null) {
                                            JSONObject innerVm = cvm.optJSONObject("commentViewModel");
                                            if (innerVm != null) {
                                                cid = innerVm.optString("commentId", "");
                                            }
                                        }
                                        if (cid.isEmpty()) {
                                            JSONObject c = ctr.optJSONObject("comment");
                                            if (c != null) {
                                                JSONObject cr = c.optJSONObject("commentRenderer");
                                                if (cr != null) {
                                                    cid = cr.optString("commentId", "");
                                                }
                                            }
                                        }

                                        JSONObject replies = ctr.optJSONObject("replies");
                                        if (replies != null) {
                                            JSONObject crr = replies.optJSONObject("commentRepliesRenderer");
                                            if (crr != null) {
                                                JSONArray contents = crr.optJSONArray("contents");
                                                if (contents != null && contents.length() > 0) {
                                                    JSONObject firstContent = contents.getJSONObject(0);
                                                    JSONObject cir2 = firstContent.optJSONObject("continuationItemRenderer");
                                                    if (cir2 != null) {
                                                        JSONObject contEp2 = cir2.optJSONObject("continuationEndpoint");
                                                        if (contEp2 != null) {
                                                            JSONObject contCmd2 = contEp2.optJSONObject("continuationCommand");
                                                            if (contCmd2 != null && contCmd2.has("token")) {
                                                                String rToken = contCmd2.getString("token");
                                                                if (!cid.isEmpty() && !rToken.isEmpty()) {
                                                                    replyTokenMap.put(cid, rToken);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Parse comments from modern mutations (commentEntityPayload)
                JSONObject frameworkUpdates = json2.optJSONObject("frameworkUpdates");
                if (frameworkUpdates != null) {
                    JSONObject entityBatch = frameworkUpdates.optJSONObject("entityBatchUpdate");
                    if (entityBatch != null) {
                        JSONArray mutations = entityBatch.optJSONArray("mutations");
                        if (mutations != null) {
                            for (int m = 0; m < mutations.length(); m++) {
                                JSONObject mut = mutations.getJSONObject(m);
                                JSONObject payload = mut.optJSONObject("payload");
                                if (payload != null) {
                                    JSONObject cep = payload.optJSONObject("commentEntityPayload");
                                    if (cep != null) {
                                        YouTubeComment comment = new YouTubeComment();
                                        JSONObject author = cep.optJSONObject("author");
                                        if (author != null) {
                                            comment.authorName = author.optString("displayName", "User");
                                            comment.authorAvatarUrl = author.optString("avatarThumbnailUrl", "");
                                        }

                                        JSONObject props = cep.optJSONObject("properties");
                                        if (props != null) {
                                            comment.commentId = props.optString("commentId", "");
                                            comment.publishedTime = props.optString("publishedTime", "");
                                            JSONObject contentObj = props.optJSONObject("content");
                                            if (contentObj != null) {
                                                comment.commentText = contentObj.optString("content", "");
                                            }
                                        }

                                        JSONObject toolbar = cep.optJSONObject("toolbar");
                                        if (toolbar != null) {
                                            comment.likeCount = toolbar.optString("likeCountNotliked", "");
                                            if (toolbar.has("replyCount")) {
                                                try {
                                                    comment.replyCount = toolbar.getInt("replyCount");
                                                } catch (Exception ignored) {
                                                    try {
                                                        comment.replyCount = Integer.parseInt(toolbar.getString("replyCount").replaceAll("[^0-9]", ""));
                                                    } catch (Exception ignored2) {}
                                                }
                                            }
                                        }

                                        if (comment.commentId != null && replyTokenMap.containsKey(comment.commentId)) {
                                            comment.replyContinuationToken = replyTokenMap.get(comment.commentId);
                                            if (comment.replyCount <= 0) {
                                                comment.replyCount = 1;
                                            }
                                        }

                                        if (comment.commentId != null && comment.commentId.contains(".")) {
                                            comment.isReply = true;
                                        }

                                        if (comment.commentText != null && !comment.commentText.isEmpty()) {
                                            result.comments.add(comment);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Fallback: Parse classic commentRenderer if mutations did not contain comments
                if (result.comments.isEmpty() && endpoints != null) {
                    for (int e = 0; e < endpoints.length(); e++) {
                        JSONObject ep = endpoints.getJSONObject(e);
                        JSONObject cmd = ep.optJSONObject("appendContinuationItemsAction");
                        if (cmd == null) cmd = ep.optJSONObject("appendContinuationItemsCommand");
                        if (cmd == null) cmd = ep.optJSONObject("reloadContinuationItemsCommand");
                        if (cmd == null && ep.has("continuationItems")) cmd = ep;
                        if (cmd != null) {
                            JSONArray items = cmd.optJSONArray("continuationItems");
                            if (items != null) {
                                for (int it = 0; it < items.length(); it++) {
                                    JSONObject item = items.getJSONObject(it);
                                    JSONObject ctr = item.optJSONObject("commentThreadRenderer");
                                    if (ctr != null) {
                                        JSONObject c = ctr.optJSONObject("comment");
                                        if (c != null) {
                                            JSONObject cr = c.optJSONObject("commentRenderer");
                                            if (cr != null) {
                                                YouTubeComment comment = new YouTubeComment();
                                                comment.commentId = cr.optString("commentId", "");
                                                JSONObject authorText = cr.optJSONObject("authorText");
                                                if (authorText != null) {
                                                    comment.authorName = authorText.optString("simpleText", "User");
                                                }
                                                JSONObject authorThumb = cr.optJSONObject("authorThumbnail");
                                                if (authorThumb != null) {
                                                    JSONArray thumbs = authorThumb.optJSONArray("thumbnails");
                                                    if (thumbs != null && thumbs.length() > 0) {
                                                        comment.authorAvatarUrl = thumbs.getJSONObject(thumbs.length() - 1).optString("url", "");
                                                    }
                                                }
                                                JSONObject contentText = cr.optJSONObject("contentText");
                                                if (contentText != null) {
                                                    if (contentText.has("simpleText")) {
                                                        comment.commentText = contentText.getString("simpleText");
                                                    } else if (contentText.has("runs")) {
                                                        JSONArray runs = contentText.getJSONArray("runs");
                                                        StringBuilder sb = new StringBuilder();
                                                        for (int r = 0; r < runs.length(); r++) {
                                                            sb.append(runs.getJSONObject(r).optString("text", ""));
                                                        }
                                                        comment.commentText = sb.toString();
                                                    }
                                                }
                                                JSONObject pubTime = cr.optJSONObject("publishedTimeText");
                                                if (pubTime != null && pubTime.has("runs")) {
                                                    comment.publishedTime = pubTime.getJSONArray("runs").getJSONObject(0).optString("text", "");
                                                }
                                                JSONObject voteCount = cr.optJSONObject("voteCount");
                                                if (voteCount != null) {
                                                    comment.likeCount = voteCount.optString("simpleText", "");
                                                }
                                                JSONObject replyCountObj = cr.optJSONObject("replyCount");
                                                if (replyCountObj != null) {
                                                    comment.replyCount = replyCountObj.optInt("simpleText", 0);
                                                }

                                                if (comment.commentId != null && replyTokenMap.containsKey(comment.commentId)) {
                                                    comment.replyContinuationToken = replyTokenMap.get(comment.commentId);
                                                    if (comment.replyCount <= 0) {
                                                        comment.replyCount = 1;
                                                    }
                                                }

                                                if (comment.commentId != null && comment.commentId.contains(".")) {
                                                    comment.isReply = true;
                                                }

                                                if (comment.commentText != null && !comment.commentText.isEmpty()) {
                                                    result.comments.add(comment);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
        } catch (Exception ex) {
            Log.e(TAG, "Error fetching YouTube comments for video " + videoId, ex);
        }

        return result;
    }
}
