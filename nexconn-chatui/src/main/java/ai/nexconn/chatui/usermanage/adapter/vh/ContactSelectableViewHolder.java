package ai.nexconn.chatui.usermanage.adapter.vh;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.interfaces.OnActionClickListener;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.handler.AppSettingsHandler;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.model.OnlineStatusFriendInfo;
import ai.nexconn.chatui.usermanage.group.mention.GroupMentionFragment;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.text.TextViewUtils;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.lang.ref.WeakReference;
import java.util.Collections;

public class ContactSelectableViewHolder extends RecyclerView.ViewHolder {

    private final TextView contactNameTextView;
    private final TextView rightText;
    private final ImageView contactPortraitImageView;
    private final ImageView contactSelectImageView;
    private final ImageView rightArrow;
    private final ImageView tvRemove;
    private final View divider;
    private final boolean showSelectButton;
    private final boolean showItemRightArrow;
    private final boolean showItemRightText;
    private final boolean showItemSelectAutoUpdate;
    private final boolean showItemRemoveButton;
    private ContactModel<FriendDetail> data;

    public ContactSelectableViewHolder(
            @NonNull View itemView,
            OnActionClickListener<ContactModel> onItemClickListener,
            OnActionClickListener<ContactModel> onItemRemoveClickListener,
            boolean showSelectButton,
            boolean showItemRightArrow,
            boolean showItemRightText,
            boolean showItemSelectAutoUpdate,
            boolean showItemRemoveButton) {
        super(itemView);
        contactPortraitImageView = itemView.findViewById(R.id.iv_contact_portrait);
        contactNameTextView = itemView.findViewById(R.id.tv_contact_name);
        rightText = itemView.findViewById(R.id.tv_right_text);
        contactSelectImageView = itemView.findViewById(R.id.iv_contact_select);
        rightArrow = itemView.findViewById(R.id.iv_right_arrow);
        tvRemove = itemView.findViewById(R.id.tv_remove);
        divider = itemView.findViewById(R.id.divider);

        this.showSelectButton = showSelectButton;
        this.showItemRightArrow = showItemRightArrow;
        this.showItemRightText = showItemRightText;
        this.showItemSelectAutoUpdate = showItemSelectAutoUpdate;
        this.showItemRemoveButton = showItemRemoveButton;

        itemView.setOnClickListener(
                v -> {
                    if (data != null) {
                        // Update selection state (checked/unchecked)
                        if (showSelectButton && showItemSelectAutoUpdate) {
                            updateCheckType();
                        }
                        if (onItemClickListener != null) {
                            onItemClickListener.onActionClickWithConfirm(
                                    data,
                                    (OnActionClickListener.OnConfirmClickListener<Boolean>)
                                            isUpdate -> {
                                                if (showSelectButton && isUpdate) {
                                                    updateCheckType();
                                                }
                                            });
                        }
                    }
                });

        tvRemove.setOnClickListener(
                v -> {
                    if (onItemRemoveClickListener != null) {
                        onItemRemoveClickListener.onActionClick(data);
                    }
                });
    }

    private void updateCheckType() {
        ContactModel.CheckType checkType = data.getCheckType();
        if (checkType == ContactModel.CheckType.CHECKED
                || checkType == ContactModel.CheckType.UNCHECKED) {
            checkType =
                    (checkType == ContactModel.CheckType.CHECKED)
                            ? ContactModel.CheckType.UNCHECKED
                            : ContactModel.CheckType.CHECKED;
            // Update view
            updateCheck(contactSelectImageView, checkType);
        }
    }

