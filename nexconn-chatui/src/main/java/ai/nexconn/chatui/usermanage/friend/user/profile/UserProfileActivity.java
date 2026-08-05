package ai.nexconn.chatui.usermanage.friend.user.profile;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.BaseActivity;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

/**
 * User profile page
 *
 * @since 5.12.0
 */
public class UserProfileActivity extends BaseActivity {

    private Fragment fragment;

    @NonNull
    public static Intent newIntent(@NonNull Context context, String userId) {
        return newIntent(context, userId, (ChannelIdentifier) null);
    }

    @NonNull
    public static Intent newIntent(
            @NonNull Context context,
            String userId,
            @Nullable ChannelIdentifier channelIdentifier) {
        Intent intent = new Intent(context, UserProfileActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(ChatUIConstants.KEY_USER_ID, userId);
        ChatUIBundleUtils.putChannelIdentifier(
                bundle, ChatUIConstants.KEY_CHANNEL_IDENTIFIER, channelIdentifier);
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
        return NCChatUI.getFragmentFactory().newUserProfileFragment(bundle);
    }
}
