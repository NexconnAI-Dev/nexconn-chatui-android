package ai.nexconn.chatui.utils.file;

import ai.nexconn.chatui.R;
// TODO: Phase 5 - Replace with nexconn equivalent
import ai.nexconn.chatui.utils.log.RLog;
import android.annotation.SuppressLint;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import androidx.annotation.StringRes;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

/** Storage utility class for ChatUI media operations. */
public class ChatUIStorageUtils {
    private static final String TAG = "LibStorageUtils";
    private static final String VIDEO_SUFFIX = ".mp4";

    public static class MediaType {
        public static final String IMAGE = "image";
        public static final String VIDEO = "video";
    }

    public static boolean isScopedStorageMode(Context context) {
        return LibStorageUtils.isScopedStorageMode(context);
    }

    public static boolean isBuildAndTargetForQ(Context context) {
        return LibStorageUtils.isBuildAndTargetForQ(context);
    }

    public static String getImageSavePath(Context context) {
        return getSavePath(context, LibStorageUtils.IMAGE, R.string.nc_image_default_saved_path);
    }

    public static String getVideoSavePath(Context context) {
        return getSavePath(context, LibStorageUtils.VIDEO, R.string.nc_video_default_saved_path);
    }

    public static String getFileSavePath(Context context) {
        return getSavePath(context, LibStorageUtils.FILE, R.string.nc_file_default_saved_path);
    }

    public static String getSavePath(Context context, String type, @StringRes int res) {
        boolean sdCardExist =
                Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        String result = context.getCacheDir().getPath();
        if (!sdCardExist) {
            RLog.d(TAG, "getSavePath error, sdcard does not exist.");
            return result;
        }

        String resourcePath = context.getString(res);
        if (TextUtils.isEmpty(resourcePath)) {
            resourcePath = type;
        }

        File path = context.getExternalFilesDir(resourcePath);
        File file = new File(path, type);
        if (!file.exists() && !file.mkdirs()) {
            result = path.getPath();
        } else {
            result = file.getPath();
        }

        return result;
    }

