package ai.nexconn.chatui.shortvideo.player;

import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.DownloadMediaMessageHandler;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.ShortVideoMessage;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.event.action.DeleteEvent;
import ai.nexconn.chatui.channel.event.action.DownloadEvent;
import ai.nexconn.chatui.channel.event.action.MessageEventListener;
import ai.nexconn.chatui.utils.file.FileUtils;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.widget.CircleProgressView;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import java.io.File;
import java.lang.ref.WeakReference;

public class SightPlayerFragment extends Fragment implements EasyVideoCallback {
    private static final String TAG = "SightPlayerFragment";

    private ShortVideoMessage mSightMessage;
    private Message mMessage;
    private int mProgress;
    private ImageView mThumbImageView;
    private FrameLayout mContainer;
    private RelativeLayout rlSightDownload;
    private CircleProgressView mSightDownloadProgress;
    private RelativeLayout mSightDownloadFailedReminder;
    private TextView mCountDownView;
    private boolean mDisplayCurrentVideoOnly = false;
    private PlaybackVideoFragment mPlaybackVideoFragment;
    private SightPlayerFragment.DownloadMediaMessageCallback downloadMediaMessageCallback;
    private int currentSeek;
    private int currentPlayerStatus;
    private TextView mFailedText;
    private ImageView failedImageView;
    private boolean mPreviewDownloadStarted;
    MessageEventListener mEvent =
            new MessageEventListener() {
                @Override
                public void onDownloadMessage(DownloadEvent event) {
                    processDownloadEvent(event);
                }

                @Override
                public void onDeleteMessage(DeleteEvent event) {
                    processMessageDelete(event);
                }
            };

