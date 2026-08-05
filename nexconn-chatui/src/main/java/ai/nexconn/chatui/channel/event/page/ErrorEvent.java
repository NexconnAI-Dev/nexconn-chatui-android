package ai.nexconn.chatui.channel.event.page;

import androidx.annotation.NonNull;

/**
 * Page-level error event.
 *
 * @param <E> the error type
 */
public class ErrorEvent<E> implements PageEvent {
    private final E error;
    private final String message;

    /**
     * Creates an error event.
     *
     * @param error the error
     * @param message the error message
     * @param <E> the error type
     * @return the error event
     */
    public static <E> ErrorEvent<E> obtain(@NonNull E error, String message) {
        return new ErrorEvent<>(error, message);
    }

    /**
     * Creates an error event.
     *
     * @param error the error
     * @param <E> the error type
     * @return the error event
     */
    public static <E> ErrorEvent<E> obtain(@NonNull E error) {
        return new ErrorEvent<>(error);
    }

    private ErrorEvent(E error) {
        this.error = error;
        this.message = "";
    }

    private ErrorEvent(E error, String message) {
        this.error = error;
        this.message = message;
    }

    /**
     * Returns the error.
     *
     * @return the error
     */
    public E getError() {
        return error;
    }

    /**
     * Returns the error message.
     *
     * @return the error message
     */
    public String getMessage() {
        return message;
    }
}
