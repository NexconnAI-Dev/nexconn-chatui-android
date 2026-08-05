package ai.nexconn.chatui.utils.common;

import ai.nexconn.chatui.utils.constant.AndroidConstant;
import ai.nexconn.chatui.utils.file.CursorUtils;
import ai.nexconn.chatui.utils.file.LibStorageUtils;
import ai.nexconn.chatui.utils.log.RLog;
import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import androidx.core.content.ContextCompat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChatUIUtils {
    private static final String TAG = ChatUIUtils.class.getSimpleName();

    private static double RATIO = 0.85;

    public static int screenWidth;
    public static int screenHeight;
    public static int screenMin; // The smaller of width and height
    public static int screenMax; // The larger of width and height

    public static float density;
    public static float scaleDensity;
    public static float xdpi;
    public static float ydpi;
    public static int densityDpi;

    public static int dialogWidth;
    public static int statusbarheight;
    public static int navbarheight;

    private static String NC_IM_KIT = "NC_IM_KIT";
    private static String KEY_KEYBOARD_HEIGHT = "KEY_BROADCAST_HEIGHT";
    private static int TEMP_KEYBOARD_HEIGHT = -1;
    private static int TEMP_KEYBOARD_ORIENTATION = -1;

    public static int dip2px(float dipValue) {
        return (int) (dipValue * density + 0.5f);
    }

    public static int px2dip(float pxValue) {
        return (int) (pxValue / density + 0.5f);
    }

    public static int getDialogWidth() {
        dialogWidth = (int) (screenMin * RATIO);
        return dialogWidth;
    }

    public static void init(Context context) {
        if (null == context) {
            return;
        }
        DisplayMetrics dm = context.getApplicationContext().getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        screenMin = (screenWidth > screenHeight) ? screenHeight : screenWidth;
        density = dm.density;
        scaleDensity = dm.scaledDensity;
        xdpi = dm.xdpi;
        ydpi = dm.ydpi;
        densityDpi = dm.densityDpi;
    }

    public static void GetInfo(Context context) {
        if (null == context) {
            return;
        }
        DisplayMetrics dm = context.getApplicationContext().getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        screenMin = (screenWidth > screenHeight) ? screenHeight : screenWidth;
        screenMax = (screenWidth < screenHeight) ? screenHeight : screenWidth;
        density = dm.density;
        scaleDensity = dm.scaledDensity;
        xdpi = dm.xdpi;
        ydpi = dm.ydpi;
        densityDpi = dm.densityDpi;
        statusbarheight = getStatusBarHeight(context);
        navbarheight = getNavBarHeight(context);
        RLog.d(
                TAG,
                "screenWidth="
                        + screenWidth
                        + " screenHeight="
                        + screenHeight
                        + " density="
                        + density);
    }

    public static int getStatusBarHeight(Context context) {
        Class<?> c;
        Object obj;
        Field field;
        int x, sbar = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            sbar = context.getResources().getDimensionPixelSize(x);
        } catch (Exception E) {
            RLog.e(TAG, "getStatusBarHeight", E);
        }
        return sbar;
    }

    public static int getScreenWidth() {
        return screenWidth;
    }

    public static int getScreenHeight() {
        return screenHeight;
    }

    public static int getNavBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public static Drawable getDrawable(Context context, String resource) {
        InputStream is = null;
        try {
            Resources resources = context.getResources();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDensity = DisplayMetrics.DENSITY_HIGH;
            options.inScreenDensity = resources.getDisplayMetrics().densityDpi;
            options.inTargetDensity = resources.getDisplayMetrics().densityDpi;
            is = context.getAssets().open(resource);
            Bitmap bitmap = BitmapFactory.decodeStream(is, new Rect(), options);
            return new BitmapDrawable(context.getResources(), bitmap);
        } catch (Exception e) {
            RLog.e(TAG, "getDrawable", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    RLog.e(TAG, "getDrawable", e);
                }
            }
        }
        return null;
    }

    public static Uri getUriFromDrawableRes(Context context, int id) {
        Resources resources = context.getResources();
        String path =
                ContentResolver.SCHEME_ANDROID_RESOURCE
                        + "://"
                        + resources.getResourcePackageName(id)
                        + "/"
                        + resources.getResourceTypeName(id)
                        + "/"
                        + resources.getResourceEntryName(id);
        return Uri.parse(path);
    }

    public static Bitmap getResizedBitmap(Context context, Uri uri, int widthLimit, int heightLimit)
            throws IOException {

        String path = null;
        Bitmap result;

        if (uri.getScheme().equals("file")) {
            path = uri.getPath();
        } else if (uri.getScheme().equals("content")) {
            Cursor cursor =
                    CursorUtils.query(
                            context,
                            uri,
                            new String[] {MediaStore.Images.Media.DATA},
                            null,
                            null,
                            null);
            try {
                if (cursor != null) {
                    cursor.moveToFirst();
                    path = cursor.getString(0);
                }
            } catch (Exception e) {
                RLog.e(TAG, "getResizedBitmap cursor error  ", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else {
            return null;
        }
        if (TextUtils.isEmpty(path)) {
            return null;
        }

        ExifInterface exifInterface = new ExifInterface(path);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);

        if (orientation == ExifInterface.ORIENTATION_ROTATE_90
                || orientation == ExifInterface.ORIENTATION_ROTATE_270
                || orientation == ExifInterface.ORIENTATION_TRANSPOSE
                || orientation == ExifInterface.ORIENTATION_TRANSVERSE) {
            int tmp = widthLimit;
            widthLimit = heightLimit;
            heightLimit = tmp;
        }

        int width = options.outWidth;
        int height = options.outHeight;
        int sampleW = 1, sampleH = 1;
        while (width / 2 > widthLimit) {
            width /= 2;
            sampleW <<= 1;
        }

        while (height / 2 > heightLimit) {
            height /= 2;
            sampleH <<= 1;
        }
        int sampleSize = 1;

        options = new BitmapFactory.Options();
        sampleSize = Math.max(sampleW, sampleH);
        options.inSampleSize = sampleSize;

        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeFile(path, options);
        } catch (OutOfMemoryError e) {
            RLog.e(TAG, "getResizedBitmap", e);
            options.inSampleSize = options.inSampleSize << 1;
            bitmap = BitmapFactory.decodeFile(path, options);
        }

        Matrix matrix = new Matrix();
        if (bitmap == null) {
            return bitmap;
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        if (orientation == ExifInterface.ORIENTATION_ROTATE_90
                || orientation == ExifInterface.ORIENTATION_ROTATE_270
                || orientation == ExifInterface.ORIENTATION_TRANSPOSE
                || orientation == ExifInterface.ORIENTATION_TRANSVERSE) {
            int tmp = w;
            w = h;
            h = tmp;
        }
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90, w / 2f, h / 2f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180, w / 2f, h / 2f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(270, w / 2f, h / 2f);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.preScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.preScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90, w / 2f, h / 2f);
                matrix.preScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(270, w / 2f, h / 2f);
                matrix.preScale(1, -1);
                break;
            default:
                break;
        }
        float xS = (float) widthLimit / bitmap.getWidth();
        float yS = (float) heightLimit / bitmap.getHeight();

        matrix.postScale(Math.min(xS, yS), Math.min(xS, yS));
        try {
            result =
                    Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
            RLog.e(TAG, "getResizedBitmap", e);
            RLog.e(
                    "ResourceCompressHandler",
                    "OOM"
                            + "Height:"
                            + bitmap.getHeight()
                            + "Width:"
                            + bitmap.getHeight()
                            + "matrix:"
                            + xS
                            + " "
                            + yS);
            return null;
        }
        return result;
    }

    /** Gets the duration of a video file in milliseconds */
    public static int getVideoDuration(Context context, String videoPath) {
        try {
            MediaPlayer mp = MediaPlayer.create(context, Uri.parse(videoPath));
            int duration = mp.getDuration();
            mp.release();
            return duration;
        } catch (Exception e) {
            RLog.e(TAG, e.toString());
            return 0;
        }
    }

    /** MD5 hash */
    public static String md5(Object object) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(toByteArray(object));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Huh, MD5 should be supported?", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }

    private static byte[] toByteArray(Object obj) {
        byte[] bytes = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            bytes = bos.toByteArray();
            oos.close();
            bos.close();
        } catch (IOException ex) {
            RLog.e(TAG, "toByteArray", ex);
        }
        return bytes;
    }

    /** Gets the application name */
    @Deprecated
    public static String getAppName(Context context) {
        return LibStorageUtils.getAppName(context);
    }

    public static boolean isDestroy(Activity activity) {
        if (activity == null
                || activity.isFinishing()
                || Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                        && activity.isDestroyed()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks whether location services are enabled. The check method varies across different
     * Android versions.
     */
    public static boolean isLocationServiceEnabled(Context context) {
        boolean isLocationServiceEnabled = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager locationManager =
                    (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                isLocationServiceEnabled = locationManager.isLocationEnabled();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                isLocationServiceEnabled =
                        Settings.Secure.getInt(
                                        context.getContentResolver(), Settings.Secure.LOCATION_MODE)
                                != Settings.Secure.LOCATION_MODE_OFF;
            } catch (Settings.SettingNotFoundException e) {
            }
        } else {
            isLocationServiceEnabled =
                    !TextUtils.isEmpty(
                            Settings.Secure.getString(
                                    context.getContentResolver(),
                                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED));
        }

        return isLocationServiceEnabled;
    }

    /** Checks whether a phone call is currently in progress */
    public static boolean phoneIsInUse(Context context) {
        if (context == null) {
            return false;
        }
        try {
            TelephonyManager mTelephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            int state = mTelephonyManager.getCallState();
            return state != TelephonyManager.CALL_STATE_IDLE || !checkMicAvailable(context);
        } catch (SecurityException e) {
            // On Vivo devices, missing READ_PHONE_STATE permission may cause a crash
            RLog.e(TAG, "phoneIsInUse,nedd android.permission.READ_PHONE_STATE");
        } catch (Exception e) {
            RLog.e(TAG, "phoneIsInUse", e);
        }
        return false;
    }

    public static boolean checkMicAvailable(Context context) {
        if (context == null) {
            return false;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        boolean available = true;
        AudioRecord recorder =
                new AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        44100,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_DEFAULT,
                        44100);
        try {
            if (recorder.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED) {
                available = false;
            }
            recorder.startRecording();
            if (recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop();
                available = false;
            }
            recorder.stop();
        } finally {
            recorder.release();
            recorder = null;
        }
        return available;
    }

    public static int getSaveKeyBoardHeight(Context context, int orientation) {
        if (TEMP_KEYBOARD_HEIGHT == -1 || orientation != TEMP_KEYBOARD_ORIENTATION) {
            SharedPreferences sharedPreferences =
                    context.getSharedPreferences(NC_IM_KIT, Context.MODE_PRIVATE);
            int height = sharedPreferences.getInt(getKeyboardHeightKey(context, orientation), 0);
            TEMP_KEYBOARD_HEIGHT = height;
            TEMP_KEYBOARD_ORIENTATION = orientation;
            return height;
        } else {
            return TEMP_KEYBOARD_HEIGHT;
        }
    }

    public static void saveKeyboardHeight(Context context, int orientation, int height) {
        if (TEMP_KEYBOARD_HEIGHT != height || orientation != TEMP_KEYBOARD_ORIENTATION) {
            TEMP_KEYBOARD_HEIGHT = height;
            TEMP_KEYBOARD_ORIENTATION = orientation;
            SharedPreferences sharedPreferences =
                    context.getSharedPreferences(NC_IM_KIT, Context.MODE_PRIVATE);
            sharedPreferences
                    .edit()
                    .putInt(getKeyboardHeightKey(context, orientation), height)
                    .apply();
        }
    }

    private static String getKeyboardHeightKey(Context context, int orientation) {
        return KEY_KEYBOARD_HEIGHT + "_" + orientation;
    }

    public static boolean checkSDKVersionAndTargetIsTIRAMISU(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        return Build.VERSION.SDK_INT >= AndroidConstant.ANDROID_TIRAMISU
                && applicationInfo != null
                && applicationInfo.targetSdkVersion >= AndroidConstant.ANDROID_TIRAMISU;
    }

    /**
     * Note: this check must be called before checkSDKVersionAndTargetIsTIRAMISU.
     *
     * @param context context
     * @return true if both the system version and targetSdkVersion are >= 34
     */
    public static boolean checkSDKVersionAndTargetIsUDC(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        return Build.VERSION.SDK_INT >= AndroidConstant.ANDROID_UPSIDE_DOWN_CAKE
                && applicationInfo != null
                && applicationInfo.targetSdkVersion >= AndroidConstant.ANDROID_UPSIDE_DOWN_CAKE;
    }

    // Fix crash for translucent-themed Activity on Android 8.0
    public static void fixAndroid8ActivityCrash(Activity activity) {
        if (activity != null
                && Build.VERSION.SDK_INT == Build.VERSION_CODES.O
                && isTranslucentOrFloating(activity)) {
            fixOrientation(activity);
        }
    }

    private static boolean isTranslucentOrFloating(Activity activity) {
        boolean isTranslucentOrFloating = false;
        try {
            int[] styleableRes =
                    (int[])
                            Class.forName("com.android.internal.R$styleable")
                                    .getField("Window")
                                    .get(null);
            final TypedArray ta = activity.obtainStyledAttributes(styleableRes);
            Method m = ActivityInfo.class.getMethod("isTranslucentOrFloating", TypedArray.class);
            m.setAccessible(true);
            isTranslucentOrFloating = (boolean) m.invoke(null, ta);
            m.setAccessible(false);
        } catch (Exception e) {
            RLog.e(TAG, "isTranslucentOrFloating: " + "e = " + e.getMessage());
        }
        return isTranslucentOrFloating;
    }

    private static void fixOrientation(Activity activity) {
        try {
            Field field = Activity.class.getDeclaredField("mActivityInfo");
            field.setAccessible(true);
            ActivityInfo o = (ActivityInfo) field.get(activity);
            o.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
            field.setAccessible(false);
        } catch (Exception e) {
            RLog.e(TAG, "fixOrientation: " + "e = " + e.getMessage());
        }
    }
}
