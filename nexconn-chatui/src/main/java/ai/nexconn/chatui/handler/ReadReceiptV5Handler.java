package ai.nexconn.chatui.handler;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.DirectChannel;
import ai.nexconn.chat.channel.GroupChannel;
import ai.nexconn.chat.channel.OpenChannel;
import ai.nexconn.chat.channel.SystemChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.MessageHandler;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.model.MessageDirection;
import ai.nexconn.chat.message.model.MessageIdentifier;
import ai.nexconn.chat.message.model.MessageReadReceiptResponse;
import ai.nexconn.chat.message.model.MessageReceiptResponseEvent;
import ai.nexconn.chat.message.model.ReadReceiptInfo;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.channel.feature.expose.SubmitReadReceiptV5Manager;
import ai.nexconn.chatui.utils.log.RLog;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data handler for V5 read-receipt operations.
 *
 * <p>Manages sending, receiving, and displaying V5 read receipts for direct and group channels.
 *
 * @since 5.30.0
 */
public class ReadReceiptV5Handler extends MultiDataHandler {

    private static final String TAG = "ReadReceiptV5Handler";

    /**
     * Data key for V5 read-receipt info queried when loading messages; falls through immediately if
     * V5 is not enabled.
     */
    public static final DataKey<HashMap<String, ReadReceiptInfo>>
            KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5 =
                    DataKey.obtain(
                            "KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5",
                            (Class<HashMap<String, ReadReceiptInfo>>) (Class<?>) HashMap.class);

    /** Data key for V5 read-receipt listener events that trigger a UI refresh. */
    public static final DataKey<HashMap<String, ReadReceiptInfo>>
            KEY_MESSAGE_READ_RECEIPT_V5_LISTENER =
                    DataKey.obtain(
                            "KEY_MESSAGE_READ_RECEIPT_V5_LISTENER",
                            (Class<HashMap<String, ReadReceiptInfo>>) (Class<?>) HashMap.class);

    /**
     * Data key for querying V5 read-receipt info of the last direct-channel message in the channel
     * list.
     */
    public static final DataKey<HashMap<String, ReadReceiptInfo>>
            KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5_BY_IDENTIFIER =
                    DataKey.obtain(
                            "KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5_BY_IDENTIFIER",
                            (Class<HashMap<String, ReadReceiptInfo>>) (Class<?>) HashMap.class);

    // Maximum batch size per request
    private static final int MAX_BATCH_SIZE = 100;

    /** Batch-submit manager for sending V5 read-receipt responses. */
    private final SubmitReadReceiptV5Manager mSubmitReadReceiptV5Manager =
            new SubmitReadReceiptV5Manager();

    /** Set of message IDs for which sendReadReceiptResponseV5 has been called. */
    private final Set<String> mPendingSendReadReceiptResponseV5Cache = new HashSet<>();

    // Cache map keyed by channel identifier; only the last message's read-receipt info is cached
    // per channel
    private final ConcurrentHashMap<String, ReadReceiptInfo> readReceiptInfoCache =
            new ConcurrentHashMap<>();

    private final MessageHandler mMessageHandler =
            new MessageHandler() {
                @Override
                public void onMessageReceiptResponse(MessageReceiptResponseEvent event) {
                    if (event == null || event.getResponses() == null) {
                        return;
                    }
                    HashMap<String, ReadReceiptInfo> map = new HashMap<>();
                    for (MessageReadReceiptResponse response : event.getResponses()) {
                        ChannelIdentifier channelId = response.getChannelIdentifier();
                        if (channelId == null || TextUtils.isEmpty(response.getMessageId())) {
                            continue;
                        }
                        ReadReceiptInfo info =
                                new ReadReceiptInfo(
                                        channelId.getChannelType(),
                                        channelId.getChannelId(),
                                        response.getMessageId(),
                                        response.getUnreadCount(),
                                        response.getReadCount(),
                                        response.getTotalCount());
                        map.put(response.getMessageId(), info);
                        readReceiptInfoCache.put(generateCacheKey(channelId), info);
                    }
                    if (!map.isEmpty()) {
                        notifyDataChange(KEY_MESSAGE_READ_RECEIPT_V5_LISTENER, map);
                    }
                }
            };

    private final String mHandlerId = "ReadReceiptV5Handler_" + hashCode();

    public ReadReceiptV5Handler() {
        super();
        NCEngine.addMessageHandler(mHandlerId, mMessageHandler);
    }

