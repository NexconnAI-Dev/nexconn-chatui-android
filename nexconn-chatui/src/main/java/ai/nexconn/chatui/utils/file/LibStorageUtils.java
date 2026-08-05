package ai.nexconn.chatui.utils.file;

import ai.nexconn.chatui.utils.log.RLog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import java.io.File;

public class LibStorageUtils {
    private static final String TAG = "LibStorageUtils";
    public static final String VIDEO = "video";
    public static final String IMAGE = "image";
    public static final String FILE = "file";
    public static final String AUDIO = "audio";
    public static final String MEDIA = "media";
    public static final String DB_STORAGE = "storage";
    public static final String DB_STORAGE_PRIVATE = "storage.db";

    private static Boolean isBuildAndTargetForQ = null;

    public static boolean isScopedStorageMode(Context context) {
        if (context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.R) {
            return true;
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.Q;
    }

    public static boolean isBuildAndTargetForQ(Context context) {
        if (isBuildAndTargetForQ == null) {
            isBuildAndTargetForQ =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                            && context.getApplicationInfo().targetSdkVersion
                                    >= Build.VERSION_CODES.Q;
        }
        return isBuildAndTargetForQ;
    }

    public static boolean isOsAndTargetForR(Context context) {
        return Build.VERSION.SDK_INT >= 30 && context.getApplicationInfo().targetSdkVersion >= 30;
    }

    @Deprecated
    public static String getMediaDownloadDir(Context context) {
        return getMediaDownloadDir(context, MEDIA);
    }

    public static String getMediaDownloadDir(Context context, String dir) {
        boolean sdCardExist =
                Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        String result = context.getCacheDir().getPath();
        if (!sdCardExist) {
            return result;
        }

        File externalFilesDir = context.getExternalFilesDir(null);
        File file = new File(externalFilesDir, dir);
        if (!file.exists() && !file.mkdirs()) {
            if (externalFilesDir != null) {
                result = externalFilesDir.getPath();
            }
        } else {
            result = file.getPath();
        }
        return result;
    }

    public static String getFilesDir(Context pContext, String dir) {
        String mDirPath = "";
        File extDirFile = null;
        try {
            extDirFile = pContext.getExternalFilesDir(dir);
        } catch (ArrayIndexOutOfBoundsException e) {
            RLog.e(TAG, "getDir dir:" + dir, e);
        }
        if (extDirFile != null) {
            mDirPath = extDirFile.getAbsolutePath();
            return mDirPath;
        }
        try {
            mDirPath = pContext.getFilesDir().getAbsoluteFile() + File.separator + dir;
        } catch (Exception e) {
            RLog.e(TAG, "getDir dir:" + dir, e);
        }
        return mDirPath;
    }

    public static String getAppName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            int labelRes = packageInfo.applicationInfo.labelRes;
            if (labelRes > 0) {
                return context.getResources().getString(labelRes);
            } else {
                CharSequence nonLocalizedLabel = packageInfo.applicationInfo.nonLocalizedLabel;
                if (TextUtils.isEmpty(nonLocalizedLabel)) {
                    return null;
                }
                return nonLocalizedLabel.toString();
            }
        } catch (PackageManager.NameNotFoundException e) {
            RLog.e(TAG, "getAppName", e);
        } catch (Exception e) {
            RLog.e(TAG, "getAppName", e);
        }
        return null;
    }
}
