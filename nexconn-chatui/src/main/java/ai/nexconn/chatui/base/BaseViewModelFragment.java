package ai.nexconn.chatui.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

/**
 * Base Fragment class with ViewModel support.
 *
 * @since 5.10.4
 */
public abstract class BaseViewModelFragment<VM extends ViewModel> extends BaseFragment
        implements BasePage {
    private VM viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments() == null ? new Bundle() : getArguments();
        this.viewModel = onCreateViewModel(bundle);
    }

    @Nullable
    @Override
    public final View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        onBeforeViewReady(this.viewModel);
        View view = onCreateView(requireActivity(), inflater, container, getArguments());
        onViewReady(this.viewModel);
        return view;
    }

    /**
     * Creates the ViewModel.
     *
     * @param bundle Bundle
     * @return the ViewModel instance
     */
    @NonNull
    protected abstract VM onCreateViewModel(@NonNull Bundle bundle);

    /**
     * Called before the View is created.
     *
     * @param viewModel VM
     */
    protected void onBeforeViewReady(@NonNull VM viewModel) {}

    /**
     * Called after the View is created.
     *
     * @param viewModel VM
     */
    protected abstract void onViewReady(@NonNull VM viewModel);

    @NonNull
    protected VM getViewModel() {
        return viewModel;
    }
}
