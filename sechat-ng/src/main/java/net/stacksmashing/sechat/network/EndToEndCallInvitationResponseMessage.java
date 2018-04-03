package net.stacksmashing.sechat.network;

import android.content.Context;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.voice.CallHandler;

import org.msgpack.type.Value;

import java.util.Map;

public class EndToEndCallInvitationResponseMessage extends EndToEndMessage {
    public static final String NAME = "CallInvitationResponseMessage";

    private final String callToken;
    private final boolean accept;

    @SuppressWarnings("unused")
    public EndToEndCallInvitationResponseMessage(Map<String, Value> values) {
        super(values);
        callToken = values.get("CallToken").asRawValue().getString();
        accept = values.get("Accept").asBooleanValue().getBoolean();
    }

    public EndToEndCallInvitationResponseMessage(Parameters parameters, String callToken, boolean accept) {
        super(parameters, NAME);
        this.callToken = callToken;
        this.accept = accept;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("CallToken", callToken);
        values.put("Accept", accept);
    }

    @Override
    void performAction(Context context, Contact contact, Chat chat) {
        CallHandler.INSTANCE.userRespondedToInvitation(context, contact.getUsername(), callToken, accept);
    }
}
