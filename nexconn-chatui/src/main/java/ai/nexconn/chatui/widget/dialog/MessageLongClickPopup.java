package ai.nexconn.chatui.widget.dialog;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/** Message long-press popup, implemented using PopupWindow with icon support */
public class MessageLongClickPopup {

    private Context mContext;
    private List<OptionItem> mOptionItems;
    private OptionsPopupDialog.OnOptionsItemClickedListener mItemClickedListener;
    private View mAnchorView; // Anchor view for positioning the popup
    private PopupWindow mPopupWindow;
    private DialogInterface.OnDismissListener mOnDismissListener;
    private ImageView mTriangleIndicator; // Triangle indicator
    private View mContentContainer; // Main content container

    public static class OptionItem {
        public String title;
        public int iconResId;

        public OptionItem(String title, int iconResId) {
            this.title = title;
            this.iconResId = iconResId;
        }
    }

    public static MessageLongClickPopup newInstance(final Context context, List<OptionItem> items) {
        return new MessageLongClickPopup(context, items);
    }

    public MessageLongClickPopup(final Context context, List<OptionItem> items) {
        mContext = context;
        mOptionItems = items;
        initPopupWindow();
    }

    private void initPopupWindow() {
        LayoutInflater inflater =
                (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View contentView = inflater.inflate(R.layout.nc_popup_message_long_click, null);

        RecyclerView recyclerView = contentView.findViewById(R.id.nc_recycler_message_long_click);
        mTriangleIndicator = contentView.findViewById(R.id.nc_popup_triangle_indicator);
        mContentContainer = contentView.findViewById(R.id.nc_popup_content_container);

        // Dynamically calculate column count based on option count, max 5
        int columnCount = Math.min(mOptionItems.size(), 5);
        GridLayoutManager layoutManager = new GridLayoutManager(mContext, columnCount);
        recyclerView.setLayoutManager(layoutManager);

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
    public MessageLongClickPopup setAnchorView(View anchorView) {
        this.mAnchorView = anchorView;
        return this;
    }

    /**
     * Set the click listener
     *
     * @param itemListener the listener
     * @return this
     */
    public MessageLongClickPopup setOptionsPopupDialogListener(
            OptionsPopupDialog.OnOptionsItemClickedListener itemListener) {
        this.mItemClickedListener = itemListener;
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

            // Set spacing from anchor to 0 so the arrow stays close to anchor
            int verticalSpacing = 0;

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
                yPos = location[1] + mAnchorView.getHeight() - verticalSpacing;
                isShowOnTop = false;
            }

            // Adjust triangle indicator position and direction based on anchor position
            adjustTriangleIndicator(
                    location[0], mAnchorView.getWidth(), xPos, popupWidth, isShowOnTop);

            // Show at the specified position using showAtLocation
            mPopupWindow.showAtLocation(mAnchorView, Gravity.NO_GRAVITY, xPos, yPos);
        } else {
            // If no anchor view, hide the triangle indicator and show at screen center
            if (mTriangleIndicator != null) {
                mTriangleIndicator.setVisibility(View.GONE);
            }
            if (mContext instanceof Activity) {
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
                            .inflate(R.layout.nc_popup_message_long_click_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OptionItem item = items.get(position);
            holder.titleView.setText(item.title);

            if (item.iconResId != 0) {
                // Use ThemeManager to get the actual drawable resource ID from the attribute
                int actualIconResId = ChatUIThemeManager.getAttrResId(mContext, item.iconResId);
                if (actualIconResId != 0) {
                    holder.iconView.setImageResource(actualIconResId);
                    holder.iconView.setVisibility(View.VISIBLE);
                } else {
                    holder.iconView.setVisibility(View.GONE);
                }
            } else {
                holder.iconView.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mItemClickedListener != null) {
                                mItemClickedListener.onOptionsItemClicked(
                                        holder.getAdapterPosition());
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
}
