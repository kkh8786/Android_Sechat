package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.db.Contact;

import org.msgpack.type.Value;

import java.util.Map;

public class ServerProfilePictureUpdateMessage extends ServerMessage {
    private static final String TAG = "ProfilePictureUpdate";

    public static final String NAME = "ServerProfilepictureUpdateMessage";

    private final String sender;
    private final byte[] profilePicture;

    public ServerProfilePictureUpdateMessage(Map<String, Value> values) {
        sender = values.get("Sender").asRawValue().getString();
        profilePicture = values.get("ProfilePicture").asRawValue().getByteArray();
    }

    @Override
    void performAction(Context context) {
        /* FIXME Duplicates ServerGetProfileResponseMessage's performAction. */
        Contact contact = Contact.findContactByUsername(context, sender);
        if (contact == null) {
            Log.d(TAG, "Contact not found: " + sender);
            return;
        }

        if (profilePicture != null) {
            Log.d(TAG, "Writing profile picture!");
            contact.setProfilePicture(context, profilePicture);
        }
    }
}
