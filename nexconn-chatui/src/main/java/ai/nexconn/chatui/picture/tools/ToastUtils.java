package ai.nexconn.chatui.picture.tools;

import android.content.Context;
import android.widget.Toast;

/**
 * @deprecated Please use {@link ai.nexconn.chatui.utils.common.ToastUtils}
 */
public final class ToastUtils {
    public static void s(Context mContext, String s) {
        ai.nexconn.chatui.utils.common.ToastUtils.show(mContext, s, Toast.LENGTH_SHORT);
    }
}
