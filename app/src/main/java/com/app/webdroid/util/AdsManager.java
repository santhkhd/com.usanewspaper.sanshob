package com.app.webdroid.util;

import static com.solodroidx.ads.util.Constant.IRONSOURCE;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnAdInspectorClosedListener;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.webdroid.Config;
import com.shobmc.san.R;
import com.app.webdroid.database.prefs.AdsPref;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.model.Ads;
import com.app.webdroid.model.App;
import com.app.webdroid.model.Placement;
import com.solodroidx.ads.appopen.AppOpenAd;
import com.solodroidx.ads.banner.BannerAd;
import com.solodroidx.ads.gdpr.GDPR;
import com.solodroidx.ads.initialization.InitializeAd;
import com.solodroidx.ads.interstitial.InterstitialAd;
import com.solodroidx.ads.listener.OnShowAdCompleteListener;
import com.solodroidx.ads.nativead.NativeAd;
import com.solodroidx.ads.nativead.NativeAdView;
import com.solodroidx.ads.nativead.NativeAdViewHolder;

public class AdsManager {

    Activity activity;
    InitializeAd initializeAd;
    AppOpenAd appOpenAd;
    BannerAd bannerAd;
    InterstitialAd interstitialAd;
    NativeAd nativeAd;
    NativeAdView nativeAdView;
    SharedPref sharedPref;
    AdsPref adsPref;
    GDPR gdpr;

    public AdsManager(Activity activity) {
        this.activity = activity;
        this.sharedPref = new SharedPref(activity);
        this.adsPref = new AdsPref(activity);
        this.gdpr = new GDPR(activity);
        initializeAd = new InitializeAd(activity);
        appOpenAd = new AppOpenAd(activity);
        bannerAd = new BannerAd(activity);
        interstitialAd = new InterstitialAd(activity);
        nativeAd = new NativeAd(activity);
        nativeAdView = new NativeAdView(activity);
    }

