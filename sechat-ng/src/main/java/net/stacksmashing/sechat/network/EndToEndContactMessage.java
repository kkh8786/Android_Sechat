package net.stacksmashing.sechat.network;

import android.content.Context;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.db.Message;

import org.msgpack.type.Value;

import java.util.Map;

public class EndToEndContactMessage extends EndToEndMessage {

    public static final String NAME = "ContactMessage";

    private final String uuid;
    private final String message;
    private final long deleteAt;

    @SuppressWarnings("unused")
    public EndToEndContactMessage(Map<String, Value> values) {
        super(values);
        this.uuid = values.get("UUID").asRawValue().getString();
        this.message = values.get("Message").asRawValue().getString();
        this.deleteAt = values.get("DeleteAt").asIntegerValue().getLong();
    }

    public EndToEndContactMessage(Parameters parameters, String uuid, long deleteIn, String username) {
        super(parameters, NAME);
        this.uuid = uuid;
        this.message = username;
        this.deleteAt = deleteIn;
    }

    @Override
    public int getNotificationType() {
        return NOTIFICATION_TYPE_MESSAGE;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("UUID", uuid);
        values.put("Message", message);
        values.put("DeleteAt", deleteAt);
    }

    @Override
    void performAction(Context context, Contact contact, Chat chat) {
        Message message = Message.createIncomingContactMessage(uuid, chat.getId(), contact.getId(), deleteAt, this.message);
        storeIncomingMessage(context, message, contact, chat);
    }
}
