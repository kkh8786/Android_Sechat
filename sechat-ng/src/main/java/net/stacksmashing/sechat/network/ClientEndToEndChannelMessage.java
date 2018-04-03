package net.stacksmashing.sechat.network;

import net.stacksmashing.sechat.db.OutgoingMessage;

import java.util.Map;

public class ClientEndToEndChannelMessage extends ClientMessage {
    private final String channel;
    private final EndToEndMessage payload;
    private final String messageId;

    public ClientEndToEndChannelMessage(String channel, EndToEndMessage payload, String messageId) {
        super("ClientEndToEndChannelMessage");
        this.channel = channel;
        this.payload = payload;
        this.messageId = messageId;
    }

    public ClientEndToEndChannelMessage(OutgoingMessage outgoingMessage) {
        this(outgoingMessage.getTarget(), outgoingMessage.getPayload(), outgoingMessage.getUUID());
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("Channel", channel);
        values.put("Encrypted", payload != null ? payload.encrypt() : new byte[0]);
        values.put("Signature", payload != null ? payload.sign() : "");
        values.put("Notify", payload != null ? payload.getNotificationType() : 0);
        values.put("ImmediateOnly", (payload != null && payload.isImmediateOnly()) ? 1 : 0);
        values.put("Priority", payload != null ? payload.getPriority() : 0);
        if (messageId != null) {
            values.put("MessageId", messageId);
        }
    }
}
