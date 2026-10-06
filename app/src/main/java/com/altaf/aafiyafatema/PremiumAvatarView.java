package com.altaf.aafiyafatema;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
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
    private long lastTapAt;
    private float lastTapX;
    private float lastTapY;
    private long burstStartedAt;
    private int burstTapCount;
    private boolean moved;
    private boolean longPressFired;
    private final Handler gestureHandler = new Handler(Looper.getMainLooper());
    private final float touchSlop;

    private final Runnable longPressRunnable = () -> {
        if (!moved && listener != null && getWidth() > 0 && getHeight() > 0) {
            longPressFired = true;
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            listener.onTouchZone("cuddle");
            playPetSound("purr");
            react("cuddle");
        }
    };

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    public PremiumAvatarView(Context context) {
        super(context);

        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setBackgroundColor(Color.TRANSPARENT);
        setLayerType(LAYER_TYPE_HARDWARE, null);
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
        String safe = sanitize(pose, "idle");
        post(() -> evaluateJavascript("window.setAafiyaPose('" + safe + "')", null));
    }

    public void react(String reaction) {
        setPose(reaction);
    }

    public void showEffect(String effect) {
        String safe = sanitize(effect, "heart");
        post(() -> evaluateJavascript("window.showAafiyaEffect('" + safe + "')", null));
    }

    public void playPetSound(String sound) {
        String safe = sanitize(sound, "meow");
        post(() -> evaluateJavascript("window.playPetSound('" + safe + "')", null));
    }

    public void setSoundEnabled(boolean enabled) {
        post(() -> evaluateJavascript(
                "window.setPetSoundEnabled(" + (enabled ? "true" : "false") + ")", null));
    }

    public void setTheme(int themeIndex) {
        int safe = Math.max(0, Math.min(3, themeIndex));
        post(() -> evaluateJavascript("window.setPetTheme(" + safe + ")", null));
    }

    public void setRoom(String room) {
        String safe = sanitize(room, "home");
        post(() -> evaluateJavascript("window.setPetRoom('" + safe + "')", null));
    }

    public void setAccessory(int accessoryIndex) {
        int safe = Math.max(0, Math.min(4, accessoryIndex));
        post(() -> evaluateJavascript("window.setPetAccessory(" + safe + ")", null));
    }

    public void lookAt(float nx, float ny) {
        float x = Math.max(0f, Math.min(1f, nx));
        float y = Math.max(0f, Math.min(1f, ny));
        post(() -> evaluateJavascript(
                "window.lookAtTouch(" + x + "," + y + ")", null));
    }

    public void touchAt(float nx, float ny) {
        float x = Math.max(0f, Math.min(1f, nx));
        float y = Math.max(0f, Math.min(1f, ny));
        post(() -> evaluateJavascript(
                "window.touchAt(" + x + "," + y + ")", null));
    }

    public void scratchAt(float nx, float ny) {
        float x = Math.max(0f, Math.min(1f, nx));
        float y = Math.max(0f, Math.min(1f, ny));
        post(() -> evaluateJavascript(
                "window.showScratch(" + x + "," + y + ")", null));
    }

    public void setSpeaking(boolean speaking) {
        setPose(speaking ? "talk" : "idle");
        if (speaking) {
            showEffect("music");
            playPetSound("chirp");
        }
    }

    public void setListening(boolean listening) {
        setPose(listening ? "listen" : "idle");
    }

    public void wave() {
        setPose("wave");
        playPetSound("chirp");
    }

    public void happy() {
        setPose("happy");
        showEffect("heart");
        playPetSound("meow");
    }

    public void sleep() {
        setPose("sleep");
        showEffect("zzz");
        playPetSound("purr");
    }

    public void eat() {
        setPose("food");
        showEffect("food");
        playPetSound("eat");
    }

    public void bath() {
        setPose("bath");
        showEffect("bath");
        playPetSound("splash");
    }

    public void play() {
        setPose("play");
        showEffect("star");
        playPetSound("chirp");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                downAt = System.currentTimeMillis();
                moved = false;
                longPressFired = false;
                gestureHandler.removeCallbacks(longPressRunnable);
                gestureHandler.postDelayed(longPressRunnable, 650);
                if (getWidth() > 0 && getHeight() > 0) {
                    float nx = x / getWidth();
                    float ny = y / getHeight();
                    lookAt(nx, ny);
                    touchAt(nx, ny);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (Math.hypot(x - downX, y - downY) > touchSlop) {
                    moved = true;
                    gestureHandler.removeCallbacks(longPressRunnable);
                }
                if (getWidth() > 0 && getHeight() > 0) {
                    lookAt(x / getWidth(), y / getHeight());
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                gestureHandler.removeCallbacks(longPressRunnable);
                break;

            case MotionEvent.ACTION_UP:
                gestureHandler.removeCallbacks(longPressRunnable);
                if (longPressFired) break;

                float dx = x - downX;
                float dy = y - downY;
                float distance = (float) Math.hypot(dx, dy);
                long elapsed = System.currentTimeMillis() - downAt;

                if (distance > Math.max(48f, touchSlop * 3f)) {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    if (listener != null) listener.onTouchZone("pet");
                    react("pet");
                    playPetSound("purr");
                    showEffect("heart");
                    break;
                }

                if (elapsed < 650 && listener != null && getHeight() > 0 && getWidth() > 0) {
                    long now = System.currentTimeMillis();

                    if (burstStartedAt == 0L || now - burstStartedAt > 2200) {
                        burstStartedAt = now;
                        burstTapCount = 1;
                    } else {
                        burstTapCount++;
                    }

                    if (burstTapCount >= 6) {
                        burstTapCount = 0;
                        burstStartedAt = now;
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        listener.onTouchZone("annoyed");
                        react("annoyed");
                        playPetSound("grumpy");
                        break;
                    }

                    boolean doubleTap = now - lastTapAt < 330
                            && Math.hypot(x - lastTapX, y - lastTapY) < touchSlop * 5f;

                    lastTapAt = now;
                    lastTapX = x;
                    lastTapY = y;

                    if (doubleTap) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        listener.onTouchZone("double");
                        react("jump");
                        playPetSound("chirp");
                        showEffect("star");
                    } else {
                        String zone = classifyZone(x / getWidth(), y / getHeight());
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                        listener.onTouchZone(zone);
                    }
                }
                break;
        }

        return true;
    }

    private String classifyZone(float nx, float ny) {
        if (ny < 0.27f && nx < 0.42f) return "ear_left";
        if (ny < 0.27f && nx > 0.58f) return "ear_right";

        if (ny < 0.44f) {
            if (nx > 0.40f && nx < 0.60f && ny > 0.25f) return "nose";
            return "head";
        }

        if (ny < 0.72f) return "belly";
        return nx < 0.50f ? "paw_left" : "paw_right";
    }

    private String sanitize(String value, String fallback) {
        if (value == null || value.isEmpty()) return fallback;
        return value.replaceAll("[^a-zA-Z0-9_\\-]", "");
    }

    public void cleanup() {
        try {
            gestureHandler.removeCallbacksAndMessages(null);
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
