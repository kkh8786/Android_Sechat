package net.stacksmashing.sechat.network;

import java.util.Map;

public class ClientCallGetStatusMessage extends ClientMessage {
    private final String callToken;

    public ClientCallGetStatusMessage(String callToken) {
        super("ClientCallGetStatusMessage");
        this.callToken = callToken;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("CallToken", callToken);
    }
}
