package net.stacksmashing.sechat;

import android.content.Context;
import android.content.CursorLoader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.db.Download;
import net.stacksmashing.sechat.db.Message;
import net.stacksmashing.sechat.media.AudioPlayerController;
import net.stacksmashing.sechat.media.AudioPlayerListItem;
import net.stacksmashing.sechat.media.FileDurationCache;
import net.stacksmashing.sechat.util.PicassoVideoFrameRequestHandler;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.hdodenhof.circleimageview.CircleImageView;

import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_CHAT_ID;
import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_ID;
import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_TYPE;
import static net.stacksmashing.sechat.db.DatabaseHelper.TABLE_MESSAGES;

/**
 * Created by kulikov on 23.12.2014.
 */
public class MessageArrayAdapter extends CursorAdapter implements AudioPlayerController.Callback {
    @SuppressWarnings("unused")
    private static final String TAG = "MessageArrayAdapter";

    private final Chat.Type chatType;

    private AudioPlayerController audioPlayerController;
    private final int previewWidth;
    private final int previewHeight;

    private final String mapApiKey;
    private final int mapScale, mapWidth, mapHeight;

    public MessageArrayAdapter(Context context, Chat.Type chatType) {
        super(context, null, 0);
        this.chatType = chatType;

        previewWidth = context.getResources().getDimensionPixelSize(R.dimen.message_preview_width);
        previewHeight = context.getResources().getDimensionPixelSize(R.dimen.message_preview_height);

        mapApiKey = getGoogleMapsApiKey(context);

        /* The Static Google Maps API only supports 1 and 2 as scaling factors. */
        float scale = context.getResources().getDisplayMetrics().density;
        mapScale = scale > 1 ? 2 : 1;
        mapWidth = (int) Math.ceil(previewWidth / mapScale);
        mapHeight = (int) Math.ceil(previewHeight / mapScale);
    }

    public static CursorLoader createCursorLoader(Context context, long chatId, int numMessages) {
        String chatIdString = String.valueOf(chatId);

        return Message.DAO.getCursorLoader(context,
                COLUMN_CHAT_ID + " = ?",
                new String[]{chatIdString},
                COLUMN_ID + " ASC LIMIT (SELECT COUNT(*) FROM " + TABLE_MESSAGES
                        + " WHERE " + COLUMN_CHAT_ID + " = " + chatIdString + ") - " + numMessages + ", " + numMessages);
    }

    public static CursorLoader createPhotoCursorLoader(Context context, long chatId) {
        String chatIdString = String.valueOf(chatId);
        String chatTypeString = String.valueOf(Message.Type.IMAGE);

        return Message.DAO.getCursorLoader(context,
                COLUMN_CHAT_ID + " = ? AND " + COLUMN_TYPE + " = ?",
                new String[]{chatIdString, chatTypeString},
                COLUMN_ID + " ASC");
    }

    private static String getGoogleMapsApiKey(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return appInfo.metaData.getString("net.stacksmashing.sechat.googleMapsWebApiKey");
        }
        catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private Uri getGoogleMapsApiUri(double latitude, double longitude) {
        return new Uri.Builder().scheme("https")
                .path("//maps.googleapis.com/maps/api/staticmap")
                .appendQueryParameter("key", mapApiKey)
                .appendQueryParameter("center", latitude + "," + longitude)
                .appendQueryParameter("zoom", "15")
                .appendQueryParameter("size", mapWidth + "x" + mapHeight)
                .appendQueryParameter("scale", String.valueOf(mapScale))
                .appendQueryParameter("markers", "|" + latitude + "," + longitude)
                .build();
    }

    public void handleItemClick(View view, int position, @NonNull ChatFragmentMessageActions actionListener) {
        if (view.getTag() != null && view.getTag() instanceof BaseViewHolder) {
            ((BaseViewHolder) view.getTag()).handleClick(messageFromCursor(getItem(position)), actionListener);
        }
    }

    public void setAudioPlayer(AudioPlayerController player) {
        audioPlayerController = player;
    }

