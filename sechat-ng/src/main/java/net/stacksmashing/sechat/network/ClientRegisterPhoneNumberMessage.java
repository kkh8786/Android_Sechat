package net.stacksmashing.sechat.network;

import java.util.Map;

public class ClientRegisterPhoneNumberMessage extends ClientMessage {
    private final String phoneNumber;

    public ClientRegisterPhoneNumberMessage(String phoneNumber) {
        super("ClientRegisterPhonenumberMessage");
        this.phoneNumber = phoneNumber;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("Phonenumber", phoneNumber);
    }
}
