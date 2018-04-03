package net.stacksmashing.sechat;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.db.Message;
import net.stacksmashing.sechat.network.ClientGetChannelsMessage;
import net.stacksmashing.sechat.network.NetworkService;
import net.stacksmashing.sechat.util.StartupUtils;
import net.stacksmashing.sechat.voice.CallHandler;
import net.stacksmashing.sechat.voice.Crypto;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.hdodenhof.circleimageview.CircleImageView;

import static android.text.TextUtils.isEmpty;

public class NavigationActivity extends GoogleApiActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener, NavigationProvider, View.OnClickListener, DrawerLayout.DrawerListener, ChatFragment.OnChatDeleted, SearchView.OnQueryTextListener {

    private static final int LOADER_GROUPS = 0;
    private static final int LOADER_CHANNELS = 1;
    private static final int LOADER_CONTACTS = 2;
    private static final int LOADER_UNREAD_MESSAGES = 3;

    private static final int REQUEST_GROUP_CHAT_USERS = 0;
    private static final int REQUEST_GROUP_CHAT_NAME = 1;
    private static final int REQUEST_CONFERENCE_CALL_USERS = 2;

    private static final String EXTRA_CHAT_ID = "chat_id";
    private static final String EXTRA_CONTACT = "contact";
    private static final String EXTRA_SECRET_CHAT = "secret_chat";
    private static final String EXTRA_CALL = "call";

    public static Intent intent(Context context) {
        return new Intent(context, NavigationActivity.class);
    }

    public static Intent intentWithChatId(Context context, long chatId) {
        Intent intent = intent(context);
        intent.putExtra(EXTRA_CHAT_ID, chatId);
        return intent;
    }

    public static Intent intentForChatWithContact(Context context, Contact contact, boolean secret) {
        Intent intent = intent(context);
        intent.putExtra(EXTRA_CONTACT, contact);
        intent.putExtra(EXTRA_SECRET_CHAT, secret);
        return intent;
    }

    public static Intent intentForCall(Context context) {
        Intent intent = intent(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CALL, true);
        return intent;
    }

    @InjectView(R.id.activity_navigation_drawer)
    DrawerLayout drawer;

    @InjectView(R.id.activity_navigation_content)
    FrameLayout content;

    @InjectView(R.id.activity_navigation_menu_container)
    FrameLayout menuContainer;

    private MenuViewHolder menuViewHolder;

    static class MenuViewHolder {
        @InjectView(R.id.activity_navigation_menu)
        ListView menu;

        @InjectView(R.id.activity_navigation_search_bar)
        SearchView searchBar;

        @InjectView(R.id.activity_navigation_current_call_button)
        Button currentCallButton;

        private MenuViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }

    private View channelsHeader;
    private ImageButton channelAdd;

    private View groupsHeader;
    private ImageButton groupAdd;

    private View contactsHeader;
    private ImageButton contactAdd;

    private View unreadHeader;

    private View profile;
    private TextView profileUsername;
    private TextView profileStatus;
    private CircleImageView profilePicture;

    private View menuButtonsContainer;
    private Button settingsButton;
    private Button conferenceCallButton;
    private Button recentCallsButton;

    private ActionBarDrawerToggle drawerToggle;
    private MultiListAdapter menuAdapter;
    private ChatListAdapter groupAdapter;
    private ChatListAdapter channelAdapter;
    private ContactListAdapter contactAdapter;
    private UnreadListAdapter unreadAdapter;

    private String searchQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (StartupUtils.startRegistrationOrLoginActivity(this)) {
            return;
        }

        setContentView(R.layout.activity_navigation);

        ButterKnife.inject(this);

