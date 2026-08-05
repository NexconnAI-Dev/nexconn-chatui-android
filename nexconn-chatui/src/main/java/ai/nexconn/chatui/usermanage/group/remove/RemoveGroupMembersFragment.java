package ai.nexconn.chatui.usermanage.group.remove;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.usermanage.component.ContactListComponent;
import ai.nexconn.chatui.utils.common.ToastUtils;
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

/**
 * Fragment for removing group members.
 *
 * @since 5.12.0
 */
public class RemoveGroupMembersFragment extends BaseViewModelFragment<RemoveGroupMembersViewModel> {

    protected HeadComponent headComponent;
    protected SearchComponent searchComponent;
    protected ContactListComponent contactListComponent;
    private TextView emptyView;

    @NonNull
    @Override
    protected RemoveGroupMembersViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(RemoveGroupMembersViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_group_remove_member, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        searchComponent = view.findViewById(R.id.nc_search_component);
        contactListComponent = view.findViewById(R.id.nc_contact_list_component);
        emptyView = view.findViewById(R.id.nc_empty_tv);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull RemoveGroupMembersViewModel viewModel) {
        onBindHeadComponent(headComponent, viewModel);
        onBindSearchComponent(searchComponent, viewModel);
        onBindContactListComponent(contactListComponent, viewModel);
    }

    protected void onBindHeadComponent(
            @NonNull HeadComponent headComponent, @NonNull RemoveGroupMembersViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());

        headComponent.setRightClickListener(
                v ->
                        viewModel.kickGroupMembers(
                                isSuccess -> {
                                    if (isSuccess) {
                                        ToastUtils.show(
                                                getActivity(),
                                                getString(R.string.nc_group_members_kick_success),
                                                Toast.LENGTH_SHORT);
                                        if (getActivity() != null) {
                                            getActivity().finish();
                                        }
                                    } else {
                                        ToastUtils.show(
                                                getActivity(),
                                                getString(R.string.nc_group_members_kick_failed),
                                                Toast.LENGTH_SHORT);
                                    }
                                }));

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
            @NonNull RemoveGroupMembersViewModel viewModel) {
        searchComponent.setSearchQueryListener(viewModel::queryGroupMembers);
    }

    protected void onBindContactListComponent(
            @NonNull ContactListComponent contactListComponent,
            @NonNull RemoveGroupMembersViewModel viewModel) {
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
                contactModel -> {
                    if (contactModel.getCheckType() != ContactModel.CheckType.DISABLE) {
                        ContactModel.CheckType newCheckType =
                                (contactModel.getCheckType() == ContactModel.CheckType.CHECKED)
                                        ? ContactModel.CheckType.UNCHECKED
                                        : ContactModel.CheckType.CHECKED;

                        contactModel.setCheckType(newCheckType);
                        viewModel.updateContact(contactModel);
                    }
                });
    }
}
