package ai.nexconn.chatui.usermanage.friend.user.profile;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chat.user.model.UserOnlineStatus;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.handler.AppSettingsHandler;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.model.UiUserDetail;
import ai.nexconn.chatui.usermanage.friend.mine.nickname.UpdateNickNameActivity;
import ai.nexconn.chatui.usermanage.group.nickname.GroupNicknameActivity;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import ai.nexconn.chatui.utils.route.RouteUtils;
import ai.nexconn.chatui.utils.text.TextViewUtils;
import ai.nexconn.chatui.widget.CommonDialog;
import ai.nexconn.chatui.widget.SettingItemView;
import ai.nexconn.chatui.widget.SimpleInputDialog;
import ai.nexconn.chatui.widget.component.HeadComponent;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

/**
 * User profile page
 *
 * @since 5.12.0
 */
public class UserProfileFragment extends BaseViewModelFragment<UserProfileViewModel> {
    private static final String TAG = "UserProfileFragment";
    protected HeadComponent headComponent;
    private View nicknameContainer;
    protected View btnStartChat;
    protected View btnStartAudio;
    protected View btnStartVideo;
    protected View btnDeleteUser;
    protected Button btnAddFriend;
    private TextView tvDisplayName;
    private TextView tvNickname;
    private ImageView ivUserPortrait;
    private View llFriendActions;
    private View llNoFriendActions;
    private LinearLayout textContainer;

    /**
     * @since 5.12.2
     */
    private SettingItemView groupNicknameView;

    @NonNull
    @Override
    protected UserProfileViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(UserProfileViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup container,
            @NonNull Bundle args) {
        View rootView = inflater.inflate(R.layout.nc_page_user_profile, container, false);
        headComponent = rootView.findViewById(R.id.nc_head_component);
        nicknameContainer = rootView.findViewById(R.id.nickname_container);
        btnStartChat = rootView.findViewById(R.id.btn_start_chat);
        btnStartAudio = rootView.findViewById(R.id.btn_start_audio);
        btnStartVideo = rootView.findViewById(R.id.btn_start_video);
        btnDeleteUser = rootView.findViewById(R.id.btn_delete_user);
        btnAddFriend = rootView.findViewById(R.id.btn_add_friend);
        tvDisplayName = rootView.findViewById(R.id.tv_display_name);
        tvNickname = rootView.findViewById(R.id.tv_nickname);
        textContainer = rootView.findViewById(R.id.text_container);
        ivUserPortrait = rootView.findViewById(R.id.user_portrait);
        llFriendActions = rootView.findViewById(R.id.ll_friend_actions);
        llNoFriendActions = rootView.findViewById(R.id.ll_no_friend_actions);

        groupNicknameView = rootView.findViewById(R.id.siv_group_nickname);
        return rootView;
    }

    @Override
    protected void onViewReady(@NonNull UserProfileViewModel viewModel) {
        // Set back button click listener
        headComponent.setLeftClickListener(v -> finishActivity());

        // Set click listener
        nicknameContainer.setOnClickListener(
                v -> {
                    ContactModel contactModel = viewModel.getContactModelLiveData().getValue();
                    if (contactModel != null && contactModel.getBean() instanceof FriendDetail) {
                        startActivity(
                                UpdateNickNameActivity.newIntent(
                                        getContext(), (FriendDetail) contactModel.getBean()));
                    }
                });

        btnStartChat.setOnClickListener(
                v -> {
                    UiUserDetail detail = viewModel.getUiUserDetail();
                    if (detail != null) {
                        RouteUtils.routeToChannelActivity(
                                getContext(),
                                new ChannelIdentifier(
                                        ai.nexconn.chat.channel.ChannelType.DIRECT,
                                        detail.getUserId()));
                    }
                });

        btnDeleteUser.setOnClickListener(v -> deleteFromContact());
        btnAddFriend.setOnClickListener(v -> showAddFriendDialog());

        // Observe user profile data changes
        viewModel
                .getUserProfilesLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        userProfile -> {
                            // Update friend status button visibility
                            llFriendActions.setVisibility(
                                    userProfile.isFriend() ? View.VISIBLE : View.GONE);
                            llNoFriendActions.setVisibility(
                                    userProfile.isFriend() ? View.GONE : View.VISIBLE);
                            if (TextUtils.isEmpty(userProfile.getNickName())) {
                                tvDisplayName.setText(
                                        userProfile.getName() != null
                                                ? userProfile.getName()
                                                : getString(R.string.nc_unknow_type));
                                tvNickname.setVisibility(View.GONE);
                                updateTextContainerGravity(false);

                            } else {
                                tvNickname.setVisibility(View.VISIBLE);
                                tvDisplayName.setText(userProfile.getNickName());
                                tvNickname.setText(
                                        String.format(
                                                "%s: %s",
                                                getString(R.string.nc_nickname_label),
                                                userProfile.getName()));
                                updateTextContainerGravity(true);
                            }

                            // Update user avatar
                            NCChatUIConfig.featureConfig()
                                    .getChatUIImageEngine()
                                    .loadUserPortrait(
                                            getContext(),
                                            userProfile.getPortrait(),
                                            ivUserPortrait);
                        });

        if (groupNicknameView != null) {
            viewModel
                    .getMyGroupMemberInfoLiveData()
                    .observe(
                            getViewLifecycleOwner(),
                            groupMemberInfo -> {
                                if (groupMemberInfo != null) {
                                    groupNicknameView.setVisibility(View.VISIBLE);
                                    groupNicknameView.setValue(groupMemberInfo.getNickname());
                                }
                                groupNicknameView.setRightImageVisibility(
                                        viewModel.hasEditPermission() ? View.VISIBLE : View.GONE);
                            });
        }

