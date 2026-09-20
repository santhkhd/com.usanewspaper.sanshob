package com.app.webdroid.webview;

import static android.content.Context.DOWNLOAD_SERVICE;
import static android.content.Intent.CATEGORY_DEFAULT;
import static android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NO_HISTORY;
import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.shobmc.san.R;
import com.app.webdroid.activity.MainActivity;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.fragment.FragmentWebView;
import com.app.webdroid.util.Constant;
import com.app.webdroid.util.GPSService;
import com.app.webdroid.util.Tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("unused")
public class WebViewHelper {

    Activity activity;
    private static final String TAG = "WebViewHelper";
    private String mCM, mVM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;
    private final static int FCR = 1;
    public static final int CODE_AUDIO_CHOOSER = 5678;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 5454;
    public static final int WEBVIEW_PERMISSION_REQUEST = 846;
    private static final String[] permissionStorage = { Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE };
    SharedPref sharedPref;

    public WebViewHelper(Activity activity) {
        this.activity = activity;
        this.sharedPref = new SharedPref(activity);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void initWebSettings(CustomWebView webView) {
        final WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setSupportZoom(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webSettings.setSafeBrowsingEnabled(true);
        }
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        if (sharedPref.getBuiltInZoomControls()) {
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
        } else {
            webSettings.setBuiltInZoomControls(false);
        }
        if (!sharedPref.getCache()) {
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        } else {
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowFileAccess(true);
        // webSettings.setLoadWithOverviewMode(true);
        // webSettings.setUseWideViewPort(true);
        webSettings.setAllowContentAccess(true);

        webSettings.setDatabaseEnabled(true);

        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Custom Text Zoom
        if (Constant.maxTextZoom > 0) {
            float systemTextZoom = activity.getResources().getConfiguration().fontScale * 100;
            if (systemTextZoom > Constant.maxTextZoom) {
                webView.getSettings().setTextZoom(Constant.maxTextZoom);
            }
        }

        // Phone orientation setting for Android 8 (Oreo)
        if (webSettings.getUserAgentString().contains("Mobile") && Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            if (Constant.phoneOrientation == "auto") {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            } else if (Constant.phoneOrientation == "portrait") {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
            } else if (Constant.phoneOrientation == "landscape") {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
            }
            // Phone orientation setting for all other Android versions
        } else if (webSettings.getUserAgentString().contains("Mobile")) {
            if (Constant.phoneOrientation == "auto") {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            } else if (Constant.phoneOrientation == "portrait") {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else if (Constant.phoneOrientation == "landscape") {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
            // Tablet/Other orientation setting
        } else {
            if (Constant.tabletOrientation == "auto") {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            } else if (Constant.tabletOrientation == "portrait") {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else if (Constant.tabletOrientation == "landscape") {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }

        if (!sharedPref.getUserAgent().isEmpty()) {
            webSettings.setUserAgentString(sharedPref.getUserAgent());
        }

        if (!sharedPref.getCache()) {
            webView.clearCache(true);
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void webViewSettings(WebView intWebView) {

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(intWebView, true);

        WebSettings webSettings = intWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        if (!sharedPref.getCache()) {
            // webSettings.setAppCacheEnabled(false);
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        } else {
            // webSettings.setAppCacheEnabled(true);
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(intWebView, true);
        intWebView.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webSettings.setSupportMultipleWindows(true);
        webSettings.setUseWideViewPort(true);

        if (!sharedPref.getUserAgent().isEmpty()) {
            webSettings.setUserAgentString(webSettings.getUserAgentString().replace("wv", ""));
        }

    }

    public void openFilePicker(WebChromeClient.FileChooserParams fileChooserParams) {

        if (Arrays.asList(fileChooserParams.getAcceptTypes()).contains("audio/*")) {
            Intent chooserIntent = fileChooserParams.createIntent();
            activity.startActivityForResult(chooserIntent, CODE_AUDIO_CHOOSER);
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
                takePictureIntent.putExtra("PhotoPath", mCM);
            } catch (IOException ex) {
                if (Tools.isDebug())
                    Log.d(TAG, "Image file creation failed", ex);
            }
            if (photoFile != null) {
                mCM = "file:" + photoFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        FileProvider.getUriForFile(activity, activity.getPackageName() + ".provider", photoFile));
            } else {
                takePictureIntent = null;
            }
        }
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(activity.getPackageManager()) != null) {
            File videoFile = null;
            try {
                videoFile = createVideoFile();
                takeVideoIntent.putExtra("PhotoPath", mVM);
            } catch (IOException ex) {
                if (Tools.isDebug())
                    Log.d(TAG, "Video file creation failed", ex);
            }
            if (videoFile != null) {
                mVM = "file:" + videoFile.getAbsolutePath();
                takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        FileProvider.getUriForFile(activity, activity.getPackageName() + ".provider", videoFile));
            } else {
                takeVideoIntent = null;
            }
        }

        Intent contentSelectionIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        contentSelectionIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/* video/*");

        String[] mimeTypes = { "text/csv", "text/comma-separated-values", "application/pdf", "image/*", "video/*",
                "*/*" };
        contentSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        Intent[] intentArray;
        if (takePictureIntent != null && takeVideoIntent != null) {
            intentArray = new Intent[] { takePictureIntent, takeVideoIntent };
        } else if (takePictureIntent != null) {
            intentArray = new Intent[] { takePictureIntent };
        } else if (takeVideoIntent != null) {
            intentArray = new Intent[] { takeVideoIntent };
        } else {
            intentArray = new Intent[0];
        }

        int pickerMode = Constant.filePickerModePermission;
        boolean shouldUseCamera = pickerMode == 0 || pickerMode == 1;
        boolean shouldUseGallery = pickerMode == 0 || pickerMode == 2;
        if (shouldUseGallery) {
            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Upload");
            if (shouldUseCamera) {
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
            }
            activity.startActivityForResult(chooserIntent, FCR);
        } else if (shouldUseCamera) {
            Intent chooserIntent = Intent.createChooser(takePictureIntent, "Capture Image or Video");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { takeVideoIntent });
            activity.startActivityForResult(chooserIntent, FCR);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "";
        File mediaStorageDir = activity.getCacheDir();
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                if (Tools.isDebug())
                    Log.d(TAG, "Oops! Failed create " + "WebView" + " directory");
                return null;
            }
        }
        return File.createTempFile(
                imageFileName,
                ".jpg",
                mediaStorageDir);
    }

    private File createVideoFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "VID_" + timeStamp + "";
        File mediaStorageDir = activity.getCacheDir();

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                if (Tools.isDebug())
                    Log.d(TAG, "Oops! Failed create " + "WebView" + " directory");
                return null;
            }
        }
        return File.createTempFile(
                imageFileName,
                ".mp4",
                mediaStorageDir);
    }

