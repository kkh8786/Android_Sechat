package net.stacksmashing.sechat.voice;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Base64;
import android.util.Log;

import net.stacksmashing.sechat.Bus;
import net.stacksmashing.sechat.IncomingCallActivity;
import net.stacksmashing.sechat.NavigationActivity;
import net.stacksmashing.sechat.Preferences;
import net.stacksmashing.sechat.R;
import net.stacksmashing.sechat.db.RecentCall;
import net.stacksmashing.sechat.network.ClientCallCreateMessage;
import net.stacksmashing.sechat.network.ClientCallGetStatusMessage;
import net.stacksmashing.sechat.network.ClientCallUpkeepMessage;
import net.stacksmashing.sechat.network.ClientEndToEndMessage;
import net.stacksmashing.sechat.network.ClientMessage;
import net.stacksmashing.sechat.network.EndToEndCallHangupMessage;
import net.stacksmashing.sechat.network.EndToEndCallInvitationMessage;
import net.stacksmashing.sechat.network.EndToEndCallInvitationResponseMessage;
import net.stacksmashing.sechat.network.EndToEndCallVideoDataMessage;
import net.stacksmashing.sechat.network.EndToEndCallVideoFinishMessage;
import net.stacksmashing.sechat.network.EndToEndCallVideoInfoMessage;
import net.stacksmashing.sechat.network.EndToEndMessage;
import net.stacksmashing.sechat.network.NetworkService;
import net.stacksmashing.sechat.video.CameraEncoder;

