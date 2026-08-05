package ai.nexconn.chatui.channel.extension;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chatui.channel.extension.component.emoticon.EmojiTab;
import ai.nexconn.chatui.channel.extension.component.emoticon.IEmoticonTab;
import ai.nexconn.chatui.channel.extension.component.plugin.FilePlugin;
import ai.nexconn.chatui.channel.extension.component.plugin.IPluginModule;
import ai.nexconn.chatui.channel.extension.component.plugin.ImagePlugin;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultExtensionConfig implements IExtensionConfig {
    private final String DEFAULT_TAG = "DefaultExtensionModule";
    /**
     * Default plugin list. Default plugins include: photo, location (when enabled), burn after
     * reading (when enabled), and file.
     *
     * @param channelType Channel type.
     * @param targetId Channel ID.
     * @return Plugin module list.
     */
    @Override
    public List<IPluginModule> getPluginModules(ChannelType channelType, String targetId) {
        List<IPluginModule> pluginModules = new ArrayList<>();
        pluginModules.add(new ImagePlugin());
        List<IExtensionModule> extensionModules =
                NCExtensionManager.getInstance().getExtensionModules();
        for (IExtensionModule module : extensionModules) {
            if (module.getPluginModules(channelType) != null
                    && module.getPluginModules(channelType).size() > 0) {
                pluginModules.addAll(module.getPluginModules(channelType));
            }
        }
        pluginModules.add(new FilePlugin());
        return pluginModules;
    }

    /**
     * Default emoticon tab data. Key-value format where the key is the class name of each
     * ExtensionModule, and the value is the IEmoticonTab list returned by each ExtensionModule.
     *
     * @param channelType Channel type.
     * @param targetId Channel ID.
     * @return Default emoticon tab data.
     */
    @Override
    public Map<String, List<IEmoticonTab>> getEmoticonTabs(
            ChannelType channelType, String targetId) {
        Map<String, List<IEmoticonTab>> emoticonTabs = new LinkedHashMap<>();
        List<IEmoticonTab> list = new ArrayList<>();
        list.add(new EmojiTab());
        emoticonTabs.put(DEFAULT_TAG, list);
        List<IExtensionModule> extensionModules =
                NCExtensionManager.getInstance().getExtensionModules();
        for (IExtensionModule module : extensionModules) {
            if (module.getEmoticonTabs() != null && module.getEmoticonTabs().size() > 0) {
                emoticonTabs.put(module.getClass().getSimpleName(), module.getEmoticonTabs());
            }
        }
        return emoticonTabs;
    }
}
