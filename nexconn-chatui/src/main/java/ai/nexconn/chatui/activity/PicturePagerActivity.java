package ai.nexconn.chatui.activity;

import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.query.LocalMessagesByTimeQuery;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.message.ImageMessage;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.ReferenceMessage;
import ai.nexconn.chat.message.StreamMessage;
import ai.nexconn.chat.message.model.MessageType;
import ai.nexconn.chat.model.PageData;
import ai.nexconn.chat.params.LocalMessagesByTimeQueryParams;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.event.action.DeleteEvent;
import ai.nexconn.chatui.channel.event.action.MessageEventListener;
import ai.nexconn.chatui.config.ChatUIMediaInterceptor;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.picture.widget.longimage.SubsamplingScaleImageView;
import ai.nexconn.chatui.picture.widget.longimage.Utils;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.constant.AndroidConstant;
import ai.nexconn.chatui.utils.file.ChatUIStorageUtils;
import ai.nexconn.chatui.utils.file.FileUtils;
import ai.nexconn.chatui.utils.image.GlideUtils;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.permission.PermissionCheckUtil;
import ai.nexconn.chatui.utils.system.ExecutorHelper;
import ai.nexconn.chatui.widget.dialog.OptionsPopupDialog;
import android.Manifest;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PicturePagerActivity extends NCBaseNoActionbarActivity
        implements View.OnLongClickListener {
    private static final String TAG = "PicturePagerActivity";
    private static final int IMAGE_MESSAGE_COUNT = 10;
    private static final int LOAD_PICTURE_TIMEOUT = 30 * 1000;
    private static final long LOAD_MORE_IMAGE_DELAYED_TIME = 500;
    private static final String OBJECT_NAME = MessageType.IMAGE;
    protected ViewPager2 mViewPager;
    protected ImageMessage mCurrentImageMessage;
    protected Message mMessage;
    protected ChannelType mConversationType;
    protected int mCurrentMessageId;
    protected int currentSelectMessageId;
    protected String mTargetId = null;
    protected ImageAdapter mImageAdapter;
    Handler mainHandler = new Handler();

    protected ViewPager2.OnPageChangeCallback mPageChangeListener =
            new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    if (null == mImageAdapter) {
                        return;
                    }
                    if (mImageAdapter.getItemCount() <= 0) {
                        return;
                    }
                    // Out of bounds, return directly
                    if (position >= mImageAdapter.getItemCount()) {
                        return;
                    }
                    ImageInfo imageInfo = mImageAdapter.getItem(position);
                    if (null == imageInfo) {
                        return;
                    }
                    Message message = imageInfo.getMessage();
                    if (null == message) {
                        return;
                    }
                    currentSelectMessageId = message.getClientId();

                    if (!enableAdjacentImageFetch()) {
                        return;
                    }
                    // First load only has the passed-in message; fetch images before and after
                    if (position == 0 && mImageAdapter.getItemCount() == 1) {
                        fetchImageMessage(currentSelectMessageId, true, true);
                    }
                    // Paginate when swiping to either end
                    else if (position == (mImageAdapter.getItemCount() - 1)) {
                        fetchImageMessage(currentSelectMessageId, true, false);
                    } else if (position == 0) {
                        fetchImageMessage(currentSelectMessageId, false, true);
                    }
                }
            };
    private final ai.nexconn.chat.handler.MessageHandler mRecallMessageHandler =
            new ai.nexconn.chat.handler.MessageHandler() {
                @Override
                public void onMessageDeleted(
                        @androidx.annotation.NonNull
                                ai.nexconn.chat.message.model.MessageDeletedEvent event) {
                    if (event.getMessages() == null) return;
                    for (ai.nexconn.chat.message.Message msg : event.getMessages()) {
                        if (msg == null) continue;
                        if (currentSelectMessageId == msg.getClientId()) {
                            new android.app.AlertDialog.Builder(
                                            PicturePagerActivity.this,
                                            android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                                    .setMessage(getString(R.string.nc_recall_success))
                                    .setPositiveButton(
                                            getString(R.string.nc_dialog_ok),
                                            (dialog, which) -> finish())
                                    .setCancelable(false)
                                    .show();
                        } else {
                            mImageAdapter.removeRecallItem(msg.getClientId());
                            if (mImageAdapter.getItemCount() == 0) {
                                finish();
                            }
                        }
                    }
                }
            };
    MessageEventListener mBaseMessageEvent =
            new MessageEventListener() {
                @Override
                public void onDeleteMessage(DeleteEvent event) {
                    RLog.d(TAG, "MessageDeleteEvent");
                    if (event.getMessageIds() != null) {
                        for (int messageId : event.getMessageIds()) {
                            mImageAdapter.removeRecallItem(messageId);
                        }
                        if (mImageAdapter.getItemCount() == 0) {
                            RLog.d(TAG, "onDeleteMessage finish ");
                            finish();
                        }
                    }
                }
            };

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mImageAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (shouldInitOnCreate()) {
            initPicturePager(ai.nexconn.chatui.utils.message.MessageHolder.takeMessage());
        }
    }

    protected boolean shouldInitOnCreate() {
        return true;
    }

    protected boolean enableAdjacentImageFetch() {
        return true;
    }

    protected void initPicturePager(Message currentMessage) {
        setContentView(R.layout.nc_fr_photo);
        if (currentMessage == null || currentMessage.getContent() == null) {
            RLog.e(TAG, "initPicturePager error, message or message content is null");
            finish();
            return;
        }

        mMessage = currentMessage;
        if (currentMessage.getContent() instanceof ReferenceMessage) {
            ReferenceMessage referenceMessage = (ReferenceMessage) currentMessage.getContent();
            if (referenceMessage.getReferMsg() instanceof ImageMessage) {
                mCurrentImageMessage = (ImageMessage) referenceMessage.getReferMsg();
            }
        } else if (currentMessage.getContent() instanceof StreamMessage) {
            StreamMessage streamMessage = (StreamMessage) currentMessage.getContent();
            if (streamMessage.getReferenceInfo() != null
                    && streamMessage.getReferenceInfo().getContent() instanceof ImageMessage) {
                mCurrentImageMessage = (ImageMessage) streamMessage.getReferenceInfo().getContent();
            }
        } else if (currentMessage.getContent() instanceof ImageMessage) {
            mCurrentImageMessage = (ImageMessage) currentMessage.getContent();
        }
        if (mCurrentImageMessage == null) {
            RLog.e(TAG, "initPicturePager error, image message content is null");
            finish();
            return;
        }
        mConversationType = currentMessage.getChannelIdentifier().getChannelType();
        mCurrentMessageId = currentMessage.getClientId();
        currentSelectMessageId = mCurrentMessageId;
        mTargetId = currentMessage.getChannelIdentifier().getChannelId();

        mViewPager = findViewById(R.id.viewpager);
        mViewPager.registerOnPageChangeCallback(mPageChangeListener);
        mImageAdapter = new ImageAdapter();
        mViewPager.setAdapter(mImageAdapter);
        // Load the image message passed to this activity
        ArrayList<ImageInfo> lists = new ArrayList<>();
        lists.add(
                new ImageInfo(
                        mMessage,
                        getImageThumbUri(mCurrentImageMessage),
                        getLargeImageUri(mCurrentImageMessage)));
        mImageAdapter.addData(lists, true);

        NCChatUI.addMessageEventListener(mBaseMessageEvent);
        ai.nexconn.chat.NCEngine.addMessageHandler(
                "PicturePagerActivity_recall", mRecallMessageHandler);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ai.nexconn.chat.NCEngine.removeMessageHandler("PicturePagerActivity_recall");
        NCChatUI.removeMessageEventListener(mBaseMessageEvent);
    }

    @Override
    public void finish() {
        super.finish();
        // Clearing fullscreen flags on finish prevents a redraw flicker when returning
        // to a non-fullscreen Activity (typical symptom: RecyclerView scrolls down slightly)
        if (getWindow() != null) {
            int flag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
            getWindow().setFlags(flag, flag);
        }
    }

    private void getConversationImageUris(int messageId, final int direction) {
        if (mConversationType == null || TextUtils.isEmpty(mTargetId)) {
            return;
        }
        int index = mImageAdapter.getIndexByMessageId(messageId);
        if (index < 0) {
            return;
        }
        ImageInfo info = mImageAdapter.getItem(index);
        if (info == null || info.getMessage() == null) {
            return;
        }
        long sentTime = info.getMessage().getSentTime();
        ChannelIdentifier channelId = new ChannelIdentifier(mConversationType, mTargetId);
        LocalMessagesByTimeQueryParams params = new LocalMessagesByTimeQueryParams(channelId);
        params.setSentTime(sentTime);
        // direction 0 = FRONT (older, descending), direction 1 = BEHIND (newer, ascending)
        params.setAscending(direction == 1);
        params.setMessageTypes(Arrays.asList(OBJECT_NAME));
        params.setPageSize(IMAGE_MESSAGE_COUNT);
        LocalMessagesByTimeQuery query = BaseChannel.createLocalMessagesByTimeQuery(params);
        query.loadNextPage(
                new OperationHandler<PageData<Message>>() {
                    @Override
                    public void onResult(PageData<Message> result, NCError error) {
                        if (error != null || result == null) {
                            return;
                        }
                        List<ImageInfo> images = convertToImageInfo(result.getData(), direction);
                        if (!images.isEmpty()) {
                            runOnUiThread(
                                    () ->
                                            mImageAdapter.addData(
                                                    images, direction == 0 /* FRONT = prepend */));
                        }
                    }
                });
    }

    private @NonNull List<ImageInfo> convertToImageInfo(
            List<Message> messages, final int direction) {
        ArrayList<ImageInfo> lists = new ArrayList<>();
        if (messages == null) {
            return lists;
        }
        if (direction == 0 /* FRONT */) {
            Collections.reverse(messages);
        }
        for (Message message : messages) {
            if (!(message.getContent() instanceof ImageMessage)
                    || message.getContent().isDestruct()) {
                continue;
            }
            ImageMessage imageMessage = (ImageMessage) message.getContent();
            Uri largeImageUri = getLargeImageUri(imageMessage);
            Uri thumbUri = getImageThumbUri(imageMessage);
            if (largeImageUri != null) {
                lists.add(new ImageInfo(message, thumbUri, largeImageUri));
            }
        }
        return lists;
    }

    protected Uri getImageThumbUri(ImageMessage imageMessage) {
        Uri thumbnailUri = imageMessage.getThumbnailUri();
        if (thumbnailUri != null) {
            return thumbnailUri;
        }
        return getImageLocalUri(imageMessage);
    }

    protected Uri getImageLocalUri(ImageMessage imageMessage) {
        return imageMessage.getLocalPath() != null ? Uri.parse(imageMessage.getLocalPath()) : null;
    }

    protected Uri getImageRemoteUri(ImageMessage imageMessage) {
        return imageMessage.getRemoteUrl() != null ? Uri.parse(imageMessage.getRemoteUrl()) : null;
    }

    protected Uri getLargeImageUri(ImageMessage imageMessage) {
        Uri localUri = getImageLocalUri(imageMessage);
        return FileUtils.isFileExistsWithUri(this, localUri)
                ? localUri
                : getImageRemoteUri(imageMessage);
    }

    // Delays fetching adjacent image messages; loading too fast causes ViewPager position issues
    private void fetchImageMessage(final int msgId, boolean fetchBehind, boolean fetchFront) {
        // Do not trigger swipe-to-load for destruct (burn-after-reading) messages
        if (mMessage.getContent().isDestruct()) {
            RLog.d(TAG, "fetchImageMessage return, message is destruct");
            return;
        }
        mainHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (fetchBehind) {
                            getConversationImageUris(msgId, 1 /* BEHIND */);
                        }
                        if (fetchFront) {
                            getConversationImageUris(msgId, 0 /* FRONT */);
                        }
                    }
                },
                LOAD_MORE_IMAGE_DELAYED_TIME);
    }

    @Override
    public boolean onLongClick(View v) {
        if (mCurrentImageMessage.isDestruct()) {
            return false;
        }

        ImageInfo imageInfo = mImageAdapter.getItem(mViewPager.getCurrentItem());
        if (imageInfo != null && imageInfo.isDownload()) {
            Uri thumbUri = imageInfo.getThumbUri();
            final Uri largeImageUri = imageInfo.getLargeImageUri();
            if (onPictureLongClick(v, thumbUri, largeImageUri)) {
                return true;
            }
            String[] items = new String[] {getString(R.string.nc_save_picture)};
            OptionsPopupDialog.newInstance(this, items)
                    .setOptionsPopupDialogListener(
                            new OptionsPopupDialog.OnOptionsItemClickedListener() {
                                @Override
                                public void onOptionsItemClicked(int which) {
                                    if (which == 0) {
                                        String[] permissions = {
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        };
                                        if (Build.VERSION.SDK_INT < AndroidConstant.ANDROID_TIRAMISU
                                                && !PermissionCheckUtil.requestPermissions(
                                                        PicturePagerActivity.this, permissions)) {
                                            return;
                                        }
                                        ExecutorHelper.getInstance()
                                                .diskIO()
                                                .execute(
                                                        new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                File file;
                                                                if (largeImageUri
                                                                                .getScheme()
                                                                                .startsWith("http")
                                                                        || largeImageUri
                                                                                .getScheme()
                                                                                .startsWith(
                                                                                        "https")) {
                                                                    try {
                                                                        file =
                                                                                Glide.with(
                                                                                                PicturePagerActivity
                                                                                                        .this)
                                                                                        .asFile()
                                                                                        .load(
                                                                                                largeImageUri)
                                                                                        .submit()
                                                                                        .get(
                                                                                                10,
                                                                                                TimeUnit
                                                                                                        .SECONDS);
                                                                    } catch (ExecutionException e) {
                                                                        file = null;
                                                                        RLog.e(
                                                                                TAG,
                                                                                "onOptionsItemClicked",
                                                                                e);
                                                                    } catch (
                                                                            InterruptedException
                                                                                    e) {
                                                                        file = null;
                                                                        RLog.e(
                                                                                TAG,
                                                                                "onOptionsItemClicked",
                                                                                e);
                                                                        // Restore interrupted
                                                                        // state...
                                                                        Thread.currentThread()
                                                                                .interrupt();
                                                                    } catch (TimeoutException e) {
                                                                        file = null;
                                                                        RLog.e(
                                                                                TAG,
                                                                                "onOptionsItemClicked",
                                                                                e);
                                                                    }
                                                                } else if (largeImageUri
                                                                        .getScheme()
                                                                        .startsWith("file")) {
                                                                    file =
                                                                            new File(
                                                                                    largeImageUri
                                                                                            .toString()
                                                                                            .substring(
                                                                                                    7));
                                                                } else {
                                                                    file =
                                                                            new File(
                                                                                    largeImageUri
                                                                                            .toString());
                                                                }
                                                                final String toast;
                                                                if (file != null && file.exists()) {
                                                                    boolean result =
                                                                            ChatUIStorageUtils
                                                                                    .saveMediaToPublicDir(
                                                                                            PicturePagerActivity
                                                                                                    .this,
                                                                                            file,
                                                                                            ChatUIStorageUtils
                                                                                                    .MediaType
                                                                                                    .IMAGE);
                                                                    if (result) {
                                                                        toast =
                                                                                getString(
                                                                                        R.string
                                                                                                .nc_save_picture_at);
                                                                    } else {
                                                                        toast =
                                                                                getString(
                                                                                        R.string
                                                                                                .nc_src_file_not_found);
                                                                    }
                                                                } else {
                                                                    toast =
                                                                            getString(
                                                                                    R.string
                                                                                            .nc_src_file_not_found);
                                                                }
                                                                ExecutorHelper.getInstance()
                                                                        .mainThread()
                                                                        .execute(
                                                                                new Runnable() {
                                                                                    @Override
                                                                                    public void
                                                                                            run() {
                                                                                        ToastUtils
                                                                                                .show(
                                                                                                        PicturePagerActivity
                                                                                                                .this,
                                                                                                        toast,
                                                                                                        Toast
                                                                                                                .LENGTH_SHORT);
                                                                                    }
                                                                                });
                                                            }
                                                        });
                                    }
                                }
                            })
                    .show();
        }
        return true;
    }

    /**
     * Handles long press on an image.
     *
     * @param v the PhotoView displaying the image
     * @param thumbUri the thumbnail Uri
     * @param largeImageUri the original image Uri
     * @return true to consume the event and skip default handling
     */
    public boolean onPictureLongClick(View v, Uri thumbUri, Uri largeImageUri) {
        return false;
    }

    protected class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
        private List<ImageInfo> mImageList = new CopyOnWriteArrayList<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.nc_fr_image, parent, false);
            ViewHolder holder = new ViewHolder(view);

            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            updatePhotoView(position, holder);
            holder.mCountDownView.setBackgroundResource(R.drawable.nc_lively_common_background);
            holder.mCountDownView.setTextColor(
                    holder.mCountDownView
                            .getContext()
                            .getResources()
                            .getColor(
                                    ChatUIThemeManager.getAttrResId(
                                            holder.mCountDownView.getContext(),
                                            R.attr.nc_hint_color)));
            holder.photoView.setOnLongClickListener(PicturePagerActivity.this);
            holder.photoView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Window window = PicturePagerActivity.this.getWindow();
                            if (window != null) {
                                int flag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
                                window.setFlags(flag, flag);
                            }
                            finish();
                        }
                    });
        }

        @Override
        public int getItemCount() {
            return mImageList.size();
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            holder.boundMessageClientId = -1;
            super.onViewRecycled(holder);
        }

        private void updatePhotoView(final int position, final ViewHolder holder) {
            updatePhotoView(position, holder, null);
        }

        private void updatePhotoView(final int position, final ViewHolder holder, Object auth) {
            final ImageInfo imageInfo = mImageList.get(position);
            final Uri originalUri = imageInfo.getLargeImageUri();
            final Uri thumbUri = imageInfo.getThumbUri();
            holder.boundMessageClientId =
                    imageInfo.getMessage() != null ? imageInfo.getMessage().getClientId() : -1;

            if (originalUri == null) {
                RLog.e(TAG, "large uri of the image should not be null.");
                loadFailed(holder);
                return;
            }
            Object originalBitmapModel = originalUri;
            ChatUIMediaInterceptor interceptor =
                    NCChatUIConfig.featureConfig().getChatUIMediaInterceptor();
            if (interceptor != null && originalBitmapModel instanceof GlideUrl) {
                GlideUrl originalGlideUrl = (GlideUrl) originalBitmapModel;
                interceptor.onGlidePrepareLoad(
                        originalGlideUrl.getCacheKey(),
                        new HashMap<>(originalGlideUrl.getHeaders()),
                        map ->
                                runOnUiThread(
                                        () ->
                                                loadImageBitmapResource(
                                                        GlideUtils.buildGlideUrl(
                                                                originalGlideUrl.getCacheKey(),
                                                                map),
                                                        holder,
                                                        Uri.parse(originalGlideUrl.getCacheKey()),
                                                        thumbUri,
                                                        imageInfo)));
            } else {
                loadImageBitmapResource(
                        originalBitmapModel, holder, originalUri, thumbUri, imageInfo);
            }
        }

        // Loads an image as Bitmap via Glide
        private void loadImageBitmapResource(
                Object model,
                final ViewHolder holder,
                final Uri originalUri,
                Uri thumbUri,
                final ImageInfo imageInfo) {
            if (isDestroyed() || isFinishing()) {
                RLog.d(TAG, "loadImageBitmapResource activity isDestroyed or isFinishing");
                return;
            }
            Glide.with(PicturePagerActivity.this)
                    .asBitmap()
                    .load(model)
                    .timeout(LOAD_PICTURE_TIMEOUT)
                    .into(
                            new CustomTarget<Bitmap>() {
                                private Runnable mLoadFailedAction = null;

                                @Override
                                public void onResourceReady(
                                        @NonNull Bitmap resource,
                                        @Nullable Transition<? super Bitmap> transition) {
                                    holder.itemView.removeCallbacks(mLoadFailedAction);
                                    int maxLoader =
                                            Utils.getMaxLoader(); // Max OpenGL texture dimension
                                    Bitmap desBitmap = null;
                                    if (resource != null
                                            && resource.getWidth() < maxLoader
                                            && resource.getHeight() < maxLoader) {
                                        try {
                                            desBitmap =
                                                    resource.copy(Bitmap.Config.ARGB_8888, true);
                                        } catch (Throwable e) {
                                            RLog.e(TAG, "onResourceReady Bitmap copy error: " + e);
                                        }
                                    }
                                    if (desBitmap != null) {
                                        holder.progressText.setVisibility(View.GONE);
                                        holder.failImg.setVisibility(View.GONE);
                                        holder.progressBar.setVisibility(View.GONE);
                                        holder.photoView.setVisibility(View.VISIBLE);
                                        holder.photoView.setBitmapAndFileUri(desBitmap, null);
                                        imageInfo.download = true;
                                    } else {
                                        if (FileUtils.uriStartWithFile(originalUri)) {
                                            holder.progressText.setVisibility(View.GONE);
                                            holder.failImg.setVisibility(View.GONE);
                                            holder.progressBar.setVisibility(View.GONE);
                                            holder.photoView.setVisibility(View.VISIBLE);
                                            holder.photoView.setBitmapAndFileUri(null, originalUri);
                                            imageInfo.download = true;
                                            return;
                                        }
                                        Glide.with(PicturePagerActivity.this)
                                                .asFile()
                                                .load(originalUri)
                                                .timeout(LOAD_PICTURE_TIMEOUT)
                                                .into(
                                                        new CustomTarget<File>() {
                                                            @Override
                                                            public void onResourceReady(
                                                                    @NonNull File resource,
                                                                    @Nullable
                                                                            Transition<? super File>
                                                                                    transition) {
                                                                holder.progressText.setVisibility(
                                                                        View.GONE);
                                                                holder.failImg.setVisibility(
                                                                        View.GONE);
                                                                holder.progressBar.setVisibility(
                                                                        View.GONE);
                                                                holder.photoView.setVisibility(
                                                                        View.VISIBLE);
                                                                holder.photoView
                                                                        .setBitmapAndFileUri(
                                                                                null,
                                                                                Uri.fromFile(
                                                                                        resource));
                                                                imageInfo.download = true;
                                                            }

                                                            @Override
                                                            public void onLoadCleared(
                                                                    @Nullable
                                                                            Drawable placeholder) {
                                                                loadFailed(holder);
                                                            }
                                                        });
                                    }
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                    loadFailed(holder);
                                }

                                @Override
                                public void onLoadStarted(@Nullable Drawable placeholder) {
                                    holder.itemView.removeCallbacks(mLoadFailedAction);
                                    holder.progressBar.setVisibility(View.VISIBLE);
                                    holder.failImg.setVisibility(View.GONE);
                                    holder.progressText.setVisibility(View.GONE);
                                    holder.startLoadTime = SystemClock.elapsedRealtime();

                                    Bitmap tempBitmap = decodeBitmapFromUri(thumbUri);
                                    if (tempBitmap != null) {
                                        /*
                                         * Use setBitmapAndFileUri instead of setImage to keep zoom
                                         * gesture scale ratios consistent and to avoid occasionally
                                         * displaying the thumbnail after the full image has loaded.
                                         */
                                        holder.photoView.setVisibility(View.VISIBLE);
                                        holder.photoView.setBitmapAndFileUri(tempBitmap, null);
                                        return;
                                    }
                                    if (thumbUri != null) {
                                        loadThumbBitmapAsync(
                                                holder, imageInfo, thumbUri, "thumbnail uri");
                                    }

                                    if (imageInfo == null || imageInfo.getMessage() == null) {
                                        return;
                                    }
                                    MessageContent content = imageInfo.getMessage().getContent();
                                    if (!(content instanceof ImageMessage)) {
                                        return;
                                    }
                                    ImageMessage imageMessage = (ImageMessage) content;
                                    String thumbnailValue = imageMessage.getThumbnailBase64();

                                    if (!TextUtils.isEmpty(thumbnailValue)) {
                                        byte[] decodedBytes = decodeThumbnailBase64(thumbnailValue);
                                        if (decodedBytes != null && decodedBytes.length > 0) {
                                            tempBitmap =
                                                    BitmapFactory.decodeByteArray(
                                                            decodedBytes, 0, decodedBytes.length);
                                        } else if (isLikelyUri(thumbnailValue)) {
                                            loadThumbBitmapAsync(
                                                    holder,
                                                    imageInfo,
                                                    thumbnailValue,
                                                    "thumbnail uri");
                                            return;
                                        }
                                    }

                                    if (tempBitmap != null) {
                                        holder.photoView.setVisibility(View.VISIBLE);
                                        holder.photoView.setBitmapAndFileUri(tempBitmap, null);
                                        return;
                                    }

                                    RLog.w(
                                            TAG,
                                            "thumbnail not available, continue loading original image.");
                                }

                                @Override
                                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                    super.onLoadFailed(errorDrawable);
                                    long delayMillis =
                                            (holder.startLoadTime + LOAD_PICTURE_TIMEOUT)
                                                    - SystemClock.elapsedRealtime();
                                    holder.itemView.removeCallbacks(mLoadFailedAction);
                                    if (delayMillis > 0) {
                                        mLoadFailedAction =
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        loadFailed(holder);
                                                    }
                                                };
                                        holder.itemView.postDelayed(mLoadFailedAction, delayMillis);
                                    } else {
                                        loadFailed(holder);
                                    }
                                }
                            });
        }

        private void loadFailed(ViewHolder holder) {
            holder.progressText.setVisibility(View.VISIBLE);
            holder.progressText.setText(R.string.nc_load_image_failed);
            holder.progressBar.setVisibility(View.GONE);
            holder.failImg.setVisibility(View.VISIBLE);
            holder.failImg.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    });
            holder.photoView.setVisibility(View.INVISIBLE);
        }

        private Bitmap decodeBitmapFromUri(Uri uri) {
            if (uri == null) {
                return null;
            }
            String scheme = uri.getScheme();
            try {
                if ("file".equalsIgnoreCase(scheme)) {
                    String filePath = uri.getPath();
                    if (TextUtils.isEmpty(filePath) && uri.toString().startsWith("file://")) {
                        filePath = uri.toString().substring(7);
                    }
                    if (!TextUtils.isEmpty(filePath)) {
                        return BitmapFactory.decodeFile(filePath);
                    }
                } else if ("content".equalsIgnoreCase(scheme)) {
                    try (java.io.InputStream inputStream =
                            PicturePagerActivity.this.getContentResolver().openInputStream(uri)) {
                        if (inputStream != null) {
                            return BitmapFactory.decodeStream(inputStream);
                        }
                    }
                }
            } catch (Exception e) {
                RLog.w(TAG, "decodeBitmapFromUri failed: " + e.getMessage());
            }
            return null;
        }

        private void loadThumbBitmapAsync(
                final ViewHolder holder,
                final ImageInfo imageInfo,
                Object model,
                String modelType) {
            if (isDestroyed() || isFinishing() || model == null) {
                return;
            }
            final int expectMessageId =
                    imageInfo.getMessage() != null ? imageInfo.getMessage().getClientId() : -1;
            Glide.with(PicturePagerActivity.this)
                    .asBitmap()
                    .load(model)
                    .timeout(LOAD_PICTURE_TIMEOUT)
                    .into(
                            new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(
                                        @NonNull Bitmap resource,
                                        @Nullable Transition<? super Bitmap> transition) {
                                    if (imageInfo.isDownload()
                                            || holder.boundMessageClientId != expectMessageId) {
                                        return;
                                    }
                                    holder.photoView.setVisibility(View.VISIBLE);
                                    holder.photoView.setBitmapAndFileUri(resource, null);
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {}
                            });
            RLog.d(TAG, "load thumb by " + modelType);
        }

        private boolean isLikelyBase64(String value) {
            if (TextUtils.isEmpty(value)) {
                return false;
            }
            String normalized = normalizeBase64(value);
            if (TextUtils.isEmpty(normalized)) {
                return false;
            }
            if (normalized.length() < 16) {
                return false;
            }
            if (normalized.startsWith("http://")
                    || normalized.startsWith("https://")
                    || normalized.startsWith("file://")
                    || normalized.startsWith("content://")) {
                return false;
            }
            return normalized.matches("^[A-Za-z0-9+/=]+$");
        }

        private byte[] decodeThumbnailBase64(String value) {
            if (!isLikelyBase64(value)) {
                return null;
            }
            try {
                String normalized = normalizeBase64(value);
                if (TextUtils.isEmpty(normalized)) {
                    return null;
                }
                byte[] decoded =
                        android.util.Base64.decode(normalized, android.util.Base64.DEFAULT);
                return (decoded != null && decoded.length > 0) ? decoded : null;
            } catch (Exception e) {
                RLog.w(TAG, "Failed to decode thumbnailBase64: " + e.getMessage());
                return null;
            }
        }

        private String normalizeBase64(String value) {
            if (TextUtils.isEmpty(value)) {
                return value;
            }
            String normalized = value.trim();
            if (normalized.startsWith("data:")) {
                int idx = normalized.indexOf("base64,");
                if (idx >= 0 && idx + 7 < normalized.length()) {
                    normalized = normalized.substring(idx + 7);
                }
            }
            return normalized.replace("\n", "").replace("\r", "").trim();
        }

        private boolean isLikelyUri(String value) {
            if (TextUtils.isEmpty(value)) {
                return false;
            }
            String normalized = value.trim();
            return normalized.startsWith("http://")
                    || normalized.startsWith("https://")
                    || normalized.startsWith("file://")
                    || normalized.startsWith("content://");
        }

        // Loads an image as File via Glide
        private void loadImageFileResource(
                Object model, final ViewHolder holder, final ImageInfo imageInfo) {
            if (isDestroyed() || isFinishing()) {
                RLog.d(TAG, "loadImageFileResource activity isDestroyed or isFinishing");
                return;
            }
            // Bitmap loading failed, retry as File
            loadImageFileResource(model, holder, imageInfo);
        }

        public Bitmap zoomImg(Bitmap bm, int newWidth, int newHeight) {
            // Get original dimensions
            int width = bm.getWidth();
            int height = bm.getHeight();
            // Calculate scale ratios
            float scaleWidth = ((float) newWidth) / width;
            float scaleHeight = ((float) newHeight) / height;
            // Build the scale matrix
            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);
            // Create the scaled bitmap
            Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
            return newbm;
        }

        public void addData(List<ImageInfo> newImages, boolean direction) {
            if (newImages == null || newImages.isEmpty()) {
                return;
            }
            List<Integer> existingIds = new ArrayList<>();
            for (ImageInfo info : mImageList) {
                existingIds.add(info.getMessage().getClientId());
            }
            List<Integer> candidateIds = new ArrayList<>();
            for (ImageInfo info : newImages) {
                candidateIds.add(info.getMessage().getClientId());
            }
            List<Integer> newIds =
                    PicturePagerActivity.filterNewMessageIds(candidateIds, existingIds);
            if (newIds.isEmpty()) {
                return;
            }
            List<ImageInfo> filteredImages = new ArrayList<>();
            List<Integer> remainingIds = new ArrayList<>(newIds);
            for (ImageInfo info : newImages) {
                int messageId = info.getMessage().getClientId();
                if (remainingIds.remove(Integer.valueOf(messageId))) {
                    filteredImages.add(info);
                }
            }
            if (direction) {
                mImageList.addAll(0, filteredImages);
                notifyItemRangeInserted(0, filteredImages.size());
            } else {
                int insertPosition = mImageList.size();
                mImageList.addAll(insertPosition, filteredImages);
                notifyItemRangeInserted(insertPosition, filteredImages.size());
            }
        }

        @Nullable
        public ImageInfo getItem(int index) {
            if (index >= mImageList.size()) {
                return null;
            }
            return mImageList.get(index);
        }

        public int getIndexByMessageId(int messageId) {
            int index = -1;
            for (int i = 0; i < mImageList.size(); i++) {
                if (mImageList.get(i).getMessage().getClientId() == messageId) {
                    index = i;
                    break;
                }
            }
            return index;
        }

        private void removeRecallItem(int messageId) {
            for (int i = mImageList.size() - 1; i >= 0; i--) {
                if (mImageList.get(i).message.getClientId() == messageId) {
                    mImageList.remove(i);
                    notifyItemRemoved(i);
                    break;
                }
            }
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ProgressBar progressBar;
            TextView progressText;
            ImageView failImg;
            SubsamplingScaleImageView photoView;
            TextView mCountDownView;
            long startLoadTime;
            int boundMessageClientId = -1;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                progressBar = itemView.findViewById(R.id.nc_progress);
                progressText = itemView.findViewById(R.id.nc_txt);
                failImg = itemView.findViewById(R.id.nc_fail_image);
                photoView = itemView.findViewById(R.id.nc_photoView);
                mCountDownView = itemView.findViewById(R.id.nc_count_down);
            }
        }
    }

    static List<Integer> filterNewMessageIds(
            List<Integer> candidateIds, List<Integer> existingIds) {
        List<Integer> result = new ArrayList<>();
        java.util.Set<Integer> seenIds = new java.util.HashSet<>();
        if (existingIds != null) {
            seenIds.addAll(existingIds);
        }
        if (candidateIds == null) {
            return result;
        }
        for (Integer messageId : candidateIds) {
            if (messageId != null && seenIds.add(messageId)) {
                result.add(messageId);
            }
        }
        return result;
    }

    protected class ImageInfo {
        private Message message;
        private Uri thumbUri;
        private Uri largeImageUri;
        private boolean download;

        ImageInfo(Message message, Uri thumbnail, Uri largeImageUri) {
            this.message = message;
            this.thumbUri = thumbnail;
            this.largeImageUri = largeImageUri;
        }

        public Message getMessage() {
            return message;
        }

        public Uri getLargeImageUri() {
            return largeImageUri;
        }

        public Uri getThumbUri() {
            return thumbUri;
        }

        public boolean isDownload() {
            return download;
        }

        public void setDownload(boolean download) {
            this.download = download;
        }
    }
}
