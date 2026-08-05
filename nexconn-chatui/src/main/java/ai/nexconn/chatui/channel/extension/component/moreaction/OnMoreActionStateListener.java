package ai.nexconn.chatui.channel.extension.component.moreaction;

public interface OnMoreActionStateListener {
    /** Entered multi-select mode. */
    void onShownMoreActionLayout();

    /** Exited multi-select mode. */
    void onHiddenMoreActionLayout();
}
