package ai.nexconn.chatui.picture.dialog;

import ai.nexconn.chatui.R;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;

public class PictureLoadingDialog extends Dialog {

    public PictureLoadingDialog(Context context) {
        super(context, R.style.Picture_Theme_AlertDialog);
        setCancelable(true);
        setCanceledOnTouchOutside(false);
        Window window = getWindow();
        window.setWindowAnimations(R.style.PictureThemeDialogWindowStyle);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nc_picture_alert_dialog);
    }
}
