package ai.nexconn.chatui.channellist.handler;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.channellist.model.BaseUiChannel;
import ai.nexconn.chatui.userinfo.NCUserInfoManager;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Channel list user info handler
 *
 * <p>Listens for user/group/group-member info changes and provides a preloading interface.
 *
 * @since 5.10.4
 */
public class ChannelListUserInfoHandler extends MultiDataHandler
        implements NCUserInfoManager.UserDataObserver {

    public static final DataKey<UserInfo> KEY_USER_UPDATE =
            DataKey.obtain("KEY_USER_UPDATE", UserInfo.class);

    public static final DataKey<GroupInfo> KEY_GROUP_UPDATE =
            DataKey.obtain("KEY_GROUP_UPDATE", GroupInfo.class);

    public static final DataKey<GroupUserInfo> KEY_GROUP_MEMBER_UPDATE =
            DataKey.obtain("KEY_GROUP_MEMBER_UPDATE", GroupUserInfo.class);

    public ChannelListUserInfoHandler() {
        NCUserInfoManager.getInstance().addUserDataObserver(this);
    }

    @Override
    public void onUserUpdate(UserInfo info) {
        if (info != null) {
            notifyDataChange(KEY_USER_UPDATE, info);
        }
    }

    @Override
    public void onGroupUpdate(GroupInfo group) {
        if (group != null) {
            notifyDataChange(KEY_GROUP_UPDATE, group);
        }
    }

    @Override
    public void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo) {
        if (groupUserInfo != null) {
            notifyDataChange(KEY_GROUP_MEMBER_UPDATE, groupUserInfo);
        }
    }

    public void preloadConversationInfos(List<BaseUiChannel> uiConversationList) {
        String currentUserId = NCEngine.getCurrentUserId();
        if (TextUtils.isEmpty(currentUserId) || uiConversationList.isEmpty()) {
            return;
        }
        List<String> privateList = new ArrayList<>();
        List<String> groupList = new ArrayList<>();
        Map<String, String> groupUserInfos = new HashMap<>();
        for (BaseUiChannel uiConversation : uiConversationList) {
            ChannelType type = uiConversation.mCore.getChannelType();
            if (ChannelType.DIRECT == type) {
                privateList.add(uiConversation.mCore.getChannelId());
            } else if (ChannelType.GROUP == type) {
                groupList.add(uiConversation.mCore.getChannelId());
                String latestSenderUserId = uiConversation.getSenderUserId();
                if (!TextUtils.isEmpty(latestSenderUserId)) {
                    groupUserInfos.put(uiConversation.mCore.getChannelId(), latestSenderUserId);
                }
            }
        }
        NCUserInfoManager.getInstance().preloadUserInfos(privateList);
        NCUserInfoManager.getInstance().preloadGroupInfos(groupList);
        NCUserInfoManager.getInstance().preloadGroupUserInfos(groupUserInfos);
    }

    @Override
    public void stop() {
        super.stop();
        NCUserInfoManager.getInstance().removeUserDataObserver(this);
    }
}
