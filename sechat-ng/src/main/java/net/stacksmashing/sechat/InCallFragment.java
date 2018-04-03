package net.stacksmashing.sechat;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ToggleButton;

import com.squareup.otto.Subscribe;

import net.stacksmashing.sechat.video.CameraEncoder;
import net.stacksmashing.sechat.video.Decoder;
import net.stacksmashing.sechat.video.StreamManager;
import net.stacksmashing.sechat.voice.CallHandler;

import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class InCallFragment extends Fragment implements View.OnClickListener, SurfaceHolder.Callback, CompoundButton.OnCheckedChangeListener {
    public static Fragment newInstance() {
        return new InCallFragment();
    }

    @InjectView(R.id.fragment_in_call_participants_list)
    ListView participantsList;

    @InjectView(R.id.fragment_in_call_send_video)
    ToggleButton sendVideoButton;

    @InjectView(R.id.fragment_in_call_end_call)
    Button endCallButton;

    @InjectView(R.id.fragment_in_call_video_holder)
    LinearLayout videoHolder;

    private Map<String, SurfaceView> videoSurfaces;
    private Map<SurfaceHolder, StreamManager.StreamInfo> streamInfoMap;
    private Map<SurfaceHolder, String> namesMap;
    private Map<String, Decoder> decoderMap;
    private boolean isSendingVideo;

    private CallParticipantsAdapter participantsAdapter;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long callDuration;

    private final Runnable updateDuration = new Runnable() {
        @Override
        public void run() {
            callDuration++;
            if (getActivity() != null) {
                ActionBar actionBar = getActivity().getActionBar();
                if (actionBar != null) {
                    actionBar.setSubtitle(DateUtils.formatElapsedTime(callDuration));
                }
            }
            handler.postDelayed(updateDuration, 1000l);
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_in_call, container, false);

        ButterKnife.inject(this, view);

        endCallButton.setOnClickListener(this);
        sendVideoButton.setOnCheckedChangeListener(this);
        sendVideoButton.setVisibility(View.INVISIBLE);
        sendVideoButton.setEnabled(false);

        videoSurfaces = new HashMap<>();
        streamInfoMap = new HashMap<>();
        namesMap = new HashMap<>();
        decoderMap = new HashMap<>();

        participantsAdapter = new CallParticipantsAdapter(getActivity());
        participantsList.setAdapter(participantsAdapter);

        isSendingVideo = false;
        callDuration = 0;

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        participantsList.setAdapter(null);
        participantsAdapter = null;

        ButterKnife.reset(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Bus.bus().register(this);
        participantsAdapter.notifyDataSetChanged();

        callDuration = CallHandler.INSTANCE.getDuration() / 1000L;

        handler.post(updateDuration);

        for (String name : StreamManager.INSTANCE.getStreams().keySet()) {
            handleVideoStreamStart(new Bus.VideoStreamStartEvent(name, StreamManager.INSTANCE.getStreams().get(name)));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Bus.bus().unregister(this);

        handler.removeCallbacks(updateDuration);

        setIsSendingVideo(false);

        videoSurfaces.clear();
        videoHolder.removeAllViews();
        streamInfoMap.clear();
        namesMap.clear();

        for (Decoder decoder : decoderMap.values()) {
            decoder.stop();
        }
        decoderMap.clear();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity.getActionBar() != null) {
            activity.getActionBar().setTitle(R.string.fragment_in_call);
        }
    }

    @Override
    public void onClick(View view) {
        if (view == endCallButton) {
            CallHandler.INSTANCE.hangUp(getActivity());
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton == sendVideoButton) {
            setIsSendingVideo(b);
        }
    }

    private void setIsSendingVideo(boolean isSendingVideo) {
        if (this.isSendingVideo == isSendingVideo) {
            return;
        }

        this.isSendingVideo = isSendingVideo;

        if (isSendingVideo) {
            CameraEncoder.INSTANCE.setCallback(CallHandler.INSTANCE);
            CameraEncoder.INSTANCE.start();
        }
        else {
            CameraEncoder.INSTANCE.stop();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleCallUserStatusChange(Bus.CallUserStatusChangedEvent event) {
        participantsAdapter.notifyDataSetChanged();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleVideoStreamStart(final Bus.VideoStreamStartEvent event) {
        SurfaceView view = new SurfaceView(getActivity());
        videoHolder.addView(view);
        videoSurfaces.put(event.getName(), view);
        streamInfoMap.put(view.getHolder(), event.getStreamInfo());
        namesMap.put(view.getHolder(), event.getName());
        if (!decoderMap.containsKey(event.getName())) {
            decoderMap.put(event.getName(), new Decoder());
        }
        view.setLayoutParams(new LinearLayout.LayoutParams(event.getStreamInfo().getWidth(), event.getStreamInfo().getHeight()));
        view.getHolder().addCallback(this);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleVideoStreamData(Bus.VideoStreamDataEvent event) {
        if (decoderMap.containsKey(event.getName())) {
            decoderMap.get(event.getName()).decode(event.getData());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleVideoStreamEnd(Bus.VideoStreamEndEvent event) {
        synchronized (this) {
            String user = event.getName();
            if (videoSurfaces.containsKey(user)) {
                SurfaceView v = videoSurfaces.get(user);
                videoSurfaces.remove(user);
                videoHolder.removeView(v);
                decoderMap.get(user).stop();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        if (videoSurfaces.containsKey(namesMap.get(surfaceHolder))) {
            decoderMap.get(namesMap.get(surfaceHolder)).start(surfaceHolder.getSurface(), streamInfoMap.get(surfaceHolder));
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        handleVideoStreamEnd(new Bus.VideoStreamEndEvent(namesMap.get(surfaceHolder)));
    }
}
