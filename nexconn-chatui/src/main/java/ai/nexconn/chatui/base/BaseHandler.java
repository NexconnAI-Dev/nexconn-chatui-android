package ai.nexconn.chatui.base;

/**
 * Business logic handler with lifecycle management.
 *
 * @since 5.10.4
 */
public abstract class BaseHandler {

    private volatile boolean isAlive = true;

    /** Stops the handler. */
    public void stop() {
        this.isAlive = false;
    }

    /**
     * Checks whether the handler is alive.
     *
     * @return true if alive
     */
    protected final boolean isAlive() {
        return isAlive;
    }
}
