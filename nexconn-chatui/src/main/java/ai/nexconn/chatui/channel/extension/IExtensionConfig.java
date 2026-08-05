package ai.nexconn.chatui.channel.extension;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chatui.channel.extension.component.emoticon.IEmoticonTab;
import ai.nexconn.chatui.channel.extension.component.plugin.IPluginModule;
import java.util.List;
import java.util.Map;

public interface IExtensionConfig {
    /**
     * Returns the plugin list configured in the "+" area of the input bar.
     *
     * @param channelType Channel type.
     * @param targetId Channel ID.
     * @return Plugin list.
     */
    List<IPluginModule> getPluginModules(ChannelType channelType, String targetId);

    /**
     * Returns the emoticon tab list configured in the emoticon area of the input bar.
     *
     * @param channelType Channel type.
     * @param targetId Channel ID.
     * @return Emoticon tab list.
     */
    Map<String, List<IEmoticonTab>> getEmoticonTabs(ChannelType channelType, String targetId);
}
