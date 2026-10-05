package com.altaf.aafiyafatema;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Aafiya3DView extends GLSurfaceView {
    public interface Listener { void onCharacterTap(); }

    private final CharacterRenderer renderer;
    private Listener listener;

    public Aafiya3DView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        renderer = new CharacterRenderer();
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        setPreserveEGLContextOnPause(true);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setListening(boolean value) {
        queueEvent(() -> renderer.listening = value);
    }

    public void setSpeaking(boolean value) {
        queueEvent(() -> renderer.speaking = value);
    }

    public void wave() {
        queueEvent(() -> renderer.waveUntil = SystemClock.uptimeMillis() + 1700);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            wave();
            if (listener != null) listener.onCharacterTap();
            performClick();
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private static final class CharacterRenderer implements Renderer {
        private static final float[] SKIN = {1.00f, 0.73f, 0.60f, 1f};
        private static final float[] PINK = {0.96f, 0.45f, 0.68f, 1f};
        private static final float[] PINK_DARK = {0.82f, 0.25f, 0.50f, 1f};
        private static final float[] CREAM = {1.00f, 0.94f, 0.86f, 1f};
        private static final float[] WHITE = {1.00f, 0.99f, 0.98f, 1f};
        private static final float[] BROWN = {0.24f, 0.10f, 0.06f, 1f};
        private static final float[] BLACK = {0.035f, 0.025f, 0.03f, 1f};
        private static final float[] LAVENDER = {0.68f, 0.52f, 0.92f, 1f};
        private static final float[] SHADOW = {0.48f, 0.33f, 0.55f, 0.16f};

        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] pv = new float[16];
        private final float[] model = new float[16];
        private final float[] mvp = new float[16];

        private Sphere sphere;
        private int program;
        private int aPosition, aNormal, uMvp, uModel, uColor;
        volatile boolean listening = false;
        volatile boolean speaking = false;
        volatile long waveUntil = 0L;

        @Override
        public void onSurfaceCreated(javax.microedition.khronos.egl.EGLConfig config) {
            GLES20.glClearColor(0.99f, 0.93f, 0.98f, 1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            program = createProgram(VERTEX, FRAGMENT);
            aPosition = GLES20.glGetAttribLocation(program, "aPosition");
            aNormal = GLES20.glGetAttribLocation(program, "aNormal");
            uMvp = GLES20.glGetUniformLocation(program, "uMVP");
            uModel = GLES20.glGetUniformLocation(program, "uModel");
            uColor = GLES20.glGetUniformLocation(program, "uColor");
            sphere = new Sphere(30, 40);
        }

        @Override
        public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            float aspect = width / (float) Math.max(1, height);
            Matrix.perspectiveM(projection, 0, 35f, aspect, 1f, 20f);
            Matrix.setLookAtM(view, 0,
                    0f, 0.15f, 7.4f,
                    0f, -0.35f, 0f,
                    0f, 1f, 0f);
            Matrix.multiplyMM(pv, 0, projection, 0, view, 0);
        }

        @Override
        public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glUseProgram(program);

            long now = SystemClock.uptimeMillis();
            float bob = (float) Math.sin(now * 0.0023) * 0.055f;
            if (listening) bob += (float) Math.sin(now * 0.010) * 0.025f;
            if (speaking) bob += (float) Math.sin(now * 0.016) * 0.065f;

            drawShadow();
            drawCharacter(now, bob);
        }

        private void drawCharacter(long now, float bob) {
            float blinkPhase = (now % 3600L);
            float blink = blinkPhase > 3300 && blinkPhase < 3460 ? 0.08f : 1f;
            float talk = speaking ? 0.10f + 0.16f * Math.abs((float) Math.sin(now * 0.030)) : 0.055f;
            float nod = listening ? (float) Math.sin(now * 0.008) * 4.0f : 0f;

            // Legs and shoes
            part(-0.34f, -2.15f + bob, 0.00f, 0.25f, 0.68f, 0.25f, 0, 0, 0, CREAM);
            part( 0.34f, -2.15f + bob, 0.00f, 0.25f, 0.68f, 0.25f, 0, 0, 0, CREAM);
            part(-0.34f, -2.72f + bob, 0.17f, 0.38f, 0.25f, 0.52f, -4, 0, 0, PINK);
            part( 0.34f, -2.72f + bob, 0.17f, 0.38f, 0.25f, 0.52f, -4, 0, 0, PINK);

            // Dress and torso
            part(0f, -1.22f + bob, 0f, 0.92f, 1.12f, 0.62f, 0, 0, 0, PINK);
            part(0f, -1.68f + bob, 0f, 1.12f, 0.82f, 0.80f, 0, 0, 0, PINK);
            part(0f, -1.47f + bob, 0.66f, 0.22f, 0.18f, 0.09f, 0, 0, 0, CREAM);

            // Neck
            part(0f, -0.15f + bob, 0.10f, 0.28f, 0.34f, 0.28f, 0, 0, 0, SKIN);

            // Hijab shell behind face
            part(0f, 0.78f + bob, -0.10f, 1.12f, 1.25f, 0.93f, nod, 0, 0, PINK);
            // Hijab shoulder drape
            part(0f, -0.12f + bob, -0.07f, 1.08f, 0.58f, 0.62f, 0, 0, 0, PINK_DARK);

            // Face in front of hijab
            part(0f, 0.83f + bob, 0.55f, 0.82f, 0.91f, 0.69f, nod, 0, 0, SKIN);

            // Small scarf/bow accents
            part(-0.78f, 1.56f + bob, 0.25f, 0.30f, 0.20f, 0.18f, 0, 0, -25, PINK_DARK);
            part(-0.55f, 1.68f + bob, 0.20f, 0.25f, 0.18f, 0.16f, 0, 0, 25, PINK_DARK);
            part(-0.66f, 1.62f + bob, 0.37f, 0.10f, 0.10f, 0.08f, 0, 0, 0, CREAM);

            // Eyes
            eye(-0.29f, 1.05f + bob, 1.08f, blink);
            eye( 0.29f, 1.05f + bob, 1.08f, blink);

            // Brows
            part(-0.30f, 1.34f + bob, 1.02f, 0.25f, 0.055f, 0.055f, 0, 0, -8, BROWN);
            part( 0.30f, 1.34f + bob, 1.02f, 0.25f, 0.055f, 0.055f, 0, 0, 8, BROWN);

            // Nose and cheeks
            part(0f, 0.91f + bob, 1.16f, 0.09f, 0.10f, 0.07f, 0, 0, 0, new float[]{0.95f,0.57f,0.48f,1f});
            part(-0.48f, 0.78f + bob, 1.00f, 0.16f, 0.10f, 0.06f, 0, 0, 0, new float[]{1f,0.52f,0.58f,0.35f});
            part( 0.48f, 0.78f + bob, 1.00f, 0.16f, 0.10f, 0.06f, 0, 0, 0, new float[]{1f,0.52f,0.58f,0.35f});

            // Mouth
            part(0f, 0.60f + bob, 1.13f, 0.24f, talk, 0.065f, 0, 0, 0, PINK_DARK);
            if (speaking) {
                part(0f, 0.59f + bob, 1.18f, 0.13f, talk * 0.52f, 0.035f, 0, 0, 0, BLACK);
            }

            // Arms; right arm waves after a tap
            boolean wave = now < waveUntil;
            float waveAngle = wave ? -48f + 26f * (float)Math.sin(now * 0.018) : -12f;
            float rightX = wave ? 1.02f : 0.91f;
            float rightY = wave ? -0.20f : -0.78f;
            part(-0.92f, -0.75f + bob, 0.08f, 0.28f, 0.88f, 0.30f, 0, 0, 13, CREAM);
            part(rightX, rightY + bob, 0.08f, 0.28f, 0.88f, 0.30f, 0, 0, waveAngle, CREAM);

            // Hands
            part(-1.04f, -1.37f + bob, 0.14f, 0.25f, 0.28f, 0.24f, 0, 0, 0, SKIN);
            if (wave) {
                part(1.36f, 0.37f + bob, 0.10f, 0.26f, 0.29f, 0.24f, 0, 0, 0, SKIN);
            } else {
                part(1.03f, -1.38f + bob, 0.14f, 0.25f, 0.28f, 0.24f, 0, 0, 0, SKIN);
            }

            // Purse
            part(-0.90f, -1.48f + bob, 0.65f, 0.42f, 0.43f, 0.16f, 0, 0, -10, CREAM);
            part(-0.90f, -1.50f + bob, 0.82f, 0.10f, 0.08f, 0.04f, 0, 0, 0, PINK_DARK);
        }

        private void eye(float x, float y, float z, float blink) {
            part(x, y, z, 0.23f, 0.28f * blink, 0.12f, 0, 0, 0, WHITE);
            part(x, y, z + 0.10f, 0.145f, 0.19f * blink, 0.075f, 0, 0, 0, BROWN);
            part(x, y, z + 0.16f, 0.072f, 0.105f * blink, 0.040f, 0, 0, 0, BLACK);
            if (blink > 0.5f) {
                part(x - 0.035f, y + 0.060f, z + 0.205f, 0.030f, 0.040f, 0.020f, 0, 0, 0, WHITE);
            }
        }

        private void drawShadow() {
            part(0f, -3.04f, -0.40f, 1.35f, 0.10f, 0.82f, 0, 0, 0, SHADOW);
        }

        private void part(float x, float y, float z,
                          float sx, float sy, float sz,
                          float rx, float ry, float rz,
                          float[] color) {
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, x, y, z);
            if (rx != 0) Matrix.rotateM(model, 0, rx, 1, 0, 0);
            if (ry != 0) Matrix.rotateM(model, 0, ry, 0, 1, 0);
            if (rz != 0) Matrix.rotateM(model, 0, rz, 0, 0, 1);
            Matrix.scaleM(model, 0, sx, sy, sz);
            Matrix.multiplyMM(mvp, 0, pv, 0, model, 0);

            GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0);
            GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0);
            GLES20.glUniform4fv(uColor, 1, color, 0);
            sphere.draw(aPosition, aNormal);
        }

        private static int createProgram(String vertex, String fragment) {
            int vs = compile(GLES20.GL_VERTEX_SHADER, vertex);
            int fs = compile(GLES20.GL_FRAGMENT_SHADER, fragment);
            int p = GLES20.glCreateProgram();
            GLES20.glAttachShader(p, vs);
            GLES20.glAttachShader(p, fs);
            GLES20.glLinkProgram(p);
            return p;
        }

        private static int compile(int type, String code) {
            int s = GLES20.glCreateShader(type);
            GLES20.glShaderSource(s, code);
            GLES20.glCompileShader(s);
            return s;
        }

        private static final String VERTEX =
                "attribute vec3 aPosition;\n" +
                "attribute vec3 aNormal;\n" +
                "uniform mat4 uMVP;\n" +
                "uniform mat4 uModel;\n" +
                "varying vec3 vNormal;\n" +
                "varying vec3 vWorld;\n" +
                "void main(){\n" +
                " vec4 w=uModel*vec4(aPosition,1.0);\n" +
                " vWorld=w.xyz;\n" +
                " vNormal=mat3(uModel)*aNormal;\n" +
                " gl_Position=uMVP*vec4(aPosition,1.0);\n" +
                "}";

        private static final String FRAGMENT =
                "precision mediump float;\n" +
                "uniform vec4 uColor;\n" +
                "varying vec3 vNormal;\n" +
                "varying vec3 vWorld;\n" +
                "void main(){\n" +
                " vec3 n=normalize(vNormal);\n" +
                " vec3 l=normalize(vec3(-0.45,0.80,1.00));\n" +
                " float d=max(dot(n,l),0.0);\n" +
                " float rim=pow(1.0-max(n.z,0.0),2.0)*0.14;\n" +
                " vec3 c=uColor.rgb*(0.50+0.55*d)+vec3(rim);\n" +
                " gl_FragColor=vec4(c,uColor.a);\n" +
                "}";

        private static final class Sphere {
            private final FloatBuffer vertices;
            private final FloatBuffer normals;
            private final ShortBuffer indices;
            private final int indexCount;

            Sphere(int stacks, int slices) {
                int vc = (stacks + 1) * (slices + 1);
                float[] v = new float[vc * 3];
                float[] n = new float[vc * 3];
                short[] idx = new short[stacks * slices * 6];

                int p = 0;
                for (int i = 0; i <= stacks; i++) {
                    float phi = (float)Math.PI * i / stacks;
                    float sy = (float)Math.cos(phi);
                    float sr = (float)Math.sin(phi);
                    for (int j = 0; j <= slices; j++) {
                        float theta = (float)(2.0 * Math.PI * j / slices);
                        float x = sr * (float)Math.cos(theta);
                        float z = sr * (float)Math.sin(theta);
                        v[p] = x; n[p++] = x;
                        v[p] = sy; n[p++] = sy;
                        v[p] = z; n[p++] = z;
                    }
                }

                int q = 0;
                for (int i = 0; i < stacks; i++) {
                    for (int j = 0; j < slices; j++) {
                        short a = (short)(i * (slices + 1) + j);
                        short b = (short)(a + slices + 1);
                        idx[q++] = a;
                        idx[q++] = b;
                        idx[q++] = (short)(a + 1);
                        idx[q++] = (short)(a + 1);
                        idx[q++] = b;
                        idx[q++] = (short)(b + 1);
                    }
                }

                vertices = ByteBuffer.allocateDirect(v.length * 4)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                vertices.put(v).position(0);
                normals = ByteBuffer.allocateDirect(n.length * 4)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                normals.put(n).position(0);
                indices = ByteBuffer.allocateDirect(idx.length * 2)
                        .order(ByteOrder.nativeOrder()).asShortBuffer();
                indices.put(idx).position(0);
                indexCount = idx.length;
            }

            void draw(int pos, int normal) {
                vertices.position(0);
                normals.position(0);
                GLES20.glEnableVertexAttribArray(pos);
                GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 0, vertices);
                GLES20.glEnableVertexAttribArray(normal);
                GLES20.glVertexAttribPointer(normal, 3, GLES20.GL_FLOAT, false, 0, normals);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount,
                        GLES20.GL_UNSIGNED_SHORT, indices);
            }
        }
    }
}
