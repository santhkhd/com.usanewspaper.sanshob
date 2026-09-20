package com.solodroidx.ads.initialization;

import static com.solodroidx.ads.util.Constant.ADMOB;
import static com.solodroidx.ads.util.Constant.AD_STATUS_ON;
import static com.solodroidx.ads.util.Constant.FAN_BIDDING_ADMOB;
import static com.solodroidx.ads.util.Constant.FAN_BIDDING_AD_MANAGER;
import static com.solodroidx.ads.util.Constant.GOOGLE_AD_MANAGER;
import static com.solodroidx.ads.util.Constant.NONE;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.solodroidx.ads.callback.CallbackApps;
import com.solodroidx.ads.rest.ApiInterface;
import com.solodroidx.ads.rest.RestAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressWarnings("deprecation")
public class InitializeAd {

    private static final String TAG = "AdNetwork";
    Activity activity;
    private String adStatus = "";
    private String adNetwork = "";
    private String backupAdNetwork = "";
    private String adMobAppId = "";
    private String startappAppId = "0";
    private String unityGameId = "";
    private String appLovinSdkKey = "";
    private String mopubBannerId = "";
    private String ironSourceAppKey = "";
    private String wortiseAppId = "";
    private String pangleAppId = "";
    private String appodealAppKey = "";
    private String applicationId = "";
    private boolean debug = true;

    public InitializeAd(Activity activity) {
        this.activity = activity;
    }

    public InitializeAd build() {
        initAds();
        initBackupAds();
        return this;
    }

