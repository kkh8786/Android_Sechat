package net.stacksmashing.sechat;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.cocosw.bottomsheet.BottomSheet;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.db.Download;
import net.stacksmashing.sechat.db.Message;
import net.stacksmashing.sechat.media.AudioPlayer;
import net.stacksmashing.sechat.media.AudioRecorder;
import net.stacksmashing.sechat.network.EndToEndMessage;
import net.stacksmashing.sechat.network.EndToEndMessageStatusMessage;
import net.stacksmashing.sechat.network.EndToEndTypingMessage;
import net.stacksmashing.sechat.util.AdapterRefreshingObserver;
import net.stacksmashing.sechat.util.RuntimeDataHelper;
import net.stacksmashing.sechat.util.WordTokenizer;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;
import it.sephiroth.android.library.widget.HListView;

public class ChatFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener,
        TextWatcher, TextView.OnEditorActionListener, AbsListView.OnScrollListener,
        ChatFragmentMessageActions, AdapterView.OnItemClickListener, DialogInterface.OnClickListener, RadioGroup.OnCheckedChangeListener, PopupMenu.OnMenuItemClickListener {

    @SuppressWarnings("unused")
    private static final String TAG = "ChatFragment";

    private static final int LOAD_MORE_AFTER_ITEM_AT_END = 5;
    private static final int PAGE_SIZE = 20;

    public static final String ARG_CHAT_ID = "ARG_CHAT_ID";
    public static final String ARG_CHAT_CONTACT = "ARG_CHAT_CONTACT";
    public static final String ARG_CHAT_SECRET = "ARG_CHAT_SECRET";

    private static final String STATE_NEW_PICTURE_UUID = "new_picture_uuid";

    public static final int TYPING_STATUS_DELAY = 5000;

    private static final int REQUEST_PICK_IMAGE = 1;
    private static final int REQUEST_CAPTURE_IMAGE = 2;
    private static final int REQUEST_PICK_VIDEO = 3;
    private static final int REQUEST_PICK_CONTACTS = 4;
    private static final int REQUEST_FORWARD_MESSAGES = 5;

    private static final String FILE_AUTHORITY = "net.stacksmashing.sechat.fileProvider";

    private static final int LOADER_MESSAGES = 0;
    private static final int LOADER_PHOTO_THUMBNAILS = 1;
    private static final int LOADER_NICKS = 2;

    public static Fragment newInstanceWithId(long chatId) {
        Fragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_CHAT_ID, chatId);
        fragment.setArguments(args);
        return fragment;
    }

    public static Fragment newInstanceWithContact(Contact contact, boolean secret) {
        Fragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CHAT_CONTACT, contact);
        args.putBoolean(ARG_CHAT_SECRET, secret);
        fragment.setArguments(args);
        return fragment;
    }

    @InjectView(R.id.fragment_chat_message_list)
    ListView messageList;
    @InjectView(R.id.fragment_chat_input)
    MultiAutoCompleteTextView inputField;
    @InjectView(R.id.fragment_chat_send)
    ImageButton sendButton;
    @InjectView(R.id.fragment_chat_audio_record_container)
    View recorderControlContainer;
    @InjectView(R.id.fragment_chat_background)
    ImageView chatBackground;
    @InjectView(R.id.burnMode)
    BurnModeView burnModeView;

    @InjectView(R.id.fragment_chat_camera)
    ImageButton cameraButton;
    @InjectView(R.id.fragment_chat_send_audio)
    ImageButton recordAudioButton;
    @InjectView(R.id.fragment_chat_send_photo)
    ImageButton sendPhotoButton;
    @InjectView(R.id.fragment_chat_send_location)
    ImageButton sendLocationButton;
    @InjectView(R.id.fragment_chat_send_ping)
    ImageButton sendPingButton;
    @InjectView(R.id.fragment_chat_send_contact)
    ImageButton sendContactButton;

    @InjectView(R.id.fragment_chat_audio_cancel)
    ImageButton recordingCancelButton;
    @InjectView(R.id.fragment_chat_audio_stop)
    ImageButton recordingStopButton;
    @InjectView(R.id.fragment_chat_audio_send)
    ImageButton recordingSendButton;
    @InjectView(R.id.fragment_chat_audio_elapsed_time)
    TextView recordingElapsedTime;

    private boolean allowLoadMore = true;
    private boolean loadingMore = false;
    private int numMessages = PAGE_SIZE;
    private AudioRecorder audioRecorder;
    private AudioPlayer audioPlayer;
    private final Set<Integer> markAsRead = new HashSet<>();
    private long typingStatusTime = 0;
    private View typingStatusView;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // The UUID of the picture that we're taking using the camera.
    private String newPictureUuid;

    private final Runnable removeTypingStatus = new Runnable() {
        @Override
        public void run() {
            if (messageList != null && typingStatusView != null && (messageList.getAdapter() instanceof HeaderViewListAdapter)) {
                messageList.removeFooterView(typingStatusView);
            }
        }
    };

    private long recordingDuration = 0;
    private String recordedFileName;

    private final Runnable updateRecordingElapsedTime = new Runnable() {
        @Override
        public void run() {
            if (audioRecorder != null && audioRecorder.isRecording()) {
                String text = DateUtils.formatElapsedTime(recordingDuration);
                recordingElapsedTime.setText(text);
                recordingDuration++;
                handler.postDelayed(this, 1000l);
            }
        }
    };

    private ContentObserver downloadsObserver;

    private MessageArrayAdapter listAdapter;

    private Chat chat = null;

    private PictureThumbnailAdapter thumbnailAdapter;

    private ArrayAdapter<String> nickCompletionAdapter;

    private void startRecording() {
        recordingDuration = 0;
        recordedFileName = null;

        // Hide the keyboard when the recording is started
        ((InputMethodManager) (getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE))).hideSoftInputFromWindow(inputField.getWindowToken(), 0);

        audioRecorder = AudioRecorder.build(getActivity(), Message.getFileForUUID(getActivity(), Message.generateUUID()).getAbsolutePath());
        audioRecorder.start(new AudioRecorder.OnStartListener() {
            @Override
            public void onStarted() {
                audioPlayer.hibernate();
                recordAudioButton.setEnabled(false);
                recorderControlContainer.setVisibility(View.VISIBLE);
                updateRecordingElapsedTime.run();
            }

            @Override
            public void onException(Exception e) {
                Toast.makeText(getActivity(), "Audio recorder error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pauseRecording() {
        recordingStopButton.setVisibility(View.GONE);
        audioRecorder.pause(new AudioRecorder.OnPauseListener() {
            @Override
            public void onPaused(String activeRecordFileName) {
                recordedFileName = activeRecordFileName.substring(activeRecordFileName.lastIndexOf("/") + 1);
                recordingStopButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onException(Exception e) {
                Toast.makeText(getActivity(), "Audio recorder error", Toast.LENGTH_SHORT).show();
                recordingStopButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void resumeRecording() {
        recordingStopButton.setVisibility(View.GONE);
        audioRecorder.start(new AudioRecorder.OnStartListener() {
            @Override
            public void onStarted() {
                recordingStopButton.setVisibility(View.VISIBLE);
                updateRecordingElapsedTime.run();
            }

            @Override
            public void onException(Exception e) {
                Toast.makeText(getActivity(), "Audio recorder error", Toast.LENGTH_SHORT).show();
                recordingStopButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void sendRecording() {
        recordAudioButton.setEnabled(true);
        recorderControlContainer.setVisibility(View.GONE);

        if (audioRecorder.isPaused()) {
            try {
                chat.sendFile(getBurnTime(), recordedFileName, Message.Type.AUDIO, recordedFileName);
            }
            catch (Exception e) {
                Log.d(TAG, "Could not send audio recording", e);
            }
        }
        else if (audioRecorder.isRecording()) {
            audioRecorder.pause(new AudioRecorder.OnPauseListener() {
                @Override
                public void onPaused(String activeRecordFileName) {
                    try {
                        recordedFileName = activeRecordFileName.substring(activeRecordFileName.lastIndexOf("/") + 1);
                        chat.sendFile(getBurnTime(), recordedFileName, Message.Type.AUDIO, recordedFileName);
                    }
                    catch (Exception e) {
                        Log.d(TAG, "Could not send audio recording", e);
                    }
                }

                @Override
                public void onException(Exception e) {
                    Toast.makeText(getActivity(), "Audio recorder error", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void cancelRecording() {
        recordAudioButton.setEnabled(true);
        recorderControlContainer.setVisibility(View.GONE);
        manualStopRecording();
    }

    private void manualStopRecording() {
        handler.removeCallbacks(updateRecordingElapsedTime);
        if (audioRecorder != null && audioRecorder.isRecording()) {
            audioRecorder.pause(new AudioRecorder.OnPauseListener() {
                @Override
                public void onPaused(String activeRecordFileName) {
                }

                @Override
                public void onException(Exception e) {
                }
            });
        }
    }

    @Override
    public void viewLocation(Message message) {
        Location location = message.getLocation();
        if (location != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(String.format(Locale.US, "geo:%f,%f", location.getLatitude(), location.getLongitude())));

            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(intent);
            }
            else {
                Toast.makeText(getActivity(), R.string.fragment_chat_no_maps_app, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void viewVideo(Message message) {
        File file = message.getFile(getActivity());
        Uri uri = FileProvider.getUriForFile(getActivity(), FILE_AUTHORITY, file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "video/*");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    @Override
    public void viewImage(Message message) {
        startActivity(ImageViewActivity.intentWithUri(getActivity(),
                message.getFile(getActivity()).toString()));
    }

    @Override
    public void viewContact(Message message) {
        Contact contact = Contact.findOrCreateContactWithUsername(getActivity(), message.getContent());
        if (contact != null) {
            startActivity(ContactActivity.intentWithContact(getActivity(), contact));
        }
    }

    private void deleteMessage(Message message) {
        int firstVisiblePosition = messageList.getFirstVisiblePosition();
        View child = null;

        for (int i = 0; i < messageList.getChildCount(); i++) {
            if (listAdapter.getItemId(firstVisiblePosition + i) == message.getId()) {
                child = messageList.getChildAt(i);
                break;
            }
        }

        if (child != null) {
            child.startAnimation(createDeletionAnimation(message));
        }
        else {
            Message.DAO.delete(getActivity(), message);
        }
    }

    private Animation createDeletionAnimation(final Message message) {
        Animation animation = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_out_right);
        animation.setDuration(500);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                Message.DAO.delete(getActivity(), message);
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        return animation;
    }

    private void dismissNotification(long id) {
        NotificationManager nm = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel((int) id);
    }

    private void setChatId(long newChatId) {
        if (newChatId == -1) {
            getActivity().finish();
            return;
        }

        chat = Chat.DAO.queryById(getActivity(), newChatId);
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(chat.getName());
            actionBar.setSubtitle(null);
        }

        setChatIsOpen(true);

        Preferences.setLastChatId(getActivity(), newChatId);

        dismissNotification(newChatId);

        receiveOnlineStatusUpdateEvent(null);

        if (chat.isSecret()) {
            ScreenShotNotificationListener.enableScreenshotNotificationListener(getActivity());
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        if (radioGroup == burnModeView) {
            chat.setBurnTime(burnModeView.getBurnTime());
            Chat.DAO.update(getActivity(), chat);
        }
    }

    /* A BroadcastReceiver for screenshot notifications.  The purpose is to inform other chat participants when a screenshot is taken. */
    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean done = intent.getBooleanExtra("done", false);
            Log.d(TAG, "Screenshot taken: " + done);
            if (chat != null && chat.isSecret() && done) {
                sendTextMessage(context.getString(R.string.screenshot_taken), null);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        if (getArguments() != null) {
            if (getArguments().containsKey(ARG_CHAT_ID)) {
                setChatId(getArguments().getLong(ARG_CHAT_ID));
            }
            else if (getArguments().containsKey(ARG_CHAT_CONTACT)) {
                final Contact contact = (Contact) getArguments().get(ARG_CHAT_CONTACT);
                final boolean secret = getArguments().getBoolean(ARG_CHAT_SECRET, false);
                setChatId(Chat.findOrCreateChatIdByContact(getActivity(), contact, secret));
            }
        }

        if (savedInstanceState != null) {
            newPictureUuid = savedInstanceState.getString(STATE_NEW_PICTURE_UUID, null);
        }

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(messageReceiver, new IntentFilter("ChatFragmentMsg"));
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(messageReceiver);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_NEW_PICTURE_UUID, newPictureUuid);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.chat, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (chat != null) {
            menu.findItem(R.id.menu_chat_show_contact).setVisible(chat.getType() == Chat.Type.SINGLE);
            menu.findItem(R.id.menu_chat_show_group_members).setVisible(chat.getType() == Chat.Type.GROUP);
        }
    }

    // This is necessary before 5.0 to (temporarily) let the camera app write the image into our files directory.
    private void grantUriPermission(Intent intent, Uri uri, int flags) {
        List<ResolveInfo> resInfoList = getActivity().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            getActivity().grantUriPermission(resolveInfo.activityInfo.packageName, uri, flags);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_chat_show_contact:
                if (chat.getType() == Chat.Type.SINGLE) {
                    Contact contact = Contact.findContactByUsername(getActivity(), chat.getName());
                    if (contact != null) {
                        startActivity(ContactActivity.intentWithContact(getActivity(), contact));
                    }
                }
                return true;
            case R.id.menu_chat_show_group_members:
                if (chat.getType() == Chat.Type.GROUP) {
                    startActivity(GroupChatDetailActivity.intentWithChat(getActivity(), chat));
                }
                return true;
            case R.id.menu_chat_delete_chat:
                promptForChatDeletion();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendLocationMessage(double latitude, double longitude) {
        Message message = Message.createOutgoingLocationMessage(chat.getId(), getBurnTime(), latitude, longitude);

        chat.storeAndSendMessage(getActivity(), message);
    }

    private void sendPingMessage() {
        Message message = Message.createOutgoingPingMessage(chat.getId(), getBurnTime());

        chat.storeAndSendMessage(getActivity(), message);
    }

    private String generateFilenameForUri(Uri uri) {
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String extension = mimeTypeMap.getExtensionFromMimeType(getActivity().getContentResolver().getType(uri));
        return uri.getLastPathSegment() + "." + extension;
    }

    private void sendImageWithLocalUuid(String filename, String localUuid) {
        startActivity(ImageSenderActivity.intentWithLocalUuid(getActivity(), chat, getBurnTime(), filename, localUuid));
    }

    private void sendImageWithUri(Uri uri) {
        startActivity(ImageSenderActivity.intentWithContentUri(getActivity(), chat, getBurnTime(), generateFilenameForUri(uri), uri));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CAPTURE_IMAGE && newPictureUuid != null) {
            getActivity().revokeUriPermission(Uri.fromFile(Message.getFileForUUID(getActivity(), newPictureUuid)), Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            if (resultCode == Activity.RESULT_OK) {
                sendImageWithLocalUuid(newPictureUuid + ".jpg", newPictureUuid);
            }

            newPictureUuid = null;
        }
        else if (resultCode == Activity.RESULT_OK) {
            if ((requestCode == REQUEST_PICK_IMAGE || requestCode == REQUEST_PICK_VIDEO) && data != null) {
                final Uri uri = data.getData();
                if (uri == null) {
                    Log.e(TAG, "No data in PICK_IMAGE/PICK_VIDEO intent");
                    return;
                }
                if (requestCode == REQUEST_PICK_IMAGE) {
                    sendImageWithUri(uri);
                }
                else {
                    chat.copyAndSendFile(uri, getBurnTime(), generateFilenameForUri(uri), Message.Type.VIDEO, Message.generateUUID());
                }
            }
            else if (requestCode == REQUEST_PICK_CONTACTS && data != null) {
                List<String> usernames = data.getStringArrayListExtra(ContactSelectionActivity.EXTRA_USERS);
                for (String username : usernames) {
                    sendContactMessage(username);
                }
            }
            else if (requestCode == REQUEST_FORWARD_MESSAGES) {
                long[] chatIds = data.getLongArrayExtra(ChatSelectionActivity.EXTRA_CHAT_IDS);
                List messages = (ArrayList) data.getSerializableExtra(ChatSelectionActivity.EXTRA_DATA);
                for (long chatId : chatIds) {
                    Chat chat = Chat.DAO.queryById(getActivity(), chatId);
                    for (Object message : messages) {
                        ((Message) message).forward(getActivity(), chat);
                    }
                }
            }
        }
    }

    private void sendContactMessage(String username) {
        Message message = Message.createOutgoingContactMessage(chat.getId(), getBurnTime(), username);

        chat.storeAndSendMessage(getActivity(), message);
    }

    private void setChatIsOpen(boolean isOpen) {
        if (chat != null && RuntimeDataHelper.getInstance().isChatOpen(chat.getId()) != isOpen) {
            RuntimeDataHelper.getInstance().setChatIsOpen(chat.getId(), isOpen);

            if (!chat.isSecret() && Preferences.shouldSendInChatMessage(getActivity())) {
                EndToEndMessage payload = chat.getInChatMessage(getActivity(), isOpen);
                chat.sendMessage(getActivity(), payload);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Bus.bus().register(this);

        downloadsObserver = new AdapterRefreshingObserver(handler, listAdapter);
        getActivity().getContentResolver().registerContentObserver(Download.DAO.getContentUri(), true, downloadsObserver);

        setChatIsOpen(true);

        /* Do this in onResume so that we pick up a new background picture if it was somehow changed while we were in the background. */
        if (Preferences.hasChatBackgroundPicture(getActivity())) {
            Picasso.with(getActivity()).load(Preferences.getChatBackgroundPictureFile(getActivity()))
                    .error(R.drawable.fragment_chat_message_list_background)
                    .fit()
                    .centerCrop()
                    .into(chatBackground);
        }
        else {
            Drawable drawable;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                drawable = getActivity().getDrawable(R.drawable.fragment_chat_message_list_background);
            }
            else {
                drawable = getResources().getDrawable(R.drawable.fragment_chat_message_list_background);
            }
            chatBackground.setImageDrawable(drawable);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Bus.bus().unregister(this);

        getActivity().getContentResolver().unregisterContentObserver(downloadsObserver);
        downloadsObserver = null;

        setChatIsOpen(false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(LOADER_MESSAGES, null, this);
        getLoaderManager().initLoader(LOADER_NICKS, null, this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        ButterKnife.inject(this, view);

        audioPlayer = new AudioPlayer();

        listAdapter = new MessageArrayAdapter(getActivity(), chat.getType());

        audioPlayer.setCallback(listAdapter);

        listAdapter.setAudioPlayer(audioPlayer);

        messageList.setOnItemClickListener(this);
        messageList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        messageList.setMultiChoiceModeListener(new MessageListMultiChoiceModeListener());
        messageList.setAdapter(listAdapter);
        messageList.setOnScrollListener(this);

        typingStatusView = inflater.inflate(android.R.layout.simple_list_item_1, null, false);

        nickCompletionAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line, new ArrayList<String>());

        inputField.addTextChangedListener(this);
        inputField.setOnEditorActionListener(this);
        inputField.setTokenizer(new WordTokenizer());
        inputField.setAdapter(nickCompletionAdapter);

        cameraButton.setOnClickListener(this);
        sendPhotoButton.setOnClickListener(this);
        recordAudioButton.setOnClickListener(this);
        sendLocationButton.setOnClickListener(this);
        sendPingButton.setOnClickListener(this);
        sendContactButton.setOnClickListener(this);

        recordingCancelButton.setOnClickListener(this);

        recordingStopButton.setOnClickListener(this);

        sendButton.setOnClickListener(this);
        sendButton.setEnabled(inputField.getText().length() != 0);

        recordingSendButton.setOnClickListener(this);

        if (chat.isSecret()) {
            sendButton.setColorFilter(Color.RED);
            recordingSendButton.setColorFilter(Color.RED);
        }

        burnModeView.setBurnTime(chat.getBurnTime());
        burnModeView.setOnCheckedChangeListener(this);
        burnModeView.setVisibility(chat.isSecret() ? View.VISIBLE : View.GONE);

        thumbnailAdapter = new PictureThumbnailAdapter(getActivity());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.reset(this);

        listAdapter.setAudioPlayer(null);

        audioPlayer.setCallback(null);
        audioPlayer.release();

        manualStopRecording();

        typingStatusView = null;

        markAsRead.clear();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        listAdapter.handleItemClick(view, position, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        if (id == LOADER_MESSAGES && chat != null) {
            return MessageArrayAdapter.createCursorLoader(getActivity(), chat.getId(), numMessages);
        }
        else if (id == LOADER_PHOTO_THUMBNAILS) {
            return PictureThumbnailAdapter.createCursorLoader(getActivity());
        }
        else if (id == LOADER_NICKS) {
            return Contact.getOrderedCursorLoaderForChatId(getActivity(), chat.getId());
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor result) {
        if (loader.getId() == LOADER_MESSAGES) {
            swapMessageListCursor(result);
        }
        else if (loader.getId() == LOADER_PHOTO_THUMBNAILS) {
            thumbnailAdapter.swapCursor(result);
        }
        else if (loader.getId() == LOADER_NICKS) {
            nickCompletionAdapter.clear();
            if (result.moveToFirst()) {
                do {
                    nickCompletionAdapter.add("@" + result.getString(result.getColumnIndexOrThrow("username")));
                } while (result.moveToNext());
            }
        }
    }

    private void swapMessageListCursor(Cursor cursor) {
        if (listAdapter == null || messageList == null) {
            return;
        }

        allowLoadMore = cursor.getCount() >= numMessages;

        final Integer previousPosition = loadingMore
                ? (messageList.getFirstVisiblePosition() + cursor.getCount() - listAdapter.getCount())
                : null;
        final Integer top = loadingMore
                ? (messageList.getChildAt(0) != null ? messageList.getChildAt(0).getTop() : 0)
                : null;

        loadingMore = false;

        listAdapter.swapCursor(cursor);

        messageList.postOnAnimation(new Runnable() {
            @Override
            public void run() {
                if (messageList != null && previousPosition != null) {
                    messageList.setSelectionFromTop(previousPosition, top);
                }
            }
        });

        messageList.post(new Runnable() {
            @Override
            public void run() {
                if (messageList != null) {
                    final int firstPosition = messageList.getFirstVisiblePosition();
                    final int lastPosition = messageList.getLastVisiblePosition();
                    for (int i = firstPosition; i <= lastPosition; i++) {
                        markMessageAsRead(messageList, i);
                    }
                }
            }
        });

        removeTypingStatus.run();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == LOADER_MESSAGES) {
            listAdapter.swapCursor(null);
        }
        else if (loader.getId() == LOADER_PHOTO_THUMBNAILS) {
            thumbnailAdapter.swapCursor(null);
        }
        else if (loader.getId() == LOADER_NICKS) {
            nickCompletionAdapter.clear();
        }
    }

    @Override
    public void onClick(View view) {
        if (view == sendButton && inputField.length() > 0) {
            sendMessage();
        }
        else if (view == recordingSendButton) {
            sendRecording();
        }
        else if (view == recordingCancelButton) {
            cancelRecording();
        }
        else if (view == recordingStopButton) {
            if (audioRecorder.isRecording()) {
                pauseRecording();
            }
            else if (audioRecorder.isPaused()) {
                resumeRecording();
            }
        }
        else if (view == recordAudioButton) {
            startRecording();
        }
        else if (view == sendPhotoButton) {
            showMediaSheet();
        }
        else if (view == cameraButton) {
            PopupMenu menu = new PopupMenu(getActivity(), cameraButton);
            menu.inflate(R.menu.chat_camera);
            menu.setOnMenuItemClickListener(this);
            menu.show();
        }
        else if (view == sendLocationButton && getActivity() instanceof LocationSource) {
            Location lastLocation = ((LocationSource) getActivity()).getLastLocation();
            if (lastLocation != null) {
                final double latitude = lastLocation.getLatitude();
                final double longitude = lastLocation.getLongitude();
                sendLocationMessage(latitude, longitude);
            }
            else {
                Toast.makeText(getActivity(), R.string.fragment_chat_no_location, Toast.LENGTH_LONG).show();
            }
        }
        else if (view == sendPingButton) {
            sendPingMessage();
        }
        else if (view == sendContactButton) {
            startActivityForResult(ContactSelectionActivity.intent(getActivity()), REQUEST_PICK_CONTACTS);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_chat_camera_take_picture: {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                newPictureUuid = Message.generateUUID();
                Uri uri = FileProvider.getUriForFile(getActivity(), FILE_AUTHORITY, Message.getFileForUUID(getActivity(), newPictureUuid));
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                grantUriPermission(intent, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
                return true;
            }
            case R.id.menu_chat_camera_take_video: {
                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                startActivityForResult(intent, REQUEST_PICK_VIDEO);
                return true;
            }
        }
        return false;
    }

    private void showMediaSheet() {
        final Dialog bottomSheet = new BottomSheet.Builder(getActivity())
                .sheet(R.menu.chat_media)
                .customView(R.layout.fragment_chat_recent_photos_list)
                .listener(this)
                .show();

        HListView recentPhotosList = (HListView) bottomSheet.findViewById(R.id.fragment_chat_recent_photos);
        recentPhotosList.setAdapter(thumbnailAdapter);
        recentPhotosList.setOnItemClickListener(new it.sephiroth.android.library.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(it.sephiroth.android.library.widget.AdapterView<?> adapterView, View view, int i, long l) {
                sendImageWithUri(Uri.fromFile(new File((String) view.getTag())));
                bottomSheet.dismiss();
            }
        });

        getLoaderManager().restartLoader(LOADER_PHOTO_THUMBNAILS, null, this);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case R.id.menu_chat_send_image: {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_PICK_IMAGE);
                break;
            }
            case R.id.menu_chat_send_video: {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("video/*");
                startActivityForResult(intent, REQUEST_PICK_VIDEO);
                break;
            }
        }
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if (textView == inputField && actionId == EditorInfo.IME_ACTION_SEND) {
            sendMessage();
        }
        return false;
    }

    private void promptForChatDeletion() {
        final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    deleteChat();
                }
            }
        };

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.fragment_chat_delete_prompt)
                .setMessage(R.string.fragment_chat_delete_prompt_body)
                .setPositiveButton(android.R.string.yes, listener)
                .setNegativeButton(android.R.string.no, listener)
                .show();
    }

    private void deleteChat() {
        if (chat != null) {
            Chat.removeChatById(getActivity(), chat.getId());
            if (getActivity() instanceof OnChatDeleted) {
                ((OnChatDeleted) getActivity()).onChatDeleted();
            }
        }
    }

    private long getBurnTime() {
        if (chat.isSecret()) {
            return burnModeView.getBurnTime();
        }
        return 0;
    }

    private void sendMessage() {
        if (inputField.getText().length() != 0 && chat != null) {
            inputField.clearComposingText();
            final String contents = inputField.getText().toString();
            inputField.setText("");

            sendTextMessage(contents, null);
        }
    }

    private void sendTextMessage(String contents, String formatting) {
        Message message = Message.createOutgoingTextMessage(chat.getId(), getBurnTime(), contents, formatting);

        chat.storeAndSendMessage(getActivity(), message);
    }

    private void markMessageAsRead(AbsListView absListView, int position) {
        if (position >= absListView.getCount()) {
            return;
        }

        Message message = Message.DAO.cursorToObject((Cursor) absListView.getItemAtPosition(position));

        if (chat != null && message != null && message.isIncoming() && !message.isRead()) {
            Log.d(TAG, "marking " + position + " as read");
            message.markAsRead(getActivity());

            if (Preferences.shouldSendReadNotification(getActivity())) {
                EndToEndMessage payload = new EndToEndMessageStatusMessage(chat.getEndToEndParameters(getActivity()), message.getUUID(), Message.Status.READ);
                chat.sendMessage(getActivity(), payload);
            }
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
        if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
            InputMethodManager imm =
                    (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(inputField.getWindowToken(), 0);
        }
        if (scrollState == SCROLL_STATE_IDLE) {
            Integer[] positions = markAsRead.toArray(new Integer[markAsRead.size()]);
            Arrays.sort(positions);
            for (Integer position : positions) {
                markMessageAsRead(absListView, position);
            }
            markAsRead.clear();
        }
    }

    private void loadMore() {
        if (allowLoadMore && !listAdapter.isEmpty()) {
            allowLoadMore = false;
            loadingMore = true;
            numMessages += PAGE_SIZE;
            getLoaderManager().restartLoader(LOADER_MESSAGES, null, this).forceLoad();
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (firstVisibleItem < LOAD_MORE_AFTER_ITEM_AT_END) {
            loadMore();
        }
        if (visibleItemCount == 0) {
            return;
        }
        final int lastIndex = firstVisibleItem + visibleItemCount;
        for (int i = firstVisibleItem; i < lastIndex; i++) {
            markAsRead.add(i);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void receiveIsTypingEvent(Bus.IsTypingEvent event) {
        if (chat.getId() == event.getChatId()) {
            ((TextView) typingStatusView.findViewById(android.R.id.text1)).setText(String.format(getString(R.string.fragment_chat_is_typing), event.getUsername()));
            if (messageList.getFooterViewsCount() == 0) {
                messageList.addFooterView(typingStatusView);
            }
            else {
                messageList.removeCallbacks(removeTypingStatus);
            }
            messageList.postDelayed(removeTypingStatus, TYPING_STATUS_DELAY);
        }
    }

    private static final DateFormat LAST_SEEN_DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

    @SuppressWarnings("unused")
    @Subscribe
    public void receiveOnlineStatusUpdateEvent(Bus.OnlineStatusUpdateEvent event) {
        if (chat.getType() == Chat.Type.SINGLE) {
            ActionBar actionBar = getActivity().getActionBar();
            Contact contact = Contact.findContactByUsername(getActivity(), chat.getName());
            if (contact != null && actionBar != null) {
                actionBar.setSubtitle(getLastSeenMessage(contact));
            }
        }
    }

    private CharSequence getLastSeenMessage(Contact contact) {
        if (RuntimeDataHelper.getInstance().getContactStatus(contact) == Contact.Status.ONLINE) {
            return getActivity().getString(R.string.fragment_chat_user_status_online);
        }
        else if (contact.getLastSeen() != null) {
            return getActivity().getString(R.string.fragment_chat_last_seen)
                    + ": " + LAST_SEEN_DATE_FORMAT.format(contact.getLastSeen());
        }
        return "";
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        sendButton.setEnabled(editable.length() > 0);
        final long now = System.currentTimeMillis();
        if (now - typingStatusTime > TYPING_STATUS_DELAY) {
            typingStatusTime = now;
            if (chat != null) {
                Log.d(TAG, "Sending IsTyping");
                EndToEndMessage payload = new EndToEndTypingMessage(chat.getEndToEndParameters(getActivity()));
                chat.sendMessage(getActivity(), payload);
            }
        }
    }

    private class MessageListMultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {

        @Override
        public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {
            actionMode.invalidate();
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            actionMode.setTitle(R.string.fragment_chat_choose_messages);
            actionMode.getMenuInflater().inflate(R.menu.chat_context, menu);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            menu.findItem(R.id.menu_chat_context_recall).setVisible(true);
            for (Message message : getSelectedMessages()) {
                if (!message.canRecall()) {
                    menu.findItem(R.id.menu_chat_context_recall).setVisible(false);
                    break;
                }
            }

            menu.findItem(R.id.menu_chat_context_copy).setVisible(!chat.isSecret());
            menu.findItem(R.id.menu_chat_context_forward).setVisible(!chat.isSecret());

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_chat_context_delete: {
                    for (Message message : getSelectedMessages()) {
                        deleteMessage(message);
                    }
                    actionMode.finish();
                    return true;
                }
                case R.id.menu_chat_context_copy: {
                    StringBuilder builder = new StringBuilder();
                    final boolean withUsernames = messageList.getCheckedItemCount() != 1;

                    for (Message message : getSelectedMessages()) {
                        if (withUsernames) {
                            builder.append(message.getSenderUsername(getActivity())).append(": ");
                        }
                        builder.append(message.getDescription(getActivity())).append('\n');
                    }

                    // Remove the trailing \n
                    if (builder.length() > 0) {
                        builder.setLength(builder.length() - 1);
                    }

                    ClipData clipData = ClipData.newPlainText("Messages", builder.toString());

                    ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setPrimaryClip(clipData);

                    actionMode.finish();
                    return true;
                }
                case R.id.menu_chat_context_recall: {
                    for (Message message : getSelectedMessages()) {
                        message.recall();
                    }
                    actionMode.finish();
                    return true;
                }
                case R.id.menu_chat_context_forward: {
                    startActivityForResult(ChatSelectionActivity.intentWithExclusionAndData(getActivity(), chat.getId(), getSelectedMessages()), REQUEST_FORWARD_MESSAGES);
                    actionMode.finish();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
        }

        private ArrayList<Message> getSelectedMessages() {
            ArrayList<Message> result = new ArrayList<>();
            SparseBooleanArray checked = messageList.getCheckedItemPositions();
            for (int i = 0; i < checked.size(); i++) {
                if (checked.valueAt(i)) {
                    final Message message = Message.DAO.cursorToObject((Cursor) listAdapter.getItem(checked.keyAt(i)));
                    if (message != null) {
                        result.add(message);
                    }
                }
            }
            return result;
        }
    }

    public interface OnChatDeleted {
        void onChatDeleted();
    }
}
