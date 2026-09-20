package com.app.webdroid.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.shobmc.san.R;
import com.app.webdroid.activity.MainActivity;
import com.app.webdroid.database.AppDatabase;
import com.app.webdroid.model.AppConfig;
import com.app.webdroid.model.NewsItem;
import com.app.webdroid.model.YouTubeItem;
import com.app.webdroid.util.RssParser;
import com.google.gson.Gson;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.app.webdroid.model.Navigation;
import java.util.ArrayList;

public class SyncWorker extends Worker {

    private static final String PREF_NAME = "app_config";
    private static final String KEY_CONFIG = "json_config";
    private static final String CHANNEL_ID = "sync_channel";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CONFIG, null);

        if (json == null) {
            android.util.Log.e("SyncWorker", "Config JSON is null in Prefs");
            return Result.failure(); // No config to sync
        }

        AppConfig config = new Gson().fromJson(json, AppConfig.class);
        android.util.Log.d("SyncWorker", "Config loaded. Channels: "
                + (config.youtubeChannels != null ? config.youtubeChannels.size() : "null"));

        AppDatabase db = AppDatabase.getDatabase(context);
        OkHttpClient client = new OkHttpClient();
        RssParser parser = new RssParser();

        int newNewsArgs = 0;
        int newVideoArgs = 0;

        // Sync RSS
        // Sync RSS
        List<AppConfig.RssSource> rssSources = new ArrayList<>();
        if (config.rssNews != null) {
            rssSources.addAll(config.rssNews);
        }
        if (config.menus != null) {
            for (Navigation nav : config.menus) {
                if ("rss".equalsIgnoreCase(nav.type)) {
                    AppConfig.RssSource source = new AppConfig.RssSource();
                    source.title = nav.name;
                    source.url = nav.url;
                    rssSources.add(source);
                }
            }
        }

        if (!rssSources.isEmpty()) {
            java.util.concurrent.ExecutorService rssExecutor = java.util.concurrent.Executors.newFixedThreadPool(8);
            java.util.concurrent.atomic.AtomicInteger newsCounter = new java.util.concurrent.atomic.AtomicInteger(0);
            List<java.util.concurrent.Future<?>> rssFutures = new ArrayList<>();

            for (AppConfig.RssSource source : rssSources) {
                rssFutures.add(rssExecutor.submit(() -> {
                    try {
                        Request request = new Request.Builder().url(source.url).build();
                        Response response = client.newCall(request).execute();
                        if (response.isSuccessful() && response.body() != null) {
                            InputStream stream = response.body().byteStream();
                            List<NewsItem> items = parser.parseNews(stream, source.title);
                            if (!items.isEmpty()) {
                                // Check for duplicates handled by DB IGNORE strategy
                                // To count new items, check returned row IDs
                                List<Long> rows = db.newsDao().insertNews(items);
                                int inserted = 0;
                                for (Long id : rows) {
                                    if (id != -1)
                                        inserted++;
                                }
                                newsCounter.addAndGet(inserted);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }));
            }

            for (java.util.concurrent.Future<?> f : rssFutures) {
                try {
                    f.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            rssExecutor.shutdown();
            newNewsArgs = newsCounter.get();
        }

        // Sync YouTube
        java.util.concurrent.atomic.AtomicReference<YouTubeItem> latestVideoRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        if (config.youtubeChannels != null && !config.youtubeChannels.isEmpty()) {
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(4);
            java.util.concurrent.atomic.AtomicInteger videoCounter = new java.util.concurrent.atomic.AtomicInteger(0);
            List<java.util.concurrent.Future<?>> futures = new ArrayList<>();

            for (AppConfig.YouTubeSource source : config.youtubeChannels) {
                futures.add(executor.submit(() -> {
                    android.util.Log.d("SyncWorker",
                            "Fetching YouTube videos for channel: " + source.name + " ID: " + source.channelId);
                    List<YouTubeItem> items = com.app.webdroid.util.YouTubeInnertubeFetcher.fetchChannelVideos(source.channelId, source.name, 2);

                    // Insert to DB - IGNORE strategy ensures items accumulate over time
                    if (!items.isEmpty()) {
                        List<Long> rows = db.youTubeDao().insertVideos(items);
                        int inserted = 0;
                        for (int i = 0; i < rows.size(); i++) {
                            if (rows.get(i) != -1) {
                                inserted++;
                                if (latestVideoRef.get() == null && i < items.size()) {
                                    latestVideoRef.set(items.get(i));
                                }
                            }
                        }
                        videoCounter.addAndGet(inserted);
                        android.util.Log.d("SyncWorker", "Inserted " + inserted + " new videos for " + source.name);
                    }
                }));
            }

            // Wait for all tasks to complete
            for (java.util.concurrent.Future<?> f : futures) {
                try {
                    f.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            executor.shutdown();
            newVideoArgs = videoCounter.get();
            android.util.Log.d("SyncWorker", "Total Videos added to DB in this sync: " + newVideoArgs);
        } else {
            android.util.Log.w("SyncWorker", "No YouTube channels in config");
        }

        if (newVideoArgs > 0 && latestVideoRef.get() != null) {
            sendVideoNotification(context, latestVideoRef.get());
        } else if (newNewsArgs > 0) {
            sendGeneralNotification(context, "📰 Latest News & Updates", "New articles and news updates available.");
        } else {
            android.util.Log.d("SyncWorker", "No new items found");
        }

        return Result.success();
    }

    private void sendVideoNotification(Context context, YouTubeItem videoItem) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        String channelId = context.getString(R.string.fcm_notification_channel_id);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Content Updates",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        String ch = (videoItem.channelName != null && !videoItem.channelName.isEmpty()) ? videoItem.channelName : "YouTube";
        String title = "🎬 " + ch + " • New Upload!";
        String content = videoItem.title != null ? videoItem.title : "Watch the new video now!";

        String thumbUrl = videoItem.thumbnailUrl;
        if ((thumbUrl == null || thumbUrl.isEmpty()) && videoItem.videoId != null) {
            thumbUrl = "https://img.youtube.com/vi/" + videoItem.videoId + "/hqdefault.jpg";
        }

        Intent intent = new Intent(context, com.app.webdroid.activity.ActivityVideoDetail.class);
        intent.putExtra("videoId", videoItem.videoId);
        intent.putExtra("title", videoItem.title);
        intent.putExtra("thumbUrl", thumbUrl);
        intent.putExtra("channelName", videoItem.channelName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_onesignal_default)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Fetch & set big picture YouTube thumbnail
        if (thumbUrl != null && !thumbUrl.isEmpty()) {
            try {
                android.graphics.Bitmap bitmap = com.bumptech.glide.Glide.with(context)
                        .asBitmap()
                        .load(thumbUrl)
                        .submit()
                        .get();
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap);
                    builder.setStyle(new NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .showBigPictureWhenCollapsed(true)
                            .bigLargeIcon((android.graphics.Bitmap) null)
                            .setBigContentTitle(title)
                            .setSummaryText(content));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Add "▶ Watch Now" Play Action Button
        builder.addAction(R.drawable.ic_play_arrow, "▶ Watch Now", pendingIntent);

        manager.notify(1001, builder.build());
    }

    private void sendGeneralNotification(Context context, String title, String content) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        String channelId = context.getString(R.string.fcm_notification_channel_id);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Content Updates",
                    NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_onesignal_default)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        manager.notify(1002, builder.build());
    }
}
