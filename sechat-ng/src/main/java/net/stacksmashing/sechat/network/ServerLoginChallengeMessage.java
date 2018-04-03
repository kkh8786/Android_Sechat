package net.stacksmashing.sechat.network;

import android.content.Context;
import android.util.Log;

import net.stacksmashing.sechat.Preferences;
import net.stacksmashing.sechat.db.Contact;

import org.msgpack.type.Value;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1Sequence;

import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.List;
import java.util.Map;

public class ServerLoginChallengeMessage extends ServerRegisterMessage {
    private static final String TAG = "LoginChallengeMessage";

    public static final String NAME = "ServerLoginChallengeMessage";

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final boolean success;
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final String error;
    private final String challenge;

    public ServerLoginChallengeMessage(Map<String, Value> values) {
        super(values);
        success = values.get("Successful").asBooleanValue().getBoolean();
        error = values.get("Error").asRawValue().getString();
        challenge = values.get("Challenge").asRawValue().getString();
    }

    @Override
    void performAction(Context context) {
        if (!Preferences.isRegistered(context)) {
            return;
        }

        KeyPair keyPair = Preferences.getKeyPair(context);

        if (keyPair == null) {
            Log.d(TAG, "No key pair");
            return;
        }

        try {
            Signature signature = Signature.getInstance("NONEwithECDSA");
            signature.initSign(keyPair.getPrivate(), new SecureRandom());
            signature.update(challenge.getBytes());
            byte[] signatureBytes = signature.sign();
            ASN1Sequence sequence = ASN1Sequence.getInstance(signatureBytes);
            ASN1Integer r = (ASN1Integer) sequence.getObjectAt(0).toASN1Primitive();
            ASN1Integer s = (ASN1Integer) sequence.getObjectAt(1).toASN1Primitive();

            List<String> subscribeTo = Contact.getUsernames(context);
            boolean invisible = Preferences.isInvisible(context);

            NetworkService.getInstance().asyncSend(new ClientLoginResponseMessage(r.getPositiveValue().toString(10), s.getPositiveValue().toString(10), subscribeTo, invisible));
            NetworkService.getInstance().asyncSend(new ClientSubscribeStatusMessage(subscribeTo));
            NetworkService.getInstance().setState(NetworkService.State.LOGGED_IN);
        }
        catch (Exception e) {
            Log.d(TAG, "Could not sign challenge", e);
        }
    }
}
