package ai.nexconn.chatui.usermanage.group.mention;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.channel.feature.mention.NCMentionManager;
import ai.nexconn.chatui.userinfo.model.ExtendedUserInfo;
import ai.nexconn.chatui.usermanage.component.ContactListComponent;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import ai.nexconn.chatui.widget.component.HeadComponent;
import ai.nexconn.chatui.widget.component.SearchComponent;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import java.util.Collections;

/**
 * Fragment for group mention member selection.
 *
 * @since 5.12.2
 */
public class GroupMentionFragment extends BaseViewModelFragment<GroupMentionViewModel> {

    protected HeadComponent headComponent;
    protected SearchComponent searchComponent;
    protected ContactListComponent memberListComponent;
    private TextView emptyView;
    private View mentionAllHeaderView;
    private ChannelIdentifier conversationIdentifier;

    @NonNull
    @Override
    protected GroupMentionViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupMentionViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_group_mention_list, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        searchComponent = view.findViewById(R.id.nc_search_component);
        memberListComponent = view.findViewById(R.id.nc_group_list_component);
        emptyView = view.findViewById(R.id.nc_empty_tv);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupMentionViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        searchComponent.setSearchQueryListener(viewModel::queryGroupMembers);
        onBindContactListComponent(memberListComponent, viewModel);
    }

    protected void onBindContactListComponent(
            @NonNull ContactListComponent memberListComponent,
            @NonNull GroupMentionViewModel viewModel) {
        ChannelIdentifier conversationIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        getArguments(), ChatUIConstants.KEY_CHANNEL_IDENTIFIER);
        this.conversationIdentifier = conversationIdentifier;
        if (this.conversationIdentifier == null) {
            return;
        }

        memberListComponent.setOnPageDataLoader(viewModel);
        memberListComponent.setEnableLoadMore(true);

        viewModel
                .getMentionAllRoleLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        role -> updateMentionAllHeader(role, this.conversationIdentifier));

        viewModel
                .getFilteredContactsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null && !contactModels.isEmpty()) {
                                emptyView.setVisibility(View.GONE);
                                memberListComponent.setVisibility(View.VISIBLE);
                                memberListComponent.post(
                                        () -> memberListComponent.setContactList(contactModels));
                            } else {
                                emptyView.setVisibility(View.VISIBLE);
                                memberListComponent.setVisibility(View.GONE);
                            }
                        });

        memberListComponent.setOnItemClickListener(
                contactModel -> {
                    if (contactModel.getBean() instanceof GroupMemberInfo) {
                        GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contactModel.getBean();
                        onMention(conversationIdentifier.getChannelId(), groupMemberInfo);
                    }
                });
    }

    private void updateMentionAllHeader(
            @Nullable GroupMemberRole role, @Nullable ChannelIdentifier conversation) {
        boolean shouldShow = enableMentionAll(conversation, role);
        if (mentionAllHeaderView == null && shouldShow) {
            mentionAllHeaderView =
                    LayoutInflater.from(getContext())
                            .inflate(
                                    R.layout.nc_item_group_mention_all, memberListComponent, false);
            mentionAllHeaderView.setOnClickListener(v -> onMentionAll());
            memberListComponent.addHeaderView(mentionAllHeaderView);
        }
        if (mentionAllHeaderView != null) {
            mentionAllHeaderView.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Whether to show the @All feature; subclasses may override.
     *
     * @return true to show, false to hide
     */
    private boolean enableMentionAll(
            @NonNull ChannelIdentifier conversationIdentifier,
            @Nullable GroupMemberRole currentUserRole) {
        return true;
    }

    private void onMentionAll() {
        finishActivity();
        ai.nexconn.chat.user.model.UserInfo ncUserInfo =
                new ai.nexconn.chat.user.model.UserInfo(
                        NCMentionManager.MENTION_ALL_USER_ID,
                        0,
                        getString(R.string.nc_group_mention_all_members),
                        null,
                        null,
                        null);
        NCMentionManager.getInstance().mentionMember(ncUserInfo);
    }

    /**
     * Mention a group member.
     *
     * @param groupId group ID
     * @param groupMemberInfo group member info
     */
    protected void onMention(String groupId, GroupMemberInfo groupMemberInfo) {
        if (TextUtils.equals(groupMemberInfo.getUserId(), NCMentionManager.MENTION_ALL_USER_ID)) {
            onMentionAll();
            return;
        }

        NCEngine.getUserModule()
                .getUserProfiles(
                        Collections.singletonList(groupMemberInfo.getUserId()),
                        (userProfiles, error) -> {
                            if (error == null) {
                                finishActivity();
                                if (userProfiles != null && !userProfiles.isEmpty()) {
                                    ai.nexconn.chat.user.model.UserProfile ncProfile =
                                            userProfiles.get(0);
                                    ai.nexconn.chat.user.model.UserInfo ncUserInfo =
                                            new ai.nexconn.chat.user.model.UserInfo(
                                                    ncProfile.getUserId(),
                                                    0,
                                                    ncProfile.getName(),
                                                    ncProfile.getPortraitUri(),
                                                    null,
                                                    null);
                                    ExtendedUserInfo extUserInfo =
                                            ExtendedUserInfo.obtain(ncUserInfo);
                                    if (!TextUtils.isEmpty(groupMemberInfo.getNickname())) {
                                        extUserInfo.setName(groupMemberInfo.getNickname());
                                    }
                                    NCMentionManager.getInstance()
                                            .mentionMember(extUserInfo.toUserInfo());
                                }
                            }
                        });
    }

    @Override
    public void onStart() {
        super.onStart();
        getViewModel().refreshGroupManagerList();
    }
}
