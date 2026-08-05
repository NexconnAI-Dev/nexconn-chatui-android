package ai.nexconn.chatui.picture.engine;

import android.content.Context;
import android.widget.ImageView;
import androidx.annotation.NonNull;

/** Image loading interface for gallery */
public interface ImageEngine {
    /**
     * Load image
     *
     * @param context
     * @param url
     * @param imageView
     */
    void loadImage(@NonNull Context context, @NonNull String url, @NonNull ImageView imageView);

    /**
     * Load album directory image
     *
     * @param context context
     * @param url image path
     * @param imageView target ImageView
     */
    void loadFolderImage(
            @NonNull Context context, @NonNull String url, @NonNull ImageView imageView);

    /**
     * Load GIF image
     *
     * @param context context
     * @param url image path
     * @param imageView target ImageView
     */
    void loadAsGifImage(
            @NonNull Context context, @NonNull String url, @NonNull ImageView imageView);

    /**
     * Load grid image
     *
     * @param context context
     * @param url image path
     * @param imageView target ImageView
     */
    void loadGridImage(@NonNull Context context, @NonNull String url, @NonNull ImageView imageView);
}
