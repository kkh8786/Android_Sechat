package net.stacksmashing.sechat.network;

import java.util.List;
import java.util.Map;

public class ClientSetProfilePictureMessage extends ClientMessage {
    private final byte[] picture;
    private final List<String> users;

    public ClientSetProfilePictureMessage(byte[] picture, List<String> users) {
        super("ClientSetProfilepictureMessage");
        this.picture = picture;
        this.users = users;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("ProfilePicture", picture);
        values.put("NotifyUsers", users);
    }
}
