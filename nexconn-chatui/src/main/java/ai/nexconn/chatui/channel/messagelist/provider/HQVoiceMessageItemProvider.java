package ai.nexconn.chatui.channel.messagelist.provider;

import ai.nexconn.chat.message.HDVoiceMessage;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.model.MessageDirection;
import ai.nexconn.chat.message.model.SentStatus;
import ai.nexconn.chat.message.model.SpeechToTextInfo;
import ai.nexconn.chat.message.model.SpeechToTextStatus;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.handler.AppSettingsHandler;
import ai.nexconn.chatui.handler.SpeechToTextHandler;
import ai.nexconn.chatui.manager.AudioRecordManager;
import ai.nexconn.chatui.model.State;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.view.RTLUtils;
import ai.nexconn.chatui.widget.LoadingDotsView;
import ai.nexconn.chatui.widget.TextAnimationHelper;
import ai.nexconn.chatui.widget.adapter.IViewProviderListener;
import ai.nexconn.chatui.widget.adapter.ViewHolder;
import ai.nexconn.chatui.widget.dialog.MessageLongClickPopup;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.LayoutDirection;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.text.TextUtilsCompat;
import androidx.core.widget.TextViewCompat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class HQVoiceMessageItemProvider extends BaseMessageItemProvider<HDVoiceMessage> {
    private static final String TAG = "HQVoiceMessageItemProvider";

    public HQVoiceMessageItemProvider() {
        mConfig.showReadState = true;
        mConfig.showContentBubble = false;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.nc_item_hq_voice_message, parent, false);
        return new ViewHolder(parent.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(
            ViewHolder holder,
            ViewHolder parentHolder,
            final HDVoiceMessage message,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        boolean isSender = uiMessage.getMessage().getDirection() == MessageDirection.SEND;
        holder.setBackgroundRes(
                R.id.nc_voice_bg,
                ChatUIThemeManager.getAttrResId(
                        holder.getContext(),
                        isSender
                                ? R.attr.nc_conversation_msg_send_background
                                : R.attr.nc_conversation_msg_receiver_background));
        int minWidth = 70, maxWidth = 204;
        float scale = holder.getContext().getResources().getDisplayMetrics().density;
        minWidth = (int) (minWidth * scale + 0.5f);
        maxWidth = (int) (maxWidth * scale + 0.5f);
        int duration = AudioRecordManager.getInstance().getMaxVoiceDuration();
        View NCVoiceBgView = holder.getView(R.id.nc_voice_bg);
        TextView NCDuration = holder.getView(R.id.nc_duration);
        if (!checkViewsValid(NCVoiceBgView, NCDuration)) {
            RLog.e(TAG, "checkViewsValid error," + uiMessage.getMessageType());
            return;
        }
        NCVoiceBgView.getLayoutParams().width =
                minWidth + (maxWidth - minWidth) / duration * message.getDuration();
        if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
                == LayoutDirection.RTL) {
            holder.setText(R.id.nc_duration, String.format("\"%s", message.getDuration()));
        } else {
            holder.setText(R.id.nc_duration, String.format("%s\"", message.getDuration()));
        }

        float voiceIconScaleX = RTLUtils.isRtl(holder.getContext()) ? -1f : 1f;

        if (uiMessage.getMessage().getDirection() == MessageDirection.SEND) {
            AnimationDrawable animationDrawable =
                    (AnimationDrawable)
                            holder.getContext()
                                    .getResources()
                                    .getDrawable(
                                            ChatUIThemeManager.getAttrResId(
                                                    holder.getContext(),
                                                    R.attr.nc_icon_voice_send_animator));
            holder.setVisible(R.id.nc_voice, false);
            holder.setVisible(R.id.nc_voice_send, true);
            NCDuration.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) NCDuration.getLayoutParams();
            lp.setMarginEnd(12);
            NCDuration.setLayoutParams(lp);
            View voiceSendView = holder.getView(R.id.nc_voice_send);
            if (voiceSendView != null) {
                voiceSendView.setScaleX(voiceIconScaleX);
            }
            if (uiMessage.isPlaying()) {
                holder.setImageDrawable(R.id.nc_voice_send, animationDrawable);
                if (animationDrawable != null) {
                    animationDrawable.start();
                }
            } else {
                holder.setImageResource(
                        R.id.nc_voice_send,
                        ChatUIThemeManager.getAttrResId(
                                holder.getContext(),
                                R.attr.nc_conversation_msg_cell_send_voice_3_img));
            }
            holder.setVisible(R.id.nc_voice_unread, false);
            holder.setVisible(R.id.nc_voice_download_error, false);
            holder.setVisible(R.id.nc_download_progress, false);
        } else {
            AnimationDrawable animationDrawable =
                    (AnimationDrawable)
                            holder.getContext()
                                    .getResources()
                                    .getDrawable(
                                            ChatUIThemeManager.getAttrResId(
                                                    holder.getContext(),
                                                    R.attr.nc_icon_voice_receive_animator));
            holder.setVisible(R.id.nc_voice, true);
            holder.setVisible(R.id.nc_voice_send, false);
            NCDuration.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) NCDuration.getLayoutParams();
            lp.setMarginStart(12);
            NCDuration.setLayoutParams(lp);
            View voiceView = holder.getView(R.id.nc_voice);
            if (voiceView != null) {
                voiceView.setScaleX(voiceIconScaleX);
            }
            if (uiMessage.isPlaying()) {
                holder.setImageDrawable(R.id.nc_voice, animationDrawable);
                if (animationDrawable != null) {
                    animationDrawable.start();
                }
            } else {
                holder.setImageResource(
                        R.id.nc_voice,
                        ChatUIThemeManager.getAttrResId(
                                holder.getContext(),
                                R.attr.nc_conversation_msg_cell_receive_voice_3_img));
            }

            if (message.getLocalPath() != null) {
                holder.setVisible(R.id.nc_voice_download_error, false);
                holder.setVisible(R.id.nc_download_progress, false);
                holder.setVisible(
                        R.id.nc_voice_unread,
                        !uiMessage.getMessage().getReceivedStatusInfo().isListened());
            } else {
                if (uiMessage.getState() == State.ERROR) {
                    holder.setVisible(R.id.nc_voice_unread, false);
                    holder.setVisible(R.id.nc_voice_download_error, true);
                    holder.setVisible(R.id.nc_download_progress, false);
                } else if (uiMessage.getState() == State.PROGRESS) {
                    holder.setVisible(R.id.nc_voice_unread, false);
                    holder.setVisible(R.id.nc_voice_download_error, false);
                    holder.setVisible(R.id.nc_download_progress, true);
                } else {
                    boolean listened = uiMessage.getMessage().getReceivedStatusInfo().isListened();
                    boolean isConvertText =
                            message.getSttInfo() != null
                                    && message.getSttInfo().getStatus()
                                            == SpeechToTextStatus.SUCCESS;
                    holder.setVisible(R.id.nc_voice_unread, !listened && !isConvertText);
                    holder.setVisible(R.id.nc_voice_download_error, false);
                    holder.setVisible(R.id.nc_download_progress, false);
                }
            }
        }

        setupSpeechToTextUI(holder, message, uiMessage, listener);
    }

    @Override
    protected boolean onItemClick(
            ViewHolder holder,
            HDVoiceMessage message,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        if (listener != null) {
            listener.onViewClick(MessageClickType.AUDIO_CLICK, uiMessage);
            return true;
        }
        return false;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof HDVoiceMessage && !messageContent.isDestruct();
    }

    @Override
    public Spannable getSummarySpannable(Context context, HDVoiceMessage hdVoiceMessage) {
        return new SpannableString(
                context.getString(R.string.nc_conversation_summary_content_voice));
    }

    /** Sets up the speech-to-text UI based on sttInfo status and visibility. */
    private void setupSpeechToTextUI(
            ViewHolder holder,
            HDVoiceMessage message,
            UiMessage uiMessage,
            IViewProviderListener<UiMessage> listener) {
        if (!AppSettingsHandler.getInstance().getAppSettings().isSpeechToTextEnabled()) {
            return;
        }

        SpeechToTextInfo sttInfo = message.getSttInfo();
        SpeechToTextViews views = getSpeechToTextViews(holder);
        if (views == null) {
            RLog.e(TAG, "getSpeechToTextViews failed," + uiMessage.getMessageType());
            return;
        }

        if (sttInfo == null) {
            hideSpeechToTextViews(views);
            return;
        }

        SentStatus sentStatus = uiMessage.getMessage().getSentStatus();
        if (sentStatus == SentStatus.SENDING
                || sentStatus == SentStatus.FAILED
                || sentStatus == SentStatus.CANCELED) {
            hideSpeechToTextViews(views);
            return;
        }

        views.loadingDots.setVisibility(View.GONE);
        setViewAlignment(views, uiMessage);

        views.sttContainer.setOnClickListener(v -> {});
        views.sttContainer.setOnLongClickListener(v -> false);

        if (Objects.equals(
                uiMessage.getBusinessState(), SpeechToTextHandler.SPEECH_TO_TEXT_HIDDEN_STATE)) {
            hideSpeechToTextViews(views);
            return;
        }

        if (Objects.equals(
                uiMessage.getBusinessState(), SpeechToTextHandler.SPEECH_TO_TEXT_LOADING_STATE)) {
            showConvertingState(views);
            return;
        }

        if (!sttInfo.isVisible()) {
            hideSpeechToTextViews(views);
            return;
        }

        String transcribedText = sttInfo.getText();

        switch (sttInfo.getStatus()) {
            case CONVERTING:
                showConvertingState(views);
                break;
            case SUCCESS:
                showSuccessState(views, transcribedText, uiMessage, listener);
                break;
            case FAILED:
                showFailedState(views);
                break;
            default:
                hideSpeechToTextViews(views);
                break;
        }
    }

    /** Hides the speech-to-text views. */
    private void hideSpeechToTextViews(SpeechToTextViews views) {
        if (views.sttContainer != null) {
            views.sttContainer.setVisibility(View.GONE);
            views.sttContainer.setClipBounds(null);
        }
        if (views.loadingDots != null) {
            views.loadingDots.setVisibility(View.GONE);
        }
        if (views.sttText != null) {
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    views.sttText, 0, 0, 0, 0);
        }
    }

    /** Sets the view alignment. */
    private void setViewAlignment(SpeechToTextViews views, UiMessage uiMessage) {
        boolean isSender = uiMessage.getMessage().getDirection() == MessageDirection.SEND;
        int gravity = isSender ? Gravity.END : Gravity.START;

        if (views.sttContainer != null) {
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) views.sttContainer.getLayoutParams();
            if (params != null) {
                params.gravity = gravity;
                views.sttContainer.setLayoutParams(params);
            }
        }

        if (views.NCLayout != null) {
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) views.NCLayout.getLayoutParams();
            if (params != null) {
                params.gravity = gravity;
                views.NCLayout.setLayoutParams(params);
            }
        }
    }

    /** Shows the converting-in-progress state. */
    private void showConvertingState(SpeechToTextViews views) {
        views.sttContainer.setVisibility(View.VISIBLE);
        views.sttContainer.setClipBounds(null);

        if (views.sttText != null) {
            views.sttText.setVisibility(View.GONE);
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    views.sttText, 0, 0, 0, 0);
        }

        if (views.loadingDots != null) {
            views.loadingDots.setVisibility(View.VISIBLE);
            views.loadingDots.startAnimation();
        }
    }

    /** Shows the conversion-failed state. */
    private void showFailedState(SpeechToTextViews views) {
        views.sttContainer.setVisibility(View.VISIBLE);
        views.sttContainer.setClipBounds(null);

        if (views.sttText != null) {
            views.sttText.setVisibility(View.VISIBLE);
            views.sttText.setText(
                    views.sttText.getContext().getString(R.string.nc_speech_to_text_failed));
            views.sttText.setTextColor(
                    ChatUIThemeManager.getColorFromAttrId(
                            views.sttText.getContext(), R.attr.nc_text_secondary_color));

            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    views.sttText,
                    ChatUIThemeManager.getAttrResId(
                            views.sttText.getContext(),
                            R.attr.nc_conversation_list_cell_msg_fail_msg),
                    0,
                    0,
                    0);
            views.sttText.setCompoundDrawablePadding(
                    (int)
                            (4
                                    * views.sttText
                                            .getContext()
                                            .getResources()
                                            .getDisplayMetrics()
                                            .density));
        }
    }

    /** Shows the conversion-success state. */
    private void showSuccessState(
            SpeechToTextViews views,
            String transcribedText,
            UiMessage uiMessage,
            IViewProviderListener<UiMessage> listener) {
        if (android.text.TextUtils.isEmpty(transcribedText)) {
            transcribedText = " ";
        }

        if (views.sttText != null) {
            views.sttText.setTextColor(
                    ChatUIThemeManager.getColorFromAttrId(
                            views.sttText.getContext(), R.attr.nc_text_primary_color));
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    views.sttText, 0, 0, 0, 0);

            String messageUid = uiMessage.getMessage().getMessageId();
            boolean isLeftToRight = uiMessage.getMessage().getDirection() != MessageDirection.SEND;

            TextAnimationHelper.startWithUidCheck(
                    views.sttContainer, views.sttText, transcribedText, messageUid, isLeftToRight);
        }

        String finalTranscribedText = transcribedText;
        views.sttContainer.setOnLongClickListener(
                v -> {
                    showSpeechToTextDialog(
                            v.getContext(),
                            finalTranscribedText,
                            listener,
                            uiMessage,
                            views.sttContainer);
                    return true;
                });
    }

    /** Shows the speech-to-text action popup. */
    private void showSpeechToTextDialog(
            Context context,
            String text,
            IViewProviderListener<UiMessage> listener,
            UiMessage uiMessage,
            View anchorView) {
        showV2SpeechToTextDialog(context, text, listener, uiMessage, anchorView);
    }

    /** Shows the V2 speech-to-text action popup (using MessageLongClickPopup style). */
    private void showV2SpeechToTextDialog(
            Context context,
            String text,
            IViewProviderListener<UiMessage> listener,
            UiMessage uiMessage,
            View anchorView) {
        java.util.ArrayList<MessageLongClickPopup.OptionItem> items = new java.util.ArrayList<>();
        items.add(
                new MessageLongClickPopup.OptionItem(
                        context.getString(R.string.nc_copy),
                        R.attr.nc_conversation_menu_item_copy_img));
        items.add(
                new MessageLongClickPopup.OptionItem(
                        context.getString(R.string.nc_cancel_speech_to_text),
                        R.attr.nc_conversation_menu_item_translation_img));

        MessageLongClickPopup popup = MessageLongClickPopup.newInstance(context, items);
        popup.setAnchorView(anchorView)
                .setOptionsPopupDialogListener(
                        index -> {
                            if (index == 0) {
                                copyTextToClipboard(context, text);
                            } else if (index == 1) {
                                if (listener != null) {
                                    listener.onViewClick(
                                            MessageClickType.SPEECH_TO_TEXT, uiMessage);
                                }
                            }
                        });

        popup.show();
    }

    private void copyTextToClipboard(Context context, String text) {
        try {
            ClipboardManager clipboard =
                    (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(null, text);
            clipboard.setPrimaryClip(clip);
        } catch (Exception e) {
            RLog.e(TAG, "copyTextToClipboard stt text", e);
        }
    }

    /** Encapsulation of speech-to-text UI components. */
    private static class SpeechToTextViews {
        final TextView sttText;
        final LinearLayout sttContainer;
        final LoadingDotsView loadingDots;
        final LinearLayout NCLayout;

        SpeechToTextViews(
                TextView sttText,
                LinearLayout sttContainer,
                LoadingDotsView loadingDots,
                LinearLayout NCLayout) {
            this.sttText = sttText;
            this.sttContainer = sttContainer;
            this.loadingDots = loadingDots;
            this.NCLayout = NCLayout;
        }
    }

    /** Gets the speech-to-text related UI components. */
    private SpeechToTextViews getSpeechToTextViews(ViewHolder holder) {
        TextView sttText = holder.getView(R.id.nc_stt_text);
        LinearLayout sttContainer = holder.getView(R.id.nc_stt_container);
        LoadingDotsView loadingDots = holder.getView(R.id.nc_loading_dots);
        LinearLayout NCLayout = holder.getView(R.id.nc_layout);

        if (!checkViewsValid(sttText, sttContainer, loadingDots, NCLayout)) {
            return null;
        }
        return new SpeechToTextViews(sttText, sttContainer, loadingDots, NCLayout);
    }
}
