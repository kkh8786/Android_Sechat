package net.stacksmashing.sechat;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class UnreadListAdapter extends CursorAdapter {
    public UnreadListAdapter(Context context) {
        super(context, null, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.unread_list_item, parent, false);
        view.setTag(new ViewHolder(view));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ((ViewHolder) view.getTag()).bind(cursor);
    }

    static class ViewHolder implements NavigationActivity.NavigationItem {
        long id;

        @InjectView(R.id.unread_list_item_name)
        TextView name;

        @InjectView(R.id.unread_list_item_count)
        TextView count;

        private ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }

        private void bind(Cursor cursor) {
            id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
            name.setText(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            count.setText(String.format("%d", cursor.getInt(cursor.getColumnIndexOrThrow("count"))));

            boolean isHighlight = cursor.getInt(cursor.getColumnIndexOrThrow("highlight")) > 0;
            int typeface = isHighlight ? Typeface.BOLD : Typeface.NORMAL;

            name.setTypeface(null, typeface);
            count.setTypeface(null, typeface);
        }

        @Override
        public void onClick(NavigationProvider provider) {
            provider.openChatById(id);
        }
    }
}
