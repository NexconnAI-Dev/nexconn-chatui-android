package ai.nexconn.chatui.base;

import ai.nexconn.chat.error.NCError;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import androidx.annotation.NonNull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Business logic handler with single-type data change callbacks.
 *
 * @since 5.10.4
 */
public abstract class SingleDataHandler<T> extends BaseHandler {

    private final List<OnDataChangeListener<T>> onDataChangeListeners =
            new CopyOnWriteArrayList<>();

    @Override
    public void stop() {
        super.stop();
        this.onDataChangeListeners.clear();
    }

    /**
     * Adds a data change listener.
     *
     * @param onDataChangeListener OnDataChangeListener<T>
     */
    public final void addDataChangeListener(@NonNull OnDataChangeListener<T> onDataChangeListener) {
        if (!this.onDataChangeListeners.contains(onDataChangeListener)) {
            this.onDataChangeListeners.add(onDataChangeListener);
        }
    }

    /**
     * Replaces the data change listener.
     *
     * @param onDataChangeListener OnDataChangeListener<T>
     */
    public final void replaceDataChangeListener(
            @NonNull OnDataChangeListener<T> onDataChangeListener) {
        this.onDataChangeListeners.clear();
        addDataChangeListener(onDataChangeListener);
    }

    /**
     * Notifies data change.
     *
     * @param t the data
     */
    protected final void notifyDataChange(@NonNull T t) {
        if (!isAlive()) {
            return;
        }
        for (OnDataChangeListener<T> listener : onDataChangeListeners) {
            listener.onDataChange(t);
        }
    }

    /**
     * Notifies a data error.
     *
     * @param error the error
     */
    protected final void notifyDataError(@NonNull NCError error) {
        if (!isAlive()) {
            return;
        }
        for (OnDataChangeListener<T> listener : onDataChangeListeners) {
            listener.onDataError(error);
        }
    }
}
