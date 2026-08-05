package ai.nexconn.chatui.utils.common;

import ai.nexconn.chatui.utils.system.ExecutorHelper;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Toast utility class for internal SDK toast display.
 *
 * <p>Apps can use {@link #setInterceptor(ToastInterceptor)} to intercept SDK toasts.
 */
public final class ToastUtils {
    private static ToastInterceptor interceptor;
    private static Toast lastToast;

    /**
     * Shows a toast, subject to {@link #setInterceptor(ToastInterceptor)}.
     *
     * @param context context
     * @param text toast text
     * @param duration toast duration, {@link Toast#LENGTH_SHORT} or {@link Toast#LENGTH_LONG}
     * @discussion Automatically switches to the UI thread if called from a non-UI thread.
     */
    public static void show(@Nullable Context context, @Nullable CharSequence text, int duration) {
        if (context == null || text == null) {
            return;
        }

        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        showOnMainThread(context, text, duration);
                    }
                });
    }

    /**
     * Runs on the UI thread. Executes directly if already on the UI thread; otherwise posts to the
     * main thread.
     *
     * @param runnable runnable
     */
    private static void runOnUiThread(@Nullable Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
            return;
        }
        ExecutorHelper.getInstance().mainThread().execute(runnable);
    }

    /**
     * Shows a toast on the main thread.
     *
     * @param context context
     * @param text toast text
     * @param duration toast duration, {@link Toast#LENGTH_SHORT} or {@link Toast#LENGTH_LONG}
     */
    private static void showOnMainThread(
            @NonNull Context context, @NonNull CharSequence text, int duration) {
        // If intercepted, return immediately
        if (interceptor != null && !interceptor.willToast(context, text, duration)) {
            return;
        }

        // On Android 9.0+, creating a new Toast prevents duplicate display and dialog popup issues
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Toast.makeText(context, text, duration).show();
            return;
        }

        if (lastToast != null) {
            lastToast.setText(text);
        } else {
            lastToast = Toast.makeText(context, text, duration);
        }
        lastToast.show();
    }

    /**
     * Sets the toast interceptor.
     *
     * @param interceptor the interceptor {@link ToastInterceptor}
     * @discussion Set to null to cancel interception.
     */
    public static void setInterceptor(@Nullable ToastInterceptor interceptor) {
        ToastUtils.interceptor = interceptor;
    }

    /** SDK ChatUI toast interceptor. */
    public interface ToastInterceptor {
        /**
         * Determines whether to intercept the toast.
         *
         * @param context context
         * @param text toast text
         * @param duration toast duration
         * @return true: do not intercept, SDK shows the toast. false: intercept, app handles the
         *     toast.
         * @discussion Called on the UI thread.
         */
        boolean willToast(@NonNull Context context, @NonNull CharSequence text, int duration);
    }
}
