package net.stacksmashing.sechat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ColorPickerView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private OnColorChangeListener listener;

    private volatile float value = 1;
    private float hue = 0;
    private float saturation = 1;

    private int program;
    private int attrPosition;
    private int valuePosition;
    private final int[] buffer = new int[1];

    private static final float[] VERTICES = new float[]{
            -1, -1, -1, 1, 1, 1,
            1, 1, 1, -1, -1, -1
    };

    private static final ByteBuffer VERTEX_BUFFER;

    private static final String VERTEX_SHADER_SOURCE =
            "attribute vec2 position;\n"
                    + "varying vec2 pos;\n"
                    + "void main(void) {\n"
                    + "pos = position;\n"
                    + "gl_Position = vec4(position, 0, 1);\n"
                    + "}";

    public static final String FRAGMENT_SHADER_SOURCE =
            "varying mediump vec2 pos;\n"
                    + "uniform mediump float value;\n"
                    + "void main(void) {\n"
                    + "if (length(pos) > 1.0) discard;\n"
                    + "mediump vec3 c = vec3(atan(pos.y, pos.x) / 6.2831853 + 0.5, value == 0.0 ? 0.0 : length(pos) / value, value);\n"
                    + "mediump vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);\n"
                    + "mediump vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);\n"
                    + "gl_FragColor = vec4(c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y), 1.0);\n"
                    + "}";

    static {
        VERTEX_BUFFER = ByteBuffer.allocateDirect(4 * VERTICES.length);
        VERTEX_BUFFER.order(ByteOrder.nativeOrder());
        FloatBuffer vertexBuffer = VERTEX_BUFFER.asFloatBuffer();
        vertexBuffer.put(VERTICES);
        vertexBuffer.position(0);
    }

    public ColorPickerView(Context context) {
        super(context);
        init();
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        if (!isInEditMode()) {
            setZOrderOnTop(true);
        }
        setRenderer(this);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public void setValue(float value) {
        this.value = value;
        requestRender();
        notifyListener();
    }

    public float getValue() {
        return value;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig eglConfig) {
        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vertexShader, VERTEX_SHADER_SOURCE);
        GLES20.glCompileShader(vertexShader);
        Log.d("GL", "Vertex shader: " + GLES20.glGetShaderInfoLog(vertexShader));

        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fragmentShader, FRAGMENT_SHADER_SOURCE);
        GLES20.glCompileShader(fragmentShader);
        Log.d("GL", "Fragment shader: " + GLES20.glGetShaderInfoLog(fragmentShader));

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        attrPosition = GLES20.glGetAttribLocation(program, "position");
        valuePosition = GLES20.glGetUniformLocation(program, "value");

        GLES20.glGenBuffers(1, buffer, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * VERTICES.length, VERTEX_BUFFER, GLES20.GL_STATIC_DRAW);

        GLES20.glClearColor(0, 0, 0, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(program);
        GLES20.glUniform1f(valuePosition, value);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[0]);
        GLES20.glVertexAttribPointer(attrPosition, 2, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glEnableVertexAttribArray(attrPosition);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_MOVE:
                updateHueAndSaturationFromPosition(event.getX(), event.getY());
                notifyListener();
                break;
        }
        return true;
    }

    private void updateHueAndSaturationFromPosition(float x, float y) {
        float cx = (float) getMeasuredWidth() / 2;
        float cy = (float) getMeasuredHeight() / 2;
        float nx = (x - cx) / cx;
        float ny = (y - cy) / cy;
        hue = (float) (Math.atan2(-ny, nx) / (2 * Math.PI) + 0.5) * 360;
        saturation = (float) Math.sqrt(nx * nx + ny * ny);
    }

    public void setColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
        requestRender();
        notifyListener();
    }

    public int getColor() {
        return Color.HSVToColor(new float[]{hue, saturation, value});
    }

    public void setOnColorChangeListener(OnColorChangeListener listener) {
        this.listener = listener;
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onColorChange(getColor());
        }
    }

    public interface OnColorChangeListener {
        void onColorChange(int color);
    }
}
