package net.stacksmashing.sechat;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.AttributeSet;
import android.widget.TextView;

import net.stacksmashing.sechat.util.TextStyleConverter;

import java.util.List;

public class MessageTextView extends TextView {

    public MessageTextView(Context context) {
        super(context);
    }

    public MessageTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets the string value of the MessageTextView with specified formatting.
     *
     * @param plainText
     * @param formatting data about formatting in json format
     */
    public void setText(String plainText, String formatting) {
        Spannable formattedText = new SpannableString(plainText);

        if (formatting != null && !formatting.isEmpty()) {
            List<TextStyleConverter.TextStyleInfo> styles = TextStyleConverter.deserializeStyle(formatting);

            for (TextStyleConverter.TextStyleInfo styleInfo : styles) {
                formattedText.setSpan(styleInfo.style, styleInfo.start, styleInfo.end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        setText(formattedText);
    }
}
