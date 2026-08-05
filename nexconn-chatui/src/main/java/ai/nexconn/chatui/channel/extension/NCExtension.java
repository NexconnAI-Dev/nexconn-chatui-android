package ai.nexconn.chatui.channel.extension;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.ChannelViewModel;
import ai.nexconn.chatui.channel.event.page.InputBarEvent;
import ai.nexconn.chatui.channel.event.page.PageEvent;
import ai.nexconn.chatui.channel.extension.component.emoticon.EmoticonBoard;
import ai.nexconn.chatui.channel.extension.component.inputpanel.InputPanel;
import ai.nexconn.chatui.channel.extension.component.moreaction.MoreInputPanel;
import ai.nexconn.chatui.channel.extension.component.plugin.IPluginModule;
import ai.nexconn.chatui.channel.extension.component.plugin.IPluginRequestPermissionResultCallback;
import ai.nexconn.chatui.channel.extension.component.plugin.PluginBoard;
import ai.nexconn.chatui.channel.feature.editmessage.EditMessageManager;
import ai.nexconn.chatui.channel.feature.mention.IExtensionEventWatcher;
import ai.nexconn.chatui.channel.feature.mention.NCMentionManager;
import ai.nexconn.chatui.utils.common.ChatUIUtils;
import ai.nexconn.chatui.utils.keyboard.KeyboardHeightObserver;
import ai.nexconn.chatui.utils.keyboard.KeyboardHeightProvider;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.permission.PermissionCheckUtil;
import ai.nexconn.chatui.utils.view.ChatUIViewUtils;
import ai.nexconn.chatui.widget.ChatUIEditText;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class NCExtension extends LinearLayout {
    private String TAG = NCExtension.class.getSimpleName();
    private Fragment mFragment;
    private ChannelIdentifier mConversationIdentifier;
    private ViewGroup mRoot;
    private NCExtensionViewModel mExtensionViewModel;
    private ChannelViewModel mMessageViewModel;

    private RelativeLayout mAttachedInfoContainer;
    private RelativeLayout mBoardContainer;
    private RelativeLayout mInputPanelContainer;
    private InputPanel mInputPanel;
    private EmoticonBoard mEmoticonBoard;
    private PluginBoard mPluginBoard;
    private InputPanel.InputStyle mInputStyle;
    private MoreInputPanel mMoreInputPanel;
    private InputMode mPreInputMode;

    private KeyboardHeightProvider keyboardHeightProvider = null;
    private boolean editTextIsFocused = false;

    private final KeyboardHeightObserver mKeyboardHeightObserver =
            new KeyboardHeightObserver() {

                @Override
                public void onKeyboardHeightChanged(
                        int orientation, boolean isOpen, int keyboardHeight) {
                    if (getActivityFromView() != null) {
                        if (isOpen) {
                            int saveKeyBoardHeight =
                                    ChatUIUtils.getSaveKeyBoardHeight(getContext(), orientation);
                            if (saveKeyBoardHeight != keyboardHeight) {
                                ChatUIUtils.saveKeyboardHeight(
                                        getContext(), orientation, keyboardHeight);
                                updateBoardContainerHeight();
                            }
                            mBoardContainer.setVisibility(VISIBLE);
                            mExtensionViewModel.getExtensionBoardState().setValue(true);
                        } else {
                            if (mExtensionViewModel != null) {
                                mExtensionViewModel.setSoftInputKeyBoard(false, true);
                                if (mPreInputMode != null
                                        && (mPreInputMode == InputMode.TextInput
                                                || mPreInputMode == InputMode.VoiceInput)) {
                                    mBoardContainer.setVisibility(GONE);
                                    mExtensionViewModel.getExtensionBoardState().setValue(false);
                                }
                            }
                        }
                        EditMessageManager.getInstance()
                                .onKeyboardHeightChange(orientation, isOpen, keyboardHeight);
                    }
                }
            };

    /**
     * NCExtension constructor.
     *
     * @param context Context.
     */
    public NCExtension(Context context) {
        super(context);
        initView(context);
    }

    /**
     * NCExtension constructor.
     *
     * @param context Context.
     * @param attrs View attribute set.
     */
    public NCExtension(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NCExtension);
        int attr = a.getInt(R.styleable.NCExtension_NCStyle, 0x123);
        a.recycle();
        mInputStyle = InputPanel.InputStyle.getStyle(attr);
        initView(context);
    }

    private void initView(Context context) {
        mRoot =
                (LinearLayout)
                        LayoutInflater.from(context)
                                .inflate(R.layout.nc_extension_board, this, true);
        mAttachedInfoContainer = mRoot.findViewById(R.id.nc_ext_attached_info_container);
        mInputPanelContainer = mRoot.findViewById(R.id.nc_ext_input_container);
        mBoardContainer = mRoot.findViewById(R.id.nc_ext_board_container);
        View extInputDivider = mRoot.findViewById(R.id.nc_ext_input_divider);
        if (extInputDivider != null) {
            extInputDivider.setVisibility(GONE);
        }
    }

    public void bindToConversation(
            Fragment fragment,
            ChannelIdentifier conversationIdentifier,
            boolean disableSystemEmoji) {
        mFragment = fragment;
        mConversationIdentifier = conversationIdentifier;
        mExtensionViewModel = new ViewModelProvider(mFragment).get(NCExtensionViewModel.class);
        mExtensionViewModel
                .getAttachedInfoState()
                .observe(
                        mFragment,
                        new Observer<Boolean>() {
                            @Override
                            public void onChanged(Boolean isVisible) {
                                mAttachedInfoContainer.setVisibility(isVisible ? VISIBLE : GONE);
                            }
                        });
        mExtensionViewModel
                .getExtensionBoardState()
                .observe(
                        mFragment,
                        new Observer<Boolean>() {
                            @Override
                            public void onChanged(Boolean value) {
                                if (!value) {
                                    mBoardContainer.setVisibility(GONE);
                                }
                            }
                        });
        mMessageViewModel = new ViewModelProvider(mFragment).get(ChannelViewModel.class);
        mMessageViewModel
                .getPageEventLiveData()
                .observe(
                        mFragment,
                        new Observer<PageEvent>() {
                            @Override
                            public void onChanged(PageEvent pageEvent) {
                                if (pageEvent instanceof InputBarEvent) {
                                    if (((InputBarEvent) pageEvent)
                                            .mType.equals(InputBarEvent.Type.ReEdit)) {
                                        Runnable reEditTask =
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        insertToEditText(
                                                                ((InputBarEvent) pageEvent).mExtra);
                                                        // May be in voice recognition mode when
                                                        // recall is tapped; switch to text mode
                                                        mExtensionViewModel
                                                                .getInputModeLiveData()
                                                                .postValue(InputMode.TextInput);
                                                    }
                                                };
                                        if (EditMessageManager.getInstance().isEditMessageState()) {
                                            EditMessageManager.getInstance().exitEditMode();
                                            postDelayed(reEditTask, 300);
                                        } else {
                                            post(reEditTask);
                                        }
                                    } else if (((InputBarEvent) pageEvent)
                                            .mType.equals(InputBarEvent.Type.ShowMoreMenu)) {
                                        mExtensionViewModel
                                                .getInputModeLiveData()
                                                .postValue(InputMode.MoreInputMode);
                                    } else if (((InputBarEvent) pageEvent)
                                            .mType.equals(InputBarEvent.Type.HideMoreMenu)) {
                                        resetToDefaultView(((InputBarEvent) pageEvent).mExtra);
                                    } else if (((InputBarEvent) pageEvent)
                                                    .mType.equals(InputBarEvent.Type.ActiveMoreMenu)
                                            && mMoreInputPanel != null) {
                                        mMoreInputPanel.refreshView(true);
                                    } else if (((InputBarEvent) pageEvent)
                                                    .mType.equals(
                                                            InputBarEvent.Type.InactiveMoreMenu)
                                            && mMoreInputPanel != null) {
                                        mMoreInputPanel.refreshView(false);
                                    }
                                }
                            }
                        });
        ChannelType channelType =
                mConversationIdentifier != null ? mConversationIdentifier.getChannelType() : null;
        mEmoticonBoard =
                new EmoticonBoard(
                        fragment, mBoardContainer, channelType, getTargetId(), disableSystemEmoji);
        mPluginBoard = new PluginBoard(fragment, mBoardContainer, channelType, getTargetId());
        mInputPanel =
                new InputPanel(
                        fragment, mInputPanelContainer, mInputStyle, mConversationIdentifier);

        if (mInputPanelContainer.getChildCount() <= 0) {
            ChatUIViewUtils.addView(mInputPanelContainer, mInputPanel.getRootView());
        }
        mExtensionViewModel.setAttachedConversation(
                conversationIdentifier, mInputPanel.getEditText());
        mExtensionViewModel
                .getInputModeLiveData()
                .observe(
                        mFragment,
                        new Observer<InputMode>() {
                            @Override
                            public void onChanged(InputMode inputMode) {
                                mPreInputMode = inputMode;
                                updateInputMode(inputMode);
                            }
                        });
        for (IExtensionModule module : NCExtensionManager.getInstance().getExtensionModules()) {
            module.onAttachedToExtension(fragment, this);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    /** Lifecycle callback when ChannelFragment onResume() is called. */
    public void onResume() {
        if (mExtensionViewModel == null) {
            return;
        }
        if (useKeyboardHeightProvider()) {
            keyboardHeightProvider = new KeyboardHeightProvider(getActivityFromView());
            keyboardHeightProvider.setKeyboardHeightObserver(mKeyboardHeightObserver);
        }
        this.post(
                new Runnable() {
                    @Override
                    public void run() {
                        KeyboardHeightProvider keyboardHeightProvider =
                                NCExtension.this.keyboardHeightProvider;
                        if (keyboardHeightProvider != null) {
                            NCExtension.this.keyboardHeightProvider.start();
                        }
                    }
                });
        final EditText editText = mExtensionViewModel.getEditTextWidget();
        if (editText != null) {
            if (editTextIsFocused) {
                this.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                mExtensionViewModel.forceSetSoftInputKeyBoard(true);
                            }
                        },
                        300);
            }

            if (editText instanceof ChatUIEditText) {
                ((ChatUIEditText) editText)
                        .setOnBackspaceListener(
                                position -> {
                                    if (position == null) {
                                        return;
                                    }
                                    NCMentionManager.getInstance()
                                            .onDeleteClick(
                                                    getConversationType(),
                                                    getTargetId(),
                                                    editText,
                                                    position);
                                });
            }
        }
        EditMessageManager.getInstance().onResume();
    }

    public void onPause() {
        if (keyboardHeightProvider != null) {
            keyboardHeightProvider.stop();
            keyboardHeightProvider.setKeyboardHeightObserver(null);
            keyboardHeightProvider = null;
        }
        if (mExtensionViewModel != null) {
            if (mExtensionViewModel.getEditTextWidget() != null) {
                editTextIsFocused = mExtensionViewModel.getEditTextWidget().isFocused();
            }
            if (mPreInputMode != null
                    && mPreInputMode == InputMode.TextInput
                    && mBoardContainer != null) {
                mExtensionViewModel.collapseExtensionBoard();
            }
        }
        if (mInputPanel != null) {
            mInputPanel.onPause();
        }
        EditMessageManager.getInstance().onPause();
    }

    public void setAttachedInfo(View view) {
        mAttachedInfoContainer.removeAllViews();
        if (view != null) {
            mAttachedInfoContainer.addView(view);
        }
        mAttachedInfoContainer.setVisibility(VISIBLE);
    }

    /**
     * Gets the container for each component of the extension.
     *
     * @param type Container type.
     * @return The container.
     */
    public RelativeLayout getContainer(ContainerType type) {
        if (type == null) {
            return null;
        }
        if (type.equals(ContainerType.ATTACH)) {
            return mAttachedInfoContainer;
        } else if (type.equals(ContainerType.INPUT)) {
            return mInputPanelContainer;
        } else {
            return mBoardContainer;
        }
    }

    public InputPanel getInputPanel() {
        return mInputPanel;
    }

    public PluginBoard getPluginBoard() {
        return mPluginBoard;
    }

    public EmoticonBoard getEmoticonBoard() {
        return mEmoticonBoard;
    }

    public void resetToDefaultView() {
        resetToDefaultView(null);
        getInputPanel().getDraft();
    }

    public void resetToDefaultView(String conversationType) {
        resetToDefaultView(conversationType, InputMode.NormalMode, false);
    }

    public void resetToDefaultView(
            String conversationType, InputMode mode, boolean forceShowKeyBoard) {
        mInputPanelContainer.removeAllViews();
        if (mInputPanel == null) {
            mInputPanel =
                    new InputPanel(
                            mFragment, mInputPanelContainer, mInputStyle, mConversationIdentifier);
        }
        mExtensionViewModel.setEditTextWidget(mInputPanel.getEditText());
        ChatUIViewUtils.addView(mInputPanelContainer, mInputPanel.getRootView());
        if (mFragment.getContext() != null) {
            mAttachedInfoContainer.removeAllViews();
            mAttachedInfoContainer.setVisibility(GONE);
            // After exiting more mode or burn-after-reading mode, reset to normal (non-input) mode
            updateInputMode(mode, forceShowKeyBoard);
        }
    }

    public void updateInputMode(InputMode inputMode) {
        updateInputMode(inputMode, false);
    }

    public void updateInputMode(InputMode inputMode, boolean forceShowKeyBoard) {
        if (inputMode == null) {
            return;
        }
        RLog.d(TAG, "update to inputMode:" + inputMode);
        if (inputMode.equals(InputMode.TextInput)) {
            // In text input mode, the soft keyboard only appears when the EditText has focus or
            // content.
            // For special cases, business modules should manually call
            // NCExtensionViewModel.setSoftInputKeyBoard() to show the keyboard
            EditText editText = mExtensionViewModel.getEditTextWidget();
            if (editText == null || editText.getText() == null) {
                return;
            }
            if (isEditTextSameProperty(editText) && !forceShowKeyBoard) {
                return;
            }
            RLog.d(TAG, "update for TextInput mode");
            mInputPanelContainer.setVisibility(VISIBLE);

            updateBoardContainerHeight();
            mBoardContainer.removeAllViews();
            ChatUIViewUtils.addView(mBoardContainer, mPluginBoard.getView());

            if (!useKeyboardHeightProvider()) {
                mExtensionViewModel.getExtensionBoardState().setValue(false);
            } else {
                mExtensionViewModel.getExtensionBoardState().setValue(true);
            }

            if (forceShowKeyBoard || (editText.isFocused() || editText.getText().length() > 0)) {
                this.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (mFragment != null
                                        && mFragment.getActivity() != null
                                        && !mFragment.getActivity().isFinishing()) {
                                    mExtensionViewModel.setSoftInputKeyBoard(true);
                                }
                            }
                        },
                        100);
            } else {
                mExtensionViewModel.setSoftInputKeyBoard(false);
                mExtensionViewModel.getExtensionBoardState().setValue(false);
            }
        } else if (inputMode.equals(InputMode.VoiceInput)) {
            mInputPanelContainer.setVisibility(VISIBLE);
            mBoardContainer.setVisibility(GONE);
            mExtensionViewModel.forceSetSoftInputKeyBoard(false);
            mExtensionViewModel.getExtensionBoardState().setValue(false);
        } else if (inputMode.equals(InputMode.EmoticonMode)) {
            mExtensionViewModel.setSoftInputKeyBoard(false);
            this.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            updateBoardContainerHeight();
                            mBoardContainer.removeAllViews();
                            ChatUIViewUtils.addView(mBoardContainer, mEmoticonBoard.getView());
                            mBoardContainer.setVisibility(VISIBLE);
                            mExtensionViewModel.getExtensionBoardState().setValue(true);
                        }
                    },
                    100);
        } else if (inputMode.equals(InputMode.PluginMode)) {
            mExtensionViewModel.setSoftInputKeyBoard(false);
            this.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            updateBoardContainerHeight();
                            mBoardContainer.removeAllViews();
                            ChatUIViewUtils.addView(mBoardContainer, mPluginBoard.getView());
                            mBoardContainer.setVisibility(VISIBLE);
                            mExtensionViewModel.forceSetSoftInputKeyBoard(false);
                            mExtensionViewModel.getExtensionBoardState().setValue(true);
                        }
                    },
                    100);

        } else if (inputMode.equals(InputMode.MoreInputMode)) {
            mInputPanelContainer.setVisibility(GONE);
            mBoardContainer.setVisibility(GONE);
            if (mMoreInputPanel == null) {
                mMoreInputPanel = new MoreInputPanel(mFragment, mAttachedInfoContainer);
            }
            mAttachedInfoContainer.removeAllViews();
            ChatUIViewUtils.addView(mAttachedInfoContainer, mMoreInputPanel.getRootView());
            mAttachedInfoContainer.setVisibility(VISIBLE);
            mExtensionViewModel.setSoftInputKeyBoard(false);
            mExtensionViewModel.getExtensionBoardState().setValue(false);
        } else if (inputMode.equals(InputMode.QuickReplyMode)) {
            mInputPanelContainer.setVisibility(VISIBLE);
            mBoardContainer.setVisibility(VISIBLE);
            mExtensionViewModel.forceSetSoftInputKeyBoard(false);
            mExtensionViewModel.getExtensionBoardState().setValue(true);
        } else if (inputMode.equals(InputMode.NormalMode)) {
            mInputPanelContainer.setVisibility(VISIBLE);
            mBoardContainer.setVisibility(GONE);
            mExtensionViewModel.forceSetSoftInputKeyBoard(false);
            mExtensionViewModel.getExtensionBoardState().setValue(false);
        }
    }

    private void updateBoardContainerHeight() {
        if (!useKeyboardHeightProvider()) {
            return;
        }
        int saveKeyboardHeight =
                ChatUIUtils.getSaveKeyBoardHeight(
                        getContext(), getContext().getResources().getConfiguration().orientation);
        ViewGroup.LayoutParams layoutParams = mBoardContainer.getLayoutParams();
        if (saveKeyboardHeight <= 0
                && layoutParams.height
                        != getResources()
                                .getDimensionPixelSize(R.dimen.nc_extension_board_height)) {
            layoutParams.height =
                    getResources().getDimensionPixelSize(R.dimen.nc_extension_board_height);
            mBoardContainer.setLayoutParams(layoutParams);
        } else if (layoutParams.height != saveKeyboardHeight) {
            layoutParams.height = saveKeyboardHeight;
            mBoardContainer.setLayoutParams(layoutParams);
        }
    }

    /** Collapse the panel. Kept for backward compatibility. Prefer using the ViewModel method. */
    public void collapseExtension() {
        RLog.d(TAG, "collapseExtension");
        mExtensionViewModel.collapseExtensionBoard();
    }

    /**
     * Adds a custom view to the plugin area. After adding, the "+" area is entirely filled with the
     * custom view. When the custom view is visible, tapping "+" toggles between the custom view and
     * the default plugin panel.
     *
     * @param v Custom view.
     */
    public void addPluginPager(View v) {
        if (null != mPluginBoard) {
            mPluginBoard.addPager(v);
        }
    }

    public EditText getInputEditText() {
        return mInputPanel != null ? mInputPanel.getEditText() : null;
    }

    /**
     * Gets the channel type of the current conversation where the Extension resides.
     *
     * @return Channel type.
     */
    public ChannelType getConversationType() {
        if (mConversationIdentifier == null) {
            RLog.e(TAG, "getConversationType mConversationIdentifier is null");
            return null;
        }
        return mConversationIdentifier.getChannelType();
    }

    /**
     * Gets the targetId of the current conversation.
     *
     * @return Target ID.
     */
    public String getTargetId() {
        if (mConversationIdentifier == null) {
            RLog.e(TAG, "getTargetId mConversationIdentifier is null");
            return "";
        }
        return mConversationIdentifier.getChannelId();
    }

    public ChannelIdentifier getConversationIdentifier() {
        return mConversationIdentifier;
    }

    public ChannelIdentifier getChannelIdentifier() {
        return mConversationIdentifier;
    }

    public void requestPermissionForPluginResult(
            String[] permissions, int requestCode, IPluginModule pluginModule) {
        if ((requestCode & 0xffffff00) != 0) {
            throw new IllegalArgumentException("requestCode must less than 256");
        }
        if (null == mPluginBoard) {
            return;
        }
        int position = mPluginBoard.getPluginPosition(pluginModule);
        int req = ((position + 1) << 8) + (requestCode & 0xff);
        PermissionCheckUtil.requestPermissions(mFragment, permissions, req);
    }

    public boolean onRequestPermissionResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        int position = (requestCode >> 8) - 1;
        int reqCode = requestCode & 0xFF;
        if (null == mPluginBoard) {
            return false;
        }
        IPluginModule pluginModule = mPluginBoard.getPluginModule(position);
        if (pluginModule instanceof IPluginRequestPermissionResultCallback) {
            return ((IPluginRequestPermissionResultCallback) pluginModule)
                    .onRequestPermissionResult(mFragment, this, reqCode, permissions, grantResults);
        }
        return false;
    }

    /**
     * @param intent The intent to start.
     * @param requestCode {@code If >= 0, this code will be returned in onActivityResult() when the
     *     activity exits.}
     */
    public void startActivityForPluginResult(
            Intent intent, int requestCode, IPluginModule pluginModule) {
        if ((requestCode & 0xffffff00) != 0) {
            throw new IllegalArgumentException("requestCode must less than 256.");
        }
        if (null == mPluginBoard) {
            return;
        }
        int position = mPluginBoard.getPluginPosition(pluginModule);
        mFragment.startActivityForResult(intent, ((position + 1) << 8) + (requestCode & 0xff));
    }

    /** Called when an activity finishes and returns a result. */
    public void onActivityPluginResult(int requestCode, int resultCode, Intent data) {
        int position = (requestCode >> 8) - 1;
        int reqCode = requestCode & 0xff;
        if (null == mPluginBoard) {
            return;
        }
        IPluginModule pluginModule = mPluginBoard.getPluginModule(position);
        if (pluginModule != null) {
            pluginModule.onActivityResult(reqCode, resultCode, data);
        }
    }

    public void onDestroy() {
        // TODO: Abstract internal components into a base interface for unified invocation.
        if (mInputPanel != null) {
            mInputPanel.onDestroy();
            NCMentionManager.getInstance()
                    .destroyInstance(
                            getConversationType(),
                            getTargetId(),
                            mExtensionViewModel.getEditTextWidget());
        }
        for (IExtensionEventWatcher watcher :
                NCExtensionManager.getInstance().getExtensionEventWatcher()) {
            watcher.onDestroy(getConversationType(), getTargetId());
        }
        for (IExtensionModule extensionModule :
                NCExtensionManager.getInstance().getExtensionModules()) {
            extensionModule.onDetachedFromExtension();
        }
    }

    private void insertToEditText(String content) {
        EditText editText = mExtensionViewModel.getEditTextWidget();
        int len = content.length();
        int cursorPos = editText.getSelectionStart();
        editText.getEditableText().insert(cursorPos, content);
        editText.setSelection(cursorPos + len);
    }

    /**
     * Checks whether the EditText properties have changed in text input mode to avoid frequent
     * refreshes.
     *
     * @param editText The input control.
     * @return Whether the properties are the same as before. If true, the caller returns directly
     *     without refreshing.
     */
    private boolean isEditTextSameProperty(EditText editText) {
        if (mPreInputMode == null) {
            return false;
        }
        return (mPreInputMode.equals(InputMode.TextInput)
                && (editText.isFocused() || editText.getText().length() > 0)
                && mExtensionViewModel.isSoftInputShow());
    }

    private Activity getActivityFromView() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    public boolean useKeyboardHeightProvider() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Activity activity = getActivityFromView();
            return activity != null && !activity.isInMultiWindowMode();
        }
        return false;
    }

    public enum ContainerType {
        ATTACH, // Attached info container
        INPUT, // Input bar container
        BOARD, // Extension board container
    }
}
