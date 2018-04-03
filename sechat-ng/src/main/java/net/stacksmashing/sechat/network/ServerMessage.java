package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import org.msgpack.type.Value;

import java.util.HashMap;
import java.util.Map;

public abstract class ServerMessage {
    private static final String TAG = "ServerMessage";

    private static final Map<String, Class<? extends ServerMessage>> CLASSES = new HashMap<>();

    static {
        CLASSES.put(ServerAddContactMessage.NAME, ServerAddContactMessage.class);
        CLASSES.put(ServerCallCreateResponseMessage.NAME, ServerCallCreateResponseMessage.class);
        CLASSES.put(ServerCallGetStatusResponseMessage.NAME, ServerCallGetStatusResponseMessage.class);
        CLASSES.put(ServerChannelInfoMessage.NAME, ServerChannelInfoMessage.class);
        CLASSES.put(ServerCreateGroupChatResponseMessage.NAME, ServerCreateGroupChatResponseMessage.class);
        CLASSES.put(ServerEndToEndChannelMessage.NAME, ServerEndToEndChannelMessage.class);
        CLASSES.put(ServerEndToEndGroupChatMessage.NAME, ServerEndToEndGroupChatMessage.class);
        CLASSES.put(ServerEndToEndMessage.NAME, ServerEndToEndMessage.class);
        CLASSES.put(ServerEndToEndResponseMessage.NAME, ServerEndToEndResponseMessage.class);
        CLASSES.put(ServerGetChannelsResponseMessage.NAME, ServerGetChannelsResponseMessage.class);
        CLASSES.put(ServerGetProfileResponseMessage.NAME, ServerGetProfileResponseMessage.class);
        CLASSES.put(ServerGroupChatInvitationMessage.NAME, ServerGroupChatInvitationMessage.class);
        CLASSES.put(ServerGroupChatUpdateMessage.NAME, ServerGroupChatUpdateMessage.class);
        CLASSES.put(ServerJoinChannelResponseMessage.NAME, ServerJoinChannelResponseMessage.class);
        CLASSES.put(ServerLoginChallengeMessage.NAME, ServerLoginChallengeMessage.class);
        CLASSES.put(ServerProfilePictureUpdateMessage.NAME, ServerProfilePictureUpdateMessage.class);
        CLASSES.put(ServerRecallEndToEndResponseMessage.NAME, ServerRecallEndToEndResponseMessage.class);
        CLASSES.put(ServerRegisterMessage.NAME, ServerRegisterMessage.class);
        CLASSES.put(ServerRegisterPhoneNumberResponseMessage.NAME, ServerRegisterPhoneNumberResponseMessage.class);
        CLASSES.put(ServerStatusNotificationMessage.NAME, ServerStatusNotificationMessage.class);
        CLASSES.put(ServerVerifyPhoneNumberResponseMessage.NAME, ServerVerifyPhoneNumberResponseMessage.class);
    }

    public static ServerMessage fromMap(Map<String, Value> values) {
        if (!values.containsKey("MessageType")) {
            return null;
        }

        Class<?> messageClass = CLASSES.get(values.get("MessageType").asRawValue().getString());

        try {
            return (ServerMessage) messageClass.getConstructor(Map.class).newInstance(values);
        }
        catch (Exception e) {
            return null;
        }
    }

    abstract void performAction(Context context);

    protected static boolean isPayloadValid(EndToEndMessage payload, String sender, String target) {
        if (payload == null) {
            return false;
        }

        if (payload.getSender() != null && !payload.getSender().isEmpty() && !payload.getSender().equals(sender)) {
            Log.d(TAG, "Server sender \"" + sender + "\" does not match payload sender \"" + payload.getSender() + "\"");
            return false;
        }

        if (payload.getTarget() != null && !payload.getTarget().isEmpty() && !payload.getTarget().equals(target)) {
            Log.d(TAG, "Payload target \"" + payload.getTarget() + "\" does not match expected target \"" + target + "\"");
            return false;
        }

        return true;
    }
}
