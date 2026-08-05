package ai.nexconn.chatui.channel.feature.editmessage;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.DirectChannel;
import ai.nexconn.chat.channel.GroupChannel;
import ai.nexconn.chat.channel.OpenChannel;
import ai.nexconn.chat.channel.SystemChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.ErrorHandler;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.message.MediaMessageContent;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.ReferenceMessage;
import ai.nexconn.chat.message.TextMessage;
import ai.nexconn.chat.message.model.MessageUpdateInfo;
import ai.nexconn.chat.message.model.MessageUpdateStatus;
import ai.nexconn.chat.message.model.SentStatus;
import ai.nexconn.chat.params.UpdateMessageParams;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.event.action.RefreshEvent;
import ai.nexconn.chatui.channel.extension.IExtensionModule;
import ai.nexconn.chatui.channel.extension.InputMode;
import ai.nexconn.chatui.channel.extension.NCExtension;
import ai.nexconn.chatui.channel.extension.NCExtensionCacheHelper;
import ai.nexconn.chatui.channel.extension.NCExtensionManager;
import ai.nexconn.chatui.channel.extension.NCExtensionViewModel;
import ai.nexconn.chatui.channel.extension.component.emoticon.IEmoticonTab;
import ai.nexconn.chatui.channel.extension.component.plugin.IPluginModule;
import ai.nexconn.chatui.channel.feature.mention.DraftHelper;
import ai.nexconn.chatui.channel.feature.mention.IExtensionEventWatcher;
import ai.nexconn.chatui.channel.feature.mention.MentionBlock;
import ai.nexconn.chatui.channel.feature.mention.MentionInstance;
import ai.nexconn.chatui.channel.feature.mention.NCMentionManager;
import ai.nexconn.chatui.channel.feature.reference.ReferenceManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.handler.EditMessageHandler;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.keyboard.KeyboardHeightObserver;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.system.ExecutorHelper;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;

public class EditMessageManager implements IExtensionEventWatcher, IExtensionModule {
    public static final String TAG = "EditMessageManager";
    private static final int MAX_MESSAGE_LENGTH_TO_SEND = 5000;
    private WeakReference<NCExtension> mNCExtension;
    private WeakReference<Fragment> mFragment;
    private final Stack<EditMessageState> stack = new Stack<>();
    // Keyboard height observers
    private final List<KeyboardHeightObserver> mObservers = new ArrayList<>();
    private static final String CONNECTION_HANDLER_ID = "EditMessageManager";

    private static final long DEFAULT_MESSAGE_MODIFIABLE_MINUTES = 2 * 60L;

    private final DraftHelper draftHelper = new DraftHelper();
    private final ai.nexconn.chat.handler.ConnectionStatusHandler connectionStatusHandler =
            event -> {};
    // Whether currently in full-screen edit state with emoji panel shown
    private boolean isEmoticonMode = false;
    private final EditMessageHandler editMessageHandler = new EditMessageHandler();
    private List<StatusListener> mStatusListenerList = new CopyOnWriteArrayList<>();

    private EditMessageManager() {
        NCEngine.addConnectionStatusHandler(CONNECTION_HANDLER_ID, connectionStatusHandler);
    }

    private static class Holder {
        private static final EditMessageManager INSTANCE = new EditMessageManager();
    }

    public static EditMessageManager getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public void onInit(Context context, String appKey) {}

    @Override
    public void onAttachedToExtension(Fragment fragment, NCExtension extension) {
        if (fragment == null
                || fragment.isDetached()
                || fragment.getContext() == null
                || fragment.getFragmentManager() == null) {
            return;
        }
        // Save Fragment, NCExtension, NCExtensionViewModel
        mFragment = new WeakReference<>(fragment);
        mNCExtension = new WeakReference<>(extension);
        // Save to Stack
        EditMessageState editMessageInstance = new EditMessageState();
        editMessageInstance.mFragment = mFragment;
        editMessageInstance.mNCExtension = mNCExtension;
        stack.add(editMessageInstance);
        NCExtensionManager.getInstance().addExtensionEventWatcher(this);
        // Decide whether to activate edit component based on last saved edit config
        editMessageHandler.resumeEditMode(
                extension.getChannelIdentifier(),
                EditMessageManager.ActiveType.OnAttachedToExtension);
    }

