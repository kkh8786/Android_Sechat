package net.stacksmashing.sechat.media;

import android.media.MediaMetadataRetriever;

import java.util.HashMap;
import java.util.Map;

public enum FileDurationCache {
    INSTANCE;

    private final Map<String, Integer> durations = new HashMap<>();

    public int getDuration(String path) {
        synchronized (durations) {
            if (!durations.containsKey(path)) {
                durations.put(path, retrieveDuration(path));
            }
            return durations.get(path);
        }
    }

    private int retrieveDuration(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        retriever.release();
        return duration != null ? Integer.parseInt(duration) : 0;
    }
}
