package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.voice.CallHandler;

import org.msgpack.type.Value;

import java.util.Map;

public class EndToEndCallHangupMessage extends EndToEndMessage {
    public static final String NAME = "CallHangupMessage";

    private final String callToken;

    @SuppressWarnings("unused")
    public EndToEndCallHangupMessage(Map<String, Value> values) {
        super(values);
        callToken = values.get("CallToken").asRawValue().getString();
    }

    public EndToEndCallHangupMessage(Parameters parameters, String callToken) {
        super(parameters, NAME);
        this.callToken = callToken;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("CallToken", callToken);
    }

    @Override
    void performAction(Context context, Contact contact, Chat chat) {
        Log.d(NAME, "Call hangup for token " + callToken);
        CallHandler.INSTANCE.userHungUp(context, contact.getUsername(), callToken);
    }
}
