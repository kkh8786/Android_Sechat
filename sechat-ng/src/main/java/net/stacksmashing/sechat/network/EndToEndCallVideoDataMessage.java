package net.stacksmashing.sechat.network;

import android.content.Context;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.video.StreamManager;

import org.msgpack.type.Value;

import java.util.Map;

public class EndToEndCallVideoDataMessage extends EndToEndMessage {
    public static final String NAME = "CallImageMessage";

    private final byte[] data;
    private final String callToken;

    @SuppressWarnings("unused")
    public EndToEndCallVideoDataMessage(Map<String, Value> values) {
        super(values);
        this.data = values.get("Image").asRawValue().getByteArray();
        this.callToken = values.get("CallToken").asRawValue().getString();
    }

    public EndToEndCallVideoDataMessage(Parameters parameters, String callToken, byte[] data) {
        super(parameters, NAME);
        this.data = data;
        this.callToken = callToken;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("Image", data);
        values.put("CallToken", callToken);
    }

    @Override
    void performAction(Context context, Contact contact, Chat chat) {
        StreamManager.INSTANCE.decodeData(contact.getUsername(), data);
    }
}
