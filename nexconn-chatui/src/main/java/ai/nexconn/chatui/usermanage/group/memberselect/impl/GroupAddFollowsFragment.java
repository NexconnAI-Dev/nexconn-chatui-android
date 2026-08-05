package ai.nexconn.chatui.usermanage.group.memberselect.impl;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.usermanage.group.memberselect.GroupMemberSelectionFragment;
import ai.nexconn.chatui.usermanage.group.memberselect.GroupMemberSelectionViewModel;
import ai.nexconn.chatui.utils.common.ToastUtils;
import android.widget.Toast;
import androidx.annotation.NonNull;
import java.util.List;

/**
 * Fragment for selecting group contacts to add as follows.
 *
 * @since 5.12.2
 */
public class GroupAddFollowsFragment extends GroupMemberSelectionFragment {

    @Override
    protected void handleConfirmSelection(
            @NonNull GroupMemberSelectionViewModel viewModel,
            ChannelIdentifier conversationIdentifier,
            List<GroupMemberInfo> selectGroupMemberInfoList) {
        viewModel.addGroupFollows(
                isSuccess -> {
                    ToastUtils.show(
                            getActivity(),
                            getString(isSuccess ? R.string.nc_add_success : R.string.nc_add_failed),
                            Toast.LENGTH_SHORT);
                    if (isSuccess) {
                        finishActivity();
                    }
                });
    }
}
