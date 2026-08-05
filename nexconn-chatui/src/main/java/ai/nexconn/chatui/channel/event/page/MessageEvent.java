package ai.nexconn.chatui.channel.event.page;

/** Page-level message event. */
public class MessageEvent implements PageEvent {

    private final boolean isHasMoreMsg;

    public MessageEvent(boolean isHasMoreMsg) {
        this.isHasMoreMsg = isHasMoreMsg;
    }

    public boolean isHasMoreMsg() {
        return isHasMoreMsg;
    }
}
