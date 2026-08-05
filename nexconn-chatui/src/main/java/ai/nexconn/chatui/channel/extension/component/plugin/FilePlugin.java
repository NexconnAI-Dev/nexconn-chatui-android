package ai.nexconn.chatui.channel.extension.component.plugin;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.extension.NCExtension;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.picture.tools.ToastUtils;
import ai.nexconn.chatui.utils.file.ChatUIStorageUtils;
import ai.nexconn.chatui.utils.file.FileUtils;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.system.ExecutorHelper;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FilePlugin implements IPluginModule {

    private static final String TAG = "FilePlugin";
    private static final int REQUEST_FILE = 100;
    // Message sending interval
    private static final int TIME_DELAY = 400;
    private ChannelIdentifier mConversationIdentifier;
    private Context mContext;

    @Override
    public Drawable obtainDrawable(Context context) {
        this.mContext = context;
        return ContextCompat.getDrawable(
                context,
                ChatUIThemeManager.getAttrResId(
                        context, R.attr.nc_conversation_plugin_item_file_img));
    }

    @Override
    public String obtainTitle(Context context) {
        return context.getString(R.string.nc_ext_plugin_file);
    }

    @Override
    public void onClick(Fragment currentFragment, NCExtension extension, int index) {
        if (extension == null) {
            RLog.e(TAG, "extension null");
            return;
        }
        mConversationIdentifier = extension.getChannelIdentifier();
        FragmentActivity activity = currentFragment.getActivity();
        if (activity != null) {
            mContext = activity.getApplicationContext();
        }

        // Devices running Android 11+ (regardless of target SDK) cannot access Android/data/ and
        // Android/obb/ directories; minimal impact, ignored.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        extension.startActivityForPluginResult(intent, REQUEST_FILE, this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_FILE) {
            return;
        }
        if (resultCode != Activity.RESULT_OK || data == null || mContext == null) {
            RLog.e(TAG, "conversationType or context null");
            return;
        }
        List<Uri> selectedUris = collectSelectedUris(data);
        if (selectedUris.isEmpty()) {
            RLog.e(TAG, "selectedUris empty");
            return;
        }
        int takeFlags =
                data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        ExecutorHelper.getInstance()
                .diskIO()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                sendSelectedFiles(selectedUris, takeFlags);
                            }
                        });
    }

    private List<Uri> collectSelectedUris(Intent data) {
        Set<Uri> uris = new LinkedHashSet<>();
        android.content.ClipData clipData = data.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                android.content.ClipData.Item item = clipData.getItemAt(i);
                if (item != null && item.getUri() != null) {
                    uris.add(item.getUri());
                }
            }
        }
        Uri singleUri = data.getData();
        if (singleUri != null) {
            uris.add(singleUri);
        }
        ArrayList<Uri> streamUris = data.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (streamUris != null) {
            uris.addAll(streamUris);
        }
        return new ArrayList<>(uris);
    }

    private void sendSelectedFiles(List<Uri> selectedUris, int takeFlags) {
        for (int i = 0; i < selectedUris.size(); i++) {
            Uri uri = selectedUris.get(i);
            if (uri == null) {
                continue;
            }
            takePersistableUriPermission(uri, takeFlags);
            if (!FileUtils.isFileExistsWithUri(mContext, uri)) {
                ToastUtils.s(mContext, mContext.getString(R.string.nc_file_not_exist));
                continue;
            }
            sendSelectedFile(uri);
            if (i < selectedUris.size() - 1) {
                try {
                    Thread.sleep(TIME_DELAY);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void takePersistableUriPermission(Uri uri, int takeFlags) {
        try {
            mContext.getContentResolver().takePersistableUriPermission(uri, takeFlags);
        } catch (Exception e) {
            RLog.e(
                    TAG,
                    "takePersistableUriPermission failed, uri="
                            + uri
                            + ", error="
                            + e.getMessage());
        }
    }

    private void sendSelectedFile(Uri uri) {
        try {
            // Get file name and size from ContentResolver
            String fileName = null;
            long fileSize = 0;
            android.database.Cursor cursor =
                    mContext.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int nameIdx =
                                cursor.getColumnIndex(
                                        android.provider.OpenableColumns.DISPLAY_NAME);
                        int sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                        if (nameIdx >= 0) {
                            fileName = cursor.getString(nameIdx);
                        }
                        if (sizeIdx >= 0) {
                            fileSize = cursor.getLong(sizeIdx);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
            if (TextUtils.isEmpty(fileName)) {
                fileName = "file_" + System.currentTimeMillis();
            }
            // Copy content URI to a locally accessible path;
            // nexconn SDK upload requires a real file path
            String savePath = ChatUIStorageUtils.getFileSavePath(mContext);
            boolean copied = FileUtils.copyFileToInternal(mContext, uri, savePath, fileName);
            if (!copied) {
                RLog.e(TAG, "copy file to internal failed");
                return;
            }
            String localFilePath = savePath + java.io.File.separator + fileName;
            ai.nexconn.chat.message.FileMessage fileMessage =
                    new ai.nexconn.chat.message.FileMessage();
            // Use file:// URI to make sdk treat it as local file instead of remote media url.
            fileMessage.setLocalPath(
                    android.net.Uri.fromFile(new java.io.File(localFilePath)).toString());
            fileMessage.setName(fileName);
            fileMessage.setSize(fileSize);
            ai.nexconn.chatui.NCChatUI.sendMediaMessage(
                    mConversationIdentifier,
                    new ai.nexconn.chat.params.SendMediaMessageParams(fileMessage),
                    null);
        } catch (Exception e) {
            RLog.e(TAG, "select file exception" + e);
        }
    }
}
