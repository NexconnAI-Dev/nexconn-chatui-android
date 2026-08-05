package ai.nexconn.chatui.channel.feature.mention;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.model.MentionedInfo;
import ai.nexconn.chat.message.model.MentionedType;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.userinfo.NCUserInfoManager;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;
import ai.nexconn.chatui.usermanage.group.mention.GroupMentionActivity;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.route.RouteUtils;
import ai.nexconn.chatui.utils.view.RTLUtils;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import org.json.JSONArray;

public class NCMentionManager {
    public static final String MENTION_ALL_USER_ID = "ALL";
    private static String TAG = "NCMentionManager";
    private Stack<MentionInstance> stack = new Stack<>();
    private IGroupMembersProvider mGroupMembersProvider;
    private IMentionedInputListener mMentionedInputListener;
    private IAddMentionedMemberListener mAddMentionedMemberListener;

    private static class SingletonHolder {
        static NCMentionManager sInstance = new NCMentionManager();
    }

    private NCMentionManager() {
        // default implementation ignored
    }

    public static NCMentionManager getInstance() {
        return SingletonHolder.sInstance;
    }

    public void createInstance(ChannelType conversationType, String targetId, EditText editText) {
        RLog.i(TAG, "createInstance");
        if (!NCChatUIConfig.channelConfig().NC_enable_mentioned_message) {
            RLog.e(TAG, "NC_enable_mentioned_message is disable");
            return;
        }
        for (int i = 0; i < stack.size(); i++) {
            MentionInstance item = stack.get(i);
            if (item.inputEditText.equals(editText)) {
                return;
            }
        }
        MentionInstance mentionInstance = new MentionInstance();
        mentionInstance.conversationType = conversationType;
        mentionInstance.targetId = targetId;
        mentionInstance.inputEditText = editText;
        mentionInstance.mentionBlocks = new ArrayList<>();
        stack.add(mentionInstance);
    }

    public void destroyInstance(ChannelType conversationType, String targetId, EditText editText) {
        RLog.i(TAG, "destroyInstance");
        if (!NCChatUIConfig.channelConfig().NC_enable_mentioned_message) {
            RLog.e(TAG, "NC_enable_mentioned_message is disable");
            return;
        }
        for (int i = 0; i < stack.size(); i++) {
            MentionInstance item = stack.get(i);
            if (item.inputEditText.equals(editText)) {
                stack.remove(i);
                return;
            }
        }
    }

    public void setInputEditText(ChannelIdentifier id, EditText editText) {
        if (ChannelType.GROUP == id.getChannelType()
                || ChannelType.COMMUNITY == id.getChannelType()) {
            MentionInstance mentionInstance = stack.get(0);
            mentionInstance.inputEditText = editText;
        }
    }

    public void mentionMember(ChannelType conversationType, String targetId, String userId) {
        RLog.d(TAG, "mentionMember " + userId);
        if (TextUtils.isEmpty(userId)
                || conversationType == null
                || TextUtils.isEmpty(targetId)
                || stack.isEmpty()) {
            RLog.e(TAG, "Illegal argument");
            return;
        }
        MentionInstance mentionInstance = stack.get(0);
        if (!conversationType.equals(mentionInstance.conversationType)
                || !targetId.equals(mentionInstance.targetId)) {
            RLog.e(TAG, "Invalid conversationType or targetId");
            return;
        }
        UserInfo userInfo = NCUserInfoManager.getInstance().getUserInfo(userId);

        if (userInfo == null || TextUtils.isEmpty(userInfo.getUserId())) {
            RLog.e(TAG, "Invalid userInfo");
            return;
        }

        if (conversationType == ChannelType.GROUP) {
            GroupUserInfo groupUserInfo =
                    NCUserInfoManager.getInstance().getGroupUserInfo(targetId, userId);
            if (groupUserInfo != null && !TextUtils.isEmpty(groupUserInfo.getNickname())) {
                userInfo.setName(groupUserInfo.getNickname());
            }
        }
        addMentionedMember(userInfo, 0);
    }

