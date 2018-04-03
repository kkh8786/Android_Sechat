package net.stacksmashing.sechat.db;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.network.EndToEndDataMessage;
import net.stacksmashing.sechat.network.EndToEndDataPartMessage;
import net.stacksmashing.sechat.network.EndToEndMessage;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import static net.stacksmashing.sechat.db.DatabaseHelper.TABLE_OUTGOING_FILES;
import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_ID;

@DAO.Table(name = TABLE_OUTGOING_FILES)
public class OutgoingFile extends Entity {
    public static final DAO<OutgoingFile> DAO = new DAO<>(OutgoingFile.class);

    @DAO.Column(name = COLUMN_ID, primaryKey = true)
    long id;

    @DAO.Column(notNull = true)
    String path;

    @DAO.Column
    long offset;

    @DAO.Column
    int chunksSent;

    @DAO.Column(notNull = true, unique = true)
    String initialUuid;

    @DAO.Column(notNull = true)
    Chat.Type targetType;

    @DAO.Column(notNull = true)
    String target;

    OutgoingFile() {
    }

    public OutgoingFile(String path, String initialUuid, String target, Chat.Type targetType) {
        this.path = path;
        this.initialUuid = initialUuid;
        this.target = target;
        this.targetType = targetType;
    }

    public EndToEndMessage getNextPayload(EndToEndMessage.Parameters parameters, byte[] data) {
        return new EndToEndDataPartMessage(parameters, initialUuid, chunksSent, data);
    }

    public EndToEndMessage getInitialPayload(EndToEndMessage.Parameters parameters, String filename, int numChunks, Message.Type type, long deleteIn) {
        return new EndToEndDataMessage(parameters, initialUuid, filename, numChunks, type.getOrdinal(), deleteIn);
    }

    public void sendNextChunk(Context context, byte[] chunk) {
        EndToEndMessage payload = getNextPayload(EndToEndMessage.Parameters.with(context), chunk);

        offset += chunk.length;
        chunksSent++;

        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        operations.add(DAO.updateOperation(this));

        OutgoingMessage.enqueueMessage(context, target, targetType, payload, null, operations);
    }

    public static void startSendingFile(Context context, Chat chat, long deleteIn, String filename, Message.Type type, String localUuid, int numChunks) {
        Message message = Message.createOutgoingFileMessage(chat.getId(), deleteIn, filename, type, localUuid);

        Log.d("OutgoingFile", "[F] Sending file " + filename + " " + localUuid);

        OutgoingFile outgoingFile = new OutgoingFile(message.getFile(context).getPath(), message.getUUID(), chat.getTarget(), chat.getType());

        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        operations.add(DAO.insertOperation(outgoingFile));

        EndToEndMessage initialPayload = outgoingFile.getInitialPayload(chat.getEndToEndParameters(context), filename, numChunks, type, deleteIn);
        OutgoingMessage.storeAndEnqueueMessage(context, chat.getTarget(), chat.getType(), initialPayload, message, operations);
    }

    public byte[] readNextChunk(int size) {
        FileInputStream inputStream = null;
        byte[] buffer = new byte[size];
        int chunkSize;

        try {
            inputStream = new FileInputStream(path);
            inputStream.skip(offset);
            chunkSize = inputStream.read(buffer);
        }
        catch (Exception e) {
            return null;
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }

        if (chunkSize <= 0) {
            return null;
        }

        byte[] chunk = new byte[chunkSize];
        System.arraycopy(buffer, 0, chunk, 0, chunkSize);
        return chunk;
    }

    public static OutgoingFile findNextFile(Context context) {
        List<OutgoingFile> outgoingFiles = DAO.query(context, null, null, COLUMN_ID + " ASC LIMIT 1");
        return outgoingFiles.isEmpty() ? null : outgoingFiles.get(0);
    }

    @Override
    public String toString() {
        return id + " (" + path + ")";
    }
}
