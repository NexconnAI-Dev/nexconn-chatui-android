package ai.nexconn.chatui.widget;

import ai.nexconn.chatui.R;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/** Common dialog */
public class CommonDialog extends DialogFragment {

    private static class ControllerParams {
        public boolean isCancelable;
        public CharSequence contentMessage;
        public Bundle expandParams;
        public OnDialogButtonClickListener listener;
        public int positiveText;
        public int negativeText;
        public int positiveTextColor;
        public int titleText;
        private boolean isOnlyConfirm;
    }

    private static final String COMMON_DIALOG_PARAMS = "common_dialog_params";

    private ControllerParams params;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Use the Activity's Context to ensure access to the app's theme attributes
        return new NoLeakDialog(requireContext());
    }

    @Override
    public void onStart() {
        super.onStart();
        // Make background transparent
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            // Background color
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            int dialogWidth = getDialogWidth();
            if (dialogWidth > 0) {

                dialog.getWindow()
                        .setLayout((int) dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = getDialogView();
        if (view == null) {
            // Use the provided inflater to ensure correct theme application
            view = inflater.inflate(R.layout.nc_commom_dialog_base, container, false);
        }

        if (params == null) {
            params = new ControllerParams();
        }

        Button negative = view.findViewById(R.id.dialog_btn_negative);
        Button positive = view.findViewById(R.id.dialog_btn_positive);
        View btnSeparate = view.findViewById(R.id.dialog_v_btn_separate);
        RelativeLayout contentContainer = view.findViewById(R.id.dialog_content_container);
        TextView content = view.findViewById(R.id.dialog_tv_content);
        TextView title = view.findViewById(R.id.dialog_tv_title);
        negative.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                        if (onNegativeClick()) {
                            return;
                        }
                        if (params.listener != null) {
                            params.listener.onNegativeClick(v, getNegativeDatas());
                        }
                    }
                });
        positive.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                        if (onPositiveClick()) {
                            return;
                        }
                        if (params.listener != null) {
                            params.listener.onPositiveClick(v, getPositiveDatas());
                        }
                    }
                });

        View contentView = onCreateContentView(contentContainer);
        if (contentView != null) {
            contentContainer.removeAllViews();
            contentContainer.addView(contentView);
        } else if (!TextUtils.isEmpty(params.contentMessage)) {
            content.setText(Html.fromHtml(params.contentMessage.toString()));
        }

        if (params.positiveText > 0) {
            positive.setText(params.positiveText);
        }
        if (params.positiveTextColor != 0) {
            positive.setTextColor(params.positiveTextColor);
        }

        if (params.negativeText > 0) {
            negative.setText(params.negativeText);
        }

        if (params.titleText > 0) {
            title.setText(params.titleText);
            title.setVisibility(View.VISIBLE);
        }

        if (params.isOnlyConfirm) {
            negative.setVisibility(View.GONE);
            btnSeparate.setVisibility(View.GONE);
            positive.setBackgroundResource(R.drawable.common_dialog_single_positive_seletor);
        }

        setCancelable(params.isCancelable);

        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        if (window != null) {
            // Remove the system default margin
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Set dialog layout attributes
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * This method is only for custom dialogs where the layout changes but the view IDs remain the
     * same.
     *
     * @return custom dialog view, or null to use the default layout
     */
    protected View getDialogView() {
        return null;
    }

    /**
     * Override this method in subclasses to create and set a new content layout.
     *
     * @return custom content view, or null to use the default
     */
    protected View onCreateContentView(ViewGroup container) {
        return null;
    }

    /**
     * Override this method to provide data for the positive button callback.
     *
     * @return data bundle for the positive button callback
     */
    protected Bundle getPositiveDatas() {
        return null;
    }

    /**
     * Override this method to provide data for the negative button callback.
     *
     * @return data bundle for the negative button callback
     */
    protected Bundle getNegativeDatas() {
        return null;
    }

    /**
     * Subclasses can override this to handle the positive click internally. Return true to
     * intercept and prevent the external click listener from being invoked.
     *
     * @return true to intercept, false otherwise
     */
    protected boolean onPositiveClick() {
        return false;
    }

    /**
     * Subclasses can override this to handle the negative click internally. Return true to
     * intercept and prevent the external click listener from being invoked.
     *
     * @return true to intercept, false otherwise
     */
    protected boolean onNegativeClick() {
        return false;
    }

    /**
     * Get the dialog width. Override this method to set a custom width. Default is nearly
     * full-screen.
     *
     * @return dialog width in pixels, or 0 for default
     */
    protected int getDialogWidth() {
        return 0;
    }

    private void setParams(ControllerParams params) {
        this.params = params;
    }

    public Bundle getExpandParams() {
        if (params == null) {
            return null;
        }
        return params.expandParams;
    }

    public interface OnDialogButtonClickListener {
        void onPositiveClick(View v, Bundle bundle);

        default void onNegativeClick(View v, Bundle bundle) {}
    }

    /**
     * Subclasses of CommonDialog should extend this Builder and override getCurrentDialog to return
     * the subclass dialog instance.
     */
    public static class Builder {
        private ControllerParams params;

        public Builder() {
            params = new ControllerParams();
        }

        public Builder setContentMessage(CharSequence content) {
            params.contentMessage = content;
            return this;
        }

        public Builder isCancelable(boolean cancelable) {
            params.isCancelable = cancelable;
            return this;
        }

        public Builder setButtonText(int positiveText, int negativeText) {
            params.positiveText = positiveText;
            params.negativeText = negativeText;
            return this;
        }

        public Builder setPositiveTextColor(int positiveTextColor) {
            params.positiveTextColor = positiveTextColor;
            return this;
        }

        public Builder setTitleText(int titleText) {
            params.titleText = titleText;
            return this;
        }

        public Builder setDialogButtonClickListener(OnDialogButtonClickListener listener) {
            params.listener = listener;
            return this;
        }

        public Builder setExpandParams(Bundle expandParams) {
            params.expandParams = expandParams;
            return this;
        }

        public Builder setIsOnlyConfirm(boolean isOnlyConfirm) {
            params.isOnlyConfirm = isOnlyConfirm;
            return this;
        }

        public CommonDialog build() {
            CommonDialog dialog = getCurrentDialog();
            dialog.setParams(params);
            return dialog;
        }

        protected CommonDialog getCurrentDialog() {
            return new CommonDialog();
        }
    }
}