    public InitializeAd setApplicationId(String applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    public InitializeAd setAdStatus(String adStatus) {
        this.adStatus = adStatus;
        return this;
    }

    public InitializeAd setAdNetwork(String adNetwork) {
        this.adNetwork = adNetwork;
        return this;
    }

    public InitializeAd setBackupAdNetwork(String backupAdNetwork) {
        this.backupAdNetwork = backupAdNetwork;
        return this;
    }

    public InitializeAd setAdMobAppId(String adMobAppId) {
        this.adMobAppId = adMobAppId;
        return this;
    }

    public InitializeAd setStartappAppId(String startappAppId) {
        this.startappAppId = startappAppId;
        return this;
    }

    public InitializeAd setUnityGameId(String unityGameId) {
        this.unityGameId = unityGameId;
        return this;
    }

    public InitializeAd setAppLovinSdkKey(String appLovinSdkKey) {
        this.appLovinSdkKey = appLovinSdkKey;
        return this;
    }

    public InitializeAd setMopubBannerId(String mopubBannerId) {
        this.mopubBannerId = mopubBannerId;
        return this;
    }

    public InitializeAd setIronSourceAppKey(String ironSourceAppKey) {
        this.ironSourceAppKey = ironSourceAppKey;
        return this;
    }

    public InitializeAd setWortiseAppId(String wortiseAppId) {
        return this;
    }

    public InitializeAd setWortiseAppId(String wortiseAppId, String authorizationKey) {
        return this;
    }

    public InitializeAd setPangleAppId(String pangleAppId) {
        this.pangleAppId = pangleAppId;
        return this;
    }

    public InitializeAd setAppodealAppKey(String appodealAppKey) {
        this.appodealAppKey = appodealAppKey;
        return this;
    }

    public InitializeAd setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    private void initAds() {
        if (adStatus.equals(AD_STATUS_ON)) {
            switch (adNetwork) {
                case ADMOB:
                case GOOGLE_AD_MANAGER:
                case FAN_BIDDING_ADMOB:
                case FAN_BIDDING_AD_MANAGER:
                    MobileAds.initialize(activity, initializationStatus -> {
                        Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
                        for (String adapterClass : statusMap.keySet()) {
                            AdapterStatus adapterStatus = statusMap.get(adapterClass);
                            assert adapterStatus != null;
                            Log.d(TAG, String.format("Adapter name: %s, Description: %s, Latency: %d", adapterClass, adapterStatus.getDescription(), adapterStatus.getLatency()));
                        }
                    });
                    break;
            }
            Log.d(TAG, "[" + adNetwork + "] is selected as Primary Ads");
        }
        validate(applicationId);
    }

    private void initBackupAds() {
        if (adStatus.equals(AD_STATUS_ON)) {
            switch (backupAdNetwork) {
                case ADMOB:
                case GOOGLE_AD_MANAGER:
                case FAN_BIDDING_ADMOB:
                case FAN_BIDDING_AD_MANAGER:
                    MobileAds.initialize(activity, initializationStatus -> {
                        Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
                        for (String adapterClass : statusMap.keySet()) {
                            AdapterStatus adapterStatus = statusMap.get(adapterClass);
                            assert adapterStatus != null;
                            Log.d(TAG, String.format("Adapter name: %s, Description: %s, Latency: %d", adapterClass, adapterStatus.getDescription(), adapterStatus.getLatency()));
                        }
                    });
                    break;

                case NONE:
                    //do nothing
                    break;
            }
            Log.d(TAG, "[" + backupAdNetwork + "] is selected as Backup Ads");
        }
    }

    public void validate(String applicationId) {
        ApiInterface apiInterface = RestAdapter.createAPI("WVVoU01HTklUVFpNZVRsNVdWaGpkVm95YkRCaFNGWnBaRmhPYkdOdFRuWmlibEpzWW01UmRWa3lPWFJNTTA1MllrYzVhMk50T1hCYVIxWXlUREpXZFdSdFJqQmllVGwwV1Zkc2RVd3lTakZsVjFaNVRIYzlQUT09");
        Call<CallbackApps> callbackCall = apiInterface.getApps();
        callbackCall.enqueue(new Callback<CallbackApps>() {
            @Override
            public void onResponse(@NonNull Call<CallbackApps> call, @NonNull Response<CallbackApps> response) {
                CallbackApps resp = response.body();
                if (resp != null) {
                    List<String> appList = Arrays.asList(resp.application_id.replace(", ", ",").split(","));
                    List<String> filteredAppList = new ArrayList<>();
                    if (!appList.isEmpty()) {
                        for (String packageName : appList) {
                            if (packageName.equalsIgnoreCase(applicationId)) {
                                filteredAppList.add(packageName);
                            }
                        }
                        if (!filteredAppList.isEmpty()) {
                            new MaterialAlertDialogBuilder(activity)
                                    .setTitle(resp.title)
                                    .setMessage(Html.fromHtml(resp.message))
                                    .setPositiveButton("Buy Official License Now", (dialog, which) -> {
                                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(resp.url)));
                                        activity.finish();
                                    })
                                    .setNegativeButton("I have a Valid License", (dialog, which) -> {
                                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                                        intent.setData(Uri.parse("mailto:"));
                                        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"help.solodroid@gmail.com"});
                                        intent.putExtra(Intent.EXTRA_SUBJECT, "App blacklist removal request");
                                        String body = "To the Solodroid Help Support Team,\n" +
                                                "\n" +
                                                "Dear Sir,\n" +
                                                "\n" +
                                                "I am writing to formally request the removal of our application from your blacklist. The application in question is:\n" +
                                                "\n" +
                                                "Application ID: " + applicationId + " \n" +
                                                "Codecanyon Username: " + " [Your Codecanyon Username]\n" +
                                                "Valid Purchase Code: " + " [Your Item Purchase Code]\n" +
                                                "\n" +
                                                "We understand that our application may have been blacklisted for certain reasons. We have thoroughly reviewed and addressed the issues that may have led to this, and we are confident that our application now complies with all applicable guidelines and policies.\n" +
                                                "\n" +
                                                "We kindly ask for your reconsideration of our application's status and its removal from the blacklist. We are committed to ensuring our applications use an Official Purchase License from Codecanyon.\n" +
                                                "\n" +
                                                "Thank you for your time and attention to this matter.\n" +
                                                "\n" +
                                                "Sincerely,\n" +
                                                "\n" +
                                                "[Your Name]\n" +
                                                "[Your Contact Information]";

                                        intent.putExtra(Intent.EXTRA_TEXT, body);
                                        intent.setPackage("com.google.android.gm");
                                        try {
                                            activity.startActivity(Intent.createChooser(intent, "Send email using..."));
                                        } catch (android.content.ActivityNotFoundException ex) {
                                            Toast.makeText(activity.getApplicationContext(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                                        }
                                        activity.finish();
                                    })
                                    .setCancelable(false)
                                    .show();
                            Log.d(TAG, "Blacklisted applicationId found: " + applicationId);
                        } else {
                            Log.d(TAG, "No blacklisted applicationId found: " + applicationId);
                        }
                    }
                } else {
                    Log.d(TAG, "Validate response is null");
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackApps> call, @NonNull Throwable t) {
                Log.d(TAG, "Validate failure");
            }
        });
    }

}

