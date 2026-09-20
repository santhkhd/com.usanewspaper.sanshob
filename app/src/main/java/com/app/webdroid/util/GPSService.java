package com.app.webdroid.util;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.shobmc.san.R;
import com.app.webdroid.activity.MainActivity;

@SuppressWarnings("unused")
public class GPSService extends Service {

    private static final int ID = 121;
    NotificationCompat.Builder builder;
    private NotificationManager mNotificationManager;
    private PowerManager.WakeLock wakeLock;

    public class LocalBinder extends Binder {
        public GPSService getServiceInstance() {
            return GPSService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder(); // IBinder

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GPSLogger:wakelock");
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(ID, getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(ID, getNotification());
        }
        return START_NOT_STICKY;
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public IBinder onBind(Intent intent) {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }
        return mBinder;
    }

    @Override
    public void onDestroy() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    private Notification getNotification() {
        final String CHANNEL_ID = "GPSLoggerServiceChannel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Location Foreground Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Location channel for foreground service notification");
            mNotificationManager.createNotificationChannel(channel);
        }

        builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.ic_launcher_round)
                .setColor(ContextCompat.getColor(this, R.color.color_light_primary))
                .setContentTitle(getString(R.string.app_name))
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentText(composeContentText());

        final Intent startIntent = new Intent(getApplicationContext(), MainActivity.class);
        startIntent.setAction(Intent.ACTION_MAIN);
        startIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 1, startIntent,
                PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(contentIntent);
        return builder.build();
    }

    private String composeContentText() {
        return "Observing location";
    }
}
