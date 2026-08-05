package ai.nexconn.chatui.utils.text;

import ai.nexconn.chatui.utils.system.ExecutorHelper;
import ai.nexconn.chatui.utils.view.RTLUtils;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.Gravity;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.widget.TextViewCompat;

/** Handles text content for TextMessage and ReferenceMessage. */
public class TextViewUtils {

    /** Content length threshold: regex processing runs on a background thread when >= 150 chars. */
    private static final int CONTENT_LIMIT_LENGTH = 150;

    /**
     * @param content the text content
     * @param callBack async callback for regex processing result
     * @return the spannable content
     */
    public static SpannableStringBuilder getSpannable(
            String content, final RegularCallBack callBack) {
        return getSpannable(content, true, callBack);
    }

    /**
     * @param content the text content
     * @param callBack async callback for regex processing result
     * @return the spannable content
     */
    public static SpannableStringBuilder getSpannable(
            String content, boolean regular, final RegularCallBack callBack) {
        if (content == null) {
            return new SpannableStringBuilder("");
        }
        // Handle RTL layout
        String adapterContent = RTLUtils.adapterAitInRTL(content);
        SpannableStringBuilder emojiSpannable = new SpannableStringBuilder(adapterContent);
        if (!regular) {
            return emojiSpannable;
        }
        if (emojiSpannable.length() < CONTENT_LIMIT_LENGTH) {
            regularContent(emojiSpannable);
        } else {
            final SpannableStringBuilder spannableStringBuilder =
                    new SpannableStringBuilder(emojiSpannable);
            ExecutorHelper.getInstance()
                    .compressExecutor()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    regularContent(spannableStringBuilder);
                                    callBack.finish(spannableStringBuilder);
                                }
                            });
        }
        return emojiSpannable;
    }

    /**
     * @param content the text content
     * @param callBack async callback for regex processing result
     * @return the spannable content
     */
    public static SpannableStringBuilder getRichSpannable(
            String content, final RegularCallBack callBack, @ColorInt int foregroundColor) {
        if (content == null) {
            return new SpannableStringBuilder("");
        }
        SpannableStringBuilder emojiSpannable = new SpannableStringBuilder(content);
        emojiSpannable.setSpan(
                new ForegroundColorSpan(foregroundColor),
                0,
                content.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (emojiSpannable.length() < CONTENT_LIMIT_LENGTH) {
            regularContent(emojiSpannable);
        } else {
            ExecutorHelper.getInstance()
                    .compressExecutor()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    regularContent(emojiSpannable);
                                    callBack.finish(emojiSpannable);
                                }
                            });
        }
        return emojiSpannable;
    }

    private static void regularContent(SpannableStringBuilder spannable) {
        Linkify.addLinks(
                spannable, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS);

        // Use custom URL recognition logic to handle all http and https links
        addCustomUrlLinks(spannable);

        // Add custom phone number recognition to ensure detection on all devices
        addCustomPhoneLinks(spannable);

        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        for (URLSpan span : spans) {
            int start = spannable.getSpanStart(span);
            int end = spannable.getSpanEnd(span);
            spannable.removeSpan(span);
            span = new URLSpanUnderlineSameColor(span.getURL());
            if (end < start) {
                continue;
            }
            spannable.setSpan(span, start, end, 0);
        }
    }

    /** Adds custom URL recognition logic to handle all http and https links. */
    private static void addCustomUrlLinks(SpannableStringBuilder spannable) {
        String text = spannable.toString();
        // Match URLs starting with http or https, including URL-encoded characters
        String urlPattern = "(https?://[^\\s]+)";
        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile(
                        urlPattern, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String url = matcher.group(1);
            int start = matcher.start(1);
            int end = matcher.end(1);

            // Strip trailing punctuation from the URL
            while (end > start && isUrlEndChar(text.charAt(end - 1))) {
                end--;
            }

            // Re-extract the cleaned URL
            url = text.substring(start, end);

            // Remove any existing URLSpan at this position
            URLSpan[] existingSpans = spannable.getSpans(start, end, URLSpan.class);
            for (URLSpan existingSpan : existingSpans) {
                spannable.removeSpan(existingSpan);
            }

            // Add a new URLSpan
            URLSpanUnderlineSameColor urlSpan = new URLSpanUnderlineSameColor(url);
            spannable.setSpan(urlSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /** Checks whether the character is a URL-ending character. */
    private static boolean isUrlEndChar(char c) {
        return c == '.' || c == ',' || c == '!' || c == '?' || c == ';' || c == ':' || c == '"'
                || c == '\'' || c == ')';
    }

    /** Adds custom phone number recognition to ensure detection on all devices. */
    private static void addCustomPhoneLinks(SpannableStringBuilder spannable) {
        String text = spannable.toString();
        // Match Chinese mainland mobile numbers: 11 digits starting with 1
        String phonePattern = "(1[3-9]\\d{9})";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(phonePattern);
        java.util.regex.Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String phone = matcher.group(1);
            int start = matcher.start(1);
            int end = matcher.end(1);

            // Check if there is already a URLSpan at this position (avoid duplicates)
            URLSpan[] existingSpans = spannable.getSpans(start, end, URLSpan.class);
            if (existingSpans.length == 0) {
                // Add phone number link
                URLSpanUnderlineSameColor phoneSpan = new URLSpanUnderlineSameColor("tel:" + phone);
                spannable.setSpan(phoneSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public interface RegularCallBack {
        void finish(SpannableStringBuilder spannable);
    }

    /**
     * Custom link style: keeps the underline while preserving the original text color instead of
     * the theme's default link color.
     */
    public static class URLSpanUnderlineSameColor extends URLSpan {
        public URLSpanUnderlineSameColor(String url) {
            super(url);
        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            int originalColor = ds.getColor();
            super.updateDrawState(ds);
            ds.setUnderlineText(true);
            ds.setColor(originalColor);
        }
    }

    /**
     * Sets compound drawables on a TextView using setCompoundDrawablesRelative.
     *
     * @param textView the TextView
     * @param gravity only supports Gravity.START, Gravity.TOP, Gravity.END, or Gravity.BOTTOM
     * @param resId drawable resource ID
     */
    public static void setCompoundDrawables(TextView textView, int gravity, int resId) {
        Drawable drawable = textView.getResources().getDrawable(resId);
        int w = drawable.getIntrinsicWidth();
        drawable.setBounds(0, 0, w, w);
        Drawable[] drawables = textView.getCompoundDrawablesRelative();
        if (Gravity.START == gravity) {
            drawables[0] = drawable;
        } else if (Gravity.TOP == gravity) {
            drawables[1] = drawable;
        } else if (Gravity.END == gravity) {
            drawables[2] = drawable;
        } else if (Gravity.BOTTOM == gravity) {
            drawables[3] = drawable;
        }
        textView.setCompoundDrawablesRelative(
                drawables[0], drawables[1], drawables[2], drawables[3]);
        textView.setCompoundDrawablePadding(w / 2);
    }

    /**
     * Enables auto-mirroring for the TextView's compound Drawables, useful for directional
     * Drawables in RTL layouts.
     *
     * @param textView the TextView
     */
    public static void enableDrawableAutoMirror(TextView textView) {
        if (textView == null) {
            return;
        }
        Drawable[] drawables = TextViewCompat.getCompoundDrawablesRelative(textView);
        if (drawables != null) {
            for (Drawable drawable : drawables) {
                if (drawable != null) {
                    drawable.setAutoMirrored(true);
                }
            }
        }
    }
}
