package ai.nexconn.chatui.channel.feature.combineforward;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.DownloadMediaMessageHandler;
import ai.nexconn.chat.message.CombineMessage;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.model.CombineMsgItem;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.utils.file.FileUtils;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.message.MessageHolder;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;

/**
 * Combined-forward message preview page ViewModel.
 *
 * @since 5.12.0
 */
public class CombineMessagePreviewViewModel extends BaseViewModel {

    private static final String TAG = "CombineMessagePreviewViewModel";

    private final MutableLiveData<String> titleLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<CombineMsgItem>> msgListLiveData = new MutableLiveData<>();
    private final MutableLiveData<LoadState> loadStateLiveData = new MutableLiveData<>();

    private Message currentMessage;
    private CombineMessage currentCombineMessage;

    public CombineMessagePreviewViewModel(@NonNull Bundle arguments) {
        super(arguments);
        loadStateLiveData.setValue(LoadState.LOADING);
        loadMessage();
    }

    public LiveData<String> getTitleLiveData() {
        return titleLiveData;
    }

    public LiveData<List<CombineMsgItem>> getMsgListLiveData() {
        return msgListLiveData;
    }

    public LiveData<LoadState> getLoadStateLiveData() {
        return loadStateLiveData;
    }

    @NonNull
    public ChannelType getPreviewChannelType() {
        if (currentCombineMessage != null && currentCombineMessage.getChannelType() != null) {
            return currentCombineMessage.getChannelType();
        }
        if (currentMessage != null && currentMessage.getChannelIdentifier() != null) {
            ChannelType channelType = currentMessage.getChannelIdentifier().getChannelType();
            if (channelType != null) {
                return channelType;
            }
        }
        return ChannelType.DIRECT;
    }

    @NonNull
    public String getPreviewTargetId() {
        if (currentCombineMessage != null) {
            List<CombineMsgItem> msgList = currentCombineMessage.getMsgList();
            if (msgList != null) {
                for (CombineMsgItem item : msgList) {
                    if (item != null && !TextUtils.isEmpty(item.getTargetId())) {
                        return item.getTargetId();
                    }
                }
            }
        }
        if (currentMessage != null && currentMessage.getChannelIdentifier() != null) {
            String channelId = currentMessage.getChannelIdentifier().getChannelId();
            if (!TextUtils.isEmpty(channelId)) {
                return channelId;
            }
        }
        return "";
    }

    public void loadMessage() {
        RLog.d(TAG, "loadMessage: start");
        Message message = MessageHolder.takeMessage();
        if (message == null) {
            RLog.e(TAG, "loadMessage: message is null");
            loadStateLiveData.postValue(LoadState.FAILED);
            return;
        }
        if (!(message.getContent() instanceof CombineMessage)) {
            RLog.e(
                    TAG,
                    "loadMessage: content is not CombineMessage, type="
                            + (message.getContent() != null
                                    ? message.getContent().getClass().getName()
                                    : "null"));
            loadStateLiveData.postValue(LoadState.FAILED);
            return;
        }

        currentMessage = message;
        currentCombineMessage = (CombineMessage) message.getContent();
        List<CombineMsgItem> msgList = currentCombineMessage.getMsgList();
        String localPath = currentCombineMessage.getLocalPath();
        String remoteUrl = currentCombineMessage.getRemoteUrl();

        RLog.d(
                TAG,
                "loadMessage: msgList="
                        + (msgList != null ? msgList.size() : "null")
                        + ", localPath="
                        + localPath
                        + ", remoteUrl="
                        + remoteUrl);

        if (msgList != null && !msgList.isEmpty()) {
            // Message list is inlined; display directly (received/sent small messages)
            RLog.d(TAG, "loadMessage: inline msgList, size=" + msgList.size());
            String title = buildTitle(currentCombineMessage);
            titleLiveData.postValue(title);
            msgListLiveData.postValue(msgList);
            loadStateLiveData.postValue(LoadState.SUCCESS);
        } else if (!TextUtils.isEmpty(localPath)) {
            // Local file exists (sent large message / already downloaded remote message); parse
            // directly
            RLog.d(TAG, "loadMessage: parsing from localPath=" + localPath);
            parseMsgListFromLocalFile(toFileUri(localPath));
        } else if (!TextUtils.isEmpty(remoteUrl)) {
            // Message list exceeds inline size limit; download from remoteUrl
            RLog.d(TAG, "loadMessage: downloading from remoteUrl=" + remoteUrl);
            downloadRemoteMsgList(remoteUrl);
        } else {
            RLog.e(TAG, "loadMessage: msgList, localPath and remoteUrl are all empty");
            loadStateLiveData.postValue(LoadState.FAILED);
        }
    }

