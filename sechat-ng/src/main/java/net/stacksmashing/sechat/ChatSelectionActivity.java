package net.stacksmashing.sechat;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;

import java.io.Serializable;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_ID;

public class ChatSelectionActivity extends Activity implements View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final String EXTRA_EXCLUSION = "exclusion";
    public static final String EXTRA_CHAT_IDS = "chat_ids";
    public static final String EXTRA_DATA = "data";

    public static Intent intentWithExclusionAndData(Context context, long excludedChatId, Serializable data) {
        Intent intent = new Intent(context, ChatSelectionActivity.class);
        intent.putExtra(EXTRA_EXCLUSION, excludedChatId);
        intent.putExtra(EXTRA_DATA, data);
        return intent;
    }

    @InjectView(R.id.activity_chat_selection_chat_list)
    ListView chatList;

    @InjectView(R.id.activity_chat_selection_cancel)
    Button buttonCancel;

    @InjectView(R.id.activity_chat_selection_ok)
    Button buttonOk;

    private ChatListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat_selection);

        ButterKnife.inject(this);

        adapter = new ChatListAdapter(this);
        adapter.setSelecting(true);

        chatList.setAdapter(adapter);

        buttonCancel.setOnClickListener(this);
        buttonOk.setOnClickListener(this);

        // FIXME: This is REALLY bad, but needed right now to prepopulate the chat list with single-user chats, which otherwise only show up after at least being opened once.
        for (Contact contact : Contact.DAO.query(this, null, null, null)) {
            Chat.findOrCreateChatIdByContact(this, contact, false);
        }

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onClick(View v) {
        if (v == buttonCancel) {
            setResult(RESULT_CANCELED);
            finish();
        }
        else if (v == buttonOk) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DATA, getIntent().getSerializableExtra(EXTRA_DATA));
            intent.putExtra(EXTRA_CHAT_IDS, chatList.getCheckedItemIds());
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String exclusion = String.valueOf(getIntent().getLongExtra(EXTRA_EXCLUSION, -1));
        return Chat.getOrderedCursorLoader(this,
                COLUMN_ID + " != ?",
                new String[]{exclusion});
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }
}
