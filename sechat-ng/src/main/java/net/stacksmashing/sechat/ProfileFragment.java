package net.stacksmashing.sechat;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.network.ClientMessage;
import net.stacksmashing.sechat.network.ClientSetProfilePictureMessage;
import net.stacksmashing.sechat.network.NetworkService;
import net.stacksmashing.sechat.util.QrCodeGenerator;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment implements View.OnClickListener {
    private static final int REQUEST_PICK_PROFILE_PICTURE = 1;

    public static Fragment newInstance() {
        return new ProfileFragment();
    }

    public ProfileFragment() {
    }

    @InjectView(R.id.fragment_profile_username)
    TextView usernameText;
    @InjectView(R.id.fragment_profile_device_name)
    TextView deviceNameText;
    @InjectView(R.id.fragment_profile_change_status)
    Button changeStatusButton;
    @InjectView(R.id.fragment_profile_change_profile_picture)
    Button changePictureButton;
    @InjectView(R.id.fragment_profile_verify_phone_number)
    Button registerPhoneNumberButton;
    @InjectView(R.id.fragment_profile_status_text)
    TextView status;
    @InjectView(R.id.fragment_profile_qr_code)
    ImageView qrCodeView;
    @InjectView(R.id.fragment_profile_circular_avatar)
    CircleImageView avatarImageView;

    //@Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        ButterKnife.inject(this, view);

        usernameText.setText(Preferences.getUsername(getActivity()));
        deviceNameText.setText(Preferences.getDeviceName(getActivity()));
        avatarImageView.setImageDrawable(new ColorDrawable(R.color.sechat_gray));
        changeStatusButton.setOnClickListener(this);
        changePictureButton.setOnClickListener(this);
        registerPhoneNumberButton.setOnClickListener(this);

        new QrCodeGenerator(qrCodeView).execute(Preferences.generateJSONProfileData(getActivity()));

        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.title_profile);
            actionBar.setSubtitle(null);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        status.setText(Preferences.getStatus(getActivity()));
        Preferences.loadProfilePictureInto(getActivity(), avatarImageView);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    @Override
    public void onClick(View view) {
        if (view == changeStatusButton) {
            startActivity(ChangeStatusActivity.intent(getActivity()));
        }
        else if (view == changePictureButton) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_PROFILE_PICTURE);
        }
        else if (view == registerPhoneNumberButton) {
            startActivity(PhoneNumberRegistrationActivity.intent(getActivity()));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK_PROFILE_PICTURE && resultCode == Activity.RESULT_OK) {
            try {
                Preferences.setProfilePicture(getActivity(), getActivity().getContentResolver().openInputStream(data.getData()));

                List<String> users = Contact.getUsernames(getActivity());
                ClientMessage message = new ClientSetProfilePictureMessage(Preferences.getProfilePictureBytes(getActivity()), users);
                NetworkService.getInstance().asyncSend(message);
            }
            catch (Exception e) {
                Log.e("Profile", "Could not read the profile picture", e);
            }
        }
    }
}
