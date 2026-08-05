package ai.nexconn.chatui.usermanage.friend.add;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.user.model.UserProfile;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.usermanage.friend.mine.profile.MyProfileActivity;
import ai.nexconn.chatui.usermanage.friend.user.profile.UserProfileActivity;
import ai.nexconn.chatui.widget.component.HeadComponent;
import ai.nexconn.chatui.widget.component.SearchComponent;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

/**
 * Add friend list page
 *
 * @since 5.12.0
 */
public class AddFriendListFragment extends BaseViewModelFragment<AddFriendListViewModel> {
    protected SearchComponent searchComponent;
    protected HeadComponent headComponent;
    protected LinearLayout llEmptyHint;
    String mQuery;

    @NonNull
    @Override
    protected AddFriendListViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(AddFriendListViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_friend_list_add, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        searchComponent = view.findViewById(R.id.nc_search_component);
        llEmptyHint = view.findViewById(R.id.ll_empty_hint);
        searchComponent.setSearchHint(R.string.nc_app_id);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull AddFriendListViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        searchComponent.setSearchQueryListener(
                new SearchComponent.OnSearchQueryListener() {
                    @Override
                    public void onSearch(String query) {}

                    @Override
                    public void onClickSearch(String query) {
                        mQuery = query;
                        getViewModel().findUser(query);
                    }
                });
        viewModel
                .getUserProfileLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        new Observer<UserProfile>() {
                            @Override
                            public void onChanged(UserProfile userProfiles) {
                                onUserProfileSearchResult(userProfiles);
                            }
                        });
    }

    /**
     * User profile search result
     *
     * @param userProfiles user profile
     */
    protected void onUserProfileSearchResult(UserProfile userProfiles) {
        if (userProfiles != null) {
            if (userProfiles.getUserId().equals(NCEngine.getCurrentUserId())) {
                startActivity(MyProfileActivity.newIntent(getActivity()));
            } else {
                startActivity(
                        UserProfileActivity.newIntent(getActivity(), userProfiles.getUserId()));
            }

        } else {
            llEmptyHint.setVisibility(TextUtils.isEmpty(mQuery) ? View.GONE : View.VISIBLE);
        }
    }
}
