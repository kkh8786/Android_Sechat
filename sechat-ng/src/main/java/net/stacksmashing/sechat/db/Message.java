package net.stacksmashing.sechat.db;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.util.SparseArray;

import net.stacksmashing.sechat.MessageDeletionReceiver;
import net.stacksmashing.sechat.Preferences;
import net.stacksmashing.sechat.R;
import net.stacksmashing.sechat.network.ClientRecallEndToEndMessage;
import net.stacksmashing.sechat.network.EndToEndContactMessage;
import net.stacksmashing.sechat.network.EndToEndLocationMessage;
import net.stacksmashing.sechat.network.EndToEndMessage;
import net.stacksmashing.sechat.network.EndToEndPingMessage;
import net.stacksmashing.sechat.network.EndToEndTextMessage;
import net.stacksmashing.sechat.network.NetworkService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_CONTENT_ATTRIBUTES;
import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_ID;
import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_UUID;
import static net.stacksmashing.sechat.db.DatabaseHelper.SQL_CURRENT_TIMESTAMP;
import static net.stacksmashing.sechat.db.DatabaseHelper.TABLE_CHATS;
import static net.stacksmashing.sechat.db.DatabaseHelper.TABLE_CONTACTS;
import static net.stacksmashing.sechat.db.DatabaseHelper.TABLE_DOWNLOADS;
import static net.stacksmashing.sechat.db.DatabaseHelper.TABLE_MESSAGES;

@DAO.Table(name = TABLE_MESSAGES)
public class Message extends Entity implements Serializable {
    public static final DAO<Message> DAO = new DAO<>(Message.class);

    public enum Type {
        TEXT(1, false),
        IMAGE(2, true),
        VIDEO(3, true),
        AUDIO(4, true),
        FILE(5, true),
        LOCATION(6, false),
        PING(7, false),
        CONTACT(8, false);

        private final int ordinal;
        private final boolean file;

        Type(int ordinal, boolean file) {
            this.ordinal = ordinal;
            this.file = file;
        }

        public int getOrdinal() {
            return ordinal;
        }

        public boolean isFile() {
            return file;
        }

        public static final int COUNT = values().length;

        private static final SparseArray<Type> TYPE_MAP = new SparseArray<>(COUNT);

        static {
            for (Type t : values()) {
                TYPE_MAP.put(t.getOrdinal(), t);
            }
        }

        public static Type fromOrdinal(int ordinal) {
            return TYPE_MAP.get(ordinal);
        }
    }

    public enum Status {
        NONE(0),
        SENT(1, R.string.message_status_sent, R.color.sechat_red),
        DELIVERED(2, R.string.message_status_delivered, R.color.sechat_orange),
        READ(3, R.string.message_status_read, R.color.sechat_green) {
            @Override
            public boolean canTransitionTo(Status status) {
                return false;
            }
        },
        RECALLING(6, R.string.message_status_recalling),
        RECALLED(7, R.string.message_status_recalled),
        RECALL_FAILED(8, R.string.message_status_recall_failed);

        private final int ordinal;
        private final int stringResource;
        private final int colorResource;

        Status(int ordinal, @StringRes int stringResource, @ColorRes int colorResource) {
            this.ordinal = ordinal;
            this.stringResource = stringResource;
            this.colorResource = colorResource;
        }

        Status(int ordinal, @StringRes int stringResource) {
            this(ordinal, stringResource, android.R.color.black);
        }

        Status(int ordinal) {
            this(ordinal, 0);
        }

        public int getOrdinal() {
            return ordinal;
        }

        @ColorRes
        public int getColorResource() {
            return colorResource;
        }

        @StringRes
        public int getStringResource() {
            return stringResource;
        }

        public static final int COUNT = values().length;

        private static final SparseArray<Status> STATUS_MAP = new SparseArray<>(COUNT);

        static {
            for (Status t : values()) {
                STATUS_MAP.put(t.getOrdinal(), t);
            }
        }

        public static Status fromOrdinal(int ordinal) {
            return STATUS_MAP.get(ordinal);
        }

