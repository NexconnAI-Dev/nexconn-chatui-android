package ai.nexconn.chatui.manager.hqvoicemessage;

import ai.nexconn.chat.message.Message;

/** Listener for HD voice message download completion. */
public interface HQVoiceDownloadListener {
    /**
     * Called when a HD voice message download completes successfully.
     *
     * @param message The downloaded message
     */
    void onDownloadComplete(Message message);
}
