package net.stacksmashing.sechat.video;

import net.stacksmashing.sechat.Bus;

import java.util.HashMap;
import java.util.Map;

public enum StreamManager {
    INSTANCE;

    private final Map<String, StreamInfo> streams = new HashMap<>();

    public synchronized void addStream(String name, StreamInfo stream) {
        /*
        if (streams.containsKey(name)) {
            removeStream(name);
        }

        streams.put(name, stream);
        Bus.bus().post(new Bus.VideoStreamStartEvent(name, stream));
        */
    }

    public synchronized void removeStream(String name) {
        /*
        streams.remove(name);
        Bus.bus().post(new Bus.VideoStreamEndEvent(name));
        */
    }

    public synchronized Map<String, StreamInfo> getStreams() {
        return streams;
    }

    public synchronized void decodeData(String name, byte[] data) {
        /*
        if (streams.containsKey(name)) {
            Bus.bus().post(new Bus.VideoStreamDataEvent(name, data));
        }
        */
    }

    public static class StreamInfo {
        private final byte[] sps, pps;
        private final int width, height;

        public StreamInfo(byte[] sps, byte[] pps, int width, int height) {
            this.sps = sps;
            this.pps = pps;
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public byte[] getSps() {
            return sps;
        }

        public byte[] getPps() {
            return pps;
        }
    }
}