        public boolean canTransitionTo(Status status) {
            return true;
        }
    }

    public enum DownloadStatus {
        DOWNLOADING,
        DOWNLOADED,
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString().toUpperCase();
    }

    @DAO.Column(name = COLUMN_ID, primaryKey = true)
    long id;

    @DAO.Column(unique = true, notNull = true)
    String uuid;

    @DAO.Column(notNull = true)
    String localUuid;

    @DAO.Column(references = TABLE_CHATS + "(" + COLUMN_ID + ")")
    long chatId;

    @DAO.Column(references = TABLE_CONTACTS + "(" + COLUMN_ID + ")")
    Long contactId;

    @DAO.Column(notNull = true)
    String content;

    @DAO.Column(name = COLUMN_CONTENT_ATTRIBUTES)
    String contentAttributes;

    @DAO.Column(notNull = true)
    @NonNull
    Type type;

    @DAO.Column(notNull = true)
    @NonNull
    Status status;

    @DAO.Column
    @Nullable
    DownloadStatus downloadStatus;

    @DAO.Column(defaultValue = "0")
    boolean read;

    @DAO.Column(references = TABLE_DOWNLOADS + "(" + COLUMN_ID + ")")
    Long downloadId;

    @DAO.Column
    Date deleteAt;

    @DAO.Column
    long deleteIn;

    @DAO.Column
    Date receivedOn;

    @DAO.Column
    Date sentOn;

    @DAO.Column(defaultValue = "0")
    boolean highlight;

    @DAO.Column(notNull = true, defaultValue = SQL_CURRENT_TIMESTAMP)
    Date createdAt;

    @DAO.Column(notNull = true, defaultValue = SQL_CURRENT_TIMESTAMP)
    Date updatedAt;

    public Message() {
    }

    /**
     * Creates an outgoing text message
     */
    private Message(String uuid, @NonNull Type type, long chatId, long deleteIn, boolean read, String content, String contentAttributes) {
        this.receivedOn = new Date();
        this.content = content;
        this.contentAttributes = contentAttributes;
        this.uuid = uuid;
        this.localUuid = uuid;
        this.chatId = chatId;
        this.type = type;
        this.read = read;
        this.deleteIn = deleteIn;
        this.status = Status.NONE;

        /* If the message is already marked as read (is outgoing), set deleteAt immediately.
         * For incoming messages it will be set when the message is marked as read. */
        this.deleteAt = (read && deleteIn != 0) ? new Date(System.currentTimeMillis() + 1000 * deleteIn) : null;
    }

    /**
     * Creates an incoming text message.
     */
    private Message(String uuid, @NonNull Type type, long chatId, long contactId, long deleteIn, String content, String contentAttributes) {
        this(uuid, type, chatId, deleteIn, false, content, contentAttributes);
        this.contactId = contactId;
        this.localUuid = generateUUID();
    }

    /**
     * Creates an incoming data message.
     */
    private Message(String uuid, @NonNull Type type, long chatId, long contactId, long deleteIn, long downloadId, String content) {
        this(uuid, type, chatId, contactId, deleteIn, content, null);
        this.downloadId = downloadId;
        this.downloadStatus = DownloadStatus.DOWNLOADING;
    }

    public static Message createIncomingDataMessage(String uuid, long chatId, long contactId, long deleteIn, long downloadId, int fileType, String content) {
        return new Message(uuid, Type.fromOrdinal(fileType), chatId, contactId, deleteIn, downloadId, content);
    }

    public static Message createIncomingPingMessage(String uuid, long chatId, long contactId, long deleteIn) {
        return new Message(uuid, Type.PING, chatId, contactId, deleteIn, "PING", null);
    }

    public static Message createIncomingTextMessage(String uuid, long chatId, long contactId, long deleteIn, String content, String contentAttributes) {
        return new Message(uuid, Type.TEXT, chatId, contactId, deleteIn, content, contentAttributes);
    }

    public static Message createIncomingLocationMessage(String uuid, long chatId, long contactId, long deleteIn, String content) {
        return new Message(uuid, Type.LOCATION, chatId, contactId, deleteIn, content, null);
    }

