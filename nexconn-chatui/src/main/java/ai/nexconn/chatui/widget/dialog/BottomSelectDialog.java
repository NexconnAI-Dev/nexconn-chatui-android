package ai.nexconn.chatui.widget.dialog;

import ai.nexconn.chatui.R;
import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import java.io.Serializable;

/** Bottom selection menu dialog. Build using {@link Builder}. */
public class BottomSelectDialog extends DialogFragment implements View.OnClickListener {
    private static final String ARGUMENT_KEY_SELECTIONS = "selections";
    private static final String ARGUMENT_KEY_SELECTIONS_COLOR = "selections_color";
    private static final String ARGUMENT_KEY_TITLE = "title";

    private String[] mSelections;
    private int[] mSelectionsColor;
    private String mTitle;
    private OnSelectListener mOnSelectListener;

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.nc_dialog_bottom_select, container, false);
        LinearLayout containerLl = contentView.findViewById(R.id.nc_dialog_bottom_container);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mSelections = arguments.getStringArray(ARGUMENT_KEY_SELECTIONS);
            mSelectionsColor = arguments.getIntArray(ARGUMENT_KEY_SELECTIONS_COLOR);
            mTitle = arguments.getString(ARGUMENT_KEY_TITLE);
        }

        View cancelView = contentView.findViewById(R.id.nc_dialog_bottom_item_cancel);
        cancelView.setOnClickListener(this);

        addTitle(containerLl);
        addSelection(containerLl);

        return contentView;
    }

    /**
     * Add a title view
     *
     * @param container the parent container
     */
    private void addTitle(LinearLayout container) {
        if (!TextUtils.isEmpty(mTitle)) {
            Resources resources = getResources();
            TextView titleView = new TextView(getContext());
            titleView.setText(mTitle);
            titleView.setTextColor(resources.getColor(R.color.nc_dialog_bottom_text_title_color));
            titleView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            int margin = (int) resources.getDimension(R.dimen.nc_dialog_bottom_text_item_margin);
            layoutParams.topMargin = margin;
            layoutParams.bottomMargin = margin;
            layoutParams.leftMargin = margin;
            layoutParams.rightMargin = margin;
            container.addView(titleView, 0, layoutParams);
            addSeparateLine(container, 1);
        }
    }

    /**
     * Add selection options
     *
     * @param container the parent container
     */
    private void addSelection(LinearLayout container) {
        if (mSelections != null && mSelections.length > 0) {
            boolean isSetSelectionColor = false;
            boolean hasTitle = false;
            if (mSelectionsColor != null && mSelections.length == mSelectionsColor.length) {
                isSetSelectionColor = true;
            }
            if (!TextUtils.isEmpty(mTitle)) {
                hasTitle = true;
            }
            Resources resources = getResources();
            int length = mSelections.length;
            int indexInContainer = hasTitle ? 2 : 0; // 2 = 1 title + 1 separator line
            for (int i = 0; i < length; i++) {
                String selectText = mSelections[i];
                TextView titleView = new TextView(getContext());
                titleView.setText(selectText);

                if (isSetSelectionColor && mSelectionsColor[i] != 0) {
                    titleView.setTextColor(resources.getColor(mSelectionsColor[i]));
                } else {
                    titleView.setTextColor(resources.getColor(R.color.nc_dialog_bottom_text_color));
                }
                titleView.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams layoutParams =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                int margin =
                        (int) resources.getDimension(R.dimen.nc_dialog_bottom_text_item_margin);
                layoutParams.topMargin = margin;
                layoutParams.bottomMargin = margin;
                layoutParams.leftMargin = margin;
                layoutParams.rightMargin = margin;
                titleView.setTag(i);
                titleView.setOnClickListener(this);
                container.addView(titleView, indexInContainer++, layoutParams);

                if (i != length - 1) {
                    addSeparateLine(container, indexInContainer++);
                }
            }
        }
    }

    /**
     * Add a separator line
     *
     * @param container the parent container
     * @param index the position to insert
     */
    private void addSeparateLine(LinearLayout container, int index) {
        View separateView = new View(getContext());
        Resources resources = getResources();
        int height =
                Math.max(
                        1,
                        (int)
                                resources.getDimension(
                                        R.dimen.nc_dialog_bottom_item_separate_height));
        LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
        separateView.setBackgroundColor(
                resources.getColor(R.color.nc_dialog_bottom_selection_separate_color));
        container.addView(separateView, index, layoutParams);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Set width to screen width, positioned near the bottom of the screen.
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window win = dialog.getWindow();
            // Background must be set, otherwise window attributes won't take effect
            if (win != null) {
                win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                WindowManager.LayoutParams params = win.getAttributes();
                params.gravity = Gravity.BOTTOM;
                // Use ViewGroup.LayoutParams to make the Dialog fill the screen width
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                win.setAttributes(params);
            }

            FragmentActivity activity = getActivity();
            if (activity != null && activity.getWindowManager() != null) {
                // Make the dialog fullscreen
                DisplayMetrics dm = new DisplayMetrics();
                activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.nc_dialog_bottom_item_cancel) {
            dismiss();
        } else {
            if (v.getTag() instanceof Integer) {
                int index = (int) v.getTag();
                if (mOnSelectListener != null) {
                    mOnSelectListener.onSelect(index);
                }
                dismiss();
            }
        }
    }

    /**
     * Set the selection click listener. Must be re-set when the Activity is recreated due to
     * configuration changes such as screen rotation.
     *
     * @param listener the listener
     */
    public void setOnSelectListener(OnSelectListener listener) {
        mOnSelectListener = listener;
    }

    /**
     * Builder. Use {@link #setSelections(String[])} to set options, {@link #build()} to create the
     * instance, and then {@link BottomSelectDialog#setOnSelectListener(OnSelectListener)} to set
     * the listener.
     */
    public static class Builder {
        private String[] mSelections;
        private int[] mSelectionsColor;
        private String mTitle;

        /**
         * Set the selection texts
         *
         * @param selections the selection texts
         * @return this Builder
         */
        public Builder setSelections(String[] selections) {
            this.mSelections = selections;
            return this;
        }

        /**
         * Set the color resources for the selection texts (must be resource color IDs). The array
         * length must match {@link #setSelections(String[])} to take effect. Use 0 for default
         * color.
         *
         * @param colors the color resource IDs
         * @return this Builder
         */
        public Builder setSelectionsColor(int[] colors) {
            this.mSelectionsColor = colors;
            return this;
        }

        /**
         * Set the title. Title is optional.
         *
         * @param title the title text
         * @return this Builder
         */
        public Builder setTitle(String title) {
            this.mTitle = title;
            return this;
        }

        public BottomSelectDialog build() {
            BottomSelectDialog bottomSelectDialog = new BottomSelectDialog();
            Bundle bundle = new Bundle();
            bundle.putStringArray(ARGUMENT_KEY_SELECTIONS, mSelections);
            bundle.putIntArray(ARGUMENT_KEY_SELECTIONS_COLOR, mSelectionsColor);
            bundle.putString(ARGUMENT_KEY_TITLE, mTitle);
            bottomSelectDialog.setArguments(bundle);
            return bottomSelectDialog;
        }
    }

    /** Selection click listener. */
    public interface OnSelectListener extends Serializable {
        /**
         * Called when a selection option is clicked
         *
         * @param index the selected index, corresponding to the index in {@link
         *     Builder#setSelections}.
         */
        void onSelect(int index);
    }
}
