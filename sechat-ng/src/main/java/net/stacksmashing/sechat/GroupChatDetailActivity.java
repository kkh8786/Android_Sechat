package net.stacksmashing.sechat;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListView;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class GroupChatDetailActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String EXTRA_CHAT = "chat";

    public static Intent intentWithChat(Context context, Chat chat) {
        Intent intent = new Intent(context, GroupChatDetailActivity.class);
        intent.putExtra(EXTRA_CHAT, chat);
        return intent;
    }

    @InjectView(R.id.activity_group_chat_detail_contact_list)
    ListView contactList;

    ContactListAdapter adapter;
    Chat chat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() == null || getIntent().getSerializableExtra(EXTRA_CHAT) == null) {
            finish();
        }

        chat = (Chat) getIntent().getSerializableExtra(EXTRA_CHAT);

        setContentView(R.layout.activity_group_chat_detail);

        ButterKnife.inject(this);

        adapter = new ContactListAdapter(this);

        contactList.setAdapter(adapter);

        if (getActionBar() != null) {
            getActionBar().setTitle(String.format(getString(R.string.activity_group_chat_detail_title), chat.getName()));
        }

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return Contact.getOrderedCursorLoaderForChatId(this, chat.getId());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        adapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        adapter.swapCursor(null);
    }
}
