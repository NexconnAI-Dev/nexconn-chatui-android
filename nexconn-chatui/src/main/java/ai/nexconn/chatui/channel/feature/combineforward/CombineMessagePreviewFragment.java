package ai.nexconn.chatui.channel.feature.combineforward;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.HDVoiceMessage;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.model.CombineMsgItem;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.channel.MessageListAdapter;
import ai.nexconn.chatui.channel.event.action.DownloadEvent;
import ai.nexconn.chatui.channel.event.action.MessageEventListener;
import ai.nexconn.chatui.channel.longclick.MessageLongClickCopyItem;
import ai.nexconn.chatui.channel.messagelist.provider.MessageClickType;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.manager.AudioPlayManager;
import ai.nexconn.chatui.manager.IAudioPlayListener;
import ai.nexconn.chatui.model.State;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.route.RouteUtils;
import ai.nexconn.chatui.widget.FixedLinearLayoutManager;
import ai.nexconn.chatui.widget.adapter.IViewProviderListener;
import ai.nexconn.chatui.widget.component.HeadComponent;
import ai.nexconn.chatui.widget.dialog.MessageLongClickPopup;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * Combined-forward message preview Fragment.
 *
 * <p>Uses {@link MessageListAdapter} to render the message list, reusing all existing message
 * ItemProviders. Supports UI display of all message types including text, image/GIF, voice, video,
 * file, etc.
 */
