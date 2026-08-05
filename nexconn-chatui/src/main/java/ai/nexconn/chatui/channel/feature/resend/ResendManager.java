package ai.nexconn.chatui.channel.feature.resend;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.SendMediaMessageHandler;
import ai.nexconn.chat.handler.SendMessageHandler;
import ai.nexconn.chat.message.ImageMessage;
import ai.nexconn.chat.message.MediaMessageContent;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.params.SendMediaMessageParams;
import ai.nexconn.chat.params.SendMessageParams;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.utils.log.RLog;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Manages message resending. */
public class ResendManager {

    private final String TAG = "ResendManager";
    // Message send interval
    private static final int TIME_DELAY = 300;
    private volatile boolean mIsProcessing = false;
    private Hashtable<Integer, Message> mMessageMap;
    private ConcurrentLinkedQueue<Integer> mMessageQueue;
    private Handler mResendHandler;
    private ai.nexconn.chat.handler.ConnectionStatusHandler connectionStatusHandler =
            event -> {
                ai.nexconn.chat.model.ConnectionStatus status = event.getStatus();
                if (status == ai.nexconn.chat.model.ConnectionStatus.CONNECTED) {
                    ResendManager.getInstance().beginResend();
                } else if (status == ai.nexconn.chat.model.ConnectionStatus.SIGNED_OUT) {
                    ResendManager.getInstance().removeAllResendMessage();
                }
            };

    private ResendManager() {
        mMessageMap = new Hashtable<>();
        mMessageQueue = new ConcurrentLinkedQueue<>();
        HandlerThread resendThread = new HandlerThread("RESEND_WORK");
        resendThread.start();
        mResendHandler = new Handler(resendThread.getLooper());
        NCEngine.addConnectionStatusHandler("ResendManager", connectionStatusHandler);
    }

    private static class ResendManagerHolder {
        private static ResendManager instance = new ResendManager();
    }

    public static ResendManager getInstance() {
        return ResendManagerHolder.instance;
    }

