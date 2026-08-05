package ai.nexconn.chatui.channel.messagelist.provider;

import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.widget.adapter.IViewProvider;

public interface IMessageProvider<T extends MessageContent>
        extends IChannelSummaryProvider<T>, IViewProvider<UiMessage> {
    // default implementation ignored
}
