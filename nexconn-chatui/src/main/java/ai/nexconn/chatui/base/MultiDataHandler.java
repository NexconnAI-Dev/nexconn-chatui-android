package ai.nexconn.chatui.base;

import ai.nexconn.chat.error.NCError;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Business logic handler with multi-type data change callbacks.
 *
 * @since 5.10.4
 */
public abstract class MultiDataHandler extends BaseHandler {

    private final Map<DataKey<?>, List<OnDataChangeListener<?>>> listenersMap =
            new ConcurrentHashMap<>();

    @Override
    public void stop() {
        super.stop();
        this.listenersMap.clear();
    }

    /**
     * Adds a data change listener.
     *
     * @param dataKey the key containing the identifier and corresponding Class type
     * @param onDataChangeListener the data change listener
     * @param <T> the data type
     */
    public final <T> void addDataChangeListener(
            @NonNull DataKey<T> dataKey, @NonNull OnDataChangeListener<T> onDataChangeListener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            listenersMap
                    .computeIfAbsent(dataKey, k -> new CopyOnWriteArrayList<>())
                    .add(onDataChangeListener);
        } else {
            List<OnDataChangeListener<?>> listeners = listenersMap.get(dataKey);
            if (listeners == null) {
                listeners = new CopyOnWriteArrayList<>();
                listenersMap.put(dataKey, listeners);
            }
            listeners.add(onDataChangeListener);
        }
    }

    /**
     * Replaces the data change listener.
     *
     * @param dataKey the key containing the identifier and corresponding Class type
     * @param onDataChangeListener the data change listener
     * @param <T> the data type
     */
    public final <T> void replaceDataChangeListener(
            @NonNull DataKey<T> dataKey, @NonNull OnDataChangeListener<T> onDataChangeListener) {
        List<OnDataChangeListener<?>> listeners = listenersMap.get(dataKey);
        if (listeners != null) {
            listeners.clear();
        }
        addDataChangeListener(dataKey, onDataChangeListener);
    }

    /**
     * Notifies data change for the specified type.
     *
     * @param dataKey the key containing the identifier and corresponding Class type
     * @param data the data
     */
    protected final <T> void notifyDataChange(@NonNull DataKey<T> dataKey, @NonNull T data) {
        if (!isAlive()) {
            return;
        }
        List<OnDataChangeListener<?>> listeners = listenersMap.get(dataKey);
        if (listeners != null) {
            for (OnDataChangeListener<?> listener : listeners) {
                try {
                    ((OnDataChangeListener<T>) listener).onDataChange(data);
                } catch (ClassCastException e) {
                    Log.e("MultiDataHandler", "notifyDataChange: ", e);
                    throw e;
                }
            }
        }
    }

    /**
     * Notifies a data error for the specified type.
     *
     * @param dataKey the key containing the identifier and corresponding Class type
     * @param error the error
     */
    protected final <T> void notifyDataError(@NonNull DataKey<T> dataKey, @NonNull NCError error) {
        if (!isAlive()) {
            return;
        }
        List<OnDataChangeListener<?>> listeners = listenersMap.get(dataKey);
        if (listeners != null) {
            for (OnDataChangeListener<?> listener : listeners) {
                try {
                    listener.onDataError(error);
                } catch (ClassCastException e) {
                    Log.e("MultiDataHandler", "notifyDataError: ", e);
                    throw e;
                }
            }
        }
    }

    /**
     * Notifies a data error for the specified type with error key list.
     *
     * @param dataKey the key containing the identifier and corresponding Class type
     * @param error the error
     * @param errorKeys the list of error keys
     */
    protected final <T> void notifyDataError(
            @NonNull DataKey<T> dataKey, @NonNull NCError error, @NonNull List<String> errorKeys) {
        if (!isAlive()) {
            return;
        }
        List<OnDataChangeListener<?>> listeners = listenersMap.get(dataKey);
        if (listeners != null) {
            for (OnDataChangeListener<?> listener : listeners) {
                try {
                    if (listener instanceof OnDataChangeEnhancedListener) {
                        ((OnDataChangeEnhancedListener) listener).onDataError(error, errorKeys);
                    } else {
                        listener.onDataError(error);
                    }
                } catch (ClassCastException e) {
                    Log.e("MultiDataHandler", "notifyDataErrors: ", e);
                    throw e;
                }
            }
        }
    }

    // Static inner class DataKey wrapping a key string and its Class type
    protected static class DataKey<T> {

        private final String key;
        private final Class<T> type;

        public static <T> DataKey<T> obtain(String key, Class<T> type) {
            return new DataKey<>(key, type);
        }

        private DataKey(@NonNull String key, @NonNull Class<T> type) {
            this.key = key;
            this.type = type;
        }

        @NonNull
        public String getKey() {
            return key;
        }

        @NonNull
        public Class<T> getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DataKey<?> that = (DataKey<?>) o;
            return key.equals(that.key) && type.equals(that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, type);
        }
    }
}
