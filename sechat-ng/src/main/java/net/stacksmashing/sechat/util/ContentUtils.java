package net.stacksmashing.sechat.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import net.stacksmashing.sechat.db.Message;

import org.apache.commons.io.IOUtils;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class ContentUtils {
    private static final String TAG = "ContentUtils";

    private ContentUtils() {
    }

    public static boolean saveFromUriToLocalStorage(Context context, Uri uri, String localUuid) {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            outputStream = new FileOutputStream(Message.getFileForUUID(context, localUuid));

            IOUtils.copy(inputStream, outputStream);

            outputStream.flush();
        }
        catch (Exception e) {
            Log.d(TAG, "Could not read file", e);
            return false;
        }
        finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
        }
        return true;
    }
}
