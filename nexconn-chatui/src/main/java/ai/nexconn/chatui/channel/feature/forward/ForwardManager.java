package ai.nexconn.chatui.channel.feature.forward;

import static android.app.Activity.RESULT_OK;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.message.CombineMessage;
import ai.nexconn.chat.message.FileMessage;
import ai.nexconn.chat.message.GIFMessage;
import ai.nexconn.chat.message.HDVoiceMessage;
import ai.nexconn.chat.message.ImageMessage;
import ai.nexconn.chat.message.MediaMessageContent;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.ReferenceMessage;
import ai.nexconn.chat.message.ShortVideoMessage;
import ai.nexconn.chat.message.TextMessage;
import ai.nexconn.chat.message.model.MessageType;
import ai.nexconn.chat.message.model.SentStatus;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.userinfo.NCUserInfoManager;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.system.ExecutorHelper;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

public class ForwardManager {
    private static final String TAG = ForwardManager.class.getSimpleName();

    // Message send interval
    private static final int TIME_DELAY = 400;

    private ForwardManager() {
        // default implementation ignored
    }

    public static ForwardManager getInstance() {
        return SingletonHolder.sInstance;
    }

    /**
     * Sets the forward conversation selection result. Can be called from a custom forward contact
     * selection UI to initiate combined forwarding.
     *
     * @param activity the contact selection activity
     * @param conversations conversation types, only direct and group channels are supported
     */
    public static void setForwardMessageResult(
            Activity activity, ArrayList<ChannelIdentifier> conversations) {
        Intent intent = activity.getIntent();
        intent.putParcelableArrayListExtra("conversations", conversations);
        activity.setResult(RESULT_OK, intent);
        activity.finish();
    }

    public static List<Message> filterMessagesList(
            Context context, List<Message> messages, int index) {
        List<Message> forwardMessagesList = new ArrayList<>();
        if (context == null) {
            RLog.e(TAG, "filterMessagesList context is null");
            return forwardMessagesList;
        }

        for (Message message : messages) {
            if (!allowForward(message, index)) {
                String msg = context.getString(R.string.nc_combine_unsupported);
                if (index == 0) {
                    msg = context.getString(R.string.nc_combine_unsupported_step);
                }
                new AlertDialog.Builder(context)
                        .setMessage(msg)
                        .setPositiveButton(context.getString(R.string.nc_dialog_ok), null)
                        .show();
                forwardMessagesList.clear();
                return forwardMessagesList;
            }
            forwardMessagesList.add(message);
        }

        // Sort by message sent time
        Collections.sort(
                forwardMessagesList,
                new Comparator<Message>() {
                    @Override
                    public int compare(Message o1, Message o2) {
                        return (int) (o1.getSentTime() - o2.getSentTime());
                    }
                });
        return forwardMessagesList;
    }

    // index: 0 = one-by-one forward, 1 = combined forward
    private static boolean allowForward(Message message, int index) {
        if (message == null) {
            RLog.d(TAG, "Forwarding is not allowed, message is null");
            return false;
        }

        if (message.getSentStatus() == SentStatus.SENDING
                || message.getSentStatus() == SentStatus.FAILED
                || message.getSentStatus() == SentStatus.CANCELED) {
            RLog.d(TAG, "Forwarding is not allowed, status:" + message.getSentStatus());
            return false;
        }

        MessageContent messageContent = message.getContent();
        if (messageContent == null) {
            RLog.d(TAG, "Forwarding is not allowed, message:" + message);
            return false;
        }

        if (messageContent.isDestruct()) {
            RLog.d(TAG, "Destruct message not allow forward");
            return false;
        }

        String objectName = message.getMessageType();
        if (objectName == null) {
            RLog.d(TAG, "Forwarding is not allowed, objectName is null");
            return false;
        }

        boolean allow =
                index == 1 ? allowForwardForCombine(objectName) : allowForwardForStep(objectName);

        RLog.d(TAG, "Forwarding allowed:" + allow + ", type:" + objectName);
        return allow;
    }

    // Message types allowed for one-by-one forward.
    private static boolean allowForwardForStep(String tag) {
        switch (tag) {
            case MessageType.TEXT:
            case MessageType.HD_VOICE:
            case MessageType.SHORT_VIDEO:
            case MessageType.IMAGE:
            case MessageType.GIF:
            case MessageType.FILE:
            case MessageType.LOCATION:
            case MessageType.COMBINE:
            case MessageType.REFERENCE:
                return true;
            default:
                return false;
        }
    }

