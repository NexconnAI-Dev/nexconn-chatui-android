package ai.nexconn.chatui.usermanage.adapter;

import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.interfaces.OnActionClickListener;
import ai.nexconn.chatui.config.NCChatUIConfig;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter for the group list.
 *
 * @since 5.12.2
 */
public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.GroupListViewHolder> {

    private List<GroupInfo> data = new ArrayList<>();

    private OnActionClickListener<GroupInfo> onItemClickListener;
    private OnActionClickListener<GroupInfo> onItemLongClickListener;

    private String highlightedText = null;

    public void setData(List<GroupInfo> newData) {
        if (newData == null) {
            newData = new CopyOnWriteArrayList<>();
        }
        this.data = new CopyOnWriteArrayList<>(newData);
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnActionClickListener<GroupInfo> listener) {
        this.onItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnActionClickListener<GroupInfo> listener) {
        this.onItemLongClickListener = listener;
    }

    /**
     * Sets the text to be highlighted.
     *
     * @param text the text to highlight
     */
    public void setHighlightedText(String text) {
        highlightedText = text;
    }

    @NonNull
    @Override
    public GroupListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.nc_item_group_info, parent, false);
        return new GroupListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupListViewHolder holder, int position) {
        GroupInfo groupInfo = data.get(position);

        // Load group portrait
        NCChatUIConfig.featureConfig()
                .getChatUIImageEngine()
                .loadGroupPortrait(
                        holder.itemView.getContext(), groupInfo.getPortraitUri(), holder.ivHead);

        String groupName = groupInfo.getGroupName();
        // Set group name
        holder.tvTitle.setText(
                getHighlightedText(holder.tvTitle.getContext(), groupName, highlightedText));

        // Add click event (customizable behavior)
        holder.itemView.setOnClickListener(
                v -> {
                    if (onItemClickListener != null) {
                        onItemClickListener.onActionClick(groupInfo);
                    }
                });
        holder.itemView.setOnLongClickListener(
                v -> {
                    if (onItemLongClickListener != null) {
                        onItemLongClickListener.onActionClick(groupInfo);
                    }
                    return false;
                });
    }

    private SpannableStringBuilder getHighlightedText(
            Context context, String fullText, String searchText) {
        if (fullText == null || TextUtils.isEmpty(searchText)) {
            return new SpannableStringBuilder(fullText);
        }
        // Get highlight color
        int highlightColor = ContextCompat.getColor(context, R.color.nc_read_receipt_status);

        // Case-insensitive search
        SpannableStringBuilder spannable = new SpannableStringBuilder(fullText);
        Pattern pattern = Pattern.compile(Pattern.quote(searchText), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(fullText);

        // Only match the first occurrence
        if (matcher.find()) {
            int startIndex = matcher.start();
            int endIndex = matcher.end();

            // Apply highlight color
            spannable.setSpan(
                    new ForegroundColorSpan(highlightColor),
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    static class GroupListViewHolder extends RecyclerView.ViewHolder {
        ImageView ivHead;
        TextView tvTitle;

        public GroupListViewHolder(@NonNull View itemView) {
            super(itemView);
            ivHead = itemView.findViewById(R.id.iv_head);
            tvTitle = itemView.findViewById(R.id.tv_title);
        }
    }
}
