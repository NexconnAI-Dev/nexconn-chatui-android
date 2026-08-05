package ai.nexconn.chatui.usermanage.friend.select;

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
 * Friend selection page
 *
 * @since 5.12.0
 */
public class FriendSelectActivity extends BaseActivity {

    private Fragment fragment;

    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        return newIntent(context, 30);
    }

    @NonNull
    public static Intent newIntent(@NonNull Context context, int maxCount) {
        Intent intent = new Intent(context, FriendSelectActivity.class);
        Bundle bundle = new Bundle();
        int validatedMaxMemberCountDisplay = Math.max(1, Math.min(100, maxCount));
        bundle.putInt(ChatUIConstants.KEY_MAX_FRIEND_SELECT_COUNT, validatedMaxMemberCountDisplay);
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
        return NCChatUI.getFragmentFactory().newFriendSelectFragment(bundle);
    }
}
