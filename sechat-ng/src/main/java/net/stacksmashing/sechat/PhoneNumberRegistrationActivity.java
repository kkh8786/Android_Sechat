package net.stacksmashing.sechat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import net.stacksmashing.sechat.network.ClientRegisterPhoneNumberMessage;
import net.stacksmashing.sechat.network.NetworkService;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class PhoneNumberRegistrationActivity extends Activity implements View.OnClickListener {
    public static Intent intent(Context context) {
        return new Intent(context, PhoneNumberRegistrationActivity.class);
    }

    @InjectView(R.id.activity_phone_number_registration_number)
    EditText numberText;

    @InjectView(R.id.activity_phone_number_registration_register)
    Button registerButton;

    @InjectView(R.id.activity_phone_number_registration_enter_code)
    Button enterCodeButton;

    @InjectView(R.id.activity_phone_number_registration_error)
    TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_phone_number_registration);
        ButterKnife.inject(this);

        registerButton.setOnClickListener(this);
        enterCodeButton.setOnClickListener(this);
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

    private void setEnabled(boolean enabled) {
        registerButton.setEnabled(enabled);
        numberText.setEnabled(enabled);
    }

    @Override
    public void onClick(View v) {
        if (v == registerButton) {
            setEnabled(false);
            final String phoneNumber = numberText.getText().toString();
            NetworkService.getInstance().asyncSend(new ClientRegisterPhoneNumberMessage(phoneNumber));
        }
        else if (v == enterCodeButton) {
            promptForCode();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleRegistrationEvent(Bus.PhoneNumberRegistrationResultEvent event) {
        if (event.isSuccessful()) {
            promptForCode();
        }
        else {
            setEnabled(true);
            errorText.setVisibility(View.VISIBLE);
            errorText.setText(event.getError());
        }
    }

    private void promptForCode() {
        finish();
        startActivity(PhoneNumberVerificationActivity.intent(this));
    }
}
