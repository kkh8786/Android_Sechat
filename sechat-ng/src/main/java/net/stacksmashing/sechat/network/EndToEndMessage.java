package net.stacksmashing.sechat.network;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import net.stacksmashing.sechat.Preferences;
import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.db.Message;

import org.msgpack.type.Value;

import java.util.HashMap;
import java.util.Map;

public abstract class EndToEndMessage implements Packable {
    private static final String TAG = "EndToEndMessage";
    private static final Map<String, Class<? extends EndToEndMessage>> CLASSES = new HashMap<>();

    public static final int NOTIFICATION_TYPE_NONE = 0;
    public static final int NOTIFICATION_TYPE_MESSAGE = 1;
    public static final int NOTIFICATION_TYPE_CALL = 2;
    public static final int NOTIFICATION_TYPE_PING = 3;

    private final String messageType;
    private final boolean secret;
    private final String sender;
    private final String target;

    static {
        CLASSES.put(EndToEndTextMessage.NAME, EndToEndTextMessage.class);
        CLASSES.put(EndToEndDataMessage.NAME, EndToEndDataMessage.class);
        CLASSES.put(EndToEndDataPartMessage.NAME, EndToEndDataPartMessage.class);
        CLASSES.put(EndToEndMessageStatusMessage.NAME, EndToEndMessageStatusMessage.class);
        CLASSES.put(EndToEndTypingMessage.NAME, EndToEndTypingMessage.class);
        CLASSES.put(EndToEndCallInvitationMessage.NAME, EndToEndCallInvitationMessage.class);
        CLASSES.put(EndToEndCallInvitationResponseMessage.NAME, EndToEndCallInvitationResponseMessage.class);
        CLASSES.put(EndToEndCallHangupMessage.NAME, EndToEndCallHangupMessage.class);
        CLASSES.put(EndToEndPingMessage.NAME, EndToEndPingMessage.class);
        CLASSES.put(EndToEndInChatMessage.NAME, EndToEndInChatMessage.class);
        CLASSES.put(EndToEndLocationMessage.NAME, EndToEndLocationMessage.class);
        CLASSES.put(EndToEndContactMessage.NAME, EndToEndContactMessage.class);
        CLASSES.put(EndToEndCallVideoInfoMessage.NAME, EndToEndCallVideoInfoMessage.class);
        CLASSES.put(EndToEndCallVideoDataMessage.NAME, EndToEndCallVideoDataMessage.class);
        CLASSES.put(EndToEndCallVideoFinishMessage.NAME, EndToEndCallVideoFinishMessage.class);
    }

    public EndToEndMessage(Parameters parameters, String messageType) {
        this.messageType = messageType;
        this.secret = parameters.getChat() != null && parameters.getChat().isSecret();
        this.sender = parameters.getSender();
        this.target = parameters.getTarget();
    }

    public EndToEndMessage(Map<String, Value> values) {
        this.messageType = values.get("MessageType").asRawValue().getString();
        this.secret = values.containsKey("Secret") && values.get("Secret").asBooleanValue().getBoolean();
        this.sender = values.containsKey("Sender") ? values.get("Sender").asRawValue().getString() : null;
        if (this.sender == null) {
            Log.w(TAG, "End-to-end message " + messageType + " without a sender");
        }
        this.target = values.containsKey("User") ? values.get("User").asRawValue().getString() : null;
        if (this.target == null) {
            Log.w(TAG, "End-to-end message " + messageType + " without a target");
        }
    }

    // TODO: This function needs to get an encryption context or something to do the actual encryption.
    // For now it's the same as encoding, but will be different once end-to-end encryption is implemented.
    public byte[] encrypt() {
        return encodeToByteBuffer();
    }

    /**
     * This is used for storing the message in the outgoing message queue.  It is NOT encryption.
     */
    public byte[] encodeToByteBuffer() {
        try {
            return MsgPackUtil.bytesFromMessage(this);
        }
        catch (Exception e) {
            Log.d(TAG, "Couldn't encode message", e);
        }
        return null;
    }

