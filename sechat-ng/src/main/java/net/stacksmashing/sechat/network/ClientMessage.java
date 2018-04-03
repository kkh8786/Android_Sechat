package net.stacksmashing.sechat.network;

import java.util.Map;

public abstract class ClientMessage implements Packable {
    private final String MessageType;

    public ClientMessage(String messageType) {
        MessageType = messageType;
    }

    public void pack(Map<String, Object> values) {
        values.put("MessageType", MessageType);
    }
}
