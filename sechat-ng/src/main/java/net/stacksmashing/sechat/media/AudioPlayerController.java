package net.stacksmashing.sechat.media;

import java.io.File;

/**
 * Created by kulikov on 10.12.2014.
 */
public interface AudioPlayerController {
    /**
     * @return current audio play time position.
     */
    int getCurrentPosition();

    /**
     * @return total length of audio file is played.
     */
    int getDuration();

    void togglePlayback();

    void loadFile(File source);

    boolean isPlaying();

    boolean isPlayingFile(File file);

    interface Callback {
        void audioPlayerStateChanged();
    }
}
