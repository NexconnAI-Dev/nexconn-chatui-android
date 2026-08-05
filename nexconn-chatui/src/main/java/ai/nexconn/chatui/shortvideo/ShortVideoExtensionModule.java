package ai.nexconn.chatui.shortvideo;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.extension.IExtensionModule;
import ai.nexconn.chatui.channel.extension.NCExtension;
import ai.nexconn.chatui.channel.extension.component.emoticon.IEmoticonTab;
import ai.nexconn.chatui.channel.extension.component.plugin.IPluginModule;
import ai.nexconn.chatui.channel.messagelist.provider.ShortVideoMessageItemProvider;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import android.content.Context;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class ShortVideoExtensionModule implements IExtensionModule {

    @Override
    public void onInit(Context context, String appKey) {
        ChatUIThemeManager.addTheme(
                ChatUIThemeManager.LIVELY_THEME,
                R.style.NCLivelyLightTheme_ShortVideoKit,
                R.style.NCLivelyDarkTheme_ShortVideoKit);
        NCChatUIConfig.channelConfig().addMessageProvider(new ShortVideoMessageItemProvider());
    }

    @Override
    public void onAttachedToExtension(Fragment fragment, NCExtension extension) {
        // default implementation ignoredq
    }

    @Override
    public void onDetachedFromExtension() {
        // default implementation ignored
    }

    @Override
    public void onReceivedMessage(Message message) {
        // default implementation ignored
    }

    @Override
    public List<IPluginModule> getPluginModules(ChannelType channelType) {
        List<IPluginModule> pluginModules = new ArrayList<>();
        ShortVideoPlugin sightPlugin = new ShortVideoPlugin();
        pluginModules.add(sightPlugin);
        return pluginModules;
    }

    @Override
    public List<IEmoticonTab> getEmoticonTabs() {
        return null;
    }

    @Override
    public void onDisconnect() {
        // default implementation ignored
    }
}
