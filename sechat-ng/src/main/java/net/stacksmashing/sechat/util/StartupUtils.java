package net.stacksmashing.sechat.util;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;
import net.stacksmashing.sechat.LoginActivity;
import net.stacksmashing.sechat.Preferences;
import net.stacksmashing.sechat.RegistrationActivity;
import net.stacksmashing.sechat.db.DatabaseProvider;

public final class StartupUtils {
    private StartupUtils() {
    }

    /**
     * Starts the registration or the login activity, if necessary.
     *
     * @param activity The activity this call is made from.
     * @return true if either the registration or the login activity was started, false otherwise.
     */
    public static boolean startRegistrationOrLoginActivity(Activity activity) {
        if (!Preferences.isRegistered(activity)) {
            activity.finish();
            activity.startActivity(RegistrationActivity.intent(activity));
            activity.overridePendingTransition(0, 0);
            return true;
        }

        if (!DatabaseProvider.isLoggedIn(activity)) {
            activity.finish();
            activity.startActivity(LoginActivity.intent(activity));
            activity.overridePendingTransition(0, 0);
            return true;
        }

        return false;
    }

    /**
     * Sets up Hockey crash handler and update manager (if enabled).
     *
     * @param activity The activity this call is made from.
     */
    public static void setupHockey(Activity activity) {
        ApplicationInfo appInfo;
        try {
            appInfo = activity.getPackageManager().getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
        }
        catch (PackageManager.NameNotFoundException e) {
            Log.d("StartupUtils", "Unable to retireve package metadata", e);
            return;
        }

        String hockeyAppId = appInfo.metaData.getString("net.stacksmashing.sechat.hockeyAppId");

        if (hockeyAppId == null) {
            return;
        }

        CrashManager.register(activity, hockeyAppId);

        boolean hockeyUpdatesEnabled = appInfo.metaData.getBoolean("net.stacksmashing.sechat.hockeyUpdatesEnabled", false);

        if (hockeyUpdatesEnabled) {
            UpdateManager.register(activity, hockeyAppId);
        }
    }
}
