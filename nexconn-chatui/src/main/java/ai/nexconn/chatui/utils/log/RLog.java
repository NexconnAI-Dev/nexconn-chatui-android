package ai.nexconn.chatui.utils.log;

import android.util.Log;

/** Logging utility class for internal SDK use. */
public class RLog {

    static final String TAG = "NCLog";

    public static final int NONE = 0;
    public static final int F = 1;
    public static final int E = 2;
    public static final int W = 3;
    public static final int I = 4;
    public static final int D = 5;
    public static final int V = 6;

    private static int sLogLevel = V;

    public static void setLogLevel(int pLevel) {
        sLogLevel = pLevel;
    }

    public static void setLogLevel(int pLevel, boolean isInitProgress) {
        sLogLevel = pLevel;
    }

    public static void setFileMaxSize(long pMaxSize) {}

    public static int v(String tag, String msg) {
        if (sLogLevel >= V) Log.v(TAG, "[ " + tag + " ] " + msg);
        return 0;
    }

    public static int d(String tag, String msg) {
        if (sLogLevel >= D) Log.d(TAG, "[ " + tag + " ] " + msg);
        return 0;
    }

    public static int i(String tag, String msg) {
        if (sLogLevel >= I) Log.i(TAG, "[ " + tag + " ] " + msg);
        return 0;
    }

    public static int w(String tag, String msg) {
        if (sLogLevel >= W) Log.w(TAG, "[ " + tag + " ] " + msg);
        return 0;
    }

    public static int e(String tag, String msg) {
        if (sLogLevel >= E) Log.e(TAG, "[ " + tag + " ] " + msg);
        return 0;
    }

    public static int e(String tag, String msg, Throwable tr) {
        if (sLogLevel >= E) Log.e(TAG, "[ " + tag + " ] " + msg, tr);
        return 0;
    }

    public static int f(String tag, String msg) {
        if (sLogLevel >= F) Log.wtf(TAG, "[ " + tag + " ] " + msg);
        return 0;
    }
}
