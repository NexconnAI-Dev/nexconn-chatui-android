package ai.nexconn.chatui.channel.readreceipt;

import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.message.model.ReadReceiptInfo;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.ViewModelFactory;
import ai.nexconn.chatui.base.BaseViewModelFragment;
import ai.nexconn.chatui.base.adapter.CommonAdapter;
import ai.nexconn.chatui.base.adapter.ViewHolder;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.ReadReceiptData;
import ai.nexconn.chatui.usermanage.component.CommonListComponent;
import ai.nexconn.chatui.utils.text.ChatUIDateUtils;
import ai.nexconn.chatui.widget.component.HeadComponent;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Group message read-status detail fragment.
 *
 * @since 5.30.0
 */
public class MessageReadDetailFragment extends BaseViewModelFragment<MessageReadDetailViewModel> {

    protected HeadComponent headComponent;
    protected TextView readTabText;
    protected TextView readUnderLine;
    protected TextView unReadTabText;
    protected TextView unReadUnderLine;
    protected CommonListComponent readList;
    protected CommonListComponent unreadList;
    protected TextView readReceiptNumberNone;
    protected int currentPosition = 0;
    private RecyclerView.OnScrollListener readScrollListener;
    private RecyclerView.OnScrollListener unreadScrollListener;

    protected CommonAdapter<ReadReceiptData> readAdapter =
            new CommonAdapter<ReadReceiptData>(R.layout.nc_read_receipt_member_item) {
                @Override
                public void bindData(ViewHolder holder, ReadReceiptData data, int position) {
                    MessageReadDetailFragment.this.onAdapterBindData(holder, data);
                }
            };

    protected CommonAdapter<ReadReceiptData> unreadAdapter =
            new CommonAdapter<ReadReceiptData>(R.layout.nc_read_receipt_member_item) {
                @Override
                public void bindData(ViewHolder holder, ReadReceiptData data, int position) {
                    MessageReadDetailFragment.this.onAdapterBindData(holder, data);
                }
            };

