package ai.nexconn.chatui.usermanage.group.nickname;

import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import ai.nexconn.chatui.widget.component.HeadComponent;
import ai.nexconn.chatui.widget.dialog.TipLoadingDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
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
 * Fragment for editing group nickname.
 *
 * @since 5.12.0
 */
public class GroupNicknameFragment extends BaseViewModelFragment<GroupNicknameViewModel> {

    protected HeadComponent headComponent;
    private EditText groupNicknameInput;
    private TipLoadingDialog dialog;

    @NonNull
    @Override
    protected GroupNicknameViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupNicknameViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_group_nickname, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        groupNicknameInput = view.findViewById(R.id.group_Nickname_input);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupNicknameViewModel viewModel) {
        String title = getArguments().getString(ChatUIConstants.KEY_TITLE, "");
        if (!TextUtils.isEmpty(title)) {
            headComponent.setTitleText(title);
        }
        headComponent.setLeftClickListener(v -> finishActivity());
        headComponent.setRightClickListener(
                v -> {
                    String newNickName = groupNicknameInput.getText().toString().trim();
                    showLoadingDialog();
                    viewModel.updateGroupNickName(
                            newNickName,
                            new OnDataChangeEnhancedListener<Boolean>() {
                                @Override
                                public void onDataChange(Boolean isSuccess) {
                                    dismissLoadingDialog();
                                    if (isSuccess) {
                                        ToastUtils.show(
                                                getActivity(),
                                                getString(R.string.nc_set_success),
                                                Toast.LENGTH_SHORT);
                                        finishActivity();
                                    }
                                }

                                @Override
                                public void onDataError(NCError error, List<String> errorKeys) {
                                    dismissLoadingDialog();
                                    String tips = getString(R.string.nc_set_failed);
                                    if (error != null && error.getCode() == 25480) {
                                        tips = getString(R.string.nc_content_contain_sensitive);
                                    }
                                    ToastUtils.show(getActivity(), tips, Toast.LENGTH_SHORT);
                                }
                            });
                });

        viewModel
                .getMyMemberInfoLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        groupMemberInfo -> {
                            if (groupMemberInfo != null) {
                                groupNicknameInput.setText(groupMemberInfo.getNickname());
                                headComponent.setRightTextViewEnable(false);
                            }
                        });

        GroupMemberInfo groupMemberInfo = viewModel.getMyMemberInfoLiveData().getValue();
        if (groupMemberInfo != null) {
            groupNicknameInput.setText(groupMemberInfo.getNickname());
        }
        groupNicknameInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        int length = s.length();
                        boolean isEnable = length >= 0 && length <= 256;
                        if (headComponent != null) {
                            headComponent.setRightTextViewEnable(isEnable);
                        }
                    }
                });
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
