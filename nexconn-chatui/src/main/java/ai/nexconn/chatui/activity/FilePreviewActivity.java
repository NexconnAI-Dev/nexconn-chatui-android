package ai.nexconn.chatui.activity;

import static android.widget.Toast.makeText;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.DownloadMediaMessageHandler;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.message.FileMessage;
import ai.nexconn.chat.message.MediaMessageContent;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.ReferenceMessage;
import ai.nexconn.chat.message.model.DownloadInfo;
import ai.nexconn.chat.message.model.MessageDirection;
import ai.nexconn.chat.model.ConnectionStatus;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.event.action.DownloadEvent;
import ai.nexconn.chatui.channel.event.action.MessageEventListener;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.utils.file.FileTypeUtils;
import ai.nexconn.chatui.utils.file.FileUtils;
import ai.nexconn.chatui.utils.file.LibStorageUtils;
import ai.nexconn.chatui.utils.log.RLog;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilePreviewActivity extends NCBaseActivity implements View.OnClickListener {
    public static final int NOT_DOWNLOAD = 0;
    public static final int DOWNLOADED = 1;
    public static final int DOWNLOADING = 2;
    public static final int DELETED = 3;
    public static final int DOWNLOAD_ERROR = 4;
    public static final int DOWNLOAD_CANCEL = 5;
    public static final int DOWNLOAD_SUCCESS = 6;
    public static final int DOWNLOAD_PAUSE = 7;
    public static final int REQUEST_CODE_PERMISSION = 104;
    private static final String TAG = "FilePreviewActivity";
    private static final String TXT_FILE = ".txt";
    private static final String APK_FILE = ".apk";
    private static final String FILE = "file://";
    protected FileDownloadInfo mFileDownloadInfo;
    protected FileMessage mFileMessage;
    protected Message mMessage;
    private ImageView mFileTypeImage;
    private TextView mFileNameView;
    private TextView mFileSizeView;
    private TextView mFileDownloadOpenView;
    private int mProgress;

    private String mFileName;
    private long mFileSize;
    private List<Toast> mToasts;
    private FrameLayout contentContainer;
    private long downloadedFileLength;
    private final ai.nexconn.chat.handler.MessageHandler mRecallListener =
            new ai.nexconn.chat.handler.MessageHandler() {
                @Override
                public void onMessageDeleted(
                        @androidx.annotation.NonNull
                                ai.nexconn.chat.message.model.MessageDeletedEvent event) {
                    if (mMessage == null || event.getMessages() == null) return;
                    for (ai.nexconn.chat.message.Message msg : event.getMessages()) {
                        if (msg != null && mMessage.getClientId() == msg.getClientId()) {
                            new android.app.AlertDialog.Builder(
                                            FilePreviewActivity.this,
                                            android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                                    .setMessage(getString(R.string.nc_recall_success))
                                    .setPositiveButton(
                                            getString(R.string.nc_dialog_ok),
                                            (dialog, which) -> finish())
                                    .setCancelable(false)
                                    .show();
                            return;
                        }
                    }
                }
            };
    private DownloadInfo mDownloadInfo = null;
    private boolean getInfoNow = false;
    private MessageEventListener mEventListener =
            new MessageEventListener() {
                @Override
                public void onDownloadMessage(DownloadEvent event) {
                    updateDownloadStatus(event);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.nc_ac_file_download);
        initStatusBar(ChatUIThemeManager.getAttrResId(this, R.attr.nc_common_background_color));
        initView();
        initData();
        if (mFileMessage == null || mMessage == null) {
            RLog.e(TAG, "message is null, return directly!");
            return;
        }
        initListener();
        getFileMessageStatus();
    }

    @Override
    public void setContentView(int resId) {
        contentContainer.removeAllViews();
        View view = LayoutInflater.from(this).inflate(resId, null);
        contentContainer.addView(view);
    }

    private void initView() {
        contentContainer = (FrameLayout) findViewById(R.id.nc_ac_ll_content_container);
        View view = LayoutInflater.from(this).inflate(R.layout.nc_ac_file_preview_content, null);
        contentContainer.addView(view);
        mFileTypeImage = (ImageView) findViewById(R.id.nc_ac_iv_file_type_image);
        mFileNameView = (TextView) findViewById(R.id.nc_ac_tv_file_name);
        mFileSizeView = (TextView) findViewById(R.id.nc_ac_tv_file_size);
        mFileDownloadOpenView = (TextView) findViewById(R.id.nc_ac_btn_download_button);
        mTitleBar.setTitle(R.string.nc_ac_file_download_preview);
        mTitleBar.setRightVisible(false);
    }

    private void initData() {
        Intent intent = getIntent();
        if (intent == null) {
            RLog.e(TAG, "intent is null, return directly!");
            return;
        }

        mFileDownloadInfo = new FileDownloadInfo();
        mMessage = ai.nexconn.chatui.utils.message.MessageHolder.takeMessage();
        ai.nexconn.chat.message.MessageContent heldContent =
                ai.nexconn.chatui.utils.message.MessageHolder.takeContent();
        mFileMessage = heldContent instanceof FileMessage ? (FileMessage) heldContent : null;
        mProgress = getIntent().getIntExtra("Progress", 0);

        if (mFileMessage == null && mMessage != null) {
            mFileMessage = resolvePreviewFileMessageFromMessage();
        }
        if (mFileMessage == null || mMessage == null) {
            RLog.e(TAG, "message is null, return directly!");
            return;
        }
        if (mMessage.getContent() instanceof ReferenceMessage) {
            normalizeReferenceDownloadPayload((ReferenceMessage) mMessage.getContent());
        }
        processReferenceMessageFileCache();
        mToasts = new ArrayList<>();
        mFileName = mFileMessage.getName();
        mFileTypeImage.setImageResource(FileTypeUtils.fileTypeImageId(this, mFileName));
        mFileNameView.setText(mFileName);
        mFileSize = mFileMessage.getSize();
        mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileSize));
        mFileDownloadOpenView.setOnClickListener(this);
    }

    private void initListener() {
        NCChatUI.addMessageEventListener(mEventListener);
        NCEngine.addMessageHandler("FilePreviewActivity_recall", mRecallListener);
    }

    private void processReferenceMessageFileCache() {
        if (mFileMessage.getRemoteUrl() == null) {
            return;
        }
        // If localPath already exists and the file is accessible, skip cache directory scanning
        String existingPath = mFileMessage.getLocalPath();
        if (existingPath != null && FileUtils.isFileExistsWithUri(this, Uri.parse(existingPath))) {
            return;
        }
        String name = mFileMessage.getName();
        if (TextUtils.isEmpty(name)) {
            return;
        }
        if (name.length() > 25) {
            name = name.substring(name.length() - 25);
        }
        String path = FileUtils.getMediaDownloadDir(this, LibStorageUtils.FILE);
        File parentFile = new File(path);
        if (!parentFile.exists()) {
            return;
        }
        File[] allFiles = parentFile.listFiles();
        if (allFiles == null || allFiles.length == 0) {
            return;
        }
        for (File file : allFiles) {
            if (file == null || !file.exists() || !file.isFile()) {
                continue;
            }
            // File naming rule: messageId + filename; match via endsWith and file length
            if (file.getName().endsWith(name) && file.length() == mFileMessage.getSize()) {
                String cachedLocalPath = "file://" + file.getPath();
                mFileMessage.setLocalPath(cachedLocalPath);
                if (mMessage != null && mMessage.getContent() instanceof ReferenceMessage) {
                    ReferenceMessage referenceMessage = (ReferenceMessage) mMessage.getContent();
                    referenceMessage.setLocalPath(cachedLocalPath);
                    if (referenceMessage.getReferMsg() instanceof MediaMessageContent) {
                        ((MediaMessageContent) referenceMessage.getReferMsg())
                                .setLocalPath(cachedLocalPath);
                    }
                }
                mFileDownloadInfo.path = cachedLocalPath;
                return;
            }
        }
    }

    private void getFileMessageStatus() {
        String fileUrl = mFileMessage.getRemoteUrl();
        String localPath = mFileMessage.getLocalPath();
        boolean isLocalPathExist = false;
        if (localPath != null) {
            if (FileUtils.isFileExistsWithUri(this, Uri.parse(localPath))) {
                isLocalPathExist = true;
            }
        }
        // Skip download info query for outgoing messages (SEND direction):
        // SDK may clear localPath after upload, but SEND messages should allow direct download
        // without querying progress first.
        boolean isSendDirection = mMessage.getDirection() == MessageDirection.SEND;
        if (!isLocalPathExist
                && !isSendDirection
                && fileUrl != null
                && !TextUtils.isEmpty(fileUrl)) {
            mFileDownloadOpenView.setEnabled(false);
            mFileDownloadOpenView.setText(R.string.nc_picture_please);
            mFileDownloadOpenView.setBackgroundResource(
                    R.drawable.nc_ac_btn_file_download_open_uncheck);
            getFileDownloadInfoInSubThread();
        } else {
            setViewStatus();
            getFileDownloadInfo();
        }
    }

    private void getFileDownloadInfoInSubThread() {
        getInfoNow = true;
        getFileInfo();
    }

    private void setViewStatus() {
        if (mMessage.getDirection() == MessageDirection.RECEIVE) {
            if (mProgress == 0 || mProgress == 100) {
                mFileDownloadOpenView.setVisibility(View.VISIBLE);
            } else {
                // Currently downloading, hide button (progress bar is shown)
                mFileDownloadOpenView.setVisibility(View.GONE);
            }
        } else {
            // SEND direction: always show the action button (open / download)
            mFileDownloadOpenView.setVisibility(View.VISIBLE);
        }
    }

    private void getFileDownloadInfo() {
        if (mFileMessage.getLocalPath() != null) {
            if (FileUtils.isFileExistsWithUri(this, Uri.parse(mFileMessage.getLocalPath()))) {
                mFileDownloadInfo.state = DOWNLOADED;
            } else {
                mFileDownloadInfo.state = DELETED;
            }
        } else {
            if (mProgress > 0 && mProgress < 100) {
                mFileDownloadInfo.state = DOWNLOADING;
                mFileDownloadInfo.progress = mProgress;
            } else {
                mFileDownloadInfo.state = NOT_DOWNLOAD;
            }
        }
        refreshDownloadState();
    }

    private void getFileInfo() {
        RLog.d("getDownloadInfo", "getFileInfo start");
        String mediaUrl = mFileMessage.getRemoteUrl();
        if (mediaUrl == null) {
            getInfoNow = false;
            setViewStatusForResumeTransfer();
            getFileDownloadInfoForResumeTransfer();
            mFileDownloadOpenView.setBackgroundResource(
                    R.drawable.nc_ac_btn_file_download_open_button);
            mFileDownloadOpenView.setEnabled(true);
            return;
        }
        BaseChannel.getMediaDownloadInfo(
                mediaUrl,
                new OperationHandler<DownloadInfo>() {
                    @Override
                    public void onResult(DownloadInfo downloadInfo, NCError error) {
                        mDownloadInfo = downloadInfo;
                        if (downloadInfo != null) {
                            mFileDownloadInfo.progress = downloadInfo.getProgress();
                        }
                        getInfoNow = false;
                        setViewStatusForResumeTransfer();
                        getFileDownloadInfoForResumeTransfer();
                        mFileDownloadOpenView.setBackgroundResource(
                                R.drawable.nc_ac_btn_file_download_open_button);
                        mFileDownloadOpenView.setEnabled(true);
                        RLog.d("getDownloadInfo", "getFileInfo finish");
                    }
                });
    }

    protected void refreshDownloadState() {
        switch (mFileDownloadInfo.state) {
            case NOT_DOWNLOAD:
                mFileDownloadOpenView.setText(
                        getString(R.string.nc_ac_file_preview_begin_download));
                break;
            case DOWNLOADING:
                downloadedFileLength =
                        (long)
                                (mFileMessage.getSize() * (mFileDownloadInfo.progress / 100.0)
                                        + 0.5f);
                mFileSizeView.setText(
                        getString(R.string.nc_ac_file_download_progress_tv)
                                + "("
                                + FileTypeUtils.formatFileSize(downloadedFileLength)
                                + "/"
                                + FileTypeUtils.formatFileSize(mFileSize)
                                + ")");
                mFileSizeView.setTextColor(
                        ChatUIThemeManager.getColorFromAttrId(this, R.attr.nc_primary_color));
                mFileDownloadOpenView.setText(getString(R.string.nc_cancel));
                break;
            case DOWNLOADED:
                mFileDownloadOpenView.setText(getOpenFileShowText());
                break;
            case DOWNLOAD_SUCCESS:
                //                mDownloadProgressView.setVisibility(View.GONE);
                mFileDownloadOpenView.setVisibility(View.VISIBLE);
                mFileDownloadOpenView.setText(getOpenFileShowText());
                mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileSize));
                mFileSizeView.setTextColor(
                        ChatUIThemeManager.getColorFromAttrId(
                                this, R.attr.nc_text_secondary_color));
                makeText(
                                FilePreviewActivity.this,
                                getString(R.string.nc_ac_file_preview_downloaded),
                                Toast.LENGTH_SHORT)
                        .show();
                break;
            case DOWNLOAD_ERROR:
                long downloadedFileLength =
                        (long)
                                (mFileMessage.getSize() * (mFileDownloadInfo.progress / 100.0)
                                        + 0.5f);
                mFileSizeView.setText(
                        getString(R.string.nc_ac_file_download_progress_pause)
                                + "("
                                + FileTypeUtils.formatFileSize(downloadedFileLength)
                                + "/"
                                + FileTypeUtils.formatFileSize(mFileSize)
                                + ")");
                mFileSizeView.setTextColor(
                        ChatUIThemeManager.getColorFromAttrId(this, R.attr.nc_primary_color));
                mFileDownloadOpenView.setText(
                        getString(
                                mFileDownloadInfo.progress == 0
                                        ? R.string.nc_ac_file_preview_begin_download
                                        : R.string.nc_ac_file_preview_download_resume));
                Toast toast =
                        makeText(
                                FilePreviewActivity.this,
                                getString(R.string.nc_ac_file_preview_download_error),
                                Toast.LENGTH_SHORT);
                if (mFileDownloadInfo.state != DOWNLOAD_CANCEL) {
                    toast.show();
                }
                mToasts.add(toast);
                break;
            case DOWNLOAD_CANCEL:
                //                mDownloadProgressView.setVisibility(View.GONE);
                //                mFileDownloadProgressBar.setProgress(0);
                mFileDownloadOpenView.setVisibility(View.VISIBLE);
                mFileDownloadOpenView.setText(
                        getString(R.string.nc_ac_file_preview_begin_download));
                mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileSize));
                mFileSizeView.setTextColor(
                        ChatUIThemeManager.getColorFromAttrId(
                                this, R.attr.nc_text_secondary_color));
                makeText(
                                FilePreviewActivity.this,
                                getString(R.string.nc_ac_file_preview_download_cancel),
                                Toast.LENGTH_SHORT)
                        .show();
                break;
            case DELETED:
                mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileSize));
                mFileSizeView.setTextColor(
                        ChatUIThemeManager.getColorFromAttrId(
                                this, R.attr.nc_text_secondary_color));
                mFileDownloadOpenView.setText(
                        getString(R.string.nc_ac_file_preview_begin_download));
                break;
            case DOWNLOAD_PAUSE:
                downloadedFileLength =
                        (long)
                                (mFileMessage.getSize() * (mFileDownloadInfo.progress / 100.0)
                                        + 0.5f);
                //                mFileDownloadProgressBar.setProgress(mFileDownloadInfo.progress);
                mFileSizeView.setText(
                        getString(R.string.nc_ac_file_download_progress_pause)
                                + "("
                                + FileTypeUtils.formatFileSize(downloadedFileLength)
                                + "/"
                                + FileTypeUtils.formatFileSize(mFileSize)
                                + ")");
                mFileSizeView.setTextColor(
                        ChatUIThemeManager.getColorFromAttrId(this, R.attr.nc_primary_color));
                mFileDownloadOpenView.setText(
                        getString(R.string.nc_ac_file_preview_download_resume));
                break;
            default:
                break;
        }
    }

    private String getOpenFileShowText() {
        return getString(
                mFileMessage != null
                                && mFileMessage.getLocalPath() != null
                                && isOpenInsideApp(mFileMessage.getLocalPath())
                        ? R.string.nc_ac_file_download_open_file_direct_btn
                        : R.string.nc_ac_file_download_open_file_btn);
    }

    private void setViewStatusForResumeTransfer() {
        mFileDownloadOpenView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        if (v == mFileDownloadOpenView) {
            switch (mFileDownloadInfo.state) {
                case NOT_DOWNLOAD:
                case DOWNLOAD_CANCEL:
                case DOWNLOAD_ERROR:
                case DELETED:
                    startToDownload();
                    break;
                case DOWNLOAD_SUCCESS:
                case DOWNLOADED:
                    openFile(
                            mFileName,
                            mFileMessage.getLocalPath() != null
                                    ? Uri.parse(mFileMessage.getLocalPath())
                                    : null);
                    break;
                case DOWNLOADING:
                    mFileDownloadInfo.state = DOWNLOAD_PAUSE;
                    mMessage.pauseDownloadingMedia(null);
                    downloadedFileLength =
                            (long)
                                    (mFileMessage.getSize() * (mFileDownloadInfo.progress / 100.0)
                                            + 0.5f);
                    mFileSizeView.setText(
                            getString(R.string.nc_ac_file_download_progress_pause)
                                    + "("
                                    + FileTypeUtils.formatFileSize(downloadedFileLength)
                                    + "/"
                                    + FileTypeUtils.formatFileSize(mFileSize)
                                    + ")");
                    mFileSizeView.setTextColor(
                            ChatUIThemeManager.getColorFromAttrId(this, R.attr.nc_primary_color));
                    mFileDownloadOpenView.setText(
                            getResources().getString(R.string.nc_ac_file_preview_download_resume));
                    break;
                case DOWNLOAD_PAUSE:
                    if (NCEngine.getConnectionStatus() == ConnectionStatus.NETWORK_UNAVAILABLE) {
                        makeText(
                                        FilePreviewActivity.this,
                                        getString(R.string.nc_notice_network_unavailable),
                                        Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }
                    mFileDownloadInfo.state = DOWNLOADING;
                    downloadFile();
                    if (mFileDownloadInfo.state != DOWNLOAD_ERROR
                            && mFileDownloadInfo.state != DOWNLOAD_CANCEL) {
                        mFileDownloadOpenView.setText(getResources().getString(R.string.nc_cancel));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void startToDownload() {
        MediaMessageContent mediaMessage = resolveMediaMessageForDownload();
        if (mediaMessage == null) {
            refreshDownloadState();
            return;
        }
        resetMediaMessageLocalPath();
        if (NCEngine.getConnectionStatus() != ConnectionStatus.CONNECTED) {
            makeText(
                            FilePreviewActivity.this,
                            getString(R.string.nc_notice_network_unavailable),
                            Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        if (mediaMessage != null
                && (mediaMessage.getRemoteUrl() == null
                        || TextUtils.isEmpty(mediaMessage.getRemoteUrl()))) {
            RLog.e(TAG, "startToDownload: remoteUrl is null or empty");
            makeText(
                            FilePreviewActivity.this,
                            getString(R.string.nc_ac_file_url_error),
                            Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }
        RLog.d(
                TAG,
                "startToDownload: remoteUrl="
                        + (mediaMessage != null ? mediaMessage.getRemoteUrl() : "null")
                        + ", localPath="
                        + (mediaMessage != null ? mediaMessage.getLocalPath() : "null")
                        + ", messageId="
                        + mMessage.getMessageId()
                        + ", clientId="
                        + mMessage.getClientId());
        if (mFileDownloadInfo.state == NOT_DOWNLOAD
                || mFileDownloadInfo.state == DOWNLOAD_ERROR
                || mFileDownloadInfo.state == DELETED
                || mFileDownloadInfo.state == DOWNLOAD_CANCEL) {
            downloadFile();
        }
    }

    private MediaMessageContent resolveMediaMessageForDownload() {
        if (mMessage == null) {
            return null;
        }
        MessageContent messageContent = mMessage.getContent();
        if (messageContent instanceof ReferenceMessage) {
            ReferenceMessage referenceMessage = (ReferenceMessage) messageContent;
            normalizeReferenceDownloadPayload(referenceMessage);
            if (!TextUtils.isEmpty(referenceMessage.getRemoteUrl())
                    || !TextUtils.isEmpty(referenceMessage.getLocalPath())) {
                return referenceMessage;
            }
            if (mFileMessage != null
                    && (!TextUtils.isEmpty(mFileMessage.getRemoteUrl())
                            || !TextUtils.isEmpty(mFileMessage.getLocalPath()))) {
                // Fallback: if reference envelope lacks media fields, use referenced file directly.
                mMessage.setContent(mFileMessage);
                return mFileMessage;
            }
            return null;
        }
        if (messageContent instanceof MediaMessageContent) {
            MediaMessageContent mediaMessage = (MediaMessageContent) messageContent;
            syncMediaMessageFields(mediaMessage, mFileMessage);
            return mediaMessage;
        }
        if (mFileMessage != null && !TextUtils.isEmpty(mFileMessage.getRemoteUrl())) {
            mMessage.setContent(mFileMessage);
            return mFileMessage;
        }
        return null;
    }

    private FileMessage resolvePreviewFileMessageFromMessage() {
        if (mMessage == null) {
            return null;
        }
        MessageContent content = mMessage.getContent();
        if (content instanceof FileMessage) {
            return (FileMessage) content;
        }
        if (content instanceof ReferenceMessage) {
            MessageContent referMsg = ((ReferenceMessage) content).getReferMsg();
            if (referMsg instanceof FileMessage) {
                return (FileMessage) referMsg;
            }
        }
        return null;
    }

    private void normalizeReferenceDownloadPayload(ReferenceMessage referenceMessage) {
        if (referenceMessage == null) {
            return;
        }
        MediaMessageContent referMedia = null;
        if (referenceMessage.getReferMsg() instanceof MediaMessageContent) {
            referMedia = (MediaMessageContent) referenceMessage.getReferMsg();
        } else if (mFileMessage != null) {
            referenceMessage.setReferMsg(mFileMessage);
            referMedia = mFileMessage;
        }

        if (referMedia != null) {
            syncMediaMessageFields(referMedia, mFileMessage);
            syncMediaMessageFields(mFileMessage, referMedia);
            syncMediaMessageFields(referenceMessage, referMedia);
            syncMediaMessageFields(referMedia, referenceMessage);
        } else {
            syncMediaMessageFields(referenceMessage, mFileMessage);
        }

        if (TextUtils.isEmpty(referenceMessage.getReferMsgSenderId())
                && mMessage != null
                && !TextUtils.isEmpty(mMessage.getSenderUserId())) {
            // Download conversion requires a non-empty referMsgSenderId for ReferenceMessage.
            referenceMessage.setReferMsgSenderId(mMessage.getSenderUserId());
        }
    }

    private void syncMediaMessageFields(MediaMessageContent target, MediaMessageContent source) {
        if (target == null || source == null || target == source) {
            return;
        }
        if (TextUtils.isEmpty(target.getRemoteUrl()) && !TextUtils.isEmpty(source.getRemoteUrl())) {
            target.setRemoteUrl(source.getRemoteUrl());
        }
        if (TextUtils.isEmpty(target.getLocalPath()) && !TextUtils.isEmpty(source.getLocalPath())) {
            target.setLocalPath(source.getLocalPath());
        }
        if (TextUtils.isEmpty(target.getName()) && !TextUtils.isEmpty(source.getName())) {
            target.setName(source.getName());
        }
        if (target instanceof FileMessage && source instanceof FileMessage) {
            FileMessage targetFile = (FileMessage) target;
            FileMessage sourceFile = (FileMessage) source;
            if (targetFile.getSize() <= 0 && sourceFile.getSize() > 0) {
                targetFile.setSize(sourceFile.getSize());
            }
            if (TextUtils.isEmpty(targetFile.getFileType())
                    && !TextUtils.isEmpty(sourceFile.getFileType())) {
                targetFile.setFileType(sourceFile.getFileType());
            }
        }
    }

    public void openFile(String fileName, Uri fileSavePath) {
        try {
            if (!openInsidePreview(fileName, fileSavePath)) {
                Intent intent = FileTypeUtils.getOpenFileIntent(this, fileName, fileSavePath);

                if (intent != null) {
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } else {
                    makeText(
                                    FilePreviewActivity.this,
                                    getString(R.string.nc_ac_file_preview_can_not_open_file),
                                    Toast.LENGTH_SHORT)
                            .show();
                }
            }
        } catch (Exception e) {
            RLog.e(TAG, "openFile" + e.getMessage());
            makeText(
                            FilePreviewActivity.this,
                            getString(R.string.nc_ac_file_preview_can_not_open_file),
                            Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void downloadFile() {
        // Downloads to app-private directory, no storage permission needed
        mFileDownloadInfo.state = DOWNLOADING;
        mFileDownloadOpenView.setText(getResources().getString(R.string.nc_cancel));
        downloadedFileLength =
                (long) (mFileMessage.getSize() * (mFileDownloadInfo.progress / 100.0) + 0.5f);
        mFileSizeView.setText(
                getString(R.string.nc_ac_file_download_progress_tv)
                        + "("
                        + FileTypeUtils.formatFileSize(downloadedFileLength)
                        + "/"
                        + FileTypeUtils.formatFileSize(mFileSize)
                        + ")");
        mFileSizeView.setTextColor(
                ChatUIThemeManager.getColorFromAttrId(this, R.attr.nc_primary_color));
        mMessage.downloadMedia(
                new DownloadMediaMessageHandler() {
                    @Override
                    public void onSuccess(Message message) {
                        runOnUiThread(
                                () -> {
                                    if (mFileDownloadInfo.state != DOWNLOAD_CANCEL) {
                                        applyDownloadedMessage(message);
                                        mFileDownloadInfo.state = DOWNLOAD_SUCCESS;
                                        refreshDownloadState();
                                    }
                                });
                    }

                    @Override
                    public void onProgress(Message message, int progress) {
                        runOnUiThread(
                                () -> {
                                    if (mFileDownloadInfo.state != DOWNLOAD_CANCEL
                                            && mFileDownloadInfo.state != DOWNLOAD_PAUSE) {
                                        mFileDownloadInfo.state = DOWNLOADING;
                                        mFileDownloadInfo.progress = progress;
                                        refreshDownloadState();
                                    }
                                });
                    }

                    @Override
                    public void onError(Message message, NCError error) {
                        runOnUiThread(
                                () -> {
                                    if (mFileDownloadInfo.state != DOWNLOAD_CANCEL) {
                                        RLog.e(
                                                TAG,
                                                "downloadFile onError, code="
                                                        + (error != null ? error.getCode() : -1)
                                                        + ", message="
                                                        + (error != null
                                                                ? error.getMessage()
                                                                : "null"));
                                        mFileDownloadInfo.state = DOWNLOAD_ERROR;
                                        refreshDownloadState();
                                    }
                                });
                    }

                    @Override
                    public void onCanceled(Message message) {
                        runOnUiThread(
                                () -> {
                                    mFileDownloadInfo.state = DOWNLOAD_CANCEL;
                                    refreshDownloadState();
                                });
                    }
                });
    }

    protected void resetMediaMessageLocalPath() {
        MediaMessageContent mediaMessage = null;
        if (mMessage.getContent() instanceof MediaMessageContent) {
            mediaMessage = (MediaMessageContent) mMessage.getContent();
        } else if (mMessage.getContent() instanceof ReferenceMessage) {
            ReferenceMessage referenceMessage = (ReferenceMessage) mMessage.getContent();
            if (referenceMessage.getReferMsg() instanceof MediaMessageContent) {
                mediaMessage = (MediaMessageContent) referenceMessage.getReferMsg();
            }
        }

        if (mediaMessage != null) {
            if (mediaMessage.getLocalPath() != null
                    && !TextUtils.isEmpty(mediaMessage.getLocalPath())) {
                mediaMessage.setLocalPath(null);
                if (mFileMessage != null) {
                    mFileMessage.setLocalPath(null);
                }
                // TODO: notify message update via nexconn when API is available
            }
        }
    }

    protected boolean openInsidePreview(String fileName, Uri uri) {
        String fileSavePath = uri.toString();
        if (isOpenInsideApp(fileSavePath)) {
            processTxtFile(fileName, uri);
            return true;
        }
        return false;
    }

    private boolean isOpenInsideApp(String fileSavePath) {
        return fileSavePath != null && fileSavePath.endsWith(TXT_FILE);
    }

    /**
     * Handles opening a text file in the in-app WebView.
     *
     * @param fileName the file name
     * @param uri the file Uri
     */
    private void processTxtFile(String fileName, Uri uri) {
        Intent webIntent = new Intent(this, NCWebviewActivity.class);
        webIntent.setPackage(getPackageName());
        // If content:// URI
        if (FileUtils.uriStartWithContent(uri)) {
            webIntent.putExtra("url", uri.toString());
        } else {
            // file:// URI
            String path = uri.toString();
            if (FileUtils.uriStartWithFile(uri)) {
                path = path.substring(7);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri txtUri =
                        FileProvider.getUriForFile(
                                this,
                                this.getApplicationContext().getPackageName()
                                        + getResources()
                                                .getString(R.string.nc_authorities_fileprovider),
                                new File(path));
                webIntent.putExtra("url", txtUri.toString());
            } else {
                webIntent.putExtra("url", FILE + path);
            }
        }
        webIntent.putExtra("title", fileName);
        startActivity(webIntent);
    }

    private void getFileDownloadInfoForResumeTransfer() {
        if (mFileMessage != null) {
            String path = mFileMessage.getLocalPath();
            if (path != null) {
                boolean exists =
                        FileUtils.isFileExistsWithUri(FilePreviewActivity.this, Uri.parse(path));
                if (exists) {
                    mFileDownloadInfo.state = DOWNLOADED;
                } else {
                    mFileDownloadInfo.state = DELETED;
                }
            } else if (mDownloadInfo != null) {
                if (mDownloadInfo.isDownloading()) {
                    mFileDownloadInfo.state = DOWNLOADING;
                } else {
                    mFileDownloadInfo.state = DOWNLOAD_PAUSE;
                }
            } else {
                mFileDownloadInfo.state = NOT_DOWNLOAD;
            }
        } else {
            mFileDownloadInfo.state = NOT_DOWNLOAD;
        }
        refreshDownloadState();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        getFileDownloadInfoInSubThread();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        try {
            for (Toast toast : mToasts) {
                toast.cancel();
            }
        } catch (Exception e) {
            RLog.e(TAG, "onDestroy" + e.getMessage());
        }
        NCChatUI.removeMessageEventListener(mEventListener);
        NCEngine.removeMessageHandler("FilePreviewActivity_recall");
        super.onDestroy();
    }

    public void updateDownloadStatus(DownloadEvent event) {
        if (mMessage.getClientId() == event.getMessage().getClientId()) {
            switch (event.getEvent()) {
                case DownloadEvent.SUCCESS:
                    if (mFileDownloadInfo.state != DOWNLOAD_CANCEL) {
                        if (event.getMessage() == null || event.getMessage().getContent() == null)
                            return;
                        applyDownloadedMessage(event.getMessage());
                        mFileDownloadInfo.state = DOWNLOAD_SUCCESS;
                        refreshDownloadState();
                    }
                    break;
                case DownloadEvent.PROGRESS:
                    if (mDownloadInfo == null && !getInfoNow) {
                        getFileDownloadInfoInSubThread();
                    }
                    if (mFileDownloadInfo.state != DOWNLOAD_CANCEL
                            && mFileDownloadInfo.state != DOWNLOAD_PAUSE) {
                        mFileDownloadInfo.state = DOWNLOADING;
                        mFileDownloadInfo.progress = event.getProgress();
                        refreshDownloadState();
                    }
                    break;
                case DownloadEvent.ERROR:
                    if (mFileDownloadInfo.state != DOWNLOAD_CANCEL) {
                        mFileDownloadInfo.state = DOWNLOAD_ERROR;
                        refreshDownloadState();
                    }
                    break;
                case DownloadEvent.CANCEL:
                    mFileDownloadInfo.state = DOWNLOAD_CANCEL;
                    refreshDownloadState();
                    break;
            }
        }
    }

    private void applyDownloadedMessage(Message downloadedMessage) {
        if (downloadedMessage == null || downloadedMessage.getContent() == null) {
            return;
        }
        MessageContent downloadedContent = downloadedMessage.getContent();
        if (downloadedContent instanceof FileMessage) {
            mFileMessage = (FileMessage) downloadedContent;
            mFileDownloadInfo.path = mFileMessage.getLocalPath();
            if (mMessage != null) {
                mMessage.setContent(mFileMessage);
            }
            return;
        }
        if (!(downloadedContent instanceof ReferenceMessage)) {
            return;
        }
        ReferenceMessage downloadedReference = (ReferenceMessage) downloadedContent;
        syncReferenceDownloadPath(downloadedReference);
        if (mMessage != null && mMessage.getContent() instanceof ReferenceMessage) {
            ReferenceMessage currentReference = (ReferenceMessage) mMessage.getContent();
            if (TextUtils.isEmpty(currentReference.getLocalPath())) {
                currentReference.setLocalPath(downloadedReference.getLocalPath());
            }
            syncReferenceDownloadPath(currentReference);
        } else if (mMessage != null) {
            mMessage.setContent(downloadedReference);
        }
    }

    private void syncReferenceDownloadPath(ReferenceMessage referenceMessage) {
        if (referenceMessage == null) {
            return;
        }
        String localPath = referenceMessage.getLocalPath();
        if (TextUtils.isEmpty(localPath)
                && referenceMessage.getReferMsg() instanceof MediaMessageContent) {
            localPath = ((MediaMessageContent) referenceMessage.getReferMsg()).getLocalPath();
        }
        if (TextUtils.isEmpty(localPath)) {
            return;
        }
        referenceMessage.setLocalPath(localPath);
        if (referenceMessage.getReferMsg() instanceof MediaMessageContent) {
            ((MediaMessageContent) referenceMessage.getReferMsg()).setLocalPath(localPath);
        }
        if (mFileMessage != null) {
            mFileMessage.setLocalPath(localPath);
        }
        mFileDownloadInfo.path = localPath;
    }

    public Message getMessage() {
        return mMessage;
    }

    public class FileDownloadInfo {
        public int state;
        public int progress;
        public String path;
    }
}
