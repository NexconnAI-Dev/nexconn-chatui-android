package ai.nexconn.chatui.usermanage.group.notice;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupInfo;
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
 * Activity for group notice/announcement.
 *
 * @since 5.12.0
 */
public class GroupNoticeActivity extends BaseActivity {

    private Fragment fragment;

    @NonNull
    public static Intent newIntent(
            @NonNull Context context,
            @NonNull ChannelIdentifier channelIdentifier,
            @NonNull GroupInfo groupInfo) {
        Intent intent = new Intent(context, GroupNoticeActivity.class);
        Bundle bundle = new Bundle();
        ChatUIBundleUtils.putChannelIdentifier(
                bundle, ChatUIConstants.KEY_CHANNEL_IDENTIFIER, channelIdentifier);
        ChatUIBundleUtils.putGroupInfo(bundle, ChatUIConstants.KEY_GROUP_INFO, groupInfo);
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
        return NCChatUI.getFragmentFactory().newGroupNoticeFragment(bundle);
    }
}
