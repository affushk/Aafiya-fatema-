package com.altaf.aafiyafatema;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.util.Random;

public class MainActivity extends Activity {
    private static final int MIC_PERMISSION_REQUEST = 77;
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            gameView = new GameView();
            setContentView(gameView);
        } catch (Throwable startupError) {
            setContentView(new SafeView(startupError));
        }
    }

    private void requestMicOrStart() {
        if (gameView == null) return;
        if (android.os.Build.VERSION.SDK_INT < 23 ||
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            gameView.startVoiceRepeat();
        } else {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION_REQUEST && gameView != null) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                gameView.startVoiceRepeat();
            } else {
                gameView.showMessage("Microphone permission needed");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) gameView.cleanupAudio();
    }

    private final class GameView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();
        private ToneGenerator tone;
        private MediaRecorder recorder;
        private MediaPlayer player;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private boolean listening = false;
        private boolean speaking = false;
        private final Bitmap character;

        private final String[] reactions = {
                "Assalamu Alaikum!", "Aafiya is happy!", "Hee hee!", "Play with me!", "So cute!", "Yay!"
        };
        private final String[] labels = {"Food", "Bath", "Sleep", "Dress", "Talk", "Play"};
        private final int[] buttonColors = {
                Color.rgb(255, 191, 97), Color.rgb(95, 199, 230), Color.rgb(151, 128, 232),
                Color.rgb(238, 117, 170), Color.rgb(105, 199, 156), Color.rgb(244, 132, 111)
        };

        private float happiness = 82, food = 68, sleep = 74, clean = 86;
        private String bubble = "Tap Aafiya Fatema";
        private boolean muted = false;
        private long reactionUntil = 0;
        private float downX, downY;

        GameView() {
            super(MainActivity.this);
            character = loadCharacter();
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        private Bitmap loadCharacter() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("aafiya_fatema.b64")));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                byte[] raw = Base64.decode(sb.toString(), Base64.DEFAULT);
                Bitmap b = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                if (b != null) return b;
            } catch (Exception ignored) { }
            Bitmap fallback = Bitmap.createBitmap(8, 10, Bitmap.Config.ARGB_8888);
            fallback.eraseColor(Color.rgb(255, 220, 235));
            return fallback;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth(), h = getHeight();
            if (w == 0 || h == 0) return;

            paint.setShader(new LinearGradient(0, 0, 0, h,
                    Color.rgb(255, 240, 249), Color.rgb(238, 231, 255), Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);

            drawSparkles(canvas, w, h);
            drawHeader(canvas, w);
            drawStats(canvas, w);
            drawCharacter(canvas, w, h);
            drawButtons(canvas, w, h);

            postInvalidateDelayed(32);
        }

        private void drawHeader(Canvas canvas, int w) {
            text.setColor(Color.rgb(95, 57, 103));
            text.setTextAlign(Paint.Align.CENTER);
            text.setFakeBoldText(true);
            text.setTextSize(w * 0.068f);
            canvas.drawText("Aafiya Fatema", w / 2f, w * 0.105f, text);

            float r = w * 0.055f, cx = w - r - w * 0.03f, cy = w * 0.085f;
            paint.setColor(Color.argb(230, 255, 255, 255));
            canvas.drawCircle(cx, cy, r, paint);
            text.setTextSize(w * 0.034f);
            text.setColor(Color.rgb(100, 75, 110));
            canvas.drawText(muted ? "OFF" : "SND", cx, cy + w * 0.012f, text);
        }

        private void drawStats(Canvas canvas, int w) {
            float y = w * 0.15f, margin = w * 0.04f, gap = w * 0.018f;
            float cardW = (w - margin * 2 - gap * 3) / 4f;
            String[] names = {"Happy", "Food", "Sleep", "Clean"};
            float[] vals = {happiness, food, sleep, clean};
            int[] colors = {
                    Color.rgb(238, 105, 162), Color.rgb(245, 168, 67),
                    Color.rgb(132, 111, 222), Color.rgb(73, 177, 213)
            };
            for (int i = 0; i < 4; i++) {
                float l = margin + i * (cardW + gap);
                RectF card = new RectF(l, y, l + cardW, y + w * 0.105f);
                paint.setColor(Color.argb(225, 255, 255, 255));
                canvas.drawRoundRect(card, w * 0.025f, w * 0.025f, paint);
                text.setTextSize(w * 0.024f);
                text.setColor(Color.rgb(92, 74, 102));
                canvas.drawText(names[i], card.centerX(), y + w * 0.035f, text);
                RectF bar = new RectF(l + w * 0.014f, y + w * 0.057f, l + cardW - w * 0.014f, y + w * 0.082f);
                paint.setColor(Color.rgb(238, 232, 241));
                canvas.drawRoundRect(bar, 20, 20, paint);
                RectF fill = new RectF(bar.left, bar.top, bar.left + bar.width() * (vals[i] / 100f), bar.bottom);
                paint.setColor(colors[i]);
                canvas.drawRoundRect(fill, 20, 20, paint);
            }
        }

        private void drawCharacter(Canvas canvas, int w, int h) {
            float top = w * 0.29f, bottom = h * 0.70f;
            double speed = speaking ? 115.0 : (listening ? 240.0 : 620.0);
            float amp = speaking ? w * 0.020f : (listening ? w * 0.012f : w * 0.008f);
            float bounce = (float) Math.sin(SystemClock.uptimeMillis() / speed) * amp;

            RectF shadow = new RectF(w * 0.25f, bottom - w * 0.015f, w * 0.75f, bottom + w * 0.04f);
            paint.setColor(Color.argb(35, 90, 50, 100));
            canvas.drawOval(shadow, paint);

            RectF dst = fitCenter(character.getWidth(), character.getHeight(),
                    w * 0.09f, top + bounce, w * 0.91f, bottom + bounce);

            paint.setColor(Color.WHITE);
            paint.setShadowLayer(w * 0.025f, 0, w * 0.01f, Color.argb(55, 90, 55, 100));
            canvas.drawRoundRect(new RectF(dst.left - 5, dst.top - 5, dst.right + 5, dst.bottom + 5),
                    w * 0.045f, w * 0.045f, paint);
            paint.clearShadowLayer();
            canvas.drawBitmap(character, null, dst, paint);

            if (SystemClock.uptimeMillis() < reactionUntil || reactionUntil == 0) {
                drawBubble(canvas, w, top - w * 0.015f);
            }
        }

        private RectF fitCenter(float bw, float bh, float l, float t, float r, float b) {
            float scale = Math.min((r - l) / bw, (b - t) / bh);
            float dw = bw * scale, dh = bh * scale;
            float cx = (l + r) / 2f, cy = (t + b) / 2f;
            return new RectF(cx - dw / 2f, cy - dh / 2f, cx + dw / 2f, cy + dh / 2f);
        }

        private void drawBubble(Canvas canvas, int w, float y) {
            text.setTextSize(w * 0.036f);
            float pad = w * 0.045f;
            float bw = Math.min(w * 0.70f, text.measureText(bubble) + pad * 2);
            RectF box = new RectF((w - bw) / 2f, y, (w + bw) / 2f, y + w * 0.12f);
            paint.setColor(Color.argb(245, 255, 255, 255));
            paint.setShadowLayer(w * 0.018f, 0, w * 0.008f, Color.argb(40, 80, 40, 90));
            canvas.drawRoundRect(box, w * 0.04f, w * 0.04f, paint);
            paint.clearShadowLayer();
            Path tail = new Path();
            tail.moveTo(w * 0.48f, box.bottom);
            tail.lineTo(w * 0.52f, box.bottom);
            tail.lineTo(w * 0.50f, box.bottom + w * 0.035f);
            tail.close();
            canvas.drawPath(tail, paint);
            text.setColor(Color.rgb(108, 70, 115));
            canvas.drawText(bubble, w / 2f, box.centerY() + w * 0.013f, text);
        }

        private void drawButtons(Canvas canvas, int w, int h) {
            float margin = w * 0.05f, gap = w * 0.025f;
            float buttonW = (w - margin * 2 - gap * 2) / 3f;
            float buttonH = h * 0.095f, startY = h * 0.765f;

            for (int i = 0; i < 6; i++) {
                int row = i / 3, col = i % 3;
                float l = margin + col * (buttonW + gap);
                float t = startY + row * (buttonH + h * 0.018f);
                RectF box = new RectF(l, t, l + buttonW, t + buttonH);
                paint.setColor(buttonColors[i]);
                paint.setShadowLayer(w * 0.016f, 0, w * 0.008f, Color.argb(50, 100, 60, 100));
                canvas.drawRoundRect(box, w * 0.035f, w * 0.035f, paint);
                paint.clearShadowLayer();
                text.setColor(Color.WHITE);
                text.setTextSize(w * 0.042f);
                canvas.drawText(labels[i], box.centerX(), box.centerY() + w * 0.015f, text);
            }
        }

        private void drawSparkles(Canvas canvas, int w, int h) {
            long t = SystemClock.uptimeMillis();
            for (int i = 0; i < 9; i++) {
                float x = (i * 97 % 91) / 100f * w + w * 0.04f;
                float y = h * (0.20f + ((i * 43) % 47) / 100f);
                float pulse = 0.5f + 0.5f * (float) Math.sin(t / 500.0 + i);
                paint.setColor(Color.argb((int) (35 + pulse * 70), 255, 255, 255));
                canvas.drawCircle(x, y, w * (0.004f + pulse * 0.006f), paint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downX = event.getX();
                downY = event.getY();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float x = event.getX(), y = event.getY();
                float w = getWidth(), h = getHeight();
                if (Math.abs(x - downX) > w * 0.08f || Math.abs(y - downY) > w * 0.08f) return true;

                float muteCx = w - w * 0.055f - w * 0.03f, muteCy = w * 0.085f;
                if (distance(x, y, muteCx, muteCy) < w * 0.075f) {
                    muted = !muted;
                    react(muted ? "Sound off" : "Sound on", 0);
                    return true;
                }

                if (y > h * 0.765f) {
                    int index = buttonIndexAt(x, y, w, h);
                    if (index >= 0) handleButton(index);
                } else if (y > w * 0.25f && y < h * 0.72f) {
                    happiness = clamp(happiness + 2);
                    react(reactions[random.nextInt(reactions.length)], 1);
                }
                return true;
            }
            return true;
        }

        private int buttonIndexAt(float x, float y, float w, float h) {
            float margin = w * 0.05f, gap = w * 0.025f;
            float buttonW = (w - margin * 2 - gap * 2) / 3f;
            float buttonH = h * 0.095f, startY = h * 0.765f;
            for (int i = 0; i < 6; i++) {
                int row = i / 3, col = i % 3;
                RectF box = new RectF(
                        margin + col * (buttonW + gap),
                        startY + row * (buttonH + h * 0.018f),
                        margin + col * (buttonW + gap) + buttonW,
                        startY + row * (buttonH + h * 0.018f) + buttonH);
                if (box.contains(x, y)) return i;
            }
            return -1;
        }

        private void handleButton(int index) {
            switch (index) {
                case 0:
                    food = clamp(food + 10); happiness = clamp(happiness + 3);
                    react("Yummy! Thank you!", 2); break;
                case 1:
                    clean = clamp(clean + 12); happiness = clamp(happiness + 2);
                    react("Splish splash!", 3); break;
                case 2:
                    sleep = clamp(sleep + 12);
                    react("Good night... zzz", 4); break;
                case 3:
                    react("Dress room comes in Phase 3", 5); break;
                case 4:
                    MainActivity.this.requestMicOrStart(); break;
                case 5:
                    happiness = clamp(happiness + 8);
                    food = clamp(food - 2);
                    sleep = clamp(sleep - 3);
                    react("Let's play!", 7); break;
            }
        }

        void showMessage(String message) {
            bubble = message;
            reactionUntil = SystemClock.uptimeMillis() + 2600;
            invalidate();
        }

        void startVoiceRepeat() {
            if (listening || speaking) {
                showMessage("Wait... Aafiya is talking");
                return;
            }
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

                listening = true;
                bubble = "I'm listening... say something!";
                reactionUntil = SystemClock.uptimeMillis() + 5000;
                invalidate();

                handler.postDelayed(this::stopAndReplay, 3500);
            } catch (Throwable error) {
                cleanupAudio();
                showMessage("Microphone couldn't start");
            }
        }

        private void stopAndReplay() {
            if (!listening) return;
            try {
                if (recorder != null) recorder.stop();
            } catch (Throwable ignored) { }
            try {
                if (recorder != null) recorder.release();
            } catch (Throwable ignored) { }
            recorder = null;
            listening = false;

            if (muted) {
                showMessage("Sound is OFF - tap SND first");
                return;
            }

            try {
                File voiceFile = new File(getCacheDir(), "aafiya_voice.m4a");
                player = new MediaPlayer();
                player.setDataSource(voiceFile.getAbsolutePath());
                player.prepare();
                try {
                    PlaybackParams params = new PlaybackParams();
                    params.setPitch(1.35f);
                    params.setSpeed(1.04f);
                    player.setPlaybackParams(params);
                } catch (Throwable ignored) { }

                speaking = true;
                bubble = "Aafiya Fatema says...";
                reactionUntil = SystemClock.uptimeMillis() + 6000;
                happiness = clamp(happiness + 5);
                player.setOnCompletionListener(mp -> {
                    speaking = false;
                    try { mp.release(); } catch (Throwable ignored) { }
                    player = null;
                    showMessage("Hee hee! Say it again!");
                });
                player.setOnErrorListener((mp, what, extra) -> {
                    speaking = false;
                    try { mp.release(); } catch (Throwable ignored) { }
                    player = null;
                    showMessage("Let's try again");
                    return true;
                });
                player.start();
                invalidate();
            } catch (Throwable error) {
                speaking = false;
                cleanupAudio();
                showMessage("I couldn't repeat that - try again");
            }
        }

        void cleanupAudio() {
            handler.removeCallbacksAndMessages(null);
            listening = false;
            speaking = false;
            try {
                if (recorder != null) recorder.stop();
            } catch (Throwable ignored) { }
            try {
                if (recorder != null) recorder.release();
            } catch (Throwable ignored) { }
            recorder = null;
            try {
                if (player != null) {
                    player.stop();
                    player.release();
                }
            } catch (Throwable ignored) { }
            player = null;
        }

        private void react(String message, int toneIndex) {
            bubble = message;
            reactionUntil = SystemClock.uptimeMillis() + 2200;
            if (!muted) {
                try {
                    if (tone == null) tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 55);
                    int[] tones = {
                            ToneGenerator.TONE_PROP_BEEP, ToneGenerator.TONE_PROP_ACK,
                            ToneGenerator.TONE_PROP_BEEP2, ToneGenerator.TONE_DTMF_6,
                            ToneGenerator.TONE_DTMF_3, ToneGenerator.TONE_DTMF_9,
                            ToneGenerator.TONE_PROP_BEEP2, ToneGenerator.TONE_PROP_ACK
                    };
                    tone.startTone(tones[Math.abs(toneIndex) % tones.length], 100);
                } catch (Throwable ignored) { }
            }
            invalidate();
        }

        private float clamp(float v) {
            return Math.max(0, Math.min(100, v));
        }

        private float distance(float x1, float y1, float x2, float y2) {
            float dx = x1 - x2, dy = y1 - y2;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
    }
    private final class SafeView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        SafeView(Throwable error) {
            super(MainActivity.this);
            setBackgroundColor(Color.rgb(255, 242, 249));
        }
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            p.setTextAlign(Paint.Align.CENTER);
            p.setColor(Color.rgb(111, 72, 121));
            p.setTextSize(w * 0.075f);
            p.setFakeBoldText(true);
            canvas.drawText("Aafiya Fatema", w / 2f, h * 0.42f, p);
            p.setTextSize(w * 0.04f);
            p.setFakeBoldText(false);
            canvas.drawText("Game is ready", w / 2f, h * 0.50f, p);
            canvas.drawText("Please reopen if needed", w / 2f, h * 0.56f, p);
        }
    }

}
