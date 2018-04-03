package net.stacksmashing.sechat.network;

import java.util.Map;

public class ClientCallUpkeepMessage extends ClientMessage {
    private final String callToken;

    public ClientCallUpkeepMessage(String callToken) {
        super("ClientCallUpkeepMessage");
        this.callToken = callToken;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("CallToken", callToken);
    }
}
