package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;

import org.msgpack.type.Value;

import java.util.Map;

public class EndToEndInChatMessage extends EndToEndMessage {
    public static final String NAME = "InChatMessage";

    private final boolean entering;
    private final String groupToken;

    @SuppressWarnings("unused")
    public EndToEndInChatMessage(Map<String, Value> values) {
        super(values);
        this.entering = values.get("Entering").asBooleanValue().getBoolean();
        this.groupToken = values.get("GroupToken").asRawValue().getString();
    }

    public EndToEndInChatMessage(Parameters parameters, boolean entering, String groupToken) {
        super(parameters, NAME);
        this.entering = entering;
        this.groupToken = groupToken;
    }

    public EndToEndInChatMessage(Parameters parameters, boolean entering) {
        this(parameters, entering, null);
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("Entering", entering);
        values.put("GroupToken", groupToken != null ? groupToken : "");
    }

    @Override
    void performAction(Context context, Contact contact, Chat chat) {
        Log.d(NAME, contact.getUsername() + " has " + (entering ? "entered" : "left") + " chat " + chat.getName());
    }
}
