package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.voice.Crypto;
import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.voice.CallHandler;

import org.msgpack.type.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EndToEndCallInvitationMessage extends EndToEndMessage {
    public static final String NAME = "CallInvitationMessage";

    private final String callToken;
    private final List<String> users;
    private final String publicIP;
    private final String publicPort;

    @SuppressWarnings("unused")
    public EndToEndCallInvitationMessage(Map<String, Value> values) {
        super(values);
        callToken = values.get("CallToken").asRawValue().getString();
        Value[] userValues = values.get("Users").asArrayValue().getElementArray();
        users = new ArrayList<>(userValues.length);
        for (Value value : userValues) {
            users.add(value.asRawValue().getString());
        }
        publicIP = values.get("VoipPublicIP").asRawValue().getString();
        publicPort = values.get("VoipPublicPort").asRawValue().getString();
    }

    public EndToEndCallInvitationMessage(Parameters parameters, String callToken, String[] users, String publicIP, String publicPort) {
        super(parameters, NAME);
        this.callToken = callToken;
        this.users = Arrays.asList(users);
        this.publicIP = publicIP;
        this.publicPort = publicPort;
    }

    @Override
    public int getNotificationType() {
        return NOTIFICATION_TYPE_CALL;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("CallToken", callToken);
        values.put("Users", users);
        values.put("VoipPublicIP", publicIP);
        values.put("VoipPublicPort", publicPort);
    }

    @Override
    void performAction(Context context, Contact contact, Chat chat) {
        CallHandler.INSTANCE.checkIncomingCallStatus(context, contact.getUsername(), callToken, users, publicIP, publicPort, Crypto.KEY);

        Log.d(NAME, "Incoming call with token " + callToken);

        /* FIXME: Use a heads up notification in 5.0 */
    }
}
