package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.Bus;
import net.stacksmashing.sechat.Preferences;

import org.msgpack.type.Value;

import java.util.Map;

public class ServerRegisterMessage extends ServerMessage {
    public static final String NAME = "ServerRegisterMessage";

    private final boolean success;
    private final String error;

    public ServerRegisterMessage(Map<String, Value> values) {
        success = values.get("Successful").asBooleanValue().getBoolean();
        error = values.get("Error").asRawValue().getString();
    }

    @Override
    void performAction(Context context) {
        if (Preferences.isRegistered(context)) {
            return;
        }

        if (success) {
            Log.d(NAME, "Registered successfully");
            Preferences.register();
        }
        else {
            Log.d(NAME, "Registration error: " + error);
        }

        Bus.bus().post(new Bus.RegistrationResultEvent(this));
    }

    public boolean isSuccessful() {
        return success;
    }

    public String getError() {
        return error;
    }
}
