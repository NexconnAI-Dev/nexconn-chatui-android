package ai.nexconn.chatui.usermanage.group.profile;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.ChannelNoDisturbLevel;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chat.channel.model.GroupOperationPermission;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.usermanage.adapter.GroupMembersAdapter;
import ai.nexconn.chatui.usermanage.friend.mine.profile.MyProfileActivity;
import ai.nexconn.chatui.usermanage.friend.user.profile.UserProfileActivity;
import ai.nexconn.chatui.usermanage.group.add.AddGroupMembersActivity;
import ai.nexconn.chatui.usermanage.group.follows.GroupFollowsActivity;
import ai.nexconn.chatui.usermanage.group.manage.GroupManagementActivity;
import ai.nexconn.chatui.usermanage.group.memberlist.GroupMemberListActivity;
import ai.nexconn.chatui.usermanage.group.name.GroupNameActivity;
import ai.nexconn.chatui.usermanage.group.nickname.GroupNicknameActivity;
import ai.nexconn.chatui.usermanage.group.notice.GroupNoticeActivity;
import ai.nexconn.chatui.usermanage.group.remove.RemoveGroupMembersActivity;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import ai.nexconn.chatui.utils.image.ImageViewUtils;
import ai.nexconn.chatui.utils.view.RTLUtils;
import ai.nexconn.chatui.widget.SettingItemView;
import ai.nexconn.chatui.widget.component.HeadComponent;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Objects;

/**
 * Fragment for group profile.
 *
 * @since 5.12.0
 */
public class GroupProfileFragment extends BaseViewModelFragment<GroupProfileViewModel> {

    private int displayMaxMemberCount;

    protected HeadComponent headComponent;
    private RecyclerView groupMemberRecyclerView;
    private GroupMembersAdapter groupMembersAdapter;
    protected SettingItemView groupAvatarView;
    protected SettingItemView groupNameView;
    protected SettingItemView groupNoticeView;
    protected SettingItemView groupNicknameView;

    /**
     * @since 5.12.2
     */
    /**
     * @since 5.12.2
     */
    protected SettingItemView groupManageView;

    /**
     * @since 5.12.2
     */
    protected SettingItemView messageDisturbView;

    /**
     * @since 5.12.2
     */
    protected SettingItemView groupFollowsView;

    /**
     * @since 5.12.2
     */
    protected SettingItemView conversationSetTopView;

    private TextView groupMembersLabel;
    private LinearLayout groupMembersLayout;
    protected Button dismissGroupButton;

