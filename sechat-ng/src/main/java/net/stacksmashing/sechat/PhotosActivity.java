package net.stacksmashing.sechat;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Point;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class PhotosActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor>, GridView.OnItemClickListener {

    private static final String EXTRA_CHAT_ID = "chat_id";

    // Number of columns of Grid View
    private static final int NUM_OF_COLUMNS = 5;

    // Grid view image padding
    private static final int GRID_PADDING = 10; // in dp

    private PhotosAdapter adapter;

    @InjectView(R.id.activity_photos_grid_view)
    GridView gridView;

    public static Intent intentWithChatId(Context context, long chatId) {
        Intent intent = new Intent(context, PhotosActivity.class);
        intent.putExtra(EXTRA_CHAT_ID, chatId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        ButterKnife.inject(this);

        float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, GRID_PADDING, getResources().getDisplayMetrics());

        int columnWidth = (int) ((getScreenWidth() - ((NUM_OF_COLUMNS + 1) * padding)) / NUM_OF_COLUMNS);

        gridView.setNumColumns(NUM_OF_COLUMNS);
        gridView.setColumnWidth(columnWidth);
        gridView.setStretchMode(GridView.NO_STRETCH);
        gridView.setPadding((int) padding, (int) padding, (int) padding, (int) padding);
        gridView.setHorizontalSpacing((int) padding);
        gridView.setVerticalSpacing((int) padding);

        adapter = new PhotosAdapter(this, columnWidth);

        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener(this);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return MessageArrayAdapter.createPhotoCursorLoader(this, getIntent().getLongExtra(EXTRA_CHAT_ID, -1));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivity(PhotosViewPagerActivity.intent(this, position));
    }

    private int getScreenWidth() {
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        final Point point = new Point();
        display.getSize(point);
        return point.x;
    }
}
