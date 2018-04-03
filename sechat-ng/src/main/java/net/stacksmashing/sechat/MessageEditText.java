package net.stacksmashing.sechat;

import android.content.Context;
import android.util.AttributeSet;

import com.commonsware.cwac.richedit.RichEditText;

import net.stacksmashing.sechat.util.TextStyleConverter;

public class MessageEditText extends RichEditText {

    public MessageEditText(Context context) {
        super(context);
    }

    public MessageEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Returns data about message's format as json
     *
     * @return json with format info
     */
    public String getFormatting() {
        return TextStyleConverter.serializeStyle(getText());
    }
}
