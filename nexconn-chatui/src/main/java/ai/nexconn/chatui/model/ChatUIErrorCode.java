package ai.nexconn.chatui.model;

public enum ChatUIErrorCode {
    NO_INFO_IN_DB(1000, "no info in db.");

    private int code;
    private String message;

    ChatUIErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