        ChannelIdentifier channelIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        getArguments(), ChatUIConstants.KEY_CHANNEL_IDENTIFIER);
        String userId = getArguments().getString(ChatUIConstants.KEY_USER_ID);
        String currentUserId = NCEngine.getCurrentUserId();
        if (groupNicknameView != null && channelIdentifier != null) {
            groupNicknameView.setOnClickListener(
                    v -> {
                        if (viewModel.hasEditPermission()) {
                            startActivity(
                                    GroupNicknameActivity.newIntent(
                                            getContext(),
                                            channelIdentifier,
                                            userId,
                                            getString(R.string.nc_group_nickname)));
                        }
                    });
        }
        viewModel
                .getOnlineStatusLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        new Observer<UserOnlineStatus>() {
                            @Override
                            public void onChanged(UserOnlineStatus status) {
                                if (!TextUtils.equals(userId, currentUserId)) {
                                    setTitleOnlineStatus(status != null && status.isOnline());
                                }
                            }
                        });
    }

    private void setTitleOnlineStatus(boolean isOnline) {
        if (!AppSettingsHandler.getInstance().isOnlineStatusEnable()) {
            return;
        }
        int resId =
                ChatUIThemeManager.getAttrResId(
                        tvDisplayName.getContext(),
                        isOnline
                                ? R.attr.nc_user_online_status_img
                                : R.attr.nc_user_offline_status_img);
        TextViewUtils.setCompoundDrawables(tvDisplayName, Gravity.START, resId);
    }

    private void updateTextContainerGravity(boolean nicknameVisible) {
        if (textContainer == null) {
            return;
        }
        textContainer.setGravity(
                nicknameVisible
                        ? (Gravity.START | Gravity.TOP)
                        : (Gravity.START | Gravity.CENTER_VERTICAL));
    }

    private void showAddFriendDialog() {
        SimpleInputDialog dialog = new SimpleInputDialog();
        dialog.setInputHint(getString(R.string.nc_add_friend_hint));
        dialog.setTitleText(getString(R.string.nc_add_as_friend));
        dialog.setInputDialogListener(
                input -> {
                    String inviteMsg = input.getText().toString();
                    getViewModel()
                            .applyFriend(
                                    inviteMsg,
                                    code -> {
                                        if (code == 0 || code == 25461) {
                                            ToastUtils.show(
                                                    getContext(),
                                                    getString(R.string.nc_send_apply_success),
                                                    Toast.LENGTH_SHORT);
                                            getViewModel().getUserProfile();
                                        } else {
                                            ToastUtils.show(
                                                    getContext(),
                                                    getString(R.string.nc_send_apply_fail),
                                                    Toast.LENGTH_SHORT);
                                        }
                                    });
                    return true;
                });
        dialog.show(getParentFragmentManager(), null);
    }

    private void deleteFromContact() {
        CommonDialog dialog =
                new CommonDialog.Builder()
                        .setContentMessage(
                                getString(R.string.nc_delete_friend_title, getDeleteFriendName()))
                        .setDialogButtonClickListener(
                                new CommonDialog.OnDialogButtonClickListener() {
                                    @Override
                                    public void onPositiveClick(View v, Bundle bundle) {
                                        getViewModel()
                                                .deleteFriend(
                                                        result -> {
                                                            if (result) {
                                                                clearFriendConversationAndMessages();
                                                                ToastUtils.show(
                                                                        getContext(),
                                                                        getString(
                                                                                R.string
                                                                                        .nc_delete_friend_success),
                                                                        Toast.LENGTH_SHORT);
                                                                finishActivity();
                                                            } else {
                                                                ToastUtils.show(
                                                                        getContext(),
                                                                        getString(
                                                                                R.string
                                                                                        .nc_delete_friend_failed),
                                                                        Toast.LENGTH_SHORT);
                                                            }
                                                        });
                                    }

                                    @Override
                                    public void onNegativeClick(View v, Bundle bundle) {}
                                })
                        .build();
        dialog.show(getParentFragmentManager(), null);
    }

    @Override
    public void onStart() {
        super.onStart();
        getViewModel().getUserProfile();
    }

    private String getDeleteFriendName() {
        UiUserDetail detail = getViewModel().getUiUserDetail();
        if (detail == null) {
            return "";
        }
        if (!TextUtils.isEmpty(detail.getNickName())) {
            return detail.getNickName();
        }
        if (!TextUtils.isEmpty(detail.getName())) {
            return detail.getName();
        }
        return "";
    }

    private void clearFriendConversationAndMessages() {
        UiUserDetail detail = getViewModel().getUiUserDetail();
        if (detail == null || TextUtils.isEmpty(detail.getUserId())) {
            return;
        }
        String targetId = detail.getUserId();
        ChannelIdentifier identifier =
                new ChannelIdentifier(ai.nexconn.chat.channel.ChannelType.DIRECT, targetId);
        java.util.List<ChannelIdentifier> identifiers =
                java.util.Collections.singletonList(identifier);
        ai.nexconn.chat.channel.BaseChannel.getChannels(
                identifiers,
                (channels, error) -> {
                    if (channels != null && !channels.isEmpty()) {
                        channels.get(0)
                                .deleteMessagesForMeByTimestamp(
                                        new ai.nexconn.chat.params
                                                .DeleteMessagesForMeByTimestampParams(
                                                0L,
                                                ai.nexconn.chat.channel.model.MessageOperationPolicy
                                                        .LOCAL_REMOTE),
                                        null);
                    }
                    ai.nexconn.chat.channel.BaseChannel.deleteChannels(identifiers, null);
                });
    }
}