    public void bind(ContactModel contactModel) {
        this.data = contactModel;

        Object contactModelBean = contactModel.getBean();
        String name = "";
        String portraitUrl = null;
        String roleText = "";

        if (contactModelBean instanceof OnlineStatusFriendInfo
                || contactModelBean instanceof FriendDetail) {
            String friendName = "";
            String friendRemark = "";
            String friendPortraitUrl = "";
            if (contactModelBean instanceof OnlineStatusFriendInfo) {
                FriendDetail fd = ((OnlineStatusFriendInfo) contactModelBean).getFriendDetail();
                if (fd != null) {
                    friendName = fd.getName();
                    friendRemark = fd.getRemark();
                    friendPortraitUrl = fd.getPortraitUri();
                }
            } else {
                FriendDetail friendDetail = (FriendDetail) contactModelBean;
                friendName = friendDetail.getName();
                friendRemark = friendDetail.getRemark();
                friendPortraitUrl = friendDetail.getPortraitUri();
            }
            name = !TextUtils.isEmpty(friendRemark) ? friendRemark : friendName;
            portraitUrl = friendPortraitUrl;
            NCChatUIConfig.featureConfig()
                    .getChatUIImageEngine()
                    .loadUserPortrait(
                            contactPortraitImageView.getContext(),
                            portraitUrl,
                            contactPortraitImageView);
        } else if (contactModelBean instanceof GroupMemberInfo) {
            GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contactModelBean;
            name =
                    !TextUtils.isEmpty(groupMemberInfo.getNickname())
                            ? groupMemberInfo.getNickname()
                            : groupMemberInfo.getName();
            // Use WeakReference callback to fetch friend info, avoiding memory leaks
            if (!GroupMentionFragment.class.getSimpleName().equals(contactModel.getExtra())) {
                WeakReference<ContactSelectableViewHolder> holderRef = new WeakReference<>(this);
                NCEngine.INSTANCE
                        .getUserModule()
                        .getFriendsInfo(
                                Collections.singletonList(groupMemberInfo.getUserId()),
                                (friendDetails, error) -> {
                                    ContactSelectableViewHolder viewHolder = holderRef.get();
                                    if (error == null
                                            && viewHolder != null
                                            && friendDetails != null
                                            && !friendDetails.isEmpty()) {
                                        FriendDetail friendDetail = friendDetails.get(0);
                                        if (friendDetail != null
                                                && !TextUtils.isEmpty(friendDetail.getRemark())
                                                && viewHolder.contactNameTextView != null) {
                                            viewHolder.contactNameTextView.setText(
                                                    friendDetail.getRemark());
                                        }
                                    } else if (error != null && viewHolder != null) {
                                        RLog.e(
                                                "SelectableContactViewHolder",
                                                "getFriendsInfo error: " + error.getMessage());
                                    }
                                });
            }

            portraitUrl = groupMemberInfo.getPortraitUri();
            roleText = getRoleText(groupMemberInfo.getRole(), rightText);
            NCChatUIConfig.featureConfig()
                    .getChatUIImageEngine()
                    .loadUserPortrait(
                            contactPortraitImageView.getContext(),
                            portraitUrl,
                            contactPortraitImageView);
        }

        contactNameTextView.setText(name);
        // If OnlineStatusFriendInfo, set the online status
        if (AppSettingsHandler.getInstance().isOnlineStatusEnable()
                && contactModelBean instanceof OnlineStatusFriendInfo) {
            int statusResID =
                    ChatUIThemeManager.getAttrResId(
                            contactNameTextView.getContext(),
                            ((OnlineStatusFriendInfo) contactModelBean).isOnline()
                                    ? R.attr.nc_user_online_status_img
                                    : R.attr.nc_user_offline_status_img);
            TextViewUtils.setCompoundDrawables(contactNameTextView, Gravity.START, statusResID);
        }
        rightText.setText(roleText);
        rightText.setVisibility(showItemRightText ? View.VISIBLE : View.GONE);
        contactSelectImageView.setVisibility(showSelectButton ? View.VISIBLE : View.GONE);

        if (showSelectButton) {
            updateCheck(contactSelectImageView, contactModel.getCheckType());
        }

        rightArrow.setVisibility(showItemRightArrow ? View.VISIBLE : View.GONE);
        tvRemove.setVisibility(showItemRemoveButton ? View.VISIBLE : View.GONE);
    }

    public void setShowItemRemoveButton(boolean isShow) {
        if (tvRemove != null) {
            tvRemove.setVisibility(isShow ? View.VISIBLE : View.GONE);
        }
    }

    public void setDividerVisibility(boolean isVisible) {
        if (divider != null) {
            divider.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    private String getRoleText(GroupMemberRole role, View view) {
        switch (role) {
            case ADMIN:
                return view.getContext().getString(R.string.nc_admin);
            case OWNER:
                return view.getContext().getString(R.string.nc_group_owner);
            default:
                return "";
        }
    }

    private void updateCheck(ImageView checkBox, ContactModel.CheckType checkType) {
        switch (checkType) {
            case NONE:
                checkBox.setVisibility(View.GONE);
                break;
            case UNCHECKED:
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setImageResource(
                        ChatUIThemeManager.getAttrResId(
                                checkBox.getContext(), R.attr.nc_group_member_unselect_img));
                break;
            case CHECKED:
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setImageResource(
                        ChatUIThemeManager.getAttrResId(
                                checkBox.getContext(), R.attr.nc_group_member_select_img));
                break;
            case DISABLE:
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setImageResource(
                        ChatUIThemeManager.getAttrResId(
                                checkBox.getContext(), R.attr.nc_group_member_disable_select_img));
                break;
            default:
                break;
        }
    }
}