    /**
     * @param context context
     * @param file source file
     * @param outputFileName output file name
     */
    private static boolean copyVideoToPublicDir(Context context, File file, String outputFileName) {
        if (context == null || file == null || !file.exists()) {
            RLog.e(TAG, "file is not exist");
            return false;
        }
        boolean result = true;
        if (!ChatUIStorageUtils.isBuildAndTargetForQ(context)) {
            File dirFile =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
            if (dirFile != null && !dirFile.exists()) {
                boolean mkdirResult = dirFile.mkdirs();
                if (!mkdirResult) {
                    RLog.e(TAG, "mkdir fail,dir path is " + dirFile.getAbsolutePath());
                    return false;
                }
            }
            if (dirFile == null) {
                RLog.e(TAG, "dirFile is null");
                return false;
            }

            FileInputStream fis = null;
            FileOutputStream fos = null;
            try {
                String name;
                if (!TextUtils.isEmpty(outputFileName)) {
                    name = outputFileName;
                } else {
                    name = file.getName();
                }
                String filePath = dirFile.getPath() + "/" + name;
                // Append suffix if missing. Some device galleries cannot recognize video files
                // without extension.
                String suffix = FileUtils.getSuffix(name);
                if (TextUtils.isEmpty(suffix)) {
                    filePath = filePath + VIDEO_SUFFIX;
                }
                fis = new FileInputStream(file);
                fos = new FileOutputStream(filePath);
                copy(fis, fos);
                File destFile = new File(filePath);
                updatePhotoMedia(destFile, context);
            } catch (FileNotFoundException e) {
                result = false;
                RLog.e(TAG, "copyVideoToPublicDir file not found", e);
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException e) {
                    RLog.e(TAG, "copyVideoToPublicDir: ", e);
                }
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {
                    RLog.e(TAG, "copyVideoToPublicDir: ", e);
                }
            }
        } else {
            result = copyVideoToPublicDirForQ(context, file, outputFileName);
        }
        return result;
    }

    // Notify the media gallery to refresh data
    public static void updatePhotoMedia(File file, Context context) {
        if (file != null && file.exists() && context != null) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            context.sendBroadcast(intent);
        }
    }

    private static boolean copyVideoToPublicDirForQ(
            Context context, File file, String outputFileName) {
        if (context == null || file == null || !file.exists() || !file.isFile()) {
            RLog.e(TAG, "file is not Found or context is null ");
            return false;
        }
        boolean result = true;
        String filePath = "";
        String name;
        if (!TextUtils.isEmpty(outputFileName)) {
            name = outputFileName;
        } else {
            name = file.getName();
        }
        Uri uri = insertVideoIntoMediaStore(context, name);
        if (uri == null) {
            return false;
        }
        filePath = uri.getPath();
        try {
            ParcelFileDescriptor w = context.getContentResolver().openFileDescriptor(uri, "w");
            writeToPublicDir(file, w);
        } catch (FileNotFoundException pE) {
            RLog.e(TAG, "copyVideoToPublicDir uri is not Found, uri is" + uri.toString());
            result = false;
        }
        File destFile = new File(filePath);
        updatePhotoMedia(destFile, context);
        return result;
    }

    private static boolean copyImageToPublicDir(
            Context pContext, File file, String outputFileName) {
        if (pContext == null || file == null || !file.exists() || !file.isFile()) {
            RLog.e(TAG, "file is not Found or context is null ");
            return false;
        }
        boolean result = true;
        if (!ChatUIStorageUtils.isBuildAndTargetForQ(pContext)) {
            File dirFile =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            if (dirFile != null && !dirFile.exists()) {
                boolean mkdirResult = dirFile.mkdirs();
                if (!mkdirResult) {
                    RLog.e(TAG, "mkdir fail,dir path is " + dirFile.getAbsolutePath());
                    return false;
                }
            }
            if (dirFile == null) {
                RLog.e(TAG, "dirFile is null");
                return false;
            }

            FileInputStream fis = null;
            FileOutputStream fos = null;
            try {
                String name;
                if (!TextUtils.isEmpty(outputFileName)) {
                    name = outputFileName;
                } else {
                    String imgMimeType = getImgMimeType(file);
                    int i = imgMimeType.lastIndexOf('/');
                    name = "Nexconn_Image_" + System.currentTimeMillis();
                    if (i != -1) {
                        name = name + "." + imgMimeType.substring(i + 1);
                    }
                }

                String filePath = dirFile.getPath() + "/" + name;
                fis = new FileInputStream(file);
                fos = new FileOutputStream(filePath);
                copy(fis, fos);
                File destFile = new File(filePath);
                updatePhotoMedia(destFile, pContext);
            } catch (FileNotFoundException e) {
                result = false;
                RLog.e(TAG, "copyImageToPublicDir file not found", e);
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException e) {
                    RLog.e(TAG, "copyImageToPublicDir: ", e);
                }
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {
                    RLog.e(TAG, "copyImageToPublicDir: ", e);
                }
            }
        } else {
            String name;
            String imgMimeType = getImgMimeType(file);
            if (!TextUtils.isEmpty(outputFileName)) {
                name = outputFileName;
            } else {
                int i = imgMimeType.lastIndexOf('/');
                name = "Nexconn_Image_" + System.currentTimeMillis();
                if (i != -1) {
                    name = name + "." + imgMimeType.substring(i + 1);
                }
            }
            Uri uri = insertImageIntoMediaStore(pContext, name, imgMimeType);
            if (uri == null) {
                return false;
            }
            try {
                ParcelFileDescriptor w = pContext.getContentResolver().openFileDescriptor(uri, "w");
                writeToPublicDir(file, w);
            } catch (FileNotFoundException pE) {
                result = false;
                RLog.e(TAG, "copyImageToPublicDir uri is not Found, uri is" + uri.toString());
            }
        }
        return result;
    }

    public static Uri insertImageIntoMediaStore(Context context, String fileName, String mimeType) {
        ContentResolver resolver = context.getContentResolver();
        if (!checkUriValid(resolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) {
            RLog.e(TAG, "insertImageIntoMediaStore error: uri valid");
            return null;
        }

        Uri uri = null;
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        } catch (Exception e) {
            RLog.e(TAG, "insertImageIntoMediaStore error: ", e);
        }
        return uri;
    }

    public static Uri insertVideoIntoMediaStore(Context context, String fileName) {
        ContentResolver resolver = context.getContentResolver();
        if (!checkUriValid(resolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)) {
            RLog.e(TAG, "insertVideoIntoMediaStore error: uri valid");
            return null;
        }

        Uri uri = null;
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
            contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
        } catch (Exception e) {
            RLog.e(TAG, "insertVideoIntoMediaStore error: ", e);
        }
        return uri;
    }

    private static boolean checkUriValid(ContentResolver resolver, Uri uri) {
        boolean acquire = false;
        try (@SuppressLint("Recycle")
                ContentProviderClient client = resolver.acquireContentProviderClient(uri)) {
            acquire = client != null;
        }
        return acquire;
    }

    public static void writeToPublicDir(File pFile, ParcelFileDescriptor pParcelFileDescriptor) {
        if (pFile == null || !pFile.exists()) {
            RLog.e(TAG, "file is not Found or is null ");
            return;
        }
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(pFile);
            fos = new FileOutputStream(pParcelFileDescriptor.getFileDescriptor());
            copy(fis, fos);
        } catch (FileNotFoundException pE) {
            RLog.e(
                    TAG,
                    "writeToPublicDir file is not found file path is " + pFile.getAbsolutePath());
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                RLog.e(TAG, "writeToPublicDir: ", e);
            }
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                RLog.e(TAG, "writeToPublicDir: ", e);
            }
        }
    }

    public static void read(ParcelFileDescriptor parcelFileDescriptor, File dst)
            throws IOException {
        FileInputStream istream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
        try {
            FileOutputStream ostream = new FileOutputStream(dst);
            try {
                copy(istream, ostream);
            } finally {
                ostream.close();
            }
        } finally {
            istream.close();
        }
    }

    public static void copy(FileInputStream ist, FileOutputStream ost) {
        if (ist == null || ost == null) return;
        FileChannel fileChannelInput = null;
        FileChannel fileChannelOutput = null;
        try {
            fileChannelInput = ist.getChannel();
            fileChannelOutput = ost.getChannel();
            fileChannelInput.transferTo(0, fileChannelInput.size(), fileChannelOutput);
        } catch (IOException e) {
            RLog.e(TAG, "copy method error", e);
        } finally {
            try {
                ist.close();
                if (fileChannelInput != null) fileChannelInput.close();
                ost.close();
                if (fileChannelOutput != null) fileChannelOutput.close();
            } catch (IOException e) {
                RLog.e(TAG, "copy method error", e);
            }
        }
    }

    public static String getImgMimeType(File imgFile) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgFile.getPath(), options);
        return options.outMimeType;
    }

    /**
     * @param type MediaStore type: 0 for Images, 1 for Video, 2 for Audio
     * @param id MediaStore."xxx".Media._ID obtained via scanning
     * @return content uri
     */
    public Uri getContentUri(int type, String id) {
        Uri uri;
        switch (type) {
            case 0:
                uri =
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                .buildUpon()
                                .appendPath(String.valueOf(id))
                                .build();
                break;
            case 1:
                uri =
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                .buildUpon()
                                .appendPath(String.valueOf(id))
                                .build();
                break;
            case 2:
                uri =
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                                .buildUpon()
                                .appendPath(String.valueOf(id))
                                .build();
                break;
            default:
                uri = null;
        }
        return uri;
    }

    public InputStream getFileInputStreamWithUri(Context pContext, Uri pUri) {
        InputStream inputStream = null;
        ContentResolver cr = pContext.getContentResolver();
        try {
            AssetFileDescriptor r = cr.openAssetFileDescriptor(pUri, "r");
            ParcelFileDescriptor parcelFileDescriptor = r.getParcelFileDescriptor();
            if (parcelFileDescriptor != null) {
                inputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
            }
        } catch (FileNotFoundException e) {
            RLog.e(TAG, "getFileInputStreamWithUri: ", e);
        }
        return inputStream;
    }

    /**
     * @param context context
     * @param file source file
     * @param type ChatUIStorageUtils.MediaType
     * @return whether the media was saved to the public directory successfully
     */
    public static boolean saveMediaToPublicDir(Context context, File file, String type) {
        return saveMediaToPublicDir(context, file, null, type);
    }

    /**
     * @param context context
     * @param file source file
     * @param outputFileName output file name (without path)
     * @param type ChatUIStorageUtils.MediaType
     * @return whether the media was saved to the public directory successfully
     */
    public static boolean saveMediaToPublicDir(
            Context context, File file, String outputFileName, String type) {
        if (MediaType.IMAGE.equals(type)) {
            return copyImageToPublicDir(context, file, outputFileName);
        } else if (MediaType.VIDEO.equals(type)) {
            return copyVideoToPublicDir(context, file, outputFileName);
        } else {
            RLog.i(TAG, "type is error");
            return false;
        }
    }
}
