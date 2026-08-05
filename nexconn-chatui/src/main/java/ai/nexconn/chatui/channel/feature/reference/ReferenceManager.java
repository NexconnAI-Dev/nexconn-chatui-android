package ai.nexconn.chatui.channel.feature.reference;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.handler.MessageHandler;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.ReferenceMessage;
import ai.nexconn.chat.message.TextMessage;
import ai.nexconn.chat.message.model.MessageDeletedEvent;
import ai.nexconn.chat.message.model.ReferenceMessageStatus;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.ChannelViewModel;
import ai.nexconn.chatui.channel.event.action.DeleteEvent;
import ai.nexconn.chatui.channel.event.action.MessageEventListener;
import ai.nexconn.chatui.channel.extension.IExtensionModule;
import ai.nexconn.chatui.channel.extension.InputMode;
import ai.nexconn.chatui.channel.extension.NCExtension;
import ai.nexconn.chatui.channel.extension.NCExtensionManager;
import ai.nexconn.chatui.channel.extension.NCExtensionViewModel;
import ai.nexconn.chatui.channel.extension.component.emoticon.IEmoticonTab;
import ai.nexconn.chatui.channel.extension.component.plugin.IPluginModule;
import ai.nexconn.chatui.channel.feature.editmessage.EditMessageManager;
import ai.nexconn.chatui.channel.feature.mention.IExtensionEventWatcher;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.log.RLog;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReferenceManager implements IExtensionModule, IExtensionEventWatcher {
    private final String TAG = ReferenceManager.class.getSimpleName();
    private ReferenceMessage mReferenceMessage;
    private WeakReference<NCExtension> mNCExtension;
    private WeakReference<Fragment> mFragment;
    private UiMessage mUiMessage;
    private Stack<ReferenceInstance> stack = new Stack<>();
    private WeakReference<NCExtensionViewModel> messageViewModel;
    private List<ReferenceStatusListener> mReferenceStatusListenerList =
            new CopyOnWriteArrayList<>();

    private static class SingletonHolder {
        static ReferenceManager instance = new ReferenceManager();
    }

    public static ReferenceManager getInstance() {
        return SingletonHolder.instance;
    }

    public void setReferenceStatusListener(ReferenceStatusListener listener) {
        mReferenceStatusListenerList.add(listener);
    }

    public void removeReferenceStatusListener(ReferenceStatusListener listener) {
        mReferenceStatusListenerList.remove(listener);
    }

    @Override
    public void onInit(Context context, String appKey) {
        // do nothing
    }

    @Override
    public void onAttachedToExtension(Fragment fragment, final NCExtension extension) {
        if (fragment == null
                || fragment.isDetached()
                || fragment.getContext() == null
                || fragment.getFragmentManager() == null) {
            return;
        }
        mFragment = new WeakReference<>(fragment);
        mNCExtension = new WeakReference<>(extension);
        ReferenceInstance referenceInstance;
        referenceInstance = new ReferenceInstance();
        referenceInstance.mFragment = mFragment;
        referenceInstance.mNCExtension = mNCExtension;
        stack.add(referenceInstance);
        if (!NCChatUIConfig.featureConfig().isReferenceEnable()) {
            return;
        }
        messageViewModel =
                new WeakReference<>(
                        new ViewModelProvider(fragment).get(NCExtensionViewModel.class));
        NCExtensionViewModel extensionViewModel = messageViewModel.get();
        if (extensionViewModel != null) {
            extensionViewModel
                    .getInputModeLiveData()
                    .observe(
                            fragment,
                            new Observer<InputMode>() {
                                @Override
                                public void onChanged(InputMode inputMode) {
                                    if (inputMode.equals(InputMode.VoiceInput)) {
                                        hideReferenceView();
                                    }
                                }
                            });
        }

        NCExtensionManager.getInstance().addExtensionEventWatcher(this);
        NCEngine.addMessageHandler(RECALL_HANDLER_ID, mRecallMessageHandler);
        NCChatUI.addMessageEventListener(mMessageEventListener);
    }

    private MessageEventListener mMessageEventListener =
            new MessageEventListener() {
                @Override
                public void onDeleteMessage(DeleteEvent event) {
                    if (mUiMessage != null
                            && mUiMessage.getMessage() != null
                            && event != null
                            && event.getMessageIds() != null
                            && mUiMessage
                                            .getMessage()
                                            .getChannelIdentifier()
                                            .getChannelType()
                                            .getValue()
                                    == event.getConversationType().getValue()
                            && mUiMessage
                                    .getMessage()
                                    .getChannelIdentifier()
                                    .getChannelId()
                                    .equals(event.getTargetId())) {
                        int messageId = mUiMessage.getMessage().getClientId();
                        for (int id : event.getMessageIds()) {
                            if (id == messageId) {
                                hideReferenceView();
                                break;
                            }
                        }
                    }
                }
            };

    private static final String RECALL_HANDLER_ID = "ReferenceManager_recall";

    private final MessageHandler mRecallMessageHandler =
            new MessageHandler() {
                @Override
                public void onMessageDeleted(@NonNull MessageDeletedEvent event) {
                    List<Message> deleted = event.getMessages();
                    if (deleted == null) return;
                    for (Message msg : deleted) {
                        if (msg != null) {
                            onMessageRecalledInternal(msg);
                        }
                    }
                }
            };

    @SuppressWarnings("unused")
    private void onMessageRecalledInternal(Message message) {
        if (mFragment == null) {
            return;
        }
        Fragment fragment = mFragment.get();
        if (fragment == null) {
            return;
        }
        if (mUiMessage != null
                && message != null
                && !TextUtils.isEmpty(mUiMessage.getMessageId())
                && mUiMessage.getMessage().getMessageId().equals(message.getMessageId())) {
            if (fragment.getActivity() == null || fragment.getContext() == null) {
                return;
            }
            new AlertDialog.Builder(fragment.getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                    .setMessage(fragment.getContext().getString(R.string.nc_recall_success))
                    .setPositiveButton(
                            fragment.getContext().getString(R.string.nc_dialog_ok),
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                    .setCancelable(false)
                    .show();
            hideReferenceView();
            if (messageViewModel != null) {
                NCExtensionViewModel viewModel = messageViewModel.get();
                if (viewModel != null) {
                    viewModel.collapseExtensionBoard();
                }
            }
        }
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
    public List<IPluginModule> getPluginModules(ai.nexconn.chat.channel.ChannelType channelType) {
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

    @Override
    public void onTextChanged(
            Context context,
            ai.nexconn.chat.channel.ChannelType type,
            String targetId,
            int cursorPos,
            int count,
            String text) {
        // do nothing
    }

    @Override
    public void onSendToggleClick(Message message) {
        if (!(message.getContent() instanceof TextMessage)) {
            RLog.e(TAG, "primary message content must be TextMessage");
            return;
        }
        String primaryString = ((TextMessage) message.getContent()).getText();
        if (mReferenceMessage != null) {
            mReferenceMessage.setContent(primaryString);
            mReferenceMessage.setMentionedInfo(message.getContent().getMentionedInfo());
            message.setContent(mReferenceMessage);
            hideReferenceView();
        }
    }

    /**
     * Merges the pending {@link TextMessage} with the current reference.
     *
     * <p>If a reference message exists (reference bar shown above the input), fills the text and
     * MentionedInfo from textMessage into {@link ReferenceMessage}, hides the reference bar, and
     * returns the {@link ReferenceMessage}; otherwise returns textMessage as-is.
     *
     * @param textMessage the plain text message constructed from the input field
     * @return a {@link ReferenceMessage} with the reference, or the original textMessage if none
     */
    public ai.nexconn.chat.message.MessageContent applyReference(TextMessage textMessage) {
        if (mReferenceMessage != null) {
            mReferenceMessage.setContent(textMessage.getText());
            mReferenceMessage.setMentionedInfo(textMessage.getMentionedInfo());
            ai.nexconn.chat.message.MessageContent result = mReferenceMessage;
            hideReferenceView();
            return result;
        }
        return textMessage;
    }

    @Override
    public void onDestroy(ai.nexconn.chat.channel.ChannelType type, String targetId) {
        mReferenceMessage = null;
        mUiMessage = null;
        stack.pop();
        if (stack.size() > 0) {
            ReferenceInstance referenceInstance = stack.peek();
            mNCExtension = referenceInstance.mNCExtension;
            mFragment = referenceInstance.mFragment;
            referenceInstance.mNCExtension = null;
            referenceInstance.mFragment = null;
        } else {
            NCEngine.removeMessageHandler(RECALL_HANDLER_ID);
            NCChatUI.removeMessageEventListener(mMessageEventListener);
            NCExtensionManager.getInstance().removeExtensionEventWatcher(this);
            mNCExtension = null;
            mFragment = null;
        }
    }

    /**
     * Shows the reference message bar.
     *
     * @param context the context
     * @param uiMessage the message entity
     * @return whether the display was successful
     */
    public boolean showReferenceView(Context context, UiMessage uiMessage) {
        if (EditMessageManager.getInstance().isEditMessageState()) {
            return false;
        }
        // If context is null, try using the Fragment's context
        if (context == null && mFragment != null) {
            Fragment frag = mFragment.get();
            if (frag != null && !frag.isDetached()) {
                context = frag.getContext();
            }
        }
        return showReferenceView(context, uiMessage, true);
    }

    public boolean showReferenceViewInEditMode(Context context, UiMessage uiMessage) {
        if (EditMessageManager.getInstance().isEditMessageState()) {
            // In edit mode, delay exiting edit mode and showing reference UI to prevent keyboard
            // flicker.
            postDelayed(
                    () -> {
                        EditMessageManager.getInstance().exitEditMode();
                        postDelayed(() -> showReferenceView(context, uiMessage, false));
                    });
        } else {
            showReferenceView(context, uiMessage);
        }
        return true;
    }

    /**
     * Shows the reference message bar.
     *
     * @param context the context
     * @param uiMessage the message entity
     * @return whether the display was successful
     */
    public boolean showReferenceView(Context context, UiMessage uiMessage, boolean showKeyBoard) {
        if (mNCExtension == null
                || mFragment == null
                || context == null
                || uiMessage == null
                || uiMessage.getMessage() == null) {
            return false;
        }
        NCExtension NCExtension = mNCExtension.get();
        Fragment fragment = mFragment.get();
        if (NCExtension == null
                || fragment == null
                || fragment.isDetached()
                || fragment.getContext() == null
                || fragment.getFragmentManager() == null) {
            return false;
        }
        mUiMessage = uiMessage;
        mReferenceMessage = new ReferenceMessage();
        mReferenceMessage.setReferMsgSenderId(uiMessage.getMessage().getSenderUserId());
        mReferenceMessage.setReferMsg(uiMessage.getMessage().getContent());
        mReferenceMessage.setReferMsgId(uiMessage.getMessageId());
        // Detect whether the referenced message was edited using the latest model from the
        // authoritative message list, to avoid a stale reference snapshot.
        if (isReferencedMessageEdited(
                uiMessage.getMessage().getMessageId(),
                uiMessage.getMessage().getHasChanged(),
                resolveCurrentUiMessages(fragment))) {
            mReferenceMessage.setReferMsgStatus(ReferenceMessageStatus.MODIFIED);
        }
        ReferenceView reference =
                new ReferenceView(
                        context,
                        NCExtension.getContainer(
                                ai.nexconn.chatui.channel.extension.NCExtension.ContainerType
                                        .ATTACH),
                        uiMessage);
        reference.setReferenceCancelListener(this::hideReferenceView);
        NCExtension.setAttachedInfo(reference.getReferenceView());
        final NCExtensionViewModel extensionViewModel =
                new ViewModelProvider(fragment).get(NCExtensionViewModel.class);
        extensionViewModel.getInputModeLiveData().postValue(InputMode.TextInput);
        if (showKeyBoard) {
            NCExtension.postDelayed(() -> extensionViewModel.setSoftInputKeyBoard(true), 100);
        }
        return true;
    }

    /**
     * Determines whether the referenced message is in the "edited" state.
     *
     * <p>Prefers the {@code hasChanged} of the latest model with the same messageId in the
     * authoritative message list; falls back to the reference snapshot's own {@code hasChanged}
     * when the message is absent from the list, to avoid a stale reference snapshot.
     *
     * @param referMsgId the referenced message UID
     * @param fallbackHasChanged the reference snapshot's own hasChanged
     * @param currentMessages the current authoritative message list (nullable)
     * @return whether the referenced message has been edited
     */
    static boolean isReferencedMessageEdited(
            String referMsgId, boolean fallbackHasChanged, List<UiMessage> currentMessages) {
        if (!TextUtils.isEmpty(referMsgId) && currentMessages != null) {
            for (UiMessage item : currentMessages) {
                if (item != null
                        && item.getMessage() != null
                        && referMsgId.equals(item.getMessage().getMessageId())) {
                    return item.getMessage().getHasChanged();
                }
            }
        }
        return fallbackHasChanged;
    }

    /** Resolves the conversation message list from the extension's bound Fragment scope; returns null to fall back on failure. */
    private List<UiMessage> resolveCurrentUiMessages(Fragment fragment) {
        try {
            return new ViewModelProvider(fragment).get(ChannelViewModel.class).getUiMessages();
        } catch (Exception e) {
            RLog.e(TAG, "resolveCurrentUiMessages failed: " + e.getMessage());
            return null;
        }
    }

    private void postDelayed(Runnable r) {
        new Handler(Looper.getMainLooper()).postDelayed(r, 100);
    }

    public void hideReferenceView() {
        mReferenceMessage = null;
        NCExtension NCExtension = null;

        if (mNCExtension != null) {
            NCExtension = mNCExtension.get();
        }

        if (NCExtension != null) {
            NCExtension.setAttachedInfo(null);
        }
        mUiMessage = null;
        for (ReferenceStatusListener listener : mReferenceStatusListenerList) {
            listener.onHide();
        }
    }

    @Override
    public void onDeleteClick(
            ai.nexconn.chat.channel.ChannelType type,
            String targetId,
            EditText editText,
            int cursorPos) {
        // default implementation ignored
    }

    public UiMessage getUiMessage() {
        return mUiMessage;
    }

    public interface ReferenceStatusListener {
        /** Callback when the reference message bar is hidden. */
        void onHide();
    }
}
