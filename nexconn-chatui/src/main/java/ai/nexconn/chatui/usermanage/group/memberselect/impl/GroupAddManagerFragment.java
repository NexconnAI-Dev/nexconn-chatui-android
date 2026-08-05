package ai.nexconn.chatui.usermanage.group.memberselect.impl;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.interfaces.OnActionClickListener;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.usermanage.group.memberselect.GroupMemberSelectionFragment;
import ai.nexconn.chatui.usermanage.group.memberselect.GroupMemberSelectionViewModel;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.widget.Toast;
import androidx.annotation.NonNull;
import java.util.List;

/**
 * Fragment for adding group managers.
 *
 * @since 5.12.2
 */
public class GroupAddManagerFragment extends GroupMemberSelectionFragment {

    @Override
    protected void handleContactSelection(
            @NonNull GroupMemberSelectionViewModel viewModel,
            ContactModel contactModel,
            OnActionClickListener.OnConfirmClickListener<Boolean> listener) {
        int maxCount = getArguments().getInt(ChatUIConstants.KEY_MAX_SELECT_COUNT, 10);
        if (contactModel.getCheckType() != ContactModel.CheckType.DISABLE) {
            List<ContactModel> contactModelList =
                    viewModel.getSelectedContactsLiveData().getValue();
            ContactModel.CheckType newCheckType = contactModel.getCheckType();
            if (newCheckType == ContactModel.CheckType.UNCHECKED
                    && contactModelList != null
                    && contactModelList.size() >= maxCount) {
                ToastUtils.show(
                        getContext(),
                        getString(R.string.nc_max_group_members_selection, maxCount),
                        Toast.LENGTH_SHORT);
                return;
            }
        }
        super.handleContactSelection(viewModel, contactModel, listener);
    }

    @Override
    protected void handleConfirmSelection(
            @NonNull GroupMemberSelectionViewModel viewModel,
            ChannelIdentifier conversationIdentifier,
            List<GroupMemberInfo> selectGroupMemberInfoList) {
        viewModel.addGroupManagers(
                isSuccess ->
                        onAddGroupManagersResult(
                                conversationIdentifier.getChannelId(),
                                selectGroupMemberInfoList,
                                isSuccess));
    }

    /**
     * Handle add group managers result.
     *
     * @param groupId group ID
     * @param selectGroupMemberInfoList selected group manager list
     * @param isSuccess whether the operation succeeded
     */
    protected void onAddGroupManagersResult(
            String groupId, List<GroupMemberInfo> selectGroupMemberInfoList, boolean isSuccess) {
        if (isSuccess) {
            ToastUtils.show(getActivity(), getString(R.string.nc_add_success), Toast.LENGTH_SHORT);
            finishActivity();
        } else {
            ToastUtils.show(getActivity(), getString(R.string.nc_add_failed), Toast.LENGTH_SHORT);
        }
    }
}
