package net.stacksmashing.sechat.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClientSubscribeStatusMessage extends ClientMessage {
    private final List<String> subscribeTo;

    public ClientSubscribeStatusMessage(List<String> subscribeTo) {
        super("ClientSubscribeStatusMessage");
        this.subscribeTo = new ArrayList<>(subscribeTo);
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("SubscribeTo", subscribeTo);
    }
}
