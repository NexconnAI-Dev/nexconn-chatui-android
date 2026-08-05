package ai.nexconn.chatui.usermanage.group.memberlist;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.usermanage.component.ContactListComponent;
import ai.nexconn.chatui.usermanage.friend.mine.profile.MyProfileActivity;
import ai.nexconn.chatui.usermanage.friend.user.profile.UserProfileActivity;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import ai.nexconn.chatui.widget.component.HeadComponent;
import ai.nexconn.chatui.widget.component.SearchComponent;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import java.util.Objects;

/**
 * Fragment for group member list.
 *
 * @since 5.12.0
 */
public class GroupMemberListFragment extends BaseViewModelFragment<GroupMemberListViewModel> {

    protected HeadComponent headComponent;
    protected SearchComponent searchComponent;
    protected ContactListComponent memberListComponent;

    @NonNull
    @Override
    protected GroupMemberListViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupMemberListViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_group_member_list, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        searchComponent = view.findViewById(R.id.nc_search_component);
        memberListComponent = view.findViewById(R.id.nc_member_list_component);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupMemberListViewModel viewModel) {
        ChannelIdentifier conversationIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        getArguments(), ChatUIConstants.KEY_CHANNEL_IDENTIFIER);

        headComponent.setLeftClickListener(v -> finishActivity());

        searchComponent.setSearchQueryListener(viewModel::queryContacts);

        memberListComponent.setOnPageDataLoader(viewModel);
        memberListComponent.setEnableLoadMore(true);
        // Set contact list click event
        memberListComponent.setOnItemClickListener(
                contactModel -> {
                    if (contactModel.getBean() instanceof GroupMemberInfo) {
                        GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contactModel.getBean();
                        String currentUserId = NCEngine.getCurrentUserId();
                        if (Objects.equals(groupMemberInfo.getUserId(), currentUserId)) {
                            startActivity(MyProfileActivity.newIntent(getContext()));
                        } else {
                            startActivity(
                                    UserProfileActivity.newIntent(
                                            getContext(),
                                            groupMemberInfo.getUserId(),
                                            conversationIdentifier));
                        }
                    }
                });

        // Observe total member count changes in ViewModel
        viewModel
                .getGroupInfoLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        groupInfo -> {
                            if (groupInfo != null) {
                                headComponent.setTitleText(
                                        getString(
                                                R.string.nc_group_members_label,
                                                groupInfo.getMembersCount()));
                            }
                        });

        // Observe contact list changes in ViewModel
        viewModel
                .getFilteredContactsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null && memberListComponent != null) {
                                memberListComponent.setContactList(contactModels);
                            }
                        });
    }

    @Override
    public void onStart() {
        super.onStart();
        getViewModel().refreshGroupMembers();
    }
}
