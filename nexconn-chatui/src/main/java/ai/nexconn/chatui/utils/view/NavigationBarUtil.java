package ai.nexconn.chatui.utils.view;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import java.lang.reflect.Method;

/** Navigation bar utility class */
public class NavigationBarUtil {
    // Core method: gets navigation bar height (returns 0 if no navigation bar)
    public static int getNavigationBarHeight(Context context) {
        if (context == null) {
            return 0;
        }
        if (!hasVirtualNavigationBar(context)) {
            return 0;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // On Xiaomi devices the value may be 0; in that case fall back to getLegacyHeight
            int height = getModernHeight(context);
            if (height != 0) {
                return height;
            }
        }
        return getLegacyHeight(context);
    }

    // Detects whether a virtual navigation bar is present (combined check)
    private static boolean hasVirtualNavigationBar(Context context) {
        // 1. Detect gesture navigation (Xiaomi/OPPO full-screen gestures, etc.)
        if (isGestureNavigationEnabled(context)) {
            return false;
        }

        // 2. Check system config (via system properties)
        return checkSystemNavigationEnabled(context);
    }

    // Android 11+ modern API approach
    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.R)
    private static int getModernHeight(Context context) {
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            WindowMetrics metrics = wm.getCurrentWindowMetrics();
            WindowInsets insets = metrics.getWindowInsets();

            // Handle Samsung landscape navigation bar displayed on the right
            if (isSamsungLandscape(context)) {
                return Math.max(
                        insets.getInsets(WindowInsets.Type.navigationBars()).right,
                        insets.getInsets(WindowInsets.Type.navigationBars()).bottom);
            }
            return insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
        } catch (Exception e) {
            return 0;
        }
    }

    // Legacy compatibility approach
    private static int getLegacyHeight(Context context) {
        // Approach 1: system resource (works on Huawei/Xiaomi)
        int resHeight = getSystemResourceHeight(context);
        if (resHeight > 0) return resHeight;

        // Approach 2: screen difference calculation (Samsung and other special devices)
        return calculateScreenDifference(context);
    }

    // Gesture navigation detection (Xiaomi/OPPO, etc.)
    private static boolean isGestureNavigationEnabled(Context context) {
        try {
            // Detect via system config (compatible with most OEMs)
            Class<?> cls = Class.forName("android.os.SystemProperties");
            Method getMethod = cls.getDeclaredMethod("get", String.class);
            String navMode = (String) getMethod.invoke(null, "ro.boot.navigation_mode");
            return "gesture".equals(navMode);
        } catch (Exception e) {
            return false;
        }
    }

    // System-level navigation bar enablement check
    private static boolean checkSystemNavigationEnabled(Context context) {
        try {
            Resources res = context.getResources();
            int resId = res.getIdentifier("config_showNavigationBar", "bool", "android");
            return resId > 0 && res.getBoolean(resId);
        } catch (Exception e) {
            return true; // Default: assume navigation bar exists
        }
    }

    // Get dimension from system resources (works on Huawei/Xiaomi)
    private static int getSystemResourceHeight(Context context) {
        try {
            int resId =
                    context.getResources()
                            .getIdentifier("navigation_bar_height", "dimen", "android");
            return resId > 0 ? context.getResources().getDimensionPixelSize(resId) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // Screen difference calculation (special handling for Samsung landscape)
    private static int calculateScreenDifference(Context context) {
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();

            DisplayMetrics realMetrics = new DisplayMetrics();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealMetrics(realMetrics);
            }

            DisplayMetrics displayMetrics = new DisplayMetrics();
            display.getMetrics(displayMetrics);

            if (isLandscape(context)) {
                return realMetrics.widthPixels - displayMetrics.widthPixels;
            } else {
                return realMetrics.heightPixels - displayMetrics.heightPixels;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    // Samsung landscape detection
    private static boolean isSamsungLandscape(Context context) {
        return Build.MANUFACTURER != null
                && Build.MANUFACTURER.toLowerCase().contains("samsung")
                && context.getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_LANDSCAPE;
    }

    private static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
    }
}
