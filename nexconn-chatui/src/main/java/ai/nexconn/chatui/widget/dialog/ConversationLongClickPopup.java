package ai.nexconn.chatui.widget.dialog;

import ai.nexconn.chatui.R;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/** Conversation list long-press popup, implemented using PopupWindow with icon support */
public class ConversationLongClickPopup {

    private Context mContext;
    private List<OptionItem> mOptionItems;
    private OnOptionItemClickListener mItemClickListener;
    private View mAnchorView; // Anchor view for positioning the popup
    private PopupWindow mPopupWindow;
    private DialogInterface.OnDismissListener mOnDismissListener;
    private ImageView mTriangleIndicator; // Triangle indicator
    private View mContentContainer; // Main content container
    private HighlightMaskView mMaskView; // Mask view for highlighting the AnchorView

    /** Option data class */
    public static class OptionItem {
        public final String title;
        public final int iconResId; // Drawable resource ID

        public OptionItem(String title, int iconResId) {
            this.title = title;
            this.iconResId = iconResId;
        }
    }

    /** Option click listener */
    public interface OnOptionItemClickListener {
        /**
         * Called when an option is clicked
         *
         * @param item the clicked option item
         * @param position position of the option in the list
         */
        void onOptionItemClick(OptionItem item, int position);
    }

    public static ConversationLongClickPopup newInstance(
            final Context context, List<OptionItem> items) {
        return new ConversationLongClickPopup(context, items);
    }

    public ConversationLongClickPopup(final Context context, List<OptionItem> items) {
        mContext = context;
        mOptionItems = items;
        initPopupWindow();
    }

    private void initPopupWindow() {
        LayoutInflater inflater =
                (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View contentView = inflater.inflate(R.layout.nc_popup_conversation_long_click, null);

        RecyclerView recyclerView =
                contentView.findViewById(R.id.nc_recycler_conversation_long_click);
        mTriangleIndicator = contentView.findViewById(R.id.nc_popup_triangle_indicator);
        mContentContainer = contentView.findViewById(R.id.nc_popup_content_container);

        // Use horizontal LinearLayoutManager with each item width adapting to its content
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        // Add item spacing (only between items, not at the start/end)
        recyclerView.addItemDecoration(new HorizontalSpaceItemDecoration(dpToPx(10)));

        OptionsAdapter adapter = new OptionsAdapter(mOptionItems);
        recyclerView.setAdapter(adapter);

        // Create the PopupWindow
        mPopupWindow =
                new PopupWindow(
                        contentView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        true); // focusable

        // Set background to allow dismiss on outside click
        mPopupWindow.setBackgroundDrawable(
                mContext.getResources().getDrawable(android.R.color.transparent));

        // Dismissable on outside touch
        mPopupWindow.setOutsideTouchable(true);

        // Set input method mode to avoid affecting existing EditText focus
        mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        mPopupWindow.setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        // Set dismiss listener
        mPopupWindow.setOnDismissListener(
                new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        // Remove the mask view
                        removeMaskView();
                        if (mOnDismissListener != null) {
                            mOnDismissListener.onDismiss(null);
                        }
                    }
                });

