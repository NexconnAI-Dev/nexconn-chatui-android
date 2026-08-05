package ai.nexconn.chatui.usermanage.group.notice;

import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chat.channel.model.GroupOperationPermission;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import ai.nexconn.chatui.widget.CommonDialog;
import ai.nexconn.chatui.widget.component.HeadComponent;
import ai.nexconn.chatui.widget.dialog.TipLoadingDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import java.util.List;

/**
 * Fragment for group notice/announcement.
 *
 * @since 5.12.0
 */
public class GroupNoticeFragment extends BaseViewModelFragment<GroupNoticeViewModel> {

    protected HeadComponent headComponent;
    private EditText etGroupNotice;
    private LinearLayout llGroupNoticeInput;

    /**
     * @since 5.12.2
     */
    private TextView tvEditPermission;

    private LinearLayout llGroupNoticeDisplay;
    private LinearLayout llEmptyNotice;
    private TextView tvNoticeContent;
    private TipLoadingDialog dialog;

    @NonNull
    @Override
    protected GroupNoticeViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupNoticeViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_group_notice, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        etGroupNotice = view.findViewById(R.id.group_notice_input);
        llGroupNoticeInput = view.findViewById(R.id.ll_group_notice_input);
        tvEditPermission = view.findViewById(R.id.tv_edit_permission);
        llGroupNoticeDisplay = view.findViewById(R.id.ll_group_notice_display);
        tvNoticeContent = view.findViewById(R.id.tv_notice_content);
        llEmptyNotice = view.findViewById(R.id.ll_empty_notice);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupNoticeViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        GroupInfo groupInfo =
                ChatUIBundleUtils.getGroupInfo(getArguments(), ChatUIConstants.KEY_GROUP_INFO);
        boolean canEditNotice =
                (groupInfo.getGroupInfoEditPermission() == GroupOperationPermission.EVERYONE)
                        || (groupInfo.getGroupInfoEditPermission()
                                        == GroupOperationPermission.OWNER_OR_ADMIN
                                && (groupInfo.getRole() == GroupMemberRole.ADMIN
                                        || groupInfo.getRole() == GroupMemberRole.OWNER))
                        || (groupInfo.getGroupInfoEditPermission() == GroupOperationPermission.OWNER
                                && groupInfo.getRole() == GroupMemberRole.OWNER);
        if (canEditNotice) {
            headComponent.setRightClickListener(
                    v -> {
                        String newNotice = etGroupNotice.getText().toString().trim();
                        GroupInfo updatedGroupInfo =
                                new GroupInfo(groupInfo.getGroupId(), null, null, null, newNotice);
                        onConfirmGroupNoticeUpdate(viewModel, updatedGroupInfo);
                    });
            llGroupNoticeInput.setVisibility(View.VISIBLE);
            llGroupNoticeDisplay.setVisibility(View.GONE);
            headComponent.setRightTextViewEnable(false);
            etGroupNotice.setText(groupInfo.getNotice());
        } else {
            headComponent.getRightTextView().setVisibility(View.GONE);
            headComponent.setRightTextViewEnable(false);
            llGroupNoticeInput.setVisibility(View.GONE);
            llGroupNoticeDisplay.setVisibility(View.VISIBLE);
            if (groupInfo.getNotice() != null && !groupInfo.getNotice().isEmpty()) {
                tvNoticeContent.setText(groupInfo.getNotice());
                tvNoticeContent.setVisibility(View.VISIBLE);
                llEmptyNotice.setVisibility(View.GONE);
            } else {
                tvNoticeContent.setVisibility(View.GONE);
                llEmptyNotice.setVisibility(View.VISIBLE);
            }
            if (tvEditPermission != null) {
                tvEditPermission.setText(
                        getString(
                                groupInfo.getGroupInfoEditPermission()
                                                == GroupOperationPermission.OWNER
                                        ? R.string.nc_group_edit_permission_owner_only
                                        : R.string.nc_edit_permission));
            }
        }

        etGroupNotice.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (headComponent != null) {
                            headComponent.setRightTextViewEnable(true);
                        }
                    }
                });
    }

    /**
     * Confirm group notice update.
     *
     * @param viewModel group notice ViewModel
     * @param groupInfo group info
     */
    protected void onConfirmGroupNoticeUpdate(
            @NonNull GroupNoticeViewModel viewModel, GroupInfo groupInfo) {
        // Show confirmation dialog
        new CommonDialog.Builder()
                .setContentMessage(getString(R.string.nc_publish_announcement_hint))
                .setDialogButtonClickListener(
                        (v, bundle) -> {
                            showLoadingDialog();
                            viewModel.updateGroupNotice(
                                    groupInfo,
                                    new OnDataChangeEnhancedListener<Boolean>() {
                                        @Override
                                        public void onDataChange(Boolean aBoolean) {
                                            onGroupNoticeUpdateResult(
                                                    groupInfo.getGroupId(),
                                                    groupInfo.getNotice(),
                                                    null);
                                        }

                                        @Override
                                        public void onDataError(
                                                NCError error, List<String> errorKeys) {
                                            onGroupNoticeUpdateResult(
                                                    groupInfo.getGroupId(),
                                                    groupInfo.getNotice(),
                                                    error);
                                        }
                                    });
                        })
                .build()
                .show(getParentFragmentManager(), null);
    }

    /**
     * Handle group notice update result.
     *
     * @param groupId group ID
     * @param notice group notice
     * @param isSuccess whether the update succeeded
     */
    protected void onGroupNoticeUpdateResult(String groupId, String notice, NCError error) {
        dismissLoadingDialog();
        if (error == null) {
            ToastUtils.show(
                    getActivity(), getString(R.string.nc_group_notice_success), Toast.LENGTH_SHORT);
            finishActivity();
        } else {
            String tips = getString(R.string.nc_group_notice_failed);
            if (error.getCode() == 25480) {
                tips = getString(R.string.nc_content_contain_sensitive);
            }
            ToastUtils.show(getActivity(), tips, Toast.LENGTH_SHORT);
        }
        onGroupNoticeUpdateResult(groupId, notice, (error == null));
    }

    protected void onGroupNoticeUpdateResult(String groupId, String notice, boolean isSuccess) {}

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
