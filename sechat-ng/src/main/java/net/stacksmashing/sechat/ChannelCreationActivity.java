package net.stacksmashing.sechat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import net.stacksmashing.sechat.network.ClientCreateChannelMessage;
import net.stacksmashing.sechat.network.NetworkService;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ChannelCreationActivity extends Activity implements View.OnClickListener {
    public static Intent intent(Context context) {
        return new Intent(context, ChannelCreationActivity.class);
    }

    @InjectView(R.id.activity_channel_creation_name)
    EditText channelName;

    @InjectView(R.id.activity_channel_creation_description)
    EditText channelDescription;

    @InjectView(R.id.activity_channel_creation_create)
    Button createButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_channel_creation);
        ButterKnife.inject(this);

        createButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == createButton) {
            String name = channelName.getText().toString();
            String description = channelDescription.getText().toString();
            NetworkService.getInstance().asyncSend(new ClientCreateChannelMessage(name, description));
            finish();
        }
    }
}
