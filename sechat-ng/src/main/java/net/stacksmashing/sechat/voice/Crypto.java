package net.stacksmashing.sechat.voice;

import java.security.Key;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {
    public static final byte[] KEY = new byte[]{-18, 110, -26, -64, 95, 68, 71, -108, 32, 54, 36, -78, -125, -54, -95, -17};

    private final Cipher encryptor, decryptor;
    private final SecureRandom rng;
    private final Key key;

    public Crypto(byte[] key) {
        try {
            encryptor = Cipher.getInstance("AES/CTR/NoPadding");
            decryptor = Cipher.getInstance("AES/CTR/NoPadding");
        }
        catch (Exception e) {
            throw new IllegalStateException("Failed to initialize ciphers");
        }
        rng = new SecureRandom();
        this.key = new SecretKeySpec(key, "AES");
    }

    public byte[] encryptBytes(byte[] data, int size) {
        byte[] iv = new byte[16];
        byte[] result = new byte[size + 16];
        rng.nextBytes(iv);
        System.arraycopy(iv, 0, result, 0, iv.length);
        IvParameterSpec param = new IvParameterSpec(iv, 0, 16);
        try {
            encryptor.init(Cipher.ENCRYPT_MODE, key, param);
            encryptor.doFinal(data, 0, size, result, 16);
            return result;
        }
        catch (Exception e) {
            return null;
        }
    }

    public byte[] decryptBytes(byte[] dataWithIv, int size) {
        IvParameterSpec param = new IvParameterSpec(dataWithIv, 0, 16);
        try {
            decryptor.init(Cipher.DECRYPT_MODE, key, param);
            return decryptor.doFinal(dataWithIv, 16, size - 16);
        }
        catch (Exception e) {
            return null;
        }
    }
}
