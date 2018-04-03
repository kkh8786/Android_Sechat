package net.stacksmashing.sechat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import net.stacksmashing.sechat.network.ClientVerifyPhoneNumberMessage;
import net.stacksmashing.sechat.network.NetworkService;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class PhoneNumberVerificationActivity extends Activity implements View.OnClickListener {
    public static Intent intent(Context context) {
        return new Intent(context, PhoneNumberVerificationActivity.class);
    }

    @InjectView(R.id.activity_phone_number_verification_code)
    EditText codeText;

    @InjectView(R.id.activity_phone_number_verification_verify)
    Button verifyButton;

    @InjectView(R.id.activity_phone_number_verification_error)
    TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_phone_number_verification);
        ButterKnife.inject(this);

        verifyButton.setOnClickListener(this);
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
        codeText.setEnabled(enabled);
        verifyButton.setEnabled(enabled);
    }

    @Override
    public void onClick(View v) {
        if (v == verifyButton) {
            setEnabled(false);
            String code = codeText.getText().toString();
            NetworkService.getInstance().asyncSend(new ClientVerifyPhoneNumberMessage(code));
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleVerificationEvent(Bus.PhoneNumberVerificationResultEvent event) {
        if (event.isSuccessful()) {
            Toast.makeText(this, R.string.activity_phone_number_verification_success, Toast.LENGTH_LONG).show();
            finish();
        }
        else {
            setEnabled(true);
            errorText.setVisibility(View.VISIBLE);
        }
    }
}
