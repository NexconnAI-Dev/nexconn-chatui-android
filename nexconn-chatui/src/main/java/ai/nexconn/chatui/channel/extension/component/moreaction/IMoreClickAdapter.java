package ai.nexconn.chatui.channel.extension.component.moreaction;

import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import java.util.List;

public interface IMoreClickAdapter {
    /**
     * Displays the bottom "more actions" layout.
     *
     * @param viewGroup Parent layout.
     * @param fragment Current fragment.
     * @param actions Click actions.
     */
    void bindView(ViewGroup viewGroup, Fragment fragment, List<IClickActions> actions);

    /** Hides the bottom actions layout. */
    void hideMoreActionLayout();

    /**
     * Sets whether the bottom action buttons are highlighted.
     *
     * @param enable Whether to highlight.
     */
    void setMoreActionEnable(boolean enable);

    /**
     * Whether the action buttons are visible.
     *
     * @return true if visible, false if hidden.
     */
    boolean isMoreActionShown();
}
