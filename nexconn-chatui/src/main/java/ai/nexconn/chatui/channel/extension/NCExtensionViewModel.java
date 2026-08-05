package ai.nexconn.chatui.channel.extension;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.feature.editmessage.EditMessageManager;
import ai.nexconn.chatui.channel.feature.mention.IExtensionEventWatcher;
import ai.nexconn.chatui.channel.feature.mention.NCMentionManager;
import ai.nexconn.chatui.picture.tools.ToastUtils;
import ai.nexconn.chatui.utils.log.RLog;
import android.app.Application;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import java.util.Objects;

public class NCExtensionViewModel extends AndroidViewModel {
    private final String TAG = this.getClass().getSimpleName();
    private MutableLiveData<Boolean> mExtensionBoardState;
    private MutableLiveData<InputMode> mInputModeLiveData;
    private MutableLiveData<Boolean> mAttachedInfoState;
    private ChannelIdentifier mConversationIdentifier;
    private EditText mEditText;
    private boolean isSoftInputShow;
    private static final int MAX_MESSAGE_LENGTH_TO_SEND = 5000;
    private TextWatcher mTextWatcher =
            new TextWatcher() {
                private int start;
                private int count;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // do nothing
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    this.start = start;
                    this.count = count;

                    int cursor, offset;
                    if (count == 0) {
                        cursor = start + before;
                        offset = -before;
                    } else {
                        cursor = start;
                        offset = count;
                    }
                    ChannelType channelType = mConversationIdentifier.getChannelType();
                    NCMentionManager.getInstance()
                            .onTextChanged(
                                    getApplication().getApplicationContext(),
                                    channelType,
                                    mConversationIdentifier.getChannelId(),
                                    cursor,
                                    offset,
                                    s.toString(),
                                    mEditText);
                    for (IExtensionEventWatcher watcher :
                            NCExtensionManager.getInstance().getExtensionEventWatcher()) {
                        watcher.onTextChanged(
                                getApplication().getApplicationContext(),
                                channelType,
                                mConversationIdentifier.getChannelId(),
                                cursor,
                                offset,
                                s.toString());
                    }

                    if (!EditMessageManager.getInstance().isEmoticonMode()
                            && mInputModeLiveData.getValue() != InputMode.EmoticonMode
                            && mInputModeLiveData.getValue() != InputMode.RecognizeMode) {
                        mInputModeLiveData.postValue(InputMode.TextInput);
                        if (mEditText.getText() != null && mEditText.getText().length() > 0) {
                            mEditText.postDelayed(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            setSoftInputKeyBoard(true);
                                        }
                                    },
                                    100);
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            };

    public NCExtensionViewModel(@NonNull Application application) {
        super(application);
        mExtensionBoardState = new MutableLiveData<>();
        mInputModeLiveData = new MutableLiveData<>();
        mAttachedInfoState = new MutableLiveData<>();
    }

    void setAttachedConversation(ChannelIdentifier conversationIdentifier, EditText editText) {
        mConversationIdentifier = conversationIdentifier;
        mEditText = editText;
        mEditText.addTextChangedListener(mTextWatcher);
        if (mConversationIdentifier.getChannelType() == ChannelType.GROUP
                || mConversationIdentifier.getChannelType() == ChannelType.COMMUNITY) {
            NCMentionManager.getInstance()
                    .createInstance(
                            mConversationIdentifier.getChannelType(),
                            mConversationIdentifier.getChannelId(),
                            mEditText);
        }
    }

