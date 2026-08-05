package ai.nexconn.chatui.channel.extension.component.moreaction;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.utils.common.CollectionsUtils;
import ai.nexconn.chatui.utils.view.RTLUtils;
import ai.nexconn.chatui.widget.MoreActionLayout;
import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class MoreInputPanel {
    private View mRootView;
    private MoreActionLayout mMoreActionLayout;

    @SuppressLint("ClickableViewAccessibility")
    public MoreInputPanel(Fragment fragment, ViewGroup parent) {
        mRootView =
                LayoutInflater.from(fragment.getContext())
                        .inflate(R.layout.nc_more_input_panel, parent, false);
        mMoreActionLayout = mRootView.findViewById(R.id.container);
        mMoreActionLayout.setFragment(fragment);
        List<IClickActions> actions = NCChatUIConfig.channelConfig().getMoreClickActions();
        if (!actions.isEmpty()) {
            ArrayList<IClickActions> clickActions = new ArrayList<>(actions);
            if (RTLUtils.isRtl(fragment.getContext())) {
                CollectionsUtils.reverse(clickActions);
            }
            mMoreActionLayout.addActions(clickActions);
        }
    }

    public View getRootView() {
        return mRootView;
    }

    public void refreshView(boolean activeState) {
        mMoreActionLayout.refreshView(activeState);
    }
}