    @Override
    public int getItemViewType(int position) {
        final Message message = messageFromCursor(getItem(position));
        return 2 * (message.isDownloading() ? 0 : (message.getType().ordinal() + 1))
                + (message.isIncoming() ? 1 : 0);
    }

    private Message messageFromCursor(Object cursor) {
        return Message.DAO.cursorToObject((Cursor) cursor);
    }

    /* View types 0 and 1 are used for (outgoing and incoming, respectively) in-progress downloads,
     * real types are shifted by 1 (hence the + 1). */
    private static final int TYPE_COUNT = 2 * (Message.Type.COUNT + 1);

    @Override
    public int getViewTypeCount() {
        return TYPE_COUNT;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        Message message = messageFromCursor(cursor);

        BaseViewHolder baseViewHolder = BaseViewHolder.createForMessage(message);
        View view = baseViewHolder.inflate(context, viewGroup);
        view.setTag(baseViewHolder);

        InnerViewHolder innerViewHolder = InnerViewHolder.createForMessage(message);
        if (innerViewHolder != null) {
            innerViewHolder.inflate(context, baseViewHolder.dataContainer);
            baseViewHolder.dataContainer.setTag(innerViewHolder);
        }

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Message message = Message.DAO.cursorToObject(cursor);
        BaseViewHolder baseViewHolder = (BaseViewHolder) view.getTag();

        baseViewHolder.bindData(context, message, this);
    }

    @Override
    public void audioPlayerStateChanged() {
        notifyDataSetChanged();
    }

    static abstract class BaseViewHolder {

        public static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance();
        public static final DateFormat TIME_FORMAT = SimpleDateFormat.getTimeInstance();

        @InjectView(R.id.message_data_container)
        FrameLayout dataContainer;

        @InjectView(R.id.message_time)
        TextView timeText;

        public abstract View inflate(Context context, ViewGroup viewGroup);

        public void bindData(Context context, Message message, MessageArrayAdapter adapter) {
            String dateString = DATE_FORMAT.format(message.getReceivedOn());
            String todayDateString = DATE_FORMAT.format(new Date());
            String timeString = TIME_FORMAT.format(message.getReceivedOn());

            String messageTimestamp = (dateString.equals(todayDateString) ? "" : dateString + " ") + timeString;

            timeText.setText(messageTimestamp);

            InnerViewHolder innerViewHolder = (InnerViewHolder) dataContainer.getTag();
            innerViewHolder.bindData(context, message, adapter);
        }

        public void handleClick(Message message, ChatFragmentMessageActions actionListener) {
            ((InnerViewHolder) dataContainer.getTag()).handleClick(message, actionListener);
        }

        public static BaseViewHolder createForMessage(Message message) {
            return message.isIncoming()
                    ? new IncomingViewHolder()
                    : new OutgoingViewHolder();
        }
    }

    static class IncomingViewHolder extends BaseViewHolder {

        @InjectView(R.id.message_incoming_contact_picture)
        CircleImageView avatarCircleImageView;

        @InjectView(R.id.message_incoming_username)
        TextView usernameText;

        @Override
        public View inflate(Context context, ViewGroup viewGroup) {
            View view = LayoutInflater.from(context).inflate(R.layout.message_incoming, viewGroup, false);
            ButterKnife.inject(this, view);
            return view;
        }

        public void bindData(Context context, Message message, MessageArrayAdapter adapter) {
            Contact contact = Contact.DAO.queryById(context, message.getContactId());

            contact.loadProfilePictureInto(context, avatarCircleImageView);

            if (adapter.chatType.hasMultipleUsers()) {
                usernameText.setVisibility(View.VISIBLE);
                usernameText.setText(contact.getUsername());
            }
            else {
                usernameText.setVisibility(View.GONE);
            }

            super.bindData(context, message, adapter);
        }
    }

    static class OutgoingViewHolder extends BaseViewHolder {

        @InjectView(R.id.message_outgoing_status)
        StatusTextView statusText;

        @Override
        public View inflate(Context context, ViewGroup viewGroup) {
            View view = LayoutInflater.from(context).inflate(R.layout.message_outgoing, viewGroup, false);
            ButterKnife.inject(this, view);
            return view;
        }

