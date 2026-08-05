package ai.nexconn.chatui.usermanage.group.manage;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.channel.model.GroupJoinPermission;
import ai.nexconn.chat.channel.model.GroupMemberInfoEditPermission;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chat.channel.model.GroupOperationPermission;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.base.interfaces.OnActionClickListener;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.picture.tools.ScreenUtils;
import ai.nexconn.chatui.usermanage.group.managerlist.GroupManagerListActivity;
import ai.nexconn.chatui.usermanage.group.transfer.GroupTransferActivity;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import ai.nexconn.chatui.widget.SettingItemView;
import ai.nexconn.chatui.widget.component.HeadComponent;
import ai.nexconn.chatui.widget.dialog.TipLoadingDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment for group management.
 *
 * @since 5.12.2
 */
public class GroupManagementFragment extends BaseViewModelFragment<GroupManagementViewModel> {

    protected HeadComponent headComponent;
    protected SettingItemView groupAdminView;
    protected SettingItemView groupEditInfoPermissionView;
    protected SettingItemView groupAddMemberPermissionView;
    protected SettingItemView groupRemoveMemberPermissionView;
    protected SettingItemView groupEditMemberInfoPermissionView;
    protected SettingItemView groupInvitationConfirmationView;
    protected SettingItemView groupTransferView;
    private TipLoadingDialog dialog;

