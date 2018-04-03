package net.stacksmashing.sechat;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

public abstract class GoogleApiActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationSource {

    private static final String STATE_RESOLVING_GOOGLE_API_ERROR = "resolving_google_api_error";
    private static final int REQUEST_GOOGLE_API_RESOLVE_ERROR = 1001;

    /* Used for location lookup. */
    private GoogleApiClient googleApiClient;
    private boolean resolvingGoogleApiError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        resolvingGoogleApiError = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RESOLVING_GOOGLE_API_ERROR);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(STATE_RESOLVING_GOOGLE_API_ERROR, resolvingGoogleApiError);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!resolvingGoogleApiError) {
            googleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (resolvingGoogleApiError) {
            return;
        }

        resolvingGoogleApiError = true;

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, REQUEST_GOOGLE_API_RESOLVE_ERROR);
            }
            catch (IntentSender.SendIntentException e) {
                googleApiClient.connect();
            }
        }
        else {
            ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
            // Pass the error that should be displayed
            Bundle args = new Bundle();
            args.putInt("dialog_error", connectionResult.getErrorCode());
            dialogFragment.setArguments(args);
            dialogFragment.show(getFragmentManager(), "error_dialog");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GOOGLE_API_RESOLVE_ERROR) {
            resolvingGoogleApiError = false;
            if (resultCode == RESULT_OK) {
                if (!googleApiClient.isConnecting() && !googleApiClient.isConnected()) {
                    googleApiClient.connect();
                }
            }
        }
    }

    @Override
    public Location getLastLocation() {
        return LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
    }
}
