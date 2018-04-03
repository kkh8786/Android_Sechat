package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.util.RuntimeDataHelper;

import org.msgpack.type.Value;

import java.util.Date;
import java.util.Map;

public class ServerStatusNotificationMessage extends ServerMessage {
    private static final String TAG = "StatusNotification";

    public static final String NAME = "ServerStatusNotificationMessage";
    private final String user;
    private final int status;
    private final Date lastSeen;
    private final String statusText;

    public ServerStatusNotificationMessage(Map<String, Value> values) {
        user = values.get("User").asRawValue().getString();
        status = values.get("Status").asIntegerValue().getInt();
        lastSeen = new Date(values.get("LastSeen").asIntegerValue().getLong() * 1000);
        statusText = values.get("StatusText").asRawValue().getString();
    }

    @Override
    void performAction(Context context) {
        Contact contact = Contact.findContactByUsername(context, user);
        if (contact == null) {
            Log.d(TAG, "Contact not found: " + user);
            return;
        }
        contact.setLastSeen(lastSeen);
        contact.setStatus(statusText);
        Contact.DAO.update(context, contact);
        RuntimeDataHelper dataHelper = RuntimeDataHelper.getInstance();
        dataHelper.setContactStatus(contact, Contact.Status.fromInteger(status));
    }
}

