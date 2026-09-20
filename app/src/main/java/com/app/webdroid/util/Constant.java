package com.app.webdroid.util;

import android.annotation.SuppressLint;
import android.content.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Constant {

    @SuppressLint("StaticFieldLeak")
    public static Context context;

    public static final String userAgentGoogle = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.101 Mobile Safari/537.36";
    public static final String userAgentFacebook = "Mozilla/5.0 (Linux; U; Android 2.2) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";

    public static final int permissionRequestCode = 9541;
    public static final int cameraRequestCode = 2;
    public static final int requestNotificationCode = 11;
    public static final int requestQrScanCode = 1234;

    public static boolean isRedirected = false;
    public static boolean isConnected = false;
    public static boolean NfcEnabled = false;
    public static final int delayClickAction = 250;

    public enum HapticChoice {SUCCESS, ERROR, LIGHT, HEAVY}

    // Set the phone orientation to either "portrait", "landscape", or "auto"
    public static final String phoneOrientation = "auto";

    // Set the tablet orientation to either "portrait", "landscape", or "auto"
    public static final String tabletOrientation = "auto";

    // Set to "true" to activate Hardware Acceleration; it can improve rendering performance but can also increase memory usage and may cause compatibility issues with some content
    public static final boolean enableHardwareAcceleration = false;

    // Set to "true" if you want to extend URL request by the system language like ?webview_language=LANGUAGE CODE (e.g., ?webview_language=EN for English users)
    public static final boolean appendLanguageCode = false;

    // Set this to "true" if you want the WebView to automatically refresh its contents when the app comes back to the foreground from the background
    public static final boolean refreshWebPageOnResume = false;

    // Set to (0) to open external links in-app by default; (1) to ALWAYS open in a new tab (an additional in-app browser); (2) to ALWAYS open in another browser
    public static final int externalLinkHandlingOptions = 0;

    // Set to (0) to open special links in-app; (1) in a new tab (an additional in-app browser); (2) in another browser
    // NOTE: Special links have a "_blank" target or end with "#"; Overrides EXTERNAL_URL_HANDLING_OPTIONS if the link is also an external link
    public static final int specialLinkHandlingOptions = 1;

    // Add domains here that should ALWAYS be opened in the external browser, regardless of what the EXTERNAL_LINK_HANDLING_OPTIONS option is set to;
    // to add another domain, insert another host like so: ["https://google.com", "https://facebook.com"]
    // please enter the host exactly how you link to it (with or without www, but always without http/https)
    public static String[] browserWithList = new String[]{};

    // Add domains here that should NEVER be opened in the external browser, regardless of what the EXTERNAL_LINK_HANDLING_OPTIONS option is set to;
    // to add another domain, insert another host like so: {"https://google.com", "https://facebook.com"}
    // please enter the host exactly how you link to it (with or without www, but always without http/https)
    public static String[] browserBlacklist = new String[]{};

    //Acts regardless of what the EXTERNAL_LINK_HANDLING_OPTIONS option is set to
    //Example: {"https://google.com", "https://facebook.com"}
    public static String[] alwaysOpenInAppTab = new String[]{};

    //Add URL prefixes that you NEVER want to open in an in-app tab (e.g., {"https://google.com", "https://facebook.com"})
    //Acts regardless of what the EXTERNAL_LINK_HANDLING_OPTIONS option is set to
    public static String[] neverOpenInAppTab = new String[]{};

    // Set to (0) to open a scanned QR code URL in the app; (1) in an in-app tab; (2) in a new browser; (3) in an in-app tab if external; (4) in a new browser if external
    public static final int qrCodeUrlOptions = 0;

    // Set to "true" in order to automatically set JavaScript variables; will eliminate the need to manually call methods like get-uuid://; currently, it supports loading the app version, OneSignal and Firebase player IDs, as well as the UUID
    public static final boolean autoInjectVariables = false;

    //Set to "true" to prevent the device from going into sleep while the app is active
    public static final boolean preventAppSleep = false;

    //Set to "true" to enable navigation by swiping left or right to move back or forward a page
    public static final boolean enableSwipeNavigate = false;

    //Set to "true" to hide scrollbar
    public static final boolean hideVerticalScrollbar = false;
    public static final boolean hideHorizontalScrollbar = false;

    //Set to a value greater than 0 to define a maximum text zoom; Set to (0) to disable this feature
    //Note: Small = 85, Default = 100, Large = 115, Largest = 130
    public static final int maxTextZoom = 0;

    // Set to "true" to add the UUID parameter 'uuid=XYZ' to the first URL request
    public static final boolean uuidEnhanceWebViewUrl = false;

    //Set to "true" to block content signed with self-signed SSL (user) certificates & faulty SSL certificates; maybe consider blocking all Non-HTTPS content
    public static boolean blockSelfSignedAndFaultySslCertificate = false;

    //Set to "false" to disable link drag and drop
    public static boolean linkDragAndDrop = true;

    //Set to "true" to always present fullscreen videos in landscape mode
    public static final boolean landscapeFullScreenVideo = true;

    // Set to "false" to prevent the "Download images" pop-up box from appearing when long-pressing on an image
    public static final boolean allowImageDownload = true;

    //Add the file formats that should trigger the manual file downloader functionality
    public static List<String> downloadableExtension = Collections.unmodifiableList(
            Arrays.asList(".epub", ".pdf", ".pptx", ".docx", ".doc", ".xlsx", ".mp3", ".mp4", ".wav") //Add them here!
    );
    // Set to "true" if you want to activate the downloader functionality based on Content-Disposition HTTP headers, regardless of the file formats listed in the downloadableExtension variable above
    public static final boolean AUTO_DOWNLOAD_FILES = true;

    //Define the URL prefixes that load during Google login for your website; acts as a trigger for the helper
    //Example: {"https://accounts.google.com", "https://accounts.youtube.com"}
    public static String[] googleLoginHelperTriggers = {};

    //Define the URL prefixes that load during Facebook login for your website; acts as a trigger for the helper
    //Example: {"https://facebook.com", "https://facebook.com"}
    public static String[] facebookLoginHelperTriggers = {};

    //Set to "true" to enable the Manual Cookie Sync tool
    public static final boolean enableManualCookieSync = false;
    public static final int cookieSyncDuration = 5000;

    //Example: {"https://domain.com/login", "https://domain.com/example"}
    public static String[] manualCookieSyncTriggerUrls = {};

    //Set to "true" if background (!) location services are also needed (also, ensure that you also uncomment the "android.permission.FOREGROUND_SERVICE_LOCATION" and ".GPSService" blocks in AndroidManifest.xml; search for these terms within); this setting is working only if "requireLocation" is also set to "true"
    public static boolean requireBackgroundLocationPermission = false;

    //Set to "false" if you do NOT require APIs related to camera images / camera videos; don't forget to also remove relevant entries in AndroidManifest.xml if you want to ensure the complete removal of the permission capability for the app
    public static boolean requireCameraPermission = true;

    //Set to "false" if you do NOT require APIs related to recording audio; don't forget to also remove relevant entries in AndroidManifest.xml if you want to ensure the complete removal of the permission capability for the app
    public static boolean requireRecordAudioPermission = true;

    //Set to "false" if you do NOT require APIs related to downloads or uploads; don't forget to also remove relevant entries in AndroidManifest.xml if you want to ensure the complete removal of the permission capability for the app
    public static boolean requireStoragePermission = true;

    // File upload options: (0) Camera & gallery; (1) Camera only; (2) Gallery only.
    public static final int filePickerModePermission = 0;

}