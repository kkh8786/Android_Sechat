package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.db.Chat;

import org.msgpack.type.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ServerChannelInfoMessage extends ServerMessage {
    public static final String NAME = "ServerChannelInfoMessage";

    private final List<String> users;
    private final String userLeft;
    private final String userJoined;
    private final String channel;

    public ServerChannelInfoMessage(Map<String, Value> values) {
        channel = values.get("Channel").asRawValue().getString();
        userJoined = values.get("Joined").asRawValue().getString();
        userLeft = values.get("Left").asRawValue().getString();
        List<String> users = new ArrayList<>();
        for (Value value : values.get("Users").asArrayValue()) {
            users.add(value.asRawValue().getString());
        }
        this.users = Collections.unmodifiableList(users);
    }

    @Override
    void performAction(Context context) {
        Chat chat = Chat.findChannel(context, channel);

        if (chat == null) {
            Log.d("ChannelInfoMessage", "Received info for channel \"" + channel + "\" that we are not on");
            return;
        }

        chat.updateUsers(context, users);
    }
}
