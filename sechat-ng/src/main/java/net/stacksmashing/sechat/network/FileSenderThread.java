package net.stacksmashing.sechat.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Message;
import net.stacksmashing.sechat.db.OutgoingFile;
import net.stacksmashing.sechat.util.ContentUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;

final class FileSenderThread extends HandlerThread {
    private static final String TAG = "FileSenderThread";

    private static final int CHUNK_SIZE = 16 * 1024;

    private final Handler handler;
    private final Context context;

    private final Runnable sendNextPartRunnable = new Runnable() {
        @Override
        public void run() {
            OutgoingFile outgoingFile = OutgoingFile.findNextFile(context);

            if (outgoingFile == null) {
                Log.d(TAG, "[F] No more files to send");
                return;
            }

            byte[] chunk = outgoingFile.readNextChunk(CHUNK_SIZE);

            if (chunk != null && chunk.length != 0) {
                Log.d(TAG, "[F] Sending next chunk of " + outgoingFile);
                outgoingFile.sendNextChunk(context, chunk);
            }
            else {
                Log.d(TAG, "[F] " + outgoingFile + " has ended");
                OutgoingFile.DAO.delete(context, outgoingFile);
            }

            sendNextPart(25);
        }
    };

    public FileSenderThread(Context context) {
        super(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        this.context = context.getApplicationContext();
        start();
        this.handler = new Handler(getLooper());
    }

    public void sendNextPart() {
        sendNextPart(0);
    }

    private void sendNextPart(long delay) {
        handler.removeCallbacks(sendNextPartRunnable);
        handler.postDelayed(sendNextPartRunnable, delay);
    }

    public void copyToLocalStorageAndSend(final Uri uri, final Chat chat, final long deleteIn, final String filename, final Message.Type type, final String localUuid) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!ContentUtils.saveFromUriToLocalStorage(context, uri, localUuid)) {
                    return;
                }

                send(chat, deleteIn, filename, type, localUuid);
            }
        });
    }

    private static int getNumChunks(Context context, String uuid) {
        return 1 + (int) (FileUtils.sizeOf(Message.getFileForUUID(context, uuid)) / CHUNK_SIZE);
    }

    public void send(final Chat chat, final long deleteIn, final String filename, final Message.Type type, final String localUuid) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (type == Message.Type.IMAGE) {
                    compressImage(context, localUuid);
                }

                OutgoingFile.startSendingFile(context, chat, deleteIn, filename, type, localUuid, getNumChunks(context, localUuid));
            }
        });
    }

    private static void compressImage(Context context, String localUuid) {
        FileOutputStream outputStream = null;
        Bitmap bitmap = null;
        try {
            File file = Message.getFileForUUID(context, localUuid);
            bitmap = BitmapFactory.decodeFile(file.getPath());
            outputStream = new FileOutputStream(file, false);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
        }
        catch (Exception e) {
            Log.e(TAG, "Failed to compress image before sending", e);
        }
        finally {
            IOUtils.closeQuietly(outputStream);
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }
}
