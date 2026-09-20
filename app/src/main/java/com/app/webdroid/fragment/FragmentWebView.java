package com.app.webdroid.fragment;

import static android.content.Context.DOWNLOAD_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Intent.CATEGORY_DEFAULT;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NO_HISTORY;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.SslError;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.webdroid.Config;
import com.shobmc.san.R;
import com.app.webdroid.activity.MainActivity;
import com.app.webdroid.adapter.AdapterNavigation;
import com.app.webdroid.database.prefs.AdsPref;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.listener.WebViewOnTouchListener;
import com.app.webdroid.util.Constant;
import com.app.webdroid.util.CustomGestureDetector;
import com.app.webdroid.util.Tools;
import com.app.webdroid.webview.CustomWebView;
import com.app.webdroid.webview.WebViewHelper;
import com.blikoon.qrcodescanner.QrCodeActivity;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings({ "deprecation", "CallToPrintStackTrace" })
public class FragmentWebView extends Fragment {

    private static final String TAG = "FragmentWebViewNew";
    @SuppressLint("StaticFieldLeak")
    private static FragmentWebView instance;
    View rootView;
    WebViewHelper webViewHelper;
    private boolean isErrorPageLoaded = false;
    private CustomWebView webView;
    private WebView webViewPopUp;
    private RelativeLayout mContainer;
    private RelativeLayout windowContainer;
    private RelativeLayout lytOffline;
    private PermissionRequest permissionRequest = null;
    public static String mGeolocationOrigin;
    public static GeolocationPermissions.Callback mGeolocationCallback;
    SwipeRefreshLayout swipeRefreshLayout;
    public static final int MULTIPLE_PERMISSIONS = 10;
    public final int REQUEST_PERMISSION_STORAGE_CAMERA = 354;
    public LinearProgressIndicator linearProgressIndicator;
    private LinearLayout lytProgress;
    String mCM;
    String mVM;
    ValueCallback<Uri[]> mUMA;
    private final static int FCR = 1;
    public String hostPart;
    public String uuid = "";
    static long TimeStamp = 0;
    static boolean isInBackGround = false;
    private Handler notificationHandler;
    private Handler CartRemindernotificationHandler;
    private Handler CategoryRecommNotificationHandler;
    private Handler ProductRecommNotificationHandler;
    Timer timer = new Timer();
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter[] writeTagFilters;
    boolean readModeNFC = false;
    boolean writeModeNFC = false;
    String textToWriteNFC = "";
    private final Handler cookieSyncHandler = new Handler();
    private Runnable cookieSyncRunnable;
    private boolean onResumeCalled = false;
    private boolean cookieSyncOn = false;
    private boolean scanningModeOn = false;
    private boolean persistentScanningMode = false;
    private float previousScreenBrightness;
    RelativeLayout fragmentView;
    private MainActivity activity;
    SharedPref sharedPref;
    private Toolbar toolbar;
    TextView toolbarTitle;
    String pageType;
    String pageUrl;
    String pageTitle;
    AdsPref adsPref;
    FrameLayout customViewContainer;

    public FragmentWebView() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        if (getArguments() != null) {
            pageTitle = getArguments().getString("name");
            pageType = getArguments().getString("type");
            String url = getArguments().getString("url");
            if ("assets".equalsIgnoreCase(pageType) || (url != null && url.endsWith(".html"))) {
                if (url != null && !url.startsWith("file:///")) {
                    pageUrl = "file:///android_asset/" + url;
                } else {
                    pageUrl = url != null ? url : "";
                }
            } else {
                pageUrl = url != null ? url : "";
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_webview_new, container, false);
        instance = this;
        sharedPref = new SharedPref(activity);
        adsPref = new AdsPref(activity);
        webViewHelper = new WebViewHelper(activity);
        setupToolbar();
        initView();
        initWebView();
        return rootView;
    }

    public static FragmentWebView GetInstance() {
        return instance;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (sharedPref.getNavigationDrawer()) {
            activity.setupNavigationDrawer(toolbar);
        }
    }

    private void initView() {
        fragmentView = rootView.findViewById(R.id.fragment_view);
        webView = rootView.findViewById(R.id.webView);
        mContainer = rootView.findViewById(R.id.web_container);
        windowContainer = rootView.findViewById(R.id.window_container);
        lytOffline = rootView.findViewById(R.id.offline_layout);
        customViewContainer = activity.findViewById(R.id.main_video_layout);

        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.color_light_primary);
        swipeRefreshLayout.setRefreshing(!Config.ENABLE_LINEAR_PROGRESS_INDICATOR);

        lytProgress = rootView.findViewById(R.id.lyt_progress);
        showLinearProgressIndicator(Config.ENABLE_LINEAR_PROGRESS_INDICATOR);

