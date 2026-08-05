package ai.nexconn.chatui.channel.feature.combineforward;

import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.BaseActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

/**
 * Combined-forward message preview Activity (no title bar; the header is managed by the Fragment).
 *
 * @since 5.12.0
 */
public class CombineMessagePreviewActivity extends BaseActivity {

    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        Intent intent = new Intent(context, CombineMessagePreviewActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nc_activity_combine_detail);

        Fragment fragment = createFragment();
        FragmentManager manager = getSupportFragmentManager();
        manager.popBackStack();
        manager.beginTransaction().replace(R.id.nc_combine_content, fragment).commit();
    }

    @NonNull
    protected Fragment createFragment() {
        Bundle bundle = getIntent().getExtras() != null ? getIntent().getExtras() : new Bundle();
        return NCChatUI.getFragmentFactory().newCombineMessagePreviewFragment(bundle);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
