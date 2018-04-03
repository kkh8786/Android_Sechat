package net.stacksmashing.sechat.network;

import android.content.Context;

import net.stacksmashing.sechat.Bus;

import org.msgpack.type.Value;

import java.util.Map;

public class ServerAddContactMessage extends ServerMessage {
    public static final String NAME = "ServerAddContactMessage";

    private final String error;
    private final boolean success;
    private final String username;
    private final String userX;
    private final String userY;

    public ServerAddContactMessage(Map<String, Value> values) {
        this.error = values.get("Error").asRawValue().getString();
        this.success = values.get("Successful").asBooleanValue().getBoolean();
        this.username = values.get("Username").asRawValue().getString();
        this.userX = values.get("UserX").asRawValue().getString();
        this.userY = values.get("UserY").asRawValue().getString();
    }

    @Override
    void performAction(Context context) {
        Bus.bus().post(new Bus.AddContactEvent(this));
    }

    public boolean isSuccessful() {
        return success;
    }

    public String getUsername() {
        return username;
    }

    public String getError() {
        return error;
    }


    public String getUserX() {
        return userX;
    }

    public String getUserY() {
        return userY;
    }
}
