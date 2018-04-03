package net.stacksmashing.sechat;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.stacksmashing.sechat.network.ClientJoinChannelMessage;
import net.stacksmashing.sechat.network.NetworkService;

import java.util.ArrayList;
import java.util.List;

public class ChannelListActivity extends ListActivity {
    private static final String EXTRA_CHANNELS = "channels";

    public static Intent intentWithChannelList(Context context, List<String> channels) {
        Intent intent = new Intent(context, ChannelListActivity.class);
        intent.putStringArrayListExtra(EXTRA_CHANNELS, new ArrayList<>(channels));
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null && getIntent().hasExtra(EXTRA_CHANNELS)) {
            List<String> channels = getIntent().getStringArrayListExtra(EXTRA_CHANNELS);
            setListAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, channels));
        }
        else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.channel_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_channel_list_new) {
            startActivity(ChannelCreationActivity.intent(this));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String channel = (String) getListView().getItemAtPosition(position);
        NetworkService.getInstance().asyncSend(new ClientJoinChannelMessage(channel));
    }
}
