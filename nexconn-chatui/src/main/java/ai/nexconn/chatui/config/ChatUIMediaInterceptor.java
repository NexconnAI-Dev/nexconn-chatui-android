package ai.nexconn.chatui.config;

import android.webkit.WebView;
import java.util.Map;

/**
 * Interceptor for media resource loading in the ChatUI SDK.
 *
 * <p>Implement this interface to customize HTTP headers for Glide image requests and to intercept
 * WebView resource loading.
 *
 * <p>Register an implementation via {@link
 * ai.nexconn.chatui.config.FeatureConfig#setChatUIMediaInterceptor}.
 */
public interface ChatUIMediaInterceptor {

    /**
     * Called before Glide loads an image, allowing custom HTTP headers to be injected.
     *
     * <p>Use cases:
     *
     * <ul>
     *   <li>Large image preview screen ({@code PicturePagerActivity})
     *   <li>Default avatar loading in {@link GlideChatUIImageEngine} ({@code
     *       loadConversationListPortrait}, {@code loadConversationPortrait})
     * </ul>
     *
     * <p><b>Note:</b> This callback runs on the main thread — do not perform blocking operations.
     * Switch to a background thread if heavy work is needed.
     *
     * <p><b>Note:</b> You must call {@code callback.onComplete(headers)} to return the headers; the
     * SDK will not proceed with image loading until the callback is invoked.
     *
     * @param url image URL
     * @param headers HTTP headers already set for this request
     * @param callback invoke {@code onComplete(headers)} to pass the (possibly modified) headers
     *     back
     */
    void onGlidePrepareLoad(
            String url, Map<String, String> headers, Callback<Map<String, String>> callback);

    /**
     * Intercepts a WebView resource request.
     *
     * <p>The WebView checks this method first; if it returns {@code true} the network request is
     * skipped. Returning {@code false} lets the WebView load the resource normally.
     *
     * <p><b>Note:</b> This callback runs on a WebView worker thread.
     *
     * @param view WebView making the request
     * @param url resource URL
     * @return {@code true} to intercept the request; {@code false} to allow it
     */
    boolean shouldInterceptRequest(WebView view, String url);

    /**
     * Called when a combined-message portrait URL is about to be loaded.
     *
     * <p>Override to transform the URL — for example, to append an authentication token.
     *
     * @param url the original portrait URL
     * @return the URL to use for loading (return the original {@code url} to use it unchanged)
     */
    default String onCombinePortraitLoad(String url) {
        return url;
    }

    /**
     * Completion callback for asynchronous interceptor operations.
     *
     * @param <T> result type
     */
    interface Callback<T> {
        /**
         * Called when the interceptor has finished processing.
         *
         * @param t the result to pass back to the SDK
         */
        void onComplete(T t);
    }
}
