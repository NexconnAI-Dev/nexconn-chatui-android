package ai.nexconn.chatui.userinfo.model;

import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chat.user.model.UserProfile;
import ai.nexconn.chat.user.model.UserType;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

public class ExtendedUserInfo implements Parcelable {

    private String userId;
    private String name;
    private String portraitUri;
    private String alias;
    private String extra;
    private UserProfile userProfile;
    private FriendDetail friendDetail;

    public static ExtendedUserInfo obtain(@NonNull UserInfo userInfo) {
        ExtendedUserInfo info = new ExtendedUserInfo();
        info.userId = userInfo.getUserId();
        info.name = userInfo.getName();
        info.portraitUri = userInfo.getPortraitUri();
        info.alias = userInfo.getAlias();
        info.extra = userInfo.getExtra();
        info.userProfile =
                new UserProfile(
                        info.userId,
                        info.name,
                        info.portraitUri,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        info.friendDetail =
                new FriendDetail(info.userId, info.name, info.portraitUri, null, null, 0);
        return info;
    }

    public static ExtendedUserInfo obtain(@NonNull UserProfile userProfile) {
        ExtendedUserInfo info = new ExtendedUserInfo();
        info.userId = userProfile.getUserId();
        info.name = userProfile.getName();
        info.portraitUri = userProfile.getPortraitUri();
        info.userProfile = userProfile;
        info.friendDetail =
                new FriendDetail(info.userId, info.name, info.portraitUri, null, null, 0);
        return info;
    }

    public static ExtendedUserInfo obtain(@NonNull FriendDetail friendDetail) {
        ExtendedUserInfo info = new ExtendedUserInfo();
        info.userId = friendDetail.getUserId();
        info.name = friendDetail.getName();
        info.portraitUri = friendDetail.getPortraitUri();
        if (friendDetail.getRemark() != null) {
            info.alias = friendDetail.getRemark();
        }
        info.userProfile =
                new UserProfile(
                        info.userId,
                        info.name,
                        info.portraitUri,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        info.friendDetail = friendDetail;
        return info;
    }

    private ExtendedUserInfo() {}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPortraitUri() {
        return portraitUri;
    }

    public void setPortraitUri(String portraitUri) {
        this.portraitUri = portraitUri;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public FriendDetail getFriendDetail() {
        return friendDetail;
    }

    public void setFriendDetail(FriendDetail friendDetail) {
        this.friendDetail = friendDetail;
    }

    public UserInfo toUserInfo() {
        return new UserInfo(userId, UserType.NORMAL, name, portraitUri, alias, extra);
    }

    public UserProfile toUserProfile() {
        return new UserProfile(
                userId,
                name,
                portraitUri,
                userProfile != null ? userProfile.getUniqueId() : null,
                userProfile != null ? userProfile.getEmail() : null,
                userProfile != null ? userProfile.getBirthday() : null,
                userProfile != null ? userProfile.getGender() : null,
                userProfile != null ? userProfile.getLocation() : null,
                userProfile != null ? userProfile.getRole() : null,
                userProfile != null ? userProfile.getLevel() : null,
                userProfile != null ? userProfile.getExtProfile() : null);
    }

    public FriendDetail toFriendDetail() {
        return new FriendDetail(
                userId,
                name,
                portraitUri,
                friendDetail != null ? friendDetail.getRemark() : null,
                friendDetail != null ? friendDetail.getExtProfile() : null,
                friendDetail != null ? friendDetail.getAddTime() : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeString(name);
        dest.writeString(portraitUri);
        dest.writeString(alias);
        dest.writeString(extra);
    }

    protected ExtendedUserInfo(Parcel in) {
        userId = in.readString();
        name = in.readString();
        portraitUri = in.readString();
        alias = in.readString();
        extra = in.readString();
        userProfile =
                new UserProfile(
                        userId, name, portraitUri, null, null, null, null, null, null, null, null);
        friendDetail = new FriendDetail(userId, name, portraitUri, null, null, 0);
    }

    public static final Creator<ExtendedUserInfo> CREATOR =
            new Creator<ExtendedUserInfo>() {
                @Override
                public ExtendedUserInfo createFromParcel(Parcel in) {
                    return new ExtendedUserInfo(in);
                }

                @Override
                public ExtendedUserInfo[] newArray(int size) {
                    return new ExtendedUserInfo[size];
                }
            };

    @Override
    public String toString() {
        return "ExtendedUserInfo{"
                + "userId='"
                + userId
                + '\''
                + ", name='"
                + name
                + '\''
                + ", alias='"
                + alias
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