        linearProgressIndicator = rootView.findViewById(R.id.progressBar);
        if (sharedPref.getIsDarkTheme()) {
            linearProgressIndicator
                    .setIndicatorColor(ContextCompat.getColor(activity, R.color.color_dark_progress_indicator));
        } else {
            linearProgressIndicator
                    .setIndicatorColor(ContextCompat.getColor(activity, R.color.color_light_progress_indicator));
        }
    }

    @SuppressWarnings({ "AccessStaticViaInstance", "ClickableViewAccessibility" })
    private void initWebView() {
        if (Constant.NfcEnabled) {
            if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.NFC) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[] { Manifest.permission.NFC },
                        Constant.permissionRequestCode);
            } else {
                initNFC();
            }
        }

        uuid = Settings.System.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if (Constant.enableHardwareAcceleration) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            webView.setLayerType(View.LAYER_TYPE_NONE, null);
        }
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.setGestureDetector(new GestureDetector(new CustomGestureDetector(activity, webView)));

        if (Tools.isDebug()) {
            WebView.setWebContentsDebuggingEnabled(true);
            webView.setWebContentsDebuggingEnabled(true);
        } else {
            WebView.setWebContentsDebuggingEnabled(false);
            webView.setWebContentsDebuggingEnabled(false);
        }

        swipeRefreshLayout.setEnabled(Config.ENABLE_SWIPE_REFRESH_LAYOUT);

        if (Constant.hideVerticalScrollbar) {
            webView.setVerticalScrollBarEnabled(false);
        }
        if (Constant.hideHorizontalScrollbar) {
            webView.setHorizontalScrollBarEnabled(false);
        }

        swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());

        setOfflineScreenBackgroundColor();

        final Button tryAgainButton = rootView.findViewById(R.id.btn_retry);
        tryAgainButton.setOnClickListener(view -> loadMainUrl(pageUrl));

        webView.setWebViewClient(new AdvanceWebViewClient());
        webView.getSettings().setSupportMultipleWindows(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);

        if (sharedPref.getGeolocation()) {
            webView.getSettings().setGeolocationEnabled(true);
            webView.setGeolocationEnabled(true);
        }

        webView.requestFocus(View.FOCUS_DOWN);
        webView.setOnTouchListener(new WebViewOnTouchListener());

        final String appName;
        String appName1;
        try {
            appName1 = activity.getApplicationInfo().loadLabel(activity.getPackageManager()).toString();
        } catch (Exception e) {
            appName1 = webView.getTitle();
        }
        appName = appName1;

        webView.setWebChromeClient(new AdvanceWebChromeClient() {

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                String[] resources = request.getResources();
                for (String resource : resources) {
                    if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(resource)) {
                        request.grant(resources);
                        return;
                    }
                }
                super.onPermissionRequest(request);
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                AlertDialog dialog = new AlertDialog.Builder(view.getContext()).setTitle(appName).setMessage(message)
                        .setPositiveButton("OK", (dialog1, which) -> {
                        }).create();
                dialog.show();
                result.confirm();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                AlertDialog.Builder b = new AlertDialog.Builder(view.getContext())
                        .setTitle(appName)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> result.cancel());
                b.show();
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue,
                    JsPromptResult result) {
                final EditText input = new EditText(activity);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(defaultValue);
                new AlertDialog.Builder(activity)
                        .setTitle(appName)
                        .setView(input)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok,
                                (dialog, which) -> result.confirm(input.getText().toString()))
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> result.cancel())
                        .create()
                        .show();
                return true;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (CustomWebView.geolocationEnabled) {
                    callback.invoke(origin, true, false);
                    if (!Tools.isLocationEnabled(activity)) {
                        Snackbar.make(activity.findViewById(R.id.parent_view), "Device location is disabled", 10000)
                                .setAction("Settings", v -> {
                                    final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    activity.startActivity(intent);
                                })
                                .show();
                    }
                } else {
                    new AdvanceWebChromeClient().onGeolocationPermissionsShowPrompt(origin, callback);
                }
            }

            @Override
            public void onGeolocationPermissionsHidePrompt() {
                new AdvanceWebChromeClient().onGeolocationPermissionsHidePrompt();
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                if (Constant.AUTO_DOWNLOAD_FILES) {
                    webViewHelper.downloadFile(url);
                } else {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                }
            } catch (Exception e) {
                Toast.makeText(activity, "No activity to handle the downloaded file.", Toast.LENGTH_SHORT).show();
            }
        });

        registerForContextMenu(webView);
        webViewHelper.initWebSettings(webView);
        if (webViewHelper.isConnectedNetwork()) {
            loadMainUrl(pageUrl);
            Constant.isConnected = true;
            showOfflineLayout(false);
        } else {
            showOfflineLayout(true);
        }

        if (!Constant.isConnected) {
            checkInternetConnection();
        }

        if (activity.getIntent().getExtras() != null) {
            String openurl = activity.getIntent().getExtras().getString("openURL");
            if (openurl != null) {
                webViewHelper.openInExternalBrowser(openurl);
            }

        }
    }

    private void setupToolbar() {
        toolbar = rootView.findViewById(R.id.toolbar);
        toolbarTitle = rootView.findViewById(R.id.toolbar_title);

        toolbar.setTitle("");
        toolbarTitle.setText(pageTitle);
        activity.setSupportActionBar(toolbar);
        if (sharedPref.getToolbar()) {
            toolbar.setVisibility(View.VISIBLE);
        } else {
            toolbar.setVisibility(View.GONE);
        }

        if (sharedPref.getIsDarkTheme()) {
            toolbar.setBackgroundColor(ContextCompat.getColor(activity, R.color.color_dark_toolbar));
            toolbar.getContext().setTheme(androidx.appcompat.R.style.ThemeOverlay_AppCompat_Dark);
            toolbarTitle.setTextColor(ContextCompat.getColor(activity, R.color.color_dark_title_toolbar));
        } else {
            toolbar.setBackgroundColor(ContextCompat.getColor(activity, R.color.color_light_primary));
            toolbar.setPopupTheme(androidx.appcompat.R.style.ThemeOverlay_AppCompat_Light);
            toolbarTitle.setTextColor(ContextCompat.getColor(activity, R.color.color_light_title_toolbar));
        }

    }

    private void checkInternetConnection() {
        if (pageUrl != null && pageUrl.startsWith("file:///")) {
            return;
        }
        class AutoRec extends TimerTask {
            public void run() {
                activity.runOnUiThread(() -> {
                    if (!webViewHelper.isConnectedNetwork()) {
                        Constant.isConnected = false;
                        showOfflineLayout(true);
                        loadMainUrl(pageUrl);
                    } else {
                        if (!Constant.isConnected) {
                            loadMainUrl(pageUrl);
                            Constant.isConnected = true;
                            showOfflineLayout(false);
                            if (timer != null) {
                                timer.cancel();
                            }
                        }
                    }
                });
            }
        }
        timer.schedule(new AutoRec(), 0, 5000);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (Settings.System.getInt(activity.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                    activity.getWindow()
                            .getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
                }
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
                if (controller != null) {
                    controller.show(WindowInsetsCompat.Type.statusBars());
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                        controller.setAppearanceLightNavigationBars(!new SharedPref(activity).getIsDarkTheme());
                    }
                }
            }
        }
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final WebView.HitTestResult webViewHitTestResult = webView.getHitTestResult();

        if (webViewHitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                webViewHitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

            if (Constant.allowImageDownload) {
                menu.setHeaderTitle("Download images");
                menu.add(0, 1, 0, "Download the image")
                        .setOnMenuItemClickListener(menuItem -> {
                            String DownloadImageURL = webViewHitTestResult.getExtra();
                            if (URLUtil.isValidUrl(DownloadImageURL)) {
                                DownloadManager.Request request = new DownloadManager.Request(
                                        Uri.parse(DownloadImageURL));
                                request.allowScanningByMediaScanner();
                                request.setNotificationVisibility(
                                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                DownloadManager downloadManager = (DownloadManager) activity
                                        .getSystemService(DOWNLOAD_SERVICE);
                                downloadManager.enqueue(request);
                                Toast.makeText(activity, "Image downloaded successfully.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(activity, "Sorry...something went wrong.", Toast.LENGTH_LONG).show();
                            }
                            return false;
                        });
            }
        }
    }

    ValueCallback<Uri[]> uploadMessage;
    public static final int REQUEST_SELECT_FILE = 100;

    @SuppressWarnings({ "ResultOfMethodCallIgnored", "ConstantValue" })
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_SELECT_FILE) {
            if (uploadMessage == null)
                return;
            uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
            uploadMessage = null;
        }
        Uri[] results = null;
        Uri uri;
        if (requestCode == FCR) {
            if (resultCode == Activity.RESULT_OK) {
                if (mUMA == null) {
                    return;
                }
                if (intent == null || intent.getData() == null) {
                    if (intent != null && intent.getClipData() != null) {
                        int count = intent.getClipData().getItemCount();
                        results = new Uri[intent.getClipData().getItemCount()];
                        for (int i = 0; i < count; i++) {
                            uri = intent.getClipData().getItemAt(i).getUri();
                            results[i] = uri;
                        }
                    }
                    if (mCM != null) {
                        File file = new File(Objects.requireNonNull(Uri.parse(mCM).getPath()));
                        if (file.length() > 0) {
                            results = new Uri[] { Uri.parse(mCM) };
                        } else {
                            file.delete();
                        }
                    }
                    if (mVM != null) {
                        File file = new File(Objects.requireNonNull(Uri.parse(mVM).getPath()));
                        if (file.length() > 0)
                            results = new Uri[] { Uri.parse(mVM) };
                        else
                            file.delete();
                    }
                } else {
                    String dataString = intent.getDataString();
                    if (dataString != null) {
                        results = new Uri[] { Uri.parse(dataString) };
                    } else {
                        if (intent.getClipData() != null) {
                            final int numSelectedFiles = intent.getClipData().getItemCount();
                            results = new Uri[numSelectedFiles];
                            for (int i = 0; i < numSelectedFiles; i++) {
                                results[i] = intent.getClipData().getItemAt(i).getUri();
                            }
                        }

                    }
                }
            } else {
                if (mCM != null) {
                    File file = new File(Objects.requireNonNull(Uri.parse(mCM).getPath()));
                    if (file != null) {
                        file.delete();
                    }
                }
                if (mVM != null) {
                    File file = new File(Objects.requireNonNull(Uri.parse(mVM).getPath()));
                    if (file != null) {
                        file.delete();
                    }
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        } else if (requestCode == WebViewHelper.CODE_AUDIO_CHOOSER) {
            if (resultCode == Activity.RESULT_OK) {
                if (intent != null && intent.getData() != null) {
                    results = new Uri[] { intent.getData() };
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        } else if (requestCode == Constant.requestQrScanCode) {
            if (resultCode == Activity.RESULT_OK) {
                if (intent != null) {
                    String result = intent.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_result");
                    if (result != null && URLUtil.isValidUrl(result)) {
                        loadQRCodeURL(result);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private boolean URLisExternal(String url) {
        return true;
    }

    @SuppressWarnings("DataFlowIssue")
    private void loadQRCodeURL(String url) {
        switch (Constant.qrCodeUrlOptions) {
            case 1:
                openInAppTab(url);
                break;
            case 2:
                openInNewBrowser(url);
                break;
            case 3:
                if (URLisExternal(url)) {
                    openInAppTab(url);
                } else {
                    webView.loadUrl(url);
                }
                break;
            case 4:
                if (URLisExternal(url)) {
                    openInNewBrowser(url);
                } else {
                    webView.loadUrl(url);
                }
                break;
            default:
                webView.loadUrl(url);
        }
    }

    @SuppressWarnings("ConstantValue")
    private void loadMainUrl(String pageUrl) {
        if (!webViewHelper.isConnectedNetwork()) {
            System.out.println("loadMainUrl no connection");
        } else {
            lytOffline.setVisibility(View.GONE);
            String urlExt = "";
            String urlExt2 = "";
            String urlExtUUID = "";
            String language;
            if (Constant.appendLanguageCode) {
                language = Locale.getDefault().getLanguage().toUpperCase();
                language = "?webview_language=" + language;
            } else {
                language = "";
            }
            String urlToLoad = pageUrl + language;

            urlToLoad += urlExt2;
            if (Constant.uuidEnhanceWebViewUrl) {
                if (urlToLoad.contains("?") || urlExt.contains("?")) {
                    urlExtUUID = String.format("%suuid=%s", "&", uuid);
                } else {
                    urlExtUUID = String.format("%suuid=%s", "?", uuid);
                }
            }
            urlToLoad += urlExtUUID;
            if (Tools.isDebug())
                Log.d(TAG, " HOME_URL " + urlToLoad);
            if (urlToLoad.contains("target=external")) {
                String cleanUrl = urlToLoad.replace("?target=external", "").replace("&target=external", "");
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl));
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "No application can handle this request.");
                }
                return;
            }
            webView.loadUrl(urlToLoad);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constant.permissionRequestCode) {
            if (Constant.NfcEnabled) {
                initNFC();
            }
        }

        if (requestCode == WebViewHelper.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mGeolocationCallback != null) {
                    mGeolocationCallback.invoke(mGeolocationOrigin, true, false);
                }
                Log.d(TAG, "Location permission granted");
            } else {
                if (mGeolocationCallback != null) {
                    mGeolocationCallback.invoke(mGeolocationOrigin, false, false);
                }
                Log.d(TAG, "Location permission denied");
            }
        }

        if (requestCode == REQUEST_PERMISSION_STORAGE_CAMERA) {
            boolean isAllPermissionGranted = webViewHelper.hasPermissions(activity, permissions);
            if (isAllPermissionGranted) {
                if (mUMA != null && webChromeClientFileChooserParams != null) {
                    webViewHelper.openFilePicker(webChromeClientFileChooserParams);
                }
            } else {
                boolean isStorageRationale = Constant.requireStoragePermission
                        && activity.checkSelfPermission(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                        &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(activity,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE);
                boolean isCameraRationale = Constant.requireCameraPermission
                        && activity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
                        &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA);
                if (isStorageRationale && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    webViewHelper.showNoPermissionMessage(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                } else if (isCameraRationale) {
                    webViewHelper.showNoPermissionMessage(Manifest.permission.CAMERA);
                }
            }
        }

        if (requestCode == Constant.cameraRequestCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (Tools.isDebug())
                    Log.d(TAG, "Camera permission granted");
            } else {
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.CAMERA);
                if (Tools.isDebug())
                    Log.d(TAG, "Camera permission denied - Rationale: " + showRationale);
                if (!showRationale) {
                    webViewHelper.showNoPermissionMessage(Manifest.permission.CAMERA);
                }
                if (Tools.isDebug())
                    Log.d(TAG, "Camera permission denied");
            }
        }

        // QR Code
        if (requestCode == 1402) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent i = new Intent(activity, QrCodeActivity.class);
                startActivityForResult(i, Constant.requestQrScanCode);
            } else {
                Toast.makeText(activity, "Camera permission is required for scanning QR Code", Toast.LENGTH_SHORT)
                        .show();
            }
        }

        switch (requestCode) {
            case WebViewHelper.WEBVIEW_PERMISSION_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (permissionRequest != null) {
                        permissionRequest.grant(permissionRequest.getResources());
                    }
                } else {
                    if (permissionRequest != null) {
                        permissionRequest.deny();
                    }
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(activity,
                            Manifest.permission.RECORD_AUDIO)) {
                        webViewHelper.showNoPermissionMessage(Manifest.permission.RECORD_AUDIO);
                    }
                }
                break;
            }
            case MULTIPLE_PERMISSIONS: {
                if (!webViewHelper.hasPermissions(activity, permissions)) {
                    boolean isStorageRationale = Constant.requireStoragePermission
                            && activity.checkSelfPermission(
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                            &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(activity,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    boolean isCameraRationale = Constant.requireCameraPermission
                            && activity
                                    .checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
                            &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA);
                    boolean isLocationRationale = sharedPref.getGeolocation() && (activity.checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
                            || activity.checkSelfPermission(
                                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED)
                            &&
                            (!ActivityCompat.shouldShowRequestPermissionRationale(activity,
                                    Manifest.permission.ACCESS_FINE_LOCATION)
                                    || !ActivityCompat.shouldShowRequestPermissionRationale(activity,
                                            Manifest.permission.ACCESS_COARSE_LOCATION));
                    if (isStorageRationale && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        webViewHelper.showNoPermissionMessage(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    } else if (isCameraRationale) {
                        webViewHelper.showNoPermissionMessage(Manifest.permission.CAMERA);
                    } else if (isLocationRationale) {
                        webViewHelper.showNoPermissionMessage(Manifest.permission.ACCESS_COARSE_LOCATION);
                    } else {
                        Log.d(TAG, "do nothing");
                    }
                }
            }
            case 1: {
                int indexOfPostNotification = 0;
                boolean foundNotification = false;
                for (int i = 0; i < permissions.length; i++) {
                    String singlePermission = permissions[i];
                    if (singlePermission.equalsIgnoreCase(Manifest.permission.POST_NOTIFICATIONS)) {
                        indexOfPostNotification = i;
                        foundNotification = true;
                        break;
                    }
                }
                if (foundNotification) {
                    if (grantResults[indexOfPostNotification] == 0) {
                        Log.d(TAG, "do nothing");
                    }
                }
            }
            default:
        }
    }

    @Override
    public void onPause() {
        isInBackGround = true;
        TimeStamp = Calendar.getInstance().getTimeInMillis();
        super.onPause();
    }

    @Override
    public void onStop() {
        if (cookieSyncOn) {
            if (Tools.isDebug())
                Log.d(TAG, "Cookies sync cancelled");
            cookieSyncHandler.removeCallbacks(cookieSyncRunnable);
            onResumeCalled = false;
        }
        if (!sharedPref.getCache()) {
            webView.clearCache(true);
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        }
        super.onStop();
    }

    @Override
    public void onResume() {
        if (Constant.refreshWebPageOnResume) {
            webView.reload();
        }
        if (Constant.enableManualCookieSync && !onResumeCalled) {
            boolean syncCookies = false;
            String url = webView.getUrl();
            int nbTriggers = Constant.manualCookieSyncTriggerUrls.length;
            if (nbTriggers == 0) {
                syncCookies = true;
            } else {
                for (int i = 0; i < nbTriggers; i++) {
                    assert url != null;
                    if (url.startsWith(Constant.manualCookieSyncTriggerUrls[i])) {
                        syncCookies = true;
                        break;
                    }
                }
            }

            if (syncCookies) {
                cookieSyncOn = true;
                if (Tools.isDebug())
                    Log.d(TAG, "Cookies sync on");
                cookieSyncHandler.postDelayed(cookieSyncRunnable = () -> {
                    CookieManager.getInstance().flush();
                    if (Tools.isDebug())
                        Log.d(TAG, "Cookies flushed");
                    cookieSyncHandler.postDelayed(cookieSyncRunnable, Constant.cookieSyncDuration);
                }, Constant.cookieSyncDuration);
            }

            onResumeCalled = true;
        }

        super.onResume();

        isInBackGround = false;
        TimeStamp = Calendar.getInstance().getTimeInMillis();
    }

    @Override
    public void onDestroy() {
        webView.destroy();
        if (!sharedPref.getCache()) {
            webView.clearCache(true);
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        }
        super.onDestroy();
    }

    @SuppressWarnings("unused")
    private class AdvanceWebViewClient extends MyWebViewClient {

        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            webViewHelper.requestLocationPermission(origin, callback);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String url) {
            showOfflineLayout(true);
        }

        @SuppressLint("WebViewClientOnReceivedSslError")
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            if (Constant.blockSelfSignedAndFaultySslCertificate) {
                handler.cancel();
            } else {
                handler.proceed();
            }
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
            Context context = view.getContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            View dialogView = layoutInflater.inflate(R.layout.dialog_credentials, new LinearLayout(context));
            EditText username = dialogView.findViewById(R.id.username);
            EditText password = dialogView.findViewById(R.id.password);

            builder.setView(dialogView)
                    .setTitle(R.string.auth_dialog_title)
                    .setPositiveButton(R.string.submit, null)
                    .setNegativeButton(android.R.string.cancel,
                            (dialog, whichButton) -> handler.cancel())
                    .setOnDismissListener(dialog -> handler.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (TextUtils.isEmpty(username.getText())) {
                    username.setError(getResources().getString(R.string.user_name_required));
                } else if (TextUtils.isEmpty(password.getText())) {
                    password.setError(getResources().getString(R.string.password_name_required));
                } else {
                    handler.proceed(username.getText().toString(), password.getText().toString());
                    dialog.dismiss();
                }
            });
        }

        @SuppressWarnings({ "ConstantValue", "ExtractMethodRecommender" })
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            if (Config.ENABLE_LINEAR_PROGRESS_INDICATOR) {
                showLinearProgressIndicator(true);
            } else {
                swipeRefreshLayout.setRefreshing(true);
            }

            isErrorPageLoaded = false;
            WebSettings webSettings = view.getSettings();

            if (Constant.googleLoginHelperTriggers.length != 0) {
                for (int i = 0; i < Constant.googleLoginHelperTriggers.length; i++) {
                    if (url.startsWith(Constant.googleLoginHelperTriggers[i])) {
                        webSettings.setUserAgentString(Constant.userAgentGoogle);
                        if (windowContainer.getVisibility() == View.VISIBLE) {
                            webViewPopUp.loadUrl(url);
                        } else {
                            view.loadUrl(url);
                        }
                        return true;
                    }
                }
            }

            if (Constant.facebookLoginHelperTriggers.length != 0) {
                for (int i = 0; i < Constant.facebookLoginHelperTriggers.length; i++) {
                    if (url.startsWith(Constant.facebookLoginHelperTriggers[i])) {
                        webSettings.setUserAgentString(Constant.userAgentFacebook);
                        if (windowContainer.getVisibility() == View.VISIBLE) {
                            webViewPopUp.loadUrl(url);
                        } else {
                            view.loadUrl(url);
                        }
                        return true;
                    }
                }
            }

            if (scanningModeOn && !persistentScanningMode) {
                turnOffScanningMode();
            }

            if (url.contains("whatsapp://")) {
                openCustomTabs(url);
                return true;
            }

            if (url.contains("push.send.cancel")) {
                webViewHelper.verifyNotificationPermission(activity);
                if (sharedPref.getUserAgent().contains("VRGl")) {
                    if (url.contains("cartreminderpush.send.cancel")) {
                        stopCartReminderNotification();
                    }
                    if (url.contains("categoryrecommpush.cancel")) {
                        stopCategoryRecommNotification();
                    }
                    if (url.contains("productrecommpush.cancel")) {
                        stopProductRecommNotification();
                    }
                } else {
                    stopNotification();
                }
                return true;
            }
            if (url.contains("push.send")) {
                webViewHelper.verifyNotificationPermission(activity);
                if (sharedPref.getUserAgent().contains("VRGl")) {
                    if (url.contains("cartreminderpush.send")) {
                        sendCartReminderNotification(url);
                    }
                    if (url.contains("categoryrecommpush.send")) {
                        sendCategoryRecommNotification(url);
                    }
                    if (url.contains("productrecommpush.send")) {
                        sendProductRecommNotification(url);
                    }
                } else {
                    sendNotification(url);
                }
                return true;
            }
            if (url.startsWith("getappversion://")) {
                webView.loadUrl("javascript: var versionNumber = '" + Tools.getVersionName() + "';" +
                        "var bundleNumber  = '" + Tools.getVersionCode() + "';");
                return true;
            }
            if (url.startsWith("get-uuid://")) {
                webView.loadUrl("javascript: var uuid = '" + uuid + "';");
                return true;
            }

            if (url.startsWith("hidebars://")) {
                String input = url.substring(url.indexOf('/') + 2);
                WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
                if (controller != null) {
                    if (input.equals("on")) {
                        controller.hide(WindowInsetsCompat.Type.systemBars());
                        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    } else if (input.equals("off")) {
                        controller.show(WindowInsetsCompat.Type.systemBars());
                    }
                }
                return true;
            }

            if (url.startsWith("successhaptic://")) {
                webViewHelper.performHapticFeedback(webView, Constant.HapticChoice.SUCCESS);
                return true;
            }

            if (url.startsWith("errorhaptic://")) {
                webViewHelper.performHapticFeedback(webView, Constant.HapticChoice.ERROR);
                return true;
            }

            if (url.startsWith("lighthaptic://")) {
                webViewHelper.performHapticFeedback(webView, Constant.HapticChoice.LIGHT);
                return true;
            }

            if (url.startsWith("heavyhaptic://")) {
                webViewHelper.performHapticFeedback(webView, Constant.HapticChoice.HEAVY);
                return true;
            }

            if (!Constant.isRedirected) {
                if (Tools.isDebug()) {
                    Log.d(TAG, "shouldOverrideUrlLoading: " + url);
                }
                if (url.startsWith("wc:")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        if (Tools.isDebug()) {
                            Log.d(TAG, "WalletConnect app not found on device; 'wc:' scheme failed");
                        }
                    }
                    return true;
                }
                if (url.startsWith("mailto:")) {
                    startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
                    return true;
                }
                if (url.startsWith("share:") || url.contains("api.whatsapp.com")) {
                    openCustomTabs(url);
                    return true;
                }

                if (url.contains("target=url-checker")) {
                    activity.urlChecker();
                    return true;
                }

                if (url.contains("target=external")) {
                    String cleanUrl = url.replace("?target=external", "").replace("&target=external", "");
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl));
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "No application can handle this request.");
                    }
                    return true;
                }

                if (url.contains("target=video") || url.contains("target=audio") || url.contains("target=image")) {
                    Tools.startIntentChooserActivity(activity, url);
                    return true;
                }

                if (url.contains("package=")) {
                    Tools.startExternalApplication(activity, url);
                    return true;
                }

                if (url.contains("?target=custom_tabs")) {
                    openCustomTabs(url.replace("?target=custom_tabs", ""));
                    return true;
                }

                if (url.contains("play.google.com")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }

                if (url.startsWith("whatsapp:")) {
                    openCustomTabs(url);
                    return true;
                }
                if (url.startsWith("https://maps.google.com")) {
                    webView.loadUrl(url);
                    Tools.checkPermissionAccessLocation(activity, activity.findViewById(R.id.parent_view));
                    return true;
                }
                if (url.startsWith("geo:") || url.contains("maps:")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("market:")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.contains("maps.app.goo.gl")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("intent:")) {
                    webViewHelper.handleIntentUrl(url);
                    return true;
                }
                if (url.startsWith("tel:")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("sms:")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("blob:")) {
                    // Prevent crash
                    return true;
                }
                if (url.startsWith("data:")) {
                    if (Constant.requireStoragePermission) {
                        // File Extension
                        String extension = "ext";

                        if (url.contains("pdf")) { // PDF
                            extension = "pdf";
                        }
                        if (url.contains("spreadsheetml")) { // Excel
                            extension = "xlsx";
                        }
                        if (url.contains("presentationml")) { // PowerPoint
                            extension = "pptx";
                        }
                        if (url.contains("wordprocessingml")) { // Word
                            extension = "docx";
                        }
                        if (url.contains("jpeg")) { // JPEG
                            extension = "jpeg";
                        }
                        if (url.contains("png")) { // PNG
                            extension = "png";
                        }
                        if (url.contains("mp3")) { // MP3
                            extension = "mp3";
                        }
                        if (url.contains("mp4")) { // MP4
                            extension = "mp4";
                        }
                        if (url.contains("m4a")) { // M4A
                            extension = "m4a";
                        }

                        int contentStartIndex = url.indexOf(",") + 1;
                        String encodedContent = url.substring(contentStartIndex);
                        byte[] decodedBytes = Base64.decode(encodedContent, Base64.DEFAULT);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy_HHmmss", Locale.getDefault());
                        String timeStamp = dateFormat.format(new Date());
                        String fileName = "download-" + timeStamp + "." + extension;
                        File downloadsDirectory = Environment
                                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        File file = new File(downloadsDirectory, fileName);
                        try {
                            FileOutputStream fos = new FileOutputStream(file);
                            fos.write(decodedBytes);
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        try {
                            webViewHelper.openDownloadedFile(file);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(activity, "Downloaded to Downloads folder.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(activity, "No Storage Permission", Toast.LENGTH_SHORT).show();

                    }
                    return true;
                }

                // Check if the URL should always open in an in-app tab
                if ((url != null) && shouldAlwaysOpenInAppTab(url)) {
                    openInAppTab(url);
                    return true;
                }

                if (Constant.specialLinkHandlingOptions != 0) {
                    WebView.HitTestResult result = view.getHitTestResult();
                    String data = result.getExtra();
                    if (Tools.isDebug())
                        Log.d(TAG, " data :" + data);

                    if ((data != null && data.endsWith("#")) || url.startsWith("newtab:")) {

                        String finalUrl = url;
                        if (url.startsWith("newtab:")) {
                            finalUrl = url.substring(7);
                        }

                        // Open special link in an in-app tab
                        if ((Constant.specialLinkHandlingOptions == 1) || shouldAlwaysOpenInAppTab(finalUrl)) {
                            openInAppTab(finalUrl);
                            return true;
                            // Open special link in Chrome
                        } else if (Constant.specialLinkHandlingOptions == 2) {
                            view.getContext().startActivity(
                                    new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)));
                            return true;
                        }
                        return false;
                    }
                }

                boolean external = isLinkExternal(url);
                boolean internal = isLinkInternal(url);
                if (!external && !internal) {
                    external = sharedPref.getOpenLinkInExternalBrowser();
                }
                if (external) {
                    Tools.startWebActivity(activity, url);
                    return true;
                }

                // AdMob Policy: Do NOT trigger interstitial ads during in-page navigation of third-party websites
                // to prevent "Valuable inventory: Replicated content" violations.

                return super.shouldOverrideUrlLoading(view, url);
            }
            return false;
        }

    }

    @SuppressWarnings("SpellCheckingInspection")
    private class MyWebViewClient extends WebViewClient {

        MyWebViewClient() {
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            webViewHelper.customCSS(webView);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (!sharedPref.getBuiltInZoomControls()) {
                webView.loadUrl(
                        "javascript:document.getElementsByName('viewport')[0].setAttribute('content', 'initial-scale=1.0, user-scalable=no');");
            }
            if (!Constant.isRedirected) {
                activity.setTitle(view.getTitle());
                webViewHelper.customCSS(webView);
                webViewHelper.customJavaScript(webView);
                if (Constant.autoInjectVariables) {
                    webView.loadUrl("javascript: var versionNumber = '" + Tools.getVersionName() + "';"
                            + "var bundleNumber  = '" + Tools.getVersionCode() + "';");
                    webView.loadUrl("javascript: var uuid = '" + uuid + "';");
                }
                if (!Constant.linkDragAndDrop) {
                    String disableLinkDragScript = "javascript: var links = document.getElementsByTagName('a');" +
                            "for (var i = 0; i < links.length; i++) {" +
                            "   links[i].draggable = false;" +
                            "}";
                    view.loadUrl(disableLinkDragScript);
                }
                super.onPageFinished(view, url);
            }
        }

        @SuppressWarnings({ "MismatchedQueryAndUpdateOfCollection", "ExtractMethodRecommender",
                "ToArrayCallWithZeroLengthArrayArgument" })
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (!Constant.isRedirected) {
                hostPart = Uri.parse(url).getHost();
                if (Tools.isDebug())
                    Log.d(TAG, "should override : " + url);

                if (webViewHelper.isConnectedNetwork()) {
                    if (url.contains(".")
                            && Constant.downloadableExtension.contains(url.substring(url.lastIndexOf(".")))) {
                        webView.stopLoading();
                        String[] PERMISSIONS = { Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE };
                        if (Constant.requireStoragePermission) {
                            if (!webViewHelper.hasPermissions(activity, PERMISSIONS)
                                    && !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)) {
                                if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                    ActivityCompat.requestPermissions(activity, PERMISSIONS, MULTIPLE_PERMISSIONS);
                                }
                            } else {
                                webViewHelper.downloadFile(url);
                            }
                        }
                        return true;
                    }

                    if (!URLisExternal(url)) {
                        return false;

                    } else if (url.startsWith("qrcode://")) {
                        if (Tools.isDebug())
                            Log.d(TAG, url);
                        if (Constant.requireCameraPermission) {
                            if (ContextCompat.checkSelfPermission(activity,
                                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(activity, new String[] { Manifest.permission.CAMERA },
                                        1402);
                            } else {
                                Intent i = new Intent(activity, QrCodeActivity.class);
                                startActivityForResult(i, Constant.requestQrScanCode);
                            }
                        }
                        return true;
                    } else if (url.startsWith("backgroundlocationoff://")) {
                        webViewHelper.toggleBackgroundLocationService(false);
                        return true;
                    } else if (url.startsWith("backgroundlocationon://")) {
                        webViewHelper.toggleBackgroundLocationService(true);
                        return true;
                    }
                    if (url.startsWith("savethisimage://?url=")) {
                        webView.stopLoading();
                        if (webView.canGoBack()) {
                            webView.goBack();
                        }
                        if (Constant.requireStoragePermission) {
                            final String imageUrl = url.substring(url.indexOf("=") + 1);
                            webViewHelper.downloadImageNew("imagesaving", imageUrl);
                        }
                        return true;
                    } else if (url.contains("push.send.cancel")) {
                        if (sharedPref.getUserAgent().contains("VRGl")) {
                            if (url.contains("cartreminderpush.send.cancel")) {
                                stopCartReminderNotification();
                            }
                            if (url.contains("categoryrecommpush.cancel")) {
                                stopCategoryRecommNotification();
                            }
                            if (url.contains("productrecommpush.cancel")) {
                                stopProductRecommNotification();
                            }
                        } else {
                            stopNotification();
                        }
                        return true;
                    } else if (url.contains("push.send")) {
                        if (sharedPref.getUserAgent().contains("VRGl")) {
                            if (url.contains("cartreminderpush.send")) {
                                sendCartReminderNotification(url);
                            }
                            if (url.contains("categoryrecommpush.send")) {
                                sendCategoryRecommNotification(url);
                            }
                            if (url.contains("productrecommpush.send")) {
                                sendProductRecommNotification(url);
                            }
                        } else {
                            sendNotification(url);
                        }
                        return true;
                    } else if (url.startsWith("get-uuid://")) {
                        webView.loadUrl("javascript: var uuid = '" + uuid + "';");
                        return true;
                    } else if (url.startsWith("reset://")) {
                        CookieManager.getInstance().removeAllCookies(null);
                        CookieManager.getInstance().flush();
                        WebSettings webSettings = webView.getSettings();
                        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                        webView.clearCache(true);
                        android.webkit.WebStorage.getInstance().deleteAllData();
                        Toast.makeText(activity, "App reset was successful.", Toast.LENGTH_LONG).show();
                        loadMainUrl(pageUrl);
                        return true;
                    } else if (url.startsWith("readnfc://")) {
                        readModeNFC = true;
                        writeModeNFC = false;
                        return true;
                    } else if (url.startsWith("writenfc://")) {
                        writeModeNFC = true;
                        readModeNFC = false;
                        textToWriteNFC = url.substring(url.indexOf("=") + 1);
                        return true;
                    } else if (url.startsWith("spinneron://")) {
                        linearProgressIndicator.setVisibility(View.VISIBLE);
                        return true;
                    } else if (url.startsWith("spinneroff://")) {
                        linearProgressIndicator.setVisibility(View.GONE);
                        return true;
                    } else if (url.startsWith("takescreenshot://")) {
                        webViewHelper.verifyStoragePermissions(activity);
                        Toast.makeText(activity, "Screenshot Saved", Toast.LENGTH_LONG).show();
                        WebViewHelper.screenshot(activity.getWindow().getDecorView().getRootView(), "result");
                        return true;
                    } else if (url.startsWith("getappversion://")) {
                        webView.loadUrl("javascript: var versionNumber = '" + Tools.getVersionName() + "';"
                                + "var bundleNumber  = '" + Tools.getVersionCode() + "';");
                        return true;
                    } else if (url.startsWith("shareapp://")) {
                        String inputString = url.substring(20);
                        String delimiter = "&url=";
                        String[] components = inputString.split(delimiter);
                        String message2;
                        String url2 = "";
                        if (components.length > 1) {
                            message2 = components[0];
                            url2 = components[1];
                        } else {
                            message2 = inputString;
                        }
                        String message1 = message2.replace("%20", " ");
                        String url1 = url2.replace("%20", " ");

                        String totalMessage;
                        if (message1.isEmpty()) {
                            totalMessage = url1;
                        } else if (url1.isEmpty()) {
                            totalMessage = message1;
                        } else {
                            totalMessage = message1 + "\n" + url1;
                        }

                        List<String> objectsToShare = new ArrayList<>();
                        objectsToShare.add(totalMessage);

                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_TEXT, totalMessage);

                        Intent chooser = Intent.createChooser(intent, "Share via");
                        chooser.setFlags(FLAG_ACTIVITY_NEW_TASK);

                        List<LabeledIntent> intents = new ArrayList<>();
                        for (ResolveInfo info : activity.getPackageManager().queryIntentActivities(intent, 0)) {
                            Intent target = new Intent(Intent.ACTION_SEND);
                            target.setType("text/plain");
                            target.putExtra(Intent.EXTRA_TEXT, totalMessage);
                            target.setPackage(info.activityInfo.packageName);
                            intents.add(new LabeledIntent(target, info.activityInfo.packageName,
                                    info.loadLabel(activity.getPackageManager()), info.icon));
                        }

                        Parcelable[] extraIntents = intents.toArray(new Parcelable[intents.size()]);
                        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);
                        startActivity(chooser);

                        return true;
                    } else if (url.startsWith("scanningmode://")) {
                        String input = url.substring(url.indexOf('/') + 2);
                        switch (input) {
                            case "auto" -> turnOnScanningMode();
                            case "on" -> {
                                persistentScanningMode = true;
                                turnOnScanningMode();
                            }
                            case "off" -> {
                                persistentScanningMode = false;
                                turnOffScanningMode();
                            }
                        }
                        return true;
                    }
                } else if (!webViewHelper.isConnectedNetwork()) {
                    showOfflineLayout(true);
                    return true;
                }

                if (hostPart.contains("whatsapp.com")) {
                    final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    final int newDocumentFlag = Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
                    intent.addFlags(FLAG_ACTIVITY_NO_HISTORY | newDocumentFlag | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    startActivity(intent);
                }

                for (String whitelistedLink : Constant.browserWithList) {
                    if (hostPart.contains(whitelistedLink)) {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(i);
                        return true;
                    }
                }

                if (((Constant.externalLinkHandlingOptions != 0) && !(url).startsWith("file://"))
                        && URLUtil.isValidUrl(url)) {
                    if (Constant.externalLinkHandlingOptions == 1) {
                        for (String blacklistedLink : Constant.neverOpenInAppTab) {
                            if (blacklistedLink.contains(hostPart)) {
                                return false;
                            }
                        }
                        openInAppTab(url);
                        return true;
                    } else if (Constant.externalLinkHandlingOptions == 2) {
                        for (String blacklistedLink : Constant.browserBlacklist) {
                            if (blacklistedLink.contains(hostPart)) {
                                return false;
                            }
                        }
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(i);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return false;
        }
    }

    private void turnOnScanningMode() {
        if (!scanningModeOn) {
            WindowManager.LayoutParams layout = activity.getWindow().getAttributes();
            previousScreenBrightness = layout.screenBrightness;
            scanningModeOn = true;
            layout.screenBrightness = 1F;
            activity.getWindow().setAttributes(layout);
            if (!Constant.preventAppSleep) {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }

    private void turnOffScanningMode() {
        if (scanningModeOn) {
            WindowManager.LayoutParams layout = activity.getWindow().getAttributes();
            scanningModeOn = false;
            layout.screenBrightness = previousScreenBrightness;
            activity.getWindow().setAttributes(layout);
            if (!Constant.preventAppSleep) {
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    private void sendNotification(String url) {
        final int secondsDelayed = Integer.parseInt(url.split("=")[1]);

        final String[] contentDetails = (url.substring((url.indexOf("msg!") + 4))).split("&!#");
        String message = contentDetails[0].replaceAll("%20", " ");
        String title = contentDetails[1].replaceAll("%20", " ");

        try {
            message = URLDecoder.decode(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            title = URLDecoder.decode(title, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String urlToOpen = null;
        if (contentDetails.length > 2) {
            urlToOpen = contentDetails[2].replaceAll("%20", " ");
        }

        final Notification.Builder builder = webViewHelper.getNotificationBuilder(title, message, urlToOpen);
        final Notification notification = builder.build();
        final NotificationManager notificationManager = (NotificationManager) activity
                .getSystemService(NOTIFICATION_SERVICE);

        notificationHandler = new Handler();
        notificationHandler.postDelayed(() -> {
            notificationManager.notify(0, notification);
            notificationHandler = null;
        }, secondsDelayed * 1000L);
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    private void sendCartReminderNotification(String url) {
        final int secondsDelayed = Integer.parseInt(url.split("=")[1]);
        final String[] contentDetails = (url.substring((url.indexOf("msg!") + 4))).split("&!#");
        String message = contentDetails[0].replaceAll("%20", " ");
        String title = contentDetails[1].replaceAll("%20", " ");
        try {
            message = URLDecoder.decode(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            title = URLDecoder.decode(title, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String urlToOpen = null;
        if (contentDetails.length > 2) {
            urlToOpen = contentDetails[2].replaceAll("%20", " ");
        }
        final Notification.Builder builder = webViewHelper.getNotificationBuilder(title, message, urlToOpen);
        final Notification notification = builder.build();
        final NotificationManager notificationManager = (NotificationManager) activity
                .getSystemService(NOTIFICATION_SERVICE);
        CartRemindernotificationHandler = new Handler();
        CartRemindernotificationHandler.postDelayed(() -> {
            notificationManager.notify(0, notification);
            CartRemindernotificationHandler = null;
        }, secondsDelayed * 1000L);
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    private void sendCategoryRecommNotification(String url) {
        final int secondsDelayed = Integer.parseInt(url.split("=")[1]);
        final String[] contentDetails = (url.substring((url.indexOf("msg!") + 4))).split("&!#");
        String message = contentDetails[0].replaceAll("%20", " ");
        String title = contentDetails[1].replaceAll("%20", " ");
        try {
            message = URLDecoder.decode(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            title = URLDecoder.decode(title, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String urlToOpen = null;
        if (contentDetails.length > 2) {
            urlToOpen = contentDetails[2].replaceAll("%20", " ");
        }

        final Notification.Builder builder = webViewHelper.getNotificationBuilder(title, message, urlToOpen);
        final Notification notification = builder.build();
        final NotificationManager notificationManager = (NotificationManager) activity
                .getSystemService(NOTIFICATION_SERVICE);

        CategoryRecommNotificationHandler = new Handler();
        CategoryRecommNotificationHandler.postDelayed(() -> {
            notificationManager.notify(0, notification);
            CategoryRecommNotificationHandler = null;
        }, secondsDelayed * 1000L);
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    private void sendProductRecommNotification(String url) {
        final int secondsDelayed = Integer.parseInt(url.split("=")[1]);
        final String[] contentDetails = (url.substring((url.indexOf("msg!") + 4))).split("&!#");
        String message = contentDetails[0].replaceAll("%20", " ");
        String title = contentDetails[1].replaceAll("%20", " ");
        try {
            message = URLDecoder.decode(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            title = URLDecoder.decode(title, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String urlToOpen = null;
        if (contentDetails.length > 2) {
            urlToOpen = contentDetails[2].replaceAll("%20", " ");
        }
        final Notification.Builder builder = webViewHelper.getNotificationBuilder(title, message, urlToOpen);
        final Notification notification = builder.build();
        final NotificationManager notificationManager = (NotificationManager) activity
                .getSystemService(NOTIFICATION_SERVICE);
        ProductRecommNotificationHandler = new Handler();
        ProductRecommNotificationHandler.postDelayed(() -> {
            notificationManager.notify(0, notification);
            ProductRecommNotificationHandler = null;
        }, secondsDelayed * 1000L);
    }

    private void stopNotification() {
        if (notificationHandler != null) {
            notificationHandler.removeCallbacksAndMessages(null);
            notificationHandler = null;
        }
    }

    private void stopCartReminderNotification() {
        if (CartRemindernotificationHandler != null) {
            CartRemindernotificationHandler.removeCallbacksAndMessages(null);
            CartRemindernotificationHandler = null;
        }
    }

    private void stopCategoryRecommNotification() {
        if (CategoryRecommNotificationHandler != null) {
            CategoryRecommNotificationHandler.removeCallbacksAndMessages(null);
            CategoryRecommNotificationHandler = null;
        }
    }

    private void stopProductRecommNotification() {
        if (ProductRecommNotificationHandler != null) {
            ProductRecommNotificationHandler.removeCallbacksAndMessages(null);
            ProductRecommNotificationHandler = null;
        }
    }

    private class AdvanceWebChromeClient extends MyWebChromeClient {

        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            webViewHelper.requestLocationPermission(origin, callback);
        }

        @Override
        public void onCloseWindow(WebView window) {
            super.onCloseWindow(window);
            closePopupWindow();
        }

        @SuppressLint("SetJavaScriptEnabled")
        @SuppressWarnings("AccessStaticViaInstance")
        @Override
        public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {

            WebView.HitTestResult result = view.getHitTestResult();
            String data = result.getExtra();

            if (result.getType() == result.SRC_IMAGE_ANCHOR_TYPE) {
                Message href = view.getHandler().obtainMessage();
                view.requestFocusNodeHref(href);
                data = href.getData().getString("url");
            }

            if ((data != null) && shouldAlwaysOpenInAppTab(data)) {
                openInAppTab(data);
                return true;
            }

            if (Constant.specialLinkHandlingOptions == 0) {

                if (Tools.isDebug())
                    Log.d(TAG, "if ");

                if ((data == null) || (data != null && data.endsWith("#"))) {
                    if (Tools.isDebug())
                        Log.d(TAG, "else true ");
                    windowContainer.setVisibility(View.VISIBLE);
                    webViewPopUp = new WebView(view.getContext());
                    webViewHelper.webViewSettings(webViewPopUp);

                    webViewPopUp.setWebChromeClient(new AdvanceWebChromeClient());
                    webViewPopUp.setWebViewClient(new AdvanceWebViewClient());
                    webViewPopUp.getSettings()
                            .setUserAgentString(webViewPopUp.getSettings().getUserAgentString().replace("wv", ""));
                    mContainer.addView(webViewPopUp);

                    WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                    transport.setWebView(webViewPopUp);
                    resultMsg.sendToTarget();
                    return true;
                } else {

                    WebSettings webSettings = webView.getSettings();
                    webSettings.setJavaScriptEnabled(true);
                    webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                    webSettings.setSupportMultipleWindows(true);

                    if (URLUtil.isValidUrl(data)) {
                        webView.loadUrl(data);
                    }
                }

            } else if (Constant.specialLinkHandlingOptions == 1) {
                if (data == null) {
                    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                    builder.setToolbarColor(ContextCompat.getColor(activity, R.color.color_light_status_bar));
                    CustomTabsIntent customTabsIntent = builder.build();
                    WebView newWebView = new WebView(view.getContext());
                    WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                    transport.setWebView(newWebView);
                    resultMsg.sendToTarget();
                    newWebView.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                            CookieManager cookieManager = CookieManager.getInstance();
                            String allCookies = cookieManager.getCookie(Uri.parse(url).toString());
                            if (allCookies != null) {
                                String[] cookieList = allCookies.split(";");
                                for (String cookie : cookieList) {
                                    customTabsIntent.intent.putExtra("android.webkit.CookieManager.COOKIE",
                                            cookie.trim());
                                }
                            }
                            customTabsIntent.launchUrl(activity, Uri.parse(url));
                            webView.stopLoading();
                            return false;
                        }
                    });
                } else {
                    openInAppTab(data);
                }

            } else if (Constant.specialLinkHandlingOptions == 2) {
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setToolbarColor(ContextCompat.getColor(activity, R.color.color_light_status_bar));
                if (Tools.isDebug())
                    Log.d("TAG", " data " + data);
                WebView newWebView = new WebView(view.getContext());
                newWebView.setWebChromeClient(new WebChromeClient());
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
            }
            if (Tools.isDebug())
                Log.d("TAG", " running this main activity ");
            return true;
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            if (Tools.isDebug())
                Log.d(TAG, " onJsalert");
            return super.onJsAlert(view, url, message, result);
        }

        @SuppressLint("InlinedApi")
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                FileChooserParams fileChooserParams) {

            boolean hasStoragePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                    (ContextCompat.checkSelfPermission(activity,
                            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(activity,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);

            boolean hasCameraPermission = ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

            mUMA = filePathCallback;
            if (hasStoragePermission && hasCameraPermission) {
                webViewHelper.openFilePicker(fileChooserParams);
            } else {
                webChromeClientFileChooserParams = fileChooserParams;
                if (Tools.isDebug())
                    Log.d(TAG, "File Chooser permissions not granted - requesting permissions");
                ArrayList<String> permissionList = new ArrayList<>();
                if (!hasCameraPermission && Constant.requireCameraPermission) {
                    permissionList.add(Manifest.permission.CAMERA);
                }

                if (!hasStoragePermission && Constant.requireStoragePermission) {
                    permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                    permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }

                if (!permissionList.isEmpty()) {
                    requestPermissions(permissionList.toArray(new String[0]), REQUEST_PERMISSION_STORAGE_CAMERA);
                }
            }
            return true;
        }

    }

    WebChromeClient.FileChooserParams webChromeClientFileChooserParams;

    private class MyWebChromeClient extends WebChromeClient {

        private CustomViewCallback mCustomViewCallback;

        MyWebChromeClient() {

        }

        public Bitmap getDefaultVideoPoster() {
            return BitmapFactory.decodeResource(activity.getResources(), 2130837573);
        }

        public void onHideCustomView() {
            customViewContainer.removeAllViews();
            customViewContainer.setVisibility(View.GONE);
            Tools.fullScreenMode(activity, false);
            this.mCustomViewCallback.onCustomViewHidden();
            this.mCustomViewCallback = null;
            webView.clearFocus();
            Log.d(TAG, "onHideCustomView");
        }

        public void onShowCustomView(View view, CustomViewCallback paramCustomViewCallback) {
            customViewContainer.addView(view);
            customViewContainer.setVisibility(View.VISIBLE);
            Tools.fullScreenMode(activity, true);
            this.mCustomViewCallback = paramCustomViewCallback;
            Log.d(TAG, "onShowCustomView");
        }

        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            webViewHelper.requestLocationPermission(origin, callback);
            callback.invoke(origin, true, false);
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                linearProgressIndicator.setProgress(newProgress, true);
            } else {
                linearProgressIndicator.setProgress(newProgress);
            }
            Constant.isRedirected = true;
            if (newProgress == 100) {
                Constant.isRedirected = false;
                Tools.postDelayed(() -> {
                    linearProgressIndicator.setVisibility(View.INVISIBLE);
                    swipeRefreshLayout.setRefreshing(false);
                    linearProgressIndicator.setProgress(0);
                }, 1000);
            } else {
                linearProgressIndicator.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            permissionRequest = request;
            for (String permission : request.getResources()) {
                if (Constant.requireRecordAudioPermission
                        && permission.equalsIgnoreCase(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                    webViewHelper.askForPermission(permissionRequest, Manifest.permission.RECORD_AUDIO,
                            WebViewHelper.WEBVIEW_PERMISSION_REQUEST);
                }
                if (Constant.requireCameraPermission
                        && permission.equalsIgnoreCase(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                    webViewHelper.askForPermission(permissionRequest, Manifest.permission.CAMERA,
                            WebViewHelper.WEBVIEW_PERMISSION_REQUEST);
                }
                Log.e(TAG, "onPermissionRequest: " + permission);
            }
        }
    }

    private void initNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter == null) {
            Toast.makeText(activity, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            activity.finish();
        } else {
            webViewHelper.readFromIntent(webView, activity.getIntent());
            pendingIntent = PendingIntent.getActivity(activity, 0,
                    new Intent(activity, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_MUTABLE);
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            tagDetected.addCategory(CATEGORY_DEFAULT);
            writeTagFilters = new IntentFilter[] { tagDetected };
        }
    }

    public void closePopupWindow() {
        linearProgressIndicator.setVisibility(View.GONE);
        mContainer.removeAllViews();
        windowContainer.setVisibility(View.GONE);
        webViewPopUp.destroy();

    }

    private void setOfflineScreenBackgroundColor() {
        if (sharedPref.getIsDarkTheme()) {
            lytOffline.setBackgroundColor(ContextCompat.getColor(activity, R.color.color_dark_background));
        } else {
            lytOffline.setBackgroundColor(ContextCompat.getColor(activity, R.color.color_light_background));
        }
    }

    @SuppressWarnings("StringEquality")
    private boolean shouldAlwaysOpenInAppTab(String URL) {
        for (int i = 0; i < Constant.alwaysOpenInAppTab.length; i++) {
            if ((Constant.alwaysOpenInAppTab[i] != "") && (URL.startsWith(Constant.alwaysOpenInAppTab[i]))) {
                return true;
            }
        }
        return false;
    }

    private void openInAppTab(String URL) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(ContextCompat.getColor(activity, R.color.color_light_status_bar));
        CustomTabsIntent customTabsIntent = builder.build();
        CookieManager cookieManager = CookieManager.getInstance();
        String allCookies = cookieManager.getCookie(URL);
        if (allCookies != null) {
            String[] cookieList = allCookies.split(";");
            for (String cookie : cookieList) {
                customTabsIntent.intent.putExtra("android.webkit.CookieManager.COOKIE", cookie.trim());
            }
        }
        customTabsIntent.launchUrl(activity, Uri.parse(URL));
        webView.stopLoading();
    }

    private void openInNewBrowser(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(i);
    }

    public void onWebViewCanGoBack() {
        if (windowContainer.getVisibility() == View.VISIBLE) {
            closePopupWindow();
        } else if (webView.canGoBack() && !isErrorPageLoaded) {
            webView.goBack();
            lytProgress.setVisibility(View.GONE);
        } else {
            activity.loadHomeOrExit();
        }
    }

    private void showLinearProgressIndicator(boolean show) {
        if (show) {
            lytProgress.setVisibility(View.VISIBLE);
        } else {
            lytProgress.setVisibility(View.GONE);
        }
    }

    private void showOfflineLayout(boolean show) {
        if (show) {
            lytOffline.setVisibility(View.VISIBLE);
        } else {
            lytOffline.setVisibility(View.GONE);
        }
    }

    private boolean isLinkExternal(String url) {
        for (String rule : Tools.LINKS_OPENED_IN_EXTERNAL_BROWSER) {
            if (url.contains(rule))
                return true;
        }
        return false;
    }

    private boolean isLinkInternal(String url) {
        for (String rule : Tools.LINKS_OPENED_IN_INTERNAL_WEBVIEW) {
            if (url.contains(rule))
                return true;
        }
        return false;
    }

    private void openCustomTabs(String url) {
        CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
        intent.launchUrl(activity, Uri.parse(url));
    }

}