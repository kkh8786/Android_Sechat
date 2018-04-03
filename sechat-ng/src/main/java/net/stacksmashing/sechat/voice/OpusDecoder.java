package net.stacksmashing.sechat.voice;

class OpusDecoder {
    @SuppressWarnings("unused")
    private byte[] decoder;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final int channels;

    static {
        System.loadLibrary("opuswrapper");
    }

    public OpusDecoder(int sampleRate, int channels) {
        this.channels = channels;
        int result;
        if ((result = opusCreateDecoder(sampleRate, channels)) < 0) {
            throw new IllegalStateException("Could not create a opus decoder: " + result);
        }
    }

    private native int opusCreateDecoder(int sampleRate, int channels);

    private native int opusDecode(byte[] input, int numBytes, short[] pcm);

    public int decode(byte[] input, int numBytes, short[] pcm) throws OpusDecodingException {
        int result = opusDecode(input, numBytes, pcm);

        if (result < 0) {
            throw new OpusDecodingException(result);
        }

        return result;
    }

    public static class OpusDecodingException extends Exception {
        private OpusDecodingException(int code) {
            super("Opus decoding error: " + code);
        }
    }
}
