package net.stacksmashing.sechat.network;

import android.content.Context;

import net.stacksmashing.sechat.Bus;

import org.msgpack.type.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ServerGetChannelsResponseMessage extends ServerMessage {
    public static final String NAME = "ServerGetChannelsResponseMessage";

    private final List<String> channels;

    public ServerGetChannelsResponseMessage(Map<String, Value> values) {
        List<String> channels = new ArrayList<>();
        for (Value value : values.get("Channels").asArrayValue()) {
            channels.add(value.asRawValue().getString());
        }
        this.channels = Collections.unmodifiableList(channels);
    }

    @Override
    void performAction(Context context) {
        Bus.bus().post(new Bus.ChannelListEvent(channels));
    }
}
