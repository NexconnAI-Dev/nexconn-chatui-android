package ai.nexconn.chatui.channel.messagelist.provider;

import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.ShortVideoMessage;
import ai.nexconn.chat.message.model.MessageDirection;
import ai.nexconn.chat.message.model.SentStatus;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.feature.resend.ResendManager;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.picture.tools.ScreenUtils;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.file.FileUtils;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.widget.CircleProgressView;
import ai.nexconn.chatui.widget.adapter.IViewProviderListener;
import ai.nexconn.chatui.widget.adapter.ViewHolder;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import java.util.List;

public class ShortVideoMessageItemProvider extends BaseMessageItemProvider<ShortVideoMessage> {

    private Integer minShortSideSize;

    public ShortVideoMessageItemProvider() {
        mConfig.showReadState = true;
        mConfig.showContentBubble = false;
        mConfig.showProgress = false;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.nc_item_sight_message, parent, false);
        return new ViewHolder(view.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(
            ViewHolder holder,
            ViewHolder parentHolder,
            ShortVideoMessage shortVideoMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        int progress = uiMessage.getProgress();
        final SentStatus status = uiMessage.getMessage().getSentStatus();
        holder.setVisible(R.id.nc_sight_thumb, true);
        String thumbnailBase64 = shortVideoMessage.getThumbnailBase64();
        String localPath = shortVideoMessage.getLocalPath();
        String remoteUrl = shortVideoMessage.getRemoteUrl();
        Object loadSource = null;
        String loadKey = null;
        if (thumbnailBase64 != null
                && !thumbnailBase64.isEmpty()
                && isLikelyBase64(thumbnailBase64)) {
            try {
                byte[] decodedBytes = Base64.decode(thumbnailBase64, Base64.DEFAULT);
                if (decodedBytes != null && decodedBytes.length > 0) {
                    loadSource = decodedBytes;
                    loadKey = "b64:" + thumbnailBase64.hashCode();
                }
            } catch (IllegalArgumentException e) {
                RLog.w(TAG, "Invalid sight thumbnailBase64, fallback to uri source");
            }
        }
        if (loadSource == null
                && localPath != null
                && FileUtils.isFileExistsWithUri(holder.getContext(), Uri.parse(localPath))) {
            loadSource = Uri.parse(localPath);
            loadKey = "local:" + localPath;
        } else if (loadSource == null && remoteUrl != null && !remoteUrl.isEmpty()) {
            loadSource = remoteUrl;
            loadKey = "remote:" + remoteUrl.hashCode();
        }
        final ImageView imageView = holder.getView(R.id.nc_sight_thumb);
        final ImageView readyButton = holder.getView(R.id.nc_sight_tag);
        if (!checkViewsValid(imageView, readyButton)) {
            RLog.e(TAG, "checkViewsValid error," + uiMessage.getMessageType());
            return;
        }
        if (loadSource != null) {
            String oldLoadKey = (String) imageView.getTag(R.id.nc_sight_thumb);
            if (!TextUtils.equals(oldLoadKey, loadKey)) {
                imageView.setTag(R.id.nc_sight_thumb, loadKey);
                RoundedCorners roundedCorners =
                        new RoundedCorners(ScreenUtils.dip2px(holder.getContext(), 6));
                RequestOptions options =
                        RequestOptions.bitmapTransform(roundedCorners)
                                .override(300, 300)
                                .dontAnimate();
                Glide.with(imageView)
                        .load(loadSource)
                        .apply(options)
                        .listener(
                                new RequestListener<Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(
                                            @Nullable GlideException e,
                                            Object model,
                                            Target<Drawable> target,
                                            boolean isFirstResource) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(
                                            Drawable resource,
                                            Object model,
                                            Target<Drawable> target,
                                            DataSource dataSource,
                                            boolean isFirstResource) {
                                        measureLayoutParams(imageView, readyButton, resource);
                                        return false;
                                    }
                                })
                        .into(imageView);
            }
        } else {
            imageView.setTag(R.id.nc_sight_thumb, null);
            Glide.with(imageView).clear(imageView);
        }
        holder.setText(R.id.nc_sight_duration, getSightDuration(shortVideoMessage.getDuration()));
        CircleProgressView loadingProgress = holder.getView(R.id.nc_sight_progress);
        ProgressBar compressProgress = holder.getView(R.id.compressVideoBar);
        if (!checkViewsValid(loadingProgress, compressProgress)) {
            RLog.e(TAG, "checkViewsValid error," + uiMessage.getMessageType());
            return;
        }
        final MessageDirection direction = uiMessage.getMessage().getDirection();
        // The download progress ring only applies to received (downloading) messages; an
        // outgoing message that is still uploading must fall through to the sending spinner.
        if (direction == MessageDirection.RECEIVE && progress > 0 && progress < 100) {
            loadingProgress.setProgress(progress, true);
            holder.setVisible(R.id.nc_sight_tag, false);
            loadingProgress.setVisibility(View.VISIBLE);
            compressProgress.setVisibility(View.GONE);
        } else if (status == SentStatus.SENDING) {
            holder.setVisible(R.id.nc_sight_tag, false);
            loadingProgress.setVisibility(View.GONE);
            compressProgress.setVisibility(View.VISIBLE);
        } else if (status == SentStatus.FAILED
                && ResendManager.getInstance().needResend(uiMessage.getMessage().getClientId())) {
            holder.setVisible(R.id.nc_sight_tag, false);
            loadingProgress.setVisibility(View.GONE);
            compressProgress.setVisibility(View.VISIBLE);
        } else {
            holder.setVisible(R.id.nc_sight_tag, true);
            loadingProgress.setVisibility(View.GONE);
            compressProgress.setVisibility(View.GONE);
        }
    }

