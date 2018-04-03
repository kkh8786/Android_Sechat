package net.stacksmashing.sechat.db;

import android.content.ContentUris;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.PicassoTools;

import net.stacksmashing.sechat.Preferences;
import net.stacksmashing.sechat.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_ALIAS;
import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_CHAT_ID;
import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_CONTACT_ID;
import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_ID;
import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_USERNAME;
import static net.stacksmashing.sechat.db.DatabaseHelper.SQL_CURRENT_TIMESTAMP;
import static net.stacksmashing.sechat.db.DatabaseHelper.TABLE_CHATS_TO_CONTACTS;
import static net.stacksmashing.sechat.db.DatabaseHelper.TABLE_CONTACTS;

@DAO.Table(name = TABLE_CONTACTS)
public class Contact extends Entity implements Serializable {
    private static final String TAG = "Contact";
    public static final DAO<Contact> DAO = new DAO<>(Contact.class);

    public enum Status {
        OFFLINE(1),
        CONNECTED(2),
        ONLINE(3);

        private final int ordinal;

        Status(int ordinal) {
            this.ordinal = ordinal;
        }

        private static final SparseArray<Status> STATUS_MAP = new SparseArray<>();

        static {
            for (Status status : values()) {
                STATUS_MAP.put(status.ordinal, status);
            }
        }

        public static Status fromInteger(int i) {
            return STATUS_MAP.get(i);
        }

        @ColorRes
        public int getColor() {
            if (this == ONLINE) {
                return R.color.sechat_green;
            }
            return R.color.sechat_gray;
        }
    }

    @DAO.Column(name = COLUMN_ID, primaryKey = true)
    long id;

    @DAO.Column(notNull = true, unique = true)
    @NonNull
    String username;

    @DAO.Column
    byte[] keyData;

    @DAO.Column(defaultValue = "0")
    int verificationLevel;

    @DAO.Column
    String status;

    @DAO.Column
    String alias;

    @DAO.Column
    Date lastSeen;

    @DAO.Column
    Date addedAt;

    @DAO.Column(defaultValue = "0")
    boolean blocked;

    @DAO.Column(notNull = true, defaultValue = SQL_CURRENT_TIMESTAMP)
    Date createdAt;

    @DAO.Column(notNull = true, defaultValue = SQL_CURRENT_TIMESTAMP)
    Date updatedAt;

    public Contact() {
    }

    public Contact(@NonNull String username) {
        this.username = username;
    }

    public Contact(@NonNull String username, byte[] keyData) {
        this.username = username;
        this.keyData = keyData;
    }

    public long getId() {
        return id;
    }

    public
    @NonNull
    String getUsername() {
        return username;
    }

    public byte[] getKeyData() {
        return keyData;
    }

    public Date getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getStatus() {
        return this.status;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isBlocked() {
        return this.blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean isVerified() {
        return verificationLevel > 0;
    }

    public void setVerified(boolean verified) {
        verificationLevel = verified ? 1 : 0;
    }

    public
    @StringRes
    int getVerificationLevelString() {
        return verificationLevel > 0 ? R.string.verification_level_verified : R.string.verification_level_not_verified;
    }

    public void setProfilePicture(Context context, byte[] profilePicture) {
        if (profilePicture.length == 0) {
            return;
        }

        try {
            // TODO This should write into a separate folder or so.
            FileOutputStream outputStream = new FileOutputStream(getProfilePicturePath(context), false);
            outputStream.write(profilePicture);
            outputStream.close();

            PicassoTools.clearCache(Picasso.with(context));

            context.getContentResolver().notifyChange(ContentUris.withAppendedId(DAO.getContentUri(), id), null);
        }
        catch (Exception e) {
            Log.d(TAG, "Failed to write profile picture: ", e);
        }
    }

    private File getProfilePicturePath(Context context) {
        return new File(context.getFilesDir(), username + "_profile_picture.jpg");
    }

    public void loadProfilePictureInto(Context context, ImageView view) {
        final File file = getProfilePicturePath(context);
        if (file.exists()) {
            Picasso.with(context).load(file).into(view);
        }
        else {
            view.setImageDrawable(Preferences.getProfilePlaceholderDrawable(context, username));
        }
    }

    public boolean isValidQrData(QrData data) {
        // TODO: Proper key data; see also AddContactActivity#handleAddContactEvent.
        return username.equals(data.getUsername()) && Arrays.equals(keyData, (data.getX() + " " + data.getY()).getBytes());
    }

    private static Contact createContactWithUsername(Context context, String username) {
        if (username.isEmpty()) {
            Log.e(TAG, "Empty username in createContactWithUsername", new Throwable());
            return null;
        }

        long newContactId;
        try {
            newContactId = DAO.insert(context, new Contact(username));
        }
        catch (Exception e) {
            return null;
        }
        return DAO.queryById(context, newContactId);
    }

    public static Contact findContactByUsername(Context context, String username) {
        List<Contact> contactList = DAO.query(context, COLUMN_USERNAME + " = ?", new String[]{username}, null);

        if (contactList.isEmpty()) {
            Log.d("Contact", "No contact found.");
            return null;
        }

        return contactList.get(0);
    }

    public static List<String> getUsernames(Context context) {
        List<String> result = new ArrayList<>();

        Cursor cursor = context.getContentResolver().query(
                DAO.getContentUri(),
                new String[]{COLUMN_USERNAME},
                null,
                null,
                null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    result.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)));
                }
                while (cursor.moveToNext());
            }
            cursor.close();
        }

        return result;
    }

    @Nullable
    public static Contact findOrCreateContactWithUsername(Context context, String username) {
        /* Trying to create the contact first so that if another thread is trying to
         * findOrCreate at the same time one insert will succeed, the other will fail and then
         * just get the contact that was created by the other thread.
         */
        Contact contact = createContactWithUsername(context, username);
        if (contact == null) {
            contact = findContactByUsername(context, username);
        }
        return contact;
    }

    public static Loader<Cursor> getOrderedCursorLoader(Context context, String selection, String[] selectionArgs) {
        return DAO.getCursorLoader(context, selection, selectionArgs, COLUMN_ALIAS + ", " + COLUMN_USERNAME + " COLLATE NOCASE ASC");
    }

    public static Loader<Cursor> getOrderedCursorLoaderForChatId(Context context, long chatId) {
        return DAO.getCursorLoader(context,
                "EXISTS (SELECT 1 FROM " + TABLE_CHATS_TO_CONTACTS
                        + " WHERE " + COLUMN_CHAT_ID + " = ? AND "
                        + COLUMN_CONTACT_ID + " = " + TABLE_CONTACTS + "." + COLUMN_ID + ")",
                new String[]{String.valueOf(chatId)},
                COLUMN_USERNAME + " COLLATE NOCASE ASC");
    }

    public static class QrData {
        String username, x, y;

        private QrData(String username, String x, String y) {
            this.username = username;
            this.x = x;
            this.y = y;
        }

        public static QrData fromJSON(String json) {
            String username, x, y;
            try {
                JSONObject contactData = new JSONObject(json);
                username = contactData.getString("username");
                x = contactData.getString("X");
                y = contactData.getString("Y");
            }
            catch (JSONException e) {
                Log.d(TAG, "Could not decode QR code contents", e);
                return null;
            }
            return new QrData(username, x, y);
        }

        public String getX() {
            return x;
        }

        public String getY() {
            return y;
        }

        public String getUsername() {
            return username;
        }
    }
}
