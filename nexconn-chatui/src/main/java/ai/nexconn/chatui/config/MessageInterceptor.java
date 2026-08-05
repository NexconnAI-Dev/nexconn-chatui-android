package ai.nexconn.chatui.config;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.message.ImageMessage;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.TextMessage;
import ai.nexconn.chat.message.model.MessageReceivedStatusInfo;
import ai.nexconn.chat.message.model.SentStatus;

/**
 * Interceptor for message send and receive operations.
 *
 * <p>Register an implementation via {@link ai.nexconn.chatui.config.FeatureConfig} to intercept
 * outgoing and incoming messages before the SDK processes them.
 *
 * <p>All intercept methods return {@code true} to consume the event (no further SDK processing) or
 * {@code false} to pass it through to the SDK.
 */
public interface MessageInterceptor {

    /**
     * Called when a real-time or offline message is received.
     *
     * <p>Offline message batching notes:
     *
     * <ul>
     *   <li>The server packs up to 200 messages per batch.
     *   <li>{@code hasPackage} indicates whether additional batches remain on the server.
     *   <li>{@code left} indicates remaining messages in the current batch after this one.
     *   <li>Offline sync is complete when both {@code hasPackage} and {@code left} are {@code 0}.
     * </ul>
     *
     * @param message the received message
     * @param left remaining message count in the current batch
     * @param hasPackage whether additional message batches remain on the server
     * @param offline whether this message is an offline message
     * @return {@code true} to intercept (SDK will not process further); {@code false} to pass
     *     through
     */
    boolean interceptReceivedMessage(
            Message message, int left, boolean hasPackage, boolean offline);

    /**
     * Called before a message is sent.
     *
     * @param message the message about to be sent
     * @return {@code true} to intercept; {@code false} to pass through
     */
    boolean interceptOnSendMessage(Message message);

    /**
     * Called after a message is successfully sent.
     *
     * @param message the sent message
     * @return {@code true} to intercept; {@code false} to pass through
     */
    boolean interceptOnSentMessage(Message message);

    /**
     * Called before an outgoing message is inserted into the local database.
     *
     * @param type channel type
     * @param targetId target channel ID (peer ID for direct, group ID for group, etc.)
     * @param sentStatus sent status of the message
     * @param content message content (e.g. {@link TextMessage}, {@link ImageMessage})
     * @param sentTime sent timestamp from {@link Message#getSentTime()}
     * @return {@code true} to intercept; {@code false} to pass through
     */
    boolean interceptOnInsertOutgoingMessage(
            ChannelType type,
            String targetId,
            SentStatus sentStatus,
            MessageContent content,
            long sentTime);

    /**
     * Called before an incoming message is inserted into the local database.
     *
     * @param type channel type
     * @param targetId target channel ID
     * @param senderId sender user ID
     * @param receivedStatus received status
     * @param content message content
     * @param sentTime sent timestamp
     * @return {@code true} to intercept; {@code false} to pass through
     */
    boolean interceptOnInsertIncomingMessage(
            ChannelType type,
            String targetId,
            String senderId,
            MessageReceivedStatusInfo receivedStatus,
            MessageContent content,
            long sentTime);

    /**
     * Called before an outgoing message is inserted, with an optional result callback.
     *
     * <p>Use this overload when you need to return an asynchronous result via the callback — for
     * example, to attach custom metadata to an image message before insertion.
     *
     * @param type channel type
     * @param targetId target channel ID
     * @param sentStatus sent status
     * @param content message content
     * @param time sent timestamp
     * @param callback callback to invoke with the modified message; call {@code onError} to signal
     *     that the message should be blocked
     * @return {@code true} to intercept; {@code false} to pass through
     */
    default boolean interceptOnInsertOutgoingMessage(
            ChannelType type,
            String targetId,
            SentStatus sentStatus,
            MessageContent content,
            long time,
            OperationHandler<Message> callback) {
        return interceptOnInsertOutgoingMessage(type, targetId, sentStatus, content, time);
    }
}
