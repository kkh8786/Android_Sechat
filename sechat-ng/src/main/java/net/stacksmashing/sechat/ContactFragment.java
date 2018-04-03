package net.stacksmashing.sechat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.db.Message;
import net.stacksmashing.sechat.util.RuntimeDataHelper;
import net.stacksmashing.sechat.voice.CallHandler;
import net.stacksmashing.sechat.voice.Crypto;

import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.hdodenhof.circleimageview.CircleImageView;

public class ContactFragment extends Fragment implements View.OnClickListener {
    @SuppressWarnings("unused")
    private static final String TAG = "ContactFragment";

    private static final int REQUEST_SCAN_QR = 0;
    private static final int REQUEST_PICK_CHATS = 1;
    private static final int REQUEST_CHANGE_ALIAS = 2;
    private static final int REQUEST_PICK_CONFERENCE_CALL_USERS = 3;
    private static final int REQUEST_PICK_GROUP_CHAT_USERS = 4;
    private static final int REQUEST_GROUP_CHAT_NAME = 5;

    public static final String ARG_CONTACT = "ARG_CONTACT";

    @InjectView(R.id.fragment_contact_circular_avatar)
    CircleImageView circularAvatar;
    @InjectView(R.id.fragment_contact_verified_status)
    ImageView verifiedStatus;
    @InjectView(R.id.fragment_contact_name)
    TextView contactName;
    @InjectView(R.id.fragment_contact_online_status)
    TextView onlineStatus;
    @InjectView(R.id.fragment_contact_alias)
    TextView contactAlias;
    @InjectView(R.id.fragment_contact_status)
    TextView contactStatus;
    @InjectView(R.id.fragment_contact_item_message)
    ViewGroup itemMessage;
    @InjectView(R.id.fragment_contact_item_call)
    ViewGroup itemCall;
    @InjectView(R.id.fragment_contact_item_conference_call)
    ViewGroup itemConferenceCall;
    @InjectView(R.id.fragment_contact_item_photos)
    ViewGroup itemPhotos;
    @InjectView(R.id.fragment_contact_item_documents)
    ViewGroup itemDocuments;
    @InjectView(R.id.fragment_contact_item_message_conversation)
    ViewGroup itemMessageConversation;
    @InjectView(R.id.fragment_contact_item_block_user)
    ViewGroup itemBlockUser;
    @InjectView(R.id.fragment_contact_item_block_checkmark)
    ImageView itemBlockCheckMarker;
    @InjectView(R.id.fragment_contact_item_send_contact)
    ViewGroup itemSendContact;
    @InjectView(R.id.fragment_contact_item_create_group)
    ViewGroup itemCreateGroup;
    @InjectView(R.id.fragment_contact_item_verify)
    ViewGroup itemVerify;

    private Contact contact;
    private long chatId;

    public ContactFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        contact = (Contact) getArguments().get(ARG_CONTACT);
        chatId = Chat.findOrCreateChatIdByContact(getActivity(), contact, false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact, container, false);
        ButterKnife.inject(this, view);

