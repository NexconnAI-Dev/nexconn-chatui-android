package ai.nexconn.chatui.channel.event.page;

public class NewMessageBarEvent implements PageEvent {
    private int count;

    public NewMessageBarEvent(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}