    @NonNull
    @Override
    protected MessageReadDetailViewModel onCreateViewModel(@NonNull Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(getArguments()))
                .get(MessageReadDetailViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.nc_page_message_read_detail, container, false);
        headComponent = view.findViewById(R.id.nc_head_component);
        readTabText = view.findViewById(R.id.nc_read_tab);
        unReadTabText = view.findViewById(R.id.nc_unread_tab);
        readUnderLine = view.findViewById(R.id.nc_read_tab_underline);
        unReadUnderLine = view.findViewById(R.id.nc_unread_tab_underline);
        readList = view.findViewById(R.id.nc_read_list_component);
        unreadList = view.findViewById(R.id.nc_unread_list_component);
        readReceiptNumberNone = view.findViewById(R.id.nc_read_receipt_number_none);
        readList.setAdapter(readAdapter);
        unreadList.setAdapter(unreadAdapter);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull MessageReadDetailViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        headComponent.setRightTextViewEnable(false);
        // Set up scroll listeners
        setupScrollListener(viewModel);
        readTabText.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        currentPosition = 0;
                        readList.setVisibility(View.VISIBLE);
                        unreadList.setVisibility(View.GONE);
                        updateTabText(viewModel);
                        updateMemberNoneView();
                    }
                });
        unReadTabText.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        currentPosition = 1;
                        readList.setVisibility(View.GONE);
                        unreadList.setVisibility(View.VISIBLE);
                        updateTabText(viewModel);
                        updateMemberNoneView();
                    }
                });
        viewModel
                .getReadReceiptInfoV5LiveData()
                .observe(
                        getViewLifecycleOwner(),
                        new Observer<ReadReceiptInfo>() {
                            @Override
                            public void onChanged(ReadReceiptInfo data) {
                                updateTabText(viewModel);
                                // Load initial page data for read/unread lists
                                getViewModel().getMessagesReadReceiptUsersByPage(true);
                                getViewModel().getMessagesReadReceiptUsersByPage(false);
                            }
                        });
        viewModel
                .getReadUsersLiveData()
                .observe(getViewLifecycleOwner(), data -> updateMemberList(data, true));
        viewModel
                .getUnreadUsersLiveData()
                .observe(getViewLifecycleOwner(), data -> updateMemberList(data, false));
        viewModel
                .getMemberInfoUpdateLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        info -> {
                            if (!onMemberInfoUpdate(readAdapter, info)) {
                                onMemberInfoUpdate(unreadAdapter, info);
                            }
                        });
    }

    public void onAdapterBindData(ViewHolder holder, ReadReceiptData data) {
        Context context = holder.itemView.getContext();
        // name
        String name = data.getInfo().getName();
        if (TextUtils.isEmpty(name)) {
            name = data.getInfo().getUserId();
        }
        holder.setText(R.id.nc_member_name, name);
        // time
        long time = data.getUser().getTimestamp();
        if (time > 0) {
            holder.setVisible(R.id.nc_member_time, true);
            String timeTxt = ChatUIDateUtils.getConversationFormatDate(time, context);
            holder.setText(R.id.nc_member_time, timeTxt);
        } else {
            holder.setVisible(R.id.nc_member_time, false);
        }
        // portrait
        NCChatUIConfig.featureConfig()
                .getChatUIImageEngine()
                .loadUserPortrait(
                        context,
                        data.getInfo().getPortraitUri(),
                        holder.<ImageView>getView(R.id.nc_member_portrait));
    }

    private boolean onMemberInfoUpdate(
            CommonAdapter<ReadReceiptData> adapter, GroupMemberInfo info) {
        if (TextUtils.isEmpty(info.getUserId())) {
            return false;
        }
        int position = -1;
        List<ReadReceiptData> data = adapter.getData();
        for (int i = 0; i < data.size(); i++) {
            ReadReceiptData item = data.get(i);
            if (TextUtils.equals(info.getUserId(), item.getInfo().getUserId())) {
                position = i;
                break;
            }
        }
        if (position >= 0) {
            ReadReceiptData readReceiptData = data.get(position);
            readReceiptData.setInfo(info);
            adapter.setData(position, readReceiptData);
        }
        return position >= 0;
    }

    private void updateMemberList(List<ReadReceiptData> data, boolean isRead) {
        if (isRead) readAdapter.addData(data);
        else unreadAdapter.addData(data);

        updateMemberNoneView();
    }

    private void updateMemberNoneView() {
        ReadReceiptInfo infoV5 = getViewModel().getReadReceiptInfoV5();
        if (currentPosition == 0) {
            if (infoV5 != null && infoV5.getReadCount() > 0) {
                readReceiptNumberNone.setVisibility(View.GONE);
            } else if (readAdapter.getItemCount() == 0) {
                readReceiptNumberNone.setVisibility(View.VISIBLE);
                readReceiptNumberNone.setText(R.string.nc_message_none_user_read);
            } else {
                readReceiptNumberNone.setVisibility(View.GONE);
            }
        } else if (currentPosition == 1) {
            if (infoV5 != null && infoV5.getUnreadCount() > 0) {
                readReceiptNumberNone.setVisibility(View.GONE);
            } else if (unreadAdapter.getItemCount() == 0) {
                readReceiptNumberNone.setVisibility(View.VISIBLE);
                readReceiptNumberNone.setText(R.string.nc_message_all_user_read);
            } else {
                readReceiptNumberNone.setVisibility(View.GONE);
            }
        } else {
            readReceiptNumberNone.setVisibility(View.GONE);
        }
    }

    private void setupScrollListener(MessageReadDetailViewModel viewModel) {
        readScrollListener =
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        onScrollToSecondLast(recyclerView, viewModel, true);
                    }
                };
        unreadScrollListener =
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        onScrollToSecondLast(recyclerView, viewModel, false);
                    }
                };
        // Add scroll listeners to both lists
        if (readList.getRecyclerView() != null) {
            readList.getRecyclerView().addOnScrollListener(readScrollListener);
        }
        if (unreadList.getRecyclerView() != null) {
            unreadList.getRecyclerView().addOnScrollListener(unreadScrollListener);
        }
    }

    private void onScrollToSecondLast(
            RecyclerView recyclerView, MessageReadDetailViewModel viewModel, boolean isRead) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) {
            return;
        }
        int totalItemCount = layoutManager.getItemCount();
        int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
        // Trigger callback when the list exceeds one screen and the second-to-last item is visible
        if (totalItemCount > 0 && lastVisibleItemPosition >= totalItemCount - 2) {
            viewModel.getMessagesReadReceiptUsersByPage(isRead);
        }
    }

    private void updateTabText(@NonNull MessageReadDetailViewModel viewModel) {
        ReadReceiptInfo infoV5 = viewModel.getReadReceiptInfoV5();
        if (infoV5 != null) {
            int readSize = infoV5.getReadCount();
            String readTxt = getString(R.string.nc_read_receipt) + "(" + readSize + ")";
            readTabText.setText(readTxt);
            int unreadSize = infoV5.getUnreadCount();
            String unreadTxt = getString(R.string.nc_unread_receipt) + "(" + unreadSize + ")";
            unReadTabText.setText(unreadTxt);
        } else {
            readTabText.setText(R.string.nc_read_receipt);
            unReadTabText.setText(R.string.nc_unread_receipt);
        }
        if (currentPosition == 0) {
            readTabText.setTextColor(
                    ChatUIThemeManager.getColorFromAttrId(getContext(), R.attr.nc_primary_color));
            unReadTabText.setTextColor(
                    ChatUIThemeManager.getColorFromAttrId(
                            getContext(), R.attr.nc_text_secondary_color));
            readUnderLine.setBackgroundColor(
                    ChatUIThemeManager.getColorFromAttrId(getContext(), R.attr.nc_primary_color));
            unReadUnderLine.setBackgroundColor(
                    ChatUIThemeManager.getColorFromAttrId(
                            getContext(), R.attr.nc_line_background_color));
        } else {
            readTabText.setTextColor(
                    ChatUIThemeManager.getColorFromAttrId(
                            getContext(), R.attr.nc_text_secondary_color));
            unReadTabText.setTextColor(
                    ChatUIThemeManager.getColorFromAttrId(getContext(), R.attr.nc_primary_color));
            readUnderLine.setBackgroundColor(
                    ChatUIThemeManager.getColorFromAttrId(
                            getContext(), R.attr.nc_line_background_color));
            unReadUnderLine.setBackgroundColor(
                    ChatUIThemeManager.getColorFromAttrId(getContext(), R.attr.nc_primary_color));
        }
    }

    @Override
    public void onDestroyView() {
        // Remove scroll listeners to avoid memory leaks
        if (readList != null && readList.getRecyclerView() != null && readScrollListener != null) {
            readList.getRecyclerView().removeOnScrollListener(readScrollListener);
        }
        if (unreadList != null
                && unreadList.getRecyclerView() != null
                && unreadScrollListener != null) {
            unreadList.getRecyclerView().removeOnScrollListener(unreadScrollListener);
        }
        super.onDestroyView();
    }
}
