package ai.nexconn.chatui.usermanage.friend.mine.gender;

import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.BaseActivity;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

/**
 * Update gender page
 *
 * @since 5.12.0
 */
public class UpdateGenderActivity extends BaseActivity {

    private Fragment fragment;

    @NonNull
    public static Intent newIntent(
            @NonNull Context context, ai.nexconn.chat.user.model.UserProfile ncUserProfile) {
        Intent intent = new Intent(context, UpdateGenderActivity.class);
        Bundle bundle = new Bundle();
        if (ncUserProfile != null) {
            bundle.putString(ChatUIConstants.KEY_USER_PROFILE_USER_ID, ncUserProfile.getUserId());
            bundle.putString(ChatUIConstants.KEY_USER_PROFILE_NAME, ncUserProfile.getName());
            bundle.putString(
                    ChatUIConstants.KEY_USER_PROFILE_PORTRAIT_URI, ncUserProfile.getPortraitUri());
            bundle.putInt(
                    ChatUIConstants.KEY_USER_PROFILE_GENDER,
                    ncUserProfile.getGender() != null ? ncUserProfile.getGender() : 0);
        }
        intent.putExtras(bundle);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nc_activity);

        fragment = createFragment();
        FragmentManager manager = getSupportFragmentManager();
        manager.popBackStack();
        manager.beginTransaction().replace(R.id.fl_fragment_container, fragment).commit();
    }

    @NonNull
    protected Fragment createFragment() {
        Bundle bundle = getIntent().getExtras() != null ? getIntent().getExtras() : new Bundle();
        return NCChatUI.getFragmentFactory().newUpdateGenderFragment(bundle);
    }
}