    public static Message createIncomingContactMessage(String uuid, long chatId, long contactId, long deleteIn, String contact) {
        return new Message(uuid, Type.CONTACT, chatId, contactId, deleteIn, contact, null);
    }

    public static Message createOutgoingPingMessage(long chatId, long deleteIn) {
        return new Message(generateUUID(), Type.PING, chatId, deleteIn, true, "PING", null);
    }

    public static Message createOutgoingTextMessage(long chatId, long deleteIn, String content, String contentAttributes) {
        return new Message(generateUUID(), Type.TEXT, chatId, deleteIn, true, content, contentAttributes);
    }

    public static Message createOutgoingFileMessage(long chatId, long deleteIn, String filename, @NonNull Type fileType, String localUuid) {
        Message message = new Message(generateUUID(), fileType, chatId, deleteIn, true, filename, null);
        message.localUuid = localUuid;
        return message;
    }

    public static Message createOutgoingLocationMessage(long chatId, long deleteIn, double latitude, double longitude) {
        return new Message(generateUUID(), Type.LOCATION, chatId, deleteIn, true, EndToEndLocationMessage.messageFromLatitudeAndLongitude(latitude, longitude), null);
    }

    public static Message createOutgoingContactMessage(long chatId, long deleteIn, String username) {
        return new Message(generateUUID(), Type.CONTACT, chatId, deleteIn, true, username, null);
    }

    public static Message createForwardedMessage(long chatId, Message message) {
        return new Message(generateUUID(), message.type, chatId, message.deleteIn, true, message.content, message.contentAttributes);
    }

    public long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getContentAttributes() {
        return contentAttributes;
    }

    public String getUUID() {
        return uuid;
    }

    public String getLocalUUID() {
        return localUuid;
    }

    public boolean isIncoming() {
        return contactId != null;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
    }

    public void markAsRead(Context context) {
        if (!isRead()) {
            setRead(true);

            if (deleteIn != 0) {
                deleteAt = new Date(System.currentTimeMillis() + 1000 * deleteIn);
            }

            DAO.update(context, this);

            scheduleMessageDeletion(context);
        }
    }

    @NonNull
    public Type getType() {
        return type;
    }

