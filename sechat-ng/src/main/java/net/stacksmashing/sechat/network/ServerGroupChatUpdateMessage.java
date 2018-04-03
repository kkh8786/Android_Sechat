package net.stacksmashing.sechat.network;

import android.content.Context;

import net.stacksmashing.sechat.db.Chat;

import org.msgpack.type.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServerGroupChatUpdateMessage extends ServerMessage {
    public static final String NAME = "ServerGroupChatUpdateMessage";

    private final String creator;
    private final List<String> users;
    private final String name;
    private final String groupToken;

    // FIXME Deduplicate this and ServerGroupChatInvitationMessage
    public ServerGroupChatUpdateMessage(Map<String, Value> values) {
        creator = values.get("Creator").asRawValue().getString();
        this.users = new ArrayList<>();
        for (Value user : values.get("Users").asArrayValue()) {
            this.users.add(user.asRawValue().getString());
        }
        name = values.get("Name").asRawValue().getString();
        groupToken = values.get("Identifier").asRawValue().getString();
    }

    @Override
    void performAction(Context context) {
        Chat.updateGroupChatWithToken(context, groupToken, name, creator, users);
    }
}
