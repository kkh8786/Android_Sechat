package net.stacksmashing.sechat;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class CheckableLinearLayout extends LinearLayout implements Checkable {
    private Checkable checkable;

    public CheckableLinearLayout(Context context) {
        super(context);
    }

    public CheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckableLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        /* Find the first Checkable child view.  Not universal, but good enough for now. */
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof Checkable) {
                checkable = (Checkable) child;
                break;
            }
        }
    }

    @Override
    public void setChecked(boolean b) {
        if (checkable != null) {
            checkable.setChecked(b);
        }
    }

    @Override
    public boolean isChecked() {
        return checkable != null && checkable.isChecked();
    }

    @Override
    public void toggle() {
        if (checkable != null) {
            checkable.toggle();
        }
    }
}
