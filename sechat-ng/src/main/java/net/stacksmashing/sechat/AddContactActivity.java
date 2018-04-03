package net.stacksmashing.sechat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.network.ClientAddContactMessage;
import net.stacksmashing.sechat.network.NetworkService;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class AddContactActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "AddContactActivity";

    private static final int REQUEST_QR_CODE = 0;

    public static Intent intent(Context context) {
        return new Intent(context, AddContactActivity.class);
    }

    @InjectView(R.id.activity_add_contact_cancel)
    Button cancelButton;

    @InjectView(R.id.activity_add_contact_scan_qr)
    Button scanQRButton;

    @InjectView(R.id.activity_add_contact_done)
    Button doneButton;

    @InjectView(R.id.activity_add_contact_name)
    EditText contactName;

    @InjectView(R.id.activity_add_contact_error)
    TextView errorText;

    @Nullable
    private String expectedX, expectedY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        ButterKnife.inject(this);

        cancelButton.setOnClickListener(this);
        scanQRButton.setOnClickListener(this);
        doneButton.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Bus.bus().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Bus.bus().register(this);
    }

    @Override
    public void onClick(View view) {
        if (view == cancelButton) {
            finish();
        }
        else if (view == scanQRButton) {
            startActivityForResult(QRCodeScannerActivity.intent(this), REQUEST_QR_CODE);
        }
        else if (view == doneButton && contactName.getText().length() != 0) {
            setEnabled(false);
            clearExpectedKeyData();
            ClientAddContactMessage addContactMessage = new ClientAddContactMessage(contactName.getText().toString());
            NetworkService.getInstance().asyncSend(addContactMessage);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleAddContactEvent(Bus.AddContactEvent event) {
        if (event.isSuccessful()) {
            boolean keyDataIsValid = matchesExpectedKeyData(event);
            clearExpectedKeyData();

            if (!keyDataIsValid) {
                showError(getString(R.string.activity_add_contact_key_data_mismatch));
                return;
            }

            Contact contact = new Contact(event.getUsername(), (event.getUserX() + " " + event.getUserY()).getBytes()); // TODO Proper key data
            try {
                Contact.DAO.insert(this, contact);
            }
            catch (Exception e) {
                Log.d(TAG, "Could not insert contact", e);
            }

            finish();
        }
        else {
            showError(event.getError());
            setEnabled(true);
        }
    }

    private void setEnabled(boolean enabled) {
        scanQRButton.setEnabled(enabled);
        doneButton.setEnabled(enabled);
        contactName.setEnabled(enabled);
    }

    private void showError(String error) {
        errorText.setVisibility(View.VISIBLE);
        errorText.setText(error);
    }

    private void clearExpectedKeyData() {
        setExpectedKeyData(null, null);
    }

    private void setExpectedKeyData(String x, String y) {
        expectedX = x;
        expectedY = y;
    }

    private boolean matchesExpectedKeyData(Bus.AddContactEvent event) {
        return expectedX == null || expectedY == null
                || (expectedX.equals(event.getUserX()) && expectedY.equals(event.getUserY()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == REQUEST_QR_CODE && data.hasExtra(QRCodeScannerActivity.EXTRA_CONTENT)) {
            String content = data.getStringExtra(QRCodeScannerActivity.EXTRA_CONTENT);

            Contact.QrData qrData = Contact.QrData.fromJSON(content);

            if (qrData != null) {
                setEnabled(false);
                setExpectedKeyData(qrData.getX(), qrData.getY());
                ClientAddContactMessage addContactMessage = new ClientAddContactMessage(qrData.getUsername());
                NetworkService.getInstance().asyncSend(addContactMessage);
            }
        }
    }
}
