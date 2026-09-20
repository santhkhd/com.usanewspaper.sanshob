package com.app.webdroid.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.appcompat.app.AppCompatActivity;

import com.shobmc.san.R;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.util.Tools;
import com.google.android.material.snackbar.Snackbar;
import com.solodroidx.ads.appopen.AppOpenAd;

public class ActivityRedirect extends AppCompatActivity {

    SharedPref sharedPref;
    ImageButton btnClose;
    Button btnRedirect;
    OnBackPressedDispatcher onBackPressedDispatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_redirect);
        Tools.setNavigation(this);
        sharedPref = new SharedPref(this);
        initView();
        handleOnBackPressed();
    }

    public void handleOnBackPressed() {
        onBackPressedDispatcher = getOnBackPressedDispatcher();
        onBackPressedDispatcher.addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                AppOpenAd.isAppOpenAdLoaded = false;
            }
        });
    }

    private void initView() {
        btnClose = findViewById(R.id.btn_close);
        btnRedirect = findViewById(R.id.btn_redirect);

        btnClose.setOnClickListener(view -> {
            finish();
            AppOpenAd.isAppOpenAdLoaded = false;
        });

        btnRedirect.setOnClickListener(view -> {
            if (sharedPref.getRedirectUrl().isEmpty()) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.redirect_error),
                        Snackbar.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(sharedPref.getRedirectUrl())));
                finish();
                AppOpenAd.isAppOpenAdLoaded = false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppOpenAd.isAppOpenAdLoaded = false;
    }

}
