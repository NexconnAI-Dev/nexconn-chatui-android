package ai.nexconn.chatui.usermanage.group.memberselect;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.base.interfaces.OnActionClickListener;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.usermanage.component.ContactListComponent;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import ai.nexconn.chatui.widget.component.HeadComponent;
import ai.nexconn.chatui.widget.component.SearchComponent;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract fragment for group member selection.
 *
 * @since 5.12.2
 */
public abstract class GroupMemberSelectionFragment
        extends BaseViewModelFragment<GroupMemberSelectionViewModel> {

    protected HeadComponent headComponent;
    protected SearchComponent searchComponent;
    protected ContactListComponent contactListComponent;
    private TextView emptyView;

    @NonNull
    @Override
    protected GroupMemberSelectionViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupMemberSelectionViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_group_member_selection, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        searchComponent = view.findViewById(R.id.nc_search_component);
        contactListComponent = view.findViewById(R.id.nc_contact_list_component);
        emptyView = view.findViewById(R.id.nc_empty_tv);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupMemberSelectionViewModel viewModel) {
        onBindHeadComponent(headComponent, viewModel);
        onBindSearchComponent(searchComponent, viewModel);
        onBindContactListComponent(contactListComponent, viewModel);
    }

    protected void onBindHeadComponent(
            @NonNull HeadComponent headComponent,
            @NonNull GroupMemberSelectionViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());

        ChannelIdentifier conversationIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        getArguments(), ChatUIConstants.KEY_CHANNEL_IDENTIFIER);
        headComponent.setRightClickListener(
                v -> {
                    // Get selected contacts
                    List<ContactModel> contactModels =
                            viewModel.getSelectedContactsLiveData().getValue();
                    List<GroupMemberInfo> groupMemberInfoList = new ArrayList<>();

                    // Iterate contacts, collect group member info
                    if (contactModels != null && !contactModels.isEmpty()) {
                        for (ContactModel contactModel : contactModels) {
                            if (contactModel.getBean() instanceof GroupMemberInfo) {
                                groupMemberInfoList.add((GroupMemberInfo) contactModel.getBean());
                            }
                        }
                    }
                    handleConfirmSelection(viewModel, conversationIdentifier, groupMemberInfoList);
                });

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

    protected void onBindSearchComponent(
            @NonNull SearchComponent searchComponent,
            @NonNull GroupMemberSelectionViewModel viewModel) {
        searchComponent.setSearchQueryListener(viewModel::queryContacts);
    }

    protected void onBindContactListComponent(
            @NonNull ContactListComponent contactListComponent,
            @NonNull GroupMemberSelectionViewModel viewModel) {
        contactListComponent.setOnPageDataLoader(viewModel);
        contactListComponent.setEnableLoadMore(true);
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
     * Handle contact selection.
     *
     * @param viewModel group member selection ViewModel
     * @param contactModel contact info
     * @param listener confirm click listener
     */
    protected void handleContactSelection(
            @NonNull GroupMemberSelectionViewModel viewModel,
            ContactModel contactModel,
            OnActionClickListener.OnConfirmClickListener<Boolean> listener) {
        if (contactModel.getCheckType() != ContactModel.CheckType.DISABLE) {
            ContactModel.CheckType newCheckType =
                    (contactModel.getCheckType() == ContactModel.CheckType.CHECKED)
                            ? ContactModel.CheckType.UNCHECKED
                            : ContactModel.CheckType.CHECKED;
            listener.onActionClick(true);
            contactModel.setCheckType(newCheckType);
            viewModel.updateContact(contactModel);
        }
    }

    /**
     * Handle confirm selection.
     *
     * @param viewModel group member selection ViewModel
     * @param conversationIdentifier channel identifier
     * @param selectGroupMemberInfoList selected group member info list
     */
    protected abstract void handleConfirmSelection(
            @NonNull GroupMemberSelectionViewModel viewModel,
            ChannelIdentifier conversationIdentifier,
            List<GroupMemberInfo> selectGroupMemberInfoList);
}
