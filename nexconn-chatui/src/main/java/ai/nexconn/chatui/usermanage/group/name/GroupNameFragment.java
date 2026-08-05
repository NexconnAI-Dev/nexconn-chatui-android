package ai.nexconn.chatui.usermanage.group.name;

import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import java.util.List;

/**
 * Fragment for editing group name.
 *
 * @since 5.12.0
 */
public class GroupNameFragment extends BaseViewModelFragment<GroupNameViewModel> {

    protected HeadComponent headComponent;
    private EditText groupNameInput;

    protected TipLoadingDialog dialog;

    @NonNull
    @Override
    protected GroupNameViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupNameViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_group_name, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        groupNameInput = view.findViewById(R.id.group_name_input);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupNameViewModel viewModel) {
        GroupInfo groupInfo =
                ChatUIBundleUtils.getGroupInfo(getArguments(), ChatUIConstants.KEY_GROUP_INFO);
        headComponent.setLeftClickListener(v -> finishActivity());
        headComponent.setRightTextViewEnable(false);
        headComponent.setRightClickListener(
                v -> {
                    if (groupInfo != null) {
                        String groupName = groupNameInput.getText().toString().trim();
                        GroupInfo updatedGroupInfo =
                                new GroupInfo(groupInfo.getGroupId(), groupName);
                        showLoadingDialog();
                        viewModel.updateGroupInfo(
                                updatedGroupInfo,
                                new OnDataChangeEnhancedListener<Boolean>() {
                                    @Override
                                    public void onDataChange(Boolean result) {
                                        dismissLoadingDialog();
                                        ToastUtils.show(
                                                getActivity(),
                                                getString(R.string.nc_set_success),
                                                Toast.LENGTH_SHORT);
                                        finishActivity();
                                    }

                                    @Override
                                    public void onDataError(NCError error, List<String> errorKeys) {
                                        dismissLoadingDialog();
                                        String tips = getString(R.string.nc_set_failed);
                                        if (error != null && error.getCode() == 25480) {
                                            tips = getString(R.string.nc_content_contain_sensitive);
                                        }
                                        ToastUtils.show(getContext(), tips, Toast.LENGTH_SHORT);
                                    }
                                });
                    }
                });

        if (groupInfo != null) {
            groupNameInput.setText(groupInfo.getGroupName());
        }
        groupNameInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        int length = s.length();
                        boolean isEnable = length >= 1 && length <= 64;
                        if (headComponent != null) {
                            headComponent.setRightTextViewEnable(isEnable);
                        }
                    }
                });
    }

    /** loading dialog */
    protected void showLoadingDialog() {
        if (dialog != null) {
            dismissLoadingDialog();
        }
        dialog = new TipLoadingDialog(getContext());
        dialog.setTips(getString(R.string.nc_loading_saving));
        dialog.show();
    }

    /** dismiss dialog */
    protected void dismissLoadingDialog() {
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
