package ai.nexconn.chatui.channel.extension.component.emoticon;

public interface IEmojiItemClickListener {
    /**
     * Emoji click event.
     *
     * @param emoji Emoji.
     */
    void onEmojiClick(String emoji);

    /** Emoji delete callback. */
    void onDeleteClick();
}
