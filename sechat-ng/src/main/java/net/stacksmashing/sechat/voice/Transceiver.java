package net.stacksmashing.sechat.voice;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AudioEffect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import net.stacksmashing.sechat.R;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;

class Transceiver implements AudioManager.OnAudioFocusChangeListener {
    private static final String TAG = "Transceiver";

    private static final HandlerThread AUXILIARY_THREAD = new HandlerThread("Audio management thread");
    private static final Handler AUXILIARY_HANDLER;

    private static MediaPlayer BUSY_TONE_PLAYER;

    static {
        AUXILIARY_THREAD.start();
        AUXILIARY_HANDLER = new Handler(AUXILIARY_THREAD.getLooper());
    }

    private static final int SAMPLE_RATE = 48000;
    private static final int NUM_CHANNELS = 1;
    private static final int CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNEL_OUT_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORDER_BUFFER_SIZE = 4 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN_CONFIG, AUDIO_FORMAT);
    private static final int PLAYER_BUFFER_SIZE = 2 * AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT_CONFIG, AUDIO_FORMAT);

    private final AudioManager am;
    private final String host;
    private final int port, streamId, numStreams;
    private final Crypto crypto;
    private final Context context;

    private MediaPlayer dialTonePlayer;

    private Thread senderThread, audioRecorderThread, receiverThread, audioPlayerThread;
    private DatagramSocket socket;

    private boolean started = false;

    public Transceiver(Context context, String host, int port, byte[] key, int streamId, int numStreams) {
        this.host = host;
        this.port = port;
        this.streamId = streamId;
        this.numStreams = numStreams;
        this.crypto = new Crypto(key);
        this.context = context.getApplicationContext();
        am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    /* FIXME: This is something super ugly and doesn't even exactly belong here. */
    public static void playBusyTone(final Context context) {
        AUXILIARY_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                if (BUSY_TONE_PLAYER != null) {
                    BUSY_TONE_PLAYER.stop();
                    BUSY_TONE_PLAYER.release();
                }

                BUSY_TONE_PLAYER = MediaPlayer.create(context.getApplicationContext(), R.raw.busy);
                BUSY_TONE_PLAYER.start();

                AUXILIARY_HANDLER.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BUSY_TONE_PLAYER.stop();
                        BUSY_TONE_PLAYER.release();
                        BUSY_TONE_PLAYER = null;
                    }
                }, 1000 * BUSY_TONE_PLAYER.getDuration());
            }
        });
    }

    private void syncPlayDialTone() {
        syncStopDialTone();

        try {
            dialTonePlayer = MediaPlayer.create(context, R.raw.ring);
            dialTonePlayer.setLooping(true);
            dialTonePlayer.start();
        }
        catch (Exception e) {
            Log.d(TAG, "Could not play dial tone", e);
        }
    }

    private void syncStopDialTone() {
        if (dialTonePlayer != null) {
            dialTonePlayer.stop();
            dialTonePlayer.release();
            dialTonePlayer = null;
        }
    }

    public void playDialTone() {
        AUXILIARY_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                syncPlayDialTone();
            }
        });
    }

    public void stopDialTone() {
        AUXILIARY_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                syncStopDialTone();
            }
        });
    }

    public void start() {
        AUXILIARY_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                if (started) {
                    return;
                }

                if (am.requestAudioFocus(Transceiver.this, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    am.setMode(AudioManager.MODE_IN_COMMUNICATION);

                    try {
                        socket = new DatagramSocket(null);
                        socket.setReuseAddress(true);
                        socket.setSoTimeout(250);
                    }
                    catch (Exception e) {
                        Log.e(TAG, "Could not create listener socket on port " + port, e);
                        return;
                    }

                    RingBuffer playerRingBuffer = new DumbRingBuffer();
                    RingBuffer recorderRingBuffer = new DumbRingBuffer();

                    started = true;

                    audioRecorderThread = new AudioRecorderThread(recorderRingBuffer);
                    audioRecorderThread.start();

                    senderThread = new SenderThread(recorderRingBuffer, crypto, socket, host, port, streamId);
                    senderThread.start();

                    audioPlayerThread = new AudioPlayerThread(playerRingBuffer, streamId, numStreams);
                    audioPlayerThread.start();

                    receiverThread = new ReceiverThread(playerRingBuffer, crypto, socket, streamId, numStreams);
                    receiverThread.start();
                }
            }
        });
    }

    private class DumbRingBuffer implements RingBuffer {
        private final short[][][] buffers = new short[numStreams][16][];
        private final int[] writeOffsets = new int[numStreams];
        private final int[] readOffsets = new int[numStreams];
        private long numReads = 0, numWrites = 0;

        @Override
        public void produce(int stream, short[] data, int length) {
            final int offset = writeOffsets[stream];
            writeOffsets[stream] = (offset + 1) & 0xf;
            buffers[stream][offset] = Arrays.copyOf(data, length);
            numWrites++;
        }

        private final short[] empty = new short[PLAYER_BUFFER_SIZE];

        @Override
        public short[] consume(int stream) {
            final int offset = readOffsets[stream];
            readOffsets[stream] = (offset + 1) & 0xf;
            short[] buf = buffers[stream][offset];
            buffers[stream][offset] = null;
            numReads++;
            return buf != null ? buf : empty;
        }

        @Override
        public String stats() {
            return "numReads " + numReads + " numWrites " + numWrites;
        }
    }

    public void stop() {
        AUXILIARY_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                if (!started) {
                    return;
                }

                started = false;

                syncStopDialTone();

                am.setMode(AudioManager.MODE_NORMAL);
                am.abandonAudioFocus(Transceiver.this);

                audioRecorderThread.interrupt();
                audioRecorderThread = null;

                senderThread.interrupt();
                senderThread = null;

                audioPlayerThread.interrupt();
                audioPlayerThread = null;

                receiverThread.interrupt();
                receiverThread = null;

                socket.close();
                socket = null;
            }
        });
    }

    @Override
    public void onAudioFocusChange(int status) {
        Log.d(TAG, "Audio focus change: " + status);
    }

    public static class SenderThread extends Thread {
        private final OpusEncoder encoder;
        private final DatagramSocket socket;
        private final String host;
        private final int port;
        private final int streamId;
        private final Crypto crypto;
        private final RingBuffer ringBuffer;
        private final byte[] encoded = new byte[8192];
        private final short[] samples = new short[1920];
        private int samplesPos = 0;

        private SenderThread(RingBuffer ringBuffer, Crypto crypto, DatagramSocket socket, String host, int port, int streamId) {
            this.ringBuffer = ringBuffer;
            this.crypto = crypto;
            this.socket = socket;
            this.host = host;
            this.port = port;
            this.streamId = streamId;
            encoder = new OpusEncoder(SAMPLE_RATE, NUM_CHANNELS);
        }

        @Override
        public void run() {
            InetAddress address;
            try {
                address = InetAddress.getByName(host);
            }
            catch (Exception e) {
                Log.e(TAG, "Could not resolve host " + host);
                return;
            }

            while (!isInterrupted()) {
                short[] unalignedSamples;
                synchronized (ringBuffer) {
                    try {
                        ringBuffer.wait(1000);
                    }
                    catch (InterruptedException e) {
                        break;
                    }

                    unalignedSamples = ringBuffer.consume(0);
                }
                int unalignedPos = 0;

                while (unalignedPos < unalignedSamples.length) {
                    int spaceLeft = samples.length - samplesPos;
                    int unalignedLeft = unalignedSamples.length - unalignedPos;
                    int toCopy = Math.min(spaceLeft, unalignedLeft);

                    System.arraycopy(unalignedSamples, unalignedPos, samples, samplesPos, toCopy);

                    unalignedPos += toCopy;
                    samplesPos += toCopy;

                    if (samplesPos == samples.length) {
                        samplesPos = 0;
                        try {
                            int encodedSize = encoder.encode(samples, samples.length, encoded);

                            byte[] encrypted = crypto.encryptBytes(encoded, encodedSize);

                            byte[] data = new byte[encrypted.length + 5];
                            int dataLength = encrypted.length;
                            System.arraycopy(encrypted, 0, data, 5, encrypted.length);
                            data[0] = (byte) streamId;
                            data[1] = (byte) (dataLength >> 24);
                            data[2] = (byte) ((dataLength >> 16) & 0xff);
                            data[3] = (byte) ((dataLength >> 8) & 0xff);
                            data[4] = (byte) (dataLength & 0xff);
                            socket.send(new DatagramPacket(data, data.length, address, port));
                        }
                        catch (OpusEncoder.OpusEncodingException e) {
                            Log.d(TAG, "Could not encode audio", e);
                        }
                        catch (IOException e) {
                            Log.d(TAG, "Could not send data", e);
                        }
                    }
                }
            }
        }
    }

    private static class AudioRecorderThread extends Thread {
        private final AudioRecord recorder;
        private AcousticEchoCanceler aec;
        private final short[] samples = new short[RECORDER_BUFFER_SIZE];
        private final RingBuffer ringBuffer;

        private AudioRecorderThread(RingBuffer ringBuffer) {
            this.ringBuffer = ringBuffer;
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_IN_CONFIG, AUDIO_FORMAT, RECORDER_BUFFER_SIZE);

        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            recorder.startRecording();
            Log.d(TAG, "AEC available: " + AcousticEchoCanceler.isAvailable());
            aec = AcousticEchoCanceler.create(recorder.getAudioSessionId());
            if (aec != null) {
                aec.setEnabled(true);
                Log.d(TAG, "AEC enabled: " + aec.getEnabled());
                AudioEffect.Descriptor descriptor = aec.getDescriptor();
                Log.d(TAG, "AcousticEchoCanceler " +
                        "name: " + descriptor.name + ", " +
                        "implementor: " + descriptor.implementor + ", " +
                        "uuid: " + descriptor.uuid);
            }

            while (!isInterrupted()) {

                int result = recorder.read(samples, 0, samples.length);

                if (result < 0) {
                    Log.d(TAG, "Could not read audio data: " + result);
                    break;
                }

                synchronized (ringBuffer) {
                    ringBuffer.produce(0, samples, result);
                    ringBuffer.notify();
                }
            }

            if (aec != null) {
                aec.release();
            }

            recorder.stop();
            recorder.release();
        }
    }

    private static class ReceiverThread extends Thread {
        private final DatagramSocket socket;
        private final OpusDecoder[] decoders;
        private final RingBuffer ringBuffer;
        private final Crypto crypto;
        private final short[] decoded = new short[PLAYER_BUFFER_SIZE];
        private final DatagramPacket packet = new DatagramPacket(new byte[4096], 4096);

        private ReceiverThread(RingBuffer ringBuffer, Crypto crypto, DatagramSocket socket, int streamId, int numStreams) {
            this.socket = socket;
            this.ringBuffer = ringBuffer;
            this.crypto = crypto;

            decoders = new OpusDecoder[numStreams];
            for (int i = 0; i < numStreams; i++) {
                if (i == streamId) {
                    continue;
                }
                decoders[i] = new OpusDecoder(SAMPLE_RATE, NUM_CHANNELS);
            }
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    socket.receive(packet);
                }
                catch (IOException e) {
                    Log.d(TAG, "Could not receive data: " + e.toString());
                    if (!(e instanceof SocketTimeoutException)) {
                        break;
                    }
                    continue;
                }

                byte[] networkData = packet.getData();

                final int streamId = networkData[0];

                if (streamId >= decoders.length || streamId < 0 || decoders[streamId] == null) {
                    Log.d(TAG, "Bad stream ID " + streamId);
                    continue;
                }

                final int size = ((networkData[1] & 0xff) << 24) | ((networkData[2] & 0xff) << 16) | ((networkData[3] & 0xff) << 8) | (networkData[4] & 0xff);

                if (packet.getLength() < 5 + size) {
                    Log.d(TAG, "Bad packet length");
                    break;
                }

                final byte[] encrypted = new byte[size];
                System.arraycopy(packet.getData(), 5, encrypted, 0, size);

                byte[] encoded = crypto.decryptBytes(encrypted, size);

                try {
                    int numSamples = decoders[streamId].decode(encoded, encoded.length, decoded);
                    ringBuffer.produce(streamId, decoded, numSamples);
                }
                catch (OpusDecoder.OpusDecodingException e) {
                    Log.d(TAG, "Could not decode data", e);
                }
            }

            Log.d("RingBuffer", ringBuffer.stats());
        }
    }

    private static class AudioPlayerThread extends Thread {
        private final AudioTrack[] tracks;
        private final RingBuffer ringBuffer;

        private AudioPlayerThread(RingBuffer ringBuffer, int streamId, int numStreams) {
            tracks = new AudioTrack[numStreams];
            this.ringBuffer = ringBuffer;
            for (int i = 0; i < numStreams; i++) {
                if (i == streamId) {
                    continue;
                }
                tracks[i] = new AudioTrack(AudioManager.STREAM_VOICE_CALL, SAMPLE_RATE, CHANNEL_OUT_CONFIG, AUDIO_FORMAT, PLAYER_BUFFER_SIZE, AudioTrack.MODE_STREAM);
            }
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            for (AudioTrack track : tracks) {
                if (track != null) {
                    track.play();
                }
            }

            while (!isInterrupted()) {
                for (int stream = 0; stream < tracks.length; stream++) {
                    if (tracks[stream] == null) {
                        continue;
                    }
                    short[] samples = ringBuffer.consume(stream);
                    tracks[stream].write(samples, 0, samples.length);
                }
            }

            for (AudioTrack track : tracks) {
                if (track != null) {
                    track.stop();
                    track.release();
                }
            }
        }
    }

    private interface RingBuffer {
        void produce(int stream, short[] data, int length);

        short[] consume(int stream);

        String stats();
    }
}
