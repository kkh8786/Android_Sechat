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

import net.stacksmashing.sechat.db.Contact;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ContactSelectionActivity extends Activity implements View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXTRA_USERS = "users";
    private static final String EXTRA_INITIAL_SELECTION = "initial_selection";

    public static Intent intent(Context context) {
        return new Intent(context, ContactSelectionActivity.class);
    }

    public static Intent intentWithInitialSelection(Context context, List<Long> ids) {
        Intent intent = intent(context);
        intent.putExtra(EXTRA_INITIAL_SELECTION, ids.toArray(new Long[ids.size()]));
        return intent;
    }

    @InjectView(R.id.activity_contact_selection_contact_list)
    ListView contactList;

    @InjectView(R.id.activity_contact_selection_cancel)
    Button buttonCancel;

    @InjectView(R.id.activity_contact_selection_ok)
    Button buttonOk;

    private ContactListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_contact_selection);

        ButterKnife.inject(this);

        adapter = new ContactListAdapter(this);
        adapter.setSelecting(true);

        contactList.setAdapter(adapter);

        buttonCancel.setOnClickListener(this);
        buttonOk.setOnClickListener(this);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onClick(View view) {
        if (view == buttonCancel) {
            setResult(RESULT_CANCELED);
            finish();
        }
        else if (view == buttonOk) {
            Intent result = new Intent();
            result.putStringArrayListExtra(EXTRA_USERS, getCheckedUsernames());
            setResult(RESULT_OK, result);
            finish();
        }
    }

    private ArrayList<String> getCheckedUsernames() {
        return adapter.getCheckedUsernames(contactList.getCheckedItemPositions());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return Contact.getOrderedCursorLoader(this, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> objectLoader, Cursor o) {
        adapter.swapCursor(o);

        if (getIntent() != null && getIntent().hasExtra(EXTRA_INITIAL_SELECTION)) {
            Long[] ids = (Long[]) getIntent().getSerializableExtra(EXTRA_INITIAL_SELECTION);
            for (Long id : ids) {
                if (id != null) {
                    for (int i = 0; i < contactList.getCount(); i++) {
                        if (contactList.getItemIdAtPosition(i) == id) {
                            contactList.setItemChecked(i, true);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> objectLoader) {
        adapter.swapCursor(null);
    }
}
