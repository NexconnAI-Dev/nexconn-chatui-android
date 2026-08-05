package ai.nexconn.chatui.picture.tools;

import ai.nexconn.chatui.picture.config.PictureMimeType;
import ai.nexconn.chatui.picture.entity.LocalMedia;
import ai.nexconn.chatui.utils.file.CursorUtils;
import ai.nexconn.chatui.utils.log.RLog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

public class MediaUtils {
    private static final String TAG = MediaUtils.class.getSimpleName();

    /**
     * Create an image URI for saving captured photos
     *
     * @param context
     * @return the image URI
     */
    public static Uri createImageUri(final Context context) {
        final Uri[] imageFilePath = {null};
        String status = Environment.getExternalStorageState();
        String time = ValueOf.toString(System.currentTimeMillis());
        // ContentValues contains the data we want the record to include when created
        ContentValues values = new ContentValues(3);
        values.put(
                MediaStore.Images.Media.DISPLAY_NAME,
                DateUtils.getInstance().getCreateFileName("IMG_"));
        values.put(MediaStore.Images.Media.DATE_TAKEN, time);
        values.put(MediaStore.Images.Media.MIME_TYPE, PictureMimeType.MIME_TYPE_IMAGE);
        // Check for SD card availability; prefer SD card storage, fall back to internal storage
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, PictureMimeType.DCIM);
            imageFilePath[0] =
                    context.getContentResolver()
                            .insert(MediaStore.Images.Media.getContentUri("external"), values);
        } else {
            imageFilePath[0] =
                    context.getContentResolver()
                            .insert(MediaStore.Images.Media.getContentUri("internal"), values);
        }
        return imageFilePath[0];
    }

    /**
     * Create a video URI for saving recorded videos
     *
     * @param context
     * @return the video URI
     */
    public static Uri createVideoUri(final Context context) {
        final Uri[] imageFilePath = {null};
        String status = Environment.getExternalStorageState();
        String time = ValueOf.toString(System.currentTimeMillis());
        // ContentValues contains the data we want the record to include when created
        ContentValues values = new ContentValues(3);
        values.put(
                MediaStore.Video.Media.DISPLAY_NAME,
                DateUtils.getInstance().getCreateFileName("VID_"));
        values.put(MediaStore.Video.Media.DATE_TAKEN, time);
        values.put(MediaStore.Video.Media.MIME_TYPE, PictureMimeType.MIME_TYPE_VIDEO);
        // Check for SD card availability; prefer SD card storage, fall back to internal storage
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, PictureMimeType.DCIM);
            imageFilePath[0] =
                    context.getContentResolver()
                            .insert(MediaStore.Video.Media.getContentUri("external"), values);
        } else {
            imageFilePath[0] =
                    context.getContentResolver()
                            .insert(MediaStore.Video.Media.getContentUri("internal"), values);
        }
        return imageFilePath[0];
    }

    /**
     * Get video duration
     *
     * @param context
     * @param isAndroidQ
     * @param path
     * @return
     */
    public static long extractDuration(Context context, boolean isAndroidQ, String path) {
        return isAndroidQ ? getLocalDuration(context, Uri.parse(path)) : getLocalDuration(path);
    }

    /**
     * Check whether it is a long image
     *
     * @param media
     * @return true if long image, false otherwise
     */
    public static boolean isLongImg(LocalMedia media) {
        if (null != media) {
            int width = media.getWidth();
            int height = media.getHeight();
            int h = width * 3;
            return height > h;
        }
        return false;
    }

    /**
     * get Local video duration
     *
     * @return
     */
    private static long getLocalDuration(Context context, Uri uri) {
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(context, uri);
            return Long.parseLong(
                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
            return 0;
        }
    }

    /**
     * get Local video duration
     *
     * @return
     */
    private static long getLocalDuration(String path) {
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(path);
            return Long.parseLong(
                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
            return 0;
        }
    }

    /**
     * get Local video width or height for api 29
     *
     * @return
     */
    @Deprecated
    public static int[] getLocalSizeToAndroidQ(Context context, String videoPath) {
        int[] size = new int[2];
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return size;
        }
        Cursor query = null;
        try {
            query =
                    CursorUtils.query(
                            context.getApplicationContext(),
                            Uri.parse(videoPath),
                            null,
                            null,
                            null);
            if (query != null) {
                query.moveToFirst();
                size[0] =
                        query.getInt(
                                query.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH));
                size[1] =
                        query.getInt(
                                query.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT));
            }
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
        } finally {
            if (query != null) {
                query.close();
            }
        }
        return size;
    }

    /**
     * get Local image width or height for api 29
     *
     * @return
     */
    public static int[] getLocalImageSizeToAndroidQ(Context context, String videoPath) {
        int[] size = new int[2];
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return size;
        }
        Cursor query = null;
        try {
            query =
                    CursorUtils.query(
                            context.getApplicationContext(),
                            Uri.parse(videoPath),
                            null,
                            null,
                            null);
            if (query != null) {
                query.moveToFirst();
                size[0] = query.getInt(query.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH));
                size[1] = query.getInt(query.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT));
            }
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
        } finally {
            if (query != null) {
                query.close();
            }
        }
        return size;
    }

    /**
     * get Local video width or height
     *
     * @return
     */
    public static int[] getLocalVideoSize(String videoPath) {
        int[] size = new int[2];
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(videoPath);
            size[0] =
                    ValueOf.toInt(
                            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            size[1] =
                    ValueOf.toInt(
                            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
        }
        return size;
    }

    /**
     * get Local video width or height
     *
     * @return
     */
    public static int[] getLocalVideoSize(Context context, Uri uri) {
        int[] size = new int[2];
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(context, uri);
            size[0] =
                    ValueOf.toInt(
                            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            size[1] =
                    ValueOf.toInt(
                            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
        }
        return size;
    }

    /**
     * get Local image width or height
     *
     * @return
     */
    public static int[] getLocalImageWidthOrHeight(String imagePath) {
        int[] size = new int[2];
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);
            size[0] = options.outWidth;
            size[1] = options.outHeight;
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
        }
        return size;
    }

    /**
     * Get the latest photo record in the DCIM folder
     *
     * @return
     */
    @Deprecated
    public static int getLastImageId(Context context, String mimeType) {
        Cursor imageCursor = null;
        int imageId = -1;
        try {
            // selection: specify query conditions
            boolean isMimeType = PictureMimeType.eqImage(mimeType);
            String absolutePath = PictureFileUtils.getDCIMCameraPath(context, mimeType);
            String ORDER_BY = MediaStore.Files.FileColumns._ID + " DESC";
            String selection =
                    isMimeType
                            ? MediaStore.Video.Media.DATA + " like ?"
                            : MediaStore.Images.Media.DATA + " like ?";
            // Define selectionArgs:
            String[] selectionArgs = {absolutePath + "%"};
            imageCursor =
                    CursorUtils.query(
                            context,
                            isMimeType
                                    ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                    : MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            null,
                            selection,
                            selectionArgs,
                            ORDER_BY);
            if (imageCursor != null && imageCursor.moveToFirst()) {
                int id =
                        imageCursor.getInt(
                                isMimeType
                                        ? imageCursor.getColumnIndex(MediaStore.Video.Media._ID)
                                        : imageCursor.getColumnIndex(MediaStore.Images.Media._ID));
                long date =
                        imageCursor.getLong(
                                isMimeType
                                        ? imageCursor.getColumnIndex(
                                                MediaStore.Video.Media.DURATION)
                                        : imageCursor.getColumnIndex(
                                                MediaStore.Images.Media.DATE_ADDED));
                int duration = DateUtils.getInstance().dateDiffer(date);
                // Photos within the last 30 seconds in DCIM can be considered duplicates
                imageId = duration <= 30 ? id : -1;
            }
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
        } finally {
            if (imageCursor != null) {
                imageCursor.close();
            }
        }
        return imageId;
    }
}
