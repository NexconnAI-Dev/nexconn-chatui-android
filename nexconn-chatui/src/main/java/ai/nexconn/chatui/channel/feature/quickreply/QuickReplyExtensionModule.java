package ai.nexconn.chatui.channel.feature.quickreply;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.extension.IExtensionModule;
import ai.nexconn.chatui.channel.extension.InputMode;
import ai.nexconn.chatui.channel.extension.NCExtension;
import ai.nexconn.chatui.channel.extension.NCExtensionViewModel;
import ai.nexconn.chatui.channel.extension.component.emoticon.IEmoticonTab;
import ai.nexconn.chatui.channel.extension.component.plugin.IPluginModule;
import ai.nexconn.chatui.channel.feature.editmessage.EditMessageManager;
import ai.nexconn.chatui.channel.feature.reference.ReferenceManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import java.lang.ref.WeakReference;
import java.util.List;

public class QuickReplyExtensionModule implements IExtensionModule {

    private View mQuickReplyIcon;
    private boolean isQuickReplyShow;
    private WeakReference<NCExtension> mExtension;
    private RelativeLayout attachContainer;
    private IQuickReplyProvider provider;

    @Override
    public void onInit(Context context, String appKey) {
        // do nothing
    }

    @Override
    public void onAttachedToExtension(final Fragment fragment, final NCExtension extension) {
        final ChannelType type = extension.getConversationType();
        provider = NCChatUIConfig.featureConfig().getQuickReplyProvider();
        mExtension = new WeakReference<>(extension);
        if (showReply(provider, type)
                && fragment != null
                && fragment.getContext() != null
                && !fragment.isDetached()) {
            final NCExtensionViewModel NCExtensionViewModel =
                    new ViewModelProvider(fragment).get(NCExtensionViewModel.class);
            attachContainer = extension.getContainer(NCExtension.ContainerType.ATTACH);
            attachContainer.removeAllViews();
            mQuickReplyIcon =
                    LayoutInflater.from(fragment.getContext())
                            .inflate(R.layout.nc_ext_quick_reply_icon, attachContainer, false);
            mQuickReplyIcon
                    .findViewById(R.id.ext_common_phrases)
                    .setBackgroundResource(R.drawable.nc_lively_common_background_radius_8);
            attachContainer.addView(mQuickReplyIcon);
            attachContainer.setVisibility(View.VISIBLE);
            NCExtensionViewModel.getInputModeLiveData()
                    .observe(
                            fragment,
                            new Observer<InputMode>() {
                                @Override
                                public void onChanged(InputMode inputMode) {
                                    if (inputMode != InputMode.QuickReplyMode) {
                                        isQuickReplyShow = false;
                                    }
                                }
                            });
            NCChatUIConfig.featureConfig()
                    .getIsQuickReply()
                    .observe(
                            fragment,
                            new Observer<Boolean>() {
                                @Override
                                public void onChanged(Boolean aBoolean) {
                                    if (isQuickReplyShow) {
                                        uploadReply(
                                                fragment.getContext(),
                                                extension,
                                                type,
                                                NCExtensionViewModel);
                                    }
                                }
                            });
            mQuickReplyIcon
                    .findViewById(R.id.ext_common_phrases)
                    .setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (NCChatUIConfig.channelConfig().getChannelClickListener()
                                                    != null
                                            && NCChatUIConfig.channelConfig()
                                                    .getChannelClickListener()
                                                    .onQuickReplyClick(v.getContext())) {
                                        return;
                                    }
                                    provider =
                                            NCChatUIConfig.featureConfig().getQuickReplyProvider();
                                    if (showReply(provider, type)) {
                                        if (isQuickReplyShow
                                                && NCExtensionViewModel.getExtensionBoardState()
                                                        .getValue()) {
                                            isQuickReplyShow = false;
                                            if (NCExtensionViewModel.getEditTextWidget() != null) {
                                                NCExtensionViewModel.getEditTextWidget()
                                                        .requestFocus();
                                            }
                                            NCExtensionViewModel.getInputModeLiveData()
                                                    .setValue(InputMode.TextInput);
                                        } else {
                                            isQuickReplyShow = true;
                                            NCExtensionViewModel.getInputModeLiveData()
                                                    .setValue(InputMode.QuickReplyMode);
                                            uploadReply(
                                                    v.getContext(),
                                                    extension,
                                                    type,
                                                    NCExtensionViewModel);
                                        }
                                    }
                                }
                            });
            ReferenceManager.getInstance().setReferenceStatusListener(ReferenceStatusListener);
            EditMessageManager.getInstance().addStatusListener(editStatusListener);
        }
    }

    public void uploadReply(
            Context context,
            NCExtension extension,
            ChannelType type,
            NCExtensionViewModel NCExtensionViewModel) {
        provider = NCChatUIConfig.featureConfig().getQuickReplyProvider();
        RelativeLayout boardContainer = extension.getContainer(NCExtension.ContainerType.BOARD);
        boardContainer.removeAllViews();
        QuickReplyBoard quickReplyBoard =
                new QuickReplyBoard(
                        context,
                        boardContainer,
                        provider.getPhraseList(type),
                        new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(
                                    AdapterView<?> parent, View view, int position, long id) {
                                isQuickReplyShow = false;
                                if (NCExtensionViewModel.getEditTextWidget() != null) {
                                    NCExtensionViewModel.getEditTextWidget().requestFocus();
                                }
                                NCExtensionViewModel.getInputModeLiveData()
                                        .setValue(InputMode.TextInput);
                            }
                        });
        quickReplyBoard.setAttachedConversation(extension);
        boardContainer.addView(quickReplyBoard.getRootView());
        boardContainer.setVisibility(View.VISIBLE);
    }

    private boolean showReply(IQuickReplyProvider iQuickReplyProvider, ChannelType type) {
        return iQuickReplyProvider != null
                && iQuickReplyProvider.getPhraseList(type) != null
                && iQuickReplyProvider.getPhraseList(type).size() > 0;
    }

    @Override
    public void onDetachedFromExtension() {
        ReferenceManager.getInstance().removeReferenceStatusListener(ReferenceStatusListener);
        EditMessageManager.getInstance().removeStatusListener(editStatusListener);
        // The following code is used to resolve memory leaks.

        if (mExtension != null) {
            NCExtension extension = mExtension.get();
            if (extension == null) {
                return;
            }
            extension.removeView(attachContainer);
            attachContainer = null;
        }

        if (mQuickReplyIcon != null) {
            mQuickReplyIcon = null;
        }
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
        // default implementation ignored
    }

    private final ReferenceManager.ReferenceStatusListener ReferenceStatusListener =
            new ReferenceManager.ReferenceStatusListener() {
                @Override
                public void onHide() {
                    NCExtension extension = mExtension.get();
                    if (extension != null) {
                        extension.setAttachedInfo(mQuickReplyIcon);
                    }
                }
            };
    private final EditMessageManager.StatusListener editStatusListener =
            new EditMessageManager.StatusListener() {
                @Override
                public void onVisibilityChanged(boolean isVisible) {
                    NCExtension extension = mExtension.get();
                    if (extension != null) {
                        extension.setAttachedInfo(isVisible ? null : mQuickReplyIcon);
                    }
                }
            };
}
