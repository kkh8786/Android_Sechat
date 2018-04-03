package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.voice.CallHandler;

import org.msgpack.type.Value;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Created by zaba on 03/01/15.
 */
public class ServerCallGetStatusResponseMessage extends ServerMessage {
    private static final String TAG = "CallGetStatusResponse";

    public static final String NAME = "ServerCallGetStatusResponseMessage";

    private static final int CALL_STATUS_ACTIVE = 0;
    private static final int CALL_STATUS_EXPIRED = 1;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);

    private final int status;
    private final String callToken;
    private final Date createdAt;

    public ServerCallGetStatusResponseMessage(Map<String, Value> values) {
        this.status = values.get("Status").asIntegerValue().getInt();
        this.callToken = values.get("CallToken").asRawValue().getString();
        Date createdAt = new Date();
        try {
            createdAt = DATE_FORMAT.parse(values.get("CreatedAt").asRawValue().getString());
        }
        catch (ParseException e) {
            Log.d(TAG, "Could not parse CreatedAt", e);
        }
        this.createdAt = createdAt;
    }

    @Override
    void performAction(Context context) {
        if (status == CALL_STATUS_ACTIVE) {
            CallHandler.INSTANCE.incomingCallIsActive(context, callToken, createdAt);
        }
        else if (status == CALL_STATUS_EXPIRED) {
            CallHandler.INSTANCE.incomingCallIsExpired(context, callToken, createdAt);
        }
        else {
            Log.d(TAG, "Unknown call status: " + status);
        }
    }
}
