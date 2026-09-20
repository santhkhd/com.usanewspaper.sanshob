package com.app.webdroid.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.shobmc.san.R;

/**
 * Complete Google AdMob Sample Implementation for ALL Ad Types:
 * 1. App Open Ad
 * 2. Adaptive / Standard Banner Ad
 * 3. Collapsible Banner Ad
 * 4. Interstitial Ad
 * 5. Rewarded Video Ad
 * 6. Rewarded Interstitial Ad
 * 7. Native Advanced Ad
 * 8. Google Ad Inspector
 */
public class AdMobSampleManager {

    private static final String TAG = "AdMobSample";

    // =========================================================================
    // OFFICIAL GOOGLE ADMOB SAMPLE / TEST AD UNIT IDS
    // =========================================================================
    public static final String SAMPLE_APP_ID = "ca-app-pub-3940256099942544~3347511713";
    public static final String SAMPLE_APP_OPEN = "ca-app-pub-3940256099942544/9257395921";
    public static final String SAMPLE_BANNER = "ca-app-pub-3940256099942544/6300978111";
    public static final String SAMPLE_COLLAPSIBLE_BANNER = "ca-app-pub-3940256099942544/2014213617";
    public static final String SAMPLE_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    public static final String SAMPLE_REWARDED = "ca-app-pub-3940256099942544/5224354917";
    public static final String SAMPLE_REWARDED_INTERSTITIAL = "ca-app-pub-3940256099942544/5354046379";
    public static final String SAMPLE_NATIVE_ADVANCED = "ca-app-pub-3940256099942544/2247696110";
    public static final String SAMPLE_NATIVE_VIDEO = "ca-app-pub-3940256099942544/1044960115";

    // =========================================================================
    // YOUR APP'S PRODUCTION ADMOB AD UNIT IDS (MALAYALAM MOVIE LIST)
    // =========================================================================
    public static final String REAL_APP_ID = SAMPLE_APP_ID;
    public static final String REAL_BANNER = SAMPLE_BANNER;
    public static final String REAL_INTERSTITIAL = SAMPLE_INTERSTITIAL;
    public static final String REAL_NATIVE = SAMPLE_NATIVE_ADVANCED;
    public static final String REAL_APP_OPEN = SAMPLE_APP_OPEN;

    // Cached references
    private static InterstitialAd cachedInterstitialAd;
    private static RewardedAd cachedRewardedAd;
    private static RewardedInterstitialAd cachedRewardedInterstitialAd;
    private static AppOpenAd cachedAppOpenAd;

