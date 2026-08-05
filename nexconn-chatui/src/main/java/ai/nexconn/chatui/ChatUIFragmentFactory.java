package ai.nexconn.chatui;

import ai.nexconn.chatui.channel.ChannelFragment;
import ai.nexconn.chatui.channel.feature.combineforward.CombineMessagePreviewFragment;
import ai.nexconn.chatui.channel.feature.forward.ForwardSelectChannelFragment;
import ai.nexconn.chatui.channel.feature.mention.MentionMemberSelectFragment;
import ai.nexconn.chatui.channel.readreceipt.MessageReadDetailFragment;
import ai.nexconn.chatui.channel.subchannel.SubChannelListFragment;
import ai.nexconn.chatui.channellist.ChannelListFragment;
import ai.nexconn.chatui.usermanage.friend.add.AddFriendListFragment;
import ai.nexconn.chatui.usermanage.friend.apply.ApplyFriendListFragment;
import ai.nexconn.chatui.usermanage.friend.friendlist.FriendListFragment;
import ai.nexconn.chatui.usermanage.friend.mine.gender.UpdateGenderFragment;
import ai.nexconn.chatui.usermanage.friend.mine.nickname.UpdateNickNameFragment;
import ai.nexconn.chatui.usermanage.friend.mine.profile.MyProfileFragment;
import ai.nexconn.chatui.usermanage.friend.search.FriendSearchFragment;
import ai.nexconn.chatui.usermanage.friend.select.FriendSelectFragment;
import ai.nexconn.chatui.usermanage.friend.user.profile.UserProfileFragment;
import ai.nexconn.chatui.usermanage.group.add.AddGroupMembersFragment;
import ai.nexconn.chatui.usermanage.group.application.GroupApplicationsFragment;
import ai.nexconn.chatui.usermanage.group.create.GroupCreateFragment;
import ai.nexconn.chatui.usermanage.group.follows.GroupFollowsFragment;
import ai.nexconn.chatui.usermanage.group.list.GroupListFragment;
import ai.nexconn.chatui.usermanage.group.manage.GroupManagementFragment;
import ai.nexconn.chatui.usermanage.group.managerlist.GroupManagerListFragment;
import ai.nexconn.chatui.usermanage.group.memberlist.GroupMemberListFragment;
import ai.nexconn.chatui.usermanage.group.memberselect.impl.GroupAddFollowsFragment;
import ai.nexconn.chatui.usermanage.group.memberselect.impl.GroupAddManagerFragment;
import ai.nexconn.chatui.usermanage.group.mention.GroupMentionFragment;
import ai.nexconn.chatui.usermanage.group.name.GroupNameFragment;
import ai.nexconn.chatui.usermanage.group.nickname.GroupNicknameFragment;
import ai.nexconn.chatui.usermanage.group.notice.GroupNoticeFragment;
import ai.nexconn.chatui.usermanage.group.profile.GroupProfileFragment;
import ai.nexconn.chatui.usermanage.group.remove.RemoveGroupMembersFragment;
import ai.nexconn.chatui.usermanage.group.search.GroupSearchFragment;
import ai.nexconn.chatui.usermanage.group.transfer.GroupTransferFragment;
import android.os.Bundle;
import androidx.annotation.NonNull;

/**
 * Factory for all UI screens provided by the ChatUI SDK.
 *
 * <p>Every screen in the SDK is created through this factory.
 *
 * <p>To replace any default screen with a custom Fragment, subclass this factory and override the
 * corresponding {@code newXxxFragment()} method.
 *
 * <p>Register the custom factory via {@link NCChatUI#setFragmentFactory(ChatUIFragmentFactory)}.
 *
 * @since 5.12.0
 */
public class ChatUIFragmentFactory {

    /**
     * Creates a new {@link MyProfileFragment} — displays the current user's own profile and allows
     * editing personal information.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link MyProfileFragment} instance
     */
    @NonNull
    public MyProfileFragment newMyProfileFragment(@NonNull Bundle args) {
        return new MyProfileFragment();
    }

