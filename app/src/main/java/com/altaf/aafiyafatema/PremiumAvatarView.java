package com.altaf.aafiyafatema;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class PremiumAvatarView extends WebView {
    public interface Listener {
        void onCharacterTap();
        void onTouchZone(String zone);
        void onModelReady();
        void onModelError(String message);
    }

    private Listener listener;
    private float downX;
    private float downY;
    private long downAt;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    public PremiumAvatarView(Context context) {
        super(context);

        setBackgroundColor(Color.TRANSPARENT);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        setOverScrollMode(OVER_SCROLL_NEVER);

        WebSettings s = getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadsImagesAutomatically(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        setWebChromeClient(new WebChromeClient());
        setWebViewClient(new WebViewClient());
        addJavascriptInterface(new Bridge(), "Android");

        loadUrl("file:///android_asset/premium_avatar.html");
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setPose(String pose) {
        String safe = pose == null ? "idle" : pose.replace("'", "");
        post(() -> evaluateJavascript("window.setAafiyaPose('" + safe + "')", null));
    }

    public void showEffect(String effect) {
        String safe = effect == null ? "heart" : effect.replace("'", "");
        post(() -> evaluateJavascript("window.showAafiyaEffect('" + safe + "')", null));
    }

    public void setSpeaking(boolean speaking) {
        setPose(speaking ? "talk" : "idle");
        if (speaking) showEffect("music");
    }

    public void setListening(boolean listening) {
        setPose(listening ? "listen" : "idle");
    }

    public void wave() {
        setPose("wave");
    }

    public void happy() {
        setPose("happy");
        showEffect("heart");
    }

    public void sleep() {
        setPose("sleep");
        showEffect("zzz");
    }

    public void eat() {
        setPose("happy");
        showEffect("food");
    }

    public void bath() {
        setPose("happy");
        showEffect("bath");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                downAt = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_UP:
                float dx = Math.abs(event.getX() - downX);
                float dy = Math.abs(event.getY() - downY);
                long elapsed = System.currentTimeMillis() - downAt;
                if (dx < 28f && dy < 28f && elapsed < 650 && listener != null && getHeight() > 0) {
                    float y = event.getY() / (float) getHeight();
                    String zone = y < 0.34f ? "head" : (y < 0.70f ? "belly" : "feet");
                    listener.onTouchZone(zone);
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    public void cleanup() {
        try {
            loadUrl("about:blank");
            stopLoading();
            clearHistory();
            removeJavascriptInterface("Android");
            removeAllViews();
            destroy();
        } catch (Throwable ignored) { }
    }

    private final class Bridge {
        @JavascriptInterface
        public void characterTapped() {
            post(() -> {
                if (listener != null) listener.onCharacterTap();
            });
        }

        @JavascriptInterface
        public void modelReady() {
            post(() -> {
                if (listener != null) listener.onModelReady();
            });
        }

        @JavascriptInterface
        public void modelError(String message) {
            post(() -> {
                if (listener != null) listener.onModelError(message);
            });
        }
    }
}
