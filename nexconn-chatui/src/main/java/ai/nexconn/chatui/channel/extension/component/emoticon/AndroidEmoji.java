package ai.nexconn.chatui.channel.extension.component.emoticon;

import ai.nexconn.chatui.utils.log.RLog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidEmoji {
    private static final String TAG = "AndroidEmoji";
    private static Map<Integer, EmojiInfo> sEmojiMap;
    private static Map<Integer, String> replaceEmojiMap;
    private static List<EmojiInfo> sEmojiList;

    public static void init(Context context) {
        sEmojiMap = new HashMap<>();
        sEmojiList = new ArrayList<>();
        int[] codes =
                context.getResources()
                        .getIntArray(
                                context.getResources()
                                        .getIdentifier(
                                                "nc_emoji_code",
                                                "array",
                                                context.getPackageName()));
        TypedArray array =
                context.getResources()
                        .obtainTypedArray(
                                context.getResources()
                                        .getIdentifier(
                                                "nc_emoji_res", "array", context.getPackageName()));
        TypedArray strArray =
                context.getResources()
                        .obtainTypedArray(
                                context.getResources()
                                        .getIdentifier(
                                                "nc_emoji_description",
                                                "array",
                                                context.getPackageName()));
        if (codes.length != array.length()) {
            throw new RuntimeException("Emoji resource init fail.");
        }

        int i = -1;
        while (++i < codes.length) {
            EmojiInfo emoji =
                    new EmojiInfo(
                            codes[i], array.getResourceId(i, -1), strArray.getResourceId(i, -1));
            sEmojiMap.put(codes[i], emoji);
            sEmojiList.add(emoji);
        }
        initReplaceEmojiMap();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        array.recycle();
    }

    // The following emojis display incorrectly on the iOS combined-forward page; use the emoji
    // chars from this map instead
    private static void initReplaceEmojiMap() {
        if (replaceEmojiMap == null) {
            replaceEmojiMap = new HashMap<>();
        }
        replaceEmojiMap.put(0x2601, "☁️");
        replaceEmojiMap.put(0x263a, "☺️");
        replaceEmojiMap.put(0x2764, "❤️");
        replaceEmojiMap.put(0x26a1, "⚡️");
        replaceEmojiMap.put(0x2600, "☀️");
        replaceEmojiMap.put(0x2744, "❄️");
        replaceEmojiMap.put(0x2614, "☔️");
        replaceEmojiMap.put(0x270c, "✌️");
        replaceEmojiMap.put(0x261d, "☝️");
        replaceEmojiMap.put(0x2615, "☕️");
        replaceEmojiMap.put(0x270f, "✏️");
    }

    /**
     * Gets the Unicode string representation of an emoji for consistent display.
     *
     * @param index Emoji index.
     * @return Unicode string of the emoji.
     */
    static String getEmojiString(int index) {
        if (index >= 0 && sEmojiList != null && index < sEmojiList.size()) {
            EmojiInfo emoji = sEmojiList.get(index);
            int code = emoji.code;

            // Prefer using strings from the replacement map (for iOS compatibility)
            if (replaceEmojiMap != null && replaceEmojiMap.containsKey(code)) {
                return replaceEmojiMap.get(code);
            }

            // Convert Unicode code point to string
            char[] chars = Character.toChars(code);
            StringBuilder result = new StringBuilder(Character.toString(chars[0]));
            for (int i = 1; i < chars.length; i++) {
                result.append(chars[i]);
            }
            return result.toString();
        }
        RLog.e(TAG, "getEmojiString sEmojiList IndexOutOfBounds");
        return "";
    }

    public static List<EmojiInfo> getEmojiList() {
        return sEmojiList;
    }

    public static int getEmojiSize() {
        return sEmojiMap != null ? sEmojiMap.size() : 0;
    }

    public static int getEmojiCode(int index) {
        if (index >= 0 && sEmojiList != null && index < sEmojiList.size()) {
            EmojiInfo info = sEmojiList.get(index);
            return info.code;
        }
        RLog.e(TAG, "getEmojiCode sEmojiList IndexOutOfBounds");
        return 0;
    }

    public static Drawable getEmojiDrawable(Context context, int index) {
        Drawable drawable = null;
        if (index >= 0 && sEmojiList != null && index < sEmojiList.size()) {
            EmojiInfo emoji = sEmojiList.get(index);
            drawable = context.getResources().getDrawable(emoji.resId);
        }
        return drawable;
    }

    private static class EmojiInfo {
        public EmojiInfo(int code, int resId) {
            this.code = code;
            this.resId = resId;
        }

        public EmojiInfo(int code, int resId, int strId) {
            this.code = code;
            this.resId = resId;
            this.strId = strId;
        }

        int code;
        int resId;
        int strId;
    }
}