    public void onSendClick() {
        if (TextUtils.isEmpty(mEditText.getText())
                || TextUtils.isEmpty(mEditText.getText().toString().trim())) {
            RLog.d(TAG, "can't send empty content.");
            mEditText.setText("");
            return;
        }

        String text = mEditText.getText().toString();
        if (text.length() > MAX_MESSAGE_LENGTH_TO_SEND) {
            ToastUtils.s(
                    getApplication().getApplicationContext(),
                    getApplication().getString(R.string.nc_message_too_long));
            RLog.d(TAG, "The text you entered is too long to send.");
            return;
        }
        mEditText.setText("");

        // Build TextMessage, apply mention info, merge reference if present, then send.
        ai.nexconn.chat.message.TextMessage textMessage =
                new ai.nexconn.chat.message.TextMessage(text);
        NCMentionManager.getInstance().applyMentionInfo(textMessage, mEditText);
        // applyReference returns ReferenceMessage when a quote is active, else the TextMessage.
        ai.nexconn.chat.message.MessageContent contentToSend =
                ai.nexconn.chatui.channel.feature.reference.ReferenceManager.getInstance()
                        .applyReference(textMessage);
        NCChatUI.sendMessage(
                mConversationIdentifier,
                new ai.nexconn.chat.params.SendMessageParams(contentToSend),
                null);
    }

    public boolean isSoftInputShow() {
        return isSoftInputShow;
    }

    /**
     * Exits "more" mode.
     *
     * @param context Context.
     */
    public void exitMoreInputMode(Context context) {
        if (context == null) {
            return;
        }
        if (NCExtensionCacheHelper.isVoiceInputMode(
                context,
                mConversationIdentifier.getChannelType(),
                mConversationIdentifier.getChannelId())) {
            mInputModeLiveData.postValue(InputMode.VoiceInput);
        } else {
            collapseExtensionBoard();
        }
    }

    /** Collapses the panel; NCExtension shows only the InputPanel. */
    public void collapseExtensionBoard() {
        if (mExtensionBoardState.getValue() != null
                && mExtensionBoardState.getValue().equals(false)) {
            RLog.d(TAG, "already collapsed, return directly.");
            return;
        }
        RLog.d(TAG, "collapseExtensionBoard");
        setSoftInputKeyBoard(false);
        mExtensionBoardState.postValue(false);
        mInputModeLiveData.postValue(InputMode.NormalMode);
    }

    public void setSoftInputKeyBoard(boolean isShow) {
        forceSetSoftInputKeyBoard(isShow);
    }

    public void setSoftInputKeyBoard(boolean isShow, boolean clearFocus) {
        forceSetSoftInputKeyBoard(isShow, clearFocus);
    }

    public void forceSetSoftInputKeyBoard(boolean isShow) {
        forceSetSoftInputKeyBoard(isShow, true);
    }

    public void forceSetSoftInputKeyBoard(boolean isShow, boolean clearFocus) {
        if (mEditText == null) {
            return;
        }
        InputMethodManager imm =
                (InputMethodManager)
                        getApplication()
                                .getApplicationContext()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (isShow) {
                mEditText.requestFocus();
                imm.showSoftInput(mEditText, 0);
            } else {
                imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
                if (clearFocus) {
                    mEditText.clearFocus();
                }
            }
            isSoftInputShow = isShow;
        }
        if (isShow
                && mExtensionBoardState.getValue() != null
                && mExtensionBoardState.getValue().equals(false)) {
            mExtensionBoardState.setValue(true);
        }
    }

    /**
     * Gets the EditText widget.
     *
     * @return EditText widget.
     */
    public EditText getEditTextWidget() {
        return mEditText;
    }

    public void setEditTextWidget(EditText editText) {
        if (!Objects.equals(mEditText, editText)) {
            mEditText = editText;
            mEditText.addTextChangedListener(mTextWatcher);
            // Update the EditText in the mention manager
            NCMentionManager.getInstance().setInputEditText(mConversationIdentifier, editText);
        }
    }

    MutableLiveData<Boolean> getAttachedInfoState() {
        return mAttachedInfoState;
    }

    public ChannelIdentifier getConversationIdentifier() {
        return mConversationIdentifier;
    }

    /**
     * Gets the extension board open state. {@code value < 0} means collapsed; {@code value > 0}
     * means open, where value is the height of the board when open.
     *
     * @return Board state LiveData.
     */
    public MutableLiveData<Boolean> getExtensionBoardState() {
        return mExtensionBoardState;
    }

    /**
     * Gets the input mode LiveData.
     *
     * @return LiveData for the input mode.
     */
    public MutableLiveData<InputMode> getInputModeLiveData() {
        return mInputModeLiveData;
    }
}
