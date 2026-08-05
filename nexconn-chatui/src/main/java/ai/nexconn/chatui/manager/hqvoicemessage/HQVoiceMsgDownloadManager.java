package ai.nexconn.chatui.manager.hqvoicemessage;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.DownloadMediaMessageHandler;
import ai.nexconn.chat.handler.MessageHandler;
import ai.nexconn.chat.message.HDVoiceMessage;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.model.MessageReceivedEvent;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.permission.PermissionCheckUtil;
import ai.nexconn.chatui.utils.system.NetUtils;
import ai.nexconn.chatui.utils.system.SystemUtils;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.text.TextUtils;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class HQVoiceMsgDownloadManager {

    private static final String TAG = HQVoiceMsgDownloadManager.class.getSimpleName();
    private Context mContext;
    private final AutoDownloadQueue autoDownloadQueue = new AutoDownloadQueue();
    private ExecutorService executorService;
    private Future<?> future = null;
    private List<AutoDownloadEntry> errorList = null;
    private final String[] writePermission =
            new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final List<HQVoiceDownloadListener> downloadListeners = new ArrayList<>();

    private final MessageHandler mMessageHandler =
            new MessageHandler() {
                @Override
                public void onMessageReceived(MessageReceivedEvent event) {
                    Message message = event.getMessage();
                    if (!Boolean.TRUE.equals(event.getOffline())
                            && message.getContent() instanceof HDVoiceMessage
                            && NCChatUIConfig.channelListConfig()
                                    .isEnableAutomaticDownloadHQVoice()) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                                && PermissionCheckUtil.checkPermissions(
                                        getInstance().mContext, writePermission)) {
                            enqueue(
                                    new AutoDownloadEntry(
                                            message, AutoDownloadEntry.DownloadPriority.NORMAL));
                        }
                    }
                }
            };

    private HQVoiceMsgDownloadManager() {
        // default implementation ignored
    }

    public void init(final Context context) {
        AutoDownloadNetWorkChangeReceiver autoDownloadNetWorkChangeReceiver;
        autoDownloadNetWorkChangeReceiver = new AutoDownloadNetWorkChangeReceiver();
        mContext = context.getApplicationContext();
        executorService = Executors.newSingleThreadExecutor();
        errorList = new ArrayList<>();
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            SystemUtils.registerReceiverCompat(
                    context, autoDownloadNetWorkChangeReceiver, intentFilter);
        } catch (Exception e) {
            RLog.e(TAG, "registerReceiver Exception", e);
        }
        downloadHQVoiceMessage();
        NCEngine.addMessageHandler("HQVoiceMsgDownloadManager", mMessageHandler);
    }

    private static class HQVoiceMsgDownloadManagerHolder {
        @SuppressLint("StaticFieldLeak")
        private static HQVoiceMsgDownloadManager instance = new HQVoiceMsgDownloadManager();
    }

    public static HQVoiceMsgDownloadManager getInstance() {
        return HQVoiceMsgDownloadManagerHolder.instance;
    }

    /**
     * Add download completion listener.
     *
     * @param listener The listener to be notified when download completes
     */
    public void addDownloadListener(HQVoiceDownloadListener listener) {
        if (listener != null && !downloadListeners.contains(listener)) {
            downloadListeners.add(listener);
        }
    }

    /**
     * Remove download completion listener.
     *
     * @param listener The listener to be removed
     */
    public void removeDownloadListener(HQVoiceDownloadListener listener) {
        downloadListeners.remove(listener);
    }

    public void enqueue(Fragment fragment, AutoDownloadEntry autoDownloadEntry) {

        if (autoDownloadEntry == null) {
            return;
        }

        Message message = autoDownloadEntry.getMessage();
        if (!(message.getContent() instanceof HDVoiceMessage)
                || (ifMsgInHashMap(message) && fragment != null)) {
            return;
        }

        HDVoiceMessage hdVoiceMessage = (HDVoiceMessage) message.getContent();

        if (!(hdVoiceMessage.getLocalPath() == null
                || TextUtils.isEmpty(hdVoiceMessage.getLocalPath().toString()))) {
            return;
        }

        synchronized (autoDownloadQueue) {
            boolean isEmpty = autoDownloadQueue.isEmpty();
            autoDownloadQueue.enqueue(autoDownloadEntry);
            if (isEmpty) {
                autoDownloadQueue.notify();
            }

            if (future.isDone() && isNetWorkAvailable(mContext)) {
                downloadHQVoiceMessage();
            }
        }
    }

    public void enqueue(AutoDownloadEntry autoDownloadEntry) {
        enqueue(null, autoDownloadEntry);
    }

    private Message dequeue() {
        return autoDownloadQueue.dequeue();
    }

    private void removeUidInHashMap(String uid) {
        autoDownloadQueue.getAutoDownloadEntryHashMap().remove(uid);
    }

    private boolean ifMsgInHashMap(Message message) {
        return autoDownloadQueue.ifMsgInHashMap(message);
    }

    private AutoDownloadEntry getMsgEntry(Message message) {
        if (message == null) {
            return null;
        }
        AutoDownloadEntry autoDownloadEntry = null;
        if (autoDownloadQueue.getAutoDownloadEntryHashMap().containsKey(message.getMessageId())) {
            autoDownloadEntry =
                    autoDownloadQueue.getAutoDownloadEntryHashMap().get(message.getMessageId());
        }
        return autoDownloadEntry;
    }

    private void downloadHQVoiceMessage() {
        future =
                executorService.submit(
                        new Runnable() {
                            @Override
                            public void run() {
                                //noinspection InfiniteLoopStatement
                                while (true) {
                                    synchronized (autoDownloadQueue) {
                                        if (autoDownloadQueue.isEmpty()) {
                                            try {
                                                autoDownloadQueue.wait();
                                            } catch (InterruptedException e) {
                                                RLog.e(
                                                        TAG,
                                                        "downloadHQVoiceMessage e:" + e.toString());
                                                Thread.currentThread().interrupt();
                                            }
                                        }
                                    }

                                    Message message = dequeue();
                                    if (message == null) continue;
                                    message.downloadMedia(
                                            new DownloadMediaMessageHandler() {
                                                @Override
                                                public void onSuccess(Message msg) {
                                                    RLog.d(
                                                            TAG,
                                                            "downloadMediaMessage success, messageId="
                                                                    + msg.getMessageId());
                                                    if (errorList != null) {
                                                        errorList.remove(getMsgEntry(msg));
                                                    }
                                                    removeUidInHashMap(msg.getMessageId());

                                                    // Notify all listeners for UI refresh
                                                    for (HQVoiceDownloadListener listener :
                                                            downloadListeners) {
                                                        if (listener != null) {
                                                            listener.onDownloadComplete(msg);
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onProgress(Message msg, int progress) {
                                                    RLog.d(TAG, "downloadMediaMessage onProgress");
                                                }

                                                @Override
                                                public void onError(Message msg, NCError error) {
                                                    if (errorList != null
                                                            && !errorList.contains(
                                                                    getMsgEntry(msg))) {
                                                        errorList.add(getMsgEntry(msg));
                                                        RLog.i(
                                                                TAG,
                                                                "onError = "
                                                                        + error.getCode()
                                                                        + " errorList size = "
                                                                        + errorList.size());
                                                    }
                                                }

                                                @Override
                                                public void onCanceled(Message msg) {
                                                    // do nothing
                                                }
                                            });
                                }
                            }
                        });
    }

    private static boolean isNetWorkAvailable(Context context) {
        return NetUtils.isNetWorkAvailable(context);
    }

    void pauseDownloadService() {
        // do nothing
    }

    public void resumeDownloadService() {
        if (errorList == null || errorList.size() == 0) {
            return;
        }
        if (future.isDone() && isNetWorkAvailable(mContext)) {
            downloadHQVoiceMessage();
        }

        for (int i = errorList.size() - 1; i >= 0; i--) {
            enqueue(errorList.get(i));
        }
    }
}
