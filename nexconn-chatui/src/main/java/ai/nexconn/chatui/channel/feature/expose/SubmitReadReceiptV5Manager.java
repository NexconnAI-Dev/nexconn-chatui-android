package ai.nexconn.chatui.channel.feature.expose;

import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.DirectChannel;
import ai.nexconn.chat.channel.GroupChannel;
import ai.nexconn.chat.channel.OpenChannel;
import ai.nexconn.chat.channel.SystemChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.ErrorHandler;
import java.util.List;

/** Batch submit manager for read receipt V5. */
public class SubmitReadReceiptV5Manager extends ExposeBatchSubmitManager<String> {
    private ChannelIdentifier mId;

    /** Constructor. */
    public SubmitReadReceiptV5Manager() {
        super();
    }

    public void bindConversation(ChannelIdentifier id) {
        this.mId = id;
    }

    @Override
    public void addSubmitTask(String item) {
        if (mId == null) {
            return;
        }
        super.addSubmitTask(item);
    }

    private static BaseChannel createChannel(ChannelIdentifier id) {
        if (id.getChannelType() == ChannelType.GROUP) return new GroupChannel(id.getChannelId());
        if (id.getChannelType() == ChannelType.OPEN) return new OpenChannel(id.getChannelId());
        if (id.getChannelType() == ChannelType.SYSTEM) return new SystemChannel(id.getChannelId());
        return new DirectChannel(id.getChannelId());
    }

    @Override
    void onBatchSubmit(List<String> items, BatchResultCallback callback) {
        if (mId == null) {
            callback.onResult(null, false);
            return;
        }
        createChannel(mId)
                .sendReadReceiptResponse(
                        items,
                        new ErrorHandler() {
                            @Override
                            public void onError(NCError error) {
                                if (error == null) {
                                    callback.onResult(null, false);
                                } else {
                                    if (error.getCode()
                                            == 34029) { // MESSAGE_READ_RECEIPT_NOT_SUPPORT
                                        callback.onResult(error, false);
                                    } else {
                                        callback.onResult(error, true);
                                    }
                                }
                            }
                        });
    }
}
