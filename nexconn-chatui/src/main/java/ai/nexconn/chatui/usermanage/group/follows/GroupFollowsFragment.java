package ai.nexconn.chatui.usermanage.group.follows;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.usermanage.component.ContactListComponent;
import ai.nexconn.chatui.usermanage.group.memberselect.impl.GroupAddFollowsActivity;
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
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fragment for group follows list.
 *
 * @since 5.12.2
 */
public class GroupFollowsFragment extends BaseViewModelFragment<GroupFollowsViewModel> {

    protected HeadComponent headComponent;
    protected ContactListComponent memberListComponent;
    protected SettingItemView groupAddMember;
    private TextView emptyView;

    @NonNull
    @Override
    protected GroupFollowsViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupFollowsViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_group_follows, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        groupAddMember = view.findViewById(R.id.siv_group_add_member);
        memberListComponent = view.findViewById(R.id.nc_group_list_component);
        emptyView = view.findViewById(R.id.nc_empty_tv);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupFollowsViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        ChannelIdentifier conversationIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        getArguments(), ChatUIConstants.KEY_CHANNEL_IDENTIFIER);

        groupAddMember.setOnClickListener(
                v -> {
                    List<String> followsUserIds = new ArrayList<>();
                    List<ContactModel> contactModels =
                            viewModel.getAllGroupFollowsLiveData().getValue();
                    if (contactModels != null) {
                        for (ContactModel contactModel : contactModels) {
                            if (contactModel.getBean() instanceof GroupMemberInfo) {
                                followsUserIds.add(
                                        ((GroupMemberInfo) contactModel.getBean()).getUserId());
                            }
                        }
                    }
                    startActivity(
                            GroupAddFollowsActivity.newIntent(
                                    getContext(), conversationIdentifier, followsUserIds));
                });

        memberListComponent.setOnItemRemoveClickListener(
                contactModel -> {
                    if (contactModel.getBean() instanceof GroupMemberInfo) {
                        GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contactModel.getBean();
                        onRemoveGroupFollow(groupMemberInfo);
                    }
                });

        // Observe contact list changes in ViewModel
        viewModel
                .getAllGroupFollowsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null && !contactModels.isEmpty()) {
                                emptyView.setVisibility(View.GONE);
                                memberListComponent.setVisibility(View.VISIBLE);
                                memberListComponent.setContactList(contactModels);
                            } else {
                                emptyView.setVisibility(View.VISIBLE);
                                memberListComponent.setVisibility(View.GONE);
                            }
                        });
    }

    /**
     * Remove a group follow.
     *
     * @param groupMemberInfo group member info
     */
    protected void onRemoveGroupFollow(GroupMemberInfo groupMemberInfo) {
        NCEngine.getUserModule()
                .getFriendsInfo(
                        Arrays.asList(groupMemberInfo.getUserId()),
                        new OperationHandler<List<FriendDetail>>() {
                            @Override
                            public void onResult(List<FriendDetail> friendDetails, NCError error) {
                                if (error == null) {
                                    transferGroupOwner(friendDetails, groupMemberInfo);
                                } else {
                                    transferGroupOwner(null, groupMemberInfo);
                                }
                            }
                        });
    }

    private void transferGroupOwner(
            List<FriendDetail> friendDetails, GroupMemberInfo groupMemberInfo) {
        String name = concatenateUserDisplayNames(friendDetails, groupMemberInfo);
        // Show removal confirmation dialog
        new CommonDialog.Builder()
                .setContentMessage(getString(R.string.nc_remove_follow_hint, name))
                .setDialogButtonClickListener(
                        (v, bundle) -> {
                            getViewModel()
                                    .removeGroupFollows(
                                            Arrays.asList(groupMemberInfo.getUserId()),
                                            isSuccess -> {
                                                if (isSuccess) {
                                                    getViewModel().refreshGroupFollows();
                                                }
                                                ToastUtils.show(
                                                        getContext(),
                                                        isSuccess
                                                                ? getString(
                                                                        R.string.nc_remove_success)
                                                                : getString(
                                                                        R.string.nc_remove_failed),
                                                        Toast.LENGTH_SHORT);
                                            });
                        })
                .build()
                .show(getParentFragmentManager(), null);
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

    @Override
    public void onStart() {
        super.onStart();
        getViewModel().refreshGroupFollows();
    }
}
