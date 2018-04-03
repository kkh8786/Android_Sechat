package net.stacksmashing.sechat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by sseitov on 28.01.15.
 */
public class ScreenShotNotificationListener extends NotificationListenerService {

    private final static String TAG = "ScreenshotListener";

    // FIXME: This is an android implementation detail and as such is technically not public.
    private static final int SCREENSHOT_NOTIFICATION_ID = 789;

    public static void enableScreenshotNotificationListener(final Context context) {
        String enabledListeners = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        if (enabledListeners != null && enabledListeners.contains("net.stacksmashing.sechat.ScreenShotNotificationListener")) {
            return;
        }

        new AlertDialog.Builder(context)
                .setMessage(R.string.notification_access_prompt)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                        context.startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "created");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "destroyed");
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn.getId() == SCREENSHOT_NOTIFICATION_ID) {
            Log.i(TAG, "Screenshot Notification Posted");
            Intent receiver = new Intent("ChatFragmentMsg");

            // FIXME: The notification only has actions once the system is done taking the screenshot
            // This is technically a non-public implementation detail, just like the notification ID.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                receiver.putExtra("done", sbn.getNotification().actions != null);
            }

            LocalBroadcastManager.getInstance(this).sendBroadcast(receiver);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG, "onNotificationRemoved " + sbn.toString());
    }
}
