package ai.nexconn.chatui.picture.model;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.picture.config.PictureConfig;
import ai.nexconn.chatui.picture.config.PictureMimeType;
import ai.nexconn.chatui.picture.config.PictureSelectionConfig;
import ai.nexconn.chatui.picture.entity.LocalMedia;
import ai.nexconn.chatui.picture.entity.LocalMediaFolder;
import ai.nexconn.chatui.picture.tools.SdkVersionUtils;
import ai.nexconn.chatui.utils.file.CursorUtils;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.system.ExecutorHelper;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class LocalMediaLoader implements Handler.Callback {
    private static final int MSG_QUERY_MEDIA_SUCCESS = 0;
    private static final int MSG_QUERY_MEDIA_ERROR = -1;
    private static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");
    private static final String ORDER_BY = MediaStore.Files.FileColumns._ID + " DESC";
    private static final String NOT_GIF = "!='image/gif'";

    /** Filter out recordings shorter than 500 milliseconds */
    private static final int AUDIO_DURATION = 500;

    private static final String TAG = LocalMediaLoader.class.getSimpleName();

    private Context mContext;
    private boolean isAndroidQ;
    private PictureSelectionConfig config;
    private Handler mHandler;

    /** unit */
    private static final long FILE_SIZE_UNIT = 1024 * 1024L;

    /** Media file database columns */
    private static final String[] PROJECTION = {
        MediaStore.Files.FileColumns._ID,
        MediaStore.MediaColumns.DATA,
        MediaStore.MediaColumns.MIME_TYPE,
        MediaStore.MediaColumns.WIDTH,
        MediaStore.MediaColumns.HEIGHT,
        MediaStore.MediaColumns.DURATION,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
    };

    /** Image selection query */
    private static final String SELECTION =
            MediaStore.Files.FileColumns.MEDIA_TYPE
                    + "=?"
                    + " AND "
                    + MediaStore.MediaColumns.SIZE
                    + ">0";

    private static final String SELECTION_NOT_GIF =
            MediaStore.Files.FileColumns.MEDIA_TYPE
                    + "=?"
                    + " AND "
                    + MediaStore.MediaColumns.SIZE
                    + ">0"
                    + " AND "
                    + MediaStore.MediaColumns.MIME_TYPE
                    + NOT_GIF;

    /** Query images with specified format suffix */
    private static final String SELECTION_SPECIFIED_FORMAT =
            MediaStore.Files.FileColumns.MEDIA_TYPE
                    + "=?"
                    + " AND "
                    + MediaStore.MediaColumns.SIZE
                    + ">0"
                    + " AND "
                    + MediaStore.MediaColumns.MIME_TYPE;

    /**
     * Query condition (audio/video)
     *
     * @param time_condition
     * @return
     */
    private static String getSelectionArgsForSingleMediaCondition(String time_condition) {
        return MediaStore.Files.FileColumns.MEDIA_TYPE
                + "=?"
                + " AND "
                + MediaStore.MediaColumns.SIZE
                + ">0"
                + " AND "
                + time_condition;
    }

    /**
     * Query (video)
     *
     * @return
     */
    private static String getSelectionArgsForSingleMediaCondition() {
        return MediaStore.Files.FileColumns.MEDIA_TYPE
                + "=?"
                + " AND "
                + MediaStore.MediaColumns.SIZE
                + ">0";
    }

    /**
     * Condition for all media types
     *
     * @param time_condition
     * @param isGif
     * @return
     */
    private static String getSelectionArgsForAllMediaCondition(
            String time_condition, boolean isGif) {
        String condition =
                "("
                        + MediaStore.Files.FileColumns.MEDIA_TYPE
                        + "=?"
                        + (isGif ? "" : " AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF)
                        + " OR "
                        + (MediaStore.Files.FileColumns.MEDIA_TYPE + "=? AND " + time_condition)
                        + ")"
                        + " AND "
                        + MediaStore.MediaColumns.SIZE
                        + ">0";
        return condition;
    }

    /** Get image or video selection args */
    private static final String[] SELECTION_ALL_ARGS = {
        String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
        String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
    };

    /**
     * Get files of specified media type
     *
     * @param mediaType
     * @return
     */
    private static String[] getSelectionArgsForSingleMediaType(int mediaType) {
        return new String[] {String.valueOf(mediaType)};
    }

    public LocalMediaLoader(Context context, PictureSelectionConfig config) {
        this.mContext = context.getApplicationContext();
        this.isAndroidQ = SdkVersionUtils.checkedAndroid_Q();
        this.config = config;
        this.mHandler = new Handler(Looper.getMainLooper(), this);
    }

    public void loadAllMedia() {
        ExecutorHelper.getInstance()
                .diskIO()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                Cursor data =
                                        CursorUtils.query(
                                                mContext,
                                                QUERY_URI,
                                                PROJECTION,
                                                getSelection(),
                                                getSelectionArgs(),
                                                ORDER_BY);

                                try {
                                    if (data != null) {
                                        String title =
                                                mContext.getString(R.string.nc_picture_camera_roll);
                                        List<LocalMediaFolder> imageFolders = new ArrayList<>();
                                        LocalMediaFolder allImageFolder = new LocalMediaFolder();
                                        List<LocalMedia> latelyImages = new ArrayList<>();
                                        int count = data.getCount();
                                        if (count > 0) {
                                            data.moveToFirst();
                                            do {
                                                long id =
                                                        data.getLong(
                                                                data.getColumnIndexOrThrow(
                                                                        PROJECTION[0]));

                                                String path =
                                                        isAndroidQ
                                                                ? getRealPathAndroid_Q(id)
                                                                : data.getString(
                                                                        data.getColumnIndexOrThrow(
                                                                                PROJECTION[1]));

                                                String mimeType =
                                                        data.getString(
                                                                data.getColumnIndexOrThrow(
                                                                        PROJECTION[2]));

                                                int width =
                                                        data.getInt(
                                                                data.getColumnIndexOrThrow(
                                                                        PROJECTION[3]));

                                                int height =
                                                        data.getInt(
                                                                data.getColumnIndexOrThrow(
                                                                        PROJECTION[4]));

                                                long duration =
                                                        data.getLong(
                                                                data.getColumnIndexOrThrow(
                                                                        PROJECTION[5]));

                                                long size =
                                                        data.getLong(
                                                                data.getColumnIndexOrThrow(
                                                                        PROJECTION[6]));

                                                String folderName =
                                                        data.getString(
                                                                data.getColumnIndexOrThrow(
                                                                        PROJECTION[7]));
                                                if (folderName == null) {
                                                    folderName = title;
                                                }

                                                if (PictureMimeType.eqVideo(mimeType)) {
                                                    if (duration == 0) {
                                                        // Filter out videos with 0 duration as they
                                                        // are considered corrupted
                                                        continue;
                                                    }
                                                    if (size <= 0) {
                                                        // Filter out videos with 0 size
                                                        continue;
                                                    }
                                                }

                                                LocalMedia image =
                                                        new LocalMedia(
                                                                path,
                                                                duration,
                                                                config.chooseMode,
                                                                mimeType,
                                                                width,
                                                                height,
                                                                size);
                                                LocalMediaFolder folder =
                                                        getImageFolder(
                                                                path, folderName, imageFolders);
                                                List<LocalMedia> images = folder.getImages();
                                                images.add(image);
                                                folder.setImageNum(folder.getImageNum() + 1);
                                                latelyImages.add(image);
                                                int imageNum = allImageFolder.getImageNum();
                                                allImageFolder.setImageNum(imageNum + 1);

                                            } while (data.moveToNext());

                                            if (latelyImages.size() > 0) {
                                                sortFolder(imageFolders);
                                                imageFolders.add(0, allImageFolder);
                                                allImageFolder.setFirstImagePath(
                                                        latelyImages.get(0).getPath());
                                                allImageFolder.setName(title);
                                                allImageFolder.setOfAllType(config.chooseMode);
                                                allImageFolder.setCameraFolder(true);
                                                allImageFolder.setImages(latelyImages);
                                            }
                                        }
                                        // Switch to main thread
                                        mHandler.sendMessage(
                                                mHandler.obtainMessage(
                                                        MSG_QUERY_MEDIA_SUCCESS, imageFolders));
                                    } else {
                                        mHandler.sendMessage(
                                                mHandler.obtainMessage(MSG_QUERY_MEDIA_ERROR));
                                    }
                                } catch (Exception e) {
                                    if (mHandler != null) {
                                        mHandler.sendMessage(
                                                mHandler.obtainMessage(MSG_QUERY_MEDIA_ERROR));
                                    }
                                    RLog.e(TAG, e.getMessage());
                                } finally {
                                    // Cursor must be closed
                                    if (data != null) {
                                        data.close();
                                    }
                                }
                            }
                        });
    }

    private String getSelection() {
        switch (config.chooseMode) {
            case PictureConfig.TYPE_ALL:
                // Get all media types except audio
                return getSelectionArgsForAllMediaCondition(
                        getDurationCondition(0, 0), config.isGif);
            case PictureConfig.TYPE_IMAGE:
                if (!TextUtils.isEmpty(config.specifiedFormat)) {
                    // Get images of specified type
                    return SELECTION_SPECIFIED_FORMAT + "='" + config.specifiedFormat + "'";
                }
                return config.isGif ? SELECTION : SELECTION_NOT_GIF;
            case PictureConfig.TYPE_VIDEO:
                // Get videos
                if (!TextUtils.isEmpty(config.specifiedFormat)) {
                    // Get images of specified type
                    return SELECTION_SPECIFIED_FORMAT + "='" + config.specifiedFormat + "'";
                }
                return getSelectionArgsForSingleMediaCondition();
        }
        return null;
    }

    private String[] getSelectionArgs() {
        switch (config.chooseMode) {
            case PictureConfig.TYPE_ALL:
                return SELECTION_ALL_ARGS;
            case PictureConfig.TYPE_IMAGE:
                // Get images only
                String[] MEDIA_TYPE_IMAGE =
                        getSelectionArgsForSingleMediaType(
                                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE);
                return MEDIA_TYPE_IMAGE;
            case PictureConfig.TYPE_VIDEO:
                // Get videos only
                return getSelectionArgsForSingleMediaType(
                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
        }
        return null;
    }

    /**
     * Sort folders by image count
     *
     * @param imageFolders
     */
    private void sortFolder(List<LocalMediaFolder> imageFolders) {
        // Sort folders by image count
        Collections.sort(
                imageFolders,
                new Comparator<LocalMediaFolder>() {
                    @Override
                    public int compare(LocalMediaFolder lhs, LocalMediaFolder rhs) {
                        if (lhs.getImages() == null || rhs.getImages() == null) {
                            return 0;
                        }
                        int lsize = lhs.getImageNum();
                        int rsize = rhs.getImageNum();
                        return lsize == rsize ? 0 : (lsize < rsize ? 1 : -1);
                    }
                });
    }

    /**
     * Adapt for Android Q
     *
     * @param id
     * @return
     */
    private String getRealPathAndroid_Q(long id) {
        return QUERY_URI.buildUpon().appendPath(String.valueOf(id)).build().toString();
    }

    /**
     * Create corresponding folder
     *
     * @param path
     * @param imageFolders
     * @param folderName
     * @return
     */
    private LocalMediaFolder getImageFolder(
            String path, String folderName, List<LocalMediaFolder> imageFolders) {
        for (LocalMediaFolder folder : imageFolders) {
            // If in the same folder, return it; otherwise create a new folder
            String name = folder.getName();
            if (TextUtils.isEmpty(name)) {
                continue;
            }
            if (name.equals(folderName)) {
                return folder;
            }
        }
        LocalMediaFolder newFolder = new LocalMediaFolder();
        newFolder.setName(folderName);
        newFolder.setFirstImagePath(path);
        imageFolders.add(newFolder);
        return newFolder;
    }

    /**
     * Get video duration condition (max or min limit)
     *
     * @param exMaxLimit
     * @param exMinLimit
     * @return
     */
    private String getDurationCondition(long exMaxLimit, long exMinLimit) {
        long maxS = Long.MAX_VALUE;
        if (exMaxLimit != 0) {
            maxS = Math.min(maxS, exMaxLimit);
        }
        return String.format(
                Locale.CHINA,
                "%d <%s "
                        + MediaStore.MediaColumns.DURATION
                        + " and "
                        + MediaStore.MediaColumns.DURATION
                        + " <= %d",
                exMinLimit,
                exMinLimit == 0 ? "" : "=",
                maxS);
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (mCompleteListener == null) return false;
        switch (msg.what) {
            case MSG_QUERY_MEDIA_SUCCESS:
                mCompleteListener.loadComplete((List<LocalMediaFolder>) msg.obj);
                break;
            case MSG_QUERY_MEDIA_ERROR:
                mCompleteListener.loadMediaDataError();
                break;
        }
        return false;
    }

    private LocalMediaLoadListener mCompleteListener;

    public void setCompleteListener(LocalMediaLoadListener mCompleteListener) {
        this.mCompleteListener = mCompleteListener;
    }

    public interface LocalMediaLoadListener {

        void loadComplete(List<LocalMediaFolder> folders);

        void loadMediaDataError();
    }
}