    @Override
    protected boolean onItemClick(
            ViewHolder holder,
            ShortVideoMessage shortVideoMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        if (shortVideoMessage != null) {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("nc")
                    .authority(holder.getContext().getPackageName())
                    .appendPath("sight")
                    .appendPath("player");
            String intentUrl = builder.build().toString();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(intentUrl));
            intent.setPackage(holder.getContext().getPackageName());
            ai.nexconn.chat.message.Message ncMsg = uiMessage.getMessage();
            if (ncMsg != null && ncMsg.getContent() instanceof ShortVideoMessage) {
                ai.nexconn.chatui.utils.message.MessageHolder.holdMessage(ncMsg);
            }
            intent.putExtra("Progress", uiMessage.getProgress());
            if (intent.resolveActivity(holder.getContext().getPackageManager()) != null) {
                holder.getContext().startActivity(intent);
            } else {
                ToastUtils.show(
                        holder.getContext(), "Sight Module does not exist.", Toast.LENGTH_SHORT);
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof ShortVideoMessage && !messageContent.isDestruct();
    }

    private void measureLayoutParams(View view, ImageView readyButton, Drawable drawable) {
        float width = drawable.getIntrinsicWidth();
        float height = drawable.getIntrinsicHeight();
        int finalWidth;
        int finalHeight;
        int minSize = 100;
        if (minShortSideSize == null) {
            minShortSideSize = ScreenUtils.dip2px(view.getContext(), 140);
        }
        if (minShortSideSize > 0) {
            if (width >= minShortSideSize || height >= minShortSideSize) {
                float scale = width / height;

                if (scale > 1) {
                    finalHeight = (int) (minShortSideSize / scale);
                    if (finalHeight < minSize) {
                        finalHeight = minSize;
                    }
                    finalWidth = (int) minShortSideSize;
                } else {
                    finalHeight = (int) minShortSideSize;
                    finalWidth = (int) (minShortSideSize * scale);
                    if (finalWidth < minSize) {
                        finalWidth = minSize;
                    }
                }

                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.height = finalHeight;
                params.width = finalWidth;
                view.setLayoutParams(params);
                measureReadyButton(readyButton, drawable, finalWidth, finalHeight);
            } else {
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.height = (int) height;
                params.width = (int) width;
                view.setLayoutParams(params);
                measureReadyButton(readyButton, drawable, width, height);
            }
        }
    }

    private void measureReadyButton(
            ImageView readyButton, Drawable drawable, float finalWidth, float finalHeight) {
        if (readyButton == null || drawable == null) {
            return;
        }
        int intrinsicHeight = drawable.getIntrinsicHeight();
        int intrinsicWidth = drawable.getIntrinsicWidth();
        if (intrinsicHeight == 0 || intrinsicWidth == 0 || finalHeight == 0 || finalWidth == 0) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = readyButton.getLayoutParams();
        int readyButtonSize;
        if ((intrinsicWidth / (finalWidth * 1.0)) > (intrinsicHeight / (finalHeight * 1.0))) {
            readyButtonSize = (int) (finalHeight * (intrinsicHeight / (intrinsicWidth * 1.0)));
        } else {
            readyButtonSize = (int) (finalWidth * (intrinsicWidth / (intrinsicHeight * 1.0)));
        }
        int min =
                Math.min(
                        readyButtonSize,
                        readyButton
                                .getResources()
                                .getDimensionPixelSize(R.dimen.nc_sight_play_size));
        layoutParams.width = min;
        layoutParams.height = min;
        readyButton.setLayoutParams(layoutParams);
    }

    private String getSightDuration(int time) {
        String recordTime;
        int hour, minute, second;
        if (time <= 0) {
            return "00:00";
        } else {
            minute = time / 60;
            if (minute < 60) {
                second = time % 60;
                recordTime = unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99) {
                    return "99:59:59";
                }
                minute = minute % 60;
                second = time - hour * 3600 - minute * 60;
                recordTime = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return recordTime;
    }

    private String unitFormat(int time) {
        String formatTime;
        if (time >= 0 && time < 10) {
            formatTime = "0" + time;
        } else {
            formatTime = "" + time;
        }
        return formatTime;
    }

    @Override
    public Spannable getSummarySpannable(Context context, ShortVideoMessage shortVideoMessage) {
        return new SpannableString(
                context.getString(R.string.nc_conversation_summary_content_sight));
    }

    private boolean isLikelyBase64(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        String normalized = value.replace("\n", "").replace("\r", "").trim();
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
}
