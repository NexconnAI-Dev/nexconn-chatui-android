package ai.nexconn.chatui;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.CommunityChannel;
import ai.nexconn.chat.channel.DirectChannel;
import ai.nexconn.chat.channel.GroupChannel;
import ai.nexconn.chat.channel.OpenChannel;
import ai.nexconn.chat.channel.SystemChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.CommunitySubChannelIdentifier;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.ConnectHandler;
import ai.nexconn.chat.handler.ErrorHandler;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.handler.SendMediaMessageHandler;
import ai.nexconn.chat.handler.SendMessageHandler;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.params.ConnectParams;
import ai.nexconn.chat.params.InitParams;
import ai.nexconn.chat.params.InsertMessageItem;
import ai.nexconn.chat.params.SendMediaMessageParams;
import ai.nexconn.chat.params.SendMessageParams;
import ai.nexconn.chatui.channel.event.action.DeleteEvent;
import ai.nexconn.chatui.channel.event.action.InsertEvent;
import ai.nexconn.chatui.channel.event.action.MessageEventListener;
import ai.nexconn.chatui.channel.event.action.RefreshEvent;
import ai.nexconn.chatui.channel.event.action.ReplaceEvent;
import ai.nexconn.chatui.channel.event.action.SendEvent;
import ai.nexconn.chatui.channel.event.action.SendMediaEvent;
import ai.nexconn.chatui.channel.extension.NCExtensionManager;
import ai.nexconn.chatui.handler.AppSettingsHandler;
import ai.nexconn.chatui.handler.ChannelEventListener;
import ai.nexconn.chatui.manager.OnLineStatusManager;
import ai.nexconn.chatui.manager.hqvoicemessage.HQVoiceMsgDownloadManager;
import ai.nexconn.chatui.notification.ChatUINotificationManager;
import ai.nexconn.chatui.notification.MessageNotificationHelper;
import ai.nexconn.chatui.utils.language.NCConfigurationManager;
import ai.nexconn.chatui.utils.text.ChatUIDateUtils;
import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NCChatUI — the main entry point for the ChatUI SDK.
 *
 * <p>Wraps nexconn-chat initialization and connection, provides a {@link ChatUIFragmentFactory} for
 * all UI screens, and exposes an internal event bus for message and channel events.
 *
 * <ul>
 *   <li>Call {@link #initialize(InitParams)} once in {@link android.app.Application#onCreate()}.
 *   <li>Call {@link #connect(ConnectParams, ConnectHandler)} after a successful user login.
 *   <li>Use {@link #getFragmentFactory()} (or {@link #setFragmentFactory}) to launch any screen.
 * </ul>
 *
 * <pre>{@code
 * // 1. Initialize in Application.onCreate
 * NCChatUI.initialize(new InitParams(context, "your-app-key"));
 *
 * // 2. Connect after login
 * NCChatUI.connect(new ConnectParams("user-token"), (userId, error) -> {
 *     if (error == null) { // connected
 *     }
 * });
 *
 * // 3. Replace any screen with a custom Fragment
 * NCChatUI.setFragmentFactory(new MyCustomFragmentFactory());
 * }</pre>
 *
 * @see ChatUIFragmentFactory
 * @see ai.nexconn.chat.NCEngine
 */
public final class NCChatUI {

    private static Context sContext;
    private static ChatUIFragmentFactory sFragmentFactory = new ChatUIFragmentFactory();
    private static final List<MessageEventListener> sMessageEventListeners =
            new CopyOnWriteArrayList<>();
    private static final List<ChannelEventListener> sChannelEventListeners =
            new CopyOnWriteArrayList<>();
    private static final List<Runnable> sCancelSendMediaListeners = new CopyOnWriteArrayList<>();

    private NCChatUI() {}

    // ========================================
    // Initialization and connection
    // ========================================

    /**
     * Initializes the SDK. Must be called once in {@link android.app.Application#onCreate()}.
     *
     * @param params initialization parameters — required: context and appKey; optional: naviServer,
     *     areaCode, logLevel, etc.
     */
    public static void initialize(@NonNull InitParams params) {
        sContext = params.getContext().getApplicationContext();
        NCEngine.initialize(params);
        initKitInfrastructure(sContext, params.getAppKey());
    }

    /**
     * Stores an application context for built-in Chat UI pages when the host app initialized only the
     * lower Chat SDK entry point.
     *
     * <p>This is only a crash guard for page startup and does not replace {@link #initialize}.
     */
    public static void ensureContext(@NonNull Context context) {
        synchronized (NCChatUI.class) {
            if (sContext == null) {
                sContext = context.getApplicationContext();
            }
        }
    }

    private static void initKitInfrastructure(Context context, String appKey) {
        ai.nexconn.chatui.config.NCChatUIConfig.syncFromXml(context);
        NCExtensionManager.getInstance().init(context, appKey);
        HQVoiceMsgDownloadManager.getInstance().init(context);
        if (context instanceof Application) {
            ChatUINotificationManager.getInstance().init((Application) context);
        }
        NCConfigurationManager.init(context);
        ChatUIDateUtils.preloadDateFormats(context);
        OnLineStatusManager.getInstance().init();
        MessageNotificationHelper.setPushNotifyLevelListener();
    }

    /**
     * Returns the Application context saved during initialization.
     *
     * @return the ApplicationContext, or {@code null} if {@link #initialize} has not been called
     */
    @Nullable
    public static Context getContext() {
        return sContext;
    }

    /**
     * Returns the ChatUI SDK version.
     *
     * <p>The value comes from {@code UNIVERSAL_NEXCONN_VERSION} and is injected into {@link
     * BuildConfig#VERSION_NAME} at build time.
     */
    @NonNull
    public static String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * Connects to the server using a business token.
     *
     * <p>Must be called after {@link #initialize}.
     *
     * @param params connection parameters — required: token; optional: timeout, reconnectKickEnable
     * @param handler connection result callback; delivers userId on success or an error on failure
     */
    public static void connect(@NonNull ConnectParams params, @Nullable ConnectHandler handler) {
        NCEngine.connect(params, handler);
    }

    /**
     * Disconnects from the server while keeping the push channel alive.
     *
     * <p>Call this when the user logs out but still wants to receive push notifications. Use {@link
     * #logout()} to also close the push channel.
     */
    public static void disconnect() {
        NCEngine.disconnect(false);
        NCExtensionManager.getInstance().disconnect();
    }

    /** Disconnects from the server and closes the push channel. */
    public static void logout() {
        NCEngine.disconnect(true);
        NCExtensionManager.getInstance().disconnect();
    }

    /**
     * Sets whether combined message forwarding is enabled.
     *
     * <p>Default is {@code false}. When enabled, messages can be forwarded as combined messages.
     * Currently supported only in direct and group channels.
     *
     * @param enable {@code true} to show the combined forwarding option
     */
    public static void setEnableSendCombineMessage(boolean enable) {
        ai.nexconn.chatui.config.NCChatUIConfig.channelConfig()
                .setEnableSendCombineMessage(enable);
    }

    /**
     * Returns whether combined message forwarding is enabled.
     *
     * @return {@code true} if combined forwarding is enabled
     */
    public static boolean isEnableSendCombineMessage() {
        return ai.nexconn.chatui.config.NCChatUIConfig.channelConfig()
                .isEnableSendCombineMessage();
    }

    // ========================================
    // Channel factory helpers
    // ========================================

    /**
     * Creates the appropriate {@link BaseChannel} subtype for the given {@link ChannelIdentifier}.
     *
     * @param identifier the channel identifier
     * @return a typed {@link BaseChannel} instance
     */
    @NonNull
    public static BaseChannel createChannel(@NonNull ChannelIdentifier identifier) {
        if (identifier.getChannelType() == null) {
            return new DirectChannel(identifier.getChannelId());
        }
        switch (identifier.getChannelType()) {
            case GROUP:
                return new GroupChannel(identifier.getChannelId());
            case OPEN:
                return new OpenChannel(identifier.getChannelId());
            case SYSTEM:
                return new SystemChannel(identifier.getChannelId());
            case COMMUNITY:
                CommunityChannel channel = new CommunityChannel(identifier.getChannelId());
                if (identifier instanceof CommunitySubChannelIdentifier) {
                    channel.setSubChannelId(
                            ((CommunitySubChannelIdentifier) identifier).getSubChannelId());
                }
                return channel;
            default:
                return new DirectChannel(identifier.getChannelId());
        }
    }

    // ========================================
    // Message event listener management (internal event bus)
    // ========================================

    /**
     * Removes a message from the UI by posting a DeleteEvent. This is used internally when a
     * message needs to be removed from the UI, such as when a resent message creates a new message
     * object.
     *
     * @param message the message to remove from UI
     */
    public static void removeMessageFromUI(@NonNull Message message) {
        DeleteEvent event =
                new DeleteEvent(
                        message.getChannelIdentifier().getChannelType(),
                        message.getChannelIdentifier().getChannelId(),
                        new int[] {message.getClientId()});
        for (MessageEventListener listener : sMessageEventListeners) {
            listener.onDeleteMessage(event);
        }
    }

    /**
     * Replaces a message in the UI with a newly-created copy while keeping its list position. Used
     * by the resend flow when a queued message is re-sent as a new message (new clientId) after
     * network recovery, so the resent message does not jump to the bottom of the list.
     *
     * @param originalMessage the original message being replaced
     * @param newMessage the newly-created message that should take the original's position
     */
    public static void replaceMessageInUI(
            @NonNull Message originalMessage, @NonNull Message newMessage) {
        ReplaceEvent event = new ReplaceEvent(originalMessage.getClientId(), newMessage);
        for (MessageEventListener listener : sMessageEventListeners) {
            listener.onReplaceMessage(event);
        }
    }

    /** Registers a message event listener. Duplicate listeners are ignored. */
    public static void addMessageEventListener(@NonNull MessageEventListener listener) {
        if (!sMessageEventListeners.contains(listener)) {
            sMessageEventListeners.add(listener);
        }
    }

    /** Unregisters a previously registered message event listener. */
    public static void removeMessageEventListener(@NonNull MessageEventListener listener) {
        sMessageEventListeners.remove(listener);
    }

    /** Returns the list of registered message event listeners. */
    public static List<MessageEventListener> getMessageEventListeners() {
        return sMessageEventListeners;
    }

    /**
     * Dispatches a message refresh event to all registered {@link MessageEventListener}s.
     *
     * @param event the refresh event to dispatch
     */
    public static void refreshMessage(@NonNull RefreshEvent event) {
        for (MessageEventListener l : sMessageEventListeners) {
            l.onRefreshEvent(event);
        }
    }

    /**
     * Notifies all registered {@link MessageEventListener}s that all messages in the given channel
     * have been cleared locally (e.g. via {@code deleteMessagesForMeByTimestamp}). Any open chat
     * page bound to that channel will drop its cached messages and reload the list so the UI
     * reflects the cleared state immediately instead of waiting for the next fresh entry.
     *
     * @param identifier target channel whose messages have been cleared
     */
    public static void notifyChannelMessagesCleared(@NonNull ChannelIdentifier identifier) {
        for (MessageEventListener l : sMessageEventListeners) {
            l.onChannelMessagesCleared(identifier);
        }
    }

    /**
     * Sends a text/binary message and dispatches send-lifecycle events to all registered {@link
     * MessageEventListener}s.
     *
     * @param identifier target channel identifier
     * @param params send parameters
     * @param handler send result callback (may be {@code null})
     */
    public static void sendMessage(
            @NonNull ChannelIdentifier identifier,
            @NonNull SendMessageParams params,
            @Nullable SendMessageHandler handler) {
        applyReadReceiptIfNeeded(identifier.getChannelType(), params);
        createChannel(identifier)
                .sendMessage(
                        params,
                        new SendMessageHandler() {
                            @Override
                            public void onAttached(@NonNull Message message) {
                                fireMessageEvent(
                                        l ->
                                                l.onSendMessage(
                                                        new SendEvent(SendEvent.ATTACH, message)));
                                if (handler != null) handler.onAttached(message);
                            }

                            @Override
                            public void onResult(
                                    @Nullable Message message, @Nullable NCError error) {
                                if (error == null) {
                                    fireMessageEvent(
                                            l ->
                                                    l.onSendMessage(
                                                            new SendEvent(
                                                                    SendEvent.SUCCESS, message)));
                                } else {
                                    fireMessageEvent(
                                            l ->
                                                    l.onSendMessage(
                                                            new SendEvent(
                                                                    SendEvent.ERROR,
                                                                    message,
                                                                    error)));
                                }
                                if (handler != null) handler.onResult(message, error);
                            }
                        });
    }

    /**
     * Sends a media message and dispatches send-lifecycle events to all registered {@link
     * MessageEventListener}s.
     *
     * @param identifier target channel identifier
     * @param params send media parameters
     * @param handler send result callback (may be {@code null})
     */
    public static void sendMediaMessage(
            @NonNull ChannelIdentifier identifier,
            @NonNull SendMediaMessageParams params,
            @Nullable SendMediaMessageHandler handler) {
        applyReadReceiptIfNeeded(identifier.getChannelType(), params);
        createChannel(identifier)
                .sendMediaMessage(
                        params,
                        new SendMediaMessageHandler() {
                            @Override
                            public void onAttached(@NonNull Message message) {
                                fireMessageEvent(
                                        l ->
                                                l.onSendMediaMessage(
                                                        new SendMediaEvent(
                                                                SendMediaEvent.ATTACH, message)));
                                if (handler != null) handler.onAttached(message);
                            }

                            @Override
                            public void onProgress(@NonNull Message message, int progress) {
                                fireMessageEvent(
                                        l ->
                                                l.onSendMediaMessage(
                                                        new SendMediaEvent(
                                                                SendMediaEvent.PROGRESS,
                                                                message,
                                                                progress)));
                                if (handler != null) handler.onProgress(message, progress);
                            }

                            @Override
                            public void onResult(
                                    @Nullable Message message, @Nullable NCError error) {
                                if (error == null) {
                                    fireMessageEvent(
                                            l ->
                                                    l.onSendMediaMessage(
                                                            new SendMediaEvent(
                                                                    SendMediaEvent.SUCCESS,
                                                                    message)));
                                } else {
                                    fireMessageEvent(
                                            l ->
                                                    l.onSendMediaMessage(
                                                            new SendMediaEvent(
                                                                    SendMediaEvent.ERROR,
                                                                    message,
                                                                    error)));
                                }
                                if (handler != null) handler.onResult(message, error);
                            }

                            @Override
                            public void onCanceled(@NonNull Message message) {
                                fireMessageEvent(
                                        l ->
                                                l.onSendMediaMessage(
                                                        new SendMediaEvent(
                                                                SendMediaEvent.CANCEL, message)));
                                if (handler != null) handler.onCanceled(message);
                            }
                        });
    }

    /**
     * Cancels an in-progress media message upload.
     *
     * <p>On success, all listeners registered via {@link #addCancelSendMediaMessageListener} are
     * notified so the UI can update the progress indicator.
     *
     * @param message the message to cancel (must be an object returned by {@link
     *     #sendMediaMessage})
     * @param handler cancellation result callback; {@code null} error means success (may be {@code
     *     null})
     */
    public static void cancelSendMediaMessage(
            @NonNull Message message, @Nullable ErrorHandler handler) {
        createChannel(message.getChannelIdentifier())
                .cancelSendMediaMessage(
                        message,
                        error -> {
                            for (Runnable listener : sCancelSendMediaListeners) {
                                listener.run();
                            }
                            if (handler != null) handler.onError(error);
                        });
    }

    /**
     * Deletes messages for the current user (local + server-side for self) and dispatches a {@link
     * DeleteEvent} to all registered {@link MessageEventListener}s.
     *
     * @param identifier target channel identifier
     * @param messages messages to delete
     * @param handler deletion result callback (may be {@code null})
     */
    public static void deleteMessages(
            @NonNull ChannelIdentifier identifier,
            @NonNull List<Message> messages,
            @Nullable ErrorHandler handler) {
        int[] ids = toClientIdArray(messages);
        createChannel(identifier)
                .deleteMessagesForMe(
                        messages,
                        error -> {
                            if (error == null) {
                                fireMessageEvent(
                                        l ->
                                                l.onDeleteMessage(
                                                        new DeleteEvent(
                                                                identifier.getChannelType(),
                                                                identifier.getChannelId(),
                                                                ids,
                                                                messages)));
                            }
                            if (handler != null) handler.onError(error);
                        });
    }

    /**
     * Deletes messages from the local database only, then dispatches a {@link DeleteEvent} so that
     * the message list refreshes immediately.
     *
     * @param identifier target channel identifier
     * @param messages messages to delete locally
     * @param handler deletion result callback (may be {@code null})
     */
    public static void deleteLocalMessages(
            @NonNull ChannelIdentifier identifier,
            @NonNull List<Message> messages,
            @Nullable ErrorHandler handler) {
        int[] ids = toClientIdArray(messages);
        List<String> stringIds = new ArrayList<>(messages.size());
        for (Message m : messages) {
            stringIds.add(String.valueOf(m.getClientId()));
        }
        createChannel(identifier)
                .deleteLocalMessages(
                        stringIds,
                        (result, error) -> {
                            if (error == null && Boolean.TRUE.equals(result)) {
                                fireMessageEvent(
                                        l ->
                                                l.onDeleteMessage(
                                                        new DeleteEvent(
                                                                identifier.getChannelType(),
                                                                identifier.getChannelId(),
                                                                ids,
                                                                messages)));
                            }
                            if (handler != null) handler.onError(error);
                        });
    }

    /**
     * Recalls (deletes for all participants) a message and dispatches a {@link DeleteEvent} to all
     * registered {@link MessageEventListener}s.
     *
     * @param identifier target channel identifier
     * @param message message to recall
     * @param handler recall result callback (may be {@code null})
     */
    public static void deleteMessageForAll(
            @NonNull ChannelIdentifier identifier,
            @NonNull Message message,
            @Nullable OperationHandler<Message> handler) {
        createChannel(identifier)
                .deleteMessageForAll(
                        message,
                        (result, error) -> {
                            if (error == null && result != null) {
                                fireMessageEvent(
                                        l ->
                                                l.onDeleteMessage(
                                                        new DeleteEvent(
                                                                identifier.getChannelType(),
                                                                identifier.getChannelId(),
                                                                new int[] {message.getClientId()},
                                                                java.util.Collections.singletonList(
                                                                        message))));
                            }
                            if (handler != null) handler.onResult(result, error);
                        });
    }

    /**
     * Inserts messages and dispatches an {@link InsertEvent} to all registered {@link
     * MessageEventListener}s.
     *
     * @param identifier target channel identifier
     * @param items messages to insert
     * @param handler insertion result callback (may be {@code null})
     */
    public static void insertMessages(
            @NonNull ChannelIdentifier identifier,
            @NonNull List<InsertMessageItem> items,
            @Nullable OperationHandler<Boolean> handler) {
        createChannel(identifier)
                .insertMessages(
                        items,
                        (result, error) -> {
                            if (error == null) {
                                fireMessageEvent(l -> l.onInsertMessage(new InsertEvent(null)));
                            }
                            if (handler != null) handler.onResult(result, error);
                        });
    }

    private static void fireMessageEvent(java.util.function.Consumer<MessageEventListener> action) {
        for (MessageEventListener l : sMessageEventListeners) {
            action.accept(l);
        }
    }

    // ========================================
    // Channel event listener management (internal event bus)
    // ========================================

    /** Registers a channel event listener. Duplicate listeners are ignored. */
    public static void addChannelEventListener(@NonNull ChannelEventListener listener) {
        if (!sChannelEventListeners.contains(listener)) {
            sChannelEventListeners.add(listener);
        }
    }

    /** Unregisters a previously registered channel event listener. */
    public static void removeChannelEventListener(@NonNull ChannelEventListener listener) {
        sChannelEventListeners.remove(listener);
    }

    // ========================================
    // Channel operations (trigger ChannelEventListener callbacks)
    // ========================================

    /**
     * Saves a draft for the given channel and notifies all registered {@link ChannelEventListener}s
     * via {@link ChannelEventListener#onSaveDraft} on success.
     *
     * @param channelIdentifier target channel
     * @param draft draft content
     * @param handler operation result callback (may be {@code null})
     */
    public static void saveDraft(
            @NonNull ChannelIdentifier channelIdentifier,
            @NonNull String draft,
            @Nullable OperationHandler<Boolean> handler) {
        createChannel(channelIdentifier)
                .saveDraft(
                        draft,
                        (result, error) -> {
                            if (handler != null) handler.onResult(result, error);
                            if (error == null) {
                                for (ChannelEventListener l : sChannelEventListeners) {
                                    l.onSaveDraft(channelIdentifier, draft);
                                }
                            }
                        });
    }

    /**
     * Clears unread count for a channel and notifies all registered {@link ChannelEventListener}s via
     * {@link ChannelEventListener#onClearedUnreadStatus} on success.
     *
     * @param channelIdentifier target channel
     * @param handler operation result callback (may be {@code null})
     */
    public static void clearUnreadCount(
            @NonNull ChannelIdentifier channelIdentifier,
            @Nullable OperationHandler<Boolean> handler) {
        createChannel(channelIdentifier)
                .clearUnreadCount(
                        (result, error) -> {
                            if (handler != null) handler.onResult(result, error);
                            if (error == null && Boolean.TRUE.equals(result)) {
                                for (ChannelEventListener l : sChannelEventListeners) {
                                    l.onClearedUnreadStatus(channelIdentifier);
                                }
                            }
                        });
    }

    // ========================================
    // Cancel-send-media listener management (internal event bus)
    // ========================================

    /** Registers a cancel-send-media listener. Duplicate listeners are ignored. */
    public static void addCancelSendMediaMessageListener(@NonNull Runnable listener) {
        if (!sCancelSendMediaListeners.contains(listener)) {
            sCancelSendMediaListeners.add(listener);
        }
    }

    /** Unregisters a cancel-send-media listener. */
    public static void removeCancelSendMediaMessageListener(@NonNull Runnable listener) {
        sCancelSendMediaListeners.remove(listener);
    }

    /** Returns the list of registered cancel-send-media listeners. */
    public static List<Runnable> getCancelSendMediaListeners() {
        return sCancelSendMediaListeners;
    }

    // ========================================
    // Fragment factory
    // ========================================

    /**
     * Returns whether the SDK has been initialized.
     *
     * @return {@code true} if {@link #initialize} has been called; {@code false} otherwise
     */
    public static boolean isInitialized() {
        return sContext != null;
    }

    /**
     * Returns the current {@link ChatUIFragmentFactory}.
     *
     * <p>Subclass {@link ChatUIFragmentFactory} and override any {@code newXxxFragment()} method to
     * replace the default screen implementation.
     */
    @NonNull
    public static ChatUIFragmentFactory getFragmentFactory() {
        return sFragmentFactory;
    }

    /**
     * Replaces the current {@link ChatUIFragmentFactory}.
     *
     * @param factory a custom factory extending {@link ChatUIFragmentFactory}
     */
    public static void setFragmentFactory(@NonNull ChatUIFragmentFactory factory) {
        sFragmentFactory = factory;
    }

    // ---- Internal utilities ----

    /** Converts a message list to a clientId array (API 21 compatible, no stream API). */
    private static int[] toClientIdArray(@NonNull List<Message> messages) {
        int[] ids = new int[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            ids[i] = messages.get(i).getClientId();
        }
        return ids;
    }

    private static void applyReadReceiptIfNeeded(
            @Nullable ChannelType type, @NonNull SendMessageParams params) {
        if (type == null || params.getNeedReceipt()) {
            return;
        }
        if (AppSettingsHandler.getInstance().isReadReceiptV5Enabled(type)) {
            params.setNeedReceipt(true);
        }
    }

    private static void applyReadReceiptIfNeeded(
            @Nullable ChannelType type, @NonNull SendMediaMessageParams params) {
        if (type == null || params.getNeedReceipt()) {
            return;
        }
        if (AppSettingsHandler.getInstance().isReadReceiptV5Enabled(type)) {
            params.setNeedReceipt(true);
        }
    }
}