    public Date getReceivedOn() {
        return receivedOn;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    public void setStatus(@NonNull Status status) {
        this.status = status;
    }

    public boolean isDownloading() {
        return downloadStatus == DownloadStatus.DOWNLOADING;
    }

    public void setDownloadStatus(@Nullable DownloadStatus downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public Long getContactId() {
        return contactId;
    }

    public String getDescription(Context context) {
        switch (this.type) {
            case TEXT:
                return getContent();
            case PING:
                return context.getString(R.string.message_description_ping);
            case AUDIO:
                return context.getString(R.string.message_description_audio);
            case FILE:
                return context.getString(R.string.message_description_file);
            case IMAGE:
                return context.getString(R.string.message_description_image);
            case VIDEO:
                return context.getString(R.string.message_description_video);
            case LOCATION:
                return context.getString(R.string.message_description_location);
            case CONTACT:
                return context.getString(R.string.message_description_contact) + ": " + getContent();
        }
        return "";
    }

    @ColorRes
    public int getContentColor() {
        if (getType() == Type.PING) {
            return R.color.sechat_red;
        }
        return isIncoming() ? android.R.color.black : android.R.color.white;
    }

    public static void setMessageStatusByUUID(Context context, String uuid, Status status) {
        Message message = findMessageByUUID(context, uuid);
        if (message != null && message.getStatus().canTransitionTo(status)) {
            message.setStatus(status);
            DAO.update(context, message);
        }
    }

    public static Message findMessageByUUID(Context context, String uuid) {
        List<Message> messageList = Message.DAO.query(context, COLUMN_UUID + " = ?", new String[]{uuid}, null);
        if (messageList.isEmpty()) {
            Log.d("Message", "No message found.");
            return null;
        }

        return messageList.get(0);
    }

    public String getSenderUsername(Context context) {
        if (isIncoming()) {
            return Contact.DAO.queryById(context, getContactId()).getUsername();
        }
        return Preferences.getUsername(context);
    }

    public static void storeMessage(Context context, Message message) {
        Message.DAO.insert(context, message);

        if (message.getDeleteAt() != null) {
            scheduleMessageDeletion(context);
        }
    }

    public CharSequence getNotificationTitle(Context context, Contact sender, Chat chat) {
        /* This is a bit hard to read, the idea was to make it easier to translate the messages, but perhaps it wasn't the wisest one at this point. */
        StringBuilder title = new StringBuilder(context.getString(R.string.notification_new)).append(' ');
        title.append(getType() == Type.PING ? context.getString(R.string.notification_ping) : context.getString(R.string.notification_message)).append(' ');
        title.append(context.getString(R.string.notification_from)).append(' ').append(sender.getUsername());
        if (chat.getType().hasMultipleUsers()) {
            title.append(' ').append(context.getString(R.string.notification_in)).append(' ').append(chat.getName());
        }
        return title.toString();
    }

    public CharSequence getNotificationText(Context context, Contact sender, Chat chat) {
        return getDescription(context);
    }

    public Location getLocation() {
        if (getType() != Type.LOCATION) {
            return null;
        }

        try {
            JSONObject object = new JSONObject(getContent());
            Location result = new Location((String) null);
            result.setLatitude(object.getDouble("latitude"));
            result.setLongitude(object.getDouble("longitude"));
            return result;
        }
        catch (JSONException e) {
            Log.d("Message", "Could not decode location information", e);
            return null;
        }
    }

    public boolean canRecall() {
        return !isIncoming() && getStatus() == Status.SENT;
    }

    public void recall() {
        if (!canRecall()) {
            return;
        }
        Log.d("Message", "Recalling " + getUUID());
        setMessageStatusByUUID(NetworkService.getInstance(), getUUID(), Status.RECALLING);
        NetworkService.getInstance().asyncSend(new ClientRecallEndToEndMessage(getUUID()));
    }

    public Date getDeleteAt() {
        return deleteAt;
    }

    public Download queryDownload(Context context) {
        return downloadId != null ? Download.DAO.queryById(context, downloadId) : null;
    }

    public File getFile(Context context) {
        return getFileForUUID(context, getLocalUUID());
    }

    public EndToEndMessage getPayload(EndToEndMessage.Parameters parameters) {
        switch (getType()) {
            case TEXT:
                return new EndToEndTextMessage(parameters, uuid, deleteIn, content, contentAttributes);
            case LOCATION:
                return new EndToEndLocationMessage(parameters, uuid, deleteIn, getLocation().getLatitude(), getLocation().getLongitude());
            case PING:
                return new EndToEndPingMessage(parameters, uuid, deleteIn, content);
            case CONTACT:
                return new EndToEndContactMessage(parameters, uuid, deleteIn, content);
        }
        return null;
    }

    public void forward(Context context, Chat chat) {
        if (type.isFile()) {
            chat.sendFile(deleteIn, content, type, localUuid);
        }
        else {
            chat.storeAndSendMessage(context, createForwardedMessage(chat.getId(), this));
        }
    }

    public static File getFileForUUID(Context context, String uuid) {
        return new File(context.getFilesDir(), uuid);
    }

    public static void scheduleMessageDeletion(Context context) {
        List<Message> messages = DAO.query(context, "DELETE_AT IS NOT NULL", null, "DELETE_AT ASC LIMIT 1");
        if (messages.isEmpty()) {
            Log.d("Message", "No burn messages");
            return;
        }
        Log.d("Message", "Next burn message: " + messages.get(0).getDeleteAt());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, MessageDeletionReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, messages.get(0).getDeleteAt().getTime(), pendingIntent);
    }

    public static void deleteExpiredMessages(Context context) {
        context.getContentResolver().delete(
                DAO.getContentUri(),
                "DELETE_AT IS NOT NULL AND DELETE_AT <= ?",
                new String[]{"" + System.currentTimeMillis()});
    }
}
