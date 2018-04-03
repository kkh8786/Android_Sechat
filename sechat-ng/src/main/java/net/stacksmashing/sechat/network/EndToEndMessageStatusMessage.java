package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.db.Message;

import org.msgpack.type.Value;

import java.util.Map;

public class EndToEndMessageStatusMessage extends EndToEndMessage {
    public static final String NAME = "MessageStatusMessage";

    private final String uuid;
    private final int status;

    @SuppressWarnings("unused")
    public EndToEndMessageStatusMessage(Map<String, Value> values) {
        super(values);
        uuid = values.get("UUID").asRawValue().getString();
        status = values.get("Status").asIntegerValue().getInt();
    }

    public EndToEndMessageStatusMessage(Parameters parameters, String uuid, Message.Status status) {
        super(parameters, NAME);
        this.uuid = uuid;
        this.status = status.getOrdinal();
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("UUID", uuid);
        values.put("Status", status);
    }

    @Override
    void performAction(Context context, Contact contact, Chat chat) {
        Log.d(NAME, "Message status " + uuid + " " + status);
        Message.setMessageStatusByUUID(context, uuid, Message.Status.fromOrdinal(status));
    }
}
