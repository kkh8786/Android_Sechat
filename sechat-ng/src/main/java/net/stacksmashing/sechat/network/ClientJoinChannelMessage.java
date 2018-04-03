package net.stacksmashing.sechat.network;

import java.util.Map;

public class ClientJoinChannelMessage extends ClientMessage {
    private final String channel;

    public ClientJoinChannelMessage(String channel) {
        super("ClientJoinChannelMessage");
        this.channel = channel;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("Channel", channel);
    }
}
