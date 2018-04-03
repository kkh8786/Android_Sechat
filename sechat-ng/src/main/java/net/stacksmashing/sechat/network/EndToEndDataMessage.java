package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.db.Download;
import net.stacksmashing.sechat.db.Message;

import org.msgpack.type.Value;

import java.util.Map;

public class EndToEndDataMessage extends EndToEndMessage {
    private static final String TAG = "EndToEndDataMessage";
    public static final String NAME = "DataMessage";

    private final int dataType;
    private final String filename;
    private final int numParts;
    private final String uuid;
    private final long deleteAt;

    @SuppressWarnings("unused")
    public EndToEndDataMessage(Map<String, Value> values) {
        super(values);
        dataType = values.get("DataType").asIntegerValue().getInt();
        filename = values.get("Message").asRawValue().getString();
        numParts = values.get("Parts").asIntegerValue().getInt();
        uuid = values.get("UUID").asRawValue().getString();
        deleteAt = values.get("DeleteAt").asIntegerValue().getLong();
    }

    public EndToEndDataMessage(Parameters parameters, String uuid, String filename, int numParts, int dataType, long deleteAt) {
        super(parameters, NAME);
        this.uuid = uuid;
        this.filename = filename;
        this.numParts = numParts;
        this.dataType = dataType;
        this.deleteAt = deleteAt;
    }

    @Override
    public int getNotificationType() {
        return NOTIFICATION_TYPE_MESSAGE;
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("DataType", dataType);
        values.put("Message", filename);
        values.put("Parts", numParts);
        values.put("UUID", uuid);
        values.put("DeleteAt", deleteAt);
    }

    @Override
    void performAction(Context context, Contact contact, Chat chat) {
        try {
            long downloadId = Download.DAO.insert(context, new Download(uuid, dataType, numParts));

            Message message = Message.createIncomingDataMessage(uuid, chat.getId(), contact.getId(), deleteAt, downloadId, dataType, filename);
            storeIncomingMessage(context, message, contact, chat);
        }
        catch (net.sqlcipher.database.SQLiteConstraintException e) {
            // The download already exists
            Log.d(TAG, "Download already exists.");
        }
    }
}
