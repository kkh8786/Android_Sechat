package net.stacksmashing.sechat.video;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public enum CameraEncoder implements Camera.PreviewCallback {
    INSTANCE;

    private Camera camera;
    private MediaCodec videoEncoder;
    private ByteBuffer[] outputBuffers;
    private boolean readSpsPps;
    private int previewWidth;
    private int previewHeight;

    private Callback callback;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void start() {
        camera = Camera.open(findFrontFacingCamera());
        setPreviewSize(camera);
        camera.setDisplayOrientation(90);
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        previewWidth = previewSize.width;
        previewHeight = previewSize.height;
        Log.d("Camera", "preview size is " + previewSize.width + " " + previewSize.height);
        camera.setPreviewCallback(this);

        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", previewWidth, previewHeight);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 125000);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);

        try {
            videoEncoder = MediaCodec.createEncoderByType("video/avc");
        }
        catch (IOException e) {
            Log.e("Camera", "Could not create codec", e);
            return;
        }

        videoEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        Surface inputSurface = videoEncoder.createInputSurface();

        videoEncoder.start();

        outputBuffers = videoEncoder.getOutputBuffers();

        try {
            camera.setPreviewDisplay(new BasicSurfaceHolder(inputSurface, previewWidth, previewHeight));
        }
        catch (IOException e) {
            Log.d("Camera", "Could not set preview display", e);
        }
        camera.startPreview();
    }

    private void setPreviewSize(Camera camera) {
        Camera.Parameters params = camera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        Camera.Size smallest = null;
        for (Camera.Size size : sizes) {
            if (smallest == null || smallest.width > size.width || smallest.height > size.height) {
                smallest = size;
            }
        }
        if (smallest != null) {
            Log.d("Camera", "Setting preview size to " + smallest.width + "x" + smallest.height);
            params.setPreviewSize(smallest.width, smallest.height);
            camera.setParameters(params);
        }
    }

    private int findFrontFacingCamera() {
        int numCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return i;
            }
        }
        return -1;
    }

    public void stop() {
        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
        camera = null;

        videoEncoder.stop();
        videoEncoder = null;

        readSpsPps = false;

        previewWidth = 0;
        previewHeight = 0;

        outputBuffers = null;

        if (callback != null) {
            callback.onFinished();
        }
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        int outputBuffer;

        while ((outputBuffer = videoEncoder.dequeueOutputBuffer(bufferInfo, 0)) != MediaCodec.INFO_TRY_AGAIN_LATER) {
            if (outputBuffer == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.d("Camera", "Output format has changed");
            }
            else if (outputBuffer == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = videoEncoder.getOutputBuffers();
            }
            else {
                byte[] output = new byte[bufferInfo.size];

                final ByteBuffer buffer = outputBuffers[outputBuffer];
                buffer.position(bufferInfo.offset);
                buffer.limit(bufferInfo.offset + bufferInfo.size);
                buffer.get(output, bufferInfo.offset, bufferInfo.size);

                if (readSpsPps) {
                    ByteBuffer frameBuffer = ByteBuffer.wrap(output);
                    frameBuffer.putInt(output.length - 4);
                    if (callback != null) {
                        callback.onFrameEncoded(output);
                    }
                }
                else {
                    SpsPps spsPps = parseSpsPps(output);

                    if (callback != null) {
                        callback.onAvcParametersEstablished(previewWidth, previewHeight, spsPps.sps, spsPps.pps);
                    }

                    readSpsPps = true;
                }

                videoEncoder.releaseOutputBuffer(outputBuffer, false);
            }
        }
    }

    public static SpsPps parseSpsPps(byte[] buffer) {
        ByteBuffer spsPpsBuffer = ByteBuffer.wrap(buffer);
        if (spsPpsBuffer.getInt() == 0x00000001) {
            Log.d("Camera", "Parsing sps/pps");
        }
        else {
            Log.d("Camera", "Unexpected data instead of sps/pps");
        }

        while (!(spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x01)) {
            ;
        }

        int ppsIndex = spsPpsBuffer.position();
        byte[] sps = new byte[ppsIndex - 8];
        System.arraycopy(buffer, 4, sps, 0, sps.length);
        byte[] pps = new byte[buffer.length - ppsIndex];
        System.arraycopy(buffer, ppsIndex, pps, 0, pps.length);

        return new SpsPps(sps, pps);
    }

    public static class SpsPps {
        public final byte[] sps, pps;

        public SpsPps(byte[] sps, byte[] pps) {
            this.sps = sps;
            this.pps = pps;
        }
    }

    public interface Callback {
        void onAvcParametersEstablished(int width, int height, byte[] sps, byte[] pps);

        void onFrameEncoded(byte[] data);

        void onFinished();
    }

    private static class BasicSurfaceHolder implements SurfaceHolder {
        private final Surface surface;
        private final int width, height;

        public BasicSurfaceHolder(Surface surface, int width, int height) {
            this.surface = surface;
            this.width = width;
            this.height = height;
        }

        @Override
        public void addCallback(Callback callback) {
        }

        @Override
        public void removeCallback(Callback callback) {
        }

        @Override
        public boolean isCreating() {
            return false;
        }

        @Override
        public void setType(int i) {
        }

        @Override
        public void setFixedSize(int i, int i2) {
        }

        @Override
        public void setSizeFromLayout() {
        }

        @Override
        public void setFormat(int i) {
        }

        @Override
        public void setKeepScreenOn(boolean b) {
        }

        @Override
        public Canvas lockCanvas() {
            return surface.lockCanvas(null);
        }

        @Override
        public Canvas lockCanvas(Rect rect) {
            return surface.lockCanvas(rect);
        }

        @Override
        public void unlockCanvasAndPost(Canvas canvas) {
            surface.unlockCanvasAndPost(canvas);
        }

        @Override
        public Rect getSurfaceFrame() {
            return new Rect(0, 0, width, height);
        }

        @Override
        public Surface getSurface() {
            return surface;
        }
    }
}
