package com.altaf.aafiyafatema;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.PlaybackParams;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends Activity {
    private static final int MIC_PERMISSION_REQUEST = 77;

    private PremiumAvatarView characterView;
    private TextView bubble;
    private ProgressBar happyBar, foodBar, sleepBar, cleanBar;

    private int happy = 82, food = 68, sleep = 74, clean = 86;
    private boolean muted = false;
    private boolean recording = false;
    private boolean speaking = false;

    private MediaRecorder recorder;
    private MediaPlayer player;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(255, 240, 249));
        getWindow().setNavigationBarColor(Color.rgb(255, 245, 251));
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(8), dp(14), dp(12));
        root.setBackground(makeGradient(Color.rgb(255, 241, 249), Color.rgb(239, 232, 255)));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Aafiya Fatema");
        title.setTextColor(Color.rgb(90, 55, 101));
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(54), 1f));

        Button sound = roundedButton("SND", Color.WHITE, Color.rgb(96, 71, 106));
        sound.setTextSize(13);
        sound.setOnClickListener(v -> {
            muted = !muted;
            sound.setText(muted ? "OFF" : "SND");
            setBubble(muted ? "Sound off" : "Sound on");
        });
        header.addView(sound, new LinearLayout.LayoutParams(dp(58), dp(48)));
        root.addView(header);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER);
        stats.setPadding(0, dp(4), 0, dp(6));

        LinearLayout s1 = statCard("Happy", happy, Color.rgb(235, 101, 159));
        LinearLayout s2 = statCard("Food", food, Color.rgb(245, 169, 66));
        LinearLayout s3 = statCard("Sleep", sleep, Color.rgb(139, 112, 226));
        LinearLayout s4 = statCard("Clean", clean, Color.rgb(76, 182, 216));
        happyBar = (ProgressBar) s1.getChildAt(1);
        foodBar = (ProgressBar) s2.getChildAt(1);
        sleepBar = (ProgressBar) s3.getChildAt(1);
        cleanBar = (ProgressBar) s4.getChildAt(1);

        stats.addView(s1, new LinearLayout.LayoutParams(0, dp(64), 1f));
        stats.addView(space(dp(5)));
        stats.addView(s2, new LinearLayout.LayoutParams(0, dp(64), 1f));
        stats.addView(space(dp(5)));
        stats.addView(s3, new LinearLayout.LayoutParams(0, dp(64), 1f));
        stats.addView(space(dp(5)));
        stats.addView(s4, new LinearLayout.LayoutParams(0, dp(64), 1f));
        root.addView(stats);

        FrameLayout stage = new FrameLayout(this);
        stage.setBackground(roundRect(Color.argb(105, 255, 255, 255), dp(28)));

        characterView = new PremiumAvatarView(this);
        characterView.setListener(new PremiumAvatarView.Listener() {
            @Override
            public void onCharacterTap() {
                happy = Math.min(100, happy + 2);
                happyBar.setProgress(happy);
                setBubble("Aafiya is happy!");
                characterView.wave();
            }

            @Override
            public void onModelReady() {
                setBubble("Aafiya Fatema is ready!");
            }

            @Override
            public void onModelError(String message) {
                setBubble("Premium 3D ke liye internet on rakho");
            }
        });
        stage.addView(characterView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        bubble = new TextView(this);
        bubble.setText("Premium 3D Aafiya Fatema loading...");
        bubble.setTextSize(17);
        bubble.setTextColor(Color.rgb(101, 66, 111));
        bubble.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bubble.setGravity(Gravity.CENTER);
        bubble.setPadding(dp(16), dp(9), dp(16), dp(9));
        bubble.setBackground(roundRect(Color.argb(240, 255, 255, 255), dp(22)));
        FrameLayout.LayoutParams bubbleLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, dp(50));
        bubbleLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        bubbleLp.topMargin = dp(10);
        stage.addView(bubble, bubbleLp);

        TextView live = new TextView(this);
        live.setText("PREMIUM 3D");
        live.setTextSize(11);
        live.setTextColor(Color.WHITE);
        live.setGravity(Gravity.CENTER);
        live.setBackground(roundRect(Color.rgb(222, 77, 142), dp(12)));
        FrameLayout.LayoutParams liveLp = new FrameLayout.LayoutParams(dp(92), dp(28));
        liveLp.gravity = Gravity.TOP | Gravity.END;
        liveLp.topMargin = dp(12);
        liveLp.rightMargin = dp(12);
        stage.addView(live, liveLp);

        root.addView(stage, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        GridLayout actions = new GridLayout(this);
        actions.setColumnCount(3);
        actions.setRowCount(2);
        actions.setPadding(0, dp(10), 0, 0);

        addAction(actions, "Food", Color.rgb(255, 188, 88), () -> {
            food = Math.min(100, food + 12);
            happy = Math.min(100, happy + 3);
            updateBars();
            setBubble("Yummy! Thank you!");
            characterView.happy();
        });

        addAction(actions, "Bath", Color.rgb(83, 190, 219), () -> {
            clean = Math.min(100, clean + 14);
            happy = Math.min(100, happy + 2);
            updateBars();
            setBubble("Splish splash!");
            characterView.happy();
        });

        addAction(actions, "Sleep", Color.rgb(142, 111, 226), () -> {
            sleep = Math.min(100, sleep + 14);
            updateBars();
            setBubble("Good night... zzz");
        });

        addAction(actions, "Dress", Color.rgb(232, 100, 157), () -> {
            setBubble("3D dress room comes next");
            characterView.wave();
        });

        addAction(actions, "Talk", Color.rgb(94, 196, 152), this::requestMicOrStart);

        addAction(actions, "Play", Color.rgb(244, 126, 105), () -> {
            happy = Math.min(100, happy + 9);
            food = Math.max(0, food - 2);
            sleep = Math.max(0, sleep - 3);
            updateBars();
            setBubble("Let's play!");
            characterView.happy();
        });

        root.addView(actions, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(176)));

        setContentView(root);
    }

    private LinearLayout statCard(String label, int value, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(8), dp(5), dp(8), dp(5));
        box.setBackground(roundRect(Color.argb(228, 255, 255, 255), dp(18)));

        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(12);
        t.setTextColor(Color.rgb(86, 73, 95));
        t.setGravity(Gravity.CENTER);
        box.addView(t, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(25)));

        ProgressBar p = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        p.setMax(100);
        p.setProgress(value);
        p.setProgressTintList(ColorStateList.valueOf(color));
        p.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(235, 229, 239)));
        box.addView(p, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(15)));
        return box;
    }

    private void addAction(GridLayout grid, String label, int color, Runnable action) {
        Button b = roundedButton(label, color, Color.WHITE);
        b.setTextSize(19);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setOnClickListener(v -> action.run());

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(76);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        grid.addView(b, lp);
    }

    private Button roundedButton(String text, int bg, int fg) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(fg);
        b.setAllCaps(false);
        b.setBackground(roundRect(bg, dp(22)));
        b.setPadding(dp(4), 0, dp(4), 0);
        return b;
    }

    private View space(int width) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(width, 1));
        return v;
    }

    private GradientDrawable roundRect(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private GradientDrawable makeGradient(int top, int bottom) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{top, bottom});
        return d;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void setBubble(String message) {
        if (bubble != null) bubble.setText(message);
    }

    private void updateBars() {
        happyBar.setProgress(happy);
        foodBar.setProgress(food);
        sleepBar.setProgress(sleep);
        cleanBar.setProgress(clean);
    }

    private void requestMicOrStart() {
        if (recording || speaking) {
            setBubble("Wait... Aafiya is talking");
            return;
        }
        if (android.os.Build.VERSION.SDK_INT < 23 ||
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceRepeat();
        } else {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION_REQUEST) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRepeat();
            } else {
                setBubble("Microphone permission needed for Talk");
            }
        }
    }

    private void startVoiceRepeat() {
        cleanupAudio();
        try {
            File voiceFile = new File(getCacheDir(), "aafiya_voice.m4a");
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(96000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(voiceFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();

            recording = true;
            characterView.setListening(true);
            setBubble("I'm listening... bolo Aafiya!");
            handler.postDelayed(this::stopAndReplay, 3500);
        } catch (Throwable e) {
            cleanupAudio();
            setBubble("Microphone couldn't start");
        }
    }

    private void stopAndReplay() {
        if (!recording) return;
        try { recorder.stop(); } catch (Throwable ignored) { }
        try { recorder.release(); } catch (Throwable ignored) { }
        recorder = null;
        recording = false;
        characterView.setListening(false);

        if (muted) {
            setBubble("Sound OFF hai - pehle SND on karo");
            return;
        }

        try {
            File voiceFile = new File(getCacheDir(), "aafiya_voice.m4a");
            player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(voiceFile.getAbsolutePath());
            player.prepare();
            try {
                PlaybackParams pp = new PlaybackParams();
                pp.setPitch(1.35f);
                pp.setSpeed(1.04f);
                player.setPlaybackParams(pp);
            } catch (Throwable ignored) { }

            speaking = true;
            characterView.setSpeaking(true);
            setBubble("Aafiya Fatema says...");
            happy = Math.min(100, happy + 5);
            updateBars();

            player.setOnCompletionListener(mp -> {
                speaking = false;
                characterView.setSpeaking(false);
                try { mp.release(); } catch (Throwable ignored) { }
                player = null;
                setBubble("Hee hee! Phir se bolo!");
                characterView.wave();
            });
            player.setOnErrorListener((mp, what, extra) -> {
                speaking = false;
                characterView.setSpeaking(false);
                try { mp.release(); } catch (Throwable ignored) { }
                player = null;
                setBubble("Phir se try karo");
                return true;
            });
            player.start();
        } catch (Throwable e) {
            speaking = false;
            characterView.setSpeaking(false);
            cleanupAudio();
            setBubble("Voice repeat nahi hua - phir try karo");
        }
    }

    private void cleanupAudio() {
        handler.removeCallbacksAndMessages(null);
        recording = false;
        speaking = false;
        if (characterView != null) {
            characterView.setListening(false);
            characterView.setSpeaking(false);
        }
        try { if (recorder != null) recorder.stop(); } catch (Throwable ignored) { }
        try { if (recorder != null) recorder.release(); } catch (Throwable ignored) { }
        recorder = null;
        try {
            if (player != null) {
                player.stop();
                player.release();
            }
        } catch (Throwable ignored) { }
        player = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        cleanupAudio();
        if (characterView != null) characterView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (characterView != null) characterView.onResume();
    }

    @Override
    protected void onDestroy() {
        cleanupAudio();
        if (characterView != null) characterView.cleanup();
        super.onDestroy();
    }
}
