package ai.nexconn.chatui.utils.file;

import ai.nexconn.chatui.utils.log.RLog;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import androidx.annotation.RequiresApi;

public class CursorUtils {

    private static final String TAG = "CursorUtils";

    public static Cursor query(
            Context context,
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        if (context == null) {
            return null;
        }
        Cursor data = null;
        try {
            data =
                    context.getContentResolver()
                            .query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (Exception e) {
            RLog.e(TAG, "query error:" + e.getMessage());
        }
        return data;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Cursor query(
            Context context,
            Uri uri,
            String[] projection,
            Bundle queryArgs,
            CancellationSignal cancellationSignal) {
        if (context == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return null;
        }
        Cursor data = null;
        try {
            data =
                    context.getContentResolver()
                            .query(uri, projection, queryArgs, cancellationSignal);
        } catch (Exception e) {
            RLog.e(TAG, "query error:" + e.getMessage());
        }
        return data;
    }

    public static int delete(Context context, Uri uri, String where, String[] selectionArgs) {
        int delete = -1;
        if (context == null) {
            return delete;
        }
        try {
            delete = context.getContentResolver().delete(uri, where, selectionArgs);
        } catch (Exception e) {
            RLog.e(TAG, "delete error:" + e.getMessage());
        }
        return delete;
    }
}
