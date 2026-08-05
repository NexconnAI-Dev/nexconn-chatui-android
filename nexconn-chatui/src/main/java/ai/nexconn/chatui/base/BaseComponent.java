package ai.nexconn.chatui.base;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Base Component class.
 *
 * @since 5.10.4
 */
public abstract class BaseComponent extends FrameLayout {
    public BaseComponent(@NonNull Context context) {
        super(context);
    }

    public BaseComponent(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseComponent(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        addView(onCreateView(context, LayoutInflater.from(context), this, attrs));
    }

    /**
     * Creates the view.
     *
     * @param context context
     * @param from layout inflater
     * @param parent parent ViewGroup
     * @param attrs attribute set
     * @return the created View
     */
    protected abstract View onCreateView(
            Context context, LayoutInflater from, @NonNull ViewGroup parent, AttributeSet attrs);
}
