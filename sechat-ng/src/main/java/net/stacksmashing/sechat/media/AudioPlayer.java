package net.stacksmashing.sechat.media;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.File;

public class AudioPlayer implements AudioPlayerController, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private static final String TAG = "AudioPlayer";

    private MediaPlayer player;
    private boolean playerPrepared;
    private File currentFile;
    private Callback callback;

    private boolean hibernated;
    private int oldPosition;
    private File oldFile;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
        playerPrepared = false;
        currentFile = null;
        notifyStateChanged();
    }

    @Override
    public int getCurrentPosition() {
        return (player != null && playerPrepared) ? player.getCurrentPosition() : 0;
    }

    @Override
    public int getDuration() {
        return (player != null && playerPrepared) ? player.getDuration() : 0;
    }

    @Override
    public boolean isPlaying() {
        return player != null && playerPrepared && player.isPlaying();
    }

    public boolean isPlayingFile(File path) {
        return path.equals(currentFile);
    }

    @Override
    public void togglePlayback() {
        if (player != null && playerPrepared) {
            if (player.isPlaying()) {
                player.pause();
            }
            else {
                player.start();
            }
            notifyStateChanged();
        }
    }

    private void notifyStateChanged() {
        if (callback != null) {
            callback.audioPlayerStateChanged();
        }
    }

    @Override
    public void loadFile(File source) {
        if (isPlayingFile(source)) {
            return;
        }

        release();

        currentFile = source;

        try {
            player = new MediaPlayer();
            player.setOnPreparedListener(this);
            player.setOnCompletionListener(this);
            player.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            player.setDataSource(source.getAbsolutePath());
            player.prepareAsync();
        }
        catch (Exception e) {
            Log.w(TAG, "Could not set data source", e);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        playerPrepared = true;
        mediaPlayer.start();
        if (oldPosition != 0) {
            mediaPlayer.seekTo(oldPosition);
            oldPosition = 0;
        }
        notifyStateChanged();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        release();
    }

    public void restore() {
        if (hibernated) {
            hibernated = false;
            loadFile(oldFile);
            oldFile = null;
        }
    }

    public void hibernate() {
        if (player != null && playerPrepared && getCurrentPosition() != 0) {
            hibernated = true;
            oldPosition = getCurrentPosition();
            oldFile = currentFile;
            release();
        }
    }
}
