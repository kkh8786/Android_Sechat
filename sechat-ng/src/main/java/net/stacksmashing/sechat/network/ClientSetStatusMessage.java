package net.stacksmashing.sechat.network;

import java.util.Map;

public class ClientSetStatusMessage extends ClientMessage {
    private final String status;

    public ClientSetStatusMessage(String status) {
        super("ClientSetStatusMessage");
        this.status = status;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("StatusText", status);
    }
}
