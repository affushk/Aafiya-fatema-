package com.altaf.aafiyafatema;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
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
    private static final String PREFS = "aafiya_pet_state";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final String[] voiceNames = {"Cute", "Tiny", "Funny", "Deep", "Normal"};
    private final float[] voicePitch = {1.34f, 1.55f, 1.16f, 0.88f, 1.00f};
    private final float[] voiceSpeed = {1.03f, 1.08f, 1.16f, 0.95f, 1.00f};
    private final String[] themeNames = {"Pink", "Sky", "Cream", "Mint"};

    private PremiumAvatarView characterView;
    private TextView bubble, starText;
    private ProgressBar happyBar, foodBar, sleepBar, cleanBar;
    private Button voiceButton;

    private SharedPreferences prefs;
    private int happy, food, sleep, clean, stars, voicePreset, themeIndex;
    private int playVariant = 0;
    private boolean muted = false;
    private boolean recording = false;
    private boolean speaking = false;
    private boolean modelReady = false;
    private boolean speechDetected = false;
    private long recordStartedAt = 0L;
    private long lastLoudAt = 0L;
    private int idleIndex = 0;

    private MediaRecorder recorder;
    private MediaPlayer player;

    private final Runnable voiceMonitor = new Runnable() {
        @Override
        public void run() {
            if (!recording || recorder == null) return;
            long now = System.currentTimeMillis();
            long elapsed = now - recordStartedAt;
            int amp = 0;
            try { amp = recorder.getMaxAmplitude(); } catch (Throwable ignored) { }

            if (amp > 1600) {
                speechDetected = true;
                lastLoudAt = now;
            }

            if (speechDetected && elapsed > 900 && now - lastLoudAt > 850) {
                stopAndReplay();
                return;
            }

            if (!speechDetected && elapsed > 4300) {
                stopRecordingOnly();
                setBubble("Mujhe awaaz nahi sunai di - phir bolo");
                if (characterView != null) characterView.setListening(false);
                return;
            }

            if (elapsed > 7200) {
                stopAndReplay();
                return;
            }

            handler.postDelayed(this, 120);
        }
    };

    private final Runnable idleReaction = new Runnable() {
        @Override
        public void run() {
            if (modelReady && !recording && !speaking && characterView != null) {
                if (food < 22) {
                    setBubble("Meow... mujhe bhook lagi hai");
                    characterView.react("hungry");
                    characterView.showEffect("food");
                } else if (sleep < 18) {
                    setBubble("Mujhe neend aa rahi hai... zzz");
                    characterView.sleep();
                } else if (clean < 18) {
                    setBubble("Bath time? 🫧");
                    characterView.react("bath");
                    characterView.showEffect("bath");
                } else if (happy < 22) {
                    setBubble("Thoda play karein?");
                    characterView.react("sad");
                } else {
                    String[] lines = {
                            "Meow! Main yahan hoon",
                            "Head tap = smile 😻",
                            "Right paw tap = scratch!",
                            "Double tap karke dekho!",
                            "Long press = cuddle 💗",
                            "Swipe karke pet karo"
                    };
                    setBubble(lines[idleIndex % lines.length]);

                    int v = idleIndex % 6;
                    if (v == 0) characterView.wave();
                    else if (v == 1) characterView.react("stretch");
                    else if (v == 2) characterView.react("shy");
                    else if (v == 3) characterView.happy();
                    else if (v == 4) characterView.react("surprise");
                    else characterView.react("smile");
                }
                idleIndex++;
            }
            handler.postDelayed(this, 8500);
        }
    };

    private final Runnable gameTick = new Runnable() {
        @Override
        public void run() {
            if (!recording && !speaking) {
                food = clamp(food - 1);
                if ((idleIndex % 2) == 0) sleep = clamp(sleep - 1);
                if ((idleIndex % 3) == 0) clean = clamp(clean - 1);
                if (food < 25 || sleep < 20 || clean < 20) happy = clamp(happy - 1);
                updateBars();
                saveState();
            }
            handler.postDelayed(this, 90000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadStateWithOfflineDecay();

        getWindow().setStatusBarColor(Color.rgb(255, 240, 249));
        getWindow().setNavigationBarColor(Color.rgb(255, 245, 251));
        buildUi();

        giveDailyRewardIfNeeded();
        handler.postDelayed(idleReaction, 7000);
        handler.postDelayed(gameTick, 90000);
    }

    private void loadStateWithOfflineDecay() {
        happy = prefs.getInt("happy", 82);
        food = prefs.getInt("food", 72);
        sleep = prefs.getInt("sleep", 76);
        clean = prefs.getInt("clean", 88);
        stars = prefs.getInt("stars", 25);
        voicePreset = prefs.getInt("voice_preset", 0);
        themeIndex = prefs.getInt("theme_index", 0);
        if (voicePreset < 0 || voicePreset >= voiceNames.length) voicePreset = 0;
        if (themeIndex < 0 || themeIndex >= themeNames.length) themeIndex = 0;

        long now = System.currentTimeMillis();
        long last = prefs.getLong("last_seen", now);
        long hours = Math.min(18, Math.max(0, (now - last) / 3600000L));
        if (hours > 0) {
            food = clamp(food - (int) hours * 2);
            sleep = clamp(sleep - (int) hours);
            clean = clamp(clean - (int) hours);
            happy = clamp(happy - (int) (hours / 2));
        }
    }

    private void giveDailyRewardIfNeeded() {
        long today = System.currentTimeMillis() / 86400000L;
        long lastDay = prefs.getLong("daily_reward_day", -1L);
        if (today != lastDay) {
            stars += 20;
            prefs.edit().putLong("daily_reward_day", today).apply();
            updateStars();
            setBubble("Daily gift: +20 stars!");
            if (characterView != null) characterView.showEffect("star");
            saveState();
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(6), dp(12), dp(10));
        root.setBackground(makeGradient(Color.rgb(255, 241, 249), Color.rgb(239, 232, 255)));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Aafiya Fatema");
        title.setTextColor(Color.rgb(90, 55, 101));
        title.setTextSize(27);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1f));

        Button sound = roundedButton("SND", Color.WHITE, Color.rgb(96, 71, 106));
        sound.setTextSize(12);
        sound.setOnClickListener(v -> {
            muted = !muted;
            sound.setText(muted ? "OFF" : "SND");
            if (characterView != null) characterView.setSoundEnabled(!muted);
            setBubble(muted ? "Sound off" : "Sound on");
        });
        header.addView(sound, new LinearLayout.LayoutParams(dp(58), dp(46)));
        root.addView(header);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER);
        stats.setPadding(0, dp(3), 0, dp(5));

        LinearLayout s1 = statCard("Happy", happy, Color.rgb(235, 101, 159));
        LinearLayout s2 = statCard("Food", food, Color.rgb(245, 169, 66));
        LinearLayout s3 = statCard("Sleep", sleep, Color.rgb(139, 112, 226));
        LinearLayout s4 = statCard("Clean", clean, Color.rgb(76, 182, 216));
        happyBar = (ProgressBar) s1.getChildAt(1);
        foodBar = (ProgressBar) s2.getChildAt(1);
        sleepBar = (ProgressBar) s3.getChildAt(1);
        cleanBar = (ProgressBar) s4.getChildAt(1);

        stats.addView(s1, new LinearLayout.LayoutParams(0, dp(60), 1f));
        stats.addView(space(dp(4)));
        stats.addView(s2, new LinearLayout.LayoutParams(0, dp(60), 1f));
        stats.addView(space(dp(4)));
        stats.addView(s3, new LinearLayout.LayoutParams(0, dp(60), 1f));
        stats.addView(space(dp(4)));
        stats.addView(s4, new LinearLayout.LayoutParams(0, dp(60), 1f));
        root.addView(stats);

        LinearLayout info = new LinearLayout(this);
        info.setGravity(Gravity.CENTER_VERTICAL);
        info.setPadding(dp(3), 0, dp(3), dp(5));

        starText = new TextView(this);
        starText.setTextSize(14);
        starText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        starText.setTextColor(Color.rgb(111, 78, 112));
        starText.setGravity(Gravity.CENTER_VERTICAL);
        updateStars();
        info.addView(starText, new LinearLayout.LayoutParams(0, dp(34), 1f));

        voiceButton = roundedButton("Voice: " + voiceNames[voicePreset],
                Color.argb(235, 255, 255, 255), Color.rgb(103, 72, 112));
        voiceButton.setTextSize(12);
        voiceButton.setOnClickListener(v -> cycleVoiceFilter());
        info.addView(voiceButton, new LinearLayout.LayoutParams(dp(118), dp(34)));
        root.addView(info);

        FrameLayout stage = new FrameLayout(this);
        stage.setBackground(roundRect(Color.argb(100, 255, 255, 255), dp(26)));

        characterView = new PremiumAvatarView(this);
        characterView.setListener(new PremiumAvatarView.Listener() {
            @Override
            public void onCharacterTap() {
                // Web viewer tap fallback. Native touch zones handle the detailed reaction.
            }

            @Override
            public void onTouchZone(String zone) {
                reactToTouch(zone);
            }

            @Override
            public void onModelReady() {
                modelReady = true;
                characterView.setSoundEnabled(!muted);
                characterView.setTheme(themeIndex);
                setBubble("Kitty ready! Head smile, paw scratch, double-tap & cuddle");
            }

            @Override
            public void onModelError(String message) {
                modelReady = false;
                setBubble("Kitty load issue - internet check karo");
            }
        });
        stage.addView(characterView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        bubble = new TextView(this);
        bubble.setText("Cute white kitty loading...");
        bubble.setTextSize(15);
        bubble.setTextColor(Color.rgb(101, 66, 111));
        bubble.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bubble.setGravity(Gravity.CENTER);
        bubble.setPadding(dp(14), dp(7), dp(14), dp(7));
        bubble.setBackground(roundRect(Color.argb(238, 255, 255, 255), dp(20)));
        FrameLayout.LayoutParams bubbleLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, dp(44));
        bubbleLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        bubbleLp.bottomMargin = dp(9);
        stage.addView(bubble, bubbleLp);

        TextView live = new TextView(this);
        live.setText("WHITE CAT 3D");
        live.setTextSize(10);
        live.setTextColor(Color.WHITE);
        live.setGravity(Gravity.CENTER);
        live.setBackground(roundRect(Color.rgb(222, 77, 142), dp(12)));
        FrameLayout.LayoutParams liveLp = new FrameLayout.LayoutParams(dp(92), dp(26));
        liveLp.gravity = Gravity.TOP | Gravity.END;
        liveLp.topMargin = dp(7);
        liveLp.rightMargin = dp(7);
        stage.addView(live, liveLp);

        root.addView(stage, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        GridLayout actions = new GridLayout(this);
        actions.setColumnCount(3);
        actions.setRowCount(2);
        actions.setPadding(0, dp(8), 0, 0);

        addAction(actions, "Food", Color.rgb(255, 188, 88), () -> {
            food = clamp(food + 16);
            happy = clamp(happy + 3);
            earnStars(1);
            updateBars();
            setBubble("Meow! Yummy!");
            characterView.eat();
            saveState();
        });

        addAction(actions, "Bath", Color.rgb(83, 190, 219), () -> {
            clean = clamp(clean + 18);
            happy = clamp(happy + 2);
            earnStars(1);
            updateBars();
            setBubble("Meow! Clean & fresh!");
            characterView.bath();
            saveState();
        });

        addAction(actions, "Sleep", Color.rgb(142, 111, 226), () -> {
            sleep = clamp(sleep + 18);
            happy = clamp(happy + 1);
            updateBars();
            setBubble("Kitty sleepy... zzz");
            characterView.sleep();
            saveState();
        });

        addAction(actions, "Style", Color.rgb(232, 100, 157), () -> {
            themeIndex = (themeIndex + 1) % themeNames.length;
            characterView.setTheme(themeIndex);
            characterView.showEffect("star");
            setBubble("Style: " + themeNames[themeIndex]);
            prefs.edit().putInt("theme_index", themeIndex).apply();
        });

        addAction(actions, "Talk", Color.rgb(94, 196, 152), this::requestMicOrStart);

        addAction(actions, "Play", Color.rgb(244, 126, 105), () -> {
            happy = clamp(happy + 12);
            food = clamp(food - 2);
            sleep = clamp(sleep - 2);
            earnStars(2);
            updateBars();

            playVariant = (playVariant + 1) % 4;
            if (playVariant == 0) {
                setBubble("Meow! Jump!");
                characterView.react("jump");
                characterView.showEffect("star");
            } else if (playVariant == 1) {
                setBubble("Scratch attack! 🐾");
                characterView.react("scratch");
                characterView.showEffect("scratch");
            } else if (playVariant == 2) {
                setBubble("Cute stretch!");
                characterView.react("stretch");
                characterView.playPetSound("purr");
            } else {
                setBubble("Shy kitty 💗");
                characterView.react("shy");
                characterView.showEffect("heart");
            }

            saveState();
        });

        root.addView(actions, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(166)));

        setContentView(root);
    }

    private void reactToTouch(String zone) {
        if (recording || speaking || characterView == null) return;

        switch (zone) {
            case "ear_left":
            case "ear_right":
                happy = clamp(happy + 2);
                setBubble("Ear twitch! 😺");
                characterView.react("ear");
                characterView.playPetSound("chirp");
                break;

            case "nose":
                happy = clamp(happy + 2);
                setBubble("Boop! Cute nose 💗");
                characterView.react("nose");
                characterView.playPetSound("boop");
                break;

            case "head":
                happy = clamp(happy + 5);
                earnStars(1);
                setBubble("Aww... cute smile! 😻");
                characterView.react("smile");
                characterView.playPetSound("purr");
                characterView.showEffect("smile");
                characterView.showEffect("heart");
                break;

            case "belly":
                happy = clamp(happy + 5);
                setBubble("Tummy tickles! Meow!");
                characterView.react("belly");
                characterView.playPetSound("chirp");
                characterView.showEffect("heart");
                break;

            case "paw_left":
                happy = clamp(happy + 3);
                setBubble("High paw! 🐾");
                characterView.react("paw");
                characterView.playPetSound("meow");
                break;

            case "paw_right":
                happy = clamp(happy + 4);
                earnStars(1);
                setBubble("Claw scratch! ✨");
                characterView.react("scratch");
                characterView.showEffect("scratch");
                break;

            case "cuddle":
                happy = clamp(happy + 7);
                earnStars(2);
                setBubble("Prrrr... cuddle mode 💗");
                characterView.react("cuddle");
                characterView.playPetSound("purr");
                characterView.showEffect("heart");
                break;

            case "double":
                happy = clamp(happy + 6);
                earnStars(2);
                food = clamp(food - 1);
                sleep = clamp(sleep - 1);
                setBubble("Woohoo! Big jump!");
                characterView.react("jump");
                characterView.showEffect("star");
                characterView.playPetSound("chirp");
                break;

            case "pet":
                happy = clamp(happy + 5);
                setBubble("Purr... aur pet karo");
                characterView.react("pet");
                characterView.playPetSound("purr");
                characterView.showEffect("heart");
                break;

            case "annoyed":
                setBubble("Hey! Itna fast nahi 😾");
                characterView.react("annoyed");
                characterView.playPetSound("grumpy");
                break;

            default:
                happy = clamp(happy + 2);
                characterView.happy();
                setBubble("Meow!");
                break;
        }

        updateBars();
        saveState();
    }

    private void cycleVoiceFilter() {
        voicePreset = (voicePreset + 1) % voiceNames.length;
        voiceButton.setText("Voice: " + voiceNames[voicePreset]);
        setBubble(voiceNames[voicePreset] + " voice selected");
        prefs.edit().putInt("voice_preset", voicePreset).apply();
    }

    private void requestMicOrStart() {
        if (recording || speaking) {
            setBubble(recording ? "Main sun rahi hoon..." : "Wait... Aafiya bol rahi hai");
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
                setBubble("Talk ke liye microphone permission chahiye");
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
            speechDetected = false;
            recordStartedAt = System.currentTimeMillis();
            lastLoudAt = recordStartedAt;
            characterView.setListening(true);
            setBubble("Bolo... main sun rahi hoon");
            handler.postDelayed(voiceMonitor, 120);
        } catch (Throwable e) {
            cleanupAudio();
            setBubble("Microphone start nahi hua");
        }
    }

    private void stopRecordingOnly() {
        handler.removeCallbacks(voiceMonitor);
        if (!recording) return;
        recording = false;
        try { if (recorder != null) recorder.stop(); } catch (Throwable ignored) { }
        try { if (recorder != null) recorder.release(); } catch (Throwable ignored) { }
        recorder = null;
    }

    private void stopAndReplay() {
        if (!recording) return;
        stopRecordingOnly();
        characterView.setListening(false);

        if (!speechDetected) {
            setBubble("Phir se bolo - mujhe awaaz nahi mili");
            return;
        }

        if (muted) {
            setBubble("Sound OFF hai - SND on karo");
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
                pp.setPitch(voicePitch[voicePreset]);
                pp.setSpeed(voiceSpeed[voicePreset]);
                player.setPlaybackParams(pp);
            } catch (Throwable ignored) { }

            speaking = true;
            characterView.setSpeaking(true);
            setBubble("Kitty repeats: " + voiceNames[voicePreset]);
            happy = clamp(happy + 5);
            earnStars(2);
            updateBars();

            player.setOnCompletionListener(mp -> {
                speaking = false;
                characterView.setSpeaking(false);
                try { mp.release(); } catch (Throwable ignored) { }
                player = null;
                setBubble("Meow! Phir se bolo!");
                characterView.wave();
                saveState();
            });
            player.setOnErrorListener((mp, what, extra) -> {
                speaking = false;
                characterView.setSpeaking(false);
                try { mp.release(); } catch (Throwable ignored) { }
                player = null;
                setBubble("Voice repeat error - phir try karo");
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

    private LinearLayout statCard(String label, int value, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(7), dp(4), dp(7), dp(4));
        box.setBackground(roundRect(Color.argb(230, 255, 255, 255), dp(17)));

        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(11);
        t.setTextColor(Color.rgb(86, 73, 95));
        t.setGravity(Gravity.CENTER);
        box.addView(t, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(23)));

        ProgressBar p = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        p.setMax(100);
        p.setProgress(value);
        p.setProgressTintList(ColorStateList.valueOf(color));
        p.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(235, 229, 239)));
        box.addView(p, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(14)));
        return box;
    }

    private void addAction(GridLayout grid, String label, int color, Runnable action) {
        Button b = roundedButton(label, color, Color.WHITE);
        b.setTextSize(18);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setOnClickListener(v -> action.run());

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(72);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        grid.addView(b, lp);
    }

    private Button roundedButton(String text, int bg, int fg) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(fg);
        b.setAllCaps(false);
        b.setBackground(roundRect(bg, dp(21)));
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
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{top, bottom});
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private void earnStars(int amount) {
        stars += Math.max(0, amount);
        updateStars();
        if (characterView != null && amount >= 2) characterView.showEffect("coin");
    }

    private void updateStars() {
        if (starText != null) starText.setText("⭐ " + stars + "   Daily rewards ON");
    }

    private void setBubble(String message) {
        if (bubble != null) bubble.setText(message);
    }

    private void updateBars() {
        if (happyBar != null) happyBar.setProgress(happy);
        if (foodBar != null) foodBar.setProgress(food);
        if (sleepBar != null) sleepBar.setProgress(sleep);
        if (cleanBar != null) cleanBar.setProgress(clean);
    }

    private void saveState() {
        if (prefs == null) return;
        prefs.edit()
                .putInt("happy", happy)
                .putInt("food", food)
                .putInt("sleep", sleep)
                .putInt("clean", clean)
                .putInt("stars", stars)
                .putInt("voice_preset", voicePreset)
                .putInt("theme_index", themeIndex)
                .putLong("last_seen", System.currentTimeMillis())
                .apply();
    }

    private void cleanupAudio() {
        handler.removeCallbacks(voiceMonitor);
        speechDetected = false;

        if (recording) {
            recording = false;
            try { if (recorder != null) recorder.stop(); } catch (Throwable ignored) { }
        }
        try { if (recorder != null) recorder.release(); } catch (Throwable ignored) { }
        recorder = null;

        speaking = false;
        try {
            if (player != null) {
                player.stop();
                player.release();
            }
        } catch (Throwable ignored) { }
        player = null;

        if (characterView != null) {
            characterView.setListening(false);
            characterView.setSpeaking(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveState();
        cleanupAudio();
        handler.removeCallbacks(idleReaction);
        handler.removeCallbacks(gameTick);
        if (characterView != null) characterView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (characterView != null) characterView.onResume();
        handler.removeCallbacks(idleReaction);
        handler.removeCallbacks(gameTick);
        handler.postDelayed(idleReaction, 7000);
        handler.postDelayed(gameTick, 90000);
    }

    @Override
    protected void onDestroy() {
        saveState();
        cleanupAudio();
        handler.removeCallbacksAndMessages(null);
        if (characterView != null) characterView.cleanup();
        super.onDestroy();
    }
}
