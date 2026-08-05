package ai.nexconn.chatui.channel.extension;

import ai.nexconn.chat.message.Message;
import ai.nexconn.chatui.channel.extension.component.emoticon.AndroidEmoji;
import ai.nexconn.chatui.channel.feature.editmessage.EditMessageManager;
import ai.nexconn.chatui.channel.feature.forward.ForwardExtensionModule;
import ai.nexconn.chatui.channel.feature.mention.IExtensionEventWatcher;
import ai.nexconn.chatui.channel.feature.quickreply.QuickReplyExtensionModule;
import ai.nexconn.chatui.channel.feature.reference.ReferenceManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.utils.common.ChatUIUtils;
import ai.nexconn.chatui.utils.log.RLog;
import android.app.Application;
import android.content.Context;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NCExtensionManager {

    private static final String TAG = "NCExtensionManager";
    private static final String DEFAULT_LOCATION_MODULE = "io.nc.location.LocationExtensionModule";
    private String mAppKey;
    private Context mApplicationContext;
    private static List<IExtensionModule> mExtModules = new CopyOnWriteArrayList<>();
    private static List<IExtensionEventWatcher> mExtensionEventWatcher =
            new CopyOnWriteArrayList<>();
    private IExtensionConfig mExtensionConfig;

    private NCExtensionManager() {
        // default implementation ignored
    }

    public static NCExtensionManager getInstance() {
        return SingletonHolder.sInstance;
    }

    /**
     * Initialization. The SDK calls this method during initialization; users do not need to call it
     * again.
     *
     * @param context Application context.
     * @param appKey Application key.
     */
    public void init(Context context, String appKey) {
        RLog.d(TAG, "init");
        AndroidEmoji.init(context);
        ChatUIUtils.init(context);
        mAppKey = appKey;
        mApplicationContext = context;
        mExtensionConfig = new DefaultExtensionConfig();
        mExtModules.clear();
        checkLocationModule();
        mExtModules.add(new ForwardExtensionModule());

        if (NCChatUIConfig.featureConfig().isReferenceEnable()) {
            mExtModules.add(ReferenceManager.getInstance());
        }
        if (NCChatUIConfig.featureConfig().isEditMessageEnable()) {
            mExtModules.add(EditMessageManager.getInstance());
        }
        if (NCChatUIConfig.featureConfig().isQuickReplyEnable()) {
            mExtModules.add(new QuickReplyExtensionModule());
        }
        for (IExtensionModule module : mExtModules) {
            module.onInit(mApplicationContext, mAppKey);
        }
    }

    /** Check for the location plugin */
    private static void checkLocationModule() {
        try {
            Class<?> cls = Class.forName(DEFAULT_LOCATION_MODULE);
            Constructor<?> constructor = cls.getConstructor();
            IExtensionModule NCbq = (IExtensionModule) constructor.newInstance();
            RLog.i(TAG, "add module " + NCbq.getClass().getSimpleName());
            mExtModules.add(NCbq);
        } catch (Exception e) {
            RLog.i(TAG, "Can't find " + DEFAULT_LOCATION_MODULE);
        }
    }

    public IExtensionConfig getExtensionConfig() {
        if (mExtensionConfig == null) {
            mExtensionConfig = new DefaultExtensionConfig();
        }
        return mExtensionConfig;
    }

    /**
     * Sets the input bar related configuration.
     *
     * @param extensionConfig Extension configuration.
     */
    public void setExtensionConfig(IExtensionConfig extensionConfig) {
        mExtensionConfig = extensionConfig;
    }

    /**
     * Registers a custom {@link IExtensionModule}. After registration, the module can be retrieved
     * via {@link #getExtensionModules()}.
     *
     * <pre>
     * Note:
     * 1. Call this method after SDK initialization {@link ai.nexconn.chatui.NCIM#init(Application, String, boolean)} to register custom {@link IExtensionModule}.
     * 2. Must be called before entering the conversation UI.
     * </pre>
     *
     * @param extensionModule Custom module.
     * @throws IllegalArgumentException Thrown when the IExtensionModule parameter is invalid.
     */
    public void registerExtensionModule(IExtensionModule extensionModule) {
        if (mExtModules == null) {
            RLog.e(TAG, "Not init in the main process.");
            return;
        }
        if (extensionModule == null || mExtModules.contains(extensionModule)) {
            RLog.e(TAG, "Illegal extensionModule.");
            return;
        }
        RLog.i(TAG, "registerExtensionModule " + extensionModule.getClass().getSimpleName());
        mExtModules.add(extensionModule);
        extensionModule.onInit(mApplicationContext, mAppKey);
    }

    public void registerExtensionModule(int index, IExtensionModule extensionModule) {
        if (mExtModules == null) {
            RLog.e(TAG, "Not init in the main process.");
            return;
        }
        if (extensionModule == null || mExtModules.contains(extensionModule)) {
            RLog.e(TAG, "Illegal extensionModule.");
            return;
        }
        RLog.i(TAG, "registerExtensionModule " + extensionModule.getClass().getSimpleName());
        mExtModules.add(index, extensionModule);
        extensionModule.onInit(mApplicationContext, mAppKey);
    }

    /**
     * Adds a custom {@link IExtensionModule}. After adding, the module can be retrieved via {@link
     * #getExtensionModules()}.
     *
     * <pre>
     * Note:
     * 1. This method only adds the custom IExtensionModule to the list without calling {@link IExtensionModule#onInit(Context, String)}.
     * 2. For registration, use {@link #registerExtensionModule(IExtensionModule)}.
     * 3. This method is intended for reordering IExtensionModules.
     * </pre>
     *
     * @param extensionModule Custom module.
     * @throws IllegalArgumentException Thrown when the IExtensionModule parameter is invalid.
     */
    public void addExtensionModule(IExtensionModule extensionModule) {
        if (mExtModules == null) {
            RLog.e(TAG, "Not init in the main process.");
            return;
        }
        if (extensionModule == null || mExtModules.contains(extensionModule)) {
            RLog.e(TAG, "Illegal extensionModule.");
            return;
        }
        RLog.i(TAG, "addExtensionModule " + extensionModule.getClass().getSimpleName());
        mExtModules.add(extensionModule);
    }

    /**
     * Unregisters an {@link IExtensionModule} module.
     *
     * <pre>
     * Note:
     * 1. Call this method after SDK initialization {@link ai.nexconn.chatui.IMCenter#init(Application, String, boolean)} to unregister {@link IExtensionModule}.
     * 2. Must be called before entering the conversation UI.
     * </pre>
     *
     * @param extensionModule The registered IExtensionModule module.
     * @throws IllegalArgumentException Thrown when the IExtensionModule parameter is invalid.
     */
    public void unregisterExtensionModule(IExtensionModule extensionModule) {
        if (mExtModules == null) {
            RLog.e(TAG, "Not init in the main process.");
            return;
        }
        if (mExtModules.remove(extensionModule)) {
            RLog.i(TAG, "unregisterExtensionModule " + extensionModule.getClass().getSimpleName());
        } else {
            RLog.e(TAG, "Illegal extensionModule.");
        }
    }

    /**
     * Gets the registered modules.
     *
     * @return List of registered modules.
     */
    public List<IExtensionModule> getExtensionModules() {
        return mExtModules;
    }

    public void addExtensionEventWatcher(IExtensionEventWatcher watcher) {
        if (!mExtensionEventWatcher.contains(watcher)) {
            mExtensionEventWatcher.add(watcher);
        }
    }

    public void removeExtensionEventWatcher(IExtensionEventWatcher watcher) {
        if (mExtensionEventWatcher.contains(watcher)) {
            mExtensionEventWatcher.remove(watcher);
        }
    }

    public List<IExtensionEventWatcher> getExtensionEventWatcher() {
        return mExtensionEventWatcher;
    }

    /** Called when the SDK disconnects. Users do not need to call this method again. */
    public void disconnect() {
        if (mExtModules == null) {
            return;
        }
        for (IExtensionModule extensionModule : mExtModules) {
            extensionModule.onDisconnect();
        }
    }

    /**
     * Called when the SDK receives a message. Users do not need to call this method again.
     * NCExtModuleManage routes the message to each {@link IExtensionModule} module.
     *
     * @param message The received message entity.
     */
    void onReceivedMessage(Message message) {
        for (IExtensionModule extensionModule : mExtModules) {
            extensionModule.onReceivedMessage(message);
        }
    }

    private static class SingletonHolder {
        static NCExtensionManager sInstance = new NCExtensionManager();
    }
}
