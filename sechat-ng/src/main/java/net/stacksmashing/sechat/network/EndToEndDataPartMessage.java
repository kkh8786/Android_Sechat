package net.stacksmashing.sechat.network;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.db.DatabaseProvider;
import net.stacksmashing.sechat.db.Download;
import net.stacksmashing.sechat.db.Message;

import org.apache.commons.io.IOUtils;
import org.msgpack.type.Value;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class EndToEndDataPartMessage extends EndToEndMessage {
    public static final String NAME = "DataPartMessage";

    private final byte[] data;
    private final int partNum;
    private final String uuid;

    @SuppressWarnings("unused")
    public EndToEndDataPartMessage(Map<String, Value> values) {
        super(values);
        data = values.get("Data").asRawValue().getByteArray();
        partNum = values.get("Part").asIntegerValue().getInt();
        uuid = values.get("UUID").asRawValue().getString();
    }

    public EndToEndDataPartMessage(Parameters parameters, String uuid, int partNum, byte[] data) {
        super(parameters, NAME);
        this.data = data.clone();
        this.partNum = partNum;
        this.uuid = uuid;
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("Data", data);
        values.put("Part", partNum);
        values.put("UUID", uuid);
    }

    @Override
    void performAction(Context context, Contact contact, Chat chat) {
        Log.d(NAME, "uuid " + uuid + " part " + partNum);

        Message message = Message.findMessageByUUID(context, uuid);

        if (message == null) {
            return;
        }

        Download download = message.queryDownload(context);

        if (download == null || !download.isNextPartNumber(partNum)) {
            return;
        }

        writePartToFile(message.getFile(context));

        updateDatabase(context, message, download);
    }

    private void writePartToFile(File file) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file, true);
            outputStream.write(data);
        }
        catch (IOException e) {
            Log.d(NAME, "Could not write file part", e);
        }
        finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    private void updateDatabase(Context context, Message message, Download download) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        download.setLastReceivedPart(partNum);

        operations.add(Download.DAO.updateOperation(download));

        if (download.isComplete()) {
            message.setDownloadStatus(Message.DownloadStatus.DOWNLOADED);

            operations.add(Message.DAO.updateOperation(message));
        }

        try {
            context.getContentResolver().applyBatch(DatabaseProvider.AUTHORITY, operations);
        }
        catch (Exception e) {
            Log.d(NAME, "Could not update database for received data part", e);
        }
    }
}
