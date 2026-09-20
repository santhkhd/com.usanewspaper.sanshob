package com.app.webdroid;

public class Config {

    // Your remote json url / access key here
    public static final String ACCESS_KEY = "XXXXX";

    // Remote GitHub CDN (jsDelivr) base URL - disabled to load clean in-app USA assets
    public static final String GITHUB_CDN_BASE_URL = "";

    // Remote GitHub Raw base URL
    public static final String GITHUB_RAW_BASE_URL = "";

    // Active Remote Base URL for USA assets (Empty to use local assets)
    public static final String GITHUB_ASSETS_BASE_URL = "";

    // Direct URL for the configuration file (Empty to load local config.json)
    public static final String JSON_CONFIG_URL = "";

    // Fallback URL via jsDelivr CDN
    public static final String JSON_CONFIG_RAW_URL = "";

    // US News Aggregator & Summarizer GitHub Raw JSON Endpoints
    public static final String US_NEWS_GITHUB_RAW_URL = "https://raw.githubusercontent.com/santhkhd/com.usanewspaper.sanshob/main/data/us_news.json";
    public static final String US_NEWS_CATEGORIES_RAW_URL = "https://raw.githubusercontent.com/santhkhd/com.usanewspaper.sanshob/main/data/categories.json";

    // Google Sheets / Apps Script WebApp URL for saving News Comments & Opinions
    // Paste your deployed Google Apps Script URL here (e.g. "https://script.google.com/macros/s/.../exec")
    public static final String GOOGLE_SHEET_WEBAPP_URL = "https://script.google.com/macros/s/AKfycbzUVsELXXxwyPZNGxGiglLIgp4C8yGHflkwQB-JIaDMKoKPTR0AgHcdNjoLdnIsyibMOg/exec";

    // RTL Direction for Arabic Language
    public static final boolean ENABLE_RTL_MODE = false;

    public static final boolean ENABLE_LINEAR_PROGRESS_INDICATOR = true;

    public static final boolean ENABLE_SWIPE_REFRESH_LAYOUT = true;

    // GDPR EU Consent (Set to false so AdMob requests aren't blocked by unconfigured UMP forms)
    public static final boolean ENABLE_GDPR_UMP_SDK = false;

    // Show exit dialog when user want to close the app
    public static final boolean SHOW_EXIT_DIALOG = true;

    // Enable it with true value if want to the app will force to display open adsgt


    // Longer duration to start the app may occur depending on internet connection
    // or open ad response time itself
    public static final boolean FORCE_TO_SHOW_APP_OPEN_AD_ON_START = false;

    // delay splash when remote config finish loading
    public static final int DELAY_SPLASH = 500;

}