package ai.nexconn.chatui.channel.extension.component.inputpanel;

import static ai.nexconn.chatui.utils.permission.PermissionCheckUtil.REQUEST_CODE_ASK_PERMISSIONS;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.DirectChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.message.model.MessageType;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.channel.extension.InputMode;
import ai.nexconn.chatui.channel.extension.NCExtensionCacheHelper;
import ai.nexconn.chatui.channel.extension.NCExtensionViewModel;
import ai.nexconn.chatui.channel.feature.editmessage.EditMessageConfig;
import ai.nexconn.chatui.channel.feature.editmessage.EditMessageManager;
import ai.nexconn.chatui.channel.feature.mention.DraftHelper;
import ai.nexconn.chatui.channel.feature.reference.ReferenceManager;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.handler.EditMessageHandler;
import ai.nexconn.chatui.manager.AudioPlayManager;
import ai.nexconn.chatui.manager.AudioRecordManager;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.permission.PermissionCheckUtil;
import ai.nexconn.chatui.widget.ChatUIEditText;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import java.lang.ref.WeakReference;
import java.util.Collections;
import org.json.JSONException;
import org.json.JSONObject;

public class InputPanel {
    private final String TAG = this.getClass().getSimpleName();
    private Context mContext;
    private ChannelIdentifier mConversationIdentifier;
    private InputStyle mInputStyle;
    private Fragment mFragment;
    private View mInputPanel;
    private boolean
            mIsVoiceInputMode; // Whether the voice/keyboard toggle button currently shows keyboard
    // mode
    private ImageView mVoiceToggleBtn;
    private EditText mEditText;
    private TextView mVoiceInputBtn;
    private ImageView mEmojiToggleBtn;
    private Button mSendBtn;
    private ImageView mAddBtn;
    private ViewGroup mAddOrSendBtn;
    private NCExtensionViewModel mExtensionViewModel;
    private String mInitialDraft = "";
    private final DraftHelper draftHelper;
    private EditMessageHandler editMessageHandler;

