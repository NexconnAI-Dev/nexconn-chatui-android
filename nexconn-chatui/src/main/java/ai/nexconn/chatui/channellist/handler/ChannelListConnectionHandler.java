package ai.nexconn.chatui.channellist.handler;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.handler.ConnectionStatusHandler;
import ai.nexconn.chat.model.ConnectionStatus;
import ai.nexconn.chatui.base.MultiDataHandler;

/**
 * General connection status handler
 *
 * <p>Only listens for connection status changes and notifies externally via DataKey; contains no UI
 * logic. UI presentation (e.g. NoticeContent) is handled by the ViewModel or Fragment layer.
 *
 * @since 5.10.4
 */
public class ChannelListConnectionHandler extends MultiDataHandler {

    private final String mHandlerId = "ChannelListConnectionHandler_" + hashCode();

    public static final DataKey<ConnectionStatus> KEY_CONNECTION_STATUS =
            DataKey.obtain("KEY_CONNECTION_STATUS", ConnectionStatus.class);

    private ConnectionStatus preConnectionStatus;

    private final ConnectionStatusHandler connectionStatusHandler =
            event -> {
                ConnectionStatus status = event.getStatus();
                if (status != preConnectionStatus) {
                    preConnectionStatus = status;
                    notifyDataChange(KEY_CONNECTION_STATUS, status);
                }
            };

    public ChannelListConnectionHandler() {
        NCEngine.addConnectionStatusHandler(mHandlerId, connectionStatusHandler);
        ConnectionStatus current = NCEngine.getConnectionStatus();
        if (current != null) {
            preConnectionStatus = current;
            notifyDataChange(KEY_CONNECTION_STATUS, current);
        }
    }

    @Override
    public void stop() {
        super.stop();
        NCEngine.removeConnectionStatusHandler(mHandlerId);
    }
}
