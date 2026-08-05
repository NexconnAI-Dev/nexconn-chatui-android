package ai.nexconn.chatui.channel.extension.component.plugin;

import ai.nexconn.chatui.channel.extension.NCExtension;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import androidx.fragment.app.Fragment;

public interface IPluginModule {

    /**
     * Gets the plugin icon.
     *
     * @param context Context.
     * @return The icon Drawable.
     */
    Drawable obtainDrawable(Context context);

    /**
     * Gets the plugin title.
     *
     * @param context Context.
     * @return The title string.
     */
    String obtainTitle(Context context);

    /**
     * Called when the plugin is clicked. 1. If data from the Extension is needed, call the
     * corresponding Extension methods to obtain it. 2. If a new activity needs to be started after
     * clicking, use {@link Activity#startActivityForResult(Intent, int)} or {@link
     * NCExtension#startActivityForPluginResult(Intent, int, IPluginModule)}.
     *
     * <p>Note: Do not hold long-lived references to the fragment or extension object, as this will
     * cause memory leaks.
     *
     * @param currentFragment The fragment associated with the plugin.
     * @param extension The NCExtension object.
     * @param index The index of the plugin in the plugin panel.
     */
    void onClick(Fragment currentFragment, NCExtension extension, int index);

    /**
     * Returns data results when the activity finishes.
     *
     * <p>In {@link #onClick(Fragment, NCExtension, int)}, you may start a new activity in two ways:
     *
     * <p>1. Using {@link Activity#startActivityForResult(Intent, int)} — you need to handle the
     * result in the corresponding Activity's {@link Activity}#onActivityResult(int, int, Intent).
     *
     * <p>2. Using {@link NCExtension#startActivityForPluginResult(Intent, int, IPluginModule)} —
     * after ChannelFragment receives {@link Activity}#onActivityResult(int, int, Intent), you must
     * call {@link NCExtension#onActivityPluginResult(int, int, Intent)} so that NCExtension can
     * return the data result through IPluginModule's onActivityResult method.
     *
     * <p>
     *
     * @param requestCode The request code used when starting the activity; will not exceed 255.
     * @param resultCode The result code returned when the activity finishes.
     * @param data The returned data.
     */
    void onActivityResult(int requestCode, int resultCode, Intent data);
}
