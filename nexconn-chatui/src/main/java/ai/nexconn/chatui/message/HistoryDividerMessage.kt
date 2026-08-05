package ai.nexconn.chatui.message

import ai.nexconn.chat.message.MessageContent

/**
 * History divider message.
 *
 * A local-only UI message used to visually separate read messages from unread messages in the
 * conversation message list. Never persisted or sent to the server.
 */
class HistoryDividerMessage(
    val content: String = "",
) : MessageContent() {
    override fun toString(): String = "HistoryDividerMessage(content='$content')"

    companion object {
        @JvmStatic fun obtain(content: String): HistoryDividerMessage = HistoryDividerMessage(content)
    }
}