    /**
     * Downloads the combined-forward message list file from a remote URL and parses it to populate
     * the UI.
     *
     * @param remoteUrl remote URL of the combined message list file
     */
    private void downloadRemoteMsgList(String remoteUrl) {
        RLog.d(TAG, "downloadRemoteMsgList: start downloading from " + remoteUrl);
        // Use nexconn-chat's Message.downloadMedia() API
        currentMessage.downloadMedia(
                new DownloadMediaMessageHandler() {
                    @Override
                    public void onSuccess(Message message) {
                        RLog.d(TAG, "downloadMedia: onSuccess");
                        // After successful download, localPath should be populated
                        CombineMessage content = (CombineMessage) message.getContent();
                        String localPath = content != null ? content.getLocalPath() : null;
                        RLog.d(TAG, "downloadMedia: localPath=" + localPath);
                        if (content != null && !TextUtils.isEmpty(localPath)) {
                            parseMsgListFromLocalFile(toFileUri(localPath));
                        } else {
                            RLog.e(TAG, "downloadMedia success but localPath is empty");
                            loadStateLiveData.postValue(LoadState.FAILED);
                        }
                    }

                    @Override
                    public void onProgress(Message message, int progress) {
                        RLog.d(TAG, "downloadMedia: onProgress " + progress + "%");
                    }

                    @Override
                    public void onError(Message message, NCError error) {
                        RLog.e(
                                TAG,
                                "downloadMedia failed, code="
                                        + error.getCode()
                                        + ", msg="
                                        + error.getMessage());
                        loadStateLiveData.postValue(LoadState.FAILED);
                    }

                    @Override
                    public void onCanceled(Message message) {
                        RLog.e(TAG, "downloadMedia: onCanceled");
                        loadStateLiveData.postValue(LoadState.FAILED);
                    }
                });
    }

    /** Converts a localPath string (may be a file:// URI or a raw path) to a Uri. */
    private android.net.Uri toFileUri(String localPath) {
        if (localPath.startsWith("file://") || localPath.startsWith("content://")) {
            return android.net.Uri.parse(localPath);
        }
        return android.net.Uri.fromFile(new java.io.File(localPath));
    }

    /** Parses the message list from a locally downloaded file. */
    private void parseMsgListFromLocalFile(android.net.Uri localUri) {
        RLog.d(TAG, "parseMsgListFromLocalFile: localUri=" + localUri);
        try {
            byte[] bytes = FileUtils.getByteFromUri(localUri);
            if (bytes == null || bytes.length == 0) {
                RLog.e(TAG, "parseMsgListFromLocalFile: bytes is null or empty");
                loadStateLiveData.postValue(LoadState.FAILED);
                return;
            }
            RLog.d(TAG, "parseMsgListFromLocalFile: bytes.length=" + bytes.length);
            String jsonStr = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            if (TextUtils.isEmpty(jsonStr)) {
                RLog.e(TAG, "parseMsgListFromLocalFile: json content is empty");
                loadStateLiveData.postValue(LoadState.FAILED);
                return;
            }
            RLog.d(
                    TAG,
                    "parseMsgListFromLocalFile: jsonStr="
                            + jsonStr.substring(0, Math.min(200, jsonStr.length())));

            // Use MessageBridgeHelper to parse JSON array
            org.json.JSONArray jsonArray =
                    ai.nexconn.chat.bridge.MessageBridgeHelper.parseMsgArrayFromString(jsonStr);
            if (jsonArray == null) {
                RLog.e(TAG, "parseMsgListFromLocalFile: failed to parse message list json");
                loadStateLiveData.postValue(LoadState.FAILED);
                return;
            }
            RLog.d(TAG, "parseMsgListFromLocalFile: parsed jsonArray.length=" + jsonArray.length());

            // Use MessageBridgeHelper to parse CombineMsgItem list
            List<CombineMsgItem> msgList =
                    ai.nexconn.chat.bridge.MessageBridgeHelper.parseCombineMsgItemsFromJson(
                            jsonArray);
            RLog.d(TAG, "parseMsgListFromLocalFile: parsed msgList.size=" + msgList.size());
            if (msgList.isEmpty()) {
                RLog.e(TAG, "parseMsgListFromLocalFile: msgList is empty");
                loadStateLiveData.postValue(LoadState.FAILED);
                return;
            }

            // Fill in targetId if missing
            for (CombineMsgItem item : msgList) {
                if (TextUtils.isEmpty(item.getTargetId())) {
                    try {
                        java.lang.reflect.Field targetIdField =
                                CombineMsgItem.class.getDeclaredField("targetId");
                        targetIdField.setAccessible(true);
                        targetIdField.set(item, getPreviewTargetId());
                    } catch (Exception e) {
                        RLog.w(TAG, "Failed to set targetId: " + e.getMessage());
                    }
                }
            }

            String title = buildTitle(currentCombineMessage);
            titleLiveData.postValue(title);
            msgListLiveData.postValue(msgList);
            loadStateLiveData.postValue(LoadState.SUCCESS);
            RLog.d(TAG, "parseMsgListFromLocalFile: SUCCESS");
        } catch (Exception e) {
            RLog.e(TAG, "parseMsgListFromLocalFile exception: " + e.getMessage());
            loadStateLiveData.postValue(LoadState.FAILED);
        }
    }

    private String buildTitle(CombineMessage combineMessage) {
        return CombineForwardTitleHelper.buildTitle(NCChatUI.getContext(), combineMessage);
    }

    public enum LoadState {
        LOADING,
        SUCCESS,
        FAILED
    }
}