        contactAlias.setOnClickListener(this);
        itemMessage.setOnClickListener(this);
        itemCall.setOnClickListener(this);
        itemConferenceCall.setOnClickListener(this);
        itemPhotos.setOnClickListener(this);
        itemDocuments.setOnClickListener(this);
        itemMessageConversation.setOnClickListener(this);
        itemBlockUser.setOnClickListener(this);
        itemSendContact.setOnClickListener(this);
        itemCreateGroup.setOnClickListener(this);
        itemVerify.setOnClickListener(this);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateView(contact);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_SCAN_QR && data.hasExtra(QRCodeScannerActivity.EXTRA_CONTENT)) {
                String content = data.getStringExtra(QRCodeScannerActivity.EXTRA_CONTENT);

                Contact.QrData qrData = Contact.QrData.fromJSON(content);

                if (qrData != null) {
                    if (contact.isValidQrData(qrData)) {
                        contact.setVerified(true);
                        Contact.DAO.update(getActivity(), contact);
                        updateView(contact);
                    }
                    else {
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.fragment_contact_verification_failed)
                                .setMessage(R.string.fragment_contact_verification_failed_body)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }
                }
            }
            else if (requestCode == REQUEST_PICK_CHATS && data != null) {
                long[] chatIds = data.getLongArrayExtra(ChatSelectionActivity.EXTRA_CHAT_IDS);
                for (long chatId : chatIds) {
                    sendContactMessage(contact, chatId);
                }
            }
            else if (requestCode == REQUEST_CHANGE_ALIAS) {
                contact.setAlias(data.getStringExtra(ChangeAliasActivity.EXTRA_ALIAS));
                Contact.DAO.update(getActivity(), contact);
                updateView(contact);
            }
            else if ((requestCode == REQUEST_PICK_CONFERENCE_CALL_USERS || requestCode == REQUEST_PICK_GROUP_CHAT_USERS) && data != null) {
                List<String> users = data.getStringArrayListExtra(ContactSelectionActivity.EXTRA_USERS);
                if (!users.isEmpty()) {
                    if (requestCode == REQUEST_PICK_CONFERENCE_CALL_USERS) {
                        CallHandler.INSTANCE.initiateCall(getActivity(), users, Crypto.KEY);
                    }
                    else {
                        startActivityForResult(GroupChatCreationActivity.intentWithUsers(getActivity(), users), REQUEST_GROUP_CHAT_NAME);
                    }
                }
            }
            else if (requestCode == REQUEST_GROUP_CHAT_NAME && data != null) {
                long id = data.getLongExtra(GroupChatCreationActivity.EXTRA_ID, -1);
                if (id != -1 && getActivity() instanceof NavigationProvider) {
                    ((NavigationProvider) getActivity()).openChatById(id);
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view == contactAlias) {
            startActivityForResult(ChangeAliasActivity.intent(getActivity()), REQUEST_CHANGE_ALIAS);
        }
        else if (view == itemMessage) {
            if (getActivity() instanceof NavigationProvider) {
                ((NavigationProvider) getActivity()).openChatForContact(contact, false);
            }
        }
        else if (view == itemCall) {
            CallHandler.INSTANCE.initiateCall(getActivity(), Collections.singletonList(contact.getUsername()), Crypto.KEY);
        }
        else if (view == itemMessageConversation) {
            Chat.shareHistoryForId(getActivity(), chatId);
        }
        else if (view == itemBlockUser) {
            contact.setBlocked(!contact.isBlocked());
            Contact.DAO.update(getActivity(), contact);
            itemBlockCheckMarker.setVisibility(contact.isBlocked() ? View.VISIBLE : View.GONE);
        }
        else if (view == itemSendContact) {
            startActivityForResult(ChatSelectionActivity.intentWithExclusionAndData(getActivity(), chatId, null), REQUEST_PICK_CHATS);
        }
        else if (view == itemPhotos) {
            startActivity(PhotosActivity.intentWithChatId(getActivity(), chatId));
        }
        else if (view == itemConferenceCall || view == itemCreateGroup) {
            Intent intent = ContactSelectionActivity.intentWithInitialSelection(getActivity(), Collections.singletonList(contact.getId()));

            if (view == itemConferenceCall) {
                startActivityForResult(intent, REQUEST_PICK_CONFERENCE_CALL_USERS);
            }
            else {
                startActivityForResult(intent, REQUEST_PICK_GROUP_CHAT_USERS);
            }
        }
        else if (view == itemVerify) {
            startActivityForResult(QRCodeScannerActivity.intent(getActivity()), REQUEST_SCAN_QR);
        }
    }

    private void updateView(Contact contact) {
        contact.loadProfilePictureInto(getActivity(), circularAvatar);
        verifiedStatus.setVisibility(contact.isVerified() ? View.VISIBLE : View.GONE);
        itemVerify.setVisibility(contact.isVerified() ? View.GONE : View.VISIBLE);
        contactName.setText(contact.getUsername());
        if (contact.getAlias() == null || contact.getAlias().isEmpty()) {
            contactAlias.setText(getActivity().getString(R.string.fragment_contact_no_alias));
        }
        else {
            contactAlias.setText(contact.getAlias());
        }
        if (contact.getStatus() == null || contact.getStatus().isEmpty()) {
            contactStatus.setText(getActivity().getString(R.string.fragment_contact_no_status));
        }
        else {
            contactStatus.setText(contact.getStatus());
        }
        int colorRes = RuntimeDataHelper.getInstance().getContactStatus(contact).getColor();
        onlineStatus.setTextColor(getActivity().getResources().getColor(colorRes));

        itemBlockCheckMarker.setVisibility(contact.isBlocked() ? View.VISIBLE : View.GONE);
    }

    private void sendContactMessage(Contact contact, long chatId) {
        Chat chat = Chat.DAO.queryById(getActivity(), chatId);
        Message message = Message.createOutgoingContactMessage(chat.getId(), 0, contact.getUsername());
        chat.storeAndSendMessage(getActivity(), message);
    }

}

