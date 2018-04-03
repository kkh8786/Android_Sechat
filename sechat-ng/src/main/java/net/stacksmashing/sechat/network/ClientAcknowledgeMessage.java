package net.stacksmashing.sechat.network;

import java.util.Map;

public class ClientAcknowledgeMessage extends ClientMessage {
    private final long messageId;

    public ClientAcknowledgeMessage(long messageId) {
        super("ClientAcknowledgeMessage");
        this.messageId = messageId;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("MessageId", messageId);
    }
}