    /**
     * Creates a new {@link UserProfileFragment} — displays another user's profile page.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link UserProfileFragment} instance
     */
    @NonNull
    public UserProfileFragment newUserProfileFragment(@NonNull Bundle args) {
        UserProfileFragment fragment = new UserProfileFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates a new {@link ApplyFriendListFragment} — lists pending friend requests received from
     * other users.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link ApplyFriendListFragment} instance
     */
    @NonNull
    public ApplyFriendListFragment newApplyFriendListFragment(@NonNull Bundle args) {
        ApplyFriendListFragment fragment = new ApplyFriendListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates a new {@link GroupCreateFragment} — screen for creating a new group with an initial
     * member selection.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link GroupCreateFragment} instance
     */
    @NonNull
    public GroupCreateFragment newGroupCreateFragment(@NonNull Bundle args) {
        GroupCreateFragment groupCreateFragment = new GroupCreateFragment();
        groupCreateFragment.setArguments(args);
        return groupCreateFragment;
    }

    /**
     * Creates a new {@link UpdateGenderFragment} — allows the user to update their gender in their
     * profile.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link UpdateGenderFragment} instance
     */
    @NonNull
    public UpdateGenderFragment newUpdateGenderFragment(@NonNull Bundle args) {
        UpdateGenderFragment updateGenderFragment = new UpdateGenderFragment();
        updateGenderFragment.setArguments(args);
        return updateGenderFragment;
    }

    /**
     * Creates a new {@link UpdateNickNameFragment} — allows the user to update their display
     * nickname.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link UpdateNickNameFragment} instance
     */
    @NonNull
    public UpdateNickNameFragment newUpdateNikeNameFragment(@NonNull Bundle args) {
        UpdateNickNameFragment updateGenderFragment = new UpdateNickNameFragment();
        updateGenderFragment.setArguments(args);
        return updateGenderFragment;
    }

    /**
     * Creates a new {@link GroupProfileFragment} — displays group details and settings for a
     * specific group.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link GroupProfileFragment} instance
     */
    @NonNull
    public GroupProfileFragment newGroupProfileFragment(@NonNull Bundle args) {
        GroupProfileFragment groupProfileFragment = new GroupProfileFragment();
        groupProfileFragment.setArguments(args);
        return groupProfileFragment;
    }

    /**
     * Creates a new {@link FriendSelectFragment} — picker for selecting one or more friends from
     * the friend list.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link FriendSelectFragment} instance
     */
    @NonNull
    public FriendSelectFragment newFriendSelectFragment(@NonNull Bundle args) {
        FriendSelectFragment friendSelectFragment = new FriendSelectFragment();
        friendSelectFragment.setArguments(args);
        return friendSelectFragment;
    }

    /**
     * Creates a new {@link AddFriendListFragment} — search and add new friends by user ID or name.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link AddFriendListFragment} instance
     */
    @NonNull
    public AddFriendListFragment newAddFriendListFragment(@NonNull Bundle args) {
        AddFriendListFragment addFriendListFragment = new AddFriendListFragment();
        addFriendListFragment.setArguments(args);
        return addFriendListFragment;
    }

    /**
     * Creates a new {@link FriendListFragment} — shows the current user's complete friend list.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link FriendListFragment} instance
     */
    @NonNull
    public FriendListFragment newFriendListFragment(@NonNull Bundle args) {
        FriendListFragment friendListFragment = new FriendListFragment();
        friendListFragment.setArguments(args);
        return friendListFragment;
    }

    /**
     * Creates a new {@link FriendSearchFragment} — search for existing friends within the friend
     * list.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link FriendSearchFragment} instance
     */
    @NonNull
    public FriendSearchFragment newFriendSearchFragment(@NonNull Bundle args) {
        FriendSearchFragment friendSearchFragment = new FriendSearchFragment();
        friendSearchFragment.setArguments(args);
        return friendSearchFragment;
    }

    /**
     * Creates a new {@link AddGroupMembersFragment} — picker for adding new members to an existing
     * group.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link AddGroupMembersFragment} instance
     */
    @NonNull
    public AddGroupMembersFragment newAddGroupMembersFragment(@NonNull Bundle args) {
        AddGroupMembersFragment addGroupMembersFragment = new AddGroupMembersFragment();
        addGroupMembersFragment.setArguments(args);
        return addGroupMembersFragment;
    }

