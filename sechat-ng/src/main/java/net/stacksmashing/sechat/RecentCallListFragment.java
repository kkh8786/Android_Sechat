package net.stacksmashing.sechat;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.stacksmashing.sechat.db.RecentCall;

import java.text.SimpleDateFormat;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class RecentCallListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private CursorAdapter listAdapter;

    public static Fragment newInstance() {
        return new RecentCallListFragment();
    }

    public RecentCallListFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        listAdapter = new RecentCallListAdapter(getActivity());

        setListAdapter(listAdapter);

        setEmptyText(getString(R.string.fragment_recent_call_list_empty));

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.title_recent_calls);
            actionBar.setSubtitle(null);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return RecentCall.DAO.getCursorLoader(getActivity(), null, null, "TIME DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        listAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        listAdapter.swapCursor(null);
    }

    static class RecentCallListAdapter extends CursorAdapter {

        public RecentCallListAdapter(Context context) {
            super(context, null, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            View view = LayoutInflater.from(context).inflate(R.layout.recent_call_list_item, viewGroup, false);
            view.setTag(new ViewHolder(view));
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            RecentCall recentCall = RecentCall.DAO.cursorToObject(cursor);
            ((ViewHolder) view.getTag()).bind(context, recentCall);
        }

        static class ViewHolder {
            @InjectView(R.id.recent_call_list_item_direction)
            ImageView directionIcon;

            @InjectView(R.id.recent_call_list_item_contact)
            TextView contactText;

            @InjectView(R.id.recent_call_list_item_time)
            TextView timeText;

            @InjectView(R.id.recent_call_list_item_duration)
            TextView durationText;

            ViewHolder(View view) {
                ButterKnife.inject(this, view);
            }

            void bind(Context context, RecentCall recentCall) {
                directionIcon.setImageDrawable(context.getResources().getDrawable(recentCall.getIcon()));

                contactText.setText(recentCall.getContact());

                timeText.setText(SimpleDateFormat.getDateTimeInstance().format(recentCall.getTime()));

                durationText.setText(DateUtils.formatElapsedTime(recentCall.getDuration() / 1000));
            }
        }
    }
}
