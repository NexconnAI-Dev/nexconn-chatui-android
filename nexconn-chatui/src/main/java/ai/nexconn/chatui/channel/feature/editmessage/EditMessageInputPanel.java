package ai.nexconn.chatui.channel.feature.editmessage;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.ErrorHandler;
import ai.nexconn.chat.handler.MessageHandler;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.ReferenceMessage;
import ai.nexconn.chat.message.model.MessageDeletedEvent;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.channel.event.action.DeleteEvent;
import ai.nexconn.chatui.channel.event.action.MessageEventListener;
import ai.nexconn.chatui.channel.extension.InputMode;
import ai.nexconn.chatui.channel.extension.NCExtensionViewModel;
import ai.nexconn.chatui.channel.extension.component.emoticon.EmoticonBoard;
import ai.nexconn.chatui.channel.feature.mention.NCMentionManager;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.handler.EditMessageHandler;
import ai.nexconn.chatui.utils.common.ChatUIUtils;
import ai.nexconn.chatui.utils.keyboard.KeyboardHeightObserver;
import ai.nexconn.chatui.utils.view.ChatUIViewUtils;
import ai.nexconn.chatui.widget.ChatUIEditText;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.util.List;

public class EditMessageInputPanel
        implements MessageEventListener, KeyboardHeightObserver, View.OnAttachStateChangeListener {

    /** Colon separator for joining username and content. */
    private static final String COLON_SEPARATOR = ":";

    private View mRootView;
    private NCExtensionViewModel mExtensionViewModel;
    private ChannelIdentifier mConversationIdentifier;
    private Fragment mFragment;
    // Edit message UID, passed in externally
    private String editMsgUid;
    // Referenced message UID of the editing message, passed in externally
    private String referUid;

    private final String mRecallHandlerId =
            "EditMessageInputPanel_recall_" + System.identityHashCode(this);
    private final MessageHandler mRecallHandler =
            new MessageHandler() {
                @Override
                public void onMessageDeleted(
                        @androidx.annotation.NonNull MessageDeletedEvent event) {
                    if (event.getMessages() == null) return;
                    for (Message msg : event.getMessages()) {
                        if (msg != null && TextUtils.equals(referUid, msg.getMessageId())) {
                            onMessageRecalled(msg);
                            break;
                        }
                    }
                }
            };
    // Referenced content
    private String referContent;
    private EditMessageHandler mEditMessageHandler;

    // Views in the layout
    private TextView mReferenceContent;
    private ChatUIEditText mEditText;
    private ImageView mExpandButton;
    private ImageView mEmojiButton;
    private LinearLayout mEditTimeoutContainer;
    private ImageView mCancelButton;
    private ImageView mConfirmButton;
    // Full-screen input Dialog
    private Dialog mExpandDialog;
    // Whether the edit has expired
    private boolean modifiable = false;
    // Current cursor start position index in the input field
    private int selectionStart = 0;

    @SuppressLint("ClickableViewAccessibility")
    EditMessageInputPanel(
            Fragment fragment,
            ViewGroup parent,
            ChannelIdentifier conversationIdentifier,
            String referUid,
            String editMsgUid) {
        if (fragment == null || fragment.getContext() == null) {
            return;
        }
        mFragment = fragment;
        mConversationIdentifier = conversationIdentifier;
        this.editMsgUid = editMsgUid;
        this.referUid = referUid;
        mEditMessageHandler = new EditMessageHandler();
        mEditMessageHandler.addDataChangeListener(
                EditMessageHandler.KEY_ON_MESSAGE_MODIFIED, this::onMessageModify);
        mEditMessageHandler.addDataChangeListener(
                EditMessageHandler.KEY_ON_MESSAGE_REFRESH, this::onRefreshReferenceMessage);
        mRootView =
                LayoutInflater.from(fragment.getContext())
                        .inflate(R.layout.nc_edit_message_input_panel, parent, false);
        mRootView.addOnAttachStateChangeListener(this);

        // Initialize views in the layout
        mReferenceContent = mRootView.findViewById(R.id.nc_reference_content);
        mEditText = mRootView.findViewById(R.id.nc_edit_btn);
        mExpandButton = mRootView.findViewById(R.id.nc_edit_message_expand_btn);
        mEmojiButton = mRootView.findViewById(R.id.nc_edit_message_emoji_btn);
        mEditTimeoutContainer = mRootView.findViewById(R.id.nc_edit_timeout_container);
        mCancelButton = mRootView.findViewById(R.id.nc_edit_message_cancel_btn);
        mConfirmButton = mRootView.findViewById(R.id.nc_edit_message_confirm_btn);
        mEditText.setBackgroundResource(R.drawable.nc_lively_panel_input_background);
        mReferenceContent.setText("");
        mExpandButton.setOnClickListener(view -> expandInputView());
        mEmojiButton.setOnClickListener(
                view ->
                        mExtensionViewModel
                                .getInputModeLiveData()
                                .postValue(InputMode.EmoticonMode));
        mCancelButton.setOnClickListener(v -> EditMessageManager.getInstance().exitEditMode());
        mConfirmButton.setOnClickListener(
                view -> {
                    if (!modifiable) {
                        mEditTimeoutContainer.setVisibility(VISIBLE);
                        mConfirmButton.setImageResource(
                                ChatUIThemeManager.getAttrResId(
                                        mConfirmButton.getContext(),
                                        R.attr.nc_conversation_msg_edit_confirm_unable_img));
                        return;
                    }
                    ErrorHandler callback =
                            new ErrorHandler() {
                                @Override
                                public void onError(NCError error) {
                                    if (error == null) {
                                        EditMessageManager.getInstance().exitEditMode();
                                    } else {
                                        if (error.getCode()
                                                == 33402) { // NC_MODIFIED_MESSAGE_TIMEOUT
                                            modifiable = false;
                                            mEditTimeoutContainer.setVisibility(VISIBLE);
                                            mConfirmButton.setImageResource(
                                                    ChatUIThemeManager.getAttrResId(
                                                            mConfirmButton.getContext(),
                                                            R.attr
                                                                    .nc_conversation_msg_edit_confirm_unable_img));
                                        }
                                    }
                                }
                            };
                    EditMessageManager.getInstance().editMessage(mEditText, callback);
                });
        mEditText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void afterTextChanged(Editable editable) {
                        if (!modifiable) {
                            mEditTimeoutContainer.setVisibility(VISIBLE);
                            mConfirmButton.setImageResource(
                                    ChatUIThemeManager.getAttrResId(
                                            mConfirmButton.getContext(),
                                            R.attr.nc_conversation_msg_edit_confirm_unable_img));
                        } else {
                            boolean isClear = EditMessageUtils.isBlankEditContent(editable);
                            mConfirmButton.setImageResource(
                                    ChatUIThemeManager.getAttrResId(
                                            mConfirmButton.getContext(),
                                            isClear
                                                    ? R.attr
                                                            .nc_conversation_msg_edit_confirm_unable_img
                                                    : R.attr
                                                            .nc_conversation_msg_edit_confirm_enable_img));
                            mConfirmButton.setClickable(!isClear);
                        }
                    }
                });
        mEditText.setOnBackspaceListener(
                new OnDataChangeEnhancedListener<Integer>() {
                    @Override
                    public void onDataChange(Integer position) {
                        if (position == null || mFragment == null) {
                            return;
                        }
                        NCMentionManager.getInstance()
                                .onDeleteClick(
                                        mConversationIdentifier.getChannelType(),
                                        mConversationIdentifier.getChannelId(),
                                        mEditText,
                                        position);
                    }
                });

        mExtensionViewModel = new ViewModelProvider(fragment).get(NCExtensionViewModel.class);

        EditMessageManager.getInstance().addKeyboardHeightObserver(this);
    }

    // Set re-edit content and referenced content
    public void setContent(EditMessageConfig config, boolean showKeyBoard) {
        mEditText.setText(config.content, false);
        mEditText.setSelection(config.content.length());
        if (showKeyBoard) {
            mEditText.requestFocus();
            mExtensionViewModel.forceSetSoftInputKeyBoard(true);
        }
        setReferenceContent(config.referContent);
    }

    // Set referenced content (for both normal and full-screen reference)
    private void setReferenceContent(String referContent) {
        ai.nexconn.chatui.utils.system.ExecutorHelper.getInstance()
                .mainThread()
                .execute(
                        () -> {
                            if (mReferenceContent != null && !TextUtils.isEmpty(referContent)) {
                                mReferenceContent.setVisibility(VISIBLE);
                                EditMessageInputPanel.this.referContent = referContent;
                                mReferenceContent.setText(referContent);
                                if (mExpandDialog != null
                                        && mExpandDialog.isShowing()
                                        && mExpandDialog.getWindow() != null) {
                                    setExpandReferContent(mExpandDialog.getWindow().getDecorView());
                                }
                            }
                        });
    }

    /**
     * Expands to full-screen input page. Process: 1. Hide keyboard; 2. After 100ms delay, slide up
     * full-screen input page and show keyboard.
     */
    private void expandInputView() {
        if (mFragment == null || mFragment.getContext() == null) {
            return;
        }
        if (mEditText != null) {
            this.selectionStart = mEditText.getSelectionStart();
        }
        // 1. Hide the soft keyboard
        if (mExtensionViewModel != null) {
            mExtensionViewModel.forceSetSoftInputKeyBoard(false);
        }

        // 2. After 100ms delay, execute expand animation sliding up the full-screen input page
        mExpandButton.postDelayed(this::showExpandDialog, 200);
    }

    /**
     * Collapses the full-screen input page.
     *
     * @param expandView the full-screen input page view; if null, obtained from Dialog
     */
    private void collapseExpandView(View expandView, boolean exitEditMode) {
        ChatUIEditText expandEditText = expandView.findViewById(R.id.nc_edit_btn_expand);
        if (expandEditText != null) {
            this.selectionStart = expandEditText.getSelectionStart();
        }
        // Collapse full-screen input page and hide soft keyboard
        if (mExtensionViewModel != null) {
            mExtensionViewModel.forceSetSoftInputKeyBoard(false);
        }
        mExpandButton.postDelayed(() -> hideExpandDialog(expandView, exitEditMode), 300);
    }

    /** Shows the full-screen input dialog. */
    private void showExpandDialog() {
        if (mFragment == null || mFragment.getContext() == null) {
            return;
        }
        EditMessageManager.getInstance().setEmoticonMode(false);
        Context context = mFragment.getContext();
        mExpandDialog = new Dialog(context, R.style.FullScreenDialogTheme);

        View expandView =
                LayoutInflater.from(context)
                        .inflate(R.layout.nc_edit_message_input_panel_expand, null);

        // Initialize full-screen input page views and set event listeners
        ViewGroup expandTimeoutContainer =
                expandView.findViewById(R.id.nc_edit_timeout_expand_container);
        ChatUIEditText expandEditText = expandView.findViewById(R.id.nc_edit_btn_expand);
        ImageView expandCollapseButton = expandView.findViewById(R.id.nc_edit_message_collapse_btn);
        ImageView expandConfirmButton = expandView.findViewById(R.id.nc_edit_message_confirm_btn);
        ImageView expandCancelButton = expandView.findViewById(R.id.nc_edit_message_cancel_btn);
        ImageView expandEmojiButton = expandView.findViewById(R.id.nc_edit_message_emoji_btn);
        ConstraintLayout expandEmojiBoardContainer =
                expandView.findViewById(R.id.nc_emoji_board_container);
        expandEditText.setBackgroundResource(R.drawable.nc_lively_panel_input_background);
        updateBoardContainerHeight(expandEmojiBoardContainer);
        // Set up emoji emoticon panel, initially hidden.
        EmoticonBoard mEmoticonBoard =
                new EmoticonBoard(
                        mFragment,
                        expandEmojiBoardContainer,
                        mConversationIdentifier.getChannelType(),
                        mConversationIdentifier.getChannelId(),
                        false);
        ChatUIViewUtils.addView(expandEmojiBoardContainer, mEmoticonBoard.getView());
        expandEmojiBoardContainer.setVisibility(GONE);
        // Set edit-ability state
        if (modifiable) {
            expandTimeoutContainer.setVisibility(GONE);
            expandConfirmButton.setImageResource(
                    ChatUIThemeManager.getAttrResId(
                            mConfirmButton.getContext(),
                            R.attr.nc_conversation_msg_edit_confirm_enable_img));
        } else {
            expandTimeoutContainer.setVisibility(VISIBLE);
            expandConfirmButton.setImageResource(
                    ChatUIThemeManager.getAttrResId(
                            mConfirmButton.getContext(),
                            R.attr.nc_conversation_msg_edit_confirm_unable_img));
        }
        expandEditText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void afterTextChanged(Editable editable) {
                        if (!modifiable) {
                            expandTimeoutContainer.setVisibility(VISIBLE);
                            expandConfirmButton.setImageResource(
                                    ChatUIThemeManager.getAttrResId(
                                            mConfirmButton.getContext(),
                                            R.attr.nc_conversation_msg_edit_confirm_unable_img));
                        } else {
                            boolean isClear = EditMessageUtils.isBlankEditContent(editable);
                            expandConfirmButton.setImageResource(
                                    ChatUIThemeManager.getAttrResId(
                                            expandConfirmButton.getContext(),
                                            isClear
                                                    ? R.attr
                                                            .nc_conversation_msg_edit_confirm_unable_img
                                                    : R.attr
                                                            .nc_conversation_msg_edit_confirm_enable_img));
                            expandConfirmButton.setClickable(!isClear);
                        }
                    }
                });
        expandEditText.setOnBackspaceListener(
                new OnDataChangeEnhancedListener<Integer>() {
                    @Override
                    public void onDataChange(Integer position) {
                        if (position == null || mFragment == null) {
                            return;
                        }
                        NCMentionManager.getInstance()
                                .onDeleteClick(
                                        mConversationIdentifier.getChannelType(),
                                        mConversationIdentifier.getChannelId(),
                                        expandEditText,
                                        position);
                    }
                });

        // Set collapse button click event
        expandCollapseButton.setOnClickListener(v -> collapseExpandView(expandView, false));

        // Set confirm button click event
        expandConfirmButton.setOnClickListener(
                v -> {
                    if (!modifiable) {
                        expandTimeoutContainer.setVisibility(VISIBLE);
                        expandConfirmButton.setImageResource(
                                ChatUIThemeManager.getAttrResId(
                                        mConfirmButton.getContext(),
                                        R.attr.nc_conversation_msg_edit_confirm_unable_img));
                        return;
                    }
                    ErrorHandler callback =
                            new ErrorHandler() {
                                @Override
                                public void onError(NCError error) {
                                    if (error == null) {
                                        collapseExpandView(expandView, true);
                                    } else {
                                        if (error.getCode()
                                                == 33402) { // NC_MODIFIED_MESSAGE_TIMEOUT
                                            modifiable = false;
                                            expandTimeoutContainer.setVisibility(VISIBLE);
                                            expandConfirmButton.setImageResource(
                                                    ChatUIThemeManager.getAttrResId(
                                                            mConfirmButton.getContext(),
                                                            R.attr
                                                                    .nc_conversation_msg_edit_confirm_unable_img));
                                        }
                                    }
                                }
                            };
                    EditMessageManager.getInstance().editMessage(expandEditText, callback);
                });
        // Set cancel button click event
        expandCancelButton.setOnClickListener(view -> collapseExpandView(expandView, true));
        // Set Emoji button click event
        expandEmojiButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        EditMessageManager.getInstance().setEmoticonMode(true);
                        mExtensionViewModel.forceSetSoftInputKeyBoard(false);
                        ViewGroup emojiBoardContainer =
                                mExpandDialog.findViewById(R.id.nc_emoji_board_container);
                        emojiBoardContainer.setVisibility(VISIBLE);
                    }
                });

        // Sync current input content to full-screen page
        if (mEditText != null && !TextUtils.isEmpty(mEditText.getText())) {
            expandEditText.setText(mEditText.getText().toString(), false);
            expandEditText.setSelection(this.selectionStart, this.selectionStart);
        }
        // Sync current reference content to full-screen page
        setExpandReferContent(expandView);

        mExpandDialog.setContentView(expandView);
        mExpandDialog.setCancelable(false);

        // Set dialog properties
        if (mExpandDialog.getWindow() != null) {
            Window window = mExpandDialog.getWindow();
            WindowManager.LayoutParams lp = mExpandDialog.getWindow().getAttributes();
            lp.gravity = Gravity.BOTTOM;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(lp);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Cancel fullscreen, allow status bar display
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            // Allow custom status bar color
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // Remove translucent status bar
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // Set status bar color
            window.setStatusBarColor(Color.parseColor("#99000000"));
            // Set keyboard resize mode
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }
        mExpandDialog.show();
        mExpandDialog.setOnDismissListener(
                dialog -> EditMessageManager.getInstance().setEmoticonMode(false));

        onKeyboardStatusChange(true);

        // Execute slide-up expand animation
        TranslateAnimation slideUpAnimation =
                new TranslateAnimation(0, 0, expandView.getHeight(), 0);
        slideUpAnimation.setDuration(300);
        slideUpAnimation.setInterpolator(new DecelerateInterpolator());
        slideUpAnimation.setAnimationListener(
                new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        // Open soft keyboard after animation ends
                        expandView.postDelayed(
                                () -> {
                                    if (mExtensionViewModel != null) {
                                        mExtensionViewModel.setEditTextWidget(expandEditText);
                                        EditMessageConfig config =
                                                EditMessageManager.getInstance()
                                                        .getEditMessageConfig();
                                        if (config != null) {
                                            EditMessageManager.getInstance()
                                                    .addMentionBlocks(
                                                            expandEditText, config.mentionBlocks);
                                        }
                                        mExtensionViewModel.forceSetSoftInputKeyBoard(true);
                                    }
                                },
                                150);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
        expandView.startAnimation(slideUpAnimation);
    }

    /**
     * Hides the full-screen input dialog.
     *
     * @param expandView the full-screen input page view; if null, the Dialog is closed directly
     */
    private void hideExpandDialog(View expandView, boolean exitEditMode) {
        if (mExpandDialog == null || !mExpandDialog.isShowing()) {
            return;
        }
        // Not exiting edit mode; sync full-screen page content back to main page
        if (!exitEditMode) {
            // Restore the original EditText
            if (mExtensionViewModel != null && mEditText != null) {
                mExtensionViewModel.setEditTextWidget(mEditText);
                EditMessageConfig config = EditMessageManager.getInstance().getEditMessageConfig();
                if (config != null) {
                    // Rebind the MentionList for the EditText in NCMentionManager.
                    EditMessageManager.getInstance()
                            .addMentionBlocks(mEditText, config.mentionBlocks);
                }
                ChatUIEditText expandEditText = expandView.findViewById(R.id.nc_edit_btn_expand);
                if (expandEditText != null) {
                    String content = "";
                    if (!TextUtils.isEmpty(expandEditText.getText())) {
                        content = expandEditText.getText().toString();
                    }
                    mEditText.setText(content, false);
                    mEditText.setSelection(this.selectionStart, this.selectionStart);
                }
            }
        }
        // Close full-screen popup, execute animation
        if (mExpandDialog.getWindow() != null) {
            Window window = mExpandDialog.getWindow();
            // Set status bar color
            window.setStatusBarColor(Color.TRANSPARENT);
            mExpandDialog.findViewById(R.id.nc_edit_top_bar).setBackgroundColor(Color.TRANSPARENT);
        }
        // Execute slide-down collapse animation
        TranslateAnimation slideDownAnimation =
                new TranslateAnimation(0, 0, 0, expandView.getHeight());
        slideDownAnimation.setDuration(300);
        slideDownAnimation.setInterpolator(new DecelerateInterpolator());

        slideDownAnimation.setAnimationListener(
                new android.view.animation.Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(android.view.animation.Animation animation) {}

                    @Override
                    public void onAnimationEnd(android.view.animation.Animation animation) {
                        if (mExpandDialog != null) {
                            mExpandDialog.dismiss();
                            mExpandDialog = null;
                        }
                        // Exit edit mode
                        if (exitEditMode) {
                            EditMessageManager.getInstance().exitEditMode();
                            return;
                        }
                        // Show soft keyboard
                        if (mExtensionViewModel != null && mEditText != null) {
                            mEditText.postDelayed(
                                    () -> mExtensionViewModel.forceSetSoftInputKeyBoard(true), 200);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(android.view.animation.Animation animation) {}
                });

        expandView.startAnimation(slideDownAnimation);
    }

    private void setExpandReferContent(View expandView) {
        if (expandView != null
                && mReferenceContent != null
                && !TextUtils.isEmpty(mReferenceContent.getText())) {
            TextView expandReferContent = expandView.findViewById(R.id.nc_reference_content);
            expandReferContent.setText(mReferenceContent.getText().toString());
        }
    }

    // Listen for soft keyboard show/hide events
    public void onKeyboardStatusChange(boolean isKeyboardShow) {
        if (mExpandDialog == null) {
            return;
        }
        ViewGroup emojiBoardContainer = mExpandDialog.findViewById(R.id.nc_emoji_board_container);
        if (isKeyboardShow) {
            EditMessageManager.getInstance().setEmoticonMode(false);
            emojiBoardContainer.setVisibility(View.INVISIBLE);
        } else {
            emojiBoardContainer.setVisibility(
                    EditMessageManager.getInstance().isEmoticonMode() ? VISIBLE : GONE);
        }
    }

    /**
     * Recalculates emoji panel height to prevent abnormally large values. Note: uses the same
     * height logic as NCExtension#updateInputMode.
     */
    private void updateBoardContainerHeight(ConstraintLayout expandEmojiBoardContainer) {
        if (mFragment == null || mFragment.getContext() == null) {
            return;
        }
        if (!useKeyboardHeightProvider()) {
            return;
        }
        Context context = mFragment.getContext();
        int saveKeyboardHeight =
                ChatUIUtils.getSaveKeyBoardHeight(
                        context, context.getResources().getConfiguration().orientation);
        ViewGroup.LayoutParams layoutParams = expandEmojiBoardContainer.getLayoutParams();
        int boardHeight =
                expandEmojiBoardContainer
                        .getResources()
                        .getDimensionPixelSize(R.dimen.nc_extension_board_height);
        if (saveKeyboardHeight <= 0 && layoutParams.height != boardHeight) {
            layoutParams.height = boardHeight;
            expandEmojiBoardContainer.setLayoutParams(layoutParams);
        } else if (layoutParams.height != saveKeyboardHeight) {
            layoutParams.height = saveKeyboardHeight;
            expandEmojiBoardContainer.setLayoutParams(layoutParams);
        }
    }

    private boolean useKeyboardHeightProvider() {
        if (mFragment == null || mFragment.getActivity() == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Activity activity = mFragment.getActivity();
            return activity != null && !activity.isInMultiWindowMode();
        }
        return false;
    }

    public void onDestroy() {
        // Clean up full-screen dialog
        if (mExpandDialog != null && mExpandDialog.isShowing()) {
            mExpandDialog.dismiss();
            mExpandDialog = null;
        }
        mFragment = null;
        mExtensionViewModel = null;
        mExpandButton.setOnClickListener(null);
        mEmojiButton.setOnClickListener(null);
        mCancelButton.setOnClickListener(null);
        mConfirmButton.setOnClickListener(null);
        mEditText.removeAllTextChangedListener();
        mEditText.setOnBackspaceListener(null);
        EditMessageManager.getInstance().removeKeyboardHeightObserver(this);
        mEditMessageHandler.stop();
        NCChatUI.removeMessageEventListener(this);
        NCEngine.removeMessageHandler(mRecallHandlerId);
    }

    View getRootView() {
        return mRootView;
    }

    EditText getEditText() {
        return mEditText;
    }

    public void setCheckMessageModifiableResult(boolean modifiable) {
        this.modifiable = modifiable;
        if (modifiable) {
            mEditTimeoutContainer.setVisibility(GONE);
            mConfirmButton.setImageResource(
                    ChatUIThemeManager.getAttrResId(
                            mConfirmButton.getContext(),
                            R.attr.nc_conversation_msg_edit_confirm_enable_img));
        } else {
            mEditTimeoutContainer.setVisibility(VISIBLE);
            mConfirmButton.setImageResource(
                    ChatUIThemeManager.getAttrResId(
                            mConfirmButton.getContext(),
                            R.attr.nc_conversation_msg_edit_confirm_unable_img));
        }
    }

    @Override
    public void onKeyboardHeightChanged(int orientation, boolean isOpen, int keyboardHeight) {
        onKeyboardStatusChange(isOpen);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull View v) {
        NCChatUI.addMessageEventListener(this);
        NCEngine.addMessageHandler(mRecallHandlerId, mRecallHandler);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull View v) {
        v.removeOnAttachStateChangeListener(this);
        onDestroy();
    }

    @Override
    public void onDeleteMessage(@NonNull DeleteEvent event) {
        // Local deletion
        if (mFragment == null || mFragment.getContext() == null) {
            return;
        }
        // If reference content is already in a final state (recalled or deleted), no need to query
        String delete = mFragment.getContext().getString(R.string.nc_reference_status_delete);
        String recall = mFragment.getContext().getString(R.string.nc_reference_status_recall);
        if (isReferContentEndWithStatus(referContent, delete)
                || isReferContentEndWithStatus(referContent, recall)) {
            return;
        }
        // When updating reference content status, only update if the deleted message UID matches
        // the UID saved in the current edit component. Re-query the message to update.
        mEditMessageHandler.refreshReferenceMessage(editMsgUid, mConversationIdentifier);
    }

    /**
     * Checks whether the reference content ends with the specified status. Rule: must end with
     * COLON_SEPARATOR + status, and there can only be one such combination.
     *
     * @param referContent the reference content
     * @param status the status string (delete or recall)
     * @return true if it ends with the specified status, false otherwise
     */
    private boolean isReferContentEndWithStatus(String referContent, String status) {
        if (TextUtils.isEmpty(referContent) || TextUtils.isEmpty(status)) {
            return false;
        }

        String expectedSuffix = COLON_SEPARATOR + status;

        // Check if it ends with the expected suffix
        if (!referContent.endsWith(expectedSuffix)) {
            return false;
        }

        // Ensure there is only one COLON_SEPARATOR + status combination
        // Remove the trailing suffix and check if the remaining part still contains the same suffix
        String remainingContent =
                referContent.substring(0, referContent.length() - expectedSuffix.length());
        return !remainingContent.contains(expectedSuffix);
    }

    // Update reference content
    private void onRefreshReferenceMessage(Message message) {
        if (mFragment == null || mFragment.getContext() == null) {
            return;
        }
        if (!(message.getContent() instanceof ReferenceMessage)) {
            return;
        }
        String name = EditMessageUtils.getDisplayName(message);
        String content = EditMessageUtils.getReferContent(message);
        setReferenceContent(name + COLON_SEPARATOR + content);
    }

    // After recall, update reference content
    private void onMessageRecalled(Message recallMessage) {
        if (mFragment == null || mFragment.getContext() == null) {
            return;
        }
        if (!TextUtils.equals(referUid, recallMessage.getMessageId())) {
            return;
        }
        String name = EditMessageUtils.getDisplayName(recallMessage);
        String content = mFragment.getContext().getString(R.string.nc_reference_status_recall);
        setReferenceContent(name + COLON_SEPARATOR + content);
    }

    private void onMessageModify(List<Message> messages) {
        if (mFragment == null || mFragment.getContext() == null) {
            return;
        }
        for (Message message : messages) {
            if (!TextUtils.equals(message.getMessageId(), referUid)) {
                continue;
            }
            String name = EditMessageUtils.getDisplayName(message);
            String content = EditMessageUtils.getOriginalContent(message);
            setReferenceContent(name + COLON_SEPARATOR + content);
        }
    }
}