    public void addResendMessage(
            final Message message,
            final NCError errorCode,
            final AddResendMessageCallBack callBack) {
        mResendHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (errorCode == null) {
                            return;
                        }
                        if (isResendErrorCode(errorCode)) {
                            if (!mMessageMap.containsKey(message.getClientId())) {
                                RLog.d(TAG, "addResendMessage : id=" + message.getClientId());
                                if (mMessageMap != null && mMessageQueue != null) {
                                    mMessageMap.put(message.getClientId(), message);
                                    mMessageQueue.add(message.getClientId());
                                    beginResend();
                                    message.setSentStatus(
                                            ai.nexconn.chat.message.model.SentStatus.SENDING);
                                }
                            }
                        }
                        callBack.onComplete(message, errorCode);
                    }
                });
    }

    public void removeResendMessage(final int messageId) {
        mResendHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mMessageMap != null) {
                            mMessageMap.remove(messageId);
                            mMessageQueue.remove(messageId);
                        }
                    }
                });
    }

    public void removeResendMessages(final int[] messageIds) {
        if (messageIds == null || messageIds.length == 0) {
            return;
        }
        mResendHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mMessageMap != null) {
                            for (int messageId : messageIds) {
                                mMessageMap.remove(messageId);
                                mMessageQueue.remove(messageId);
                            }
                        }
                    }
                });
    }

    public void removeAllResendMessage() {
        mResendHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mMessageMap != null) {
                            mMessageMap.clear();
                            mMessageQueue.clear();
                            mIsProcessing = false;
                        }
                    }
                });
    }

    public boolean needResend(int messageId) {
        if (mMessageMap == null) {
            return false;
        }
        return mMessageMap.containsKey(messageId);
    }

    /**
     * Whether the resend loop is currently flushing queued messages (e.g. after network recovery).
     *
     * <p>Used by the message list to suppress auto scroll-to-end for messages re-added by resend,
     * so the page stays put and only the send status updates.
     *
     * @return true while resending is in progress
     */
    public boolean isResending() {
        return mIsProcessing;
    }

    public void beginResend() {
        mResendHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mMessageMap == null || mMessageMap.size() == 0) {
                            RLog.i(TAG, "beginResend onChanged no message need resend");
                            mIsProcessing = false;
                            return;
                        }
                        if (mIsProcessing) {
                            RLog.i(TAG, "beginResend ConnectionStatus is resending");
                            return;
                        }
                        mIsProcessing = true;
                        loopResendMessage();
                    }
                });
    }

    private void loopResendMessage() {
        mResendHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        final Integer idInteger = mMessageQueue.peek();
                        RLog.d(TAG, "beginResend: messageId = " + idInteger);
                        if (idInteger == null
                                || NCEngine.getConnectionStatus()
                                        != ai.nexconn.chat.model.ConnectionStatus.CONNECTED) {
                            mIsProcessing = false;
                            return;
                        }
                        resendMessage(
                                mMessageMap.get(idInteger),
                                new ReSendMessageCallback() {
                                    @Override
                                    public void onCancel(Message message) {
                                        removeResendMessage(idInteger);
                                        loopResendMessage();
                                    }

                                    @Override
                                    public void onAttached(Message message) {
                                        Message originalMessage = mMessageMap.get(idInteger);
                                        if (originalMessage != null
                                                && message != null
                                                && originalMessage.getClientId()
                                                        != message.getClientId()) {
                                            RLog.d(
                                                    TAG,
                                                    "Replacing original resend message on attach, original="
                                                            + originalMessage.getClientId()
                                                            + ", new="
                                                            + message.getClientId());
                                            // Replace in place (keep list position) instead of
                                            // removing the original and letting the new copy append
                                            // to the bottom, which would make the resent message
                                            // sink out of view under an open keyboard.
                                            NCChatUI.replaceMessageInUI(originalMessage, message);
                                        }
                                    }

                                    @Override
                                    public void onSuccess(Message message) {
                                        RLog.i(
                                                TAG,
                                                "resendMessage success messageId = "
                                                        + (message != null
                                                                ? message.getClientId()
                                                                : null));
                                        deleteReplacedLocalMessage(
                                                mMessageMap.get(idInteger), message);
                                        removeResendMessage(idInteger);
                                        loopResendMessage();
                                    }

                                    @Override
                                    public void onError(Message message, NCError coreErrorCode) {
                                        RLog.i(
                                                TAG,
                                                "resendMessage error messageId = "
                                                        + (message != null
                                                                ? message.getClientId()
                                                                : null));
                                        if (!isResendErrorCode(coreErrorCode)) {
                                            removeResendMessage(idInteger);
                                        }
                                        loopResendMessage();
                                    }
                                });
                    }
                },
                TIME_DELAY);
    }

    /**
     * Checks if the error code requires resend handling.
     *
     * @param errorCode the send failure error code
     * @return true if resend is needed, false otherwise
     */
    public boolean isResendErrorCode(NCError errorCode) {
        if (errorCode == null) return false;
        int code = errorCode.getCode();
        return (code == 30001 || code == 30002 || code == 30003 || code == 34011);
    }

    /**
     * Resends a message.
     *
     * @param message the message to resend
     */
    private void resendMessage(Message message, final ReSendMessageCallback callback) {
        if (message == null) {
            RLog.i(TAG, "resendMessage: Message is Null");
            return;
        }
        if (TextUtils.isEmpty(message.getChannelIdentifier().getChannelId())
                || message.getContent() == null) {
            RLog.e(TAG, "targetId or messageContent is Null");
            removeResendMessage(message.getClientId());
            return;
        }

        SendMessageHandler sendHandler =
                new SendMessageHandler() {
                    @Override
                    public void onAttached(Message msg) {
                        callback.onAttached(msg);
                    }

                    @Override
                    public void onResult(@Nullable Message msg, @Nullable NCError error) {
                        if (error == null) {
                            callback.onSuccess(msg);
                        } else {
                            callback.onError(msg, error);
                        }
                    }
                };

        ChannelIdentifier channelIdentifier = message.getChannelIdentifier();
        if (message.getContent() instanceof ImageMessage) {
            ImageMessage imageMessage = (ImageMessage) message.getContent();
            if (hasUploadedRemoteUrl(imageMessage.getRemoteUrl())) {
                NCChatUI.sendMessage(
                        channelIdentifier, new SendMessageParams(imageMessage), sendHandler);
            } else {
                NCChatUI.sendMediaMessage(
                        channelIdentifier,
                        new SendMediaMessageParams(imageMessage),
                        new SendMediaMessageHandler() {
                            @Override
                            public void onProgress(Message msg, int progress) {
                                // do nothing
                            }

                            @Override
                            public void onCanceled(Message msg) {
                                callback.onCancel(msg);
                            }

                            @Override
                            public void onAttached(Message msg) {
                                callback.onAttached(msg);
                            }

                            @Override
                            public void onResult(@Nullable Message msg, @Nullable NCError error) {
                                if (error == null) {
                                    callback.onSuccess(msg);
                                } else {
                                    callback.onError(msg, error);
                                }
                            }
                        });
            }
        } else if (message.getContent() instanceof MediaMessageContent) {
            MediaMessageContent mediaContent = (MediaMessageContent) message.getContent();
            if (hasUploadedRemoteUrl(mediaContent.getRemoteUrl())) {
                NCChatUI.sendMessage(
                        channelIdentifier, new SendMessageParams(mediaContent), sendHandler);
            } else {
                NCChatUI.sendMediaMessage(
                        channelIdentifier,
                        new SendMediaMessageParams(mediaContent),
                        new SendMediaMessageHandler() {
                            @Override
                            public void onProgress(Message msg, int progress) {
                                // do nothing
                            }

                            @Override
                            public void onCanceled(Message msg) {
                                callback.onCancel(msg);
                            }

                            @Override
                            public void onAttached(Message msg) {
                                callback.onAttached(msg);
                            }

                            @Override
                            public void onResult(@Nullable Message msg, @Nullable NCError error) {
                                if (error == null) {
                                    callback.onSuccess(msg);
                                } else {
                                    callback.onError(msg, error);
                                }
                            }
                        });
            }
        } else {
            NCChatUI.sendMessage(
                    channelIdentifier, new SendMessageParams(message.getContent()), sendHandler);
        }
    }

    private boolean hasUploadedRemoteUrl(String remoteUrl) {
        if (TextUtils.isEmpty(remoteUrl)) {
            return false;
        }
        return !(remoteUrl.startsWith("file://") || remoteUrl.startsWith("content://"));
    }

    private void deleteReplacedLocalMessage(Message originalMessage, Message sentMessage) {
        if (originalMessage == null
                || sentMessage == null
                || originalMessage.getClientId() <= 0
                || originalMessage.getClientId() == sentMessage.getClientId()) {
            return;
        }
        NCChatUI.deleteLocalMessages(
                originalMessage.getChannelIdentifier(),
                java.util.Collections.singletonList(originalMessage),
                error -> {
                    if (error != null) {
                        RLog.w(
                                TAG,
                                "delete replaced local message failed, clientId="
                                        + originalMessage.getClientId()
                                        + ", error="
                                        + error.getCode());
                    }
                });
    }

    public interface AddResendMessageCallBack {
        void onComplete(Message message, NCError errorCode);
    }

    interface ReSendMessageCallback {
        void onCancel(Message message);

        void onAttached(Message message);

        void onSuccess(Message message);

        void onError(Message message, NCError coreErrorCode);
    }
}
