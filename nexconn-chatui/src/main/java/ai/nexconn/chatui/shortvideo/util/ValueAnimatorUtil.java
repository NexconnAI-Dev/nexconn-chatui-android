package ai.nexconn.chatui.shortvideo.util;

import ai.nexconn.chatui.utils.log.RLog;
import android.animation.ValueAnimator;
import androidx.annotation.NonNull;
import java.lang.reflect.Field;

/** Created by zhouxuming on 2018/11/8. */
public class ValueAnimatorUtil {
    private static final String TAG = ValueAnimatorUtil.class.getSimpleName();

    /** Reset animation duration scale if animations are disabled */
    public static void resetDurationScaleIfDisable() {
        if (getDurationScale() == 0) resetDurationScale();
    }

    /** Reset animation duration scale */
    public static void resetDurationScale() {
        try {
            getField().setFloat(null, 1);
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
        }
    }

    private static float getDurationScale() {
        try {
            return getField().getFloat(null);
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
            return -1;
        }
    }

    @NonNull
    private static Field getField() throws NoSuchFieldException {
        Field field = ValueAnimator.class.getDeclaredField("sDurationScale");
        field.setAccessible(true);
        return field;
    }
}
