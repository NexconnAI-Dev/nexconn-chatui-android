package ai.nexconn.chatui.shortvideo.compressor;

import ai.nexconn.chatui.shortvideo.compressor.videoslimmer.listner.SlimProgressListener;
import ai.nexconn.chatui.utils.file.ChatUIStorageUtils;
import ai.nexconn.chatui.utils.file.FileUtils;
import ai.nexconn.chatui.utils.system.ExecutorHelper;
import android.content.Context;
import android.net.Uri;
import java.io.File;

/** Created by Vincent Woo Date: 2017/8/16 Time: 15:15 */
public class VideoCompress {
    private static final String TAG = VideoCompress.class.getSimpleName();
    private static final String CACHE = "/cache_";

    public static void compressVideo(
            final Context context,
            final String srcPath,
            final String destPath,
            final CompressListener listener) {
        ExecutorHelper.getInstance()
                .compressExecutor()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                ExecutorHelper.getInstance()
                                        .mainThread()
                                        .execute(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        listener.onStart();
                                                    }
                                                });
                                String path =
                                        ChatUIStorageUtils.getVideoSavePath(context)
                                                + CACHE
                                                + System.currentTimeMillis()
                                                + ".mp4";
                                Uri srcUri = Uri.parse(srcPath);
                                boolean isContent = false;
                                // Copy a cache for content type first
                                if (FileUtils.uriStartWithContent(srcUri)) {
                                    boolean result = FileUtils.copyFile(context, srcUri, path);
                                    // Return failure directly if copy fails
                                    if (result) {
                                        isContent = true;

                                    } else {
                                        ExecutorHelper.getInstance()
                                                .mainThread()
                                                .execute(
                                                        new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                listener.onFail();
                                                            }
                                                        });
                                        return;
                                    }
                                } else if (FileUtils.uriStartWithFile(srcUri)) {
                                    path = srcPath.substring(7);
                                } else {
                                    path = srcPath;
                                }
                                boolean result =
                                        VideoController.getInstance()
                                                .convertVideo(
                                                        path,
                                                        destPath,
                                                        new SlimProgressListener() {
                                                            @Override
                                                            public void onProgress(
                                                                    final float percent) {
                                                                ExecutorHelper.getInstance()
                                                                        .mainThread()
                                                                        .execute(
                                                                                new Runnable() {
                                                                                    @Override
                                                                                    public void
                                                                                            run() {
                                                                                        listener
                                                                                                .onProgress(
                                                                                                        percent);
                                                                                    }
                                                                                });
                                                            }
                                                        });
                                if (result) {
                                    ExecutorHelper.getInstance()
                                            .mainThread()
                                            .execute(
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            listener.onSuccess();
                                                        }
                                                    });

                                } else {
                                    ExecutorHelper.getInstance()
                                            .mainThread()
                                            .execute(
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            listener.onFail();
                                                        }
                                                    });
                                }
                                // Delete temporary file
                                if (isContent) {
                                    new File(path).delete();
                                }
                            }
                        });
    }

    public interface CompressListener {
        void onStart();

        void onSuccess();

        void onFail();

        void onProgress(float percent);
    }
}
