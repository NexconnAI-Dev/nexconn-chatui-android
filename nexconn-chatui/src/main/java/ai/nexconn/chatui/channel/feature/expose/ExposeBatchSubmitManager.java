package ai.nexconn.chatui.channel.feature.expose;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.ConnectionStatusHandler;
import ai.nexconn.chat.model.ConnectionStatus;
import ai.nexconn.chatui.utils.log.RLog;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Batch submit manager for batching high-frequency operations to reduce network requests.
 *
 * <p>Features: 1. Debouncing: multiple calls within the specified delay are merged into a single
 * submit. 2. State machine: uses clear state transitions to avoid race conditions. 3. Thread
 * safety: uses a unified state lock for multi-thread safety. 4. Generic support: supports different
 * data types for batch processing. 5. Ordering: uses LinkedHashSet to maintain insertion order. 6.
 * Deduplication: automatically removes duplicate tasks (based on equals and hashCode). 7.
 * Connection-aware: automatically pauses/resumes task processing based on connection status.
 *
 * <p>State machine: IDLE ⇄ ACTIVE
 *
 * <p>State descriptions: - IDLE: idle, no pending data, no scheduled tasks. - ACTIVE: active, has
 * pending data or is currently processing.
 *
 * @param <T> the data type to batch process (must correctly implement equals and hashCode)
 * @since 5.30.0
 */
public abstract class ExposeBatchSubmitManager<T> {
    private static final String TAG = "BatchSubmitManager";
    private static final String HANDLER_ID_PREFIX = "ExposeBatchSubmitManager_";
    private static final int DEFAULT_DELAY_MS = 100;
    private static final int MAX_BATCH_SIZE = 100;

    /** Batch submit state enum. */
    private enum SubmitState {
        IDLE, // Idle: no pending data, no scheduled tasks
        ACTIVE // Active: has pending data or is currently processing
    }

    private final String mHandlerId = HANDLER_ID_PREFIX + System.identityHashCode(this);
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Object mStateLock = new Object();

    private final Set<T> mPendingItems = new LinkedHashSet<>();
    private SubmitState mCurrentState = SubmitState.IDLE;
    private boolean mIsConnected = true;
    private int mDelayMs = DEFAULT_DELAY_MS;
    private Runnable mBatchSubmitRunnable;
    private final ConnectionStatusHandler connectionStatusHandler =
            event -> {
                ConnectionStatus status = event.getStatus();
                synchronized (mStateLock) {
                    mIsConnected = status == ConnectionStatus.CONNECTED;
                    RLog.d(
                            TAG,
                            "IsConnected "
                                    + mIsConnected
                                    + ", pending items: "
                                    + mPendingItems.size());
                    if (mIsConnected) {
                        if (!mPendingItems.isEmpty() && mCurrentState == SubmitState.IDLE) {
                            scheduleDelayedSubmit();
                            mCurrentState = SubmitState.ACTIVE;
                        }
                    }
                }
            };

    /** Batch submit result callback interface. */
    public interface BatchResultCallback {
        /**
         * Submit result.
         *
         * @param error submit result error: null means success, non-null means failure
         * @param refillData whether data needs to be refilled (e.g., for retry on certain failures)
         */
        void onResult(NCError error, boolean refillData);
    }

    /** Constructor. */
    protected ExposeBatchSubmitManager() {
        ConnectionStatus current = NCEngine.getConnectionStatus();
        mIsConnected = current == ConnectionStatus.CONNECTED;
        NCEngine.addConnectionStatusHandler(mHandlerId, connectionStatusHandler);
    }

    /**
     * Triggers a batch submit.
     *
     * @param items the data items
     * @param callback call callback#onResult to notify the manager after business logic completes
     */
    abstract void onBatchSubmit(List<T> items, BatchResultCallback callback);

    /**
     * Adds data to the batch processing queue.
     *
     * @param item the data to add
     */
    public void addSubmitTask(T item) {
        if (item == null) {
            return;
        }
        synchronized (mStateLock) {
            // Add to pending queue (LinkedHashSet maintains insertion order and deduplicates via
            // equals/hashCode)
            boolean added = mPendingItems.add(item);
            if (!added) {
                RLog.d(TAG, "Item already exists in pending queue, skipped");
                return;
            }

            RLog.d(TAG, "Added item to pending queue, total: " + mPendingItems.size());

            // Only schedule a new delayed task if connected and idle
            if (mIsConnected && mCurrentState == SubmitState.IDLE) {
                scheduleDelayedSubmit();
                mCurrentState = SubmitState.ACTIVE;
            }
            // When not connected, only add to queue without starting delayed task
        }
    }

