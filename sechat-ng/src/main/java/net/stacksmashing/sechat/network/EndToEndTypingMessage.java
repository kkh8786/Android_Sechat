package net.stacksmashing.sechat.network;

import android.content.Context;

import net.stacksmashing.sechat.Bus;
import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;

import org.msgpack.type.Value;

import java.util.Map;

public class EndToEndTypingMessage extends EndToEndMessage {
    public static final String NAME = "TypingMessage";

    @SuppressWarnings("unused")
    public EndToEndTypingMessage(Map<String, Value> values) {
        super(values);
    }

    public EndToEndTypingMessage(Parameters parameters) {
        super(parameters, NAME);
    }

    @Override
    public boolean isImmediateOnly() {
        return true;
    }

    @Override
    void performAction(Context context, Contact contact, Chat chat) {
        Bus.bus().post(new Bus.IsTypingEvent(getSender(), chat.getId()));
    }
}
