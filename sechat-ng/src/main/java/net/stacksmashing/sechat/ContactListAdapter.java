package net.stacksmashing.sechat;

import android.content.Context;
import android.database.Cursor;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.TextView;

import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.util.RuntimeDataHelper;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

class ContactListAdapter extends CursorAdapter {

    private boolean isSelecting;

    public ContactListAdapter(Context context) {
        super(context, null, 0);
        isSelecting = false;
    }

    public void setSelecting(boolean isSelecting) {
        this.isSelecting = isSelecting;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View view = LayoutInflater.from(context).inflate(R.layout.contact_list_item, viewGroup, false);
        view.setTag(new ViewHolder(view));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Contact contact = Contact.DAO.cursorToObject(cursor);
        ((ViewHolder) view.getTag()).bind(context, this, contact);
    }

    public ArrayList<String> getCheckedUsernames(SparseBooleanArray checkedStatus) {
        // FIXME: Likely not the most efficient way to do this.
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < checkedStatus.size(); i++) {
            if (checkedStatus.valueAt(i)) {
                Contact contact = Contact.DAO.cursorToObject((Cursor) getItem(checkedStatus.keyAt(i)));
                result.add(contact.getUsername());
            }
        }
        return result;
    }

    static class ViewHolder implements NavigationActivity.NavigationItem {
        Contact contact;

        @InjectView(R.id.contact_list_item_username)
        TextView usernameText;

        @InjectView(R.id.contact_list_item_online_status)
        TextView onlineStatus;

        @InjectView(R.id.contact_list_item_checkbox)
        CheckBox checkBox;

        ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }

        void bind(Context context, ContactListAdapter adapter, Contact contact) {
            this.contact = contact;

            checkBox.setVisibility(adapter.isSelecting ? View.VISIBLE : View.GONE);

            usernameText.setText(contact.getUsername());

            int colorRes = RuntimeDataHelper.getInstance().getContactStatus(contact).getColor();
            onlineStatus.setTextColor(context.getResources().getColor(colorRes));
        }

        @Override
        public void onClick(NavigationProvider provider) {
            provider.openChatForContact(contact, false);
        }
    }
}
