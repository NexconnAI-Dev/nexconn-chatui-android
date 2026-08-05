package ai.nexconn.chatui.usermanage.friend.search;

import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.base.adapter.CommonAdapter;
import ai.nexconn.chatui.base.adapter.MultiItemTypeAdapter;
import ai.nexconn.chatui.base.adapter.ViewHolder;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.usermanage.friend.user.profile.UserProfileActivity;
import ai.nexconn.chatui.widget.component.HeadComponent;
import ai.nexconn.chatui.widget.component.ListComponent;
import ai.nexconn.chatui.widget.component.SearchComponent;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Friend search page
 *
 * @since 5.12.0
 */
public class FriendSearchFragment extends BaseViewModelFragment<FriendSearchViewModel> {

    protected ListComponent listComponent;
    protected SearchComponent searchComponent;
    protected HeadComponent headComponent;

    protected CommonAdapter<FriendDetail> adapter =
            new CommonAdapter<FriendDetail>(R.layout.nc_contact_item) {
                @Override
                public void bindData(ViewHolder holder, FriendDetail friendInfo, int position) {
                    holder.setText(R.id.tv_contact_name, friendInfo.getName());
                    NCChatUIConfig.featureConfig()
                            .getChatUIImageEngine()
                            .loadUserPortrait(
                                    holder.itemView.getContext(),
                                    friendInfo.getPortraitUri(),
                                    holder.<ImageView>getView(R.id.iv_contact_portrait));
                }
            };

    @NonNull
    @Override
    protected FriendSearchViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(FriendSearchViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_friend_search, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        searchComponent = view.findViewById(R.id.nc_search_component);
        listComponent = view.findViewById(R.id.nc_list_component);
        listComponent = view.findViewById(R.id.nc_list_component);
        listComponent.setEnableLoadMore(false);
        listComponent.setEnableRefresh(false);
        listComponent.setAdapter(adapter);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull FriendSearchViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());

        searchComponent.setSearchQueryListener(viewModel::queryContacts);

        // Observe contact list changes from ViewModel
        viewModel
                .getFriendInfoLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        data -> {
                            adapter.setData(data);
                        });
        // Set contact list item click listener
        adapter.setOnItemClickListener(
                new MultiItemTypeAdapter.OnItemClickListener<FriendDetail>() {
                    @Override
                    public void onItemClick(
                            View view,
                            RecyclerView.ViewHolder holder,
                            FriendDetail friendInfo,
                            int position) {
                        onFriendItemClick(friendInfo);
                    }

                    @Override
                    public boolean onItemLongClick(
                            View view,
                            RecyclerView.ViewHolder holder,
                            FriendDetail friendInfo,
                            int position) {
                        return false;
                    }
                });
    }

    /**
     * Handle contact list item click
     *
     * @param friendInfo friend info
     */
    protected void onFriendItemClick(FriendDetail friendInfo) {
        startActivity(UserProfileActivity.newIntent(getActivity(), friendInfo.getUserId()));
    }
}
