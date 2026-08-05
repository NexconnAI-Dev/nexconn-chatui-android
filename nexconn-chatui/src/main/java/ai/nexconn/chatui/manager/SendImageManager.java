package ai.nexconn.chatui.manager;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.ConnectionStatusHandler;
import ai.nexconn.chat.handler.SendMediaMessageHandler;
import ai.nexconn.chat.message.GIFMessage;
import ai.nexconn.chat.message.ImageMessage;
import ai.nexconn.chat.message.MediaMessageContent;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.model.SentStatus;
import ai.nexconn.chat.model.ConnectionStatus;
import ai.nexconn.chat.params.SendMediaMessageParams;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.channel.event.action.RefreshEvent;
import ai.nexconn.chatui.picture.config.PictureMimeType;
import ai.nexconn.chatui.picture.entity.LocalMedia;
import ai.nexconn.chatui.utils.file.FileInfo;
import ai.nexconn.chatui.utils.file.FileUtils;
import ai.nexconn.chatui.utils.log.RLog;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SendImageManager {
    private static final String TAG = "SendImageManager";
    private static final String GIF_FILE_TYPE = "gif";
    private static final String TEMP_GIF_DIR = "nc_gif_send";
    private static final String CONNECTION_HANDLER_ID = "SendImageManager";

    private ExecutorService executorService;
    private final List<Message> mExecutingMessages = new CopyOnWriteArrayList<>();

    private final ConnectionStatusHandler connectionStatusHandler =
            event -> {
                ConnectionStatus status = event.getStatus();
                if (status == ConnectionStatus.NETWORK_UNAVAILABLE
                        || status == ConnectionStatus.UNCONNECTED
                        || status == ConnectionStatus.KICKED_OFFLINE_BY_OTHER_CLIENT
                        || status == ConnectionStatus.TOKEN_INCORRECT
                        || status == ConnectionStatus.CONN_USER_BLOCKED) {
                    RLog.w(TAG, "Connection lost, resetting pending image messages");
                    reset();
                }
            };

    static class SingletonHolder {
        static SendImageManager sInstance = new SendImageManager();
    }

    public static SendImageManager getInstance() {
        return SingletonHolder.sInstance;
    }

    private SendImageManager() {
        executorService = getExecutorService();
        NCEngine.addConnectionStatusHandler(CONNECTION_HANDLER_ID, connectionStatusHandler);
    }

    public void sendImage(
            ChannelIdentifier conversationIdentifier, LocalMedia image, boolean isFull) {
        if (image.getPath() == null) {
            return;
        }
        MediaMessageContent content;
        String mimeType = image.getMimeType();
        Uri uri = toUri(image.getPath());
        if (uri == null) {
            return;
        }
        if (PictureMimeType.isGif(mimeType)) {
            Uri gifUri = prepareGifSourceUri(uri);
            String detectedType = detectFileType(gifUri);
            if (!TextUtils.isEmpty(detectedType) && !GIF_FILE_TYPE.equalsIgnoreCase(detectedType)) {
                RLog.w(
                        TAG,
                        "GIF mime mismatch, detected type: "
                                + detectedType
                                + ", fallback to ImageMessage. uri="
                                + gifUri);
                ImageMessage imgMsg = new ImageMessage();
                imgMsg.setLocalPath(gifUri.toString());
                imgMsg.setOriginal(isFull);
                content = imgMsg;
            } else {
                GIFMessage gifMsg = new GIFMessage();
                gifMsg.setLocalPath(gifUri.toString());
                gifMsg.setWidth(image.getWidth());
                gifMsg.setHeight(image.getHeight());
                gifMsg.setDataSize(resolveMediaSize(gifUri, image.getSize()));
                content = gifMsg;
            }
        } else {
            ImageMessage imgMsg = new ImageMessage();
            imgMsg.setLocalPath(uri.toString());
            imgMsg.setOriginal(isFull);
            content = imgMsg;
        }
        final MediaMessageContent finalContent = content;
        NCChatUI.sendMediaMessage(
                conversationIdentifier,
                new SendMediaMessageParams(finalContent),
                new SendMediaMessageHandler() {
                    @Override
                    public void onAttached(Message message) {
                        mExecutingMessages.add(message);
                    }

                    @Override
                    public void onProgress(Message message, int progress) {
                        // progress is handled by NCChatUI.sendMediaMessage automatically
                    }

                    @Override
                    public void onCanceled(Message message) {
                        mExecutingMessages.remove(message);
                    }

                    @Override
                    public void onResult(@Nullable Message message, @Nullable NCError error) {
                        if (message != null) mExecutingMessages.remove(message);
                    }
                });
    }

    private Uri toUri(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        if (!path.startsWith("content://") && !path.startsWith("file://")) {
            path = "file://" + path;
        }
        return Uri.parse(path);
    }

    private Uri prepareGifSourceUri(Uri sourceUri) {
        if (sourceUri == null || !"content".equals(sourceUri.getScheme())) {
            return sourceUri;
        }
        Context context = NCChatUI.getContext();
        if (context == null) {
            return sourceUri;
        }
        File tempDir = new File(context.getCacheDir(), TEMP_GIF_DIR);
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            return sourceUri;
        }
        String fileName =
                "gif_"
                        + System.currentTimeMillis()
                        + "_"
                        + Math.abs(sourceUri.toString().hashCode())
                        + ".gif";
        File targetFile = new File(tempDir, fileName);
        boolean copied = FileUtils.copyFile(context, sourceUri, targetFile.getAbsolutePath());
        if (copied) {
            return Uri.fromFile(targetFile);
        }
        return sourceUri;
    }

    private String detectFileType(Uri uri) {
        Context context = NCChatUI.getContext();
        if (context == null || uri == null) {
            return "";
        }
        InputStream inputStream = null;
        try {
            inputStream = FileUtils.getFileInputStream(context, uri);
            return FileUtils.getFileTypeFromInputStream(inputStream);
        } catch (Exception e) {
            RLog.e(TAG, "detectFileType failed. uri=" + uri, e);
            return "";
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private long resolveMediaSize(Uri uri, long fallback) {
        if (fallback > 0 || uri == null) {
            return fallback;
        }
        Context context = NCChatUI.getContext();
        if (context == null) {
            return fallback;
        }
        FileInfo fileInfo = FileUtils.getFileInfoByUri(context, uri);
        return fileInfo != null ? fileInfo.getSize() : fallback;
    }

    public void cancelSendingImages(ChannelType conversationType, String targetId) {
        RLog.d(TAG, "cancelSendingImages");
        List<Message> toRemove = new ArrayList<>();
        for (Message msg : mExecutingMessages) {
            if (msg.getChannelIdentifier().getChannelType().equals(conversationType)
                    && msg.getChannelIdentifier().getChannelId().equals(targetId)) {
                toRemove.add(msg);
            }
        }
        mExecutingMessages.removeAll(toRemove);
    }

    public void cancelSendingImage(ChannelType conversationType, String targetId, int messageId) {
        RLog.d(TAG, "cancelSendingImages");
        for (Message msg : mExecutingMessages) {
            if (msg.getChannelIdentifier().getChannelType().equals(conversationType)
                    && msg.getChannelIdentifier().getChannelId().equals(targetId)
                    && msg.getClientId() == messageId) {
                mExecutingMessages.remove(msg);
                break;
            }
        }
    }

    public void reset() {
        RLog.w(TAG, "Reset Sending images.");
        List<Message> pending = new ArrayList<>(mExecutingMessages);
        mExecutingMessages.clear();
        for (Message message : pending) {
            message.setSentStatus(SentStatus.FAILED);
            NCChatUI.refreshMessage(new RefreshEvent(message));
        }
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService =
                    new ThreadPoolExecutor(
                            1,
                            Integer.MAX_VALUE,
                            60,
                            TimeUnit.SECONDS,
                            new SynchronousQueue<Runnable>(),
                            threadFactory());
        }
        return executorService;
    }

    private ThreadFactory threadFactory() {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread result = new Thread(runnable, "NC SendImageManager");
                result.setDaemon(false);
                return result;
            }
        };
    }
}
