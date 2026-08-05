package ai.nexconn.chatui.handler;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.MessageHandler;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.model.SpeechToTextCompletedEvent;
import ai.nexconn.chat.params.GetMessageByIdParams;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.model.UiMessage;
import androidx.annotation.NonNull;

/**
 * Data handler for speech-to-text operations on voice messages.
 *
 * <p>Manages:
 *
 * <ul>
 *   <li>Initiating speech-to-text requests
 *   <li>Setting the visibility of transcription results
 *   <li>Listening for transcription result callbacks
 *   <li>Managing error states for transcription operations
 * </ul>
 *
 * @since 5.22.0
 */
public class SpeechToTextHandler extends MultiDataHandler {

    private static final String TAG = "SpeechToTextHandler";

    public static final DataKey<UiMessage> KEY_SPEECH_TO_TEXT_LISTENER =
            DataKey.obtain("KEY_SPEECH_TO_TEXT_LISTENER", UiMessage.class);

    public static final DataKey<UiMessage> KEY_REQUEST_SPEECH_TO_TEXT =
            DataKey.obtain("KEY_REQUEST_SPEECH_TO_TEXT", UiMessage.class);

    public static final DataKey<UiMessage> KEY_SET_SPEECH_TO_TEXT_VISIBLE =
            DataKey.obtain("KEY_SET_SPEECH_TO_TEXT_VISIBLE", UiMessage.class);

    /** Business-state identifier for hidden speech-to-text results. */
    public static final String SPEECH_TO_TEXT_HIDDEN_STATE = "SpeechToText_Hidden";

    /** Business-state identifier for in-progress speech-to-text loading. */
    public static final String SPEECH_TO_TEXT_LOADING_STATE = "SpeechToText_Loading";

    private final String mHandlerId = "SpeechToTextHandler_" + hashCode();

    private final MessageHandler mMessageHandler =
            new MessageHandler() {
                @Override
                public void onSpeechToTextCompleted(@NonNull SpeechToTextCompletedEvent event) {
                    if (event.getError() != null) {
                        notifyDataError(KEY_SPEECH_TO_TEXT_LISTENER, event.getError());
                        return;
                    }
                    String messageId = event.getMessageId();
                    if (messageId == null) {
                        return;
                    }
                    BaseChannel.getMessageById(
                            new GetMessageByIdParams(messageId, null),
                            new OperationHandler<Message>() {
                                @Override
                                public void onResult(Message message, NCError error) {
                                    if (error != null) {
                                        notifyDataError(KEY_SPEECH_TO_TEXT_LISTENER, error);
                                        return;
                                    }
                                    notifyDataChange(
                                            KEY_SPEECH_TO_TEXT_LISTENER, new UiMessage(message));
                                }
                            });
                }
            };

    public SpeechToTextHandler() {
        super();
        NCEngine.addMessageHandler(mHandlerId, mMessageHandler);
    }

    /**
     * Initiates a speech-to-text transcription request for a voice message.
     *
     * @param messageUId the unique message ID of the voice message to transcribe
     */
    public void requestSpeechToTextForMessage(String messageUId) {
        BaseChannel.getMessageById(
                new GetMessageByIdParams(messageUId, null),
                new OperationHandler<Message>() {
                    @Override
                    public void onResult(Message message, NCError error) {
                        if (error != null) {
                            notifyDataError(KEY_REQUEST_SPEECH_TO_TEXT, error);
                            return;
                        }
                        if (message == null) {
                            return;
                        }
                        message.requestSpeechToText(
                                requestError -> {
                                    if (requestError != null) {
                                        notifyDataError(
                                                KEY_REQUEST_SPEECH_TO_TEXT, requestError);
                                        return;
                                    }
                                    UiMessage uiMessage = new UiMessage(message);
                                    uiMessage.setBusinessState(SPEECH_TO_TEXT_LOADING_STATE);
                                    notifyDataChange(KEY_REQUEST_SPEECH_TO_TEXT, uiMessage);
                                });
                    }
                });
    }

    /**
     * Sets the visibility of a speech-to-text transcription result.
     *
     * @param messageId the client message ID
     * @param isVisible {@code true} to show the transcription; {@code false} to hide it
     */
    public void setMessageSpeechToTextVisible(final int messageId, final boolean isVisible) {
        GetMessageByIdParams params = new GetMessageByIdParams(null, messageId);
        BaseChannel.getMessageById(
                params,
                new OperationHandler<Message>() {
                    @Override
                    public void onResult(Message message, NCError error) {
                        if (error != null) {
                            notifyDataError(KEY_SET_SPEECH_TO_TEXT_VISIBLE, error);
                            return;
                        }
                        if (message == null) {
                            return;
                        }
                        message.setSpeechToTextVisible(
                                isVisible,
                                updateError -> {
                                    if (updateError != null) {
                                        notifyDataError(
                                                KEY_SET_SPEECH_TO_TEXT_VISIBLE, updateError);
                                        return;
                                    }
                                    BaseChannel.getMessageById(
                                            params,
                                            new OperationHandler<Message>() {
                                                @Override
                                                public void onResult(
                                                        Message updatedMessage, NCError queryError) {
                                                    if (queryError != null) {
                                                        notifyDataError(
                                                                KEY_SET_SPEECH_TO_TEXT_VISIBLE,
                                                                queryError);
                                                        return;
                                                    }
                                                    if (updatedMessage != null) {
                                                        notifyDataChange(
                                                                KEY_SET_SPEECH_TO_TEXT_VISIBLE,
                                                                new UiMessage(updatedMessage));
                                                    }
                                                }
                                            });
                                });
                    }
                });
    }

    @Override
    public void stop() {
        super.stop();
        NCEngine.removeMessageHandler(mHandlerId);
    }
}
