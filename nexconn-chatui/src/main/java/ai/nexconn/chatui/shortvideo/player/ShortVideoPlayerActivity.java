package ai.nexconn.chatui.shortvideo.player;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.query.LocalMessagesByTimeQuery;
import ai.nexconn.chat.handler.MessageHandler;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.ReferenceMessage;
import ai.nexconn.chat.message.ShortVideoMessage;
import ai.nexconn.chat.message.model.MessageDeletedEvent;
import ai.nexconn.chat.message.model.MessageType;
import ai.nexconn.chat.params.LocalMessagesByTimeQueryParams;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.activity.NCBaseNoActionbarActivity;
import ai.nexconn.chatui.channel.event.action.DeleteEvent;
import ai.nexconn.chatui.channel.event.action.MessageEventListener;
import ai.nexconn.chatui.utils.log.RLog;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ShortVideoPlayerActivity extends NCBaseNoActionbarActivity {

    private static final String TAG = "ShortVideoPlayerActivity";
    private static final int VIDEO_MESSAGE_COUNT =
            10; // Number of video messages to fetch each time.
    private static final long LOAD_MORE_VIDEO_DELAYED_TIME = 800;
    private static final java.util.concurrent.ConcurrentHashMap<Integer, Message> sMessageCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    static void putMessageToCache(Message message) {
        if (message != null && message.getClientId() > 0) {
            sMessageCache.put(message.getClientId(), message);
        }
    }

    @androidx.annotation.Nullable
    static Message getMessageFromCache(int clientId) {
        return sMessageCache.get(clientId);
    }

    public static void clearMessageCache() {
        sMessageCache.clear();
    }

    protected ViewPager2 mViewPager;
    protected ShortVideoMessage mCurrentSightMessage;
    protected Message mMessage;
    protected boolean mFromList;
    private boolean mDisplayCurrentVideoOnly;
    protected ChannelType mConversationType;

    protected String mTargetId;

    private VideoPagerAdapter mVideoPagerAdapter;
    private int currentSelectMessageId = -1;
    private boolean loadingOlderVideos;
    private boolean loadingNewerVideos;
    private boolean noMoreOlderVideos;
    private boolean noMoreNewerVideos;

    protected ViewPager2.OnPageChangeCallback mPageChangeListener =
            new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    if (position >= 0 && position < mVideoPagerAdapter.getItemCount()) {
                        currentSelectMessageId = mVideoPagerAdapter.getItem(position).getClientId();
                    }
                    if (isLoadSingleMessage()) {
                        return;
                    }
                    if (position == (mVideoPagerAdapter.getItemCount() - 1)) {
                        if (mVideoPagerAdapter.getItemCount() > 0) {
                            getSightMessageList(
                                    mVideoPagerAdapter.getItem(position).getClientId(), false);
                        }
                    } else if (position == 0) {
                        if (mVideoPagerAdapter.getItemCount() > 0) {
                            getSightMessageList(
                                    mVideoPagerAdapter.getItem(position).getClientId(), true);
                        }
                    }
                }
            };

    MessageEventListener mBaseMessageEvent =
            new MessageEventListener() {
                @Override
                public void onDeleteMessage(DeleteEvent event) {
                    RLog.d(TAG, "MessageDeleteEvent");
                    if (event.getMessageIds() != null) {
                        for (int messageId : event.getMessageIds()) {
                            mVideoPagerAdapter.removeRecallItem(messageId);
                        }
                        if (mVideoPagerAdapter.getItemCount() == 0) {
                            finish();
                        }
                    }
                }
            };
    private final MessageHandler mRecallMessageHandler =
            new MessageHandler() {
                @Override
                public void onMessageDeleted(@NonNull MessageDeletedEvent event) {
                    if (event.getMessages() == null) {
                        return;
                    }
                    for (Message msg : event.getMessages()) {
                        if (msg == null) {
                            continue;
                        }
                        mVideoPagerAdapter.removeRecallItem(msg.getClientId());
                        if (currentSelectMessageId == msg.getClientId()) {
                            showRecallDialog();
                            return;
                        }
                    }
                    if (mVideoPagerAdapter.getItemCount() == 0) {
                        finish();
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getWindow() != null) {
            int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getWindow().setFlags(flag, flag);
        }
        setContentView(R.layout.nc_activity_sight_player);
        initView();
        initData();
        NCChatUI.addMessageEventListener(mBaseMessageEvent);
        NCEngine.addMessageHandler("ShortVideoPlayerActivity_recall", mRecallMessageHandler);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NCChatUI.removeMessageEventListener(mBaseMessageEvent);
        NCEngine.removeMessageHandler("ShortVideoPlayerActivity_recall");
    }

    @Override
    public void finish() {
        super.finish();
        // When a fullscreen Activity finishes and returns to a non-fullscreen one, it causes page
        // redraw flicker
        // (typical symptom: RecyclerView scrolls down slightly). Clear the fullscreen flag after
        // finish to avoid this.
        if (getWindow() != null) {
            int flag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
            getWindow().setFlags(flag, flag);
        }
    }

    private void initData() {
        mMessage = ai.nexconn.chatui.utils.message.MessageHolder.takeMessage();
        if (mMessage == null || !(mMessage.getContent() instanceof ShortVideoMessage)) {
            RLog.e(TAG, "initData: invalid short video message, finish activity");
            finish();
            return;
        }
        mCurrentSightMessage = (ShortVideoMessage) mMessage.getContent();
        putMessageToCache(mMessage);
        mConversationType = mMessage.getChannelIdentifier().getChannelType();
        mTargetId = mMessage.getChannelIdentifier().getChannelId();
        mFromList = getIntent().getBooleanExtra("fromList", false);
        mDisplayCurrentVideoOnly = getIntent().getBooleanExtra("displayCurrentVideoOnly", false);
        initPager();
    }

    private void initPager() {
        // Burn-after-reading and reference messages only show a single video
        mVideoPagerAdapter =
                new VideoPagerAdapter(this, mMessage != null ? mMessage.getClientId() : -1);

        ArrayList<Message> messages = new ArrayList<>();
        mViewPager.setAdapter(mVideoPagerAdapter);
        // Load the current video message first, then load previous and next video messages
        messages.add(mMessage);
        mVideoPagerAdapter.addMessage(messages, mFromList, mDisplayCurrentVideoOnly, true);
        // If no need to display previous/next video messages, return directly
        if (isLoadSingleMessage() || mDisplayCurrentVideoOnly) {
            return;
        }
        // Delay loading previous/next video messages to prevent ViewPager from not showing the
        // first item
        new Handler()
                .postDelayed(
                        () -> {
                            getSightMessageList(mMessage.getClientId(), true);
                            getSightMessageList(mMessage.getClientId(), false);
                        },
                        LOAD_MORE_VIDEO_DELAYED_TIME);
    }

    // Burn-after-reading, reference messages, and ultra group channels only show a single video
    private boolean isLoadSingleMessage() {
        if (mMessage == null || mMessage.getContent() == null) {
            return true;
        }
        return mMessage.getContent().isDestruct()
                || mMessage.getContent() instanceof ReferenceMessage
                || (mConversationType != null && mConversationType.getValue() == 10);
    }

    private void getSightMessageList(int messageId, boolean loadOlder) {
        if (mConversationType == null || mTargetId == null || mTargetId.length() == 0) {
            RLog.e(TAG, "getSightMessageList: invalid channel");
            return;
        }
        if (mVideoPagerAdapter == null) {
            return;
        }
        if ((loadOlder && (loadingOlderVideos || noMoreOlderVideos))
                || (!loadOlder && (loadingNewerVideos || noMoreNewerVideos))) {
            return;
        }

        Message anchor = mVideoPagerAdapter.getItemByMessageId(messageId);
        if (anchor == null) {
            RLog.e(TAG, "getSightMessageList: anchor message not found, clientId=" + messageId);
            return;
        }
        ChannelIdentifier channelIdentifier = anchor.getChannelIdentifier();
        if (channelIdentifier == null) {
            RLog.e(TAG, "getSightMessageList: anchor channel is null, clientId=" + messageId);
            return;
        }

        if (loadOlder) {
            loadingOlderVideos = true;
        } else {
            loadingNewerVideos = true;
        }

        LocalMessagesByTimeQueryParams params =
                new LocalMessagesByTimeQueryParams(channelIdentifier);
        params.setSentTime(anchor.getSentTime());
        params.setAscending(!loadOlder);
        params.setMessageTypes(Collections.singletonList(MessageType.SHORT_VIDEO));
        params.setPageSize(VIDEO_MESSAGE_COUNT);
        LocalMessagesByTimeQuery query = BaseChannel.createLocalMessagesByTimeQuery(params);
        query.loadNextPage(
                (result, error) -> {
                    if (loadOlder) {
                        loadingOlderVideos = false;
                    } else {
                        loadingNewerVideos = false;
                    }
                    if (error != null || result == null) {
                        RLog.e(TAG, "getSightMessageList failed: " + error);
                        return;
                    }
                    List<Message> messages = result.getData();
                    if (messages == null || messages.isEmpty()) {
                        markNoMoreVideos(loadOlder);
                        return;
                    }
                    runOnUiThread(
                            () -> {
                                List<Message> lists = new ArrayList<>(messages);
                                if (loadOlder) {
                                    Collections.reverse(lists);
                                }
                                int insertedCount =
                                        mVideoPagerAdapter.addMessageAndReturnInsertedCount(
                                                lists,
                                                mFromList,
                                                mDisplayCurrentVideoOnly,
                                                loadOlder);
                                if (insertedCount == 0 || lists.size() < VIDEO_MESSAGE_COUNT) {
                                    markNoMoreVideos(loadOlder);
                                }
                                if (loadOlder && insertedCount > 0) {
                                    mViewPager.setCurrentItem(
                                            keepCurrentItemAfterHeadInsert(
                                                    mViewPager.getCurrentItem(), insertedCount),
                                            false);
                                }
                            });
                });
    }

    private void markNoMoreVideos(boolean loadOlder) {
        if (loadOlder) {
            noMoreOlderVideos = true;
        } else {
            noMoreNewerVideos = true;
        }
    }

    static int keepCurrentItemAfterHeadInsert(int currentItem, int insertedCount) {
        return insertedCount > 0 ? currentItem + insertedCount : currentItem;
    }

    private void initView() {
        mViewPager = findViewById(R.id.viewpager);
        mViewPager.registerOnPageChangeCallback(mPageChangeListener);
        mViewPager.setOffscreenPageLimit(ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT);
    }

    private void showRecallDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setMessage(getString(R.string.nc_recall_success))
                .setPositiveButton(getString(R.string.nc_dialog_ok), (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    /** Pause the currently playing video */
    private void pauseCurrentVideo() {
        int currentPosition = mViewPager.getCurrentItem();
        Fragment fragment =
                getSupportFragmentManager()
                        .findFragmentByTag("f" + mVideoPagerAdapter.getItemId(currentPosition));
        if (fragment instanceof SightPlayerFragment) {
            ((SightPlayerFragment) fragment).pauseVideo();
        }
    }

    public static class VideoPagerAdapter extends FragmentStateAdapter {

        private final int mDefaultMessageId;
        List<Message> mMessages;
        boolean mFromList;
        private boolean mDisplayCurrentVideoOnly;

        public VideoPagerAdapter(@NonNull FragmentActivity fragmentActivity, int defaultMessageId) {
            super(fragmentActivity);
            mMessages = new ArrayList<>();
            mDefaultMessageId = defaultMessageId;
        }

        public void addMessage(
                List<Message> messages,
                boolean fromList,
                boolean displayCurrentVideoOnly,
                boolean direction) {
            addMessageAndReturnInsertedCount(messages, fromList, displayCurrentVideoOnly, direction);
        }

        public int addMessageAndReturnInsertedCount(
                List<Message> messages,
                boolean fromList,
                boolean displayCurrentVideoOnly,
                boolean direction) {
            this.mDisplayCurrentVideoOnly = displayCurrentVideoOnly;
            if (messages == null || messages.size() == 0) {
                return 0;
            }
            mFromList = fromList;
            List<Message> deduplication = deduplication(messages);
            if (deduplication.isEmpty()) {
                return 0;
            }
            if (direction) {
                mMessages.addAll(0, deduplication);
                notifyItemRangeInserted(0, deduplication.size());
            } else {
                int size = mMessages.size();
                mMessages.addAll(deduplication);
                notifyItemRangeInserted(size, deduplication.size());
            }
            return deduplication.size();
        }

        public List<Message> deduplication(List<Message> messages) {
            List<Message> list = new ArrayList<>();
            Set<Integer> set = new HashSet<>();
            for (Message message : mMessages) {
                set.add(message.getClientId());
            }

            for (Message message : messages) {
                if (!set.contains(message.getClientId())) {
                    list.add(message);
                    set.add(message.getClientId());
                }
            }
            return list;
        }

        private SightPlayerFragment createFragment(Message message, boolean fromList) {
            SightPlayerFragment sightPlayerFragment = new SightPlayerFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("MessageClientId", message.getClientId());
            ShortVideoPlayerActivity.putMessageToCache(message);
            bundle.putBoolean("fromList", fromList);
            bundle.putBoolean("auto_play", mDefaultMessageId == message.getClientId());
            bundle.putBoolean("displayCurrentVideoOnly", mDisplayCurrentVideoOnly);
            sightPlayerFragment.setArguments(bundle);
            return sightPlayerFragment;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return createFragment(mMessages.get(position), mFromList);
        }

        @Override
        public int getItemCount() {
            return mMessages.size();
        }

        public Message getItem(int position) {
            return mMessages.get(position);
        }

        public Message getItemByMessageId(int messageId) {
            int index = getIndexByMessageId(messageId);
            return index >= 0 ? mMessages.get(index) : null;
        }

        @Override
        public long getItemId(int position) {
            return mMessages.get(position).getClientId();
        }

        private void removeRecallItem(int messageId) {
            Iterator<Message> iterator = mMessages.iterator();
            while (iterator.hasNext()) {
                Message message = iterator.next();
                if (message.getClientId() == messageId) {
                    int index = getIndexByMessageId(messageId);
                    if (index == -1) {
                        return;
                    }
                    iterator.remove();
                    notifyItemRemoved(index);
                    break;
                }
            }
        }

        public int getIndexByMessageId(int messageId) {
            for (int i = 0; i < mMessages.size(); i++) {
                if (mMessages.get(i).getClientId() == messageId) {
                    return i;
                }
            }
            return -1;
        }
    }
}
