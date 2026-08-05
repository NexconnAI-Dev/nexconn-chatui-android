package ai.nexconn.chatui.usermanage.friend.mine.nickname;

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
 * Update nickname page
 *
 * @since 5.12.0
 */
public class UpdateNickNameActivity extends BaseActivity {

    private Fragment fragment;

    @NonNull
    public static Intent newIntent(
            @NonNull Context context,
            @NonNull ai.nexconn.chat.user.model.FriendDetail friendDetail) {
        Intent intent = new Intent(context, UpdateNickNameActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(ChatUIConstants.KEY_FRIEND_DETAIL_USER_ID, friendDetail.getUserId());
        bundle.putString(ChatUIConstants.KEY_FRIEND_DETAIL_NAME, friendDetail.getName());
        bundle.putString(ChatUIConstants.KEY_FRIEND_DETAIL_REMARK, friendDetail.getRemark());
        intent.putExtras(bundle);
        return intent;
    }

    @NonNull
    public static Intent newIntent(
            @NonNull Context context,
            @NonNull ai.nexconn.chat.user.model.UserProfile ncUserProfile) {
        Intent intent = new Intent(context, UpdateNickNameActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(ChatUIConstants.KEY_USER_PROFILE_USER_ID, ncUserProfile.getUserId());
        bundle.putString(ChatUIConstants.KEY_USER_PROFILE_NAME, ncUserProfile.getName());
        bundle.putString(
                ChatUIConstants.KEY_USER_PROFILE_PORTRAIT_URI, ncUserProfile.getPortraitUri());
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
        return NCChatUI.getFragmentFactory().newUpdateNikeNameFragment(bundle);
    }
}
