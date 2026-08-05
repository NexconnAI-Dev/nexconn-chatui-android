package ai.nexconn.chatui.widget.dialog;

import ai.nexconn.chatui.R;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

public class TipLoadingDialog extends Dialog {
    private TextView textView;

    public TipLoadingDialog(Context context) {
        super(context, R.style.Picture_Theme_AlertDialog);
        setCancelable(true);
        setCanceledOnTouchOutside(false);
        Window window = getWindow();
        window.setWindowAnimations(R.style.PictureThemeDialogWindowStyle);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nc_tip_alert_dialog);
        // Initialize views
        textView = findViewById(R.id.tv_tips);
        setCancelable(false);
    }

    // Set the loading tip text
    public void setTips(String text) {
        if (textView != null) {
            textView.setText(text);
        }
    }
}
