package net.stacksmashing.sechat.util;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.IOException;

/**
 * Created by kulikov on 14.12.2014.
 */

/**
 * Loads frames from a video at the specified microsecond.
 * Usage: Picasso picasso = new Picasso.Builder(this).addRequestHandler(new VideoFrameRequestHandler()).build();
 * picasso.load("videoframe://path/to/video#microseconds)
 * The offset is in microseconds, so #1000000 = 1 second in.
 */
public class PicassoVideoFrameRequestHandler extends RequestHandler {

    public static final String SCHEME = "videoframe";

    @Override
    public boolean canHandleRequest(Request data) {
        return SCHEME.equals(data.uri.getScheme());
    }

    @Override
    public RequestHandler.Result load(Request data) throws IOException {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        String path = data.uri.getPath();
        if (path == null) {
            path = data.uri.getSchemeSpecificPart();
        }
        mediaMetadataRetriever.setDataSource(path);
        String offsetString = data.uri.getFragment();
        long offset = Long.parseLong(offsetString);
        Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(offset);
        mediaMetadataRetriever.release();
        return new RequestHandler.Result(bitmap, Picasso.LoadedFrom.DISK);
    }

}
