package net.stacksmashing.sechat.network;

import java.util.List;
import java.util.Map;

public class ClientCreateGroupChatMessage extends ClientMessage {
    private final String name;
    private final List<String> users;

    public ClientCreateGroupChatMessage(String name, List<String> users) {
        super("ClientCreateGroupChatMessage");
        this.name = name;
        this.users = users;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("Name", name);
        values.put("Users", users);
    }
}
