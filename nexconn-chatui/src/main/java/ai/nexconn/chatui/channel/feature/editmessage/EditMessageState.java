package ai.nexconn.chatui.channel.feature.editmessage;

import ai.nexconn.chatui.channel.extension.NCExtension;
import androidx.fragment.app.Fragment;
import java.lang.ref.WeakReference;

public class EditMessageState {
    public WeakReference<Fragment> mFragment;
    public WeakReference<NCExtension> mNCExtension;
    public EditMessageConfig config;
}
