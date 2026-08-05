package ai.nexconn.chatui.picture.tools;

import ai.nexconn.chatui.R;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.widget.TextView;
import java.util.regex.Pattern;

public class StringUtils {

    private static final Pattern pattern = Pattern.compile("^[-\\+]?[\\d]+$");

    public static void tempTextFont(TextView tv, int mimeType) {
        String text = tv.getText().toString().trim();
        String str = tv.getContext().getString(R.string.nc_picture_empty_title);
        String sumText = str + text;
        Spannable placeSpan = new SpannableString(sumText);
        placeSpan.setSpan(
                new RelativeSizeSpan(0.8f),
                str.length(),
                sumText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(placeSpan);
    }

    /**
     * Match numeric value
     *
     * @param str
     * @return
     */
    public static int stringToInt(String str) {
        return pattern.matcher(str).matches() ? Integer.valueOf(str) : 0;
    }
}
