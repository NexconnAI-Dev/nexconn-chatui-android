package ai.nexconn.chatui.usermanage.adapter;

import ai.nexconn.chat.user.model.FriendApplicationStatus;
import ai.nexconn.chat.user.model.FriendApplicationType;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.adapter.CommonAdapter;
import ai.nexconn.chatui.base.adapter.ViewHolder;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.UiFriendApplicationInfo;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class ApplyFriendAdapter extends CommonAdapter<UiFriendApplicationInfo> {

    private OnBtnClickListener onBtnClickListener;

    public ApplyFriendAdapter() {
        super(R.layout.nc_item_apply_friend);
    }

    public void setOnBtnClickListener(OnBtnClickListener onBtnClickListener) {
        this.onBtnClickListener = onBtnClickListener;
    }

    @Override
    public void bindData(ViewHolder holder, UiFriendApplicationInfo item, int position) {
        holder.setText(R.id.tv_title, item.getInfo().getName());
        //        holder.setText(R.id.tv_content, item.getInfo().getRemark());
        TextView tv = holder.<TextView>getView(R.id.tv_content);
        if (position == 0) {
            holder.setVisible(R.id.tv_time, true);
            holder.setText(R.id.tv_time, item.getShowTime());
        } else {
            int showTime = item.getShowTime();
            UiFriendApplicationInfo preInfo = mData.get(position - 1);
            if (preInfo.getShowTime() == showTime) {
                holder.setVisible(R.id.tv_time, false);
                holder.setText(R.id.tv_time, item.getShowTime());
            } else {
                holder.setVisible(R.id.tv_time, true);
                holder.setText(R.id.tv_time, item.getShowTime());
            }
        }

        tv.getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                String desc =
                                        TextUtils.isEmpty(item.getInfo().getExtra())
                                                ? holder.getString(R.string.nc_request_add_friend)
                                                : item.getInfo().getExtra();
                                CharSequence ellipsizeStr =
                                        TextUtils.ellipsize(
                                                desc,
                                                tv.getPaint(),
                                                tv.getWidth(),
                                                TextUtils.TruncateAt.END);
                                // Text needs truncation (collapsed state)
                                if (ellipsizeStr.length() < desc.length()) {
                                    showCollapsedState(
                                            tv, holder.getView(R.id.tv_expand), ellipsizeStr, desc);
                                }
                                // Text does not need truncation (normal state)
                                else {
                                    tv.setText(desc);
                                    holder.setVisible(R.id.tv_expand, false);
                                }
                                tv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }

                            // Show collapsed state
                            private void showCollapsedState(
                                    final TextView tv,
                                    final TextView tvExpand,
                                    final CharSequence ellipsizeStr,
                                    final String desc) {
                                tv.setMaxLines(2);
                                tv.setEllipsize(TextUtils.TruncateAt.END);
                                tv.setText(ellipsizeStr);
                                if (tvExpand != null) {
                                    tvExpand.setVisibility(View.VISIBLE);
                                    tvExpand.setOnClickListener(
                                            v -> showExpandedState(tv, tvExpand, desc));
                                }
                            }

                            // Show expanded state
                            private void showExpandedState(
                                    final TextView tv, final TextView tvExpand, final String desc) {
                                tv.setMaxLines(Integer.MAX_VALUE);
                                tv.setEllipsize(null);
                                tv.setText(desc);
                                if (tvExpand != null) {
                                    tvExpand.setVisibility(View.GONE);
                                }
                            }
                        });

        NCChatUIConfig.featureConfig()
                .getChatUIImageEngine()
                .loadUserPortrait(
                        holder.itemView.getContext(),
                        item.getInfo().getPortraitUri(),
                        holder.<ImageView>getView(R.id.iv_head));
        FriendApplicationStatus status = item.getInfo().getApplicationStatus();
        FriendApplicationType applicationType = item.getInfo().getApplicationType();
        if (applicationType == FriendApplicationType.RECEIVED) {
            if (status == FriendApplicationStatus.UN_HANDLED) {
                holder.setVisible(R.id.tv_result, false);
                holder.setVisible(R.id.tv_reject, true);
                holder.setVisible(R.id.tv_accept, true);
                holder.setOnClickListener(
                        R.id.tv_reject,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (onBtnClickListener != null) {
                                    onBtnClickListener.onRejectClick(holder, item, position);
                                }
                            }
                        });
                holder.setOnClickListener(
                        R.id.tv_accept,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (onBtnClickListener != null) {
                                    onBtnClickListener.onAcceptClick(holder, item, position);
                                }
                            }
                        });

            } else {
                holder.setVisible(R.id.tv_result, true);
                holder.setVisible(R.id.tv_reject, false);
                holder.setVisible(R.id.tv_accept, false);
                String text;
                if (status == FriendApplicationStatus.ACCEPTED) {
                    text = holder.itemView.getContext().getString(R.string.nc_passed);
                } else if (status == FriendApplicationStatus.REFUSED) {
                    text = holder.itemView.getContext().getString(R.string.nc_reject);
                } else {
                    text = holder.itemView.getContext().getString(R.string.nc_expired);
                }
                holder.setText(R.id.tv_result, text);
            }
        } else {
            holder.setVisible(R.id.tv_result, true);
            holder.setVisible(R.id.tv_reject, false);
            holder.setVisible(R.id.tv_accept, false);
            String text;
            if (status == FriendApplicationStatus.ACCEPTED) {
                text = holder.itemView.getContext().getString(R.string.nc_added);
            } else if (status == FriendApplicationStatus.REFUSED) {
                text = holder.itemView.getContext().getString(R.string.nc_rejected);
            } else if (status == FriendApplicationStatus.UN_HANDLED) {
                text = holder.itemView.getContext().getString(R.string.nc_waiting);
            } else {
                text = holder.itemView.getContext().getString(R.string.nc_expired);
            }
            holder.setText(R.id.tv_result, text);
        }
    }

    @Override
    public void setData(List<UiFriendApplicationInfo> data) {
        if (data == null) {
            return;
        }
        mData.clear();
        mData.addAll(data);
        notifyDataSetChanged();
    }

    public interface OnBtnClickListener {
        void onAcceptClick(ViewHolder holder, UiFriendApplicationInfo item, int position);

        void onRejectClick(ViewHolder holder, UiFriendApplicationInfo item, int position);
    }
}
