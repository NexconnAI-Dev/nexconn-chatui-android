package ai.nexconn.chatui.picture.widget;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.picture.adapter.PictureAlbumDirectoryAdapter;
import ai.nexconn.chatui.picture.config.PictureSelectionConfig;
import ai.nexconn.chatui.picture.entity.LocalMedia;
import ai.nexconn.chatui.picture.entity.LocalMediaFolder;
import ai.nexconn.chatui.picture.tools.AnimUtils;
import ai.nexconn.chatui.picture.tools.ScreenUtils;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.widget.FixedLinearLayoutManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FolderPopWindow extends PopupWindow {
    private static final String TAG = FolderPopWindow.class.getSimpleName();
    private Context context;
    private View window;
    private RecyclerView recyclerView;
    private PictureAlbumDirectoryAdapter adapter;
    private boolean isDismiss = false;
    private ImageView ivArrowView;
    private Drawable drawableUp, drawableDown;
    private int chooseMode;
    private PictureSelectionConfig config;
    private int maxHeight;
    private View rootViewBg;

    public FolderPopWindow(Context context, PictureSelectionConfig config) {
        this.context = context;
        this.config = config;
        this.chooseMode = config.chooseMode;
        this.window = LayoutInflater.from(context).inflate(R.layout.nc_picture_window_folder, null);
        this.setContentView(window);
        this.setWidth(RelativeLayout.LayoutParams.MATCH_PARENT);
        this.setHeight(RelativeLayout.LayoutParams.WRAP_CONTENT);
        this.setAnimationStyle(R.style.PictureThemeWindowStyle);
        this.setFocusable(true);
        this.setOutsideTouchable(true);
        this.update();
        this.drawableUp = ContextCompat.getDrawable(context, R.drawable.nc_picture_icon_wechat_up);
        this.drawableDown =
                ContextCompat.getDrawable(context, R.drawable.nc_picture_icon_wechat_down);
        this.maxHeight = (int) (ScreenUtils.getScreenHeight(context) * 0.6);
        initView();
    }

    public void initView() {
        rootViewBg = window.findViewById(R.id.rootViewBg);
        adapter = new PictureAlbumDirectoryAdapter(config);
        recyclerView = window.findViewById(R.id.folder_list);
        recyclerView.setLayoutManager(new FixedLinearLayoutManager(context));
        recyclerView.setAdapter(adapter);
        rootViewBg.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                });
    }

    public void bindFolder(List<LocalMediaFolder> folders) {
        adapter.bindFolderData(folders);
    }

    public void setArrowImageView(ImageView ivArrowView) {
        this.ivArrowView = ivArrowView;
    }

    @Override
    public void showAsDropDown(View anchor) {
        try {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N) {
                int[] location = new int[2];
                anchor.getLocationInWindow(location);
                showAtLocation(anchor, Gravity.NO_GRAVITY, 0, location[1] + anchor.getHeight());
            } else {
                super.showAsDropDown(anchor);
            }
            isDismiss = false;
            ivArrowView.setImageDrawable(drawableUp);
            AnimUtils.rotateArrow(ivArrowView, true);
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
        }
    }

    public void setOnItemClickListener(
            PictureAlbumDirectoryAdapter.OnItemClickListener onItemClickListener) {
        adapter.setOnItemClickListener(onItemClickListener);
    }

    @Override
    public void dismiss() {
        if (isDismiss) {
            return;
        }
        ivArrowView.setImageDrawable(drawableDown);
        AnimUtils.rotateArrow(ivArrowView, false);
        isDismiss = true;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            dismiss4Pop();
            isDismiss = false;
        } else {
            FolderPopWindow.super.dismiss();
            isDismiss = false;
        }
    }

    /** Dismiss PopWindow on Android 4.1.1 and 4.1.2 */
    private void dismiss4Pop() {
        new Handler()
                .post(
                        new Runnable() {
                            @Override
                            public void run() {
                                FolderPopWindow.super.dismiss();
                            }
                        });
    }

    /** Update checked status */
    public void notifyDataCheckedStatus(List<LocalMedia> medias) {
        try {
            // Get selected images
            List<LocalMediaFolder> folders = adapter.getFolderData();
            for (LocalMediaFolder folder : folders) {
                folder.setCheckedNum(0);
            }
            if (medias.size() > 0) {
                for (LocalMediaFolder folder : folders) {
                    int num = 0; // Count of selected images in the current album
                    List<LocalMedia> images = folder.getImages();
                    for (LocalMedia media : images) {
                        String path = media.getPath();
                        for (LocalMedia m : medias) {
                            if (path.equals(m.getPath())) {
                                num++;
                                folder.setCheckedNum(num);
                            }
                        }
                    }
                }
            }
            adapter.bindFolderData(folders);
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
        }
    }
}
