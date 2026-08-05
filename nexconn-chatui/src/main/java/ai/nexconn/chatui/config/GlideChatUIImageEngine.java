package ai.nexconn.chatui.config;

import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.utils.image.GlideUtils;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.ImageView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.Target;

/**
 * Default {@link ChatUIImageEngine} implementation backed by Glide.
 *
 * <p>Loaded automatically by the SDK unless a custom engine is set via {@link
 * ai.nexconn.chatui.config.FeatureConfig#setChatUIImageEngine}.
 */
public class GlideChatUIImageEngine implements ChatUIImageEngine {
    private Transformation<Bitmap> transformation = new CenterCrop();

    /**
     * Loads an image into the given ImageView.
     *
     * @param context context
     * @param url image URL
     * @param imageView target ImageView
     */
    @Override
    public void loadImage(
            @NonNull Context context, @NonNull String url, @NonNull ImageView imageView) {
        Glide.with(context)
                .load(url)
                .error(R.drawable.nc_received_thumb_image_broken)
                .into(imageView);
    }

    /**
     * Loads a thumbnail for an album/folder entry with rounded corners.
     *
     * @param context context
     * @param url image path
     * @param imageView target ImageView
     */
    @Override
    public void loadFolderImage(
            @NonNull final Context context,
            @NonNull String url,
            @NonNull final ImageView imageView) {
        Glide.with(context)
                .asBitmap()
                .override(180, 180)
                .centerCrop()
                .sizeMultiplier(0.5f)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                //                .placeholder(R.drawable.picture_icon_placeholder)
                .load(url)
                .into(
                        new BitmapImageViewTarget(imageView) {
                            @Override
                            protected void setResource(Bitmap resource) {
                                RoundedBitmapDrawable circularBitmapDrawable =
                                        RoundedBitmapDrawableFactory.create(
                                                context.getResources(), resource);
                                circularBitmapDrawable.setCornerRadius(8);
                                imageView.setImageDrawable(circularBitmapDrawable);
                            }
                        });
    }

    /**
     * Loads a GIF image into the given ImageView.
     *
     * @param context context
     * @param url GIF URL or path
     * @param imageView target ImageView
     */
    @Override
    public void loadAsGifImage(
            @NonNull Context context, @NonNull String url, @NonNull ImageView imageView) {
        Object model = resolveModel(url);
        Glide.with(context)
                .asGif()
                .load(model)
                .listener(
                        new RequestListener<GifDrawable>() {
                            @Override
                            public boolean onLoadFailed(
                                    GlideException e,
                                    Object model,
                                    Target<GifDrawable> target,
                                    boolean isFirstResource) {
                                imageView.post(
                                        () ->
                                                Glide.with(context)
                                                        .asBitmap()
                                                        .load(model)
                                                        .fitCenter()
                                                        .error(
                                                                R.drawable
                                                                        .nc_received_thumb_image_broken)
                                                        .into(imageView));
                                return true;
                            }

                            @Override
                            public boolean onResourceReady(
                                    GifDrawable resource,
                                    Object model,
                                    Target<GifDrawable> target,
                                    DataSource dataSource,
                                    boolean isFirstResource) {
                                return false;
                            }
                        })
                .error(R.drawable.nc_received_thumb_image_broken)
                .into(imageView);
    }

    /**
     * Loads a thumbnail for a grid image picker item.
     *
     * @param context context
     * @param url image URL or path
     * @param imageView target ImageView
     */
    @Override
    public void loadGridImage(
            @NonNull Context context, @NonNull String url, @NonNull ImageView imageView) {
        Glide.with(context)
                .asBitmap()
                .load(url)
                .override(200, 200)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                //                .placeholder(R.drawable.picture_image_placeholder)
                .into(imageView);
    }

    @Override
    public void loadConversationListPortrait(
            @NonNull Context context,
            @NonNull String url,
            @NonNull ImageView imageView,
            BaseChannel channel) {
        @DrawableRes
        int resourceId =
                ChatUIThemeManager.getAttrResId(
                        context, R.attr.nc_conversation_list_cell_portrait_msg_img);
        if (channel.getChannelType() == ChannelType.GROUP) {
            resourceId =
                    ChatUIThemeManager.getAttrResId(
                            context, R.attr.nc_conversation_list_cell_group_portrait_img);
        } else if (channel.getChannelType() == ChannelType.OPEN) {
            resourceId =
                    ChatUIThemeManager.getAttrResId(
                            context, R.attr.nc_conversation_list_cell_discussion_portrait_img);
        }
        loadPortrait(context, url, imageView, resourceId);
    }

    @Override
    public void loadConversationPortrait(
            @NonNull Context context,
            @NonNull String url,
            @NonNull ImageView imageView,
            Message message) {
        @DrawableRes
        int resourceId =
                ChatUIThemeManager.getAttrResId(
                        context, R.attr.nc_conversation_list_cell_portrait_msg_img);

        loadPortrait(context, url, imageView, resourceId);
    }

    @Override
    public void loadUserPortrait(
            @NonNull Context context, @NonNull String url, @NonNull ImageView imageView) {
        int defaultPortrait =
                ChatUIThemeManager.getAttrResId(
                        context, R.attr.nc_conversation_list_cell_portrait_msg_img);
        Glide.with(imageView)
                .load(url)
                .placeholder(defaultPortrait)
                .error(defaultPortrait)
                .apply(RequestOptions.bitmapTransform(getPortraitTransformation()))
                .into(imageView);
    }

    @Override
    public void loadGroupPortrait(
            @NonNull Context context, @NonNull String url, @NonNull ImageView imageView) {
        int defaultGroupPortrait =
                ChatUIThemeManager.getAttrResId(
                        context, R.attr.nc_conversation_list_cell_group_portrait_img);
        Glide.with(imageView)
                .load(url)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .error(
                        Glide.with(imageView)
                                .load(defaultGroupPortrait)
                                .apply(RequestOptions.bitmapTransform(new CircleCrop())))
                .into(imageView);
    }

    // Load portrait with optional media interceptor
    private void loadPortrait(
            @NonNull Context context,
            @NonNull String url,
            @NonNull ImageView imageView,
            @DrawableRes int resourceId) {
        ChatUIMediaInterceptor interceptor =
                NCChatUIConfig.featureConfig().getChatUIMediaInterceptor();
        boolean isHttpUrl = url.startsWith("http") || url.startsWith("https");
        if (!isHttpUrl || interceptor == null) {
            loadImage(context, url, imageView, resourceId);
            return;
        }

        interceptor.onGlidePrepareLoad(
                url,
                null,
                map ->
                        imageView.post(
                                () ->
                                        loadImage(
                                                context,
                                                GlideUtils.buildGlideUrl(url, map),
                                                imageView,
                                                resourceId)));
    }

    private void loadImage(Context context, Object model, ImageView imageView, int resourceId) {
        if (context instanceof Activity) {
            if (((Activity) context).isDestroyed() || ((Activity) context).isFinishing()) {
                return;
            }
        }

        Glide.with(imageView)
                .load(model)
                .placeholder(resourceId)
                .error(resourceId)
                .apply(RequestOptions.bitmapTransform(getPortraitTransformation()))
                .into(imageView);
    }

    private Object resolveModel(@NonNull String url) {
        if (url.startsWith("content://") || url.startsWith("file://")) {
            return Uri.parse(url);
        }
        return url;
    }

    protected Transformation<Bitmap> getPortraitTransformation() {
        return transformation;
    }
}
