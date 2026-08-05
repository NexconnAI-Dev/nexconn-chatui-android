package ai.nexconn.chatui.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Base Fragment class.
 *
 * @since 5.10.4
 */
public abstract class BaseFragment extends Fragment {

    private final BroadcastReceiver receiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (BaseFragment.this.getClass().getName().equals(intent.getAction())) {
                        finishActivity();
                    }
                }
            };

    /**
     * Sends a broadcast to close the specified page.
     *
     * @param action the page class
     */
    protected void sendFinishActivityBroadcast(Class<? extends BaseViewModelFragment> action) {
        Intent intent = new Intent(action.getName());
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter filter = new IntentFilter(getClass().getName());
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(receiver);
    }

    /**
     * Checks whether the current Fragment is alive.
     *
     * @return true if the Fragment is currently active
     */
    protected boolean isFragmentAlive() {
        boolean isDeactivated = !isAdded() || isRemoving() || isDetached() || getContext() == null;
        return !isDeactivated;
    }

    /** Closes the current screen. */
    protected void finishActivity() {
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    /**
     * Handles the back press event.
     *
     * @return false to intercept the back navigation
     */
    public boolean onBackPressed() {
        return false;
    }
}
