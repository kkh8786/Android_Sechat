package net.stacksmashing.sechat.network;

import android.content.Context;

import net.stacksmashing.sechat.voice.CallHandler;

import org.msgpack.type.Value;

import java.util.Map;

public class ServerCallCreateResponseMessage extends ServerMessage {
    public static final String NAME = "ServerCallCreateResponseMessage";

    private final String callToken;

    public ServerCallCreateResponseMessage(Map<String, Value> values) {
        this.callToken = values.get("CallToken").asRawValue().getString();
    }

    @Override
    void performAction(Context context) {
        CallHandler.INSTANCE.callCreated(context, callToken);
    }
}
