package net.stacksmashing.sechat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.voice.CallHandler;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.hdodenhof.circleimageview.CircleImageView;

public class IncomingCallActivity extends Activity implements View.OnClickListener {
    private static final String EXTRA_CALLER = "caller";

    @InjectView(R.id.activity_incoming_call_caller_picture)
    CircleImageView callerPicture;

    @InjectView(R.id.activity_incoming_call_caller_name)
    TextView callerName;

    @InjectView(R.id.activity_incoming_call_participants_list)
    ListView participantsList;

    @InjectView(R.id.activity_incoming_call_accept_call)
    Button acceptCallButton;

    @InjectView(R.id.activity_incoming_call_decline_call)
    Button declineCallButton;

    private CallParticipantsAdapter participantsAdapter;
    private Ringtone ringtone;
    private Vibrator vibrator;

    public static Intent intentWithCallerName(Context context, String caller) {
        Intent intent = new Intent(context, IncomingCallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CALLER, caller);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_incoming_call);

        ButterKnife.inject(this);

        acceptCallButton.setOnClickListener(this);
        declineCallButton.setOnClickListener(this);

        participantsAdapter = new CallParticipantsAdapter(this, false);
        participantsList.setAdapter(participantsAdapter);

        final String caller = getIntent().getStringExtra(EXTRA_CALLER);

        callerName.setText(caller);

        final Contact contact = Contact.findContactByUsername(this, caller);
        if (contact != null) {
            contact.loadProfilePictureInto(this, callerPicture);
        }

        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_RINGTONE);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(new long[]{0, 1000, 500}, 1);

        ringtone = RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
        ringtone.play();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (vibrator != null) {
            vibrator.cancel();
        }

        if (ringtone != null) {
            ringtone.stop();
            ringtone = null;
        }

        if (CallHandler.INSTANCE.getState() == CallHandler.State.ACCEPTING) {
            CallHandler.INSTANCE.decline(this);
        }

        participantsList.setAdapter(null);
        participantsAdapter = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Bus.bus().register(this);
        handleCallStateChange(new Bus.CallStateChangedEvent(CallHandler.INSTANCE.getState()));
        participantsAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Bus.bus().unregister(this);
    }

    @Override
    public void onClick(View view) {
        if (view == acceptCallButton) {
            CallHandler.INSTANCE.accept(this);
        }
        else if (view == declineCallButton) {
            CallHandler.INSTANCE.decline(this);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleCallStateChange(Bus.CallStateChangedEvent event) {
        switch (event.getState()) {
            case NONE:
                finish();
                break;
            case CHECKING_STATUS:
            case ACCEPTING:
            case CREATING:
            case OBTAINING_GATEWAY:
                break;
            case IN_CALL:
                finish();
                startActivity(NavigationActivity.intentForCall(this));
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleCallUserStatusChange(Bus.CallUserStatusChangedEvent event) {
        participantsAdapter.notifyDataSetChanged();
    }
}
