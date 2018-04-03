package net.stacksmashing.sechat.db;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import net.stacksmashing.sechat.NavigationActivity;
import net.stacksmashing.sechat.Preferences;
import net.stacksmashing.sechat.R;
import net.stacksmashing.sechat.network.EndToEndInChatMessage;
import net.stacksmashing.sechat.network.EndToEndMessage;
import net.stacksmashing.sechat.network.NetworkService;
import net.stacksmashing.sechat.util.RuntimeDataHelper;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_CHAT_ID;
import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_CONTACT_ID;
import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_GROUP_TOKEN;
import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_ID;
import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_READ;
import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_TYPE;
import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_USERNAME;
import static net.stacksmashing.sechat.db.DatabaseHelper.SQL_CURRENT_TIMESTAMP;
import static net.stacksmashing.sechat.db.DatabaseHelper.TABLE_CHATS;
import static net.stacksmashing.sechat.db.DatabaseHelper.TABLE_MESSAGES;

@DAO.Table(name = TABLE_CHATS, uniqueConstraints = @DAO.UniqueConstraint(columnNames = {"name", "type", "secret"}))
public class Chat extends Entity implements Serializable {
    private static final String TAG = "Chat";
    public static final DAO<Chat> DAO = new DAO<>(Chat.class);

    public enum Type {
        SINGLE,
        GROUP,
        CHANNEL;

        public boolean hasMultipleUsers() {
            switch (this) {
                case SINGLE:
                    return false;
                case GROUP:
                case CHANNEL:
                    return true;
            }
            return false;
        }
    }

    @DAO.Column(name = COLUMN_ID, primaryKey = true)
    long id;

    @DAO.Column(notNull = true)
    @NonNull
    String name;

    @DAO.Column(notNull = true)
    @NonNull
    Type type;

    @DAO.Column(unique = true)
    String groupToken;

    @DAO.Column
    String creator;

    @DAO.Column(defaultValue = "0")
    boolean secret;

    @DAO.Column(defaultValue = "0")
    int burnTime;

    @DAO.Column(notNull = true, defaultValue = SQL_CURRENT_TIMESTAMP)
    Date createdAt;

    @DAO.Column(notNull = true, defaultValue = SQL_CURRENT_TIMESTAMP)
    Date updatedAt;

    public Chat() {
    }

    public Chat(@NonNull String name, boolean secret) {
        this.name = name;
        this.secret = secret;
        this.type = Type.SINGLE;
    }

    public Chat(@NonNull String name, @NonNull String groupToken, @NonNull String creator) {
        this.name = name;
        this.secret = false;
        this.groupToken = groupToken;
        this.creator = creator;
        this.type = Type.GROUP;
    }

    public Chat(@NonNull String name) {
        this.name = name;
        this.secret = false;
        this.type = Type.CHANNEL;
    }

    public long getId() {
        return id;
    }

