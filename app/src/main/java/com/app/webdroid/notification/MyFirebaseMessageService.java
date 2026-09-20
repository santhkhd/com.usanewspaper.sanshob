package com.app.webdroid.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.shobmc.san.R;
import com.app.webdroid.activity.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.bumptech.glide.Glide;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class MyFirebaseMessageService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = null;
        String message = null;
        String bigImage = null;
        String link = null;
        String uniqueId = null;
        String postId = null;

        // 1. Notification payload (e.g. from Firebase Console or standard FCM)
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            message = remoteMessage.getNotification().getBody();
            if (remoteMessage.getNotification().getImageUrl() != null) {
                bigImage = remoteMessage.getNotification().getImageUrl().toString();
            }
        }

        // 2. Data payload (e.g. from Python automation script)
        if (!remoteMessage.getData().isEmpty()) {
            Map<String, String> data = remoteMessage.getData();
            Log.d("FCM_RECEIVE", "Data payload: " + data);

            if (data.get("title") != null && !data.get("title").isEmpty()) title = data.get("title");
            if (data.get("message") != null && !data.get("message").isEmpty()) message = data.get("message");
            if (data.get("body") != null && !data.get("body").isEmpty() && (message == null || message.isEmpty())) message = data.get("body");
            if (data.get("big_image") != null && !data.get("big_image").isEmpty()) bigImage = data.get("big_image");
            if (data.get("image") != null && !data.get("image").isEmpty() && (bigImage == null || bigImage.isEmpty())) bigImage = data.get("image");
            if (data.get("link") != null) link = data.get("link");
            if (data.get("unique_id") != null) uniqueId = data.get("unique_id");
            if (data.get("post_id") != null) postId = data.get("post_id");
        }

        if (title == null || title.isEmpty()) {
            title = getString(R.string.app_name);
        }
        if (message == null || message.isEmpty()) {
            message = "New update available!";
        }

        createNotification(uniqueId, title, message, bigImage, link, postId);
    }

    private String extractYouTubeId(String url) {
        if (url == null || url.isEmpty()) return null;
        if (url.matches("^[a-zA-Z0-9_-]{11}$")) return url;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?:youtu\\.be\\/|youtube\\.com\\/(?:watch\\?v=|embed\\/|v\\/|shorts\\/)|yt:video:)([a-zA-Z0-9_-]{11})").matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void createNotification(String uniqueId, String title, String message, String imageUrl, String link,
            String postId) {

        String videoId = extractYouTubeId(link);
        if (videoId == null && postId != null) {
            videoId = extractYouTubeId(postId);
        }

        if (videoId != null && (imageUrl == null || imageUrl.isEmpty())) {
            imageUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
        }

        Intent intent;
        if (videoId != null) {
            intent = new Intent(this, com.app.webdroid.activity.ActivityVideoDetail.class);
            intent.putExtra("videoId", videoId);
            intent.putExtra("title", title);
            intent.putExtra("thumbUrl", imageUrl);
            intent.putExtra("channelName", title);
        } else {
            intent = new Intent(this, MainActivity.class);
            intent.putExtra("unique_id", uniqueId);
            intent.putExtra("post_id", postId);
            intent.putExtra("title", title);
            intent.putExtra("link", link);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        String NOTIFICATION_CHANNEL_ID = getString(R.string.fcm_notification_channel_id);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationChannel.setDescription("New video releases and news alerts");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{0, 400, 200, 400});
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(getNotificationIcon(notificationBuilder))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent);

        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationBuilder.setSound(alarmSound).setVibrate(new long[] { 0, 400, 200, 400 });

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Bitmap image = fetchBitmap(imageUrl);
            if (image != null) {
                notificationBuilder.setLargeIcon(image);
                notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(image)
                        .showBigPictureWhenCollapsed(true)
                        .bigLargeIcon((Bitmap) null)
                        .setBigContentTitle(title)
                        .setSummaryText(message));
            }
        }

        // Add "▶ Watch Now" Play Action Button for YouTube videos
        if (videoId != null) {
            notificationBuilder.addAction(R.drawable.ic_play_arrow, "▶ Watch Now", pendingIntent);
        }

        int notifId = (int) System.currentTimeMillis();
        if (uniqueId != null && !uniqueId.trim().isEmpty()) {
            try {
                String cleanDigits = uniqueId.replaceAll("[^0-9]", "");
                if (!cleanDigits.isEmpty()) {
                    notifId = Integer.parseInt(cleanDigits) % 1000000;
                }
            } catch (Exception ignored) {}
        }

        notificationManager.notify(notifId, notificationBuilder.build());
    }

    private int getNotificationIcon(NotificationCompat.Builder notificationBuilder) {
        notificationBuilder.setColor(ContextCompat.getColor(getApplicationContext(), R.color.color_light_primary));
        return R.drawable.ic_stat_onesignal_default;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private Bitmap fetchBitmap(String src) {
        try {
            if (src != null) {
                return Glide.with(getApplicationContext())
                        .asBitmap()
                        .load(src)
                        .submit()
                        .get();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
