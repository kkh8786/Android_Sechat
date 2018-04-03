package net.stacksmashing.sechat.network;

import android.util.Log;

import org.msgpack.MessagePack;
import org.msgpack.packer.BufferPacker;
import org.msgpack.template.Templates;
import org.msgpack.type.Value;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

final class MsgPackUtil {
    private static final String TAG = "MsgPackUtil";

    public static final MessagePack MESSAGE_PACK = new MessagePack();

    public static Map<String, Value> mapFromBuffer(byte[] buffer) throws IOException {
        return MESSAGE_PACK.createBufferUnpacker(buffer).read(Templates.tMap(Templates.TString, Templates.TValue));
    }

    public static void dumpMessage(Map<String, Value> values) {
        Log.d(TAG, "Unknown message of type " + values.get("MessageType").asRawValue().getString());
        for (String key : values.keySet()) {
            Log.d(TAG, key + " = " + values.get(key).toString());
        }
        Log.d(TAG, "-- End message --");
    }

    public static ServerMessage serverMessageFromBuffer(byte[] buffer) throws IOException {
        Map<String, Value> values = mapFromBuffer(buffer);
        if (values.get("Frame") != null) {
            long messageId = values.get("Id").asIntegerValue().getLong();
            values = mapFromBuffer(values.get("Frame").asRawValue().getByteArray());
            sendAcknowledgement(messageId);
        }
        ServerMessage message = ServerMessage.fromMap(values);
        if (message == null) {
            dumpMessage(values);
        }
        return message;
    }

    private static void sendAcknowledgement(long id) {
        NetworkService.getInstance().asyncSend(new ClientAcknowledgeMessage(id));
        Log.d(TAG, "Sending ack for " + id);
    }

    public static byte[] bytesFromMessage(Packable packable) throws IOException {
        BufferPacker packer = MESSAGE_PACK.createBufferPacker();
        Map<String, Object> values = new HashMap<>();
        packable.pack(values);
        packer.write(values);
        return packer.toByteArray();
    }

    private MsgPackUtil() {
    }
}
