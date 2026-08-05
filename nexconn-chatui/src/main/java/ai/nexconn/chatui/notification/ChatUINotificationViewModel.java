package ai.nexconn.chatui.notification;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.ErrorHandler;
import ai.nexconn.chatui.model.OperationResult;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class ChatUINotificationViewModel extends AndroidViewModel {
    MutableLiveData<OperationResult> mOperationResult;
    MutableLiveData<NotificationQuietInfo> mQuietInfoLiveData;

    public ChatUINotificationViewModel(@NonNull Application application) {
        super(application);
        mOperationResult = new MutableLiveData<>();
        mQuietInfoLiveData = new MutableLiveData<>();
    }

    public void setNotificationQuietHours(String startTime, int spanMinutes) {
        ChatUINotificationManager.getInstance()
                .setNotificationQuietHours(
                        startTime,
                        spanMinutes,
                        new ErrorHandler() {
                            @Override
                            public void onError(NCError error) {
                                if (error == null) {
                                    mOperationResult.postValue(
                                            new OperationResult(
                                                    OperationResult.Action
                                                            .SET_NOTIFICATION_QUIET_HOURS,
                                                    OperationResult.SUCCESS));
                                } else {
                                    mOperationResult.postValue(
                                            new OperationResult(
                                                    OperationResult.Action.SET_NOTIFICATION_STATUS,
                                                    error.getCode()));
                                }
                            }
                        });
    }

    /**
     * Gets the notification do-not-disturb time and posts the result to {@link
     * #mQuietInfoLiveData}.
     */
    public void getNotificationQuietHours() {
        NCEngine.getNoDisturbTime(
                (info, error) -> {
                    if (error == null && info != null) {
                        mQuietInfoLiveData.postValue(
                                new NotificationQuietInfo(
                                        info.getStartTime(), info.getSpanMinutes()));
                    }
                });
    }

    public MutableLiveData<OperationResult> getOperationResult() {
        return mOperationResult;
    }

    public MutableLiveData<NotificationQuietInfo> getQuietInfoLiveData() {
        return mQuietInfoLiveData;
    }

    public class NotificationQuietInfo {
        String startTime;
        int spanMinutes;

        public NotificationQuietInfo(String startTime, int spanMinutes) {
            this.startTime = startTime;
            this.spanMinutes = spanMinutes;
        }
    }
}
