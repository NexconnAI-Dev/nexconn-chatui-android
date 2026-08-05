package ai.nexconn.chatui.picture.adapter;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.picture.anim.OptAnimationLoader;
import ai.nexconn.chatui.picture.config.PictureConfig;
import ai.nexconn.chatui.picture.config.PictureMimeType;
import ai.nexconn.chatui.picture.config.PictureSelectionConfig;
import ai.nexconn.chatui.picture.entity.LocalMedia;
import ai.nexconn.chatui.picture.tools.AnimUtils;
import ai.nexconn.chatui.picture.tools.DateUtils;
import ai.nexconn.chatui.picture.tools.MediaUtils;
import ai.nexconn.chatui.picture.tools.PictureFileUtils;
import ai.nexconn.chatui.picture.tools.SdkVersionUtils;
import ai.nexconn.chatui.picture.tools.ToastUtils;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PictureImageGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private boolean showCamera;
    private OnPhotoSelectChangedListener imageSelectChangedListener;
    private int maxSelectNum;
    private List<LocalMedia> images = new ArrayList<>();
    private List<LocalMedia> selectImages = new ArrayList<>();
    private boolean enablePreview;
    private int selectMode;
    private boolean enablePreviewVideo;
    private boolean is_checked_num;
    private Animation animation;
    private PictureSelectionConfig config;
    private boolean zoomAnim;
    private boolean isSingleDirectReturn;

    public PictureImageGridAdapter(Context context, PictureSelectionConfig config) {
        this.context = context;
        this.config = config;
        this.selectMode = config.selectionMode;
        this.showCamera = config.isCamera;
        this.maxSelectNum = config.maxSelectNum;
        this.enablePreview = config.enablePreview;
        this.enablePreviewVideo = config.enPreviewVideo;
        this.is_checked_num = config.checkNumMode;
        this.zoomAnim = config.zoomAnim;
        this.isSingleDirectReturn = config.isSingleDirectReturn;
        animation = OptAnimationLoader.loadAnimation(context, R.anim.nc_picture_anim_modal_in);
    }

    public void setShowCamera(boolean showCamera) {
        this.showCamera = showCamera;
    }

    public void bindImagesData(List<LocalMedia> images) {
        this.images = images;
        notifyDataSetChanged();
    }

    public void bindSelectImages(List<LocalMedia> images) {
        // Reconstruct a new collection to avoid the issue where changes to the selected collection
        // also affect the result collection
        List<LocalMedia> selection = new ArrayList<>();
        for (LocalMedia media : images) {
            selection.add(media);
        }
        this.selectImages = selection;
        if (!config.isSingleDirectReturn) {
            subSelectPosition();
            if (imageSelectChangedListener != null) {
                imageSelectChangedListener.onChange(selectImages);
            }
        }
    }

    public List<LocalMedia> getSelectedImages() {
        return selectImages == null ? new ArrayList<LocalMedia>() : selectImages;
    }

    public List<LocalMedia> getImages() {
        return images == null ? new ArrayList<LocalMedia>() : images;
    }

    @Override
    public int getItemViewType(int position) {
        if (showCamera && position == 0) {
            return PictureConfig.TYPE_CAMERA;
        } else {
            return PictureConfig.TYPE_PICTURE;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == PictureConfig.TYPE_CAMERA) {
            View view =
                    LayoutInflater.from(context)
                            .inflate(R.layout.nc_picture_item_camera, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view =
                    LayoutInflater.from(context)
                            .inflate(R.layout.nc_picture_image_grid_item, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        if (getItemViewType(position) == PictureConfig.TYPE_CAMERA) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.headerView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (imageSelectChangedListener != null) {
                                imageSelectChangedListener.onTakePhoto();
                            }
                        }
                    });
        } else {
            final ViewHolder contentHolder = (ViewHolder) holder;
            final LocalMedia image = images.get(showCamera ? position - 1 : position);
            image.position = contentHolder.getAdapterPosition();
            final String path = image.getPath();
            final String mimeType = image.getMimeType();
            if (is_checked_num) {
                notifyCheckChanged(contentHolder, image);
            }
            selectImage(contentHolder, isSelected(image), false);

            boolean gif = PictureMimeType.isGif(mimeType);
            contentHolder.tvCheck.setVisibility(isSingleDirectReturn ? View.GONE : View.VISIBLE);
            contentHolder.btnCheck.setVisibility(isSingleDirectReturn ? View.GONE : View.VISIBLE);
            contentHolder.tvIsGif.setVisibility(gif ? View.VISIBLE : View.GONE);
            boolean eqImage = PictureMimeType.eqImage(image.getMimeType());
            if (eqImage) {
                boolean eqLongImg = MediaUtils.isLongImg(image);
                contentHolder.tvLongChart.setVisibility(eqLongImg ? View.VISIBLE : View.GONE);
            } else {
                contentHolder.tvLongChart.setVisibility(View.GONE);
            }
            contentHolder.tvDuration.setText(
                    DateUtils.getInstance().formatDurationTime(image.getDuration()));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                contentHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.nc_picture_icon_video, 0, 0, 0);
            }
            contentHolder.tvDuration.setVisibility(
                    PictureMimeType.eqVideo(mimeType) ? View.VISIBLE : View.GONE);
            if (config != null && config.imageEngine != null) {
                config.imageEngine.loadGridImage(context, path, contentHolder.ivPicture);
            }

            if (enablePreview || enablePreviewVideo) {
                contentHolder.btnCheck.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                {
                                    // If the original path doesn't exist or the path exists but the
                                    // file doesn't
                                    String newPath =
                                            SdkVersionUtils.checkedAndroid_Q()
                                                    ? PictureFileUtils.getPath(
                                                            context, Uri.parse(path))
                                                    : path;
                                    if (TextUtils.isEmpty(newPath) || !new File(newPath).exists()) {
                                        ToastUtils.s(context, PictureMimeType.s(context, mimeType));
                                        return;
                                    }
                                    if (isGIFAboveMaxSize(image)) {
                                        return;
                                    }

                                    if (PictureMimeType.eqVideo(mimeType)) {
                                        if (TextUtils.isEmpty(contentHolder.tvDuration.getText()))
                                            return;
                                        int maxDuration =
                                                PictureSelectionConfig.getInstance()
                                                        .videoDurationLimit;
                                        if (maxDuration < 1)
                                            maxDuration =
                                                    PictureConfig.DEFAULT_VIDEO_DURATION_LIMIT;
                                        String[] videoTime =
                                                contentHolder
                                                        .tvDuration
                                                        .getText()
                                                        .toString()
                                                        .split(":");
                                        if ((Integer.parseInt(videoTime[0]) * 60
                                                        + Integer.parseInt(videoTime[1]))
                                                > maxDuration) {
                                            String time;
                                            if (maxDuration >= 60) {
                                                time =
                                                        context.getResources()
                                                                .getString(
                                                                        R.string
                                                                                .nc_picsel_selected_max_time_span_with_param,
                                                                        Math.round(
                                                                                        maxDuration
                                                                                                / 60.0
                                                                                                * 10)
                                                                                / 10.0);
                                            } else {
                                                time =
                                                        context.getResources()
                                                                .getString(
                                                                        R.string
                                                                                .nc_picsel_selected_max_second_span_with_param,
                                                                        maxDuration);
                                            }

                                            new AlertDialog.Builder(context)
                                                    .setMessage(time)
                                                    .setPositiveButton(R.string.nc_confirm, null)
                                                    .setCancelable(false)
                                                    .create()
                                                    .show();
                                            return;
                                        }
                                    }
                                    if (PictureMimeType.isGif(mimeType)) {
                                        long limit = config.gifSizeLimit;
                                        if (limit != -1 && limit < image.getSize()) {
                                            new AlertDialog.Builder(context)
                                                    .setMessage(
                                                            context.getResources()
                                                                    .getString(
                                                                            R.string
                                                                                    .nc_send_large_gif_failed))
                                                    .setPositiveButton(R.string.nc_confirm, null)
                                                    .setCancelable(false)
                                                    .create()
                                                    .show();
                                            return;
                                        }
                                    }
                                    changeCheckboxState(contentHolder, image);
                                }
                            }
                        });
            }
            contentHolder.contentView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            {
                                // If the original path doesn't exist or the path exists but the
                                // file doesn't
                                String newPath =
                                        SdkVersionUtils.checkedAndroid_Q()
                                                ? PictureFileUtils.getPath(context, Uri.parse(path))
                                                : path;
                                if (TextUtils.isEmpty(newPath) || !new File(newPath).exists()) {
                                    ToastUtils.s(context, PictureMimeType.s(context, mimeType));
                                    return;
                                }
                                int index = showCamera ? position - 1 : position;
                                if (index == -1) {
                                    return;
                                }
                                boolean eqResult =
                                        PictureMimeType.eqImage(mimeType) && enablePreview
                                                || PictureMimeType.eqVideo(mimeType)
                                                        && (enablePreviewVideo
                                                                || selectMode
                                                                        == PictureConfig.SINGLE);
                                if (eqResult) {
                                    imageSelectChangedListener.onPictureClick(image, index);
                                } else {
                                    changeCheckboxState(contentHolder, image);
                                }
                            }
                        }
                    });
        }
    }

    @Override
    public int getItemCount() {
        return showCamera ? images.size() + 1 : images.size();
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {
        View headerView;
        TextView tvCamera;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            headerView = itemView;
            tvCamera = itemView.findViewById(R.id.tvCamera);
            tvCamera.setText(R.string.nc_picture_take_picture);
            tvCamera.setTextColor(
                    ChatUIThemeManager.getColorFromAttrId(
                            itemView.getContext(), R.attr.nc_text_primary_color));
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPicture;
        TextView tvCheck;
        TextView tvDuration, tvIsGif, tvLongChart;
        View contentView;
        View btnCheck;

        public ViewHolder(View itemView) {
            super(itemView);
            contentView = itemView;
            ivPicture = itemView.findViewById(R.id.ivPicture);
            tvCheck = itemView.findViewById(R.id.tvCheck);
            btnCheck = itemView.findViewById(R.id.btnCheck);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvIsGif = itemView.findViewById(R.id.tv_isGif);
            tvLongChart = itemView.findViewById(R.id.tv_long_chart);
        }
    }

    public boolean isSelected(LocalMedia image) {
        int size = selectImages.size();
        for (int i = 0; i < size; i++) {
            LocalMedia media = selectImages.get(i);
            if (media == null || TextUtils.isEmpty(media.getPath())) {
                continue;
            }
            if (media.getPath().equals(image.getPath())) {
                return true;
            }
        }
        return false;
    }

    /** Update selection button */
    private void notifyCheckChanged(ViewHolder viewHolder, LocalMedia imageBean) {
        viewHolder.tvCheck.setText("");
        int size = selectImages.size();
        for (int i = 0; i < size; i++) {
            LocalMedia media = selectImages.get(i);
            if (media.getPath().equals(imageBean.getPath())) {
                imageBean.setNum(media.getNum());
                media.setPosition(imageBean.getPosition());
                viewHolder.tvCheck.setText(String.valueOf(imageBean.getNum()));
            }
        }
    }

    /**
     * Change image selection state
     *
     * @param contentHolder
     * @param image
     */
    @SuppressLint("StringFormatMatches")
    private void changeCheckboxState(ViewHolder contentHolder, LocalMedia image) {
        boolean isChecked = contentHolder.tvCheck.isSelected();
        int size = selectImages.size();
        if (size >= maxSelectNum && !isChecked) {
            ToastUtils.s(
                    context,
                    context.getString(R.string.nc_picture_message_max_num, config.maxSelectNum));
            return;
        }

        if (isChecked) {
            for (int i = 0; i < size; i++) {
                LocalMedia media = selectImages.get(i);
                if (media == null || TextUtils.isEmpty(media.getPath())) {
                    continue;
                }
                if (media.getPath().equals(image.getPath())) {
                    selectImages.remove(media);
                    subSelectPosition();
                    AnimUtils.disZoom(contentHolder.ivPicture, zoomAnim);
                    break;
                }
            }
        } else {
            // If single selection mode, clear selected items and refresh list
            if (selectMode == PictureConfig.SINGLE) {
                singleRadioMediaImage();
            }
            selectImages.add(image);
            image.setNum(selectImages.size());
            AnimUtils.zoom(contentHolder.ivPicture, zoomAnim);
        }
        // Notify that the clicked item has changed
        notifyItemChanged(contentHolder.getAdapterPosition());
        selectImage(contentHolder, !isChecked, true);
        if (imageSelectChangedListener != null) {
            imageSelectChangedListener.onChange(selectImages);
        }
    }

    /** Single selection mode */
    private void singleRadioMediaImage() {
        if (selectImages != null && selectImages.size() > 0) {
            LocalMedia media = selectImages.get(0);
            notifyItemChanged(media.position);
            selectImages.clear();
        }
    }

    /** Update selection order */
    private void subSelectPosition() {
        if (is_checked_num) {
            int size = selectImages.size();
            for (int index = 0, length = size; index < length; index++) {
                LocalMedia media = selectImages.get(index);
                media.setNum(index + 1);
                notifyItemChanged(media.position);
            }
        }
    }

    /**
     * Select image and execute animation
     *
     * @param holder
     * @param isChecked
     * @param isAnim
     */
    public void selectImage(ViewHolder holder, boolean isChecked, boolean isAnim) {
        holder.tvCheck.setSelected(isChecked);
        if (isChecked) {
            if (isAnim) {
                if (animation != null) {
                    holder.tvCheck.startAnimation(animation);
                }
            }
            holder.ivPicture.setColorFilter(
                    ContextCompat.getColor(context, R.color.picture_color_80),
                    PorterDuff.Mode.SRC_ATOP);
        } else {
            holder.ivPicture.setColorFilter(
                    ContextCompat.getColor(context, R.color.picture_color_20),
                    PorterDuff.Mode.SRC_ATOP);
        }
    }

    public interface OnPhotoSelectChangedListener {
        /** Camera capture callback */
        void onTakePhoto();

        /**
         * Selected media callback
         *
         * @param selectImages
         */
        void onChange(List<LocalMedia> selectImages);

        /**
         * Image preview callback
         *
         * @param media
         * @param position
         */
        void onPictureClick(LocalMedia media, int position);
    }

    public void setOnPhotoSelectChangedListener(
            OnPhotoSelectChangedListener imageSelectChangedListener) {
        this.imageSelectChangedListener = imageSelectChangedListener;
    }

    private boolean isGIFAboveMaxSize(LocalMedia media) {

        if (media == null) {
            return false;
        }
        String mimeType = media.getMimeType();
        if (!mimeType.toLowerCase().contains("gif")) {
            return false;
        }
        long limit = config != null ? config.gifSizeLimit : -1L;
        if (limit != -1 && media.getSize() > limit) {
            Toast.makeText(context, R.string.nc_gif_message_too_large, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }
}