    @Override
    public void stop() {
        super.stop();
        NCEngine.removeMessageHandler(mHandlerId);
        mSubmitReadReceiptV5Manager.release();
        readReceiptInfoCache.clear();
    }

    /**
     * Binds a channel for batch read-receipt submission.
     *
     * @param id channel identifier
     */
    public void bindConversation(ChannelIdentifier id) {
        mSubmitReadReceiptV5Manager.bindConversation(
                new ChannelIdentifier(id.getChannelType(), id.getChannelId()));
    }

    /**
     * Batch-fetches V5 read-receipt info for the given messages.
     *
     * @param id channel identifier
     * @param messages list of messages to query
     */
    public void getMessageReadReceiptInfo(ChannelIdentifier id, List<Message> messages) {
        if (!AppSettingsHandler.getInstance().isReadReceiptV5Enabled(id.getChannelType())
                || messages == null
                || messages.isEmpty()) {
            notifyDataChange(KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5, new HashMap<>());
            return;
        }
        List<String> uIds = new ArrayList<>();
        for (Message message : messages) {
            if (message != null
                    && MessageDirection.SEND == message.getDirection()
                    && !TextUtils.isEmpty(message.getMessageId())
                    && message.getNeedReceipt()) {
                uIds.add(message.getMessageId());
            }
        }
        if (uIds.isEmpty()) {
            notifyDataChange(KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5, new HashMap<>());
            return;
        }
        createChannel(id)
                .getMessageReadReceiptInfo(
                        uIds,
                        new OperationHandler<List<ReadReceiptInfo>>() {
                            @Override
                            public void onResult(
                                    List<ReadReceiptInfo> readReceiptInfoV5s, NCError error) {
                                if (error != null) {
                                    notifyDataChange(
                                            KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5, new HashMap<>());
                                    return;
                                }
                                HashMap<String, ReadReceiptInfo> map = new HashMap<>();
                                if (readReceiptInfoV5s != null && !readReceiptInfoV5s.isEmpty()) {
                                    for (ReadReceiptInfo infoV5 : readReceiptInfoV5s) {
                                        map.put(infoV5.getMessageId(), infoV5);
                                    }
                                }
                                notifyDataChange(KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5, map);
                            }
                        });
    }

    /**
     * Sends a V5 read-receipt response for the given message.
     *
     * @param message the message to acknowledge
     */
    public void sendReadReceiptResponseV5(Message message) {
        if (message == null || TextUtils.isEmpty(message.getMessageId())) {
            return;
        }
        if (mPendingSendReadReceiptResponseV5Cache.contains(message.getMessageId())) {
            return;
        }
        mPendingSendReadReceiptResponseV5Cache.add(message.getMessageId());
        if (!AppSettingsHandler.getInstance()
                        .isReadReceiptV5Enabled(message.getChannelIdentifier().getChannelType())
                || TextUtils.isEmpty(message.getMessageId())
                || !message.getNeedReceipt()
                || message.getSentReceipt()
                || MessageDirection.SEND == message.getDirection()) {
            return;
        }
        mSubmitReadReceiptV5Manager.addSubmitTask(message.getMessageId());
    }

    /**
     * Generates a cache key from the channel identifier.
     *
     * @param identifier channel identifier
     * @return cache key string
     */
    private String generateCacheKey(ChannelIdentifier identifier) {
        return identifier.getChannelType().getValue() + "_" + identifier.getChannelId();
    }

    /** Entry point for fetching V5 read-receipt info by nexconn MessageIdentifier. */
    public void getMessageReadReceiptInfoByNcIdentifiers(
            @NonNull List<MessageIdentifier> ncIdentifiers) {
        getMessageReadReceiptInfoByIdentifiers(ncIdentifiers);
    }

    /** Entry point for fetching V5 read-receipt info by message identifiers. */
    public void getMessageReadReceiptInfoByIdentifiers(
            @NonNull List<MessageIdentifier> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return;
        }

        List<MessageIdentifier> unCachedIdentifiers = new ArrayList<>();
        HashMap<String, ReadReceiptInfo> cacheMap = new HashMap<>();

