package ai.nexconn.chatui.channel;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.BaseActivity;
import ai.nexconn.chatui.utils.route.RouteUtils;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import java.util.Locale;

/**
 * Channel page Activity. Uses BaseActivity + ChatUIFragmentFactory pattern;
 * title/back/input-state/online-status logic lives in ChannelFragment.
 */
public class ChannelActivity extends BaseActivity {

    @NonNull
    public static Intent newIntent(
            @NonNull Context context, @NonNull ChannelType channelType, @NonNull String targetId) {
        return newIntent(context, channelType, targetId, false, null);
    }

    @NonNull
    public static Intent newIntent(
            @NonNull Context context,
            @NonNull ChannelType channelType,
            @NonNull String targetId,
            boolean disableSystemEmoji,
            Bundle extras) {
        Intent intent = new Intent(context, ChannelActivity.class);
        intent.putExtra(RouteUtils.CHANNEL_TYPE, channelType.name().toLowerCase(Locale.US));
        intent.putExtra(RouteUtils.TARGET_ID, targetId);
        intent.putExtra(RouteUtils.DISABLE_SYSTEM_EMOJI, disableSystemEmoji);
        if (extras != null) intent.putExtras(extras);
        return intent;
    }

    @NonNull
    public static Intent newIntent(
            @NonNull Context context,
            @NonNull ai.nexconn.chat.channel.model.ChannelIdentifier identifier) {
        Intent intent = new Intent(context, ChannelActivity.class);
        intent.putExtra(
                RouteUtils.CHANNEL_TYPE, identifier.getChannelType().name().toLowerCase(Locale.US));
        intent.putExtra(RouteUtils.TARGET_ID, identifier.getChannelId());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nc_activity);
        Fragment fragment = createFragment();
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.fl_fragment_container, fragment).commit();
    }

    @NonNull
    protected Fragment createFragment() {
        Bundle bundle = getIntent().getExtras() != null ? getIntent().getExtras() : new Bundle();
        return NCChatUI.getFragmentFactory().newChannelFragment(bundle);
    }
}
