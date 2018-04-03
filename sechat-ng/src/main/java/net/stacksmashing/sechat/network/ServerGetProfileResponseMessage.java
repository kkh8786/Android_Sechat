package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.db.Contact;

import org.msgpack.type.Value;

import java.util.Map;

public class ServerGetProfileResponseMessage extends ServerMessage {
    private static final String TAG = "GetProfileResponse";

    public static final String NAME = "ServerGetProfileResponseMessage";
    private final String username;
    private final byte[] profilePicture;
    private final String userX;
    private final String userY;
    private final boolean successful;
    private final String error;

    public ServerGetProfileResponseMessage(Map<String, Value> values) {
        username = values.get("Username").asRawValue().getString();
        successful = values.get("Successful").asBooleanValue().getBoolean();
        profilePicture = values.get("Profilepicture").asRawValue().getByteArray();
        userX = values.get("UserX").asRawValue().getString();
        userY = values.get("UserY").asRawValue().getString();
        error = values.get("Error").asRawValue().getString();
    }


    @Override
    void performAction(Context context) {
        if (!successful) {
            Log.d(TAG, "Not successful, error: " + error);
            return;
        }

        Contact contact = Contact.findContactByUsername(context, username);
        if (contact == null) {
            Log.d(TAG, "Contact not found: " + username);
            return;
        }

        if (profilePicture != null) {
            Log.d(TAG, "Writing profile picture!");
            contact.setProfilePicture(context, profilePicture);
        }
    }
}
