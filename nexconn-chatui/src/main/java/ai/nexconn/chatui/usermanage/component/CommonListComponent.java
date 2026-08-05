package ai.nexconn.chatui.usermanage.component;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.BaseComponent;
import ai.nexconn.chatui.base.interfaces.OnPagedDataLoader;
import ai.nexconn.chatui.widget.pullrefresh.SmartRefreshLayout;
import ai.nexconn.chatui.widget.pullrefresh.wrapper.NCRefreshHeader;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public final class CommonListComponent extends BaseComponent {

    private SmartRefreshLayout refreshLayout;
    private RecyclerView recyclerView;

    private OnPagedDataLoader onPagedDataLoader;

    private boolean enableLoadMore;
    private boolean enableRefresh;

    public CommonListComponent(@NonNull Context context) {
        super(context);
    }

    public CommonListComponent(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CommonListComponent(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View onCreateView(
            Context context, LayoutInflater from, @NonNull ViewGroup parent, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a =
                    context.getTheme()
                            .obtainStyledAttributes(attrs, R.styleable.CommonListComponent, 0, 0);
            try {
                enableLoadMore =
                        a.getBoolean(R.styleable.CommonListComponent_enable_load_more, false);
                enableRefresh = a.getBoolean(R.styleable.CommonListComponent_enable_refresh, false);
            } finally {
                if (a != null) {
                    a.recycle();
                }
            }
        }

        View view = from.inflate(R.layout.nc_list_component, parent, false);
        refreshLayout = view.findViewById(R.id.nc_refresh);
        refreshLayout.setNestedScrollingEnabled(false);
        refreshLayout.setRefreshHeader(new NCRefreshHeader(context));
        refreshLayout.setRefreshFooter(new NCRefreshHeader(context));
        refreshLayout.setOnLoadMoreListener(
                refreshLayout -> {
                    if (onPagedDataLoader != null) {
                        onPagedDataLoader.loadNext(
                                aBoolean -> {
                                    refreshLayout.finishLoadMore();
                                    if (!onPagedDataLoader.hasNext()) {
                                        refreshLayout.setEnableLoadMore(false);
                                    }
                                });
                    }
                });
        refreshLayout.setOnRefreshListener(
                refreshLayout -> {
                    if (onPagedDataLoader != null) {
                        onPagedDataLoader.loadPrevious(
                                aBoolean -> {
                                    refreshLayout.finishRefresh();
                                    if (!onPagedDataLoader.hasPrevious()) {
                                        refreshLayout.setEnableRefresh(false);
                                    }
                                });
                    }
                });
        refreshLayout.setEnableRefresh(enableRefresh);
        refreshLayout.setEnableLoadMore(enableLoadMore);
        recyclerView = view.findViewById(R.id.nc_list);
        recyclerView.setItemAnimator(null);
        return view;
    }

    /**
     * Sets the adapter for the RecyclerView.
     *
     * @param adapter the adapter to set
     */
    public void setAdapter(RecyclerView.Adapter adapter) {
        recyclerView.setAdapter(adapter);
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    /**
     * Sets the paged data loader.
     *
     * @param onPageLoader the paged data loader
     */
    public void setOnPageDataLoader(OnPagedDataLoader onPageLoader) {
        this.onPagedDataLoader = onPageLoader;
    }
}
