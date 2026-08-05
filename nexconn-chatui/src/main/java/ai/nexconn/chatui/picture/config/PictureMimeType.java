package ai.nexconn.chatui.picture.config;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.utils.file.CursorUtils;
import ai.nexconn.chatui.utils.log.RLog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import java.io.File;

public final class PictureMimeType {
    private static final String TAG = PictureMimeType.class.getSimpleName();

    public static final int ofAll() {
        return PictureConfig.TYPE_ALL;
    }

    public static final int ofImage() {
        return PictureConfig.TYPE_IMAGE;
    }

    public static final int ofVideo() {
        return PictureConfig.TYPE_VIDEO;
    }

    public static final String ofPNG() {
        return MIME_TYPE_PNG;
    }

    public static final String ofJPEG() {
        return MIME_TYPE_JPEG;
    }

    public static final String ofBMP() {
        return MIME_TYPE_BMP;
    }

    public static final String ofGIF() {
        return MIME_TYPE_GIF;
    }

    public static final String ofWEBP() {
        return MIME_TYPE_WEBP;
    }

    public static final String of3GP() {
        return MIME_TYPE_3GP;
    }

    public static final String ofMP4() {
        return MIME_TYPE_MP4;
    }

    public static final String ofMPEG() {
        return MIME_TYPE_MPEG;
    }

    public static final String ofAVI() {
        return MIME_TYPE_AVI;
    }

    private static final String MIME_TYPE_PNG = "image/png";
    private static final String MIME_TYPE_JPEG = "image/jpeg";
    private static final String MIME_TYPE_BMP = "image/bmp";
    private static final String MIME_TYPE_GIF = "image/gif";
    private static final String MIME_TYPE_WEBP = "image/webp";

    private static final String MIME_TYPE_3GP = "video/3gp";
    private static final String MIME_TYPE_MP4 = "video/mp4";
    private static final String MIME_TYPE_MPEG = "video/mpeg";
    private static final String MIME_TYPE_AVI = "video/avi";

    @Deprecated
    public static int isPictureType(String pictureType) {
        switch (pictureType) {
            case "video/3gp":
            case "video/3gpp":
            case "video/3gpp2":
            case "video/avi":
            case "video/mp4":
            case "video/quicktime":
            case "video/x-msvideo":
            case "video/x-matroska":
            case "video/mpeg":
            case "video/webm":
            case "video/mp2ts":
                return PictureConfig.TYPE_VIDEO;
            default:
                return PictureConfig.TYPE_IMAGE;
        }
    }

    /**
     * Check whether it is a GIF
     *
     * @param mimeType
     * @return
     */
    public static boolean isGif(String mimeType) {
        return mimeType != null && (mimeType.equals("image/gif") || mimeType.equals("image/GIF"));
    }

    /**
     * Check whether it is a video
     *
     * @param pictureType
     * @return
     */
    @Deprecated
    public static boolean isVideo(String pictureType) {
        switch (pictureType) {
            case "video/3gp":
            case "video/3gpp":
            case "video/3gpp2":
            case "video/avi":
            case "video/mp4":
            case "video/quicktime":
            case "video/x-msvideo":
            case "video/x-matroska":
            case "video/mpeg":
            case "video/webm":
            case "video/mp2ts":
                return true;
            default:
                return false;
        }
    }

    /**
     * Check whether it is a video
     *
     * @param mimeType
     * @return
     */
    public static boolean eqVideo(String mimeType) {
        return mimeType != null && mimeType.startsWith(MIME_TYPE_PREFIX_VIDEO);
    }

    /**
     * Check whether it is an image
     *
     * @param mimeType
     * @return
     */
    public static boolean eqImage(String mimeType) {
        return mimeType != null && mimeType.startsWith(MIME_TYPE_PREFIX_IMAGE);
    }

    /**
     * Check whether it is a network image
     *
     * @param path
     * @return
     */
    public static boolean isHttp(String path) {
        return !TextUtils.isEmpty(path) && (path.startsWith("http") || path.startsWith("https"));
    }

    /**
     * Determine whether the file type is image or video
     *
     * @param file
     * @return
     */
    public static String fileToType(File file) {
        if (file != null) {
            String name = file.getName();
            if (name.endsWith(".mp4")
                    || name.endsWith(".avi")
                    || name.endsWith(".3gpp")
                    || name.endsWith(".3gp")
                    || name.endsWith(".mov")) {
                return MIME_TYPE_VIDEO;
            } else if (name.endsWith(".PNG")
                    || name.endsWith(".png")
                    || name.endsWith(".jpeg")
                    || name.endsWith(".gif")
                    || name.endsWith(".GIF")
                    || name.endsWith(".jpg")
                    || name.endsWith(".webp")
                    || name.endsWith(".WEBP")
                    || name.endsWith(".JPEG")
                    || name.endsWith(".bmp")) {
                return MIME_TYPE_IMAGE;
            }
        }
        return MIME_TYPE_IMAGE;
    }

