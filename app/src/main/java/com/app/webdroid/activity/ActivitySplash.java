package com.app.webdroid.activity;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.app.webdroid.Config;
import com.shobmc.san.R;
import com.app.webdroid.callback.CallbackConfig;
import com.app.webdroid.database.prefs.AdsPref;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.database.sqlite.DbNavigation;
import com.app.webdroid.model.Ads;
import com.app.webdroid.model.App;
import com.app.webdroid.model.Navigation;
import com.app.webdroid.model.Slider;
import com.app.webdroid.rest.RestAdapter;
import com.app.webdroid.util.AdsManager;
import com.app.webdroid.util.Tools;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.solodroidx.ads.appopen.AppOpenAd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ActivitySplash extends AppCompatActivity {

    public static final String TAG = "SplashActivity";
    Call<CallbackConfig> callbackConfigCall = null;

    // ... (keep existing fields)

    @SuppressWarnings("ConstantConditions")
    private void requestConfig() {
        try {
            if (Config.JSON_CONFIG_URL != null && !Config.JSON_CONFIG_URL.isEmpty()) {
                requestAPI(Config.JSON_CONFIG_URL);
                return;
            }

            if (Config.ACCESS_KEY != null && Config.ACCESS_KEY.contains("XXXXX")) {
                loadConfigFromAssets();
            } else if (Config.ACCESS_KEY != null && !Config.ACCESS_KEY.isEmpty()) {
                String data = com.solodroidx.ads.util.Tools.decode(Config.ACCESS_KEY);
                if (data != null && data.contains("_applicationId_")) {
                    String[] results = data.split("_applicationId_");
                    String remoteUrl = results[0];
                    String applicationId = results.length > 1 ? results[1] : "";

                    if (applicationId.equals(Tools.getApplicationId())) {
                        requestAPI(remoteUrl);
                    } else if (results.length == 1 && (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://"))) {
                        requestAPI(remoteUrl);
                    } else {
                        loadConfigFromAssets();
                    }
                } else {
                    loadConfigFromAssets();
                }
                Log.d(TAG, "Start request config");
            } else {
                loadConfigFromAssets();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in requestConfig: " + e.getMessage(), e);
            loadConfigFromAssets();
        }
    }

    private void requestAPI(String remoteUrl) {
        if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
            if (remoteUrl.contains("https://drive.google.com")) {
                String driveUrl = remoteUrl.replace("https://", "").replace("http://", "");
                List<String> data = Arrays.asList(driveUrl.split("/"));
                String googleDriveFileId = data.get(3);
                callbackConfigCall = RestAdapter.createApi().getDriveJsonFileId(googleDriveFileId);
            } else {
                callbackConfigCall = RestAdapter.createApi().getJsonUrl(remoteUrl);
            }
        } else {
            callbackConfigCall = RestAdapter.createApi().getDriveJsonFileId(remoteUrl);
        }
        callbackConfigCall.enqueue(new Callback<>() {
            public void onResponse(@NonNull Call<CallbackConfig> call, @NonNull Response<CallbackConfig> response) {
                CallbackConfig resp = response.body();
                if (resp != null) {
                    displayApiResults(resp);
                } else {
                    onRemoteConfigFailed(remoteUrl);
                }
            }

            public void onFailure(@NonNull Call<CallbackConfig> call, @NonNull Throwable th) {
                Log.e(TAG, "initialize failed: " + th.getMessage());
                onRemoteConfigFailed(remoteUrl);
            }
        });
    }

    private void onRemoteConfigFailed(String failedUrl) {
        if (failedUrl != null && failedUrl.equals(Config.JSON_CONFIG_URL) && Config.JSON_CONFIG_RAW_URL != null && !failedUrl.equals(Config.JSON_CONFIG_RAW_URL)) {
            Log.w(TAG, "CDN config failed, attempting fallback to GitHub raw: " + Config.JSON_CONFIG_RAW_URL);
            requestAPI(Config.JSON_CONFIG_RAW_URL);
        } else {
            loadConfigFromAssets();
        }
    }

    private void loadConfigFromAssets() {
        try {
            Log.d(TAG, "Attempting to load config.json from assets...");
            InputStream is = getAssets().open("config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            Gson gson = new Gson();
            CallbackConfig resp = gson.fromJson(json, CallbackConfig.class);
            if (resp != null) {
                if (resp.menus != null) {
                    Log.d(TAG, "Config loaded. Menu count: " + resp.menus.size());
                } else {
                    Log.d(TAG, "Config loaded but menus is null");
                }
                displayApiResults(resp);
                Log.d(TAG, "Loaded config from assets and called displayApiResults");
            } else {
                Log.e(TAG, "Failed to parse config.json (resp is null)");
                showAppOpenAdIfAvailable();
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException loading config from assets", e);
            e.printStackTrace();
            showAppOpenAdIfAvailable();
        } catch (Exception e) {
            Log.e(TAG, "Exception loading config from assets", e);
            e.printStackTrace();
            showAppOpenAdIfAvailable();
        }
    }

    ProgressBar progressBar;
    AdsManager adsManager;
    SharedPref sharedPref;
    AdsPref adsPref;
    App app;
    Ads ads;
    List<Navigation> navigationList = new ArrayList<>();
    List<Slider> sliders = new ArrayList<>();
    DbNavigation dbNavigation;
    ImageView imgSplash;
    boolean isForceOpenAds;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppOpenAd.isAppOpenAdLoaded = false;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(false);

        // Clear any legacy cache or configs from previous sessions
        com.app.webdroid.util.AppJsonManager.clearCache(this);
        try {
            getSharedPreferences("app_config", android.content.Context.MODE_PRIVATE).edit().clear().apply();
            getSharedPreferences("custom_user_channels_pref", android.content.Context.MODE_PRIVATE).edit().clear().apply();
            getSharedPreferences(com.app.webdroid.util.Tools.getApplicationId() + "_ads_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply();
        } catch (Exception ignored) {}

        isForceOpenAds = Config.FORCE_TO_SHOW_APP_OPEN_AD_ON_START;

        dbNavigation = new DbNavigation(this);
        adsManager = new AdsManager(this);
        adsManager.initializeAd();

        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);

        imgSplash = findViewById(R.id.img_splash);
        if (sharedPref.getIsDarkTheme()) {
            imgSplash.setImageResource(R.drawable.bg_splash_dark);
        } else {
            imgSplash.setImageResource(R.drawable.bg_splash_default);
        }

        View layoutBranding = findViewById(R.id.layout_branding);
        if (layoutBranding != null) {
            layoutBranding.setAlpha(0f);
            layoutBranding.setScaleX(0.92f);
            layoutBranding.setScaleY(0.92f);
            layoutBranding.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(600)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }

        android.widget.TextView txtVersion = findViewById(R.id.txt_splash_version);
        if (txtVersion != null) {
            txtVersion.setText("Version " + com.shobmc.san.BuildConfig.VERSION_NAME);
        }

        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);
        Tools.postDelayed(this::requestConfig, Config.DELAY_SPLASH);

        Log.d("Rawr", "open ad status: " + AppOpenAd.isAppOpenAdLoaded);
    }

    private void displayApiResults(CallbackConfig resp) {
        if (resp != null) {
            app = resp.app;
            ads = resp.ads;

            if (resp.menus != null) {
                navigationList = new ArrayList<>(resp.menus);
            } else {
                navigationList = new ArrayList<>();
            }

            Navigation navFav = new Navigation();
            navFav.name = "Favorites";
            navFav.type = "FAVORITES";
            navFav.icon = "https://cdn-icons-png.flaticon.com/512/2589/2589175.png";
            navigationList.add(navFav);

            Navigation navStates = new Navigation();
            navStates.name = "Newspapers by State";
            navStates.type = "PAGE";
            navStates.url = "usa_states_newspapers.json";
            navStates.icon = "https://cdn-icons-png.flaticon.com/512/2991/2991148.png";
            navigationList.add(navStates);

            Navigation navRadio = new Navigation();
            navRadio.name = "US News & Talk Radio";
            navRadio.type = "PAGE";
            navRadio.url = "news_radio.json";
            navRadio.icon = "https://cdn-icons-png.flaticon.com/512/3075/3075908.png";
            navigationList.add(navRadio);

            // Save config for SyncWorker safely
            try {
                android.content.SharedPreferences prefs = getSharedPreferences("app_config",
                        android.content.Context.MODE_PRIVATE);
                prefs.edit().putString("json_config", new Gson().toJson(resp)).apply();
                if (resp.googleSheetUrl != null && !resp.googleSheetUrl.isEmpty()) {
                    getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("google_sheet_url", resp.googleSheetUrl)
                            .apply();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (resp.intro != null) {
                sliders = resp.intro.sliders != null ? resp.intro.sliders : new ArrayList<>();
                sharedPref.setIntroSliderStatus(resp.intro.status);
                sharedPref.saveSliderList(sliders);
            }

            if (adsManager != null) {
                if (app != null) adsManager.saveConfig(sharedPref, app);
                if (ads != null) {
                    adsManager.saveAds(adsPref, ads);
                    if (ads.placement != null) adsManager.saveAdsPlacement(adsPref, ads.placement);
                    adsManager.initializeAd();
                } else {
                    try {
                        InputStream is = getAssets().open("config.json");
                        int size = is.available();
                        byte[] buffer = new byte[size];
                        is.read(buffer);
                        is.close();
                        String json = new String(buffer, StandardCharsets.UTF_8);
                        CallbackConfig localConfig = new Gson().fromJson(json, CallbackConfig.class);
                        if (localConfig != null && localConfig.ads != null) {
                            adsManager.saveAds(adsPref, localConfig.ads);
                            if (localConfig.ads.placement != null) adsManager.saveAdsPlacement(adsPref, localConfig.ads.placement);
                        }
                    } catch (Exception ignored) {}
                    adsManager.initializeAd();
                }
            }


            if (dbNavigation != null) {
                try {
                    dbNavigation.truncateTableMenu(DbNavigation.TABLE_MENU);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (app != null && app.status) {
                Tools.postDelayed(() -> {
                    if (dbNavigation != null) {
                        try {
                            dbNavigation.addListCategory(navigationList, DbNavigation.TABLE_MENU);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    showAppOpenAdIfAvailable();
                }, 100);
                Log.d(TAG, "App status is live");
            } else if (app != null && !app.status) {
                Intent intent = new Intent(getApplicationContext(), ActivityRedirect.class);
                startActivity(intent);
                finish();
                Log.d(TAG, "App status is suspended");
            } else {
                showAppOpenAdIfAvailable();
            }
            Log.d(TAG, "initialize success");
        } else {
            Log.d(TAG, "initialize failed");
            showAppOpenAdIfAvailable();
        }
    }

    private void showAppOpenAdIfAvailable() {
        try {
            if (isForceOpenAds) {
                if (adsPref != null && adsPref.getIsAppOpenAdOnStart() && adsManager != null) {
                    adsManager.loadAppOpenAd(adsPref.getIsAppOpenAdOnStart(), true, () -> {
                        startMainActivity();
                        AppOpenAd.isAppOpenAdLoaded = false;
                        Log.d(TAG, "showAppOpenAdIfAvailable");
                    });
                } else {
                    startMainActivity();
                    Log.d(TAG, "app open on start disabled");
                }
            } else {
                if (adsPref != null && adsPref.getAdStatus() && adsPref.getIsAppOpenAdOnStart()) {
                    Application application = getApplication();
                    if (application instanceof MyApplication) {
                        ((MyApplication) application).showAdIfAvailable(this, this::startMainActivity);
                    } else {
                        startMainActivity();
                    }
                } else {
                    startMainActivity();
                    Log.d(TAG, "startMainActivity");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in showAppOpenAdIfAvailable", e);
            startMainActivity();
        }
    }

    private void startMainActivity() {
        try {
            Intent intent;
            if (sharedPref != null && sharedPref.getIntroSliderStatus()) {
                if (sharedPref.getIsShowIntroSlider()) {
                    if (sharedPref.getSliderList() != null && !sharedPref.getSliderList().isEmpty()) {
                        intent = new Intent(getApplicationContext(), ActivitySlider.class);
                    } else {
                        intent = new Intent(getApplicationContext(), MainActivity.class);
                    }
                } else {
                    intent = new Intent(getApplicationContext(), MainActivity.class);
                }
            } else {
                intent = new Intent(getApplicationContext(), MainActivity.class);
            }
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error starting MainActivity", e);
            try {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppOpenAd.isAppOpenAdLoaded = false;
    }

}
