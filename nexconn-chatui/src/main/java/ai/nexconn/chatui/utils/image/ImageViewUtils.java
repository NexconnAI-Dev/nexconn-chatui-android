package ai.nexconn.chatui.utils.image;

import android.widget.ImageView;

/** ImageView utility class */
public class ImageViewUtils {

    /**
     * Enables auto-mirroring for the ImageView's Drawable, useful for directional Drawables.
     *
     * @param imageView ImageView
     */
    public static void enableDrawableAutoMirror(ImageView imageView) {
        // Enable RTL auto-mirroring for group member arrow
        if (imageView != null && imageView.getDrawable() != null) {
            imageView.getDrawable().setAutoMirrored(true);
        }
    }
}
