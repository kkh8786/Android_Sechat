package net.stacksmashing.sechat.network;

import java.util.Map;

public class ClientVerifyPhoneNumberMessage extends ClientMessage {
    private final String code;

    public ClientVerifyPhoneNumberMessage(String code) {
        super("ClientVerifyPhonenumberMessage");
        this.code = code;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("VerificationCode", code);
    }
}