    @NonNull
    @Override
    protected GroupProfileViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupProfileViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_group_profile, container, false);

        // Initialize components
        headComponent = view.findViewById(R.id.nc_head_component);
        groupMemberRecyclerView = view.findViewById(R.id.rv_group_members);
        groupAvatarView = view.findViewById(R.id.siv_group_avatar);
        groupNameView = view.findViewById(R.id.siv_group_name);
        groupNoticeView = view.findViewById(R.id.siv_group_announcement);
        groupNicknameView = view.findViewById(R.id.siv_my_nickname);
        groupManageView = view.findViewById(R.id.siv_group_manage);
        messageDisturbView = view.findViewById(R.id.siv_message_disturb);
        groupFollowsView = view.findViewById(R.id.siv_group_follows);
        conversationSetTopView = view.findViewById(R.id.siv_conversation_set_top);

        dismissGroupButton = view.findViewById(R.id.btn_dissolve_group);
        groupMembersLabel = view.findViewById(R.id.tv_group_members_label);
        groupMembersLayout = view.findViewById(R.id.ll_group_members);

        // Enable RTL auto-mirror for group members arrow
        ImageViewUtils.enableDrawableAutoMirror(view.findViewById(R.id.iv_group_members_arrow));

        // Configure RecyclerView
        groupMemberRecyclerView.setLayoutManager(new GridLayoutManager(context, 5));
        displayMaxMemberCount =
                Math.max(
                        5,
                        Math.min(
                                50,
                                getArguments()
                                        .getInt(ChatUIConstants.KEY_MAX_MEMBER_COUNT_DISPLAY, 30)));
        groupMembersAdapter = new GroupMembersAdapter(context, displayMaxMemberCount);
        groupMemberRecyclerView.setAdapter(groupMembersAdapter);

        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupProfileViewModel viewModel) {

        headComponent.setLeftClickListener(v -> finishActivity());

        ChannelIdentifier conversationIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        getArguments(), ChatUIConstants.KEY_CHANNEL_IDENTIFIER);
        groupMembersLayout.setOnClickListener(
                v ->
                        startActivity(
                                GroupMemberListActivity.newIntent(
                                        getContext(), conversationIdentifier)));

        // Observe group member data in ViewModel
        viewModel
                .getGroupMemberInfosLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        groupMemberInfos -> {
                            if (groupMembersAdapter != null) {
                                groupMemberRecyclerView.post(
                                        () ->
                                                groupMembersAdapter.updateGroupInfoList(
                                                        groupMemberInfos));
                            }
                        });

        // Observe group info data in ViewModel
        groupAvatarView.setSelected(true);
        groupAvatarView.setRightImageVisibility(View.GONE);
        viewModel
                .getGroupInfoLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        groupInfo -> {
                            if (groupMembersLabel != null && headComponent != null) {
                                groupMembersLabel.setText(
                                        getString(
                                                R.string.nc_group_members_label,
                                                groupInfo.getMembersCount()));
                                headComponent.setTitleText(
                                        getString(
                                                R.string.nc_group_info,
                                                groupInfo.getMembersCount()));
                            }
                            // In RTL mode, set ScaleType to FIT_START to align left
                            if (RTLUtils.isRtl(this.getContext())) {
                                groupAvatarView
                                        .getSelectImage()
                                        .setScaleType(ImageView.ScaleType.FIT_START);
                            }
                            // Set group avatar
                            NCChatUIConfig.featureConfig()
                                    .getChatUIImageEngine()
                                    .loadGroupPortrait(
                                            groupAvatarView.getContext(),
                                            groupInfo.getPortraitUri(),
                                            groupAvatarView.getSelectImage());

                            // Set group name
                            groupNameView.setValue(groupInfo.getGroupName());
                            boolean canAddMembers =
                                    (groupInfo.getInvitePermission()
                                                    == GroupOperationPermission.EVERYONE)
                                            || (groupInfo.getInvitePermission()
                                                            == GroupOperationPermission
                                                                    .OWNER_OR_ADMIN
                                                    && (groupInfo.getRole() == GroupMemberRole.ADMIN
                                                            || groupInfo.getRole()
                                                                    == GroupMemberRole.OWNER))
                                            || (groupInfo.getInvitePermission()
                                                            == GroupOperationPermission.OWNER
                                                    && groupInfo.getRole()
                                                            == GroupMemberRole.OWNER);
                            groupMembersAdapter.setAllowGroupAddition(canAddMembers);

                            boolean canRemoveMembers =
                                    (groupInfo.getRemoveMemberPermission()
                                                    == GroupOperationPermission.EVERYONE)
                                            || (groupInfo.getRemoveMemberPermission()
                                                            == GroupOperationPermission
                                                                    .OWNER_OR_ADMIN
                                                    && (groupInfo.getRole() == GroupMemberRole.ADMIN
                                                            || groupInfo.getRole()
                                                                    == GroupMemberRole.OWNER))
                                            || (groupInfo.getRemoveMemberPermission()
                                                            == GroupOperationPermission.OWNER
                                                    && groupInfo.getRole()
                                                            == GroupMemberRole.OWNER);
                            groupMembersAdapter.setAllowGroupRemoval(canRemoveMembers);

                            if (groupManageView != null) {
                                boolean canManageGroup =
                                        groupInfo.getRole() == GroupMemberRole.ADMIN
                                                || groupInfo.getRole() == GroupMemberRole.OWNER;
                                groupManageView.setVisibility(
                                        canManageGroup ? View.VISIBLE : View.GONE);
                            }
                        });

        viewModel
                .getMyMemberInfoLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        groupMemberInfo -> {
                            // Set "my nickname in this group"
                            groupNicknameView.setValue(groupMemberInfo.getNickname());
                            // Set dismiss group button visibility
                            boolean isOwner = groupMemberInfo.getRole() == GroupMemberRole.OWNER;
                            dismissGroupButton.setText(
                                    isOwner ? R.string.nc_dissolve_group : R.string.nc_leave_group);
                        });

        // Handle add/remove member click events
        groupMembersAdapter.setOnGroupActionListener(
                new GroupMembersAdapter.OnGroupActionListener() {
                    @Override
                    public void addMemberClick() {
                        onAddMemberClick(conversationIdentifier);
                    }

                    @Override
                    public void removeMemberClick() {
                        onRemoveMemberClick(conversationIdentifier);
                    }

                    @Override
                    public void onGroupClicked(GroupMemberInfo groupMemberInfo) {
                        // Show member details
                        onGroupMemberClick(conversationIdentifier, groupMemberInfo);
                    }
                });

        // Group name
        groupNameView.setOnClickListener(
                v -> {
                    // Handle group name click, possibly edit group name
                    GroupInfo groupInfo = viewModel.getGroupInfoLiveData().getValue();

                    if (groupInfo != null) {
                        boolean canEditNotice =
                                (groupInfo.getGroupInfoEditPermission()
                                                == GroupOperationPermission.EVERYONE)
                                        || (groupInfo.getGroupInfoEditPermission()
                                                        == GroupOperationPermission.OWNER_OR_ADMIN
                                                && (groupInfo.getRole() == GroupMemberRole.ADMIN
                                                        || groupInfo.getRole()
                                                                == GroupMemberRole.OWNER))
                                        || (groupInfo.getGroupInfoEditPermission()
                                                        == GroupOperationPermission.OWNER
                                                && groupInfo.getRole() == GroupMemberRole.OWNER);
                        if (!canEditNotice) {
                            ToastUtils.show(
                                    getContext(),
                                    getString(R.string.nc_no_permission_to_modify_group_info),
                                    Toast.LENGTH_SHORT);
                            return;
                        }
                        startActivity(
                                GroupNameActivity.newIntent(
                                        getContext(), conversationIdentifier, groupInfo));
                    }
                });

        // Group notice
        groupNoticeView.setOnClickListener(
                v -> {
                    GroupInfo groupInfo = viewModel.getGroupInfoLiveData().getValue();
                    if (groupInfo != null) {
                        startActivity(
                                GroupNoticeActivity.newIntent(
                                        getContext(), conversationIdentifier, groupInfo));
                    }
                });
        // Group nickname
        groupNicknameView.setOnClickListener(
                v -> {
                    String currentUserId = NCEngine.getCurrentUserId();
                    if (currentUserId != null) {
                        startActivity(
                                GroupNicknameActivity.newIntent(
                                        getContext(),
                                        conversationIdentifier,
                                        currentUserId,
                                        getString(R.string.nc_my_nickname_in_group)));
                    }
                });

        // Group management
        if (groupManageView != null) {
            groupManageView.setOnClickListener(
                    v ->
                            startActivity(
                                    GroupManagementActivity.newIntent(
                                            getContext(), conversationIdentifier)));
        }

        // Conversation pin and do-not-disturb
        viewModel
                .getConversationNotificationStatusLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        conversationNotificationStatus -> {
                            if (messageDisturbView != null && groupFollowsView != null) {
                                boolean isNotDisturb =
                                        conversationNotificationStatus
                                                == ChannelNoDisturbLevel.MUTED;
                                messageDisturbView.setChecked(isNotDisturb);
                                groupFollowsView.setVisibility(
                                        isNotDisturb ? View.VISIBLE : View.GONE);
                            }
                        });
        viewModel
                .getIsConversationTopLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        isTop -> {
                            if (conversationSetTopView != null) {
                                conversationSetTopView.setChecked(Boolean.TRUE.equals(isTop));
                            }
                        });
        // Do-not-disturb
        if (messageDisturbView != null) {
            messageDisturbView.setSwitchCheckListener(
                    (buttonView, isChecked) ->
                            viewModel.setConversationNotificationStatus(
                                    isChecked
                                            ? ChannelNoDisturbLevel.MUTED
                                            : ChannelNoDisturbLevel.ALL_MESSAGE,
                                    conversationNotificationStatus -> {
                                        if (messageDisturbView != null
                                                && groupFollowsView != null) {
                                            boolean isNodDisturb =
                                                    conversationNotificationStatus
                                                            == ChannelNoDisturbLevel.MUTED;
                                            messageDisturbView.setChecked(isNodDisturb);
                                            groupFollowsView.setVisibility(
                                                    isNodDisturb ? View.VISIBLE : View.GONE);
                                        }
                                    }));
        }
        // Special follows
        if (groupFollowsView != null) {
            groupFollowsView.setContent("   --" + getString(R.string.nc_group_follows));
            groupFollowsView.setOnClickListener(
                    v ->
                            startActivity(
                                    GroupFollowsActivity.newIntent(
                                            getContext(), conversationIdentifier)));
        }

        // Conversation pin to top
        if (conversationSetTopView != null) {
            conversationSetTopView.setSwitchCheckListener(
                    (buttonView, isChecked) ->
                            viewModel.setConversationTopStatus(
                                    isChecked,
                                    isTop -> {
                                        if (isTop != null && !isTop) {
                                            conversationSetTopView.setChecked(
                                                    Boolean.TRUE.equals(
                                                            viewModel
                                                                    .getIsConversationTopLiveData()
                                                                    .getValue()));
                                            ToastUtils.show(
                                                    getContext(),
                                                    getString(R.string.nc_set_failed),
                                                    Toast.LENGTH_SHORT);
                                        }
                                    }));
        }

        // Set dismiss group button click event
        dismissGroupButton.setOnClickListener(
                v -> {
                    GroupMemberInfo groupMemberInfo =
                            viewModel.getMyMemberInfoLiveData().getValue();
                    if (groupMemberInfo != null) {
                        boolean isOwner = groupMemberInfo.getRole() == GroupMemberRole.OWNER;
                        if (isOwner) {
                            viewModel.dismissGroup(
                                    isSuccess -> {
                                        if (isSuccess) {
                                            finishActivity();
                                            ToastUtils.show(
                                                    getContext(),
                                                    getString(R.string.nc_group_dismiss_success),
                                                    Toast.LENGTH_SHORT);
                                        } else {
                                            ToastUtils.show(
                                                    getContext(),
                                                    getString(R.string.nc_group_dismiss_failed),
                                                    Toast.LENGTH_SHORT);
                                        }
                                    });
                        } else {
                            viewModel.quitGroup(
                                    isSuccess -> {
                                        if (isSuccess) {
                                            finishActivity();
                                            ToastUtils.show(
                                                    getContext(),
                                                    getString(R.string.nc_group_quit_success),
                                                    Toast.LENGTH_SHORT);
                                        } else {
                                            ToastUtils.show(
                                                    getContext(),
                                                    getString(R.string.nc_group_quit_failed),
                                                    Toast.LENGTH_SHORT);
                                        }
                                    });
                        }
                    }
                });
    }

    /**
     * Handle add member click event.
     *
     * @param conversationIdentifier channel identifier
     * @since 5.12.2
     */
    protected void onAddMemberClick(ChannelIdentifier conversationIdentifier) {
        startActivity(AddGroupMembersActivity.newIntent(getContext(), conversationIdentifier));
    }

    /**
     * Handle remove member click event.
     *
     * @param conversationIdentifier channel identifier
     * @since 5.12.2
     */
    protected void onRemoveMemberClick(ChannelIdentifier conversationIdentifier) {
        // Handle remove member logic
        GroupInfo groupInfo = getViewModel().getGroupInfoLiveData().getValue();
        if (groupInfo != null) {
            startActivity(
                    RemoveGroupMembersActivity.newIntent(
                            getContext(), conversationIdentifier, groupInfo.getRole()));
        }
    }

    /**
     * Handle group member click event.
     *
     * @param conversationIdentifier channel identifier
     * @param groupMemberInfo group member info
     * @since 5.12.2
     */
    protected void onGroupMemberClick(
            ChannelIdentifier conversationIdentifier, GroupMemberInfo groupMemberInfo) {
        if (groupMemberInfo != null) {
            String currentUserId = NCEngine.getCurrentUserId();
            if (Objects.equals(groupMemberInfo.getUserId(), currentUserId)) {
                startActivity(MyProfileActivity.newIntent(getContext()));
            } else {
                startActivity(
                        UserProfileActivity.newIntent(
                                getContext(), groupMemberInfo.getUserId(), conversationIdentifier));
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getViewModel().refreshGroupInfo();
    }
}
