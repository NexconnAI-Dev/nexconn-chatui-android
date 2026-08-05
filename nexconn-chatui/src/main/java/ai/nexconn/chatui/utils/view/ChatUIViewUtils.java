package ai.nexconn.chatui.utils.view;

import ai.nexconn.chatui.utils.log.RLog;
import android.view.View;
import android.view.ViewGroup;

public class ChatUIViewUtils {

    /** Safe addView */
    public static void addView(ViewGroup viewGroup, View addedView) {
        addView(viewGroup, addedView, -1);
    }

    /** Safe addView with index */
    public static void addView(ViewGroup viewGroup, View addedView, int index) {
        if (addedView == null || viewGroup == null) {
            return;
        }
        try {
            if (addedView.getParent() != null) {
                ((ViewGroup) addedView.getParent()).removeView(addedView);
            }
            viewGroup.addView(addedView, index);
        } catch (Exception e) {
            RLog.d("ChatUIViewUtils", "addView e:" + e);
        }
    }
}
