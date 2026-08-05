package ai.nexconn.chatui.utils.text;

import android.text.TextUtils;

/** String utility class. */
public class StringUtils {
    private static final String SEPARATOR = "#@6NC_CLOUD9@#";

    public static String getKey(String arg1, String arg2) {
        return arg1 + SEPARATOR + arg2;
    }

    public static String getArg1(String key) {
        String arg = null;
        if (key.contains(SEPARATOR)) {
            int index = key.indexOf(SEPARATOR);
            arg = key.substring(0, index);
        }
        return arg;
    }

    public static String getArg2(String key) {
        String arg = null;
        if (key.contains(SEPARATOR)) {
            int index = key.indexOf(SEPARATOR) + SEPARATOR.length();
            arg = key.substring(index);
        }
        return arg;
    }

    public static String getStringNoBlank(String str) {
        if (!TextUtils.isEmpty(str)) {
            return str.replaceAll("\\s", " ");
        } else {
            return str;
        }
    }

    /**
     * Gets the Pinyin initial of a character. Returns the uppercase Pinyin initial for Chinese
     * characters, or the uppercase form of the character itself otherwise.
     *
     * @param c the character
     * @return the uppercase Pinyin initial or the uppercase character
     */
    public static char getFirstChar(char c) {
        if (c >= 'a' && c <= 'z') {
            return (char) (c - 'a' + 'A');
        }
        if (c >= 'A' && c <= 'Z') {
            return c;
        }

        String convert = CharacterParser.getInstance().convert(String.valueOf(c));
        if (convert == null || convert.isEmpty()) {
            return c;
        }
        c = convert.charAt(0);

        if (c >= 'a' && c <= 'z') {
            return (char) (c - 'a' + 'A');
        }
        if (c >= 'A' && c <= 'Z') {
            return c;
        }

        // Non-Chinese character, return uppercase '#'
        return Character.toUpperCase('#');
    }
}
