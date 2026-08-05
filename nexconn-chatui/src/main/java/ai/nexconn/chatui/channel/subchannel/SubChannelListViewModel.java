package ai.nexconn.chatui.channel.subchannel;

import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.query.ChannelsQuery;
import ai.nexconn.chat.params.ChannelsQueryParams;
import ai.nexconn.chatui.channel.event.Event;
import ai.nexconn.chatui.channellist.model.BaseUiChannel;
import ai.nexconn.chatui.channellist.model.UiDirectChannel;
import ai.nexconn.chatui.channellist.model.UiGroupChannel;
import ai.nexconn.chatui.config.BaseDataProcessor;
import ai.nexconn.chatui.config.DataProcessor;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.NoticeContent;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.widget.pullrefresh.constant.RefreshState;
import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

class SubChannelListViewModel extends AndroidViewModel {
    private static final String TAG = SubChannelListViewModel.class.getSimpleName();

    protected final Application mApplication;
    protected ChannelType[] mSupportedTypes;
    protected DataProcessor<BaseChannel> mDataFilter;
    protected int mSizePerPage;
    protected int mPageLimit;
    protected final CopyOnWriteArrayList<BaseUiChannel> mUiConversationList =
            new CopyOnWriteArrayList<>();
    private ChannelsQuery mChannelsQuery;

    private final MutableLiveData<List<BaseUiChannel>> conversationListLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<NoticeContent> noticeContentLiveData = new MutableLiveData<>();
    private final MutableLiveData<Event.RefreshEvent> refreshEventLiveData =
            new MutableLiveData<>();

    SubChannelListViewModel(Application application, ChannelType channelType) {
        super(application);
        mApplication = application;
        mSupportedTypes = new ChannelType[] {channelType};
        mSizePerPage = NCChatUIConfig.channelListConfig().getConversationCountPerPage();
        mPageLimit = 10;

        final DataProcessor<BaseChannel> userSetDataFilter =
                NCChatUIConfig.channelListConfig().getDataProcessor();
        mDataFilter =
                new BaseDataProcessor<BaseChannel>() {

                    @Override
                    public ChannelType[] supportedTypes() {
                        if (userSetDataFilter != null) {
                            return userSetDataFilter.supportedTypes();
                        }
                        return super.supportedTypes();
                    }

                    @Override
                    public List<BaseChannel> filtered(List<BaseChannel> data) {
                        if (userSetDataFilter != null) {
                            return userSetDataFilter.filtered(data);
                        }
                        return super.filtered(data);
                    }

                    @Override
                    public boolean isGathered(ChannelType type) {
                        return false;
                    }
                };
    }

    public void getConversationList(
            final boolean loadMore, final boolean isEventManual, long delayTime) {
        if (loadMore) {
            mSizePerPage += mPageLimit;
        }

        if (!loadMore || mChannelsQuery == null) {
            ChannelsQueryParams params =
                    new ChannelsQueryParams(
                            Collections.singletonList(
                                    mSupportedTypes.length > 0
                                            ? mSupportedTypes[0]
                                            : ChannelType.DIRECT));
            params.setPageSize(mSizePerPage);
            params.setTopPriority(true);
            mChannelsQuery = BaseChannel.createChannelsQuery(params);
        }

        mChannelsQuery.loadNextPage(
                (result, error) -> {
                    if (error != null) {
                        RLog.e(TAG, "getConversationList error: " + error.getMessage());
                        return;
                    }
                    List<BaseChannel> channels =
                            result != null ? result.getData() : new ArrayList<>();
                    if (channels == null) {
                        channels = new ArrayList<>();
                    }

                    if (loadMore) {
                        mSizePerPage += channels.size();
                    } else {
                        mUiConversationList.clear();
                    }
                    if (isEventManual) {
                        if (loadMore) {
                            refreshEventLiveData.postValue(
                                    new Event.RefreshEvent(RefreshState.LoadFinish));
                        } else {
                            refreshEventLiveData.postValue(
                                    new Event.RefreshEvent(RefreshState.RefreshFinish));
                        }
                    }
                    if (channels.isEmpty()) {
                        return;
                    }
                    RLog.d(TAG, "getConversationList. size:" + channels.size());
                    for (BaseChannel channel : channels) {
                        BaseUiChannel oldItem =
                                findConversationFromList(
                                        channel.getChannelType(), channel.getChannelId(), false);
                        if (oldItem != null) {
                            oldItem.onConversationUpdate(channel);
                        } else {
                            if (ChannelType.GROUP.equals(channel.getChannelType())) {
                                mUiConversationList.add(
                                        new UiGroupChannel(
                                                mApplication.getApplicationContext(), channel));
                            } else {
                                mUiConversationList.add(
                                        new UiDirectChannel(
                                                mApplication.getApplicationContext(), channel));
                            }
                        }
                    }
                    sort();
                    refreshConversationList();
                });
    }

    protected void updateByConversation(BaseChannel channel) {
        if (channel == null) {
            return;
        }
        List<BaseChannel> list = new CopyOnWriteArrayList<>();
        list.add(channel);
        List<BaseChannel> filterList = mDataFilter.filtered(list);
        if (filterList != null && filterList.size() > 0 && isSupported(channel.getChannelType())) {
            BaseUiChannel oldItem =
                    findConversationFromList(
                            channel.getChannelType(), channel.getChannelId(), false);
            if (oldItem != null) {
                oldItem.onConversationUpdate(channel);
            } else {
                if (ChannelType.GROUP.equals(channel.getChannelType())) {
                    mUiConversationList.add(
                            new UiGroupChannel(mApplication.getApplicationContext(), channel));
                } else {
                    mUiConversationList.add(
                            new UiDirectChannel(mApplication.getApplicationContext(), channel));
                }
            }
            sort();
            refreshConversationList();
        }
    }

    protected BaseUiChannel findConversationFromList(
            ChannelType channelType, String channelId, boolean isGathered) {
        List<BaseUiChannel> list = new ArrayList<>(mUiConversationList);
        for (int i = list.size() - 1; i >= 0; i--) {
            BaseUiChannel uiConversation = list.get(i);
            if (!isGathered
                    && uiConversation.mCore.getChannelType().equals(channelType)
                    && Objects.equals(uiConversation.mCore.getChannelId(), channelId)) {
                return uiConversation;
            }
        }
        return null;
    }

    protected void sort() {
        // No special sorting for sub-channels
    }

    protected void refreshConversationList() {
        conversationListLiveData.postValue(new ArrayList<>(mUiConversationList));
    }

    protected boolean isSupported(ChannelType type) {
        if (mSupportedTypes == null) {
            return false;
        }
        for (ChannelType ct : mSupportedTypes) {
            if (ct.equals(type)) {
                return true;
            }
        }
        return false;
    }

    public void onResume() {
        // No additional operations for sub-channel onResume
    }

    public LiveData<List<BaseUiChannel>> getConversationListLiveData() {
        return conversationListLiveData;
    }

    public LiveData<NoticeContent> getNoticeContentLiveData() {
        return noticeContentLiveData;
    }

    public LiveData<Event.RefreshEvent> getRefreshEventLiveData() {
        return refreshEventLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
