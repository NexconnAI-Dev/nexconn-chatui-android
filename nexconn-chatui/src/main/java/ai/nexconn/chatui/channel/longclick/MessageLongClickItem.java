package ai.nexconn.chatui.channel.longclick;

import ai.nexconn.chatui.model.UiMessage;
import android.content.Context;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;

/**
 * Contract interface for message long-press menu items. The order in the List determines the menu
 * display order.
 */
public interface MessageLongClickItem {

    /** Menu item title. */
    String getTitle(@NonNull Context context);

    /**
     * Menu item title for a specific message.
     *
     * <p>Default behavior falls back to {@link #getTitle(Context)} so existing items stay
     * compatible.
     */
    default String getTitle(@NonNull Context context, @NonNull UiMessage message) {
        return getTitle(context);
    }

    /** Menu item icon attr resId; pass 0 to hide the icon. */
    @AttrRes
    int getIconAttrResId();

    /** Whether this menu item is visible for the current message; false filters it out. */
    boolean isEnabled(@NonNull UiMessage message);

    /** Action to execute when the user taps this menu item; return true if consumed. */
    boolean onAction(@NonNull Context context, @NonNull UiMessage message);
}
