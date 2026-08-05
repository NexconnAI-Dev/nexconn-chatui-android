package ai.nexconn.chatui.usermanage.group.create;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.usermanage.friend.select.FriendSelectFragment;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.route.RouteUtils;
import ai.nexconn.chatui.widget.component.HeadComponent;
import ai.nexconn.chatui.widget.dialog.TipLoadingDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import java.util.List;

/**
 * Fragment for creating a group.
 *
 * @since 5.12.0
 */
public class GroupCreateFragment extends BaseViewModelFragment<GroupCreateViewModel> {

    protected HeadComponent headComponent;
    protected EditText etGroupName;
    protected ImageView ivGroupIcon;
    protected Button btnCreateGroup;

    protected TipLoadingDialog dialog;

    @NonNull
    @Override
    protected GroupCreateViewModel onCreateViewModel(@NonNull Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(getArguments()))
                .get(GroupCreateViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_group_create, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        etGroupName = view.findViewById(R.id.et_group_name);
        ivGroupIcon = view.findViewById(R.id.iv_group_icon);
        btnCreateGroup = view.findViewById(R.id.btn_create_group);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupCreateViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        headComponent.setRightTextViewEnable(false);

        NCChatUIConfig.featureConfig()
                .getChatUIImageEngine()
                .loadGroupPortrait(ivGroupIcon.getContext(), null, ivGroupIcon);

        btnCreateGroup.setOnClickListener(
                v -> {
                    if (etGroupName != null) {
                        String groupName = etGroupName.getText().toString().trim();
                        if (TextUtils.isEmpty(groupName)) {
                            ToastUtils.show(
                                    getContext(),
                                    getString(R.string.nc_group_name_cannot_be_empty),
                                    Toast.LENGTH_SHORT);
                            return;
                        }
                        if (TextUtils.isEmpty(groupName) || groupName.length() > 64) {
                            ToastUtils.show(
                                    getContext(),
                                    getString(R.string.nc_input_length_invalid),
                                    Toast.LENGTH_SHORT);
                            return;
                        }
                        showLoadingDialog();
                        getViewModel()
                                .createGroup(
                                        groupName,
                                        new OnDataChangeEnhancedListener<Integer>() {
                                            @Override
                                            public void onDataChange(Integer code) {
                                                onCreateGroupResult(
                                                        viewModel.getGroupId(),
                                                        viewModel.getInviteeUserIds(),
                                                        code != null ? code : -1);
                                            }
                                        });
                    }
                });
    }

    /**
     * Handle group creation result.
     *
     * @param groupId group ID
     * @param inviteeUserIds invited user ID list
     * @param errorCode error code
     * @since 5.12.2
     */
    protected void onCreateGroupResult(String groupId, List<String> inviteeUserIds, int errorCode) {
        dismissLoadingDialog();
        if (errorCode == 25427 || errorCode == 0) {
            ChannelIdentifier channelIdentifier = new ChannelIdentifier(ChannelType.GROUP, groupId);
            RouteUtils.routeToChannelActivity(getContext(), channelIdentifier);
            sendFinishActivityBroadcast(FriendSelectFragment.class);
            finishActivity();
        } else {
            String tips = getString(R.string.nc_create_group_failure);
            if (errorCode == 25480) {
                tips = getString(R.string.nc_content_contain_sensitive);
            }
            ToastUtils.show(getContext(), tips, Toast.LENGTH_SHORT);
        }
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
