package com.app.webdroid.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.shobmc.san.R;
import com.app.webdroid.activity.ActivityRadioPlayer;
import com.app.webdroid.model.RadioStation;

public class RadioService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "RadioService";
    public static final String ACTION_PLAY = "com.app.webdroid.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.app.webdroid.ACTION_PAUSE";
    public static final String ACTION_STOP = "com.app.webdroid.ACTION_STOP";
    public static final String ACTION_TOGGLE = "com.app.webdroid.ACTION_TOGGLE";
    public static final String EXTRA_STATION = "extra_station";

    private static final String CHANNEL_ID = "radio_playback_channel";
    private static final int NOTIFICATION_ID = 9912;

    private final IBinder binder = new RadioBinder();
    private MediaPlayer mediaPlayer;
    private RadioStation currentStation;
    private boolean isPreparing = false;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private RadioStateCallback callback;

    public interface RadioStateCallback {
        void onPlaybackStateChanged(RadioStation station, boolean isPlaying, boolean isBuffering);
    }

    public class RadioBinder extends Binder {
        public RadioService getService() {
            return RadioService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        createNotificationChannel();
    }

    public void setCallback(RadioStateCallback cb) {
        this.callback = cb;
        if (currentStation != null && cb != null) {
            cb.onPlaybackStateChanged(currentStation, isPlaying(), isPreparing);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (ACTION_PLAY.equals(action)) {
                RadioStation station = (RadioStation) intent.getSerializableExtra(EXTRA_STATION);
                if (station != null) {
                    playStation(station);
                }
            } else if (ACTION_PAUSE.equals(action)) {
                pause();
            } else if (ACTION_STOP.equals(action)) {
                stopPlayback();
            } else if (ACTION_TOGGLE.equals(action)) {
                toggle();
            }
        }
        return START_NOT_STICKY;
    }

    public void playStation(RadioStation station) {
        if (currentStation != null && currentStation.getStreamUrl().equals(station.getStreamUrl()) && mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                return;
            } else if (!isPreparing) {
                resume();
                return;
            }
        }

        this.currentStation = station;
        stopMediaPlayer();

        if (!requestAudioFocus()) {
            return;
        }

        isPreparing = true;
        notifyCallback();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, buildNotification("Buffering...", false), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, buildNotification("Buffering...", false));
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());
            try {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("User-Agent", "VLC/3.0.0 LibVLC/3.0.0");
                android.net.Uri streamUri = android.net.Uri.parse(station.getStreamUrl());
                mediaPlayer.setDataSource(getApplicationContext(), streamUri, headers);
            } catch (Exception ex) {
                mediaPlayer.setDataSource(station.getStreamUrl());
            }
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "Error starting radio stream: " + station.getStreamUrl(), e);
            isPreparing = false;
            stopMediaPlayer();
            notifyCallback();
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPreparing = false;
            notifyCallback();
            updateNotification();
        }
    }

    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            if (requestAudioFocus()) {
                mediaPlayer.start();
                notifyCallback();
                updateNotification();
            }
        }
    }

    public void toggle() {
        if (isPlaying()) {
            pause();
        } else {
            resume();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public boolean isBuffering() {
        return isPreparing;
    }

    public RadioStation getCurrentStation() {
        return currentStation;
    }

    public void stop() {
        stopPlayback();
    }

    public void stopPlayback() {
        stopMediaPlayer();
        abandonAudioFocus();
        stopForeground(true);
        stopSelf();
        notifyCallback();
    }

    private void stopMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        isPreparing = false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPreparing = false;
        mp.start();
        notifyCallback();
        updateNotification();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer playback error: what=" + what + ", extra=" + extra);
        isPreparing = false;
        stopMediaPlayer();
        notifyCallback();
        updateNotification();
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        isPreparing = false;
        notifyCallback();
        updateNotification();
    }

    private void notifyCallback() {
        if (callback != null) {
            callback.onPlaybackStateChanged(currentStation, isPlaying(), isPreparing);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Live Radio Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows controls for 24/7 US News Radio streaming");
            channel.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String status, boolean isPlaying) {
        Intent openIntent = new Intent(this, ActivityRadioPlayer.class);
        PendingIntent pendingOpenIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent toggleIntent = new Intent(this, RadioService.class);
        toggleIntent.setAction(ACTION_TOGGLE);
        PendingIntent pendingToggleIntent = PendingIntent.getService(
                this, 1, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, RadioService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent pendingStopIntent = PendingIntent.getService(
                this, 2, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String stationName = currentStation != null ? currentStation.getName() : "US News & Talk Radio";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_radio)
                .setContentTitle(stationName)
                .setContentText(status)
                .setContentIntent(pendingOpenIntent)
                .setOngoing(isPlaying)
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow, isPlaying ? "Pause" : "Play", pendingToggleIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStopIntent);

        return builder.build();
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, buildNotification(isPlaying() ? "Playing Live" : "Paused", isPlaying()));
        }
    }

    private boolean requestAudioFocus() {
        if (audioManager == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(this)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build())
                    .build();
            return audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            return audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    private void abandonAudioFocus() {
        if (audioManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(this);
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            stopPlayback();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            pause();
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            resume();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMediaPlayer();
        abandonAudioFocus();
    }
}
