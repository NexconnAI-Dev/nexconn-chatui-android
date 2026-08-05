package ai.nexconn.chatui.base;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

/**
 * Base Fragment class with page and ViewModel support.
 *
 * @since 5.10.4
 */
public abstract class BasePageFragment<P extends BasePage, VM extends ViewModel>
        extends BaseViewModelFragment<VM> {
    private P page;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments() == null ? new Bundle() : getArguments();
        this.page = onCreatePage(bundle);
    }

    @Nullable
    @Override
    public final View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return page.onCreateView(context, inflater, container, getArguments());
    }

    /**
     * Creates the page.
     *
     * @param bundle Bundle
     * @return the page instance
     */
    @NonNull
    protected abstract P onCreatePage(@NonNull Bundle bundle);

    /**
     * Called before the View is created.
     *
     * @param page P
     * @param viewModel VM
     */
    protected void onBeforeViewReady(@NonNull P page, @NonNull VM viewModel) {}

    /**
     * Called after the View is created.
     *
     * @param page P
     * @param viewModel VM
     */
    protected abstract void onViewReady(@NonNull P page, @NonNull VM viewModel);

    @NonNull
    protected P getPage() {
        return page;
    }

    @Override
    protected final void onBeforeViewReady(@NonNull VM viewModel) {
        onBeforeViewReady(this.page, viewModel);
    }

    @Override
    protected final void onViewReady(@NonNull VM viewModel) {
        onViewReady(this.page, viewModel);
    }
}
