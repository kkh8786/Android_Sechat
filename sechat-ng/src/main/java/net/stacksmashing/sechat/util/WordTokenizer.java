package net.stacksmashing.sechat.util;

import android.widget.MultiAutoCompleteTextView;

public class WordTokenizer implements MultiAutoCompleteTextView.Tokenizer {
    @Override
    public int findTokenStart(CharSequence text, int cursor) {
        while (cursor > 0 && text.charAt(cursor - 1) != ' ') {
            cursor--;
        }
        return cursor;
    }

    @Override
    public int findTokenEnd(CharSequence text, int cursor) {
        int length = text.length();
        while (cursor < length && text.charAt(cursor) != ' ') {
            cursor++;
        }
        return cursor;
    }

    @Override
    public CharSequence terminateToken(CharSequence text) {
        return text;
    }
}
