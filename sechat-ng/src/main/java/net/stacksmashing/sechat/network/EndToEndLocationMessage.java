package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.db.Message;

import org.json.JSONException;
import org.json.JSONObject;
import org.msgpack.type.Value;

import java.util.Map;

public class EndToEndLocationMessage extends EndToEndMessage {
    public static final String NAME = "LocationMessage";

    public static String messageFromLatitudeAndLongitude(double latitude, double longitude) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("latitude", latitude);
            obj.put("longitude", longitude);
        }
        catch (JSONException e) {
            Log.d(NAME, "Could not encode location as JSON", e);
            return "{\"longitude\":0,\"latitude\":0}";
        }
        return obj.toString();
    }

    private final String uuid;
    private final String message;
    private final long deleteAt;

    @SuppressWarnings("unused")
    public EndToEndLocationMessage(Map<String, Value> values) {
        super(values);
        this.uuid = values.get("UUID").asRawValue().getString();
        this.message = values.get("Message").asRawValue().getString();
        this.deleteAt = values.get("DeleteAt").asIntegerValue().getLong();
    }

    public EndToEndLocationMessage(Parameters parameters, String uuid, long deleteAt, double latitude, double longitude) {
        super(parameters, NAME);
        this.uuid = uuid;
        this.message = messageFromLatitudeAndLongitude(latitude, longitude);
        this.deleteAt = deleteAt;
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
        Message message = Message.createIncomingLocationMessage(this.uuid, chat.getId(), contact.getId(), deleteAt, this.message);
        storeIncomingMessage(context, message, contact, chat);
    }
}
