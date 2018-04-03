package net.stacksmashing.sechat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class ImageViewActivity extends Activity implements Callback {
    private static final String EXTRA_URI = "uri";
    private PhotoViewAttacher attacher;

    public static Intent intentWithUri(Context context, String uri) {
        Intent intent = new Intent(context, ImageViewActivity.class);
        intent.putExtra(EXTRA_URI, uri);
        return intent;
    }

    @InjectView(R.id.activity_image_view_image_view)
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_view);

        ButterKnife.inject(this);

        if (getIntent() != null && getIntent().hasExtra(EXTRA_URI)) {
            Picasso.with(this).load(new File(getIntent().getStringExtra(EXTRA_URI))).into(imageView, this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Picasso.with(this).cancelRequest(imageView);

        if (attacher != null) {
            attacher.cleanup();
        }
    }

    @Override
    public void onSuccess() {
        attacher = new PhotoViewAttacher(imageView);
    }

    @Override
    public void onError() {
        finish();
    }
}
