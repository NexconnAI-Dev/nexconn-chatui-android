package ai.nexconn.chatui.utils.timer;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

/** Created by Android Studio. User: lvhongzhen Date: 2019-09-03 Time: 10:59 */
public abstract class CountDownTimer {
    private boolean isStart;

    /** Millis since epoch when alarm should stop. */
    private final long mMillisInFuture;

    /** The interval in millis that the user receives callbacks */
    private final long mCountdownInterval;

    private long mStopTimeInFuture;

    /** boolean representing if the timer was cancelled */
    private boolean mCancelled = false;

    private static final int MSG = 1;

    /**
     * @param millisInFuture The number of millis in the future from the call to {@link #start()}
     *     until the countdown is done and {@link #onFinish()} is called.
     * @param countDownInterval The interval along the way to receive {@link #onTick(long)}
     *     callbacks.
     */
    protected CountDownTimer(long millisInFuture, long countDownInterval) {
        mMillisInFuture = millisInFuture;
        mCountdownInterval = countDownInterval;
    }

    /** Cancel the countdown. */
    public final synchronized void cancel() {
        mCancelled = true;
        isStart = false;
        mHandler.removeMessages(MSG);
    }

    /** Start the countdown. */
    public final synchronized CountDownTimer start() {
        mCancelled = false;
        isStart = true;
        if (mMillisInFuture <= 0) {
            onFinish();
            return this;
        }
        mStopTimeInFuture = SystemClock.elapsedRealtime() + mMillisInFuture;
        mHandler.sendMessage(mHandler.obtainMessage(MSG));
        return this;
    }

    /**
     * Callback fired on regular interval.
     *
     * @param millisUntilFinished The amount of time until finished.
     */
    public abstract void onTick(long millisUntilFinished);

    /** Callback fired when the time is up. */
    public abstract void onFinish();

    public boolean isStart() {
        return isStart;
    }

    // handles counting down
    private Handler mHandler =
            new Handler(Looper.getMainLooper()) {

                @Override
                public void handleMessage(Message msg) {

                    synchronized (CountDownTimer.this) {
                        if (mCancelled) {
                            return;
                        }

                        final long millisLeft = mStopTimeInFuture - SystemClock.elapsedRealtime();
                        if (millisLeft <= 0) {
                            onFinish();
                        } else {
                            long lastTickStart = SystemClock.elapsedRealtime();
                            onTick(millisLeft);

                            // take into account user's onTick taking time to execute
                            long lastTickDuration = SystemClock.elapsedRealtime() - lastTickStart;
                            long delay;

                            if (millisLeft < mCountdownInterval) {
                                // just delay until done
                                delay = millisLeft - lastTickDuration;

                                // special case: user's onTick took more than interval to
                                // complete, trigger onFinish without delay
                                if (delay < 0) {
                                    delay = 0;
                                }
                            } else {
                                delay = mCountdownInterval - lastTickDuration;

                                // special case: user's onTick took more than interval to
                                // complete, skip to next interval
                                while (delay < 0) {
                                    delay += mCountdownInterval;
                                }
                            }

                            sendMessageDelayed(obtainMessage(MSG), delay);
                        }
                    }
                }
            };
}
