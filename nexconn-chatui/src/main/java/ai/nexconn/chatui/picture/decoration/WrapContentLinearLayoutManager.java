/*
 * Copyright (c) 2018.
 * Author: Zhao
 * Email: joeyzhao1005@gmail.com
 */

package ai.nexconn.chatui.picture.decoration;

import ai.nexconn.chatui.utils.log.RLog;
import android.content.Context;
import android.util.AttributeSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by luck on 2017/12/4.
 *
 * <p>RecyclerView Bug: IndexOutOfBoundsException: Inconsistency detected. Invalid view holder
 * adapter workaround. This simply catches the exception to prevent crashes. The ultimate fix needs
 * to come from Google.
 */
public class WrapContentLinearLayoutManager extends LinearLayoutManager {
    private static final String TAG = WrapContentLinearLayoutManager.class.getSimpleName();

    public WrapContentLinearLayoutManager(Context context) {
        super(context);
    }

    public WrapContentLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public WrapContentLinearLayoutManager(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            super.onLayoutChildren(recycler, state);
        } catch (IndexOutOfBoundsException e) {
            RLog.e(TAG, e.getMessage());
        }
    }
}
