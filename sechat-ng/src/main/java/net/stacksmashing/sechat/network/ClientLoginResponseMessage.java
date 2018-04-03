package net.stacksmashing.sechat.network;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClientLoginResponseMessage extends ClientMessage {
    private static final String TAG = "LoginResponseMessage";

    private final String r, s;
    private final List<String> subscribeTo;
    private final boolean invisible;

    public ClientLoginResponseMessage(String r, String s, List<String> subscribeTo, boolean invisible) {
        super("ClientLoginResponseMessage");
        this.r = r;
        this.s = s;
        this.subscribeTo = new ArrayList<>(subscribeTo);
        this.invisible = invisible;
    }

    @Override
    public void pack(Map<String, Object> values) {
        super.pack(values);
        values.put("R", r);
        values.put("S", s);
        values.put("SubscribeTo", subscribeTo);
        values.put("Invisible", invisible);
        Log.d(TAG, "Subscribing to: " + subscribeTo);
    }
}
