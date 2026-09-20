package com.app.webdroid.model;

import java.io.Serializable;

public class Ads implements Serializable {

    public boolean ad_status;
    public String main_ads = "";
    public String backup_ads = "";
    public String admob_banner_unit_id = "";
    public String admob_interstitial_unit_id = "";
    public String admob_native_unit_id = "";
    public String admob_app_open_ad_unit_id = "";
    public String ad_manager_banner_unit_id = "";
    public String ad_manager_interstitial_unit_id = "";
    public String ad_manager_native_unit_id = "";
    public String ad_manager_app_open_ad_unit_id = "";
    public String fan_banner_unit_id = "";
    public String fan_interstitial_unit_id = "";
    public String fan_native_unit_id = "";
    public String startapp_app_id = "";
    public String unity_game_id = "";
    public String unity_banner_placement_id = "";
    public String unity_interstitial_placement_id = "";
    public String applovin_banner_ad_unit_id = "";
    public String applovin_interstitial_ad_unit_id = "";
    public String applovin_native_ad_manual_unit_id = "";
    public String applovin_app_open_ad_unit_id = "";
    public String applovin_banner_zone_id = "";
    public String applovin_banner_mrec_zone_id = "";
    public String applovin_interstitial_zone_id = "";
    public String ironsource_app_key = "";
    public String ironsource_banner_placement_name = "";
    public String ironsource_interstitial_placement_name = "";
    public int interstitial_ad_interval_on_drawer_menu;
    public int interstitial_ad_interval_on_web_page_link;
    public String native_ad_style_drawer_menu = "";
    public String native_ad_style_exit_dialog = "";
    public String native_ad_style_product_list = "";
    public int native_ad_index;
    public boolean interstitial_ad_on_list_item_click;
    public int interstitial_ad_interval_on_list_item_click = 3;
    public Placement placement = null;

}
