package net.stacksmashing.sechat.network;

import android.content.Context;

import net.stacksmashing.sechat.Bus;

import org.msgpack.type.Value;

import java.util.Map;

public class ServerVerifyPhoneNumberResponseMessage extends ServerMessage {
    public static final String NAME = "ServerVerifyPhonenumberResponseMessage";

    private final boolean successful;

    public ServerVerifyPhoneNumberResponseMessage(Map<String, Value> values) {
        successful = values.get("Successful").asBooleanValue().getBoolean();
    }

    @Override
    void performAction(Context context) {
        Bus.bus().post(new Bus.PhoneNumberVerificationResultEvent(this));
    }

    public boolean isSuccessful() {
        return successful;
    }
}
