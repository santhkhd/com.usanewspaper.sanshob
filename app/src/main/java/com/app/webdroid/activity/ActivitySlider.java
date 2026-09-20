package com.app.webdroid.activity;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.shobmc.san.R;
import com.app.webdroid.adapter.AdapterSlider;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.model.Slider;
import com.app.webdroid.util.Tools;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.solodroidx.ads.appopen.AppOpenAd;

import java.util.List;

public class ActivitySlider extends AppCompatActivity {

    ViewPager2 viewPager2;
    TabLayout tabLayout;
    AdapterSlider adapterSlider;
    ImageButton btnNext;
    MaterialButton btnSkip;
    SharedPref sharedPref;
    RelativeLayout rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_slider);
        Tools.setNavigation(this);
        sharedPref = new SharedPref(this);
        initView();
    }

    private void initView() {
        rootView = findViewById(R.id.root_view);
        if (sharedPref.getIsDarkTheme()) {
            rootView.setBackgroundColor(ContextCompat.getColor(this, R.color.color_dark_status_bar));
        } else {
            rootView.setBackgroundColor(ContextCompat.getColor(this, R.color.color_light_status_bar));
        }

        viewPager2 = findViewById(R.id.view_pager_slider);
        tabLayout = findViewById(R.id.tab_layout);
        btnNext = findViewById(R.id.btn_next);
        btnSkip = findViewById(R.id.btn_skip);
        setupViewPager2(sharedPref.getSliderList());
    }

    private void setupViewPager2(List<Slider> sliders) {
        int lastItemPosition = (sliders.size() - 1);
        adapterSlider = new AdapterSlider(this, sliders);
        viewPager2.setAdapter(adapterSlider);
        viewPager2.setOffscreenPageLimit(sliders.size());
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == lastItemPosition) {
                    btnNext.setColorFilter(ContextCompat.getColor(ActivitySlider.this, R.color.color_dark_text),
                            PorterDuff.Mode.SRC_IN);
                    btnNext.setEnabled(false);
                    btnSkip.setVisibility(View.GONE);
                } else {
                    btnNext.setColorFilter(ContextCompat.getColor(ActivitySlider.this, R.color.color_light_text),
                            PorterDuff.Mode.SRC_IN);
                    btnNext.setEnabled(true);
                    btnSkip.setVisibility(View.VISIBLE);
                    btnNext.setOnClickListener(view -> viewPager2.setCurrentItem(position + 1));
                }
                super.onPageSelected(position);
            }
        });

        new TabLayoutMediator(tabLayout, viewPager2, (tab, position) -> {
        }).attach();

        btnSkip.setOnClickListener(view -> startMainActivity());
    }

    public void startMainActivity() {
        sharedPref.setIsShowIntroSlider(false);
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppOpenAd.isAppOpenAdLoaded = false;
    }

}
