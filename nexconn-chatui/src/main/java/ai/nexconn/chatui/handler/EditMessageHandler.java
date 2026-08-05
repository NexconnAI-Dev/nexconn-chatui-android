package ai.nexconn.chatui.handler;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.EditedMessageDraft;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.MessageHandler;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.handler.RefreshReferenceMessageHandler;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.ReferenceMessage;
import ai.nexconn.chat.message.TextMessage;
import ai.nexconn.chat.message.model.MessageUpdateInfo;
import ai.nexconn.chat.message.model.MessageUpdateStatus;
import ai.nexconn.chat.message.model.MessagesUpdatedEvent;
import ai.nexconn.chat.message.model.ReferenceMessageResult;
import ai.nexconn.chat.message.model.ReferenceMessageStatus;
import ai.nexconn.chat.message.model.UpdatedMessageSyncCompletedEvent;
import ai.nexconn.chat.params.RefreshReferenceMessageParams;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.channel.extension.NCExtensionCacheHelper;
import ai.nexconn.chatui.channel.feature.editmessage.EditMessageConfig;
import ai.nexconn.chatui.channel.feature.editmessage.EditMessageManager;
import ai.nexconn.chatui.channel.feature.editmessage.EditMessageUtils;
import ai.nexconn.chatui.channel.feature.reference.ReferenceManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.system.ExecutorHelper;
import android.content.Context;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Data handler for message editing operations.
 *
 * <p>Observes message-modification events from the SDK, processes edited messages and their
 * associated reference messages, and exposes data keys for observers to consume.
 *
 * @since 5.26.0
 */
public class EditMessageHandler extends MultiDataHandler {

    private static final String TAG = "EditMessageHandler";
    private static final String COLON_SEPARATOR = ":";

    public static final DataKey<List<Message>> KEY_ON_MESSAGE_MODIFIED =
            DataKey.obtain("KEY_ON_MESSAGE_MODIFIED", (Class<List<Message>>) (Class<?>) List.class);

    public static final DataKey<Message> KEY_ON_MESSAGE_REFRESH =
            DataKey.obtain("KEY_ON_MESSAGE_REFRESH", Message.class);

    public static final DataKey<EditMessageConfig> KEY_INPUT_PANEL_GET_DRAFT =
            DataKey.obtain("KEY_INPUT_PANEL_GET_DRAFT", EditMessageConfig.class);

    private final CopyOnWriteArraySet<String> runningMsgSet = new CopyOnWriteArraySet<>();

    private final MessageHandler mMessageHandler =
            new MessageHandler() {
                @Override
                public void onMessagesUpdated(MessagesUpdatedEvent event) {
                    List<Message> messages = event.getMessages();
                    if (messages != null && !messages.isEmpty()) {
                        notifyDataChange(KEY_ON_MESSAGE_MODIFIED, messages);
                    }
                }

                @Override
                public void onUpdatedMessageSyncCompleted(UpdatedMessageSyncCompletedEvent event) {
                    // sync completed — no action needed in UI handler
                }
            };

    private final String mHandlerId = "EditMessageHandler_" + hashCode();

    public EditMessageHandler() {
        super();
        NCEngine.addMessageHandler(mHandlerId, mMessageHandler);
    }

    @Override
    public void stop() {
        super.stop();
        NCEngine.removeMessageHandler(mHandlerId);
    }

