package net.stacksmashing.sechat;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import net.stacksmashing.sechat.network.NetworkService;

import java.io.InputStream;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "SettingsFragment";

    private static final int REQUEST_PICK_BACKGROUND_IMAGE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(Preferences.PREFERENCES_NAME);
        addPreferencesFromResource(R.xml.preferences);

        findChatBackgroundPicturePreference().setOnPreferenceClickListener(this);
        findChatBackgroundPictureClearPreference().setOnPreferenceClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        setHasChatBackgroundPicture(Preferences.hasChatBackgroundPicture(getActivity()));
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals("chat_background_picture")) {
            Intent backgroundImageIntent = new Intent(Intent.ACTION_PICK);
            backgroundImageIntent.setType("image/*");
            startActivityForResult(backgroundImageIntent, REQUEST_PICK_BACKGROUND_IMAGE);
        }
        else if (preference.getKey().equals("chat_background_picture_clear")) {
            Preferences.clearChatBackgroundPicture(getActivity());
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_BACKGROUND_IMAGE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            try {
                // FIXME: This was previously done on a background thread.
                InputStream inputStream = getActivity().getContentResolver().openInputStream(data.getData());
                Preferences.setChatBackgroundPicture(getActivity(), inputStream);
            }
            catch (Exception e) {
                Log.w(TAG, "Could not read the picked background picture", e);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Preferences.PREFERENCE_CHAT_BACKGROUND)) {
            setHasChatBackgroundPicture(Preferences.hasChatBackgroundPicture(getActivity()));
        }
        else if (key.equals(Preferences.PREFERENCE_INVISIBLE)) {
            // NetworkService will reconnect automatically.
            NetworkService.getInstance().asyncDisconnect();
        }
    }

    private void setHasChatBackgroundPicture(boolean hasPicture) {
        findChatBackgroundPictureClearPreference().setEnabled(hasPicture);

        Drawable icon = hasPicture ? Drawable.createFromPath(Preferences.getChatBackgroundPictureFile(getActivity()).toString()) : null;

        findChatBackgroundPicturePreference().setIcon(icon);
    }

    private Preference findChatBackgroundPicturePreference() {
        return getPreferenceManager().findPreference("chat_background_picture");
    }

    private Preference findChatBackgroundPictureClearPreference() {
        return getPreferenceManager().findPreference("chat_background_picture_clear");
    }
}
