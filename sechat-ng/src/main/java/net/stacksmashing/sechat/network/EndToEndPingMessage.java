package net.stacksmashing.sechat.network;

import android.content.Context;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.db.Message;

import org.msgpack.type.Value;

import java.util.Map;

public class EndToEndPingMessage extends EndToEndMessage {
    public static final String NAME = "PingMessage";

    private final String uuid;
    private final long deleteAt;
    private final String message;

    public EndToEndPingMessage(Parameters parameters, String uuid, long deleteAt, String message) {
        super(parameters, NAME);
        this.uuid = uuid;
        this.deleteAt = deleteAt;
        this.message = message;
    }

    @SuppressWarnings("unused")
    public EndToEndPingMessage(Map<String, Value> values) {
        super(values);
        uuid = values.get("UUID").asRawValue().getString();
        deleteAt = values.get("DeleteAt").asIntegerValue().getInt();
        if (values.containsKey("Message")) {
            message = values.get("Message").asRawValue().getString();
        }
        else {
            message = "PING";
        }
    }

    @Override
    public int getNotificationType() {
        return NOTIFICATION_TYPE_PING;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);

        values.put("Message", message);
        values.put("UUID", uuid);
        values.put("DeleteAt", deleteAt);
    }

    void performAction(Context context, Contact contact, Chat chat) {
        Message message = Message.createIncomingPingMessage(uuid, chat.getId(), contact.getId(), deleteAt);
        storeIncomingMessage(context, message, contact, chat);
    }
}

