package ai.nexconn.chatui.usermanage.group.managerlist;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.usermanage.component.ContactListComponent;
import ai.nexconn.chatui.usermanage.group.memberselect.impl.GroupAddManagerActivity;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import ai.nexconn.chatui.widget.CommonDialog;
import ai.nexconn.chatui.widget.SettingItemView;
import ai.nexconn.chatui.widget.component.HeadComponent;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fragment for group manager list.
 *
 * @since 5.12.2
 */
public class GroupManagerListFragment extends BaseViewModelFragment<GroupManagerListViewModel> {

    protected HeadComponent headComponent;
    protected ContactListComponent memberListComponent;
    protected SettingItemView groupAddManager;
    private static final int MAX_COUNT = 10;

    @NonNull
    @Override
    protected GroupManagerListViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupManagerListViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_group_manager_list, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        groupAddManager = view.findViewById(R.id.siv_group_add_manager);
        memberListComponent = view.findViewById(R.id.nc_group_list_component);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupManagerListViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());

        GroupMemberRole groupMemberRole =
                (GroupMemberRole)
                        getArguments().getSerializable(ChatUIConstants.KEY_GROUP_MEMBER_ROLE);

        ChannelIdentifier conversationIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        getArguments(), ChatUIConstants.KEY_CHANNEL_IDENTIFIER);
        if (conversationIdentifier == null) {
            return;
        }

        boolean isOwner = groupMemberRole == GroupMemberRole.OWNER;
        groupAddManager.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        memberListComponent.setShowItemRemoveButton(isOwner);

        // Add group manager
        groupAddManager.setOnClickListener(
                v -> {
                    List<String> userIdList = new ArrayList<>();
                    List<ContactModel> contactModels =
                            viewModel.getAllGroupManagersLiveData().getValue();
                    if (contactModels != null) {
                        for (ContactModel contactModel : contactModels) {
                            if (contactModel.getBean() instanceof GroupMemberInfo) {
                                GroupMemberInfo groupMemberInfo =
                                        (GroupMemberInfo) contactModel.getBean();
                                userIdList.add(groupMemberInfo.getUserId());
                            }
                        }
                    }
                    addGroupManager(conversationIdentifier, userIdList);
                });

        memberListComponent.setOnItemRemoveClickListener(
                contactModel -> {
                    if (contactModel.getBean() instanceof GroupMemberInfo) {
                        GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contactModel.getBean();
                        onRemoveGroupManager(
                                conversationIdentifier.getChannelId(), groupMemberInfo);
                    }
                });

        // Observe contact list changes in ViewModel
        viewModel
                .getAllGroupManagersLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null && memberListComponent != null) {
                                memberListComponent.setContactList(contactModels);
                            }
                        });
    }

    /**
     * Add group manager.
     *
     * @param conversationIdentifier channel identifier
     * @param UserIdList user ID list
     */
    protected void addGroupManager(
            ChannelIdentifier conversationIdentifier, List<String> UserIdList) {
        startActivity(
                GroupAddManagerActivity.newIntent(
                        getContext(),
                        conversationIdentifier,
                        UserIdList,
                        MAX_COUNT - UserIdList.size()));
    }

    /**
     * Remove a group manager.
     *
     * @param groupId group ID
     * @param groupMemberInfo group member info
     */
    protected void onRemoveGroupManager(String groupId, GroupMemberInfo groupMemberInfo) {
        NCEngine.getUserModule()
                .getFriendsInfo(
                        Arrays.asList(groupMemberInfo.getUserId()),
                        new OperationHandler<List<FriendDetail>>() {
                            @Override
                            public void onResult(List<FriendDetail> friendDetails, NCError error) {
                                if (error == null) {
                                    removeGroupManager(groupId, friendDetails, groupMemberInfo);
                                } else {
                                    removeGroupManager(groupId, null, groupMemberInfo);
                                }
                            }
                        });
    }

    private void removeGroupManager(
            String groupId, List<FriendDetail> friendDetails, GroupMemberInfo groupMemberInfo) {
        String name = concatenateUserDisplayNames(friendDetails, groupMemberInfo);
        new CommonDialog.Builder()
                .setContentMessage(getString(R.string.nc_remove_manager_hint, name))
                .setButtonText(R.string.nc_remove, R.string.nc_cancel)
                .setDialogButtonClickListener(
                        (v, bundle) -> {
                            getViewModel()
                                    .removeGroupManager(
                                            Arrays.asList(groupMemberInfo.getUserId()),
                                            isSuccess -> {
                                                onGroupManagerRemovalResult(
                                                        groupId, groupMemberInfo, isSuccess);
                                            });
                        })
                .build()
                .show(getParentFragmentManager(), null);
    }

    /**
     * Handle group manager removal result.
     *
     * @param groupId group ID
     * @param groupMemberInfo group member info
     * @param isSuccess whether the operation succeeded
     */
    protected void onGroupManagerRemovalResult(
            String groupId, GroupMemberInfo groupMemberInfo, boolean isSuccess) {
        if (isSuccess) {
            getViewModel().refreshGroupManagerList();
        }
        ToastUtils.show(
                getContext(),
                isSuccess
                        ? getString(R.string.nc_remove_success)
                        : getString(R.string.nc_remove_failed),
                Toast.LENGTH_SHORT);
    }

    @Override
    public void onStart() {
        super.onStart();
        getViewModel().refreshGroupManagerList();
    }

    private String concatenateUserDisplayNames(
            List<FriendDetail> friendDetails, GroupMemberInfo groupMemberInfo) {
        String displayName =
                TextUtils.isEmpty(groupMemberInfo.getNickname())
                        ? groupMemberInfo.getName()
                        : groupMemberInfo.getNickname();
        if (friendDetails != null && !friendDetails.isEmpty()) {
            String remark = friendDetails.get(0).getRemark();
            if (!TextUtils.isEmpty(remark)) {
                displayName = remark;
            }
        }
        return displayName;
    }
}
