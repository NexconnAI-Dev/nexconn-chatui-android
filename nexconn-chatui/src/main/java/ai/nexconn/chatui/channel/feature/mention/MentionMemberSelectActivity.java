package ai.nexconn.chatui.channel.feature.mention;

import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.BaseActivity;
import ai.nexconn.chatui.utils.route.RouteUtils;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

/**
 * @ mention member selection Activity. Uses BaseActivity + ChatUIFragmentFactory pattern; logic is
 * in MentionMemberSelectFragment.
 */
public class MentionMemberSelectActivity extends BaseActivity {

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
        if (getIntent().hasExtra(RouteUtils.TARGET_ID)) {
            bundle.putString(
                    RouteUtils.TARGET_ID, getIntent().getStringExtra(RouteUtils.TARGET_ID));
        }
        if (getIntent().hasExtra(RouteUtils.CHANNEL_TYPE)) {
            bundle.putInt(
                    RouteUtils.CHANNEL_TYPE, getIntent().getIntExtra(RouteUtils.CHANNEL_TYPE, 3));
        }
        return NCChatUI.getFragmentFactory().newMentionMemberSelectFragment(bundle);
    }
}