    public void initializeAd() {
        if (adsPref != null && adsPref.getAdStatus()) {
            try {
                initializeAd.setAdStatus("1")
                        .setAdNetwork(adsPref.getMainAds())
                        .setBackupAdNetwork(adsPref.getBackupAds())
                        .setAppLovinSdkKey(activity.getResources().getString(R.string.applovin_sdk_key))
                        .setStartappAppId(adsPref.getStartappAppId())
                        .setUnityGameId(adsPref.getUnityGameId())
                        .setIronSourceAppKey(adsPref.getIronSourceAppKey())
                        .setWortiseAppId(adsPref.getWortiseAppId())
                        .setApplicationId(Tools.getApplicationId())
                        .setDebug(Tools.isDebug())
                        .build();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void loadAppOpenAd(boolean placement, boolean withListener,
            OnShowAdCompleteListener onShowAdCompleteListener) {
        if (placement) {
            if (adsPref != null && adsPref.getAdStatus()) {
                try {
                    appOpenAd.setAdStatus("1")
                            .setAdNetwork(adsPref.getMainAds())
                            .setBackupAdNetwork(adsPref.getBackupAds())
                            .setAdMobAppOpenId(adsPref.getAdMobAppOpenAdId())
                            .setAdManagerAppOpenId(adsPref.getAdManagerAppOpenAdId())
                            .setApplovinAppOpenId(adsPref.getAppLovinAppOpenAdUnitId())
                            .setWortiseAppOpenId(adsPref.getWortiseAppOpenAdUnitId())
                            .setWithListener(withListener)
                            .build(onShowAdCompleteListener);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (onShowAdCompleteListener != null) onShowAdCompleteListener.onShowAdComplete();
                }
            } else {
                if (onShowAdCompleteListener != null) onShowAdCompleteListener.onShowAdComplete();
            }
        } else {
            if (onShowAdCompleteListener != null) onShowAdCompleteListener.onShowAdComplete();
        }
    }

    public void showAppOpenAd(OnShowAdCompleteListener onShowAdCompleteListener) {
        appOpenAd.show(onShowAdCompleteListener);
    }

    public void destroyAppOpenAd() {
        appOpenAd.destroyOpenAd();
    }

    public void loadBannerAd(boolean placement) {
        if (placement) {
            if (adsPref.getAdStatus()) {
                android.util.Log.d("AdMob", "Loading high-yield collapsible banner for: " + adsPref.getAdMobBannerId());
                bannerAd.setAdStatus("1")
                        .setAdNetwork(adsPref.getMainAds())
                        .setBackupAdNetwork(adsPref.getBackupAds())
                        .setAdMobBannerId(adsPref.getAdMobBannerId())
                        .setGoogleAdManagerBannerId(adsPref.getAdManagerBannerId())
                        .setFanBannerId(adsPref.getFanBannerUnitId())
                        .setUnityBannerId(adsPref.getUnityBannerPlacementId())
                        .setAppLovinBannerId(adsPref.getAppLovinBannerAdUnitId())
                        .setAppLovinBannerZoneId(adsPref.getAppLovinBannerZoneId())
                        .setIronSourceBannerId(adsPref.getIronSourceBannerId())
                        .setWortiseBannerId(adsPref.getWortiseBannerAdUnitId())
                        .setIsCollapsibleBanner(true)
                        .setDarkTheme(sharedPref.getIsDarkTheme())
                        .build();
            } else {
                android.util.Log.w("AdMob", "Banner ad skipped: adsPref.getAdStatus() is false");
            }
        }
    }

    public void destroyBannerAd() {
        if (adsPref.getAdStatus()) {
            bannerAd.destroyAndDetachBanner();
        }
    }

    public void resumeBannerAd(boolean placement) {
        if (adsPref.getAdStatus() && !adsPref.getIronSourceBannerId().equals("0")) {
            if (adsPref.getMainAds().equals(IRONSOURCE) || adsPref.getBackupAds().equals(IRONSOURCE)) {
                loadBannerAd(placement);
            }
        }
    }

    public void loadInterstitialAd() {
        if (adsPref.getAdStatus()) {
            interstitialAd.setAdStatus("1")
                    .setAdNetwork(adsPref.getMainAds())
                    .setBackupAdNetwork(adsPref.getBackupAds())
                    .setAdMobInterstitialId(adsPref.getAdMobInterstitialId())
                    .setGoogleAdManagerInterstitialId(adsPref.getAdManagerInterstitialId())
                    .setFanInterstitialId(adsPref.getFanInterstitialUnitId())
                    .setUnityInterstitialId(adsPref.getUnityInterstitialPlacementId())
                    .setAppLovinInterstitialId(adsPref.getAppLovinInterstitialAdUnitId())
                    .setAppLovinInterstitialZoneId(adsPref.getAppLovinInterstitialZoneId())
                    .setIronSourceInterstitialId(adsPref.getIronSourceInterstitialId())
                    .setWortiseInterstitialId(adsPref.getWortiseInterstitialAdUnitId())
                    .setInterval(1)
                    .build();
        }
    }

    public void showInterstitialAd(boolean placement) {
        if (adsPref.getAdStatus()) {
            if (placement) {
                interstitialAd.show();
            }
        }
    }

    public void showInterstitialAd(boolean placement, OnShowAdCompleteListener onShowAdCompleteListener) {
        if (adsPref.getAdStatus()) {
            if (placement) {
                interstitialAd.show();
                onShowAdCompleteListener.onShowAdComplete();
            } else {
                onShowAdCompleteListener.onShowAdComplete();
            }
        } else {
            onShowAdCompleteListener.onShowAdComplete();
        }
    }

    public void loadNativeAd(boolean placement, String style) {
        if (placement) {
            if (adsPref.getAdStatus()) {
                nativeAd.setAdStatus("1")
                        .setAdNetwork(adsPref.getMainAds())
                        .setBackupAdNetwork(adsPref.getBackupAds())
                        .setAdMobNativeId(adsPref.getAdMobNativeId())
                        .setAdManagerNativeId(adsPref.getAdManagerNativeId())
                        .setFanNativeId(adsPref.getFanNativeUnitId())
                        .setAppLovinNativeId(adsPref.getAppLovinNativeAdManualUnitId())
                        .setAppLovinDiscoveryMrecZoneId(adsPref.getAppLovinBannerMrecZoneId())
                        .setWortiseNativeId(adsPref.getWortiseNativeAdUnitId())
                        .setNativeAdStyle(style)
                        .setRadius(R.dimen.corner_radius)
                        .setStrokeWidth(R.dimen.native_ad_stroke_width)
                        .setStrokeColor(R.color.color_stroke_native_ad)
                        .setBackgroundColor(R.color.color_light_native_ad_background,
                                R.color.color_dark_native_ad_background)
                        .setMargin(R.dimen.no_margin, R.dimen.no_margin, R.dimen.no_margin, R.dimen.no_margin)
                        .setDarkTheme(sharedPref.getIsDarkTheme())
                        .build();
            }
        }
    }

    public void loadNativeAdView(View view, boolean placement, String nativeAdStyle) {
        if (placement) {
            if (adsPref.getAdStatus()) {
                nativeAdView.setView(view)
                        .setAdStatus("1")
                        .setAdNetwork(adsPref.getMainAds())
                        .setBackupAdNetwork(adsPref.getBackupAds())
                        .setAdMobNativeId(adsPref.getAdMobNativeId())
                        .setAdManagerNativeId(adsPref.getAdManagerNativeId())
                        .setFanNativeId(adsPref.getFanNativeUnitId())
                        .setAppLovinNativeId(adsPref.getAppLovinNativeAdManualUnitId())
                        .setAppLovinDiscoveryMrecZoneId(adsPref.getAppLovinBannerMrecZoneId())
                        .setWortiseNativeId(adsPref.getWortiseNativeAdUnitId())
                        .setNativeAdStyle(Tools.nativeAdStyleFormatter(nativeAdStyle))
                        .setDarkTheme(sharedPref.getIsDarkTheme());
                view.post(() -> nativeAdView.build());
            }
        }
    }

    public RecyclerView.ViewHolder createNativeAdViewHolder(Context context, @NonNull ViewGroup parent) {
        return createNativeAdViewHolder(context, parent, adsPref.getNativeAdStyleDrawerMenu());
    }

    public RecyclerView.ViewHolder createNativeAdViewHolder(Context context, @NonNull ViewGroup parent, String style) {
        String adStatus;
        if (adsPref.getAdStatus()) {
            adStatus = "1";
        } else {
            adStatus = "0";
        }

        int noMargin = R.dimen.no_margin;
        int marginEnd = R.dimen.spacing_medium;

        return new NativeAdViewHolder(NativeAdViewHolder.setLayoutInflater(parent,
                Tools.nativeAdStyleFormatter(style)))
                .setAdStatus(adStatus)
                .setAdNetwork(adsPref.getMainAds())
                .setBackupAdNetwork(adsPref.getBackupAds())
                .setAdMobNativeId(adsPref.getAdMobNativeId())
                .setAdManagerNativeId(adsPref.getAdManagerNativeId())
                .setFanNativeId(adsPref.getFanNativeUnitId())
                .setAppLovinNativeId(adsPref.getAppLovinNativeAdManualUnitId())
                .setAppLovinDiscoveryMrecZoneId(adsPref.getAppLovinBannerMrecZoneId())
                .setWortiseNativeId(adsPref.getWortiseNativeAdUnitId())
                .setNativeAdStyle(Tools.nativeAdStyleFormatter(style))
                .setBackgroundColor(R.color.color_light_native_ad_background, R.color.color_dark_native_ad_background)
                .setRadius(context, R.dimen.no_corner_radius)
                .setStrokeWidth(context, R.dimen.native_ad_stroke_width)
                .setStrokeColor(context, R.color.color_stroke_native_ad)
                .setMargin(context, noMargin, noMargin, noMargin, marginEnd)
                // .setPadding(context, noPadding, padding, noPadding, padding)
                .setDarkTheme(sharedPref.getIsDarkTheme());
    }

    public void bindNativeAdViewHolder(Context context, NativeAdViewHolder holder) {
        holder.buildNativeAd(context);
    }

    public void updateConsentStatus() {
        if (Config.ENABLE_GDPR_UMP_SDK) {
            gdpr.updateGDPRConsentStatus(adsPref.getMainAds(), false, false);
        }
    }

    public void showAdInspector() {
        MobileAds.openAdInspector(activity, error -> {
            // Error will be null if the ad inspector closed successfully.
        });
    }

    public void saveAds(AdsPref adsPref, Ads ads) {
        adsPref.saveAds(
                ads.ad_status,
                ads.main_ads,
                ads.backup_ads,
                ads.admob_banner_unit_id,
                ads.admob_interstitial_unit_id,
                ads.admob_native_unit_id,
                ads.admob_app_open_ad_unit_id,
                ads.ad_manager_banner_unit_id,
                ads.ad_manager_interstitial_unit_id,
                ads.ad_manager_native_unit_id,
                ads.ad_manager_app_open_ad_unit_id,
                ads.fan_banner_unit_id,
                ads.fan_interstitial_unit_id,
                ads.fan_native_unit_id,
                ads.startapp_app_id,
                ads.unity_game_id,
                ads.unity_banner_placement_id,
                ads.unity_interstitial_placement_id,
                ads.applovin_banner_ad_unit_id,
                ads.applovin_interstitial_ad_unit_id,
                ads.applovin_native_ad_manual_unit_id,
                ads.applovin_app_open_ad_unit_id,
                ads.applovin_banner_zone_id,
                ads.applovin_banner_mrec_zone_id,
                ads.applovin_interstitial_zone_id,
                ads.ironsource_app_key,
                ads.ironsource_banner_placement_name,
                ads.ironsource_interstitial_placement_name,
                ads.interstitial_ad_interval_on_drawer_menu,
                ads.interstitial_ad_interval_on_web_page_link,
                ads.native_ad_style_drawer_menu,
                ads.native_ad_style_exit_dialog,
                ads.native_ad_style_product_list,
                ads.native_ad_index,
                ads.interstitial_ad_on_list_item_click,
                ads.interstitial_ad_interval_on_list_item_click);
    }

    public void saveAdsPlacement(AdsPref adsPref, Placement placement) {
        adsPref.setAdPlacements(
                placement.banner_home,
                placement.interstitial_drawer_menu,
                placement.interstitial_web_page_link,
                placement.native_drawer_menu,
                placement.native_exit_dialog,
                placement.app_open_ad_on_start,
                placement.app_open_ad_on_resume);
    }

    public void saveConfig(SharedPref sharedPref, App app) {
        sharedPref.saveConfig(
                app.toolbar,
                app.navigation_drawer,
                app.geolocation,
                app.cache,
                app.open_link_in_external_browser,
                app.zoom_controls,
                app.user_agent,
                app.privacy_policy_url,
                app.more_apps_url,
                app.redirect_url,
                app.webview_disclaimer_active);
    }

    // =========================================================================
    // SAMPLE & REWARDED ADMOB AD HELPERS

    public void loadAndShowRewardedAd(com.google.android.gms.ads.OnUserEarnedRewardListener rewardListener, Runnable onClosed) {
        if (activity != null) {
            AdMobSampleManager.showRewardedAd(activity, rewardListener, onClosed);
        }
    }

    public void loadAndShowRewardedInterstitialAd(com.google.android.gms.ads.OnUserEarnedRewardListener rewardListener, Runnable onClosed) {
        if (activity != null) {
            AdMobSampleManager.showRewardedInterstitialAd(activity, rewardListener, onClosed);
        }
    }

}
