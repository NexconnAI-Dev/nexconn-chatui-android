package ai.nexconn.chatui.usermanage.group.create;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for creating a group.
 *
 * @since 5.12.0
 */
public class GroupCreateActivity extends BaseActivity {

    private Fragment fragment;

    public static Intent newIntent(@NonNull Context context, @NonNull List<String> inviteeUserIds) {
        return newIntent(context, "", inviteeUserIds);
    }

    @NonNull
    public static Intent newIntent(
            @NonNull Context context,
            @NonNull String groupIds,
            @NonNull List<String> inviteeUserIds) {
        Intent intent = new Intent(context, GroupCreateActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(ChatUIConstants.KEY_GROUP_ID, groupIds);
        bundle.putStringArrayList(
                ChatUIConstants.KEY_INVITEE_USER_IDS, new ArrayList<>(inviteeUserIds));
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
        return NCChatUI.getFragmentFactory().newGroupCreateFragment(bundle);
    }
}
