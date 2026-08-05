package ai.nexconn.chatui.usermanage.group.application;

import ai.nexconn.chat.channel.model.GroupApplicationDirection;
import ai.nexconn.chat.channel.model.GroupApplicationInfo;
import ai.nexconn.chat.channel.model.GroupApplicationStatus;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.base.interfaces.OnActionClickListener;
import ai.nexconn.chatui.picture.tools.ScreenUtils;
import ai.nexconn.chatui.usermanage.adapter.GroupApplicationsAdapter;
import ai.nexconn.chatui.usermanage.component.CommonListComponent;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.view.RTLUtils;
import ai.nexconn.chatui.widget.component.HeadComponent;
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
 * Fragment for group applications.
 *
 * @since 5.12.2
 */
public class GroupApplicationsFragment extends BaseViewModelFragment<GroupApplicationsViewModel> {

    protected HeadComponent headComponent;
    protected CommonListComponent commonListComponent;
    protected GroupApplicationsAdapter groupApplicationsAdapter;
    private TextView emptyView;

    @NonNull
    @Override
    protected GroupApplicationsViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupApplicationsViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_group_applications, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        commonListComponent = view.findViewById(R.id.nc_common_list_component);
        groupApplicationsAdapter = new GroupApplicationsAdapter();
        commonListComponent.setAdapter(groupApplicationsAdapter);
        emptyView = view.findViewById(R.id.nc_empty_tv);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupApplicationsViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        headComponent.setRightClickListener(this::onOptionsMenuClick);

