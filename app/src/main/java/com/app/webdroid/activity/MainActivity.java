package com.app.webdroid.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.webdroid.Config;
import com.shobmc.san.R;
import com.app.webdroid.adapter.AdapterNavigation;
import com.app.webdroid.database.prefs.AdsPref;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.database.sqlite.DbNavigation;
import com.app.webdroid.fragment.FragmentWebView;
import com.app.webdroid.listener.DrawerStateListener;
import com.app.webdroid.model.Navigation;
import com.app.webdroid.util.AdsManager;
import com.app.webdroid.util.Tools;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.solodroid.push.sdk.provider.OneSignalPush;
import com.solodroidx.ads.appopen.AppOpenAd;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.solodroidx.ads.listener.OnShowAdCompleteListener;

public class MainActivity extends AppCompatActivity implements DrawerStateListener, DefaultLifecycleObserver {

    private long exitTime = 0;
    private final static String SELECTED_TAG = "selected_index";
    private final static String TAG = "MainActivity";
    private static int selectedIndex;
    private final static int COLLAPSING_TOOLBAR = 0;
    private DrawerLayout drawerLayout;
    private FragmentManager fragmentManager;
    ActionBarDrawerToggle actionBarDrawerToggle;
    NavigationView navigationView;
    BottomNavigationView bottomNavigationView;
    View navigationBarView;
    View btnBottomAdd;
    private boolean isBarsVisible = true;
    RecyclerView recyclerView;
    DbNavigation dbNavigation;
    List<Navigation> items;
    AdsManager adsManager;

    public AdsManager getAdsManager() {
        return adsManager;
    }