    /**
     * is type Equal
     *
     * @param p1
     * @param p2
     * @return
     */
    @Deprecated
    public static boolean mimeToEqual(String p1, String p2) {
        return isPictureType(p1) == isPictureType(p2);
    }

    /**
     * Check whether it is the same type
     *
     * @param oldMimeType
     * @param newMimeType
     * @return
     */
    public static boolean isMimeTypeSame(String oldMimeType, String newMimeType) {

        return getMimeType(oldMimeType) == getMimeType(newMimeType);
    }

    public static String getImageMimeType(String path) {
        try {
            if (!TextUtils.isEmpty(path)) {
                File file = new File(path);
                String fileName = file.getName();
                int last = fileName.lastIndexOf(".") + 1;
                String temp = fileName.substring(last);
                return "image/" + temp;
            }
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
            return MIME_TYPE_IMAGE;
        }
        return MIME_TYPE_IMAGE;
    }

    /**
     * Get MIME type from URI
     *
     * @param uri
     * @return
     */
    public static String getMimeType(Context context, Uri uri) {
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            Cursor cursor =
                    CursorUtils.query(
                            context.getApplicationContext(),
                            uri,
                            new String[] {MediaStore.Files.FileColumns.MIME_TYPE},
                            null,
                            null,
                            null);
            if (cursor == null) {
                return MIME_TYPE_IMAGE;
            }
            String mimeType = MIME_TYPE_IMAGE;
            try {
                if (cursor.moveToFirst()) {
                    int columnIndex =
                            cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);
                    if (columnIndex > -1) {
                        mimeType = cursor.getString(columnIndex);
                    }
                }
            } catch (Exception e) {
                RLog.e(TAG, "getMimeType cursor error  ", e);
            } finally {
                cursor.close();
            }
            return mimeType;
        }
        return MIME_TYPE_IMAGE;
    }

    /**
     * Picture or video
     *
     * @return
     */
    public static int getMimeType(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) {
            return PictureConfig.TYPE_IMAGE;
        }
        if (mimeType.startsWith(MIME_TYPE_PREFIX_VIDEO)) {
            return PictureConfig.TYPE_VIDEO;
        } else {
            return PictureConfig.TYPE_IMAGE;
        }
    }

    /**
     * Get image file extension
     *
     * @param path
     * @return
     */
    public static String getLastImgType(String path) {
        try {
            int index = path.lastIndexOf(".");
            if (index > 0) {
                String imageType = path.substring(index);
                switch (imageType) {
                    case ".png":
                    case ".PNG":
                    case ".jpg":
                    case ".jpeg":
                    case ".JPEG":
                    case ".WEBP":
                    case ".bmp":
                    case ".BMP":
                    case ".webp":
                    case ".gif":
                    case ".GIF":
                        return imageType;
                    default:
                        return PNG;
                }
            } else {
                return PNG;
            }
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
            return PNG;
        }
    }

    /**
     * Get image suffix from MIME type
     *
     * @param mineType
     * @return
     */
    public static String getLastImgSuffix(String mineType) {
        String defaultSuffix = PNG;
        try {
            int index = mineType.lastIndexOf("/") + 1;
            if (index > 0) {
                return "." + mineType.substring(index);
            }
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
            return defaultSuffix;
        }
        return defaultSuffix;
    }

    /**
     * Return different error messages based on type
     *
     * @param mimeType
     * @return
     */
    public static String s(Context context, String mimeType) {
        Context ctx = context.getApplicationContext();
        if (eqVideo(mimeType)) {
            return ctx.getString(R.string.nc_picture_video_error);
        } else {
            return ctx.getString(R.string.nc_picture_error);
        }
    }

    public static final String JPEG = ".jpg";

    public static final String PNG = ".png";

    public static final String DCIM = "DCIM/Camera";
    public static final String MIME_TYPE_IMAGE = "image/jpeg";
    public static final String MIME_TYPE_VIDEO = "video/mp4";

    public static final String MIME_TYPE_PREFIX_IMAGE = "image";
    public static final String MIME_TYPE_PREFIX_VIDEO = "video";
}