        commonListComponent.setOnPageDataLoader(viewModel.getOnPageDataLoader());
        // Observe contact list changes in ViewModel
        viewModel
                .getGroupApplicationInfoListLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        groupApplicationInfos -> {
                            if (groupApplicationInfos != null && !groupApplicationInfos.isEmpty()) {
                                emptyView.setVisibility(View.GONE);
                                commonListComponent.setVisibility(View.VISIBLE);
                                groupApplicationsAdapter.setData(groupApplicationInfos);
                            } else {
                                emptyView.setVisibility(View.VISIBLE);
                                commonListComponent.setVisibility(View.GONE);
                            }
                        });

        groupApplicationsAdapter.setOnAcceptClickListener(
                new OnActionClickListener<GroupApplicationInfo>() {
                    @Override
                    public void onActionClick(GroupApplicationInfo groupApplicationInfo) {}

                    @Override
                    public <E> void onActionClickWithConfirm(
                            GroupApplicationInfo groupApplicationInfo,
                            OnConfirmClickListener<E> listener) {
                        OnActionClickListener.super.onActionClickWithConfirm(
                                groupApplicationInfo, listener);
                        if (listener != null) {
                            onApplicationAccept(
                                    groupApplicationInfo,
                                    (OnConfirmClickListener<Integer>) listener);
                        }
                    }
                });

        groupApplicationsAdapter.setOnRejectClickListener(
                new OnActionClickListener<GroupApplicationInfo>() {
                    @Override
                    public void onActionClick(GroupApplicationInfo groupApplicationInfo) {}

                    @Override
                    public <E> void onActionClickWithConfirm(
                            GroupApplicationInfo groupApplicationInfo,
                            OnActionClickListener.OnConfirmClickListener<E> listener) {
                        OnActionClickListener.super.onActionClickWithConfirm(
                                groupApplicationInfo, listener);
                        if (listener != null) {
                            onApplicationReject(
                                    groupApplicationInfo,
                                    (OnConfirmClickListener<Boolean>) listener);
                        }
                    }
                });
    }

    /**
     * Accept an application.
     *
     * @param groupApplicationInfo group application info
     * @param listener confirm click listener
     */
    protected void onApplicationAccept(
            GroupApplicationInfo groupApplicationInfo,
            @NonNull OnActionClickListener.OnConfirmClickListener<Integer> listener) {
        GroupApplicationDirection direction = groupApplicationInfo.getDirection();
        GroupApplicationStatus status = groupApplicationInfo.getStatus();
        if (status == GroupApplicationStatus.INVITEE_UNHANDLED
                || status == GroupApplicationStatus.ADMIN_UNHANDLED) {
            if (direction == GroupApplicationDirection.INVITATION_RECEIVED) {
                getViewModel()
                        .acceptGroupInvite(
                                groupApplicationInfo.getGroupId(),
                                groupApplicationInfo.getInviterInfo().getUserId(),
                                isSuccess -> {
                                    if (!isSuccess) {
                                        ToastUtils.show(
                                                getContext(),
                                                getString(R.string.nc_invite_confirm_failed),
                                                Toast.LENGTH_SHORT);
                                    }
                                    listener.onActionClick(0);
                                });
            } else if (direction == GroupApplicationDirection.APPLICATION_RECEIVED) {
                getViewModel()
                        .acceptGroupApplication(
                                groupApplicationInfo.getGroupId(),
                                groupApplicationInfo.getInviterInfo().getUserId(),
                                groupApplicationInfo.getJoinMemberInfo().getUserId(),
                                resultCode -> {
                                    int code = resultCode != null ? resultCode : 0;
                                    boolean isSuccess = code == 0 || code == 25427;
                                    if (!isSuccess) {
                                        ToastUtils.show(
                                                getContext(),
                                                getString(R.string.nc_invite_confirm_failed),
                                                Toast.LENGTH_SHORT);
                                    }
                                    if (isSuccess) {
                                        listener.onActionClick(code);
                                    }
                                });
            }
        }
    }

    /**
     * Reject an application.
     *
     * @param groupApplicationInfo group application info
     * @param listener confirm click listener
     */
    protected void onApplicationReject(
            GroupApplicationInfo groupApplicationInfo,
            @NonNull OnActionClickListener.OnConfirmClickListener<Boolean> listener) {
        GroupApplicationDirection direction = groupApplicationInfo.getDirection();
        GroupApplicationStatus status = groupApplicationInfo.getStatus();
        if (status == GroupApplicationStatus.INVITEE_UNHANDLED
                || status == GroupApplicationStatus.ADMIN_UNHANDLED) {
            if (direction == GroupApplicationDirection.INVITATION_RECEIVED) {
                getViewModel()
                        .refuseGroupInvite(
                                groupApplicationInfo.getGroupId(),
                                groupApplicationInfo.getInviterInfo().getUserId(),
                                groupApplicationInfo.getReason(),
                                isSuccess -> {
                                    if (!isSuccess) {
                                        ToastUtils.show(
                                                getContext(),
                                                getString(R.string.nc_invite_reject_failed),
                                                Toast.LENGTH_SHORT);
                                    }
                                    listener.onActionClick(isSuccess);
                                });
            } else if (direction == GroupApplicationDirection.APPLICATION_RECEIVED) {
                getViewModel()
                        .refuseGroupApplication(
                                groupApplicationInfo.getGroupId(),
                                groupApplicationInfo.getInviterInfo().getUserId(),
                                groupApplicationInfo.getJoinMemberInfo().getUserId(),
                                groupApplicationInfo.getReason(),
                                isSuccess -> {
                                    if (!isSuccess) {
                                        ToastUtils.show(
                                                getContext(),
                                                getString(R.string.nc_invite_reject_failed),
                                                Toast.LENGTH_SHORT);
                                    }
                                    listener.onActionClick(isSuccess);
                                });
            }
        }
    }

    /**
     * Handle options menu click.
     *
     * @param anchor anchor View
     */
    protected void onOptionsMenuClick(View anchor) {
        // Load custom layout
        View rootView =
                LayoutInflater.from(anchor.getContext())
                        .inflate(R.layout.nc_pop_group_applications_category, null);

        // Configure PopupWindow
        final PopupWindow popupWindow =
                new PopupWindow(
                        rootView,
                        ScreenUtils.dip2px(anchor.getContext(), 136),
                        WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new ColorDrawable());
        popupWindow.setOutsideTouchable(true); // Required to enable dismiss on outside touch
        popupWindow.setFocusable(true);

        // Initialize menu items
        TextView menuAllRequests = rootView.findViewById(R.id.menu_item_all_requests);
        TextView menuPendingConfirmation =
                rootView.findViewById(R.id.menu_item_pending_confirmation);
        TextView menuProcessedRequests = rootView.findViewById(R.id.menu_item_processed_requests);
        TextView menuExpiredRequests = rootView.findViewById(R.id.menu_item_expired_requests);
        // All requests
        menuAllRequests.setOnClickListener(
                v -> {
                    headComponent.setTitleText(R.string.nc_all_requests);
                    popupWindow.dismiss();
                    GroupApplicationDirection[] directions =
                            new GroupApplicationDirection[] {
                                GroupApplicationDirection.APPLICATION_SENT,
                                GroupApplicationDirection.INVITATION_SENT,
                                GroupApplicationDirection.APPLICATION_RECEIVED,
                                GroupApplicationDirection.INVITATION_RECEIVED
                            };
                    GroupApplicationStatus[] status =
                            new GroupApplicationStatus[] {
                                GroupApplicationStatus.ADMIN_UNHANDLED,
                                GroupApplicationStatus.ADMIN_REFUSED,
                                GroupApplicationStatus.JOINED,
                                GroupApplicationStatus.EXPIRED,
                                GroupApplicationStatus.INVITEE_REFUSED,
                                GroupApplicationStatus.INVITEE_UNHANDLED
                            };
                    getViewModel().getGroupApplications(directions, status);
                });
        // Pending confirmation
        menuPendingConfirmation.setOnClickListener(
                v -> {
                    headComponent.setTitleText(R.string.nc_pending_confirmation);
                    popupWindow.dismiss();
                    GroupApplicationDirection[] directions =
                            new GroupApplicationDirection[] {
                                GroupApplicationDirection.APPLICATION_SENT,
                                GroupApplicationDirection.INVITATION_SENT,
                                GroupApplicationDirection.APPLICATION_RECEIVED,
                                GroupApplicationDirection.INVITATION_RECEIVED
                            };
                    GroupApplicationStatus[] status =
                            new GroupApplicationStatus[] {
                                GroupApplicationStatus.ADMIN_UNHANDLED,
                                GroupApplicationStatus.INVITEE_UNHANDLED
                            };
                    getViewModel().getGroupApplications(directions, status);
                });
        // Processed
        menuProcessedRequests.setOnClickListener(
                v -> {
                    headComponent.setTitleText(R.string.nc_completed);
                    popupWindow.dismiss();
                    GroupApplicationDirection[] directions =
                            new GroupApplicationDirection[] {
                                GroupApplicationDirection.APPLICATION_SENT,
                                GroupApplicationDirection.INVITATION_SENT,
                                GroupApplicationDirection.APPLICATION_RECEIVED,
                                GroupApplicationDirection.INVITATION_RECEIVED
                            };
                    GroupApplicationStatus[] status =
                            new GroupApplicationStatus[] {
                                GroupApplicationStatus.JOINED,
                                GroupApplicationStatus.ADMIN_REFUSED,
                                GroupApplicationStatus.INVITEE_REFUSED,
                            };
                    getViewModel().getGroupApplications(directions, status);
                });
        menuExpiredRequests.setOnClickListener(
                v -> {
                    headComponent.setTitleText(R.string.nc_expired);
                    popupWindow.dismiss();
                    GroupApplicationDirection[] directions =
                            new GroupApplicationDirection[] {
                                GroupApplicationDirection.APPLICATION_SENT,
                                GroupApplicationDirection.INVITATION_SENT,
                                GroupApplicationDirection.APPLICATION_RECEIVED,
                                GroupApplicationDirection.INVITATION_RECEIVED
                            };
                    GroupApplicationStatus[] status =
                            new GroupApplicationStatus[] {GroupApplicationStatus.EXPIRED};
                    getViewModel().getGroupApplications(directions, status);
                });

        // Set PopupWindow show and dismiss listener
        popupWindow.setOnDismissListener(() -> setWindowAlpha(anchor.getContext(), 1.0f));

        // Calculate x offset based on layout direction to position PopupWindow
        int screenWidth = ScreenUtils.getScreenWidth(anchor.getContext());
        int popupWidth = ScreenUtils.dip2px(anchor.getContext(), 136);
        int margin = ScreenUtils.dip2px(anchor.getContext(), 16);
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int anchorX = location[0];
        int xOffset;

        // Check for RTL mode
        if (RTLUtils.isRtl(anchor.getContext())) {
            // RTL mode: show from left side, 16dp from screen left
            xOffset = -(anchorX - margin);
        } else {
            // LTR mode: show from right side, 16dp from screen right
            xOffset = screenWidth - anchorX - popupWidth - margin;
        }

        popupWindow.showAsDropDown(anchor, xOffset, 0);
        setWindowAlpha(anchor.getContext(), 0.5f);
    }

    private void setWindowAlpha(Context context, float alpha) {
        Window window = ((Activity) context).getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.alpha = alpha;
        window.setAttributes(layoutParams);
    }
}