    public InputPanel(
            Fragment fragment,
            ViewGroup parent,
            InputStyle inputStyle,
            ChannelIdentifier conversationIdentifier) {
        mFragment = fragment;
        mInputStyle = inputStyle;
        mConversationIdentifier = conversationIdentifier;
        editMessageHandler = new EditMessageHandler();
        editMessageHandler.addDataChangeListener(
                EditMessageHandler.KEY_INPUT_PANEL_GET_DRAFT,
                (OnDataChangeEnhancedListener<EditMessageConfig>) this::getDraftReally);
        initView(fragment.getContext(), parent);
        draftHelper = new DraftHelper(mEditText);

        mExtensionViewModel = new ViewModelProvider(fragment).get(NCExtensionViewModel.class);
        mExtensionViewModel
                .getInputModeLiveData()
                .observe(
                        fragment.getViewLifecycleOwner(),
                        new Observer<InputMode>() {
                            @Override
                            public void onChanged(InputMode inputMode) {
                                updateViewByInputMode(inputMode);
                            }
                        });
        if (fragment.getContext() != null) {
            mIsVoiceInputMode =
                    NCExtensionCacheHelper.isVoiceInputMode(
                            fragment.getContext(),
                            conversationIdentifier.getChannelType(),
                            conversationIdentifier.getChannelId());
        }
        if (mIsVoiceInputMode) {
            mExtensionViewModel.getInputModeLiveData().setValue(InputMode.VoiceInput);
        } else {
            mExtensionViewModel.getInputModeLiveData().setValue(InputMode.TextInput);
        }
        ReferenceManager.getInstance().setReferenceStatusListener(ReferenceStatusListener);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView(final Context context, ViewGroup parent) {
        mContext = context;
        mInputPanel =
                LayoutInflater.from(context)
                        .inflate(R.layout.nc_extension_input_panel, parent, false);
        mVoiceToggleBtn = mInputPanel.findViewById(R.id.input_panel_voice_toggle);
        mEditText = mInputPanel.findViewById(R.id.edit_btn);
        mVoiceInputBtn = mInputPanel.findViewById(R.id.press_to_speech_btn);
        mEmojiToggleBtn = mInputPanel.findViewById(R.id.input_panel_emoji_btn);
        mAddOrSendBtn = mInputPanel.findViewById(R.id.input_panel_add_or_send);
        mSendBtn = mInputPanel.findViewById(R.id.input_panel_send_btn);
        mAddBtn = mInputPanel.findViewById(R.id.input_panel_add_btn);
        mInputPanel.setBackgroundColor(
                ChatUIThemeManager.getColorFromAttrId(context, R.attr.nc_common_background_color));
        mEditText.setBackgroundResource(R.drawable.nc_lively_panel_input_background);
        mVoiceInputBtn.setBackgroundResource(R.drawable.nc_lively_auxiliary_background_1_radius_8);
        mSendBtn.setOnClickListener(mOnSendBtnClick);
        mEditText.setOnFocusChangeListener(mOnEditTextFocusChangeListener);
        mEditText.addTextChangedListener(mEditTextWatcher);
        mVoiceToggleBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mExtensionViewModel == null) {
                            return;
                        }
                        if (mIsVoiceInputMode) {
                            mIsVoiceInputMode = false;
                            mExtensionViewModel
                                    .getInputModeLiveData()
                                    .setValue(InputMode.TextInput);
                            // Show the soft keyboard after switching to text input mode
                            mEditText.requestFocus();
                        } else {
                            mExtensionViewModel
                                    .getInputModeLiveData()
                                    .postValue(InputMode.VoiceInput);
                            mIsVoiceInputMode = true;
                        }
                        NCExtensionCacheHelper.saveVoiceInputMode(
                                context,
                                mConversationIdentifier.getChannelType(),
                                mConversationIdentifier.getChannelId(),
                                mIsVoiceInputMode);
                    }
                });
        mEmojiToggleBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mExtensionViewModel == null) {
                            return;
                        }
                        if (mExtensionViewModel.getInputModeLiveData().getValue() != null
                                && mExtensionViewModel
                                        .getInputModeLiveData()
                                        .getValue()
                                        .equals(InputMode.EmoticonMode)) {
                            mEditText.requestFocus();
                            mExtensionViewModel
                                    .getInputModeLiveData()
                                    .postValue(InputMode.TextInput);
                        } else {
                            mExtensionViewModel
                                    .getInputModeLiveData()
                                    .postValue(InputMode.EmoticonMode);
                        }
                    }
                });
        mAddBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mExtensionViewModel == null) {
                            return;
                        }
                        if (mExtensionViewModel.getInputModeLiveData().getValue() != null
                                && mExtensionViewModel
                                        .getInputModeLiveData()
                                        .getValue()
                                        .equals(InputMode.PluginMode)) {
                            mEditText.requestFocus();
                            mExtensionViewModel
                                    .getInputModeLiveData()
                                    .setValue(InputMode.TextInput);
                        } else {
                            mExtensionViewModel
                                    .getInputModeLiveData()
                                    .setValue(InputMode.PluginMode);
                            ReferenceManager.getInstance().hideReferenceView();
                        }
                    }
                });

        if (TextUtils.isEmpty(mInitialDraft)) {
            getDraft();
        }
        mVoiceInputBtn.setOnTouchListener(mOnVoiceBtnTouchListener);
        setInputPanelStyle(mInputStyle);
    }

    private void updateViewByInputMode(InputMode inputMode) {
        if (inputMode.equals(InputMode.TextInput) || inputMode.equals(InputMode.PluginMode)) {
            if (inputMode.equals(InputMode.TextInput)) {
                mIsVoiceInputMode = false;
            }
            mVoiceToggleBtn.setImageDrawable(
                    mContext.getResources()
                            .getDrawable(
                                    ChatUIThemeManager.getAttrResId(
                                            mContext, R.attr.nc_conversation_input_bar_voice_img)));
            mEmojiToggleBtn.setImageDrawable(
                    mContext.getResources()
                            .getDrawable(
                                    ChatUIThemeManager.getAttrResId(
                                            mContext, R.attr.nc_conversation_input_bar_emoji_img)));
            mEditText.setVisibility(VISIBLE);
            mVoiceInputBtn.setVisibility(GONE);
            resetInputView();
        } else if (inputMode.equals(InputMode.VoiceInput)) {
            mVoiceToggleBtn.setImageDrawable(
                    mContext.getResources()
                            .getDrawable(
                                    ChatUIThemeManager.getAttrResId(
                                            mContext,
                                            R.attr.nc_conversation_input_bar_keyboard_img)));
            mVoiceInputBtn.setVisibility(VISIBLE);
            mEditText.setVisibility(GONE);
            mEmojiToggleBtn.setImageDrawable(
                    mContext.getResources()
                            .getDrawable(
                                    ChatUIThemeManager.getAttrResId(
                                            mContext, R.attr.nc_conversation_input_bar_emoji_img)));
            if (mInputStyle.equals(InputStyle.STYLE_CONTAINER_EXTENSION)
                    || mInputStyle.equals(InputStyle.STYLE_SWITCH_CONTAINER_EXTENSION)) {
                mAddOrSendBtn.setVisibility(VISIBLE);
                mAddBtn.setVisibility(VISIBLE);
                mSendBtn.setVisibility(GONE);
            } else {
                mAddOrSendBtn.setVisibility(GONE);
            }
        } else if (inputMode.equals(InputMode.EmoticonMode)) {
            mVoiceToggleBtn.setImageDrawable(
                    mContext.getResources()
                            .getDrawable(
                                    ChatUIThemeManager.getAttrResId(
                                            mContext, R.attr.nc_conversation_input_bar_voice_img)));
            mEmojiToggleBtn.setImageDrawable(
                    mContext.getResources()
                            .getDrawable(
                                    ChatUIThemeManager.getAttrResId(
                                            mContext,
                                            R.attr.nc_conversation_input_bar_keyboard_img)));
            mEditText.setVisibility(VISIBLE);
            mVoiceInputBtn.setVisibility(GONE);
            resetInputView();
        } else if (inputMode.equals(InputMode.QuickReplyMode)) {
            mIsVoiceInputMode = false;
            mVoiceToggleBtn.setImageDrawable(
                    mContext.getResources()
                            .getDrawable(
                                    ChatUIThemeManager.getAttrResId(
                                            mContext, R.attr.nc_conversation_input_bar_voice_img)));
            mEmojiToggleBtn.setImageDrawable(
                    mContext.getResources()
                            .getDrawable(
                                    ChatUIThemeManager.getAttrResId(
                                            mContext, R.attr.nc_conversation_input_bar_emoji_img)));
            mEditText.setVisibility(VISIBLE);
            mVoiceInputBtn.setVisibility(GONE);
            mEmojiToggleBtn.setImageDrawable(
                    mContext.getResources()
                            .getDrawable(
                                    ChatUIThemeManager.getAttrResId(
                                            mContext, R.attr.nc_conversation_input_bar_emoji_img)));
        } else if (inputMode.equals(InputMode.NormalMode)) {
            mIsVoiceInputMode = false;
            mVoiceToggleBtn.setImageDrawable(
                    mContext.getResources()
                            .getDrawable(
                                    ChatUIThemeManager.getAttrResId(
                                            mContext, R.attr.nc_conversation_input_bar_voice_img)));
            mEmojiToggleBtn.setImageDrawable(
                    mContext.getResources()
                            .getDrawable(
                                    ChatUIThemeManager.getAttrResId(
                                            mContext, R.attr.nc_conversation_input_bar_emoji_img)));
            mEditText.setVisibility(VISIBLE);
            mVoiceInputBtn.setVisibility(GONE);
            resetInputView();
        }
    }

    private void resetInputView() {
        Editable text = mEditText.getText();
        if (text == null || text.length() == 0) {
            if (mInputStyle.equals(InputStyle.STYLE_CONTAINER_EXTENSION)
                    || mInputStyle.equals(InputStyle.STYLE_SWITCH_CONTAINER_EXTENSION)) {
                mAddOrSendBtn.setVisibility(VISIBLE);
                mAddBtn.setVisibility(VISIBLE);
                mSendBtn.setVisibility(GONE);
            } else {
                mAddOrSendBtn.setVisibility(GONE);
            }
        } else {
            mAddOrSendBtn.setVisibility(VISIBLE);
            mSendBtn.setVisibility(VISIBLE);
            mAddBtn.setVisibility(GONE);
        }
    }

    public EditText getEditText() {
        return mEditText;
    }

    public View getRootView() {
        return mInputPanel;
    }

    public void setVisible(int viewId, boolean visible) {
        mInputPanel.findViewById(viewId).setVisibility(visible ? VISIBLE : GONE);
    }

    /**
     * Sets the InputPanel style.
     *
     * @param style Currently supports 5 styles, see: {@link InputStyle}.
     */
    public void setInputPanelStyle(InputStyle style) {
        switch (style) {
            case STYLE_SWITCH_CONTAINER_EXTENSION:
                setSCE();
                break;
            case STYLE_CONTAINER:
                setC();
                break;
            case STYLE_CONTAINER_EXTENSION:
                setCE();
                break;
            case STYLE_SWITCH_CONTAINER:
                setSC();
                break;
            default:
                setSCE();
                break;
        }
        mInputStyle = style;
    }

    private void setSCE() {
        if (mInputPanel != null) {
            mVoiceToggleBtn.setVisibility(VISIBLE);
            mEmojiToggleBtn.setVisibility(shouldShowEmojiButton() ? VISIBLE : GONE);
            mAddBtn.setVisibility(VISIBLE);
        }
    }

    private void setC() {
        if (mInputPanel != null) {
            mVoiceToggleBtn.setVisibility(GONE);
            mAddOrSendBtn.setVisibility(GONE);
            mEmojiToggleBtn.setVisibility(GONE);
            mAddBtn.setVisibility(GONE);
            mSendBtn.setVisibility(GONE);
        }
    }

    private void setCE() {
        if (mInputPanel != null) {
            mVoiceToggleBtn.setVisibility(GONE);
            mAddOrSendBtn.setVisibility(VISIBLE);
            mEmojiToggleBtn.setVisibility(shouldShowEmojiButton() ? VISIBLE : GONE);
            mAddBtn.setVisibility(VISIBLE);
        }
    }

    private void setSC() {
        if (mInputPanel != null) {
            mVoiceToggleBtn.setVisibility(VISIBLE);
            mAddOrSendBtn.setVisibility(GONE);
            mAddBtn.setVisibility(GONE);
        }
    }

    private boolean shouldShowEmojiButton() {
        return !NCChatUIConfig.featureConfig().isHideEmojiButton();
    }

    /** Gets the draft. Logic: edit draft => normal draft. */
    public void getDraft() {
        editMessageHandler.checkEditedMessageDraftStatus(mConversationIdentifier);
    }

    /** Gets the normal draft. */
    public void getDraftReally(EditMessageConfig config) {
        if (config == null) {
            WeakReference<InputPanel> weakThis = new WeakReference<>(InputPanel.this);
            BaseChannel.getChannels(
                    Collections.singletonList(mConversationIdentifier),
                    new OperationHandler<java.util.List<BaseChannel>>() {
                        @Override
                        public void onResult(java.util.List<BaseChannel> channels, NCError error) {
                            InputPanel inputPanel = weakThis.get();
                            if (inputPanel == null) {
                                return;
                            }
                            String content = null;
                            if (channels != null && !channels.isEmpty()) {
                                content = channels.get(0).getDraft();
                            }
                            if (content == null || content.isEmpty()) {
                                return;
                            }
                            inputPanel.mInitialDraft = content;
                            inputPanel.processDraftContent(content);
                        }
                    });
        }
    }

    private void processDraftContent(String content) {
        String draftContent = "";
        String referencedMessageUId = null;
        String mentionedRangeInfoList = null;

        try {
            JSONObject draftJson = new JSONObject(content);
            draftContent = draftJson.optString("draftContent", "");
            referencedMessageUId = draftJson.optString("referencedMessageUId", null);
            mentionedRangeInfoList = draftJson.optString("mentionedRangeInfoList", null);
        } catch (Exception e) {
            draftContent = content;
        }

        String finalDraftContent = draftContent;
        draftHelper.addMentionBlocks(mentionedRangeInfoList);

        if (referencedMessageUId != null && !referencedMessageUId.isEmpty()) {
            WeakReference<InputPanel> weakThis = new WeakReference<>(this);
            ai.nexconn.chat.channel.BaseChannel.getMessageById(
                    new ai.nexconn.chat.params.GetMessageByIdParams(referencedMessageUId, null),
                    (message, error) -> {
                        InputPanel panel = weakThis.get();
                        if (panel != null) {
                            panel.updateMessageDraft(finalDraftContent, message);
                        }
                    });
        } else {
            updateMessageDraft(finalDraftContent, null);
        }
    }

    private float mLastTouchY;
    private boolean mUpDirection;
    private View.OnTouchListener mOnVoiceBtnTouchListener =
            new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    float mOffsetLimit =
                            70 * v.getContext().getResources().getDisplayMetrics().density;
                    String[] permissions = {Manifest.permission.RECORD_AUDIO};

                    if (!PermissionCheckUtil.checkPermissions(v.getContext(), permissions)
                            && event.getAction() == MotionEvent.ACTION_DOWN) {
                        PermissionCheckUtil.requestPermissions(
                                mFragment, permissions, REQUEST_CODE_ASK_PERMISSIONS);
                        return true;
                    }

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (AudioPlayManager.getInstance().isPlaying()) {
                            AudioPlayManager.getInstance().stopPlay();
                        }
                        AudioRecordManager.getInstance()
                                .startRecord(v.getRootView(), mConversationIdentifier);
                        mLastTouchY = event.getY();
                        mUpDirection = false;
                        ((TextView) v).setText(R.string.nc_voice_release_to_send);
                        v.setBackgroundResource(
                                R.drawable.nc_lively_auxiliary_background_2_radius_8);
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        if (mLastTouchY - event.getY() > mOffsetLimit && !mUpDirection) {
                            AudioRecordManager.getInstance().willCancelRecord();
                            mUpDirection = true;
                            ((TextView) v).setText(R.string.nc_voice_press_to_input);
                            v.setBackgroundResource(
                                    R.drawable.nc_lively_auxiliary_background_1_radius_8);
                        } else if (event.getY() - mLastTouchY > -mOffsetLimit && mUpDirection) {
                            AudioRecordManager.getInstance().continueRecord();
                            mUpDirection = false;
                            ((TextView) v).setText(R.string.nc_voice_release_to_send);
                            v.setBackgroundResource(
                                    R.drawable.nc_lively_auxiliary_background_2_radius_8);
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_UP
                            || event.getAction() == MotionEvent.ACTION_CANCEL) {
                        AudioRecordManager.getInstance().stopRecord();
                        ((TextView) v).setText(R.string.nc_voice_press_to_input);
                        v.setBackgroundResource(
                                R.drawable.nc_lively_auxiliary_background_1_radius_8);
                    }
                    if (mConversationIdentifier.getChannelType() == ChannelType.DIRECT) {
                        new DirectChannel(mConversationIdentifier.getChannelId())
                                .sendTypingStatus(MessageType.HD_VOICE);
                    }
                    return true;
                }
            };

    private View.OnClickListener mOnSendBtnClick =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mExtensionViewModel == null) {
                        return;
                    }
                    mExtensionViewModel.onSendClick();
                }
            };

    private View.OnFocusChangeListener mOnEditTextFocusChangeListener =
            new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        if (mExtensionViewModel != null
                                && mExtensionViewModel.getInputModeLiveData() != null) {
                            mExtensionViewModel
                                    .getInputModeLiveData()
                                    .postValue(InputMode.TextInput);
                        }
                        if (!TextUtils.isEmpty(mEditText.getText())) {
                            mSendBtn.setVisibility(VISIBLE);
                            mAddBtn.setVisibility(GONE);
                        }
                    } else {
                        if (mExtensionViewModel != null) {
                            EditText editText = mExtensionViewModel.getEditTextWidget();
                            if (editText.getText() != null && editText.getText().length() == 0) {
                                mSendBtn.setVisibility(GONE);
                                mAddBtn.setVisibility(VISIBLE);
                            }
                        }
                    }
                }
            };

    private TextWatcher mEditTextWatcher =
            new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // do nothing
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s == null || s.length() == 0) {
                        saveTextMessageDraft();
                        if (mInputStyle.equals(InputStyle.STYLE_CONTAINER_EXTENSION)
                                || mInputStyle.equals(
                                        InputStyle.STYLE_SWITCH_CONTAINER_EXTENSION)) {
                            mAddOrSendBtn.setVisibility(VISIBLE);
                            mAddBtn.setVisibility(VISIBLE);
                            mSendBtn.setVisibility(GONE);
                        } else {
                            mAddOrSendBtn.setVisibility(GONE);
                        }
                    } else {
                        mAddOrSendBtn.setVisibility(VISIBLE);
                        mSendBtn.setVisibility(VISIBLE);
                        mAddBtn.setVisibility(GONE);
                    }

                    int cursor, offset;
                    if (count == 0) {
                        cursor = start + before;
                        offset = -before;
                    } else {
                        cursor = start;
                        offset = count;
                    }
                    if (mConversationIdentifier.getChannelType() == ChannelType.DIRECT
                            && offset != 0) {
                        new DirectChannel(mConversationIdentifier.getChannelId())
                                .sendTypingStatus(MessageType.TEXT);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // do nothing
                }
            };

    public void onPause() {
        // Save draft in onPause to handle the case where the process is killed after pressing Home
        saveTextMessageDraft();
    }

    public void onDestroy() {
        ReferenceManager.getInstance().removeReferenceStatusListener(ReferenceStatusListener);
        // Save the current input mode when leaving the page
        NCExtensionCacheHelper.saveVoiceInputMode(
                mContext,
                mConversationIdentifier.getChannelType(),
                mConversationIdentifier.getChannelId(),
                mIsVoiceInputMode);
        mFragment = null;
        mContext = null;
        mExtensionViewModel = null;
        saveTextMessageDraft();
        editMessageHandler.stop();
    }

    private static class SaveDraftCallback
            implements ai.nexconn.chat.handler.OperationHandler<Boolean> {
        private final WeakReference<InputPanel> panelRef;
        private final String draft;

        public SaveDraftCallback(InputPanel panel, final String draft) {
            this.panelRef = new WeakReference<>(panel);
            this.draft = draft;
        }

        @Override
        public void onResult(Boolean result, ai.nexconn.chat.error.NCError error) {
            InputPanel panel = panelRef.get();
            if (panel != null && Boolean.TRUE.equals(result)) {
                panel.mInitialDraft = draft;
            }
        }
    }

    private final ReferenceManager.ReferenceStatusListener ReferenceStatusListener =
            new ReferenceManager.ReferenceStatusListener() {
                @Override
                public void onHide() {
                    saveTextMessageDraft();
                }
            };

    private void saveTextMessageDraft() {
        // No need to save draft if currently in edit mode
        if (EditMessageManager.getInstance().isEditMessageState()) {
            return;
        }
        if (mEditText != null && mEditText.getText() != null) {
            String draftText = mEditText.getText().toString();
            String draft = getDraft(draftText);
            // Skip saving if the draft content has not changed
            if ((TextUtils.isEmpty(mInitialDraft) && TextUtils.isEmpty(draftText))
                    || (mInitialDraft != null && mInitialDraft.equals(draft))) {
                return;
            }
            ai.nexconn.chatui.NCChatUI.saveDraft(
                    mConversationIdentifier, draft, new SaveDraftCallback(this, draft));
        }
    }

    @NonNull
    private String getDraft(String draftContent) {
        UiMessage uiMessage = ReferenceManager.getInstance().getUiMessage();
        String referencedMessageUId = null;
        if (uiMessage != null && uiMessage.getMessage() != null) {
            referencedMessageUId = uiMessage.getMessage().getMessageId();
        }

        // Build draft data in JSON format
        JSONObject draftJson = new JSONObject();
        try {
            draftJson.put("draftContent", draftContent);
            if (referencedMessageUId != null && !referencedMessageUId.isEmpty()) {
                draftJson.put("referencedMessageUId", referencedMessageUId);
            }
            String mentionBlocks = draftHelper.getMentionBlocks();
            if (mentionBlocks != null && !mentionBlocks.isEmpty()) {
                draftJson.put("mentionedRangeInfoList", mentionBlocks);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return draftJson.toString();
    }

    private void updateMessageDraft(final String draft, ai.nexconn.chat.message.Message message) {
        if (TextUtils.isEmpty(draft)) {
            return;
        }
        if (message != null) {
            ReferenceManager.getInstance().showReferenceView(mContext, new UiMessage(message));
        }
        mEditText.postDelayed(
                () -> {
                    if (mEditText instanceof ChatUIEditText) {
                        ((ChatUIEditText) mEditText).setText(draft, false);
                    } else {
                        mEditText.setText(draft);
                    }
                    // On some low Android versions / devices, the text after EditText#setText may
                    // be shorter than expected,
                    // so set cursor to the end using EditText#length()
                    mEditText.setSelection(mEditText.length());
                    mEditText.requestFocus();
                    resetInputView();
                },
                50);
    }

    public enum InputStyle {
        /** Voice toggle - Input box - Extension */
        STYLE_SWITCH_CONTAINER_EXTENSION(0x123),
        /** Voice toggle - Input box */
        STYLE_SWITCH_CONTAINER(0x120),
        /** Input box - Extension */
        STYLE_CONTAINER_EXTENSION(0x023),
        /** Input box only */
        STYLE_CONTAINER(0x020);

        int v;

        InputStyle(int v) {
            this.v = v;
        }

        public static InputStyle getStyle(int v) {
            InputStyle result = null;
            for (InputStyle style : values()) {
                if (style.v == v) {
                    result = style;
                    break;
                }
            }
            return result;
        }
    }
}
