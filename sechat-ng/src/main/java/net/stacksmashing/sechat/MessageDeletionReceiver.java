package net.stacksmashing.sechat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import net.stacksmashing.sechat.db.Message;

public class MessageDeletionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Message.deleteExpiredMessages(context);
        Message.scheduleMessageDeletion(context);
    }
}
