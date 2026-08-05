package ai.nexconn.chatui.model;

public interface ResultCallback<T> {
    void onSuccess(T t);

    void onError(ChatUIErrorCode errorCode);
}
