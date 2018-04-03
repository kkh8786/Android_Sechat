package net.stacksmashing.sechat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.PicassoTools;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public final class Preferences {
    private static final String TAG = "Preferences";

    static final String PREFERENCES_NAME = "development_preferences";

    private static final String PREFERENCE_USERNAME = "username";
    private static final String PREFERENCE_DEVICE_NAME = "device_name";
    private static final String PREFERENCE_PUBLIC_KEY = "public_key";
    private static final String PREFERENCE_PRIVATE_KEY = "private_key";
    private static final String PREFERENCE_STATUS = "status";
    static final String PREFERENCE_CHAT_BACKGROUND = "chat_background_is_set";
    static final String PREFERENCE_INVISIBLE = "invisible";
    private static final String PREFERENCE_SEND_IN_CHAT_MESSAGES = "send_in_chat_messages";
    private static final String PREFERENCE_SEND_DELIVERY_NOTIFICATIONS = "send_delivery_notifications";
    private static final String PREFERENCE_SEND_READ_NOTIFICATIONS = "send_read_notifications";
    private static final String PREFERENCE_LAST_CHAT_ID = "last_chat_id";

    private static final String FILE_CHAT_BACKGROUND = "chat_background.jpg";
    private static final String FILE_PROFILE_PICTURE = "profile_picture.jpg";

    private static volatile KeyPair keyPair = null;
    private static volatile String username = null;

    private Preferences() {
    }

    private static SharedPreferences.Editor registrationEditor = null;

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public static void setRegistrationData(Context context, String username, String deviceName, KeyPair keyPair) {
        registrationEditor = getPreferences(context).edit();
        registrationEditor.putString(PREFERENCE_USERNAME, username);
        registrationEditor.putString(PREFERENCE_DEVICE_NAME, deviceName);
        registrationEditor.putString(PREFERENCE_PUBLIC_KEY, Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.DEFAULT));
        registrationEditor.putString(PREFERENCE_PRIVATE_KEY, Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.DEFAULT));
    }

    public static void register() {
        registrationEditor.commit();
        registrationEditor = null;
    }

    public static boolean isRegistered(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.contains(PREFERENCE_USERNAME)
                && prefs.contains(PREFERENCE_DEVICE_NAME)
                && prefs.contains(PREFERENCE_PUBLIC_KEY)
                && prefs.contains(PREFERENCE_PRIVATE_KEY);
    }

    public synchronized static String getUsername(Context context) {
        if (username == null) {
            username = getPreferences(context).getString(PREFERENCE_USERNAME, "");
        }
        return username;
    }

    public static String getDeviceName(Context context) {
        return getPreferences(context).getString(PREFERENCE_DEVICE_NAME, "");
    }

    public synchronized static KeyPair getKeyPair(Context context) {
        SharedPreferences prefs = getPreferences(context);
        if (!prefs.contains(PREFERENCE_PRIVATE_KEY) || !prefs.contains(PREFERENCE_PUBLIC_KEY)) {
            Log.e(TAG, "getKeyPair called, but no key is saved in preferences");
            return null;
        }

        if (keyPair == null) {
            try {
                byte[] privateKey = Base64.decode(prefs.getString(PREFERENCE_PRIVATE_KEY, ""), Base64.DEFAULT);
                byte[] publicKey = Base64.decode(prefs.getString(PREFERENCE_PUBLIC_KEY, ""), Base64.DEFAULT);

                keyPair = new KeyPair(
                        KeyFactory.getInstance("EC", "BC").generatePublic(new X509EncodedKeySpec(publicKey)),
                        KeyFactory.getInstance("EC", "BC").generatePrivate(new PKCS8EncodedKeySpec(privateKey)));
            }
            catch (Exception e) {
                Log.e(TAG, "Could not read key from preferences", e);
                keyPair = null;
            }
        }

        return keyPair;
    }

    public static String getStatus(Context context) {
        return getPreferences(context).getString(PREFERENCE_STATUS, context.getString(R.string.fragment_profile_no_status));
    }

    public static void setStatus(Context context, String status) {
        getPreferences(context).edit().putString(PREFERENCE_STATUS, status).commit();
    }

    public static long getLastChatId(Context context) {
        return getPreferences(context).getLong(PREFERENCE_LAST_CHAT_ID, -1);
    }

    public static void setLastChatId(Context context, long id) {
        getPreferences(context).edit().putLong(PREFERENCE_LAST_CHAT_ID, id).commit();
    }

    public static void loadProfilePictureInto(Context context, ImageView imageView) {
        if (getProfilePictureFile(context).exists()) {
            Picasso.with(context).load(getProfilePictureFile(context)).into(imageView);
        }
        else {
            imageView.setImageDrawable(getProfilePlaceholderDrawable(context, getUsername(context)));
        }
    }

    private static File getProfilePictureFile(Context context) {
        return context.getFileStreamPath(FILE_PROFILE_PICTURE);
    }

    public synchronized static byte[] getProfilePictureBytes(Context context) {
        try {
            return IOUtils.toByteArray(context.openFileInput(FILE_PROFILE_PICTURE));
        }
        catch (Exception e) {
            Log.w(TAG, "Could not read the profile picture", e);
            return null;
        }
    }

    public synchronized static void setProfilePicture(Context context, InputStream pictureStream) {
        try {
            byte[] pictureBytes = IOUtils.toByteArray(pictureStream);
            Bitmap bitmap = BitmapFactory.decodeByteArray(pictureBytes, 0, pictureBytes.length);

            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            Bitmap croppedBitmap;
            if (w > h) {
                croppedBitmap = Bitmap.createBitmap(bitmap, (w - h) / 2, 0, h, h);
            }
            else {
                croppedBitmap = Bitmap.createBitmap(bitmap, 0, (h - w) / 2, w, w);
            }

            Bitmap.createScaledBitmap(croppedBitmap, 300, 300, true).compress(Bitmap.CompressFormat.JPEG, 80, context.openFileOutput("profile_picture.jpg", 0));

            bitmap.recycle();
            croppedBitmap.recycle();
        }
        catch (Exception e) {
            Log.e(TAG, "Could not save the profile picture", e);
            return;
        }
        PicassoTools.clearCache(Picasso.with(context));
    }

    public static Drawable getProfilePlaceholderDrawable(Context context, String string) {
        return new LayerDrawable(new Drawable[]{
                createColorDrawable(string),
                createTextDrawable(context, string.substring(0, Math.min(string.length(), 2)).toUpperCase(), 240, 240)
        });
    }

    private static Drawable createColorDrawable(String string) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(string.getBytes());
            byte bytes[] = digest.digest();

            float r = 10.f * (bytes[0] & 0xff) / 256.f;
            r -= (int) r;

            return new ColorDrawable(Color.HSVToColor(new float[]{360.f * r, 1.f, 1.f}));
        }
        catch (Exception e) {
            return new ColorDrawable();
        }
    }

    private static Drawable createTextDrawable(Context context, String text, int width, int height) {
        Bitmap canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // Create a canvas, that will draw on to canvasBitmap.
        Canvas canvas = new Canvas(canvasBitmap);

        // Set up the paint for use with our Canvas
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(height * 0.4f);
        paint.setAntiAlias(true);
        paint.setFakeBoldText(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);

        // Draw the text on top of our image
        Rect textBounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), textBounds);
        canvas.drawText(text, width / 2 - textBounds.exactCenterX(), height / 2 - textBounds.exactCenterY(), paint);

        return new BitmapDrawable(context.getResources(), canvasBitmap);
    }

    public static void setChatBackgroundPicture(Context context, InputStream inputStream) {
        try {
            getPreferences(context).edit().putBoolean(PREFERENCE_CHAT_BACKGROUND, false).commit();
            FileUtils.copyInputStreamToFile(inputStream, context.getFileStreamPath(FILE_CHAT_BACKGROUND));
            PicassoTools.clearCache(Picasso.with(context));
        }
        catch (Exception e) {
            Log.w(TAG, "Could not save chat background picture", e);
            return;
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }

        getPreferences(context).edit().putBoolean(PREFERENCE_CHAT_BACKGROUND, true).commit();
    }

    public static void clearChatBackgroundPicture(Context context) {
        if (hasChatBackgroundPicture(context)) {
            getPreferences(context).edit().putBoolean(PREFERENCE_CHAT_BACKGROUND, false).commit();
            FileUtils.deleteQuietly(context.getFileStreamPath(FILE_CHAT_BACKGROUND));
        }
    }

    public static File getChatBackgroundPictureFile(Context context) {
        if (hasChatBackgroundPicture(context)) {
            return context.getFileStreamPath(FILE_CHAT_BACKGROUND);
        }
        return null;
    }

    public static boolean hasChatBackgroundPicture(Context context) {
        return getPreferences(context).getBoolean(PREFERENCE_CHAT_BACKGROUND, false);
    }

    public static boolean shouldSendInChatMessage(Context context) {
        return getPreferences(context).getBoolean(PREFERENCE_SEND_IN_CHAT_MESSAGES, true);
    }

    public static boolean isInvisible(Context context) {
        return getPreferences(context).getBoolean(PREFERENCE_INVISIBLE, false);
    }

    public static boolean shouldSendDeliveryNotification(Context context) {
        return getPreferences(context).getBoolean(PREFERENCE_SEND_DELIVERY_NOTIFICATIONS, true);
    }

    public static boolean shouldSendReadNotification(Context context) {
        return getPreferences(context).getBoolean(PREFERENCE_SEND_READ_NOTIFICATIONS, true);
    }

    public static String generateJSONProfileData(Context context) {
        JSONObject result = new JSONObject();
        try {
            result.put("username", Preferences.getUsername(context));
            KeyPair keyPair = Preferences.getKeyPair(context);
            ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
            ECPoint w = publicKey.getW();
            result.put("X", w.getAffineX().toString());
            result.put("Y", w.getAffineY().toString());
        }
        catch (Exception e) {
            Log.d(TAG, "Failed to encode profile data into JSON", e);
        }
        return result.toString();
    }

}