    // TODO: This function needs an encryption context to create a real signature.
    public String sign() {
        return "";
    }

    public void pack(Map<String, Object> values) {
        values.put("MessageType", messageType);
        values.put("Secret", secret);
        values.put("Sender", sender);
        values.put("User", target);
        values.put("SentOn", System.currentTimeMillis() / 1000l);
    }

    private static EndToEndMessage fromMap(Map<String, Value> values) {
        if (!values.containsKey("MessageType")) {
            return null;
        }

        Class<?> messageClass = CLASSES.get(values.get("MessageType").asRawValue().getString());

        if (messageClass == null) {
            Log.d(TAG, "Unknown end-to-end message");
            MsgPackUtil.dumpMessage(values);
            return null;
        }

        try {
            return (EndToEndMessage) messageClass.getConstructor(Map.class).newInstance(values);
        }
        catch (Exception e) {
            Log.d(TAG, "Could not construct end-to-end encrypted message from values", e);
            return null;
        }
    }

    public static EndToEndMessage fromEncryptedBuffer(byte[] buffer) {
        return fromByteBuffer(buffer);
    }

    public static EndToEndMessage fromByteBuffer(byte[] buffer) {
        try {
            Map<String, Value> values = MsgPackUtil.mapFromBuffer(buffer);
            return fromMap(values);
        }
        catch (Exception e) {
            Log.d(TAG, "Could not decode end-to-end encrypted message", e);
            return null;
        }
    }

    abstract void performAction(Context context, Contact contact, Chat chat);

    public int getNotificationType() {
        return NOTIFICATION_TYPE_NONE;
    }

    public boolean isImmediateOnly() {
        return false;
    }

    public int getPriority() {
        return 10;
    }

    /**
     * A helper function for EndToEndMessage subclasses that store an incoming message.
     * <p/>
     * 1) Checks that the message is not a duplicate
     * 2) Stores the message
     * 3) Triggers a notification
     * 4) Sends a delivery confirmation
     *
     * @param context A context
     * @param message The newly received message
     * @param contact The sender
     * @param chat    The chat the message belongs to
     */
    protected final void storeIncomingMessage(Context context, Message message, Contact contact, Chat chat) {
        if (Message.findMessageByUUID(context, message.getUUID()) != null) {
            Log.d(TAG, "Received message that was already received. Ignoring.");
            return;
        }

        if (contact.isBlocked()) {
            Log.d(TAG, "Contact is blocked, ignoring message.");
            return;
        }

        Message.storeMessage(context, message);
        chat.showNotificationForMessage(context, contact, message);

        if (Preferences.shouldSendDeliveryNotification(context)) {
            EndToEndMessage payload = new EndToEndMessageStatusMessage(Parameters.with(context, chat, contact), message.getUUID(), Message.Status.DELIVERED);
            chat.sendMessage(context, payload);
        }
    }

    public boolean isSecret() {
        return secret;
    }

    @Nullable
    public String getSender() {
        return sender;
    }

    @Nullable
    public String getTarget() {
        return target;
    }

    public static final class Parameters {
        public static final String DEFAULT_TARGET = "";

        private final Chat chat;
        private final String target;
        private final String sender;

        @Nullable
        public Chat getChat() {
            return chat;
        }

        @Nullable
        public String getTarget() {
            return target;
        }

        @Nullable
        public String getSender() {
            return sender;
        }

        private Parameters(Chat chat, String target, String sender) {
            this.chat = chat;
            this.target = target;
            this.sender = sender;
        }

        public static Parameters with(Context context, Chat chat, Contact contact) {
            return new Parameters(chat, contact.getUsername(), Preferences.getUsername(context));
        }

        public static Parameters with(Context context, Chat chat) {
            return new Parameters(chat, chat.getType() == Chat.Type.SINGLE ? chat.getTarget() : DEFAULT_TARGET, Preferences.getUsername(context));
        }

        public static Parameters with(Context context) {
            return new Parameters(null, DEFAULT_TARGET, Preferences.getUsername(context));
        }
    }
}
