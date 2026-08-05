package ai.nexconn.chatui.shortvideo.compressor;

import ai.nexconn.chatui.shortvideo.compressor.videoslimmer.VideoSlimEncoder;
import ai.nexconn.chatui.shortvideo.compressor.videoslimmer.listner.SlimProgressListener;
import ai.nexconn.chatui.utils.log.RLog;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.text.TextUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

@SuppressLint("NewApi")
public class VideoController {
    private static final String TAG = VideoController.class.getSimpleName();

    private VideoController() {
        // default implementation ignored
    }

    private static class SingletonHolder {
        static VideoController sInstance = new VideoController();
    }

    public static VideoController getInstance() {
        return SingletonHolder.sInstance;
    }

    public static void copyFile(File src, File dst) {
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            fileInputStream = new FileInputStream(src);
            fileOutputStream = new FileOutputStream(dst);
            inChannel = fileInputStream.getChannel();
            outChannel = fileOutputStream.getChannel();
            inChannel.transferTo(1, inChannel.size(), outChannel);
        } catch (Exception e) {
            RLog.e(TAG, "copyFile", e);
        } finally {
            try {
                if (inChannel != null) {
                    inChannel.close();
                }
            } catch (IOException e) {
                RLog.e(TAG, "copyFile inChannel close", e);
            }
            try {
                if (outChannel != null) {
                    outChannel.close();
                }
            } catch (IOException e) {
                RLog.e(TAG, "copyFile outChannel close", e);
            }

            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                RLog.e(TAG, "copyFile fileInputStream close", e);
            }

            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                RLog.e(TAG, "copyFile fileOutputStream close", e);
            }
        }
    }

    /**
     * Does not support content:// and file:// schemes, please convert before using
     *
     * @param sourcePath the source uri for the file as per
     * @param destinationPath the destination directory where compressed video is eventually saved
     * @return
     */
    @TargetApi(16)
    public boolean convertVideo(
            final String sourcePath, String destinationPath, SlimProgressListener listener) {
        if (!new File(sourcePath).exists()) {
            return false;
        }
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(sourcePath);
        } catch (Exception e) {
            RLog.e(TAG, e.toString());
            return false;
        }
        String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String rotation =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if (TextUtils.isEmpty(width) || TextUtils.isEmpty(height) || TextUtils.isEmpty(rotation)) {
            return false;
        }
        long startTime = -1;

        int rotationValue = Integer.valueOf(rotation);
        int originalWidth = Integer.valueOf(width);
        int originalHeight = Integer.valueOf(height);
        RLog.d(TAG, "Resolution of origin width is " + originalWidth);
        RLog.d(TAG, "Resolution of origin height is " + originalHeight);
        RLog.d(TAG, "Origin rotation value is " + rotationValue);
        // Video resolution must be multiples of 16 to prevent screen artifacts in YUV format
        // decoding
        if (originalWidth % 16 != 0) {
            int quotient = originalWidth / 16;
            originalWidth = 16 * quotient;
        }
        if (originalHeight % 16 != 0) {
            int quotient = originalHeight / 16;
            originalHeight = 16 * quotient;
        }

        int resultWidth;
        int resultHeight;
        // Add portrait/landscape detection: compress to 960x544 for portrait, 544x960 for landscape
        if (originalHeight >= originalWidth) {
            if (originalHeight <= 960 && originalWidth <= 544) {
                if (originalHeight == 960 && originalWidth == 544) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        resultWidth = originalWidth - 16;
                        resultHeight = originalHeight - 16;
                    } else {
                        resultWidth = originalWidth;
                        resultHeight = originalHeight;
                    }

                } else {
                    resultWidth = originalWidth;
                    resultHeight = originalHeight;
                }
            } else if (originalHeight <= 960 && originalWidth > 544) {
                resultWidth = 544;
                double ratio = (double) originalHeight / originalWidth;
                int quotient = (int) (ratio * resultWidth) / 16;
                resultHeight = 16 * quotient;
            } else {
                resultHeight = 960;
                double ratio = (double) originalWidth / originalHeight;
                int quotient = (int) (ratio * resultHeight) / 16;
                resultWidth = 16 * quotient;
            }
        } else {
            // Set maximum compressed video resolution to 544x960
            if (originalWidth <= 960 && originalHeight <= 544) {
                resultWidth = originalWidth;
                resultHeight = originalHeight;
            } else if (originalWidth <= 960 && originalHeight > 544) {
                resultHeight = 544;
                double ratio = (double) originalWidth / originalHeight;
                int quotient = (int) (ratio * resultHeight) / 16;
                resultWidth = 16 * quotient;
            } else {
                resultWidth = 960;
                double ratio = (double) originalHeight / originalWidth;
                int quotient = (int) (ratio * resultWidth) / 16;
                resultHeight = 16 * quotient;
            }
        }

        int bitrate = (resultWidth / 2) * (resultHeight / 2) * 10;
        int rotateRender = 0;

        if (Build.VERSION.SDK_INT < 18
                && resultHeight > resultWidth
                && resultWidth != originalWidth
                && resultHeight != originalHeight) {
            int temp = resultHeight;
            resultHeight = resultWidth;
            resultWidth = temp;
            rotationValue = 90;
            rotateRender = 270;
        } else if (Build.VERSION.SDK_INT > 20) {
            if (rotationValue == 90) {
                int temp = resultHeight;
                resultHeight = resultWidth;
                resultWidth = temp;
                rotationValue = 0;
                rotateRender = 270;
            } else if (rotationValue == 180) {
                rotateRender = 180;
                rotationValue = 0;
            } else if (rotationValue == 270) {
                int temp = resultHeight;
                resultHeight = resultWidth;
                resultWidth = temp;
                rotationValue = 0;
                rotateRender = 90;
            }
        }
        RLog.d(TAG, "Resolution of result width is " + resultWidth);
        RLog.d(TAG, "Resolution of result height is " + resultHeight);
        RLog.d(TAG, "Result rotation value is " + rotationValue);
        RLog.d(TAG, "Result render value is " + rotateRender);

        // Use the latest codec for API level 18 and above
        if (resultWidth != 0 && resultHeight != 0 && Build.VERSION.SDK_INT >= 18) {
            try {
                boolean result;
                // First compression attempt; if failed, retry with width+16 and height+16 to
                // prevent artifacts; if second attempt also fails, return failure
                result =
                        new VideoSlimEncoder()
                                .convertVideo(
                                        sourcePath,
                                        destinationPath,
                                        resultWidth,
                                        resultHeight,
                                        bitrate,
                                        (ai.nexconn.chatui.shortvideo.compressor.videoslimmer
                                                        .listner.SlimProgressListener)
                                                listener);
                if (!result) {
                    File file = new File(destinationPath);
                    if (file != null && file.exists()) {
                        boolean delete = file.delete();
                        RLog.d(TAG, "delete:" + delete);
                    }
                    resultWidth += 16;
                    resultHeight += 16;
                    result =
                            new VideoSlimEncoder()
                                    .convertVideo(
                                            sourcePath,
                                            destinationPath,
                                            resultWidth,
                                            resultHeight,
                                            (resultWidth / 2) * (resultHeight / 2) * 10,
                                            (ai.nexconn.chatui.shortvideo.compressor.videoslimmer
                                                            .listner.SlimProgressListener)
                                                    listener);
                }
                return result;
            } catch (Exception e) {
                RLog.e(TAG, "compress fail", e);
                return false;
            }
        }
        return true;
    }
}