    /**
     * Processes the edit status of messages and updates any reference messages that quote them.
     *
     * @param editMessageList list of edited messages
     * @param uiMessageList the current UI message list
     * @return updated UI message list for refreshing the screen
     */
    public List<UiMessage> processMessageEditStatusAndReferMsgStatus(
            List<Message> editMessageList, List<UiMessage> uiMessageList) {
        if (editMessageList == null
                || editMessageList.isEmpty()
                || uiMessageList == null
                || uiMessageList.isEmpty()) {
            return uiMessageList;
        }
        // Update quoted-message status for reference messages pointing to edited messages
        List<Message> result = new ArrayList<>();
        HashMap<String, Message> uIdMap = new HashMap<>();
        for (Message message : editMessageList) {
            uIdMap.put(message.getMessageId(), message);
        }
        for (UiMessage item : uiMessageList) {
            if (item.getMessage().getContent() instanceof ReferenceMessage) {
                ReferenceMessage referMsg = (ReferenceMessage) item.getMessage().getContent();
                Message msg = uIdMap.get(referMsg.getReferMsgId());
                if (msg != null) {
                    referMsg.setReferMsgStatus(ReferenceMessageStatus.MODIFIED);
                    MessageContent referenceContent = referMsg.getReferMsg();
                    MessageContent sourceContent = msg.getContent();
                    if (referenceContent instanceof TextMessage
                            && sourceContent instanceof TextMessage) {
                        String newContent = ((TextMessage) msg.getContent()).getText();
                        referMsg.setReferMsg(new TextMessage(newContent));
                    } else if (referenceContent instanceof ReferenceMessage
                            && sourceContent instanceof ReferenceMessage) {
                        String newContent = ((ReferenceMessage) msg.getContent()).getContent();
                        ((ReferenceMessage) referenceContent).setContent(newContent);
                    }
                    result.add(item.getMessage());
                }
            }
        }
        // Merge edited messages and their referencing messages into a single refresh pass
        if (!result.isEmpty()) {
            editMessageList.addAll(result);
        }
        return processMessageEditStatus(editMessageList, uiMessageList);
    }

    /**
     * Updates the reference-message status for messages that quote the given message.
     *
     * @param message the source message
     * @param status reference message status (recalled or deleted)
     * @param uiMessageList the current UI message list
     * @return updated UI message list for refreshing the screen
     */
    public List<UiMessage> processMessageReferMsgStatus(
            Message message, ReferenceMessageStatus status, List<UiMessage> uiMessageList) {
        if (message != null && !TextUtils.isEmpty(message.getMessageId())) {
            return processMessageReferMsgStatus(new Message[] {message}, status, uiMessageList);
        }
        return uiMessageList;
    }

    /**
     * Updates the reference-message status for messages that quote any of the given messages.
     *
     * @param messages the source messages
     * @param status reference message status (recalled or deleted)
     * @param uiMessageList the current UI message list
     * @return updated UI message list for refreshing the screen
     */
    public List<UiMessage> processMessageReferMsgStatus(
            Message[] messages, ReferenceMessageStatus status, List<UiMessage> uiMessageList) {
        if (uiMessageList == null || uiMessageList.isEmpty()) {
            return uiMessageList;
        }
        List<Message> result = new ArrayList<>();
        HashSet<String> uIdSet = new HashSet<>();
        for (Message message : messages) {
            uIdSet.add(message.getMessageId());
        }
        for (UiMessage item : uiMessageList) {
            if (item.getMessage().getContent() instanceof ReferenceMessage) {
                ReferenceMessage referMsg = (ReferenceMessage) item.getMessage().getContent();
                if (uIdSet.contains(referMsg.getReferMsgId())) {
                    referMsg.setReferMsgStatus(status);
                    result.add(item.getMessage());
                }
            }
        }
        return processMessageEditStatus(result, uiMessageList);
    }

    /**
     * Refreshes the edit status of in-memory UI messages from the given edited message list.
     *
     * @param editMessageList list of edited messages
     * @param uiMessageList the current in-memory UI message list
     */
    private List<UiMessage> processMessageEditStatus(
            List<Message> editMessageList, List<UiMessage> uiMessageList) {
        if (editMessageList == null
                || editMessageList.isEmpty()
                || uiMessageList == null
                || uiMessageList.isEmpty()) {
            return uiMessageList;
        }
        for (Message message : editMessageList) {
            if (message == null || TextUtils.isEmpty(message.getMessageId())) {
                continue;
            }
            UiMessage uiMessage = findUIMessage(uiMessageList, message.getMessageId());
            if (uiMessage == null) {
                continue;
            }
            // Preserve the in-memory reference status when both are ReferenceMessages
            if (uiMessage.getContent() instanceof ReferenceMessage
                    && message.getContent() instanceof ReferenceMessage) {
                ReferenceMessageStatus uiStatus =
                        ((ReferenceMessage) uiMessage.getContent()).getReferMsgStatus();
                ReferenceMessageStatus messageStatus =
                        ((ReferenceMessage) message.getContent()).getReferMsgStatus();
                // ReferenceMessageStatus is a forward-only state; keep the higher (more advanced)
                // value.
                if (uiStatus.getValue() > messageStatus.getValue()) {
                    ((ReferenceMessage) message.getContent()).setReferMsgStatus(uiStatus);
                }
            }
            uiMessage.setMessage(message);
            uiMessage.setContentSpannable(null);
            uiMessage.setReferenceContentSpannable(null);
            uiMessage.setChange(true);
        }
        return uiMessageList;
    }

