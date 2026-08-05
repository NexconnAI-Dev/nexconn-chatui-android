package ai.nexconn.chatui.widget.component;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.BaseComponent;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.picture.tools.ScreenUtils;
import ai.nexconn.chatui.utils.text.TextViewUtils;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.widget.TextViewCompat;

/**
 * Header component for top navigation bar.
 *
 * @since 5.10.4
 */
public class HeadComponent extends BaseComponent {

    private static final int TITLE_SAFE_GAP_DP = 8;

    private LinearLayout leftContainer;
    private LinearLayout rightContainer;
    private View titleContainer;
    private TextView leftTextView;
    private TextView titleTextView;
    private TextView rightTextView;

    private View.OnClickListener onLeftClickListener;
    private View.OnClickListener onTitleClickListener;
    private View.OnClickListener onRightClickListener;
    private int rightTextColorDefault;
    private int rightTextColorDisable;
    private int titleSafeGapPx;
    private int lastTitleMaxWidth = -1;

    public HeadComponent(Context context) {
        super(context);
    }

    public HeadComponent(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HeadComponent(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View onCreateView(
            Context context, LayoutInflater from, @NonNull ViewGroup parent, AttributeSet attrs) {
        // Apply default background only when no external background is set
        if (getBackground() == null) {
            setBackgroundColor(
                    ChatUIThemeManager.getColorFromAttrId(
                            context, R.attr.nc_user_manager_background_color));
        }
        View view = from.inflate(R.layout.nc_head_component, parent, false);
        leftContainer = view.findViewById(R.id.left_container);
        rightContainer = view.findViewById(R.id.right_container);
        titleContainer = view.findViewById(R.id.title_container);
        leftTextView = view.findViewById(R.id.left_text);
        titleTextView = view.findViewById(R.id.title_text);
        rightTextView = view.findViewById(R.id.right_text);
        titleSafeGapPx = ScreenUtils.dip2px(context, TITLE_SAFE_GAP_DP);

        if (attrs != null) {
            TypedArray typedArray = null;
            try {
                typedArray = context.obtainStyledAttributes(attrs, R.styleable.HeadComponent);

                String title = typedArray.getString(R.styleable.HeadComponent_head_title_text);
                String leftText = typedArray.getString(R.styleable.HeadComponent_head_left_text);
                String rightText = typedArray.getString(R.styleable.HeadComponent_head_right_text);
                rightTextColorDefault =
                        typedArray.getColor(
                                R.styleable.HeadComponent_head_right_text_color_default, -1);
                rightTextColorDisable =
                        typedArray.getColor(
                                R.styleable.HeadComponent_head_right_text_color_disable, -1);
                int leftDrawable =
                        typedArray.getResourceId(
                                R.styleable.HeadComponent_head_left_text_drawable, -1);
                int titleDrawable =
                        typedArray.getResourceId(
                                R.styleable.HeadComponent_head_title_text_drawable, -1);
                int rightDrawable =
                        typedArray.getResourceId(
                                R.styleable.HeadComponent_head_right_text_drawable, -1);

                if (title != null) {
                    titleTextView.setText(title);
                }

                if (leftText != null) {
                    leftTextView.setText(leftText);
                }

                if (rightText != null) {
                    rightTextView.setText(rightText);
                    rightTextView.setVisibility(View.VISIBLE);
                }

                if (leftDrawable != -1) {
                    setLeftTextDrawable(leftDrawable);
                }

                if (titleDrawable != -1) {
                    setTitleTextDrawable(titleDrawable);
                }

                if (rightDrawable != -1) {
                    setRightTextDrawable(rightDrawable);
                }
                if (rightTextColorDefault != -1) {
                    rightTextView.setTextColor(rightTextColorDefault);
                }
            } finally {
                if (typedArray != null) {
                    typedArray.recycle();
                }
            }
        }

        // Set click listeners
        leftTextView.setOnClickListener(
                v -> {
                    if (onLeftClickListener != null) {
                        onLeftClickListener.onClick(v);
                    } else {
                        if (getContext() instanceof Activity) {
                            ((Activity) getContext()).finish();
                        }
                    }
                });

        titleTextView.setOnClickListener(
                v -> {
                    if (onTitleClickListener != null) {
                        onTitleClickListener.onClick(v);
                    }
                });

        rightTextView.setOnClickListener(
                v -> {
                    if (onRightClickListener != null) {
                        onRightClickListener.onClick(v);
                    }
                });

        // Enable auto-mirroring for drawables set via drawableStartCompat in XML for RTL support
        TextViewUtils.enableDrawableAutoMirror(leftTextView);
        addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        updateTitleMaxWidth());
        post(this::updateTitleMaxWidth);
        return view;
    }

    /**
     * Set the title text
     *
     * @param title the title text
     */
    public void setTitleText(String title) {
        titleTextView.setText(title);
        requestUpdateTitleMaxWidth();
    }

    public void setTitleText(@StringRes int id) {
        titleTextView.setText(id);
        requestUpdateTitleMaxWidth();
    }

    /**
     * Set the left text
     *
     * @param text the text
     */
    public void setLeftText(String text) {
        leftTextView.setText(text);
        requestUpdateTitleMaxWidth();
    }

    /**
     * Set the right text
     *
     * @param text the text
     */
    public void setRightText(String text) {
        rightTextView.setText(text);
        rightTextView.setVisibility(View.VISIBLE);
        requestUpdateTitleMaxWidth();
    }

    /**
     * Set the left text drawable icon
     *
     * @param resId icon resource ID
     */
    public void setLeftTextDrawable(int resId) {
        Drawable drawable = getResources().getDrawable(resId);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        TextViewCompat.setCompoundDrawablesRelative(leftTextView, drawable, null, null, null);
        leftTextView.setVisibility(View.VISIBLE);
        requestUpdateTitleMaxWidth();
    }

    /**
     * Set the title text drawable icon
     *
     * @param resId icon resource ID
     */
    public void setTitleTextDrawable(int resId) {
        Drawable drawable = getResources().getDrawable(resId);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        TextViewCompat.setCompoundDrawablesRelative(titleTextView, drawable, null, null, null);
        titleTextView.setVisibility(View.VISIBLE);
        requestUpdateTitleMaxWidth();
    }

    /**
     * Set the right text drawable icon
     *
     * @param resId icon resource ID
     */
    public void setRightTextDrawable(int resId) {
        Drawable drawable = getResources().getDrawable(resId);
        drawable.setBounds(
                0, 0, ScreenUtils.dip2px(getContext(), 24), ScreenUtils.dip2px(getContext(), 24));
        TextViewCompat.setCompoundDrawablesRelative(rightTextView, null, null, drawable, null);
        rightTextView.setVisibility(View.VISIBLE);
        requestUpdateTitleMaxWidth();
    }

    /**
     * Set whether the right text view is enabled
     *
     * @param enable whether to enable
     */
    public void setRightTextViewEnable(boolean enable) {
        if (rightTextView != null) {
            rightTextColorDefault =
                    rightTextColorDefault != -1
                            ? rightTextColorDefault
                            : ChatUIThemeManager.getColorFromAttrId(
                                    getContext(), R.attr.nc_primary_color);
            rightTextColorDisable =
                    rightTextColorDisable != -1
                            ? rightTextColorDisable
                            : ChatUIThemeManager.getColorFromAttrId(
                                    getContext(), R.attr.nc_text_secondary_color);
            rightTextView.setTextColor(enable ? rightTextColorDefault : rightTextColorDisable);
            rightTextView.setEnabled(enable);
            rightTextView.setClickable(enable);
        }
    }

    /**
     * Add a view to the right container
     *
     * @param view the view to add
     */
    public void addRightView(View view) {
        if (rightContainer != null) {
            rightContainer.addView(view);
            requestUpdateTitleMaxWidth();
        }
    }

    /**
     * Get the left text view
     *
     * @return the left text view
     */
    public TextView getLeftTextView() {
        return leftTextView;
    }

    /**
     * Get the title text view
     *
     * @return the title text view
     */
    public TextView getTitleTextView() {
        return titleTextView;
    }

    /**
     * Get the right text view
     *
     * @return the right text view
     */
    public TextView getRightTextView() {
        return rightTextView;
    }

    /**
     * Set the left click listener
     *
     * @param listener click listener
     */
    public void setLeftClickListener(View.OnClickListener listener) {
        this.onLeftClickListener = listener;
    }

    /**
     * Set the title click listener
     *
     * @param listener click listener
     */
    public void setTitleClickListener(View.OnClickListener listener) {
        this.onTitleClickListener = listener;
    }

    /**
     * Set the right click listener
     *
     * @param listener click listener
     */
    public void setRightClickListener(View.OnClickListener listener) {
        this.onRightClickListener = listener;
    }

    private void requestUpdateTitleMaxWidth() {
        post(this::updateTitleMaxWidth);
    }

    private void updateTitleMaxWidth() {
        if (titleTextView == null
                || leftContainer == null
                || rightContainer == null
                || titleContainer == null) {
            return;
        }
        int componentWidth = getWidth();
        if (componentWidth <= 0) {
            return;
        }

        int contentWidth = componentWidth - getPaddingStart() - getPaddingEnd();
        int leftWidth = leftContainer.getVisibility() == VISIBLE ? leftContainer.getWidth() : 0;
        int rightWidth = rightContainer.getVisibility() == VISIBLE ? rightContainer.getWidth() : 0;
        int sideReserved = Math.max(leftWidth, rightWidth) + titleSafeGapPx;
        int maxTitleWidth = Math.max(0, contentWidth - sideReserved * 2);
        // Only apply when changed. TextView.setMaxWidth() unconditionally calls
        // requestLayout(); called from onLayoutChange it would loop layout passes
        // and spam "requestLayout() improperly called during layout".
        if (maxTitleWidth == lastTitleMaxWidth) {
            return;
        }
        lastTitleMaxWidth = maxTitleWidth;
        titleTextView.setMaxWidth(maxTitleWidth);
    }
}
