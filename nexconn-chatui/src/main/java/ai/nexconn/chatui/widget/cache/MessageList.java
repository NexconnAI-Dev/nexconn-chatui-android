package ai.nexconn.chatui.widget.cache;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A list specialized for message display. When the max capacity is reached, inserting at the bottom
 * discards top items, while inserting at the top retains bottom items regardless of the max.
 *
 * @param <T> element type
 */
public class MessageList<T> extends ArrayList<T> {
    /** Maximum list capacity */
    private int mMaxCount;

    public MessageList(int maxCount, int initialCapacity) {
        super(initialCapacity);
        mMaxCount = maxCount;
    }

    public MessageList(int maxCount) {
        mMaxCount = maxCount;
    }

    public MessageList(int maxCount, @NonNull Collection<? extends T> c) {
        super(c);
        mMaxCount = maxCount;
    }

    @Override
    public boolean add(T t) {
        boolean result = super.add(t);
        int overCount = size() - mMaxCount;
        // Remove excess items from the top if over capacity
        if (overCount > 0) {
            removeRange(0, overCount);
        }
        return result;
    }

    @Override
    public void add(int index, T element) {
        super.add(index, element);
        int overCount = size() - mMaxCount;
        // Remove excess items if over capacity
        if (overCount > 0) {
            // Use median: if index > median, remove from the beginning; otherwise keep
            if (index > mMaxCount / 2) {
                removeRange(0, overCount);
            }
        }
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> c) {
        boolean result = super.addAll(c);
        int overCount = size() - mMaxCount;
        // Remove excess items from the top if over capacity
        if (overCount > 0) {
            removeRange(0, overCount);
        }
        return result;
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends T> c) {
        boolean result = super.addAll(index, c);
        int overCount = size() - mMaxCount;
        // Remove excess items if over capacity
        if (overCount > 0) {
            // Use median: if index > median, remove from the beginning; otherwise keep
            if (index > mMaxCount / 2) {
                removeRange(0, overCount);
            }
        }
        return result;
    }
}
