package ai.nexconn.chatui.picture;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.picture.adapter.ViewPagerAdapter;
import ai.nexconn.chatui.picture.anim.OptAnimationLoader;
import ai.nexconn.chatui.picture.broadcast.BroadcastAction;
import ai.nexconn.chatui.picture.broadcast.BroadcastManager;
import ai.nexconn.chatui.picture.config.PictureConfig;
import ai.nexconn.chatui.picture.config.PictureMimeType;
import ai.nexconn.chatui.picture.entity.LocalMedia;
import ai.nexconn.chatui.picture.observable.ImagesObservable;
import ai.nexconn.chatui.picture.tools.ScreenUtils;
import ai.nexconn.chatui.picture.tools.ToastUtils;
import ai.nexconn.chatui.utils.image.ImageViewUtils;
import ai.nexconn.chatui.utils.log.RLog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.view.View;
import android.view.animation.Animation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PicturePreviewActivity extends PictureBaseActivity
        implements View.OnClickListener, ViewPagerAdapter.OnCallBackActivity {
    protected ImageView picture_left_back;
    protected TextView mTvPictureOk;
    protected ViewPager2 viewPager;
    protected int position;
    protected boolean is_bottom_preview;
    protected List<LocalMedia> images = new ArrayList<>();
    protected List<LocalMedia> selectImages = new ArrayList<>();
    protected TextView check;
    protected ViewPagerAdapter adapter;
    protected Animation animation;
    protected View btnCheck;
    protected boolean refresh;
    protected int index;
    protected int screenWidth;
    protected Handler mHandler;
    protected FrameLayout selectBarLayout, topLayout;
    protected CheckBox mCbOriginal;
    private BroadcastReceiver commonBroadcastReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    switch (action) {
                        case BroadcastAction.ACTION_CLOSE_PREVIEW:
                            // Close preview after compression
                            dismissDialog();
                            mHandler.postDelayed(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            onBackPressed();
                                        }
                                    },
                                    150);
                            break;
                        default:
                            break;
                    }
                }
            };

    @Override
    public int getResourceId() {
        return R.layout.nc_picture_preview;
    }

    @Override
    protected void initWidgets() {
        super.initWidgets();
        mHandler = new Handler();
        screenWidth = ScreenUtils.getScreenWidth(this);
        animation = OptAnimationLoader.loadAnimation(this, R.anim.nc_picture_anim_modal_in);
        picture_left_back = findViewById(R.id.picture_left_back);
        ImageViewUtils.enableDrawableAutoMirror(picture_left_back);
        topLayout = findViewById(R.id.fl_top);
        viewPager = findViewById(R.id.preview_pager);
        btnCheck = findViewById(R.id.btnCheck);
        check = findViewById(R.id.check);
        picture_left_back.setOnClickListener(this);
        mTvPictureOk = findViewById(R.id.tv_ok);
        mCbOriginal = findViewById(R.id.cb_original);
        selectBarLayout = findViewById(R.id.select_bar_layout);
        topLayout.setOnClickListener(this);
        mTvPictureOk.setOnClickListener(this);
        selectBarLayout.setOnClickListener(this);
        position = getIntent().getIntExtra(PictureConfig.EXTRA_POSITION, 0);
        btnCheck.setOnClickListener(this);
        selectImages = getIntent().getParcelableArrayListExtra(PictureConfig.EXTRA_SELECT_LIST);
        is_bottom_preview = getIntent().getBooleanExtra(PictureConfig.EXTRA_BOTTOM_PREVIEW, false);
        mTvPictureOk.setBackgroundResource(R.drawable.nc_lively_send_primary_color_background);
        mTvPictureOk.setTextColor(
                ChatUIThemeManager.getAttrResId(
                        mTvPictureOk.getContext(), R.attr.nc_control_title_white_color));
        // Coming from bottom preview button
        images.clear();
        if (is_bottom_preview) {
            images.addAll(
                    getIntent()
                            .<LocalMedia>getParcelableArrayListExtra(
                                    PictureConfig.EXTRA_PREVIEW_SELECT_LIST));
        } else {
            images.addAll(ImagesObservable.getInstance().readPreviewMediaData());
        }
        if (images.isEmpty() || images.size() < position) {
            mCbOriginal.setVisibility(View.GONE);
            RLog.i("PicturePreviewActivity", "images is empty");
            return;
        }
        initViewPageAdapterData();
        // Original image
        LocalMedia media = images.get(position);
        boolean eqVideo = PictureMimeType.eqVideo(media.getMimeType());
        mCbOriginal.setVisibility(eqVideo ? View.GONE : View.VISIBLE);
        mCbOriginal.setChecked(config.isCheckOriginalImage);
        mCbOriginal.setText(
                config.isCheckOriginalImage
                        ? getString(
                                R.string.nc_picture_original_image_size, getSize(media.getSize()))
                        : getString(R.string.nc_picture_original_image));
        mCbOriginal.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            LocalMedia media = images.get(position);
                            if (isGIFAboveMaxSize(media)) {
                                mCbOriginal.setChecked(false);
                                return;
                            }
                        }
                        config.isCheckOriginalImage = isChecked;
                        mCbOriginal.setText(
                                isChecked
                                        ? getString(
                                                R.string.nc_picture_original_image_size,
                                                getSize(images.get(position).getSize()))
                                        : getString(R.string.nc_picture_original_image));
                    }
                });
    }

    /** Dynamically set album theme */
    @Override
    public void initPictureSelectorStyle() {
        mCbOriginal.setButtonDrawable(
                ContextCompat.getDrawable(
                        this,
                        ChatUIThemeManager.getAttrResId(
                                getContext(), R.attr.nc_media_file_state_check_img)));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        saveAndApplyStatusBar(Color.BLACK, false);
        BroadcastManager.getInstance(this)
                .registerReceiver(commonBroadcastReceiver, BroadcastAction.ACTION_CLOSE_PREVIEW);
    }

    @Override
    public void onResult(List<LocalMedia> images) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("selectImages", (ArrayList<? extends Parcelable>) images);
        BroadcastManager.getInstance(this)
                .action(BroadcastAction.ACTION_PREVIEW_COMPRESSION)
                .extras(bundle)
                .broadcast();
        onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImagesObservable.getInstance().clearPreviewMediaData();
        if (commonBroadcastReceiver != null) {
            BroadcastManager.getInstance(this)
                    .unregisterReceiver(
                            commonBroadcastReceiver, BroadcastAction.ACTION_CLOSE_PREVIEW);
            commonBroadcastReceiver = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (animation != null) {
            animation.cancel();
            animation = null;
        }
    }

    /** Initialize ViewPager data */
    private void initViewPageAdapterData() {
        // adapter = new PictureSimpleFragmentAdapter(config, images, this, this);
        adapter = new ViewPagerAdapter(config, images, this, this);
        viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        PicturePreviewActivity.this.position = position;
                        LocalMedia media = images.get(position);
                        index = media.getPosition();
                        if (config.checkNumMode) {
                            check.setText(MessageFormat.format("{0}", media.getNum()));
                            notifyCheckChanged(media);
                        }
                        onImageChecked(position);
                        boolean eqVideo = PictureMimeType.eqVideo(media.getMimeType());
                        mCbOriginal.setVisibility(eqVideo ? View.GONE : View.VISIBLE);
                        mCbOriginal.setChecked(config.isCheckOriginalImage);
                        mCbOriginal.setText(
                                config.isCheckOriginalImage
                                        ? getString(
                                                R.string.nc_picture_original_image_size,
                                                getSize(media.getSize()))
                                        : getString(R.string.nc_picture_original_image));
                    }
                });
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position, false);
        onSelectNumChange(false);
        onImageChecked(position);
        if (images.size() > 0) {
            LocalMedia media = images.get(position);
            index = media.getPosition();
            if (config.checkNumMode) {
                check.setText(media.getNum() + "");
                notifyCheckChanged(media);
            }
        }
    }

    private String getSize(long size) {
        if (size / 1024 / 1024 < 1) {
            return String.format("%dK", size / 1024);
        } else {
            return String.format("%.2fM", size / 1024f / 1024f);
        }
    }

    /** Update selection button */
    private void notifyCheckChanged(LocalMedia imageBean) {
        if (config.checkNumMode) {
            check.setText("");
            for (LocalMedia media : selectImages) {
                if (media.getPath().equals(imageBean.getPath())) {
                    imageBean.setNum(media.getNum());
                    check.setText(String.valueOf(imageBean.getNum()));
                }
            }
        }
    }

    /**
     * Check whether the current image is selected.
     *
     * @param position
     */
    public void onImageChecked(int position) {
        if (images != null && images.size() > 0 && images.size() > position) {
            LocalMedia media = images.get(position);
            check.setSelected(isSelected(media));
        } else {
            check.setSelected(false);
        }
    }

    /** Update selected image count */
    protected void onSelectNumChange(boolean isRefresh) {
        this.refresh = isRefresh;
        boolean enable = !selectImages.isEmpty();
        int textColor =
                enable
                        ? getResources().getColor(R.color.nc_main_theme)
                        : getResources().getColor(R.color.nc_main_theme_lucency);
        textColor =
                ChatUIThemeManager.getColorFromAttrId(
                        getContext(), R.attr.nc_control_title_white_color);
        mTvPictureOk.setBackgroundResource(
                enable
                        ? R.drawable.nc_lively_send_primary_color_background
                        : R.drawable.nc_lively_send_disable_color_background);
        mTvPictureOk.setTextColor(textColor);
        mTvPictureOk.setText(
                config.selectionMode == PictureConfig.SINGLE || !enable
                        ? getString(R.string.nc_picture_send)
                        : getString(R.string.nc_picture_send_num)
                                + "("
                                + selectImages.size()
                                + ")");
        if (enable) {
            mTvPictureOk.setEnabled(true);
            mTvPictureOk.setSelected(true);
        } else {
            mTvPictureOk.setEnabled(false);
            mTvPictureOk.setSelected(false);
        }
        updateSelector(refresh);
    }

    /**
     * Check whether the current image is selected.
     *
     * @param image
     * @return
     */
    public boolean isSelected(LocalMedia image) {
        for (LocalMedia media : selectImages) {
            if (media.getPath().equals(image.getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update image list selection effect.
     *
     * @param isRefresh
     */
    protected void updateSelector(boolean isRefresh) {
        if (isRefresh) {
            Bundle bundle = new Bundle();
            bundle.putInt("position", index);
            bundle.putParcelableArrayList(
                    "selectImages", (ArrayList<? extends Parcelable>) selectImages);
            BroadcastManager.getInstance(this)
                    .action(BroadcastAction.ACTION_SELECTED_DATA)
                    .extras(bundle)
                    .broadcast();
        }
    }

    /**
     * ViewPager page change callback.
     *
     * @param media
     */
    protected void onPageSelectedChange(LocalMedia media) {
        // do nothing
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.picture_left_back) {
            onBackPressed();
        } else if (id == R.id.tv_ok) {
            onComplete();
        } else if (id == R.id.btnCheck) {
            if (images.isEmpty() || images.size() <= position) {
                return;
            }
            LocalMedia currentMedia = images.get(position);
            if (isGIFAboveMaxSize(currentMedia)) {
                return;
            }
            if (PictureMimeType.eqVideo(currentMedia.getMimeType())) {
                int maxDuration = config.videoDurationLimit;
                if (maxDuration < 1) maxDuration = PictureConfig.DEFAULT_VIDEO_DURATION_LIMIT;
                if (TimeUnit.MILLISECONDS.toSeconds(currentMedia.getDuration()) > maxDuration) {
                    new AlertDialog.Builder(PicturePreviewActivity.this)
                            .setMessage(
                                    getResources()
                                            .getString(
                                                    R.string
                                                            .nc_picsel_selected_max_time_span_with_param,
                                                    Math.round(maxDuration / 60.0 * 10) / 10.0))
                            .setPositiveButton(R.string.nc_confirm, null)
                            .setCancelable(false)
                            .create()
                            .show();
                    return;
                }
            }
            if (PictureMimeType.isGif(currentMedia.getMimeType())) {
                long limit = config.gifSizeLimit;
                if (limit != -1 && limit < currentMedia.getSize()) {
                    new AlertDialog.Builder(PicturePreviewActivity.this)
                            .setMessage(getResources().getString(R.string.nc_send_large_gif_failed))
                            .setPositiveButton(R.string.nc_confirm, null)
                            .setCancelable(false)
                            .create()
                            .show();
                    return;
                }
            }
            onCheckedComplete();
        } else if (id == R.id.select_bar_layout || id == R.id.fl_top) {
            return;
        }
    }

    @Override
    public void onBackPressed() {
        closeActivity();
    }

    protected void onComplete() {
        // If minimum selection count is set, check whether it is satisfied
        int size = selectImages.size();
        LocalMedia image = selectImages.size() > 0 ? selectImages.get(0) : null;
        String mimeType = image != null ? image.getMimeType() : "";
        if (config.minSelectNum > 0) {
            if (size < config.minSelectNum && config.selectionMode == PictureConfig.MULTIPLE) {
                boolean eqImg = PictureMimeType.eqImage(mimeType);
                String str =
                        eqImg
                                ? getString(R.string.nc_picture_min_img_num, config.minSelectNum)
                                : getString(R.string.nc_picture_min_video_num, config.minSelectNum);
                ToastUtils.s(getContext(), str);
                return;
            }
        }
        onResult(selectImages);
    }

    protected void onCheckedComplete() {
        if (images != null && images.size() > 0) {
            LocalMedia image = images.get(viewPager.getCurrentItem());
            // Refresh image status in the image list
            boolean isChecked;
            if (!check.isSelected()) {
                isChecked = true;
                check.setSelected(true);
                check.startAnimation(animation);
            } else {
                isChecked = false;
                check.setSelected(false);
            }
            if (selectImages.size() >= config.maxSelectNum && isChecked) {
                ToastUtils.s(
                        getContext(),
                        getString(R.string.nc_picture_message_max_num, config.maxSelectNum));
                check.setSelected(false);
                return;
            }
            if (isChecked) {
                // If single selection mode, clear selected items and refresh list
                if (config.selectionMode == PictureConfig.SINGLE) {
                    singleRadioMediaImage();
                }
                selectImages.add(image);
                onSelectedChange(true, image);
                image.setNum(selectImages.size());
                if (config.checkNumMode) {
                    check.setText(String.valueOf(image.getNum()));
                }
            } else {
                for (LocalMedia media : selectImages) {
                    if (media.getPath().equals(image.getPath())) {
                        selectImages.remove(media);
                        onSelectedChange(false, image);
                        subSelectPosition();
                        notifyCheckChanged(media);
                        break;
                    }
                }
            }
            onSelectNumChange(true);
        }
    }

    /** Single select image */
    private void singleRadioMediaImage() {
        LocalMedia media =
                selectImages != null && selectImages.size() > 0 ? selectImages.get(0) : null;
        if (media != null) {
            Bundle bundle = new Bundle();
            bundle.putInt("position", media.getPosition());
            bundle.putParcelableArrayList(
                    "selectImages", (ArrayList<? extends Parcelable>) selectImages);
            BroadcastManager.getInstance(this)
                    .action(BroadcastAction.ACTION_SELECTED_DATA)
                    .extras(bundle)
                    .broadcast();
            selectImages.clear();
        }
    }

    /**
     * Select or remove.
     *
     * @param isAddRemove
     * @param media
     */
    protected void onSelectedChange(boolean isAddRemove, LocalMedia media) {
        // do nothing
    }

    /** Update selection order */
    private void subSelectPosition() {
        for (int index = 0, len = selectImages.size(); index < len; index++) {
            LocalMedia media = selectImages.get(index);
            media.setNum(index + 1);
        }
    }

    @Override
    public void onActivityBackPressed() {
        onBackPressed();
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
            Toast.makeText(this, R.string.nc_gif_message_too_large, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }
}
