package ai.nexconn.chatui.activity;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.handler.DownloadMediaMessageHandler;
import ai.nexconn.chat.handler.MessageHandler;
import ai.nexconn.chat.message.GIFMessage;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.model.MessageDeletedEvent;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.event.action.DeleteEvent;
import ai.nexconn.chatui.channel.event.action.MessageEventListener;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.utils.common.ChatUIUtils;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.constant.AndroidConstant;
import ai.nexconn.chatui.utils.file.ChatUIStorageUtils;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.permission.PermissionCheckUtil;
import ai.nexconn.chatui.widget.dialog.OptionsPopupDialog;
import android.Manifest;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class GIFPreviewActivity extends NCBaseNoActionbarActivity {
    TextView mCountDownView;
    TextView mFailedTxt;
    Message currentMessage;
    private static final String TAG = "GIFPreviewActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nc_gif_preview);
        mCountDownView = findViewById(R.id.nc_count_down);
        mCountDownView.setBackgroundResource(R.drawable.nc_lively_common_background);
        mCountDownView.setTextColor(
                mCountDownView
                        .getContext()
                        .getResources()
                        .getColor(
                                ChatUIThemeManager.getAttrResId(
                                        mCountDownView.getContext(), R.attr.nc_hint_color)));
        mFailedTxt = findViewById(R.id.nc_gif_txt);
        final ImageView gifPreview = findViewById(R.id.nc_gif_preview);
        currentMessage = ai.nexconn.chatui.utils.message.MessageHolder.takeMessage();
        if (currentMessage == null
                || currentMessage.getContent() == null
                || !(currentMessage.getContent() instanceof GIFMessage)) {
            finish();
            return;
        }

        gifPreview.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        GIFMessage gifMessage = (GIFMessage) currentMessage.getContent();
                        if (!gifMessage.isDestruct()) {
                            saveGif(gifMessage);
                        }
                        return true;
                    }
                });

        gifPreview.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Window window = GIFPreviewActivity.this.getWindow();
                        if (window != null) {
                            int flag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
                            window.setFlags(flag, flag);
                        }
                        finish();
                    }
                });

        GIFMessage gifMessage = (GIFMessage) currentMessage.getContent();

        if (isGifLocalReady(gifMessage)) {
            loadGif(gifPreview, gifMessage);
        } else if (!TextUtils.isEmpty(gifMessage.getRemoteUrl())
                && !currentMessage.isPersisted()) {
            loadGifFromUrl(gifPreview, gifMessage.getRemoteUrl());
        } else {
            currentMessage.downloadMedia(
                    new DownloadMediaMessageHandler() {
                        @Override
                        public void onSuccess(@NonNull Message message) {
                            if (message.getContent() instanceof GIFMessage) {
                                currentMessage = message;
                                loadGif(gifPreview, (GIFMessage) message.getContent());
                            }
                        }

                        @Override
                        public void onProgress(@NonNull Message message, int progress) {}

                        @Override
                        public void onError(
                                @NonNull Message message,
                                @NonNull ai.nexconn.chat.error.NCError error) {
                            GIFMessage msg = (GIFMessage) currentMessage.getContent();
                            if (msg != null && !TextUtils.isEmpty(msg.getRemoteUrl())) {
                                loadGifFromUrl(gifPreview, msg.getRemoteUrl());
                            }
                        }

                        @Override
                        public void onCanceled(@NonNull Message message) {}
                    });
        }

        NCChatUI.addMessageEventListener(mBaseMessageEvent);
        NCEngine.addMessageHandler("GIFPreviewActivity", mRecallMessageHandler);
    }

    private void loadGif(ImageView gifPreview, GIFMessage gifMessage) {
        if (ChatUIUtils.isDestroy(GIFPreviewActivity.this)) {
            return;
        }
        Glide.with(this)
                .asGif()
                .load(gifMessage.getLocalPath())
                .listener(
                        new RequestListener<GifDrawable>() {
                            @Override
                            public boolean onLoadFailed(
                                    @Nullable GlideException e,
                                    Object model,
                                    Target<GifDrawable> target,
                                    boolean isFirstResource) {
                                if (ChatUIUtils.isDestroy(GIFPreviewActivity.this)) {
                                    return true;
                                }
                                gifPreview.post(
                                        () -> {
                                            if (ChatUIUtils.isDestroy(GIFPreviewActivity.this)) {
                                                return;
                                            }
                                            Glide.with(GIFPreviewActivity.this)
                                                    .asBitmap()
                                                    .load(gifMessage.getLocalPath())
                                                    .fitCenter()
                                                    .error(
                                                            R.drawable
                                                                    .nc_received_thumb_image_broken)
                                                    .into(gifPreview);
                                        });
                                return true;
                            }

                            @Override
                            public boolean onResourceReady(
                                    GifDrawable resource,
                                    Object model,
                                    Target<GifDrawable> target,
                                    DataSource dataSource,
                                    boolean isFirstResource) {
                                return false;
                            }
                        })
                .error(R.drawable.nc_received_thumb_image_broken)
                .into(gifPreview);
    }

    private void loadGifFromUrl(ImageView gifPreview, String url) {
        if (ChatUIUtils.isDestroy(GIFPreviewActivity.this) || TextUtils.isEmpty(url)) {
            return;
        }
        Glide.with(this)
                .asGif()
                .load(url)
                .listener(
                        new RequestListener<GifDrawable>() {
                            @Override
                            public boolean onLoadFailed(
                                    @Nullable GlideException e,
                                    Object model,
                                    Target<GifDrawable> target,
                                    boolean isFirstResource) {
                                if (ChatUIUtils.isDestroy(GIFPreviewActivity.this)) {
                                    return true;
                                }
                                gifPreview.post(
                                        () -> {
                                            if (ChatUIUtils.isDestroy(
                                                    GIFPreviewActivity.this)) {
                                                return;
                                            }
                                            Glide.with(GIFPreviewActivity.this)
                                                    .asBitmap()
                                                    .load(url)
                                                    .fitCenter()
                                                    .error(
                                                            R.drawable
                                                                    .nc_received_thumb_image_broken)
                                                    .into(gifPreview);
                                        });
                                return true;
                            }

                            @Override
                            public boolean onResourceReady(
                                    GifDrawable resource,
                                    Object model,
                                    Target<GifDrawable> target,
                                    DataSource dataSource,
                                    boolean isFirstResource) {
                                return false;
                            }
                        })
                .error(R.drawable.nc_received_thumb_image_broken)
                .into(gifPreview);
    }

    private boolean isGifLocalReady(GIFMessage gifMessage) {
        if (gifMessage == null || TextUtils.isEmpty(gifMessage.getLocalPath())) {
            return false;
        }
        try {
            return ai.nexconn.chatui.utils.file.FileUtils.isFileExistsWithUri(
                    this, Uri.parse(gifMessage.getLocalPath()));
        } catch (Exception e) {
            RLog.w(TAG, "isGifLocalReady parse localPath failed");
            return false;
        }
    }

    private void saveGif(GIFMessage message) {
        String path = message.getLocalPath();
        if (TextUtils.isEmpty(path)) {
            return;
        }
        String[] items = new String[] {getString(R.string.nc_save_picture)};
        OptionsPopupDialog.newInstance(GIFPreviewActivity.this, items)
                .setOptionsPopupDialogListener(
                        new OptionsPopupDialog.OnOptionsItemClickedListener() {
                            @Override
                            public void onOptionsItemClicked(int which) {
                                if (which == 0) {
                                    String[] permissions = {
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    };
                                    if (Build.VERSION.SDK_INT < AndroidConstant.ANDROID_TIRAMISU
                                            && !PermissionCheckUtil.requestPermissions(
                                                    GIFPreviewActivity.this, permissions)) {
                                        return;
                                    }

                                    File file = getLocalGifFile(message);
                                    String text = getString(R.string.nc_src_file_not_found);
                                    if (file != null && file.exists()) {
                                        boolean result =
                                                ChatUIStorageUtils.saveMediaToPublicDir(
                                                        GIFPreviewActivity.this,
                                                        file,
                                                        ChatUIStorageUtils.MediaType.IMAGE);
                                        if (result) {
                                            text =
                                                    GIFPreviewActivity.this.getString(
                                                            R.string.nc_save_picture_at);
                                        }
                                    }

                                    ToastUtils.show(
                                            GIFPreviewActivity.this, text, Toast.LENGTH_SHORT);
                                }
                            }
                        })
                .show();
    }

    private File getLocalGifFile(GIFMessage message) {
        if (message == null || TextUtils.isEmpty(message.getLocalPath())) {
            return null;
        }
        String path = message.getLocalPath();
        try {
            Uri uri = Uri.parse(path);
            String scheme = uri.getScheme();
            if (TextUtils.isEmpty(scheme)) {
                return new File(path);
            }
            if ("file".equalsIgnoreCase(scheme)) {
                return new File(uri.getPath());
            }
            if ("content".equalsIgnoreCase(scheme)) {
                return copyGifUriToCache(uri);
            }
        } catch (Exception e) {
            RLog.e(TAG, "getLocalGifFile", e);
        }
        return new File(path);
    }

    private File copyGifUriToCache(Uri uri) {
        File cacheFile =
                new File(getCacheDir(), "Nexconn_GIF_" + System.currentTimeMillis() + ".gif");
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
                FileOutputStream outputStream = new FileOutputStream(cacheFile)) {
            if (inputStream == null) {
                return null;
            }
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return cacheFile;
        } catch (IOException e) {
            RLog.e(TAG, "copyGifUriToCache", e);
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NCChatUI.removeMessageEventListener(mBaseMessageEvent);
        NCEngine.removeMessageHandler("GIFPreviewActivity");
    }

    @Override
    public void finish() {
        super.finish();
        // Clearing fullscreen flags on finish prevents a redraw flicker when returning
        // to a non-fullscreen Activity (typical symptom: RecyclerView scrolls down slightly)
        if (getWindow() != null) {
            int flag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
            getWindow().setFlags(flag, flag);
        }
    }

    MessageEventListener mBaseMessageEvent =
            new MessageEventListener() {
                @Override
                public void onDeleteMessage(DeleteEvent event) {
                    RLog.d(TAG, "MessageDeleteEvent");
                    if (event.getMessageIds() != null && currentMessage != null) {
                        for (int messageId : event.getMessageIds()) {
                            if (messageId == currentMessage.getClientId()) {
                                finish();
                                break;
                            }
                        }
                    }
                }
            };
    private final MessageHandler mRecallMessageHandler =
            new MessageHandler() {
                @Override
                public void onMessageDeleted(@NonNull MessageDeletedEvent event) {
                    if (currentMessage == null || event.getMessages() == null) return;
                    for (Message msg : event.getMessages()) {
                        if (msg != null && currentMessage.getClientId() == msg.getClientId()) {
                            new AlertDialog.Builder(
                                            GIFPreviewActivity.this,
                                            AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
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
}
