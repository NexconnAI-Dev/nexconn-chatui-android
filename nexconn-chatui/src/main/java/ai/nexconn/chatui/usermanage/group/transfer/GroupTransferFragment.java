package ai.nexconn.chatui.usermanage.group.transfer;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.usermanage.component.ContactListComponent;
import ai.nexconn.chatui.usermanage.group.manage.GroupManagementFragment;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.widget.CommonDialog;
import ai.nexconn.chatui.widget.component.HeadComponent;
import ai.nexconn.chatui.widget.component.SearchComponent;
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
import java.util.Arrays;
import java.util.List;

/**
 * Fragment for group ownership transfer.
 *
 * @since 5.12.2
 */
public class GroupTransferFragment extends BaseViewModelFragment<GroupTransferViewModel> {

    protected HeadComponent headComponent;
    protected SearchComponent searchComponent;
    protected ContactListComponent contactListComponent;

    @NonNull
    @Override
    protected GroupTransferViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupTransferViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_group_transfer, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        searchComponent = view.findViewById(R.id.nc_search_component);
        contactListComponent = view.findViewById(R.id.nc_contact_list_component);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupTransferViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        searchComponent.setSearchQueryListener(viewModel::queryGroupMembers);
        contactListComponent.setOnPageDataLoader(viewModel);
        contactListComponent.setEnableLoadMore(true);
        viewModel
                .getFilteredContactsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null) {
                                contactListComponent.post(
                                        () -> contactListComponent.setContactList(contactModels));
                            }
                        });

        contactListComponent.setOnItemClickListener(
                contactModel -> {
                    if (contactModel != null && contactModel.getBean() instanceof GroupMemberInfo) {
                        GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contactModel.getBean();
                        if (groupMemberInfo.getRole() == GroupMemberRole.OWNER) {
                            return;
                        }
                        onGroupOwnerTransfer(groupMemberInfo);
                    }
                });
    }

    /**
     * Transfer group ownership.
     *
     * @param groupMemberInfo target group member info
     */
    protected void onGroupOwnerTransfer(GroupMemberInfo groupMemberInfo) {
        NCEngine.getUserModule()
                .getFriendsInfo(
                        Arrays.asList(groupMemberInfo.getUserId()),
                        (friendDetails, error) -> {
                            if (error == null) {
                                transferGroupOwner(friendDetails, groupMemberInfo);
                            } else {
                                transferGroupOwner(null, groupMemberInfo);
                            }
                        });
    }

    private void transferGroupOwner(
            List<FriendDetail> friendDetails, GroupMemberInfo groupMemberInfo) {
        String name = concatenateUserDisplayNames(friendDetails, groupMemberInfo);
        // Show transfer confirmation dialog
        new CommonDialog.Builder()
                .setTitleText(R.string.nc_prompt)
                .setContentMessage(getString(R.string.nc_group_transfer_hint, name))
                .setPositiveTextColor(
                        ChatUIThemeManager.getColorFromAttrId(
                                getContext(), R.attr.nc_primary_color))
                .setDialogButtonClickListener(
                        (v, bundle) -> {
                            getViewModel()
                                    .transferGroupOwner(
                                            groupMemberInfo,
                                            isSuccess ->
                                                    onGroupOwnerTransferResult(
                                                            getViewModel().getGroupId(),
                                                            groupMemberInfo,
                                                            isSuccess));
                        })
                .build()
                .show(getParentFragmentManager(), null);
    }

    /**
     * Handle group ownership transfer result.
     *
     * @param groupId group ID
     * @param groupMemberInfo target group member info
     * @param isSuccess whether the transfer succeeded
     */
    protected void onGroupOwnerTransferResult(
            String groupId, GroupMemberInfo groupMemberInfo, boolean isSuccess) {
        if (isSuccess) {
            ToastUtils.show(
                    getContext(),
                    getString(R.string.nc_group_transfer_success),
                    Toast.LENGTH_SHORT);
            sendFinishActivityBroadcast(GroupManagementFragment.class);
            finishActivity();
        } else {
            ToastUtils.show(
                    getContext(), getString(R.string.nc_group_transfer_failed), Toast.LENGTH_SHORT);
        }
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
