package ai.nexconn.chatui.shortvideo;

import static android.app.Activity.RESULT_OK;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.params.SendMediaMessageParams;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.extension.NCExtension;
import ai.nexconn.chatui.channel.extension.component.plugin.IPluginModule;
import ai.nexconn.chatui.channel.extension.component.plugin.IPluginRequestPermissionResultCallback;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.shortvideo.record.ShortVideoRecordActivity;
import ai.nexconn.chatui.utils.file.FileUtils;
import ai.nexconn.chatui.utils.file.LibStorageUtils;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.permission.PermissionCheckUtil;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import java.io.File;

public class ShortVideoPlugin implements IPluginModule, IPluginRequestPermissionResultCallback {
    private static final String TAG = "ShortVideoPlugin";
    protected ChannelIdentifier conversationIdentifier;
    protected Context context;
    private static final int REQUEST_SIGHT = 104;

    @Override
    public Drawable obtainDrawable(Context context) {
        this.context = context;
        int drawableResId =
                ChatUIThemeManager.getAttrResId(
                        context, ai.nexconn.chatui.R.attr.nc_conversation_plugin_item_sight_img);
        return ContextCompat.getDrawable(context, drawableResId);
    }

    @Override
    public String obtainTitle(Context context) {
        this.context = context;
        return context.getString(R.string.nc_plugin_sight);
    }

    @Override
    public void onClick(Fragment currentFragment, NCExtension extension, int index) {
        this.context = currentFragment.getContext();
        if (extension == null) {
            RLog.e(TAG, "onClick extension null");
            return;
        }
        // Short video recordings are saved to private directory, no storage permission needed
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        conversationIdentifier = extension.getConversationIdentifier();
        if (PermissionCheckUtil.checkPermissions(currentFragment.getActivity(), permissions)) {
            startSightRecord(currentFragment, extension);
        } else {
            extension.requestPermissionForPluginResult(
                    permissions,
                    IPluginRequestPermissionResultCallback.REQUEST_CODE_PERMISSION_PLUGIN,
                    this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK
                && requestCode == REQUEST_SIGHT
                && data != null
                && context != null) {
            String fileUrl = data.getStringExtra("recordSightUrl");
            File file = new File(fileUrl);
            if (file.exists() && conversationIdentifier != null) {
                int recordTime = data.getIntExtra("recordSightTime", 0);
                ai.nexconn.chat.message.ShortVideoMessage shortVideoMsg =
                        new ai.nexconn.chat.message.ShortVideoMessage();
                // Must use file:// scheme so SightMessage.obtain(Uri, int) can resolve the path
                shortVideoMsg.setLocalPath("file://" + fileUrl);
                shortVideoMsg.setDuration(recordTime);
                SendMediaMessageParams params = new SendMediaMessageParams(shortVideoMsg);
                NCChatUI.sendMediaMessage(conversationIdentifier, params, null);
            }
        }
    }

    private void startSightRecord(Fragment currentFragment, NCExtension extension) {
        FragmentActivity activity = currentFragment.getActivity();
        if (activity == null || activity.isDestroyed() || activity.isFinishing()) {
            RLog.e(TAG, "startSightRecord activity null");
            return;
        }
        File saveDir = null;
        saveDir =
                new File(
                        FileUtils.getMediaDownloadDir(
                                currentFragment.getContext(), LibStorageUtils.VIDEO));
        boolean successMkdir = saveDir.mkdirs();
        if (!successMkdir) {
            RLog.e(TAG, "Created folders UnSuccessfully");
        }

        Intent intent = new Intent(currentFragment.getActivity(), ShortVideoRecordActivity.class);
        if (saveDir != null) {
            intent.putExtra("recordSightDir", saveDir.getAbsolutePath());
        }
        int maxRecordDuration = 10;
        try {
            maxRecordDuration =
                    currentFragment
                            .getActivity()
                            .getResources()
                            .getInteger(R.integer.nc_sight_max_record_duration);
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
        int maxVideoDurationSeconds = NCEngine.getAppSettings().getMaxVideoDurationSeconds();
        if (maxVideoDurationSeconds > 0 && maxRecordDuration > maxVideoDurationSeconds) {
            maxRecordDuration = maxVideoDurationSeconds;
        }
        intent.putExtra("maxRecordDuration", maxRecordDuration); // seconds
        extension.startActivityForPluginResult(intent, REQUEST_SIGHT, this);
    }

    @Override
    public boolean onRequestPermissionResult(
            Fragment fragment,
            NCExtension extension,
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (PermissionCheckUtil.checkPermissions(fragment.getActivity(), permissions)) {
            startSightRecord(fragment, extension);
        } else {
            PermissionCheckUtil.showRequestPermissionFailedAlter(
                    fragment.getContext(), permissions, grantResults);
        }
        return true;
    }
}