        public void bindData(Context context, Message message, MessageArrayAdapter adapter) {
            statusText.setMessageStatus(message.getStatus());
            super.bindData(context, message, adapter);
        }
    }

    private static abstract class InnerViewHolder {
        public abstract void inflate(Context context, ViewGroup viewGroup);

        public abstract void bindData(Context context, Message message, MessageArrayAdapter adapter);

        protected View inflateInner(Context context, ViewGroup viewGroup, int layoutId) {
            return LayoutInflater.from(context).inflate(layoutId, viewGroup, true);
        }

        public void handleClick(Message message, ChatFragmentMessageActions actionListener) {
        }

        private static InnerViewHolder createForMessage(Message message) {
            if (message.isDownloading()) {
                return new DownloadInnerViewHolder();
            }
            switch (message.getType()) {
                case TEXT:
                    return new TextInnerViewHolder();
                case PING:
                    return new PingInnerViewHolder();
                case LOCATION:
                    return new LocationInnerViewHolder();
                case FILE:
                    return new FileInnerViewHolder();
                case IMAGE:
                    return new ImageInnerViewHolder();
                case AUDIO:
                    return new AudioInnerViewHolder();
                case VIDEO:
                    return new VideoInnerViewHolder();
                case CONTACT:
                    return new ContactInnerViewHolder();
            }
            return null;
        }
    }

    static class AudioInnerViewHolder extends InnerViewHolder {
        @InjectView(R.id.message_data_audio_controller)
        AudioPlayerListItem playerControl;

        @Override
        public void inflate(Context context, ViewGroup viewGroup) {
            ButterKnife.inject(this, inflateInner(context, viewGroup, R.layout.message_data_audio));
        }

        @Override
        public void bindData(Context context, Message message, MessageArrayAdapter adapter) {
            playerControl.bindData(context, message, adapter.audioPlayerController);
        }

        @Override
        public void handleClick(Message message, ChatFragmentMessageActions actionListener) {
            playerControl.callOnClick();
        }
    }

    static class VideoInnerViewHolder extends InnerViewHolder {
        @InjectView(R.id.message_data_video_preview)
        ImageView videoFrame;

        @InjectView(R.id.message_data_video_duration)
        TextView duration;

        private static Picasso PICASSO;

        @Override
        public void inflate(Context context, ViewGroup viewGroup) {
            ButterKnife.inject(this, inflateInner(context, viewGroup, R.layout.message_data_video));
        }

        @Override
        public void bindData(Context context, Message message, MessageArrayAdapter adapter) {
            File videoFile = message.getFile(context);

            Uri videoUri = new Uri.Builder().scheme(PicassoVideoFrameRequestHandler.SCHEME)
                    .appendPath(videoFile.getAbsolutePath())
                    .fragment(Long.toString(0))
                    .build();

            if (PICASSO == null) {
                PICASSO = new Picasso.Builder(context)
                        .addRequestHandler(new PicassoVideoFrameRequestHandler())
                        .build();
            }

            PICASSO.load(videoUri)
                    .resize(adapter.previewWidth, adapter.previewHeight)
                    .centerCrop()
                    .into(videoFrame);

            duration.setText(DateUtils.formatElapsedTime(FileDurationCache.INSTANCE.getDuration(videoFile.getAbsolutePath()) / 1000l));
        }

        @Override
        public void handleClick(Message message, ChatFragmentMessageActions actionListener) {
            actionListener.viewVideo(message);
        }
    }

    static class ImageInnerViewHolder extends InnerViewHolder {
        @InjectView(R.id.message_data_image_view)
        ImageView image;

        @Override
        public void inflate(Context context, ViewGroup viewGroup) {
            ButterKnife.inject(this, inflateInner(context, viewGroup, R.layout.message_data_image));
        }

        @Override
        public void bindData(Context context, Message message, MessageArrayAdapter adapter) {
            File imageFile = message.getFile(context);

            Picasso.with(context).load(imageFile)
                    .resize(adapter.previewWidth, adapter.previewHeight)
                    .centerCrop()
                    .into(image);
        }

