package net.stacksmashing.sechat.util;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.widget.BaseAdapter;

public class AdapterRefreshingObserver extends ContentObserver {
    private final BaseAdapter adapter;

    public AdapterRefreshingObserver(Handler handler, BaseAdapter adapter) {
        super(handler);
        this.adapter = adapter;
    }

    @Override
    public boolean deliverSelfNotifications() {
        return true;
    }

    @Override
    public void onChange(boolean selfChange) {
        onChange(selfChange, null);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