    /** Handles resume edit mode result with different logic based on ActiveType. */
    public void onResumeEditModeResult(ActiveType type, EditMessageConfig config) {
        if (type == EditMessageManager.ActiveType.OnCancelMultiSelectStatus) {
            activeEditMode(type, config, false);
        } else if (type == EditMessageManager.ActiveType.OnAttachedToExtension) {
            activeEditMode(type, config, true);
        }
    }

    @Override
    public void onDetachedFromExtension() {}

    @Override
    public void onReceivedMessage(ai.nexconn.chat.message.Message message) {}

    @Override
    public List<IPluginModule> getPluginModules(ai.nexconn.chat.channel.ChannelType channelType) {
        return Collections.emptyList();
    }

    @Override
    public List<IEmoticonTab> getEmoticonTabs() {
        return Collections.emptyList();
    }

    @Override
    public void onDisconnect() {}

    // Initialize context: mFragment, mNCExtension
    private void initContext() {
        if ((mNCExtension == null || mFragment == null) && !stack.isEmpty()) {
            EditMessageState topState = stack.peek();
            mNCExtension = topState.mNCExtension;
            mFragment = topState.mFragment;
        }
    }

    private boolean onMessageItemLongClick(Context context, UiMessage uiMessage) {
        Message message = uiMessage.getMessage();
        EditMessageConfig config = new EditMessageConfig();
        config.uid = uiMessage.getMessageId();
        config.content = EditMessageUtils.getOriginalContent(message);
        String referContent = EditMessageUtils.getReferContent(message);
        if (!TextUtils.isEmpty(referContent)) {
            String name = EditMessageUtils.getDisplayName(message);
            config.referContent = name + ":" + referContent;
        }
        if (message.getContent() instanceof ReferenceMessage) {
            ReferenceMessage referenceMessage = (ReferenceMessage) message.getContent();
            config.referStatus = referenceMessage.getReferMsgStatus();
            config.referUid = referenceMessage.getReferMsgId();
        }
        config.sentTime = uiMessage.getSentTime();
        config.mentionBlocks = getMentionBlocks(uiMessage);
        activeEditMode(ActiveType.OnLongClickMessage, config, true);
        return true;
    }

    public boolean onMessageLongClickEdit(Context context, UiMessage uiMessage) {
        return onMessageItemLongClick(context, uiMessage);
    }

    /**
     * Activates edit message mode.
     *
     * @param type activation type
     * @param config edit message configuration
     * @param showKeyBoard whether to show the soft keyboard
     */
    public void activeEditMode(ActiveType type, EditMessageConfig config, boolean showKeyBoard) {
        if (config == null || TextUtils.isEmpty(config.uid) || TextUtils.isEmpty(config.content)) {
            return;
        }
        initContext();
        if (mNCExtension == null || mFragment == null) {
            return;
        }
        NCExtension extension = mNCExtension.get();
        Fragment fragment = mFragment.get();
        if (extension == null || fragment == null) {
            return;
        }
        Activity activity = fragment.getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (stack.isEmpty()) {
            return;
        }
        EditMessageState lastState = stack.peek();
        String lastUid = lastState.config != null ? lastState.config.uid : "";
        // If already editing a message, check if the new message UID matches the current one
        if (!TextUtils.isEmpty(lastUid)) {
            // Long-press edit with same UID, ignore
            if (type == ActiveType.OnLongClickMessage && TextUtils.equals(lastUid, config.uid)) {
                return;
            }
            // Different UID, show confirmation dialog
            if (!TextUtils.equals(lastUid, config.uid)) {
                EditMessageDialog.OnClickListener listener =
                        (v, b) -> postDelayed(() -> activeEditModeReally(config, showKeyBoard));
                EditMessageDialog dialog =
                        EditMessageDialog.newInstance(activity)
                                .setTitleText(R.string.nc_prompt)
                                .setContentMessage(R.string.nc_dialog_edit_message_content)
                                .setButtonText(R.string.nc_dialog_ok, R.string.nc_back)
                                .setOnClickListener(listener);
                dialog.show();
                return;
            }
        }
        postDelayed(() -> activeEditModeReally(config, showKeyBoard));
    }

