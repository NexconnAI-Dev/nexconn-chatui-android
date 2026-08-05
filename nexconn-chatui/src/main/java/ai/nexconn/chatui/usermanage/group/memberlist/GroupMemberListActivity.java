package ai.nexconn.chatui.usermanage.group.memberlist;

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
 * Activity for group member list.
 *
 * @since 5.12.0
 */
public class GroupMemberListActivity extends BaseActivity {

    private Fragment fragment;

    @NonNull
    public static Intent newIntent(
            @NonNull Context context, @NonNull ChannelIdentifier channelIdentifier) {
        return newIntent(context, channelIdentifier, 50);
    }

    @NonNull
    public static Intent newIntent(
            @NonNull Context context, @NonNull ChannelIdentifier channelIdentifier, int maxCount) {
        Intent intent = new Intent(context, GroupMemberListActivity.class);
        Bundle bundle = new Bundle();
        ChatUIBundleUtils.putChannelIdentifier(
                bundle, ChatUIConstants.KEY_CHANNEL_IDENTIFIER, channelIdentifier);
        int validatedMaxMemberCountPaged = Math.max(1, Math.min(100, maxCount));
        bundle.putInt(ChatUIConstants.KEY_MAX_MEMBER_COUNT_PAGED, validatedMaxMemberCountPaged);
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
        return NCChatUI.getFragmentFactory().newGroupMemberListFragment(bundle);
    }
}
