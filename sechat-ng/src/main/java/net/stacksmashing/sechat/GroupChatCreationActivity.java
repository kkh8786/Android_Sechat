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

import com.squareup.otto.Subscribe;

import net.stacksmashing.sechat.network.ClientCreateGroupChatMessage;
import net.stacksmashing.sechat.network.NetworkService;
import net.stacksmashing.sechat.util.RuntimeDataHelper;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class GroupChatCreationActivity extends Activity implements View.OnClickListener, TextWatcher {
    private static final String EXTRA_USERS = "users";
    public static final String EXTRA_ID = "id";

    public static Intent intentWithUsers(Context context, List<String> users) {
        Intent intent = new Intent(context, GroupChatCreationActivity.class);
        intent.putStringArrayListExtra(EXTRA_USERS, new ArrayList<>(users));
        return intent;
    }

    @InjectView(R.id.activity_group_chat_creation_name)
    EditText name;

    @InjectView(R.id.activity_group_chat_creation_cancel)
    Button buttonCancel;

    @InjectView(R.id.activity_group_chat_creation_ok)
    Button buttonOk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_group_chat_creation);
        ButterKnife.inject(this);

        buttonCancel.setOnClickListener(this);
        buttonOk.setOnClickListener(this);
        buttonOk.setEnabled(false);
        name.addTextChangedListener(this);
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

    @SuppressWarnings("unused")
    @Subscribe
    public void handleGroupChatCreated(Bus.GroupChatCreatedEvent event) {
        if (event.getChatId() != -1) {
            Intent result = new Intent();
            result.putExtra(EXTRA_ID, event.getChatId());
            setResult(RESULT_OK, result);
        }
        else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    @Override
    public void onClick(View view) {
        if (view == buttonCancel) {
            finish();
        }
        else if (view == buttonOk) {
            buttonOk.setEnabled(false);
            this.name.setEnabled(false);

            List<String> users = getIntent().getStringArrayListExtra(EXTRA_USERS);
            String name = this.name.getText().toString();
            RuntimeDataHelper.getInstance().setGroupChatUsers(name, users);
            NetworkService.getInstance().asyncSend(new ClientCreateGroupChatMessage(name, users));
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        buttonOk.setEnabled(isValidGroupChatName(editable.toString()));
    }

    private static boolean isValidGroupChatName(String name) {
        return !name.isEmpty();
    }
}
