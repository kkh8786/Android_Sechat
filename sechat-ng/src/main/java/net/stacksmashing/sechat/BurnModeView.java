package net.stacksmashing.sechat;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.widget.RadioGroup;

public class BurnModeView extends RadioGroup {

    private static final int[] RADIO_BUTTONS_IDS = new int[]{
            R.id.burn_mode_1, R.id.burn_mode_2,
            R.id.burn_mode_3, R.id.burn_mode_4,
            R.id.burn_mode_5, R.id.burn_mode_6,
            R.id.burn_mode_off
    };

    private static final int[] BURN_TIME = new int[]{
            30,
            60,
            5 * 60,
            60 * 60,
            24 * 60 * 60,
            7 * 24 * 60 * 60,
            0
    };

    private static final SparseIntArray BURN_BUTTON_TIMES = new SparseIntArray();
    private static final SparseIntArray BURN_BUTTON_IDS = new SparseIntArray();

    static {
        for (int i = 0; i < RADIO_BUTTONS_IDS.length; i++) {
            BURN_BUTTON_TIMES.put(RADIO_BUTTONS_IDS[i], BURN_TIME[i]);
            BURN_BUTTON_IDS.put(BURN_TIME[i], RADIO_BUTTONS_IDS[i]);
        }
    }

    public BurnModeView(Context context) {
        super(context);
    }

    public BurnModeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public int getBurnTime() {
        return BURN_BUTTON_TIMES.get(getCheckedRadioButtonId());
    }

    public void setBurnTime(int burnTime) {
        check(BURN_BUTTON_IDS.get(burnTime, BURN_BUTTON_IDS.get(0)));
    }
}