    public boolean isSecret() {
        return secret;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public Type getType() {
        return type;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    @Nullable
    public String getGroupToken() {
        return groupToken;
    }

    public String getTarget() {
        switch (getType()) {
            case SINGLE:
            case CHANNEL:
                return getName();
            case GROUP:
                return getGroupToken();
        }
        return null;
    }

    public Message getLastMessage(Context context) {
        List<Message> messages = Message.DAO.query(context,
                COLUMN_CHAT_ID + " = ?",
                new String[]{String.valueOf(id)},
                COLUMN_ID + " DESC LIMIT 1");

        return messages.isEmpty() ? null : messages.get(0);
    }

    public List<Message> getLastMessages(Context context, int numMessages) {
        String chatIdString = String.valueOf(id);
        return Message.DAO.query(context,
                COLUMN_CHAT_ID + " = ?",
                new String[]{chatIdString},
                COLUMN_ID + " ASC LIMIT (SELECT COUNT(*) FROM " + TABLE_MESSAGES
                        + " WHERE " + COLUMN_CHAT_ID + " = " + chatIdString + ") - " + numMessages + ", " + numMessages);
    }

    public void storeAndSendMessage(Context context, EndToEndMessage payload, @NonNull Message message) {
        OutgoingMessage.storeAndEnqueueMessage(context, getTarget(), getType(), payload, message);
    }

    public void storeAndSendMessage(Context context, Message message) {
        EndToEndMessage payload = message.getPayload(getEndToEndParameters(context));
        if (payload != null) {
            storeAndSendMessage(context, payload, message);
        }
    }

    public void sendMessage(Context context, EndToEndMessage payload) {
        OutgoingMessage.enqueueMessage(context, getTarget(), getType(), payload, null, null);
    }

    public int getUnreadMessagesCount(Context context) {
        Cursor cursor = context.getContentResolver().query(
                Message.DAO.getContentUri(),
                new String[]{COLUMN_ID},
                COLUMN_CHAT_ID + " = ? AND " + COLUMN_READ + " = 0",
                new String[]{String.valueOf(getId())},
                null);

        int countMessage = 0;
        if (cursor != null) {
            countMessage = cursor.getCount();
            cursor.close();
        }
        return countMessage;
    }

    public EndToEndMessage.Parameters getEndToEndParameters(Context context) {
        return EndToEndMessage.Parameters.with(context, this);
    }

    public EndToEndMessage getInChatMessage(Context context, boolean entering) {
        switch (getType()) {
            case GROUP:
                return new EndToEndInChatMessage(getEndToEndParameters(context), entering, getGroupToken());
            case CHANNEL:
            case SINGLE:
                return new EndToEndInChatMessage(getEndToEndParameters(context), entering);
        }
        return null;
    }

    private static final int HISTORY_MESSAGE_COUNT = 20;

    private static final DateFormat TIME_FORMAT = SimpleDateFormat.getTimeInstance();
    private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance();

    public static String getHistoryForId(Context context, long chatId) {
        StringBuilder messageHistory = new StringBuilder();
        Chat chat = Chat.DAO.queryById(context, chatId);
        List<Message> lastMessages = chat.getLastMessages(context, HISTORY_MESSAGE_COUNT);
        for (Message message : lastMessages) {
            appendMessageContent(context, messageHistory, message);
        }
        return messageHistory.toString();
    }

    private static void appendMessageContent(Context context, StringBuilder builder, Message message) {
        String dateString = DATE_FORMAT.format(message.getReceivedOn());
        String timeString = TIME_FORMAT.format(message.getReceivedOn());

        builder.append("At ").append(dateString).append(' ').append(timeString).append(' ');
        if (message.isIncoming()) {
            Contact contact = Contact.DAO.queryById(context, message.getContactId());
            builder.append(contact.getUsername()).append(" wrote: ");
        }
        else {
            builder.append(Preferences.getUsername(context)).append(" wrote: ");
        }
        builder.append(message.getDescription(context)).append('\n');
    }

    public static void shareHistoryForId(Context context, long chatId) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, getHistoryForId(context, chatId));
        intent.setType("text/plain");
        context.startActivity(intent);
    }

    public static void removeChatById(Context context, long chatId) {
        context.getContentResolver().delete(
                DAO.getContentUri(),
                COLUMN_ID + "= ?",
                new String[]{String.valueOf(chatId)});
    }

    public static CursorLoader getOrderedCursorLoader(Context context, String selection, String[] selectionArgs) {
        return DAO.getCursorLoader(context,
                selection,
                selectionArgs,
                "(SELECT _id FROM messages WHERE messages.chat_id = chats._id ORDER BY _id DESC LIMIT 1) DESC");
    }

    public static long findOrCreateChatIdByContact(Context context, Contact contact, boolean secret) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        operations.add(Chat.DAO.insertOperation(new Chat(contact.getUsername(), secret)));
        operations.add(ContentProviderOperation.newInsert(DatabaseProvider.CHATS_TO_CONTACTS_URI)
                .withValueBackReference(COLUMN_CHAT_ID, 0)
                .withValue(COLUMN_CONTACT_ID, contact.getId())
                .build());

        try {
            ContentProviderResult[] results = context.getContentResolver().applyBatch(DatabaseProvider.AUTHORITY, operations);
            return ContentUris.parseId(results[0].uri);
        }
        catch (Exception e) {
            Log.d("Chat", "Chat for user " + contact.getUsername() + " already exists");
        }

        Cursor cursor = context.getContentResolver().query(
                DatabaseProvider.CHATS_AND_CONTACTS_URI,
                new String[]{COLUMN_CHAT_ID},
                COLUMN_USERNAME + " = ? AND " + COLUMN_TYPE + " = ?" + (secret ? " AND SECRET" : ""),
                new String[]{contact.getUsername(), Type.SINGLE.name()},
                null);