    /**
     * Creates a new {@link RemoveGroupMembersFragment} — picker for removing existing members from
     * a group.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link RemoveGroupMembersFragment} instance
     */
    @NonNull
    public RemoveGroupMembersFragment newRemoveGroupMembersFragment(@NonNull Bundle args) {
        RemoveGroupMembersFragment removeGroupMembersFragment = new RemoveGroupMembersFragment();
        removeGroupMembersFragment.setArguments(args);
        return removeGroupMembersFragment;
    }

    /**
     * Creates a new {@link GroupNicknameFragment} — allows the user to set their nickname within a
     * specific group.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link GroupNicknameFragment} instance
     */
    @NonNull
    public GroupNicknameFragment newGroupNicknameFragment(@NonNull Bundle args) {
        GroupNicknameFragment groupNicknameFragment = new GroupNicknameFragment();
        groupNicknameFragment.setArguments(args);
        return groupNicknameFragment;
    }

    /**
     * Creates a new {@link GroupNameFragment} — allows the group owner to edit the group's name.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link GroupNameFragment} instance
     */
    @NonNull
    public GroupNameFragment newGroupNameFragment(@NonNull Bundle args) {
        GroupNameFragment groupNameFragment = new GroupNameFragment();
        groupNameFragment.setArguments(args);
        return groupNameFragment;
    }

    /**
     * Creates a new {@link GroupNoticeFragment} — displays and allows editing of the group
     * announcement/notice.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link GroupNoticeFragment} instance
     */
    @NonNull
    public GroupNoticeFragment newGroupNoticeFragment(@NonNull Bundle args) {
        GroupNoticeFragment groupNoticeFragment = new GroupNoticeFragment();
        groupNoticeFragment.setArguments(args);
        return groupNoticeFragment;
    }

    /**
     * Creates a new {@link GroupMemberListFragment} — lists all members of a group.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link GroupMemberListFragment} instance
     */
    @NonNull
    public GroupMemberListFragment newGroupMemberListFragment(@NonNull Bundle args) {
        GroupMemberListFragment groupMemberListFragment = new GroupMemberListFragment();
        groupMemberListFragment.setArguments(args);
        return groupMemberListFragment;
    }

    /**
     * Creates a new {@link GroupFollowsFragment} — lists group members the user is following.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link GroupFollowsFragment} instance
     */
    @NonNull
    public GroupFollowsFragment newGroupFollowsFragment(@NonNull Bundle args) {
        GroupFollowsFragment groupFollowsFragment = new GroupFollowsFragment();
        groupFollowsFragment.setArguments(args);
        return groupFollowsFragment;
    }

    /**
     * Creates a new {@link GroupManagementFragment} — group management screen for owner/admin
     * settings.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link GroupManagementFragment} instance
     */
    @NonNull
    public GroupManagementFragment newGroupManagementFragment(@NonNull Bundle args) {
        GroupManagementFragment groupManagementFragment = new GroupManagementFragment();
        groupManagementFragment.setArguments(args);
        return groupManagementFragment;
    }

    /**
     * Creates a new {@link GroupManagerListFragment} — lists the current group administrators.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link GroupManagerListFragment} instance
     */
    @NonNull
    public GroupManagerListFragment newGroupManagerListFragment(@NonNull Bundle args) {
        GroupManagerListFragment groupManagerListFragment = new GroupManagerListFragment();
        groupManagerListFragment.setArguments(args);
        return groupManagerListFragment;
    }

    /**
     * Creates a new {@link GroupAddFollowsFragment} — picker for selecting group members to follow.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link GroupAddFollowsFragment} instance
     */
    @NonNull
    public GroupAddFollowsFragment newGroupAddFollowsFragment(@NonNull Bundle args) {
        GroupAddFollowsFragment groupAddFollowsFragment = new GroupAddFollowsFragment();
        groupAddFollowsFragment.setArguments(args);
        return groupAddFollowsFragment;
    }

    /**
     * Creates a new {@link GroupAddManagerFragment} — picker for promoting group members to
     * administrator.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link GroupAddManagerFragment} instance
     */
    @NonNull
    public GroupAddManagerFragment newGroupAddManagerFragment(@NonNull Bundle args) {
        GroupAddManagerFragment groupAddManagerFragment = new GroupAddManagerFragment();
        groupAddManagerFragment.setArguments(args);
        return groupAddManagerFragment;
    }

