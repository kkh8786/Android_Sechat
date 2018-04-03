package net.stacksmashing.sechat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import net.stacksmashing.sechat.db.Contact;

public class ContactActivity extends Activity implements NavigationProvider {
    public static Intent intentWithContact(Context context, @NonNull Contact contact) {
        Intent intent = new Intent(context, ContactActivity.class);
        intent.putExtra(ContactFragment.ARG_CONTACT, contact);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            ContactFragment fragment = new ContactFragment();
            fragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
        }
    }

    @Override
    public void openChatById(long id) {
        finish();
        startActivity(NavigationActivity.intentWithChatId(this, id));
    }

    @Override
    public void openChatForContact(Contact contact, boolean secret) {
        finish();
        startActivity(NavigationActivity.intentForChatWithContact(this, contact, secret));
    }
}
