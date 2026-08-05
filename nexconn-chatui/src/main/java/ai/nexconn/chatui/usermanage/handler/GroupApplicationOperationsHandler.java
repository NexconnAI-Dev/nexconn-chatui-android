package ai.nexconn.chatui.usermanage.handler;

import ai.nexconn.chat.channel.GroupChannel;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.ErrorHandler;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.params.AcceptGroupApplicationParams;
import ai.nexconn.chat.params.RefuseGroupApplicationParams;
import ai.nexconn.chat.params.RefuseGroupInviteParams;
import ai.nexconn.chatui.base.MultiDataHandler;

/**
 * Group application operations handler.
 *
 * <p>Note: Call {@link #stop()} to release resources when done.
 *
 * @since 5.12.2
 */
public class GroupApplicationOperationsHandler extends MultiDataHandler {

    /** Key for accepting a group invitation. */
    public static final DataKey<Boolean> KEY_ACCEPT_GROUP_INVITE =
            DataKey.obtain("KEY_ACCEPT_GROUP_INVITE", Boolean.class);

    /** Key for refusing a group invitation. */
    public static final DataKey<Boolean> KEY_REFUSE_GROUP_INVITE =
            DataKey.obtain("KEY_REFUSE_GROUP_INVITE", Boolean.class);

    /** Key for accepting a group application. */
    public static final DataKey<Integer> KEY_ACCEPT_GROUP_APPLICATION =
            DataKey.obtain("KEY_ACCEPT_GROUP_APPLICATION", Integer.class);

    /** Key for refusing a group application. */
    public static final DataKey<Boolean> KEY_REFUSE_GROUP_APPLICATION =
            DataKey.obtain("KEY_REFUSE_GROUP_APPLICATION", Boolean.class);

    public GroupApplicationOperationsHandler() {}

    /**
     * Accepts a group invitation.
     *
     * @param groupId the group ID
     * @param inviterId the inviter's user ID
     */
    public void acceptGroupInvite(String groupId, String inviterId) {
        new GroupChannel(groupId)
                .acceptInvite(
                        inviterId,
                        new ErrorHandler() {
                            @Override
                            public void onError(NCError error) {
                                if (error == null) {
                                    notifyDataChange(KEY_ACCEPT_GROUP_INVITE, true);
                                } else {
                                    notifyDataChange(KEY_ACCEPT_GROUP_INVITE, false);
                                    notifyDataError(KEY_ACCEPT_GROUP_INVITE, error);
                                }
                            }
                        });
    }

    /**
     * Refuses a group invitation.
     *
     * @param groupId the group ID
     * @param inviterId the inviter's user ID
     * @param reason the reason for refusal
     */
    public void refuseGroupInvite(String groupId, String inviterId, String reason) {
        RefuseGroupInviteParams params = new RefuseGroupInviteParams(inviterId);
        params.setReason(reason);
        new GroupChannel(groupId)
                .refuseInvite(
                        params,
                        new ErrorHandler() {
                            @Override
                            public void onError(NCError error) {
                                if (error == null) {
                                    notifyDataChange(KEY_REFUSE_GROUP_INVITE, true);
                                } else {
                                    notifyDataChange(KEY_REFUSE_GROUP_INVITE, false);
                                    notifyDataError(KEY_REFUSE_GROUP_INVITE, error);
                                }
                            }
                        });
    }

    /**
     * Accepts a group application.
     *
     * @param groupId the group ID
     * @param inviterId the inviter's user ID
     * @param applicantId the applicant's user ID
     */
    public void acceptGroupApplication(String groupId, String inviterId, String applicantId) {
        AcceptGroupApplicationParams params = new AcceptGroupApplicationParams(applicantId);
        params.setInviterId(inviterId);
        new GroupChannel(groupId)
                .acceptApplication(
                        params,
                        new OperationHandler<Integer>() {
                            @Override
                            public void onResult(Integer resultCode, NCError error) {
                                if (error == null) {
                                    notifyDataChange(KEY_ACCEPT_GROUP_APPLICATION, resultCode);
                                } else {
                                    notifyDataError(KEY_ACCEPT_GROUP_APPLICATION, error);
                                }
                            }
                        });
    }

    /**
     * Refuses a group application.
     *
     * @param groupId the group ID
     * @param inviterId the inviter's user ID
     * @param applicantId the applicant's user ID
     * @param reason the reason for refusal
     */
    public void refuseGroupApplication(
            String groupId, String inviterId, String applicantId, String reason) {
        RefuseGroupApplicationParams params = new RefuseGroupApplicationParams(applicantId);
        params.setInviterId(inviterId);
        params.setReason(reason);
        new GroupChannel(groupId)
                .refuseApplication(
                        params,
                        new ErrorHandler() {
                            @Override
                            public void onError(NCError error) {
                                if (error == null) {
                                    notifyDataChange(KEY_REFUSE_GROUP_APPLICATION, true);
                                } else {
                                    notifyDataChange(KEY_REFUSE_GROUP_APPLICATION, false);
                                    notifyDataError(KEY_REFUSE_GROUP_APPLICATION, error);
                                }
                            }
                        });
    }
}
