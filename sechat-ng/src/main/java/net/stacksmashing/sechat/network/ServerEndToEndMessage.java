package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.Preferences;
import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;

import org.msgpack.type.Value;

import java.util.Map;

public class ServerEndToEndMessage extends ServerMessage {
    public static final String NAME = "ServerEndToEndMessage";

    private final String sender;
    private final String signature;
    private final int priority;
    private final byte[] encrypted;

    public ServerEndToEndMessage(Map<String, Value> values) {
        sender = values.get("Sender").asRawValue().getString();
        signature = values.get("Signature").asRawValue().getString();
        priority = values.get("Priority").asIntegerValue().getInt();
        encrypted = values.get("Encrypted").asRawValue().getByteArray();
    }

    @Override
    void performAction(Context context) {
        EndToEndMessage payload = EndToEndMessage.fromEncryptedBuffer(encrypted);

        if (!isPayloadValid(payload, sender, Preferences.getUsername(context))) {
            return;
        }

        Contact contact = Contact.findOrCreateContactWithUsername(context, sender);
        if (contact == null) {
            Log.d(NAME, "Invalid sender \"" + sender + "\"");
            return;
        }

        long chatId = Chat.findOrCreateChatIdByContact(context, contact, payload.isSecret());
        if (chatId == -1) {
            Log.d(NAME, "Could not get chat for contact \"" + contact.getUsername() + "\"");
            return;
        }

        Chat chat = Chat.DAO.queryById(context, chatId);
        payload.performAction(context, contact, chat);
    }
}