        long result = -1;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                result = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CHAT_ID));
            }
            cursor.close();
        }

        return result;
    }

    public static Chat findChannel(Context context, String name) {
        List<Chat> chats = Chat.DAO.query(context,
                "TYPE = ? AND NAME = ? AND SECRET = 0",
                new String[]{Chat.Type.CHANNEL.name(), name},
                null);

        if (chats.isEmpty()) {
            return null;
        }
        return chats.get(0);
    }

    public static long createGroupChat(Context context, String groupToken, String name, String creator, List<String> users) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        operations.add(DAO.insertOperation(new Chat(name, groupToken, creator)));
        operations.addAll(userAddOperationList(context, listWithItem(users, creator), null));

        long chatId = -1;

        try {
            ContentProviderResult[] results = context.getContentResolver().applyBatch(DatabaseProvider.AUTHORITY, operations);

            chatId = ContentUris.parseId(results[0].uri);
        }
        catch (Exception e) {
            Log.d(TAG, "Could not create group chat", e);
        }

        return chatId;
    }

    public static void updateGroupChatWithToken(Context context, String groupToken, String name, String creator, List<String> users) {
        List<Chat> chats = DAO.query(context, COLUMN_GROUP_TOKEN + " = ?", new String[]{groupToken}, null);

        if (chats.isEmpty()) {
            Log.d(TAG, "Tried to update a non-existent group chat with token " + groupToken);
            createGroupChat(context, groupToken, name, creator, users);
            return;
        }

        Chat chat = chats.get(0);

        chat.name = name;
        chat.creator = creator;

        DAO.update(context, chat);

        chat.updateUsers(context, listWithItem(users, creator));
    }

    public void updateUsers(Context context, List<String> users) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        operations.add(usersRemoveOperation(getId()));
        operations.addAll(userAddOperationList(context, users, getId()));

        try {
            context.getContentResolver().applyBatch(DatabaseProvider.AUTHORITY, operations);
        }
        catch (Exception e) {
            Log.d(TAG, "Could not update group chat", e);
        }
    }

    public static ContentProviderOperation usersRemoveOperation(long chatId) {
        return ContentProviderOperation.newDelete(DatabaseProvider.CHATS_TO_CONTACTS_URI)
                .withSelection(COLUMN_CHAT_ID + " = ?", new String[]{String.valueOf(chatId)})
                .build();
    }

    public static ArrayList<ContentProviderOperation> userAddOperationList(Context context, List<String> users, Long chatId) {
        final String ownUsername = Preferences.getUsername(context);

        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        Set<String> allUsers = new HashSet<>(users);

        for (String user : allUsers) {
            if (!user.equals(ownUsername)) {
                ContentProviderOperation operation = userAddOperation(context, user, chatId);
                if (operation != null) {
                    operations.add(operation);
                }
            }
        }

        return operations;
    }

    public static ContentProviderOperation userAddOperation(Context context, String username, Long chatId) {
        Contact contact = Contact.findOrCreateContactWithUsername(context, username);

        if (contact == null) {
            return null;
        }

        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(DatabaseProvider.CHATS_TO_CONTACTS_URI);

        if (chatId == null) {
            builder.withValueBackReference(COLUMN_CHAT_ID, 0);
        }
        else {
            builder.withValue(COLUMN_CHAT_ID, chatId);
        }

        return builder.withValue(COLUMN_CONTACT_ID, contact.getId()).build();
    }

    private static <T> List<T> listWithItem(List<T> list, T item) {
        ArrayList<T> l = new ArrayList<>(list);
        l.add(item);
        return l;
    }

    public void showNotificationForMessage(Context context, Contact sender, Message message) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(200);

        if (RuntimeDataHelper.getInstance().isChatOpen(getId())) {
            return;
        }

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(message.getNotificationTitle(context, sender, this))
                .setContentText(message.getNotificationText(context, sender, this))
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS);

        Intent intent = NavigationActivity.intentWithChatId(context, getId());
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(NavigationActivity.class);
        stackBuilder.addNextIntent(intent);

        PendingIntent notificationIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(notificationIntent);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify((int) getId(), builder.build());
    }

    public void sendFile(long deleteIn, String filename, Message.Type fileType, String localUuid) {
        NetworkService.getInstance().asyncSendFile(this, deleteIn, filename, fileType, localUuid);
    }

    public void copyAndSendFile(Uri uri, long deleteIn, String filename, Message.Type fileType, String localUuid) {
        NetworkService.getInstance().asyncCopyAndSendFile(uri, this, deleteIn, filename, fileType, localUuid);
    }
}
