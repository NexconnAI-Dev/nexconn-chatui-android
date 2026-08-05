package ai.nexconn.chatui.usermanage.friend.mine.profile;

import ai.nexconn.chat.user.model.UserProfile;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.usermanage.friend.mine.gender.UpdateGenderActivity;
import ai.nexconn.chatui.usermanage.friend.mine.nickname.UpdateNickNameActivity;
import ai.nexconn.chatui.widget.component.HeadComponent;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

/**
 * My profile page
 *
 * @since 5.12.0
 */
public class MyProfileFragment extends BaseViewModelFragment<MyProfileViewModel>
        implements View.OnClickListener {
    protected HeadComponent headComponent;
    @NonNull View rootView;

    @NonNull
    @Override
    protected MyProfileViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory()).get(MyProfileViewModel.class);
    }

    /**
     * Called after the view is created
     *
     * @param viewModel VM
     */
    @Override
    protected void onViewReady(@NonNull MyProfileViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        viewModel
                .getUserProfilesLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        userProfile -> {
                            // Update UI
                            notifyUserProfileChanged(userProfile);
                        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getViewModel().loadMyUserProfile();
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        rootView = inflater.inflate(R.layout.nc_page_my_profile, container, false);
        headComponent = rootView.findViewById(R.id.nc_head_component);
        rootView.findViewById(R.id.cl_head).setOnClickListener(this);
        rootView.findViewById(R.id.cl_nickname).setOnClickListener(this);
        rootView.findViewById(R.id.cl_app).setOnClickListener(this);
        rootView.findViewById(R.id.cl_gender).setOnClickListener(this);
        return rootView;
    }

    protected void notifyUserProfileChanged(@NonNull UserProfile userProfile) {
        // Update UI
        setText(R.id.tv_nickname_content, userProfile.getName());
        setText(R.id.tv_app_content, userProfile.getUniqueId());
        int nGender = userProfile.getGender();
        String sGender = getString(R.string.nc_unknow_type);
        if (nGender == 1) {
            sGender = getString(R.string.nc_gender_man);
        } else if (nGender == 2) {
            sGender = getString(R.string.nc_gender_female);
        }
        setText(R.id.tv_gender_content, sGender);
        NCChatUIConfig.featureConfig()
                .getChatUIImageEngine()
                .loadUserPortrait(
                        rootView.getContext(),
                        userProfile.getPortraitUri(),
                        rootView.<ImageView>findViewById(R.id.iv_head));
    }

    private void setText(@IdRes int id, String text) {
        TextView view = rootView.findViewById(id);
        if (view != null) {
            view.setText(text);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.cl_head) {
            onUserHeaderClick(v);
        } else if (id == R.id.cl_nickname) {
            onNickNameClick(v);
        } else if (id == R.id.cl_gender) {
            onGenderClick(v);
        }
    }

    /**
     * User avatar click
     *
     * @param view View
     */
    protected void onUserHeaderClick(View view) {}

    /**
     * Nickname click
     *
     * @param view View
     */
    protected void onNickNameClick(View view) {
        UserProfile value = getViewModel().getUserProfilesLiveData().getValue();
        if (value != null) {
            startActivity(UpdateNickNameActivity.newIntent(getActivity(), value));
        }
    }

    /**
     * Gender click
     *
     * @param view View
     */
    protected void onGenderClick(View view) {
        UserProfile value = getViewModel().getUserProfilesLiveData().getValue();
        if (value != null) {
            startActivity(UpdateGenderActivity.newIntent(getActivity(), value));
        }
    }
}
