package ai.nexconn.chatui.channel.subchannel;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chatui.channellist.ChannelListFragment;
import ai.nexconn.chatui.channellist.ChannelListViewModel;
import ai.nexconn.chatui.utils.route.RouteUtils;
import ai.nexconn.chatui.widget.pullrefresh.constant.RefreshState;
import ai.nexconn.chatui.widget.pullrefresh.wrapper.NCRefreshHeader;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

public class SubChannelListFragment extends ChannelListFragment {
    private final String TAG = SubChannelListFragment.class.getSimpleName();
    private SubChannelListViewModel mSubChannelListViewModel;
    private ChannelType mChannelType;

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        if (getActivity() != null && getActivity().getIntent() != null) {
            mChannelType =
                    (ChannelType)
                            getActivity().getIntent().getSerializableExtra(RouteUtils.CHANNEL_TYPE);
        }
        return super.onCreateView(context, inflater, container, savedInstanceState);
    }

    @Override
    protected void initRefreshView() {
        if (mRefreshLayout == null) return;
        mRefreshLayout.setNestedScrollingEnabled(false);
        mRefreshLayout.setRefreshHeader(new NCRefreshHeader(getContext()));
        mRefreshLayout.setRefreshFooter(new NCRefreshHeader(getContext()));
        mRefreshLayout.setOnRefreshListener(
                refreshLayout -> {
                    if (mSubChannelListViewModel != null) {
                        mSubChannelListViewModel.getConversationList(false, true, 0);
                    }
                });
        mRefreshLayout.setOnLoadMoreListener(
                refreshLayout -> {
                    if (mSubChannelListViewModel != null) {
                        mSubChannelListViewModel.getConversationList(true, true, 0);
                    }
                });
    }

    @Override
    protected void subscribeUi(@NonNull ChannelListViewModel viewModel) {
        if (getActivity() == null) return;
        SubChannelListVMFactory factory =
                new SubChannelListVMFactory(getActivity().getApplication(), mChannelType);
        mSubChannelListViewModel =
                new ViewModelProvider(this, factory).get(SubChannelListViewModel.class);
        mSubChannelListViewModel.getConversationList(false, false, 0);

        mSubChannelListViewModel
                .getConversationListLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        baseUiConversations -> mAdapter.setDataCollection(baseUiConversations));

        mSubChannelListViewModel
                .getNoticeContentLiveData()
                .observe(getViewLifecycleOwner(), this::updateNoticeContent);

        mSubChannelListViewModel
                .getRefreshEventLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        refreshEvent -> {
                            if (mRefreshLayout == null) return;
                            if (refreshEvent.state.equals(RefreshState.LoadFinish)) {
                                mRefreshLayout.finishLoadMore();
                            } else if (refreshEvent.state.equals(RefreshState.RefreshFinish)) {
                                mRefreshLayout.finishRefresh();
                            }
                        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSubChannelListViewModel != null) {
            mSubChannelListViewModel.onResume();
        }
    }
}
