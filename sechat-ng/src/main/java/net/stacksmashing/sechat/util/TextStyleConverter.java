package net.stacksmashing.sechat.util;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that responsible for conversion TextView formatting from/to json
 */
public class TextStyleConverter {

    public static class TextStyleInfo {
        public CharacterStyle style;
        public int start;
        public int end;
    }

    /**
     * Analyzes text and extracts information about applied style (underline, bold, italic).
     * These data are returned as json text
     *
     * @param styledText
     * @return json with text's style data
     */
    public static String serializeStyle(Editable styledText) {
        JSONArray spansArray = new JSONArray();
        CharacterStyle[] spans = styledText.getSpans(0, styledText.length(), CharacterStyle.class);
        for (CharacterStyle span : spans) {
            try {
                String type;
                if (span instanceof UnderlineSpan) {
                    type = "underline";
                }
                else if (span instanceof StyleSpan) {
                    int style = ((StyleSpan) span).getStyle();
                    if (style == Typeface.BOLD) {
                        type = "bold";
                    }
                    else if (style == Typeface.ITALIC) {
                        type = "italic";
                    }
                    else {
                        continue;
                    }
                }
                else {
                    continue;
                }


                JSONObject jsonSpanInfo = new JSONObject();
                int spanStart = styledText.getSpanStart(span);
                int spanEnd = styledText.getSpanEnd(span);
                jsonSpanInfo.put("start", spanStart);
                jsonSpanInfo.put("length", spanEnd - spanStart);

                JSONObject jsonSpan = new JSONObject();
                jsonSpan.put(type, jsonSpanInfo);

                spansArray.put(jsonSpan);
            }
            catch (JSONException error) {
                // should not happen
            }
        }
        return spansArray.toString();
    }

    /**
     * Converts style info from json format to the list of styles
     *
     * @param jsonStyle
     * @return
     */
    public static List<TextStyleInfo> deserializeStyle(String jsonStyle) {
        List<TextStyleInfo> styles = new ArrayList<>();
        try {
            JSONArray jsonSpans = new JSONArray(jsonStyle);
            for (int i = 0; i < jsonSpans.length(); i++) {

                JSONObject jsonSpan = jsonSpans.getJSONObject(i);
                CharacterStyle style;
                JSONObject jsonSpanInfo;
                if (jsonSpan.optJSONObject("underline") != null) {
                    style = new UnderlineSpan();
                    jsonSpanInfo = jsonSpan.getJSONObject("underline");
                }
                else if (jsonSpan.optJSONObject("bold") != null) {
                    style = new StyleSpan(Typeface.BOLD);
                    jsonSpanInfo = jsonSpan.getJSONObject("bold");
                }
                else if (jsonSpan.optJSONObject("italic") != null) {
                    style = new StyleSpan(Typeface.ITALIC);
                    jsonSpanInfo = jsonSpan.getJSONObject("italic");
                }
                else {
                    continue;
                }
                int start = jsonSpanInfo.getInt("start");
                int length = jsonSpanInfo.getInt("length");
                TextStyleInfo styleInfo = new TextStyleInfo();
                styleInfo.style = style;
                styleInfo.start = start;
                styleInfo.end = start + length;
                styles.add(styleInfo);
            }
        }
        catch (JSONException exception) {
            Log.d("TextStyleConverter", "Failed to decode text style information", exception);
        }
        return styles;
    }
}
