package ai.nexconn.chatui.usermanage.group.memberselect.impl;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for adding group managers.
 *
 * @since 5.12.2
 */
public class GroupAddManagerActivity extends BaseActivity {

    private Fragment fragment;

    @NonNull
    public static Intent newIntent(
            @NonNull Context context,
            @NonNull ChannelIdentifier channelIdentifier,
            List<String> disableUserIds) {
        return newIntent(context, channelIdentifier, disableUserIds, 30);
    }

    @NonNull
    public static Intent newIntent(
            @NonNull Context context,
            @NonNull ChannelIdentifier channelIdentifier,
            List<String> disableUserIds,
            int maxCount) {
        Intent intent = new Intent(context, GroupAddManagerActivity.class);
        Bundle bundle = new Bundle();
        ChatUIBundleUtils.putChannelIdentifier(
                bundle, ChatUIConstants.KEY_CHANNEL_IDENTIFIER, channelIdentifier);
        int validatedMaxCountDisplay = Math.max(1, Math.min(100, maxCount));
        bundle.putInt(ChatUIConstants.KEY_MAX_SELECT_COUNT, validatedMaxCountDisplay);
        bundle.putStringArrayList(
                ChatUIConstants.KEY_DISABLE_USER_IDS, new ArrayList<>(disableUserIds));
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
        return NCChatUI.getFragmentFactory().newGroupAddManagerFragment(bundle);
    }
}
