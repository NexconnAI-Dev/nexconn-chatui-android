package ai.nexconn.chatui.channel;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.event.Event;
import ai.nexconn.chatui.channel.event.page.MessageEvent;
import ai.nexconn.chatui.channel.event.page.PageDestroyEvent;
import ai.nexconn.chatui.channel.event.page.PageEvent;
import ai.nexconn.chatui.channel.event.page.ReadReceiptStateClickEvent;
import ai.nexconn.chatui.channel.event.page.ScrollEvent;
import ai.nexconn.chatui.channel.event.page.ScrollMentionEvent;
import ai.nexconn.chatui.channel.event.page.ScrollToEndEvent;
import ai.nexconn.chatui.channel.event.page.ShowLoadMessageDialogEvent;
import ai.nexconn.chatui.channel.event.page.ShowWarningDialogEvent;
import ai.nexconn.chatui.channel.event.page.SmoothScrollEvent;
import ai.nexconn.chatui.channel.event.page.ToastEvent;
import ai.nexconn.chatui.channel.extension.InputMode;
import ai.nexconn.chatui.channel.extension.NCExtension;
import ai.nexconn.chatui.channel.extension.NCExtensionViewModel;
import ai.nexconn.chatui.channel.feature.expose.ItemExposeManager;
import ai.nexconn.chatui.channel.feature.expose.OnItemExposeListener;
import ai.nexconn.chatui.channel.feature.location.LocationUiRender;
import ai.nexconn.chatui.channel.feature.reference.ReferenceManager;
import ai.nexconn.chatui.channel.longclick.MessageLongClickCopyItem;
import ai.nexconn.chatui.channel.longclick.MessageLongClickDeleteItem;
import ai.nexconn.chatui.channel.longclick.MessageLongClickEditItem;
import ai.nexconn.chatui.channel.longclick.MessageLongClickItem;
import ai.nexconn.chatui.channel.longclick.MessageLongClickMultiSelectItem;
import ai.nexconn.chatui.channel.longclick.MessageLongClickReferenceItem;
import ai.nexconn.chatui.channel.messagelist.MessageProviderPermissionHandler;
import ai.nexconn.chatui.channel.messagelist.processor.IChannelUIRenderer;
import ai.nexconn.chatui.channel.messagelist.provider.MessageClickType;
import ai.nexconn.chatui.channel.messagelist.status.MessageProcessor;
import ai.nexconn.chatui.channel.readreceipt.MessageReadDetailActivity;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.handler.AppSettingsHandler;
import ai.nexconn.chatui.manager.hqvoicemessage.HQVoiceMsgDownloadManager;
import ai.nexconn.chatui.model.TypingInfo;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.notification.ChatUINotificationManager;
import ai.nexconn.chatui.userinfo.NCUserInfoManager;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.permission.PermissionCheckUtil;
import ai.nexconn.chatui.utils.route.RouteUtils;
import ai.nexconn.chatui.utils.view.ChatUIViewUtils;
import ai.nexconn.chatui.widget.FixedLinearLayoutManager;
import ai.nexconn.chatui.widget.TitleBar;
import ai.nexconn.chatui.widget.adapter.BaseAdapter;
import ai.nexconn.chatui.widget.adapter.IViewProviderListener;
import ai.nexconn.chatui.widget.adapter.ViewHolder;
import ai.nexconn.chatui.widget.dialog.MessageLongClickPopup;
import ai.nexconn.chatui.widget.pullrefresh.SmartRefreshLayout;
import ai.nexconn.chatui.widget.pullrefresh.api.RefreshLayout;
import ai.nexconn.chatui.widget.pullrefresh.constant.RefreshState;
import ai.nexconn.chatui.widget.pullrefresh.listener.OnLoadMoreListener;
import ai.nexconn.chatui.widget.pullrefresh.listener.OnRefreshListener;
import ai.nexconn.chatui.widget.pullrefresh.wrapper.NCRefreshHeader;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** */
public class ChannelFragment extends Fragment
        implements OnRefreshListener,
                View.OnClickListener,
                OnLoadMoreListener,
                IViewProviderListener<UiMessage> {
    /** Request code for the combined-forward conversation selection page. */
    public static final int REQUEST_CODE_FORWARD = 104;

    private static final int REQUEST_MSG_DOWNLOAD_PERMISSION = 1000;
    private final String TAG = ChannelFragment.class.getSimpleName();
    protected SmartRefreshLayout mRefreshLayout;
    protected RecyclerView mList;
    protected RecyclerView.LayoutManager mLinearLayoutManager;
    protected MessageListAdapter mAdapter;
    protected ItemExposeManager<UiMessage> mExposeManager;
    protected ChannelViewModel mMessageViewModel;
    protected NCExtensionViewModel mNCExtensionViewModel;
    protected NCExtension mNCExtension;
    protected TextView mNewMessageNum;
    protected TextView mUnreadHistoryMessageNum;
    protected TextView mUnreadMentionMessageNum;
    protected int activitySoftInputMode = 0;
    // Whether to refresh the list when scrolling stops
    protected boolean onScrollStopRefreshList = false;
    private boolean bindToConversation = false;
    private List<MessageLongClickItem> mLongClickItems;
    Observer<List<UiMessage>> mListObserver =
            new Observer<List<UiMessage>>() {
                @Override
                public void onChanged(List<UiMessage> uiMessages) {
                    refreshList(uiMessages);
                }
            };
    Observer<Integer> mNewMessageUnreadObserver =
            new Observer<Integer>() {
                @Override
                public void onChanged(Integer count) {
                    if (NCChatUIConfig.channelConfig()
                            .isShowNewMessageBar(mMessageViewModel.getCurChannelType())) {
                        if (count != null && count > 0) {
                            mNewMessageNum.setVisibility(View.VISIBLE);
                            mNewMessageNum.setText(count > 99 ? "99+" : String.valueOf(count));
                        } else {
                            mNewMessageNum.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            };
    Observer<Integer> mHistoryMessageUnreadObserver =
            new Observer<Integer>() {
                @Override
                public void onChanged(Integer count) {
                    if (NCChatUIConfig.channelConfig()
                            .isShowHistoryMessageBar(mMessageViewModel.getCurChannelType())) {
                        if (count != null && count > 0) {
                            mUnreadHistoryMessageNum.setVisibility(View.VISIBLE);
                            mUnreadHistoryMessageNum.setText(
                                    MessageFormat.format(
                                            getString(R.string.nc_unread_message),
                                            count > 99 ? "99+" : count));
                            mUnreadHistoryMessageNum.setTextColor(
                                    getResources()
                                            .getColor(
                                                    ChatUIThemeManager.getAttrResId(
                                                            mUnreadHistoryMessageNum.getContext(),
                                                            R.attr.nc_primary_color)));
                        } else {
                            mUnreadHistoryMessageNum.setVisibility(View.GONE);
                        }
                    }
                }
            };
    Observer<Integer> mNewMentionMessageUnreadObserver =
            new Observer<Integer>() {
                @Override
                public void onChanged(Integer count) {
                    if (NCChatUIConfig.channelConfig()
                            .isShowNewMentionMessageBar(mMessageViewModel.getCurChannelType())) {
                        if (count != null && count > 0) {
                            mUnreadMentionMessageNum.setVisibility(View.VISIBLE);
                            mUnreadMentionMessageNum.setTextColor(
                                    getResources()
                                            .getColor(
                                                    ChatUIThemeManager.getAttrResId(
                                                            mUnreadMentionMessageNum.getContext(),
                                                            R.attr.nc_hint_color)));
                            mUnreadMentionMessageNum.setText(
                                    getString(R.string.nc_mention_messages, "(" + count + ")"));
                        } else {
                            mUnreadMentionMessageNum.setVisibility(View.GONE);
                        }
                    }
                }
            };
    Observer<PageEvent> mPageObserver =
            new Observer<PageEvent>() {
                @Override
                public void onChanged(PageEvent event) {
                    // Dispatch to each module's view processor first; if it returns true the event
                    // is consumed.
                    for (IChannelUIRenderer processor :
                            NCChatUIConfig.channelConfig().getViewProcessors()) {
                        if (processor.handlePageEvent(event)) {
                            return;
                        }
                    }
                    if (event instanceof ReadReceiptStateClickEvent) {
                        // Launch read-receipt detail page
                        if (getActivity() != null) {
                            UiMessage uiMessage = ((ReadReceiptStateClickEvent) event).getMessage();
                            ai.nexconn.chat.message.Message ncMessage = uiMessage.getMessage();
                            if (ncMessage != null) {
                                getActivity()
                                        .startActivity(
                                                MessageReadDetailActivity.newIntent(
                                                        getActivity(),
                                                        ncMessage,
                                                        uiMessage.getReadReceiptInfo()));
                            }
                        }
                    } else if (event instanceof MessageEvent) {
                        noMoreMessageToFetch();
                    } else if (event instanceof Event.RefreshEvent) {
                        if (((Event.RefreshEvent) event).state.equals(RefreshState.RefreshFinish)) {
                            mRefreshLayout.finishRefresh();
                        } else if (((Event.RefreshEvent) event)
                                .state.equals(RefreshState.LoadFinish)) {
                            mRefreshLayout.finishLoadMore();
                        }
                    } else if (event instanceof ToastEvent) {
                        String msg = ((ToastEvent) event).getMessage();
                        if (!TextUtils.isEmpty(msg)) {
                            ToastUtils.show(getContext(), msg, Toast.LENGTH_SHORT);
                        }
                    } else if (event instanceof ScrollToEndEvent) {
                        mList.scrollToPosition(mAdapter.getItemCount() - 1);
                    } else if (event instanceof ScrollMentionEvent) {
                        mMessageViewModel.onScrolled(
                                mList,
                                0,
                                0,
                                mAdapter.getHeadersCount(),
                                mAdapter.getFootersCount());
                    } else if (event instanceof ScrollEvent) {
                        if (mList.getLayoutManager() instanceof LinearLayoutManager) {
                            int positionScrollEvent = ((ScrollEvent) event).getPosition();
                            // In History mode the index needs to be decremented by 1, otherwise the
                            // target message is not visible.
                            int scrollPosition =
                                    mMessageViewModel.isHistoryState()
                                            ? Math.max(positionScrollEvent - 1, 0)
                                            : Math.max(positionScrollEvent, 0);
                            mList.postDelayed(
                                    () ->
                                            ((LinearLayoutManager) mList.getLayoutManager())
                                                    .scrollToPositionWithOffset(
                                                            mAdapter.getHeadersCount()
                                                                    + scrollPosition,
                                                            0),
                                    150);
                        }
                    } else if (event instanceof SmoothScrollEvent) {
                        if (mList.getLayoutManager() instanceof LinearLayoutManager) {
                            ((LinearLayoutManager) mList.getLayoutManager())
                                    .scrollToPositionWithOffset(
                                            mAdapter.getHeadersCount()
                                                    + ((SmoothScrollEvent) event).getPosition(),
                                            0);
                        }
                    } else if (event instanceof PageDestroyEvent) {
                        FragmentManager fm = getChildFragmentManager();
                        if (fm.getBackStackEntryCount() > 0) {
                            fm.popBackStack();
                        } else {
                            if (getActivity() != null) {
                                getActivity().finish();
                            }
                        }
                    } else if (event instanceof ShowWarningDialogEvent) {
                        onWarningDialog(((ShowWarningDialogEvent) event).getMessage());
                    } else if (event instanceof ShowLoadMessageDialogEvent) {
                        showLoadMessageDialog(
                                ((ShowLoadMessageDialogEvent) event).getCallback(),
                                ((ShowLoadMessageDialogEvent) event).getList());
                    }
                }
            };
    private LinearLayout mNotificationContainer;
    private boolean onViewCreated = false;
    private boolean mDisableSystemEmoji;
    private Bundle mBundle;
    private ChannelIdentifier conversationIdentifier;
    private final RecyclerView.OnScrollListener mScrollListener =
            new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    mMessageViewModel.onScrolled(
                            recyclerView,
                            dx,
                            dy,
                            mAdapter.getHeadersCount(),
                            mAdapter.getFootersCount());
                }

                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE && onScrollStopRefreshList) {
                        onScrollStopRefreshList = false;
                        RLog.d(TAG, "onScrollStateChanged refresh List");
                        refreshList(mMessageViewModel.getUiMessageLiveData().getValue());
                    }
                }
            };
    private final OnItemExposeListener<UiMessage> mOnItemExposeListener =
            (visible, position, data) -> mMessageViewModel.onItemViewVisible(visible, data);

    {
        mAdapter = onResolveAdapter();
    }

    public void initConversation(String targetId, ChannelType channelType, Bundle bundle) {
        ChannelIdentifier identifier = new ChannelIdentifier(channelType, targetId);
        if (onViewCreated) {
            bindConversation(identifier, false, bundle);
        } else {
            conversationIdentifier = identifier;
            mBundle = bundle;
        }
    }

    private void bindConversation(
            ChannelIdentifier identifier, boolean disableSystemEmoji, Bundle bundle) {
        if (identifier.getChannelType() != null && !TextUtils.isEmpty(identifier.getChannelId())) {
            ChannelType bindChannelType = identifier.getChannelType();
            for (IChannelUIRenderer processor :
                    NCChatUIConfig.channelConfig().getViewProcessors()) {
                processor.init(this, mNCExtension, bindChannelType, identifier.getChannelId());
            }
            mNCExtension.bindToConversation(this, identifier, disableSystemEmoji);
            mMessageViewModel.bindConversation(identifier, bundle);
            subscribeUi();
            bindToConversation = true;
        } else {
            RLog.e(
                    TAG,
                    "Invalid intent data !!! Must put targetId and conversation type to intent.");
        }
    }

    private void subscribeUi() {
        mMessageViewModel.getPageEventLiveData().observeForever(mPageObserver);
        mMessageViewModel.getUiMessageLiveData().observeForever(mListObserver);
        mMessageViewModel
                .getNewMessageUnreadLiveData()
                .observe(getViewLifecycleOwner(), mNewMessageUnreadObserver);
        mMessageViewModel
                .getHistoryMessageUnreadLiveData()
                .observe(getViewLifecycleOwner(), mHistoryMessageUnreadObserver);
        mMessageViewModel
                .getNewMentionMessageUnreadLiveData()
                .observe(getViewLifecycleOwner(), mNewMentionMessageUnreadObserver);
        mNCExtensionViewModel
                .getExtensionBoardState()
                .observe(
                        getViewLifecycleOwner(),
                        new Observer<Boolean>() {
                            @Override
                            public void onChanged(final Boolean value) {
                                RLog.d(TAG, "scroll to the bottom");
                                mList.postDelayed(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                InputMode inputMode =
                                                        mNCExtensionViewModel
                                                                .getInputModeLiveData()
                                                                .getValue();
                                                if (!Objects.equals(
                                                                inputMode, InputMode.MoreInputMode)
                                                        && Boolean.TRUE.equals(value)) {
                                                    if (mMessageViewModel.isNormalState()) {
                                                        mList.scrollToPosition(
                                                                mAdapter.getItemCount() - 1);
                                                    } else {
                                                        mMessageViewModel.newMessageBarClick();
                                                    }
                                                }
                                            }
                                        },
                                        150);
                            }
                        });
    }

    @Override
    public void onViewClick(int clickType, UiMessage data) {
        if (MessageProviderPermissionHandler.getInstance()
                .handleMessageClickPermission(data, this)) {
            return;
        }

        mMessageViewModel.onViewClick(clickType, data);
    }

    @Override
    public boolean onViewLongClick(View view, int clickType, UiMessage data) {
        if (clickType == MessageClickType.CONTENT_LONG_CLICK) {
            return handleLongClick(view, data);
        }
        return mMessageViewModel.onViewLongClick(clickType, data);
    }

    @Override
    public boolean onViewLongClick(int clickType, UiMessage data) {
        return onViewLongClick(null, clickType, data);
    }

    private boolean handleLongClick(View anchorView, UiMessage message) {
        if (mLongClickItems == null || message == null) return false;
        List<MessageLongClickItem> visible = new ArrayList<>();
        for (MessageLongClickItem item : mLongClickItems) {
            if (item.isEnabled(message)) {
                visible.add(item);
            }
        }
        if (visible.isEmpty()) return false;
        showLongClickPopup(anchorView, message, visible);
        return true;
    }

    private void showLongClickPopup(
            View anchorView, UiMessage message, List<MessageLongClickItem> items) {
        List<MessageLongClickPopup.OptionItem> optionItems = new ArrayList<>();
        for (MessageLongClickItem item : items) {
            optionItems.add(
                    new MessageLongClickPopup.OptionItem(
                            item.getTitle(requireContext(), message), item.getIconAttrResId()));
        }
        MessageLongClickPopup popup =
                MessageLongClickPopup.newInstance(getContext(), optionItems)
                        .setOptionsPopupDialogListener(
                                which -> items.get(which).onAction(requireContext(), message));
        if (anchorView != null) {
            popup.setAnchorView(anchorView);
        }
        popup.show();
    }

    /**
     * Builds the list of long-press menu items. The List order determines menu display order.
     *
     * <p>Subclasses can:
     *
     * <pre>
     * // a) Add/remove items on top of the defaults
     * List&lt;MessageLongClickItem&gt; items = super.onSetupLongClickItems();
     * items.add(new MyItem());
     * return items;
     *
     * // b) Fully customize
     * List&lt;MessageLongClickItem&gt; items = new ArrayList&lt;&gt;();
     * items.add(new MessageLongClickCopyItem());
     * return items;
     * </pre>
     */
    protected List<MessageLongClickItem> onSetupLongClickItems() {
        List<MessageLongClickItem> items = new ArrayList<>();
        items.add(new MessageLongClickCopyItem());
        items.add(new MessageLongClickDeleteItem());
        items.add(new MessageLongClickEditItem());
        items.add(new MessageLongClickReferenceItem());
        items.add(new MessageLongClickMultiSelectItem(mMessageViewModel));
        return items;
    }

    /**
     * Returns the top notification bar container.
     *
     * @return the notification container
     */
    public LinearLayout getNotificationContainer() {
        return mNotificationContainer;
    }

    /**
     * Hides the notification view previously shown by {@link #showNotificationView(View)}.
     *
     * @param notificationView the notification view to hide
     */
    public void hideNotificationView(View notificationView) {
        if (notificationView == null) {
            return;
        }
        View view = mNotificationContainer.findViewById(notificationView.getId());
        if (view != null) {
            mNotificationContainer.removeView(view);
            if (mNotificationContainer.getChildCount() == 0) {
                mNotificationContainer.setVisibility(View.GONE);
            }
        }
    }

    /** Displays a view in the notification area. */
    public void showNotificationView(View notificationView) {
        if (notificationView == null) {
            return;
        }
        mNotificationContainer.removeAllViews();
        ChatUIViewUtils.addView(mNotificationContainer, notificationView);
        mNotificationContainer.setVisibility(View.VISIBLE);
    }

    private void refreshList(final List<UiMessage> data) {
        if (!mList.isComputingLayout()
                && mList.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
            mAdapter.setDataCollection(data);
        } else {
            onScrollStopRefreshList = true;
        }
    }

    public boolean onBackPressed() {
        boolean result = false;
        for (IChannelUIRenderer processor : NCChatUIConfig.channelConfig().getViewProcessors()) {
            boolean temp = processor.onBackPressed();
            if (temp) {
                result = true;
            }
        }
        if (mMessageViewModel != null) {
            boolean temp = mMessageViewModel.onBackPressed();
            if (temp) {
                result = true;
            }
        }
        if (mNCExtensionViewModel != null) {
            mNCExtensionViewModel.exitMoreInputMode(getContext());
            mNCExtensionViewModel.collapseExtensionBoard();
        }
        return result;
    }

    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {
        if (mMessageViewModel != null && bindToConversation) {
            mMessageViewModel.onRefresh();
        }
    }

    public NCExtension getNCExtension() {
        return mNCExtension;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            ReferenceManager.getInstance().hideReferenceView();
        }
        if (requestCode == REQUEST_CODE_FORWARD) {
            if (mMessageViewModel != null) mMessageViewModel.forwardMessage(data);
            return;
        }
        if (mNCExtension != null) {
            mNCExtension.onActivityPluginResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (PermissionCheckUtil.checkPermissionResultIncompatible(permissions, grantResults)) {
            if (getContext() != null) {
                ToastUtils.show(
                        getContext(),
                        getString(R.string.nc_permission_request_failed),
                        Toast.LENGTH_SHORT);
            }
            return;
        }

        if (requestCode == REQUEST_MSG_DOWNLOAD_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                HQVoiceMsgDownloadManager.getInstance().resumeDownloadService();
            } else {
                PermissionCheckUtil.showRequestPermissionFailedAlter(
                        this.getContext(), permissions, grantResults);
            }
            return;
        } else if (requestCode == PermissionCheckUtil.REQUEST_CODE_LOCATION_SHARE) {
            if (PermissionCheckUtil.checkPermissions(getActivity(), permissions)) {
                LocationUiRender locationUiRender = null;
                for (IChannelUIRenderer processor :
                        NCChatUIConfig.channelConfig().getViewProcessors()) {
                    if (processor instanceof LocationUiRender) {
                        locationUiRender = (LocationUiRender) processor;
                        break;
                    }
                }

                if (locationUiRender != null) {
                    locationUiRender.joinLocation();
                }
            } else {
                if (getActivity() != null) {
                    PermissionCheckUtil.showRequestPermissionFailedAlter(
                            getActivity(), permissions, grantResults);
                }
            }
        } else if (requestCode
                == MessageProviderPermissionHandler.REQUEST_CODE_ITEM_PROVIDER_PERMISSIONS) {
            MessageProviderPermissionHandler.getInstance()
                    .onRequestPermissionsResult(getActivity(), permissions, grantResults);
        }

        if (requestCode == PermissionCheckUtil.REQUEST_CODE_ASK_PERMISSIONS
                && grantResults.length > 0
                && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            PermissionCheckUtil.showRequestPermissionFailedAlter(
                    this.getContext(), permissions, grantResults);
        } else {
            mNCExtension.onRequestPermissionResult(requestCode, permissions, grantResults);
        }
    }

    /** Find views by ID and bindlisteners. */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.nc_conversation_fragment, container, false);
        mList = rootView.findViewById(R.id.nc_message_list);
        mNCExtension = rootView.findViewById(R.id.nc_extension);
        mRefreshLayout = rootView.findViewById(R.id.nc_refresh);
        mNewMessageNum = rootView.findViewById(R.id.nc_new_message_number);
        mUnreadHistoryMessageNum = rootView.findViewById(R.id.nc_unread_message_count);
        mUnreadMentionMessageNum = rootView.findViewById(R.id.nc_mention_message_count);
        mNotificationContainer = rootView.findViewById(R.id.nc_notification_container);
        mNewMessageNum.setOnClickListener(this);
        mUnreadHistoryMessageNum.setOnClickListener(this);
        mUnreadMentionMessageNum.setOnClickListener(this);
        mLinearLayoutManager = createLayoutManager();
        if (mList != null) {
            mList.setLayoutManager(mLinearLayoutManager);
        }
        mRefreshLayout.setOnTouchListener(
                new View.OnTouchListener() {
                    @SuppressLint("ClickableViewAccessibility")
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        closeExpand();
                        return false;
                    }
                });
        mAdapter.setItemClickListener(
                new BaseAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, ViewHolder holder, int position) {
                        closeExpand();
                    }

                    @Override
                    public boolean onItemLongClick(View view, ViewHolder holder, int position) {
                        return false;
                    }
                });
        // Disable item animations
        if (mList != null) {
            mList.setAdapter(mAdapter);
            mList.addOnScrollListener(mScrollListener);
            mList.setItemAnimator(null);
            final GestureDetector gd =
                    new GestureDetector(
                            getContext(),
                            new GestureDetector.SimpleOnGestureListener() {
                                @Override
                                public boolean onScroll(
                                        MotionEvent e1,
                                        MotionEvent e2,
                                        float distanceX,
                                        float distanceY) {
                                    closeExpand();
                                    return super.onScroll(e1, e2, distanceX, distanceY);
                                }
                            });
            mList.addOnItemTouchListener(
                    new RecyclerView.OnItemTouchListener() {
                        @Override
                        public boolean onInterceptTouchEvent(
                                @NonNull RecyclerView rv, @NonNull MotionEvent e) {
                            return gd.onTouchEvent(e);
                        }

                        @Override
                        public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                            // Do nothing
                        }

                        @Override
                        public void onRequestDisallowInterceptTouchEvent(
                                boolean disallowIntercept) {
                            // Do nothing
                        }
                    });
        }

        mRefreshLayout.setNestedScrollingEnabled(false);
        mRefreshLayout.setRefreshHeader(new NCRefreshHeader(getContext()));
        mRefreshLayout.setRefreshFooter(new NCRefreshHeader(getContext()));
        mRefreshLayout.setEnableRefresh(true);
        mRefreshLayout.setOnRefreshListener(this);
        mRefreshLayout.setOnLoadMoreListener(this);
        mExposeManager = new ItemExposeManager<>();
        mExposeManager.attach(mList, mAdapter, mOnItemExposeListener);
        return rootView;
    }

    private RecyclerView.LayoutManager createLayoutManager() {
        LinearLayoutManager linearLayoutManager = new FixedLinearLayoutManager(getContext());
        linearLayoutManager.setStackFromEnd(true);
        return linearLayoutManager;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        Intent intent = getActivity() != null ? getActivity().getIntent() : null;
        boolean hasArgs =
                args != null
                        && (args.containsKey(RouteUtils.CHANNEL_TYPE)
                                || args.containsKey(RouteUtils.CHANNEL_IDENTIFIER));
        boolean hasIntent =
                intent != null
                        && (intent.hasExtra(RouteUtils.CHANNEL_TYPE)
                                || intent.hasExtra(RouteUtils.CHANNEL_IDENTIFIER));
        if (getActivity() == null || (!hasArgs && !hasIntent)) {
            RLog.e(
                    TAG,
                    "Must put targetId and conversation type to intent or arguments when start conversation.");
            return;
        }
        if (!ai.nexconn.chatui.NCChatUI.isInitialized()) {
            RLog.e(TAG, "Please init SDK first!");
            return;
        }
        super.onViewCreated(view, savedInstanceState);
        initIntentExtra();
        if (ChannelType.SYSTEM.equals(getChannelType())) {
            mNCExtension.setVisibility(View.GONE);
        } else {
            mNCExtension.setVisibility(View.VISIBLE);
        }
        mMessageViewModel = new ViewModelProvider(this).get(ChannelViewModel.class);
        mNCExtensionViewModel = new ViewModelProvider(this).get(NCExtensionViewModel.class);
        mLongClickItems = onSetupLongClickItems();
        bindConversation(conversationIdentifier, mDisableSystemEmoji, mBundle);

        if (getArguments() != null) {
            initConversationTitleBar(view);
        }

        // NOTE: 2021/8/25  HD voice auto-download no longer requires storage permission; removed.
        onViewCreated = true;
    }

    private NCUserInfoManager.UserDataObserver mUserDataObserver;

    private void initConversationTitleBar(View view) {
        TitleBar titleBar = view.findViewById(R.id.nc_conversation_title_bar);
        if (titleBar == null || conversationIdentifier == null) return;
        titleBar.setVisibility(View.VISIBLE);
        titleBar.getRightView().setVisibility(View.GONE);
        updateConversationTitleBar(titleBar);
        titleBar.setOnBackClickListener(
                new TitleBar.OnBackClickListener() {
                    @Override
                    public void onBackClick() {
                        if (onBackPressed()) return;
                        if (getActivity() != null) getActivity().finish();
                    }
                });

        String targetId = conversationIdentifier.getChannelId();
        if (!TextUtils.isEmpty(targetId)) {
            mUserDataObserver =
                    new NCUserInfoManager.UserDataObserver() {
                        @Override
                        public void onUserUpdate(UserInfo info) {
                            if (TextUtils.equals(targetId, info.getUserId())) {
                                if (getActivity() != null) {
                                    getActivity()
                                            .runOnUiThread(
                                                    () -> updateConversationTitleBar(titleBar));
                                }
                            }
                        }

                        @Override
                        public void onGroupUpdate(GroupInfo group) {
                            if (TextUtils.equals(targetId, group.getGroupId())) {
                                if (getActivity() != null) {
                                    getActivity()
                                            .runOnUiThread(
                                                    () -> updateConversationTitleBar(titleBar));
                                }
                            }
                        }

                        @Override
                        public void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo) {}
                    };
            NCUserInfoManager.getInstance().addUserDataObserver(mUserDataObserver);
        }

        ChannelType channelType = conversationIdentifier.getChannelType();
        mMessageViewModel
                .getTypingStatusInfo()
                .observe(
                        getViewLifecycleOwner(),
                        typingInfo -> {
                            if (typingInfo == null) return;
                            if (!Objects.equals(
                                            conversationIdentifier.getChannelType(),
                                            typingInfo.conversationType)
                                    || !targetId.equals(typingInfo.targetId)) return;
                            if (typingInfo.typingList == null) {
                                titleBar.getMiddleView().setVisibility(View.VISIBLE);
                                titleBar.getTypingView().setVisibility(View.GONE);
                                setTitleOnlineStatus(titleBar, mMessageViewModel.isOnlineStatus());
                            } else {
                                // Keep title slot for constraint positioning; typing text aligns
                                // with title.
                                titleBar.getMiddleView().setVisibility(View.INVISIBLE);
                                titleBar.getTypingView().setVisibility(View.VISIBLE);
                                TypingInfo.TypingUserInfo typing =
                                        typingInfo.typingList.get(typingInfo.typingList.size() - 1);
                                if (typing.type == TypingInfo.TypingUserInfo.Type.text) {
                                    titleBar.setTyping(
                                            R.string.nc_conversation_remote_side_is_typing);
                                } else if (typing.type == TypingInfo.TypingUserInfo.Type.voice) {
                                    titleBar.setTyping(
                                            R.string.nc_conversation_remote_side_speaking);
                                }
                                setTitleOnlineStatus(titleBar, mMessageViewModel.isOnlineStatus());
                            }
                        });
        if (ChannelType.DIRECT.equals(getChannelType())
                && !TextUtils.isEmpty(targetId)
                && !TextUtils.equals(targetId, NCEngine.getCurrentUserId())) {
            setTitleOnlineStatus(titleBar, false);
            mMessageViewModel
                    .getOnlineStatus()
                    .observe(
                            getViewLifecycleOwner(),
                            status -> {
                                setTitleOnlineStatus(titleBar, status != null && status.isOnline());
                            });
            mMessageViewModel.getUserOnlineStatus(targetId);
        }
        mMessageViewModel.getNotify().observe(getViewLifecycleOwner(), aBoolean -> {});
        mMessageViewModel.getNotificationStatus(channelType, targetId);
    }

    private void updateConversationTitleBar(TitleBar titleBar) {
        if (titleBar == null || conversationIdentifier == null) return;
        String targetId = conversationIdentifier.getChannelId();
        if (TextUtils.isEmpty(targetId)) return;
        if (ChannelType.GROUP.equals(getChannelType())) {
            GroupInfo group = NCUserInfoManager.getInstance().getGroupInfo(targetId);
            titleBar.setTitle(group == null ? targetId : group.getGroupName());
        } else {
            UserInfo userInfo = NCUserInfoManager.getInstance().getUserInfo(targetId);
            titleBar.setTitle(
                    userInfo == null
                            ? targetId
                            : TextUtils.isEmpty(userInfo.getAlias())
                                    ? userInfo.getName()
                                    : userInfo.getAlias());
        }
        if (ChannelType.OPEN.equals(getChannelType())) {
            titleBar.setRightVisible(false);
        }
    }

    private ChannelType getChannelType() {
        if (conversationIdentifier == null) return null;
        return conversationIdentifier.getChannelType();
    }

    private void setTitleOnlineStatus(TitleBar titleBar, boolean isOnline) {
        if (titleBar == null) return;
        if (titleBar.getMiddleLeftIcon() == null || titleBar.getMiddleRightIcon() == null) return;
        if (!AppSettingsHandler.getInstance().isOnlineStatusEnable()
                || !shouldDisplayOnlineStatus()) {
            titleBar.getMiddleLeftIcon().setVisibility(View.GONE);
            titleBar.getMiddleRightIcon().setVisibility(View.GONE);
            return;
        }
        int resId =
                ChatUIThemeManager.getAttrResId(
                        requireContext(),
                        isOnline
                                ? R.attr.nc_user_online_status_img
                                : R.attr.nc_user_offline_status_img);
        titleBar.getMiddleRightIcon().setVisibility(View.GONE);
        titleBar.getMiddleLeftIcon().setVisibility(View.VISIBLE);
        titleBar.getMiddleLeftIcon().setImageResource(resId);
    }

    private boolean shouldDisplayOnlineStatus() {
        if (conversationIdentifier == null) return false;
        if (!ChannelType.DIRECT.equals(conversationIdentifier.getChannelType())) return false;
        String targetId = conversationIdentifier.getChannelId();
        return !TextUtils.isEmpty(targetId)
                && !TextUtils.equals(targetId, NCEngine.getCurrentUserId());
    }

    private void initIntentExtra() {
        Bundle args = getArguments();
        Intent intent = getActivity() != null ? getActivity().getIntent() : null;
        // Read from arguments (injected by ChatUIFragmentFactory) first, then fall back to intent
        if (args != null && args.containsKey(RouteUtils.CHANNEL_IDENTIFIER)) {
            ChannelIdentifier identifier = args.getParcelable(RouteUtils.CHANNEL_IDENTIFIER);
            if (identifier != null) {
                conversationIdentifier = identifier;
            }
        }
        if (conversationIdentifier == null
                && intent != null
                && intent.hasExtra(RouteUtils.CHANNEL_IDENTIFIER)) {
            ChannelIdentifier identifier = intent.getParcelableExtra(RouteUtils.CHANNEL_IDENTIFIER);
            if (identifier != null) {
                conversationIdentifier = identifier;
            }
        }
        if (conversationIdentifier == null) {
            String typeValue = null;
            String targetId = null;
            if (args != null) {
                typeValue = args.getString(RouteUtils.CHANNEL_TYPE);
                targetId = args.getString(RouteUtils.TARGET_ID);
            }
            if ((typeValue == null || targetId == null) && intent != null) {
                if (typeValue == null) typeValue = intent.getStringExtra(RouteUtils.CHANNEL_TYPE);
                if (targetId == null) targetId = intent.getStringExtra(RouteUtils.TARGET_ID);
            }
            ChannelType type = null;
            if (!TextUtils.isEmpty(typeValue)) {
                try {
                    type = ChannelType.valueOf(typeValue.toUpperCase(Locale.US));
                } catch (IllegalArgumentException ignored) {
                }
            }
            conversationIdentifier = new ChannelIdentifier(type, targetId != null ? targetId : "");
        }

        if (args != null) {
            mDisableSystemEmoji = args.getBoolean(RouteUtils.DISABLE_SYSTEM_EMOJI, false);
            if (mBundle == null) mBundle = args;
        }
        if (intent != null) {
            if (!mDisableSystemEmoji)
                mDisableSystemEmoji =
                        intent.getBooleanExtra(RouteUtils.DISABLE_SYSTEM_EMOJI, false);
            if (mBundle == null) mBundle = intent.getExtras();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() == null) {
            return;
        }
        if (mMessageViewModel != null) mMessageViewModel.onResume();
        ChatUINotificationManager.getInstance().onChannelPageResumed(conversationIdentifier);
        getView()
                .setOnKeyListener(
                        new View.OnKeyListener() {
                            @Override
                            public boolean onKey(View v, int keyCode, KeyEvent event) {
                                if (event.getAction() == KeyEvent.ACTION_UP
                                        && keyCode == KeyEvent.KEYCODE_BACK) {
                                    return onBackPressed();
                                }
                                return false;
                            }
                        });
        mNCExtension.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        ChatUINotificationManager.getInstance().onChannelPagePaused(conversationIdentifier);
        if (mMessageViewModel != null) mMessageViewModel.onPause();
        if (mNCExtension != null) mNCExtension.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Save the activity's original softInputMode
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activitySoftInputMode = activity.getWindow().getAttributes().softInputMode;
            if (mNCExtension != null && mNCExtension.useKeyboardHeightProvider()) {
                resetSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
            } else {
                resetSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mMessageViewModel != null) mMessageViewModel.onStop();
        resetSoftInputMode(activitySoftInputMode);
    }

    @Override
    public void onDestroyView() {
        if (mUserDataObserver != null
                && conversationIdentifier != null
                && !TextUtils.isEmpty(conversationIdentifier.getChannelId())) {
            NCUserInfoManager.getInstance().removeUserDataObserver(mUserDataObserver);
            mUserDataObserver = null;
        }
        super.onDestroyView();
        for (IChannelUIRenderer processor : NCChatUIConfig.channelConfig().getViewProcessors()) {
            processor.onDestroy();
        }
        mList.removeOnScrollListener(mScrollListener);
        if (mExposeManager != null) {
            mExposeManager.release();
            mExposeManager = null;
        }

        if (mMessageViewModel != null) {
            mMessageViewModel.getPageEventLiveData().removeObserver(mPageObserver);
            mMessageViewModel.getUiMessageLiveData().removeObserver(mListObserver);
            mMessageViewModel
                    .getNewMentionMessageUnreadLiveData()
                    .removeObserver(mNewMentionMessageUnreadObserver);
            mMessageViewModel.onDestroy();
        }

        if (mNCExtension != null) {
            mNCExtension.onDestroy();
            mNCExtension = null;
        }
        bindToConversation = false;
    }

    private void resetSoftInputMode(int mode) {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.getWindow().setSoftInputMode(mode);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.nc_new_message_number) {
            if (mMessageViewModel != null) mMessageViewModel.newMessageBarClick();
        } else if (id == R.id.nc_unread_message_count) {
            if (mMessageViewModel != null) mMessageViewModel.unreadBarClick();
        } else if (id == R.id.nc_mention_message_count) {
            if (mMessageViewModel != null) mMessageViewModel.newMentionMessageBarClick();
        }
    }

    @Override
    public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
        if (mMessageViewModel != null && bindToConversation) {
            mMessageViewModel.onLoadMore();
        }
    }

    /**
     * Warning dialog, e.g. "Failed to join chatroom". To customize: 1. Create a subclass of
     * ChannelFragment. 2. Override onWarningDialog.
     *
     * @param msg the dialog message
     */
    public void onWarningDialog(String msg) {
        if (getActivity() == null) return;
        new AlertDialog.Builder(getActivity())
                .setCancelable(false)
                .setMessage(msg)
                .setPositiveButton(
                        R.string.nc_dialog_ok,
                        (dialog, which) -> {
                            dialog.dismiss();
                            if (!isAdded()) return;
                            FragmentManager fm = getChildFragmentManager();
                            if (fm.getBackStackEntryCount() > 0) {
                                fm.popBackStack();
                            } else if (getActivity() != null) {
                                getActivity().finish();
                            }
                        })
                .show();
    }

    private void showLoadMessageDialog(
            final MessageProcessor.GetMessageCallback callback,
            final List<ai.nexconn.chat.message.Message> list) {
        new AlertDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setMessage(getString(R.string.nc_load_local_message))
                .setPositiveButton(
                        getString(R.string.nc_dialog_ok),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (callback != null) {
                                    callback.onSuccess(list, true);
                                }
                            }
                        })
                .setNegativeButton(
                        getString(R.string.nc_cancel),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (callback != null) {
                                    callback.onErrorAsk(list);
                                }
                            }
                        })
                .show();
    }

    private void closeExpand() {
        if (mNCExtensionViewModel != null) {
            mNCExtensionViewModel.collapseExtensionBoard();
        }
    }

    /**
     * Called when message fetching completes and there are no more messages to fetch.
     *
     * @since 5.8.2
     */
    protected void noMoreMessageToFetch() {}

    /**
     * Returns the adapter. Override this method to provide a custom adapter.
     *
     * @return the message list adapter
     */
    protected MessageListAdapter onResolveAdapter() {
        return new MessageListAdapter(this);
    }

    /**
     * @param view custom list header view
     */
    public void addHeaderView(View view) {
        mAdapter.addHeaderView(view);
    }

    /**
     * @param view custom list footer view
     */
    public void addFooterView(View view) {
        mAdapter.addFootView(view);
    }

    /**
     * @param view custom empty-state view for the list
     */
    public void setEmptyView(View view) {
        mAdapter.setEmptyView(view);
    }

    /**
     * @param emptyId layout resource ID for the custom empty-state view
     */
    public void setEmptyView(@LayoutRes int emptyId) {
        mAdapter.setEmptyView(emptyId);
    }
}
