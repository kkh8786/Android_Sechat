package net.stacksmashing.sechat.network;

import java.util.Map;

public class ClientLoginInitialMessage extends ClientMessage {
    private final String username;
    private final String deviceName;

    public ClientLoginInitialMessage(String username, String deviceName) {
        super("ClientLoginInitialMessage");
        this.username = username;
        this.deviceName = deviceName;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("Username", username);
        values.put("Devicename", deviceName);
    }
}
