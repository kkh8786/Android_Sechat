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

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ChangeAliasActivity extends Activity implements TextWatcher, View.OnClickListener {

    public static final String EXTRA_ALIAS = "alias";

    public static Intent intent(Context context) {
        return new Intent(context, ChangeAliasActivity.class);
    }

    @InjectView(R.id.activity_change_alias_alias)
    EditText alias;

    @InjectView(R.id.activity_change_alias_ok)
    Button buttonOk;

    @InjectView(R.id.activity_change_alias_cancel)
    Button buttonCancel;

    @InjectView(R.id.activity_change_alias_clear)
    Button buttonClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_change_alias);

        ButterKnife.inject(this);

        buttonOk.setEnabled(false);
        buttonOk.setOnClickListener(this);
        buttonCancel.setOnClickListener(this);
        buttonClear.setOnClickListener(this);
        alias.addTextChangedListener(this);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        buttonOk.setEnabled(!alias.getText().toString().isEmpty());
    }

    @Override
    public void onClick(View view) {
        if (view == buttonCancel) {
            finishActivityWithResult(RESULT_CANCELED, null);
        }
        else if (view == buttonOk) {
            finishActivityWithResult(RESULT_OK, alias.getText().toString());
        }
        else if (view == buttonClear) {
            finishActivityWithResult(RESULT_OK, null);
        }
    }

    private void finishActivityWithResult(int status, String alias) {
        Intent result = new Intent();
        if (status == RESULT_OK) {
            result.putExtra(EXTRA_ALIAS, alias);
        }
        setResult(status, result);
        finish();
    }
}
