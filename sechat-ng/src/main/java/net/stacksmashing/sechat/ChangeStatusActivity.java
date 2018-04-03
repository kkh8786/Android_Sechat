package net.stacksmashing.sechat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import net.stacksmashing.sechat.network.ClientSetStatusMessage;
import net.stacksmashing.sechat.network.NetworkService;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ChangeStatusActivity extends Activity implements TextWatcher, View.OnClickListener {

    public static Intent intent(Context context) {
        return new Intent(context, ChangeStatusActivity.class);
    }

    @InjectView(R.id.activity_change_status_status)
    EditText status;

    @InjectView(R.id.activity_change_status_ok)
    Button buttonOk;

    @InjectView(R.id.activity_change_status_cancel)
    Button buttonCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_change_status);

        ButterKnife.inject(this);

        buttonOk.setOnClickListener(this);
        buttonOk.setEnabled(false);
        buttonCancel.setOnClickListener(this);
        status.addTextChangedListener(this);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        buttonOk.setEnabled(!status.getText().toString().isEmpty());
    }

    @Override
    public void onClick(View view) {
        if (view == buttonCancel) {
            finish();
        }
        else if (view == buttonOk) {
            final String newStatus = status.getText().toString();
            NetworkService.getInstance().asyncSend(new ClientSetStatusMessage(newStatus));
            Preferences.setStatus(this, newStatus);
            finish();
        }
    }
}