        // Check cache and collect results without aggregation
        for (MessageIdentifier identifier : identifiers) {
            String cacheKey = generateCacheKey(identifier.getChannelIdentifier());
            ReadReceiptInfo cachedInfo = readReceiptInfoCache.get(cacheKey);
            if (cachedInfo != null) {
                // Use cache only when the cached message UID matches the requested one
                if (cachedInfo.getMessageId() != null
                        && cachedInfo.getMessageId().equals(identifier.getMessageId())) {
                    cacheMap.put(cachedInfo.getMessageId(), cachedInfo);
                } else {
                    // Cached entry is for a different message — need to re-fetch
                    unCachedIdentifiers.add(identifier);
                }
            } else {
                unCachedIdentifiers.add(identifier);
            }
        }

        // Notify cached results first if available
        if (!cacheMap.isEmpty()) {
            notifyDataChange(KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5_BY_IDENTIFIER, cacheMap);
        }

        // All messages were cached — nothing more to fetch
        if (unCachedIdentifiers.isEmpty()) {
            return;
        }

        // Process uncached messages in batches
        if (unCachedIdentifiers.size() <= MAX_BATCH_SIZE) {
            getMessageReadReceiptInfoByIdentifierBatch(unCachedIdentifiers);
        } else {
            RLog.d(
                    TAG,
                    "getMessageReadReceiptInfoByIdentifier: splitting "
                            + unCachedIdentifiers.size()
                            + " messages into batches");
            for (int i = 0; i < unCachedIdentifiers.size(); i += MAX_BATCH_SIZE) {
                int endIndex = Math.min(i + MAX_BATCH_SIZE, unCachedIdentifiers.size());
                List<MessageIdentifier> batch = unCachedIdentifiers.subList(i, endIndex);
                getMessageReadReceiptInfoByIdentifierBatch(batch);
            }
        }
    }

    /**
     * Fetches V5 read-receipt info for a single batch of message identifiers.
     *
     * @param identifiers list of message identifiers (max 100)
     */
    private void getMessageReadReceiptInfoByIdentifierBatch(
            @NonNull List<MessageIdentifier> identifiers) {
        if (identifiers.isEmpty()) {
            return;
        }
        BaseChannel.getMessageReadReceiptInfoByIdentifiers(
                identifiers,
                new OperationHandler<List<ReadReceiptInfo>>() {
                    @Override
                    public void onResult(List<ReadReceiptInfo> readReceiptInfoV5s, NCError error) {
                        if (error != null) {
                            RLog.e(
                                    TAG,
                                    "getMessageReadReceiptInfoByIdentifierBatch onError: "
                                            + error
                                            + ", batch size: "
                                            + identifiers.size());
                            return;
                        }
                        RLog.d(
                                TAG,
                                "getMessageReadReceiptInfoByIdentifierBatch onSuccess, batch size: "
                                        + identifiers.size());
                        if (readReceiptInfoV5s == null || readReceiptInfoV5s.isEmpty()) {
                            return;
                        }
                        HashMap<String, ReadReceiptInfo> resultMap = new HashMap<>();
                        for (ReadReceiptInfo info : readReceiptInfoV5s) {
                            ChannelIdentifier infoId =
                                    new ChannelIdentifier(
                                            info.getChannelType(), info.getChannelId());
                            String cacheKey = generateCacheKey(infoId);
                            readReceiptInfoCache.put(cacheKey, info);
                            resultMap.put(info.getMessageId(), info);
                            RLog.d(
                                    TAG,
                                    "Updated cache for conversation: "
                                            + cacheKey
                                            + ", messageUId: "
                                            + info.getMessageId());
                        }
                        if (!resultMap.isEmpty()) {
                            notifyDataChange(
                                    KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5_BY_IDENTIFIER, resultMap);
                        }
                    }
                });
    }

    /** Converts a ReadReceiptInfo HashMap for data-bridge use by channel-list and other modules. */
    public static HashMap<String, ai.nexconn.chat.message.model.ReadReceiptInfo> toNcReceiptMap(
            HashMap<String, ReadReceiptInfo> data) {
        if (data == null) return new HashMap<>();
        // ReadReceiptInfo is already ai.nexconn.chat.message.model.ReadReceiptInfo
        return new HashMap<>(data);
    }

    private static BaseChannel createChannel(ChannelIdentifier id) {
        if (id.getChannelType() == ChannelType.GROUP) return new GroupChannel(id.getChannelId());
        if (id.getChannelType() == ChannelType.OPEN) return new OpenChannel(id.getChannelId());
        if (id.getChannelType() == ChannelType.SYSTEM) return new SystemChannel(id.getChannelId());
        return new DirectChannel(id.getChannelId());
    }
}
