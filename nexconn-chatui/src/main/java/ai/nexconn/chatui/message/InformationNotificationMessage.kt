package ai.nexconn.chatui.message

import ai.nexconn.chat.message.MessageContent

/**
 * Information notification message (small gray notification bar).
 *
 * Used for system-level informational notifications within a conversation, such as anti-fraud
 * reminders, membership state tips, or other server-generated conversation notices.
 */
class InformationNotificationMessage(
    val message: String = "",
) : MessageContent() {
    override fun toString(): String = "InformationNotificationMessage(message='$message')"

    companion object {
        @JvmStatic
        fun obtain(message: String): InformationNotificationMessage =
            InformationNotificationMessage(message)
    }
}
