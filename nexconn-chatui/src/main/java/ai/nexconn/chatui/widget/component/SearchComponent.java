package ai.nexconn.chatui.widget.component;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.BaseComponent;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class SearchComponent extends BaseComponent {

    private LinearLayout searchLayout;
    private ImageView clearButton;
    private EditText searchEditText;
    private OnSearchQueryListener onSearchQueryListener;
    private OnClickListener onSearchClickListener;
    private boolean isSearchComponentClickable;

    public SearchComponent(@NonNull Context context) {
        super(context);
    }

    public SearchComponent(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchComponent(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View onCreateView(
            Context context,
            LayoutInflater inflater,
            @NonNull ViewGroup parent,
            AttributeSet attrs) {
        // Apply default background only when no external background is set
        if (getBackground() == null) {
            setBackgroundColor(
                    ChatUIThemeManager.getColorFromAttrId(
                            context, R.attr.nc_user_manager_background_color));
        }
        View view = inflater.inflate(R.layout.nc_search_component, parent, false);
        searchLayout = view.findViewById(R.id.layout_search);
        searchEditText = view.findViewById(R.id.et_search);
        clearButton = view.findViewById(R.id.iv_clear);

        // Read custom attributes
        if (attrs != null) {
            TypedArray a =
                    context.getTheme()
                            .obtainStyledAttributes(attrs, R.styleable.SearchComponent, 0, 0);
            try {
                isSearchComponentClickable =
                        a.getBoolean(R.styleable.SearchComponent_search_component_clickable, false);
            } finally {
                a.recycle();
            }
        }

        FrameLayout flSearch = view.findViewById(R.id.fl_search);
        flSearch.setOnClickListener(
                v -> {
                    if (isSearchComponentClickable) {
                        if (onSearchClickListener != null) {
                            onSearchClickListener.onClick(v);
                        }
                    } else {
                        searchEditText.requestFocus();
                        showKeyboard(searchEditText);
                    }
                });

        if (!isSearchComponentClickable) {
            clearButton.setOnClickListener(v -> searchEditText.setText(""));
            searchEditText.setOnEditorActionListener(
                    new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                                if (onSearchQueryListener != null) {
                                    onSearchQueryListener.onClickSearch(v.getText().toString());
                                }
                                return true;
                            }
                            return false;
                        }
                    });

            searchEditText.addTextChangedListener(
                    new TextWatcher() {
                        @Override
                        public void beforeTextChanged(
                                CharSequence s, int start, int count, int after) {
                            // Do nothing
                        }

                        @Override
                        public void onTextChanged(
                                CharSequence s, int start, int before, int count) {
                            if (onSearchQueryListener != null) {
                                onSearchQueryListener.onSearch(s.toString());
                            }
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            boolean hasText = s.length() > 0;
                            clearButton.setVisibility(hasText ? VISIBLE : GONE);
                        }
                    });
        } else {
            // Keep non-editable but clickable, delegate events to flSearch
            searchEditText.setFocusable(false);
            searchEditText.setFocusableInTouchMode(false);
            searchEditText.setCursorVisible(false);
            searchEditText.setClickable(true);
            searchEditText.setLongClickable(false);
            searchEditText.setOnClickListener(v -> flSearch.callOnClick());
        }

        return view;
    }

    /** Set the search box click listener */
    public void setSearchClickListener(@NonNull OnClickListener onSearchClickListener) {
        this.onSearchClickListener = onSearchClickListener;
    }

    public void setSearchHint(@StringRes int resId) {
        this.searchEditText.setHint(resId);
    }

    public void setSearchContent(String searchContent) {
        if (searchContent != null) {
            this.searchEditText.setText(searchContent);
            this.searchEditText.setSelection(searchContent.length());
        }
    }

    /**
     * Set the search query listener
     *
     * @param listener the search query listener
     */
    public void setSearchQueryListener(OnSearchQueryListener listener) {
        this.onSearchQueryListener = listener;
    }

    public interface OnSearchQueryListener {
        /**
         * Search callback
         *
         * @param query the search keyword
         */
        void onSearch(String query);

        default void onClickSearch(String query) {}
    }

    /**
     * Update search layout position based on text input or focus state.
     *
     * @param alignLeft if true, align layout to the left; otherwise center it
     */
    private void updateSearchLayoutPosition(boolean alignLeft) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) searchLayout.getLayoutParams();
        params.gravity = alignLeft ? Gravity.START | Gravity.CENTER_VERTICAL : Gravity.CENTER;
        searchLayout.setLayoutParams(params);
    }

    /**
     * Force show the soft keyboard
     *
     * @param editText the EditText that has focus
     */
    private void showKeyboard(EditText editText) {
        if (editText == null) {
            return;
        }
        InputMethodManager imm =
                (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
    }
}
