package ai.nexconn.chatui.channellist.handler;

import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.query.ChannelsQuery;
import ai.nexconn.chat.params.ChannelsQueryParams;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.base.interfaces.OnPagedDataLoader;
import ai.nexconn.chatui.channellist.model.BaseUiChannel;
import ai.nexconn.chatui.channellist.model.UiDirectChannel;
import ai.nexconn.chatui.channellist.model.UiGroupChannel;
import ai.nexconn.chatui.config.DataProcessor;
import ai.nexconn.chatui.config.NCChatUIConfig;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Core data handler for the channel list
 *
 * <p>Responsible for channel data fetching, filtering, sorting, deduplication, and UI model
 * management. Implements {@link OnPagedDataLoader} to support load-more pagination.
 *
 * <p>Data loading has two paths:
 *
 * <ul>
 *   <li>{@link #refresh()} — initial load or event-driven refresh, resets pagination
 *       (auto-coalesces rapid calls)
 *   <li>{@link #loadNext(OnDataChangeListener)} — paginated load-more
 * </ul>
 *
 * @since 5.10.4
 */
public class ChannelListDataHandler extends MultiDataHandler implements OnPagedDataLoader {

    private static final String TAG = "ChannelListDataHandler";
    private static final long DEBOUNCE_MS = 500;
    private static final int PAGE_INCREMENT = 10;

    @SuppressWarnings("unchecked")
    public static final DataKey<List<BaseUiChannel>> KEY_CONVERSATION_LIST =
            DataKey.obtain(
                    "KEY_CONVERSATION_LIST", (Class<List<BaseUiChannel>>) (Class<?>) List.class);

    private final Context applicationContext;
    private final HandlerThread workThread;
    private final Handler workHandler;
    private final ChannelType[] supportedTypes;
    private final DataProcessor<BaseChannel> dataFilter;
    private final boolean topPriority;
    private final int initialPageSize;

    private int requestedPageSize;
    private long lastRefreshTime = 0;
    private boolean isFetchScheduled = false;
    private boolean hasMoreData = true;
    private boolean isRefreshing = false;
    private OnDataChangeListener<Boolean> pendingLoadMoreListener;

    protected final CopyOnWriteArrayList<BaseUiChannel> uiConversationList =
            new CopyOnWriteArrayList<>();

    public ChannelListDataHandler(Context applicationContext) {
        this.applicationContext = applicationContext;
        workThread = new HandlerThread("ConversationList_Thread");
        workThread.start();
        workHandler = new Handler(workThread.getLooper());

        dataFilter = NCChatUIConfig.channelListConfig().getDataProcessor();
        supportedTypes = dataFilter.supportedTypes();
        initialPageSize = NCChatUIConfig.channelListConfig().getConversationCountPerPage();
        requestedPageSize = initialPageSize;
        topPriority = NCChatUIConfig.channelListConfig().isTopPriority();
    }

    // region Data fetching

    // Increase coalesce delay to reduce refresh frequency during message bursts
    private static final long REFRESH_COALESCE_MS = 500;

    private final Runnable fetchRunnable =
            () -> {
                isFetchScheduled = false;
                doRefresh();
            };

    private void doRefresh() {
        requestedPageSize =
                Math.max(initialPageSize, Math.max(requestedPageSize, uiConversationList.size()));
        isRefreshing = true;
        pendingLoadMoreListener = null;
        doFetch();
    }

    /** Refreshes the channel list. Rapid successive calls are auto-coalesced. */
    public void refresh() {
        refresh(REFRESH_COALESCE_MS);
    }

    /**
     * Refreshes the channel list with a custom delay.
     *
     * <p>If delay is non-positive, falls back to the default coalescing window.
     */
    public void refresh(long delayMs) {
        long safeDelay = delayMs > 0 ? delayMs : REFRESH_COALESCE_MS;
        workHandler.post(
                () -> {
                    if (isFetchScheduled) {
                        return;
                    }
                    isFetchScheduled = true;
                    workHandler.postDelayed(fetchRunnable, safeDelay);
                });
    }

    private void doFetch() {
        ChannelsQueryParams params = new ChannelsQueryParams(Arrays.asList(supportedTypes));
        params.setPageSize(requestedPageSize);
        params.setTopPriority(topPriority);

        ChannelsQuery query = BaseChannel.createChannelsQuery(params);
        WeakReference<ChannelListDataHandler> ref = new WeakReference<>(this);
        query.loadNextPage(
                (result, error) -> {
                    ChannelListDataHandler h = ref.get();
                    if (h == null) return;
                    if (error != null) {
                        h.notifyPendingLoadMoreListener();
                        return;
                    }
                    List<BaseChannel> channels = result != null ? result.getData() : null;
                    h.doUpdate(channels);
                });
    }

    // endregion

    // region Data update

    protected void doUpdate(List<BaseChannel> channels) {
        workHandler.post(
                () -> {
                    if (isRefreshing) {
                        hasMoreData = true;
                    }
                    if (channels == null) {
                        notifyPendingLoadMoreListener();
                        return;
                    }
                    if (channels.isEmpty()) {
                        if (!isRefreshing) {
                            hasMoreData = false;
                            notifyPendingLoadMoreListener();
                            return;
                        }
                        notifyPendingLoadMoreListener();
                        // A successful refresh may legitimately return empty after the last
                        // channel is removed. Treat it as the new source of truth.
                        rebuildFromLatestFetch(new ArrayList<>());
                        refreshConversationList();
                        return;
                    }

                    if (!isRefreshing) {
                        hasMoreData = channels.size() >= requestedPageSize;
                    }
                    notifyPendingLoadMoreListener();

                    List<BaseChannel> filtered = dataFilter.filtered(new ArrayList<>(channels));
                    // Always rebuild from the latest fetched dataset (including empty result)
                    // to avoid stale cache entries remaining in the old list.
                    rebuildFromLatestFetch(filtered != null ? filtered : new ArrayList<>());
                    refreshConversationList();
                });
    }

    private void notifyPendingLoadMoreListener() {
        if (pendingLoadMoreListener != null) {
            pendingLoadMoreListener.onDataChange(hasMoreData);
            pendingLoadMoreListener = null;
        }
    }

    /** Updates a single channel (from event callback) */
    public void updateByChannel(BaseChannel channel) {
        if (channel == null || !isSupported(channel.getChannelType())) {
            return;
        }
        workHandler.post(
                () -> {
                    List<BaseChannel> single = new ArrayList<>(1);
                    single.add(channel);
                    List<BaseChannel> filtered = dataFilter.filtered(single);
                    if (filtered != null && !filtered.isEmpty()) {
                        // Use post-filter channel as source of truth.
                        mergeOrAdd(filtered.get(0));
                        refreshConversationList();
                        return;
                    }

                    // If this channel is filtered out now, remove stale cached item.
                    BaseUiChannel existing =
                            findConversationFromList(
                                    channel.getChannelType(), channel.getChannelId());
                    if (existing != null) {
                        uiConversationList.remove(existing);
                        refreshConversationList();
                    }
                });
    }

    private void mergeOrAdd(BaseChannel channel) {
        BaseUiChannel existing =
                findConversationFromList(channel.getChannelType(), channel.getChannelId());
        if (existing != null) {
            existing.onConversationUpdate(channel);
        } else {
            uiConversationList.add(createUiConversation(channel));
        }
    }

    /**
     * Rebuilds the UI list using the latest fetch result as source of truth.
     *
     * <p>Only channels present in {@code latestChannels} are kept. Existing UI items are reused
     * when possible to reduce object churn, but their underlying data is always refreshed with the
     * latest channel snapshot.
     */
    private void rebuildFromLatestFetch(List<BaseChannel> latestChannels) {
        Map<String, BaseUiChannel> existingMap = new HashMap<>(uiConversationList.size());
        for (BaseUiChannel item : uiConversationList) {
            existingMap.put(item.getChannelKey(), item);
        }

        List<BaseUiChannel> latestUiList = new ArrayList<>(latestChannels.size());
        for (BaseChannel channel : latestChannels) {
            String key = buildChannelKey(channel);
            BaseUiChannel existing = existingMap.get(key);
            if (existing != null) {
                existing.onConversationUpdate(channel);
                latestUiList.add(existing);
            } else {
                latestUiList.add(createUiConversation(channel));
            }
        }

        uiConversationList.clear();
        uiConversationList.addAll(latestUiList);
    }

    private String buildChannelKey(BaseChannel channel) {
        ChannelType type =
                channel.getChannelType() != null ? channel.getChannelType() : ChannelType.DIRECT;
        String channelId = channel.getChannelId() != null ? channel.getChannelId() : "";
        return "type=" + type + "&tid=" + channelId;
    }

    // endregion

    // region Find & Sort & Deduplicate

    public BaseUiChannel findConversationFromList(ChannelType type, String channelId) {
        for (int i = uiConversationList.size() - 1; i >= 0; i--) {
            BaseUiChannel item = uiConversationList.get(i);
            if (item.mCore.getChannelType().equals(type)
                    && Objects.equals(item.mCore.getChannelId(), channelId)) {
                return item;
            }
        }
        return null;
    }

    // endregion

    // region Refresh notification (debounced)

    private final Runnable refreshRunnable =
            () -> {
                synchronized (uiConversationList) {
                    lastRefreshTime = SystemClock.elapsedRealtime();
                    notifyDataChange(KEY_CONVERSATION_LIST, uiConversationList);
                }
            };

    public void refreshConversationList() {
        workHandler.removeCallbacks(refreshRunnable);
        if (SystemClock.elapsedRealtime() - lastRefreshTime > DEBOUNCE_MS) {
            refreshRunnable.run();
        } else {
            workHandler.postDelayed(refreshRunnable, DEBOUNCE_MS);
        }
    }

    // endregion

    // region Utility methods

    public boolean isSupported(ChannelType type) {
        if (supportedTypes == null) return false;
        for (ChannelType ct : supportedTypes) {
            if (ct.equals(type)) return true;
        }
        return false;
    }

    private BaseUiChannel createUiConversation(BaseChannel channel) {
        if (ChannelType.GROUP.equals(channel.getChannelType())
                || ChannelType.COMMUNITY.equals(channel.getChannelType())) {
            return new UiGroupChannel(applicationContext, channel);
        }
        return new UiDirectChannel(applicationContext, channel);
    }

    // endregion

    // region OnPagedDataLoader

    @Override
    public void loadNext(OnDataChangeListener<Boolean> listener) {
        pendingLoadMoreListener = listener;
        workHandler.post(
                () -> {
                    requestedPageSize += PAGE_INCREMENT;
                    isRefreshing = false;
                    doFetch();
                });
    }

    @Override
    public boolean hasNext() {
        return hasMoreData;
    }

    // endregion

    @Override
    public void stop() {
        super.stop();
        workHandler.removeCallbacksAndMessages(null);
        workThread.quit();
    }
}
