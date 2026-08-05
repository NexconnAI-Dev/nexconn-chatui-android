package ai.nexconn.chatui.utils.image;

import android.text.TextUtils;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import java.util.Map;

/** Glide utility class */
public class GlideUtils {

    /**
     * Builds a GlideUrl with optional headers before loading images and avatars.
     *
     * @param url image URL
     * @param headers HTTP headers for image loading
     */
    public static Object buildGlideUrl(String url, Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return new GlideUrl(url);
        } else {
            LazyHeaders.Builder builder = new LazyHeaders.Builder();
            for (Map.Entry<String, String> item : headers.entrySet()) {
                builder.addHeader(item.getKey(), item.getValue());
            }
            return new GlideUrl(url, builder.build());
        }
    }

    /**
     * Gets the file name from an image URL.
     *
     * @param url image URL
     * @return file name extracted from the URL
     */
    public static String getUrlName(String url) {
        if (TextUtils.isEmpty(url)) {
            return "temp";
        }
        String name = "";
        int start = url.lastIndexOf("/");
        if (start != -1) {
            name = url.substring(start + 1);
        }
        int indexOfSuffix = name.lastIndexOf(".");
        if (indexOfSuffix != -1) {
            return name.substring(0, indexOfSuffix);
        }
        return name;
    }
}
