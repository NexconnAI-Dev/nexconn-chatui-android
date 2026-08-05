package ai.nexconn.chatui.channel.event;

import ai.nexconn.chatui.channel.event.page.PageEvent;
import ai.nexconn.chatui.widget.pullrefresh.constant.RefreshState;

public class Event {
    public static class RefreshEvent implements PageEvent {
        public RefreshState state;

        public RefreshEvent(RefreshState state) {
            this.state = state;
        }
    }
}
