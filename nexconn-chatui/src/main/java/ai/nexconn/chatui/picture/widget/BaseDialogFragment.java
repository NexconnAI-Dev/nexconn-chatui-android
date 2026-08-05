package ai.nexconn.chatui.picture.widget;

import ai.nexconn.chatui.picture.tools.ScreenUtils;
import ai.nexconn.chatui.utils.log.RLog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

/** Created by lhz on 2020/11/30 */
public abstract class BaseDialogFragment extends DialogFragment {
    private static final String TAG = BaseDialogFragment.class.getSimpleName();
    protected View mRootView;
    protected Dialog mDialog;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(
                DialogFragment.STYLE_NO_TITLE,
                androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDialog = getDialog();
        if (mDialog != null) {
            Window dialogWindow = mDialog.getWindow();
            dialogWindow.setBackgroundDrawableResource(getBackgroundDrawableRes());
            DisplayMetrics dm = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            dialogWindow.setLayout(
                    (int) (dm.widthPixels * getScreenWidthProportion()),
                    getScreenHeightProportion());
            // Set vertical offset
            WindowManager.LayoutParams attributes = dialogWindow.getAttributes();
            attributes.gravity = getGravity();
            attributes.x = -ScreenUtils.dip2px(getContext(), getHorizontalMovement());
            attributes.y = ScreenUtils.dip2px(getContext(), getVerticalMovement());
            dialogWindow.setAttributes(attributes);
            dialogWindow.setBackgroundDrawableResource(android.R.color.white);
        }
    }

    protected int getGravity() {
        return Gravity.CENTER;
    }

    /**
     * @return the height proportion of the screen
     */
    protected int getScreenHeightProportion() {
        return ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    protected @DrawableRes int getBackgroundDrawableRes() {
        return android.R.color.transparent;
    }

    /**
     * Screen proportion
     *
     * @return the width proportion of the screen
     */
    protected float getScreenWidthProportion() {
        return 0.9f;
    }

    /**
     * Set vertical offset
     *
     * @return negative values offset upward, positive values offset downward, in dp. For example,
     *     to offset 20dp upward, return -20.
     */
    protected float getVerticalMovement() {
        return 0;
    }

    /**
     * Set horizontal offset
     *
     * @return negative values offset left, positive values offset right, in dp. For example, to
     *     offset 20dp left, return -20.
     */
    protected float getHorizontalMovement() {
        return 0;
    }

    public void show(FragmentManager manager) {
        try {
            show(manager, "");
        } catch (IllegalStateException e) {
            RLog.e(TAG, e.getMessage());
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(getContentView(), container, false);
        findView();
        initView();
        bindData();
        return mRootView;
    }

    protected abstract void findView();

    protected abstract void initView();

    public abstract void bindData();

    protected abstract @LayoutRes int getContentView();
}
