package ai.nexconn.chatui.manager;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.MessageHandler;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.model.MessageDeletedEvent;
import ai.nexconn.chat.message.model.MessageDirection;
import ai.nexconn.chat.message.model.MessageReceivedEvent;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.channel.event.action.InsertEvent;
import ai.nexconn.chatui.channel.event.action.MessageEventListener;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.handler.ChannelEventListener;
import ai.nexconn.chatui.utils.log.RLog;
import androidx.annotation.NonNull;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

// with nexconn equivalent
public class UnReadMessageManager /* extends NCIMClient.OnReceiveMessageWrapperListener
        implements NCIMClient.SyncConversationReadStatusListener */ {
    private static final String TAG = "UnReadMessageManager";
    private final List<WeakReference<MultiConversationUnreadMsgInfo>> mMultiConversationUnreadInfos;
    private final List<MultiConversationUnreadMsgInfo> mForeverMultiConversationUnreadInfos;
    private ChannelEventListener mChannelEventListener =
            new ChannelEventListener() {
                @Override
                public void onClearedUnreadStatus(ChannelIdentifier channelIdentifier) {
                    syncUnreadCount();
                }
            };
    private MessageEventListener mMessageEventListener =
            new MessageEventListener() {
                @Override
                public void onInsertMessage(InsertEvent event) {
                    if (event == null) {
                        return;
                    }
                    Message message = event.getMessage();
                    if (message != null
                            && message.getDirection() == MessageDirection.RECEIVE
                            && !message.getReceivedStatusInfo().isRead()) {
                        syncUnreadCount();
                    }
                }
            };

    private final MessageHandler mMessageHandler =
            new MessageHandler() {
                @Override
                public void onMessageReceived(@NonNull MessageReceivedEvent event) {
                    // Sync unread count when last message is received (left==0 means queue is
                    // empty)
                    if (event.getLeft() == null || event.getLeft() == 0) {
                        syncUnreadCount();
                    }
                }

                @Override
                public void onMessageDeleted(@NonNull MessageDeletedEvent event) {
                    // Sync unread count after message recall
                    syncUnreadCount();
                }
            };

    // Replace NCIMClient.ConnectCallback with nexconn equivalent
    //    private NCIMClient.ConnectCallback connectCallback =
    //            new NCIMClient.ConnectCallback() {
    //                @Override
    //                public void onSuccess(String s) {
    //                    // do nothing
    //                }
    //
    //                @Override
    //                public void onError(NCIMClient.ConnectionErrorCode connectionErrorCode) {
    //                    // do nothing
    //                }
    //
    //                @Override
    //                public void onDatabaseOpened(NCIMClient.DatabaseOpenStatus
    // databaseOpenStatus) {
    //                    syncUnreadCount();
    //                }
    //            };

    // Recall (onMessageDeleted) is now handled in mMessageHandler above.

    private UnReadMessageManager() {
        this.mMultiConversationUnreadInfos = new ArrayList<>();
        this.mForeverMultiConversationUnreadInfos = new ArrayList<>();
        NCEngine.addMessageHandler("UnReadMessageManager", mMessageHandler);
        NCChatUI.addChannelEventListener(mChannelEventListener);
        NCChatUI.addMessageEventListener(mMessageEventListener);
    }

    public static UnReadMessageManager getInstance() {
        return SingletonHolder.sInstance;
    }

    public void onSyncConversationReadStatus(ChannelIdentifier channelIdentifier) {
        // Keep old contract: syncing read status should refresh aggregated unread badges.
        if (channelIdentifier == null) {
            return;
        }
        syncUnreadCount();
    }

    // onReceived → handled via mMessageHandler.onMessageReceived above

    private void syncUnreadCount() {
        syncWeakReferenceUnreadCount();
        syncForeverObserverUnreadCount();
    }

    private void syncWeakReferenceUnreadCount() {
        List<WeakReference<MultiConversationUnreadMsgInfo>> snapshot;
        synchronized (mMultiConversationUnreadInfos) {
            snapshot = new ArrayList<>(mMultiConversationUnreadInfos);
        }
        for (final WeakReference<MultiConversationUnreadMsgInfo> weakMsgInfo : snapshot) {
            if (weakMsgInfo == null) {
                continue;
            }
            MultiConversationUnreadMsgInfo msgInfo = weakMsgInfo.get();
            if (msgInfo == null) {
                continue;
            }
            getUnreadCountByTypes(
                    msgInfo.conversationTypes,
                    new OperationHandler<Integer>() {
                        @Override
                        public void onResult(Integer count, NCError error) {
                            if (error != null) return;
                            RLog.d(TAG, "get result: " + count);
                            MultiConversationUnreadMsgInfo info = weakMsgInfo.get();
                            if (info == null) return;
                            info.count = count;
                            info.observer.onCountChanged(count);
                        }
                    });
        }
    }

    private void syncForeverObserverUnreadCount() {
        List<MultiConversationUnreadMsgInfo> snapshot;
        synchronized (mForeverMultiConversationUnreadInfos) {
            snapshot = new ArrayList<>(mForeverMultiConversationUnreadInfos);
        }
        for (final MultiConversationUnreadMsgInfo msgInfo : snapshot) {
            if (msgInfo == null) {
                continue;
            }
            getUnreadCountByTypes(
                    msgInfo.conversationTypes,
                    new OperationHandler<Integer>() {
                        @Override
                        public void onResult(Integer count, NCError error) {
                            if (error != null) return;
                            RLog.d(TAG, "get result: " + count);
                            msgInfo.count = count;
                            msgInfo.observer.onCountChanged(count);
                        }
                    });
        }
    }

    private void getUnreadCountByTypes(ChannelType[] types, OperationHandler<Integer> callback) {
        BaseChannel.getUnreadChannels(
                types != null ? java.util.Arrays.asList(types) : java.util.Collections.emptyList(),
                new OperationHandler<List<BaseChannel>>() {
                    @Override
                    public void onResult(List<BaseChannel> channels, NCError error) {
                        if (error != null) {
                            callback.onResult(0, error);
                            return;
                        }
                        int total = 0;
                        if (channels != null) {
                            for (BaseChannel ch : channels) {
                                total += ch.getUnreadCount();
                            }
                        }
                        callback.onResult(total, null);
                    }
                });
    }

    /**
     * Sets a listener for unread message count changes. Note: if set in an activity, call {@link
     * UnReadMessageManager#removeObserver(UnReadMessageManager.IUnReadMessageObserver)} when the
     * activity is destroyed to avoid memory leaks.
     *
     * @param observer the listener for unread message count changes.
     * @param conversationTypes the channel types to monitor for unread messages.
     */
    public void addObserver(
            ChannelType[] conversationTypes, final IUnReadMessageObserver observer) {
        if (observer == null) {
            RLog.e(TAG, "can't add a null observer!");
            return;
        }
        if (conversationTypes == null) {
            conversationTypes =
                    NCChatUIConfig.channelListConfig().getDataProcessor().supportedTypes();
        }
        synchronized (mMultiConversationUnreadInfos) {
            final MultiConversationUnreadMsgInfo msgInfo = new MultiConversationUnreadMsgInfo();
            msgInfo.conversationTypes = conversationTypes;
            msgInfo.observer = observer;
            final WeakReference<MultiConversationUnreadMsgInfo> weakMsgInfo =
                    new WeakReference<>(msgInfo);
            mMultiConversationUnreadInfos.add(weakMsgInfo);
            getUnreadCountByTypes(
                    conversationTypes,
                    new OperationHandler<Integer>() {
                        @Override
                        public void onResult(Integer count, NCError error) {
                            if (error != null || weakMsgInfo.get() == null) return;
                            weakMsgInfo.get().count = count;
                            weakMsgInfo.get().observer.onCountChanged(count);
                        }
                    });
        }
    }

    /**
     * Sets a permanent listener for unread message count changes. Note: if set in an activity, call
     * {@link UnReadMessageManager#removeObserver(UnReadMessageManager.IUnReadMessageObserver)} when
     * the activity is destroyed to avoid memory leaks.
     *
     * @param observer the listener for unread message count changes.
     * @param conversationTypes the channel types to monitor for unread messages.
     */
    public void addForeverObserver(
            ChannelType[] conversationTypes, final IUnReadMessageObserver observer) {
        if (observer == null) {
            RLog.e(TAG, "can't add a null observer!");
            return;
        }
        if (conversationTypes == null) {
            conversationTypes =
                    NCChatUIConfig.channelListConfig().getDataProcessor().supportedTypes();
        }
        synchronized (mForeverMultiConversationUnreadInfos) {
            final MultiConversationUnreadMsgInfo msgInfo = new MultiConversationUnreadMsgInfo();
            msgInfo.conversationTypes = conversationTypes;
            msgInfo.observer = observer;
            mForeverMultiConversationUnreadInfos.add(msgInfo);
            getUnreadCountByTypes(
                    conversationTypes,
                    new OperationHandler<Integer>() {
                        @Override
                        public void onResult(Integer count, NCError error) {
                            if (error != null) return;
                            msgInfo.count = count;
                            msgInfo.observer.onCountChanged(count);
                        }
                    });
        }
    }

    public void removeObserver(final IUnReadMessageObserver observer) {
        if (observer == null) {
            RLog.w(TAG, "removeOnReceiveUnreadCountChangedListener Illegal argument");
            return;
        }
        synchronized (mMultiConversationUnreadInfos) {
            WeakReference<MultiConversationUnreadMsgInfo> result = null;
            for (final WeakReference<MultiConversationUnreadMsgInfo> weakMsgInfo :
                    mMultiConversationUnreadInfos) {
                if (weakMsgInfo == null) {
                    continue;
                }
                MultiConversationUnreadMsgInfo msgInfo = weakMsgInfo.get();
                if (msgInfo == null) {
                    continue;
                }
                if (msgInfo.observer == observer) {
                    result = weakMsgInfo;
                    break;
                }
            }
            if (result != null) {
                mMultiConversationUnreadInfos.remove(result);
            }
        }
    }

    public void removeForeverObserver(final IUnReadMessageObserver observer) {
        if (observer == null) {
            RLog.w(TAG, "removeOnReceiveUnreadCountChangedListener Illegal argument");
            return;
        }
        synchronized (mForeverMultiConversationUnreadInfos) {
            MultiConversationUnreadMsgInfo result = null;
            for (MultiConversationUnreadMsgInfo msgInfo : mForeverMultiConversationUnreadInfos) {
                if (msgInfo == null) {
                    continue;
                }
                if (msgInfo.observer == observer) {
                    result = msgInfo;
                    break;
                }
            }
            if (result != null) {
                mForeverMultiConversationUnreadInfos.remove(result);
            }
        }
    }

    public void clearObserver() {
        synchronized (mMultiConversationUnreadInfos) {
            mMultiConversationUnreadInfos.clear();
        }
    }

    public void clearForeverObserver() {
        synchronized (mForeverMultiConversationUnreadInfos) {
            mForeverMultiConversationUnreadInfos.clear();
        }
    }

    public interface IUnReadMessageObserver {
        void onCountChanged(int count);
    }

    private static class SingletonHolder {
        static UnReadMessageManager sInstance = new UnReadMessageManager();
    }

    private class MultiConversationUnreadMsgInfo {
        ChannelType[] conversationTypes;
        int count;
        IUnReadMessageObserver observer;
    }
}
