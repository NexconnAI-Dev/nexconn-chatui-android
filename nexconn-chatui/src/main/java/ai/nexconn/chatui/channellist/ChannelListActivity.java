package ai.nexconn.chatui.channellist;

import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.BaseActivity;
import ai.nexconn.chatui.utils.route.RouteUtils;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

/**
 * Channel list Activity. Uses BaseActivity + ChatUIFragmentFactory pattern; logic resides in
 * ChannelListFragment.
 *
 * @since 5.10.4
 */
public class ChannelListActivity extends BaseActivity {

    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, ChannelListActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nc_activity);
        Fragment fragment = createFragment();
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.fl_fragment_container, fragment).commit();
    }

    @NonNull
    protected Fragment createFragment() {
        Bundle bundle = getIntent().getExtras() != null ? getIntent().getExtras() : new Bundle();
        String title = getIntent().getStringExtra(RouteUtils.TITLE);
        if (!TextUtils.isEmpty(title)) {
            bundle.putString(RouteUtils.TITLE, title);
        } else {
            bundle.putString(
                    RouteUtils.TITLE,
                    getResources().getString(R.string.nc_conversation_list_title));
        }
        return NCChatUI.getFragmentFactory().newChannelListFragment(bundle);
    }
}
