package ai.nexconn.chatui.channel.extension.component.moreaction;

import ai.nexconn.chatui.model.UiMessage;
import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.fragment.app.Fragment;

public interface IClickActions {

    /**
     * Gets the icon for the click button.
     *
     * @param context Context.
     * @return The button's Drawable. For highlight/grey states, return a selector type that
     *     displays different drawables for enabled/disabled states.
     */
    Drawable obtainDrawable(Context context);

    /**
     * Icon button click event.
     *
     * @param curFragment Current Fragment. Do not hold a reference to this fragment to avoid memory
     *     leaks.
     */
    void onClick(Fragment curFragment);

    /**
     * UIMessage filter.
     *
     * @param message The message.
     * @return Returns true to show the action.
     */
    boolean filter(UiMessage message);
}
