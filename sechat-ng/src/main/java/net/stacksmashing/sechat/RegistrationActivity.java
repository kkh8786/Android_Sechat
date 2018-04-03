package net.stacksmashing.sechat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import net.stacksmashing.sechat.network.NetworkService;

import java.lang.ref.WeakReference;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class RegistrationActivity extends Activity implements View.OnClickListener, TextWatcher, TextView.OnEditorActionListener {
    @InjectView(R.id.activity_registration_username)
    EditText usernameField;
    @InjectView(R.id.activity_registration_device_name)
    EditText deviceNameField;
    @InjectView(R.id.activity_registration_register)
    Button registerButton;
    @InjectView(R.id.activity_registration_error)
    TextView errorText;

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_registration);
        ButterKnife.inject(this);

        usernameField.addTextChangedListener(this);
        deviceNameField.addTextChangedListener(this);
        deviceNameField.setOnEditorActionListener(this);
        deviceNameField.setEnabled(false);
        deviceNameField.setVisibility(View.GONE);

        registerButton.setEnabled(false);
        registerButton.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Bus.bus().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Bus.bus().unregister(this);
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if (textView == deviceNameField && actionId == EditorInfo.IME_ACTION_DONE) {
            doRegister();
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        if (view == registerButton) {
            doRegister();
        }
    }

    private boolean canRegister() {
        return usernameField.getText().length() != 0;
    }

    private void doRegister() {
        if (canRegister()) {
            setEnabled(false);
            new KeyGenerationTask(this).execute();
        }
    }

    private void setEnabled(boolean enabled) {
        registerButton.setEnabled(enabled);
        usernameField.setEnabled(enabled);
        deviceNameField.setEnabled(enabled);
    }

    private void registerWithServer(KeyPair keyPair) {
        String username = usernameField.getText().toString();
        String deviceName = deviceNameField.getText().toString();

        if (deviceName.isEmpty()) {
            deviceName = getString(R.string.default_device_name);
        }

        Preferences.setRegistrationData(this, username, deviceName, keyPair);

        ECPoint point = ((ECPublicKey) keyPair.getPublic()).getW();
        final String ecX = point.getAffineX().toString(10);
        final String ecY = point.getAffineY().toString(10);

        startService(NetworkService.intentWithRegistration(this, username, deviceName, ecX, ecY));
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        registerButton.setEnabled(canRegister());
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void receiveRegistrationResultEvent(Bus.RegistrationResultEvent event) {
        if (event.isSuccessful()) {
            finish();
            startActivity(getPackageManager().getLaunchIntentForPackage(getPackageName()));
        }
        else {
            Log.d("RegistrationActivity", "Error during registration");
            errorText.setText(event.getError());
            errorText.setVisibility(View.VISIBLE);
            setEnabled(true);
        }
    }

    public static Intent intent(Context context) {
        return new Intent(context, RegistrationActivity.class);
    }

    private static class KeyGenerationTask extends AsyncTask<Void, Void, KeyPair> {
        private final WeakReference<RegistrationActivity> activityRef;

        KeyGenerationTask(RegistrationActivity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        protected KeyPair doInBackground(Void... voids) {
            try {
                ECGenParameterSpec spec = new ECGenParameterSpec("secp521r1");
                KeyPairGenerator generator = KeyPairGenerator.getInstance("ECDSA");
                generator.initialize(spec);

                return generator.generateKeyPair();
            }
            catch (Exception e) {
                Log.i("RegistrationActivity", "Could not generate a key pair", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(KeyPair keyPair) {
            super.onPostExecute(keyPair);

            RegistrationActivity activity = activityRef.get();
            if (keyPair != null && activity != null) {
                activity.registerWithServer(keyPair);
            }
        }
    }
}
