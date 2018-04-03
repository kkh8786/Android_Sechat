package net.stacksmashing.sechat;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.File;

public class PictureThumbnailAdapter extends CursorAdapter {
    public PictureThumbnailAdapter(Context context) {
        super(context, null, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.fragment_chat_recent_photos_list_item, viewGroup, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
        view.setTag(path);
        Picasso.with(context)
                .load(new File(path))
                .resize(0, context.getResources().getDimensionPixelSize(R.dimen.photo_thumbnail_height))
                .into((ImageView) view.findViewById(R.id.fragment_chat_recent_photos_image));
    }

    public static CursorLoader createCursorLoader(Context context) {
        return new CursorLoader(context,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA},
                null,
                null,
                MediaStore.Images.Media._ID + " DESC");
    }
}
