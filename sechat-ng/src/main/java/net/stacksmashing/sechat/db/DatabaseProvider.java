package net.stacksmashing.sechat.db;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.stacksmashing.sechat.db.DatabaseHelper.TABLE_CHATS_TO_CONTACTS;
import static net.stacksmashing.sechat.db.DatabaseHelper.VIEW_CHATS_AND_CONTACTS;

public class DatabaseProvider extends ContentProvider {
    private static final String TAG = "DatabaseProvider";

    public static final String AUTHORITY = "net.stacksmashing.sechat.provider";

    public static final Uri CHATS_TO_CONTACTS_URI = tableUri(TABLE_CHATS_TO_CONTACTS);
    public static final Uri CHATS_AND_CONTACTS_URI = tableUri(VIEW_CHATS_AND_CONTACTS);

    public static Uri tableUri(String tableName) {
        return Uri.parse("content://" + AUTHORITY + "/" + tableName);
    }

    /* A pseudo-table used to pass the password into the content provider. */
    private static final Uri LOGIN_URI = tableUri("__login__");

    public static boolean login(Context context, String password) {
        ContentValues values = new ContentValues();
        values.put("password", password);
        Uri result = context.getContentResolver().insert(LOGIN_URI, values);
        return result != null && result.getLastPathSegment().equals("true");
    }

    public static boolean isLoggedIn(Context context) {
        return null != context.getContentResolver().query(LOGIN_URI, null, null, null, null);
    }

    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    @Override
    public boolean onCreate() {
        SQLiteDatabase.loadLibs(getContext());
        dbHelper = new DatabaseHelper(getContext().getApplicationContext());
        return true;
    }

    private synchronized boolean openDatabase(String password) {
        if (database != null) {
            return true;
        }

        try {
            database = dbHelper.getWritableDatabase(password);
            return true;
        }
        catch (Exception e) {
            Log.d(TAG, "Failed to open database", e);
            return false;
        }
    }

    private synchronized SQLiteDatabase getDatabase() {
        if (database == null) {
            throw new IllegalStateException("Database is not open");
        }
        return database;
    }

    private static String getTableName(Uri uri) {
        return uri.getPathSegments().get(0);
    }

    private static String addIdToSelection(Uri uri, String selection) {
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() == 2) {
            return (TextUtils.isEmpty(selection) ? "" : "(" + selection + ") AND ") + "_ID = ?";
        }
        return selection;
    }

    private static String[] addIdToSelectionArgs(Uri uri, String[] selectionArgs) {
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() == 2) {
            if (selectionArgs == null) {
                return new String[]{pathSegments.get(1)};
            }
            List<String> args = Arrays.asList(selectionArgs);
            args.add(pathSegments.get(1));
            return (String[]) args.toArray();
        }
        return selectionArgs;
    }

    private static void checkThread(String description) {
//        final String threadName = Thread.currentThread().getName();
//        Log.d(TAG, description + " " + threadName);
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] columns, String selection, String[] selectionArgs, String sort) {
        checkThread("query " + uri.toString() + " (" + selection + ")");

        if (uri.equals(LOGIN_URI)) {
            synchronized (this) {
                return database == null ? null : new MatrixCursor(new String[]{""});
            }
        }

        selection = addIdToSelection(uri, selection);
        selectionArgs = addIdToSelectionArgs(uri, selectionArgs);
        Cursor cursor = getDatabase().query(getTableName(uri), columns, selection, selectionArgs, null, null, sort);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        checkThread("insert " + uri.toString());

        if (uri.equals(LOGIN_URI)) {
            return Uri.withAppendedPath(uri, openDatabase(contentValues.getAsString("password")) ? "true" : "false");
        }

        long id = getDatabase().insertOrThrow(getTableName(uri), null, contentValues);
        Uri result = Uri.withAppendedPath(uri, Long.toString(id));
        getContext().getContentResolver().notifyChange(result, null);
        return result;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        checkThread("delete " + uri.toString() + " (" + selection + ")");

        selection = addIdToSelection(uri, selection);
        selectionArgs = addIdToSelectionArgs(uri, selectionArgs);
        int count = getDatabase().delete(getTableName(uri), selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        checkThread("update " + uri.toString() + " (" + selection + ")");

        if (contentValues == null || contentValues.size() == 0) {
            return 0;
        }

        selection = addIdToSelection(uri, selection);
        selectionArgs = addIdToSelectionArgs(uri, selectionArgs);
        int count = getDatabase().update(getTableName(uri), contentValues, selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        checkThread("applyBatch");

        SQLiteDatabase db = getDatabase();
        db.beginTransaction();
        ContentProviderResult[] results;
        try {
            results = super.applyBatch(operations);
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
        return results;
    }
}
