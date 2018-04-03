package net.stacksmashing.sechat.network;

import android.content.Context;

import net.stacksmashing.sechat.Bus;

import org.msgpack.type.Value;

import java.util.Map;

public class ServerRegisterPhoneNumberResponseMessage extends ServerMessage {
    public static final String NAME = "ServerRegisterPhonenumberResponseMessage";

    private final boolean successful;
    private final String error;

    public ServerRegisterPhoneNumberResponseMessage(Map<String, Value> values) {
        successful = values.get("Successful").asBooleanValue().getBoolean();
        error = values.get("Error").asRawValue().getString();
    }

    @Override
    void performAction(Context context) {
        Bus.bus().post(new Bus.PhoneNumberRegistrationResultEvent(this));
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getError() {
        return error;
    }
}