    private void activeEditModeReally(EditMessageConfig config, boolean showKeyBoard) {
        if (config == null || TextUtils.isEmpty(config.uid) || TextUtils.isEmpty(config.content)) {
            return;
        }
        initContext();
        if (mNCExtension == null || mFragment == null) {
            return;
        }
        NCExtension extension = mNCExtension.get();
        Fragment fragment = mFragment.get();
        if (extension == null || fragment == null) {
            return;
        }
        Activity activity = fragment.getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (stack.isEmpty()) {
            return;
        }
        // Call InputPanel.onPause() to save draft.
        // Must be called before adding edit UI (EditMessageInputPanel) and before hiding
        // reference UI (hideReferenceView), otherwise the reference message will be cleared.
        extension.getInputPanel().onPause();
        // Add view
        RelativeLayout container = extension.getContainer(NCExtension.ContainerType.INPUT);
        EditMessageInputPanel mEditMessageInputPanel =
                new EditMessageInputPanel(
                        fragment,
                        container,
                        extension.getChannelIdentifier(),
                        config.referUid,
                        config.uid);
        if (mEditMessageInputPanel.getRootView() == null) {
            return;
        }
        // Remove default InputPanel layout
        container.removeAllViews();
        // Add edit message input panel layout
        container.addView(mEditMessageInputPanel.getRootView());
        // Get ViewModel
        NCExtensionViewModel mExtensionViewModel =
                new ViewModelProvider(fragment).get(NCExtensionViewModel.class);
        // Bind ViewModel to edit message input panel's EditText
        mExtensionViewModel.setEditTextWidget(mEditMessageInputPanel.getEditText());
        addMentionBlocks(mEditMessageInputPanel.getEditText(), config.mentionBlocks);
        // Delay keyboard popup to avoid abrupt animation from quick keyboard hide/show
        postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mFragment == null) {
                            return;
                        }
                        Fragment fragment = mFragment.get();
                        if (fragment == null
                                || fragment.getActivity() == null
                                || fragment.getActivity().isFinishing()) {
                            return;
                        }
                        // Insert message content into edit message input EditText and set reference
                        // content
                        mEditMessageInputPanel.setContent(config, showKeyBoard);
                    }
                });
        // Save activation state to memory
        EditMessageState lastState = stack.peek();
        lastState.config = config;
        // Query if editable and refresh UI based on result
        mEditMessageInputPanel.setCheckMessageModifiableResult(
                checkMessageModifiable(config.sentTime));
        // Clear reference message component; must be placed after setEditMessageConfig
        ReferenceManager.getInstance().hideReferenceView();
        // Save edit message state
        editMessageHandler.saveEditedMessageDraft(extension.getChannelIdentifier(), config);
        notifyVisibilityChanged(true);
    }

    /** Exits edit message state, clears edit message configuration, and retrieves draft. */
    public void exitEditMode() {
        initContext();
        NCExtension extension = mNCExtension.get();
        if (extension != null) {
            // Clear in-memory state
            if (!stack.isEmpty()) {
                EditMessageState lastState = stack.peek();
                lastState.config = null;
            }
            // Clear activation state
            editMessageHandler.clearEditedMessageDraft(extension.getChannelIdentifier());
            extension.resetToDefaultView(null, InputMode.TextInput, true);
            postDelayed(() -> extension.getInputPanel().getDraftReally(null));
        }
        notifyVisibilityChanged(false);
    }

    private void notifyVisibilityChanged(boolean isVisible) {
        for (StatusListener listener : this.mStatusListenerList) {
            listener.onVisibilityChanged(isVisible);
        }
    }

    public void onKeyboardHeightChange(int orientation, boolean isOpen, int keyboardHeight) {
        for (KeyboardHeightObserver observer : mObservers) {
            observer.onKeyboardHeightChanged(orientation, isOpen, keyboardHeight);
        }
    }

    /** Whether currently in edit message state. */
    public boolean isEditMessageState() {
        return !EditMessageConfig.isInvalid(getEditMessageConfig());
    }

    /** Gets the edit message config if in edit message state. */
    public EditMessageConfig getEditMessageConfig() {
        // Get edit state from memory, not from Lib in real time
        if (!stack.isEmpty()) {
            EditMessageState lastState = stack.peek();
            if (lastState != null && lastState.config != null) {
                return lastState.config;
            }
        }
        return null;
    }

    // Whether in full-screen input state and emoji input mode
    public boolean isEmoticonMode() {
        return isEmoticonMode;
    }

    public void setEmoticonMode(boolean show) {
        this.isEmoticonMode = show;
    }

    public void editMessage(EditText editText, ErrorHandler callback) {
        String text = editText.getText().toString();
        if (EditMessageUtils.isBlankEditContent(text)) {
            return;
        }
        if (text.length() > MAX_MESSAGE_LENGTH_TO_SEND) {
            ToastUtils.show(
                    editText.getContext(),
                    editText.getContext().getString(R.string.nc_message_too_long),
                    Toast.LENGTH_SHORT);
            RLog.d(TAG, "The text you entered is too long to send.");
            return;
        }
        if (stack.isEmpty()) {
            RLog.d(TAG, "The stack is empty.");
            return;
        }
        EditMessageState lastState = stack.peek();
        String uid = "";
        if (lastState.config != null && !TextUtils.isEmpty(lastState.config.uid)) {
            uid = lastState.config.uid;
        }
        BaseChannel.getMessageById(
                new ai.nexconn.chat.params.GetMessageByIdParams(uid, null),
                new OperationHandler<Message>() {
                    @Override
                    public void onResult(Message message, NCError error) {
                        if (error != null || message == null || message.getContent() == null) {
                            showEditFailedDialog(R.string.nc_dialog_edit_message_is_delete);
                            return;
                        }
                        boolean checked = checkMessageModifiable(message.getSentTime());
                        if (!checked) {
                            callback.onError(new NCError(33402, "NC_MODIFIED_MESSAGE_TIMEOUT"));
                            return;
                        }
                        NCMentionManager.getInstance().onClickEditMessageConfirm(message, editText);
                        callback.onError(null);
                        editMessage(message, text);
                    }
                });
    }

    public void editMessage(Message message, String editContent) {
        if (message == null || message.getContent() == null) {
            RLog.e(TAG, "editMessage message or content null");
            return;
        }
        MessageContent oldContent = message.getContent();
        MessageContent newContent;
        MessageUpdateInfo oldUpdateInfo = message.getUpdateInfo();
        if (TextUtils.isEmpty(editContent)) {
            if (oldUpdateInfo != null && oldUpdateInfo.getContent() != null) {
                newContent = oldUpdateInfo.getContent();
            } else {
                RLog.e(
                        TAG,
                        "editMessage updateInfo null editContent null" + message.getMessageId());
                showEditErrorToast(new NCError(-1, "EDIT_CONTENT_EMPTY"), editContent);
                return;
            }
        } else if (EditMessageUtils.isBlankEditContent(editContent)) {
            showEditErrorToast(new NCError(-1, "EDIT_CONTENT_BLANK"), editContent);
            return;
        } else if (oldContent instanceof TextMessage) {
            TextMessage oldText = (TextMessage) oldContent;
            TextMessage newText = new TextMessage(editContent);
            copyCommonMessageContent(oldText, newText);
            newContent = newText;
        } else if (oldContent instanceof ReferenceMessage) {
            newContent = copyReferenceMessage((ReferenceMessage) oldContent, editContent);
        } else {
            RLog.e(
                    TAG,
                    "editMessage objectName error" + message.getMessageId() + "," + editContent);
            return;
        }
        long updateTimestamp =
                oldUpdateInfo != null ? oldUpdateInfo.getTimestamp() : System.currentTimeMillis();
        message.setUpdateInfo(
                new MessageUpdateInfo(updateTimestamp, newContent, MessageUpdateStatus.UPDATING));
        message.setContent(newContent);
        refreshUIMessage(message);
        UpdateMessageParams params = new UpdateMessageParams(message.getMessageId(), newContent);
        createChannel(message.getChannelIdentifier())
                .updateMessage(
                        params,
                        new OperationHandler<Message>() {
                            @Override
                            public void onResult(Message updated, NCError error) {
                                if (error != null) {
                                    showEditErrorToast(error, editContent);
                                    MessageUpdateInfo info = message.getUpdateInfo();
                                    long timestamp =
                                            info != null
                                                    ? info.getTimestamp()
                                                    : System.currentTimeMillis();
                                    MessageContent retryContent =
                                            info != null ? info.getContent() : newContent;
                                    message.setContent(oldContent);
                                    message.setUpdateInfo(
                                            new MessageUpdateInfo(
                                                    timestamp,
                                                    retryContent,
                                                    MessageUpdateStatus.FAILED));
                                    refreshUIMessage(message);
                                    return;
                                }
                                if (updated != null) {
                                    refreshUIMessage(updated);
                                }
                            }
                        });
    }

    private MessageContent copyReferenceMessage(ReferenceMessage source, String editContent) {
        ReferenceMessage copy = new ReferenceMessage();
        copyCommonMessageContent(source, copy);
        copyMediaMessageContent(source, copy);
        copy.setContent(editContent);
        copy.setReferMsgSenderId(source.getReferMsgSenderId());
        copy.setReferMsgType(source.getReferMsgType());
        copy.setReferMsg(source.getReferMsg());
        copy.setReferMsgId(source.getReferMsgId());
        copy.setReferMsgStatus(source.getReferMsgStatus());
        return copy;
    }

    private void copyCommonMessageContent(MessageContent source, MessageContent target) {
        target.setExtra(source.getExtra());
        target.setMentionedInfo(source.getMentionedInfo());
        target.setDestruct(source.isDestruct());
        target.setDestructTime(source.getDestructTime());
        target.setSenderUserInfo(source.getSenderUserInfo());
    }

    private void copyMediaMessageContent(MediaMessageContent source, MediaMessageContent target) {
        target.setLocalPath(source.getLocalPath());
        target.setRemoteUrl(source.getRemoteUrl());
        target.setName(source.getName());
    }

    private void showEditErrorToast(NCError error, String editContent) {
        if (error == null) {
            return;
        }
        int redId;
        if (error.getCode() == 22201) { // NC_ORIGINAL_MESSAGE_NOT_EXIST
            redId = R.string.nc_edit_failed_by_remote_msg_not_exist;
        } else if (error.getCode() == 20112 // DANGEROUS_CONTENT
                || error.getCode() == 20113) { // CONTENT_REVIEW_REJECTED
            redId = R.string.nc_edit_failed_by_sensitive;
        } else if (error.getCode() == 20114 // MESSAGE_OVER_MODIFY_TIME_FAIL
                || error.getCode() == 33402) { // NC_MODIFIED_MESSAGE_TIMEOUT
            redId = R.string.nc_edit_failed_by_expire;
        } else {
            redId =
                    TextUtils.isEmpty(editContent)
                            ? R.string.nc_edit_status_retry_failed
                            : R.string.nc_edit_status_failed;
        }
        Context context = NCChatUI.getContext();
        ToastUtils.show(context, context.getText(redId), Toast.LENGTH_SHORT);
    }

    private void refreshUIMessage(Message message) {
        if (message == null) {
            return;
        }
        ExecutorHelper.getInstance()
                .mainThread()
                .execute(
                        () -> {
                            List<Message> messages = new ArrayList<>();
                            messages.add(message);
                            NCChatUI.refreshMessage(new RefreshEvent(messages, true));
                        });
    }

    @Override
    public void onTextChanged(
            Context context,
            ChannelType type,
            String targetId,
            int cursorPos,
            int count,
            String text) {
        // Re-sync config whenever content changes
        if (stack.isEmpty()) {
            return;
        }
        EditMessageState lastState = stack.peek();
        if (lastState == null || lastState.mFragment == null || lastState.config == null) {
            return;
        }
        Fragment fragment = lastState.mFragment.get();
        if (fragment == null) {
            return;
        }
        NCExtensionViewModel extensionViewModel =
                new ViewModelProvider(fragment).get(NCExtensionViewModel.class);
        if (extensionViewModel.getEditTextWidget() == null) {
            return;
        }
        MentionInstance mentionInstance =
                NCMentionManager.getInstance()
                        .obtainMentionInstance(extensionViewModel.getEditTextWidget());
        lastState.config.mentionBlocks =
                mentionInstance != null ? mentionInstance.mentionBlocks : null;
        lastState.config.content = text;
    }

    @Override
    public void onSendToggleClick(Message message) {
        // do nothing
    }

    @Override
    public void onDeleteClick(ChannelType type, String targetId, EditText editText, int cursorPos) {
        // do nothing
    }

    @Override
    public void onDestroy(ChannelType type, String targetId) {
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (!stack.isEmpty()) {
            EditMessageState nextState = stack.peek();
            mNCExtension = nextState.mNCExtension;
            mFragment = nextState.mFragment;
        } else {
            NCExtensionManager.getInstance().removeExtensionEventWatcher(this);
            mNCExtension = null;
            mFragment = null;
        }
    }

    public void onPause() {
        updateCurrentEditConfig();
    }

    public void onResume() {
        updateCurrentEditConfig();
    }

    // Update in-memory edit config to SharedPreferences
    private void updateCurrentEditConfig() {
        if (!NCChatUIConfig.featureConfig().isEditMessageEnable()) {
            return;
        }
        initContext();
        if (stack.isEmpty()) {
            return;
        }
        if (mFragment == null || mNCExtension == null) {
            return;
        }
        NCExtension extension = mNCExtension.get();
        Fragment fragment = mFragment.get();
        if (extension == null || fragment == null) {
            return;
        }
        EditMessageState state = stack.peek();
        ChannelIdentifier id = extension.getChannelIdentifier();
        if (state != null && state.config != null && !TextUtils.isEmpty(state.config.content)) {
            editMessageHandler.saveEditedMessageDraft(id, state.config);
        } else {
            editMessageHandler.clearEditedMessageDraft(id);
        }
    }

    // Generated by Cursor
    private List<MentionBlock> getMentionBlocks(UiMessage uiMessage) {
        initContext();
        if (mFragment == null || mNCExtension == null) {
            return new ArrayList<>();
        }
        NCExtension extension = mNCExtension.get();
        Fragment fragment = mFragment.get();
        if (extension == null || fragment == null) {
            return new ArrayList<>();
        }
        return EditMessageUtils.getMentionBlocks(uiMessage);
    }

    // Whether to filter
    private boolean isFilter(UiMessage uiMessage) {
        initContext();
        if (mNCExtension == null || mFragment == null) {
            return false;
        }
        NCExtension extension = mNCExtension.get();
        Fragment fragment = mFragment.get();
        if (extension == null || fragment == null) {
            return false;
        }
        if (!NCChatUIConfig.featureConfig().isEditMessageEnable()) {
            return false;
        }
        // Filter failed messages
        Message message = uiMessage.getMessage();
        boolean hasUid = !TextUtils.isEmpty(message.getMessageId());
        boolean supportSentStatus =
                message.getSentStatus() != SentStatus.CANCELED
                        && message.getSentStatus() != SentStatus.FAILED
                        && message.getSentStatus() != SentStatus.SENDING;
        ai.nexconn.chat.channel.ChannelType channelType =
                message.getChannelIdentifier().getChannelType();
        boolean supportConversationType =
                channelType == ai.nexconn.chat.channel.ChannelType.DIRECT
                        || channelType == ai.nexconn.chat.channel.ChannelType.GROUP;
        boolean supportMsgType =
                (message.getContent() instanceof TextMessage)
                        || (message.getContent() instanceof ReferenceMessage);
        boolean supportDirection =
                message.getDirection() == ai.nexconn.chat.message.model.MessageDirection.SEND;
        boolean isFireMsg = message.getContent().isDestruct();
        boolean isFireMode =
                NCExtensionCacheHelper.isDestructMode(
                        extension.getContext(),
                        extension.getConversationType(),
                        extension.getTargetId());
        boolean messageSupport =
                supportSentStatus
                        && supportConversationType
                        && supportMsgType
                        && supportDirection
                        && hasUid
                        && !isFireMsg
                        && !isFireMode;
        if (!messageSupport) {
            return false;
        }
        return checkMessageModifiable(message.getSentTime());
    }

    public boolean canEditByLongClick(UiMessage uiMessage) {
        return isFilter(uiMessage);
    }

    public void addKeyboardHeightObserver(KeyboardHeightObserver observer) {
        mObservers.add(observer);
    }

    public void removeKeyboardHeightObserver(KeyboardHeightObserver observer) {
        mObservers.remove(observer);
    }

    private void showEditFailedDialog(int txtResId) {
        initContext();
        if (mFragment == null) {
            return;
        }
        Fragment fragment = mFragment.get();
        if (fragment == null
                || fragment.getActivity() == null
                || fragment.getActivity().isFinishing()) {
            return;
        }
        EditMessageFailedDialog dialog =
                EditMessageFailedDialog.newInstance(fragment.getActivity())
                        .setTitleText(R.string.nc_prompt)
                        .setContentMessage(txtResId)
                        .setButtonText(R.string.nc_dialog_ok);
        dialog.show();
    }

    private boolean checkMessageModifiable(long sentTime) {
        long intervalTime = System.currentTimeMillis() - sentTime;
        long windowMinutes = getMessageModifiableMinutes();
        return intervalTime < windowMinutes * 60 * 1000L;
    }

    private long getMessageModifiableMinutes() {
        int messageModifiableMinutes = NCEngine.getAppSettings().getMessageModifiableMinutes();
        return messageModifiableMinutes > 0
                ? messageModifiableMinutes
                : DEFAULT_MESSAGE_MODIFIABLE_MINUTES;
    }

    /**
     * Rebinds the MentionList for the EditText in NCMentionManager. MentionBlock: the starting
     * position of "@name" in the input field. Must be called after
     * NCExtensionViewModel#setEditTextWidget rebinds the EditText.
     */
    public void addMentionBlocks(EditText editText, List<MentionBlock> mentionBlocks) {
        draftHelper.addMentionBlocks(editText, mentionBlocks);
    }

    private static BaseChannel createChannel(ChannelIdentifier id) {
        if (id == null) return new DirectChannel("");
        if (id.getChannelType() == ChannelType.GROUP) return new GroupChannel(id.getChannelId());
        if (id.getChannelType() == ChannelType.OPEN) return new OpenChannel(id.getChannelId());
        if (id.getChannelType() == ChannelType.SYSTEM) return new SystemChannel(id.getChannelId());
        return new DirectChannel(id.getChannelId());
    }

    private void postDelayed(Runnable r) {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(r, 100);
    }

    public void addStatusListener(StatusListener listener) {
        if (listener != null && !this.mStatusListenerList.contains(listener)) {
            this.mStatusListenerList.add(listener);
        }
    }

    public void removeStatusListener(StatusListener listener) {
        if (listener != null) {
            this.mStatusListenerList.remove(listener);
        }
    }

    public enum ActiveType {
        // Long-press message.
        OnLongClickMessage,
        // onAttachedToExtension determines if saved edit info should be restored.
        OnAttachedToExtension,
        // Cancel multi-select status in message list, restore edit state.
        OnCancelMultiSelectStatus
    }

    public interface StatusListener {
        /** Callback when the edit message bar is shown or hidden. */
        void onVisibilityChanged(boolean isVisible);
    }
}
