package com.app.webdroid.util;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.app.webdroid.webview.CustomWebView;

public class CustomGestureDetector extends GestureDetector.SimpleOnGestureListener {

    public static final String TAG = "CustomGestureDetector";
    Activity activity;
    CustomWebView webView;

    public CustomGestureDetector(Activity activity, CustomWebView webView) {
        this.activity = activity;
        this.webView = webView;
    }

    @SuppressWarnings({"ConstantValue", "IfStatementWithIdenticalBranches"})
    @Override
    public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
        if (Constant.enableSwipeNavigate) {
            if (e1 == null || e2 == null) return false;
            if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
            else {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int screenWidth = displayMetrics.widthPixels;
                int edgeSwipeTolerance = 30;
                try {
                    if (e1.getX() - e2.getX() > 100 && Math.abs(velocityX) > 800) {
                        if (e1.getX() > (screenWidth - edgeSwipeTolerance)) {
                            Log.d(TAG, "Forwards swipe detected");
                            if (webView.canGoForward()) {
                                webView.goForward();
                            }
                            return true;
                        }
                    } else if (e2.getX() - e1.getX() > 100 && Math.abs(velocityX) > 800) {
                        if (e1.getX() < edgeSwipeTolerance) {
                            Log.d(TAG, "Backwards swipe detected");
                            if (webView.canGoBack()) {
                                webView.goBack();
                            }
                            return true;
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "error: " + e.getMessage());
                }
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
        return true;
    }

}