    /** Schedules a delayed submit task. */
    private void scheduleDelayedSubmit() {
        // Cancel previous task (if any)
        if (mBatchSubmitRunnable != null) {
            mHandler.removeCallbacks(mBatchSubmitRunnable);
        }
        // Create new delayed task
        mBatchSubmitRunnable = this::executeBatchSubmit;
        // Execute with delay
        mHandler.postDelayed(mBatchSubmitRunnable, mDelayMs);
    }

    /** Executes the batch submit. */
    private void executeBatchSubmit() {
        final List<T> itemsToSubmit = new ArrayList<>();
        synchronized (mStateLock) {
            if (mPendingItems.isEmpty()) {
                // No pending data, return to idle state
                mCurrentState = SubmitState.IDLE;
                return;
            }

            // Submit at most 100 items per batch; excess remains in the queue
            int count = 0;
            for (T item : mPendingItems) {
                if (count >= MAX_BATCH_SIZE) {
                    break;
                }
                itemsToSubmit.add(item);
                count++;
            }

            // Remove items to be submitted from the pending queue
            mPendingItems.removeAll(itemsToSubmit);

            RLog.d(
                    TAG,
                    "Preparing to submit "
                            + itemsToSubmit.size()
                            + " items, remaining: "
                            + mPendingItems.size());
        }

        if (!itemsToSubmit.isEmpty()) {
            try {
                onBatchSubmit(
                        itemsToSubmit,
                        new BatchResultCallback() {
                            @Override
                            public void onResult(NCError error, boolean refillData) {
                                RLog.d(TAG, "Batch submit result: " + error);
                                // On failure, refill data to the head of the pending queue
                                // (prioritize failed-retry data)
                                if (refillData) {
                                    refillData(itemsToSubmit);
                                }
                                onBatchSubmitComplete();
                            }
                        });
            } catch (Exception e) {
                RLog.e(TAG, "Exception during batch submit: " + e.getMessage());
                // On exception, refill data to the head of the pending queue
                refillData(itemsToSubmit);
                onBatchSubmitComplete();
            }
        } else {
            onBatchSubmitComplete();
        }
    }

    private void refillData(List<T> itemsToSubmit) {
        synchronized (mStateLock) {
            Set<T> newPendingItems = new LinkedHashSet<>(itemsToSubmit);
            newPendingItems.addAll(mPendingItems);
            mPendingItems.clear();
            mPendingItems.addAll(newPendingItems);
            RLog.w(
                    TAG,
                    "refillData "
                            + itemsToSubmit.size()
                            + " items to queue head, total pending: "
                            + mPendingItems.size());
        }
    }

    /** Post-processing after batch submit completes. */
    private void onBatchSubmitComplete() {
        synchronized (mStateLock) {
            if (mPendingItems.isEmpty()) {
                // No new data, return to idle state
                mCurrentState = SubmitState.IDLE;
            } else if (mIsConnected) {
                // Has new data and connected, schedule next round while staying ACTIVE
                scheduleDelayedSubmit();
            } else {
                // Has new data but not connected; return to idle, wait for connection restore
                mCurrentState = SubmitState.IDLE;
                RLog.d(TAG, "Has pending items but not connected, waiting for connection restore");
            }
        }
    }

    /**
     * Releases external resources and cleans up internal state. Stops receiving connection status
     * notifications, cancels pending tasks, and clears the waiting queue. Use when the batch submit
     * functionality needs to be completely stopped.
     *
     * <p>Note: After calling this method, BatchSubmitManager will immediately stop all task
     * processing. All pending data will be cleared and will not be executed.
     */
    public void release() {
        // Remove connection status listener to stop receiving new connection status notifications
        NCEngine.removeConnectionStatusHandler(mHandlerId);

        // If connected, do not clear the cache queue
        if (mIsConnected) {
            RLog.d(TAG, "BatchSubmitManager mIsConnected not released");
            return;
        }

        // Cancel pending delayed tasks
        if (mBatchSubmitRunnable != null) {
            mHandler.removeCallbacks(mBatchSubmitRunnable);
        }

        // Clear pending queue
        synchronized (mStateLock) {
            mPendingItems.clear();
        }

        RLog.d(TAG, "BatchSubmitManager released");
    }
}
