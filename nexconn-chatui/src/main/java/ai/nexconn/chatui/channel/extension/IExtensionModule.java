package ai.nexconn.chatui.channel.extension;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chatui.channel.extension.component.emoticon.IEmoticonTab;
import ai.nexconn.chatui.channel.extension.component.plugin.IPluginModule;
import android.content.Context;
import androidx.fragment.app.Fragment;
import java.util.List;

public interface IExtensionModule {
    /**
     * SDK initialization. Users can register custom messages, message templates, and initialize
     * their own modules in this method.
     *
     * @param context Context.
     * @param appKey Unique application key.
     */
    void onInit(Context context, String appKey);

    /**
     * After entering a conversation, Extension loads all registered modules. Modules can hold and
     * use the Extension. Note: if a module holds an Extension object, it must release it in the
     * onDetachedFromExtension callback to avoid memory leaks.
     *
     * @param fragment The fragment where the Extension object resides.
     * @param extension The Extension object.
     */
    void onAttachedToExtension(Fragment fragment, NCExtension extension);

    /**
     * Called when leaving a conversation; Extension releases all loaded modules. Note: if a module
     * holds an Extension object, it must release it in this callback to avoid memory leaks.
     */
    void onDetachedFromExtension();

    /**
     * After the SDK receives a message, it routes the message to the corresponding module via this
     * method. Users can selectively handle received messages based on their registered message
     * types.
     *
     * @param message The message entity.
     */
    void onReceivedMessage(Message message);

    /**
     * Users can configure plugins in the "+" area based on different conversations. One or multiple
     * plugins can be configured; the extension displays all returned plugins. Note: if no plugins
     * are configured, this method does not need to be implemented.
     *
     * @param channelType Channel type.
     * @return Plugin list.
     */
    List<IPluginModule> getPluginModules(ChannelType channelType);

    /**
     * Multiple or single emoticon tabs can be configured in a conversation. Once configured, this
     * tab will be displayed in all conversations. Note: if no emoticons are configured, this method
     * does not need to be implemented.
     *
     * @return Emoticon tab list.
     */
    List<IEmoticonTab> getEmoticonTabs();

    /** SDK disconnected. */
    void onDisconnect();
}
