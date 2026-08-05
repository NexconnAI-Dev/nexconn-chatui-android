package ai.nexconn.chatui.userinfo.db.model;

import ai.nexconn.chat.user.model.UserInfo;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user")
public class User {
    @ColumnInfo(name = "id")
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "alias")
    public String alias;

    @ColumnInfo(name = "portraitUri")
    public String portraitUrl;

    @ColumnInfo(name = "extra")
    public String extra;

    public User() {
        // default implementation ignored
    }

    public User(String id, String name, Uri portraitUrl) {
        this.id = id;
        this.name = name;
        if (portraitUrl != null) {
            this.portraitUrl = portraitUrl.toString();
        }
    }

    public User(UserInfo info) {
        this.id = info.getUserId();
        this.name = info.getName();
        this.alias = info.getAlias();
        this.portraitUrl = info.getPortraitUri();
        this.extra = info.getExtra();
    }
}
