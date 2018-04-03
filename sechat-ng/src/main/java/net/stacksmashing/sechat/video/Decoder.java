package net.stacksmashing.sechat.video;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;

public class Decoder {

    private final String TAG = "DECODER";

    private ByteBuffer SpsPps;
    private ByteBuffer[] inputBuffers;
    private final ArrayDeque<ByteBuffer> videoSampleQueue;

    private Thread decodeThread;
    private Handler handler;
    private volatile boolean isRunning;

    public Decoder() {
        videoSampleQueue = new ArrayDeque<>();
        isRunning = false;
    }

    public void start(final Surface surface, final StreamManager.StreamInfo streamInfo) {
        if (isRunning) {
            stop();
        }

        final MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", streamInfo.getWidth(), streamInfo.getHeight());
        SpsPps = ByteBuffer.allocate(streamInfo.getSps().length + streamInfo.getPps().length + 8);
        SpsPps.putInt(1).put(streamInfo.getSps());
        SpsPps.putInt(1).put(streamInfo.getPps());
        mediaFormat.setByteBuffer("csd-0", SpsPps);

        handler = new Handler();
        decodeThread = new Thread(new Runnable() {

            @Override
            public void run() {

                final MediaCodec decoder;
                try {
                    decoder = MediaCodec.createDecoderByType("video/avc");
                }
                catch (IOException e) {
                    Log.e(TAG, "Could not create decoder", e);
                    return;
                }
                decoder.configure(mediaFormat, surface, null, 0);
                decoder.start();

                inputBuffers = decoder.getInputBuffers();

                isRunning = true;
                Log.d(TAG, "================= THREAD STARTED ==================");
                while (isRunning) {
                    final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    final int outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);
                    if (outputIndex >= 0) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                onOutputBufferAvailable(decoder, outputIndex, bufferInfo);
                            }
                        });
                    }
                    final int inputIndex = decoder.dequeueInputBuffer(0);
                    if (inputIndex >= 0) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                onInputBufferAvailable(decoder, inputIndex);
                            }
                        });
                    }
                }
                Log.d(TAG, "================= THREAD FINISHED ==================");
                decoder.stop();
            }
        });
        decodeThread.start();
    }

    private void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo bufferInfo) {
        if (!isRunning) {
            return;
        }

        codec.releaseOutputBuffer(index, bufferInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        Log.d(TAG, "releasing buffer with flags " + bufferInfo.flags);
    }

    private void onInputBufferAvailable(MediaCodec codec, int index) {
        if (!isRunning) {
            return;
        }

        if (videoSampleQueue.isEmpty()) {
            codec.queueInputBuffer(index, 0, 0, 0, 0);
        }
        else {
            ByteBuffer data = videoSampleQueue.remove();
            data.rewind();
            ByteBuffer buffer = inputBuffers[index];
            buffer.clear();
            buffer.put(data);
            codec.queueInputBuffer(index, 0, data.capacity(), 0, 0);
            Log.v(TAG, "enqueueing buffer with capacity " + data.capacity());
        }
    }

    public void decode(final byte[] data) {
        if (!isRunning) {
            return;
        }

        int offset = 0;
        while (offset < data.length) {
            int length = data[offset + 3] & 0xff | (data[offset + 2] & 0xff) << 8 | (data[offset + 1] & 0xff) << 16 | (data[offset] & 0xff) << 24;
            int type = data[offset + 4] & 0x1f;
            if (type == 6) {
                videoSampleQueue.add(SpsPps);
            }
            else {
                data[offset] = 0;
                data[offset + 1] = 0;
                data[offset + 2] = 0;
                data[offset + 3] = 1;
                ByteBuffer buffer = ByteBuffer.wrap(data, offset, length);
                videoSampleQueue.add(buffer);
            }
            offset += length + 4;
        }
    }

    public void stop() {
        if (isRunning) {
            isRunning = false;
            try {
                decodeThread.join();
            }
            catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            }
            videoSampleQueue.clear();
        }
    }
}