import org.apache.commons.io.Charsets;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public enum CallHandler implements CameraEncoder.Callback {
    INSTANCE;

    private static final String TAG = "CallHandler";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /* The interval between upkeep messages (for outgoing calls) or status requests (for incoming calls) */
    private static final int PERIODIC_MESSAGE_INTERVAL = 4000;

    private static final String IP_GETTER_HOST = "178.22.66.217";
    private static final int IP_GETTER_PORT = 6888;

    private State state = State.NONE;
    private String token;
    private String host, port;
    private byte[] key;
    private List<String> users;
    private String caller;
    private List<Participant> participants;
    private Date createdAt; // This is the timestamp we receive from the call initiator
    private Date startedAt; // This is our local timestamp

    private Transceiver transceiver;
    private PeriodicMessageSender periodicMessageSender;
    private Thread ipGetterThread;

    private PowerManager.WakeLock wakeLock;

    private void setToken(String token) {
        this.token = token;
    }

    public long getDuration() {
        return startedAt != null ? (System.currentTimeMillis() - startedAt.getTime()) : 0;
    }

    private void setUsers(List<String> users, String ourUsername, String caller) {
        /* Add ourselves to the user list.  We could simplify some things by /not/ keeping ourselves in the user list, but right now it's needed to determine stream IDs correctly. */
        if (!users.contains(ourUsername)) {
            users = new ArrayList<>(users);
            users.add(ourUsername);
        }
        /* Paranoia: Make a copy of the user list and only keep a reference to its unmodifiable view. */
        this.users = Collections.unmodifiableList(new ArrayList<>(users));
        participants = new ArrayList<>(this.users.size());
        for (String user : this.users) {
            /* Don't list ourselves among participants, because we don't have a meaningful status anyway. */
            if (user.equals(ourUsername)) {
                continue;
            }
            participants.add(new Participant(user, user.equals(caller) ? UserStatus.CALLER : UserStatus.UNKNOWN));
        }
    }

    public void setState(Context context, State state) {
        this.state = state;
        Bus.bus().post(new Bus.CallStateChangedEvent(state));

        if (state == State.NONE) {
            releaseWakeLock();
        }
        else {
            acquireWakeLock(context);
        }

        if (state == State.IN_CALL) {
            startedAt = new Date();
        }
        else if (state == State.NONE && startedAt != null) {
            String contact = participants.isEmpty() ? caller : participants.size() == 1 ? participants.get(0).getName() : "Conference call";
            if (contact != null) {
                RecentCall.DAO.insert(context, new RecentCall(contact, caller != null, startedAt, getDuration()));
            }
            startedAt = null;
        }
    }

    private void acquireWakeLock(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        int wakeLockType;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            wakeLockType = PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK;
        }
        else {
            /* XXX: Nasty hack. */
            try {
                Field f = PowerManager.class.getDeclaredField("PROXIMITY_SCREEN_OFF_WAKE_LOCK");
                wakeLockType = f.getInt(null);
            }
            catch (Exception e) {
                Log.d("InCallFragment", "PROXIMITY_SCREEN_OFF_WAKE_LOCK is not available");
                wakeLockType = PowerManager.SCREEN_BRIGHT_WAKE_LOCK;
            }
        }

        wakeLock = pm.newWakeLock(wakeLockType, "net.stacksmashing.sechat.InCallFragment");

        wakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    public void checkIncomingCallStatus(Context context, String caller, String callToken, List<String> users, String publicIP, String publicPort, byte[] key) {
        if (!checkState(State.NONE)) {
            return;
        }

        Log.d(TAG, "Checking status of " + callToken);
        setToken(callToken);
        setUsers(users, Preferences.getUsername(context), caller);
        this.caller = caller;
        this.host = publicIP;
        this.port = publicPort;
        this.key = key;
        setState(context, State.CHECKING_STATUS);
        NetworkService.getInstance().asyncSend(new ClientCallGetStatusMessage(callToken));
    }

    public void initiateCall(Context context, List<String> users, byte[] key) {
        if (!checkState(State.NONE)) {
            return;
        }

        Log.d(TAG, "Creating call");
        setUsers(users, Preferences.getUsername(context), null);
        this.key = key;
        setState(context, State.CREATING);
        NetworkService.getInstance().asyncSend(new ClientCallCreateMessage());

        context.startActivity(NavigationActivity.intentForCall(context));
    }

    public void callCreated(final Context context, String callToken) {
        if (!checkState(State.CREATING)) {
            return;
        }

        Log.d(TAG, "Created call " + callToken);
        setToken(callToken);
        setState(context, State.OBTAINING_GATEWAY);
        ipGetterThread = new Thread() {
            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    DatagramPacket packet = new DatagramPacket("GIVE_IP".getBytes(), 7);
                    packet.setSocketAddress(new InetSocketAddress(IP_GETTER_HOST, IP_GETTER_PORT));
                    socket.setSoTimeout(30000);
                    //while (true) {
                    socket.send(packet);
                    Log.d("IPGetter", "sent IP request");
                    DatagramPacket response = new DatagramPacket(new byte[4096], 4096);
                    socket.receive(response);
                    Log.d("IPGetter", "received response " + new String(response.getData(), 0, response.getLength()));
                    String json = new String(Base64.decode(response.getData(), Base64.DEFAULT), Charsets.UTF_8);
                    JSONObject obj = new JSONObject(json);
                    final String publicIP = obj.getString("PublicIP");
                    final String publicPort = obj.getString("PublicPort");
                    MAIN_HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            obtainedGateway(context, publicIP, publicPort);
                        }
                    });
                    //    break;
                    //}
                }
                catch (Exception e) {
                    Log.d("IPGetter", "Exception", e);
                    MAIN_HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            reset(context);
                        }
                    });
                }
            }
        };
        ipGetterThread.start();
    }

    private void obtainedGateway(Context context, String host, String port) {
        if (!checkState(State.OBTAINING_GATEWAY)) {
            return;
        }

        Log.d(TAG, "Obtained gateway " + host + ":" + port);
        this.host = host;
        this.port = port;

        EndToEndMessage payload = new EndToEndCallInvitationMessage(EndToEndMessage.Parameters.with(context), token, users.toArray(new String[users.size()]), host, port);
        sendCallStatusMessage(context, payload);

        periodicMessageSender = new PeriodicMessageSender(new ClientCallUpkeepMessage(token), MAIN_HANDLER);
        MAIN_HANDLER.postDelayed(periodicMessageSender, PERIODIC_MESSAGE_INTERVAL);

        startTransceiver(context);

        if (transceiver != null) {
            transceiver.playDialTone();
        }

        setState(context, State.IN_CALL);
    }

    public void incomingCallIsActive(Context context, String callToken, Date createdAt) {
        if (state == State.CHECKING_STATUS && token.equals(callToken)) {
            Log.d(TAG, "Incoming call " + callToken + " is active");

            this.createdAt = createdAt;

            context.startActivity(IncomingCallActivity.intentWithCallerName(context, caller));

            setState(context, State.ACCEPTING);
        }
        else if (state == State.IN_CALL) {
            Log.d(TAG, "Incoming call " + callToken + " is STILL active");
        }
        else {
            Log.d(TAG, "Received unexpected call state message (active) for token " + callToken);
        }
    }

    public void incomingCallIsExpired(Context context, String callToken, Date createdAt) {
        if ((state == State.CHECKING_STATUS || state == State.IN_CALL) && token.equals(callToken)) {
            Log.d(TAG, "Incoming call " + callToken + " has expired");
            reset(context);
        }
        else {
            Log.d(TAG, "Received unexpected call state message (expired) for token " + callToken);
        }
    }

    public void accept(Context context) {
        if (!checkState(State.ACCEPTING)) {
            return;
        }

        Log.d(TAG, "Accepted call");
        sendCallStatusMessage(context, new EndToEndCallInvitationResponseMessage(EndToEndMessage.Parameters.with(context), token, true));
        startTransceiver(context);
        periodicMessageSender = new PeriodicMessageSender(new ClientCallGetStatusMessage(token), MAIN_HANDLER);
        MAIN_HANDLER.postDelayed(periodicMessageSender, PERIODIC_MESSAGE_INTERVAL);
        setState(context, State.IN_CALL);
    }

    public void decline(Context context) {
        if (!checkState(State.ACCEPTING)) {
            return;
        }

        Log.d(TAG, "Declined call");
        sendCallStatusMessage(context, new EndToEndCallInvitationResponseMessage(EndToEndMessage.Parameters.with(context), token, false));
        reset(context);
    }

    public void hangUp(Context context) {
        /* XXX Don't check the state strictly here, to let the user end call at any point during its setup. */
        /*if (!checkState(State.IN_CALL)) {
            return;
        }*/

        Log.d(TAG, "Hanging up call");
        if (token != null) {
            sendCallStatusMessage(context, new EndToEndCallHangupMessage(EndToEndMessage.Parameters.with(context), token));
        }
        reset(context);
    }

    private void sendCallStatusMessage(Context context, EndToEndMessage payload) {
        if (users == null) {
            return;
        }

        final String ourUsername = Preferences.getUsername(context);

        for (String user : users) {
            if (ourUsername.equals(user)) {
                continue;
            }
            NetworkService.getInstance().asyncSend(new ClientEndToEndMessage(user, payload, null));
        }
    }

    private void reset(Context context) {
        if (transceiver != null) {
            transceiver.stop();
            transceiver = null;
        }
        if (periodicMessageSender != null) {
            periodicMessageSender.stop();
            periodicMessageSender = null;
        }
        if (ipGetterThread != null) {
            ipGetterThread.interrupt();
            ipGetterThread = null;
        }
        setState(context, State.NONE);
        token = null;
        host = null;
        port = null;
        key = null;
        users = null;
        caller = null;
        participants = null;
        createdAt = null;
    }

    private void startTransceiver(final Context context) {
        if (transceiver != null) {
            Log.d("IncomingCallActivity", "Tried to start transceiver when one already exists");
            return;
        }

        final String ourUsername = Preferences.getUsername(context);
        int streamId = users.indexOf(ourUsername);
        int numStreams = users.size();

        transceiver = new Transceiver(context, host, Integer.parseInt(port), key, streamId, numStreams);
        transceiver.start();
    }

    private boolean checkState(State expectedState) {
        if (state != expectedState) {
            Log.e(TAG, "Expected state: " + expectedState.name() + ", actual: " + state.name());
            return false;
        }
        return true;
    }

    public void userRespondedToInvitation(Context context, String username, String callToken, boolean accept) {
        if (!checkState(State.IN_CALL) || !token.equals(callToken)) {
            return;
        }

        if (transceiver != null) {
            transceiver.stopDialTone();
        }

        updateParticipant(context, username, accept ? UserStatus.ACCEPTED : UserStatus.DECLINED);
    }

    public void userHungUp(Context context, String username, String callToken) {
        /* If the caller hangs up before we've accepted or declined, assume that the call is over. */
        if (state == State.ACCEPTING && token.equals(callToken) && caller != null && caller.equals(username)) {
            reset(context);
        }

        if (!checkState(State.IN_CALL) || !token.equals(callToken)) {
            return;
        }

        updateParticipant(context, username, UserStatus.HUNG_UP);
    }

    private void updateParticipant(Context context, String username, UserStatus status) {
        int remaining = 0;

        for (Participant participant : participants) { // FIXME: Use a Map.
            if (participant.getName().equals(username)) {
                participant.setStatus(status);
            }

            if (participant.getStatus().isActive()) {
                remaining++;
            }
        }

        Bus.bus().post(new Bus.CallUserStatusChangedEvent(username, status));

        if (remaining == 0) {
            Transceiver.playBusyTone(context);
            hangUp(context);
        }
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public String getCaller() {
        return caller;
    }

    public State getState() {
        return state;
    }

    @Override
    public void onAvcParametersEstablished(int width, int height, byte[] sps, byte[] pps) {
        if (!checkState(State.IN_CALL)) {
            return;
        }

        Context context = NetworkService.getInstance();
        EndToEndMessage payload = new EndToEndCallVideoInfoMessage(EndToEndMessage.Parameters.with(context), sps, pps, width, height);
        sendCallStatusMessage(context, payload);
    }

    @Override
    public void onFrameEncoded(byte[] data) {
        if (!checkState(State.IN_CALL)) {
            return;
        }

        Context context = NetworkService.getInstance();
        EndToEndMessage payload = new EndToEndCallVideoDataMessage(EndToEndMessage.Parameters.with(context), token, data);
        sendCallStatusMessage(context, payload);
    }

    @Override
    public void onFinished() {
        Context context = NetworkService.getInstance();
        EndToEndMessage payload = new EndToEndCallVideoFinishMessage(EndToEndMessage.Parameters.with(context));
        sendCallStatusMessage(context, payload);
    }

    public enum State {
        /**
         * Idle.
         */
        NONE,
        /**
         * Waiting for ServerCallGetStatusResponseMessage.
         */
        CHECKING_STATUS,
        /**
         * Waiting for the user to accept or decline.
         */
        ACCEPTING,
        /**
         * Waiting for ServerCallCreateResponseMessage.
         */
        CREATING,
        /**
         * Waiting for IP getter.
         */
        OBTAINING_GATEWAY,
        /**
         * A call is in progress.
         */
        IN_CALL,
    }

    public enum UserStatus {
        UNKNOWN,
        ACCEPTED,
        DECLINED,
        HUNG_UP,
        CALLER;

        public int descriptionId() {
            switch (this) {
                case UNKNOWN:
                    return R.string.call_status_unknown;
                case ACCEPTED:
                    return R.string.call_status_accepted;
                case DECLINED:
                    return R.string.call_status_declined;
                case HUNG_UP:
                    return R.string.call_status_hung_up;
                case CALLER:
                    return R.string.call_status_caller;
            }
            return 0;
        }

        public boolean isActive() {
            switch (this) {
                case UNKNOWN:
                case ACCEPTED:
                case CALLER:
                    return true;
            }
            return false;
        }
    }

    private static class PeriodicMessageSender implements Runnable {
        private final ClientMessage message;
        private final Handler handler;
        private boolean stopped;

        private PeriodicMessageSender(ClientMessage message, Handler handler) {
            this.message = message;
            this.handler = handler;
        }

        void stop() {
            stopped = true;
            handler.removeCallbacks(this);
        }

        @Override
        public void run() {
            if (!stopped) {
                NetworkService.getInstance().asyncSend(message);
                handler.postDelayed(this, PERIODIC_MESSAGE_INTERVAL);
            }
        }
    }

    /**
     * A user together with their respective UserStatus.
     */
    public static class Participant {
        private final String name;
        private CallHandler.UserStatus status;

        Participant(String name, CallHandler.UserStatus status) {
            this.name = name;
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public void setStatus(CallHandler.UserStatus status) {
            this.status = status;
        }

        public CallHandler.UserStatus getStatus() {
            return status;
        }
    }
}
