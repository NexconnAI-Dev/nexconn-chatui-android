package ai.nexconn.chatui.channel.event.page;

public class SmoothScrollEvent implements PageEvent {
    private int position;

    public SmoothScrollEvent(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
