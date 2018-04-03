package net.stacksmashing.sechat.network;

import android.content.Context;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.video.StreamManager;

import org.msgpack.type.Value;

import java.util.Map;

public class EndToEndCallVideoInfoMessage extends EndToEndMessage {
    public static final String NAME = "CallVideoInfoMessage";

    private final byte[] sps;
    private final byte[] pps;
    private final int width, height;

    @SuppressWarnings("unused")
    public EndToEndCallVideoInfoMessage(Map<String, Value> values) {
        super(values);
        this.sps = values.get("sps").asRawValue().getByteArray();
        this.pps = values.get("pps").asRawValue().getByteArray();
        this.width = values.get("width").asIntegerValue().getInt();
        this.height = values.get("height").asIntegerValue().getInt();
    }

    public EndToEndCallVideoInfoMessage(Parameters parameters, byte[] sps, byte[] pps, int width, int height) {
        super(parameters, NAME);
        this.sps = sps;
        this.pps = pps;
        this.width = width;
        this.height = height;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("sps", sps);
        values.put("pps", pps);
        values.put("width", width);
        values.put("height", height);
    }

    @Override
    void performAction(Context context, Contact contact, Chat chat) {
        StreamManager.INSTANCE.addStream(contact.getUsername(), new StreamManager.StreamInfo(sps, pps, width, height));
    }
}