    // =========================================================================
    // 0. INITIALIZE MOBILE ADS SDK
    // =========================================================================
    public static void initialize(@NonNull Context context, @Nullable Runnable onComplete) {
        MobileAds.initialize(context, initializationStatus -> {
            Log.d(TAG, "AdMob MobileAds initialized successfully");
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    // =========================================================================
    // 1. APP OPEN AD
    // =========================================================================
    public static void loadAndShowAppOpenAd(@NonNull Activity activity, @Nullable String adUnitId) {
        final String unitId = (adUnitId != null && !adUnitId.isEmpty()) ? adUnitId : SAMPLE_APP_OPEN;
        AlertDialog loadingDialog = showLoadingDialog(activity, "Loading App Open Ad...");

        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(activity, unitId, request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                Log.d(TAG, "App Open Ad loaded successfully");
                safeDismiss(loadingDialog);
                cachedAppOpenAd = appOpenAd;
                appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        Log.d(TAG, "App Open Ad dismissed");
                        cachedAppOpenAd = null;
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        Log.e(TAG, "App Open Ad failed to show: " + adError.getMessage());
                        cachedAppOpenAd = null;
                    }
                });
                appOpenAd.show(activity);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "App Open Ad failed to load: " + loadAdError.getMessage());
                safeDismiss(loadingDialog);
                showAdErrorDialog(activity, "App Open Ad", loadAdError);
            }
        });
    }

    // =========================================================================
    // 2. ADAPTIVE BANNER AD
    // =========================================================================
    public static AdView loadAdaptiveBannerAd(@NonNull Activity activity, @NonNull ViewGroup container,
                                              @Nullable String adUnitId, @Nullable Runnable onLoaded,
                                              @Nullable Runnable onFailed) {
        final String unitId = (adUnitId != null && !adUnitId.isEmpty()) ? adUnitId : SAMPLE_BANNER;

        container.removeAllViews();
        ProgressBar spinner = new ProgressBar(activity);
        container.addView(spinner);

        AdView adView = new AdView(activity);
        adView.setAdUnitId(unitId);

        AdSize adSize = getAdaptiveAdSize(activity);
        adView.setAdSize(adSize);

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "Adaptive Banner Ad loaded");
                container.removeAllViews();
                container.addView(adView);
                Toast.makeText(activity, "✅ Adaptive Banner loaded!", Toast.LENGTH_SHORT).show();
                if (onLoaded != null) onLoaded.run();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "Adaptive Banner Ad failed to load: " + loadAdError.getMessage());
                container.removeAllViews();
                TextView errorTv = createErrorTextView(activity, "Banner Error: " + formatErrorMessage(loadAdError));
                container.addView(errorTv);
                if (onFailed != null) onFailed.run();
            }
        });

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        return adView;
    }

    // =========================================================================
    // 3. COLLAPSIBLE BANNER AD
    // =========================================================================
    public static AdView loadCollapsibleBannerAd(@NonNull Activity activity, @NonNull ViewGroup container,
                                                 @Nullable String adUnitId, boolean isBottom) {
        final String unitId = (adUnitId != null && !adUnitId.isEmpty()) ? adUnitId : SAMPLE_COLLAPSIBLE_BANNER;

        container.removeAllViews();
        ProgressBar spinner = new ProgressBar(activity);
        container.addView(spinner);

        AdView adView = new AdView(activity);
        adView.setAdUnitId(unitId);

        AdSize adSize = getAdaptiveAdSize(activity);
        adView.setAdSize(adSize);

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "Collapsible Banner Ad loaded");
                container.removeAllViews();
                container.addView(adView);
                Toast.makeText(activity, "✅ Collapsible Banner loaded!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "Collapsible Banner Ad failed to load: " + loadAdError.getMessage());
                container.removeAllViews();
                TextView errorTv = createErrorTextView(activity, "Collapsible Banner Error: " + formatErrorMessage(loadAdError));
                container.addView(errorTv);
            }
        });

        // Set collapsible extra
        Bundle extras = new Bundle();
        extras.putString("collapsible", isBottom ? "bottom" : "top");
        AdRequest adRequest = new AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                .build();

        adView.loadAd(adRequest);
        return adView;
    }

    // =========================================================================
    // 4. INTERSTITIAL AD
    // =========================================================================
    public static void loadInterstitialAd(@NonNull Context context, @Nullable String adUnitId,
                                          @Nullable OnAdLoadedCallback<InterstitialAd> callback) {
        final String unitId = (adUnitId != null && !adUnitId.isEmpty()) ? adUnitId : SAMPLE_INTERSTITIAL;

        AdRequest request = new AdRequest.Builder().build();
        InterstitialAd.load(context, unitId, request, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                Log.d(TAG, "Interstitial Ad loaded successfully");
                cachedInterstitialAd = interstitialAd;
                if (callback != null) callback.onSuccess(interstitialAd);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "Interstitial Ad failed to load: " + loadAdError.getMessage());
                if (callback != null) callback.onError(formatErrorMessage(loadAdError));
            }
        });
    }

    public static void showInterstitialAd(@NonNull Activity activity, @Nullable Runnable onClosed) {
        if (cachedInterstitialAd != null) {
            cachedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    cachedInterstitialAd = null;
                    if (onClosed != null) onClosed.run();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    cachedInterstitialAd = null;
                    if (onClosed != null) onClosed.run();
                }
            });
            cachedInterstitialAd.show(activity);
        } else {
            AlertDialog loadingDialog = showLoadingDialog(activity, "Loading Interstitial Ad...");
            loadInterstitialAd(activity, SAMPLE_INTERSTITIAL, new OnAdLoadedCallback<InterstitialAd>() {
                @Override
                public void onSuccess(InterstitialAd ad) {
                    safeDismiss(loadingDialog);
                    showInterstitialAd(activity, onClosed);
                }

                @Override
                public void onError(String error) {
                    safeDismiss(loadingDialog);
                    showSimpleAlertDialog(activity, "Interstitial Ad Failed", error);
                    if (onClosed != null) onClosed.run();
                }
            });
        }
    }

    // =========================================================================
    // 5. REWARDED VIDEO AD
    // =========================================================================
    public static void loadRewardedAd(@NonNull Context context, @Nullable String adUnitId,
                                      @Nullable OnAdLoadedCallback<RewardedAd> callback) {
        final String unitId = (adUnitId != null && !adUnitId.isEmpty()) ? adUnitId : SAMPLE_REWARDED;

        AdRequest request = new AdRequest.Builder().build();
        RewardedAd.load(context, unitId, request, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                Log.d(TAG, "Rewarded Ad loaded successfully");
                cachedRewardedAd = rewardedAd;
                if (callback != null) callback.onSuccess(rewardedAd);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "Rewarded Ad failed to load: " + loadAdError.getMessage());
                if (callback != null) callback.onError(formatErrorMessage(loadAdError));
            }
        });
    }

    public static void showRewardedAd(@NonNull Activity activity, @Nullable OnUserEarnedRewardListener rewardListener,
                                       @Nullable Runnable onClosed) {
        if (cachedRewardedAd != null) {
            cachedRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    cachedRewardedAd = null;
                    if (onClosed != null) onClosed.run();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    cachedRewardedAd = null;
                    if (onClosed != null) onClosed.run();
                }
            });
            cachedRewardedAd.show(activity, rewardItem -> {
                Log.d(TAG, "User earned reward: " + rewardItem.getAmount() + " " + rewardItem.getType());
                if (rewardListener != null) {
                    rewardListener.onUserEarnedReward(rewardItem);
                }
            });
        } else {
            AlertDialog loadingDialog = showLoadingDialog(activity, "Loading Rewarded Video Ad...");
            loadRewardedAd(activity, SAMPLE_REWARDED, new OnAdLoadedCallback<RewardedAd>() {
                @Override
                public void onSuccess(RewardedAd ad) {
                    safeDismiss(loadingDialog);
                    showRewardedAd(activity, rewardListener, onClosed);
                }

                @Override
                public void onError(String error) {
                    safeDismiss(loadingDialog);
                    showSimpleAlertDialog(activity, "Rewarded Ad Failed", error);
                    if (onClosed != null) onClosed.run();
                }
            });
        }
    }

    // =========================================================================
    // 6. REWARDED INTERSTITIAL AD
    // =========================================================================
    public static void loadRewardedInterstitialAd(@NonNull Context context, @Nullable String adUnitId,
                                                  @Nullable OnAdLoadedCallback<RewardedInterstitialAd> callback) {
        final String unitId = (adUnitId != null && !adUnitId.isEmpty()) ? adUnitId : SAMPLE_REWARDED_INTERSTITIAL;

        AdRequest request = new AdRequest.Builder().build();
        RewardedInterstitialAd.load(context, unitId, request, new RewardedInterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedInterstitialAd rewardedInterstitialAd) {
                Log.d(TAG, "Rewarded Interstitial Ad loaded");
                cachedRewardedInterstitialAd = rewardedInterstitialAd;
                if (callback != null) callback.onSuccess(rewardedInterstitialAd);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "Rewarded Interstitial Ad failed to load: " + loadAdError.getMessage());
                if (callback != null) callback.onError(formatErrorMessage(loadAdError));
            }
        });
    }

    public static void showRewardedInterstitialAd(@NonNull Activity activity,
                                                   @Nullable OnUserEarnedRewardListener rewardListener,
                                                   @Nullable Runnable onClosed) {
        if (cachedRewardedInterstitialAd != null) {
            cachedRewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    cachedRewardedInterstitialAd = null;
                    if (onClosed != null) onClosed.run();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    cachedRewardedInterstitialAd = null;
                    if (onClosed != null) onClosed.run();
                }
            });
            cachedRewardedInterstitialAd.show(activity, rewardItem -> {
                Log.d(TAG, "User earned reward: " + rewardItem.getAmount() + " " + rewardItem.getType());
                if (rewardListener != null) {
                    rewardListener.onUserEarnedReward(rewardItem);
                }
            });
        } else {
            AlertDialog loadingDialog = showLoadingDialog(activity, "Loading Rewarded Interstitial...");
            loadRewardedInterstitialAd(activity, SAMPLE_REWARDED_INTERSTITIAL, new OnAdLoadedCallback<RewardedInterstitialAd>() {
                @Override
                public void onSuccess(RewardedInterstitialAd ad) {
                    safeDismiss(loadingDialog);
                    showRewardedInterstitialAd(activity, rewardListener, onClosed);
                }

                @Override
                public void onError(String error) {
                    safeDismiss(loadingDialog);
                    showSimpleAlertDialog(activity, "Rewarded Interstitial Failed", error);
                    if (onClosed != null) onClosed.run();
                }
            });
        }
    }

    // =========================================================================
    // 7. NATIVE ADVANCED AD
    // =========================================================================
    public static void loadNativeAd(@NonNull Context context, @NonNull ViewGroup container,
                                    @Nullable String adUnitId, @Nullable Runnable onLoaded) {
        final String unitId = (adUnitId != null && !adUnitId.isEmpty()) ? adUnitId : SAMPLE_NATIVE_ADVANCED;

        container.removeAllViews();
        ProgressBar spinner = new ProgressBar(context);
        container.addView(spinner);

        AdLoader adLoader = new AdLoader.Builder(context, unitId)
                .forNativeAd(nativeAd -> {
                    Log.d(TAG, "Native Ad loaded successfully");
                    container.removeAllViews();
                    populateNativeAdView(context, container, nativeAd);
                    Toast.makeText(context, "✅ Native Ad loaded!", Toast.LENGTH_SHORT).show();
                    if (onLoaded != null) onLoaded.run();
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Native Ad failed to load: " + loadAdError.getMessage());
                        container.removeAllViews();
                        TextView errorTv = createErrorTextView(context, "Native Ad Error: " + formatErrorMessage(loadAdError));
                        container.addView(errorTv);
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder().build())
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private static void populateNativeAdView(Context context, ViewGroup container, NativeAd nativeAd) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admob_sample_native, container, false);
        NativeAdView nativeAdView = (NativeAdView) view;

        // Headline
        TextView headlineView = view.findViewById(R.id.ad_headline);
        headlineView.setText(nativeAd.getHeadline());
        nativeAdView.setHeadlineView(headlineView);

        // Body
        TextView bodyView = view.findViewById(R.id.ad_body);
        if (nativeAd.getBody() != null) {
            bodyView.setText(nativeAd.getBody());
            bodyView.setVisibility(View.VISIBLE);
        } else {
            bodyView.setVisibility(View.GONE);
        }
        nativeAdView.setBodyView(bodyView);

        // Call to action
        Button ctaView = view.findViewById(R.id.ad_call_to_action);
        if (nativeAd.getCallToAction() != null) {
            ctaView.setText(nativeAd.getCallToAction());
            ctaView.setVisibility(View.VISIBLE);
        } else {
            ctaView.setVisibility(View.GONE);
        }
        nativeAdView.setCallToActionView(ctaView);

        // Icon
        ImageView iconView = view.findViewById(R.id.ad_app_icon);
        if (nativeAd.getIcon() != null) {
            iconView.setImageDrawable(nativeAd.getIcon().getDrawable());
            iconView.setVisibility(View.VISIBLE);
        } else {
            iconView.setVisibility(View.GONE);
        }
        nativeAdView.setIconView(iconView);

        // Advertiser
        TextView advertiserView = view.findViewById(R.id.ad_advertiser);
        if (nativeAd.getAdvertiser() != null) {
            advertiserView.setText(nativeAd.getAdvertiser());
            advertiserView.setVisibility(View.VISIBLE);
        } else {
            advertiserView.setVisibility(View.GONE);
        }
        nativeAdView.setAdvertiserView(advertiserView);

        // MediaView
        MediaView mediaView = view.findViewById(R.id.ad_media);
        nativeAdView.setMediaView(mediaView);

        // Register native ad
        nativeAdView.setNativeAd(nativeAd);
        container.addView(nativeAdView);
    }

    // =========================================================================
    // 8. GOOGLE AD INSPECTOR
    // =========================================================================
    public static void openAdInspector(@NonNull Activity activity) {
        try {
            java.util.List<String> list = new java.util.ArrayList<>();
            list.add(AdRequest.DEVICE_ID_EMULATOR);
            list.add("61143FA4F558E5F35D032B30280D1FC4");
            list.add("2E3547E7DE80ADD99D8B4C5C8512F162");
            list.add("e4a7eea7-65b9-49e7-adb1-26b686602f44");
            list.add("E4A7EEA7-65B9-49E7-ADB1-26B686602F44");
            String androidIdMd5 = Tools.getAdMobTestDeviceId(activity);
            if (!androidIdMd5.isEmpty() && !list.contains(androidIdMd5)) list.add(androidIdMd5);

            android.content.SharedPreferences sp = activity.getSharedPreferences("admob_test_devices", Context.MODE_PRIVATE);
            String customId = sp.getString("custom_test_device_id", "");
            if (customId != null && !customId.isEmpty() && !list.contains(customId)) {
                list.add(customId);
            }
            MobileAds.setRequestConfiguration(new RequestConfiguration.Builder().setTestDeviceIds(list).build());
        } catch (Exception ignored) {}

        // Pre-warm a test request so GMA SDK session is established with AdMob
        try {
            AdView preWarmView = new AdView(activity);
            preWarmView.setAdUnitId(SAMPLE_BANNER);
            preWarmView.setAdSize(AdSize.BANNER);
            preWarmView.loadAd(new AdRequest.Builder().build());
        } catch (Exception ignored) {}

        MobileAds.openAdInspector(activity, error -> {
            if (error != null) {
                Log.e(TAG, "Ad Inspector error: " + error.getMessage() + " (Code: " + error.getCode() + ")");
                new AlertDialog.Builder(activity)
                        .setTitle("Ad Inspector (Code " + error.getCode() + ")")
                        .setMessage(error.getMessage() + "\n\n" +
                                "⚠️ Why Ad Inspector opened black or failed:\n" +
                                "AdMob requires your phone to be registered as a Test Device.\n\n" +
                                "Quick Fix:\n" +
                                "1. Copy your Test Device ID from the demo dialog.\n" +
                                "2. Open AdMob Console -> Settings -> Test devices.\n" +
                                "3. Click 'Add test device', select Android, paste the ID, and Save.\n" +
                                "4. Re-open Ad Inspector.")
                        .setPositiveButton("OK", null)
                        .show();
            } else {
                Log.d(TAG, "Ad Inspector closed successfully");
            }
        });
    }

    // =========================================================================
    // REAL AD UNITS TESTING METHODS (MALAYALAM MOVIE LIST)
    // =========================================================================
    public static void testRealBannerAd(@NonNull Activity activity, @NonNull ViewGroup container) {
        container.removeAllViews();
        ProgressBar pb = new ProgressBar(activity);
        container.addView(pb);

        AdView adView = new AdView(activity);
        adView.setAdUnitId(REAL_BANNER);
        adView.setAdSize(getAdaptiveAdSize(activity));
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                container.removeAllViews();
                container.addView(adView);
                Toast.makeText(activity, "🎉 REAL BANNER LOADED SUCCESSFULLY!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                container.removeAllViews();
                showRealAdErrorDialog(activity, "Real Banner (" + REAL_BANNER + ")", loadAdError);
            }
        });
        adView.loadAd(new AdRequest.Builder().build());
    }

    public static void testRealInterstitialAd(@NonNull Activity activity) {
        AlertDialog loading = showLoadingDialog(activity, "Requesting Real Interstitial Ad...");
        AdRequest request = new AdRequest.Builder().build();
        InterstitialAd.load(activity, REAL_INTERSTITIAL, request, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                safeDismiss(loading);
                Toast.makeText(activity, "🎉 REAL INTERSTITIAL LOADED!", Toast.LENGTH_SHORT).show();
                interstitialAd.show(activity);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                safeDismiss(loading);
                showRealAdErrorDialog(activity, "Real Interstitial (" + REAL_INTERSTITIAL + ")", loadAdError);
            }
        });
    }

    public static void testRealNativeAd(@NonNull Activity activity, @NonNull ViewGroup container) {
        container.removeAllViews();
        ProgressBar pb = new ProgressBar(activity);
        container.addView(pb);

        AdLoader adLoader = new AdLoader.Builder(activity, REAL_NATIVE)
                .forNativeAd(nativeAd -> {
                    container.removeAllViews();
                    populateNativeAdView(activity, container, nativeAd);
                    Toast.makeText(activity, "🎉 REAL NATIVE AD LOADED!", Toast.LENGTH_LONG).show();
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        container.removeAllViews();
                        showRealAdErrorDialog(activity, "Real Native Ad (" + REAL_NATIVE + ")", loadAdError);
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder().build())
                .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public static void testRealAppOpenAd(@NonNull Activity activity) {
        loadAndShowAppOpenAd(activity, REAL_APP_OPEN);
    }

    public static void showRealAdErrorDialog(Activity activity, String title, LoadAdError error) {
        if (error == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("Ad Unit Status: Recognized by AdMob\n");
        sb.append("Response Code: ").append(error.getCode()).append("\n");
        sb.append("Message: ").append(error.getMessage()).append("\n\n");

        if (error.getCode() == AdRequest.ERROR_CODE_NO_FILL) {
            sb.append("📋 What Error 3 (NO_FILL) means:\n");
            sb.append("1. Your Ad Unit ID is valid and active on AdMob.\n");
            sb.append("2. Google's invalid traffic algorithm blocks live commercial ads on developer devices so your account does not get banned for self-clicks.\n");
            sb.append("3. Newly created ad units also undergo a 24-48h warm-up period before global ad inventory is filled.\n\n");
            sb.append("✅ To test ads on this device now:\n");
            sb.append("Copy your Test Device ID from this dialog and add it under AdMob Console ➔ Settings ➔ Test devices.\n\n");
            sb.append("📱 Production Users:\n");
            sb.append("Real users downloading your app from Google Play Store will receive live commercial ads automatically.");
        } else {
            sb.append(formatErrorMessage(error));
        }

        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    // =========================================================================
    // HELPER: ADAPTIVE BANNER SIZE CALCULATION
    // =========================================================================
    public static AdSize getAdaptiveAdSize(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = outMetrics.density;
        float adWidthPixels = outMetrics.widthPixels;

        int adWidth = (int) (adWidthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }

    // =========================================================================
    // INTERACTIVE DEMO DIALOG (TEST ALL AD TYPES LIVE ON DEVICE)
    // =========================================================================
    public static void showSampleAdsDemoDialog(@NonNull Activity activity) {
        // Ensure MobileAds initialized immediately
        initialize(activity.getApplicationContext(), null);

        ScrollView scrollView = new ScrollView(activity);
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 36, 36, 36);
        scrollView.addView(layout);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("🎯 AdMob Diagnostics & Test Studio");
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(activity.getResources().getColor(android.R.color.white));
        layout.addView(tvTitle);

        TextView tvSubApp = new TextView(activity);
        tvSubApp.setText("USA Newspapers & News • Status: Ready in AdMob\nApp ID: " + REAL_APP_ID);
        tvSubApp.setTextSize(11f);
        tvSubApp.setTextColor(Color.parseColor("#00C896"));
        tvSubApp.setPadding(0, 6, 0, 16);
        layout.addView(tvSubApp);

        // --- TEST DEVICE REGISTRATION CARD ---
        LinearLayout devCard = new LinearLayout(activity);
        devCard.setOrientation(LinearLayout.VERTICAL);
        devCard.setPadding(24, 20, 24, 20);
        devCard.setBackgroundColor(Color.parseColor("#131B2B"));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, 16);
        devCard.setLayoutParams(cardLp);

        TextView tvCardTitle = new TextView(activity);
        tvCardTitle.setText("📱 Your Device AdMob Test ID");
        tvCardTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvCardTitle.setTextColor(Color.parseColor("#FFB800"));
        tvCardTitle.setTextSize(13f);
        devCard.addView(tvCardTitle);

        final String deviceId = Tools.getAdMobTestDeviceId(activity);
        final TextView tvDeviceId = new TextView(activity);
        tvDeviceId.setText(deviceId.isEmpty() ? "Generating..." : deviceId);
        tvDeviceId.setTextSize(13f);
        tvDeviceId.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        tvDeviceId.setTextColor(Color.WHITE);
        tvDeviceId.setPadding(0, 6, 0, 10);
        devCard.addView(tvDeviceId);

        // Button: Copy Test Device ID
        Button btnCopy = new Button(activity);
        btnCopy.setText("📋 Copy Test Device ID");
        btnCopy.setTextSize(12f);
        btnCopy.setAllCaps(false);
        btnCopy.setBackgroundColor(Color.parseColor("#2563EB"));
        btnCopy.setTextColor(Color.WHITE);
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("AdMob Test Device ID", tvDeviceId.getText().toString());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(activity, "📋 Copied Test Device ID!\nPaste in AdMob Console -> Settings -> Test devices", Toast.LENGTH_LONG).show();
            }
        });
        devCard.addView(btnCopy);

        // Button: Open Google Ad Inspector
        Button btnInspector = new Button(activity);
        btnInspector.setText("🔍 Open Google Ad Inspector");
        btnInspector.setTextSize(12f);
        btnInspector.setAllCaps(false);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, 10, 0, 0);
        btnInspector.setLayoutParams(btnLp);
        btnInspector.setBackgroundColor(Color.parseColor("#059669"));
        btnInspector.setTextColor(Color.WHITE);
        btnInspector.setOnClickListener(v -> openAdInspector(activity));
        devCard.addView(btnInspector);

        TextView tvHelp = new TextView(activity);
        tvHelp.setText("ℹ️ Why Ad Inspector opened black:\nAdMob requires your phone in Test Devices. Copy your ID above, open AdMob Console ➔ Settings ➔ Test devices ➔ 'Add test device' to enable live inspector.");
        tvHelp.setTextSize(11f);
        tvHelp.setTextColor(Color.parseColor("#94A3B8"));
        tvHelp.setPadding(0, 8, 0, 0);
        devCard.addView(tvHelp);

        layout.addView(devCard);

        // Banner and Native Ad preview containers
        LinearLayout bannerContainer = new LinearLayout(activity);
        bannerContainer.setOrientation(LinearLayout.VERTICAL);
        bannerContainer.setGravity(Gravity.CENTER);
        bannerContainer.setPadding(0, 10, 0, 10);

        LinearLayout nativeContainer = new LinearLayout(activity);
        nativeContainer.setOrientation(LinearLayout.VERTICAL);
        nativeContainer.setGravity(Gravity.CENTER);
        nativeContainer.setPadding(0, 10, 0, 10);

        // --- SECTION 1: YOUR REAL LIVE AD UNITS ---
        TextView tvRealHeader = new TextView(activity);
        tvRealHeader.setText("🎯 Section 1: Test Your Real Ad Units");
        tvRealHeader.setTextSize(14f);
        tvRealHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        tvRealHeader.setTextColor(Color.parseColor("#38BDF8"));
        tvRealHeader.setPadding(0, 8, 0, 4);
        layout.addView(tvRealHeader);

        TextView tvRealNotice = new TextView(activity);
        tvRealNotice.setText("Tap each format below to test your app's actual ad units. Error 3 (No Fill) indicates AdMob anti-fraud protection on developer devices.");
        tvRealNotice.setTextSize(11f);
        tvRealNotice.setTextColor(Color.parseColor("#94A3B8"));
        tvRealNotice.setPadding(0, 0, 0, 10);
        layout.addView(tvRealNotice);

        layout.addView(createDemoButton(activity, "🎯 Real App Open (1588844143)", v -> testRealAppOpenAd(activity)));
        layout.addView(createDemoButton(activity, "🎯 Real Adaptive Banner (2250941706)", v -> testRealBannerAd(activity, bannerContainer)));
        layout.addView(createDemoButton(activity, "🎯 Real Interstitial Ad (7583406851)", v -> testRealInterstitialAd(activity)));
        layout.addView(createDemoButton(activity, "🎯 Real Native Ad (8433206679)", v -> testRealNativeAd(activity, nativeContainer)));

        // --- SECTION 2: GOOGLE SAMPLE ADS ---
        TextView tvSampleHeader = new TextView(activity);
        tvSampleHeader.setText("🧪 Section 2: Google Sample Ads (Always Works)");
        tvSampleHeader.setTextSize(14f);
        tvSampleHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        tvSampleHeader.setTextColor(Color.parseColor("#A78BFA"));
        tvSampleHeader.setPadding(0, 16, 0, 4);
        layout.addView(tvSampleHeader);

        layout.addView(createDemoButton(activity, "📱 Sample App Open Ad", v -> loadAndShowAppOpenAd(activity, SAMPLE_APP_OPEN)));
        layout.addView(createDemoButton(activity, "🏷️ Sample Adaptive Banner", v -> loadAdaptiveBannerAd(activity, bannerContainer, SAMPLE_BANNER, null, null)));
        layout.addView(createDemoButton(activity, "🔽 Sample Collapsible Banner", v -> loadCollapsibleBannerAd(activity, bannerContainer, SAMPLE_COLLAPSIBLE_BANNER, true)));
        layout.addView(createDemoButton(activity, "📺 Sample Interstitial Ad", v -> showInterstitialAd(activity, () -> Toast.makeText(activity, "Sample Interstitial closed", Toast.LENGTH_SHORT).show())));
        layout.addView(createDemoButton(activity, "🎁 Sample Rewarded Video Ad", v -> showRewardedAd(activity, rewardItem -> Toast.makeText(activity, "Reward: " + rewardItem.getAmount() + " " + rewardItem.getType(), Toast.LENGTH_LONG).show(), null)));
        layout.addView(createDemoButton(activity, "🏆 Sample Rewarded Interstitial", v -> showRewardedInterstitialAd(activity, rewardItem -> Toast.makeText(activity, "Rewarded Interstitial closed", Toast.LENGTH_SHORT).show(), null)));
        layout.addView(createDemoButton(activity, "🖼️ Sample Native Advanced Ad", v -> loadNativeAd(activity, nativeContainer, SAMPLE_NATIVE_ADVANCED, null)));

        // Containers for live banner and native ads inside dialog
        layout.addView(bannerContainer);
        layout.addView(nativeContainer);

        new AlertDialog.Builder(activity)
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
    }

    private static Button createDemoButton(Context context, String label, View.OnClickListener onClickListener) {
        Button btn = new Button(context);
        btn.setText(label);
        btn.setTextSize(13f);
        btn.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 8, 0, 8);
        btn.setLayoutParams(lp);
        btn.setBackgroundResource(R.drawable.bg_btn_accent);
        btn.setTextColor(context.getResources().getColor(android.R.color.white));
        btn.setOnClickListener(onClickListener);
        return btn;
    }

    private static TextView createErrorTextView(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#FF4D4F"));
        tv.setTextSize(12f);
        tv.setPadding(16, 16, 16, 16);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    private static AlertDialog showLoadingDialog(Activity activity, String message) {
        ProgressBar progressBar = new ProgressBar(activity);
        progressBar.setPadding(40, 40, 40, 40);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(message)
                .setView(progressBar)
                .setCancelable(true)
                .create();
        dialog.show();
        return dialog;
    }

    private static void safeDismiss(AlertDialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            try {
                dialog.dismiss();
            } catch (Exception ignored) {}
        }
    }

    private static void showSimpleAlertDialog(Activity activity, String title, String message) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private static void showAdErrorDialog(Activity activity, String adType, LoadAdError error) {
        String detail = formatErrorMessage(error);
        new AlertDialog.Builder(activity)
                .setTitle(adType + " Failed to Load")
                .setMessage(detail)
                .setPositiveButton("OK", null)
                .show();
    }

    public static String formatErrorMessage(LoadAdError error) {
        if (error == null) return "Unknown Error";
        String reason = "";
        switch (error.getCode()) {
            case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                reason = "INTERNAL_ERROR (Code 0): Google AdMob service error.";
                break;
            case AdRequest.ERROR_CODE_INVALID_REQUEST:
                reason = "INVALID_REQUEST (Code 1): Invalid ad unit ID or request parameter.";
                break;
            case AdRequest.ERROR_CODE_NETWORK_ERROR:
                reason = "NETWORK_ERROR (Code 2): Check device internet connection.";
                break;
            case AdRequest.ERROR_CODE_NO_FILL:
                reason = "NO_FILL (Code 3): No ad inventory available from Google AdMob.";
                break;
            default:
                reason = "Code: " + error.getCode();
                break;
        }
        return reason + "\nMessage: " + error.getMessage();
    }

    public interface OnAdLoadedCallback<T> {
        void onSuccess(T ad);
        void onError(String error);
    }
}
