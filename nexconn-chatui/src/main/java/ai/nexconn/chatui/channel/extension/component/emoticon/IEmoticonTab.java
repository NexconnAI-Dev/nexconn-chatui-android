package ai.nexconn.chatui.channel.extension.component.emoticon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import androidx.lifecycle.LiveData;

public interface IEmoticonTab {

    /**
     * Creates the small icon for the tab, displayed in the tab bar.
     *
     * @param context Application context.
     * @return The tab icon drawable; must not be null.
     */
    Drawable obtainTabDrawable(Context context);

    /**
     * Creates the tab page view.
     *
     * @param context Application context.
     * @param parent ViewGroup.
     * @return The constructed tab view; must not be null.
     */
    View obtainTabPager(Context context, ViewGroup parent);

    /**
     * Called when the emoticon panel is swiped left or right.
     *
     * @param position The current tab position.
     */
    void onTableSelected(int position);

    /**
     * Returns the update information for the input field corresponding to this tab page.
     *
     * @return LiveData for input to the EditText.
     */
    LiveData<String> getEditInfo();
}
