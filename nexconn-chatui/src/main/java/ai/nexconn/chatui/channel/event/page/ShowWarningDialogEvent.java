package ai.nexconn.chatui.channel.event.page;

public class ShowWarningDialogEvent implements PageEvent {
    private String msg;

    public ShowWarningDialogEvent(String msg) {
        this.msg = msg;
    }

    public String getMessage() {
        return msg;
    }
}
