package ai.nexconn.chatui.usermanage.friend.mine.nickname;

import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chat.user.model.UserProfile;
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
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import java.util.List;

/**
 * Update nickname page
 *
 * @since 5.12.0
 */
public class UpdateNickNameFragment extends BaseViewModelFragment<UpdateNickNameViewModel> {
    protected EditText etContent;
    protected TextView tvTitle;
    protected HeadComponent headComponent;
    protected UserProfile userProfile;
    protected FriendDetail friendInfo;
    private TipLoadingDialog dialog;

    @NonNull
    @Override
    protected UpdateNickNameViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(UpdateNickNameViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_update_nickname, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        tvTitle = view.findViewById(R.id.tv_title);
        etContent = view.findViewById(R.id.et_content);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull UpdateNickNameViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());

        String profileUserId = getArguments().getString(ChatUIConstants.KEY_USER_PROFILE_USER_ID);
        if (profileUserId != null) {
            userProfile =
                    new UserProfile(
                            profileUserId,
                            getArguments().getString(ChatUIConstants.KEY_USER_PROFILE_NAME),
                            getArguments().getString(ChatUIConstants.KEY_USER_PROFILE_PORTRAIT_URI),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null);
        }
        String friendUserId = getArguments().getString(ChatUIConstants.KEY_FRIEND_DETAIL_USER_ID);
        if (friendUserId != null) {
            friendInfo =
                    new FriendDetail(
                            friendUserId,
                            getArguments().getString(ChatUIConstants.KEY_FRIEND_DETAIL_NAME),
                            null,
                            getArguments().getString(ChatUIConstants.KEY_FRIEND_DETAIL_REMARK),
                            null,
                            0);
        }
        if (userProfile != null) {
            headComponent.setTitleText(R.string.nc_set_nick_name);
            etContent.setHint(R.string.nc_nickname_hint);
            tvTitle.setText(R.string.nc_nickname_label);
        }
        if (friendInfo != null) {
            headComponent.setTitleText(R.string.nc_set_remark_name);
            etContent.setHint(R.string.nc_friend_nickname_hint);
            tvTitle.setText(R.string.nc_remark);
        }
        headComponent.setRightTextViewEnable(false);
        headComponent.setRightClickListener(
                v -> {
                    String name = etContent.getText().toString();
                    if (userProfile != null) {
                        UserProfile updatedProfile =
                                new UserProfile(
                                        null, name, null, null, null, null, null, null, null, null,
                                        null);
                        showLoadingDialog();
                        getViewModel()
                                .updateUserProfile(
                                        updatedProfile,
                                        new OnDataChangeEnhancedListener<Boolean>() {
                                            @Override
                                            public void onDataChange(Boolean isSuccess) {
                                                dismissLoadingDialog();
                                                if (isSuccess) {
                                                    finishActivity();
                                                }
                                            }

                                            @Override
                                            public void onDataError(
                                                    NCError error, List<String> errorKeys) {
                                                dismissLoadingDialog();
                                                String tips = getString(R.string.nc_set_failed);
                                                if (error != null && error.getCode() == 25480) {
                                                    tips =
                                                            getString(
                                                                    R.string
                                                                            .nc_content_contain_sensitive);
                                                }
                                                ToastUtils.show(
                                                        getContext(), tips, Toast.LENGTH_SHORT);
                                            }
                                        });
                    } else if (friendInfo != null) {
                        showLoadingDialog();
                        viewModel.setFriendInfo(
                                friendInfo.getUserId(),
                                name,
                                new OnDataChangeEnhancedListener<Boolean>() {
                                    @Override
                                    public void onDataChange(Boolean isSuccess) {
                                        dismissLoadingDialog();
                                        if (isSuccess) {
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
                                        ToastUtils.show(getContext(), tips, Toast.LENGTH_SHORT);
                                    }
                                });
                    }
                });

        String nickName =
                userProfile != null
                        ? userProfile.getName()
                        : friendInfo != null ? friendInfo.getRemark() : "";
        etContent.setText(nickName);

        etContent.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        int length = s.length();
                        boolean isEnable = length <= 64;
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