        // Measure contentView to get actual dimensions
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
    }

    /**
     * Set the anchor view; the popup will appear above it
     *
     * @param anchorView the anchor view
     * @return this
     */
    public ConversationLongClickPopup setAnchorView(View anchorView) {
        this.mAnchorView = anchorView;
        return this;
    }

    /**
     * Set the option click listener
     *
     * @param listener the listener
     * @return this
     */
    public ConversationLongClickPopup setOnOptionItemClickListener(
            OnOptionItemClickListener listener) {
        this.mItemClickListener = listener;
        return this;
    }

    /**
     * Set the option click listener (backward compatibility)
     *
     * @param itemListener the listener
     * @return this
     * @deprecated Please use {@link #setOnOptionItemClickListener(OnOptionItemClickListener)}
     */
    @Deprecated
    public ConversationLongClickPopup setOptionsPopupDialogListener(
            final OptionsPopupDialog.OnOptionsItemClickedListener itemListener) {
        if (itemListener != null) {
            this.mItemClickListener =
                    new OnOptionItemClickListener() {
                        @Override
                        public void onOptionItemClick(OptionItem item, int position) {
                            itemListener.onOptionsItemClicked(position);
                        }
                    };
        }
        return this;
    }

    /**
     * Set the dismiss listener
     *
     * @param listener the listener
     */
    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        this.mOnDismissListener = listener;
    }

    /** Show the popup */
    public void show() {
        if (mContext instanceof Activity) {
            Activity activity = (Activity) mContext;
            if (activity.isFinishing()) {
                return;
            }
        }

        if (mPopupWindow == null) {
            return;
        }

        if (mAnchorView != null) {
            // Get the anchorView's position on screen
            int[] location = new int[2];
            mAnchorView.getLocationOnScreen(location);

            View contentView = mPopupWindow.getContentView();
            contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int popupHeight = contentView.getMeasuredHeight();
            int popupWidth = contentView.getMeasuredWidth();

            // Get status bar height and top safe distance
            int statusBarHeight = getStatusBarHeight();
            int topMargin = statusBarHeight + dpToPx(40); // Status bar + 40dp margin

            // Spacing from the anchor view, ensure the triangle doesn't overlap the anchorView
            // Use negative value to leave space between the popup and anchorView
            int verticalSpacing = -dpToPx(7); // Leave 2dp spacing

            // Get screen width and margin
            int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
            int horizontalMargin = dpToPx(10); // Left/right margin

            // Calculate the anchor's center position
            int anchorCenterX = location[0] + mAnchorView.getWidth() / 2;

            // Prefer centering the popup on the anchor, then adjust to fit screen
            int xPos = anchorCenterX - popupWidth / 2;

            // Ensure it doesn't exceed screen boundaries
            if (xPos < horizontalMargin) {
                xPos = horizontalMargin;
            } else if (xPos + popupWidth > screenWidth - horizontalMargin) {
                xPos = screenWidth - popupWidth - horizontalMargin;
            }

            // Calculate vertical position
            int yPos = location[1] - popupHeight + verticalSpacing;
            boolean isShowOnTop = true; // Default to showing above

            // Check if there's enough space above (considering top safe distance)
            if (yPos < topMargin) {
                // Not enough space above, show below
                yPos = location[1] + mAnchorView.getHeight();
                isShowOnTop = false;
            }

            // Adjust triangle indicator position and direction based on anchor position
            adjustTriangleIndicator(
                    location[0], mAnchorView.getWidth(), xPos, popupWidth, isShowOnTop);

            // Add highlight mask view
            addMaskView();

            // Show at the specified position using showAtLocation
            mPopupWindow.showAtLocation(mAnchorView, Gravity.NO_GRAVITY, xPos, yPos);
        } else {
            // If no anchor view, hide the triangle indicator and show at screen center
            if (mTriangleIndicator != null) {
                mTriangleIndicator.setVisibility(View.GONE);
            }
            if (mContext instanceof Activity) {
                // Add mask view (no highlight area)
                addMaskView();

                View decorView = ((Activity) mContext).getWindow().getDecorView();
                mPopupWindow.showAtLocation(decorView, Gravity.CENTER, 0, 0);
            }
        }
    }

    /** Dismiss the popup */
    public void dismiss() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }

    /** Add mask view and set the highlight area to the AnchorView */
    private void addMaskView() {
        if (!(mContext instanceof Activity)) {
            return;
        }

        Activity activity = (Activity) mContext;
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();

        // Create the mask view
        mMaskView = new HighlightMaskView(mContext);
        mMaskView.setMaskColor(0x99000000); // Semi-transparent black mask

        // If there's an anchor view, set the highlight area
        if (mAnchorView != null) {
            // Get the AnchorView's position on screen
            int[] location = new int[2];
            mAnchorView.getLocationOnScreen(location);

            Rect highlightRect =
                    new Rect(
                            location[0],
                            location[1],
                            location[0] + mAnchorView.getWidth(),
                            location[1] + mAnchorView.getHeight());

            mMaskView.setHighlightRect(highlightRect);

            // Set corner radius (assuming conversation list items have rounded corners, set to 8dp)
            mMaskView.setCornerRadius(dpToPx(0));
        }

        // Add mask view to the DecorView
        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        decorView.addView(mMaskView, params);

        // Consume all touch events on mask to prevent underlying list from scrolling.
        mMaskView.setClickable(true);
        mMaskView.setFocusable(true);
        mMaskView.setOnTouchListener(
                new View.OnTouchListener() {
                    private final int touchSlop =
                            ViewConfiguration.get(mContext).getScaledTouchSlop();
                    private float downX;
                    private float downY;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event == null) {
                            return true;
                        }
                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                downX = event.getRawX();
                                downY = event.getRawY();
                                return true;
                            case MotionEvent.ACTION_UP:
                                if (Math.abs(event.getRawX() - downX) <= touchSlop
                                        && Math.abs(event.getRawY() - downY) <= touchSlop) {
                                    dismiss();
                                }
                                return true;
                            case MotionEvent.ACTION_MOVE:
                            case MotionEvent.ACTION_CANCEL:
                            default:
                                return true;
                        }
                    }
                });
    }

    /** Remove mask view */
    private void removeMaskView() {
        if (mMaskView != null && mContext instanceof Activity) {
            Activity activity = (Activity) mContext;
            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            decorView.removeView(mMaskView);
            mMaskView = null;
        }
    }

    /**
     * Check whether the popup is currently showing
     *
     * @return true if currently showing
     */
    public boolean isShowing() {
        return mPopupWindow != null && mPopupWindow.isShowing();
    }

    /**
     * Get the status bar height
     *
     * @return status bar height in px
     */
    private int getStatusBarHeight() {
        int statusBarHeight = 0;
        int resourceId =
                mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = mContext.getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }

    /**
     * Convert dp to px
     *
     * @param dp dp value
     * @return px value
     */
    private int dpToPx(int dp) {
        float density = mContext.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    /**
     * Adjust the triangle indicator position and direction
     *
     * @param anchorX screen X coordinate of the anchor view
     * @param anchorWidth width of the anchor view
     * @param popupX X coordinate of the popup
     * @param popupWidth width of the popup
     * @param isShowOnTop whether to show above the anchor
     */
    private void adjustTriangleIndicator(
            int anchorX, int anchorWidth, int popupX, int popupWidth, boolean isShowOnTop) {
        if (mTriangleIndicator == null || mContentContainer == null) {
            return;
        }

        // Show the triangle
        mTriangleIndicator.setVisibility(View.VISIBLE);

        // Calculate the anchor view's center position
        int anchorCenterX = anchorX + anchorWidth / 2;

        // Calculate triangle offset relative to popup (arrow center aligned to anchor center)
        int triangleX = anchorCenterX - popupX - dpToPx(8); // 8dp is half the triangle width

        // Ensure triangle doesn't completely exceed popup bounds (allow arrow at edge, but at least
        // half must be inside)
        int minX = -dpToPx(4); // Allow arrow to extend half beyond the left
        int maxX = popupWidth - dpToPx(12); // Allow arrow to extend half beyond the right

        // Only constrain when the arrow would completely exceed bounds
        if (triangleX < minX) {
            triangleX = minX;
        } else if (triangleX > maxX) {
            triangleX = maxX;
        }

        // Adjust content container margin to make room for the triangle
        ViewGroup.MarginLayoutParams contentParams =
                (ViewGroup.MarginLayoutParams) mContentContainer.getLayoutParams();

        // Set the triangle's horizontal position
        android.widget.FrameLayout.LayoutParams triangleParams =
                (android.widget.FrameLayout.LayoutParams) mTriangleIndicator.getLayoutParams();
        triangleParams.leftMargin = triangleX;

        // Adjust triangle direction and content area margin based on display position
        if (isShowOnTop) {
            // Popup above, triangle at bottom pointing down to anchor
            mTriangleIndicator.setRotation(180); // Triangle tip pointing down
            triangleParams.gravity = Gravity.BOTTOM | Gravity.START;
            // Leave space for the triangle at the bottom
            contentParams.topMargin = 0;
            contentParams.bottomMargin = dpToPx(7);
        } else {
            // Popup below, triangle at top pointing up to anchor
            mTriangleIndicator.setRotation(0); // Triangle tip pointing up
            triangleParams.gravity = Gravity.TOP | Gravity.START;
            // Leave space for the triangle at the top
            contentParams.topMargin = dpToPx(7);
            contentParams.bottomMargin = 0;
        }

        mTriangleIndicator.setLayoutParams(triangleParams);
        mContentContainer.setLayoutParams(contentParams);
    }

    private class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.ViewHolder> {

        private List<OptionItem> items;

        public OptionsAdapter(List<OptionItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.nc_popup_conversation_long_click_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final OptionItem item = items.get(position);
            holder.titleView.setText(item.title);

            // Set the icon
            if (item.iconResId != 0) {
                holder.iconView.setImageResource(item.iconResId);
                holder.iconView.setVisibility(View.VISIBLE);
            } else {
                holder.iconView.setVisibility(View.GONE);
            }

            // Set the click event
            holder.itemView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int clickPosition = holder.getAdapterPosition();
                            if (clickPosition != RecyclerView.NO_POSITION
                                    && mItemClickListener != null) {
                                mItemClickListener.onOptionItemClick(item, clickPosition);
                                dismiss();
                            }
                        }
                    });
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView iconView;
            TextView titleView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                iconView = itemView.findViewById(R.id.nc_popup_item_icon);
                titleView = itemView.findViewById(R.id.nc_popup_item_text);
            }
        }
    }

    /** Horizontal ItemDecoration for adding spacing between items */
    private static class HorizontalSpaceItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        public HorizontalSpaceItemDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(
                @NonNull Rect outRect,
                @NonNull View view,
                @NonNull RecyclerView parent,
                @NonNull RecyclerView.State state) {
            // Not the last item, add right spacing
            if (parent.getChildAdapterPosition(view) != parent.getAdapter().getItemCount() - 1) {
                outRect.right = spacing;
            }
        }
    }
}