        Context menuContext = new ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault);

        LayoutInflater menuInflater = getLayoutInflater().cloneInContext(menuContext);
        menuInflater.inflate(R.layout.activity_navigation_menu, menuContainer, true);

        menuViewHolder = new MenuViewHolder(menuContainer);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        drawerToggle = new ActionBarDrawerToggle(this, drawer, R.string.activity_navigation_drawer_open, R.string.activity_navigation_drawer_close);
        drawer.setDrawerListener(this);

        channelsHeader = menuInflater.inflate(R.layout.activity_navigation_channels_header, menuViewHolder.menu, false);
        channelAdd = ButterKnife.findById(channelsHeader, R.id.activity_navigation_channel_add);
        channelAdd.setOnClickListener(this);

        groupsHeader = menuInflater.inflate(R.layout.activity_navigation_groups_header, menuViewHolder.menu, false);
        groupAdd = ButterKnife.findById(groupsHeader, R.id.activity_navigation_group_add);
        groupAdd.setOnClickListener(this);

        contactsHeader = menuInflater.inflate(R.layout.activity_navigation_contacts_header, menuViewHolder.menu, false);
        contactAdd = ButterKnife.findById(contactsHeader, R.id.activity_navigation_contact_add);
        contactAdd.setOnClickListener(this);

        unreadHeader = menuInflater.inflate(R.layout.activity_navigation_unread_header, menuViewHolder.menu, false);

        profile = menuInflater.inflate(R.layout.activity_navigation_profile, menuViewHolder.menu, false);
        profileUsername = ButterKnife.findById(profile, R.id.activity_navigation_profile_username);
        profileStatus = ButterKnife.findById(profile, R.id.activity_navigation_profile_status);
        profilePicture = ButterKnife.findById(profile, R.id.activity_navigation_profile_picture);

        menuButtonsContainer = menuInflater.inflate(R.layout.activity_navigation_menu_buttons, menuViewHolder.menu, false);
        settingsButton = ButterKnife.findById(menuButtonsContainer, R.id.activity_navigation_settings_button);
        settingsButton.setOnClickListener(this);
        conferenceCallButton = ButterKnife.findById(menuButtonsContainer, R.id.activity_navigation_conference_call_button);
        conferenceCallButton.setOnClickListener(this);
        recentCallsButton = ButterKnife.findById(menuButtonsContainer, R.id.activity_navigation_recent_calls_button);
        recentCallsButton.setOnClickListener(this);

        groupAdapter = new ChatListAdapter(menuContext);

        channelAdapter = new ChatListAdapter(menuContext);

        contactAdapter = new ContactListAdapter(menuContext);

        unreadAdapter = new UnreadListAdapter(menuContext);

        menuAdapter = new MultiListAdapter();
        menuAdapter.addView(profile);
        menuAdapter.addView(unreadHeader);
        menuAdapter.addAdapter(unreadAdapter);
        menuAdapter.addView(channelsHeader);
        menuAdapter.addAdapter(channelAdapter);
        menuAdapter.addView(groupsHeader);
        menuAdapter.addAdapter(groupAdapter);
        menuAdapter.addView(contactsHeader);
        menuAdapter.addAdapter(contactAdapter);
        menuAdapter.addView(menuButtonsContainer);

        menuViewHolder.menu.setAdapter(menuAdapter);
        menuViewHolder.menu.setOnItemClickListener(this);

        menuViewHolder.searchBar.setOnQueryTextListener(this);

        menuViewHolder.currentCallButton.setOnClickListener(this);

        getLoaderManager().initLoader(LOADER_GROUPS, null, this);
        getLoaderManager().initLoader(LOADER_CHANNELS, null, this);
        getLoaderManager().initLoader(LOADER_CONTACTS, null, this);
        getLoaderManager().initLoader(LOADER_UNREAD_MESSAGES, null, this);

        updateProfileData();

        if (openChatFromIntent(getIntent())) {
            /* We no longer need the intent, since ChatFragment saves the last chat ID. */
            setIntent(null);
        }
        else {
            final long lastChatId = Preferences.getLastChatId(this);
            if (lastChatId != -1) {
                openChatById(lastChatId);
            }
        }
    }

    private void updateProfileData() {
        profileUsername.setText(Preferences.getUsername(this));
        profileStatus.setText(Preferences.getStatus(this));
        Preferences.loadProfilePictureInto(this, profilePicture);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final String query = searchQuery != null && !searchQuery.isEmpty()
                ? "%" + escapeForSqlLike(searchQuery) + "%"
                : "%";

        if (id == LOADER_GROUPS) {
            return Chat.getOrderedCursorLoader(this, "name LIKE ? ESCAPE '\\' AND type = ?", new String[]{query, Chat.Type.GROUP.name()});
        }
        else if (id == LOADER_CHANNELS) {
            return Chat.getOrderedCursorLoader(this, "name LIKE ? ESCAPE '\\' AND type = ?", new String[]{query, Chat.Type.CHANNEL.name()});
        }
        else if (id == LOADER_CONTACTS) {
            return Contact.getOrderedCursorLoader(this, "username LIKE ? ESCAPE '\\'", new String[]{query});
        }
        else if (id == LOADER_UNREAD_MESSAGES) {
            // FIXME: This is not an overly civilized way to do this...
            return new CursorLoader(this,
                    Message.DAO.getContentUri(),
                    new String[]{
                            "chat_id as _id",
                            "(select name from chats where chats._id = chat_id) as name",
                            "count(*) as count",
                            "sum(highlight) > 0 as highlight"
                    },
                    "name LIKE ? ESCAPE '\\' AND NOT read GROUP BY chat_id",
                    new String[]{query},
                    null);
        }
        return null;
    }

    private static String escapeForSqlLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == LOADER_GROUPS) {
            groupAdapter.swapCursor(data);
            menuAdapter.setViewVisibility(groupsHeader, data.getCount() != 0 || isEmpty(searchQuery));
        }
        else if (loader.getId() == LOADER_CHANNELS) {
            channelAdapter.swapCursor(data);
            menuAdapter.setViewVisibility(channelsHeader, data.getCount() != 0 || isEmpty(searchQuery));
        }
        else if (loader.getId() == LOADER_CONTACTS) {
            contactAdapter.swapCursor(data);
            menuAdapter.setViewVisibility(contactsHeader, data.getCount() != 0 || isEmpty(searchQuery));
        }
        else if (loader.getId() == LOADER_UNREAD_MESSAGES) {
            unreadAdapter.swapCursor(data);
            menuAdapter.setViewVisibility(unreadHeader, data.getCount() != 0);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == LOADER_GROUPS) {
            groupAdapter.swapCursor(null);
        }
        else if (loader.getId() == LOADER_CHANNELS) {
            channelAdapter.swapCursor(null);
        }
        else if (loader.getId() == LOADER_CONTACTS) {
            contactAdapter.swapCursor(null);
        }
        else if (loader.getId() == LOADER_UNREAD_MESSAGES) {
            unreadAdapter.swapCursor(null);
            menuAdapter.setViewVisibility(unreadHeader, false);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view.getTag() != null && view.getTag() instanceof NavigationItem) {
            NavigationItem navigationItem = (NavigationItem) view.getTag();
            navigationItem.onClick(this);
        }
        else if (view == profile) {
            openFragment(ProfileFragment.newInstance());
        }
        drawer.closeDrawers();
    }

    @Override
    public void openChatById(long chatId) {
        openFragment(ChatFragment.newInstanceWithId(chatId));
    }

    @Override
    public void openChatForContact(Contact contact, boolean secret) {
        openFragment(ChatFragment.newInstanceWithContact(contact, secret));
    }

    private void openFragment(Fragment fragment) {
        getFragmentManager().beginTransaction().replace(R.id.activity_navigation_content, fragment).commit();
    }

    @Override
    public void onClick(View v) {
        if (v == contactAdd) {
            startActivity(AddContactActivity.intent(this));
        }
        else if (v == channelAdd) {
            NetworkService.getInstance().asyncSend(new ClientGetChannelsMessage());
        }
        else if (v == groupAdd) {
            startActivityForResult(ContactSelectionActivity.intent(this), REQUEST_GROUP_CHAT_USERS);
        }
        else if (v == settingsButton) {
            startActivity(SettingsActivity.intent(this));
        }
        else if (v == conferenceCallButton) {
            startActivityForResult(ContactSelectionActivity.intent(this), REQUEST_CONFERENCE_CALL_USERS);
        }
        else if (v == recentCallsButton) {
            openFragment(RecentCallListFragment.newInstance());
            drawer.closeDrawers();
        }
        else if (v == menuViewHolder.currentCallButton) {
            openInCallFragment();
            drawer.closeDrawers();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_GROUP_CHAT_USERS || requestCode == REQUEST_CONFERENCE_CALL_USERS) {
            final ArrayList<String> users = data.getStringArrayListExtra(ContactSelectionActivity.EXTRA_USERS);
            if (!users.isEmpty()) {
                if (requestCode == REQUEST_GROUP_CHAT_USERS) {
                    startActivityForResult(GroupChatCreationActivity.intentWithUsers(this, users), REQUEST_GROUP_CHAT_NAME);
                }
                else {
                    CallHandler.INSTANCE.initiateCall(this, users, Crypto.KEY);
                }
            }
            drawer.closeDrawers();
        }
        else {
            if (requestCode == REQUEST_GROUP_CHAT_NAME) {
                openChatById(data.getLongExtra(GroupChatCreationActivity.EXTRA_ID, -1));
            }
        }
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {
        drawerToggle.onDrawerSlide(drawerView, slideOffset);
    }

    @Override
    public void onDrawerOpened(View drawerView) {
        drawerToggle.onDrawerOpened(drawerView);
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        drawerToggle.onDrawerClosed(drawerView);
    }

    @Override
    public void onDrawerStateChanged(int newState) {
        drawerToggle.onDrawerStateChanged(newState);
        if (newState == DrawerLayout.STATE_DRAGGING) {
            updateProfileData();
            InputMethodManager im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(drawer.getWindowToken(), 0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Bus.bus().unregister(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openChatFromIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        StartupUtils.setupHockey(this);
        handleCallStatusChange(new Bus.CallStateChangedEvent(CallHandler.INSTANCE.getState()));
        Bus.bus().register(this);
    }

    private boolean openChatFromIntent(Intent intent) {
        if (intent != null) {
            if (intent.hasExtra(EXTRA_CALL) && intent.getBooleanExtra(EXTRA_CALL, false)) {
                openInCallFragment();
                return true;
            }
            else if (intent.hasExtra(EXTRA_CHAT_ID)) {
                openChatById(intent.getLongExtra(EXTRA_CHAT_ID, -1));
                return true;
            }
            else if (intent.hasExtra(EXTRA_CONTACT)) {
                final Contact contact = (Contact) intent.getSerializableExtra(EXTRA_CONTACT);
                openChatForContact(contact, intent.getBooleanExtra(EXTRA_SECRET_CHAT, false));
                return true;
            }
        }
        return false;
    }

    private Fragment inCallFragment = null;

    private void openInCallFragment() {
        if (inCallFragment == null) {
            inCallFragment = InCallFragment.newInstance();
        }
        openFragment(inCallFragment);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleOnlineStatusUpdateEvent(Bus.OnlineStatusUpdateEvent event) {
        contactAdapter.notifyDataSetChanged();
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleCallStatusChange(Bus.CallStateChangedEvent event) {
        switch (event.getState()) {
            case NONE:
                Fragment fragment = getFragmentManager().findFragmentById(R.id.activity_navigation_content);
                if (fragment == inCallFragment) {
                    getFragmentManager().beginTransaction().replace(R.id.activity_navigation_content, new RecentCallListFragment()).commit();
                    inCallFragment = null;
                }
                menuViewHolder.currentCallButton.setVisibility(View.GONE);
                break;
            default:
                menuViewHolder.currentCallButton.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleChannelList(Bus.ChannelListEvent event) {
        startActivity(ChannelListActivity.intentWithChannelList(this, event.getChannels()));
    }

    @Override
    public void onChatDeleted() {
        /* For now, simply remove whatever fragment was open. */
        Fragment fragment = getFragmentManager().findFragmentById(R.id.activity_navigation_content);
        getFragmentManager().beginTransaction().remove(fragment).commit();
        drawer.openDrawer(GravityCompat.START);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        searchQuery = newText;
        getLoaderManager().restartLoader(LOADER_GROUPS, null, this);
        getLoaderManager().restartLoader(LOADER_CHANNELS, null, this);
        getLoaderManager().restartLoader(LOADER_CONTACTS, null, this);
        getLoaderManager().restartLoader(LOADER_UNREAD_MESSAGES, null, this);

        menuAdapter.setViewVisibility(profile, isEmpty(searchQuery));
        menuAdapter.setViewVisibility(menuButtonsContainer, isEmpty(searchQuery));

        return true;
    }

    public interface NavigationItem {
        void onClick(NavigationProvider provider);
    }

}
