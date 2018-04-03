package net.stacksmashing.sechat.network;

import java.util.Map;

public class ClientRegisterMessage extends ClientMessage {
    private final String username, userX, userY;
    private final String deviceName, deviceX, deviceY;

    public ClientRegisterMessage(String username, String userX, String userY, String deviceName, String deviceX, String deviceY) {
        super("ClientRegisterMessage");
        this.username = username;
        this.userX = userX;
        this.userY = userY;
        this.deviceName = deviceName;
        this.deviceX = deviceX;
        this.deviceY = deviceY;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("Username", username);
        values.put("UserX", userX);
        values.put("UserY", userY);
        values.put("Devicename", deviceName);
        values.put("DeviceX", deviceX);
        values.put("DeviceY", deviceY);
    }
}
