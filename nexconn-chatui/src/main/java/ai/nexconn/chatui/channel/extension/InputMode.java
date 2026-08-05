package ai.nexconn.chatui.channel.extension;

public enum InputMode {
    /* Text input mode */
    TextInput,
    /* Voice input mode */
    VoiceInput,
    /* Emoticon input mode, triggered by tapping the smiley icon in the input bar */
    EmoticonMode,
    /* Plugin input mode, triggered by tapping the plus icon in the input bar */
    PluginMode,
    /* More input mode, triggered by tapping "More" in the long-press message popup */
    MoreInputMode,
    /* Voice recognition input mode, triggered by tapping the voice input plugin */
    RecognizeMode,
    /* Quick reply input mode */
    QuickReplyMode,
    /* Normal mode, non-input state */
    NormalMode
}
