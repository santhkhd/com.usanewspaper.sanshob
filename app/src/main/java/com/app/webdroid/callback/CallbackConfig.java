package com.app.webdroid.callback;

import com.app.webdroid.model.Ads;
import com.app.webdroid.model.App;
import com.app.webdroid.model.Intro;
import com.app.webdroid.model.Navigation;

import com.app.webdroid.model.AppConfig;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class CallbackConfig {

    public App app = null;
    public Intro intro = null;
    public List<Navigation> menus = new ArrayList<>();
    public Ads ads = null;

    @SerializedName("google_sheet_url")
    public String googleSheetUrl = "";

    @SerializedName("github_cdn_base_url")
    public String githubCdnBaseUrl = "";

    @SerializedName("github_raw_base_url")
    public String githubRawBaseUrl = "";

    @SerializedName("youtube_channels")
    public List<AppConfig.YouTubeSource> youtubeChannels = new ArrayList<>();

}