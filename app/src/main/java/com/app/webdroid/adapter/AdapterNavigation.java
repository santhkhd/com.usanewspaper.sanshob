package com.app.webdroid.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.shobmc.san.R;
import com.app.webdroid.activity.MainActivity;
import com.app.webdroid.database.prefs.AdsPref;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.model.Navigation;
import com.app.webdroid.util.AdsManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.solodroidx.ads.nativead.NativeAdViewHolder;

import java.util.List;

public class AdapterNavigation extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int VIEW_ITEM = 0;
    private final int VIEW_AD = 1;
    private List<Navigation> items;
    Context context;
    private OnItemClickListener mOnItemClickListener;
    private int clickedItemPosition = -1;
    SharedPref sharedPref;
    AdsPref adsPref;
    AdsManager adsManager;
    public static boolean isFirstItemClicked = false;
    private int counter = 1;

    public interface OnItemClickListener {
        void onItemClick(View view, Navigation obj, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    public AdapterNavigation(Context context, List<Navigation> items) {
        this.items = items;
        this.context = context;
        this.sharedPref = new SharedPref(context);
        this.adsPref = new AdsPref(context);
        this.adsManager = new AdsManager((Activity) context);
    }

    public static class OriginalViewHolder extends RecyclerView.ViewHolder {

        public TextView menuName;
        public ImageView menuIcon;
        public LinearLayout lytItem;
        public MaterialCardView cardView;

        public OriginalViewHolder(View v) {
            super(v);
            menuName = v.findViewById(R.id.menu_name);
            menuIcon = v.findViewById(R.id.menu_icon);
            lytItem = v.findViewById(R.id.lyt_item);
            cardView = v.findViewById(R.id.lyt_parent);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        if (viewType == VIEW_AD) {
            vh = adsManager.createNativeAdViewHolder(context, parent);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_drawer, parent, false);
            vh = new OriginalViewHolder(v);
        }
        return vh;
    }

    @SuppressLint({ "RecyclerView", "NotifyDataSetChanged" })
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {

        if (holder instanceof OriginalViewHolder) {

            final Navigation obj = items.get(position);
            final OriginalViewHolder vItem = (OriginalViewHolder) holder;

            vItem.menuName.setText(obj.name);
            Glide.with(context)
                    .load(obj.icon.replace(" ", "%20"))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_home)
                    .centerCrop()
                    .into(vItem.menuIcon);

            if (sharedPref.getIsDarkTheme()) {
                vItem.cardView
                        .setBackgroundColor(ContextCompat.getColor(context, R.color.color_dark_navigation_drawer));
            } else {
                vItem.cardView
                        .setBackgroundColor(ContextCompat.getColor(context, R.color.color_light_navigation_drawer));
            }

            vItem.cardView.setOnClickListener(view -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(view, obj, position);
                    clickedItemPosition = position;
                    notifyDataSetChanged();
                    ((MainActivity) context).loadWebPage(obj.name, obj.type, obj.url, obj.url_dark);
                    if (isFirstItemClicked) {
                        String url = "";
                        if (sharedPref.getIsDarkTheme()) {
                            if (obj.url_dark != null && !obj.url_dark.isEmpty()) {
                                url = obj.url_dark;
                            } else if (obj.url != null) {
                                url = obj.url;
                            }
                        } else if (obj.url != null) {
                            url = obj.url;
                        }
                        if (url != null && url.contains("?target=interstitial")) {
                            ((MainActivity) context).showInterstitialAd(adsPref.getIsInterstitialDrawerMenu());
                        } else {
                            if (counter >= adsPref.getInterstitialAdIntervalOnDrawerMenu()) {
                                ((MainActivity) context).showInterstitialAd(adsPref.getIsInterstitialDrawerMenu());
                                counter = 1;
                            } else {
                                counter++;
                            }
                        }
                    }
                }
            });

            if (clickedItemPosition == position) {
                vItem.lytItem.setBackgroundResource(R.drawable.bg_item_selected);
                vItem.menuName.setTextColor(ContextCompat.getColor(context, R.color.color_light_primary));
                vItem.menuIcon.setColorFilter(ContextCompat.getColor(context, R.color.color_light_primary));
            } else {
                vItem.lytItem.setBackgroundResource(R.drawable.bg_item_unselected);
                if (sharedPref.getIsDarkTheme()) {
                    vItem.menuName.setTextColor(ContextCompat.getColor(context, R.color.color_dark_text));
                    vItem.menuIcon.setColorFilter(ContextCompat.getColor(context, R.color.color_dark_text));
                } else {
                    vItem.menuName.setTextColor(ContextCompat.getColor(context, R.color.color_light_text));
                    vItem.menuIcon.setColorFilter(ContextCompat.getColor(context, R.color.color_light_text));
                }
            }

        } else if (holder instanceof NativeAdViewHolder) {
            adsManager.bindNativeAdViewHolder(context, (NativeAdViewHolder) holder);
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    public void setListData(List<Navigation> items) {
        this.items = items;
        // if (adsPref.getIsNativeDrawerMenu()) {
        // items.add(0, new Navigation());
        // }
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void resetListData() {
        this.items.clear();
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSelected(int position) {
        this.clickedItemPosition = position;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        Navigation obj = items.get(position);
        if (obj != null) {
            if (obj.name == null || obj.name.equals("")) {
                return VIEW_AD;
            }
            return VIEW_ITEM;
        } else {
            return VIEW_ITEM;
        }
    }

}