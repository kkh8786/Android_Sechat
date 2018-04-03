package net.stacksmashing.sechat.network;

import android.content.Context;

import net.stacksmashing.sechat.Preferences;
import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.db.Message;

import org.msgpack.type.Value;

import java.util.Map;
import java.util.regex.Pattern;

public class EndToEndTextMessage extends EndToEndMessage {
    public static final String NAME = "TextMessage";

    private final String message;
    private final String attributes;
    private final String uuid;
    private final long deleteAt;

    public EndToEndTextMessage(Parameters parameters, String uuid, long deleteAt, String message, String attributes) {
        super(parameters, NAME);
        this.message = message;
        this.attributes = attributes;
        this.uuid = uuid;
        this.deleteAt = deleteAt;
    }

    @SuppressWarnings("unused")
    public EndToEndTextMessage(Map<String, Value> values) {
        super(values);
        message = values.get("Message").asRawValue().getString();
        if (values.containsKey("Attributes")) {
            attributes = values.get("Attributes").asRawValue().getString();
        }
        else {
            attributes = null;
        }
        uuid = values.get("UUID").asRawValue().getString();
        deleteAt = values.get("DeleteAt").asIntegerValue().getInt();
    }

    @Override
    public int getNotificationType() {
        return NOTIFICATION_TYPE_MESSAGE;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("Message", message);
        values.put("UUID", uuid);
        values.put("DeleteAt", deleteAt);
        if (attributes != null) {
            values.put("Attributes", attributes);
        }
    }

    void performAction(Context context, Contact contact, Chat chat) {
        Message message = Message.createIncomingTextMessage(uuid, chat.getId(), contact.getId(), deleteAt, this.message, attributes);

        message.setHighlight(isHighlight(context, this.message));

        storeIncomingMessage(context, message, contact, chat);
    }

    private static Pattern HIGHLIGHT_PATTERN;

    private static boolean isHighlight(Context context, String message) {
        if (HIGHLIGHT_PATTERN == null) {
            HIGHLIGHT_PATTERN = Pattern.compile("@" + Preferences.getUsername(context) + "(\\W|$)");
        }

        return HIGHLIGHT_PATTERN.matcher(message).find();
    }
}
