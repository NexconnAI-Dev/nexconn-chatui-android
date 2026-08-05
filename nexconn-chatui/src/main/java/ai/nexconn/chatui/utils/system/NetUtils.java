package ai.nexconn.chatui.utils.system;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public final class NetUtils {
    private NetUtils() {}

    public static boolean isNetWorkAvailable(Context context) {
        return isNetWorkAvailable(getActiveNetworkInfo(context));
    }

    public static boolean isNetWorkAvailable(NetworkInfo activeNetworkInfo) {
        return activeNetworkInfo != null
                && activeNetworkInfo.isAvailable()
                && activeNetworkInfo.isConnected()
                && activeNetworkInfo.getState() == NetworkInfo.State.CONNECTED;
    }

    private static NetworkInfo getActiveNetworkInfo(Context context) {
        if (context == null) {
            return null;
        }
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return null;
        }
        try {
            return manager.getActiveNetworkInfo();
        } catch (Exception ignored) {
            return null;
        }
    }
}