    /**
     * @noinspection CharsetObjectCanBeUsed
     */
    public NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        if (text.startsWith("VCARD")) {
            String nameVcard = "BEGIN:" +
                    text.replace('_', '\n').replace("%20", " ")
                    + '\n' + "END:VCARD";
            byte[] uriField = nameVcard.getBytes(StandardCharsets.US_ASCII);
            byte[] payload = new byte[uriField.length + 1];
            System.arraycopy(uriField, 0, payload, 1, uriField.length);
            return new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "text/vcard".getBytes(), new byte[0], payload);
        }
        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];
        payload[0] = (byte) langLength;
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
    }

    public String sanitizeURL(String url) {
        String localURL = url.replaceAll("[^-%=&|?_.a-zA-Z\\d/:]", "");
        return applyCSP(localURL);
    }

    public String applyCSP(String url) {
        String cspHeaderValue = "default-src 'self'; script-src 'self' 'unsafe-inline'; object-src 'none'; style-src 'self' 'unsafe-inline'; img-src 'self'; media-src 'self'; frame-src 'none'; font-src 'self'; connect-src 'self';";
        return url + "?CSP_HEADER=" + Uri.encode(cspHeaderValue);
    }

    public void requestLocationPermission(String origin, GeolocationPermissions.Callback callback) {
        if (!sharedPref.getGeolocation()) {
            return;
        }
        FragmentWebView.mGeolocationOrigin = null;
        FragmentWebView.mGeolocationCallback = null;

        // If we don't have location permissions, we must request them first
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] locationPermissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                locationPermissions = new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        // Manifest.permission.ACCESS_BACKGROUND_LOCATION
                };
            } else {
                locationPermissions = new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                };
            }

            // Show rationale if necessary
            boolean fineRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            boolean coarseRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION);
            // boolean bgLocationRationale =
            // ActivityCompat.shouldShowRequestPermissionRationale(activity,
            // Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            boolean bgLocationRationale = false;
            Log.e(TAG, "requestLocationPermission: " + fineRationale + " : " + coarseRationale + " : "
                    + bgLocationRationale);
            if (fineRationale || coarseRationale || bgLocationRationale) {
                new AlertDialog.Builder(activity)
                        .setMessage(activity.getString(R.string.requires_location_permission))
                        .setNeutralButton(android.R.string.ok, (dialogInterface, i) -> {
                            FragmentWebView.mGeolocationOrigin = origin;
                            FragmentWebView.mGeolocationCallback = callback;
                            ActivityCompat.requestPermissions(activity,
                                    locationPermissions, LOCATION_PERMISSION_REQUEST_CODE);
                        })
                        .show();
            } else {
                FragmentWebView.mGeolocationOrigin = origin;
                FragmentWebView.mGeolocationCallback = callback;
                ActivityCompat.requestPermissions(activity,
                        locationPermissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }
        // Otherwise just tell webview that permission has been granted
        else {
            callback.invoke(origin, true, false);
        }
    }

    public void askForPermission(PermissionRequest permissionRequest, String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                if (permission.equalsIgnoreCase(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                    showNoPermissionMessage(Manifest.permission.RECORD_AUDIO);
                } else if (permission.equalsIgnoreCase(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                    showNoPermissionMessage(Manifest.permission.CAMERA);
                }
            } else {
                ActivityCompat.requestPermissions(activity, new String[] { permission }, requestCode);
            }
        } else {
            permissionRequest.grant(permissionRequest.getResources());
        }
    }

    public void showNoPermissionMessage(String permission) {
        String message = "";
        switch (permission) {
            case Manifest.permission.RECORD_AUDIO:
                message = "audio";
                break;
            case Manifest.permission.CAMERA:
                message = "camera";
                break;
            case Manifest.permission.READ_EXTERNAL_STORAGE:
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                message = "storage";
                break;
            case Manifest.permission.ACCESS_COARSE_LOCATION:
            case Manifest.permission.ACCESS_FINE_LOCATION:
                message = "location";
                break;
        }

        new AlertDialog.Builder(activity)
                .setTitle("Permission Error")
                .setMessage(activity.getString(R.string.no_camera_permission, message))
                .setPositiveButton("Go to settings", (dialog, which) -> {
                    Intent intent = new Intent(ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", activity.getApplicationContext().getPackageName(), null));
                    intent.addCategory(CATEGORY_DEFAULT);
                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(FLAG_ACTIVITY_NO_HISTORY);
                    intent.addFlags(FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    activity.startActivity(intent);
                })
                .setCancelable(false)
                .show();
    }

    public void readFromIntent(CustomWebView webView, Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];

                }
            }
            read(webView, msgs);
        }
    }

    private void read(CustomWebView webView, NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0)
            return;
        String text = "";
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 0063;
        try {
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
            webView.loadUrl("javascript: readNFCResult('" + text + "');");
        } catch (UnsupportedEncodingException e) {
            if (Tools.isDebug())
                Log.d("UnsupportedEncoding", e.toString());
        }
        TextView textView = new TextView(activity);
        textView.setPadding(16, 16, 16, 16);
        textView.setTextColor(Color.BLUE);
        textView.setText("read : " + text);
    }

    public void customCSS(CustomWebView webView) {
        try {
            InputStream inputStream = activity.getAssets().open("custom.css");
            byte[] cssbuffer = new byte[inputStream.available()];
            inputStream.read(cssbuffer);
            inputStream.close();

            String encodedcss = Base64.encodeToString(cssbuffer, Base64.NO_WRAP);
            if (!TextUtils.isEmpty(encodedcss)) {
                if (Tools.isDebug())
                    Log.d("css", "Custom CSS loaded");
                webView.loadUrl("javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "style.innerHTML = window.atob('" + encodedcss + "');" +
                        "parent.appendChild(style)" +
                        "})()");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void customJavaScript(CustomWebView webView) {
        try {
            InputStream inputStream = activity.getAssets().open("custom.js");
            byte[] jsBuffer = new byte[inputStream.available()];
            inputStream.read(jsBuffer);
            inputStream.close();

            String encodedJs = Base64.encodeToString(jsBuffer, Base64.NO_WRAP);
            if (!TextUtils.isEmpty(encodedJs)) {
                if (Tools.isDebug())
                    Log.d(TAG, "Custom Javascript loaded");
                webView.loadUrl("javascript:(function() {" +
                        "var customJsCode = window.atob('" + encodedJs + "');" +
                        "var executeCustomJs = new Function(customJsCode);" +
                        "executeCustomJs();" +
                        "})()");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openDownloadedFile(File file) {
        Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent chooser = Intent.createChooser(intent, "App");
        try {
            activity.startActivity(chooser);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, activity.getResources().getString(R.string.download_no_app), Toast.LENGTH_LONG)
                    .show();
            e.printStackTrace();
        }
    }

    @SuppressLint("WrongConstant")
    public void downloadFile(String url) {
        try {
            boolean isICSFile = false;
            String fileName = getFileNameFromURL(url);
            if (fileName.endsWith(".ics")) {
                isICSFile = true;
            } else {
                Toast.makeText(activity, "Downloading file...", Toast.LENGTH_SHORT).show();
            }
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            String cookie = CookieManager.getInstance().getCookie(url);
            request.addRequestHeader("Cookie", cookie);
            request.allowScanningByMediaScanner();
            if (isICSFile) {
                request.setVisibleInDownloadsUi(false);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            } else {
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            }
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            DownloadManager dm = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
        } catch (Exception e) {
            e.printStackTrace();
        }

        BroadcastReceiver onComplete = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    openDownloadedAttachment(activity, downloadId);
                }
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    ContextCompat.RECEIVER_NOT_EXPORTED);
        }
    }

    public static String getFileNameFromURL(String url) {
        if (url == null) {
            return "";
        }
        try {
            URL resource = new URL(url);
            String host = resource.getHost();
            if (host.length() > 0 && url.endsWith(host)) {
                return "";
            }
        } catch (MalformedURLException e) {
            return "";
        }

        int startIndex = url.lastIndexOf('/') + 1;
        int length = url.length();

        int lastQMPos = url.lastIndexOf('?');
        if (lastQMPos == -1) {
            lastQMPos = length;
        }

        int lastHashPos = url.lastIndexOf('#');
        if (lastHashPos == -1) {
            lastHashPos = length;
        }

        int endIndex = Math.min(lastQMPos, lastHashPos);
        return url.substring(startIndex, endIndex);
    }

    @SuppressLint("Range")
    private void openDownloadedAttachment(final Context context, final long downloadId) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);

        if (cursor.moveToFirst()) {
            int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            String downloadLocalUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            String downloadMimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));

            if ((downloadStatus == DownloadManager.STATUS_SUCCESSFUL) && downloadLocalUri != null) {
                if (Tools.isDebug())
                    Log.d("texts", "Download done");
                if (downloadMimeType.equalsIgnoreCase("text/calendar")) {
                    openDownloadedAttachment(context, Uri.parse(downloadLocalUri), downloadMimeType);
                } else {
                    Toast.makeText(context, "Saved to SD card", Toast.LENGTH_LONG).show();
                }
            }
        }
        cursor.close();
    }

    private void openDownloadedAttachment(Context context, Uri downloadedUri, String downloadedMimeType) {
        if (downloadedMimeType.equalsIgnoreCase("text/calendar")) {
            try {
                openCalenderApp(context, downloadedUri, downloadedMimeType);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void openCalenderApp(Context context, Uri downloadedUri, String downloadedMimeType) {

        File downloadedFile = new File(downloadedUri.getPath());
        Uri contentURI = FileProvider.getUriForFile(context,
                context.getApplicationContext().getPackageName() + ".provider", downloadedFile);

        Log.e(TAG, "openCalenderApp: uri: " + contentURI);
        Log.e(TAG, "openCalenderApp: mimeType: " + downloadedMimeType);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(contentURI, downloadedMimeType);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        List<android.content.pm.ResolveInfo> matches = context.getPackageManager().queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL);
        if (matches != null && !matches.isEmpty()) {
            ActivityInfo resolvedComponentInfo = matches.get(0).activityInfo;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && resolvedComponentInfo != null) {
                intent.setComponent(
                        ComponentName.createRelative(resolvedComponentInfo.packageName, resolvedComponentInfo.name));
            }
        }
        activity.startActivity(intent);
    }

    public void downloadImageNew(String filename, String downloadUrlOfImage) {
        try {
            DownloadManager dm = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
            Uri downloadUri = Uri.parse(downloadUrlOfImage);
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);
            request.setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                    .setAllowedOverRoaming(false)
                    .setTitle(filename)
                    .setMimeType("image/jpeg") // Your file type. You can use this code to download other file types
                                               // also.
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES,
                            File.separator + filename + ".jpg");
            dm.enqueue(request);
            Toast.makeText(activity, "Image download started.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            if (Tools.isDebug())
                Log.d("Error downloadImageNew", e.toString());
            Toast.makeText(activity, "Image download failed.", Toast.LENGTH_SHORT).show();

            throw e;
        }
    }

    public boolean isConnectedNetwork() {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }

    public void verifyStoragePermissions(Activity activity) {
        int permissions = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        System.out.println("?!" + permissions);
        System.out.println("?!!" + PackageManager.PERMISSION_GRANTED);
        ActivityCompat.requestPermissions(activity, permissionStorage, 1);
    }

    public void performHapticFeedback(CustomWebView webView, Constant.HapticChoice hapticChoice) {
        // fallback if this haptic isn't available on device
        if (!isHapticVersionValid(hapticChoice)) {
            webView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            return;
        }

        // ... otherwise, perform relevant haptic
        if (hapticChoice == Constant.HapticChoice.SUCCESS) {
            webView.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            return;
        } else if (hapticChoice == Constant.HapticChoice.ERROR) {
            webView.performHapticFeedback(HapticFeedbackConstants.REJECT);
            return;
        } else if (hapticChoice == Constant.HapticChoice.LIGHT) {
            Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
            if ((vibrator != null) && (vibrator.hasVibrator())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                }
                return;
            }
        } else if (hapticChoice == Constant.HapticChoice.HEAVY) {
            Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
            if ((vibrator != null) && (vibrator.hasVibrator())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK));
                }
                return;
            }
        }
    }

    private boolean isHapticVersionValid(Constant.HapticChoice hapticChoice) {
        if (hapticChoice == Constant.HapticChoice.SUCCESS || hapticChoice == Constant.HapticChoice.ERROR) {
            return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R);
        } else if (hapticChoice == Constant.HapticChoice.LIGHT || hapticChoice == Constant.HapticChoice.HEAVY) {
            return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
        }
        return false;
    }

    public void handleIntentUrl(String url) {
        try {
            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            activity.startActivity(intent);
        } catch (Exception e) {
            // noinspection CallToPrintStackTrace
            e.printStackTrace();
            if (Tools.isDebug()) {
                Log.d(TAG, "No app to handle intent URL");
            }
            String fallbackParameter = "browser_fallback_url=";
            String separatorChar = ";";
            int startingIndex = 0;
            if (url.contains(fallbackParameter)) {
                try {
                    String fallbackURL = url.substring(url.indexOf(fallbackParameter) + fallbackParameter.length());
                    fallbackURL = fallbackURL.substring(startingIndex, fallbackURL.indexOf(separatorChar));
                    if (URLUtil.isValidUrl(fallbackURL)) {
                        if (Tools.isDebug())
                            Log.d(TAG, "Fallback URL found, loading in external browser");
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackURL));
                        activity.startActivity(i);
                    }
                } catch (Exception f) {
                    if (Tools.isDebug())
                        Log.d(TAG, "Fallback URL failed");
                }
            }
        }
    }

    public void verifyNotificationPermission(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[] { Manifest.permission.POST_NOTIFICATIONS },
                    Constant.requestNotificationCode);
        }
    }

    public static void saveImage(Bitmap bitmap, @NonNull String name) throws IOException {
        boolean saved;
        OutputStream fos;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = Constant.context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/" + "img");
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(imageUri);
        } else {
            String imagesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM).toString() + File.separator + "img";

            File file = new File(imagesDir);

            if (!file.exists()) {
                file.mkdir();
            }

            File image = new File(imagesDir, name + ".png");
            fos = new FileOutputStream(image);
        }

        saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.flush();
        fos.close();
    }

    public static File screenshot(View view, String filename) {
        Date date = new Date();
        // Here we are initialising the format of our image name
        CharSequence format = android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", date);
        try {
            // Initialising the directory of storage
            String dirpath = Environment.getExternalStorageDirectory().getAbsolutePath() + "";
            File file = new File(dirpath);
            if (!file.exists()) {
                boolean mkdir = file.mkdir();
            }
            // File name
            String path = dirpath + "/DCIM/" + filename + "-" + format + ".jpeg";
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);
            File imageurl = new File(path);
            saveImage(bitmap, format.toString());
            return imageurl;

        } catch (IOException e) {
            System.out.println("!!!");
            e.printStackTrace();
        }
        return null;
    }

    public boolean findBinary(String binaryName) { // credit to Sanjay Bhalani (https://stackoverflow.com/a/57590343)
        boolean found = false;
        if (!found) {
            String[] places = { "/sbin/", "/system/bin/", "/system/xbin/",
                    "/data/local/xbin/", "/data/local/bin/",
                    "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/" };
            for (String where : places) {
                if (new File(where + binaryName).exists()) {
                    found = true;

                    break;
                }
            }
        }
        return found;
    }

    // executes a command on the system
    public boolean canExecuteCommand(String command) { // credit to Sanjay Bhalani
                                                       // (https://stackoverflow.com/a/57590343)
        boolean executedSuccesfully;
        try {
            Runtime.getRuntime().exec(command);
            executedSuccesfully = true;
        } catch (Exception e) {
            executedSuccesfully = false;
        }
        return executedSuccesfully;
    }

    public void toggleBackgroundLocationService(boolean shouldStart) {
        if (Constant.requireBackgroundLocationPermission) {
            Intent backgroundLocationIntent = new Intent(activity, GPSService.class);
            if (shouldStart) {
                activity.startService(backgroundLocationIntent);
            } else {
                activity.stopService(backgroundLocationIntent);
            }
        }
    }

    public void openInExternalBrowser(String launchUrl) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(launchUrl));
        activity.startActivity(browserIntent);
    }

    public Notification.Builder getNotificationBuilder(String title, String message, String urlToOpen) {
        createNotificationChannel();
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(activity, activity.getString(R.string.local_notification_channel_id));
        } else {
            builder = new Notification.Builder(activity);
        }
        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra("ONESIGNAL_URL", urlToOpen);
        PendingIntent pendingIntent = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(activity, 1, intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
        }
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_launcher))
                .setContentTitle(title)
                .setAutoCancel(true)
                .setContentText(message)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);
        return builder;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = activity.getString(R.string.local_notification_channel_name);
            String description = activity.getString(R.string.local_notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(
                    activity.getString(R.string.local_notification_channel_id), name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = activity.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /** @noinspection UnusedReturnValue */
    public boolean onWebViewOverriding(String url) {

        if (url.contains("target=url-checker")) {
            ((MainActivity) activity).urlChecker();
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
            CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
            intent.launchUrl(activity, Uri.parse(url.replace("?target=custom_tabs", "")));
            return true;
        }

        return false;
    }

}
