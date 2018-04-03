package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;

import org.msgpack.type.Value;

import java.util.Map;

public class ServerEndToEndChannelMessage extends ServerMessage {
    public static final String NAME = "ServerEndToEndChannelMessage";

    private final String sender;
    private final String signature;
    private final int priority;
    private final byte[] encrypted;
    private final String channel;

    public ServerEndToEndChannelMessage(Map<String, Value> values) {
        sender = values.get("Sender").asRawValue().getString();
        signature = values.get("Signature").asRawValue().getString();
        priority = values.get("Priority").asIntegerValue().getInt();
        encrypted = values.get("Encrypted").asRawValue().getByteArray();
        channel = values.get("Channel").asRawValue().getString();
    }

    @Override
    void performAction(Context context) {
        Chat chat = Chat.findChannel(context, channel);

        if (chat == null) {
            Log.d("EndToEndChannelMessage", "Received message for non-existent channel \"" + channel + "\"");
            return;
        }

        EndToEndMessage payload = EndToEndMessage.fromEncryptedBuffer(encrypted);

        if (!isPayloadValid(payload, sender, "")) {
            return;
        }

        Contact contact = Contact.findOrCreateContactWithUsername(context, sender);
        if (contact == null) {
            return;
        }

        payload.performAction(context, contact, chat);
    }
}
