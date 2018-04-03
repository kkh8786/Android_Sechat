package net.stacksmashing.sechat;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class PhotosViewPager extends ViewPager {

    public PhotosViewPager(Context context) {
        super(context);
    }

    public PhotosViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        }
        catch (IllegalArgumentException e) {
            return false;
        }
    }
}
