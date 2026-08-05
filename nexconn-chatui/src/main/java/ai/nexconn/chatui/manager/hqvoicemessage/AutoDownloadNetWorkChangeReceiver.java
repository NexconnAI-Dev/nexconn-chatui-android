package ai.nexconn.chatui.manager.hqvoicemessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class AutoDownloadNetWorkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = AutoDownloadNetWorkChangeReceiver.class.getSimpleName();

    @SuppressWarnings("deprecation")
    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm != null ? cm.getActiveNetworkInfo() : null;
        boolean networkAvailable = info != null && info.isConnected();
        if ((ConnectivityManager.CONNECTIVITY_ACTION).equals(intent.getAction())
                && networkAvailable) {
            HQVoiceMsgDownloadManager.getInstance().resumeDownloadService();
        } else {
            HQVoiceMsgDownloadManager.getInstance().pauseDownloadService();
        }
    }
}
