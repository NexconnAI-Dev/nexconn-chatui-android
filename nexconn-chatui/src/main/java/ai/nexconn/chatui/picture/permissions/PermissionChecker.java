package ai.nexconn.chatui.picture.permissions;

import ai.nexconn.chatui.utils.permission.PermissionCheckUtil;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class PermissionChecker {

    /**
     * Check whether a specific permission is granted
     *
     * @param context
     * @param permissions
     * @return
     */
    public static boolean checkSelfPermission(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context.getApplicationContext(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Request multiple permissions dynamically
     *
     * @param activity
     * @param code
     */
    public static void requestPermissions(
            Activity activity, @NonNull String[] permissions, int code) {
        PermissionCheckUtil.requestPermissions(activity, permissions, code);
    }
}
