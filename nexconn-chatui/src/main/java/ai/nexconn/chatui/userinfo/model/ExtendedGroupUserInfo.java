package ai.nexconn.chatui.userinfo.model;

import ai.nexconn.chat.channel.model.GroupMemberInfo;
import android.os.Parcel;
import android.os.Parcelable;

public class ExtendedGroupUserInfo extends GroupUserInfo implements Parcelable {

    private GroupMemberInfo groupMemberInfo;

    public static ExtendedGroupUserInfo obtain(GroupUserInfo groupUserInfo) {
        return new ExtendedGroupUserInfo(groupUserInfo, null);
    }

    public static ExtendedGroupUserInfo obtain(GroupMemberInfo groupMemberInfo) {
        return new ExtendedGroupUserInfo(null, groupMemberInfo);
    }

    private ExtendedGroupUserInfo(GroupUserInfo groupUserInfo, GroupMemberInfo groupMemberInfo) {
        super(
                groupUserInfo != null ? groupUserInfo.getGroupId() : "",
                groupUserInfo != null
                        ? groupUserInfo.getUserId()
                        : (groupMemberInfo != null ? groupMemberInfo.getUserId() : ""),
                groupUserInfo != null
                        ? groupUserInfo.getNickname()
                        : (groupMemberInfo != null ? groupMemberInfo.getNickname() : ""),
                groupUserInfo != null
                        ? groupUserInfo.getExtra()
                        : (groupMemberInfo != null ? groupMemberInfo.getExtra() : ""));

        if (groupMemberInfo != null) {
            this.groupMemberInfo = groupMemberInfo;
        } else {
            String uid = groupUserInfo != null ? groupUserInfo.getUserId() : "";
            String nickname = groupUserInfo != null ? groupUserInfo.getNickname() : null;
            String extra = groupUserInfo != null ? groupUserInfo.getExtra() : null;
            this.groupMemberInfo =
                    new GroupMemberInfo(uid, null, null, nickname, extra, null, 0, false);
        }
    }

    public GroupMemberInfo getGroupMemberInfo() {
        return groupMemberInfo;
    }

    public GroupMemberInfo toGroupMemberInfo() {
        return new GroupMemberInfo(
                getUserId(),
                groupMemberInfo.getName(),
                groupMemberInfo.getPortraitUri(),
                getNickname(),
                getExtra(),
                groupMemberInfo.getRole(),
                groupMemberInfo.getJoinedTime(),
                groupMemberInfo.isRobot());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(groupMemberInfo.getUserId());
        dest.writeString(groupMemberInfo.getName());
        dest.writeString(groupMemberInfo.getPortraitUri());
        dest.writeString(groupMemberInfo.getNickname());
        dest.writeString(groupMemberInfo.getExtra());
        dest.writeLong(groupMemberInfo.getJoinedTime());
        dest.writeInt(groupMemberInfo.isRobot() ? 1 : 0);
    }

    protected ExtendedGroupUserInfo(Parcel in) {
        super(in.readString(), in.readString(), in.readString(), in.readString());
        String memberId = in.readString();
        String memberName = in.readString();
        String memberPortrait = in.readString();
        String memberNickname = in.readString();
        String memberExtra = in.readString();
        long joinedTime = in.readLong();
        boolean isRobot = in.readInt() == 1;
        groupMemberInfo =
                new GroupMemberInfo(
                        memberId != null ? memberId : "",
                        memberName,
                        memberPortrait,
                        memberNickname,
                        memberExtra,
                        null,
                        joinedTime,
                        isRobot);
    }

    public static final Creator<ExtendedGroupUserInfo> CREATOR =
            new Creator<ExtendedGroupUserInfo>() {
                @Override
                public ExtendedGroupUserInfo createFromParcel(Parcel in) {
                    return new ExtendedGroupUserInfo(in);
                }

                @Override
                public ExtendedGroupUserInfo[] newArray(int size) {
                    return new ExtendedGroupUserInfo[size];
                }
            };

    @Override
    public String toString() {
        return "ExtendedGroupUserInfo{"
                + "groupMemberInfo="
                + groupMemberInfo
                + ", groupId='"
                + getGroupId()
                + '\''
                + ", userId='"
                + getUserId()
                + '\''
                + ", nickname='"
                + getNickname()
                + '\''
                + ", extra='"
                + getExtra()
                + '\''
                + '}';
    }
}
