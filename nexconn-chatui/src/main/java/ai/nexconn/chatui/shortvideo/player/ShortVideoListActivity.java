package ai.nexconn.chatui.shortvideo.player;

import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.query.LocalMessagesByTimeQuery;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.ShortVideoMessage;
import ai.nexconn.chat.message.model.MessageType;
import ai.nexconn.chat.params.LocalMessagesByTimeQueryParams;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.activity.NCBaseNoActionbarActivity;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.userinfo.NCUserInfoManager;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.text.TimeUtils;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShortVideoListActivity extends NCBaseNoActionbarActivity
        implements NCUserInfoManager.UserDataObserver {
    private String targetId;
    private ChannelType conversationType;
    private SightListAdapter sightListAdapter;
    private static final int DEFAULT_FILE_COUNT = 100;
    private boolean isDestruct;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nc_activity_sight_list);
        Intent intent = getIntent();
        targetId = intent.getStringExtra("targetId");
        isDestruct = intent.getBooleanExtra("isDestruct", false);
        conversationType = ChannelType.fromValue(intent.getIntExtra("conversationType", 0));
        ListView fileListView = findViewById(R.id.sightList);
        sightListAdapter = new SightListAdapter();
        fileListView.setAdapter(sightListAdapter);

        loadData();
        findViewById(R.id.imgbtn_nav_back)
                .setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finish();
                            }
                        });
        initUserInfoChangeListener();
    }

    private void loadData() {
        if (targetId == null || conversationType == null) {
            RLog.e("ShortVideoListActivity", "loadData: targetId or conversationType is null");
            return;
        }
        ChannelIdentifier channelId = new ChannelIdentifier(conversationType, targetId);
        LocalMessagesByTimeQueryParams params = new LocalMessagesByTimeQueryParams(channelId);
        params.setAscending(false);
        params.setMessageTypes(Collections.singletonList(MessageType.SHORT_VIDEO));
        params.setPageSize(DEFAULT_FILE_COUNT);
        LocalMessagesByTimeQuery query = BaseChannel.createLocalMessagesByTimeQuery(params);
        query.loadNextPage(
                (result, error) -> {
                    if (error != null || result == null) {
                        RLog.e("ShortVideoListActivity", "loadData failed: " + error);
                        return;
                    }
                    List<Message> messages = result.getData();
                    runOnUiThread(() -> setListAdapterData(messages, sightListAdapter));
                });
    }

    private void initUserInfoChangeListener() {
        NCUserInfoManager.getInstance().addUserDataObserver(this);
    }

    @Override
    protected void onDestroy() {
        NCUserInfoManager.getInstance().removeUserDataObserver(this);
        super.onDestroy();
    }

    private void setListAdapterData(List<Message> messages, SightListAdapter sightListAdapter) {
        List<ItemData> itemDataList = new ArrayList<>();
        for (Message message : messages) {
            if (isDestruct != message.getContent().isDestruct()) {
                continue;
            }
            ItemData data = new ItemData();
            data.message = message;
            data.senderName = getSenderName(message.getSenderUserId());
            itemDataList.add(data);
        }
        sightListAdapter.setFileData(itemDataList);
        sightListAdapter.notifyDataSetChanged();
    }

    public void updateUserInfo(UserInfo userInfo) {
        boolean needUpdate = false;
        for (ItemData itemData : sightListAdapter.getData()) {
            if (itemData.message.getSenderUserId().equals(userInfo.getUserId())) {
                itemData.senderName = getSenderName(itemData.message.getSenderUserId());
                needUpdate = true;
            }
        }
        if (needUpdate) {
            sightListAdapter.notifyDataSetChanged();
        }
    }

    public void updateGroupUserInfo(GroupUserInfo groupMember) {
        boolean needUpdate = false;
        if (groupMember != null
                && conversationType == ChannelType.GROUP
                && targetId.equals(groupMember.getGroupId())) {
            for (ItemData itemData : sightListAdapter.getData()) {
                if (itemData.message.getSenderUserId().equals(groupMember.getUserId())) {
                    itemData.senderName = getSenderName(itemData.message.getSenderUserId());
                    needUpdate = true;
                }
            }
            if (needUpdate) {
                sightListAdapter.notifyDataSetChanged();
            }
        }
    }

    private String getSenderName(String senderUserId) {
        UserInfo userInfo = NCUserInfoManager.getInstance().getUserInfo(senderUserId);
        String groupMemberName = "";
        if (conversationType == ChannelType.GROUP) {
            GroupUserInfo groupUserInfo =
                    NCUserInfoManager.getInstance().getGroupUserInfo(targetId, senderUserId);
            groupMemberName = groupUserInfo != null ? groupUserInfo.getNickname() : "";
        }
        return NCUserInfoManager.getInstance().getUserDisplayName(userInfo, groupMemberName);
    }

    @Override
    public void onUserUpdate(UserInfo user) {
        updateUserInfo(user);
    }

    @Override
    public void onGroupUpdate(ai.nexconn.chat.channel.model.GroupInfo group) {
        // default implementation ignored
    }

    @Override
    public void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo) {
        updateGroupUserInfo(groupUserInfo);
    }

    private class SightListAdapter extends BaseAdapter {
        List<ItemData> fileData = new ArrayList<>();

        public void setFileData(List<ItemData> fileData) {
            this.fileData.addAll(fileData);
        }

        public List<ItemData> getData() {
            return fileData;
        }

        @Override
        public int getCount() {
            return fileData.size();
        }

        @Override
        public Object getItem(int position) {
            return fileData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                convertView = inflater.inflate(R.layout.nc_lively_sight_list_item, null);
                viewHolder = new ViewHolder();
                viewHolder.itemIcon = convertView.findViewById(R.id.nc_portrait);
                viewHolder.itemTitle = convertView.findViewById(R.id.nc_title);
                viewHolder.itemDetail = convertView.findViewById(R.id.nc_detail);
                viewHolder.itemTime = convertView.findViewById(R.id.nc_time);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            ItemData itemData = fileData.get(position);
            final Message message = itemData.message;
            final ShortVideoMessage sightMessage = (ShortVideoMessage) message.getContent();
            viewHolder.itemTitle.setText(sightMessage.getName());
            String time = TimeUtils.formatData(ShortVideoListActivity.this, message.getSentTime());
            String size = convertFileSize(sightMessage.getSize());
            String detail = String.format("%s %s", size, itemData.senderName);
            viewHolder.itemDetail.setText(detail);
            if (viewHolder.itemTime != null) {
                viewHolder.itemTime.setText(time);
            }
            viewHolder.itemIcon.setImageResource(
                    ChatUIThemeManager.getAttrResId(
                            ShortVideoListActivity.this, R.attr.nc_ic_sight_video));
            convertView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ai.nexconn.chatui.utils.message.MessageHolder.holdMessage(message);
                            Intent intent =
                                    new Intent(
                                            ShortVideoListActivity.this,
                                            ShortVideoPlayerActivity.class);
                            intent.putExtra("fromList", true);
                            startActivity(intent);
                        }
                    });
            return convertView;
        }
    }

    private static class ViewHolder {
        ImageView itemIcon;
        TextView itemTitle;
        TextView itemDetail;
        TextView itemTime;
    }

    private static class ItemData {
        Message message;
        String senderName;
    }

    private String convertFileSize(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;
        if (size < kb) {
            return String.format("%.2fB", (float) size);
        } else if (size < mb) return String.format("%.2fKB", (float) size / kb);
        else if (size < gb) return String.format("%.2fMB", (float) size / mb);
        else return String.format("%.2fG", (float) size / gb);
    }
}
