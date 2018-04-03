package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.db.Chat;

import org.msgpack.type.Value;

import java.util.Map;

public class ServerJoinChannelResponseMessage extends ServerMessage {
    public static final String NAME = "ServerJoinChannelResponseMessage";

    private boolean success;
    private String channel;

    public ServerJoinChannelResponseMessage(Map<String, Value> values) {
        success = values.get("Successful").asBooleanValue().getBoolean();
        channel = values.get("Channel").asRawValue().getString();
    }

    @Override
    void performAction(Context context) {
        if (success) {
            Log.d("JoinResponseMessage", "Joined channel \"" + channel + "\"");

            /* We won't insert a duplicate channel because of the unique constraint in the database. */
            Chat.DAO.insert(context, new Chat(channel));
        }
        else {
            Log.d("JoinResponseMessage", "Unable to join channel \"" + channel + "\"");
        }
    }
}
