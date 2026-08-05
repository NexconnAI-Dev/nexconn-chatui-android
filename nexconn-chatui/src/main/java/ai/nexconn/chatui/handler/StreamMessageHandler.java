package ai.nexconn.chatui.handler;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.MessageHandler;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.model.StreamMessageDeltaEvent;
import ai.nexconn.chat.message.model.StreamMessageRequestCompleteEvent;
import ai.nexconn.chat.message.model.StreamMessageRequestInitEvent;
import ai.nexconn.chat.params.GetMessageByIdParams;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.log.RLog;
import androidx.annotation.NonNull;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Data handler for streaming message operations.
 *
 * <p>Manages fetching and delivering incremental stream message content. Deduplicates in-flight
 * requests and exposes data via {@link #KEY_FETCH_STREAM_MESSAGE}.
 *
 * @since 5.16.0
 */
public class StreamMessageHandler extends MultiDataHandler {

    private static final String TAG = "StreamMessageHandler";

    /** Business-state constants for stream message errors. */
    public interface State {
        String CONNECT_ERROR = "CONNECT_ERROR";
        String RETRY_PULL = "RETRY_PULL";
    }

    public static final DataKey<UiMessage> KEY_FETCH_STREAM_MESSAGE =
            DataKey.obtain("KEY_FETCH_STREAM_MESSAGE", UiMessage.class);

    private final String mHandlerId = "StreamMessageHandler_" + hashCode();

    private final CopyOnWriteArraySet<String> runningMsgSet = new CopyOnWriteArraySet<>();
    private final CopyOnWriteArraySet<String> hasPullMsgSet = new CopyOnWriteArraySet<>();

    private final MessageHandler mMessageHandler =
            new MessageHandler() {
                @Override
                public void onStreamMessageRequestInit(
                        @NonNull StreamMessageRequestInitEvent event) {
                    // no-op: initialization handled by fetchStreamMessage
                }

                @Override
                public void onStreamMessageRequestDelta(@NonNull StreamMessageDeltaEvent event) {
                    if (event.getMessage() != null) {
                        notifyDataChange(
                                KEY_FETCH_STREAM_MESSAGE, new UiMessage(event.getMessage()));
                    }
                }

                @Override
                public void onStreamMessageRequestComplete(
                        @NonNull StreamMessageRequestCompleteEvent event) {
                    runningMsgSet.remove(event.getMessageId());
                    BaseChannel.getMessageById(
                            new GetMessageByIdParams(event.getMessageId(), null),
                            new OperationHandler<Message>() {
                                @Override
                                public void onResult(Message message, NCError error) {
                                    if (error != null) {
                                        notifyDataError(KEY_FETCH_STREAM_MESSAGE, error);
                                        return;
                                    }
                                    if (message == null) {
                                        RLog.e(
                                                TAG,
                                                "onStreamMessageRequestComplete: message is null, id="
                                                        + event.getMessageId());
                                        return;
                                    }
                                    UiMessage uiMessage = new UiMessage(message);
                                    if (event.getError() != null
                                            && event.getError().getCode() == 39005) {
                                        uiMessage.setBusinessState(State.CONNECT_ERROR);
                                    }
                                    notifyDataChange(KEY_FETCH_STREAM_MESSAGE, uiMessage);
                                }
                            });
                }
            };

    public StreamMessageHandler() {
        super();
        NCEngine.addMessageHandler(mHandlerId, mMessageHandler);
    }

    /**
     * Fetches streaming message content for the given message UID.
     *
     * <p>Deduplicates concurrent requests for the same message. If the message has already been
     * fetched and {@code isRetry} is {@code false}, the request is skipped.
     *
     * @param msgUId the unique message ID of the stream message
     * @param isRetry {@code true} to force a re-fetch even if the message was already pulled
     */
    public void fetchStreamMessage(String msgUId, boolean isRetry) {
        boolean isNeedPull = isRetry || !hasPullMsgSet.contains(msgUId);
        if (!runningMsgSet.contains(msgUId) && isNeedPull) {
            runningMsgSet.add(msgUId);
            hasPullMsgSet.add(msgUId);
            BaseChannel.getMessageById(
                    new GetMessageByIdParams(msgUId, null),
                    new OperationHandler<Message>() {
                        @Override
                        public void onResult(Message message, NCError error) {
                            if (error != null || message == null) {
                                runningMsgSet.remove(msgUId);
                                if (error != null) {
                                    notifyDataError(KEY_FETCH_STREAM_MESSAGE, error);
                                }
                                return;
                            }
                            message.requestStreamMessage(
                                    requestError -> {
                                        if (requestError == null) return;
                                        notifyDataError(KEY_FETCH_STREAM_MESSAGE, requestError);
                                        if (requestError.getCode() != 39006) {
                                            runningMsgSet.remove(msgUId);
                                            BaseChannel.getMessageById(
                                                    new GetMessageByIdParams(msgUId, null),
                                                    new OperationHandler<Message>() {
                                                        @Override
                                                        public void onResult(
                                                                Message m, NCError err) {
                                                            if (err != null) {
                                                                notifyDataError(
                                                                        KEY_FETCH_STREAM_MESSAGE,
                                                                        err);
                                                                return;
                                                            }
                                                            UiMessage uiMessage = new UiMessage(m);
                                                            uiMessage.setBusinessState(
                                                                    State.CONNECT_ERROR);
                                                            notifyDataChange(
                                                                    KEY_FETCH_STREAM_MESSAGE,
                                                                    uiMessage);
                                                        }
                                                    });
                                        }
                                    });
                        }
                    });
        }
    }

    @Override
    public void stop() {
        super.stop();
        NCEngine.removeMessageHandler(mHandlerId);
    }
}