        @Override
        public void handleClick(Message message, ChatFragmentMessageActions actionListener) {
            actionListener.viewImage(message);
        }
    }

    static class TextInnerViewHolder extends InnerViewHolder {
        @InjectView(R.id.message_data_text_content)
        MessageTextView messageText;

        @Override
        public void inflate(Context context, ViewGroup viewGroup) {
            ButterKnife.inject(this, inflateInner(context, viewGroup, R.layout.message_data_text));
        }

        @Override
        public void bindData(Context context, Message message, MessageArrayAdapter adapter) {
            messageText.setText(message.getContent(), message.getContentAttributes());
            messageText.setTextColor(context.getResources().getColor(message.getContentColor()));
        }
    }

    static class PingInnerViewHolder extends InnerViewHolder {
        @InjectView(R.id.message_data_text_content)
        MessageTextView messageText;

        @Override
        public void inflate(Context context, ViewGroup viewGroup) {
            ButterKnife.inject(this, inflateInner(context, viewGroup, R.layout.message_data_text));
        }

        @Override
        public void bindData(Context context, Message message, MessageArrayAdapter adapter) {
            messageText.setText(context.getString(R.string.ping_message));
            messageText.setTextColor(context.getResources().getColor(message.getContentColor()));
        }
    }

    static class DownloadInnerViewHolder extends InnerViewHolder {
        @InjectView(R.id.message_data_download_label)
        TextView actionText;

        @InjectView(R.id.message_data_download_progress)
        ProgressBar downloadProgress;

        @Override
        public void inflate(Context context, ViewGroup viewGroup) {
            ButterKnife.inject(this, inflateInner(context, viewGroup, R.layout.message_data_download));
        }

        @Override
        public void bindData(Context context, Message message, MessageArrayAdapter adapter) {
            actionText.setTextColor(context.getResources().getColor(message.getContentColor()));
            Download download = message.queryDownload(context);
            if (download != null) {
                downloadProgress.setMax(download.getTotalParts());
                downloadProgress.setProgress(download.getReceivedParts());
            }
        }
    }

    static class LocationInnerViewHolder extends InnerViewHolder {
        @InjectView(R.id.message_data_location_map_picture)
        ImageView mapPicture;

        @Override
        public void inflate(Context context, ViewGroup viewGroup) {
            ButterKnife.inject(this, inflateInner(context, viewGroup, R.layout.message_data_location));
        }

        @Override
        public void bindData(Context context, Message message, MessageArrayAdapter adapter) {
            Location location = message.getLocation();
            if (location != null) {
                Picasso.with(context).load(adapter.getGoogleMapsApiUri(location.getLatitude(), location.getLongitude()))
                        .into(mapPicture);
            }
        }

        @Override
        public void handleClick(Message message, ChatFragmentMessageActions actionListener) {
            actionListener.viewLocation(message);
        }
    }

    static class ContactInnerViewHolder extends InnerViewHolder {
        @InjectView(R.id.message_data_contact_label)
        TextView label;

        @InjectView(R.id.message_data_contact_name)
        TextView contactName;

        @Override
        public void inflate(Context context, ViewGroup viewGroup) {
            ButterKnife.inject(this, inflateInner(context, viewGroup, R.layout.message_data_contact));
        }

        @Override
        public void bindData(Context context, Message message, MessageArrayAdapter adapter) {
            contactName.setText(message.getContent());

            final int color = context.getResources().getColor(message.getContentColor());
            label.setTextColor(color);
            contactName.setTextColor(color);
        }

        @Override
        public void handleClick(Message message, ChatFragmentMessageActions actionListener) {
            actionListener.viewContact(message);
        }
    }

    static class FileInnerViewHolder extends InnerViewHolder {
        // TODO: This is a stub.

        @Override
        public void inflate(Context context, ViewGroup viewGroup) {
            ButterKnife.inject(this, inflateInner(context, viewGroup, R.layout.message_data_file));
        }

        @Override
        public void bindData(Context context, Message message, MessageArrayAdapter adapter) {

        }
    }
}
