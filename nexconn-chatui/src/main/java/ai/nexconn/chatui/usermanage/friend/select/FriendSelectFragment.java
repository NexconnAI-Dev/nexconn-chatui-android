package ai.nexconn.chatui.usermanage.friend.select;

import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.base.interfaces.OnActionClickListener;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.usermanage.component.ContactListComponent;
import ai.nexconn.chatui.usermanage.group.create.GroupCreateActivity;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import ai.nexconn.chatui.widget.component.HeadComponent;
import ai.nexconn.chatui.widget.component.SearchComponent;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;
import java.util.List;

/**
 * Friend selection page
 *
 * @since 5.12.0
 */
public class FriendSelectFragment extends BaseViewModelFragment<FriendSelectViewModel> {

    protected HeadComponent headComponent;
    protected SearchComponent searchComponent;
    protected ContactListComponent contactListComponent;
    private int maxCount;
    private TextView tvEmptyContacts;

    @NonNull
    @Override
    protected FriendSelectViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(FriendSelectViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_friend_select, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        searchComponent = view.findViewById(R.id.nc_search_component);
        contactListComponent = view.findViewById(R.id.nc_contact_list_component);
        tvEmptyContacts = view.findViewById(R.id.tv_empty_contacts);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull FriendSelectViewModel viewModel) {
        maxCount =
                Math.max(
                        1,
                        Math.min(
                                100,
                                getArguments()
                                        .getInt(ChatUIConstants.KEY_MAX_FRIEND_SELECT_COUNT, 30)));
        headComponent.setLeftClickListener(v -> finishActivity());
        headComponent.setRightClickListener(
                v -> {
                    List<ContactModel> value = viewModel.getSelectedContactsLiveData().getValue();
                    if (value == null || value.isEmpty()) {
                        return;
                    }
                    List<String> inviteeUserIds = new ArrayList<>();
                    for (int i = 0; i < value.size(); i++) {
                        Object bean = value.get(i).getBean();
                        if (bean instanceof FriendDetail) {
                            inviteeUserIds.add(((FriendDetail) bean).getUserId());
                        }
                    }
                    startActivity(GroupCreateActivity.newIntent(getContext(), inviteeUserIds));
                });
        headComponent.setRightTextViewEnable(false);
        viewModel
                .getSelectedContactsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels ->
                                headComponent.setRightTextViewEnable(
                                        contactModels != null && !contactModels.isEmpty()));

        searchComponent.setSearchQueryListener(viewModel::queryContacts);

        // Observe contact list changes from ViewModel
        viewModel
                .getFilteredContactsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null && !contactModels.isEmpty()) {
                                tvEmptyContacts.setVisibility(View.GONE);
                                contactListComponent.setVisibility(View.VISIBLE);
                                contactListComponent.post(
                                        () -> contactListComponent.setContactList(contactModels));
                            } else {
                                tvEmptyContacts.setVisibility(View.VISIBLE);
                                contactListComponent.setVisibility(View.GONE);
                            }
                        });

        // Set contact list item click listener
        contactListComponent.setOnItemClickListener(
                new OnActionClickListener<ContactModel>() {
                    @Override
                    public void onActionClick(ContactModel contactModel) {}

                    @Override
                    public <E> void onActionClickWithConfirm(
                            ContactModel contactModel, OnConfirmClickListener<E> listener) {
                        OnActionClickListener.super.onActionClickWithConfirm(
                                contactModel, listener);
                        if (listener != null) {
                            handleContactSelection(
                                    viewModel,
                                    contactModel,
                                    (OnConfirmClickListener<Boolean>) listener);
                        }
                    }
                });
    }

    /**
     * Handle contact selection
     *
     * @param viewModel friend selection ViewModel
     * @param contactModel contact info
     * @param listener confirm click listener
     */
    private void handleContactSelection(
            @NonNull FriendSelectViewModel viewModel,
            ContactModel contactModel,
            OnActionClickListener.OnConfirmClickListener<Boolean> listener) {
        List<ContactModel> contactModelList = viewModel.getSelectedContactsLiveData().getValue();
        ContactModel.CheckType newCheckType = contactModel.getCheckType();
        if (newCheckType == ContactModel.CheckType.UNCHECKED
                && contactModelList != null
                && contactModelList.size() >= maxCount) {
            ToastUtils.show(
                    getContext(),
                    getString(R.string.nc_max_group_members_selection, maxCount),
                    Toast.LENGTH_SHORT);
            return;
        }

        if (contactModel.getCheckType() != ContactModel.CheckType.DISABLE) {
            ContactModel.CheckType updateCheckType =
                    (contactModel.getCheckType() == ContactModel.CheckType.CHECKED)
                            ? ContactModel.CheckType.UNCHECKED
                            : ContactModel.CheckType.CHECKED;
            listener.onActionClick(true);
            contactModel.setCheckType(updateCheckType);
            viewModel.updateContact(contactModel);
        }
    }
}
