package ai.nexconn.chatui.userinfo.model;

import ai.nexconn.chat.channel.model.GroupInfo;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import androidx.annotation.NonNull;

public class ExtendedGroup implements Parcelable {

    private String groupId;
    private String groupName;
    private String portraitUri;
    private String extra;
    private String remark;
    private GroupInfo groupInfo;

    public static ExtendedGroup obtain(@NonNull GroupInfo groupInfo) {
        ExtendedGroup group = new ExtendedGroup();
        group.groupId = groupInfo.getGroupId();
        group.groupName = groupInfo.getGroupName();
        group.portraitUri = groupInfo.getPortraitUri();
        group.groupInfo = groupInfo;
        return group;
    }

    private ExtendedGroup() {}

    public String getId() {
        return groupId;
    }

    public String getName() {
        if (!TextUtils.isEmpty(remark)) {
            return remark;
        }
        return groupName;
    }

    public Uri getPortraitUri() {
        return portraitUri != null ? Uri.parse(portraitUri) : null;
    }

    public String getPortraitUriStr() {
        return portraitUri;
    }

    public String getExtra() {
        return extra;
    }

    public GroupInfo getGroupInfo() {
        return groupInfo;
    }

    public GroupInfo toGroupInfo() {
        if (groupInfo != null) {
            return groupInfo;
        }
        return new GroupInfo(groupId, groupName, portraitUri);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(groupId);
        dest.writeString(groupName);
        dest.writeString(portraitUri);
        dest.writeString(extra);
        dest.writeString(remark);
    }

    protected ExtendedGroup(Parcel in) {
        groupId = in.readString();
        groupName = in.readString();
        portraitUri = in.readString();
        extra = in.readString();
        remark = in.readString();
        groupInfo = new GroupInfo(groupId, groupName, portraitUri);
    }

    public static final Creator<ExtendedGroup> CREATOR =
            new Creator<ExtendedGroup>() {
                @Override
                public ExtendedGroup createFromParcel(Parcel in) {
                    return new ExtendedGroup(in);
                }

                @Override
                public ExtendedGroup[] newArray(int size) {
                    return new ExtendedGroup[size];
                }
            };

    @Override
    public String toString() {
        return "ExtendedGroup{"
                + "groupId='"
                + groupId
                + '\''
                + ", groupName='"
                + groupName
                + '\''
                + ", portraitUri='"
                + portraitUri
                + '\''
                + ", extra='"
                + extra
                + '\''
                + '}';
    }
}
