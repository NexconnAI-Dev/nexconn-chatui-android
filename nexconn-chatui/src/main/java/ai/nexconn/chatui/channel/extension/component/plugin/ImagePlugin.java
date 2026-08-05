package ai.nexconn.chatui.channel.extension.component.plugin;

import static android.app.Activity.RESULT_OK;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.DirectChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.message.model.MessageType;
import ai.nexconn.chat.model.AppSettings;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.extension.NCExtension;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.manager.SendImageManager;
import ai.nexconn.chatui.manager.SendMediaManager;
import ai.nexconn.chatui.picture.PictureSelector;
import ai.nexconn.chatui.picture.config.PictureConfig;
import ai.nexconn.chatui.picture.config.PictureMimeType;
import ai.nexconn.chatui.picture.entity.LocalMedia;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.permission.PermissionCheckUtil;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import java.util.List;

public class ImagePlugin implements IPluginModule, IPluginRequestPermissionResultCallback {
    private static final String TAG = "ImagePlugin";
    ChannelIdentifier conversationIdentifier;
    private int mRequestCode = -1;

    @Override
    public Drawable obtainDrawable(Context context) {
        return ContextCompat.getDrawable(
                context,
                ChatUIThemeManager.getAttrResId(
                        context, R.attr.nc_conversation_plugin_item_image_img));
    }

    @Override
    public String obtainTitle(Context context) {
        return context.getString(R.string.nc_ext_plugin_image);
    }

    @Override
    public void onClick(Fragment currentFragment, NCExtension extension, int index) {
        if (extension == null) {
            RLog.e(TAG, "onClick extension null");
            return;
        }
        conversationIdentifier = extension.getChannelIdentifier();
        mRequestCode = ((index + 1) << 8) + (PictureConfig.CHOOSE_REQUEST & 0xff);

        FragmentActivity activity = currentFragment.getActivity();
        if (activity == null || activity.isDestroyed() || activity.isFinishing()) {
            RLog.e(TAG, "onClick activity null");
            return;
        }

        if (PermissionCheckUtil.checkMediaStoragePermissions(currentFragment.getContext())) {
            openPictureSelector(currentFragment);
        } else {
            String[] permissions =
                    PermissionCheckUtil.getMediaStoragePermissions(currentFragment.getContext());
            extension.requestPermissionForPluginResult(
                    permissions,
                    IPluginRequestPermissionResultCallback.REQUEST_CODE_PERMISSION_PLUGIN,
                    this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (conversationIdentifier == null) {
                RLog.e(
                        TAG,
                        "onActivityResult conversationIdentifier is null, requestCode="
                                + requestCode
                                + ",resultCode="
                                + resultCode);
                return;
            }
            // Image, video, and audio selection result callback
            List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);
            if (selectList != null && selectList.size() > 0) {
                boolean sendOrigin = selectList.get(0).isOriginal();
                for (LocalMedia item : selectList) {
                    String mimeType = item.getMimeType();
                    if (mimeType.startsWith("image")) {
                        SendImageManager.getInstance()
                                .sendImage(conversationIdentifier, item, sendOrigin);
                        if (conversationIdentifier.getChannelType() == ChannelType.DIRECT) {
                            new DirectChannel(conversationIdentifier.getChannelId())
                                    .sendTypingStatus(MessageType.IMAGE);
                        }
                    } else if (mimeType.startsWith("video")) {
                        Uri path = Uri.parse(item.getPath());
                        if (TextUtils.isEmpty(path.getScheme())) {
                            path = Uri.parse("file://" + item.getPath());
                        }
                        SendMediaManager.getInstance()
                                .sendMedia(
                                        ai.nexconn.chatui.NCChatUI.getContext(),
                                        conversationIdentifier,
                                        path,
                                        item.getDuration());
                        if (conversationIdentifier.getChannelType() == ChannelType.DIRECT) {
                            new DirectChannel(conversationIdentifier.getChannelId())
                                    .sendTypingStatus(MessageType.SHORT_VIDEO);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean onRequestPermissionResult(
            Fragment fragment,
            NCExtension extension,
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (PermissionCheckUtil.checkPermissions(fragment.getActivity(), permissions)) {
            if (requestCode != -1) {
                openPictureSelector(fragment);
            }
        } else {
            if (PermissionCheckUtil.checkMediaStoragePermissions(fragment.getContext())) {
                openPictureSelector(fragment);
            } else {
                if (fragment.getActivity() != null) {
                    PermissionCheckUtil.showRequestPermissionFailedAlter(
                            fragment.getContext(), permissions, grantResults);
                }
            }
        }
        return true;
    }

    private void openPictureSelector(Fragment currentFragment) {
        AppSettings settings = NCEngine.getAppSettings();
        int gifLimitKb = settings.getGifLimitSize();
        long gifLimitBytes = gifLimitKb > 0 ? gifLimitKb * 1024L : -1L;
        int selectorGifLimit =
                gifLimitBytes > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) gifLimitBytes;
        PictureSelector.create(currentFragment)
                .openGallery(
                        NCChatUIConfig.channelConfig().NC_media_selector_contain_video
                                ? PictureMimeType.ofAll()
                                : PictureMimeType.ofImage())
                .loadImageEngine(NCChatUIConfig.featureConfig().getChatUIImageEngine())
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .videoDurationLimit(settings.getMaxVideoDurationSeconds())
                .gifSizeLimit(selectorGifLimit)
                .maxSelectNum(9)
                .imageSpanCount(3)
                .isGif(true)
                .forResult(mRequestCode);
    }
}
