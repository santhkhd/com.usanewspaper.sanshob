package com.app.webdroid.model;

import java.io.Serializable;

public class App implements Serializable {

    public boolean status;
    public boolean toolbar;
    public boolean navigation_drawer;
    public boolean geolocation;
    public boolean cache;
    public boolean open_link_in_external_browser;
    public boolean zoom_controls;
    public String user_agent = "";
    public String privacy_policy_url = "";
    public String more_apps_url = "";
    public String redirect_url = "";
    public boolean webview_disclaimer_active;

}
