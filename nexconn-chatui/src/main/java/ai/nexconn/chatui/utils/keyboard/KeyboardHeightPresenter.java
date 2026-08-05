package ai.nexconn.chatui.utils.keyboard;

/**
 * @author gusd
 */
interface KeyboardHeightPresenter {
    void start();

    void stop();

    void setKeyboardHeightObserver(KeyboardHeightObserver observer);
}
