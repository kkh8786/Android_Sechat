package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.db.DatabaseHelper;

import org.msgpack.type.Value;

import java.util.List;
import java.util.Map;

public class ServerEndToEndGroupChatMessage extends ServerMessage {
    private static final String TAG = "GroupChatMessage";

    public static final String NAME = "ServerEndToEndGroupChatMessage";

    private final String sender;
    private final String groupToken;
    private final String signature;
    private final int priority;
    private final byte[] encrypted;

    public ServerEndToEndGroupChatMessage(Map<String, Value> values) {
        sender = values.get("Sender").asRawValue().getString();
        groupToken = values.get("Identifier").asRawValue().getString();
        signature = values.get("Signature").asRawValue().getString();
        priority = values.get("Priority").asIntegerValue().getInt();
        encrypted = values.get("Encrypted").asRawValue().getByteArray();
    }

    @Override
    void performAction(Context context) {
        List<Chat> chats = Chat.DAO.query(context,
                DatabaseHelper.COLUMN_GROUP_TOKEN + " = ?",
                new String[]{groupToken},
                null);

        if (chats.isEmpty()) {
            Log.d(TAG, "Received message for non-existent group token " + groupToken);
            return;
        }

        Chat chat = chats.get(0);

        EndToEndMessage payload = EndToEndMessage.fromEncryptedBuffer(encrypted);

        if (!isPayloadValid(payload, sender, "")) {
            return;
        }

        Contact contact = Contact.findOrCreateContactWithUsername(context, sender);

        if (contact == null) {
            Log.d(TAG, "Invalid sender \"" + sender + "\"");
            return;
        }

        payload.performAction(context, contact, chat);
    }
}
