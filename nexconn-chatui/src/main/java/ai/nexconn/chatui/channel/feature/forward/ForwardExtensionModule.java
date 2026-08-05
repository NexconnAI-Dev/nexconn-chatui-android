package ai.nexconn.chatui.channel.feature.forward;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chatui.channel.extension.IExtensionModule;
import ai.nexconn.chatui.channel.extension.NCExtension;
import ai.nexconn.chatui.channel.extension.component.emoticon.IEmoticonTab;
import ai.nexconn.chatui.channel.extension.component.plugin.IPluginModule;
import android.content.Context;
import androidx.fragment.app.Fragment;
import java.lang.ref.WeakReference;
import java.util.List;

public class ForwardExtensionModule implements IExtensionModule {
    static WeakReference<NCExtension> sNCExtension;
    static WeakReference<Fragment> sFragment;

    @Override
    public void onInit(Context context, String appKey) {
        // default implementation ignored
    }

    @Override
    public void onAttachedToExtension(Fragment fragment, NCExtension extension) {
        sFragment = new WeakReference<>(fragment);
        sNCExtension = new WeakReference<>(extension);
    }

    @Override
    public void onDetachedFromExtension() {
        // do nothing
    }

    @Override
    public void onReceivedMessage(Message message) {
        // do nothing
    }

    @Override
    public List<IPluginModule> getPluginModules(ChannelType conversationType) {
        return null;
    }

    @Override
    public List<IEmoticonTab> getEmoticonTabs() {
        return null;
    }

    @Override
    public void onDisconnect() {
        // do nothing
    }
}
