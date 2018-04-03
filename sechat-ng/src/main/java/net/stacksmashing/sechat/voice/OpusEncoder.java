package net.stacksmashing.sechat.voice;

class OpusEncoder {
    @SuppressWarnings("unused")
    private byte[] encoder;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final int channels;

    static {
        System.loadLibrary("opuswrapper");
    }

    public OpusEncoder(int sampleRate, int channels) {
        this.channels = channels;
        int result;
        if ((result = opusCreateEncoder(sampleRate, channels)) < 0) {
            throw new IllegalStateException("Could not create a opus encoder: " + result);
        }
    }

    private native int opusCreateEncoder(int sampleRate, int channels);

    private native int opusEncode(short[] pcm, int numSamples, byte[] output);

    public int encode(short[] pcm, int numSamples, byte[] output) throws OpusEncodingException {
        int result = opusEncode(pcm, numSamples, output);

        if (result < 0) {
            throw new OpusEncodingException(result);
        }

        return result;
    }

    public static class OpusEncodingException extends Exception {
        private OpusEncodingException(int code) {
            super("Opus encoding error: " + code);
        }
    }
}
