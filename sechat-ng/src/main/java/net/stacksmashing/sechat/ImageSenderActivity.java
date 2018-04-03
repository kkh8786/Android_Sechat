package net.stacksmashing.sechat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Message;
import net.stacksmashing.sechat.util.ContentUtils;

import java.io.File;
import java.lang.ref.WeakReference;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ImageSenderActivity extends Activity implements View.OnClickListener {

    private static final String EXTRA_CONTENT_URI = "content_uri";
    private static final String EXTRA_LOCAL_UUID = "local_uuid";
    private static final String EXTRA_CHAT = "chat";
    private static final String EXTRA_DELETE_IN = "delete_in";
    private static final String EXTRA_FILENAME = "filename";

    public static Intent intentWithContentUri(Context context, Chat chat, long deleteIn, String filename, Uri uri) {
        Intent intent = intentWithLocalUuid(context, chat, deleteIn, filename, Message.generateUUID());
        intent.putExtra(EXTRA_CONTENT_URI, uri.toString());
        return intent;
    }

    public static Intent intentWithLocalUuid(Context context, Chat chat, long deleteIn, String filename, String localUuid) {
        Intent intent = new Intent(context, ImageSenderActivity.class);
        intent.putExtra(EXTRA_CHAT, chat);
        intent.putExtra(EXTRA_DELETE_IN, deleteIn);
        intent.putExtra(EXTRA_FILENAME, filename);
        intent.putExtra(EXTRA_LOCAL_UUID, localUuid);
        return intent;
    }

    @InjectView(R.id.activity_image_sender_preview)
    ImageView preview;

    @InjectView(R.id.activity_image_sender_edit)
    ImageButton editButton;

    @InjectView(R.id.activity_image_sender_send)
    ImageButton sendButton;

    @InjectView(R.id.activity_image_sender_cancel)
    ImageButton cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_sender);
        ButterKnife.inject(this);

        editButton.setOnClickListener(this);
        sendButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
        setEnabled(false);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_LOCAL_UUID)) {
            if (intent.hasExtra(EXTRA_CONTENT_URI)) {
                new FileCopyingTask(this).execute(intent.getStringExtra(EXTRA_CONTENT_URI), intent.getStringExtra(EXTRA_LOCAL_UUID));
                intent.removeExtra(EXTRA_CONTENT_URI);
            }
            else {
                loadImage();
            }
        }
        else {
            finish();
        }
    }

    private void loadImage() {
        File path = Message.getFileForUUID(this, getExtraLocalUuid());
        int width = getResources().getDisplayMetrics().widthPixels; // FIXME: This is duplicated a few times.
        int height = getResources().getDisplayMetrics().heightPixels;
        Picasso.with(this)
                .load(path)
                .resize(width, height)
                .centerInside()
                .skipMemoryCache()
                .into(preview);
        setEnabled(true);
    }

    private void setEnabled(boolean enabled) {
        editButton.setEnabled(enabled);
        sendButton.setEnabled(enabled);
    }

    private String getExtraLocalUuid() {
        return getIntent().getStringExtra(EXTRA_LOCAL_UUID);
    }

    @Override
    public void onClick(View view) {
        if (view == editButton) {
            startActivityForResult(ImageEditorActivity.intentWithFile(this, Message.getFileForUUID(this, getExtraLocalUuid())), 0);
        }
        else if (view == cancelButton) {
            finish();
        }
        else if (view == sendButton) {
            Chat chat = (Chat) getIntent().getSerializableExtra(EXTRA_CHAT);
            long deleteIn = getIntent().getLongExtra(EXTRA_DELETE_IN, 0);
            String filename = getIntent().getStringExtra(EXTRA_FILENAME);
            String localUuid = getExtraLocalUuid();
            chat.sendFile(deleteIn, filename, Message.Type.IMAGE, localUuid);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        loadImage();
    }

    private static class FileCopyingTask extends AsyncTask<String, Void, Boolean> {
        private final WeakReference<ImageSenderActivity> activityRef;

        private FileCopyingTask(ImageSenderActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            Context context = activityRef.get();
            Log.d("Image", "loading in the background");
            return context != null && ContentUtils.saveFromUriToLocalStorage(context, Uri.parse(params[0]), params[1]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            ImageSenderActivity activity = activityRef.get();
            if (activity != null) {
                if (result) {
                    activity.loadImage();
                }
                else {
                    activity.finish();
                }
            }
        }
    }
}