    /**
     * Creates a new {@link GroupTransferFragment} — transfers group ownership to another member.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link GroupTransferFragment} instance
     */
    @NonNull
    public GroupTransferFragment newGroupTransferFragment(@NonNull Bundle args) {
        GroupTransferFragment groupTransferFragment = new GroupTransferFragment();
        groupTransferFragment.setArguments(args);
        return groupTransferFragment;
    }

    /**
     * Creates a new {@link GroupListFragment} — shows all groups the current user has joined.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link GroupListFragment} instance
     */
    @NonNull
    public GroupListFragment newGroupListFragment(@NonNull Bundle args) {
        GroupListFragment groupListFragment = new GroupListFragment();
        groupListFragment.setArguments(args);
        return groupListFragment;
    }

    /**
     * Creates a new {@link GroupSearchFragment} — search for groups by name or group ID.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link GroupSearchFragment} instance
     */
    @NonNull
    public GroupSearchFragment newGroupSearchFragment(@NonNull Bundle args) {
        GroupSearchFragment groupSearchFragment = new GroupSearchFragment();
        groupSearchFragment.setArguments(args);
        return groupSearchFragment;
    }

    /**
     * Creates a new {@link GroupApplicationsFragment} — lists pending group join applications for
     * review.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link GroupApplicationsFragment} instance
     */
    @NonNull
    public GroupApplicationsFragment newGroupApplicationsFragment(@NonNull Bundle args) {
        GroupApplicationsFragment groupApplicationsFragment = new GroupApplicationsFragment();
        groupApplicationsFragment.setArguments(args);
        return groupApplicationsFragment;
    }

    /**
     * Creates a new {@link MessageReadDetailFragment} — shows who has read a specific message
     * (read-receipt detail).
     *
     * @param args arguments passed to the fragment
     * @return a new {@link MessageReadDetailFragment} instance
     */
    @NonNull
    public MessageReadDetailFragment newMessageReadDetailFragment(@NonNull Bundle args) {
        MessageReadDetailFragment fragment = new MessageReadDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates a new {@link GroupMentionFragment} — picker for selecting group members to @mention
     * in a message.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link GroupMentionFragment} instance
     */
    @NonNull
    public GroupMentionFragment newGroupMentionFragment(@NonNull Bundle args) {
        GroupMentionFragment fragment = new GroupMentionFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates a new {@link ChannelFragment} — the main conversation screen showing message history
     * and an input bar.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link ChannelFragment} instance
     */
    @NonNull
    public ChannelFragment newChannelFragment(@NonNull Bundle args) {
        ChannelFragment fragment = new ChannelFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates a new {@link ChannelListFragment} — the conversation list screen showing all recent
     * conversations.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link ChannelListFragment} instance
     */
    @NonNull
    public ChannelListFragment newChannelListFragment(@NonNull Bundle args) {
        ChannelListFragment fragment = new ChannelListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates a new {@link SubChannelListFragment} — the sub-channel list screen for
     * community/forum channels.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link SubChannelListFragment} instance
     */
    @NonNull
    public SubChannelListFragment newSubChannelListFragment(@NonNull Bundle args) {
        SubChannelListFragment fragment = new SubChannelListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates a new {@link ForwardSelectChannelFragment} — picker for selecting a conversation
     * target to forward a message to.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link ForwardSelectChannelFragment} instance
     */
    @NonNull
    public ForwardSelectChannelFragment newForwardSelectFragment(@NonNull Bundle args) {
        ForwardSelectChannelFragment fragment = new ForwardSelectChannelFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates a new {@link MentionMemberSelectFragment} — picker for selecting a group member
     * to @mention.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link MentionMemberSelectFragment} instance
     */
    @NonNull
    public MentionMemberSelectFragment newMentionMemberSelectFragment(@NonNull Bundle args) {
        MentionMemberSelectFragment fragment = new MentionMemberSelectFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates a new {@link CombineMessagePreviewFragment} — preview screen for a combined
     * (merged-forward) message.
     *
     * @param args arguments passed to the fragment
     * @return a new {@link CombineMessagePreviewFragment} instance
     */
    @NonNull
    public CombineMessagePreviewFragment newCombineMessagePreviewFragment(@NonNull Bundle args) {
        CombineMessagePreviewFragment fragment = new CombineMessagePreviewFragment();
        fragment.setArguments(args);
        return fragment;
    }
}
