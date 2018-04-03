package net.stacksmashing.sechat.network;

import java.util.Map;

public class ClientRecallEndToEndMessage extends ClientMessage {
    private final String messageId;

    public ClientRecallEndToEndMessage(String messageId) {
        super("ClientRecallEndToEndMessage");
        this.messageId = messageId;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("MessageId", messageId);
    }
}
