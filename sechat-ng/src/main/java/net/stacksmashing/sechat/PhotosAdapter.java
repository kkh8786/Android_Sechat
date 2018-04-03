package net.stacksmashing.sechat;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.squareup.picasso.Picasso;

import net.stacksmashing.sechat.db.Message;

import butterknife.ButterKnife;
import butterknife.InjectView;

class PhotosAdapter extends CursorAdapter {

    private int columnSize;

    public PhotosAdapter(Context context, int width) {
        super(context, null, 0);
        columnSize = width;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.activity_photos_itemview, parent, false);
        view.setTag(new ViewHolder(view));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Message message = Message.DAO.cursorToObject(cursor);
        ((ViewHolder) view.getTag()).bind(context, message);
    }

    public class ViewHolder {

        @InjectView(R.id.photo_grid_item)
        ImageView imageView;

        ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }

        void bind(Context context, Message message) {
            imageView.setLayoutParams(new LinearLayout.LayoutParams(columnSize, columnSize));
            Picasso.with(context).load(message.getFile(context)).resize(columnSize, columnSize).centerInside().into(imageView);
        }
    }
}