public class CombineMessagePreviewFragment
        extends BaseViewModelFragment<CombineMessagePreviewViewModel>
        implements IViewProviderListener<UiMessage> {

    private static final String TAG = "CombinePreviewFragment";

    private RecyclerView recyclerView;
    private ProgressBar loadingView;
    private LinearLayout failedView;
    private HeadComponent headComponent;
    private MessageListAdapter messageListAdapter;
    private final List<UiMessage> uiMessages = new ArrayList<>();
    private ChannelType previewChannelType = ChannelType.DIRECT;
    private String previewTargetId = "";
    private final MessageLongClickCopyItem copyLongClickItem = new MessageLongClickCopyItem();
    private final MessageEventListener downloadEventListener =
            new MessageEventListener() {
                @Override
                public void onDownloadMessage(DownloadEvent event) {
                    updateDownloadedMessage(event);
                }
            };

    @NonNull
    @Override
    protected CombineMessagePreviewViewModel onCreateViewModel(@NonNull Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(CombineMessagePreviewViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_fragment_combine_preview, container, false);

        headComponent = view.findViewById(R.id.nc_combine_head);
        configureTitleEllipsize();
        int backResId =
                ChatUIThemeManager.getAttrResId(context, R.attr.nc_navigation_bar_btn_back_img);
        if (backResId == 0) {
            backResId = R.drawable.nc_lively_back_light;
        }
        headComponent.setLeftTextDrawable(backResId);
        headComponent.setLeftClickListener(
                v -> {
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                });
        recyclerView = view.findViewById(R.id.nc_message_list);
        loadingView = view.findViewById(R.id.nc_combine_loading);
        failedView = view.findViewById(R.id.nc_combine_failed);

        // RecyclerView + Adapter
        messageListAdapter = new MessageListAdapter(this);
        recyclerView.setLayoutManager(new FixedLinearLayoutManager(context));
        recyclerView.setAdapter(messageListAdapter);
        NCChatUI.addMessageEventListener(downloadEventListener);

        return view;
    }

    private void configureTitleEllipsize() {
        if (headComponent == null) {
            return;
        }
        TextView titleTextView = headComponent.getTitleTextView();
        if (titleTextView == null) {
            return;
        }
        titleTextView.setSingleLine(true);
        titleTextView.setMaxLines(1);
        titleTextView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
    }

    @Override
    protected void onViewReady(@NonNull CombineMessagePreviewViewModel viewModel) {
        previewChannelType = viewModel.getPreviewChannelType();
        previewTargetId = viewModel.getPreviewTargetId();

        viewModel
                .getTitleLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        title -> {
                            if (headComponent != null && !TextUtils.isEmpty(title)) {
                                headComponent.setTitleText(title);
                            }
                        });

        viewModel
                .getLoadStateLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        state -> {
                            if (state == null) return;
                            switch (state) {
                                case LOADING:
                                    showLoading();
                                    break;
                                case SUCCESS:
                                    showContent();
                                    break;
                                case FAILED:
                                    showFailed();
                                    break;
                            }
                        });

        viewModel
                .getMsgListLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        msgList -> {
                            if (msgList == null || msgList.isEmpty()) return;
                            buildUiMessages(msgList);
                            if (messageListAdapter != null) {
                                messageListAdapter.setDataCollection(uiMessages);
                            }
                            showContent();
                        });
    }

    // ===========================
    // IViewProviderListener
    // ===========================

    @Override
    public void onViewClick(int clickType, UiMessage data) {
        if (data == null) return;
        if (clickType == MessageClickType.AUDIO_CLICK) {
            handleAudioClick(data);
        } else if (clickType == MessageClickType.CONTENT_CLICK) {
            handleContentClick(data);
        }
        // Other clicks (avatar, etc.) are ignored in the preview page
    }

    @Override
    public boolean onViewLongClick(View view, int clickType, UiMessage data) {
        if (clickType != MessageClickType.CONTENT_LONG_CLICK || data == null) {
            return false;
        }
        return showCopyLongClickPopup(view, data);
    }

    @Override
    public boolean onViewLongClick(int clickType, UiMessage data) {
        return onViewLongClick(null, clickType, data);
    }

    // ===========================
    // Private helpers
    // ===========================

    private boolean showCopyLongClickPopup(View anchorView, UiMessage message) {
        Context context = getContext();
        if (context == null || !copyLongClickItem.isEnabled(message)) {
            return false;
        }

        List<MessageLongClickPopup.OptionItem> optionItems = new ArrayList<>();
        optionItems.add(
                new MessageLongClickPopup.OptionItem(
                        copyLongClickItem.getTitle(context, message),
                        copyLongClickItem.getIconAttrResId()));

        MessageLongClickPopup popup =
                MessageLongClickPopup.newInstance(context, optionItems)
                        .setOptionsPopupDialogListener(
                                which -> copyLongClickItem.onAction(context, message));
        if (anchorView != null) {
            popup.setAnchorView(anchorView);
        }
        popup.show();
        return true;
    }

    /** Converts a list of CombineMsgItem to UiMessage list to drive the MessageListAdapter. */
    private void buildUiMessages(List<CombineMsgItem> msgList) {
        String currentUserId = NCEngine.getCurrentUserId();
        uiMessages.clear();
        for (CombineMsgItem item : msgList) {
            Message msg =
                    Message.createForPreview(
                            item,
                            currentUserId,
                            previewChannelType != null ? previewChannelType : ChannelType.DIRECT,
                            previewTargetId);
            if (msg != null) {
                markPreviewVoiceMessageListened(msg);
                uiMessages.add(new UiMessage(msg));
            }
        }
        RLog.d(TAG, "buildUiMessages: size=" + uiMessages.size());
    }

    private void markPreviewVoiceMessageListened(Message message) {
        if (message == null || !(message.getContent() instanceof HDVoiceMessage)) {
            return;
        }
        message.getReceivedStatusInfo().setListened();
    }

    /** Handles voice message click: downloads first if not downloaded, otherwise plays. */
    private void handleAudioClick(UiMessage uiMessage) {
        MessageContent content = uiMessage.getMessage().getContent();
        if (!(content instanceof ai.nexconn.chat.message.HDVoiceMessage)) return;

        ai.nexconn.chat.message.HDVoiceMessage voiceMsg =
                (ai.nexconn.chat.message.HDVoiceMessage) content;

        // Stop if another voice message is currently playing
        if (AudioPlayManager.getInstance().isPlaying()) {
            Uri playing = AudioPlayManager.getInstance().getPlayingUri();
            AudioPlayManager.getInstance().stopPlay();
            String localPath = voiceMsg.getLocalPath();
            if (playing != null
                    && !TextUtils.isEmpty(localPath)
                    && playing.toString().equals(localPath)) {
                // Clicked the currently playing message, just stop
                uiMessage.setPlaying(false);
                refreshMessage(uiMessage);
                return;
            }
        }

        Context context = requireContext();
        if (AudioPlayManager.getInstance().isInVOIPMode(context)) {
            ToastUtils.show(
                    context,
                    getString(ai.nexconn.chatui.R.string.nc_voip_occupying),
                    Toast.LENGTH_SHORT);
            return;
        }

        String localPath = voiceMsg.getLocalPath();
        if (hasPlayableLocalPath(localPath)) {
            playVoice(uiMessage, Uri.parse(localPath));
        } else {
            downloadAndPlayVoice(uiMessage, voiceMsg);
        }
    }

    private boolean hasPlayableLocalPath(String localPath) {
        if (TextUtils.isEmpty(localPath)) {
            return false;
        }
        Uri uri = Uri.parse(localPath);
        String scheme = uri.getScheme();
        if ("content".equalsIgnoreCase(scheme)) {
            return true;
        }
        if ("file".equalsIgnoreCase(scheme)) {
            String path = uri.getPath();
            return !TextUtils.isEmpty(path) && new java.io.File(path).exists();
        }
        return new java.io.File(localPath).exists();
    }

    private void downloadAndPlayVoice(
            UiMessage uiMessage, ai.nexconn.chat.message.HDVoiceMessage voiceMsg) {
        uiMessage.setState(ai.nexconn.chatui.model.State.PROGRESS);
        refreshMessage(uiMessage);

        uiMessage
                .getMessage()
                .downloadMedia(
                        new ai.nexconn.chat.handler.DownloadMediaMessageHandler() {
                            @Override
                            public void onSuccess(Message message) {
                                // Check if Fragment is still attached before accessing context
                                if (!isAdded() || getContext() == null) {
                                    RLog.w(TAG, "Fragment not attached, skip playVoice");
                                    return;
                                }
                                if (message != null && message.getContent() != null) {
                                    uiMessage.getMessage().setContent(message.getContent());
                                }
                                uiMessage.setState(State.NORMAL);
                                refreshMessage(uiMessage);
                                ai.nexconn.chat.message.HDVoiceMessage updated =
                                        (ai.nexconn.chat.message.HDVoiceMessage)
                                                uiMessage.getMessage().getContent();
                                String path = updated != null ? updated.getLocalPath() : null;
                                if (!TextUtils.isEmpty(path)) {
                                    playVoice(uiMessage, Uri.parse(path));
                                }
                            }

                            @Override
                            public void onProgress(Message message, int progress) {
                                if (!isAdded()) return;
                                uiMessage.setState(State.PROGRESS);
                                uiMessage.setProgress(progress);
                                refreshMessage(uiMessage);
                            }

                            @Override
                            public void onError(
                                    Message message, ai.nexconn.chat.error.NCError error) {
                                if (!isAdded()) return;
                                uiMessage.setState(State.ERROR);
                                refreshMessage(uiMessage);
                            }

                            @Override
                            public void onCanceled(Message message) {
                                if (!isAdded()) return;
                                uiMessage.setState(State.CANCEL);
                                refreshMessage(uiMessage);
                            }
                        });
    }

    private void playVoice(UiMessage uiMessage, Uri voiceUri) {
        if (voiceUri == null) return;
        // Check if Fragment is still attached before accessing context
        if (!isAdded() || getContext() == null) {
            RLog.w(TAG, "Fragment not attached, skip playVoice");
            return;
        }
        markVoiceMessageListened(uiMessage);
        Context context = requireContext();
        AudioPlayManager.getInstance()
                .startPlay(
                        context,
                        voiceUri,
                        new IAudioPlayListener() {
                            @Override
                            public void onStart(Uri uri) {
                                if (!isAdded()) return;
                                uiMessage.setPlaying(true);
                                refreshMessage(uiMessage);
                            }

                            @Override
                            public void onStop(Uri uri) {
                                if (!isAdded()) return;
                                uiMessage.setPlaying(false);
                                refreshMessage(uiMessage);
                            }

                            @Override
                            public void onComplete(Uri uri) {
                                if (!isAdded()) return;
                                uiMessage.setPlaying(false);
                                refreshMessage(uiMessage);
                            }
                        });
    }

    private void markVoiceMessageListened(UiMessage uiMessage) {
        if (uiMessage == null || uiMessage.getMessage() == null) {
            return;
        }
        uiMessage.getMessage().getReceivedStatusInfo().setListened();
        uiMessage.change();
        refreshMessage(uiMessage);
    }

    /**
     * Handles content click routing inside combined-forward preview.
     *
     * <p>Rules: - ImageMessage/GIFMessage: open preview page - ShortVideoMessage: directly open
     * short-video player, which owns download/playback logic - CombineMessage: open nested
     * combined-forward preview - HDVoiceMessage: play audio
     */
    private void handleContentClick(UiMessage uiMessage) {
        // Check if Fragment is still attached before accessing context
        if (!isAdded() || getContext() == null) {
            RLog.w(TAG, "Fragment not attached, skip handleContentClick");
            return;
        }
        MessageContent content = uiMessage.getMessage().getContent();
        if (content instanceof ai.nexconn.chat.message.ImageMessage) {
            RouteUtils.routeToCombinePicturePagerActivity(requireContext(), uiMessage.getMessage());
        } else if (content instanceof ai.nexconn.chat.message.GIFMessage) {
            ai.nexconn.chatui.utils.message.MessageHolder.holdMessage(uiMessage.getMessage());
            startActivity(
                    new android.content.Intent(
                            requireContext(), ai.nexconn.chatui.activity.GIFPreviewActivity.class));
        } else if (content instanceof ai.nexconn.chat.message.ShortVideoMessage) {
            ai.nexconn.chatui.utils.message.MessageHolder.holdMessage(uiMessage.getMessage());
            android.content.Intent intent =
                    new android.content.Intent(
                            requireContext(),
                            ai.nexconn.chatui.shortvideo.player.ShortVideoPlayerActivity.class);
            intent.putExtra("fromList", false);
            intent.putExtra("displayCurrentVideoOnly", true);
            startActivity(intent);
        } else if (content instanceof ai.nexconn.chat.message.CombineMessage) {
            RouteUtils.routeToCombineMessageDetailActivity(
                    requireContext(), uiMessage.getMessage());
        } else if (content instanceof ai.nexconn.chat.message.HDVoiceMessage) {
            handleAudioClick(uiMessage);
        } else if (content != null) {
            RLog.d(
                    TAG,
                    "handleContentClick: unsupported message type: "
                            + content.getClass().getSimpleName());
        }
    }

    private void refreshMessage(UiMessage uiMessage) {
        if (!isAdded() || messageListAdapter == null) return;
        int idx = uiMessages.indexOf(uiMessage);
        if (idx >= 0) {
            uiMessage.setChange(true);
            messageListAdapter.setDataCollection(uiMessages);
        }
    }

    private void updateDownloadedMessage(DownloadEvent event) {
        if (event == null || event.getMessage() == null) {
            return;
        }
        Message eventMessage = event.getMessage();
        int clientId = eventMessage.getClientId();
        for (UiMessage uiMessage : uiMessages) {
            Message msg = uiMessage.getMessage();
            if (msg == null || msg.getClientId() != clientId) {
                continue;
            }
            switch (event.getEvent()) {
                case DownloadEvent.SUCCESS:
                    if (eventMessage.getContent() != null) {
                        msg.setContent(eventMessage.getContent());
                    }
                    uiMessage.setState(State.NORMAL);
                    uiMessage.setProgress(100);
                    break;
                case DownloadEvent.PROGRESS:
                    uiMessage.setState(State.PROGRESS);
                    uiMessage.setProgress(event.getProgress());
                    break;
                case DownloadEvent.ERROR:
                    uiMessage.setState(State.ERROR);
                    break;
                case DownloadEvent.CANCEL:
                    uiMessage.setState(State.CANCEL);
                    break;
                default:
                    break;
            }
            refreshMessage(uiMessage);
            break;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        AudioPlayManager.getInstance().stopPlay();
    }

    @Override
    public void onDestroyView() {
        NCChatUI.removeMessageEventListener(downloadEventListener);
        super.onDestroyView();
    }

    private void showLoading() {
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (loadingView != null) loadingView.setVisibility(View.VISIBLE);
        if (failedView != null) failedView.setVisibility(View.GONE);
    }

    private void showContent() {
        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        if (loadingView != null) loadingView.setVisibility(View.GONE);
        if (failedView != null) failedView.setVisibility(View.GONE);
    }

    private void showFailed() {
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (loadingView != null) loadingView.setVisibility(View.GONE);
        if (failedView != null) failedView.setVisibility(View.VISIBLE);
    }
}
