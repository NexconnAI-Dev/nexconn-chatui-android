package ai.nexconn.chatui.channel.feature.forward;

import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.query.ChannelsQuery;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.model.PageData;
import ai.nexconn.chat.params.ChannelsQueryParams;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.userinfo.NCUserInfoManager;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.widget.ChatUISwipeRefreshLayout;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Forward select channel list Fragment, logic migrated from ForwardSelectConversationActivity. */
public class ForwardSelectChannelFragment extends Fragment
        implements View.OnClickListener,
                ChatUISwipeRefreshLayout.OnLoadListener,
                NCUserInfoManager.UserDataObserver {

    private static final String TAG = "ForwardSelectChannelFragment";
    private TextView btOK;
    private ListAdapter mAdapter;
    private ChatUISwipeRefreshLayout mRefreshLayout;
    private final ArrayList<ChannelIdentifier> selectedMember = new ArrayList<>();
    private static final List<ChannelType> defConversationType =
            Arrays.asList(ChannelType.DIRECT, ChannelType.GROUP);
    private ChannelsQuery mChannelsQuery;

    static class ConversationItem {
        ChannelIdentifier identifier;
        String title;
        String portraitUrl;

        ConversationItem(ChannelIdentifier id, String title, String portraitUrl) {
            this.identifier = id;
            this.title = title;
            this.portraitUrl = portraitUrl;
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        if (getActivity() != null && getActivity().getWindow() != null) {
            int flag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
            getActivity().getWindow().setFlags(flag, flag);
        }
        return inflater.inflate(R.layout.nc_activity_forward_select, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btOK = view.findViewById(R.id.nc_btn_ok);
        TextView btCancel = view.findViewById(R.id.nc_btn_cancel);
        mRefreshLayout = view.findViewById(R.id.nc_refresh);
        ListView listView = view.findViewById(R.id.nc_list);
        btOK.setEnabled(false);
        btOK.setOnClickListener(this);
        btCancel.setOnClickListener(this);
        mRefreshLayout.setCanRefresh(false);
        mRefreshLayout.setCanLoading(true);
        mRefreshLayout.setOnLoadListener(this);
        Context ctx = getContext();
        mAdapter = new ListAdapter(ctx != null ? ctx : requireContext());
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new ForwardItemClickListener());
        NCUserInfoManager.getInstance().addUserDataObserver(this);
        mChannelsQuery =
                BaseChannel.createChannelsQuery(new ChannelsQueryParams(defConversationType));
        loadChannels();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        NCUserInfoManager.getInstance().removeUserDataObserver(this);
    }

    private void loadChannels() {
        if (mChannelsQuery == null) {
            mRefreshLayout.setLoadMoreFinish(false);
            mRefreshLayout.setCanLoading(false);
            return;
        }
        mChannelsQuery.loadNextPage(
                new OperationHandler<PageData<BaseChannel>>() {
                    @Override
                    public void onResult(PageData<BaseChannel> pageData, NCError error) {
                        if (getActivity() == null || getActivity().isFinishing()) return;
                        if (error != null || pageData == null) {
                            mRefreshLayout.setLoadMoreFinish(false);
                            return;
                        }
                        List<BaseChannel> channels = pageData.getData();
                        if (channels == null || channels.isEmpty()) {
                            mRefreshLayout.setLoadMoreFinish(false);
                            mRefreshLayout.setCanLoading(false);
                            return;
                        }
                        List<ConversationItem> items = new ArrayList<>();
                        for (BaseChannel ch : channels) {
                            ChannelIdentifier id =
                                    new ChannelIdentifier(ch.getChannelType(), ch.getChannelId());
                            String title = "";
                            String portrait = "";
                            if (ChannelType.DIRECT.equals(id.getChannelType())) {
                                UserInfo u =
                                        NCUserInfoManager.getInstance()
                                                .getUserInfo(id.getChannelId());
                                if (u != null) {
                                    title = NCUserInfoManager.getInstance().getUserDisplayName(u);
                                    portrait = u.getPortraitUri();
                                } else {
                                    title = id.getChannelId();
                                }
                            } else {
                                ai.nexconn.chat.channel.model.GroupInfo g =
                                        NCUserInfoManager.getInstance()
                                                .getGroupInfo(id.getChannelId());
                                if (g != null) {
                                    title = g.getGroupName();
                                    portrait = g.getPortraitUri();
                                } else {
                                    title = id.getChannelId();
                                }
                            }
                            items.add(new ConversationItem(id, title, portrait));
                        }
                        mAdapter.addAll(items);
                        mAdapter.notifyDataSetChanged();
                        mRefreshLayout.setLoadMoreFinish(false);
                    }
                });
    }

    @Override
    public void onLoad() {
        loadChannels();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.nc_btn_ok) {
            if (getActivity() != null && !getActivity().isFinishing())
                ForwardManager.setForwardMessageResult(requireActivity(), selectedMember);
        } else if (v.getId() == R.id.nc_btn_cancel) {
            if (getActivity() != null) getActivity().finish();
        }
    }

    @Override
    public void onUserUpdate(ai.nexconn.chat.user.model.UserInfo user) {
        if (user != null && mAdapter != null) {
            for (ConversationItem item : mAdapter.allItems) {
                if (user.getUserId().equals(item.identifier.getChannelId())
                        && ChannelType.DIRECT.equals(item.identifier.getChannelType())) {
                    if (user.getName() != null) item.title = user.getName();
                    item.portraitUrl = user.getPortraitUri();
                    break;
                }
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onGroupUpdate(ai.nexconn.chat.channel.model.GroupInfo group) {
        if (group == null || mAdapter == null) return;
        for (ConversationItem item : mAdapter.allItems) {
            if (TextUtils.equals(group.getGroupId(), item.identifier.getChannelId())) {
                if (group.getGroupName() != null) item.title = group.getGroupName();
                item.portraitUrl = group.getPortraitUri();
                mAdapter.notifyDataSetChanged();
                break;
            }
        }
    }

    @Override
    public void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo) {}

    private class ForwardItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            View v = view.findViewById(R.id.nc_checkbox);
            if (v == null) {
                RLog.d(TAG, "NC_checkbox is null.");
                return;
            }
            ChannelIdentifier member = (ChannelIdentifier) v.getTag();
            selectedMember.remove(member);
            v.setSelected(!v.isSelected());
            if (v.isSelected()) selectedMember.add(member);
            btOK.setEnabled(selectedMember.size() > 0);
        }
    }

    private class ListAdapter extends BaseAdapter {
        private final Context activity;
        private final List<ConversationItem> allItems = new ArrayList<>();

        ListAdapter(Context activity) {
            this.activity = activity;
        }

        void addAll(List<ConversationItem> items) {
            allItems.addAll(items);
        }

        @Override
        public int getCount() {
            return allItems.size();
        }

        @Override
        public Object getItem(int position) {
            return allItems.isEmpty() ? null : allItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView =
                        LayoutInflater.from(activity)
                                .inflate(R.layout.nc_listitem_forward_select_member, null);
                holder.checkbox = convertView.findViewById(R.id.nc_checkbox);
                holder.portrait = convertView.findViewById(R.id.nc_user_portrait);
                holder.name = convertView.findViewById(R.id.nc_user_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            ConversationItem c = allItems.get(position);
            holder.checkbox.setTag(c.identifier);
            holder.checkbox.setClickable(false);
            holder.checkbox.setImageResource(R.drawable.nc_select_conversation_checkbox);
            holder.checkbox.setEnabled(true);
            holder.checkbox.setSelected(selectedMember.contains(c.identifier));
            Context ctx = NCChatUI.getContext() != null ? NCChatUI.getContext() : activity;
            if (ctx != null)
                Glide.with(ctx)
                        .load(c.portraitUrl)
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .into(holder.portrait);
            holder.name.setText(c.title);
            return convertView;
        }
    }

    private static class ViewHolder {
        ImageView checkbox;
        ImageView portrait;
        TextView name;
    }
}
