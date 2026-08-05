package ai.nexconn.chatui.utils.view;

import ai.nexconn.chatui.NCChatUI;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utility class for handling RTL (right-to-left) layout mode for certain languages */
public class RTLUtils {

    private static final String AIT = "@";
    private static final Pattern pLetter = Pattern.compile("[a-zA-Z0-9]");
    private static final Pattern pChinese = Pattern.compile("[\u4e00-\u9fa5]");

    public static String adapterAitInRTL(String str) {
        return getRTLCode(str, AIT) + str;
    }

    public static String getRTLCode(String str, String first) {
        if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                != View.LAYOUT_DIRECTION_RTL) {
            return "";
        }
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        // Must prepend "\u200e" (LRM) or "\u200f" (RLM) before '@' to indicate direction; otherwise
        // mixed scripts break under RTL
        if (str.length() < 2) {
            return "";
        }
        String[] splitStr = str.split("");
        if (splitStr.length < 2) {
            return "";
        }
        // Check the first character; skip if empty
        if (!TextUtils.isEmpty(first) && !splitStr[0].equals(first)) {
            return "";
        }
        // Check the second character
        if (isChineseOrLetter(splitStr[1])) {
            return "\u200e";
        } else {
            return "\u200f";
        }
    }

    public static boolean isChineseOrLetter(String txt) {
        // English characters and digits
        Matcher mLetter = pLetter.matcher(txt);
        if (mLetter.matches()) {
            return true;
        }
        // Chinese characters
        Matcher mChinese = pChinese.matcher(txt);
        return mChinese.matches();
    }

    /** Checks whether the current layout direction is RTL */
    public static boolean isRtl(Context c) {
        Context context = c;
        if (context == null) {
            context = NCChatUI.getContext();
        }
        if (context == null) {
            return false;
        }
        return context.getResources().getConfiguration().getLayoutDirection()
                == View.LAYOUT_DIRECTION_RTL;
    }
}
