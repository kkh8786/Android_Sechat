package net.stacksmashing.sechat.db;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import net.stacksmashing.sechat.Bus;
import net.stacksmashing.sechat.network.ClientEndToEndChannelMessage;
import net.stacksmashing.sechat.network.ClientEndToEndGroupChatMessage;
import net.stacksmashing.sechat.network.ClientEndToEndMessage;
import net.stacksmashing.sechat.network.ClientMessage;
import net.stacksmashing.sechat.network.EndToEndMessage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_ID;
import static net.stacksmashing.sechat.db.DatabaseHelper.TABLE_OUTGOING_MESSAGES;

@DAO.Table(name = TABLE_OUTGOING_MESSAGES)
public class OutgoingMessage extends Entity implements Serializable {
    private static final String TAG = "OutgoingMessage";

    public static final DAO<OutgoingMessage> DAO = new DAO<>(OutgoingMessage.class);

    @DAO.Column(name = COLUMN_ID, primaryKey = true)
    long id;

    @DAO.Column
    byte[] payload;

    @DAO.Column
    int localPriority;

    @DAO.Column(notNull = true)
    Chat.Type targetType;

    @DAO.Column
    String target;

    @DAO.Column
    long createdAt;

    @DAO.Column(unique = true)
    String uuid;

    @DAO.Column
    Long session;

    OutgoingMessage() {
    }

    public OutgoingMessage(Chat.Type targetType, String target, byte[] payload, @Nullable String uuid, int localPriority) {
        this.targetType = targetType;
        this.target = target;
        this.payload = payload;
        this.localPriority = localPriority;
        this.uuid = uuid != null ? uuid : Message.generateUUID();
        this.createdAt = System.currentTimeMillis();
    }

    public static OutgoingMessage findNextOutgoingMessage(Context context, long session) {
        List<OutgoingMessage> messages = DAO.query(context, "SESSION IS NULL OR SESSION <> ?", new String[]{Long.toString(session)}, "LOCAL_PRIORITY DESC, CREATED_AT ASC LIMIT 1");
        return messages.isEmpty() ? null : messages.get(0);
    }

    public static OutgoingMessage findOutgoingMessageByUUID(Context context, String uuid) {
        List<OutgoingMessage> result = DAO.query(context, "UUID = ?", new String[]{uuid}, null);
        return result.isEmpty() ? null : result.get(0);
    }

    public static void storeAndEnqueueMessage(Context context, String target, Chat.Type targetType, EndToEndMessage payload, @NonNull Message message) {
        storeAndEnqueueMessage(context, target, targetType, payload, message, null);
    }

    public static void storeAndEnqueueMessage(Context context, String target, Chat.Type targetType, EndToEndMessage payload, @NonNull Message message, @Nullable ArrayList<ContentProviderOperation> otherOperations) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        operations.add(Message.DAO.insertOperation(message));

        if (otherOperations != null) {
            operations.addAll(otherOperations);
        }

        enqueueMessage(context, target, targetType, payload, message.getUUID(), operations);

        // FIXME: This duplicates Message#storeMessage.
        if (message.getDeleteAt() != null) {
            Message.scheduleMessageDeletion(context);
        }
    }

    public static void enqueueMessage(Context context, String target, Chat.Type targetType, EndToEndMessage payload, @Nullable String uuid, @Nullable ArrayList<ContentProviderOperation> otherOperations) {
        OutgoingMessage newMessage = new OutgoingMessage(targetType, target, payload.encodeToByteBuffer(), uuid, payload.getPriority());

        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        operations.add(DAO.insertOperation(newMessage));

        if (otherOperations != null) {
            operations.addAll(otherOperations);
        }

        try {
            context.getContentResolver().applyBatch(DatabaseProvider.AUTHORITY, operations);
        }
        catch (Exception e) {
            Log.e(TAG, "Could not store and enqueue outgoing message", e);
            return;
        }

        Bus.bus().post(new Bus.OutgoingMessageEnqueuedEvent());
    }

    public ClientMessage asClientMessage() {
        switch (getTargetType()) {
            case SINGLE:
                return new ClientEndToEndMessage(this);
            case GROUP:
                return new ClientEndToEndGroupChatMessage(this);
            case CHANNEL:
                return new ClientEndToEndChannelMessage(this);
        }
        return null;
    }

    public String getTarget() {
        return target;
    }

    public EndToEndMessage getPayload() {
        return EndToEndMessage.fromByteBuffer(payload);
    }

    public Chat.Type getTargetType() {
        return targetType;
    }

    public long getId() {
        return id;
    }

    public int getLocalPriority() {
        return localPriority;
    }

    public String getUUID() {
        return uuid;
    }

    public void setSession(long session) {
        this.session = session;
    }
}