    private View mRootView;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.nc_fragment_sight_player, container, false);
        Bundle arguments = getArguments();
        if (arguments == null) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
            return mRootView;
        }
        try {
            int clientId = arguments.getInt("MessageClientId", -1);
            if (clientId >= 0) {
                mMessage = ShortVideoPlayerActivity.getMessageFromCache(clientId);
                if (mMessage != null && mMessage.getContent() instanceof ShortVideoMessage) {
                    mSightMessage = (ShortVideoMessage) mMessage.getContent();
                }
            }
            mProgress = arguments.getInt("Progress", 0);
            mDisplayCurrentVideoOnly = arguments.getBoolean("displayCurrentVideoOnly", false);
        } catch (Exception exception) {
            RLog.i(TAG, "getIntent exception");
        }

        mContainer = findViewById(R.id.container);
        rlSightDownload = findViewById(R.id.rl_sight_download);
        mCountDownView = findViewById(R.id.nc_count_down);
        mFailedText = findViewById(R.id.nc_sight_download_failed_tv_reminder);
        failedImageView = findViewById(R.id.nc_sight_download_failed_iv_reminder);
        mSightDownloadFailedReminder = findViewById(R.id.nc_sight_download_failed_reminder);

        downloadMediaMessageCallback = new SightPlayerFragment.DownloadMediaMessageCallback(this);
        if (savedInstanceState != null) {
            currentSeek = savedInstanceState.getInt("seek", 0);
            currentPlayerStatus = savedInstanceState.getInt("status", 0);
            int savedClientId = savedInstanceState.getInt("messageClientId", -1);
            if (savedClientId >= 0) {
                Message savedMsg = ShortVideoPlayerActivity.getMessageFromCache(savedClientId);
                if (savedMsg != null) {
                    mMessage = savedMsg;
                    if (mMessage.getContent() instanceof ShortVideoMessage) {
                        mSightMessage = (ShortVideoMessage) mMessage.getContent();
                    }
                }
            }
        }
        if (mSightMessage == null) {
            RLog.e(TAG, "onCreateView: mSightMessage=null, finish");
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
            return mRootView;
        }
        boolean localReady = isLocalVideoReady();
        if (localReady) {
            initSightPlayer();
        } else if (shouldPlayRemoteDirectly(localReady)) {
            initPreviewRemotePlayback();
        } else {
            initDownloadView();
        }
        NCChatUI.addMessageEventListener(mEvent);

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean localReady = isLocalVideoReady();
        if (!localReady) {
            if (shouldPlayRemoteDirectly(localReady)) {
                return;
            }
            if (mProgress == 0) {
                if (!downloadSight()) {
                    tryInitSightPlayerWithRemoteUrl();
                }
            }
        }
    }

    private boolean isLocalVideoReady() {
        Activity activity = getActivity();
        if (activity == null || mSightMessage == null) {
            return false;
        }
        String localPath = mSightMessage.getLocalPath();
        if (TextUtils.isEmpty(localPath)) {
            return false;
        }
        try {
            return FileUtils.isFileExistsWithUri(activity, Uri.parse(localPath));
        } catch (Exception e) {
            RLog.w(TAG, "isLocalVideoReady parse localPath failed: " + localPath);
            return false;
        }
    }

    @Override
    public void onDestroyView() {
        NCChatUI.removeMessageEventListener(mEvent);
        super.onDestroyView();
    }

    private void initDownloadView() {
        rlSightDownload.setVisibility(View.VISIBLE);
        bindThumbnailPlaceholder();
        mSightDownloadProgress = findViewById(R.id.nc_sight_download_progress);
        if (mProgress > 0) {
            showDownloadProgress(mProgress);
        } else {
            showDownloadLoading();
        }
        setupCloseButton();
    }

    private void showThumbnailPlaceholder() {
        if (rlSightDownload == null) {
            return;
        }
        rlSightDownload.setVisibility(View.VISIBLE);
        bindThumbnailPlaceholder();
        hideDownloadIndicators();
        if (mSightDownloadFailedReminder != null) {
            mSightDownloadFailedReminder.setVisibility(View.GONE);
        }
        setupCloseButton();
    }

    private void hideThumbnailPlaceholder() {
        if (rlSightDownload != null) {
            rlSightDownload.setVisibility(View.GONE);
        }
        if (mThumbImageView != null) {
            mThumbImageView.setVisibility(View.GONE);
        }
        hideDownloadIndicators();
    }

    private void bindThumbnailPlaceholder() {
        mThumbImageView = findViewById(R.id.nc_sight_thumb);
        String thumbBase64 = mSightMessage.getThumbnailBase64();
        if (thumbBase64 != null && !thumbBase64.isEmpty()) {
            try {
                byte[] bytes = android.util.Base64.decode(thumbBase64, android.util.Base64.DEFAULT);
                android.graphics.Bitmap bmp =
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp != null) {
                    mThumbImageView.setImageBitmap(bmp);
                }
            } catch (Exception e) {
                RLog.e(TAG, "initDownloadView decode thumb failed");
            }
        }
        if (mThumbImageView != null) {
            mThumbImageView.setVisibility(View.VISIBLE);
        }
    }

    private void setupCloseButton() {
        findViewById(R.id.nc_sight_download_close)
                .setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Activity activity = getActivity();
                                if (activity != null) {
                                    activity.finish();
                                }
                            }
                        });
    }

    private <T extends View> T findViewById(@IdRes int id) {
        return mRootView.findViewById(id);
    }

    private boolean downloadSight() {
        if (mMessage != null) {
            if (shouldPlayRemoteDirectly(isLocalVideoReady())) {
                showThumbnailPlaceholder();
            } else {
                showDownloadLoading();
            }
            mMessage.downloadMedia(downloadMediaMessageCallback);
            return true;
        }
        return false;
    }

    private void initSightPlayer() {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (mMessage == null || mMessage.getChannelIdentifier() == null) {
            RLog.e(TAG, "initSightPlayer failed: message or channel identifier is null");
            activity.finish();
            return;
        }
        String playUri = resolvePlayableVideoUri();
        if (TextUtils.isEmpty(playUri)) {
            RLog.e(TAG, "initSightPlayer failed: both localPath and remoteUrl are empty");
            return;
        }
        showThumbnailPlaceholder();
        mContainer.setVisibility(View.VISIBLE);
        mPlaybackVideoFragment =
                PlaybackVideoFragment.newInstance(
                        mSightMessage,
                        playUri,
                        mMessage.getChannelIdentifier().getChannelId(),
                        mMessage.getChannelIdentifier().getChannelType(),
                        getArguments().getBoolean("fromList", false),
                        mDisplayCurrentVideoOnly,
                        currentSeek,
                        currentPlayerStatus,
                        getArguments().getBoolean("auto_play", false));
        mPlaybackVideoFragment.setVideoCallback(this);
        if (mSightMessage != null && mSightMessage.isDestruct()) {
            mPlaybackVideoFragment.setplayBtnVisible(View.GONE);
            mPlaybackVideoFragment.setSeekBarClickable(false);
        }
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.container, mPlaybackVideoFragment)
                .commitAllowingStateLoss();
    }

    @Nullable
    private String resolvePlayableVideoUri() {
        if (mSightMessage == null) {
            return null;
        }
        if (isLocalVideoReady()) {
            return mSightMessage.getLocalPath();
        }
        return mSightMessage.getRemoteUrl();
    }

    private boolean isSameVideoMessage(@Nullable Message message) {
        if (mMessage == null || message == null) {
            return false;
        }
        if (message.getClientId() > 0 && message.getClientId() == mMessage.getClientId()) {
            return true;
        }
        String currentUid = mMessage.getMessageId();
        String incomingUid = message.getMessageId();
        if (!TextUtils.isEmpty(currentUid)
                && !TextUtils.isEmpty(incomingUid)
                && TextUtils.equals(currentUid, incomingUid)) {
            return true;
        }
        if (message.getContent() instanceof ShortVideoMessage && mSightMessage != null) {
            String incomingRemote = ((ShortVideoMessage) message.getContent()).getRemoteUrl();
            String currentRemote = mSightMessage.getRemoteUrl();
            return !TextUtils.isEmpty(incomingRemote)
                    && TextUtils.equals(incomingRemote, currentRemote);
        }
        return false;
    }

    private boolean tryInitSightPlayerWithRemoteUrl() {
        if (mSightMessage == null || TextUtils.isEmpty(mSightMessage.getRemoteUrl())) {
            return false;
        }
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        if (rlSightDownload != null) {
            rlSightDownload.setVisibility(View.VISIBLE);
        }
        if (mSightDownloadFailedReminder != null) {
            mSightDownloadFailedReminder.setVisibility(View.GONE);
        }
        hideDownloadIndicators();
        initSightPlayer();
        return true;
    }

    private boolean shouldPlayRemoteDirectly(boolean localReady) {
        return mMessage != null
                && mSightMessage != null
                && !mMessage.isPersisted()
                && !localReady
                && !TextUtils.isEmpty(mSightMessage.getRemoteUrl());
    }

    private void initPreviewRemotePlayback() {
        Activity activity = getActivity();
        if (activity == null || mSightMessage == null) {
            return;
        }
        if (isLocalVideoReady()) {
            initSightPlayer();
            return;
        }
        showThumbnailPlaceholder();
        if (!mPreviewDownloadStarted && downloadSight()) {
            mPreviewDownloadStarted = true;
            return;
        }
        tryInitSightPlayerWithRemoteUrl();
    }

    private void showDownloadLoading() {
        if (mSightDownloadFailedReminder != null) {
            mSightDownloadFailedReminder.setVisibility(View.GONE);
        }
        if (mSightDownloadProgress != null) {
            mSightDownloadProgress.setVisibility(View.VISIBLE);
            mSightDownloadProgress.startAnimAutomatic(true);
        }
    }

    private void showDownloadProgress(int progress) {
        if (mSightDownloadFailedReminder != null) {
            mSightDownloadFailedReminder.setVisibility(View.GONE);
        }
        if (mSightDownloadProgress != null) {
            mSightDownloadProgress.stopAnimAutomatic();
            mSightDownloadProgress.setVisibility(View.VISIBLE);
            mSightDownloadProgress.setProgress(progress, true);
        }
    }

    private void hideDownloadIndicators() {
        if (mSightDownloadProgress != null) {
            mSightDownloadProgress.stopAnimAutomatic();
            mSightDownloadProgress.setVisibility(View.GONE);
        }
    }

    public void processMessageDelete(DeleteEvent deleteEvent) {
        RLog.d(TAG, "MessageDeleteEvent");
        if (deleteEvent.getMessageIds() != null && mMessage != null) {
            for (int messageId : deleteEvent.getMessageIds()) {
                if (messageId == mMessage.getClientId()) {
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.finish();
                    }
                    break;
                }
            }
        }
    }

    public void processDownloadEvent(DownloadEvent downloadEvent) {
        RLog.d(TAG, "FileMessageEvent");
        Message ncMessage = downloadEvent.getMessage();
        if (ncMessage == null) {
            return;
        }
        RLog.e(
                TAG,
                "DownloadEvent:" + downloadEvent.getProgress() + "===" + downloadEvent.getEvent());
        if (downloadMediaMessageCallback != null && isSameVideoMessage(ncMessage)) {
            int callBackType = downloadEvent.getEvent();
            NCError ncError = downloadEvent.getCode();
            switch (callBackType) {
                case DownloadEvent.SUCCESS:
                    downloadMediaMessageCallback.onSuccess(ncMessage);
                    break;
                case DownloadEvent.ERROR:
                    downloadMediaMessageCallback.onError(
                            ncMessage, ncError != null ? ncError : new NCError(-1, null));
                    break;
                case DownloadEvent.CANCEL:
                    downloadMediaMessageCallback.onCanceled(ncMessage);
                    break;
                case DownloadEvent.PROGRESS:
                    downloadMediaMessageCallback.onProgress(ncMessage, downloadEvent.getProgress());
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onStarted(EasyVideoPlayer player) {
        hideThumbnailPlaceholder();
    }

    @Override
    public void onPaused(EasyVideoPlayer player) {
        // default implementation ignored
    }

    @Override
    public void onPreparing(EasyVideoPlayer player) {
        // default implementation ignored
    }

    @Override
    public void onPrepared(EasyVideoPlayer player) {
        hideThumbnailPlaceholder();
    }

    @Override
    public void onBuffering(int percent) {
        // default implementation ignored
    }

    @Override
    public void onError(EasyVideoPlayer player, Exception e) {
        // default implementation ignored
    }

    @Override
    public void onPlayError(final Uri source, int what, int extra) {
        RLog.d(
                TAG,
                "onPlayError: " + "source = " + source + ", what = " + what + ", extra = " + extra);
        new AlertDialog.Builder(this.getActivity())
                .setMessage(R.string.nc_video_play_error_open_system_player)
                .setPositiveButton(
                        ai.nexconn.chatui.R.string.nc_confirm,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                openExternalPlayer(source);
                                Activity activity = getActivity();
                                if (activity != null) {
                                    activity.finish();
                                }
                            }
                        })
                .setNegativeButton(
                        ai.nexconn.chatui.R.string.nc_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Activity activity = getActivity();
                                if (activity != null) {
                                    activity.finish();
                                }
                            }
                        })
                .show();
    }

    private void openExternalPlayer(Uri source) {
        try {

            Context context = getActivity();
            if (context == null) {
                return;
            }
            Uri uri = source;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    && "file".equals(source.getScheme())) {
                uri =
                        FileProvider.getUriForFile(
                                context.getApplicationContext(),
                                context.getApplicationContext().getPackageName()
                                        + context.getResources()
                                                .getString(
                                                        ai.nexconn.chatui.R.string
                                                                .nc_authorities_fileprovider),
                                new File(source.getPath()));
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(
                    uri, MimeTypeMap.getSingleton().getMimeTypeFromExtension(source.toString()));
            startActivity(intent);
        } catch (Exception e) {
            RLog.e(TAG, "onPlayError: " + "Exception = " + e);
        }
    }

    @Override
    public void onCompletion(EasyVideoPlayer player) {
        // Close video player on completion if this is a burn-after-reading message
        if (mSightMessage != null && mSightMessage.isDestruct()) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    }

    @Override
    public void onSightListRequest() {
        // default implementation ignored
    }

    @Override
    public void onClose() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    /** Pause video playback */
    public void pauseVideo() {
        if (mPlaybackVideoFragment != null) {
            mPlaybackVideoFragment.pause();
        }
    }

    public static class DownloadMediaMessageCallback implements DownloadMediaMessageHandler {
        WeakReference<SightPlayerFragment> reference;

        public DownloadMediaMessageCallback(SightPlayerFragment fragment) {
            reference = new WeakReference<>(fragment);
        }

        @Override
        public void onSuccess(@NonNull Message message) {
            SightPlayerFragment fragment = reference.get();
            if (fragment != null
                    && message != null
                    && message.getContent() instanceof ShortVideoMessage) {
                ShortVideoMessage sightMsg = (ShortVideoMessage) message.getContent();
                if (fragment.isSameVideoMessage(message)) {
                    if (fragment.getActivity() == null || fragment.getActivity().isFinishing()) {
                        return;
                    }
                    fragment.rlSightDownload.setVisibility(View.GONE);
                    if (fragment.mThumbImageView != null) {
                        fragment.mThumbImageView.setVisibility(View.GONE);
                    }
                    fragment.hideDownloadIndicators();
                    fragment.mSightMessage = sightMsg;
                    if (fragment.mMessage != null) {
                        fragment.mMessage.setContent(sightMsg);
                        ShortVideoPlayerActivity.putMessageToCache(fragment.mMessage);
                    }
                    fragment.mMessage = message;
                    fragment.initSightPlayer();
                }
            }
        }

        @Override
        public void onProgress(@NonNull Message message, int progress) {
            SightPlayerFragment fragment = reference.get();
            if (fragment != null
                    && message != null
                    && message.getContent() instanceof ShortVideoMessage) {
                if (fragment.isSameVideoMessage(message)) {
                    fragment.mProgress = progress;
                    if (fragment.shouldPlayRemoteDirectly(fragment.isLocalVideoReady())) {
                        fragment.showThumbnailPlaceholder();
                    } else if (fragment.mProgress > 0) {
                        fragment.showDownloadProgress(fragment.mProgress);
                    } else {
                        fragment.showDownloadLoading();
                    }
                }
            }
        }

        @Override
        public void onError(@NonNull Message message, @NonNull NCError error) {
            final SightPlayerFragment fragment = reference.get();
            if (fragment != null
                    && message != null
                    && message.getContent() instanceof ShortVideoMessage) {
                if (fragment.isSameVideoMessage(message)) {
                    if (fragment.tryInitSightPlayerWithRemoteUrl()) {
                        return;
                    }
                    fragment.hideDownloadIndicators();
                    fragment.mSightDownloadFailedReminder.setVisibility(View.VISIBLE);
                    fragment.mSightDownloadFailedReminder.setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    fragment.mSightDownloadFailedReminder.setVisibility(View.GONE);
                                    fragment.mProgress = 0;
                                    fragment.showDownloadLoading();
                                    fragment.downloadSight();
                                }
                            });
                    if (error.getCode() == 34020) { // NC_FILE_EXPIRED
                        fragment.failedImageView.setVisibility(View.GONE);
                        fragment.mFailedText.setText(R.string.nc_sight_file_expired);
                    } else {
                        fragment.mFailedText.setText(R.string.nc_sight_download_failed);
                    }
                }
            }
        }

        @Override
        public void onCanceled(@NonNull Message message) {
            SightPlayerFragment fragment = reference.get();
            if (fragment != null && fragment.isSameVideoMessage(message)) {
                fragment.hideDownloadIndicators();
            }
        }
    }
}
