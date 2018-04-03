package net.stacksmashing.sechat.network;

import java.util.Map;

public class ClientCreateChannelMessage extends ClientMessage {
    private final String name;
    private final String description;

    public ClientCreateChannelMessage(String name, String description) {
        super("ClientCreateChannelMessage");
        this.name = name;
        this.description = description;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("Channel", name);
        values.put("Description", description);
    }
}
