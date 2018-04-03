package net.stacksmashing.sechat.media;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.stacksmashing.sechat.R;

import java.io.File;
import java.lang.ref.WeakReference;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by kulikov on 15.12.2014.
 */

/**
 * Audio player control implementation for chat fragment.
 */
public class AudioPlayerListItem extends FrameLayout implements View.OnClickListener {

    private final Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            AudioPlayerController controller;
            if (controllerRef != null && (controller = controllerRef.get()) != null) {
                final int currentPosition = controller.getCurrentPosition();
                position.setProgress(currentPosition);
                durationText.setText(DateUtils.formatElapsedTime(currentPosition / 1000l));
                HANDLER.postDelayed(this, 100L);
            }
        }
    };

    private WeakReference<AudioPlayerController> controllerRef;

    @InjectView(R.id.audio_controller_play)
    ImageView icon;

    @InjectView(R.id.audio_controller_progress)
    ProgressBar position;

    @InjectView(R.id.audio_controller_duration)
    TextView durationText;

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private File source;

    public AudioPlayerListItem(Context context) {
        super(context);
        initialize();
    }

    public AudioPlayerListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public AudioPlayerListItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    private void initialize() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.view_audio_controller, this, true);
        ButterKnife.inject(this, view);
        this.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        AudioPlayerController controller;
        if (controllerRef != null && (controller = controllerRef.get()) != null) {
            controller.loadFile(source);
            controller.togglePlayback();
        }
    }

    public void bindData(Context context, net.stacksmashing.sechat.db.Message message, AudioPlayerController controller) {
        source = message.getFile(context);

        icon.setColorFilter(context.getResources().getColor(message.getContentColor()));
        durationText.setTextColor(context.getResources().getColor(message.getContentColor()));

        controllerRef = new WeakReference<>(controller);

        if (controller.isPlayingFile(source)) {
            int duration = controller.getDuration();
            int currentPosition = controller.getCurrentPosition();
            position.setMax(duration == 0 ? 1 : duration);
            position.setProgress(duration == 0 ? 0 : currentPosition);

            durationText.setText(DateUtils.formatElapsedTime(currentPosition / 1000l));

            setIsPlaying(controller.isPlaying(), message.isIncoming());
        }
        else {
            position.setMax(1);
            position.setProgress(0);

            durationText.setText(DateUtils.formatElapsedTime(getDuration() / 1000l));

            setIsPlaying(false, message.isIncoming());
        }
    }

    private int getDuration() {
        return FileDurationCache.INSTANCE.getDuration(source.getPath());
    }

    private void setIsPlaying(boolean playing, boolean incoming) {
        if (playing) {
            icon.setImageResource(incoming ? R.drawable.ic_pause_grey600_18dp : R.drawable.ic_pause_white_18dp);
            HANDLER.removeCallbacks(updateProgress);
            HANDLER.post(updateProgress);
        }
        else {
            icon.setImageResource(incoming ? R.drawable.ic_play_arrow_grey600_18dp : R.drawable.ic_play_arrow_white_18dp);
            HANDLER.removeCallbacks(updateProgress);
        }
    }

}
