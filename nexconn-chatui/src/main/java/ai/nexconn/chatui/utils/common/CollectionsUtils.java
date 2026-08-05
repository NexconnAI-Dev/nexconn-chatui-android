package ai.nexconn.chatui.utils.common;

import android.os.Build;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

public class CollectionsUtils {

    /**
     * Collections.reverse implementation compatible with Android M.
     *
     * <p>Uses Collections.reverse on Android M+ and a custom implementation on Android M and below.
     *
     * @param list the list to reverse
     */
    public static void reverse(List<?> list) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Collections.reverse(list);
        } else {
            reverseAdapterAndroidM(list);
        }
    }

    /**
     * This method copies the Collections.reverse implementation from Android M+. On Android M and
     * below, Collections.reverse throws UnsupportedOperationException as shown below:
     *
     * <pre class="">
     * public static void reverse(List<?> list) {
     *     int size = list.size();
     *     ListIterator front = (ListIterator) list.listIterator();
     *     ListIterator back = (ListIterator) list.listIterator(size);
     *     for (int i = 0; i < size / 2; i++) {
     *         Object frontNext = front.next();
     *         Object backPrev = back.previous();
     *         front.set(backPrev);
     *         back.set(frontNext);
     *     }
     * }
     * </pre>
     *
     * @param list
     */
    private static void reverseAdapterAndroidM(List<?> list) {
        int size = list.size();
        if (size < 18 || list instanceof RandomAccess) {
            for (int i = 0, mid = size >> 1, j = size - 1; i < mid; i++, j--) swap(list, i, j);
        } else {
            // instead of using a raw type here, it's possible to capture
            // the wildcard but it will require a call to a supplementary
            // private method
            ListIterator fwd = list.listIterator();
            ListIterator rev = list.listIterator(size);
            for (int i = 0, mid = list.size() >> 1; i < mid; i++) {
                Object tmp = fwd.next();
                fwd.set(rev.previous());
                rev.set(tmp);
            }
        }
    }

    private static void swap(List<?> list, int i, int j) {
        // instead of using a raw type here, it's possible to capture
        // the wildcard but it will require a call to a supplementary
        // private method
        final List l = list;
        l.set(i, l.set(j, l.get(i)));
    }
}
