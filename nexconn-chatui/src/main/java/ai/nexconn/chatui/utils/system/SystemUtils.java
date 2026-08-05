package ai.nexconn.chatui.utils.system;

import ai.nexconn.chatui.utils.log.RLog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

public class SystemUtils {
    private static final String TAG = "SystemUtils";
    private static final String NC_ACCESS_RECEIVER = ".permission.NC_ACCESS_RECEIVER";

    public static Intent registerReceiverCompat(
            Context context, BroadcastReceiver receiver, IntentFilter filter) {
        Intent intent = null;
        try {
            String permission = context.getApplicationInfo().packageName + NC_ACCESS_RECEIVER;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent =
                        context.registerReceiver(
                                receiver, filter, permission, null, Context.RECEIVER_NOT_EXPORTED);
            } else {
                intent = context.registerReceiver(receiver, filter, permission, null);
            }
        } catch (Exception e) {
            RLog.e(TAG, "registerReceiver Exception", e);
        }
        return intent;
    }

    public static String getCurrentProcessName(Context context) {
        if (context == null) return "";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return android.app.Application.getProcessName();
            }
        } catch (Exception ignored) {
        }
        return context.getPackageName();
    }
}
