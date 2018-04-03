package net.stacksmashing.sechat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import net.stacksmashing.sechat.db.DatabaseProvider;
import net.stacksmashing.sechat.network.NetworkService;

import java.lang.ref.WeakReference;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class LoginActivity extends Activity implements View.OnClickListener, TextView.OnEditorActionListener {
    @InjectView(R.id.activity_login_password)
    EditText passwordText;
    @InjectView(R.id.activity_login_login)
    Button loginButton;
    @InjectView(R.id.activity_login_error)
    TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        ButterKnife.inject(this);

        loginButton.setOnClickListener(this);
        passwordText.setOnEditorActionListener(this);

        loginButton.setEnabled(false);
        loginButton.setVisibility(View.GONE);
        passwordText.setEnabled(false);
        passwordText.setVisibility(View.GONE);
        doLogin();
    }

    @Override
    public void onClick(View view) {
        if (view == loginButton) {
            doLogin();
        }
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if (textView == passwordText && actionId == 0) {
            doLogin();
            return true;
        }
        return false;
    }

    private void doLogin() {
        setEnabled(false);
        new LoginTask(this).execute(passwordText.getText().toString());
    }

    private void setEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        passwordText.setEnabled(enabled);
    }

    private void loginFinished(boolean success) {
        if (success) {
            finish();
            startActivity(getPackageManager().getLaunchIntentForPackage(getPackageName()));
            startService(new Intent(this, NetworkService.class));
        }
        else {
            setEnabled(true);
            errorText.setVisibility(View.VISIBLE);
        }
    }

    public static Intent intent(Context context) {
        return new Intent(context, LoginActivity.class);
    }

    private static class LoginTask extends AsyncTask<String, Void, Boolean> {
        private final WeakReference<LoginActivity> activityRef;

        LoginTask(LoginActivity loginActivity) {
            activityRef = new WeakReference<>(loginActivity);
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            LoginActivity loginActivity = activityRef.get();
            return loginActivity != null && DatabaseProvider.login(loginActivity, strings[0]);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            LoginActivity loginActivity = activityRef.get();
            if (loginActivity != null) {
                loginActivity.loginFinished(success);
            }
        }
    }
}
