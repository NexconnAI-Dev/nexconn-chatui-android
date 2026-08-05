package ai.nexconn.chatui.channel.messagelist;

import ai.nexconn.chatui.model.UiMessage;
import android.app.Activity;

/** Interface for message ItemProviders that need to request permissions in onItemClick. */
public interface IMessageProviderPermissionHandler {
    void handleRequestPermissionsResult(
            Activity activity, UiMessage uiMessage, String[] permissions, int[] grantResults);
}
