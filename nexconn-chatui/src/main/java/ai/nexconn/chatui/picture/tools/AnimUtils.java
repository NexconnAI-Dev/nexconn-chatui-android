package ai.nexconn.chatui.picture.tools;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

public class AnimUtils {
    private static final int DURATION = 450;

    public static void zoom(View view, boolean isZoomAnim) {
        if (isZoomAnim) {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                    ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.12f),
                    ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.12f));
            set.setDuration(DURATION);
            set.start();
        }
    }

    public static void disZoom(View view, boolean isZoomAnim) {
        if (isZoomAnim) {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                    ObjectAnimator.ofFloat(view, "scaleX", 1.12f, 1f),
                    ObjectAnimator.ofFloat(view, "scaleY", 1.12f, 1f));
            set.setDuration(DURATION);
            set.start();
        }
    }

    /**
     * Arrow rotation animation
     *
     * @param arrow ImageView
     * @param flag whether to rotate upward
     */
    public static void rotateArrow(ImageView arrow, boolean flag) {
        float pivotX = arrow.getWidth() / 2f;
        float pivotY = arrow.getHeight() / 2f;
        // Rotate upward if flag is true
        float fromDegrees = 180f;
        float toDegrees = flag ? 360f : 0f;
        RotateAnimation animation = new RotateAnimation(fromDegrees, toDegrees, pivotX, pivotY);
        animation.setDuration(350);
        arrow.startAnimation(animation);
    }
}
