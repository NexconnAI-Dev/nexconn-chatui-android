package ai.nexconn.chatui.channel.feature.editmessage;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.EditedMessageDraft;
import ai.nexconn.chat.message.FileMessage;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.ReferenceMessage;
import ai.nexconn.chat.message.TextMessage;
import ai.nexconn.chat.message.model.MentionedInfo;
import ai.nexconn.chat.message.model.MentionedType;
import ai.nexconn.chat.message.model.ReferenceMessageStatus;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.feature.mention.MentionBlock;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.userinfo.NCUserInfoManager;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.text.StringUtils;
import android.content.Context;
import android.text.Spannable;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EditMessageUtils {

    private static final String TAG = "EditMessageUtils";

    public static boolean isBlankEditContent(CharSequence content) {
        if (content == null || content.length() == 0) {
            return true;
        }
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (!Character.isWhitespace(c) && !Character.isSpaceChar(c)) {
                return false;
            }
        }
        return true;
    }

    public static String getDisplayName(Message message) {
        if (message == null || TextUtils.isEmpty(message.getSenderUserId())) {
            return "";
        }
        String sendId = "";
        MessageContent content = null;
        if (message.getContent() instanceof ReferenceMessage) {
            ReferenceMessage reference = (ReferenceMessage) message.getContent();
            sendId = reference.getReferMsgSenderId();
            content = reference;
        } else if (message.getContent() instanceof TextMessage) {
            sendId = message.getSenderUserId();
            content = message.getContent();
        }
        if (content == null) {
            return "";
        }
        UserInfo userInfo = getUserInfo(sendId, content);
        String groupMemberName = "";
        if (message.getChannelIdentifier().getChannelType() == ChannelType.GROUP) {
            GroupUserInfo groupUserInfo =
                    NCUserInfoManager.getInstance()
                            .getGroupUserInfo(
                                    message.getChannelIdentifier().getChannelId(), sendId);
            groupMemberName = groupUserInfo != null ? groupUserInfo.getNickname() : "";
        }
        return NCUserInfoManager.getInstance().getUserDisplayName(userInfo, groupMemberName);
    }

    private static UserInfo getUserInfo(String userId, MessageContent messageContent) {
        boolean isInfoManagement =
                NCUserInfoManager.getInstance().getDataSourceType()
                        == NCUserInfoManager.DataSourceType.INFO_MANAGEMENT;
        if (isInfoManagement
                && messageContent != null
                && messageContent.getSenderUserInfo() != null
                && messageContent.getSenderUserInfo().getUserId() != null
                && messageContent.getSenderUserInfo().getUserId().equals(userId)) {
            return messageContent.getSenderUserInfo();
        }
        return NCUserInfoManager.getInstance().getUserInfo(userId);
    }

    static List<MentionBlock> getMentionBlocks(UiMessage uiMessage) {
        String content = "";
        if (uiMessage.getContent() instanceof TextMessage) {
            content = ((TextMessage) uiMessage.getContent()).getText();
        } else if (uiMessage.getContent() instanceof ReferenceMessage) {
            content = ((ReferenceMessage) uiMessage.getContent()).getContent();
        }
        if (TextUtils.isEmpty(content)) {
            return new ArrayList<>();
        }

        MentionedInfo mentionedInfo = uiMessage.getContent().getMentionedInfo();
        if (mentionedInfo == null) {
            return new ArrayList<>();
        }
        List<String> mentionedUserIdList = new ArrayList<>();
        if (mentionedInfo.getUserIdList() != null && !mentionedInfo.getUserIdList().isEmpty()) {
            mentionedUserIdList.addAll(mentionedInfo.getUserIdList());
        }
        if (mentionedInfo.getType() == MentionedType.ALL) {
            boolean hasAll = false;
            if (!mentionedUserIdList.isEmpty()) {
                for (String uid : mentionedUserIdList) {
                    if (TextUtils.equals(uid, "-1")) {
                        hasAll = true;
                    }
                }
            }
            if (!hasAll) {
                mentionedUserIdList.add("-1");
            }
        }
        if (mentionedUserIdList.isEmpty()) {
            return new ArrayList<>();
        }
        String targetId = uiMessage.getChannelId();
        ChannelType type = uiMessage.getChannelType();
        List<MentionBlock> mentionBlocks = new ArrayList<>();

        // Track processed ranges to avoid duplicate matching - using arrays to store [start, end]
        List<int[]> processedRanges = new ArrayList<>();

        for (String uid : mentionedUserIdList) {
            // Get the username for @mention
            String mentionUserName = EditMessageUtils.getMentionUserName(type, targetId, uid);
            // Check if username is empty
            if (TextUtils.isEmpty(mentionUserName)) {
                RLog.e(TAG, "Empty user name for uid: " + uid);
                continue;
            }

            String searchPattern = "@" + mentionUserName + " ";
            int searchIndex = 0;

            // Find all matching positions
            while (searchIndex < content.length()) {
                int foundIndex = content.indexOf(searchPattern, searchIndex);
                if (foundIndex == -1) {
                    break; // No more matches found
                }

                int foundEnd = foundIndex + searchPattern.length();

                // Check if this position has already been processed (range overlap check)
                boolean alreadyProcessed = false;
                for (int[] processedRange : processedRanges) {
                    int processedStart = processedRange[0];
                    int processedEnd = processedRange[1];
                    // Check if two ranges overlap: foundIndex < processedEnd && foundEnd >
                    // processedStart
                    if (foundIndex < processedEnd && foundEnd > processedStart) {
                        alreadyProcessed = true;
                        break;
                    }
                }

                if (!alreadyProcessed) {
                    // Create new MentionBlock
                    MentionBlock block = new MentionBlock();
                    block.userId = uid;
                    block.name = mentionUserName;
                    block.offset = true;
                    block.start = foundIndex;
                    block.end = foundEnd;
                    mentionBlocks.add(block);

                    // Record the processed range
                    processedRanges.add(new int[] {foundIndex, foundEnd});
                }

                // Move to next search position to avoid duplicate matching
                searchIndex = foundIndex + searchPattern.length();
            }
        }

        return mentionBlocks;
    }

    // Returns the name corresponding to the userID in Mention; returns empty for @all.
    private static String getMentionUserName(ChannelType type, String targetId, String uid) {
        if (type == ChannelType.GROUP) {
            if (TextUtils.equals(uid, "-1")) {
                return "";
            }
            GroupUserInfo groupUserInfo =
                    NCUserInfoManager.getInstance().getGroupUserInfo(targetId, uid);
            if (groupUserInfo != null && !TextUtils.isEmpty(groupUserInfo.getNickname())) {
                return groupUserInfo.getNickname();
            }
        }
        UserInfo userInfo = NCUserInfoManager.getInstance().getUserInfo(uid);
        if (userInfo == null
                || TextUtils.isEmpty(userInfo.getUserId())
                || userInfo.getName() == null) {
            return "";
        }
        return userInfo.getName();
    }

    public static String getReferContent(Message message) {
        Context context = NCChatUI.getContext();
        if (context == null
                || message == null
                || !(message.getContent() instanceof ReferenceMessage)) {
            return "";
        }
        ReferenceMessage referenceMessage = (ReferenceMessage) message.getContent();
        ReferenceMessageStatus status = referenceMessage.getReferMsgStatus();
        if (ReferenceMessageStatus.DELETED == status) {
            return context.getString(R.string.nc_reference_status_delete);
        } else if (ReferenceMessageStatus.RECALLED == status) {
            return context.getString(R.string.nc_reference_status_recall);
        }
        MessageContent content = referenceMessage.getReferMsg();
        Spannable messageSummary =
                NCChatUIConfig.channelConfig().getMessageSummary(context, content);
        if (content instanceof FileMessage) {
            return messageSummary.toString();
        } else {
            return StringUtils.getStringNoBlank(messageSummary.toString());
        }
    }

    static String getOriginalContent(Message message) {
        if (message.getContent() instanceof TextMessage) {
            return ((TextMessage) message.getContent()).getText();
        }
        if (message.getContent() instanceof ReferenceMessage) {
            return ((ReferenceMessage) message.getContent()).getContent();
        }
        return "";
    }

    static String getReferUid(Message message) {
        if (message == null || !(message.getContent() instanceof ReferenceMessage)) {
            return "";
        }
        return ((ReferenceMessage) message.getContent()).getReferMsgId();
    }

    /**
     * Serializes {@link EditMessageConfig} to {@link EditedMessageDraft} for persistence via {@link
     * ai.nexconn.chat.channel.BaseChannel#editedMessageDraft}.
     */
    public static EditedMessageDraft convertDraftString(EditMessageConfig config) {
        if (config == null || TextUtils.isEmpty(config.uid)) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("content", config.content);
            jsonObject.put(
                    "referContent",
                    TextUtils.isEmpty(config.referContent) ? "" : config.referContent);
            jsonObject.put("referUid", config.referUid);
            jsonObject.put("sentTime", config.sentTime);
            jsonObject.put(
                    "referStatus", config.referStatus != null ? config.referStatus.getValue() : 0);
            if (config.mentionBlocks != null && !config.mentionBlocks.isEmpty()) {
                JSONArray jsonArray = new JSONArray();
                for (MentionBlock mentionBlock : config.mentionBlocks) {
                    jsonArray.put(mentionBlock.toJson());
                }
                jsonObject.put("mentionBlocks", jsonArray);
            }
        } catch (JSONException e) {
            RLog.e(TAG, "convertDraftString: " + e);
            return null;
        }
        return new EditedMessageDraft(config.uid, jsonObject.toString());
    }

    /**
     * Deserializes {@link EditedMessageDraft} to {@link EditMessageConfig} for restoring edit
     * message UI state.
     */
    public static EditMessageConfig convertEditMessageConfig(EditedMessageDraft draft) {
        if (draft == null
                || TextUtils.isEmpty(draft.getMessageId())
                || TextUtils.isEmpty(draft.getContent())) {
            return null;
        }
        EditMessageConfig config = new EditMessageConfig();
        config.uid = draft.getMessageId();
        try {
            JSONObject json = new JSONObject(draft.getContent());
            config.content = json.optString("content", "");
            if (TextUtils.isEmpty(config.uid) || TextUtils.isEmpty(config.content)) {
                return null;
            }
            config.sentTime = json.optLong("sentTime", 0);
            config.referContent = json.optString("referContent", "");
            config.referUid = json.optString("referUid", "");
            int statusValue = json.optInt("referStatus", 0);
            config.referStatus = ReferenceMessageStatus.fromValue(statusValue);
            JSONArray mentionBlocksJson = json.optJSONArray("mentionBlocks");
            if (mentionBlocksJson != null) {
                List<MentionBlock> mentionBlocksList = new ArrayList<>();
                for (int i = 0; i < mentionBlocksJson.length(); i++) {
                    MentionBlock block = new MentionBlock(mentionBlocksJson.optString(i));
                    mentionBlocksList.add(block);
                }
                config.mentionBlocks = mentionBlocksList;
            }
            return config;
        } catch (Exception e) {
            RLog.e(TAG, "convertEditMessageConfig: " + e);
        }
        return null;
    }
}
