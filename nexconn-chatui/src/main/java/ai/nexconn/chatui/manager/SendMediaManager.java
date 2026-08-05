package ai.nexconn.chatui.manager;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.ConnectionStatusHandler;
import ai.nexconn.chat.handler.SendMediaMessageHandler;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.ShortVideoMessage;
import ai.nexconn.chat.message.model.SentStatus;
import ai.nexconn.chat.model.ConnectionStatus;
import ai.nexconn.chat.params.SendMediaMessageParams;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.event.action.RefreshEvent;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.file.FileUtils;
import ai.nexconn.chatui.utils.log.RLog;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SendMediaManager {
    private static final String TAG = SendMediaManager.class.getSimpleName();
    private static final String CONNECTION_HANDLER_ID = "SendMediaManager";

    private ExecutorService executorService;
    private final List<Message> mExecutingMessages = new CopyOnWriteArrayList<>();

    private final ConnectionStatusHandler connectionStatusHandler =
            event -> {
                ConnectionStatus status = event.getStatus();
                if (status == ConnectionStatus.NETWORK_UNAVAILABLE
                        || status == ConnectionStatus.UNCONNECTED
                        || status == ConnectionStatus.KICKED_OFFLINE_BY_OTHER_CLIENT
                        || status == ConnectionStatus.TOKEN_INCORRECT
                        || status == ConnectionStatus.CONN_USER_BLOCKED) {
                    RLog.w(TAG, "Connection lost, resetting pending media messages");
                    reset();
                }
            };

    static class SingletonHolder {
        static SendMediaManager sInstance = new SendMediaManager();
    }

    public static SendMediaManager getInstance() {
        return SingletonHolder.sInstance;
    }

    private SendMediaManager() {
        executorService = getExecutorService();
        NCEngine.addConnectionStatusHandler(CONNECTION_HANDLER_ID, connectionStatusHandler);
    }

    public void sendMedia(
            Context context,
            ChannelIdentifier conversationIdentifier,
            Uri mediaUri,
            long duration) {
        if (TextUtils.isEmpty(mediaUri.toString())) {
            return;
        }
        if (!FileUtils.isFileExistsWithUri(context, mediaUri)) {
            return;
        }
        ShortVideoMessage shortVideoMessage = new ShortVideoMessage();
        shortVideoMessage.setLocalPath(mediaUri.toString());
        shortVideoMessage.setDuration((int) duration / 1000);
        // NCChatUI.sendMediaMessage fires all SendMediaEvent stages (ATTACH/PROGRESS/SUCCESS/
        // ERROR/CANCEL) automatically; the handler here adds business-specific logic only.
        NCChatUI.sendMediaMessage(
                conversationIdentifier,
                new SendMediaMessageParams(shortVideoMessage),
                new SendMediaMessageHandler() {
                    @Override
                    public void onAttached(Message message) {
                        mExecutingMessages.add(message);
                    }

                    @Override
                    public void onProgress(Message message, int progress) {
                        // progress events fired automatically by NCChatUI.sendMediaMessage
                    }

                    @Override
                    public void onResult(@Nullable Message message, @Nullable NCError error) {
                        if (message != null) mExecutingMessages.remove(message);
                        if (error != null && error.getCode() == 34015) {
                            String text = context.getString(R.string.nc_picsel_video_corrupted);
                            ToastUtils.show(context, text, Toast.LENGTH_SHORT);
                        }
                    }

                    @Override
                    public void onCanceled(Message message) {
                        mExecutingMessages.remove(message);
                    }
                });
    }

    public void cancelSendingMedia(ChannelType conversationType, String targetId) {
        RLog.d(TAG, "cancel Sending media");
        List<Message> toRemove = new ArrayList<>();
        for (Message msg : mExecutingMessages) {
            if (msg.getChannelIdentifier().getChannelType().equals(conversationType)
                    && msg.getChannelIdentifier().getChannelId().equals(targetId)) {
                toRemove.add(msg);
            }
        }
        mExecutingMessages.removeAll(toRemove);
    }

    public void cancelSendingMedia(ChannelType conversationType, String targetId, int messageId) {
        RLog.d(TAG, "cancel Sending media");
        for (Message msg : mExecutingMessages) {
            if (msg.getChannelIdentifier().getChannelType().equals(conversationType)
                    && msg.getChannelIdentifier().getChannelId().equals(targetId)
                    && msg.getClientId() == messageId) {
                mExecutingMessages.remove(msg);
                break;
            }
        }
    }

    public void reset() {
        RLog.w(TAG, "Reset Sending media.");
        List<Message> pending = new ArrayList<>(mExecutingMessages);
        mExecutingMessages.clear();
        for (Message message : pending) {
            message.setSentStatus(SentStatus.FAILED);
            NCChatUI.refreshMessage(new RefreshEvent(message));
        }
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService =
                    new ThreadPoolExecutor(
                            1,
                            Integer.MAX_VALUE,
                            60,
                            TimeUnit.SECONDS,
                            new SynchronousQueue<Runnable>(),
                            threadFactory());
        }
        return executorService;
    }

    private ThreadFactory threadFactory() {
        return new ThreadFactory() {
            @Override
            public Thread newThread(@Nullable Runnable runnable) {
                Thread result = new Thread(runnable, "NC SendMediaManager");
                result.setDaemon(false);
                return result;
            }
        };
    }
}