    /**
     * Updates the reference view after receiving message-edit events.
     *
     * @param messages the edited messages
     * @param uiMessageList the current UI message list
     */
    public void updateReferenceView(List<Message> messages, List<UiMessage> uiMessageList) {
        // Reference view not showing — nothing to update
        UiMessage referMessage = ReferenceManager.getInstance().getUiMessage();
        if (referMessage == null) {
            return;
        }
        if (messages == null
                || messages.isEmpty()
                || uiMessageList == null
                || uiMessageList.isEmpty()) {
            return;
        }
        for (Message message : messages) {
            MessageUpdateInfo info = message.getUpdateInfo();
            if (info != null && MessageUpdateStatus.SUCCESS == info.getStatus()) {
                UiMessage uiMessage = findUIMessage(uiMessageList, message.getMessageId());
                if (uiMessage == null
                        || !TextUtils.equals(
                                referMessage.getMessageId(), uiMessage.getMessageId())) {
                    continue;
                }
                ReferenceManager.getInstance().showReferenceView(null, uiMessage);
            }
        }
    }

    private UiMessage findUIMessage(List<UiMessage> uiMessageList, String messageUId) {
        if (uiMessageList == null || uiMessageList.isEmpty()) {
            return null;
        }
        for (UiMessage item : uiMessageList) {
            if (messageUId.equals(item.getMessage().getMessageId())) {
                return item;
            }
        }
        return null;
    }

    /**
     * Refreshes the reference message status for the given message UID.
     *
     * @param editMsgUid message UID
     * @param identifier channel identifier
     */
    public void refreshReferenceMessage(String editMsgUid, ChannelIdentifier identifier) {
        if (runningMsgSet.contains(editMsgUid)) {
            return;
        }
        runningMsgSet.add(editMsgUid);
        RefreshReferenceMessageParams params =
                new RefreshReferenceMessageParams(
                        identifier, Collections.singletonList(editMsgUid));
        BaseChannel.refreshReferenceMessage(
                params,
                new RefreshReferenceMessageHandler() {
                    @Override
                    public void onLocalMessages(List<ReferenceMessageResult> results) {
                        onRefreshReferenceMessage(results, editMsgUid);
                    }

                    @Override
                    public void onRemoteMessages(List<ReferenceMessageResult> results) {
                        onRefreshReferenceMessage(results, editMsgUid);
                    }

                    @Override
                    public void onError(NCError error) {
                        runningMsgSet.remove(editMsgUid);
                    }
                });
    }

    private void onRefreshReferenceMessage(
            List<ReferenceMessageResult> results, String editMsgUid) {
        if (results == null || results.isEmpty()) {
            runningMsgSet.remove(editMsgUid);
            return;
        }
        ReferenceMessageResult first = results.get(0);
        Message msg = first.getMessage();
        if (msg != null) {
            ExecutorHelper.getInstance()
                    .mainThread()
                    .execute(() -> notifyDataChange(KEY_ON_MESSAGE_REFRESH, msg));
        }
        runningMsgSet.remove(editMsgUid);
    }

    /** Saves edit-message state to both local cache and the Chat SDK draft. */
    public void saveEditedMessageDraft(ChannelIdentifier id, EditMessageConfig config) {
        if (!NCChatUIConfig.featureConfig().isEditMessageEnable()) {
            return;
        }
        NCExtensionCacheHelper.setEditMessageConfig(
                NCChatUI.getContext(), id.getChannelType(), id.getChannelId(), config);
        EditedMessageDraft draft = EditMessageUtils.convertDraftString(config);
        if (draft != null) {
            NCChatUI.createChannel(id).setEditedMessageDraft(draft);
        }
    }

