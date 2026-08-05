package ai.nexconn.chatui.manager.hqvoicemessage;

import ai.nexconn.chat.message.Message;

public class AutoDownloadEntry {
    private Message message;
    private DownloadPriority priority;

    public AutoDownloadEntry(Message message, DownloadPriority priority) {
        this.message = message;
        this.priority = priority;
    }

    /**
     * File download priority
     *
     * @return the current file's download priority
     */
    DownloadPriority getPriority() {
        return priority;
    }

    /**
     * Message
     *
     * @return the currently set message
     */
    public Message getMessage() {
        return message;
    }

    /**
     * Sets the message
     *
     * @param message the message
     */
    public void setMessage(Message message) {
        this.message = message;
    }

    /** Download priority */
    public enum DownloadPriority {
        /** Normal priority */
        NORMAL,
        /** High priority */
        HIGH
    }
}
