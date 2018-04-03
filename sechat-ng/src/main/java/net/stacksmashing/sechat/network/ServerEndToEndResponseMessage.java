package net.stacksmashing.sechat.network;

import android.content.Context;

import net.stacksmashing.sechat.db.Message;
import net.stacksmashing.sechat.db.OutgoingMessage;

import org.msgpack.type.Value;

import java.util.Map;

public class ServerEndToEndResponseMessage extends ServerMessage {
    public static final String NAME = "ServerEndToEndResponseMessage";

    private final String messageId;

    public ServerEndToEndResponseMessage(Map<String, Value> values) {
        messageId = values.get("MessageId").asRawValue().getString();
    }

    @Override
    void performAction(Context context) {
        OutgoingMessage outgoingMessage = OutgoingMessage.findOutgoingMessageByUUID(context, messageId);

        if (outgoingMessage != null) {
            OutgoingMessage.DAO.delete(context, outgoingMessage);

            Message.setMessageStatusByUUID(context, outgoingMessage.getUUID(), Message.Status.SENT);
        }
    }
}
