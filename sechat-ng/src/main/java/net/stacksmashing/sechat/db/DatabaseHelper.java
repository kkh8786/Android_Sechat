package net.stacksmashing.sechat.db;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "data.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_CONTACTS = "contacts";
    public static final String TABLE_CHATS = "chats";
    public static final String TABLE_CHATS_TO_CONTACTS = "chats_to_contacts";
    public static final String TABLE_MESSAGES = "messages";
    public static final String TABLE_DOWNLOADS = "downloads";
    public static final String TABLE_OUTGOING_MESSAGES = "outgoing_messages";
    public static final String TABLE_RECENT_CALLS = "recent_calls";
    public static final String TABLE_OUTGOING_FILES = "outgoing_files";

    public static final String VIEW_CHATS_AND_CONTACTS = "chats_and_contacts";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_GROUP_TOKEN = "group_token";
    public static final String COLUMN_CHAT_ID = "chat_id";
    public static final String COLUMN_CONTACT_ID = "contact_id";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_UUID = "uuid";
    public static final String COLUMN_LOCAL_UUID = "local_uuid";
    public static final String COLUMN_EC_X = "ec_x";
    public static final String COLUMN_EC_Y = "ec_y";
    public static final String COLUMN_VERIFICATION_LEVEL = "verification_level";
    public static final String COLUMN_LAST_SEEN = "last_seen";
    public static final String COLUMN_ADDED_AT = "added_at";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_ALIAS = "alias";
    public static final String COLUMN_READ = "read";
    public static final String COLUMN_DOWNLOAD_ID = "download_id";
    public static final String COLUMN_DELETE_AT = "delete_at";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_UPDATED_AT = "updated_at";
    public static final String COLUMN_LAST_RECEIVED_PART = "last_received_part";
    public static final String COLUMN_CONTENT_ATTRIBUTES = "content_attributes";
    public static final String COLUMN_SECRET = "secret";

    public static final String SQL_CURRENT_TIMESTAMP = "(1000 * STRFTIME('%s', 'now'))";

    private static final DAO[] DAOS = new DAO[]{
            Contact.DAO,
            Chat.DAO,
            Message.DAO,
            Download.DAO,
            OutgoingMessage.DAO,
            RecentCall.DAO,
            OutgoingFile.DAO
    };

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_key = 1");
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        for (DAO dao : DAOS) {
            database.execSQL(dao.getTableCreationSql());
        }

        database.execSQL("CREATE TRIGGER " + TABLE_CONTACTS + "_on_update AFTER UPDATE ON " + TABLE_CONTACTS
                + " BEGIN UPDATE " + TABLE_CONTACTS + " SET " + COLUMN_UPDATED_AT + " = datetime('now'); END");

        database.execSQL("CREATE TABLE " + TABLE_CHATS_TO_CONTACTS + " ("
                        + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COLUMN_CHAT_ID + " INTEGER NOT NULL REFERENCES " + TABLE_CHATS + "(" + COLUMN_ID + "), "
                        + COLUMN_CONTACT_ID + " INTEGER REFERENCES " + TABLE_CONTACTS + "(" + COLUMN_ID + "), "
                        + "UNIQUE (" + COLUMN_CHAT_ID + ", " + COLUMN_CONTACT_ID + "))"
        );

        database.execSQL("CREATE INDEX messages_chat_id ON " + TABLE_MESSAGES + "(" + COLUMN_CHAT_ID + ")");

        /* Join of contacts and chats using contacts_to_chats.  (chat_id, contacts.username as username, type, secret). */
        database.execSQL("CREATE VIEW " + VIEW_CHATS_AND_CONTACTS + " AS SELECT "
                + COLUMN_CHAT_ID + ", " + TABLE_CONTACTS + "." + COLUMN_USERNAME + " AS " + COLUMN_USERNAME + ", " + COLUMN_TYPE + ", " + COLUMN_SECRET + " FROM "
                + TABLE_CONTACTS + ", " + TABLE_CHATS_TO_CONTACTS + ", " + TABLE_CHATS + " "
                + " ON " + COLUMN_CONTACT_ID + " = " + TABLE_CONTACTS + "." + COLUMN_ID + " AND "
                + COLUMN_CHAT_ID + " = " + TABLE_CHATS + "." + COLUMN_ID);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        for (DAO<?> dao : DAOS) {
            if (dao.shouldCreateTable(oldVersion, newVersion)) {
                database.execSQL(dao.getTableCreationSql());
            }
            for (String query : dao.getTableUpdateSql(oldVersion, newVersion)) {
                database.execSQL(query);
            }
        }
    }
}
