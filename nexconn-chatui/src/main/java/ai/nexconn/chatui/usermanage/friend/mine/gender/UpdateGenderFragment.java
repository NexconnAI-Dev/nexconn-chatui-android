package ai.nexconn.chatui.usermanage.friend.mine.gender;

import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.user.model.UserProfile;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.widget.SettingItemView;
import ai.nexconn.chatui.widget.component.HeadComponent;
import ai.nexconn.chatui.widget.dialog.TipLoadingDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import java.util.List;

/**
 * @since 5.12.0
 * @author NC
 * @since 5.12.0
 */
public class UpdateGenderFragment extends BaseViewModelFragment<UpdateGenderViewModel>
        implements View.OnClickListener {
    protected SettingItemView manSiv;
    protected SettingItemView femaleSiv;
    protected HeadComponent headComponent;

    private TipLoadingDialog dialog;

    @NonNull
    @Override
    protected UpdateGenderViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(UpdateGenderViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_update_gender, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        manSiv = view.findViewById(R.id.siv_gender_man);
        manSiv.setOnClickListener(this);
        femaleSiv = view.findViewById(R.id.siv_gender_female);
        femaleSiv.setOnClickListener(this);

        return view;
    }

    @Override
    protected void onViewReady(@NonNull UpdateGenderViewModel viewModel) {
        int gender = viewModel.getUserProfile().getGender();
        if (gender == 1) {
            manSiv.setRightImageVisibility(View.VISIBLE);
            femaleSiv.setRightImageVisibility(View.GONE);
        } else if (gender == 2) {
            manSiv.setRightImageVisibility(View.GONE);
            femaleSiv.setRightImageVisibility(View.VISIBLE);
        } else {
            manSiv.setRightImageVisibility(View.GONE);
            femaleSiv.setRightImageVisibility(View.GONE);
        }

        headComponent.setLeftClickListener(v -> finishActivity());
        headComponent.setRightClickListener(
                v -> {
                    if (getViewModel().getUserProfile() != null) {
                        showLoadingDialog();
                        getViewModel()
                                .updateUserProfile(
                                        getViewModel().getUserProfile(),
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
                    }
                });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.siv_gender_man) {
            manSiv.setRightImageVisibility(View.VISIBLE);
            femaleSiv.setRightImageVisibility(View.GONE);
            UserProfile current = getViewModel().getUserProfile();
            getViewModel()
                    .setUserProfile(
                            new UserProfile(
                                    current != null ? current.getUserId() : null,
                                    current != null ? current.getName() : null,
                                    current != null ? current.getPortraitUri() : null,
                                    current != null ? current.getUniqueId() : null,
                                    current != null ? current.getEmail() : null,
                                    current != null ? current.getBirthday() : null,
                                    1,
                                    current != null ? current.getLocation() : null,
                                    current != null ? current.getRole() : null,
                                    current != null ? current.getLevel() : null,
                                    current != null ? current.getExtProfile() : null));
        } else if (id == R.id.siv_gender_female) {
            manSiv.setRightImageVisibility(View.GONE);
            femaleSiv.setRightImageVisibility(View.VISIBLE);
            UserProfile current = getViewModel().getUserProfile();
            getViewModel()
                    .setUserProfile(
                            new UserProfile(
                                    current != null ? current.getUserId() : null,
                                    current != null ? current.getName() : null,
                                    current != null ? current.getPortraitUri() : null,
                                    current != null ? current.getUniqueId() : null,
                                    current != null ? current.getEmail() : null,
                                    current != null ? current.getBirthday() : null,
                                    2,
                                    current != null ? current.getLocation() : null,
                                    current != null ? current.getRole() : null,
                                    current != null ? current.getLevel() : null,
                                    current != null ? current.getExtProfile() : null));
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
