package ai.nexconn.chatui.widget;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.widget.switchbutton.SwitchButton;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SettingItemView extends LinearLayout {
    private ImageView ivTagImage;
    private SwitchButton sbSwitch;
    private TextView tvValue;
    private TextView tvContent;
    private ImageView ivImage;
    private ImageView ivSelectImage;
    private boolean isShowSelected = false;
    private ImageView ivRightImage;

    private CompoundButton.OnCheckedChangeListener checkedListener;

    public SettingItemView(Context context) {
        super(context);
        init(null);
    }

    public SettingItemView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SettingItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {

        View view = View.inflate(getContext(), R.layout.nc_widget_setting_item, this);
        ivImage = findViewById(R.id.iv_image);
        tvContent = findViewById(R.id.tv_content);
        tvValue = findViewById(R.id.tv_value);
        View vDivider = findViewById(R.id.v_divider);
        ivTagImage = findViewById(R.id.iv_tag_image);
        sbSwitch = findViewById(R.id.sb_switch);
        ivSelectImage = findViewById(R.id.iv_select_image);
        ivRightImage = findViewById(R.id.iv_right_image);

        ivImage.setVisibility(GONE);
        tvValue.setVisibility(GONE);
        ivTagImage.setVisibility(GONE);
        sbSwitch.setVisibility(GONE);
        vDivider.setVisibility(GONE);
        ivSelectImage.setVisibility(GONE);
        ivRightImage.setVisibility(GONE);

        view.setBackgroundResource(R.drawable.nc_setting_item_selector);

        TypedArray ta =
                attrs == null
                        ? null
                        : getContext().obtainStyledAttributes(attrs, R.styleable.SettingItemView);
        if (ta != null) {
            final int N = ta.getIndexCount();
            boolean ivRightImageAutoMirrored = false;
            for (int i = 0; i < N; i++) {
                int attr = ta.getIndex(i);
                if (attr == R.styleable.SettingItemView_item_right_image_auto_mirrored) {
                    ivRightImageAutoMirrored = ta.getBoolean(attr, false);
                    break;
                }
            }
            for (int i = 0; i < N; i++) {
                int attr = ta.getIndex(i);
                if (attr == R.styleable.SettingItemView_item_image) {
                    Drawable drawable = ta.getDrawable(attr);
                    ivImage.setVisibility(VISIBLE);
                    ivImage.setImageDrawable(drawable);
                } else if (attr == R.styleable.SettingItemView_item_image_height) {
                    float imageHeight = ta.getDimension(attr, 0);
                    if (imageHeight > 0) {
                        ViewGroup.LayoutParams layoutParamsHeight = ivImage.getLayoutParams();
                        layoutParamsHeight.height = Math.round(imageHeight);
                        ivImage.setLayoutParams(layoutParamsHeight);
                    }
                } else if (attr == R.styleable.SettingItemView_item_image_width) {
                    float imageWidth = ta.getDimension(attr, 0);
                    if (imageWidth > 0) {
                        ViewGroup.LayoutParams layoutParamsWidth = ivImage.getLayoutParams();
                        layoutParamsWidth.width = Math.round(imageWidth);
                        ivImage.setLayoutParams(layoutParamsWidth);
                    }
                } else if (attr == R.styleable.SettingItemView_item_content) {
                    String content = ta.getString(attr);
                    tvContent.setText(content == null ? "" : content);
                } else if (attr == R.styleable.SettingItemView_item_content_text_size) {
                    float contentSize = ta.getDimension(attr, 0);
                    if (contentSize > 0) {
                        tvContent.setText(Math.round(contentSize));
                    }
                } else if (attr == R.styleable.SettingItemView_item_content_text_color) {
                    int color = ta.getColor(attr, -1);
                    if (color > 0) {
                        tvContent.setTextColor(color);
                    }
                } else if (attr == R.styleable.SettingItemView_item_value) {
                    String value = ta.getString(attr);
                    tvValue.setVisibility(VISIBLE);
                    tvValue.setText(value);
                } else if (attr == R.styleable.SettingItemView_item_value_text_size) {
                    float valueSize = ta.getDimension(attr, 0);
                    if (valueSize > 0) {
                        tvValue.setTextSize(TypedValue.COMPLEX_UNIT_PX, Math.round(valueSize));
                    }
                } else if (attr == R.styleable.SettingItemView_item_value_text_color) {
                    int valueColor = ta.getColor(attr, -1);
                    if (valueColor > 0) {
                        tvValue.setTextColor(valueColor);
                    }
                } else if (attr == R.styleable.SettingItemView_item_tag_image) {
                    Drawable tagImage = ta.getDrawable(R.styleable.SettingItemView_item_tag_image);
                    if (tagImage != null) {
                        ivTagImage.setImageDrawable(tagImage);
                    }
                } else if (attr == R.styleable.SettingItemView_item_tag_image_height) {
                    float tagImageHeight =
                            ta.getDimension(R.styleable.SettingItemView_item_tag_image_height, 0);
                    if (tagImageHeight > 0) {
                        ViewGroup.LayoutParams layoutParamsTagHeight = ivTagImage.getLayoutParams();
                        layoutParamsTagHeight.height = Math.round(tagImageHeight);
                        ivTagImage.setLayoutParams(layoutParamsTagHeight);
                    }
                } else if (attr == R.styleable.SettingItemView_item_tag_image_width) {
                    float tagImageWidth =
                            ta.getDimension(R.styleable.SettingItemView_item_tag_image_width, 0);
                    if (tagImageWidth > 0) {
                        ViewGroup.LayoutParams layoutParamsTagWidth = ivTagImage.getLayoutParams();
                        layoutParamsTagWidth.width = Math.round(tagImageWidth);
                        ivTagImage.setLayoutParams(layoutParamsTagWidth);
                    }
                } else if (attr == R.styleable.SettingItemView_item_divider) {
                    boolean divider = ta.getBoolean(attr, false);
                    vDivider.setVisibility(divider ? VISIBLE : GONE);
                } else if (attr == R.styleable.SettingItemView_item_switch) {
                    boolean switchCheck = ta.getBoolean(attr, false);
                    if (switchCheck) {
                        sbSwitch.setVisibility(VISIBLE);
                    } else {
                        sbSwitch.setVisibility(GONE);
                    }
                } else if (attr == R.styleable.SettingItemView_item_null_background) {
                    Boolean bgNull = ta.getBoolean(attr, false);
                    if (bgNull) {
                        setBackground(null);
                    }
                } else if (attr == R.styleable.SettingItemView_item_background) {
                    Drawable bg = ta.getDrawable(attr);
                    setBackground(bg);
                } else if (attr == R.styleable.SettingItemView_item_show_selected) {
                    isShowSelected = ta.getBoolean(attr, false);
                } else if (attr == R.styleable.SettingItemView_item_selected_image) {
                    Drawable selectedImage = ta.getDrawable(attr);
                    ivSelectImage.setImageDrawable(selectedImage);
                } else if (attr == R.styleable.SettingItemView_item_right_image) {
                    Drawable rightImage = ta.getDrawable(attr);
                    if (ivRightImageAutoMirrored && rightImage != null) {
                        rightImage.setAutoMirrored(true);
                    }
                    ivRightImage.setImageDrawable(rightImage);
                    ivRightImage.setVisibility(VISIBLE);
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float defHeight = getResources().getDimension(R.dimen.nc_widget_setting_item_height);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int specMode = MeasureSpec.getMode(heightMeasureSpec);

        if (specMode != MeasureSpec.EXACTLY) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec((int) defHeight, MeasureSpec.EXACTLY);
        } else {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec((int) height, MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Set the visibility of the right image
     *
     * @param visibility visibility value
     */
    public void setRightImageVisibility(int visibility) {
        ivRightImage.setVisibility(visibility);
    }

    /**
     * Set the visibility of the tag image
     *
     * @param visibility visibility value
     */
    public void setTagImageVisibility(int visibility) {
        ivTagImage.setVisibility(visibility);
    }

    /**
     * Set the tag image resource
     *
     * @param resId resource ID
     */
    public void setTagImage(int resId) {
        ivTagImage.setImageResource(resId);
    }

    /**
     * Set the visibility of the switch button
     *
     * @param visibility visibility value
     */
    public void setSwitchButtonVisibility(int visibility) {
        sbSwitch.setVisibility(visibility);
    }

    /**
     * Set the switch button checked change listener
     *
     * @param listener the listener
     */
    public void setSwitchCheckListener(CompoundButton.OnCheckedChangeListener listener) {
        checkedListener = listener;
        sbSwitch.setOnCheckedChangeListener(checkedListener);
    }

    public void setSwitchTouchListener(OnTouchListener listener) {
        sbSwitch.setOnTouchListener(listener);
    }

    /**
     * Set the visibility of the value text
     *
     * @param visibility visibility value
     */
    public void setValueVisibility(int visibility) {
        tvValue.setVisibility(visibility);
    }

    /**
     * Set the value text from a string resource
     *
     * @param resId string resource ID
     */
    public void setValue(int resId) {
        tvValue.setText(resId);
        tvValue.setVisibility(VISIBLE);
    }

    /**
     * Set the value text
     *
     * @param value the value text
     */
    public void setValue(String value) {
        tvValue.setText(value);
        tvValue.setVisibility(VISIBLE);
    }

    /** Get the value TextView for customizing text color */
    public TextView getValueView() {
        return tvValue;
    }

    /**
     * Get the value text
     *
     * @return the value string
     */
    public String getValue() {
        if (tvValue.getText() == null) {
            return "";
        }
        return tvValue.getText().toString();
    }

    /**
     * Set the content text from a string resource
     *
     * @param resId string resource ID
     */
    public void setContent(int resId) {
        tvContent.setText(resId);
    }

    /**
     * Set the content text
     *
     * @param content the content text
     */
    public void setContent(String content) {
        tvContent.setText(content);
    }

    /**
     * Set the visibility of the left image
     *
     * @param visibility visibility value
     */
    public void setImageVisibility(int visibility) {
        ivImage.setVisibility(visibility);
    }

    /**
     * Set the left image resource
     *
     * @param resId resource ID
     */
    public void setImage(int resId) {
        ivImage.setImageResource(resId);
    }

    /**
     * Set the switch button checked state
     *
     * @param isChecked whether checked
     */
    public void setChecked(boolean isChecked) {
        sbSwitch.setChecked(isChecked);
    }

    /**
     * Set the switch button checked state without triggering the checked change event
     *
     * @param isChecked whether checked
     */
    public void setCheckedWithOutEvent(boolean isChecked) {
        sbSwitch.setOnCheckedChangeListener(null);
        sbSwitch.setChecked(isChecked);
        sbSwitch.setOnCheckedChangeListener(checkedListener);
    }

    /**
     * Set the switch button checked state immediately without animation
     *
     * @param isChecked whether checked
     */
    public void setCheckedImmediately(boolean isChecked) {
        sbSwitch.setCheckedImmediately(isChecked);
    }

    /**
     * Set the switch button checked state immediately without animation or triggering the checked
     * change event
     *
     * @param isChecked whether checked
     */
    public void setCheckedImmediatelyWithOutEvent(boolean isChecked) {
        sbSwitch.setOnCheckedChangeListener(null);
        sbSwitch.setCheckedImmediately(isChecked);
        sbSwitch.setOnCheckedChangeListener(checkedListener);
    }

    /** Get the current switch checked state */
    public boolean isChecked() {
        return sbSwitch.isChecked();
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        if (selected && isShowSelected) {
            ivSelectImage.setVisibility(View.VISIBLE);
        } else {
            ivSelectImage.setVisibility(View.GONE);
        }
    }

    public void setSelectImage(@NonNull Uri uri) {
        if (ivSelectImage != null && uri != null) {
            ivSelectImage.setImageURI(uri);
        }
    }

    public ImageView getSelectImage() {
        return ivSelectImage;
    }
}
