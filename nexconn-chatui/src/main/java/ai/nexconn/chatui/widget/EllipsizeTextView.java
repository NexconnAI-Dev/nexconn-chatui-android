package ai.nexconn.chatui.widget;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.utils.log.RLog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.util.AttributeSet;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A TextView that supports multi-line middle ellipsis and ignores auto line-break behavior */
public class EllipsizeTextView extends TextView {
    private static final String TAG = "EllipsizeTextView";
    private static final String DEFAULT_ELLIPSIZE_TEXT = "...";

    private CharSequence mEllipsizeText;
    private CharSequence mOriginText;

    private int mEllipsizeIndex;
    private int mMaxLines;

    private boolean mIsExactlyMode;
    private boolean mEnableUpdateOriginText = true;

    public EllipsizeTextView(Context context) {
        this(context, null);
    }

    public EllipsizeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.EllipsizeTextView);
        mEllipsizeIndex = ta.getInt(R.styleable.EllipsizeTextView_NCEllipsizeIndex, 0);
        mEllipsizeText = ta.getText(R.styleable.EllipsizeTextView_NCEllipsizeText);

        if (mEllipsizeText == null) {
            mEllipsizeText = DEFAULT_ELLIPSIZE_TEXT;
        }
        ta.recycle();
    }

    @Override
    public void setMaxLines(int maxLines) {
        if (mMaxLines != maxLines) {
            super.setMaxLines(maxLines);
            this.mMaxLines = maxLines;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        try {
            mIsExactlyMode = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY;
            final Layout layout = getLayout();
            if (layout != null) {
                if (isExceedMaxLine(layout) || isOutOfBounds(layout)) {
                    adjustEllipsizeEndText(layout);
                }
            }
        } catch (Exception e) {
            RLog.d(TAG, "onMeasure:" + e);
        }
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (mEnableUpdateOriginText) {
            mOriginText = text;
        }

        super.setText(text, type);

        if (mIsExactlyMode) {
            requestLayout();
        }
    }

    private boolean isExceedMaxLine(Layout layout) {
        return layout.getLineCount() > mMaxLines && mMaxLines > 0;
    }

    private boolean isOutOfBounds(Layout layout) {
        return layout.getHeight() > getMeasuredHeight() - getPaddingBottom() - getPaddingTop();
    }

    private void adjustEllipsizeEndText(Layout layout) {
        final CharSequence originText = mOriginText;
        final CharSequence restSuffixText =
                originText.subSequence(originText.length() - mEllipsizeIndex, originText.length());

        final int width = layout.getWidth() - getPaddingLeft() - getPaddingRight();
        final int maxLineCount = Math.max(1, computeMaxLineCount(layout));
        final int lastLineWidth = (int) layout.getLineWidth(maxLineCount - 1);
        final int mLastCharacterIndex = layout.getLineEnd(maxLineCount - 1);

        final int suffixWidth =
                (int)
                                (Layout.getDesiredWidth(mEllipsizeText, getPaint())
                                        + Layout.getDesiredWidth(restSuffixText, getPaint()))
                        + 1;

        mEnableUpdateOriginText = false;
        if (lastLineWidth + suffixWidth > width) {
            final int widthDiff = lastLineWidth + suffixWidth - width;

            final int removedCharacterCount =
                    computeRemovedEllipsizeEndCharacterCount(
                            widthDiff, originText.subSequence(0, mLastCharacterIndex));

            setText(originText.subSequence(0, mLastCharacterIndex - removedCharacterCount));
            append(mEllipsizeText);
            append(restSuffixText);
        } else {
            setText(originText.subSequence(0, mLastCharacterIndex));
            append(mEllipsizeText);
            append(restSuffixText);
        }

        mEnableUpdateOriginText = true;
    }

    private int computeMaxLineCount(Layout layout) {
        int availableHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        for (int i = 0; i < layout.getLineCount(); i++) {
            if (availableHeight < layout.getLineBottom(i)) {
                return i;
            }
        }

        return layout.getLineCount();
    }

    // Remove excess characters
    private int computeRemovedEllipsizeEndCharacterCount(
            final int widthDiff, final CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return 0;
        }

        final List<Range<Integer>> characterStyleRanges = computeCharacterStyleRanges(text);
        final String textStr = text.toString();

        int characterIndex = text.length();
        int codePointIndex = textStr.codePointCount(0, text.length());
        int currentRemovedWidth = 0;

        while (codePointIndex > 0 && widthDiff > currentRemovedWidth) {
            codePointIndex--;
            characterIndex = textStr.offsetByCodePoints(0, codePointIndex);

            Range<Integer> characterStyleRange =
                    computeCharacterStyleRange(characterStyleRanges, characterIndex);
            if (characterStyleRange != null) {
                characterIndex = characterStyleRange.getLower();
                codePointIndex = textStr.codePointCount(0, characterIndex);
            }

            currentRemovedWidth =
                    (int)
                            Layout.getDesiredWidth(
                                    text.subSequence(characterIndex, text.length()), getPaint());
        }

        return text.length() - textStr.offsetByCodePoints(0, codePointIndex);
    }

    private Range<Integer> computeCharacterStyleRange(
            List<Range<Integer>> characterStyleRanges, int index) {
        if (characterStyleRanges == null || characterStyleRanges.isEmpty()) {
            return null;
        }

        for (Range<Integer> characterStyleRange : characterStyleRanges) {
            if (characterStyleRange.contains(index)) {
                return characterStyleRange;
            }
        }

        return null;
    }

    private List<Range<Integer>> computeCharacterStyleRanges(CharSequence text) {
        final SpannableStringBuilder ssb = SpannableStringBuilder.valueOf(text);
        final CharacterStyle[] characterStyles =
                ssb.getSpans(0, ssb.length(), CharacterStyle.class);

        if (characterStyles == null || characterStyles.length == 0) {
            return Collections.EMPTY_LIST;
        }

        List<Range<Integer>> ranges = new ArrayList<>();
        for (CharacterStyle characterStyle : characterStyles) {
            ranges.add(
                    new Range<>(ssb.getSpanStart(characterStyle), ssb.getSpanEnd(characterStyle)));
        }

        return ranges;
    }

    /**
     * @param ellipsizeText the ellipsis indicator text
     * @param ellipsizeIndex the number of characters from the end where ellipsis begins
     */
    public void setEllipsizeText(CharSequence ellipsizeText, int ellipsizeIndex) {
        this.mEllipsizeText = ellipsizeText;
        this.mEllipsizeIndex = ellipsizeIndex;
    }

    public static final class Range<T extends Comparable<? super T>> {

        private final T mLower;
        private final T mUpper;

        public Range(final T lower, final T upper) {
            mLower = lower;
            mUpper = upper;

            if (lower.compareTo(upper) > 0) {
                throw new IllegalArgumentException("lower must be less than or equal to upper");
            }
        }

        public T getLower() {
            return mLower;
        }

        public T getUpper() {
            return mUpper;
        }

        public boolean contains(T value) {

            boolean gteLower = value.compareTo(mLower) >= 0;
            boolean lteUpper = value.compareTo(mUpper) < 0;

            return gteLower && lteUpper;
        }
    }

    /**
     * Set the text content of this TextView without automatic line-break adaptation.
     *
     * @param text the text to set
     */
    public void setAdaptiveText(final String text) {
        post(
                new Runnable() {
                    @Override
                    public void run() {
                        setText(text);
                        setText(adaptiveText(EllipsizeTextView.this));
                    }
                });
    }

    // Width must be an exact value for width-based line wrapping to work correctly
    private String adaptiveText(final TextView textView) {
        final String originalText = textView.getText().toString(); // original text
        final Paint tvPaint = textView.getPaint(); // get the TextView's Paint
        final float tvWidth =
                textView.getWidth()
                        - textView.getPaddingLeft()
                        - textView.getPaddingRight(); // available width of the TextView
        int enterCount = 0;
        /*
        Track whether the current character is the first character of a line.
        If a single first character already exceeds the available width, return
        the original text to avoid an infinite line-breaking loop.
         */
        boolean isFirstCharInLine = true;
        // Split the original text into lines
        String[] originalTextLines = originalText.replace("\r", "").split("\n");
        StringBuilder newTextBuilder = new StringBuilder();
        for (String originalTextLine : originalTextLines) {
            // Text fits within the TextView width; no wrapping needed
            if (tvPaint.measureText(originalTextLine) <= tvWidth) {
                newTextBuilder.append(originalTextLine);
            } else {
                // If the line exceeds the available width, measure by character and wrap manually
                float lineWidth = 0;
                for (int i = 0; i != originalTextLine.length(); ++i) {
                    char charAt = originalTextLine.charAt(i);
                    lineWidth += tvPaint.measureText(String.valueOf(charAt));
                    if (isFirstCharInLine && lineWidth > tvWidth) {
                        return originalText;
                    } else if (lineWidth <= tvWidth) {
                        newTextBuilder.append(charAt);
                        isFirstCharInLine = false;
                    } else {
                        // Line exceeds the available width and line count is under the max
                        if (enterCount < getMaxLines() - 1) {
                            newTextBuilder.append("\n");
                            ++enterCount;
                            isFirstCharInLine = true;
                        }
                        lineWidth = 0;
                        --i; // Roll back the loop index to re-evaluate this character on the new
                        // line
                    }
                }
            }
        }
        return newTextBuilder.toString();
    }
}
