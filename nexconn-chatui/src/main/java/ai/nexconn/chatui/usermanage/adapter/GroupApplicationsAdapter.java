package ai.nexconn.chatui.usermanage.adapter;

import ai.nexconn.chat.channel.GroupChannel;
import ai.nexconn.chat.channel.model.GroupApplicationDirection;
import ai.nexconn.chat.channel.model.GroupApplicationInfo;
import ai.nexconn.chat.channel.model.GroupApplicationStatus;
import ai.nexconn.chat.channel.model.GroupApplicationType;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.interfaces.OnActionClickListener;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.utils.log.RLog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Adapter for the group application list.
 *
 * @since 5.12.2
 */
public class GroupApplicationsAdapter
        extends RecyclerView.Adapter<GroupApplicationsAdapter.GroupApplicationViewHolder> {

    private final List<GroupApplicationInfo> data = new ArrayList<>();
    private OnActionClickListener<GroupApplicationInfo> onAcceptClickListener;
    private OnActionClickListener<GroupApplicationInfo> onRejectClickListener;

    private final WeakHashMap<String, GroupInfo> groupInfoCacheMap = new WeakHashMap<>();

    /**
     * Sets the accept button click listener.
     *
     * @param listener the click event listener
     */
    public void setOnAcceptClickListener(OnActionClickListener<GroupApplicationInfo> listener) {
        this.onAcceptClickListener = listener;
    }

    /**
     * Sets the reject button click listener.
     *
     * @param listener the click event listener
     */
    public void setOnRejectClickListener(OnActionClickListener<GroupApplicationInfo> listener) {
        this.onRejectClickListener = listener;
    }

    public void setData(List<GroupApplicationInfo> newData) {
        if (newData != null) {
            data.clear();
            data.addAll(newData);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.nc_item_group_application, parent, false);
        return new GroupApplicationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupApplicationViewHolder holder, int position) {
        GroupApplicationInfo groupApplicationInfo = data.get(position);
        NCChatUIConfig.featureConfig()
                .getChatUIImageEngine()
                .loadUserPortrait(
                        holder.getContext(), getPortraitUri(groupApplicationInfo), holder.ivHead);

        // Set title and content
        holder.tvTitle.setText(
                getTitleFromApplicationInfo(groupApplicationInfo, holder.tvTitle.getContext()));

        String groupId = groupApplicationInfo.getGroupId();
        // Load group name from cache
        if (groupInfoCacheMap.containsKey(groupId)) {
            holder.tvContent.setText(groupInfoCacheMap.get(groupId).getGroupName());
        } else {
            holder.tvContent.setText(""); // Clear old content to avoid recycling issues
            new GroupInfoLoader(holder, groupId, groupInfoCacheMap).execute();
        }

        // Show buttons based on status
        switch (groupApplicationInfo.getStatus()) {
            case INVITEE_UNHANDLED:
                if (groupApplicationInfo.getDirection()
                        == GroupApplicationDirection.APPLICATION_SENT) {
                    updateButtonVisibility(holder, R.string.nc_manager_pending, true);
                } else if (groupApplicationInfo.getDirection()
                        == GroupApplicationDirection.INVITATION_SENT) {
                    updateButtonVisibility(holder, R.string.nc_invitee_pending, true);
                } else if (groupApplicationInfo.getDirection()
                        == GroupApplicationDirection.APPLICATION_RECEIVED) {
                    updateButtonVisibility(holder, R.string.nc_invitee_pending, true);
                } else {
                    updateButtonVisibility(holder, 0, false);
                }
                break;
            case ADMIN_UNHANDLED:
                if (groupApplicationInfo.getDirection()
                                == GroupApplicationDirection.APPLICATION_SENT
                        || groupApplicationInfo.getDirection()
                                == GroupApplicationDirection.INVITATION_SENT) {
                    updateButtonVisibility(holder, R.string.nc_manager_pending, true);
                } else {
                    updateButtonVisibility(holder, 0, false);
                }
                break;
            case ADMIN_REFUSED:
                updateButtonVisibility(holder, R.string.nc_manager_rejected, true);
                break;
            case INVITEE_REFUSED:
                updateButtonVisibility(holder, R.string.nc_invitee_rejected, true);
                break;
            case JOINED:
                updateButtonVisibility(holder, R.string.nc_joined, true);
                break;
            case EXPIRED:
                updateButtonVisibility(holder, R.string.nc_expired, true);
                break;
        }

        // Accept button click event
        holder.tvAccept.setOnClickListener(
                v -> {
                    if (onAcceptClickListener != null) {
                        onAcceptClickListener.onActionClickWithConfirm(
                                groupApplicationInfo,
                                result -> {
                                    if (result instanceof Integer) {
                                        int code = (Integer) result;
                                        if (code == 0 || code == 25427) {
                                            @StringRes int resid = R.string.nc_joined;
                                            if (code == 0) {
                                                resid = R.string.nc_joined;
                                            } else if (code == 25427) {
                                                resid = R.string.nc_invitee_pending;
                                            } else {
                                                resid = R.string.nc_set_failed;
                                            }
                                            holder.tvResult.setVisibility(View.VISIBLE);
                                            holder.tvResult.setText(resid);
                                            holder.tvReject.setVisibility(View.GONE);
                                            holder.tvAccept.setVisibility(View.GONE);
                                        }
                                    }
                                });
                    }
                });

        // Reject button click event
        holder.tvReject.setOnClickListener(
                v -> {
                    if (onRejectClickListener != null) {
                        onRejectClickListener.onActionClickWithConfirm(
                                groupApplicationInfo,
                                isSuccess -> {
                                    if (isSuccess instanceof Boolean && (Boolean) isSuccess) {
                                        GroupApplicationStatus status =
                                                groupApplicationInfo.getStatus();
                                        @StringRes int resid = R.string.nc_rejected;
                                        if (status == GroupApplicationStatus.INVITEE_UNHANDLED) {
                                            resid = R.string.nc_invitee_rejected;
                                        } else if (status
                                                == GroupApplicationStatus.ADMIN_UNHANDLED) {
                                            resid = R.string.nc_manager_rejected;
                                        }
                                        holder.tvResult.setVisibility(View.VISIBLE);
                                        holder.tvResult.setText(resid);
                                        holder.tvReject.setVisibility(View.GONE);
                                        holder.tvAccept.setVisibility(View.GONE);
                                    }
                                });
                    }
                });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private void updateButtonVisibility(
            GroupApplicationViewHolder holder,
            @StringRes int resultTextResId,
            boolean isShowResult) {
        if (resultTextResId != 0) {
            holder.tvResult.setText(resultTextResId);
        }
        holder.tvResult.setVisibility(isShowResult ? View.VISIBLE : View.GONE);
        holder.tvReject.setVisibility(isShowResult ? View.GONE : View.VISIBLE);
        holder.tvAccept.setVisibility(isShowResult ? View.GONE : View.VISIBLE);
    }

    private String getPortraitUri(GroupApplicationInfo info) {
        GroupApplicationDirection direction = info.getDirection();
        if (direction == GroupApplicationDirection.INVITATION_RECEIVED
                || direction == GroupApplicationDirection.INVITATION_SENT) {
            return info.getInviterInfo().getPortraitUri();
        }
        if (direction == GroupApplicationDirection.APPLICATION_SENT) {
            return info.getJoinMemberInfo().getPortraitUri();
        }
        return info.getInviterInfo() != null
                ? info.getInviterInfo().getPortraitUri()
                : info.getJoinMemberInfo().getPortraitUri();
    }

    private String getTitleFromApplicationInfo(GroupApplicationInfo info, Context context) {
        GroupApplicationDirection direction = info.getDirection();
        GroupApplicationType type = info.getType();
        String inviterName = getGroupMemberName(info.getInviterInfo());
        String joinMemberName = getGroupMemberName(info.getJoinMemberInfo());

        switch (direction) {
            case INVITATION_RECEIVED:
                return type == GroupApplicationType.INVITATION
                        ? context.getString(
                                R.string.nc_group_invitation_received_inviter, inviterName)
                        : context.getString(
                                R.string.nc_group_application_received_invitation,
                                inviterName,
                                joinMemberName);
            case APPLICATION_RECEIVED:
                return type == GroupApplicationType.INVITATION
                        ? context.getString(
                                R.string.nc_group_application_received_invitation,
                                inviterName,
                                joinMemberName)
                        : context.getString(
                                R.string.nc_group_application_received_request, joinMemberName);
            case APPLICATION_SENT:
                return context.getString(R.string.nc_group_application_sent);
            case INVITATION_SENT:
                return context.getString(R.string.nc_group_invitation_sent, joinMemberName);
            default:
                return "";
        }
    }

    private String getGroupMemberName(GroupMemberInfo info) {
        if (info == null) {
            return "";
        }
        return TextUtils.isEmpty(info.getNickname()) ? info.getName() : info.getNickname();
    }

    static class GroupApplicationViewHolder extends RecyclerView.ViewHolder {
        ImageView ivHead;
        TextView tvTitle, tvContent, tvResult, tvReject, tvAccept;
        LinearLayout llBtn;

        public GroupApplicationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivHead = itemView.findViewById(R.id.iv_head);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvResult = itemView.findViewById(R.id.tv_result);
            tvReject = itemView.findViewById(R.id.tv_reject);
            tvAccept = itemView.findViewById(R.id.tv_accept);
            llBtn = itemView.findViewById(R.id.ll_btn);
        }

        Context getContext() {
            return itemView.getContext();
        }
    }

    private static class GroupInfoLoader {
        private final WeakReference<GroupApplicationViewHolder> weakHolder;
        private final String groupId;
        private final WeakHashMap<String, GroupInfo> cache;

        GroupInfoLoader(
                GroupApplicationViewHolder holder,
                String groupId,
                WeakHashMap<String, GroupInfo> cache) {
            this.weakHolder = new WeakReference<>(holder);
            this.groupId = groupId;
            this.cache = cache;
        }

        void execute() {
            GroupChannel.getGroupsInfo(
                    Arrays.asList(groupId),
                    (groupInfos, error) -> {
                        if (error == null && groupInfos != null && !groupInfos.isEmpty()) {
                            GroupInfo groupInfo = groupInfos.get(0);
                            cache.put(groupId, groupInfo);

                            GroupApplicationViewHolder holder = weakHolder.get();
                            if (holder != null) {
                                holder.tvContent.setText(groupInfo.getGroupName());
                            }
                        } else if (error != null) {
                            RLog.w(
                                    "GroupApplicationsAdapter",
                                    "GroupInfoLoader get group info error: " + error);
                        }
                    });
        }
    }
}