    @NonNull
    @Override
    protected GroupManagementViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupManagementViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_group_management, container, false);

        // Initialize components
        headComponent = view.findViewById(R.id.nc_head_component);
        groupAdminView = view.findViewById(R.id.siv_group_admin);
        groupEditInfoPermissionView = view.findViewById(R.id.siv_group_edit_info_permission);
        groupAddMemberPermissionView = view.findViewById(R.id.siv_group_add_member_permission);
        groupRemoveMemberPermissionView =
                view.findViewById(R.id.siv_group_remove_member_permission);
        groupEditMemberInfoPermissionView =
                view.findViewById(R.id.siv_group_edit_member_info_permission);
        groupInvitationConfirmationView = view.findViewById(R.id.siv_group_invitation_confirmation);
        groupTransferView = view.findViewById(R.id.siv_group_transfer);

        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupManagementViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());

        // Group admin settings click event
        groupAdminView.setOnClickListener(
                v -> {
                    GroupInfo groupInfo = viewModel.getGroupInfoLiveData().getValue();
                    if (groupInfo != null
                            && (groupInfo.getRole() == GroupMemberRole.OWNER
                                    || groupInfo.getRole() == GroupMemberRole.ADMIN)) {
                        ChannelIdentifier conversationIdentifier =
                                ChatUIBundleUtils.getChannelIdentifier(
                                        getArguments(), ChatUIConstants.KEY_CHANNEL_IDENTIFIER);
                        startActivity(
                                GroupManagerListActivity.newIntent(
                                        getContext(), conversationIdentifier, groupInfo.getRole()));
                    }
                });

        // Edit group info permission click event
        groupEditInfoPermissionView.setOnClickListener(
                v -> {
                    Map<GroupOperationPermission, String> map = new LinkedHashMap<>();
                    map.put(
                            GroupOperationPermission.OWNER,
                            getLocalizedPermissionLabel(GroupOperationPermission.OWNER));
                    map.put(
                            GroupOperationPermission.OWNER_OR_ADMIN,
                            getLocalizedPermissionLabel(GroupOperationPermission.OWNER_OR_ADMIN));
                    map.put(
                            GroupOperationPermission.EVERYONE,
                            getLocalizedPermissionLabel(GroupOperationPermission.EVERYONE));
                    showPermissionSelectionDialog(
                            getGroupInfoEditPermissionLabels(map),
                            selectedPermission -> {
                                GroupInfo groupInfo = viewModel.getGroupInfoLiveData().getValue();
                                if (groupInfo != null) {
                                    GroupInfo updatedGroupInfo =
                                            new GroupInfo(
                                                    groupInfo.getGroupId(),
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    selectedPermission);
                                    updateGroupInfo(viewModel, updatedGroupInfo);
                                }
                            });
                });

        // Add group member permission click event
        groupAddMemberPermissionView.setOnClickListener(
                v -> {
                    Map<GroupOperationPermission, String> map = new LinkedHashMap<>();
                    map.put(
                            GroupOperationPermission.OWNER,
                            getLocalizedPermissionLabel(GroupOperationPermission.OWNER));
                    map.put(
                            GroupOperationPermission.OWNER_OR_ADMIN,
                            getLocalizedPermissionLabel(GroupOperationPermission.OWNER_OR_ADMIN));
                    map.put(
                            GroupOperationPermission.EVERYONE,
                            getLocalizedPermissionLabel(GroupOperationPermission.EVERYONE));
                    showPermissionSelectionDialog(
                            getAddMemberPermissionLabels(map),
                            selectedPermission -> {
                                GroupInfo groupInfo = viewModel.getGroupInfoLiveData().getValue();
                                if (groupInfo != null) {
                                    GroupInfo updatedGroupInfo =
                                            new GroupInfo(
                                                    groupInfo.getGroupId(),
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    selectedPermission);
                                    updateGroupInfo(viewModel, updatedGroupInfo);
                                }
                            });
                });

        // Remove group member permission click event
        groupRemoveMemberPermissionView.setOnClickListener(
                v -> {
                    Map<GroupOperationPermission, String> map = new LinkedHashMap<>();
                    map.put(
                            GroupOperationPermission.OWNER,
                            getLocalizedPermissionLabel(GroupOperationPermission.OWNER));
                    map.put(
                            GroupOperationPermission.OWNER_OR_ADMIN,
                            getLocalizedPermissionLabel(GroupOperationPermission.OWNER_OR_ADMIN));
                    map.put(
                            GroupOperationPermission.EVERYONE,
                            getLocalizedPermissionLabel(GroupOperationPermission.EVERYONE));
                    showPermissionSelectionDialog(
                            getRemoveMemberPermissionLabels(map),
                            selectedPermission -> {
                                GroupInfo groupInfo = viewModel.getGroupInfoLiveData().getValue();
                                if (groupInfo != null) {
                                    GroupInfo updatedGroupInfo =
                                            new GroupInfo(
                                                    groupInfo.getGroupId(),
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    selectedPermission);
                                    updateGroupInfo(viewModel, updatedGroupInfo);
                                }
                            });
                });

        // Edit group member info permission click event
        groupEditMemberInfoPermissionView.setOnClickListener(
                v -> {
                    Map<GroupMemberInfoEditPermission, String> map = new LinkedHashMap<>();
                    map.put(
                            GroupMemberInfoEditPermission.OWNER_OR_SELF,
                            getLocalizedPermissionLabel(
                                    GroupMemberInfoEditPermission.OWNER_OR_SELF));
                    map.put(
                            GroupMemberInfoEditPermission.OWNER_OR_ADMIN_OR_SELF,
                            getLocalizedPermissionLabel(
                                    GroupMemberInfoEditPermission.OWNER_OR_ADMIN_OR_SELF));
                    showPermissionSelectionDialog(
                            getMemberInfoEditPermissionLabels(map),
                            selectedPermission -> {
                                GroupInfo groupInfo = viewModel.getGroupInfoLiveData().getValue();
                                if (groupInfo != null) {
                                    GroupInfo updatedGroupInfo =
                                            new GroupInfo(
                                                    groupInfo.getGroupId(),
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    selectedPermission);
                                    updateGroupInfo(viewModel, updatedGroupInfo);
                                }
                            });
                });

        // Group invitation confirmation toggle event
        groupInvitationConfirmationView.setSwitchCheckListener(
                (buttonView, isChecked) -> {
                    GroupInfo groupInfo = viewModel.getGroupInfoLiveData().getValue();
                    if (groupInfo != null) {
                        GroupInfo updatedGroupInfo =
                                new GroupInfo(
                                        groupInfo.getGroupId(),
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        isChecked
                                                ? GroupJoinPermission.OWNER_OR_ADMIN_VERIFY
                                                : GroupJoinPermission.FREE);
                        viewModel.updateGroupInfo(
                                updatedGroupInfo,
                                isSuccess -> {
                                    if (!isSuccess) {
                                        groupInvitationConfirmationView
                                                .setCheckedImmediatelyWithOutEvent(!isChecked);
                                        ToastUtils.show(
                                                getActivity(),
                                                getString(R.string.nc_set_failed),
                                                Toast.LENGTH_SHORT);
                                    } else {
                                        viewModel.refreshGroupInfo();
                                    }
                                });
                    }
                });

        // Group transfer click event
        groupTransferView.setOnClickListener(
                v -> {
                    ChannelIdentifier conversationIdentifier =
                            ChatUIBundleUtils.getChannelIdentifier(
                                    getArguments(), ChatUIConstants.KEY_CHANNEL_IDENTIFIER);
                    startActivity(
                            GroupTransferActivity.newIntent(
                                    getContext(), conversationIdentifier, GroupMemberRole.UNDEF));
                });

        // Real-time UI update
        viewModel
                .getGroupInfoLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        groupInfo -> {
                            if (groupInfo != null) {
                                boolean isOwner = groupInfo.getRole() == GroupMemberRole.OWNER;
                                groupTransferView.setVisibility(isOwner ? View.VISIBLE : View.GONE);
                                groupEditInfoPermissionView.setVisibility(
                                        isOwner ? View.VISIBLE : View.GONE);
                                groupEditInfoPermissionView.setValue(
                                        getLocalizedPermissionLabel(
                                                groupInfo.getGroupInfoEditPermission()));
                                groupEditInfoPermissionView.setRightImageVisibility(
                                        isOwner ? View.VISIBLE : View.GONE);

                                groupAddMemberPermissionView.setVisibility(
                                        isOwner ? View.VISIBLE : View.GONE);
                                groupAddMemberPermissionView.setValue(
                                        getLocalizedPermissionLabel(
                                                groupInfo.getInvitePermission()));

                                groupRemoveMemberPermissionView.setVisibility(
                                        isOwner ? View.VISIBLE : View.GONE);
                                groupRemoveMemberPermissionView.setValue(
                                        getLocalizedPermissionLabel(
                                                groupInfo.getRemoveMemberPermission()));

                                groupEditMemberInfoPermissionView.setVisibility(
                                        isOwner ? View.VISIBLE : View.GONE);
                                groupEditMemberInfoPermissionView.setValue(
                                        getLocalizedPermissionLabel(
                                                groupInfo.getMemberInfoEditPermission()));

                                groupInvitationConfirmationView.setVisibility(
                                        isOwner ? View.VISIBLE : View.GONE);
                                groupInvitationConfirmationView.setCheckedImmediatelyWithOutEvent(
                                        groupInfo.getJoinPermission()
                                                == GroupJoinPermission.OWNER_OR_ADMIN_VERIFY);
                            }
                        });
    }

    protected Map<GroupMemberInfoEditPermission, String> getMemberInfoEditPermissionLabels(
            Map<GroupMemberInfoEditPermission, String> map) {
        return map;
    }

    protected Map<GroupOperationPermission, String> getRemoveMemberPermissionLabels(
            Map<GroupOperationPermission, String> map) {
        return map;
    }

    protected Map<GroupOperationPermission, String> getAddMemberPermissionLabels(
            Map<GroupOperationPermission, String> map) {
        return map;
    }

    protected Map<GroupOperationPermission, String> getGroupInfoEditPermissionLabels(
            Map<GroupOperationPermission, String> map) {
        return map;
    }

    @NonNull
    protected <T extends Enum<T>> String getLocalizedPermissionLabel(@NonNull T permission) {
        if (permission instanceof GroupOperationPermission) {
            switch ((GroupOperationPermission) permission) {
                case OWNER:
                    return getString(R.string.nc_group_permission_owner_only);
                case OWNER_OR_ADMIN:
                    return getString(R.string.nc_group_permission_owner_and_admins);
                case EVERYONE:
                    return getString(R.string.nc_group_permission_all);
            }
        } else if (permission instanceof GroupMemberInfoEditPermission) {
            switch ((GroupMemberInfoEditPermission) permission) {
                case OWNER_OR_SELF:
                    return getString(R.string.nc_group_permission_owner_only);
                case OWNER_OR_ADMIN_OR_SELF:
                    return getString(R.string.nc_group_permission_owner_and_admins);
            }
        }
        return "";
    }

    private void updateGroupInfo(
            @NonNull GroupManagementViewModel viewModel, @NonNull GroupInfo groupInfo) {
        viewModel.updateGroupInfo(
                groupInfo,
                new OnDataChangeEnhancedListener<Boolean>() {
                    @Override
                    public void onDataChange(Boolean isSuccess) {
                        dismissLoadingDialog();
                        if (isSuccess) {
                            ToastUtils.show(
                                    getContext(),
                                    getString(R.string.nc_set_success),
                                    Toast.LENGTH_SHORT);
                            viewModel.refreshGroupInfo();
                        }
                    }

                    @Override
                    public void onDataError(NCError error, List<String> errorKeys) {
                        String tips = getString(R.string.nc_set_failed);
                        if (error != null && error.getCode() == 25480) {
                            tips = getString(R.string.nc_content_contain_sensitive);
                        }
                        ToastUtils.show(getContext(), tips, Toast.LENGTH_SHORT);
                    }
                });
    }

    private <T extends Enum<T>> void showPermissionSelectionDialog(
            Map<T, String> permissionLabels, OnActionClickListener<T> listener) {
        Dialog dialog = new Dialog(requireContext());

        // Main container
        LinearLayout mainContainer = new LinearLayout(requireContext());
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        int horizontalMargin = ScreenUtils.dip2px(requireContext(), 18);
        mainContainer.setPadding(
                horizontalMargin,
                0,
                horizontalMargin,
                (int) (34 * requireContext().getResources().getDisplayMetrics().density));

        // Options container
        LinearLayout optionsContainer = new LinearLayout(requireContext());
        optionsContainer.setOrientation(LinearLayout.VERTICAL);
        optionsContainer.setBackgroundResource(
                R.drawable.nc_bottom_dialog_background_color_radius_10);

        // Add options
        int index = 0;
        for (Map.Entry<T, String> entry : permissionLabels.entrySet()) {
            T permission = entry.getKey();
            String label = entry.getValue();

            // Add divider if not the first option
            if (index > 0) {
                View divider = new View(requireContext());
                LinearLayout.LayoutParams dividerParams =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                (int)
                                        (1
                                                * requireContext()
                                                        .getResources()
                                                        .getDisplayMetrics()
                                                        .density));
                int dividerMargin =
                        (int) (10 * requireContext().getResources().getDisplayMetrics().density);
                dividerParams.setMargins(dividerMargin, 0, dividerMargin, 0);
                divider.setLayoutParams(dividerParams);
                divider.setBackgroundColor(
                        ChatUIThemeManager.getColorFromAttrId(
                                divider.getContext(),
                                R.attr.nc_line_background_color)); // rgba(255, 255, 255, 0.1)
                optionsContainer.addView(divider);
            }

            optionsContainer.addView(
                    createPermissionOptionView(
                            label,
                            v -> {
                                listener.onActionClick(permission);
                                dialog.dismiss();
                            }));
            index++;
        }

        mainContainer.addView(optionsContainer);

        // Add cancel button
        TextView cancelView =
                createCancelButtonView(
                        requireContext().getString(R.string.nc_cancel), v -> dialog.dismiss());
        mainContainer.addView(cancelView);

        dialog.setContentView(mainContainer);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            // Set background dim effect (overlay)
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.5f); // Overlay opacity per Figma design: 0.5
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        // Add animation after Dialog is shown
        mainContainer.post(() -> playBottomToTopAnimation(mainContainer));
        dialog.show();
    }

    /** Create a permission option view. */
    private TextView createPermissionOptionView(String text, View.OnClickListener clickListener) {
        TextView textView = new TextView(requireContext());
        textView.setText(text);
        textView.setTextSize(14); // 14sp per Figma design
        textView.setGravity(Gravity.CENTER);

        // Height: 49dp
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        ScreenUtils.dip2px(requireContext(), 49));
        textView.setLayoutParams(params);

        textView.setBackgroundResource(R.drawable.nc_bottom_dialog_background_color_radius_10);
        textView.setOnClickListener(clickListener);
        textView.setTextColor(
                ChatUIThemeManager.getColorFromAttrId(
                        textView.getContext(), R.attr.nc_text_primary_color));
        return textView;
    }

    /** Create the cancel button view. */
    private TextView createCancelButtonView(String text, View.OnClickListener clickListener) {
        TextView textView = new TextView(requireContext());
        textView.setText(text);
        textView.setTextSize(14); // 14sp per Figma design
        textView.setGravity(Gravity.CENTER);

        // Height: 49dp
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        ScreenUtils.dip2px(requireContext(), 49));
        params.setMargins(0, ScreenUtils.dip2px(requireContext(), 12), 0, 0);
        textView.setLayoutParams(params);

        textView.setBackgroundResource(R.drawable.nc_bottom_dialog_background_color_radius_10);
        textView.setOnClickListener(clickListener);
        textView.setTextColor(
                ChatUIThemeManager.getColorFromAttrId(
                        textView.getContext(), R.attr.nc_text_primary_color));
        return textView;
    }

    private void playBottomToTopAnimation(View view) {
        // Create bottom-to-top slide animation
        TranslateAnimation slideUp =
                new TranslateAnimation(
                        Animation.RELATIVE_TO_PARENT, 0f, // From horizontal position
                        Animation.RELATIVE_TO_PARENT, 0f, // To target horizontal position
                        Animation.RELATIVE_TO_PARENT, 1f, // From screen bottom
                        Animation.RELATIVE_TO_PARENT, 0f // To own position
                        );
        slideUp.setDuration(150); // Animation duration
        slideUp.setInterpolator(new DecelerateInterpolator()); // Decelerate interpolator
        view.startAnimation(slideUp);
    }

    /** loading dialog */
    private void showLoadingDialog() {
        if (dialog != null) {
            dismissLoadingDialog();
        }
        dialog = new TipLoadingDialog(getContext());
        dialog.setTips(getString(R.string.nc_loading_saving));
        dialog.show();
    }

    /** dismiss dialog */
    private void dismissLoadingDialog() {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
                dialog = null;
            }
        } catch (Exception e) {
            dialog = null;
        }
    }
}
