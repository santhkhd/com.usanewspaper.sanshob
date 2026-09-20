package com.app.webdroid.database.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import com.shobmc.san.R;
import com.app.webdroid.model.Slider;
import com.app.webdroid.util.Tools;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class SharedPref {

    Context context;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    public SharedPref(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(Tools.getApplicationId() + "_settings_prefs",
                Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // blog credentials
    public void saveBlogCredentials(String bloggerId, String apiKey) {
        editor.putString("blogger_id", bloggerId);
        editor.putString("api_key", apiKey);
        editor.apply();
    }

    public Boolean getIsDarkTheme() {
        return sharedPreferences.getBoolean("theme", false);
    }

    public void setIsDarkTheme(Boolean isDarkTheme) {
        editor.putBoolean("theme", isDarkTheme);
        editor.apply();
    }

    public Integer getLastItemPosition() {
        return sharedPreferences.getInt("item_position", 0);
    }

    public void setLastItemPosition(int position) {
        editor.putInt("item_position", position);
        editor.apply();
    }

    public Boolean getIsNotificationOn() {
        return sharedPreferences.getBoolean("notification", true);
    }

    public void setIsNotificationOn(Boolean isNotificationOn) {
        editor.putBoolean("notification", isNotificationOn);
        editor.apply();
    }

    public void saveConfig(boolean toolbar, boolean navigationDrawer, boolean geolocation, boolean cache,
            boolean openLinkInExternalBrowser, boolean zoom_controls, String userAgent, String privacyPolicyUrl,
            String moreAppsUrl, String redirectUrl, boolean webviewDisclaimerActive) {
        editor.putBoolean("toolbar", toolbar);
        editor.putBoolean("navigation_drawer", navigationDrawer);
        editor.putBoolean("geolocation", geolocation);
        editor.putBoolean("cache", cache);
        editor.putBoolean("open_link_in_external_browser", openLinkInExternalBrowser);
        editor.putBoolean("zoom_controls", zoom_controls);
        editor.putString("user_agent", userAgent);
        editor.putString("privacy_policy_url", privacyPolicyUrl);
        editor.putString("more_apps_url", moreAppsUrl);
        editor.putString("redirect_url", redirectUrl);
        editor.putBoolean("webview_disclaimer_active", webviewDisclaimerActive);
        editor.apply();
    }

    public boolean getToolbar() {
        return sharedPreferences.getBoolean("toolbar", true);
    }

    public boolean getNavigationDrawer() {
        return sharedPreferences.getBoolean("navigation_drawer", true);
    }

    public boolean getGeolocation() {
        return sharedPreferences.getBoolean("geolocation", true);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean getCache() {
        return sharedPreferences.getBoolean("cache", true);
    }

    public boolean getOpenLinkInExternalBrowser() {
        return sharedPreferences.getBoolean("open_link_in_external_browser", true);
    }

    public boolean getBuiltInZoomControls() {
        return sharedPreferences.getBoolean("zoom_controls", false);
    }

    public String getUserAgent() {
        return sharedPreferences.getString("user_agent", "");
    }

    public String getPrivacyPolicyUrl() {
        return sharedPreferences.getString("privacy_policy_url", "");
    }

    public String getMoreAppsUrl() {
        return sharedPreferences.getString("more_apps_url", "");
    }

    public String getRedirectUrl() {
        return sharedPreferences.getString("redirect_url", "");
    }

    public boolean getWebViewDisclaimerActive() {
        return sharedPreferences.getBoolean("webview_disclaimer_active", false);
    }

    public void resetPageToken() {
        sharedPreferences.edit().remove("page_token").apply();
    }

    public Integer getFontSize() {
        return sharedPreferences.getInt("font_size", 2);
    }

    public void updateFontSize(int font_size) {
        editor.putInt("font_size", font_size);
        editor.apply();
    }

    public Integer getInAppReviewToken() {
        return sharedPreferences.getInt("in_app_review_token", 0);
    }

    public void updateInAppReviewToken(int value) {
        editor.putInt("in_app_review_token", value);
        editor.apply();
    }

    public Boolean getIntroSliderStatus() {
        return sharedPreferences.getBoolean("intro_slider_status", true);
    }

    public void setIntroSliderStatus(Boolean sliderStatus) {
        editor.putBoolean("intro_slider_status", sliderStatus);
        editor.apply();
    }

    public Boolean getIsShowIntroSlider() {
        return sharedPreferences.getBoolean("intro_slider", true);
    }

    public void setIsShowIntroSlider(Boolean isShowIntroSlider) {
        editor.putBoolean("intro_slider", isShowIntroSlider);
        editor.apply();
    }

    public void saveSliderList(List<Slider> sliders) {
        Gson gson = new Gson();
        String json = gson.toJson(sliders);
        editor.putString("intro_slider_list", json);
        editor.apply();
    }

    public List<Slider> getSliderList() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString("intro_slider_list", null);
        Type type = new TypeToken<ArrayList<Slider>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    public boolean getShowRealChannelLogo() {
        return sharedPreferences.getBoolean("show_real_channel_logo", true);
    }

    public void setShowRealChannelLogo(boolean show) {
        editor.putBoolean("show_real_channel_logo", show);
        editor.apply();
    }

    public String getYoutubeApiKey() {
        // Use the API key provided by user
        return context.getString(R.string.youtube_api_key);
    }

    public int getVideoViewMode() {
        return sharedPreferences.getInt("video_view_mode", 1);
    }

    public void setVideoViewMode(int mode) {
        editor.putInt("video_view_mode", mode);
        editor.apply();
    }
}
