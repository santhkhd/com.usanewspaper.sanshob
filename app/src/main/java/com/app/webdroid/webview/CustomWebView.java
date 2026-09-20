package com.app.webdroid.webview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.webkit.WebView;

import java.lang.ref.WeakReference;

public class CustomWebView extends WebView {

    private GestureDetector gestureDetector;
    protected WeakReference<Activity> activityWeakReference;
    protected WeakReference<Fragment> fragmentWeakReference;
    public static boolean geolocationEnabled;

    public CustomWebView(Context context) {
        super(context);
    }

    public CustomWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onScrollChanged(int left, int top, int oldLeft, int oldTop) {
        super.onScrollChanged(left, top, oldLeft, oldTop);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return gestureDetector.onTouchEvent(motionEvent) || super.onTouchEvent(motionEvent);
    }

    public void setGestureDetector(GestureDetector gestureDetector) {
        this.gestureDetector = gestureDetector;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void setGeolocationEnabled(final boolean enabled) {
        if (enabled) {
            getSettings().setJavaScriptEnabled(true);
            getSettings().setGeolocationEnabled(true);
            setGeolocationDatabasePath();
        }
        geolocationEnabled = enabled;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    protected void setGeolocationDatabasePath() {
        final Activity activity;
        if (fragmentWeakReference != null && fragmentWeakReference.get() != null && fragmentWeakReference.get().getActivity() != null) {
            activity = fragmentWeakReference.get().getActivity();
        } else if (activityWeakReference != null && activityWeakReference.get() != null) {
            activity = activityWeakReference.get();
        } else {
            return;
        }

        getSettings().setGeolocationDatabasePath(activity.getFilesDir().getPath());
    }

}