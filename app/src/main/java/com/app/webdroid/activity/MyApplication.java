package com.app.webdroid.activity;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDex;

import com.app.webdroid.Config;
import com.shobmc.san.R;
import com.app.webdroid.database.prefs.AdsPref;
import com.app.webdroid.database.prefs.SharedPref;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.solodroid.push.sdk.provider.OneSignalPush;
import com.solodroidx.ads.appopen.AppOpenAd;
import com.solodroidx.ads.appopen.AppOpenAdAppLovin;
import com.solodroidx.ads.appopen.AppOpenAdManager;
import com.solodroidx.ads.appopen.AppOpenAdMob;
import com.solodroidx.ads.appopen.AppOpenAdPangle;
import com.solodroidx.ads.appopen.AppOpenAdWortise;
import com.solodroidx.ads.appopen.AppOpenAdYandex;
import com.solodroidx.ads.listener.OnShowAdCompleteListener;
import com.solodroidx.ads.util.Constant;

public class MyApplication extends Application {

    AppOpenAd appOpenAd;
    FirebaseAnalytics mFirebaseAnalytics;
    SharedPref sharedPref;
    AdsPref adsPref;

    @Override
    public void onCreate() {
        super.onCreate();
        // Purge old disk cache and legacy configs to guarantee fresh USA datasets
        try {
            com.app.webdroid.util.AppJsonManager.clearCache(this);
            getSharedPreferences("app_config", Context.MODE_PRIVATE).edit().clear().apply();
            getSharedPreferences("custom_user_channels_pref", Context.MODE_PRIVATE).edit().clear().apply();
            getSharedPreferences(com.app.webdroid.util.Tools.getApplicationId() + "_ads_prefs", Context.MODE_PRIVATE).edit().clear().apply();
        } catch (Exception ignored) {}

        try {
            FirebaseApp.initializeApp(this);
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);

        try {
            initOpenAds();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (com.app.webdroid.util.Tools.isDebug()) {
                String testDeviceId = com.app.webdroid.util.Tools.getAdMobTestDeviceId(this);
                java.util.List<String> testDeviceIds = new java.util.ArrayList<>();
                testDeviceIds.add(com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR);
                testDeviceIds.add("61143FA4F558E5F35D032B30280D1FC4");
                testDeviceIds.add("2E3547E7DE80ADD99D8B4C5C8512F162");
                testDeviceIds.add("e4a7eea7-65b9-49e7-adb1-26b686602f44");
                testDeviceIds.add("E4A7EEA7-65B9-49E7-ADB1-26B686602F44");
                if (testDeviceId != null && !testDeviceId.isEmpty() && !testDeviceIds.contains(testDeviceId)) {
                    testDeviceIds.add(testDeviceId);
                    android.util.Log.d("AdMob", "Registered AdMob Test Device (Android ID): " + testDeviceId);
                }

                android.content.SharedPreferences sp = getSharedPreferences("admob_test_devices", android.content.Context.MODE_PRIVATE);
                String customId = sp.getString("custom_test_device_id", "");
                if (customId != null && !customId.isEmpty() && !testDeviceIds.contains(customId)) {
                    testDeviceIds.add(customId);
                    android.util.Log.d("AdMob", "Registered Custom Test Device: " + customId);
                }

                com.google.android.gms.ads.RequestConfiguration configuration =
                        new com.google.android.gms.ads.RequestConfiguration.Builder()
                                .setTestDeviceIds(testDeviceIds)
                                .build();
                com.google.android.gms.ads.MobileAds.setRequestConfiguration(configuration);

                // Asynchronously register Advertising ID (GAID) MD5 hash as well
                com.app.webdroid.util.Tools.fetchGoogleAdvertisingId(this, (aaid, md5Aaid, md5AndroidId) -> {
                    boolean updated = false;
                    if (md5Aaid != null && !md5Aaid.isEmpty() && !testDeviceIds.contains(md5Aaid)) {
                        testDeviceIds.add(md5Aaid);
                        updated = true;
                    }
                    if (updated) {
                        com.google.android.gms.ads.RequestConfiguration updatedConfig =
                                new com.google.android.gms.ads.RequestConfiguration.Builder()
                                        .setTestDeviceIds(testDeviceIds)
                                        .build();
                        com.google.android.gms.ads.MobileAds.setRequestConfiguration(updatedConfig);
                        android.util.Log.d("AdMob", "Updated AdMob test devices with AAID hash: " + md5Aaid);
                    }
                });
            }

            com.google.android.gms.ads.MobileAds.initialize(this, initializationStatus -> {
                android.util.Log.d("AdMob", "Google MobileAds initialized in Application");
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            initNotification();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load remote configuration safely
        try {
            com.app.webdroid.util.ConfigManager.getInstance().loadConfig(this, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Schedule background sync safely
        try {
            androidx.work.PeriodicWorkRequest syncRequest = new androidx.work.PeriodicWorkRequest.Builder(
                    com.app.webdroid.worker.SyncWorker.class, 15, java.util.concurrent.TimeUnit.MINUTES)
                    .build();
            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "sync_worker",
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initNotification() {
        try {
            String oneSignalId = getResources().getString(R.string.onesignal_app_id);
            if (oneSignalId != null && !oneSignalId.isEmpty() && !oneSignalId.startsWith("0000")) {
                new OneSignalPush.Builder(this)
                        .setOneSignalAppId(oneSignalId)
                        .build(() -> {
                            try {
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                intent.putExtra(OneSignalPush.EXTRA_ID, OneSignalPush.Data.id);
                                intent.putExtra(OneSignalPush.EXTRA_TITLE, OneSignalPush.Data.title);
                                intent.putExtra(OneSignalPush.EXTRA_MESSAGE, OneSignalPush.Data.message);
                                intent.putExtra(OneSignalPush.EXTRA_IMAGE, OneSignalPush.Data.bigImage);
                                intent.putExtra(OneSignalPush.EXTRA_LAUNCH_URL, OneSignalPush.Data.launchUrl);
                                intent.putExtra(OneSignalPush.EXTRA_UNIQUE_ID, OneSignalPush.AdditionalData.uniqueId);
                                intent.putExtra(OneSignalPush.EXTRA_POST_ID, OneSignalPush.AdditionalData.postId);
                                intent.putExtra(OneSignalPush.EXTRA_LINK, OneSignalPush.AdditionalData.link);
                                startActivity(intent);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        createNotificationChannel();

        try {
            String topic = getResources().getString(R.string.fcm_notification_topic);
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            android.util.Log.d("FCM_INIT", "Successfully subscribed to topic: " + topic);
                        } else {
                            android.util.Log.e("FCM_INIT", "Failed to subscribe to topic: " + topic, task.getException());
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                String channelId = getString(R.string.fcm_notification_channel_id);
                String channelName = getString(R.string.app_name);
                android.app.NotificationChannel channel = new android.app.NotificationChannel(
                        channelId,
                        channelName,
                        android.app.NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("New video releases and news alerts");
                channel.enableLights(true);
                channel.setLightColor(android.graphics.Color.RED);
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 400, 200, 400});

                android.app.NotificationManager manager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private void initOpenAds() {
        adsPref = new AdsPref(this);
        if (adsPref.getAdStatus()) {
            if (!Config.FORCE_TO_SHOW_APP_OPEN_AD_ON_START) {
                registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
                ProcessLifecycleOwner.get().getLifecycle().addObserver(lifecycleObserver);
                appOpenAd = new AppOpenAd()
                        .initAppOpenAdMob(new AppOpenAdMob())
                        .initAppOpenAdManager(new AppOpenAdManager())
                        .initAppOpenAdAppLovin(new AppOpenAdAppLovin())
                        .initAppOpenAdWortise(new AppOpenAdWortise())
                        .initAppOpenAdPangle(new AppOpenAdPangle())
                        .initAppOpenAdYandex(new AppOpenAdYandex())
                        .setAdStatus(Constant.AD_STATUS_ON)
                        .setAdNetwork(adsPref.getMainAds())
                        .setBackupAdNetwork(adsPref.getBackupAds())
                        .setPlacementOnStart(adsPref.getIsAppOpenAdOnStart())
                        .setPlacementOnResume(adsPref.getIsAppOpenAdOnResume())
                        .setAdMobAppOpenId(adsPref.getAdMobAppOpenAdId())
                        .setAdManagerAppOpenId(adsPref.getAdManagerAppOpenAdId())
                        .setApplovinAppOpenId(adsPref.getAppLovinAppOpenAdUnitId())
                        .setWortiseAppOpenId(adsPref.getWortiseAppOpenAdUnitId());
            }
        }
    }

    LifecycleObserver lifecycleObserver = new DefaultLifecycleObserver() {
        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
            DefaultLifecycleObserver.super.onStart(owner);
            if (AppOpenAd.isAppOpenAdLoaded && appOpenAd != null) {
                appOpenAd.setOnStartLifecycleObserver();
            }
        }
    };

    ActivityLifecycleCallbacks activityLifecycleCallbacks = new ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            if (appOpenAd != null) {
                appOpenAd.setOnStartActivityLifecycleCallbacks(activity);
            }
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
        }
    };

    public void showAdIfAvailable(@NonNull Activity activity,
            @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
        if (appOpenAd != null) {
            appOpenAd.showAdIfAvailable(activity, onShowAdCompleteListener);
        } else {
            onShowAdCompleteListener.onShowAdComplete();
        }
    }

}