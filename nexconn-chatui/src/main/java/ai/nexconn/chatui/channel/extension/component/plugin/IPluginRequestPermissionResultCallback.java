package ai.nexconn.chatui.channel.extension.component.plugin;

import ai.nexconn.chatui.channel.extension.NCExtension;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public interface IPluginRequestPermissionResultCallback {
    /** All plugins should use this request code when requesting permissions. */
    int REQUEST_CODE_PERMISSION_PLUGIN = 255;

    /**
     * Plugin permission request result.
     *
     * @param fragment Fragment.
     * @param extension NCExtension.
     * @param permissions Requested permissions.
     * @param requestCode Request code.
     * @param grantResults Permission grant results.
     * @return true if the plugin has handled the permission result; false otherwise.
     */
    boolean onRequestPermissionResult(
            Fragment fragment,
            NCExtension extension,
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults);
}
