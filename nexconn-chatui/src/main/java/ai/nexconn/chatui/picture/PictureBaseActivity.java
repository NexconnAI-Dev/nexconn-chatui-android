package ai.nexconn.chatui.picture;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.picture.config.PictureConfig;
import ai.nexconn.chatui.picture.config.PictureSelectionConfig;
import ai.nexconn.chatui.picture.dialog.PictureLoadingDialog;
import ai.nexconn.chatui.picture.entity.LocalMedia;
import ai.nexconn.chatui.picture.entity.LocalMediaFolder;
import ai.nexconn.chatui.picture.tools.AttrsUtils;
import ai.nexconn.chatui.picture.tools.MediaUtils;
import ai.nexconn.chatui.picture.tools.PictureFileUtils;
import ai.nexconn.chatui.picture.tools.SdkVersionUtils;
import ai.nexconn.chatui.picture.tools.ToastUtils;
import ai.nexconn.chatui.utils.common.ChatUIUtils;
import ai.nexconn.chatui.utils.file.CursorUtils;
import ai.nexconn.chatui.utils.language.NCConfigurationManager;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.permission.PermissionCheckUtil;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class PictureBaseActivity extends AppCompatActivity {
    private static final String TAG = PictureBaseActivity.class.getCanonicalName();
    protected PictureSelectionConfig config;
    protected boolean openWhiteStatusBar, numComplete;
    protected int colorPrimary, colorPrimaryDark;
    protected String cameraPath;
    protected String originalPath;
    protected PictureLoadingDialog dialog;
    protected List<LocalMedia> selectionMedias;
    protected Handler mHandler;
    protected View container;
    private Integer previousStatusBarColor;
    private int previousSystemUiVisibility = -1;
    private boolean statusBarStateSaved = false;

    /**
     * Get the layout resource ID.
     *
     * @return
     */
    public abstract int getResourceId();

    protected void initWidgets() {
        // default implementation ignored
    }

    protected void initPictureSelectorStyle() {
        // default implementation ignored
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = NCConfigurationManager.getInstance().getConfigurationContext(newBase);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ChatUIUtils.fixAndroid8ActivityCrash(this);
        if (savedInstanceState != null) {
            config = savedInstanceState.getParcelable(PictureConfig.EXTRA_CONFIG);
            cameraPath = savedInstanceState.getString(PictureConfig.BUNDLE_CAMERA_PATH);
            originalPath = savedInstanceState.getString(PictureConfig.BUNDLE_ORIGINAL_PATH);
        } else {
            config = PictureSelectionConfig.getInstance();
        }
        setTheme(config.themeStyleId);
        super.onCreate(savedInstanceState);
        initConfig();
        int layoutResID = getResourceId();
        if (layoutResID != 0) {
            setContentView(layoutResID);
        }
        initWidgets();
        initPictureSelectorStyle();
    }

    /**
     * Get the Context.
     *
     * @return
     */
    protected Context getContext() {
        return this;
    }

    /** Initialize configuration parameters */
    private void initConfig() {
        // Selected image list
        selectionMedias =
                config.selectionMedias == null
                        ? new ArrayList<LocalMedia>()
                        : config.selectionMedias;
        openWhiteStatusBar = AttrsUtils.getTypeValueBoolean(this, R.attr.picture_statusFontColor);

        numComplete = AttrsUtils.getTypeValueBoolean(this, R.attr.picture_style_numComplete);

        config.checkNumMode =
                AttrsUtils.getTypeValueBoolean(this, R.attr.picture_style_checkNumMode);

        // Title bar background color
        colorPrimary =
                AttrsUtils.getTypeValueColor(
                        this,
                        ChatUIThemeManager.getAttrResId(
                                getContext(), R.attr.nc_common_background_color));

        // Status bar color value
        colorPrimaryDark =
                AttrsUtils.getTypeValueColor(
                        this,
                        ChatUIThemeManager.getAttrResId(
                                getContext(), R.attr.nc_common_background_color));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(PictureConfig.BUNDLE_CAMERA_PATH, cameraPath);
        outState.putString(PictureConfig.BUNDLE_ORIGINAL_PATH, originalPath);
        outState.putParcelable(PictureConfig.EXTRA_CONFIG, config);
    }

    /** loading dialog */
    protected void showPleaseDialog() {
        if (!isFinishing()) {
            dismissDialog();
            dialog = new PictureLoadingDialog(getContext());
            dialog.show();
        }
    }

    /** dismiss dialog */
    protected void dismissDialog() {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
                dialog = null;
            }
        } catch (Exception e) {
            dialog = null;
            RLog.e(TAG, e.getMessage());
        }
    }

    /**
     * compress or callback
     *
     * @param result
     */
    protected void handlerResult(List<LocalMedia> result) {
        onResult(result);
    }

    /**
     * If there are no albums, create a Camera Roll album first.
     *
     * @param folders
     */
    protected void createNewFolder(List<LocalMediaFolder> folders) {
        if (folders.size() == 0) {
            // No albums found, create a Camera Roll album first
            LocalMediaFolder newFolder = new LocalMediaFolder();
            String folderName = getString(R.string.nc_picture_camera_roll);
            newFolder.setName(folderName);
            newFolder.setFirstImagePath("");
            folders.add(newFolder);
        }
    }

    /**
     * Insert image into the camera folder.
     *
     * @param path
     * @param imageFolders
     * @return
     */
    protected LocalMediaFolder getImageFolder(String path, List<LocalMediaFolder> imageFolders) {
        File imageFile = new File(path);
        File folderFile = imageFile.getParentFile();

        for (LocalMediaFolder folder : imageFolders) {
            if (folder.getName().equals(folderFile.getName())) {
                return folder;
            }
        }
        LocalMediaFolder newFolder = new LocalMediaFolder();
        newFolder.setName(folderFile.getName());
        newFolder.setFirstImagePath(path);
        imageFolders.add(newFolder);
        return newFolder;
    }

    /**
     * return image result
     *
     * @param images
     */
    protected void onResult(List<LocalMedia> images) {
        if (images == null) {
            return;
        }
        if (config.camera
                && config.selectionMode == PictureConfig.MULTIPLE
                && selectionMedias != null) {
            images.addAll(images.size() > 0 ? images.size() - 1 : 0, selectionMedias);
        }
        if (config.isCheckOriginalImage) {
            int size = images.size();
            for (int i = 0; i < size; i++) {
                LocalMedia media = images.get(i);
                media.setOriginal(true);
            }
        }
        Intent intent = PictureSelector.putIntentResult(images);
        setResult(RESULT_OK, intent);
        closeActivity();
    }

    /** Close Activity */
    protected void closeActivity() {
        finish();
        if (config.camera) {
            overridePendingTransition(0, R.anim.nc_picture_anim_fade_out);
        } else {
            overridePendingTransition(0, R.anim.nc_picture_anim_exit);
        }
    }

    @Override
    protected void onDestroy() {
        restoreStatusBarAppearance();
        super.onDestroy();
        dismissDialog();
    }

    protected void saveAndApplyStatusBar(int color, boolean isDarkText) {
        saveStatusBarAppearance();
        applyStatusBarAppearance(color, isDarkText);
    }

    private void saveStatusBarAppearance() {
        if (statusBarStateSaved) {
            return;
        }
        Window window = getWindow();
        if (window == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            previousStatusBarColor = window.getStatusBarColor();
        }
        View decorView = window.getDecorView();
        previousSystemUiVisibility =
                decorView != null ? decorView.getSystemUiVisibility() : previousSystemUiVisibility;
        statusBarStateSaved = true;
    }

    private void applyStatusBarAppearance(int color, boolean isDarkText) {
        Window window = getWindow();
        if (window == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(color);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = window.getDecorView();
            if (decorView != null) {
                int visibility = decorView.getSystemUiVisibility();
                if (isDarkText) {
                    visibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                } else {
                    visibility &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
                decorView.setSystemUiVisibility(visibility);
            }
        }
    }

    protected void restoreStatusBarAppearance() {
        if (!statusBarStateSaved) {
            return;
        }
        Window window = getWindow();
        if (window != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && previousStatusBarColor != null) {
                window.setStatusBarColor(previousStatusBarColor);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && previousSystemUiVisibility != -1) {
                View decorView = window.getDecorView();
                if (decorView != null) {
                    decorView.setSystemUiVisibility(previousSystemUiVisibility);
                }
            }
        }
        statusBarStateSaved = false;
    }

    /**
     * Fix issue where some phones generate a duplicate photo in DCIM when taking pictures.
     *
     * @param id
     * @param eqVideo
     */
    @Deprecated
    protected void removeImage(int id, boolean eqVideo) {
        try {
            Uri uri =
                    eqVideo
                            ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String selection =
                    eqVideo
                            ? MediaStore.Video.Media._ID + "=?"
                            : MediaStore.Images.Media._ID + "=?";
            CursorUtils.delete(this, uri, selection, new String[] {Long.toString(id)});
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
        }
    }

    /** Starts camera, preview, or crop. */
    protected void startOpenCamera() {
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                Uri imageUri;
                if (SdkVersionUtils.checkedAndroid_Q()) {
                    imageUri = MediaUtils.createImageUri(getApplicationContext());
                    if (imageUri != null) {
                        cameraPath = imageUri.toString();
                    }
                } else {
                    int chooseMode =
                            config.chooseMode == PictureConfig.TYPE_ALL
                                    ? PictureConfig.TYPE_IMAGE
                                    : config.chooseMode;
                    File cameraFile =
                            PictureFileUtils.createCameraFile(
                                    getApplicationContext(),
                                    chooseMode,
                                    config.cameraFileName,
                                    config.suffixType);
                    cameraPath = cameraFile.getAbsolutePath();
                    imageUri = PictureFileUtils.parUri(this, cameraFile);
                }
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA);
            }
        } catch (Exception e) {
            RLog.i(TAG, e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (PermissionCheckUtil.checkPermissionResultIncompatible(permissions, grantResults)) {
            if (getContext() != null) {
                ToastUtils.s(getContext(), getString(R.string.nc_permission_request_failed));
            }
            return;
        }

        switch (requestCode) {
            case PictureConfig.APPLY_AUDIO_PERMISSIONS_CODE:
                // Audio recording permission
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                    if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA);
                    }
                } else {
                    ToastUtils.s(getContext(), getString(R.string.nc_picture_audio));
                }
                break;
            default:
                break;
        }
    }
}
