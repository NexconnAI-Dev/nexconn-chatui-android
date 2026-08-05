package ai.nexconn.chatui.usermanage.friend.friendlist;

import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.model.OnlineStatusFriendInfo;
import ai.nexconn.chatui.usermanage.component.ContactListComponent;
import ai.nexconn.chatui.usermanage.friend.add.AddFriendListActivity;
import ai.nexconn.chatui.usermanage.friend.search.FriendSearchActivity;
import ai.nexconn.chatui.usermanage.friend.user.profile.UserProfileActivity;
import ai.nexconn.chatui.widget.component.HeadComponent;
import ai.nexconn.chatui.widget.component.SearchComponent;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

/**
 * Friend list page
 *
 * @since 5.12.0
 */
public class FriendListFragment extends BaseViewModelFragment<FriendListViewModel> {

    protected ContactListComponent contactListComponent;
    protected SearchComponent searchComponent;
    protected HeadComponent headComponent;

    @NonNull
    @Override
    protected FriendListViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(FriendListViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_friend_list, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        searchComponent = view.findViewById(R.id.nc_search_component);
        contactListComponent = view.findViewById(R.id.nc_contact_list_component);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull FriendListViewModel viewModel) {
        headComponent.setRightClickListener(
                v -> {
                    // Navigate to add friend page
                    startActivity(AddFriendListActivity.newIntent(getActivity()));
                });

        searchComponent.setSearchClickListener(
                v -> startActivity(FriendSearchActivity.newIntent(getContext())));

        // Observe contact list changes from ViewModel
        viewModel
                .getAllContactsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null && contactListComponent != null) {
                                contactListComponent.setContactList(contactModels);
                            }
                        });
        // Observe online status updates
        viewModel
                .getOnlineStatusLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        data -> {
                            if (data != null && contactListComponent != null) {
                                contactListComponent.setContactOnlineStatusList(data);
                            }
                        });
        // Set contact list item click listener
        contactListComponent.setOnItemClickListener(
                contactModel -> {
                    if (contactModel != null) {
                        String userId = null;
                        if (contactModel.getBean() instanceof FriendDetail) {
                            userId = ((FriendDetail) contactModel.getBean()).getUserId();
                        } else if (contactModel.getBean() instanceof OnlineStatusFriendInfo) {
                            FriendDetail fd =
                                    ((OnlineStatusFriendInfo) contactModel.getBean())
                                            .getFriendDetail();
                            if (fd != null) userId = fd.getUserId();
                        }
                        if (userId != null) {
                            startActivity(UserProfileActivity.newIntent(getActivity(), userId));
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        getViewModel().getAllFriends();
    }
}
