package ai.nexconn.chatui.activity;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.model.ConnectionStatus;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.utils.common.ChatUIUtils;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.file.FileTypeUtils;
import ai.nexconn.chatui.utils.file.FileUtils;
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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WebFilePreviewActivity extends NCBaseActivity implements View.OnClickListener {
    public static final int NOT_DOWNLOAD = 0;
    public static final int DOWNLOADED = 1;
    public static final int DOWNLOADING = 2;
    public static final int DELETED = 3;
    public static final int DOWNLOAD_ERROR = 4;
    public static final int DOWNLOAD_CANCEL = 5;
    public static final int DOWNLOAD_SUCCESS = 6;
    public static final int DOWNLOAD_PAUSE = 7;
    public static final int REQUEST_CODE_PERMISSION = 104;
    private static final String TAG = "WebFilePreviewActivity";
    private static final String PATH = "webfile";
    private static final String TXT_FILE = ".txt";
    private static final String APK_FILE = ".apk";
    private static final String FILE = "file://";
    //    private ProgressBar mFileDownloadProgressBar;
    //    private LinearLayout mDownloadProgressView;
    //    protected TextView mDownloadProgressTextView;
    protected View mCancel;
    protected FileDownloadInfo mFileDownloadInfo;
    private ImageView mFileTypeImage;
    private TextView mFileNameView;
    private TextView mFileSizeView;
    private TextView mFileDownloadOpenView;
    private File mAttachFile;
    private FrameLayout mContentContainer;
    private SupportResumeStatus supportResumeTransfer = SupportResumeStatus.NOT_SET;
    private String pausedPath;
    private long downloadedFileLength;
    private String savedPath;
    private final ExecutorService mDownloadExecutor = Executors.newSingleThreadExecutor();
    private Future<?> mDownloadFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.nc_ac_file_download);
        initStatusBar(R.color.app_color_white);
        initView();
        initData();
    }

    @Override
    public void setContentView(int resId) {
        mContentContainer.removeAllViews();
        mContentContainer.addView(LayoutInflater.from(this).inflate(resId, null));
    }

    private void initView() {
        mContentContainer = findViewById(R.id.nc_ac_ll_content_container);
        View view = LayoutInflater.from(this).inflate(R.layout.nc_ac_file_preview_content, null);
        mContentContainer.addView(view);

        mFileTypeImage = findViewById(R.id.nc_ac_iv_file_type_image);
        mFileNameView = findViewById(R.id.nc_ac_tv_file_name);
        mFileSizeView = findViewById(R.id.nc_ac_tv_file_size);
        mFileDownloadOpenView = findViewById(R.id.nc_ac_btn_download_button);
        mTitleBar.setTitle(R.string.nc_ac_file_download_preview);
        mTitleBar.setRightVisible(false);
    }

    private void initData() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        mFileDownloadInfo = new FileDownloadInfo();
        mFileDownloadInfo.url = intent.getStringExtra("fileUrl");
        mFileDownloadInfo.fileName = intent.getStringExtra("fileName");
        try {
            mFileDownloadInfo.size = Long.parseLong(intent.getStringExtra("fileSize"));
        } catch (NumberFormatException e) {
            RLog.e(TAG, "NumberFormatException, default value is 0L");
            mFileDownloadInfo.size = 0L;
        }

        mFileDownloadInfo.uid =
                ChatUIUtils.md5(getFileNameFromDownloadUrl() + mFileDownloadInfo.size);
        mFileDownloadInfo.path = FileUtils.getCachePath(this, PATH);

        mFileTypeImage.setImageResource(
                FileTypeUtils.fileTypeImageId(this, mFileDownloadInfo.fileName));
        mFileNameView.setText(mFileDownloadInfo.fileName);
        mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileDownloadInfo.size));
        mFileDownloadOpenView.setOnClickListener(this);
        savedPath = mFileDownloadInfo.path + "/" + getFileNameFromDownloadUrl();
        mAttachFile = new File(savedPath);
        if (isAttachFileExists()) {
            mFileDownloadOpenView.setText(getOpenFileShowText());
        }
        supportResumeTransfer = SupportResumeStatus.NOT_SUPPORT;
        getFileDownloadInfo();
    }

    private String getFileNameFromDownloadUrl() {
        return FileUtils.getUrlFileName(mFileDownloadInfo.url, mFileDownloadInfo.fileName);
    }

    private String getOpenFileShowText() {
        return getString(
                mFileDownloadInfo != null && isOpenInsideApp(mFileDownloadInfo.fileName)
                        ? R.string.nc_ac_file_download_open_file_direct_btn
                        : R.string.nc_ac_file_download_open_file_btn);
    }

    private void getFileDownloadInfo() {
        pausedPath = FileUtils.getTempFilePath(this, mFileDownloadInfo.uid);
        mFileDownloadInfo.state = isAttachFileExists() ? DOWNLOADED : NOT_DOWNLOAD;
        refreshDownloadState();
    }

    protected void refreshDownloadState() {
        switch (mFileDownloadInfo.state) {
            case NOT_DOWNLOAD:
                mFileDownloadOpenView.setText(R.string.nc_ac_file_preview_begin_download);
                break;
            case DOWNLOADING:
                downloadedFileLength = getDownloadedFileLength();
                mFileSizeView.setText(
                        getString(R.string.nc_ac_file_download_progress_tv)
                                + "("
                                + FileTypeUtils.formatFileSize(downloadedFileLength)
                                + "/"
                                + FileTypeUtils.formatFileSize(mFileDownloadInfo.size)
                                + ")");
                if (supportResumeTransfer == SupportResumeStatus.SUPPORT) {
                    mFileDownloadOpenView.setText(R.string.nc_cancel);
                } else {
                    mFileDownloadOpenView.setVisibility(View.GONE);
                }
                break;
            case DOWNLOADED:
                mFileDownloadOpenView.setText(getOpenFileShowText());
                break;
            case DOWNLOAD_SUCCESS:
                mFileDownloadOpenView.setVisibility(View.VISIBLE);
                mFileDownloadOpenView.setText(getOpenFileShowText());
                mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileDownloadInfo.size));
                String text =
                        getString(R.string.nc_ac_file_preview_downloaded) + mFileDownloadInfo.path;
                ToastUtils.show(WebFilePreviewActivity.this, text, Toast.LENGTH_SHORT);
                break;
            case DOWNLOAD_ERROR:
                if (supportResumeTransfer == SupportResumeStatus.SUPPORT) {
                    long downloadedFileLength = getDownloadedFileLength();
                    mFileSizeView.setText(
                            getString(R.string.nc_ac_file_download_progress_pause)
                                    + "("
                                    + FileTypeUtils.formatFileSize(downloadedFileLength)
                                    + "/"
                                    + FileTypeUtils.formatFileSize(mFileDownloadInfo.size)
                                    + ")");
                    mFileDownloadOpenView.setText(R.string.nc_ac_file_preview_download_resume);
                } else {
                    mFileDownloadOpenView.setVisibility(View.VISIBLE);
                    mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileDownloadInfo.size));
                    mFileDownloadOpenView.setText(R.string.nc_ac_file_preview_begin_download);
                }
                text = getString(R.string.nc_ac_file_preview_download_error);
                ToastUtils.show(WebFilePreviewActivity.this, text, Toast.LENGTH_SHORT);
                break;
            case DOWNLOAD_CANCEL:
                mFileDownloadOpenView.setVisibility(View.VISIBLE);
                mFileDownloadOpenView.setText(R.string.nc_ac_file_preview_begin_download);
                mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileDownloadInfo.size));
                text = getString(R.string.nc_ac_file_preview_download_cancel);
                ToastUtils.show(WebFilePreviewActivity.this, text, Toast.LENGTH_SHORT);
                break;
            case DELETED:
                mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileDownloadInfo.size));
                mFileDownloadOpenView.setText(R.string.nc_ac_file_preview_begin_download);
                break;
            case DOWNLOAD_PAUSE:
                downloadedFileLength = getDownloadedFileLength();
                mFileSizeView.setText(
                        getString(R.string.nc_ac_file_download_progress_pause)
                                + "("
                                + FileTypeUtils.formatFileSize(downloadedFileLength)
                                + "/"
                                + FileTypeUtils.formatFileSize(mFileDownloadInfo.size)
                                + ")");
                mFileDownloadOpenView.setText(R.string.nc_ac_file_preview_download_resume);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDownloadFuture != null) {
            mDownloadFuture.cancel(true);
        }
        mDownloadExecutor.shutdownNow();
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
                    if (mAttachFile != null) {
                        openFile(mFileDownloadInfo.fileName, mAttachFile.getAbsolutePath());
                    }
                    break;
                case DOWNLOADING:
                    mFileDownloadInfo.state = DOWNLOAD_PAUSE;
                    if (mDownloadFuture != null) {
                        mDownloadFuture.cancel(true);
                    }
                    mFileDownloadOpenView.setText(R.string.nc_ac_file_preview_download_resume);
                    downloadedFileLength = getDownloadedFileLength();
                    mFileSizeView.setText(
                            getString(R.string.nc_ac_file_download_progress_pause)
                                    + "("
                                    + FileTypeUtils.formatFileSize(downloadedFileLength)
                                    + "/"
                                    + FileTypeUtils.formatFileSize(mFileDownloadInfo.size)
                                    + ")");

                    break;
                case DOWNLOAD_PAUSE:
                    if (NCEngine.getConnectionStatus() == ConnectionStatus.NETWORK_UNAVAILABLE) {
                        String text = getString(R.string.nc_notice_network_unavailable);
                        ToastUtils.show(WebFilePreviewActivity.this, text, Toast.LENGTH_SHORT);
                        return;
                    }
                    if (supportResumeTransfer == SupportResumeStatus.SUPPORT) {
                        mFileDownloadInfo.state = DOWNLOADING;
                        downloadFile();
                        if (mFileDownloadInfo.state != DOWNLOAD_ERROR
                                && mFileDownloadInfo.state != DOWNLOAD_CANCEL) {
                            mFileDownloadOpenView.setText(R.string.nc_cancel);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void startToDownload() {
        if (NCEngine.getConnectionStatus() == ConnectionStatus.NETWORK_UNAVAILABLE) {
            String text = getString(R.string.nc_notice_network_unavailable);
            ToastUtils.show(WebFilePreviewActivity.this, text, Toast.LENGTH_SHORT);
            return;
        }

        if (supportResumeTransfer == SupportResumeStatus.NOT_SET) {
            supportResumeTransfer = SupportResumeStatus.NOT_SUPPORT;
            downloadFile();
        } else {
            if (mFileDownloadInfo.state == NOT_DOWNLOAD
                    || mFileDownloadInfo.state == DOWNLOAD_ERROR
                    || mFileDownloadInfo.state == DELETED
                    || mFileDownloadInfo.state == DOWNLOAD_CANCEL) {
                downloadFile();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void downloadFile() {
        mFileDownloadInfo.state = DOWNLOADING;
        mFileDownloadOpenView.setVisibility(View.GONE);

        String fileUrl = mFileDownloadInfo.url;
        String destPath = savedPath;
        mDownloadFuture =
                mDownloadExecutor.submit(
                        () -> {
                            HttpURLConnection connection = null;
                            InputStream input = null;
                            FileOutputStream output = null;
                            try {
                                File dir = new File(mFileDownloadInfo.path);
                                if (!dir.exists()) {
                                    dir.mkdirs();
                                }
                                connection = (HttpURLConnection) new URL(fileUrl).openConnection();
                                connection.setConnectTimeout(15000);
                                connection.setReadTimeout(30000);
                                connection.connect();
                                int responseCode = connection.getResponseCode();
                                if (responseCode != HttpURLConnection.HTTP_OK) {
                                    notifyDownloadError();
                                    return;
                                }
                                long contentLength = connection.getContentLength();
                                if (contentLength <= 0 && mFileDownloadInfo.size > 0) {
                                    contentLength = mFileDownloadInfo.size;
                                }
                                input = connection.getInputStream();
                                output = new FileOutputStream(new File(destPath));
                                byte[] buffer = new byte[8192];
                                long downloaded = 0;
                                int read;
                                while ((read = input.read(buffer)) != -1) {
                                    if (Thread.currentThread().isInterrupted()) {
                                        notifyDownloadPaused();
                                        return;
                                    }
                                    output.write(buffer, 0, read);
                                    downloaded += read;
                                    if (contentLength > 0) {
                                        int progress = (int) (downloaded * 100 / contentLength);
                                        notifyProgress(progress);
                                    }
                                }
                                output.flush();
                                notifyDownloadSuccess(destPath);
                            } catch (Exception e) {
                                if (Thread.currentThread().isInterrupted()) {
                                    notifyDownloadPaused();
                                } else {
                                    RLog.e(TAG, "downloadFile error", e);
                                    notifyDownloadError();
                                }
                            } finally {
                                try {
                                    if (input != null) input.close();
                                    if (output != null) output.close();
                                    if (connection != null) connection.disconnect();
                                } catch (Exception ignored) {
                                }
                            }
                        });
    }

    private void notifyProgress(int progress) {
        runOnUiThread(
                () -> {
                    if (mFileDownloadInfo.state == DOWNLOADING) {
                        mFileDownloadInfo.progress = progress;
                        refreshDownloadState();
                    }
                });
    }

    private void notifyDownloadSuccess(String filePath) {
        runOnUiThread(
                () -> {
                    if (mFileDownloadInfo.state != DOWNLOAD_CANCEL) {
                        mFileDownloadInfo.path = new File(filePath).getParent();
                        mAttachFile = new File(filePath);
                        mFileDownloadInfo.state = DOWNLOAD_SUCCESS;
                        refreshDownloadState();
                    }
                });
    }

    private void notifyDownloadError() {
        runOnUiThread(
                () -> {
                    if (mFileDownloadInfo.state != DOWNLOAD_CANCEL) {
                        mFileDownloadInfo.state = DOWNLOAD_ERROR;
                        refreshDownloadState();
                    }
                });
    }

    private void notifyDownloadPaused() {
        runOnUiThread(
                () -> {
                    mFileDownloadInfo.state = DOWNLOAD_PAUSE;
                    refreshDownloadState();
                });
    }

    public void openFile(String fileName, String fileSavePath) {
        if (!openInsidePreview(fileName, fileSavePath)) {
            Intent intent = FileTypeUtils.getOpenFileIntent(this, fileName, fileSavePath);
            try {
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } else {
                    String text = getString(R.string.nc_ac_file_preview_can_not_open_file);
                    ToastUtils.show(WebFilePreviewActivity.this, text, Toast.LENGTH_SHORT);
                }
            } catch (Exception e) {
                String text = getString(R.string.nc_ac_file_preview_can_not_open_file);
                ToastUtils.show(WebFilePreviewActivity.this, text, Toast.LENGTH_SHORT);
            }
        }
    }

    protected boolean openInsidePreview(String fileName, String fileSavePath) {
        if (isOpenInsideApp(fileSavePath)) {
            Intent webIntent = new Intent(this, NCWebviewActivity.class);
            webIntent.setPackage(getPackageName());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri uri =
                        FileProvider.getUriForFile(
                                this,
                                getPackageName() + getString(R.string.nc_authorities_fileprovider),
                                new File(fileSavePath));
                webIntent.putExtra("url", uri.toString());
            } else {
                webIntent.putExtra("url", FILE + fileSavePath);
            }
            webIntent.putExtra("title", fileName);
            startActivity(webIntent);
            return true;
        }
        return false;
    }

    private boolean isOpenInsideApp(String fileSavePath) {
        return fileSavePath != null && fileSavePath.endsWith(TXT_FILE);
    }

    // Downloaded file length
    private long getDownloadedFileLength() {
        return (long) (mFileDownloadInfo.size * (mFileDownloadInfo.progress / 100.0) + 0.5f);
    }

    private boolean isPartAttachFileExists() {
        // For chunked downloads, files are saved as filePath + "_" + chunkIndex
        String partSavedPath = savedPath + "_0";
        File mPartAttachFile = new File(partSavedPath);
        return mPartAttachFile.exists();
    }

    private boolean isAttachFileExists() {
        return mAttachFile.exists();
    }

    private boolean isPauseFileExists() {
        return !TextUtils.isEmpty(pausedPath) && new File(pausedPath).exists();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        getFileDownloadInfo();
    }

    private enum SupportResumeStatus {
        NOT_SET(-1),
        NOT_SUPPORT(0),
        SUPPORT(1);

        int value;

        SupportResumeStatus(int value) {
            this.value = value;
        }

        public static SupportResumeStatus valueOf(int code) {
            for (SupportResumeStatus c : SupportResumeStatus.values()) {
                if (code == c.getValue()) {
                    return c;
                }
            }
            SupportResumeStatus c = NOT_SET;
            c.value = code;
            return c;
        }

        public int getValue() {
            return this.value;
        }
    }

    private class FileDownloadInfo {
        int state;
        int progress;
        String path;
        String fileName;
        String url;
        String uid;
        long size;
    }
}
