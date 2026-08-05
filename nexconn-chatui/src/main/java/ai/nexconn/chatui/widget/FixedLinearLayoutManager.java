package ai.nexconn.chatui.widget;

import ai.nexconn.chatui.utils.log.RLog;
import android.content.Context;
import android.util.AttributeSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Workaround for a known RecyclerView bug: wraps LinearLayoutManager and catches
 * IndexOutOfBoundsException in onLayoutChildren().
 */
public class FixedLinearLayoutManager extends LinearLayoutManager {

    private static final String TAG = FixedLinearLayoutManager.class.getSimpleName();

    public FixedLinearLayoutManager(Context context) {
        super(context);
    }

    public FixedLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public FixedLinearLayoutManager(
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
