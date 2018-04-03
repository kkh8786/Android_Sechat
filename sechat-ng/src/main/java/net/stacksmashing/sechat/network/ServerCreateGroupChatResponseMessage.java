package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.Bus;
import net.stacksmashing.sechat.Preferences;
import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.util.RuntimeDataHelper;

import org.msgpack.type.Value;

import java.util.List;
import java.util.Map;

public class ServerCreateGroupChatResponseMessage extends ServerMessage {
    private static final String TAG = "CreateGroupChatResponse";

    public static final String NAME = "ServerCreateGroupChatResponseMessage";

    private final String error;
    private final boolean success;
    private final String name;
    private final String token;

    public ServerCreateGroupChatResponseMessage(Map<String, Value> values) {
        this.error = values.get("Error").asRawValue().getString();
        this.success = values.get("Successful").asBooleanValue().getBoolean();
        this.name = values.get("Name").asRawValue().getString();
        this.token = values.get("Identifier").asRawValue().getString();
    }

    @Override
    void performAction(Context context) {
        if (!success) {
            Log.d(TAG, "Failed to create a group chat: " + error);
            return;
        }

        List<String> users = RuntimeDataHelper.getInstance().getGroupChatUsers(name);
        if (users == null) {
            Log.d(TAG, "No users stored for " + name);
            return;
        }

        Log.d(TAG, "Created group chat " + name + " with token " + token);

        long chatId = Chat.createGroupChat(context, token, name, Preferences.getUsername(context), users);

        RuntimeDataHelper.getInstance().removeGroupChatUsers(name);

        Bus.bus().post(new Bus.GroupChatCreatedEvent(chatId));
    }
}