    // Message types allowed for combined forward. Reference messages are explicitly excluded.
    private static boolean allowForwardForCombine(String tag) {
        switch (tag) {
            case MessageType.TEXT:
            case MessageType.HD_VOICE:
            case MessageType.SHORT_VIDEO:
            case MessageType.IMAGE:
            case MessageType.GIF:
            case MessageType.FILE:
            case MessageType.LOCATION:
            case MessageType.COMBINE:
                return true;
            default:
                return false;
        }
    }

    public void forwardMessages(
            final int index,
            final List<ChannelIdentifier> conversations,
            final List<Integer> messageIds,
            List<Message> messages) {
        if (conversations == null || conversations.isEmpty()) {
            RLog.e(TAG, "forwardMessages conversations empty");
            return;
        }
        if (messages == null || messages.isEmpty()) {
            RLog.e(TAG, "forwardMessages source messages empty");
            return;
        }
        final List<Message> forwardMessages = new ArrayList<>();
        if (messageIds != null && !messageIds.isEmpty()) {
            for (Integer messageId : messageIds) {
                if (messageId == null) {
                    continue;
                }
                for (Message msg : messages) {
                    if (messageId == msg.getClientId()) {
                        forwardMessages.add(msg);
                    }
                }
            }
        }
        // Fallback to source selection to avoid silent no-op when id mapping misses.
        if (forwardMessages.isEmpty()) {
            forwardMessages.addAll(messages);
        }
        forwardMessages(index, conversations, forwardMessages);
    }

