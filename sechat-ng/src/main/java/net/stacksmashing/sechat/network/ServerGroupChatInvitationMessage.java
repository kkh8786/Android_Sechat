package net.stacksmashing.sechat.network;

import android.content.Context;

import net.stacksmashing.sechat.db.Chat;

import org.msgpack.type.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServerGroupChatInvitationMessage extends ServerMessage {
    public static final String NAME = "ServerGroupChatInvitationMessage";

    private final String creator;
    private final List<String> users;
    private final String name;
    private final String groupToken;

    public ServerGroupChatInvitationMessage(Map<String, Value> values) {
        creator = values.get("Creator").asRawValue().getString();
        Value[] users = values.get("Users").asArrayValue().getElementArray();
        this.users = new ArrayList<>();
        for (Value user : users) {
            this.users.add(user.asRawValue().getString());
        }
        name = values.get("Name").asRawValue().getString();
        groupToken = values.get("Identifier").asRawValue().getString();
    }

    @Override
    void performAction(Context context) {
        Chat.createGroupChat(context, groupToken, name, creator, users);
    }
}
