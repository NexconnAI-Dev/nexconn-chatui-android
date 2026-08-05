package ai.nexconn.chatui.usermanage.group.add;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.base.interfaces.OnActionClickListener;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.usermanage.component.ContactListComponent;
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
import java.util.List;

/**
 * Fragment for adding group members.
 *
 * @since 5.12.0
 */
public class AddGroupMembersFragment extends BaseViewModelFragment<AddGroupMembersViewModel> {

    protected HeadComponent headComponent;
    protected SearchComponent searchComponent;
    protected ContactListComponent contactListComponent;
    private TextView emptyView;

    @NonNull
    @Override
    protected AddGroupMembersViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(AddGroupMembersViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_group_add_member, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        searchComponent = view.findViewById(R.id.nc_search_component);
        contactListComponent = view.findViewById(R.id.nc_contact_list_component);
        emptyView = view.findViewById(R.id.nc_empty_tv);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull AddGroupMembersViewModel viewModel) {
        onBindHeadComponent(headComponent, viewModel);
        onBindSearchComponent(searchComponent, viewModel);
        onBindContactListComponent(contactListComponent, viewModel);
    }

    protected void onBindHeadComponent(
            @NonNull HeadComponent headComponent, @NonNull AddGroupMembersViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        headComponent.setRightClickListener(v -> handleConfirmSelection(viewModel));

        viewModel
                .getSelectedContactsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null && headComponent != null) {
                                headComponent.setRightTextViewEnable(!contactModels.isEmpty());
                            }
                        });
    }

    private void handleConfirmSelection(@NonNull AddGroupMembersViewModel viewModel) {
        viewModel.inviteUsersToGroup(
                code ->
                        onInviteUsersToGroupResult(
                                viewModel.getGroupId(), viewModel.getSelectUserIds(), code));
    }

    protected void onInviteUsersToGroupResult(
            String groupId, List<String> selectedIdList, int errorCode) {
        if (errorCode == 0) {
            ToastUtils.show(
                    getActivity(),
                    getString(R.string.nc_invite_join_group_success),
                    Toast.LENGTH_SHORT);
            finishActivity();
        } else if (errorCode == 25427) {
            ToastUtils.show(
                    getActivity(),
                    getString(R.string.nc_invite_join_group_send),
                    Toast.LENGTH_SHORT);
            finishActivity();
        } else if (errorCode == 25424) {
            ToastUtils.show(
                    getActivity(),
                    getString(R.string.nc_invite_join_group_send_need_manange),
                    Toast.LENGTH_SHORT);
            finishActivity();
        } else {
            ToastUtils.show(
                    getActivity(),
                    getString(R.string.nc_invite_join_group_failed),
                    Toast.LENGTH_SHORT);
        }
    }

    protected void onBindSearchComponent(
            @NonNull SearchComponent searchComponent, @NonNull AddGroupMembersViewModel viewModel) {
        searchComponent.setSearchQueryListener(viewModel::queryContacts);
    }

    protected void onBindContactListComponent(
            @NonNull ContactListComponent contactListComponent,
            @NonNull AddGroupMembersViewModel viewModel) {
        int maxCount =
                Math.max(
                        1,
                        Math.min(
                                30,
                                getArguments()
                                        .getInt(ChatUIConstants.KEY_MAX_MEMBER_COUNT_ADD, 30)));
        viewModel
                .getFilteredContactsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null && !contactModels.isEmpty()) {
                                emptyView.setVisibility(View.GONE);
                                contactListComponent.setVisibility(View.VISIBLE);
                                contactListComponent.post(
                                        () -> contactListComponent.setContactList(contactModels));
                            } else {
                                emptyView.setVisibility(View.VISIBLE);
                                contactListComponent.setVisibility(View.GONE);
                            }
                        });

        // Set contact list click event
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
                                    maxCount,
                                    (OnConfirmClickListener<Boolean>) listener);
                        }
                    }
                });
    }

    private void handleContactSelection(
            @NonNull AddGroupMembersViewModel viewModel,
            ContactModel contactModel,
            int maxCount,
            OnActionClickListener.OnConfirmClickListener<Boolean> listener) {
        if (contactModel.getCheckType() != ContactModel.CheckType.DISABLE) {
            List<ContactModel> contactModelList =
                    viewModel.getSelectedContactsLiveData().getValue();
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