    /**
     * Checks edit-message draft status; called by InputPanel to decide whether to load a regular
     * draft.
     */
    public void checkEditedMessageDraftStatus(ChannelIdentifier id) {
        if (!isAlive()) {
            return;
        }
        if (!NCChatUIConfig.featureConfig().isEditMessageEnable()) {
            notifyDataChange(KEY_INPUT_PANEL_GET_DRAFT, null);
            return;
        }
        getEditedMessageDraft(
                id,
                new OperationHandler<EditMessageConfig>() {
                    @Override
                    public void onResult(EditMessageConfig config, NCError error) {
                        if (!isAlive()) {
                            return;
                        }
                        if (error != null) {
                            notifyDataChange(KEY_INPUT_PANEL_GET_DRAFT, null);
                        } else {
                            notifyDataChange(KEY_INPUT_PANEL_GET_DRAFT, config);
                        }
                    }
                });
    }

    /** Resumes the edit-message input UI. */
    public void resumeEditMode(ChannelIdentifier id, EditMessageManager.ActiveType type) {
        if (!isAlive()) {
            return;
        }
        if (!NCChatUIConfig.featureConfig().isEditMessageEnable()) {
            EditMessageManager.getInstance().onResumeEditModeResult(type, null);
            return;
        }
        getEditedMessageDraft(
                id,
                new OperationHandler<EditMessageConfig>() {
                    @Override
                    public void onResult(EditMessageConfig config, NCError error) {
                        if (!isAlive()) {
                            return;
                        }
                        if (error != null) {
                            EditMessageManager.getInstance().onResumeEditModeResult(type, null);
                        } else {
                            EditMessageManager.getInstance().onResumeEditModeResult(type, config);
                        }
                    }
                });
    }

    /** Retrieves edit draft: local cache first, then BaseChannel.editedMessageDraft. */
    private void getEditedMessageDraft(
            ChannelIdentifier id, OperationHandler<EditMessageConfig> callback) {
        EditMessageConfig editMessageConfig =
                NCExtensionCacheHelper.getEditMessageConfig(
                        NCChatUI.getContext(), id.getChannelType(), id.getChannelId());
        if (editMessageConfig != null) {
            callback.onResult(editMessageConfig, null);
            return;
        }
        EditedMessageDraft draft = NCChatUI.createChannel(id).getEditedMessageDraft();
        if (draft != null) {
            EditMessageConfig fromDraft = EditMessageUtils.convertEditMessageConfig(draft);
            callback.onResult(fromDraft, null);
            return;
        }
        callback.onResult(null, null);
    }

    /** Refreshes the reference content inside an edit-message draft. */
    private void refreshEditedDraftReferenceMessage(
            ChannelIdentifier id,
            EditMessageConfig config,
            OperationHandler<EditMessageConfig> callback) {
        if (config == null || TextUtils.isEmpty(config.referUid)) {
            RLog.d(TAG, "refreshEditedDraftReferenceMessage valid ");
            callback.onResult(config, null);
            return;
        }
        RLog.d(TAG, "refreshEditedDraftReferenceMessage config " + config);
        if (ReferenceMessageStatus.DELETED == config.referStatus
                || ReferenceMessageStatus.RECALLED == config.referStatus) {
            int index = config.referContent.indexOf(COLON_SEPARATOR);
            if (index > 0) {
                String name = config.referContent.substring(0, index);
                Context context = NCChatUI.getContext();
                if (ReferenceMessageStatus.DELETED == config.referStatus) {
                    String newContent = context.getString(R.string.nc_reference_status_delete);
                    config.referContent = name + COLON_SEPARATOR + newContent;
                } else if (ReferenceMessageStatus.RECALLED == config.referStatus) {
                    String newContent = context.getString(R.string.nc_reference_status_recall);
                    config.referContent = name + COLON_SEPARATOR + newContent;
                }
            }
            callback.onResult(config, null);
            return;
        }
        callback.onResult(config, null);
    }

    /** Clears edit draft from both local cache and BaseChannel.editedMessageDraft. */
    public void clearEditedMessageDraft(ChannelIdentifier id) {
        if (!NCChatUIConfig.featureConfig().isEditMessageEnable()) {
            return;
        }
        NCExtensionCacheHelper.clearEditMessageConfig(
                NCChatUI.getContext(), id.getChannelType(), id.getChannelId());
        NCChatUI.createChannel(id).setEditedMessageDraft(null);
    }
}
