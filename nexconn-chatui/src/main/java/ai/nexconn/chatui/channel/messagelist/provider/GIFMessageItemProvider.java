package ai.nexconn.chatui.channel.messagelist.provider;

import ai.nexconn.chat.message.GIFMessage;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.model.MessageDirection;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.activity.GIFPreviewActivity;
import ai.nexconn.chatui.channel.feature.resend.ResendManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.State;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.picture.tools.ScreenUtils;
import ai.nexconn.chatui.utils.file.FileUtils;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.widget.CircleProgressView;
import ai.nexconn.chatui.widget.adapter.IViewProviderListener;
import ai.nexconn.chatui.widget.adapter.ViewHolder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import java.util.List;

public class GIFMessageItemProvider extends BaseMessageItemProvider<GIFMessage> {

    private Integer minSize = null;
    private Integer maxSize = null;

    public GIFMessageItemProvider() {
        mConfig.showReadState = true;
        mConfig.showProgress = false;
        mConfig.showContentBubble = false;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.nc_item_gif_message, parent, false);
        return new ViewHolder(view.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(
            ViewHolder holder,
            ViewHolder parentHolder,
            GIFMessage gifMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        final ImageView imageView = holder.getView(R.id.nc_img);
        CircleProgressView loadingProgress = holder.getView(R.id.nc_gif_progress);
        if (!checkViewsValid(imageView, loadingProgress)) {
            RLog.e(TAG, "checkViewsValid error," + uiMessage.getMessageType());
            return;
        }
        measureLayoutParams(imageView, gifMessage.getWidth(), gifMessage.getHeight());
        loadingProgress.setVisibility(View.GONE);
        holder.setVisible(R.id.nc_download_failed, false);
        holder.setVisible(R.id.nc_start_download, false);
        holder.setVisible(R.id.nc_pre_progress, false);
        holder.setVisible(R.id.nc_length, false);
        int progress = uiMessage.getProgress();
        if (uiMessage.getMessage().getDirection() == MessageDirection.SEND) {
            if (((progress > 0 && progress < 100) || uiMessage.getState() == State.PROGRESS)
                    || (uiMessage.getState() == State.ERROR)
                            && ResendManager.getInstance()
                                    .needResend(uiMessage.getMessage().getClientId())) {
                loadingProgress.setProgress(progress, true);
                loadingProgress.setVisibility(View.VISIBLE);
                holder.setVisible(R.id.nc_pre_progress, false);
            } else if (uiMessage.getState() == State.ERROR) {
                loadingProgress.setVisibility(View.GONE);
                holder.setVisible(R.id.nc_pre_progress, false);
                holder.setVisible(R.id.nc_download_failed, true);
                holder.setVisible(R.id.nc_length, true);
            } else {
                loadingProgress.setVisibility(View.GONE);
                holder.setVisible(R.id.nc_pre_progress, false);
            }
        } else {
            if (uiMessage.getMessage().getReceivedStatusInfo().isDownloaded()) {
                if (progress > 0 && progress < 100) {
                    loadingProgress.setProgress(progress, true);
                    loadingProgress.setVisibility(View.VISIBLE);
                    holder.setVisible(R.id.nc_pre_progress, false);
                    holder.setVisible(R.id.nc_start_download, false);
                } else if (progress == 100) {
                    loadingProgress.setVisibility(View.GONE);
                    holder.setVisible(R.id.nc_pre_progress, false);
                    holder.setVisible(R.id.nc_length, false);
                    holder.setVisible(R.id.nc_start_download, false);
                } else if (uiMessage.getState() == State.ERROR) {
                    loadingProgress.setVisibility(View.GONE);
                    holder.setVisible(R.id.nc_pre_progress, false);
                    holder.setVisible(R.id.nc_download_failed, true);
                    holder.setVisible(R.id.nc_length, true);
                    holder.setText(R.id.nc_length, formatSize(gifMessage.getDataSize()));
                    holder.setVisible(R.id.nc_start_download, false);
                } else {
                    loadingProgress.setVisibility(View.GONE);
                    holder.setVisible(R.id.nc_pre_progress, true);
                    holder.setVisible(R.id.nc_length, true);
                    holder.setVisible(R.id.nc_start_download, false);
                }
            } else {
                loadingProgress.setVisibility(View.GONE);
                holder.setVisible(R.id.nc_pre_progress, false);
                holder.setVisible(R.id.nc_length, false);
                holder.setVisible(R.id.nc_start_download, false);

                if (uiMessage.getState() == State.ERROR) {
                    holder.setVisible(R.id.nc_download_failed, true);
                    holder.setVisible(R.id.nc_length, true);
                    holder.setText(R.id.nc_length, formatSize(gifMessage.getDataSize()));
                }
            }
        }
        String localPath = gifMessage.getLocalPath();
        if (isLocalPathAvailable(localPath)) {
            loadGif(localPath, imageView);
        } else if (shouldLoadPreviewRemoteGif(uiMessage, gifMessage)) {
            hideDownloadViews(holder, loadingProgress);
            ProgressBar preProgress = holder.getView(R.id.nc_pre_progress);
            if (preProgress != null) {
                preProgress.setVisibility(View.VISIBLE);
            }
            loadGifWithLoading(gifMessage.getRemoteUrl(), imageView, preProgress, gifMessage);
        } else {
            gifMessage.setLocalPath(null);
            imageView.setImageResource(R.drawable.def_gif_bg);
            final int size = NCChatUIConfig.channelConfig().NC_gifmsg_auto_download_size;
            if (gifMessage.getDataSize() <= size * 1024L) {
                if (!uiMessage.getMessage().getReceivedStatusInfo().isDownloaded()
                        && uiMessage.getState() != State.PROGRESS) {
                    downLoad(uiMessage.getMessage(), holder, uiMessage);
                }
            } else {
                if (progress > 0 && progress < 100) {
                    loadingProgress.setVisibility(View.VISIBLE);
                    loadingProgress.setProgress(progress, true);
                    holder.setVisible(R.id.nc_start_download, false);
                    holder.setVisible(R.id.nc_length, true);
                    holder.setText(R.id.nc_length, formatSize(gifMessage.getDataSize()));
                } else if (progress == 100) {
                    loadingProgress.setVisibility(View.GONE);
                    holder.setVisible(R.id.nc_pre_progress, false);
                    holder.setVisible(R.id.nc_length, false);
                    holder.setVisible(R.id.nc_start_download, false);
                } else if (uiMessage.getState() == State.PROGRESS) {
                    // Download started but progress not yet reflected (progress==0 initial report)
                    loadingProgress.setVisibility(View.VISIBLE);
                    loadingProgress.setProgress(0, true);
                    holder.setVisible(R.id.nc_start_download, false);
                    holder.setVisible(R.id.nc_pre_progress, false);
                    holder.setVisible(R.id.nc_length, true);
                    holder.setText(R.id.nc_length, formatSize(gifMessage.getDataSize()));
                } else if (uiMessage.getState() != State.ERROR) {
                    holder.setVisible(R.id.nc_start_download, true);
                    holder.setVisible(R.id.nc_pre_progress, false);
                    loadingProgress.setVisibility(View.GONE);
                    holder.setVisible(R.id.nc_download_failed, false);
                    holder.setVisible(R.id.nc_length, true);
                    holder.setText(R.id.nc_length, formatSize(gifMessage.getDataSize()));
                }
            }
        }
    }

    private void loadGif(final String source, final ImageView imageView) {
        if (!TextUtils.isEmpty(source)) {
            RequestOptions options =
                    RequestOptions.bitmapTransform(
                                    new RoundedCorners(
                                            ScreenUtils.dip2px(NCChatUI.getContext(), 6)))
                            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
            Glide.with(imageView.getContext())
                    .asGif()
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .load(source)
                    .listener(
                            new RequestListener<GifDrawable>() {
                                @Override
                                public boolean onLoadFailed(
                                        GlideException e,
                                        Object model,
                                        Target<GifDrawable> target,
                                        boolean isFirstResource) {
                                    imageView.post(
                                            () ->
                                                    Glide.with(imageView.getContext())
                                                            .asBitmap()
                                                            .load(source)
                                                            .apply(options)
                                                            .error(
                                                                    R.drawable
                                                                            .nc_received_thumb_image_broken)
                                                            .into(imageView));
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
                    .apply(options)
                    .into(imageView);
        }
    }

    private void loadGifWithLoading(
            final String source,
            final ImageView imageView,
            final ProgressBar progressBar,
            final GIFMessage gifMessage) {
        if (TextUtils.isEmpty(source)) return;
        RequestOptions options =
                RequestOptions.bitmapTransform(
                                new RoundedCorners(ScreenUtils.dip2px(NCChatUI.getContext(), 6)))
                        .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
        Glide.with(imageView.getContext())
                .asGif()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .load(source)
                .listener(
                        new RequestListener<GifDrawable>() {
                            @Override
                            public boolean onLoadFailed(
                                    GlideException e,
                                    Object model,
                                    Target<GifDrawable> target,
                                    boolean isFirstResource) {
                                if (progressBar != null) {
                                    progressBar.setVisibility(View.GONE);
                                }
                                imageView.post(
                                        () ->
                                                Glide.with(imageView.getContext())
                                                        .asBitmap()
                                                        .load(source)
                                                        .apply(options)
                                                        .error(
                                                                R.drawable
                                                                        .nc_received_thumb_image_broken)
                                                        .into(imageView));
                                return true;
                            }

                            @Override
                            public boolean onResourceReady(
                                    GifDrawable resource,
                                    Object model,
                                    Target<GifDrawable> target,
                                    DataSource dataSource,
                                    boolean isFirstResource) {
                                if (progressBar != null) {
                                    progressBar.setVisibility(View.GONE);
                                }
                                cacheGifToMediaDir(source, imageView, gifMessage);
                                return false;
                            }
                        })
                .error(R.drawable.nc_received_thumb_image_broken)
                .apply(options)
                .into(imageView);
    }

    private boolean shouldLoadPreviewRemoteGif(UiMessage uiMessage, GIFMessage gifMessage) {
        return uiMessage != null
                && uiMessage.getMessage() != null
                && !uiMessage.getMessage().isPersisted()
                && gifMessage != null
                && !TextUtils.isEmpty(gifMessage.getRemoteUrl());
    }

    private void cacheGifToMediaDir(
            final String remoteUrl, final ImageView imageView, final GIFMessage gifMessage) {
        String cacheDir = ai.nexconn.chat.bridge.MessageBridgeHelper.getGifMediaCacheDir();
        String fileName = ai.nexconn.chat.bridge.MessageBridgeHelper.getGifExpectedFileName(remoteUrl);
        if (cacheDir == null || fileName == null) return;
        java.io.File target = new java.io.File(cacheDir, fileName);
        if (target.exists()) {
            gifMessage.setLocalPath(android.net.Uri.fromFile(target).toString());
            return;
        }
        new Thread(
                        () -> {
                            try {
                                java.io.File source =
                                        Glide.with(imageView.getContext())
                                                .asFile()
                                                .load(remoteUrl)
                                                .submit()
                                                .get();
                                if (source == null || !source.exists()) return;
                                java.io.File dir = new java.io.File(cacheDir);
                                if (!dir.exists()) dir.mkdirs();
                                copyFile(source, target);
                                if (target.exists()) {
                                    gifMessage.setLocalPath(
                                            android.net.Uri.fromFile(target).toString());
                                }
                            } catch (Exception ignored) {
                            }
                        })
                .start();
    }

    private void copyFile(java.io.File src, java.io.File dst) throws java.io.IOException {
        try (java.io.InputStream in = new java.io.FileInputStream(src);
                java.io.OutputStream out = new java.io.FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    private void hideDownloadViews(ViewHolder holder, CircleProgressView loadingProgress) {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.GONE);
        }
        holder.setVisible(R.id.nc_pre_progress, false);
        holder.setVisible(R.id.nc_start_download, false);
        holder.setVisible(R.id.nc_download_failed, false);
        holder.setVisible(R.id.nc_length, false);
    }

    @Override
    protected boolean onItemClick(
            ViewHolder holder,
            GIFMessage gifMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        ImageView startDownLoad = holder.getView(R.id.nc_start_download);
        ImageView downLoadFailed = holder.getView(R.id.nc_download_failed);
        TextView length = holder.getView(R.id.nc_length);
        ProgressBar preProgress = holder.getView(R.id.nc_pre_progress);
        CircleProgressView loadingProgress = holder.getView(R.id.nc_gif_progress);

        if (startDownLoad.getVisibility() == View.VISIBLE) {
            startDownLoad.setVisibility(View.GONE);
            downLoad(uiMessage.getMessage(), holder, uiMessage);
            return true;
        } else if (downLoadFailed.getVisibility() == View.VISIBLE) {
            downLoadFailed.setVisibility(View.GONE);
            downLoad(uiMessage.getMessage(), holder, uiMessage);
            return true;
        } else if (preProgress.getVisibility() != View.VISIBLE
                && loadingProgress.getVisibility() != View.VISIBLE) {
            if (gifMessage != null) {
                ai.nexconn.chatui.utils.message.MessageHolder.holdMessage(uiMessage.getMessage());
                Intent intent = new Intent(holder.getContext(), GIFPreviewActivity.class);
                holder.getContext().startActivity(intent);
                return true;
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof GIFMessage && !messageContent.isDestruct();
    }

    private boolean isLocalPathAvailable(String localPath) {
        if (TextUtils.isEmpty(localPath)) {
            return false;
        }
        try {
            Uri uri = Uri.parse(localPath);
            String scheme = uri.getScheme();
            if (TextUtils.isEmpty(scheme)) {
                return new java.io.File(localPath).exists();
            }
            if ("file".equalsIgnoreCase(scheme) || "content".equalsIgnoreCase(scheme)) {
                Context context = NCChatUI.getContext();
                return context != null && FileUtils.isFileExistsWithUri(context, uri);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private String formatSize(long length) {
        if (length > 1024 * 1024) { // M
            float size = Math.round(length / (1024f * 1024f) * 100) / 100f;
            return size + "M";
        } else if (length > 1024) {
            float size = Math.round(length / (1024f) * 100) / 100f;
            return size + "KB";
        } else {
            return length + "B";
        }
    }

    private void measureLayoutParams(View view, int width, int height) {
        if (minSize == null) {
            minSize = ScreenUtils.dip2px(view.getContext(), 79);
        }
        if (maxSize == null) {
            maxSize = ScreenUtils.dip2px(view.getContext(), 120);
        }
        if (width <= 0 || height <= 0) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = minSize;
            params.width = minSize;
            view.setLayoutParams(params);
            return;
        }
        int finalWidth;
        int finalHeight;
        if (width < minSize || height < minSize) {
            if (width < height) {
                finalWidth = minSize;
                finalHeight = Math.min((int) (minSize * 1f / width * height), maxSize);
            } else {
                finalHeight = minSize;
                finalWidth = Math.min((int) (minSize * 1f / height * width), maxSize);
            }
        } else if (width < maxSize && height < maxSize) {
            if (width > height) {
                finalWidth = maxSize;
                finalHeight = (int) (maxSize * 1f / width * height);
            } else {
                finalHeight = maxSize;
                finalWidth = (int) (maxSize * 1f / height * width);
            }
        } else {
            if (width > height) {
                if (width * 1f / height <= 2.4) {
                    finalWidth = maxSize;
                    finalHeight = (int) (maxSize * 1f / width * height);
                } else {
                    finalWidth = maxSize;
                    finalHeight = minSize;
                }
            } else {
                if (height * 1f / width <= 2.4) {
                    finalHeight = maxSize;
                    finalWidth = (int) (maxSize * 1f / height * width);
                } else {
                    finalHeight = maxSize;
                    finalWidth = minSize;
                }
            }
        }
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = finalHeight;
        params.width = finalWidth;
        view.setLayoutParams(params);
    }

    private void downLoad(
            final Message downloadMsg, final ViewHolder holder, final UiMessage uiMessage) {
        holder.setVisible(R.id.nc_pre_progress, true);
        if (downloadMsg == null) return;

        // Mark as in-progress immediately so rebinds during download don't re-show the button
        uiMessage.setState(State.PROGRESS);
        uiMessage.setProgress(0);

        downloadMsg.downloadMedia(
                new ai.nexconn.chat.handler.DownloadMediaMessageHandler() {
                    @Override
                    public void onSuccess(Message message) {
                        if (message != null
                                && message.getContent()
                                        instanceof ai.nexconn.chat.message.GIFMessage) {
                            downloadMsg.setContent(message.getContent());
                        }
                        uiMessage.setProgress(100);
                        uiMessage.setState(State.NORMAL);
                        refreshDownloadMessage(downloadMsg, holder, uiMessage);
                    }

                    @Override
                    public void onProgress(Message message, int progress) {
                        uiMessage.setProgress(progress);
                        uiMessage.setState(State.PROGRESS);
                        refreshDownloadMessage(downloadMsg, holder, uiMessage);
                    }

                    @Override
                    public void onError(Message message, ai.nexconn.chat.error.NCError error) {
                        uiMessage.setProgress(0);
                        uiMessage.setState(State.ERROR);
                        refreshDownloadMessage(downloadMsg, holder, uiMessage);
                    }

                    @Override
                    public void onCanceled(Message message) {
                        uiMessage.setProgress(0);
                        uiMessage.setState(State.NORMAL);
                        refreshDownloadMessage(downloadMsg, holder, uiMessage);
                    }
                });
    }

    private void refreshDownloadMessage(
            final Message downloadMsg, final ViewHolder holder, final UiMessage uiMessage) {
        if (shouldDispatchGlobalRefresh(downloadMsg)) {
            ai.nexconn.chatui.NCChatUI.refreshMessage(
                    new ai.nexconn.chatui.channel.event.action.RefreshEvent(downloadMsg));
            return;
        }
        updatePreviewDownloadView(holder, uiMessage);
    }

    private boolean shouldDispatchGlobalRefresh(Message message) {
        return message != null
                && message.isPersisted()
                && !TextUtils.isEmpty(message.getMessageId());
    }

    private void updatePreviewDownloadView(final ViewHolder holder, final UiMessage uiMessage) {
        if (holder == null || uiMessage == null || uiMessage.getMessage() == null) {
            return;
        }
        holder.itemView.post(
                new Runnable() {
                    @Override
                    public void run() {
                        MessageContent content = uiMessage.getMessage().getContent();
                        if (!(content instanceof GIFMessage)) {
                            return;
                        }
                        GIFMessage gifMessage = (GIFMessage) content;
                        ImageView imageView = holder.getView(R.id.nc_img);
                        CircleProgressView loadingProgress = holder.getView(R.id.nc_gif_progress);
                        if (!checkViewsValid(imageView, loadingProgress)) {
                            return;
                        }
                        ProgressBar preProgress = holder.getView(R.id.nc_pre_progress);
                        int progress = uiMessage.getProgress();
                        if (isLocalPathAvailable(gifMessage.getLocalPath())) {
                            loadingProgress.setVisibility(View.GONE);
                            if (preProgress != null) {
                                preProgress.setVisibility(View.GONE);
                            }
                            holder.setVisible(R.id.nc_start_download, false);
                            holder.setVisible(R.id.nc_download_failed, false);
                            holder.setVisible(R.id.nc_length, false);
                            loadGif(gifMessage.getLocalPath(), imageView);
                        } else if (shouldLoadPreviewRemoteGif(uiMessage, gifMessage)) {
                            hideDownloadViews(holder, loadingProgress);
                            if (preProgress != null) {
                                preProgress.setVisibility(View.VISIBLE);
                            }
                            loadGifWithLoading(
                                    gifMessage.getRemoteUrl(), imageView, preProgress, gifMessage);
                        } else if (uiMessage.getState() == State.PROGRESS) {
                            imageView.setImageResource(R.drawable.def_gif_bg);
                            if (preProgress != null) {
                                preProgress.setVisibility(View.GONE);
                            }
                            loadingProgress.setProgress(progress, true);
                            loadingProgress.setVisibility(View.VISIBLE);
                            holder.setVisible(R.id.nc_start_download, false);
                            holder.setVisible(R.id.nc_download_failed, false);
                            holder.setVisible(R.id.nc_length, true);
                            holder.setText(R.id.nc_length, formatSize(gifMessage.getDataSize()));
                        } else if (uiMessage.getState() == State.ERROR) {
                            loadingProgress.setVisibility(View.GONE);
                            if (preProgress != null) {
                                preProgress.setVisibility(View.GONE);
                            }
                            holder.setVisible(R.id.nc_start_download, false);
                            holder.setVisible(R.id.nc_download_failed, true);
                            holder.setVisible(R.id.nc_length, true);
                            holder.setText(R.id.nc_length, formatSize(gifMessage.getDataSize()));
                        } else {
                            loadingProgress.setVisibility(View.GONE);
                            if (preProgress != null) {
                                preProgress.setVisibility(View.GONE);
                            }
                            holder.setVisible(R.id.nc_start_download, true);
                            holder.setVisible(R.id.nc_download_failed, false);
                            holder.setVisible(R.id.nc_length, true);
                            holder.setText(R.id.nc_length, formatSize(gifMessage.getDataSize()));
                        }
                    }
                });
    }

    @Override
    public Spannable getSummarySpannable(Context context, GIFMessage gifMessage) {
        return new SpannableString(
                context.getString(R.string.nc_conversation_summary_content_image));
    }
}
