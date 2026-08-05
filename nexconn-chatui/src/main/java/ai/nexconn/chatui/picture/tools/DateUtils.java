package ai.nexconn.chatui.picture.tools;

import ai.nexconn.chatui.utils.log.RLog;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateUtils {
    private static final String TAG = DateUtils.class.getSimpleName();
    private SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd_HHmmssSS");

    private DateUtils() {
        // default implementation ignored
    }

    private static class SingletonHolder {
        static DateUtils sInstance = new DateUtils();
    }

    public static DateUtils getInstance() {
        return SingletonHolder.sInstance;
    }

    /**
     * Calculate the difference in seconds between two timestamps
     *
     * @param d
     * @return
     */
    public int dateDiffer(long d) {
        try {
            long l1 = Long.parseLong(String.valueOf(System.currentTimeMillis()).substring(0, 10));
            long interval = l1 - d;
            return (int) Math.abs(interval);
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
            return -1;
        }
    }

    /**
     * Convert timestamp to time format
     *
     * @param duration
     * @return
     */
    public String formatDurationTime(long duration) {
        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
    }

    /**
     * Create file name from timestamp
     *
     * @param prefix name prefix
     * @return
     */
    public String getCreateFileName(String prefix) {
        long millis = System.currentTimeMillis();
        return prefix + sf.format(millis);
    }

    /**
     * Calculate the time interval between two timestamps
     *
     * @param sTime
     * @param eTime
     * @return
     */
    public String cdTime(long sTime, long eTime) {
        long diff = eTime - sTime;
        return diff > 1000 ? diff / 1000 + "s" : diff + "ms";
    }
}
