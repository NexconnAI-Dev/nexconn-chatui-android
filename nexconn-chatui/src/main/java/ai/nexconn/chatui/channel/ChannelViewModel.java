package ai.nexconn.chatui.channel;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.DirectChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.ChannelNoDisturbLevel;
import ai.nexconn.chat.channel.model.ChannelUserTypingStatusInfo;
import ai.nexconn.chat.channel.model.TypingStatusChangedEvent;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.ChannelHandler;
import ai.nexconn.chat.handler.ConnectionStatusHandler;
import ai.nexconn.chat.handler.DownloadMediaMessageHandler;
import ai.nexconn.chat.handler.MessageHandler;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.message.HDVoiceMessage;
import ai.nexconn.chat.message.MediaMessageContent;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.UnknownMessage;
import ai.nexconn.chat.message.model.MentionedInfo;
import ai.nexconn.chat.message.model.MessageDeletedEvent;
import ai.nexconn.chat.message.model.MessageDirection;
import ai.nexconn.chat.message.model.MessageReceivedEvent;
import ai.nexconn.chat.message.model.MessageType;
import ai.nexconn.chat.message.model.MessagesUpdatedEvent;
import ai.nexconn.chat.message.model.ReadReceiptInfo;
import ai.nexconn.chat.message.model.SentStatus;
import ai.nexconn.chat.model.ConnectionStatus;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chat.user.model.UserOnlineStatus;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.channel.event.action.DeleteEvent;
import ai.nexconn.chatui.channel.event.action.DownloadEvent;
import ai.nexconn.chatui.channel.event.action.InsertEvent;
import ai.nexconn.chatui.channel.event.action.MessageEventListener;
import ai.nexconn.chatui.channel.event.action.RefreshEvent;
import ai.nexconn.chatui.channel.event.action.ReplaceEvent;
import ai.nexconn.chatui.channel.event.action.SendEvent;
import ai.nexconn.chatui.channel.event.action.SendMediaEvent;
import ai.nexconn.chatui.channel.event.page.InputBarEvent;
import ai.nexconn.chatui.channel.event.page.MessageEvent;
import ai.nexconn.chatui.channel.event.page.PageEvent;
import ai.nexconn.chatui.channel.event.page.ReadReceiptStateClickEvent;
import ai.nexconn.chatui.channel.event.page.ScrollEvent;
import ai.nexconn.chatui.channel.event.page.ScrollToEndEvent;
import ai.nexconn.chatui.channel.event.page.ToastEvent;
import ai.nexconn.chatui.channel.feature.editmessage.EditMessageManager;
import ai.nexconn.chatui.channel.feature.forward.ForwardManager;
import ai.nexconn.chatui.channel.feature.resend.ResendManager;
import ai.nexconn.chatui.channel.messagelist.processor.ChannelProcessorFactory;
import ai.nexconn.chatui.channel.messagelist.processor.IChannelProcessor;
import ai.nexconn.chatui.channel.messagelist.provider.MessageClickType;
import ai.nexconn.chatui.config.ChannelConfig;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.handler.AppSettingsHandler;
import ai.nexconn.chatui.handler.ChannelEventListener;
import ai.nexconn.chatui.handler.EditMessageHandler;
import ai.nexconn.chatui.handler.ReadReceiptV5Handler;
import ai.nexconn.chatui.handler.SpeechToTextHandler;
import ai.nexconn.chatui.handler.StreamMessageHandler;
import ai.nexconn.chatui.manager.AudioPlayManager;
import ai.nexconn.chatui.manager.IAudioPlayListener;
import ai.nexconn.chatui.manager.OnLineStatusListener;
import ai.nexconn.chatui.manager.OnLineStatusManager;
import ai.nexconn.chatui.manager.UnReadMessageManager;
import ai.nexconn.chatui.manager.hqvoicemessage.AutoDownloadEntry;
import ai.nexconn.chatui.manager.hqvoicemessage.HQVoiceMsgDownloadManager;
import ai.nexconn.chatui.message.HistoryDividerMessage;
import ai.nexconn.chatui.model.State;
import ai.nexconn.chatui.model.TypingInfo;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.notification.ChatUINotificationManager;
import ai.nexconn.chatui.picture.tools.ToastUtils;
import ai.nexconn.chatui.userinfo.NCUserInfoManager;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;
import ai.nexconn.chatui.utils.file.FileUtils;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.route.RouteUtils;
import ai.nexconn.chatui.utils.system.ExecutorHelper;
import ai.nexconn.chatui.widget.TextAnimationHelper;
import ai.nexconn.chatui.widget.cache.MessageList;
import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChannelViewModel extends AndroidViewModel
        implements MessageEventListener, NCUserInfoManager.UserDataObserver {
    public static final int DEFAULT_COUNT =
            NCChatUIConfig.channelConfig().getConversationHistoryMessageCount();
    public static final int DEFAULT_REMOTE_COUNT =
            NCChatUIConfig.channelConfig().getConversationRemoteMessageCount();
    public static final int SHOW_UNREAD_MESSAGE_COUNT =
            NCChatUIConfig.channelConfig().getConversationShowUnreadMessageCount();
    private static final String TAG = "ChannelViewModel";
    private static final String CONV_HANDLER_KEY = "MessageViewModel_ConvHandler";
    private static final int ERROR_NETWORK_UNAVAILABLE = 30001;
    private static final int ERROR_CONNECTION_UNAVAILABLE = 30002;
    private static final int ERROR_TIMEOUT = 30003;
    private static final int ERROR_MEDIA_EXCEPTION = 34018;
    private static final int ERROR_GIF_SIZE_LIMIT_EXCEED = 34003;
    private static final int ERROR_FILE_SIZE_EXCEED_LIMIT = 26106;
    public static String[] writePermission =
            new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    // --- Conversation meta (typing / online / notify) ---
    private volatile boolean mConvCleared = false;
    private MediatorLiveData<TypingInfo> mTypingStatusInfo = new MediatorLiveData<>();
    private MediatorLiveData<UserOnlineStatus> mOnlineStatus = new MediatorLiveData<>();
    private MediatorLiveData<Boolean> mIsNotify = new MediatorLiveData<>();
    private String mConvTargetId = "";
    private ai.nexconn.chatui.manager.hqvoicemessage.HQVoiceDownloadListener
            mHQVoiceDownloadListener;
    private final OnLineStatusListener mOnLineStatusListener =
            statuses -> {
                for (java.util.Map.Entry<String, UserOnlineStatus> entry : statuses.entrySet()) {
                    if (TextUtils.equals(entry.getKey(), mConvTargetId)) {
                        mOnlineStatus.postValue(entry.getValue());
                        return;
                    }
                }
            };
    private final ChannelHandler mConvChannelHandler =
            new ChannelHandler() {
                @Override
                public void onTypingStatusChanged(TypingStatusChangedEvent event) {
                    TypingInfo info = new TypingInfo();
                    ChannelType channelType = event.getChannelIdentifier().getChannelType();
                    info.conversationType = channelType;
                    info.targetId = event.getChannelIdentifier().getChannelId();
                    List<ChannelUserTypingStatusInfo> statusList = event.getUserTypingStatus();
                    if (statusList != null && !statusList.isEmpty()) {
                        List<TypingInfo.TypingUserInfo> typingUserInfoList = new ArrayList<>();
                        for (ChannelUserTypingStatusInfo status : statusList) {
                            TypingInfo.TypingUserInfo typing = new TypingInfo.TypingUserInfo();
                            String objectName = status.getTypingMessageType();
                            if (MessageType.TEXT.equals(objectName)) {
                                typing.type = TypingInfo.TypingUserInfo.Type.text;
                            } else if (MessageType.HD_VOICE.equals(objectName)) {
                                typing.type = TypingInfo.TypingUserInfo.Type.voice;
                            }
                            typing.sendTime = status.getSentTime();
                            typing.userId = status.getUserId();
                            typingUserInfoList.add(typing);
                        }
                        info.typingList = typingUserInfoList;
                    }
                    mTypingStatusInfo.postValue(info);
                }
            };
    // --- End conversation meta ---
    private List<UiMessage> mUiMessages = new MessageList<>(6000);
    private List<UiMessage> mSelectedUiMessage = new ArrayList<>();
    private MediatorLiveData<PageEvent> mPageEventLiveData = new MediatorLiveData<>();
    private MediatorLiveData<List<UiMessage>> mUiMessageLiveData = new MediatorLiveData<>();
    private ChannelIdentifier mChannelIdentifier;
    private final StreamMessageHandler mStreamMessageHandler;
    private final SpeechToTextHandler mSpeechToTextHandler;
    private final EditMessageHandler mEditMessageHandler;
    private final ReadReceiptV5Handler mReadReceiptV5Handler;
    // Tracks messages currently handling manual warning-click resend to avoid duplicate re-entry.
    private final Set<Integer> mManualResendInFlight = ConcurrentHashMap.newKeySet();
    // Whether all remote data has been fully fetched
    private boolean mRemoteMessageLoadFinish = false;
    private boolean mInitUnreadMessageFinish;
    private List<UiMessage> mNewUnReadMessages = new ArrayList<>();
    private List<Message> mNewUnReadMentionMessages = new CopyOnWriteArrayList<>();
    private MediatorLiveData<Integer> mHistoryMessageUnreadLiveData = new MediatorLiveData<>();
    private MediatorLiveData<Integer> mNewMessageUnreadLiveData = new MediatorLiveData<>();
    private MediatorLiveData<Integer> mNewMentionMessageUnreadLiveData = new MediatorLiveData<>();
    private boolean mInitMentionedMessageFinish;
    private MediatorLiveData<Boolean> mIsEditStatus = new MediatorLiveData<>();

    /**
     * Client IDs of messages deleted during this session, preventing loadMore from re-fetching them
     * from remote.
     */
    private final java.util.Set<Integer> mDeletedMessageIds = new java.util.HashSet<>();

    // Whether the app is in the foreground
    private boolean mIsForegroundActivity;
    // Whether scrolled to the very bottom of the page
    private boolean mScrollToBottom;
    private IChannelProcessor mProcessor;
    private Bundle mBundle;
    private Message mFirstUnreadMessage;
    private boolean mKeepHistoryBarOnNextUnreadClear;
    private Handler mainHandler;
    private static final long REFRESH_THROTTLE_MS = 300;
    private long mLastRefreshTime = 0;
    private boolean mRefreshPending = false;
    private final Handler mThrottleHandler = new Handler(Looper.getMainLooper());
    private final Runnable mThrottledRefreshRunnable =
            () -> {
                mRefreshPending = false;
                mLastRefreshTime = SystemClock.elapsedRealtime();
                mUiMessageLiveData.setValue(mUiMessages);
            };
    private static final String MSG_HANDLER_KEY = "MessageViewModel_MsgHandler";
    private static final String CONN_HANDLER_KEY = "MessageViewModel_ConnHandler";
    private final MessageHandler mMessageHandler =
            new MessageHandler() {
                @Override
                public void onMessageReceived(MessageReceivedEvent event) {
                    Message message = event.getMessage();
                    int left = event.getLeft();
                    boolean hasPackage = event.getHasPackage();
                    boolean offline = event.getOffline();
                    if (!isSameConversationMessage(message)) {
                        return;
                    }
                    ExecutorHelper.getInstance()
                            .mainThread()
                            .execute(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mProcessor == null) {
                                                return;
                                            }
                                            if (!message.isCounted() && !message.isPersisted()) {
                                                if (mProcessor.onReceivedCmd(
                                                        ChannelViewModel.this, message)) {
                                                    return;
                                                }
                                            }
                                            if (!shouldContainUnknownMessage(message)) {
                                                return;
                                            }
                                            UiMessage uiMessage = mapUIMessage(message);
                                            if (left == 0 && !hasPackage) {
                                                if (message.getContent()
                                                        instanceof HDVoiceMessage) {
                                                    if (NCChatUIConfig.channelConfig()
                                                            .NC_enable_automatic_download_voice_msg) {
                                                        HQVoiceMsgDownloadManager.getInstance()
                                                                .enqueue(
                                                                        new AutoDownloadEntry(
                                                                                message,
                                                                                AutoDownloadEntry
                                                                                        .DownloadPriority
                                                                                        .HIGH));
                                                    } else {
                                                        RLog.e(
                                                                TAG,
                                                                "NC_enable_automatic_download_voice_msg disabled");
                                                    }
                                                    uiMessage.setState(State.PROGRESS);
                                                }
                                            }
                                            if (message.getClientId() > 0
                                                    && isForegroundActivity()) {
                                                message.getReceivedStatusInfo().setRead();
                                                message.setReceivedStatusInfo(
                                                        message.getReceivedStatusInfo(), null);
                                            }
                                            mProcessor.onReceived(
                                                    ChannelViewModel.this,
                                                    uiMessage,
                                                    left,
                                                    hasPackage,
                                                    offline);
                                        }
                                    });
                }

                @Override
                public void onMessageDeleted(MessageDeletedEvent event) {
                    if (event.getMessages() == null) return;
                    for (Message msg : event.getMessages()) {
                        if (!isSameConversationMessage(msg)) continue;
                        RLog.d(TAG, "onMessageDeleted");
                        mDeletedMessageIds.add(msg.getClientId());
                        UiMessage uiMessage = findUIMessage(msg.getClientId());
                        removeRecallMentionMsg(msg);
                        UiMessage newUnreadMessage = findNewUnreadMessage(msg.getClientId());
                        if (newUnreadMessage != null) {
                            mNewUnReadMessages.remove(newUnreadMessage);
                            processNewMessageUnread(true);
                        }
                        if (uiMessage != null) {
                            if (uiMessage.isSelected()) {
                                mSelectedUiMessage.remove(uiMessage);
                                if (mSelectedUiMessage.size() <= 0) {
                                    mPageEventLiveData.postValue(
                                            new InputBarEvent(
                                                    InputBarEvent.Type.InactiveMoreMenu, null));
                                }
                                uiMessage.setSelected(false);
                            }
                            MessageContent content = uiMessage.getMessage().getContent();
                            if (content instanceof HDVoiceMessage) {
                                Uri playingUri = AudioPlayManager.getInstance().getPlayingUri();
                                String localPath = ((HDVoiceMessage) content).getLocalPath();
                                if (playingUri != null
                                        && localPath != null
                                        && playingUri.toString().equals(localPath)) {
                                    AudioPlayManager.getInstance().stopPlay();
                                }
                                stopDestructTime(uiMessage);
                            } else if (content instanceof MediaMessageContent) {
                                uiMessage.getMessage().cancelDownloadingMedia(null);
                            }
                            removeUIMessage(uiMessage.getClientId());
                        }
                        List<UiMessage> uiMessages =
                                mEditMessageHandler.processMessageReferMsgStatus(
                                        msg,
                                        ai.nexconn.chat.message.model.ReferenceMessageStatus
                                                .RECALLED,
                                        getUiMessages());
                        mUiMessageLiveData.postValue(uiMessages);
                    }
                }

                @Override
                public void onMessagesUpdated(MessagesUpdatedEvent event) {
                    if (event.getMessages() == null) return;
                    ExecutorHelper.getInstance()
                            .mainThread()
                            .execute(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            for (Message msg : event.getMessages()) {
                                                if (!isSameConversationMessage(msg)) continue;

                                                UiMessage uiMessage =
                                                        findUIMessage(msg.getClientId());
                                                if (uiMessage != null) {
                                                    RLog.d(
                                                            TAG,
                                                            "onMessagesUpdated: message "
                                                                    + msg.getClientId());

                                                    // Update message content
                                                    uiMessage.setMessage(msg);

                                                    // If media message downloaded, update state
                                                    if (msg.getContent()
                                                            instanceof MediaMessageContent) {
                                                        MediaMessageContent content =
                                                                (MediaMessageContent)
                                                                        msg.getContent();
                                                        if (content.getLocalPath() != null
                                                                && !TextUtils.isEmpty(
                                                                        content.getLocalPath())) {
                                                            // Downloaded successfully
                                                            if (uiMessage.getState()
                                                                    == State.PROGRESS) {
                                                                uiMessage.setState(State.NORMAL);
                                                                RLog.d(
                                                                        TAG,
                                                                        "onMessagesUpdated: media"
                                                                                + " downloaded, update"
                                                                                + " state to NORMAL");
                                                            }
                                                        }
                                                    }

                                                    // Refresh UI
                                                    refreshSingleMessage(uiMessage);
                                                }
                                            }
                                        }
                                    });
                }
            };

    private final ConnectionStatusHandler mConnectionStatusHandler =
            event -> {
                if (mProcessor != null) {
                    mProcessor.onConnectStatusChange(ChannelViewModel.this, event.getStatus());
                }
                if (ConnectionStatus.CONNECTED == event.getStatus()
                        && !TextUtils.isEmpty(mConvTargetId)) {
                    mOnlineStatus.postValue(null);
                    OnLineStatusManager.getInstance().fetchUsersOnlineStatus(mConvTargetId, false);
                }
            };
    private ChannelEventListener mChannelEventListener =
            new ChannelEventListener() {
                @Override
                public void onClearedUnreadStatus(
                        ai.nexconn.chat.channel.model.ChannelIdentifier channelIdentifier) {
                    if (channelIdentifier == null
                            || getCurChannelType() == null
                            || TextUtils.isEmpty(getCurTargetId())) {
                        return;
                    }
                    if (channelIdentifier.getChannelType().equals(getCurChannelType())
                            && channelIdentifier.getChannelId().equals(getCurTargetId())) {
                        // This event is fired by clearUnreadCount (multi-device read sync + local
                        // unread clear).
                        // Only clear unread UI indicators — do NOT wipe the message list, as the
                        // messages themselves are still valid and visible to the user.
                        mNewUnReadMentionMessages.clear();
                        updateNewMentionMessageUnreadBar();
                        if (mKeepHistoryBarOnNextUnreadClear && mFirstUnreadMessage != null) {
                            mKeepHistoryBarOnNextUnreadClear = false;
                            return;
                        }
                        mKeepHistoryBarOnNextUnreadClear = false;
                        mFirstUnreadMessage = null;
                        hideHistoryBar();
                    }
                }
            };

    public ChannelViewModel(@NonNull Application application) {
        super(application);
        mainHandler = new Handler(Looper.getMainLooper());
        mStreamMessageHandler = new StreamMessageHandler();
        mStreamMessageHandler.addDataChangeListener(
                StreamMessageHandler.KEY_FETCH_STREAM_MESSAGE, this::refreshModifyMessage);
        mSpeechToTextHandler = new SpeechToTextHandler();
        mSpeechToTextHandler.addDataChangeListener(
                SpeechToTextHandler.KEY_SPEECH_TO_TEXT_LISTENER, this::refreshModifyMessage);
        mSpeechToTextHandler.addDataChangeListener(
                SpeechToTextHandler.KEY_REQUEST_SPEECH_TO_TEXT,
                new OnDataChangeListener<UiMessage>() {
                    @Override
                    public void onDataChange(UiMessage uiMessage) {
                        ChannelViewModel.this.refreshModifyMessage(uiMessage);
                    }

                    @Override
                    public void onDataError(NCError error) {
                        String message;
                        if (error != null && error.getCode() == 35059) {
                            message =
                                    getApplication()
                                            .getString(
                                                    R.string.nc_speech_to_text_unsupported_format);
                        } else {
                            message =
                                    getApplication()
                                            .getString(R.string.nc_speech_to_text_network_error);
                        }
                        ai.nexconn.chatui.utils.common.ToastUtils.show(
                                NCChatUI.getContext(), message, Toast.LENGTH_SHORT);
                    }
                });
        mSpeechToTextHandler.addDataChangeListener(
                SpeechToTextHandler.KEY_SET_SPEECH_TO_TEXT_VISIBLE,
                new OnDataChangeListener<UiMessage>() {
                    @Override
                    public void onDataChange(UiMessage uiMessage) {
                        ChannelViewModel.this.refreshModifyMessage(uiMessage);
                    }

                    @Override
                    public void onDataError(NCError error) {
                        String message;
                        if (error != null && error.getCode() == 35059) {
                            message =
                                    getApplication()
                                            .getString(
                                                    R.string.nc_speech_to_text_unsupported_format);
                        } else {
                            message =
                                    getApplication()
                                            .getString(R.string.nc_speech_to_text_network_error);
                        }
                        ai.nexconn.chatui.utils.common.ToastUtils.show(
                                NCChatUI.getContext(), message, Toast.LENGTH_SHORT);
                    }
                });
        mEditMessageHandler = new EditMessageHandler();
        mEditMessageHandler.addDataChangeListener(
                EditMessageHandler.KEY_ON_MESSAGE_MODIFIED,
                new OnDataChangeListener<List<Message>>() {
                    @Override
                    public void onDataChange(List<Message> editMessageList) {
                        List<UiMessage> uiMessages =
                                mEditMessageHandler.processMessageEditStatusAndReferMsgStatus(
                                        editMessageList, getUiMessages());
                        mUiMessageLiveData.postValue(uiMessages);
                        // Update reference messages
                        mEditMessageHandler.updateReferenceView(editMessageList, getUiMessages());
                    }
                });
        mReadReceiptV5Handler = new ReadReceiptV5Handler();
        mReadReceiptV5Handler.addDataChangeListener(
                ReadReceiptV5Handler.KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5,
                new OnDataChangeEnhancedListener<HashMap<String, ReadReceiptInfo>>() {
                    @Override
                    public void onDataChange(HashMap<String, ReadReceiptInfo> map) {
                        if (!map.isEmpty()) {
                            for (UiMessage uiMessage : mUiMessages) {
                                ReadReceiptInfo info = map.get(uiMessage.getMessageId());
                                if (info != null) {
                                    uiMessage.setReadReceiptInfo(info);
                                }
                            }
                        }
                        refreshAllMessage();
                    }
                });
        mReadReceiptV5Handler.addDataChangeListener(
                ReadReceiptV5Handler.KEY_MESSAGE_READ_RECEIPT_V5_LISTENER,
                new OnDataChangeEnhancedListener<HashMap<String, ReadReceiptInfo>>() {
                    @Override
                    public void onDataChange(HashMap<String, ReadReceiptInfo> map) {
                        boolean needRefresh = false;
                        for (UiMessage uiMessage : mUiMessages) {
                            ReadReceiptInfo info = map.get(uiMessage.getMessageId());
                            if (info != null) {
                                needRefresh = true;
                                uiMessage.setReadReceiptInfo(info);
                            }
                        }
                        if (needRefresh) {
                            refreshAllMessage(false);
                        }
                    }
                });
        NCEngine.addMessageHandler("MessageViewModel_msg_" + hashCode(), mMessageHandler);
        NCEngine.addConnectionStatusHandler(
                "MessageViewModel_conn_" + hashCode(), mConnectionStatusHandler);
        NCEngine.addChannelHandler(CONV_HANDLER_KEY + hashCode(), mConvChannelHandler);
        NCChatUI.addMessageEventListener(this);
        NCChatUI.addChannelEventListener(mChannelEventListener);
        NCUserInfoManager.getInstance().addUserDataObserver(this);
        OnLineStatusManager.getInstance().addOnLineStatusListener(mOnLineStatusListener);
        initTranslationListener();

        // Add download listener for HQVoiceMsgDownloadManager
        // Delay initialization to avoid issues during construction
        mainHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        mHQVoiceDownloadListener =
                                new ai.nexconn.chatui.manager.hqvoicemessage
                                        .HQVoiceDownloadListener() {
                                    @Override
                                    public void onDownloadComplete(Message message) {
                                        try {
                                            if (!isSameConversationMessage(message)) return;

                                            UiMessage uiMessage =
                                                    findUIMessage(message.getClientId());
                                            if (uiMessage != null
                                                    && uiMessage.getMessage() != null) {
                                                RLog.d(
                                                        TAG,
                                                        "HQVoice download complete, refresh UI for messageId="
                                                                + message.getClientId());
                                                // Only update the content, keep other properties
                                                // like direction
                                                // unchanged
                                                uiMessage
                                                        .getMessage()
                                                        .setContent(message.getContent());
                                                uiMessage.setState(State.NORMAL);
                                                refreshSingleMessage(uiMessage);
                                            }
                                        } catch (Exception e) {
                                            RLog.e(TAG, "Error in HQVoice download listener", e);
                                        }
                                    }
                                };
                        HQVoiceMsgDownloadManager.getInstance()
                                .addDownloadListener(mHQVoiceDownloadListener);
                    }
                });
    }

    private void refreshModifyMessage(UiMessage uiMessage) {
        if (uiMessage != null && uiMessage.getMessage() != null) {
            UiMessage findUiMessage = findUIMessage(uiMessage.getMessage().getMessageId());
            if (findUiMessage != null) {
                findUiMessage.setMessage(uiMessage.getMessage());
                findUiMessage.setBusinessState(uiMessage.getBusinessState());
                refreshSingleMessage(findUiMessage);
            }
        }
    }

    private void stopDestructTime(UiMessage uiMessage) {}

    public void bindConversation(ChannelIdentifier conversationIdentifier, Bundle bundle) {
        mChannelIdentifier = conversationIdentifier;
        mProcessor = ChannelProcessorFactory.getInstance().getProcessor(getCurChannelType());
        mBundle = bundle;
        mProcessor.init(this, bundle);
        mIsEditStatus.setValue(false);
        mReadReceiptV5Handler.bindConversation(mChannelIdentifier);
    }

    /** Initial local message loading / pull-down history loading. */
    public void onGetHistoryMessage(List<Message> messages, boolean isHasMoreMsg) {
        onGetHistoryMessage(messages);
        // No more history messages
        if (!isHasMoreMsg) {
            executePageEvent(new MessageEvent(false));
        }
    }

    /** Initial local message loading / pull-down history loading. */
    public void onGetHistoryMessage(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        mProcessor.onLoadMessage(this, messages);
        for (Message message : messages) {
            if (!message.isCounted() && !message.isPersisted()) {
                if (mProcessor.onReceivedCmd(ChannelViewModel.this, message)) {
                    continue;
                }
            }
            boolean contains = false;
            for (UiMessage uiMessage : mUiMessages) {
                if (uiMessage.getMessage().getClientId() == message.getClientId()) {
                    contains = true;
                    break;
                }
            }
            if (!contains && shouldContainUnknownMessage(message)) {
                // Determine the correct insertion position by sentTime instead of always inserting
                // at index 0
                int position = findPositionBySendTime(message.getSentTime());
                mUiMessages.add(position, mapUIMessage(message));
            }
        }
        processHistoryDividerMessage();
        refreshAllMessage();
    }

    public UiMessage mapUIMessage(Message message) {
        UiMessage uiMessage = new UiMessage(message);
        if (mIsEditStatus.getValue() != null) {
            uiMessage.setEdit(mIsEditStatus.getValue());
        }
        return uiMessage;
    }

    public void processHistoryDividerMessage() {
        // Only proceed if there are unread history messages
        if (getFirstUnreadMessage() == null) {
            return;
        }
        int position = findPositionByMessageId(getFirstUnreadMessage().getClientId());
        if (position >= 0) {
            if (NCChatUIConfig.channelConfig().isShowHistoryDividerMessage()) {
                if (hasHistoryDividerMessage()) {
                    return;
                }
                Message hisMessage =
                        Message.createLocalMessage(
                                mChannelIdentifier,
                                HistoryDividerMessage.obtain(
                                        getApplication()
                                                .getString(
                                                        R.string.nc_new_message_divider_content)));
                hisMessage.setSenderUserId(NCEngine.getCurrentUserId());
                UiMessage uiMessage = new UiMessage(hisMessage);
                UiMessage firstUiMessage = mUiMessages.get(position);
                // Set timestamp 1ms before the first unread message
                uiMessage.setSentTime(firstUiMessage.getMessage().getSentTime() - 1);
                mUiMessages.add(position, uiMessage);
            }
        }
    }

    private boolean hasHistoryDividerMessage() {
        for (UiMessage uiMessage : mUiMessages) {
            if (uiMessage.getContent() instanceof HistoryDividerMessage) {
                return true;
            }
        }
        return false;
    }

    public void refreshAllMessage() {
        refreshAllMessage(true);
    }

    public Message getFirstUnreadMessage() {
        return mFirstUnreadMessage;
    }

    public void setFirstUnreadMessage(Message firstUnreadMessage) {
        mFirstUnreadMessage = firstUnreadMessage;
    }

    public int findPositionByMessageId(int clientId) {
        int position = -1;
        for (int i = 0; i < mUiMessages.size(); i++) {
            UiMessage item = mUiMessages.get(i);
            if (item.getMessage().getClientId() == clientId) {
                position = i;
                break;
            }
        }
        return position;
    }

    public String getCurTargetId() {
        if (mChannelIdentifier == null) {
            return null;
        }
        return mChannelIdentifier.getChannelId();
    }

    public ChannelType getCurChannelType() {
        if (mChannelIdentifier == null) {
            return null;
        }
        return mChannelIdentifier.getChannelType();
    }

    private static boolean isMessagePersisted(Message message) {
        return message != null && message.isPersisted();
    }

    private static boolean isMessageCounted(Message message) {
        return message != null && message.isCounted();
    }

    public String getCurChannelId() {
        return mChannelIdentifier == null ? "" : mChannelIdentifier.getChannelId();
    }

    public ChannelIdentifier getChannelIdentifier() {
        return mChannelIdentifier;
    }

    public ChannelIdentifier getConversationIdentifier() {
        return mChannelIdentifier;
    }

    public BaseChannel createChannel() {
        if (mChannelIdentifier == null) return new DirectChannel("");
        return NCChatUI.createChannel(mChannelIdentifier);
    }

    public void refreshAllMessage(boolean force) {
        if (force) {
            for (UiMessage item : mUiMessages) {
                item.change();
            }
            mThrottleHandler.removeCallbacks(mThrottledRefreshRunnable);
            mRefreshPending = false;
            mLastRefreshTime = SystemClock.elapsedRealtime();
            if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                mUiMessageLiveData.setValue(mUiMessages);
            } else {
                mUiMessageLiveData.postValue(mUiMessages);
            }
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - mLastRefreshTime >= REFRESH_THROTTLE_MS) {
            mLastRefreshTime = now;
            if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                mUiMessageLiveData.setValue(mUiMessages);
            } else {
                mUiMessageLiveData.postValue(mUiMessages);
            }
        } else if (!mRefreshPending) {
            mRefreshPending = true;
            long delay = REFRESH_THROTTLE_MS - (now - mLastRefreshTime);
            mThrottleHandler.postDelayed(mThrottledRefreshRunnable, delay);
        }
    }

    /** Loads more messages on pull-up. */
    public void onLoadMoreMessage(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        mProcessor.onLoadMessage(this, messages);
        ArrayList<UiMessage> list = new ArrayList<>();
        for (Message message : messages) {
            boolean contains = false;
            for (UiMessage uiMessage : mUiMessages) {
                if (uiMessage.getMessage().getClientId() == message.getClientId()) {
                    contains = true;
                    break;
                }
            }
            if (!contains
                    && !mDeletedMessageIds.contains(message.getClientId())
                    && shouldContainUnknownMessage(message)) {
                list.add(0, mapUIMessage(message));
            }
        }
        mUiMessages.addAll(list);
        processHistoryDividerMessage();
        refreshAllMessage();
    }

    /**
     * Reloads messages: triggered by clicking the history bar in normal state or the new-message
     * bar in history state.
     */
    public void onReloadMessage(List<Message> messages) {
        mProcessor.onLoadMessage(this, messages);
        mUiMessages.clear();
        for (Message message : messages) {
            boolean contains = false;
            for (UiMessage uiMessage : mUiMessages) {
                if (uiMessage.getMessage().getClientId() == message.getClientId()) {
                    contains = true;
                    break;
                }
            }
            if (!contains && shouldContainUnknownMessage(message)) {
                mUiMessages.add(0, mapUIMessage(message));
            }
        }
        processHistoryDividerMessage();
        refreshAllMessage();
    }

    /**
     * Queries read-receipt V5 info for the given messages, binds results to UiMessages, then
     * refreshes.
     *
     * @param messages the messages to query
     */
    public void getMessageReadReceiptInfoV5(List<Message> messages) {
        mReadReceiptV5Handler.getMessageReadReceiptInfo(mChannelIdentifier, messages);
    }

    public MediatorLiveData<PageEvent> getPageEventLiveData() {
        return mPageEventLiveData;
    }

    public void onWarnClick(final UiMessage uiMessage) {
        final Message msg = uiMessage.getMessage();
        if (msg == null || msg.getContent() == null) {
            return;
        }
        if (mChannelIdentifier == null) {
            RLog.w(TAG, "onWarnClick: channelIdentifier is null");
            return;
        }
        final int clientId = msg.getClientId();
        // Check if message is already being resent to prevent duplicates
        if (ResendManager.getInstance().needResend(clientId)) {
            RLog.d(TAG, "onWarnClick: message is already being resent, clientId=" + clientId);
            return;
        }
        if (!mManualResendInFlight.add(clientId)) {
            RLog.d(TAG, "onWarnClick: manual resend already in progress, clientId=" + clientId);
            return;
        }
        // Delete failed local message first, then resend to avoid accumulating duplicate failed
        // rows.
        NCChatUI.deleteLocalMessages(
                mChannelIdentifier,
                Collections.singletonList(msg),
                error -> {
                    mManualResendInFlight.remove(clientId);
                    if (error != null) {
                        RLog.w(
                                TAG,
                                "onWarnClick: deleteLocalMessages failed, clientId="
                                        + clientId
                                        + ", errorCode="
                                        + error.getCode());
                        return;
                    }
                    reSendMessage(msg);
                });
    }

    public void onItemClick(UiMessage uiMessage) {
        ChannelType type = uiMessage.getChannelType();
        ChannelProcessorFactory.getInstance().getProcessor(type).onMessageItemClick(uiMessage);
    }

    public void onUserPortraitClick(
            Context context, ChannelType conversationType, UserInfo userInfo, String targetId) {
        ChannelProcessorFactory.getInstance()
                .getProcessor(conversationType)
                .onUserPortraitClick(context, conversationType, userInfo, targetId);
    }

    public boolean onUserPortraitLongClick(
            Context context, ChannelType conversationType, UserInfo userInfo, String targetId) {
        return ChannelProcessorFactory.getInstance()
                .getProcessor(conversationType)
                .onUserPortraitLongClick(context, conversationType, userInfo, targetId);
    }

    public void reSendMessage(Message message) {
        if (message == null || message.getContent() == null) return;
        if (message.getContent() instanceof MediaMessageContent) {
            NCChatUI.sendMediaMessage(
                    mChannelIdentifier,
                    new ai.nexconn.chat.params.SendMediaMessageParams(
                            (MediaMessageContent) message.getContent()),
                    null);
        } else {
            NCChatUI.sendMessage(
                    mChannelIdentifier,
                    new ai.nexconn.chat.params.SendMessageParams(message.getContent()),
                    null);
        }
    }

    public void onAudioClick(UiMessage uiMessage) {
        // Handle pause logic
        MessageContent content = uiMessage.getMessage().getContent();
        if (content instanceof HDVoiceMessage) {
            if (AudioPlayManager.getInstance().isPlaying()) {
                Uri playingUri = AudioPlayManager.getInstance().getPlayingUri();
                AudioPlayManager.getInstance().stopPlay();
                String localPath = ((HDVoiceMessage) content).getLocalPath();
                if (playingUri != null
                        && localPath != null
                        && playingUri.toString().equals(localPath)) return;
            }
            if (AudioPlayManager.getInstance().isInVOIPMode(getApplication())) {
                mPageEventLiveData.setValue(
                        new ToastEvent(getApplication().getString(R.string.nc_voip_occupying)));
                return;
            }
            playOrDownloadHQVoiceMsg(
                    (HDVoiceMessage) uiMessage.getMessage().getContent(), uiMessage);
        }
    }

    private void playOrDownloadHQVoiceMsg(HDVoiceMessage content, UiMessage uiMessage) {
        boolean ifDownloadHQVoiceMsg =
                (content.getLocalPath() == null
                        || TextUtils.isEmpty(content.getLocalPath())
                        || !FileUtils.isFileExistsWithUri(
                                getApplication(), Uri.parse(content.getLocalPath())));
        if (ifDownloadHQVoiceMsg) {
            downloadHQVoiceMsg(uiMessage);
        } else {
            playVoiceMessage(uiMessage);
        }
    }

    private void downloadHQVoiceMsg(final UiMessage uiMessage) {
        uiMessage
                .getMessage()
                .downloadMedia(
                        new DownloadMediaMessageHandler() {
                            @Override
                            public void onSuccess(Message message) {
                                if (message != null && message.getContent() != null) {
                                    uiMessage.getMessage().setContent(message.getContent());
                                }
                                uiMessage.setState(State.NORMAL);
                                refreshSingleMessage(uiMessage);
                                playVoiceMessage(uiMessage);
                            }

                            @Override
                            public void onProgress(Message message, int progress) {
                                uiMessage.setState(State.PROGRESS);
                                uiMessage.setProgress(progress);
                                refreshSingleMessage(uiMessage);
                            }

                            @Override
                            public void onError(
                                    Message message, ai.nexconn.chat.error.NCError error) {
                                uiMessage.setState(State.ERROR);
                                refreshSingleMessage(uiMessage);
                                findNextHQVoice(uiMessage);
                            }

                            @Override
                            public void onCanceled(Message message) {
                                uiMessage.setState(State.CANCEL);
                                refreshSingleMessage(uiMessage);
                            }
                        });
    }

    private void playVoiceMessage(final UiMessage uiMessage) {
        final MessageContent content = uiMessage.getMessage().getContent();
        Uri voicePath = null;
        if (content instanceof HDVoiceMessage) {
            String localPath = ((HDVoiceMessage) content).getLocalPath();
            voicePath = localPath != null ? Uri.parse(localPath) : null;
        }
        if (voicePath != null) {
            AudioPlayManager.getInstance()
                    .startPlay(
                            getApplication(),
                            voicePath,
                            new IAudioPlayListener() {
                                @Override
                                public void onStart(Uri uri) {
                                    uiMessage.setPlaying(true);
                                    Message message = uiMessage.getMessage();
                                    message.getReceivedStatusInfo().setListened();
                                    message.setReceivedStatusInfo(
                                            message.getReceivedStatusInfo(), null);
                                    refreshSingleMessage(uiMessage);
                                }

                                @Override
                                public void onStop(Uri uri) {
                                    uiMessage.setPlaying(false);
                                    refreshSingleMessage(uiMessage);
                                }

                                @Override
                                public void onComplete(Uri uri) {
                                    uiMessage.setPlaying(false);
                                    refreshSingleMessage(uiMessage);
                                    ExecutorHelper.getInstance()
                                            .mainThread()
                                            .execute(
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            findNextHQVoice(uiMessage);
                                                        }
                                                    });
                                }
                            });
        }
    }

    private void findNextHQVoice(UiMessage uiMessage) {
        if (!NCChatUIConfig.channelConfig().NC_play_audio_continuous) {
            RLog.e(TAG, "NC_play_audio_continuous is disabled.");
            return;
        }
        int position = findPositionByMessageId(uiMessage.getMessage().getClientId());
        if (position == -1) {
            RLog.w(TAG, "the message isn't found in the list.");
            return;
        }
        for (int i = position; i < mUiMessages.size(); i++) {
            UiMessage item = mUiMessages.get(i);
            if (item.getMessage().getContent() instanceof HDVoiceMessage) {
                if (!item.getMessage().getReceivedStatusInfo().isListened()
                        && !item.getMessage().getContent().isDestruct()
                        && !TextUtils.equals(
                                item.getMessage().getSenderUserId(), NCEngine.getCurrentUserId())) {
                    onAudioClick(item);
                    break;
                }
            }
        }
    }

    public boolean onBackPressed() {
        if (Objects.equals(mIsEditStatus.getValue(), true)) {
            quitEditMode();
            return true;
        }
        return mProcessor.onBackPressed(this);
    }

    /** Exits edit mode. */
    public void quitEditMode() {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            mIsEditStatus.setValue(false);
        } else {
            mIsEditStatus.postValue(false);
        }
        List<UiMessage> uiMessageList = mUiMessages;
        for (UiMessage uiMessage : uiMessageList) {
            uiMessage.setEdit(false);
            uiMessage.setSelected(false);
        }
        mSelectedUiMessage.clear();
        mUiMessageLiveData.setValue(mUiMessages);
        // Notify the input bar to refresh
        mPageEventLiveData.setValue(
                new InputBarEvent(
                        InputBarEvent.Type.HideMoreMenu,
                        getCurChannelType() != null ? getCurChannelType().name() : ""));

        // If edit-message mode was active before multi-select, restore the editing input UI.
        mEditMessageHandler.resumeEditMode(
                mChannelIdentifier, EditMessageManager.ActiveType.OnCancelMultiSelectStatus);
    }

    public UiMessage findUIMessage(String messageUId) {
        if (TextUtils.isEmpty(messageUId)) {
            return null;
        }
        UiMessage uiMessage = null;
        for (UiMessage item : mUiMessages) {
            if (item.getMessage() != null && messageUId.equals(item.getMessage().getMessageId())) {
                uiMessage = item;
                break;
            }
        }

        return uiMessage;
    }

    @Override
    public void onSendMessage(SendEvent event) {
        Message msg = event.getMessage();
        if (isSameConversationMessage(msg) && msg.getClientId() > 0) {
            UiMessage uiMessage = findUIMessage(msg.getClientId());
            boolean isAdd = uiMessage == null;
            if (isAdd) {
                uiMessage = mapUIMessage(msg);
            } else {
                uiMessage.setMessage(msg);
            }
            long sentTime;
            switch (event.getEvent()) {
                case SendEvent.ATTACH:
                    sentTime = msg.getSentTime() - NCEngine.getServerTimeDelta();
                    msg.setSentTime(sentTime); // Update to server time
                    msg.setSentStatus(SentStatus.SENDING);
                    uiMessage.setState(State.PROGRESS);
                    break;
                case SendEvent.ERROR:
                    sentTime = msg.getSentTime() - NCEngine.getServerTimeDelta();
                    msg.setSentTime(sentTime); // Update to server time
                    // When the network is unavailable, keep the message in PROGRESS state
                    // (visually "sending") so the user knows it is queued for retry rather
                    // than permanently failed. The SDK will retry once reconnected.
                    if (shouldShowSendingForSendError(event.getCode())) {
                        msg.setSentStatus(SentStatus.SENDING);
                        uiMessage.setState(State.PROGRESS);
                        // Add message to resend queue so it will be retried when network recovers
                        ResendManager.getInstance()
                                .addResendMessage(
                                        msg,
                                        event.getCode(),
                                        (message, error) -> {
                                            // Callback after adding to resend queue
                                            RLog.d(
                                                    TAG,
                                                    "Message added to resend queue: "
                                                            + message.getClientId());
                                        });
                    } else {
                        uiMessage.setState(State.ERROR);
                    }
                    break;
                case SendEvent.SUCCESS:
                    msg.setSentStatus(SentStatus.SENT);
                    uiMessage.setState(State.NORMAL);
                    break;
            }
            if (isAdd) {
                sendMessageEvent(uiMessage);
            } else {
                refreshSingleMessage(uiMessage);
            }
        }
    }

    @Override
    public void onSendMediaMessage(SendMediaEvent event) {
        if (event == null) {
            return;
        }
        Message msg = event.getMessage();
        if (msg == null) {
            return;
        }
        if (mDeletedMessageIds.contains(msg.getClientId())) {
            RLog.d(TAG, "ignore deleted media message event, clientId=" + msg.getClientId());
            return;
        }
        if (isSameConversationMessage(msg) && msg.getClientId() > 0) {
            UiMessage uiMessage = findUIMessage(msg.getClientId());
            boolean isAdd = uiMessage == null;
            if (isAdd) {
                uiMessage = mapUIMessage(msg);
            } else {
                uiMessage.setMessage(msg);
            }
            long sentTime;
            switch (event.getEvent()) {
                case SendMediaEvent.ATTACH:
                    sentTime = msg.getSentTime() - NCEngine.getServerTimeDelta();
                    msg.setSentTime(sentTime); // Update to server time
                    msg.setSentStatus(SentStatus.SENDING);
                    uiMessage.setState(State.PROGRESS);
                    uiMessage.setProgress(0);
                    break;
                case SendMediaEvent.PROGRESS:
                    uiMessage.setState(State.PROGRESS);
                    uiMessage.setProgress(event.getProgress());
                    break;
                case SendMediaEvent.ERROR:
                    if (event.getCode() != null) {
                        int code = event.getCode().getCode();
                        if (code == ERROR_MEDIA_EXCEPTION) { // NC_MEDIA_EXCEPTION
                            ToastUtils.s(
                                    getApplication(),
                                    getApplication().getString(R.string.nc_media_upload_error));
                        } else if (code
                                == ERROR_GIF_SIZE_LIMIT_EXCEED) { // NC_GIF_MSG_SIZE_LIMIT_EXCEED
                            ToastUtils.s(
                                    getApplication(),
                                    getApplication().getString(R.string.nc_gif_message_too_large));
                        } else if (code
                                == ERROR_FILE_SIZE_EXCEED_LIMIT) { // NC_FILE_SIZE_EXCEED_LIMIT
                            ToastUtils.s(
                                    getApplication(),
                                    getApplication().getString(R.string.nc_upload_file_too_large));
                        }
                    }
                    sentTime = msg.getSentTime() - NCEngine.getServerTimeDelta();
                    msg.setSentTime(sentTime); // Update to server time
                    if (shouldShowSendingForSendError(event.getCode())) {
                        msg.setSentStatus(SentStatus.SENDING);
                        uiMessage.setState(State.PROGRESS);
                        // Add media message to resend queue so it will be retried when network
                        // recovers
                        ResendManager.getInstance()
                                .addResendMessage(
                                        msg,
                                        event.getCode(),
                                        (message, error) -> {
                                            // Callback after adding to resend queue
                                            RLog.d(
                                                    TAG,
                                                    "Media message added to resend queue: "
                                                            + message.getClientId());
                                        });
                    } else {
                        msg.setSentStatus(SentStatus.FAILED);
                        uiMessage.setState(State.ERROR);
                    }
                    break;
                case SendMediaEvent.SUCCESS:
                    msg.setSentStatus(SentStatus.SENT);
                    uiMessage.setProgress(100);
                    uiMessage.setState(State.NORMAL);
                    break;
                case SendMediaEvent.CANCEL:
                    uiMessage.setState(State.CANCEL);
                    break;
            }
            uiMessage.setMessage(msg);

            if (isAdd) {
                sendMessageEvent(uiMessage);
            } else {
                refreshSingleMessage(uiMessage);
            }
        }
    }

    private boolean shouldShowSendingForSendError(NCError error) {
        int code = error == null ? -1 : error.getCode();
        if (code == ERROR_MEDIA_EXCEPTION
                || code == ERROR_GIF_SIZE_LIMIT_EXCEED
                || code == ERROR_FILE_SIZE_EXCEED_LIMIT) {
            return false;
        }
        return isNetworkError(code) || NCEngine.getConnectionStatus() != ConnectionStatus.CONNECTED;
    }

    private boolean isNetworkError(int code) {
        return code == ERROR_NETWORK_UNAVAILABLE
                || code == ERROR_CONNECTION_UNAVAILABLE
                || code == ERROR_TIMEOUT;
    }

    @Override
    public void onDownloadMessage(DownloadEvent event) {
        Message msg = event.getMessage();
        if (isSameConversationMessage(msg) && msg.getClientId() > 0) {
            UiMessage uiMessage = findUIMessage(msg.getClientId());
            if (uiMessage != null) {
                switch (event.getEvent()) {
                    case DownloadEvent.SUCCESS:
                        uiMessage.setProgress(100);
                        uiMessage.setState(State.NORMAL);
                        break;
                    case DownloadEvent.PROGRESS:
                        uiMessage.setState(State.PROGRESS);
                        uiMessage.setProgress(event.getProgress());
                        break;
                    case DownloadEvent.ERROR:
                        uiMessage.setProgress(0);
                        uiMessage.setState(State.ERROR);
                        break;
                    case DownloadEvent.CANCEL:
                        uiMessage.setState(State.CANCEL);
                        break;
                    case DownloadEvent.PAUSE:
                        uiMessage.setState(State.PAUSE);
                        break;
                }
                uiMessage.setMessage(msg);
                refreshSingleMessage(uiMessage);
            }
        }
    }

    @Override
    public void onDeleteMessage(DeleteEvent event) {
        List<Message> deleteMessages = new ArrayList<>();
        if (event.getMessages() != null) {
            deleteMessages.addAll(event.getMessages());
        }
        if (event.getMessageIds() == null) {
            return;
        }
        for (int messageId : event.getMessageIds()) {
            mDeletedMessageIds.add(messageId);
            int position = findPositionByMessageId(messageId);
            if (position >= 0) {
                UiMessage uiMessage = mUiMessages.get(position);
                if (!containsMessage(deleteMessages, uiMessage.getMessage())) {
                    deleteMessages.add(uiMessage.getMessage());
                }
                MessageContent content = uiMessage.getMessage().getContent();
                if (AudioPlayManager.getInstance().isPlaying()) {
                    if (content instanceof HDVoiceMessage) {
                        String hqVoicePath = ((HDVoiceMessage) content).getLocalPath();
                        Uri playingUri = AudioPlayManager.getInstance().getPlayingUri();
                        if (hqVoicePath != null
                                && playingUri != null
                                && hqVoicePath.equals(playingUri.toString())) {
                            AudioPlayManager.getInstance().stopPlay();
                        }
                    }
                }
                if (content instanceof MediaMessageContent) {
                    uiMessage.getMessage().cancelDownloadingMedia(null);
                }
                mUiMessages.remove(position);
            }
        }
        mUiMessageLiveData.setValue(mUiMessages);

        // When the message list becomes empty after deletion, refresh the list
        if (NCChatUIConfig.channelConfig().isNeedRefreshWhenListIsEmptyAfterDelete()
                && mUiMessages.isEmpty()
                && mProcessor != null) {
            onRefresh();
        }
        if (!deleteMessages.isEmpty()) {
            List<UiMessage> uiMessages =
                    mEditMessageHandler.processMessageReferMsgStatus(
                            deleteMessages.toArray(new Message[0]),
                            ai.nexconn.chat.message.model.ReferenceMessageStatus.DELETED,
                            getUiMessages());
            mUiMessageLiveData.postValue(uiMessages);
        }
    }

    @Override
    public void onReplaceMessage(ReplaceEvent event) {
        if (event == null || event.getNewMessage() == null) {
            return;
        }
        int originalClientId = event.getOriginalClientId();
        int newClientId = event.getNewMessage().getClientId();
        int originalPos = findPositionByMessageId(originalClientId);
        int newPos = findPositionByMessageId(newClientId);
        // Neither message belongs to this conversation's list: nothing to do.
        if (originalPos < 0 && newPos < 0) {
            return;
        }
        mDeletedMessageIds.add(originalClientId);
        if (originalPos < 0) {
            // Original already gone; the new copy is already in the list, leave it as-is.
            return;
        }
        if (newPos < 0) {
            // New copy not in the list (unexpected); just drop the original.
            mUiMessages.remove(originalPos);
            refreshAllMessage();
            return;
        }
        // Move the newly-appended copy into the original's slot so the resent message keeps its
        // position instead of jumping to the bottom (which makes it sink under an open keyboard).
        UiMessage newUiMessage = mUiMessages.remove(newPos);
        int insertPos = findPositionByMessageId(originalClientId); // recompute after removal
        mUiMessages.remove(insertPos); // remove the original spinning copy
        mUiMessages.add(insertPos, newUiMessage);
        refreshAllMessage();
    }

    private boolean containsMessage(List<Message> messages, Message target) {
        if (messages == null || target == null) {
            return false;
        }
        for (Message message : messages) {
            if (message == target) {
                return true;
            }
            if (message != null
                    && !TextUtils.isEmpty(message.getMessageId())
                    && TextUtils.equals(message.getMessageId(), target.getMessageId())) {
                return true;
            }
            if (message != null
                    && message.getClientId() > 0
                    && message.getClientId() == target.getClientId()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onChannelMessagesCleared(ChannelIdentifier identifier) {
        if (identifier == null || mChannelIdentifier == null) {
            return;
        }
        if (!Objects.equals(mChannelIdentifier.getChannelType(), identifier.getChannelType())
                || !Objects.equals(mChannelIdentifier.getChannelId(), identifier.getChannelId())) {
            return;
        }
        // Bulk deleteMessagesForMeByTimestamp does not fire per-message onMessageDeleted,
        // so caches must be reset explicitly and an empty list pushed to the UI to avoid
        // showing stale messages when the user returns to this page.
        mainHandler.post(
                () -> {
                    mUiMessages.clear();
                    mNewUnReadMessages.clear();
                    mNewUnReadMentionMessages.clear();
                    mDeletedMessageIds.clear();
                    hideHistoryBar();
                    mNewMessageUnreadLiveData.setValue(0);
                    mNewMentionMessageUnreadLiveData.setValue(0);
                    // Push a fresh empty list reference so the adapter's DiffUtil path sees
                    // a list different from its cached reference and triggers the
                    // non-empty -> empty full refresh branch.
                    mUiMessageLiveData.setValue(new ArrayList<>());
                });
    }

    @Override
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getMessages() != null && !event.getMessages().isEmpty()) {
            List<UiMessage> uiMessages =
                    mEditMessageHandler.processMessageEditStatusAndReferMsgStatus(
                            event.getMessages(), getUiMessages());
            mUiMessageLiveData.postValue(uiMessages);
            // Update reference messages
            mEditMessageHandler.updateReferenceView(event.getMessages(), getUiMessages());
            return;
        }
        Message message = event.getMessage();
        if (message == null || TextUtils.isEmpty(message.getMessageId())) {
            return;
        }
        UiMessage uiMessage = findUIMessage(message.getMessageId());
        if (uiMessage != null) {
            // If message content was modified, clear UiMessage's ContentSpannable to re-render.
            if (event.isModifyMessageContent()) {
                uiMessage.setContentSpannable(null);
                uiMessage.setReferenceContentSpannable(null);
            }
            uiMessage.setMessage(message);
            refreshSingleMessage(uiMessage);
        }
    }

    @Override
    public void onInsertMessage(InsertEvent event) {
        Message msg = event.getMessage();
        if (msg == null) {
            return;
        }
        if (mChannelIdentifier != null
                && isSameConversationMessage(msg)
                && msg.getClientId() > 0
                && shouldContainUnknownMessage(msg)) {
            long sentTime = msg.getSentTime() - NCEngine.getServerTimeDelta();
            msg.setSentTime(sentTime); // Update to server time
            int position = findPositionBySendTime(msg.getSentTime());
            mUiMessages.add(position, mapUIMessage(msg));
            refreshAllMessage();
            executePageEvent(new ScrollEvent(position));
        }
    }

    public UiMessage findUIMessage(int clientId) {
        UiMessage uiMessage = null;
        for (UiMessage item : mUiMessages) {
            if (item.getMessage().getClientId() == clientId) {
                uiMessage = item;
                break;
            }
        }
        return uiMessage;
    }

    public void removeUIMessage(int messageId) {
        UiMessage uiMessage = findUIMessage(messageId);
        if (uiMessage != null) {
            mUiMessages.remove(uiMessage);
            refreshAllMessage();
        }
    }

    private void sendMessageEvent(UiMessage uiMessage) {
        if (!shouldContainUnknownMessage(uiMessage.getMessage())) {
            return;
        }
        if (mProcessor.isHistoryState(this)
                && uiMessage.getContent() instanceof MediaMessageContent
                && uiMessage.getState() == State.PROGRESS) {
            return;
        }
        mUiMessages.add(uiMessage);
        refreshAllMessage();
        // Skip auto scroll-to-end while the resend loop is flushing queued messages (network
        // recovery). Resend deletes the original message and appends a new one; forcing
        // scroll-to-end per resent message pushes them below an open keyboard panel. Keep the list
        // still and let the in-place status update reflect the send result instead.
        if (!ResendManager.getInstance().isResending()) {
            executePageEvent(new ScrollToEndEvent());
        }
    }

    public void refreshSingleMessage(UiMessage uiMessage) {
        int position = findPositionByMessageId(uiMessage.getMessage().getClientId());
        if (position != -1) {
            uiMessage.setChange(true);
            mUiMessageLiveData.postValue(mUiMessages);
        }
    }

    public int findPositionBySendTime(long sentTime) {
        for (int i = mUiMessages.size() - 1; i >= 0; i--) {
            UiMessage message = mUiMessages.get(i);
            if (message.getSentTime() <= sentTime) {
                return i + 1;
            }
        }
        return 0;
    }

    public void executePageEvent(PageEvent pageEvent) {
        if (Looper.getMainLooper().getThread().equals(Thread.currentThread())) {
            mPageEventLiveData.setValue(pageEvent);
        } else {
            mPageEventLiveData.postValue(pageEvent);
        }
    }

    private UiMessage findNewUnreadMessage(int clientId) {
        UiMessage result = null;
        for (UiMessage item : mNewUnReadMessages) {
            if (item.getClientId() == clientId) {
                result = item;
                break;
            }
        }
        return result;
    }

    public void processNewMessageUnread(boolean isMainThread) {
        if (NCChatUIConfig.channelConfig().isShowNewMessageBar(getCurChannelType())) {
            // Previous logic used setValue on main thread and postValue off main thread, causing
            // timing issues. Now always runs on main thread.
            mainHandler.post(() -> mNewMessageUnreadLiveData.setValue(mNewUnReadMessages.size()));
        }
    }

    public void updateNewMentionMessageUnreadBar() {
        if (NCChatUIConfig.channelConfig().isShowNewMentionMessageBar(getCurChannelType())) {
            if (Looper.getMainLooper().getThread().equals(Thread.currentThread())) {
                mNewMentionMessageUnreadLiveData.setValue(mNewUnReadMentionMessages.size());
            } else {
                mNewMentionMessageUnreadLiveData.postValue(mNewUnReadMentionMessages.size());
            }
        }
    }

    public void hideHistoryBar() {
        setFirstUnreadMessage(null);
        if (Looper.getMainLooper().getThread().equals(Thread.currentThread())) {
            mHistoryMessageUnreadLiveData.setValue(0);
        } else {
            mHistoryMessageUnreadLiveData.postValue(0);
        }
    }

    public int getRefreshMessageId() {
        int result = -1;
        if (mUiMessages.size() > 0) {
            for (UiMessage item : mUiMessages) {
                if (!(item.getClientId() == 0 || item.getClientId() == -1)) {
                    result = item.getClientId();
                    break;
                }
            }
        }
        return result;
    }

    public long getRefreshSentTime() {
        long result = 0;
        if (mUiMessages.size() > 0) {
            for (UiMessage item : mUiMessages) {
                if (item.getSentTime() > 0) {
                    result = item.getSentTime();
                    break;
                }
            }
        }
        return result;
    }

    public long getLoadMoreSentTime() {
        long result = 0;
        if (mUiMessages.size() > 0) {
            for (int i = mUiMessages.size() - 1; i >= 0; i--) {
                if (SentStatus.SENT.equals(mUiMessages.get(i).getSentStatus())) {
                    result = mUiMessages.get(i).getSentTime();
                } else {
                    result = mUiMessages.get(i).getSentTime() - NCEngine.getServerTimeDelta();
                }
                if (result > 0) {
                    break;
                }
            }
        }
        return result;
    }

    public boolean isRemoteMessageLoadFinish() {
        return mRemoteMessageLoadFinish;
    }

    public void setRemoteMessageLoadFinish(boolean remoteMessageLoadFinish) {
        this.mRemoteMessageLoadFinish = remoteMessageLoadFinish;
    }

    public boolean isScrollToBottom() {
        return mScrollToBottom;
    }

    public void setScrollToBottom(boolean scrollToBottom) {
        mScrollToBottom = scrollToBottom;
    }

    public void updateMentionMessage(Message message) {
        if (NCChatUIConfig.channelConfig()
                        .isShowNewMentionMessageBar(message.getChannelIdentifier().getChannelType())
                && message != null
                && message.getContent() != null
                && message.getContent().getMentionedInfo() != null) {
            MentionedInfo mentionedInfo = message.getContent().getMentionedInfo();
            ai.nexconn.chat.message.model.MentionedType mType = mentionedInfo.getType();
            if (mType == ai.nexconn.chat.message.model.MentionedType.ALL
                    && message.getSenderUserId() != null
                    && !message.getSenderUserId().equals(NCEngine.getCurrentUserId())) {
                mNewUnReadMentionMessages.add(message);
            } else if (mType == ai.nexconn.chat.message.model.MentionedType.USERS
                    && mentionedInfo.getUserIdList() != null
                    && mentionedInfo.getUserIdList().contains(NCEngine.getCurrentUserId())) {
                mNewUnReadMentionMessages.add(message);
            }
            updateNewMentionMessageUnreadBar();
        }
    }

    /**
     * Posts the event from a background thread to the main thread.
     *
     * @param pageEvent the event to execute
     */
    public void executePostPageEvent(PageEvent pageEvent) {
        mPageEventLiveData.postValue(pageEvent);
    }

    public void onReadReceiptStateClick(UiMessage uiMessage) {
        // If read-receipt V5 is enabled, navigate to the message read-status detail page
        if (AppSettingsHandler.getInstance().isReadReceiptV5Enabled(uiMessage.getChannelType())) {
            executePageEvent(new ReadReceiptStateClickEvent(uiMessage));
        }
    }

    public void onReEditClick(UiMessage uiMessage) {
        // Re-edit is intentionally unsupported.
    }

    public void onRefresh() {
        mProcessor.onRefresh(ChannelViewModel.this);
    }

    public void cleanUnreadNewCount() {
        mNewUnReadMessages.clear();
    }

    public void addUnreadNewMessage(UiMessage message) {
        if (MessageDirection.SEND.equals(message.getDirection())) {
            return;
        }
        UiMessage newUnreadMessage = findNewUnreadMessage(message.getClientId());
        if (newUnreadMessage == null) {
            mNewUnReadMessages.add(message);
        }
    }

    public void newMessageBarClick() {
        mProcessor.newMessageBarClick(ChannelViewModel.this);
    }

    public void unreadBarClick() {
        mProcessor.unreadBarClick(ChannelViewModel.this);
    }

    public void newMentionMessageBarClick() {
        mProcessor.newMentionMessageBarClick(ChannelViewModel.this);
    }

    /** Enters edit mode. */
    public void enterEditState() {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            mIsEditStatus.setValue(true);
        } else {
            mIsEditStatus.postValue(true);
        }
        List<UiMessage> uiMessageList = mUiMessages;
        for (UiMessage uiMessage : uiMessageList) {
            uiMessage.setEdit(true);
            uiMessage.setSelected(false);
        }
        mSelectedUiMessage.clear();
        mUiMessageLiveData.setValue(mUiMessages);
        // Notify the input bar to refresh
        mPageEventLiveData.setValue(new InputBarEvent(InputBarEvent.Type.ShowMoreMenu, ""));
    }

    public void forwardMessage(Intent data) {
        if (data == null) return;
        List<Message> messageList = new ArrayList<>();
        for (UiMessage uiMessage : getSelectedUiMessages()) {
            messageList.add(uiMessage.getMessage());
        }
        ForwardManager.getInstance()
                .forwardMessages(
                        data.getIntExtra(RouteUtils.FORWARD_TYPE, 0),
                        data
                                .<ai.nexconn.chat.channel.model.ChannelIdentifier>
                                        getParcelableArrayListExtra("conversations"),
                        data.getIntegerArrayListExtra(RouteUtils.MESSAGE_IDS),
                        messageList);
        quitEditMode();
    }

    public List<UiMessage> getSelectedUiMessages() {
        return mSelectedUiMessage;
    }

    public void onLoadMore() {
        mProcessor.onLoadMore(this);
    }

    private void removeRecallMentionMsg(Message message) {
        // Traverse the unread @-mention list; if a recalled message exists, remove it and refresh
        // the @ bar
        boolean needRefresh = false;

        int size = mNewUnReadMentionMessages.size();
        for (int i = size - 1; i >= 0; i--) {
            if (TextUtils.equals(
                    mNewUnReadMentionMessages.get(i).getMessageId(), message.getMessageId())) {
                mNewUnReadMentionMessages.remove(mNewUnReadMentionMessages.get(i));
                needRefresh = true;
                break;
            }
        }
        if (needRefresh) {
            updateNewMentionMessageUnreadBar();
        }
    }

    @Override
    protected void onCleared() {
        mConvCleared = true;
        super.onCleared();
        mThrottleHandler.removeCallbacks(mThrottledRefreshRunnable);
        mStreamMessageHandler.stop();
        mSpeechToTextHandler.stop();
        mEditMessageHandler.stop();
        mReadReceiptV5Handler.stop();
        NCEngine.removeMessageHandler("MessageViewModel_msg_" + hashCode());
        NCEngine.removeConnectionStatusHandler("MessageViewModel_conn_" + hashCode());
        NCEngine.removeChannelHandler(CONV_HANDLER_KEY + hashCode());
        NCChatUI.removeMessageEventListener(this);
        NCChatUI.removeChannelEventListener(mChannelEventListener);
        NCUserInfoManager.getInstance().removeUserDataObserver(this);
        OnLineStatusManager.getInstance().removeOnLineStatusListener(mOnLineStatusListener);
        unInitTranslationListener();
        TextAnimationHelper.clearAllCache();

        // Remove download listener
        if (mHQVoiceDownloadListener != null) {
            HQVoiceMsgDownloadManager.getInstance()
                    .removeDownloadListener(mHQVoiceDownloadListener);
            mHQVoiceDownloadListener = null;
        }
    }

    public boolean isForegroundActivity() {
        return mIsForegroundActivity;
    }

    public void onViewClick(int clickType, UiMessage data) {
        IChannelViewModelProcessor viewModelProcessor =
                NCChatUIConfig.channelConfig().getViewModelProcessor();
        boolean isProcess = false;
        if (viewModelProcessor != null) {
            isProcess = viewModelProcessor.onViewClick(this, clickType, data);
        }
        if (!isProcess) {
            switch (clickType) {
                case MessageClickType.AUDIO_CLICK:
                    onAudioClick(data);
                    break;
                case MessageClickType.WARNING_CLICK:
                    onWarnClick(data);
                    break;
                case MessageClickType.REEDIT_CLICK:
                    onReEditClick(data);
                    break;
                case MessageClickType.READ_RECEIPT_STATE_CLICK:
                    onReadReceiptStateClick(data);
                    break;
                case MessageClickType.CONTENT_CLICK:
                    onItemClick(data);
                    break;
                case MessageClickType.USER_PORTRAIT_CLICK:
                    onUserPortraitClick(
                            getApplication(),
                            data.getChannelType(),
                            data.getUserInfo(),
                            data.getMessage().getChannelIdentifier().getChannelId());
                    break;
                case MessageClickType.EDIT_CLICK:
                    boolean selected = data.isSelected();
                    int preSize = mSelectedUiMessage.size();
                    if (selected) {
                        mSelectedUiMessage.remove(data);
                        if (mSelectedUiMessage.size() <= 0) {
                            mPageEventLiveData.postValue(
                                    new InputBarEvent(InputBarEvent.Type.InactiveMoreMenu, null));
                        }
                        data.setSelected(false);
                        refreshSingleMessage(data);
                    } else {
                        if (mSelectedUiMessage.size()
                                < NCChatUIConfig.channelConfig().NC_max_message_selected_count) {
                            mSelectedUiMessage.add(data);
                            if (mSelectedUiMessage.size() > 0 && preSize <= 0) {
                                mPageEventLiveData.setValue(
                                        new InputBarEvent(InputBarEvent.Type.ActiveMoreMenu, null));
                            }
                            data.setSelected(true);
                            refreshSingleMessage(data);
                        } else {
                            if (NCChatUIConfig.channelConfig().NC_max_message_selected_count
                                    == 100) {
                                executePageEvent(
                                        new ToastEvent(
                                                getApplication()
                                                        .getString(
                                                                R.string
                                                                        .nc_exceeded_max_limit_100)));
                            }
                        }
                    }
                    break;
                case MessageClickType.STREAM_MSG_PULL:
                    mStreamMessageHandler.fetchStreamMessage(
                            data.getMessageId(),
                            Objects.equals(
                                    data.getBusinessState(),
                                    StreamMessageHandler.State.RETRY_PULL));
                    break;
                case MessageClickType.SPEECH_TO_TEXT:
                    mSpeechToTextHandler.setMessageSpeechToTextVisible(data.getClientId(), false);
                    break;
                default:
                    break;
            }
        }
    }

    public boolean onViewLongClick(int clickType, UiMessage data) {
        IChannelViewModelProcessor viewModelProcessor =
                NCChatUIConfig.channelConfig().getViewModelProcessor();
        boolean isProcess = false;
        if (viewModelProcessor != null) {
            isProcess = viewModelProcessor.onViewLongClick(this, clickType, data);
        }
        if (!isProcess) {
            switch (clickType) {
                case MessageClickType.USER_PORTRAIT_LONG_CLICK:
                    return onUserPortraitLongClick(
                            getApplication(),
                            data.getChannelType(),
                            data.getUserInfo(),
                            data.getMessage().getChannelIdentifier().getChannelId());
                default:
                    return false;
            }
        } else {
            return true;
        }
    }

    public LiveData<List<UiMessage>> getUiMessageLiveData() {
        return mUiMessageLiveData;
    }

    public MediatorLiveData<Integer> getNewMessageUnreadLiveData() {
        return mNewMessageUnreadLiveData;
    }

    public MediatorLiveData<Boolean> IsEditStatusLiveData() {
        return mIsEditStatus;
    }

    public List<UiMessage> getNewUnReadMessages() {
        return mNewUnReadMessages;
    }

    public List<Message> getNewUnReadMentionMessages() {
        return mNewUnReadMentionMessages;
    }

    public void setNewUnReadMentionMessages(List<Message> newUnReadMentionMessages) {
        this.mNewUnReadMentionMessages = newUnReadMentionMessages;
    }

    public void showHistoryBar(int unreadMessageCount) {
        mHistoryMessageUnreadLiveData.setValue(unreadMessageCount);
    }

    public LiveData<Integer> getHistoryMessageUnreadLiveData() {
        return mHistoryMessageUnreadLiveData;
    }

    public void showNewMentionMessageBar(int unreadMessageCount) {
        mNewMentionMessageUnreadLiveData.setValue(unreadMessageCount);
    }

    public void hideNewMentionMessageBar() {
        if (mNewUnReadMentionMessages != null) {
            mNewUnReadMentionMessages.clear();
        }
        mNewMentionMessageUnreadLiveData.setValue(0);
    }

    public LiveData<Integer> getNewMentionMessageUnreadLiveData() {
        return mNewMentionMessageUnreadLiveData;
    }

    public void onScrolled(
            RecyclerView recyclerView, int dx, int dy, int headerCount, int footerCount) {
        // Check whether scrolled to the bottom
        if (!recyclerView.canScrollVertically(1)) {
            setScrollToBottom(true);
            mProcessor.onScrollToBottom(this);
        } else {
            setScrollToBottom(false);
        }
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (NCChatUIConfig.channelConfig().isShowHistoryMessageBar(getCurChannelType())
                && getFirstUnreadMessage() != null) {
            int firstPosition = findPositionByMessageId(getFirstUnreadMessage().getClientId());
            if (layoutManager instanceof LinearLayoutManager) {
                int firstVisibleItemPosition =
                        ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                if (firstVisibleItemPosition <= firstPosition) {
                    hideHistoryBar();
                }
            }
        }

        if (NCChatUIConfig.channelConfig().isShowHistoryMessageBar(getCurChannelType())
                && mNewUnReadMentionMessages != null
                && mNewUnReadMentionMessages.size() > 0
                && getUiMessages().size() > 0) {
            int firstVisibleItemPosition = 0;
            int lastPosition = 0;
            if (layoutManager instanceof LinearLayoutManager) {
                firstVisibleItemPosition =
                        ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition()
                                - headerCount;
                lastPosition =
                        ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition()
                                - headerCount;
            }
            // Calculate first and last visible message positions, clamped within the list bounds
            int msgSize = getUiMessages().size();
            int firstMessagePosition = Math.min(msgSize - 1, Math.max(firstVisibleItemPosition, 0));
            int lastMessagePosition =
                    lastPosition < msgSize && lastPosition >= 0 ? lastPosition : msgSize - 1;
            UiMessage firstMessage = getUiMessages().get(0);
            if (firstMessagePosition >= 0 && firstMessagePosition < msgSize) {
                firstMessage = getUiMessages().get(firstMessagePosition);
            }
            UiMessage lastMessage = getUiMessages().get(msgSize - 1);
            if (lastMessagePosition >= 0 && lastMessagePosition < msgSize) {
                lastMessage = getUiMessages().get(lastMessagePosition);
            }
            long topTime = firstMessage.getSentTime();
            long bottomTime = lastMessage.getSentTime();
            int size = mNewUnReadMentionMessages.size();
            for (int i = size - 1; i >= 0; i--) {
                if (i < mNewUnReadMentionMessages.size()) {
                    Message newUnReadMentionMessage = mNewUnReadMentionMessages.get(i);
                    if (newUnReadMentionMessage.getSentTime() >= topTime
                            && newUnReadMentionMessage.getSentTime() <= bottomTime) {
                        mNewUnReadMentionMessages.remove(newUnReadMentionMessage);
                    }
                }
            }
        }
        updateNewMentionMessageUnreadBar();
    }

    public List<UiMessage> getUiMessages() {
        return mUiMessages;
    }

    public void onExistUnreadMessage(long sentTime, int unreadMessageCount) {
        if (mProcessor != null) mProcessor.onExistUnreadMessage(this, sentTime, unreadMessageCount);
    }

    public void onResume() {
        mIsForegroundActivity = true;
        if (mProcessor != null) mProcessor.onResume(this);
        cleanUnreadStatus();
        if (NCChatUIConfig.featureConfig().NC_wipe_out_notification_message) {
            clearAllNotification();
        }
    }

    /** Clears the unread status. */
    public void cleanUnreadStatus() {
        if (isInitUnreadMessageFinish() && isInitMentionedMessageFinish()) {
            if (getFirstUnreadMessage() != null) {
                mKeepHistoryBarOnNextUnreadClear = true;
            }
            if (mChannelIdentifier != null) {
                NCChatUI.clearUnreadCount(mChannelIdentifier, null);
            }
        }
    }

    private void clearAllNotification() {
        ChatUINotificationManager.getInstance().clearAllNotification();
    }

    public boolean isInitUnreadMessageFinish() {
        return mInitUnreadMessageFinish;
    }

    public void setInitUnreadMessageFinish(boolean initUnreadMessageFinish) {
        mInitUnreadMessageFinish = initUnreadMessageFinish;
    }

    public boolean isInitMentionedMessageFinish() {
        return mInitMentionedMessageFinish;
    }

    public void setInitMentionedMessageFinish(boolean initMentionedMessageFinish) {
        mInitMentionedMessageFinish = initMentionedMessageFinish;
    }

    public void onPause() {
        stopPlay();
    }

    public void stopPlay() {
        AudioPlayManager.getInstance().stopPlay();
    }

    public void onStop() {
        mIsForegroundActivity = false;
    }

    public void onDestroy() {
        syncConversationUnReadStatus(getCurChannelType(), getCurTargetId());
        stopPlay();
        mManualResendInFlight.clear();
        if (mProcessor != null) mProcessor.onDestroy(this);
    }

    /**
     * Filters persisted-but-uncounted messages so they do not trigger the new-message bubble UI at
     * the bottom-right of the chat page.
     *
     * @param uiMessage the message to check
     * @return true if the message should be hidden from the new-message bar
     */
    public boolean filterMessageToHideNewMessageBar(UiMessage uiMessage) {
        if (uiMessage == null
                || uiMessage.getMessage() == null
                || uiMessage.getMessage().getContent() == null) {
            return false;
        }

        if (uiMessage.getMessage() != null
                && isMessagePersisted(uiMessage.getMessage())
                && !isMessageCounted(uiMessage.getMessage())) {
            return true;
        }

        return false;
    }

    public boolean isNormalState() {
        return mProcessor.isNormalState(ChannelViewModel.this);
    }

    public boolean isHistoryState() {
        return mProcessor.isHistoryState(ChannelViewModel.this);
    }

    @Override
    public void onUserUpdate(ai.nexconn.chat.user.model.UserInfo user) {
        if (user != null) {
            for (UiMessage item : getUiMessages()) {
                item.onUserInfoUpdate(user);
            }
            refreshAllMessage(false);
        }
    }

    @Override
    public void onGroupUpdate(ai.nexconn.chat.channel.model.GroupInfo group) {
        // default implementation ignored
    }

    @Override
    public void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo) {
        if (!ChannelType.GROUP.equals(getCurChannelType())
                || groupUserInfo == null
                || !groupUserInfo.getGroupId().equals(getCurTargetId())) {
            return;
        }
        for (UiMessage item : getUiMessages()) {
            item.onGroupMemberInfoUpdate(groupUserInfo);
        }
        refreshAllMessage(false);
    }

    public void initTranslationListener() {
        // Translation feature removed — no nexconn equivalent
    }

    public void unInitTranslationListener() {
        // Translation feature removed — no nexconn equivalent
    }

    private UiMessage getUiMessageById(int clientId) {
        for (int i = mUiMessages.size() - 1; i >= 0; i--) {
            if (mUiMessages.get(i).getClientId() == clientId) {
                return mUiMessages.get(i);
            }
        }
        return null;
    }

    /**
     * Checks whether the message belongs to the same channel as the current one.
     *
     * @param message the message to check
     * @return true if the message's channel matches mChannelIdentifier; false otherwise
     */
    private boolean isSameConversationMessage(Message message) {
        if (mChannelIdentifier == null
                || mChannelIdentifier.getChannelType() == null
                || TextUtils.isEmpty(mChannelIdentifier.getChannelId())) {
            return false;
        }
        if (message == null
                || message.getChannelIdentifier() == null
                || message.getChannelIdentifier().getChannelType() == null
                || TextUtils.isEmpty(message.getChannelIdentifier().getChannelId())) {
            return false;
        }
        ChannelType msgType = message.getChannelIdentifier().getChannelType();
        String msgTargetId = message.getChannelIdentifier().getChannelId();
        return Objects.equals(mChannelIdentifier.getChannelType(), msgType)
                && Objects.equals(mChannelIdentifier.getChannelId(), msgTargetId);
    }

    /**
     * Checks whether the message should be included in the message list (filters out
     * UnknownMessages).
     *
     * @param message the message to check
     * @return true if the message should be displayed; false if it should be filtered out
     */
    private boolean shouldContainUnknownMessage(Message message) {
        if (message != null && message.getContent() instanceof UnknownMessage) {
            // Uncounted UnknownMessages are typically internal control/notification payloads.
            // Filter them out from message list UI to avoid showing fallback gray bars.
            if (!isMessageCounted(message) || !isMessagePersisted(message)) {
                return false;
            }
            return NCChatUIConfig.featureConfig().isShowUnknownMessage();
        }
        return true;
    }

    /**
     * Syncs unread count changes with UnReadMessageManager. Note: if both NC_read_receipt and
     * NC_enable_sync_read_status are set to false, UnReadMessageManager will not trigger changes
     * automatically and must be triggered manually.
     */
    private void syncConversationUnReadStatus(ChannelType type, String targetId) {
        ChannelConfig config = NCChatUIConfig.channelConfig();
        if (config.isEnableMultiDeviceSync(type)) {
            return;
        }
        if (ChannelType.DIRECT != type && ChannelType.GROUP != type) {
            return;
        }
        if (TextUtils.isEmpty(targetId)) {
            return;
        }
        if (!config.isShowReadReceipt(type) && !config.isShowReadReceiptRequest(type)) {
            RLog.d(TAG, "syncConversationUnReadStatus");
            UnReadMessageManager.getInstance()
                    .onSyncConversationReadStatus(new ChannelIdentifier(type, targetId));
        }
    }

    /** Called when a message item becomes visible; sends a read receipt. */
    public void onItemViewVisible(boolean visible, UiMessage data) {
        if (!visible || data == null || data.getMessage() == null) {
            return;
        }
        mReadReceiptV5Handler.sendReadReceiptResponseV5(data.getMessage());
    }

    // =========================================================
    // Conversation meta (merged from ConversationViewModel)
    // =========================================================

    public MediatorLiveData<TypingInfo> getTypingStatusInfo() {
        return mTypingStatusInfo;
    }

    public MediatorLiveData<UserOnlineStatus> getOnlineStatus() {
        return mOnlineStatus;
    }

    public boolean isOnlineStatus() {
        return mOnlineStatus.getValue() != null && mOnlineStatus.getValue().isOnline();
    }

    public MediatorLiveData<Boolean> getNotify() {
        return mIsNotify;
    }

    public void getUserOnlineStatus(String targetId) {
        this.mConvTargetId = targetId;
        RLog.d(TAG, "fetchUsersOnlineStatus.");
        OnLineStatusManager.getInstance().fetchUsersOnlineStatus(targetId, false);
    }

    public void getNotificationStatus(ChannelType channelType, String targetId) {
        ChannelIdentifier identifier = new ChannelIdentifier(channelType, targetId);
        BaseChannel.getChannels(
                java.util.Collections.singletonList(identifier),
                new OperationHandler<List<BaseChannel>>() {
                    @Override
                    public void onResult(List<BaseChannel> result, NCError error) {
                        if (mConvCleared) {
                            return;
                        }
                        if (error == null && result != null && !result.isEmpty()) {
                            ChannelNoDisturbLevel level = result.get(0).getNoDisturbLevel();
                            mIsNotify.postValue(level != ChannelNoDisturbLevel.MUTED);
                        }
                    }
                });
    }
}
