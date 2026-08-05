package ai.nexconn.chatui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Text display animation helper - handles ViewHolder reuse in RecyclerView */
public class TextAnimationHelper {

    private static final long DEFAULT_ANIMATION_DURATION = 450;
    private static final float OVERSHOOT_TENSION = 0.3f;

    /** Set of currently running animations */
    private static final Map<String, AnimatorSet> sRunningAnimations = new HashMap<>();

    /** Set of message UIDs with pending animations */
    private static final Set<String> sAnimationPendingUids = new HashSet<>();

    // ==================== Public API ====================

    /** Add a message UID that needs a display animation */
    public static void addPendingAnimation(String messageUid) {
        if (!TextUtils.isEmpty(messageUid)) {
            sAnimationPendingUids.add(messageUid);
        }
    }

    /**
     * Smartly start a display animation based on message UID. Uses animation for the first display,
     * otherwise shows full content directly.
     */
    public static void startWithUidCheck(
            View containerView,
            TextView textView,
            String text,
            String messageUid,
            boolean isLeftToRight) {
        startWithUidCheck(
                containerView,
                textView,
                text,
                messageUid,
                isLeftToRight,
                DEFAULT_ANIMATION_DURATION);
    }

    /** Check whether the specified message UID needs an animation */
    public static boolean needsAnimation(String messageUid) {
        return !TextUtils.isEmpty(messageUid) && sAnimationPendingUids.contains(messageUid);
    }

    /** Check whether the specified message UID is currently animating */
    private static boolean isRunningAnimation(String messageUid) {
        return !TextUtils.isEmpty(messageUid) && sRunningAnimations.containsKey(messageUid);
    }

    /** Immediately complete the animation and show full content */
    private static void completeAnimation(
            View containerView, TextView textView, String text, String messageUid) {
        if (containerView == null || textView == null) return;

        stopAnimation(messageUid);

        // Set the final state
        textView.setText(text);
        textView.setVisibility(View.VISIBLE);
        containerView.setVisibility(View.VISIBLE);

        // Restore layout params
        ViewGroup.LayoutParams params = containerView.getLayoutParams();
        if (params.height == 0) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            containerView.setLayoutParams(params);
        }

        containerView.setClipBounds(null);
    }

    /** Clear all caches */
    public static void clearAllCache() {
        // Stop all animations
        for (AnimatorSet animatorSet : sRunningAnimations.values()) {
            animatorSet.cancel();
        }
        sRunningAnimations.clear();
        sAnimationPendingUids.clear();
    }

    // ==================== Core animation logic ====================

    /** Smart animation start method with custom duration */
    private static void startWithUidCheck(
            View containerView,
            TextView textView,
            String text,
            String messageUid,
            boolean isLeftToRight,
            long duration) {
        // Validate input parameters
        if (containerView == null || textView == null || text == null) return;

        // Check if the animation is already running
        if (!TextUtils.isEmpty(messageUid) && sRunningAnimations.containsKey(messageUid)) return;

        // Check whether the animation should start
        boolean shouldStartAnimation =
                !TextUtils.isEmpty(messageUid) && sAnimationPendingUids.contains(messageUid);

        if (shouldStartAnimation) {
            sAnimationPendingUids.remove(messageUid);
            startExpandAnimation(
                    containerView, textView, text, messageUid, isLeftToRight, duration);
        } else {
            // Show content directly (no animation)
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
            containerView.setVisibility(View.VISIBLE);

            // Restore layout parameters
            ViewGroup.LayoutParams params = containerView.getLayoutParams();
            if (params.height == 0) {
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                containerView.setLayoutParams(params);
            }

            containerView.setClipBounds(null);
        }
    }

    /** Start the expand animation */
    private static void startExpandAnimation(
            View containerView,
            TextView textView,
            String text,
            String messageUid,
            boolean isLeftToRight,
            long duration) {
        stopAnimation(messageUid);

        // Set initial state
        textView.setText(text);
        textView.setVisibility(View.VISIBLE);
        containerView.setVisibility(View.VISIBLE);

        containerView
                .getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                containerView
                                        .getViewTreeObserver()
                                        .removeOnGlobalLayoutListener(this);

                                int width = containerView.getWidth();
                                int height = containerView.getHeight();

                                if (width <= 0 || height <= 0) {
                                    // If dimensions are invalid, show content directly
                                    textView.setText(text);
                                    textView.setVisibility(View.VISIBLE);
                                    containerView.setVisibility(View.VISIBLE);
                                    containerView.setClipBounds(null);
                                    return;
                                }

                                createAndStartAnimation(
                                        containerView,
                                        messageUid,
                                        width,
                                        height,
                                        isLeftToRight,
                                        duration);
                            }
                        });
    }

    /** Create and start the animation */
    private static void createAndStartAnimation(
            View containerView,
            String messageUid,
            int finalWidth,
            int finalHeight,
            boolean isLeftToRight,
            long duration) {
        // Save and set initial layout params
        ViewGroup.LayoutParams originalParams = containerView.getLayoutParams();
        int originalHeight = originalParams.height;
        originalParams.height = 0;
        containerView.setLayoutParams(originalParams);

        // Create expand animator
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(duration);
        animator.setInterpolator(new OvershootInterpolator(OVERSHOOT_TENSION));

        // Set animation update listener
        animator.addUpdateListener(
                animation -> {
                    float progress = (Float) animation.getAnimatedValue();

                    // Update container height
                    ViewGroup.LayoutParams params = containerView.getLayoutParams();
                    params.height = (int) (finalHeight * progress);
                    containerView.setLayoutParams(params);

                    // Calculate clip region
                    int currentWidth = (int) (finalWidth * Math.min(1, progress * 1.3));
                    int currentHeight = (int) (finalHeight * progress);

                    Rect clipBounds;
                    if (isLeftToRight) {
                        // Expand from top-left corner
                        clipBounds = new Rect(0, 0, currentWidth, currentHeight);
                    } else {
                        // Expand from top-right corner
                        int leftPos = finalWidth - currentWidth;
                        clipBounds = new Rect(leftPos, 0, finalWidth, currentHeight);
                    }
                    containerView.setClipBounds(clipBounds);
                });

        // Set animation end listener
        animator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        restoreContainerState(containerView, originalHeight, messageUid);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        restoreContainerState(containerView, originalHeight, messageUid);
                    }
                });

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(animator);

        if (!TextUtils.isEmpty(messageUid)) {
            sRunningAnimations.put(messageUid, animatorSet);
        }
        animatorSet.start();
    }

    /** Stop the animation for the specified message UID */
    private static void stopAnimation(String messageUid) {
        if (TextUtils.isEmpty(messageUid)) return;

        AnimatorSet animatorSet = sRunningAnimations.remove(messageUid);
        if (animatorSet != null) {
            animatorSet.cancel();
        }
    }

    /** Restore container state */
    private static void restoreContainerState(
            View containerView, int originalHeight, String messageUid) {
        ViewGroup.LayoutParams params = containerView.getLayoutParams();
        params.height = originalHeight;
        containerView.setLayoutParams(params);

        containerView.setClipBounds(null);

        // Clean up running animation state
        if (!TextUtils.isEmpty(messageUid)) {
            sRunningAnimations.remove(messageUid);
        }
    }
}
