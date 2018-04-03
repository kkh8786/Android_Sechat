package net.stacksmashing.sechat.network;

import java.util.Map;

public class ClientAddContactMessage extends ClientMessage {
    private final String username;

    public ClientAddContactMessage(String username) {
        super("ClientAddContactMessage");
        this.username = username;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("Name", username);
    }
}
