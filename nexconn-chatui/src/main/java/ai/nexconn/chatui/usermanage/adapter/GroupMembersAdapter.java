package ai.nexconn.chatui.usermanage.adapter;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.utils.log.RLog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GroupMembersAdapter
        extends RecyclerView.Adapter<GroupMembersAdapter.GroupInfoViewHolder> {

    private List<GroupMemberInfo> groupInfoList;
    private final Context context;
    private final int groupDisplayLimit;
    private boolean allowGroupRemoval = false;
    private boolean allowGroupAddition = false;
    private OnGroupActionListener groupActionListener;

    public GroupMembersAdapter(Context context, int groupDisplayLimit) {
        this.context = context;
        this.groupDisplayLimit = groupDisplayLimit;
    }

    @NonNull
    @Override
    public GroupInfoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(context).inflate(R.layout.nc_item_group_member, parent, false);
        return new GroupInfoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupInfoViewHolder holder, int position) {
        if (isSpecialActionPosition(position)) {
            setupSpecialActionItem(holder, position);
        } else {
            setupGroupInfoItem(holder, position);
        }
    }

    @Override
    public int getItemCount() {
        int extraItems =
                (allowGroupRemoval && allowGroupAddition)
                        ? 2
                        : (allowGroupRemoval || allowGroupAddition) ? 1 : 0;
        return (groupInfoList != null ? groupInfoList.size() : 0) + extraItems;
    }

    private boolean isSpecialActionPosition(int position) {
        return groupInfoList == null || position >= groupInfoList.size();
    }

    private void setupSpecialActionItem(@NonNull GroupInfoViewHolder holder, int position) {
        // Clear any ongoing Glide request on ImageView to prevent async loading from overriding
        // special action icons
        Glide.with(context).clear(holder.avatarImageView);

        if (position == getItemCount() - 1 && allowGroupRemoval) {
            holder.groupNameTextView.setText("");
            holder.groupNameTextView.setVisibility(View.GONE);
            holder.avatarImageView.setImageResource(
                    ChatUIThemeManager.getAttrResId(context, R.attr.nc_group_member_remove_img));
            holder.itemView.setOnClickListener(
                    v -> {
                        if (groupActionListener != null) {
                            groupActionListener.removeMemberClick();
                        }
                    });
        } else if (allowGroupAddition) {
            holder.groupNameTextView.setText("");
            holder.groupNameTextView.setVisibility(View.GONE);
            holder.avatarImageView.setImageResource(
                    ChatUIThemeManager.getAttrResId(context, R.attr.nc_group_member_add_img));
            holder.itemView.setOnClickListener(
                    v -> {
                        if (groupActionListener != null) {
                            groupActionListener.addMemberClick();
                        }
                    });
        }
    }

    private void setupGroupInfoItem(@NonNull GroupInfoViewHolder holder, int position) {
        GroupMemberInfo groupMemberInfo = groupInfoList.get(position);
        holder.groupNameTextView.setText(
                !TextUtils.isEmpty(groupMemberInfo.getNickname())
                        ? groupMemberInfo.getNickname()
                        : groupMemberInfo.getName());
        holder.groupNameTextView.setVisibility(View.VISIBLE);

        WeakReference<GroupInfoViewHolder> holderRef = new WeakReference<>(holder);
        NCEngine.getUserModule()
                .getFriendsInfo(
                        Collections.singletonList(groupMemberInfo.getUserId()),
                        (friendDetails, error) -> {
                            GroupInfoViewHolder viewHolder = holderRef.get();
                            if (error == null
                                    && viewHolder != null
                                    && friendDetails != null
                                    && !friendDetails.isEmpty()) {
                                FriendDetail friendDetail = friendDetails.get(0);
                                if (friendDetail != null
                                        && !TextUtils.isEmpty(friendDetail.getRemark())
                                        && viewHolder.groupNameTextView != null) {
                                    viewHolder.groupNameTextView.setText(friendDetail.getRemark());
                                }
                            } else if (error != null && viewHolder != null) {
                                RLog.e(
                                        "GroupMembersAdapter",
                                        "getFriendsInfo error: " + error.getMessage());
                            }
                        });

        String portraitUri = groupMemberInfo.getPortraitUri();
        NCChatUIConfig.featureConfig()
                .getChatUIImageEngine()
                .loadUserPortrait(
                        holder.avatarImageView.getContext(), portraitUri, holder.avatarImageView);

        holder.itemView.setOnClickListener(
                v -> {
                    if (groupActionListener != null) {
                        groupActionListener.onGroupClicked(groupMemberInfo);
                    }
                });
    }

    public void setAllowGroupRemoval(boolean allowGroupRemoval) {
        this.allowGroupRemoval = allowGroupRemoval;
        notifyDataSetChanged();
    }

    public void setAllowGroupAddition(boolean allowGroupAddition) {
        this.allowGroupAddition = allowGroupAddition;
        notifyDataSetChanged();
    }

    public void updateGroupInfoList(List<GroupMemberInfo> groupInfoList) {
        groupInfoList = new CopyOnWriteArrayList<>(groupInfoList);
        if (groupDisplayLimit > 0
                && groupInfoList != null
                && groupInfoList.size() > groupDisplayLimit) {
            this.groupInfoList = groupInfoList.subList(0, groupDisplayLimit);
        } else {
            this.groupInfoList = groupInfoList;
        }
        notifyDataSetChanged();
    }

    public void setOnGroupActionListener(OnGroupActionListener listener) {
        this.groupActionListener = listener;
    }

    public interface OnGroupActionListener {
        /** Callback for add member click event. */
        default void addMemberClick() {}

        /** Callback for remove member click event. */
        default void removeMemberClick() {}

        /**
         * Callback for group member click event.
         *
         * @param groupInfo the group member info
         */
        void onGroupClicked(GroupMemberInfo groupInfo);
    }

    public static class GroupInfoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView avatarImageView;
        private final TextView groupNameTextView;

        public GroupInfoViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.iv_group_member_avatar);
            groupNameTextView = itemView.findViewById(R.id.tv_group_member_name);
        }
    }
}
