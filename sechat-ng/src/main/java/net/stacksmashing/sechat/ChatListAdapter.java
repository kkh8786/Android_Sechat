package net.stacksmashing.sechat;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.TextView;

import net.stacksmashing.sechat.db.Chat;

import butterknife.ButterKnife;
import butterknife.InjectView;

class ChatListAdapter extends CursorAdapter {
    private boolean isSelecting = false;

    public ChatListAdapter(Context context) {
        super(context, null, 0);
    }

    public void setSelecting(boolean isSelecting) {
        this.isSelecting = isSelecting;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_list_item, viewGroup, false);
        view.setTag(new ViewHolder(view));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Chat chat = Chat.DAO.cursorToObject(cursor);
        ((ViewHolder) view.getTag()).bind(this, chat);
    }

    static class ViewHolder implements NavigationActivity.NavigationItem {
        long id;

        @InjectView(R.id.chat_list_item_name)
        TextView nameText;

        @InjectView(R.id.chat_list_item_checkbox)
        CheckBox checkBox;

        ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }

        void bind(ChatListAdapter adapter, Chat chat) {
            id = chat.getId();

            checkBox.setVisibility(adapter.isSelecting ? View.VISIBLE : View.GONE);

            nameText.setText(chat.getName());
        }

        @Override
        public void onClick(NavigationProvider provider) {
            provider.openChatById(id);
        }
    }
}
