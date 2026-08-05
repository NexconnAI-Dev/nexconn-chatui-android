package ai.nexconn.chatui.usermanage.group.list;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.usermanage.adapter.GroupListAdapter;
import ai.nexconn.chatui.usermanage.component.CommonListComponent;
import ai.nexconn.chatui.usermanage.group.search.GroupSearchActivity;
import ai.nexconn.chatui.utils.route.RouteUtils;
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

/**
 * Fragment for group list.
 *
 * @since 5.12.2
 */
public class GroupListFragment extends BaseViewModelFragment<GroupListViewModel> {

    protected HeadComponent headComponent;
    protected SearchComponent searchComponent;
    protected CommonListComponent groupListComponent;
    protected GroupListAdapter groupListAdapter;
    private TextView emptyView;

    @NonNull
    @Override
    protected GroupListViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupListViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_group_list, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        searchComponent = view.findViewById(R.id.nc_search_component);
        emptyView = view.findViewById(R.id.nc_empty_tv);
        groupListComponent = view.findViewById(R.id.nc_group_list_component);
        groupListAdapter = new GroupListAdapter();
        groupListComponent.setAdapter(groupListAdapter);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupListViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        searchComponent.setSearchClickListener(
                v -> startActivity(GroupSearchActivity.newIntent(getContext())));

        groupListComponent.setOnPageDataLoader(viewModel.getOnPageDataLoader());
        groupListAdapter.setOnItemClickListener(this::onGroupItemClick);
        groupListAdapter.setOnItemLongClickListener(this::onGroupItemLongClick);

        // Observe contact list changes in ViewModel
        viewModel
                .getAllGroupInfoListLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null && !contactModels.isEmpty()) {
                                emptyView.setVisibility(View.GONE);
                                groupListComponent.setVisibility(View.VISIBLE);
                                groupListAdapter.setData(contactModels);
                            } else {
                                emptyView.setVisibility(View.VISIBLE);
                                groupListComponent.setVisibility(View.GONE);
                            }
                        });
    }

    /**
     * Handle group item click.
     *
     * @param groupInfo group info
     */
    protected void onGroupItemClick(GroupInfo groupInfo) {
        if (groupInfo != null) {
            RouteUtils.routeToChannelActivity(
                    getActivity(),
                    new ChannelIdentifier(ChannelType.GROUP, groupInfo.getGroupId()));
        }
    }

    /**
     * Handle group item long click.
     *
     * @param groupInfo group info
     */
    protected void onGroupItemLongClick(GroupInfo groupInfo) {}
}