    SharedPref sharedPref;
    AdsPref adsPref;
    CoordinatorLayout parentView;
    public static final int IMMEDIATE_APP_UPDATE_REQ_CODE = 124;
    private AppUpdateManager appUpdateManager;
    private ActivityResultLauncher<IntentSenderRequest> appUpdateResultLauncher;
    LinearLayout customDialogLayout;
    View dimBackground;
    FloatingActionButton btnRate;
    FloatingActionButton btnShare;
    Button btnExit;
    Button btnCancel;
    LinearLayout nativeAdView;
    private int screenWidth;
    OnBackPressedDispatcher onBackPressedDispatcher;
    int counter = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_main);
        Tools.setNavigation(this);

        fragmentManager = getSupportFragmentManager();

        sharedPref = new SharedPref(this);

        adsPref = new AdsPref(this);
        adsManager = new AdsManager(this);

        if (Config.FORCE_TO_SHOW_APP_OPEN_AD_ON_START) {
            ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        }

        // Initialize WorkManager periodic background sync for US News Hub
        try {
            com.app.webdroid.news.worker.NewsSyncWorker.enqueuePeriodicSync(this);
        } catch (Exception ignored) {
        }

        requestNotificationPermission();
        initFcmTopicSubscription();

        Tools.postDelayed(() -> {
            adsManager.initializeAd();
            adsManager.updateConsentStatus();
            adsManager.loadAppOpenAd(adsPref.getIsAppOpenAdOnResume(), false, () -> {
            });
            adsManager.loadBannerAd(adsPref.getIsBannerHome());
            adsManager.loadInterstitialAd();
            adsPref.setIsAppOpen(true);
        }, 100);

        handleOnBackPressed();
        if (fragmentManager != null) {
            fragmentManager.addOnBackStackChangedListener(this::updateBannerVisibility);
        }

        parentView = findViewById(R.id.parent_view);
        navigationView = findViewById(R.id.navigationView);

        if (sharedPref.getIsDarkTheme()) {
            navigationView.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_navigation_dark));
            parentView.setBackgroundColor(ContextCompat.getColor(this, R.color.color_dark_background));
        } else {
            navigationView.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_navigation_default));
            parentView.setBackgroundColor(ContextCompat.getColor(this, R.color.color_light_background));
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        if (!sharedPref.getNavigationDrawer()) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        navigationBarView = findViewById(R.id.navigation_bar_view);
        btnBottomAdd = findViewById(R.id.btn_bottom_add);
        if (btnBottomAdd != null) {
            if (sharedPref.getIsDarkTheme()) {
                btnBottomAdd.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_nav_center_circle_dark));
            } else {
                btnBottomAdd.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_nav_center_circle));
            }
            btnBottomAdd.setOnClickListener(v -> {
                loadWebPage("Newspapers by State", "CATEGORY", "usa_states_newspapers.json", "usa_states_newspapers.json");
            });
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView, (v, insets) -> insets);
            bottomNavigationView.setPadding(0, 0, 0, 0);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_bottom_home) {
                    loadHomeDiscovery();
                    return true;
                } else if (id == R.id.nav_bottom_channels) {
                    loadWebPage("News Channels", "CATEGORY", "news_channels.json", "news_channels.json");
                    return true;
                } else if (id == R.id.nav_bottom_add) {
                    loadWebPage("Newspapers by State", "CATEGORY", "usa_states_newspapers.json", "usa_states_newspapers.json");
                    return true;
                } else if (id == R.id.nav_bottom_categories) {
                    loadWebPage("News Feed", "RSS", "ALL_NEWS", "ALL_NEWS");
                    return true;
                } else if (id == R.id.nav_bottom_saved) {
                    loadWebPage("Saved", "FAVORITES", null, null);
                    return true;
                }
                return false;
            });
        }

        selectedIndex = COLLAPSING_TOOLBAR;
        // getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,
        // new FragmentWebView(), COLLAPSING_TOOLBAR_FRAGMENT_TAG).commit();

        dbNavigation = new DbNavigation(this);
        items = dbNavigation.getAllMenu(DbNavigation.TABLE_MENU);
        if (items != null) {
            Log.d(TAG, "Nav items loaded from DB. Size: " + items.size());
        } else {
            Log.e(TAG, "Nav items from DB is null!");
            items = new ArrayList<>();
        }

        appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());
        if (!Tools.isDebug()) {
            inAppUpdate();
            inAppReview();
        }

        loadWebPage();
        Tools.postDelayed(this::notificationOpenHandler, 500);

        try {
            String oneSignalId = getResources().getString(R.string.onesignal_app_id);
            if (oneSignalId != null && !oneSignalId.isEmpty() && !oneSignalId.startsWith("0000")) {
                new OneSignalPush.Builder(this).requestPushNotificationPermission();
            }
        } catch (Exception e) {
            Log.e(TAG, "OneSignal push permission error: " + e.getMessage());
        }

        // Rocket Speed: Trigger Sync immediately on app launch safely
        try {
            androidx.work.OneTimeWorkRequest syncRequest = new androidx.work.OneTimeWorkRequest.Builder(
                    com.app.webdroid.worker.SyncWorker.class).build();
            androidx.work.WorkManager.getInstance(this).enqueue(syncRequest);
        } catch (Exception e) {
            Log.e(TAG, "WorkManager sync error: " + e.getMessage());
        }

        Uri deepLinkUrl = getIntent().getData();
        if (deepLinkUrl != null) {
            String link = String.valueOf(deepLinkUrl);
            Tools.postDelayed(() -> {
                if (link.contains("?target=custom_tabs")) {
                    CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
                    intent.launchUrl(this, Uri.parse(link.replace("?target=custom_tabs", "")));
                } else {
                    Intent intent = new Intent(getApplicationContext(), ActivityWebView.class);
                    intent.putExtra("title", getString(R.string.app_name));
                    intent.putExtra("link", link);
                    startActivity(intent);
                }
            }, 500);
        }

        initExitDialog();
        getScreenDimensions();
        setCustomDialogWidth();

    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void initFcmTopicSubscription() {
        try {
            String topic = getString(R.string.fcm_notification_topic);
            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("FCM_MAIN", "Successfully subscribed to topic: " + topic);
                        } else {
                            Log.e("FCM_MAIN", "Failed to subscribe to topic: " + topic, task.getException());
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
        Tools.postDelayed(() -> {
            if (adsPref.getIsAppOpenAdOnResume()) {
                if (AppOpenAd.isAppOpenAdLoaded) {
                    adsManager.showAppOpenAd(() -> {
                    });
                    Log.d(AppOpenAd.TAG, "lifecycleObserver Show App Open Ad");
                }
            }
        }, 100);
    }

    private void notificationOpenHandler() {
        String title = getIntent().getStringExtra(OneSignalPush.EXTRA_TITLE);
        String link = getIntent().getStringExtra(OneSignalPush.EXTRA_LINK);
        if (getIntent().hasExtra("id") || getIntent().hasExtra("unique_id")) {
            if (link != null && !link.isEmpty()) {
                if (!link.equals("0")) {
                    if (link.contains("play.google.com") || link.contains("?target=external")) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
                    } else {
                        Intent intent = new Intent(getApplicationContext(), ActivityWebView.class);
                        intent.putExtra("title", title);
                        intent.putExtra("link", link);
                        startActivity(intent);
                    }
                }
            }
        }
    }

    private void loadWebPage() {
        loadNavigationMenu();
        Tools.postDelayed(this::loadHomeDiscovery, 100);
        sharedPref.setLastItemPosition(0);
    }

    public void openDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    private void loadNavigationMenu() {
        if (items == null) {
            items = new ArrayList<>();
        }
        recyclerView = findViewById(R.id.recyclerView);
        if (recyclerView == null) return;

        View btnDrawerAiPrompts = findViewById(R.id.btn_drawer_ai_prompts);
        if (btnDrawerAiPrompts != null) {
            btnDrawerAiPrompts.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                startActivity(new Intent(getApplicationContext(), ActivityAiPrompts.class));
            });
        }

        View btnDrawerAddNewspaper = findViewById(R.id.btn_drawer_add_newspaper);
        if (btnDrawerAddNewspaper != null) {
            btnDrawerAddNewspaper.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                com.app.webdroid.util.CustomChannelManager.showAddNewspaperDialog(this, null, null);
            });
        }

        View btnDrawerAddChannel = findViewById(R.id.btn_drawer_add_channel);
        if (btnDrawerAddChannel != null) {
            btnDrawerAddChannel.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                loadWebPage("Add Channel / Playlist", "ADD_CHANNEL", null, null);
            });
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        AdapterNavigation adapterNavigation = new AdapterNavigation(this, new ArrayList<>());
        adapterNavigation.setListData(items);

        recyclerView.setAdapter(adapterNavigation);

        if (!items.isEmpty()) {
            AdapterNavigation.isFirstItemClicked = true;
        } else {
            // Fallback load config from remote or assets if empty
            com.app.webdroid.util.ConfigManager.getInstance().loadConfig(this, new com.app.webdroid.util.ConfigManager.LoadCallback() {
                @Override
                public void onConfigLoaded(com.app.webdroid.model.AppConfig config) {
                    runOnUiThread(() -> {
                        if (dbNavigation != null) {
                            items = dbNavigation.getAllMenu(DbNavigation.TABLE_MENU);
                            if (items != null && !items.isEmpty()) {
                                adapterNavigation.setListData(items);
                                Navigation obj = items.get(0);
                                if (obj != null) {
                                    loadWebPage(obj.name, obj.type, obj.url, obj.url_dark);
                                }
                            }
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }
            });
        }

        adapterNavigation.setOnItemClickListener((v, obj, position) -> {
            if (drawerLayout != null) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        });
    }

    public void loadWebPage(String name, String type, ArrayList<String> urls) {
        Tools.postDelayed(() -> {
            Fragment fragment;
            if ("RSS".equalsIgnoreCase(type)) {
                fragment = new com.app.webdroid.fragment.FragmentNews();
                Bundle data = new Bundle();
                data.putString("name", name);
                data.putStringArrayList("urls", urls);
                // Also pass the first url as 'url' for backward compatibility or single-url
                // logic fallback
                if (!urls.isEmpty()) {
                    data.putString("url", urls.get(0));
                }
                fragment.setArguments(data);
            } else {
                // Fallback to single URL version if somehow we got here with non-RSS type
                String url = (urls != null && !urls.isEmpty()) ? urls.get(0) : "";
                loadWebPage(name, type, url, url);
                return;
            }

            if (bottomNavigationView != null && bottomNavigationView.getMenu() != null) {
                MenuItem item = bottomNavigationView.getMenu().findItem(R.id.nav_bottom_categories);
                if (item != null) item.setChecked(true);
            }
            androidx.fragment.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(name);
            transaction.commitAllowingStateLoss();

        }, 250);
    }

    public void loadHomeDiscovery() {
        showBars();
        if (bottomNavigationView != null && bottomNavigationView.getMenu() != null) {
            MenuItem homeItem = bottomNavigationView.getMenu().findItem(R.id.nav_bottom_home);
            if (homeItem != null) {
                homeItem.setChecked(true);
            }
        }
        com.app.webdroid.fragment.FragmentHomeDiscovery discoveryFragment = new com.app.webdroid.fragment.FragmentHomeDiscovery();
        androidx.fragment.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, discoveryFragment, "HOME_DISCOVERY");
        transaction.commitAllowingStateLoss();
    }

    public void onScroll(int dy) {
        if (isFinishing() || isDestroyed()) return;
        if (dy > 12) {
            hideBars();
        } else if (dy < -12) {
            showBars();
        }
    }

    public void hideBars() {
        if (isFinishing() || isDestroyed()) return;
        try {
            if (navigationBarView != null && isBarsVisible) {
                isBarsVisible = false;
                int h = navigationBarView.getHeight();
                if (h <= 0) h = (int) (80 * getResources().getDisplayMetrics().density);
                navigationBarView.animate()
                        .translationY(h)
                        .setInterpolator(new android.view.animation.AccelerateInterpolator(2))
                        .setDuration(220)
                        .start();
            }
        } catch (Exception ignored) {}
    }

    public void showBars() {
        if (isFinishing() || isDestroyed()) return;
        try {
            if (navigationBarView != null && !isBarsVisible) {
                isBarsVisible = true;
                navigationBarView.animate()
                        .translationY(0)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator(2))
                        .setDuration(220)
                        .start();
            }
        } catch (Exception ignored) {}
    }

    public void loadWebPage(String name, String type, String url, String urlDark) {
        showBars();
        if ("Home".equalsIgnoreCase(name) || "HOME_DISCOVERY".equalsIgnoreCase(type)) {
            loadHomeDiscovery();
            return;
        }
        final String effectiveType = type;
        Tools.postDelayed(() -> {
            if (bottomNavigationView != null && bottomNavigationView.getMenu() != null) {
                int targetTabId = -1;
                if ((url != null && (url.contains("channel") || url.contains("news_channels"))) || (name != null && name.toLowerCase().contains("channel"))) {
                    targetTabId = R.id.nav_bottom_channels;
                } else if ((url != null && url.contains("usa_states")) || (name != null && name.toLowerCase().contains("state"))) {
                    targetTabId = R.id.nav_bottom_add;
                } else if ("RSS".equalsIgnoreCase(effectiveType) || (name != null && name.toLowerCase().contains("news"))) {
                    targetTabId = R.id.nav_bottom_categories;
                } else if ("FAVORITES".equalsIgnoreCase(effectiveType) || (name != null && (name.equalsIgnoreCase("Saved") || name.equalsIgnoreCase("You")))) {
                    targetTabId = R.id.nav_bottom_saved;
                }
                if (targetTabId != -1) {
                    MenuItem tabItem = bottomNavigationView.getMenu().findItem(targetTabId);
                    if (tabItem != null) tabItem.setChecked(true);
                }
            }

            if ("RADIO".equalsIgnoreCase(effectiveType)) {
                Intent intent = new Intent(MainActivity.this, com.app.webdroid.activity.ActivityRadioPlayer.class);
                startActivity(intent);
                return;
            }

            if ("IPTV".equalsIgnoreCase(effectiveType) || (url != null && url.contains(".m3u8"))) {
                Intent intent = new Intent(MainActivity.this, com.app.webdroid.activity.ActivityVideoDetail.class);
                intent.putExtra("videoId", url);
                intent.putExtra("title", name);
                intent.putExtra("date", "Live 24/7 • HD");
                startActivity(intent);
                return;
            }

            Fragment fragment;
            if ("RSS".equalsIgnoreCase(effectiveType)) {
                fragment = new com.app.webdroid.fragment.FragmentNews();
                Bundle data = new Bundle();
                data.putString("name", name);
                data.putString("url", url);
                fragment.setArguments(data);
            } else if ("YOUTUBE".equalsIgnoreCase(effectiveType)) {
                com.app.webdroid.fragment.FragmentVideos fragmentVideos = new com.app.webdroid.fragment.FragmentVideos();
                Bundle data = new Bundle();
                data.putString("name", name);
                data.putString("url", url); // ID
                fragmentVideos.setArguments(data);
                fragment = fragmentVideos;
            } else if ("FAVORITES".equalsIgnoreCase(effectiveType)) {
                fragment = new com.app.webdroid.fragment.FragmentFavorites();
                Bundle data = new Bundle();
                data.putString("name", name);
                fragment.setArguments(data);
            } else if ("EXTERNAL_BROWSER".equalsIgnoreCase(effectiveType)) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return;
            } else if ("CATEGORY".equalsIgnoreCase(effectiveType) || "MOVIES".equalsIgnoreCase(effectiveType)) {
                // Open Category Fragment
                com.app.webdroid.fragment.FragmentCategory catFragment = new com.app.webdroid.fragment.FragmentCategory();
                Bundle data = new Bundle();
                data.putString("name", name);
                data.putString("url", url);
                data.putString("type", effectiveType);
                catFragment.setArguments(data);
                fragment = catFragment;
            } else if ("VIDEOS".equalsIgnoreCase(effectiveType)) {
                com.app.webdroid.fragment.FragmentVideoList videoFragment = new com.app.webdroid.fragment.FragmentVideoList();
                Bundle data = new Bundle();
                data.putString("name", name);
                data.putString("url", url);
                videoFragment.setArguments(data);
                fragment = videoFragment;
            } else if ("ADD_CHANNEL".equalsIgnoreCase(effectiveType)) {
                com.app.webdroid.fragment.FragmentAddChannel addFragment = com.app.webdroid.fragment.FragmentAddChannel.newInstance(url);
                fragment = addFragment;
            } else if ("SONGS".equalsIgnoreCase(effectiveType)) {
                fragment = com.app.webdroid.fragment.FragmentSongs.newInstance();
            } else {

                // FEATURE: Check for ?target=external
                String targetUrl = url;
                if (url.contains("?target=external")) {
                    try {
                        String cleanUrl = url.replace("?target=external", "");
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl));
                        startActivity(intent);
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // FEATURE: Ads Disclaimer
                if (sharedPref.getWebViewDisclaimerActive()) {
                    android.widget.Toast.makeText(this, "Disclaimer: Third-party ads may appear on this website.",
                            android.widget.Toast.LENGTH_LONG).show();
                }

                String resolvedType = effectiveType;
                if ("assets".equalsIgnoreCase(resolvedType) || (targetUrl != null && targetUrl.endsWith(".html"))) {
                    resolvedType = "assets";
                    if (targetUrl != null && !targetUrl.startsWith("file:///")) {
                        targetUrl = "file:///android_asset/" + targetUrl;
                    }
                }

                com.app.webdroid.fragment.FragmentWebView argumentFragment = new com.app.webdroid.fragment.FragmentWebView();
                Bundle data = new Bundle();
                data.putString("name", name);
                data.putString("type", resolvedType);
                if (sharedPref.getIsDarkTheme()) {
                    if (urlDark != null && !urlDark.isEmpty()) {
                        data.putString("url", urlDark);
                    } else {
                        data.putString("url", targetUrl);
                    }
                } else {
                    data.putString("url", targetUrl);
                }
                argumentFragment.setArguments(data);
                fragment = argumentFragment;
            }

            // Fix: Check if this is Home Fragment
            int homeIndex = (adsPref != null && adsPref.getIsNativeDrawerMenu()) ? 1 : 0;
            boolean isHome = false;
            // Safer check for items/bounds
            if (items != null && items.size() > homeIndex) {
                if (items.get(homeIndex).name.equals(name)) {
                    isHome = true;
                }
            }

            androidx.fragment.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);

            if (!isHome) {
                transaction.addToBackStack(name);
            }
            transaction.commitAllowingStateLoss();

            // AdMob Policy: Dynamically update banner visibility so external web content is never framed
            updateBannerVisibilityForFragment(fragment);

        }, 250);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_TAG, selectedIndex);
    }

    public void showInterstitialAd(String url) {
        // AdMob Policy Compliance: Navigation inside external third-party websites in WebView
        // must never trigger unexpected interstitial ads ("Valuable inventory: Replicated content").
        if (url != null && url.contains("?target=interstitial") && adsPref.getIsInterstitialWebPageLink()) {
            showInterstitialAd(true);
        }
    }

    private int listItemCounter = 1;

    public void showInterstitialAdForListItem(com.solodroidx.ads.listener.OnShowAdCompleteListener listener) {
        if (!adsPref.getInterstitialAdOnListItemClick()) {
            listener.onShowAdComplete();
            return;
        }

        if (listItemCounter >= adsPref.getInterstitialAdIntervalOnListItemClick()) {
            listItemCounter = 1;
            adsManager.showInterstitialAd(true, listener);
        } else {
            listItemCounter++;
            listener.onShowAdComplete();
        }
    }

    public void showInterstitialAd(boolean placement) {
        adsManager.showInterstitialAd(placement);
    }

    public void showInterstitialAd(boolean placement, com.solodroidx.ads.listener.OnShowAdCompleteListener listener) {
        adsManager.showInterstitialAd(placement, listener);
    }

    public void setupNavigationDrawer(Toolbar toolbar) {
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0) {
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                if (slideOffset > 0) {
                    showBannerAd(false);
                } else {
                    updateBannerVisibility();
                }
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {

            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
    }

    public int getHomeItemIndex() {
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                Navigation item = items.get(i);
                if (item != null) {
                    if ((item.url != null && item.url.contains("home.json"))
                            || (item.name != null && item.name.toLowerCase().contains("home"))) {
                        return i;
                    }
                }
            }
            int nativeAdIndex = (adsPref != null && adsPref.getIsNativeDrawerMenu()) ? 1 : 0;
            if (items.size() > nativeAdIndex) {
                return nativeAdIndex;
            }
        }
        return 0;
    }

    public void handleOnBackPressed() {
        onBackPressedDispatcher = getOnBackPressedDispatcher();
        onBackPressedDispatcher.addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    Fragment currentFrag = fragmentManager.findFragmentById(R.id.fragment_container);
                    if (currentFrag instanceof FragmentWebView) {
                        if (FragmentWebView.GetInstance() != null) {
                            FragmentWebView.GetInstance().onWebViewCanGoBack();
                            return;
                        }
                    }

                    if (currentFrag instanceof com.app.webdroid.fragment.FragmentHomeDiscovery) {
                        exitApp();
                    } else {
                        if (fragmentManager.getBackStackEntryCount() > 0) {
                            fragmentManager.popBackStack();
                            Tools.postDelayed(() -> {
                                Fragment newFrag = fragmentManager.findFragmentById(R.id.fragment_container);
                                if (newFrag instanceof com.app.webdroid.fragment.FragmentHomeDiscovery) {
                                    if (bottomNavigationView != null && bottomNavigationView.getMenu() != null) {
                                        MenuItem item = bottomNavigationView.getMenu().findItem(R.id.nav_bottom_home);
                                        if (item != null) {
                                            item.setChecked(true);
                                        }
                                    }
                                }
                            }, 100);
                        } else {
                            loadHomeOrExit();
                        }
                    }
                }
            }
        });
    }

    @Override
    public boolean isDrawerOpen() {
        return drawerLayout.isDrawerOpen(GravityCompat.START);
    }

    @Override
    public void onBackButtonPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            exitApp();
        }
    }

    public void exitApp() {
        if (Config.SHOW_EXIT_DIALOG) {
            if (customDialogLayout != null) {
                if (customDialogLayout.getVisibility() == View.VISIBLE) {
                    hideCustomDialog();
                } else {
                    showCustomDialog();
                }
            } else {
                showCustomDialog();
            }
        } else {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                showSnackBar(getString(R.string.exit_msg));
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                destroyBannerAd();
                destroyAppOpenAd();
            }
        }
    }

    private int webPageExitCounter = 0;

    public void showInterstitialAdOnWebPageBack(Runnable onComplete) {
        if (adsPref == null || !adsPref.getAdStatus()) {
            onComplete.run();
            return;
        }
        webPageExitCounter++;
        // Safe frequency capping: every 3rd web page exit as a natural transition point
        int interval = 3;
        if (webPageExitCounter >= interval) {
            webPageExitCounter = 0;
            adsManager.showInterstitialAd(true, onComplete::run);
        } else {
            onComplete.run();
        }
    }

    public void loadHomeOrExit() {
        Fragment currentFrag = fragmentManager.findFragmentById(R.id.fragment_container);
        if (currentFrag instanceof com.app.webdroid.fragment.FragmentHomeDiscovery) {
            exitApp();
        } else if (currentFrag instanceof FragmentWebView) {
            // Natural transition point: User finished viewing a web link/newspaper and clicked Back
            showInterstitialAdOnWebPageBack(() -> {
                fragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                loadHomeDiscovery();
            });
        } else {
            fragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            loadHomeDiscovery();
        }
    }


    private void showCustomDialog() {
        if (customDialogLayout == null || dimBackground == null) return;
        customDialogLayout.setAlpha(1f);
        dimBackground.setAlpha(1f);
        customDialogLayout.setVisibility(View.VISIBLE);
        dimBackground.setVisibility(View.VISIBLE);
        customDialogLayout.bringToFront(); // Ensure on top
        isWindowLightStatusBarNavigation();
        onDialogShowStatusBarChanged(true);
        showBannerAd(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyBannerAd();
        destroyAppOpenAd();
    }

    @Override
    public void onResume() {
        super.onResume();
        adsManager.resumeBannerAd(adsPref.getIsBannerHome());
        Tools.checkAndShowRatingPrompt(this);
    }

    public void destroyBannerAd() {
        adsManager.destroyBannerAd();
    }

    public void destroyAppOpenAd() {
        AppOpenAd.isAppOpenAdLoaded = false;
        if (Config.FORCE_TO_SHOW_APP_OPEN_AD_ON_START) {
            adsManager.destroyAppOpenAd();
            ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            android.graphics.drawable.Drawable searchIcon = searchItem.getIcon();
            if (searchIcon != null) {
                searchIcon = androidx.core.graphics.drawable.DrawableCompat.wrap(searchIcon.mutate());
                if (sharedPref.getIsDarkTheme()) {
                    androidx.core.graphics.drawable.DrawableCompat.setTint(searchIcon, android.graphics.Color.WHITE);
                } else {
                    androidx.core.graphics.drawable.DrawableCompat.setTint(searchIcon, androidx.core.content.ContextCompat.getColor(this, R.color.color_light_text_primary));
                }
                searchItem.setIcon(searchIcon);
            }
            androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
            if (searchView != null) {
                androidx.fragment.app.Fragment currentFrag = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (currentFrag instanceof com.app.webdroid.fragment.FragmentCategory) {
                    searchView.setQueryHint("Search movies, actors, directors...");
                } else {
                    searchView.setQueryHint("Search videos, songs, comedy...");
                }

                searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        androidx.fragment.app.Fragment cf = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        if (cf instanceof com.app.webdroid.fragment.FragmentCategory) {
                            searchView.setQueryHint("Search movies, actors, directors...");
                        } else {
                            searchView.setQueryHint("Search videos, songs, comedy...");
                        }
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        androidx.fragment.app.Fragment cf = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        if (cf instanceof com.app.webdroid.fragment.FragmentCategory) {
                            ((com.app.webdroid.fragment.FragmentCategory) cf).performSearch("");
                        }
                        return true;
                    }
                });

                searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        if (query != null && !query.trim().isEmpty()) {
                            searchView.clearFocus();
                            androidx.fragment.app.Fragment cf = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                            if (cf instanceof com.app.webdroid.fragment.FragmentVideoList) {
                                searchItem.collapseActionView();
                                ((com.app.webdroid.fragment.FragmentVideoList) cf).performUserSearch(query.trim());
                            } else if (cf instanceof com.app.webdroid.fragment.FragmentCategory) {
                                ((com.app.webdroid.fragment.FragmentCategory) cf).performSearch(query.trim());
                            } else {
                                searchItem.collapseActionView();
                                String searchUrl = "search:" + query.trim() + "|CAI%3D";
                                loadWebPage("Search: " + query.trim(), "VIDEOS", searchUrl, searchUrl);
                            }
                        }
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        androidx.fragment.app.Fragment cf = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        if (cf instanceof com.app.webdroid.fragment.FragmentCategory) {
                            ((com.app.webdroid.fragment.FragmentCategory) cf).performSearch(newText);
                            return true;
                        }
                        return false;
                    }
                });
            }
        }
        return true;
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_ai_prompts) {
            startActivity(new Intent(getApplicationContext(), ActivityAiPrompts.class));
            return true;
        } else if (itemId == R.id.menu_add_newspaper) {
            com.app.webdroid.util.CustomChannelManager.showAddNewspaperDialog(this, null, null);
            return true;
        } else if (itemId == R.id.menu_add_channel) {
            loadWebPage("Add Channel / Playlist", "ADD_CHANNEL", null, null);
            return true;
        } else if (itemId == R.id.menu_settings) {
            startActivity(new Intent(getApplicationContext(), ActivitySettings.class));
            return true;
        } else if (itemId == R.id.menu_share) {
            Tools.shareApp(this, getString(R.string.share_text));
            return true;
        } else if (itemId == R.id.menu_rate) {
            Tools.rateApp(this);
            return true;
        } else if (itemId == R.id.menu_more) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(sharedPref.getMoreAppsUrl())));
            return true;
        } else if (itemId == R.id.menu_privacy) {
            Intent intent = new Intent(getApplicationContext(), ActivityWebView.class);
            intent.putExtra("title", getString(R.string.menu_privacy));
            intent.putExtra("link", sharedPref.getPrivacyPolicyUrl());
            startActivity(intent);
            return true;
        } else if (itemId == R.id.menu_about) {
            Tools.showAboutDialog(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void inAppReview() {
        if (sharedPref.getInAppReviewToken() <= 3) {
            sharedPref.updateInAppReviewToken(sharedPref.getInAppReviewToken() + 1);
        } else {
            ReviewManager manager = ReviewManagerFactory.create(this);
            Task<ReviewInfo> request = manager.requestReviewFlow();
            request.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ReviewInfo reviewInfo = task.getResult();
                    manager.launchReviewFlow(MainActivity.this, reviewInfo).addOnFailureListener(e -> {
                    }).addOnCompleteListener(complete -> {
                    }).addOnFailureListener(failure -> {
                    });
                }
            }).addOnFailureListener(failure -> Log.d("In-App Review", "In-App Request Failed " + failure));
        }
    }

    private void inAppUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());
        appUpdateResultLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        showSnackBar(getString(R.string.msg_success_update));
                    } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        showSnackBar(getString(R.string.msg_cancel_update));
                        Log.d(TAG, "Update flow cancelled by user.");
                    } else {
                        showSnackBar(getString(R.string.msg_failed_update));
                        Log.e(TAG, "Update flow failed with result code: " + result.getResultCode());
                    }
                });

        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                startUpdateFlow(appUpdateInfo);
            } else if (appUpdateInfo
                    .updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdateFlow(appUpdateInfo);
            }
        });
    }

    private void startUpdateFlow(AppUpdateInfo appUpdateInfo) {
        AppUpdateOptions appUpdateOptions = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                .setAllowAssetPackDeletion(true).build();
        appUpdateManager.startUpdateFlowForResult(appUpdateInfo, appUpdateResultLauncher, appUpdateOptions);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, intent);
        }
        if (requestCode == IMMEDIATE_APP_UPDATE_REQ_CODE) {
            if (resultCode == RESULT_OK) {
                showSnackBar(getString(R.string.msg_success_update));
            } else if (resultCode == RESULT_CANCELED) {
                showSnackBar(getString(R.string.msg_cancel_update));
            } else {
                showSnackBar(getString(R.string.msg_failed_update));
                inAppUpdate();
            }
        }
    }

    public void showSnackBar(String message) {
        Snackbar.make(parentView, message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void urlChecker() {
        Tools.urlChecker(this, fragmentManager);
    }

    private void initExitDialog() {
        customDialogLayout = findViewById(R.id.custom_dialog_layout);
        dimBackground = findViewById(R.id.dim_background);
        if (customDialogLayout == null || dimBackground == null) return;

        if (sharedPref.getIsDarkTheme()) {
            customDialogLayout.setBackgroundResource(R.drawable.bg_dialog_dark);
        } else {
            customDialogLayout.setBackgroundResource(R.drawable.bg_dialog_light);
        }
        customDialogLayout.setOnClickListener(v -> {
            // do nothing
        });

        btnRate = findViewById(R.id.btn_rate);
        btnShare = findViewById(R.id.btn_share);
        btnExit = findViewById(R.id.btn_exit);
        btnCancel = findViewById(R.id.btn_cancel);
        nativeAdView = findViewById(R.id.native_ad_view);

        if (nativeAdView != null) {
            Tools.setNativeAdStyle(MainActivity.this, nativeAdView, adsPref.getNativeAdStyleExitDialog());
            adsManager.loadNativeAdView(nativeAdView, adsPref.getIsNativeExitDialog(),
                    adsPref.getNativeAdStyleExitDialog());
        }

        if (btnRate != null) {
            btnRate.setOnClickListener(v -> {
                hideCustomDialog();
                Tools.showFiveStarRatingDialog(MainActivity.this, true);
            });
        }

        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text) + "\n"
                        + "https://play.google.com/store/apps/details?id=" + Tools.getApplicationId());
                intent.setType("text/plain");
                startActivity(intent);
                hideCustomDialog();
            });
        }

        if (btnExit != null) {
            btnExit.setOnClickListener(v -> {
                finish();
                destroyBannerAd();
                destroyAppOpenAd();
                hideCustomDialog();
            });
        }

        dimBackground.setOnClickListener(v -> hideCustomDialog());
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> hideCustomDialog());
        }
    }

    private void getScreenDimensions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();
            screenWidth = windowMetrics.getBounds().width();
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            screenWidth = displayMetrics.widthPixels;
        }
    }

    private void setCustomDialogWidth() {
        if (customDialogLayout == null) return;
        int desiredWidth = (int) (screenWidth * 0.85);
        ViewGroup.LayoutParams params = customDialogLayout.getLayoutParams();
        if (params != null) {
            params.width = desiredWidth;
            customDialogLayout.setLayoutParams(params);
        }
    }

    private void hideCustomDialog() {
        if (customDialogLayout != null) {
            customDialogLayout.animate().alpha(0f).setDuration(300).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    customDialogLayout.setVisibility(View.GONE);
                }
            });
        }
        if (dimBackground != null) {
            dimBackground.animate().alpha(0f).setDuration(300).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    dimBackground.setVisibility(View.GONE);
                }
            });
        }
        isWindowLightStatusBarNavigation();
        onDialogShowStatusBarChanged(false);
        updateBannerVisibility();
    }

    private void onDialogShowStatusBarChanged(boolean onShow) {
        Tools.postDelayed(() -> {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                int defaultColor;
                if (sharedPref.getIsDarkTheme()) {
                    defaultColor = ContextCompat.getColor(this, R.color.color_dark_background);
                } else {
                    defaultColor = ContextCompat.getColor(this, R.color.color_light_background);
                }
                int newColor = ColorUtils.blendARGB(defaultColor, Color.BLACK, 0.5f);
                if (onShow) {
                    getWindow().setNavigationBarColor(newColor);
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                            .setAppearanceLightNavigationBars(false);
                } else {
                    getWindow().setNavigationBarColor(defaultColor);
                }
            }
        }, 100);
    }

    private void isWindowLightStatusBarNavigation() {
        if (sharedPref.getIsDarkTheme()) {
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                    .setAppearanceLightStatusBars(false);
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                    .setAppearanceLightNavigationBars(false);
        } else {
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                    .setAppearanceLightStatusBars(false);
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                    .setAppearanceLightNavigationBars(true);
        }
    }

    public void updateBannerVisibility() {
        if (fragmentManager == null) return;
        Fragment currentFrag = fragmentManager.findFragmentById(R.id.fragment_container);
        updateBannerVisibilityForFragment(currentFrag);
    }

    public void updateBannerVisibilityForFragment(Fragment frag) {
        if (frag instanceof FragmentWebView) {
            // AdMob Policy: NEVER frame third-party websites with banner ads (Replicated content / Framing)
            showBannerAd(false);
            return;
        }
        if (adsPref != null && adsPref.getIsBannerHome()) {
            showBannerAd(true);
        } else {
            showBannerAd(false);
        }
    }

    private void showBannerAd(boolean show) {
        View adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

}