    public void mentionMember(UserInfo userInfo) {
        if (userInfo == null || TextUtils.isEmpty(userInfo.getUserId())) {
            RLog.e(TAG, "Invalid userInfo");
            return;
        }
        addMentionedMember(userInfo, 1);
    }

    public String getMentionBlockInfo() {
        if (!stack.isEmpty()) {
            MentionInstance mentionInstance = stack.peek();
            if (mentionInstance.mentionBlocks != null && !mentionInstance.mentionBlocks.isEmpty()) {
                JSONArray jsonArray = new JSONArray();
                for (MentionBlock mentionBlock : mentionInstance.mentionBlocks) {
                    jsonArray.put(mentionBlock.toJson());
                }
                return jsonArray.toString();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    void addMentionBlock(MentionBlock mentionBlock) {
        if (!stack.isEmpty()) {
            MentionInstance mentionInstance = stack.peek();
            mentionInstance.mentionBlocks.add(mentionBlock);
        }
    }

    public MentionInstance obtainMentionInstance(EditText editText) {
        if (!stack.isEmpty()) {
            for (int i = 0; i < stack.size(); i++) {
                MentionInstance item = stack.get(i);
                if (item.inputEditText.equals(editText)) {
                    return item;
                }
            }
        }
        return null;
    }

    /**
     * @param userInfo user info
     * @param from 0 means from the conversation page, 1 means from the group member selection page.
     */
    private void addMentionedMember(UserInfo userInfo, int from) {
        if (!stack.isEmpty()) {
            MentionInstance mentionInstance = stack.peek();
            EditText editText = mentionInstance.inputEditText;
            if (userInfo != null && editText != null) {

                String mentionContent;
                if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                        == View.LAYOUT_DIRECTION_RTL) {
                    // RTL layout handling
                    // @ character and name need to be added to EditText together; delete @ first
                    // then re-add for convenience;
                    if (from == 1) deleteLastChar(editText);
                    // Add "\u200e"(LRM) or "\u200f"(RLM) before @ character to indicate direction,
                    // otherwise mixed text in RTL has issues
                    String str = "@" + userInfo.getName() + " ";
                    mentionContent = RTLUtils.adapterAitInRTL(str);
                } else {
                    mentionContent =
                            from == 0 ? "@" + userInfo.getName() + " " : userInfo.getName() + " ";
                }
                int len = mentionContent.length();
                int cursorPos = editText.getSelectionStart();

                MentionBlock brokenBlock =
                        getBrokenMentionedBlock(cursorPos, mentionInstance.mentionBlocks);
                if (brokenBlock != null) {
                    mentionInstance.mentionBlocks.remove(brokenBlock);
                }

                MentionBlock mentionBlock = new MentionBlock();
                mentionBlock.userId = userInfo.getUserId();
                mentionBlock.offset = false;
                mentionBlock.name = userInfo.getName();
                if (from == 1) {
                    mentionBlock.start = cursorPos - 1;
                } else {
                    mentionBlock.start = cursorPos;
                }
                mentionBlock.end = cursorPos + len;
                mentionInstance.mentionBlocks.add(mentionBlock);

                editText.getEditableText().insert(cursorPos, mentionContent);
                // Show soft keyboard when mentioning someone
                if (mAddMentionedMemberListener != null) {
                    mAddMentionedMemberListener.onAddMentionedMember(userInfo, from);
                }
                mentionBlock.offset = true;
            }
        }
    }

    private MentionBlock getBrokenMentionedBlock(int cursorPos, List<MentionBlock> blocks) {
        MentionBlock brokenBlock = null;
        for (MentionBlock block : blocks) {
            if (block.offset && cursorPos < block.end && cursorPos > block.start) {
                brokenBlock = block;
                break;
            }
        }
        return brokenBlock;
    }

    private void offsetMentionedBlocks(int cursorPos, int offset, List<MentionBlock> blocks) {
        for (MentionBlock block : blocks) {
            if (cursorPos <= block.start && block.offset) {
                block.start += offset;
                block.end += offset;
            }
            block.offset = true;
        }
    }

    private MentionBlock getDeleteMentionedBlock(int cursorPos, List<MentionBlock> blocks) {
        MentionBlock deleteBlock = null;
        // On backspace, cursor moves one position back; since @xxx is followed by a space, add 1 to
        // cursor position
        cursorPos = cursorPos + 1;
        for (MentionBlock block : blocks) {
            if (cursorPos == block.end) {
                deleteBlock = block;
                break;
            }
        }
        return deleteBlock;
    }

    /**
     * Called when the input field text changes.
     *
     * @param conversationType conversation type
     * @param targetId target ID
     * @param cursorPos cursor position at the start of text input
     * @param offset text change amount: positive for additions, negative for deletions
     * @param text text content
     */
    public void onTextChanged(
            Context context,
            ChannelType conversationType,
            String targetId,
            int cursorPos,
            int offset,
            String text,
            EditText editText) {
        RLog.d(TAG, "onTextEdit " + cursorPos + ", " + text);

        if (stack == null || stack.isEmpty()) {
            RLog.w(TAG, "onTextEdit ignore.");
            return;
        }
        MentionInstance mentionInstance = null;
        for (int i = 0; i < stack.size(); i++) {
            MentionInstance item = stack.get(i);
            if (item.inputEditText.equals(editText)) {
                mentionInstance = item;
                break;
            }
        }
        if (mentionInstance == null) {
            RLog.w(TAG, "onTextEdit ignore conversation.");
            return;
        }
        // Check if the single character is @
        if (offset == 1) {
            if (!TextUtils.isEmpty(text)) {
                boolean showMention = false;
                String str;
                if (cursorPos == 0) {
                    str = text.substring(0, 1);
                    showMention = str.equals("@");
                } else {
                    String preChar = text.substring(cursorPos - 1, cursorPos);
                    str = text.substring(cursorPos, cursorPos + 1);
                    if (str.equals("@")
                            && !preChar.matches("^[a-zA-Z]*")
                            && !preChar.matches("^\\d+$")) {
                        showMention = true;
                    }
                }
                if (showMention
                        && (mMentionedInputListener == null
                                || !mMentionedInputListener.onMentionedInput(
                                        conversationType, targetId))) {
                    if (NCUserInfoManager.getInstance().getDataSourceType()
                            == NCUserInfoManager.DataSourceType.INFO_MANAGEMENT) {
                        Intent intent =
                                GroupMentionActivity.newIntent(
                                        context,
                                        new ChannelIdentifier(conversationType, targetId),
                                        ai.nexconn.chat.channel.model.GroupMemberRole.NORMAL);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } else {
                        RouteUtils.routeToMentionMemberSelectActivity(
                                context, targetId, conversationType);
                    }
                }
            }
        }

        // Check if the input cursor position breaks an existing "@block".
        MentionBlock brokenBlock =
                getBrokenMentionedBlock(cursorPos, mentionInstance.mentionBlocks);
        if (brokenBlock != null) {
            mentionInstance.mentionBlocks.remove(brokenBlock);
        }
        // Offset all valid "@block" positions.
        offsetMentionedBlocks(cursorPos, offset, mentionInstance.mentionBlocks);
    }

    public void onSendToggleClick(Message message, EditText editText) {
        MessageContent messageContent = message.getContent();
        applyMentionInfo(messageContent, editText);
    }

    /**
     * Applies @ mention info to the specified message content (no Message wrapper needed).
     *
     * @param messageContent the message content to apply mention info to
     * @param editText the current input field
     */
    public void applyMentionInfo(MessageContent messageContent, EditText editText) {
        if (!stack.isEmpty()) {
            MentionInstance curInstance = null;
            for (int i = 0; i < stack.size(); i++) {
                MentionInstance item = stack.get(i);
                if (item.inputEditText.equals(editText)) {
                    curInstance = item;
                    break;
                }
            }
            if (curInstance == null) {
                RLog.w(TAG, "not found editText");
                return;
            }
            MentionedInfo mentionedInfo = buildMentionedInfo(curInstance);
            if (mentionedInfo != null) {
                curInstance.mentionBlocks.clear();
                messageContent.setMentionedInfo(mentionedInfo);
            }
        }
    }

    public void onClickEditMessageConfirm(Message message, EditText editText) {
        MessageContent messageContent = message.getContent();
        if (!stack.isEmpty()) {
            MentionInstance curInstance = null;
            for (int i = 0; i < stack.size(); i++) {
                MentionInstance item = stack.get(i);
                if (item.inputEditText.equals(editText)) {
                    curInstance = item;
                    break;
                }
            }
            if (curInstance == null) {
                RLog.w(TAG, "not found editText");
                return;
            }
            MentionedInfo mentionedInfo = buildMentionedInfo(curInstance);
            if (mentionedInfo != null) {
                curInstance.mentionBlocks.clear();
                messageContent.setMentionedInfo(mentionedInfo);
            } else {
                messageContent.setMentionedInfo(null);
            }
            message.setContent(messageContent);
        }
    }

    private MentionedInfo buildMentionedInfo(MentionInstance mentionInstance) {
        if (mentionInstance == null || mentionInstance.mentionBlocks == null) {
            return null;
        }
        List<String> userIds = new ArrayList<>();
        boolean mentionAll = false;
        boolean allowMentionAll =
                NCUserInfoManager.getInstance().getDataSourceType()
                        == NCUserInfoManager.DataSourceType.INFO_MANAGEMENT;
        for (MentionBlock block : mentionInstance.mentionBlocks) {
            if (allowMentionAll && MENTION_ALL_USER_ID.equals(block.userId)) {
                mentionAll = true;
            } else if (!userIds.contains(block.userId)) {
                userIds.add(block.userId);
            }
        }
        if (mentionAll) {
            return new MentionedInfo(MentionedType.ALL, null, null);
        } else if (!userIds.isEmpty()) {
            return new MentionedInfo(MentionedType.USERS, userIds, null);
        } else {
            return null;
        }
    }

    public void onDeleteClick(ChannelType type, String targetId, EditText editText, int cursorPos) {
        RLog.d(TAG, "onTextEdit " + cursorPos);

        if (!stack.isEmpty() && cursorPos > 0) {
            MentionInstance mentionInstance = null;
            for (int i = 0; i < stack.size(); i++) {
                MentionInstance item = stack.get(i);
                if (item.inputEditText.equals(editText)) {
                    mentionInstance = item;
                    break;
                }
            }
            if (mentionInstance == null) {
                RLog.w(TAG, "not found editText");
                return;
            }
            MentionBlock deleteBlock =
                    getDeleteMentionedBlock(cursorPos, mentionInstance.mentionBlocks);
            if (deleteBlock != null) {
                mentionInstance.mentionBlocks.remove(deleteBlock);
                String delText = deleteBlock.name;
                int start = cursorPos - delText.length() - 1;
                editText.getEditableText().delete(start, cursorPos);
                editText.setSelection(start);
            }
        }
    }

    private void deleteLastChar(EditText editText) {
        int index = editText.getSelectionStart();
        Editable editable = editText.getText();
        if (editable != null && editable.length() > 0) {
            editable.delete(index - 1, index);
        }
    }

    public IGroupMembersProvider getGroupMembersProvider() {
        return mGroupMembersProvider;
    }

    public void setMentionedInputListener(IMentionedInputListener listener) {
        mMentionedInputListener = listener;
    }

    public void setAddMentionedMemberListener(IAddMentionedMemberListener listener) {
        mAddMentionedMemberListener = listener;
    }

    /**
     * Sets the group members provider.
     *
     * <p>The '@' mention feature and VoIP feature need group member information for the member
     * selection UI. Developers must set this provider and return group member info via {@link
     * IGroupMemberCallback}.
     *
     * @param groupMembersProvider the group members provider.
     */
    public void setGroupMembersProvider(final IGroupMembersProvider groupMembersProvider) {
        mGroupMembersProvider = groupMembersProvider;
    }

    public interface IGroupMembersProvider {
        void getGroupMembers(String groupId, IGroupMemberCallback callback);
    }

    public interface IGroupMemberCallback {
        void onGetGroupMembersResult(List<UserInfo> members);
    }
}
