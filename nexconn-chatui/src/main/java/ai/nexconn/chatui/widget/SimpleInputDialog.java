package ai.nexconn.chatui.widget;

import ai.nexconn.chatui.R;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class SimpleInputDialog extends DialogFragment implements View.OnClickListener {
    private EditText inputEt;
    private TextView confirmTv;
    private TextView cancelTv;
    private TextView titleTv;

    private String hintText;
    private String confirmText;
    private String cancelText;
    private String titleText;
    private InputDialogListener inputDialogListener;

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
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            // Set transparent background
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Use the provided inflater to ensure correct theme application
        View contentView =
                inflater.inflate(R.layout.nc_common_dialog_simple_input, container, false);

        // Initialize views
        inputEt = contentView.findViewById(R.id.common_et_dialog_input);
        confirmTv = contentView.findViewById(R.id.common_tv_dialog_confirm);
        cancelTv = contentView.findViewById(R.id.common_tv_dialog_cancel);
        titleTv = contentView.findViewById(R.id.common_tv_title);

        // Set click listeners
        confirmTv.setOnClickListener(this);
        cancelTv.setOnClickListener(this);

        // Set hint text
        if (!TextUtils.isEmpty(hintText)) {
            inputEt.setHint(hintText);
        }

        // Set confirm button text
        if (!TextUtils.isEmpty(confirmText)) {
            confirmTv.setText(confirmText);
        }

        // Set cancel button text
        if (!TextUtils.isEmpty(cancelText)) {
            cancelTv.setText(cancelText);
        }

        // Set title text
        if (!TextUtils.isEmpty(titleText)) {
            titleTv.setText(titleText);
        }

        // Request no title bar
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        return contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog() != null ? getDialog().getWindow() : null;
        if (window != null) {
            // Remove the system default margin
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Set dialog layout attributes
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    public void setInputHint(String hint) {
        hintText = hint;
    }

    public void setConfirmText(String confirmText) {
        this.confirmText = confirmText;
    }

    public void setCancelText(String cancelText) {
        this.cancelText = cancelText;
    }

    public void setTitleText(String titleText) {
        this.titleText = titleText;
    }

    public void setInputDialogListener(InputDialogListener listener) {
        this.inputDialogListener = listener;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.common_tv_dialog_confirm) {
            boolean isClose = true;
            if (inputDialogListener != null) {
                isClose = inputDialogListener.onConfirmClicked(inputEt);
            }
            if (isClose) {
                dismiss();
            }
        } else if (id == R.id.common_tv_dialog_cancel) {
            dismiss();
        }
    }

    public interface InputDialogListener {
        /**
         * Callback when the confirm button is clicked
         *
         * @return false to keep the dialog open, true to dismiss it
         */
        boolean onConfirmClicked(EditText input);
    }
}