    private void forwardMessages(
            final int index,
            final List<ChannelIdentifier> conversations,
            final List<Message> messages) {
        ExecutorHelper.getInstance()
                .networkIO()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (index == 1) {
                                    forwardMessageByCombine(conversations, messages);
                                } else {
                                    forwardMessageByStep(conversations, messages);
                                }
                            }
                        });
    }

    // Combined forward
    private void forwardMessageByCombine(
            List<ChannelIdentifier> conversations, List<Message> messages) {
        Context context = NCChatUI.getContext();
        if (conversations == null || conversations.isEmpty()) {
            RLog.e(TAG, "forwardMessageByCombine conversations empty");
            showErrorToast(context, R.string.nc_forward_no_target_selected);
            return;
        }
        if (messages.isEmpty()) {
            RLog.e(TAG, "forwardMessageByCombine messages empty");
            showErrorToast(context, R.string.nc_forward_no_message_selected);
            return;
        }
        for (Message message : messages) {
            if (!allowForward(message, 1)) {
                RLog.d(TAG, "forwardMessageByCombine blocked unsupported message type");
                showErrorToast(context, R.string.nc_combine_unsupported);
                return;
            }
        }
        if (context == null) {
            RLog.e(TAG, "forwardMessageByCombine context is null");
            return;
        }
        // Get source channel type (based on the first message)
        ChannelType sourceChannelType = messages.get(0).getChannelIdentifier().getChannelType();

        // Build nameList (deduplicate, preserve order)
        LinkedHashSet<String> nameSet = new LinkedHashSet<>();
        for (Message msg : messages) {
            String senderId = msg.getSenderUserId();
            if (TextUtils.isEmpty(senderId)) continue;
            ai.nexconn.chat.user.model.UserInfo userInfo =
                    NCUserInfoManager.getInstance().getUserInfo(senderId);
            String displayName =
                    userInfo != null
                            ? NCUserInfoManager.getInstance().getUserDisplayName(userInfo)
                            : senderId;
            nameSet.add(displayName);
        }
        List<String> nameList = new ArrayList<>(nameSet);

        // Build summaryList (one summary per message)
        List<String> summaryList = new ArrayList<>();
        for (Message msg : messages) {
            String senderName = "";
            String senderId = msg.getSenderUserId();
            if (!TextUtils.isEmpty(senderId)) {
                ai.nexconn.chat.user.model.UserInfo userInfo =
                        NCUserInfoManager.getInstance().getUserInfo(senderId);
                senderName =
                        userInfo != null
                                ? NCUserInfoManager.getInstance().getUserDisplayName(userInfo)
                                : senderId;
            }
            String contentSummary = getContentSummary(context, msg.getContent());
            summaryList.add(senderName + "：" + contentSummary);
        }

        // Build CombineMessage using nexconn-chat SDK factory method
        CombineMessage combineMessage =
                CombineMessage.obtain(context, messages, nameList, summaryList, sourceChannelType);
        if (combineMessage == null) {
            RLog.e(TAG, "forwardMessageByCombine build CombineMessage failed");
            return;
        }

        // Send combined forward message to each target conversation
        for (ChannelIdentifier conversation : conversations) {
            NCChatUI.sendMediaMessage(
                    conversation,
                    new ai.nexconn.chat.params.SendMediaMessageParams(combineMessage),
                    null);
        }
    }

    private String getContentSummary(Context context, MessageContent content) {
        if (content == null) return "";
        if (content instanceof TextMessage) {
            String text = ((TextMessage) content).getText();
            return text != null ? text : "";
        } else if (content instanceof ImageMessage || content instanceof GIFMessage) {
            return context.getString(R.string.nc_message_content_image);
        } else if (content instanceof HDVoiceMessage) {
            return context.getString(R.string.nc_message_content_voice);
        } else if (content instanceof ShortVideoMessage) {
            return context.getString(R.string.nc_message_content_sight);
        } else if (content instanceof FileMessage) {
            String fileName = ((FileMessage) content).getName();
            if (!TextUtils.isEmpty(fileName)) {
                return context.getString(R.string.nc_message_content_file) + fileName;
            }
            return context.getString(R.string.nc_message_content_file);
        } else if (content instanceof CombineMessage) {
            return context.getString(R.string.nc_combine_chat_record);
        } else if (content instanceof ReferenceMessage) {
            ReferenceMessage ref = (ReferenceMessage) content;
            return ref.getContent() != null ? ref.getContent() : "";
        }
        return "";
    }

    // One-by-one forward
    private void forwardMessageByStep(
            List<ChannelIdentifier> conversations, final List<Message> messages) {
        Context context = NCChatUI.getContext();
        if (conversations == null || conversations.isEmpty()) {
            RLog.e(TAG, "forwardMessageByStep conversations empty");
            showErrorToast(context, R.string.nc_forward_no_target_selected);
            return;
        }
        if (messages.isEmpty()) {
            RLog.e(TAG, "forwardMessageByStep messages empty");
            showErrorToast(context, R.string.nc_forward_no_message_selected);
            return;
        }
        for (ChannelIdentifier conversation : conversations) {
            for (Message msg : messages) {
                startForwardMessageByStep(
                        conversation.getChannelId(), conversation.getChannelType(), msg);
                try {
                    Thread.sleep(TIME_DELAY);
                } catch (InterruptedException e) {
                    RLog.e(TAG, "forwardMessageByStep e:" + e.toString());
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void startForwardMessageByStep(String id, ChannelType type, Message fwdMessage) {
        MessageContent messageContent = fwdMessage.getContent();
        if (messageContent == null) return;
        messageContent.setSenderUserInfo(null);
        messageContent.setMentionedInfo(null);
        ai.nexconn.chat.channel.model.ChannelIdentifier channelId =
                new ai.nexconn.chat.channel.model.ChannelIdentifier(type, id);
        if (messageContent instanceof ImageMessage) {
            ImageMessage imageMessage = (ImageMessage) messageContent;
            if (hasUploadedRemoteUrl(imageMessage.getRemoteUrl())) {
                ai.nexconn.chatui.NCChatUI.sendMessage(
                        channelId,
                        new ai.nexconn.chat.params.SendMessageParams(messageContent),
                        null);
            } else {
                ai.nexconn.chatui.NCChatUI.sendMediaMessage(
                        channelId,
                        new ai.nexconn.chat.params.SendMediaMessageParams(
                                (MediaMessageContent) messageContent),
                        null);
            }
        } else if (messageContent instanceof ReferenceMessage) {
            ai.nexconn.chatui.NCChatUI.sendMessage(
                    channelId, new ai.nexconn.chat.params.SendMessageParams(messageContent), null);
        } else if (messageContent instanceof MediaMessageContent) {
            MediaMessageContent mediaMessageContent = (MediaMessageContent) messageContent;
            if (hasUploadedRemoteUrl(mediaMessageContent.getRemoteUrl())) {
                ai.nexconn.chatui.NCChatUI.sendMessage(
                        channelId,
                        new ai.nexconn.chat.params.SendMessageParams(messageContent),
                        null);
            } else {
                ai.nexconn.chatui.NCChatUI.sendMediaMessage(
                        channelId,
                        new ai.nexconn.chat.params.SendMediaMessageParams(mediaMessageContent),
                        null);
            }
        } else {
            ai.nexconn.chatui.NCChatUI.sendMessage(
                    channelId, new ai.nexconn.chat.params.SendMessageParams(messageContent), null);
        }
    }

    private boolean hasUploadedRemoteUrl(String remoteUrl) {
        if (TextUtils.isEmpty(remoteUrl)) {
            return false;
        }
        return !(remoteUrl.startsWith("file://") || remoteUrl.startsWith("content://"));
    }

    private void showErrorToast(final Context context, final int messageResId) {
        if (context == null) return;
        ExecutorHelper.getInstance()
                .mainThread()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                android.widget.Toast.makeText(
                                                context,
                                                context.getString(messageResId),
                                                android.widget.Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
    }

    private static class SingletonHolder {
        static ForwardManager sInstance = new ForwardManager();
    }
}
