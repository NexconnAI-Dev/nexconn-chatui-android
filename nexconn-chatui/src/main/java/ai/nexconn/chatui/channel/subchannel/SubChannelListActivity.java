package ai.nexconn.chatui.channel.subchannel;

import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.BaseActivity;
import ai.nexconn.chatui.utils.route.RouteUtils;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

/**
 * Sub-channel list Activity. Uses BaseActivity + ChatUIFragmentFactory pattern; logic resides in
 * SubChannelListFragment.
 */
public class SubChannelListActivity extends BaseActivity {

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
        if (getIntent().hasExtra(RouteUtils.TITLE)) {
            bundle.putString(RouteUtils.TITLE, getIntent().getStringExtra(RouteUtils.TITLE));
        }
        if (getIntent().hasExtra(RouteUtils.CHANNEL_TYPE)) {
            bundle.putSerializable(
                    RouteUtils.CHANNEL_TYPE,
                    getIntent().getSerializableExtra(RouteUtils.CHANNEL_TYPE));
        }
        return NCChatUI.getFragmentFactory().newSubChannelListFragment(bundle);
    }
}
