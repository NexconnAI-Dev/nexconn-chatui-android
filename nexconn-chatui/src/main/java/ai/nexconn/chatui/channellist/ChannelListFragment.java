package ai.nexconn.chatui.channellist;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.base.interfaces.OnPagedDataLoader;
import ai.nexconn.chatui.channellist.model.BaseUiChannel;
import ai.nexconn.chatui.channellist.provider.ConversationListProvider;
import ai.nexconn.chatui.config.ChannelListBehaviorListener;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.NoticeContent;
import ai.nexconn.chatui.notification.ChatUINotificationManager;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.route.RouteUtils;
import ai.nexconn.chatui.widget.FixedLinearLayoutManager;
import ai.nexconn.chatui.widget.adapter.ViewHolder;
import ai.nexconn.chatui.widget.dialog.ConversationLongClickPopup;
import ai.nexconn.chatui.widget.pullrefresh.SmartRefreshLayout;
import ai.nexconn.chatui.widget.pullrefresh.api.RefreshLayout;
import ai.nexconn.chatui.widget.pullrefresh.listener.OnRefreshListener;
import ai.nexconn.chatui.widget.pullrefresh.wrapper.NCRefreshHeader;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

/**
 * Channel list Fragment
 *
 * <p>Override entry points for subclasses:
 *
 * <ul>
 *   <li>{@link #onCreateConversationListProvider()} — customize item rendering
 *   <li>{@link #onCreateChannelListAdapter()} — customize Adapter
 *   <li>{@link #onCreateViewModel(Bundle)} — customize ViewModel
 *   <li>{@link #subscribeUi(ChannelListViewModel)} — customize data binding
 *   <li>{@link #initRefreshView()} — customize refresh behavior
 *   <li>{@link #onItemClick(View, ViewHolder, int)} — customize click handling
 *   <li>{@link #onItemLongClick(View, ViewHolder, int)} — customize long-press handling
 *   <li>{@link #showConversationLongClickMenu(View, BaseUiChannel)} — customize long-press menu
 * </ul>
 *
 * @since 5.10.4
 */
public class ChannelListFragment extends BaseViewModelFragment<ChannelListViewModel> {

    private final String TAG = ChannelListFragment.class.getSimpleName();

    protected ChannelListAdapter mAdapter;
    protected RecyclerView mList;
    protected View mNoticeContainerView;
    protected TextView mNoticeContentTv;
    protected ImageView mNoticeIconIv;
    protected SmartRefreshLayout mRefreshLayout;

    private int mScrollState = RecyclerView.SCROLL_STATE_IDLE;
    private boolean pendingRefresh = false;
    private boolean isUserTouchingList = false;
    private LinearLayoutManager layoutManager;

    // region ViewModel / Provider / Adapter creation entry points

    @NonNull
    @Override
    protected ChannelListViewModel onCreateViewModel(@NonNull Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(ChannelListViewModel.class);
    }

    /** Creates the Provider; subclasses may override to customize item rendering. */
    @NonNull
    protected ConversationListProvider onCreateConversationListProvider() {
        return new ConversationListProvider();
    }

    /** Creates the Adapter; subclasses may override to customize adapter behavior. */
    @NonNull
    protected ChannelListAdapter onCreateChannelListAdapter() {
        ChannelListAdapter adapter = new ChannelListAdapter(onCreateConversationListProvider());
        adapter.setEmptyView(R.layout.nc_conversationlist_empty_view);
        return adapter;
    }

    // endregion

