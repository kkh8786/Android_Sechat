package net.stacksmashing.sechat.network;

import android.content.Context;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.video.StreamManager;

import org.msgpack.type.Value;

import java.util.Map;

public class EndToEndCallVideoFinishMessage extends EndToEndMessage {
    public static final String NAME = "CallVideoFinishMessage";

    public EndToEndCallVideoFinishMessage(Parameters parameters) {
        super(parameters, NAME);
    }

    @SuppressWarnings("unused")
    public EndToEndCallVideoFinishMessage(Map<String, Value> values) {
        super(values);
    }

    @Override
    void performAction(Context context, Contact contact, Chat chat) {
        StreamManager.INSTANCE.removeStream(contact.getUsername());
    }
}
