package ai.nexconn.chatui.usermanage.friend.apply;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.base.adapter.ViewHolder;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.model.UiFriendApplicationInfo;
import ai.nexconn.chatui.picture.tools.ScreenUtils;
import ai.nexconn.chatui.usermanage.adapter.ApplyFriendAdapter;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.view.RTLUtils;
import ai.nexconn.chatui.widget.CommonDialog;
import ai.nexconn.chatui.widget.component.HeadComponent;
import ai.nexconn.chatui.widget.component.ListComponent;
import ai.nexconn.chatui.widget.pullrefresh.api.RefreshLayout;
import ai.nexconn.chatui.widget.pullrefresh.listener.OnLoadMoreListener;
import ai.nexconn.chatui.widget.pullrefresh.listener.OnRefreshListener;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

/**
 * Friend application list page
 *
 * @since 5.12.0
 */
public class ApplyFriendListFragment extends BaseViewModelFragment<ApplyFriendViewModel> {

    protected View rootView;
    protected HeadComponent headComponent;
    protected ListComponent listComponent;
    protected PopupWindow popupWindow;
    private TextView emptyView;

    protected ApplyFriendAdapter applyFriendAdapter = new ApplyFriendAdapter();

    protected int status = 0;
    private OnRefreshListener onRefreshListener =
            new OnRefreshListener() {
                @Override
                public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                    status = 1;
                    getViewModel().loadFriendApplications(false);
                }
            };
    protected OnLoadMoreListener onLoadMoreListener =
            new OnLoadMoreListener() {
                @Override
                public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                    status = 2;
                    getViewModel().loadFriendApplications(true);
                }
            };

    @NonNull
    @Override
    protected ApplyFriendViewModel onCreateViewModel(Bundle bundle) {
        ApplyFriendViewModel applyFriendViewModel =
                new ViewModelProvider(this, new ViewModelFactory(bundle))
                        .get(ApplyFriendViewModel.class);
        return applyFriendViewModel;
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        rootView = inflater.inflate(R.layout.nc_page_friend_list_apply, container, false);
        headComponent = rootView.findViewById(R.id.tb_bar);
        headComponent.setRightTextDrawable(
                ChatUIThemeManager.getAttrResId(context, R.attr.nc_navigation_bar_btn_more_img));

        listComponent = rootView.findViewById(R.id.nc_list_component);
        listComponent.setOnRefreshListener(onRefreshListener);
        listComponent.setOnLoadMoreListener(onLoadMoreListener);
        emptyView = rootView.findViewById(R.id.nc_empty_tv);

        listComponent.setAdapter(applyFriendAdapter);
        return rootView;
    }

    /**
     * Called after the view is created
     *
     * @param viewModel VM
     */
    @Override
    protected void onViewReady(@NonNull ApplyFriendViewModel viewModel) {
        headComponent.setRightClickListener(v -> showPopupWindow(v));

        applyFriendAdapter.setOnBtnClickListener(
                new ApplyFriendAdapter.OnBtnClickListener() {
                    public void onAcceptClick(
                            ViewHolder holder, UiFriendApplicationInfo item, int position) {
                        onFriendApplyAcceptClick(item);
                    }

                    @Override
                    public void onRejectClick(
                            ViewHolder holder, UiFriendApplicationInfo item, int position) {
                        onFriendApplyRejectClick(item);
                    }
                });

        viewModel
                .getFriendApplicationsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        data -> {
                            // Update UI
                            if (listComponent != null) {
                                if (status == 1) {
                                    listComponent.finishRefresh();
                                } else if (status == 2) {
                                    listComponent.finishLoadMore();
                                }
                            }
                            status = 0;
                            boolean hasData = data != null && !data.isEmpty();
                            emptyView.setVisibility(hasData ? View.GONE : View.VISIBLE);
                            listComponent.setVisibility(hasData ? View.VISIBLE : View.GONE);
                            applyFriendAdapter.setData(data);
                        });
        viewModel.loadFriendApplications(false);
    }

    /**
     * Handle accept click on friend application list item
     *
     * @param uiFriendApplicationInfo friend application info
     */
    protected void onFriendApplyAcceptClick(UiFriendApplicationInfo uiFriendApplicationInfo) {
        getViewModel()
                .acceptFriendApplication(
                        uiFriendApplicationInfo.getInfo().getUserId(),
                        aBoolean -> {
                            if (aBoolean) {
                                ToastUtils.show(
                                        getContext(),
                                        getString(R.string.nc_send_apply_success),
                                        Toast.LENGTH_SHORT);
                                getViewModel().loadFriendApplications(false);
                            }
                        });
    }

    /**
     * Handle reject click on friend application list item
     *
     * @param uiFriendApplicationInfo friend application info
     */
    protected void onFriendApplyRejectClick(UiFriendApplicationInfo uiFriendApplicationInfo) {
        showDialog(uiFriendApplicationInfo.getInfo().getUserId());
    }

    /**
     * Show popup window
     *
     * @param anchor anchor view
     */
    protected void showPopupWindow(View anchor) {
        if (popupWindow == null) {
            View view =
                    LayoutInflater.from(anchor.getContext())
                            .inflate(R.layout.nc_pop_apply_list, null);
            TextView allView = view.findViewById(R.id.tv_all);
            TextView receivedView = view.findViewById(R.id.tv_received_apply);
            TextView sendView = view.findViewById(R.id.tv_send_apply);
            popupWindow =
                    new PopupWindow(
                            view,
                            ScreenUtils.dip2px(anchor.getContext(), 136),
                            WindowManager.LayoutParams.WRAP_CONTENT);
            popupWindow.setBackgroundDrawable(
                    new ColorDrawable()); // Required for outside touch events to work
            popupWindow.setOutsideTouchable(true);
            popupWindow.setFocusable(true);
            allView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getViewModel().loadFriendApplications(0);
                            popupWindow.dismiss();
                        }
                    });
            receivedView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getViewModel().loadFriendApplications(1);
                            popupWindow.dismiss();
                        }
                    });
            sendView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getViewModel().loadFriendApplications(2);
                            popupWindow.dismiss();
                        }
                    });
        }
        if (popupWindow.isShowing()) {
            popupWindow.dismiss();
            return;
        }
        // Set PopupWindow show/dismiss listener
        popupWindow.setOnDismissListener(() -> setWindowAlpha(anchor.getContext(), 1.0f));

        // Calculate x offset based on layout direction for PopupWindow positioning
        int screenWidth = ScreenUtils.getScreenWidth(anchor.getContext());
        int popupWidth = ScreenUtils.dip2px(anchor.getContext(), 136);
        int margin = ScreenUtils.dip2px(anchor.getContext(), 16);
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int anchorX = location[0];
        int xOffset;

        // Check if RTL mode
        if (RTLUtils.isRtl(anchor.getContext())) {
            // RTL mode: show from left side, 16dp from left edge
            xOffset = -(anchorX - margin);
        } else {
            // LTR mode: show from right side, 16dp from right edge
            xOffset = screenWidth - anchorX - popupWidth - margin;
        }

        popupWindow.showAsDropDown(anchor, xOffset, 0);

        setWindowAlpha(anchor.getContext(), 0.5f);
    }

    protected void showDialog(String userId) {
        // Show reject friend request confirmation dialog
        CommonDialog dialog =
                new CommonDialog.Builder()
                        .setContentMessage(getString(R.string.nc_reject_request))
                        .setDialogButtonClickListener(
                                new CommonDialog.OnDialogButtonClickListener() {
                                    @Override
                                    public void onPositiveClick(View v, Bundle bundle) {
                                        getViewModel()
                                                .refuseFriendApplication(
                                                        userId,
                                                        new OnDataChangeListener<Boolean>() {
                                                            @Override
                                                            public void onDataChange(
                                                                    Boolean aBoolean) {
                                                                if (aBoolean) {
                                                                    ToastUtils.show(
                                                                            getContext(),
                                                                            getString(
                                                                                    R.string
                                                                                            .nc_reject_success),
                                                                            Toast.LENGTH_SHORT);
                                                                    getViewModel()
                                                                            .loadFriendApplications(
                                                                                    false);
                                                                }
                                                            }
                                                        });
                                    }

                                    @Override
                                    public void onNegativeClick(View v, Bundle bundle) {}
                                })
                        .build();
        dialog.show(getParentFragmentManager(), null);
    }

    private void setWindowAlpha(Context context, float alpha) {
        Window window = ((Activity) context).getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.alpha = alpha;
        window.setAttributes(layoutParams);
    }
}