    // region Lifecycle

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.nc_conversationlist_fragment, container, false);
        mList = view.findViewById(R.id.nc_conversation_list);
        mRefreshLayout = view.findViewById(R.id.nc_refresh);
        mNoticeContainerView = view.findViewById(R.id.nc_conversationlist_notice_container);
        mNoticeContentTv = view.findViewById(R.id.nc_conversationlist_notice_tv);
        mNoticeIconIv = view.findViewById(R.id.nc_conversationlist_notice_icon_iv);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull ChannelListViewModel viewModel) {
        if (!NCChatUI.isInitialized()) {
            RLog.e(TAG, "Please init SDK first!");
            return;
        }
        mAdapter = onCreateChannelListAdapter();
        mAdapter.setItemClickListener(
                new ChannelListAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, ViewHolder holder, int position) {
                        ChannelListFragment.this.onItemClick(view, holder, position);
                    }

                    @Override
                    public boolean onItemLongClick(View view, ViewHolder holder, int position) {
                        return ChannelListFragment.this.onItemLongClick(view, holder, position);
                    }
                });

        layoutManager = new FixedLinearLayoutManager(getActivity());
        mList.setLayoutManager(layoutManager);
        mList.setAdapter(mAdapter);
        mList.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(
                            @NonNull RecyclerView recyclerView, int newState) {
                        mScrollState = newState;
                        tryConsumePendingRefresh(viewModel);
                    }
                });
        mList.addOnItemTouchListener(
                new RecyclerView.SimpleOnItemTouchListener() {
                    @Override
                    public boolean onInterceptTouchEvent(
                            @NonNull RecyclerView rv, @NonNull MotionEvent e) {
                        int action = e.getActionMasked();
                        if (action == MotionEvent.ACTION_DOWN
                                || action == MotionEvent.ACTION_POINTER_DOWN) {
                            isUserTouchingList = true;
                        } else if (action == MotionEvent.ACTION_UP
                                || action == MotionEvent.ACTION_CANCEL) {
                            isUserTouchingList = false;
                            tryConsumePendingRefresh(viewModel);
                        }
                        return false;
                    }
                });

        initRefreshView();
        subscribeUi(viewModel);
    }

    @Override
    public void onResume() {
        super.onResume();
        ChatUINotificationManager.getInstance().onChannelListPageResumed();
        getViewModel().onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        ChatUINotificationManager.getInstance().onChannelListPagePaused();
    }

    // endregion

    // region Refresh controls (pull-to-refresh + load more)

    /**
     * Initializes the refresh controls. Enables pull-to-refresh and load-more.
     *
     * <p>Subclasses may override this method to customize refresh behavior.
     */
    protected void initRefreshView() {
        if (mRefreshLayout == null) return;
        mRefreshLayout.setNestedScrollingEnabled(false);

        // Pull-to-refresh header
        mRefreshLayout.setRefreshHeader(new NCRefreshHeader(getContext()));
        mRefreshLayout.setEnableRefresh(true);
        mRefreshLayout.setOnRefreshListener(
                new OnRefreshListener() {
                    @Override
                    public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                        getViewModel().refresh();
                        refreshLayout.finishRefresh(true);
                    }
                });

        // Load more on scroll up
        mRefreshLayout.setRefreshFooter(new NCRefreshHeader(getContext()));
        OnPagedDataLoader loader = getViewModel().getPagedDataLoader();
        mRefreshLayout.setOnLoadMoreListener(
                refreshLayout -> {
                    if (loader != null) {
                        loader.loadNext(
                                hasMore -> {
                                    refreshLayout.finishLoadMore();
                                    if (!loader.hasNext()) {
                                        refreshLayout.setEnableLoadMore(false);
                                    }
                                });
                    }
                });
    }

    // endregion

    // region Data subscription

    /**
     * Subscribes to ViewModel's LiveData and binds to UI.
     *
     * <p>Subclasses may override to fully customize data binding (e.g. using a different
     * ViewModel).
     */
    protected void subscribeUi(@NonNull ChannelListViewModel viewModel) {
        viewModel.refresh();

        viewModel
                .getConversationListLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        uiConversations -> {
                            if (mScrollState == RecyclerView.SCROLL_STATE_IDLE
                                    && !isUserTouchingList) {
                                mAdapter.setDataCollection(uiConversations);
                            } else {
                                pendingRefresh = true;
                            }
                        });

        viewModel
                .getNoticeContentLiveData()
                .observe(getViewLifecycleOwner(), this::updateNoticeContent);
    }

    // endregion

    // region Notice bar

    protected void updateNoticeContent(NoticeContent content) {
        if (content == null) return;
        if (content.isShowNotice()) {
            mNoticeContainerView.setVisibility(View.VISIBLE);
            mNoticeContentTv.setText(content.getContent());
            if (content.getIconResId() != 0) {
                mNoticeIconIv.setImageResource(content.getIconResId());
            }
        } else {
            mNoticeContainerView.setVisibility(View.GONE);
        }
    }

    // endregion

    // region Click events (overridable by subclasses)

    /** Channel item click; subclasses may override to customize navigation. */
    protected void onItemClick(View view, ViewHolder holder, int position) {
        if (position < 0 || position >= mAdapter.getData().size()) return;
        BaseUiChannel conversation = mAdapter.getItem(position);
        ChannelListBehaviorListener listener = NCChatUIConfig.channelListConfig().getListener();
        if (listener != null
                && listener.onConversationClick(view.getContext(), view, conversation)) {
            return;
        }
        if (conversation != null && conversation.mCore != null) {
            RouteUtils.routeToChannelActivity(
                    view.getContext(), conversation.getConversationIdentifier());
        }
    }

    /** Channel item long-press; subclasses may override to customize popup. */
    protected boolean onItemLongClick(View view, ViewHolder holder, int position) {
        if (position < 0 || position >= mAdapter.getData().size()) return false;
        BaseUiChannel conversation = mAdapter.getItem(position);
        ChannelListBehaviorListener listener = NCChatUIConfig.channelListConfig().getListener();
        if (listener != null
                && listener.onConversationLongClick(view.getContext(), view, conversation)) {
            return true;
        }
        showConversationLongClickMenu(view, conversation);
        return true;
    }

    /** Shows the long-press menu; subclasses may override to customize menu items. */
    protected void showConversationLongClickMenu(View view, BaseUiChannel conversation) {
        String removeItem = view.getContext().getString(R.string.nc_delete);
        String setTopItem = view.getContext().getString(R.string.nc_set_top);
        String cancelTopItem =
                view.getContext().getString(R.string.nc_conversation_list_dialog_cancel_top);

        showLivelyLongClickMenu(view, conversation, removeItem, setTopItem, cancelTopItem);
    }

    private void showLivelyLongClickMenu(
            View view,
            BaseUiChannel conversation,
            String removeItem,
            String setTopItem,
            String cancelTopItem) {
        boolean isTop = conversation.mCore.isPinned();
        ArrayList<ConversationLongClickPopup.OptionItem> items = new ArrayList<>();
        items.add(
                new ConversationLongClickPopup.OptionItem(
                        isTop ? cancelTopItem : setTopItem, R.drawable.nc_lively_pin_up));
        items.add(
                new ConversationLongClickPopup.OptionItem(
                        removeItem, R.drawable.nc_lively_delete_read));

        ConversationLongClickPopup.newInstance(view.getContext(), items)
                .setAnchorView(view)
                .setOnOptionItemClickListener(
                        (item, pos) -> {
                            if (item.title.equals(setTopItem) || item.title.equals(cancelTopItem)) {
                                toggleConversationTop(
                                        conversation, isTop ? cancelTopItem : setTopItem);
                            } else if (item.title.equals(removeItem)) {
                                removeConversation(conversation);
                            }
                        })
                .show();
    }

    /** Pin/unpin a channel */
    protected void toggleConversationTop(BaseUiChannel conversation, String toastText) {
        ChannelIdentifier id = conversation.getConversationIdentifier();
        getViewModel()
                .setChannelTop(
                        id,
                        !conversation.mCore.isPinned(),
                        (result, error) -> {
                            if (error == null) {
                                Activity activity = getActivity();
                                if (activity != null
                                        && !activity.isFinishing()
                                        && !activity.isDestroyed()) {
                                    ToastUtils.show(activity, toastText, Toast.LENGTH_SHORT);
                                }
                            }
                        });
    }

    /** Delete a channel */
    protected void removeConversation(BaseUiChannel conversation) {
        getViewModel()
                .removeChannel(
                        conversation.mCore.getChannelType(), conversation.mCore.getChannelId());
    }

    // endregion

    // region Public API

    public void addHeaderView(View view) {
        mAdapter.addHeaderView(view);
    }

    public void addFooterView(View view) {
        mAdapter.addFootView(view);
    }

    public void setEmptyView(View view) {
        mAdapter.setEmptyView(view);
    }

    public void setEmptyView(@LayoutRes int emptyId) {
        mAdapter.setEmptyView(emptyId);
    }

    // endregion

    private void tryConsumePendingRefresh(@NonNull ChannelListViewModel viewModel) {
        if (mScrollState != RecyclerView.SCROLL_STATE_IDLE
                || isUserTouchingList
                || !pendingRefresh
                || mAdapter == null) {
            return;
        }
        pendingRefresh = false;
        mAdapter.setDataCollection(viewModel.getConversationListLiveData().getValue());
    }
}
