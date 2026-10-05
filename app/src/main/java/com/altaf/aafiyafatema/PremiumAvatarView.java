package com.altaf.aafiyafatema;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class PremiumAvatarView extends WebView {
    public interface Listener {
        void onCharacterTap();
        void onModelReady();
        void onModelError(String message);
    }

    private Listener listener;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    public PremiumAvatarView(Context context) {
        super(context);

        setBackgroundColor(Color.TRANSPARENT);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

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

    public void setSpeaking(boolean speaking) {
        setPose(speaking ? "talk" : "idle");
    }

    public void setListening(boolean listening) {
        setPose(listening ? "listen" : "idle");
    }

    public void wave() {
        setPose("wave");
    }

    public void happy() {
        setPose("happy");
    }

    public void cleanup() {
        try {
            loadUrl("about:blank");
            stopLoading();
            clearHistory();
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
