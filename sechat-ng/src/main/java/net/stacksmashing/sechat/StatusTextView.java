package net.stacksmashing.sechat;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import net.stacksmashing.sechat.db.Message;

public class StatusTextView extends TextView {
    public StatusTextView(Context context) {
        super(context);
    }

    public StatusTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StatusTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setMessageStatus(Message.Status status) {
        if (status.getStringResource() != 0) {
            setVisibility(VISIBLE);
            setText(status.getStringResource());
            setTextColor(getResources().getColor(status.getColorResource()));
        }
        else {
            setVisibility(GONE);
        }
    }
}
