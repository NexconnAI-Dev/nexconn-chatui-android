package ai.nexconn.chatui.usermanage.friend.apply;

import ai.nexconn.chat.model.PageResult;
import ai.nexconn.chat.user.model.FriendApplicationDetail;
import ai.nexconn.chat.user.model.FriendApplicationStatus;
import ai.nexconn.chat.user.model.FriendApplicationType;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.model.UiFriendApplicationInfo;
import ai.nexconn.chatui.usermanage.handler.FriendApplicationHandler;
import android.os.Bundle;
import androidx.annotation.StringRes;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;

/**
 * Friend application list page ViewModel
 *
 * @since 5.12.0
 */
public class ApplyFriendViewModel extends BaseViewModel {
    private final MutableLiveData<List<UiFriendApplicationInfo>> mLivedata =
            new MutableLiveData<>();
    private final FriendApplicationHandler friendApplicationHandler;
    private FriendApplicationType[] types = {
        FriendApplicationType.RECEIVED, FriendApplicationType.SENT
    };

    FriendApplicationStatus[] status = {
        FriendApplicationStatus.UN_HANDLED,
        FriendApplicationStatus.EXPIRED,
        FriendApplicationStatus.REFUSED,
        FriendApplicationStatus.ACCEPTED
    };
    private List<UiFriendApplicationInfo> mData = new ArrayList<>();

    private volatile boolean onLoad = false;

    public ApplyFriendViewModel(Bundle bundle) {
        super(bundle);
        friendApplicationHandler = new FriendApplicationHandler();
        friendApplicationHandler.addDataChangeListener(
                FriendApplicationHandler.KEY_GET_FRIEND_APPLICATIONS,
                new SafeDataHandler<PageResult>() {
                    @Override
                    public void onDataChange(PageResult data) {
                        List<FriendApplicationDetail> items = data.getData();
                        List<UiFriendApplicationInfo> uiData = new ArrayList<>();
                        for (FriendApplicationDetail item : items) {
                            uiData.add(
                                    new UiFriendApplicationInfo(
                                            item, getTimeLabel(item.getOperationTime())));
                        }
                        onLoad = false;
                        mData.addAll(uiData);
                        mLivedata.postValue(mData);
                    }
                });
    }

    /**
     * Get time label
     *
     * @param timestamp timestamp
     * @return time label string resource
     */
    protected @StringRes int getTimeLabel(long timestamp) {
        long currentTimeMillis = System.currentTimeMillis();
        long result = currentTimeMillis - timestamp;
        if (result <= 86400000) {
            return R.string.nc_just_now;
        } else if (result < 259200000) {
            return R.string.nc_within_three_days;
        } else {
            return R.string.nc_three_days_ago;
        }
    }

    public MutableLiveData<List<UiFriendApplicationInfo>> getFriendApplicationsLiveData() {
        return mLivedata;
    }

    /**
     * @param type 0=all,1=received,2=sent
     */
    public void loadFriendApplications(int type) {
        if (type == 0) {
            types =
                    new FriendApplicationType[] {
                        FriendApplicationType.RECEIVED, FriendApplicationType.SENT
                    };
        } else if (type == 1) {
            types = new FriendApplicationType[] {FriendApplicationType.RECEIVED};
        } else if (type == 2) {
            types = new FriendApplicationType[] {FriendApplicationType.SENT};
        }
        loadFriendApplications(false);
    }

    public void loadFriendApplications(boolean isLoadMore) {
        if (onLoad) {
            return;
        }
        onLoad = true;
        if (!isLoadMore) {
            mData.clear();
        }
        friendApplicationHandler.getFriendApplications(types, status);
    }

    public void acceptFriendApplication(String userId, OnDataChangeListener<Boolean> callback) {
        friendApplicationHandler.acceptFriendApplication(userId, callback);
    }

    public void refuseFriendApplication(String userId, OnDataChangeListener<Boolean> callback) {
        friendApplicationHandler.refuseFriendApplication(userId, callback);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        friendApplicationHandler.stop();
    }
}
