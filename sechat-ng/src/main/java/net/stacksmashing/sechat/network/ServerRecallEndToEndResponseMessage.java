package net.stacksmashing.sechat.network;

import android.content.Context;

import net.stacksmashing.sechat.db.Message;

import org.msgpack.type.Value;

import java.util.Map;

public class ServerRecallEndToEndResponseMessage extends ServerMessage {
    public static final String NAME = "ServerRecallEndToEndResponseMessage";

    private final boolean success;
    private final String messageId;

    public ServerRecallEndToEndResponseMessage(Map<String, Value> values) {
        success = values.get("Successful").asBooleanValue().getBoolean();
        messageId = values.get("MessageId").asRawValue().getString();
    }

    @Override
    void performAction(Context context) {
        Message.setMessageStatusByUUID(context, messageId, success ? Message.Status.RECALLED : Message.Status.RECALL_FAILED);
    }
}
