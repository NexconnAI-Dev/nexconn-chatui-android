package ai.nexconn.chatui.picture;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.picture.config.PictureConfig;
import ai.nexconn.chatui.picture.config.PictureMimeType;
import ai.nexconn.chatui.picture.entity.LocalMedia;
import ai.nexconn.chatui.picture.permissions.PermissionChecker;
import ai.nexconn.chatui.picture.tools.MediaUtils;
import ai.nexconn.chatui.picture.tools.PictureFileUtils;
import ai.nexconn.chatui.picture.tools.SdkVersionUtils;
import ai.nexconn.chatui.picture.tools.ToastUtils;
import ai.nexconn.chatui.utils.permission.PermissionCheckUtil;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PictureSelectorCameraEmptyActivity extends PictureBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        saveAndApplyStatusBar(Color.WHITE, true);
        if (PermissionCheckUtil.checkMediaStoragePermissions(this)) {
            onTakePhoto();
        } else {
            ToastUtils.s(getContext(), getString(R.string.nc_picture_camera));
            closeActivity();
            return;
        }
        setTheme(R.style.Picture_Theme_Translucent);
    }

    @Override
    public int getResourceId() {
        return R.layout.nc_picture_empty;
    }

    @Override
    protected void initWidgets() {
        super.initWidgets();
    }

    /** Start camera */
    private void onTakePhoto() {
        // Start camera, first check whether the device has camera permission
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            startCamera();
        } else {
            PermissionChecker.requestPermissions(
                    this,
                    new String[] {Manifest.permission.CAMERA},
                    PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE);
        }
    }

    /** Start the corresponding camera based on type */
    private void startCamera() {
        switch (config.chooseMode) {
            case PictureConfig.TYPE_ALL:
            case PictureConfig.TYPE_IMAGE:
                // Take photo
                startOpenCamera();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PictureConfig.REQUEST_CAMERA:
                    requestCamera(data);
                    break;
                default:
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            closeActivity();
        }
    }

    /**
     * Process camera capture result
     *
     * @param data
     */
    private void requestCamera(Intent data) {
        // on take photo success
        String mimeType = null;
        long duration = 0;
        boolean isAndroidQ = SdkVersionUtils.checkedAndroid_Q();
        if (TextUtils.isEmpty(cameraPath) || new File(cameraPath) == null) {
            return;
        }
        int[] newSize = new int[2];
        final File file = new File(cameraPath);
        if (!isAndroidQ) {
            new PictureMediaScannerConnection(
                    getApplicationContext(),
                    cameraPath,
                    new PictureMediaScannerConnection.ScanListener() {
                        @Override
                        public void onScanFinish() {
                            // do nothing
                        }
                    });
        }
        LocalMedia media = new LocalMedia();
        mimeType = PictureMimeType.fileToType(file);
        if (PictureMimeType.eqImage(mimeType)) {
            int degree = PictureFileUtils.readPictureDegree(this, cameraPath);
            PictureFileUtils.rotateImage(degree, cameraPath);
            newSize = MediaUtils.getLocalImageWidthOrHeight(cameraPath);
        } else {
            newSize = MediaUtils.getLocalVideoSize(cameraPath);
            duration = MediaUtils.extractDuration(getContext(), false, cameraPath);
        }
        media.setDuration(duration);
        media.setWidth(newSize[0]);
        media.setHeight(newSize[1]);
        media.setPath(cameraPath);
        media.setMimeType(mimeType);
        media.setSize(PictureFileUtils.getMediaSize(getContext(), cameraPath));
        media.setChooseModel(config.chooseMode);
        cameraHandleResult(media, mimeType);
    }

    /**
     * Camera post-processing handler
     *
     * @param media
     * @param mimeType
     */
    private void cameraHandleResult(LocalMedia media, String mimeType) {
        // No cropping, no compression, return result directly
        List<LocalMedia> result = new ArrayList<>();
        result.add(media);
        onResult(result);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        closeActivity();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (PermissionCheckUtil.checkPermissionResultIncompatible(permissions, grantResults)) {
            // The condition checking is also made in the super class,so return directly.
            return;
        }

        switch (requestCode) {
            case PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE:
                // Storage permission
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        onTakePhoto();
                    } else {
                        closeActivity();
                        ToastUtils.s(getContext(), getString(R.string.nc_picture_camera));
                    }
                }
                break;
            case PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE:
                // Camera permission
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onTakePhoto();
                } else {
                    closeActivity();
                    ToastUtils.s(getContext(), getString(R.string.nc_picture_camera));
                }
                break;
            default:
                break;
        }
    }
}
